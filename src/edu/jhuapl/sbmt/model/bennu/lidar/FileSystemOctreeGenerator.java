package edu.jhuapl.sbmt.model.bennu.lidar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import vtk.vtkCellArray;
import vtk.vtkHexahedron;
import vtk.vtkPoints;
import vtk.vtkStringArray;
import vtk.vtkUnstructuredGrid;
import vtk.vtkUnstructuredGridWriter;

import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.saavtk.util.BoundingBox;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.NativeLibraryLoader;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.lidar.DataOutputStreamPool;
import edu.jhuapl.sbmt.lidar.FileSystemOctreeNode;
import edu.jhuapl.sbmt.lidar.hyperoctree.ola.OlaPointList;
import edu.jhuapl.sbmt.model.bennu.shapeModel.Bennu;

public class FileSystemOctreeGenerator
{

	final Path nearsdcRootDirectory;
	final Path fullyResolvedOutputDirectory;
	final int maxNumberOfPointsPerLeaf;
	final BoundingBox boundingBox;
	final int maxNumberOfOpenFiles;
	FileSystemOctreeNode root;
	final DataOutputStreamPool streamManager;
	long totalPointsWritten = 0;

	public FileSystemOctreeGenerator(Path nearsdcRootDirectory, Path relativeTreeConstructionDirectory,
			int maxNumberOfPointsPerLeaf, BoundingBox bbox, int maxNumFiles) throws IOException
	{
		this.fullyResolvedOutputDirectory = nearsdcRootDirectory.resolve(relativeTreeConstructionDirectory);
		this.nearsdcRootDirectory = nearsdcRootDirectory;
		this.maxNumberOfPointsPerLeaf = maxNumberOfPointsPerLeaf;
		boundingBox = bbox;
		maxNumberOfOpenFiles = maxNumFiles;
		streamManager = new DataOutputStreamPool(maxNumberOfOpenFiles);
		root = new FileSystemOctreeNode(fullyResolvedOutputDirectory, bbox, maxNumberOfPointsPerLeaf, streamManager);
		root.writeBounds();
	}

	public void addPointsFromFileToRoot(Path inputFilePath) throws IOException
	{
		addPointsFromFileToRoot(inputFilePath, Integer.MAX_VALUE);
	}

	public void addPointsFromFileToRoot(Path inputFilePath, int nmax) throws IOException
	{ // first add all points to the root node, then expand the tree
		OlaPointList pointList = new OlaPointList();
		pointList.appendFromFile(inputFilePath);
		int limit = Math.min(pointList.getNumberOfPoints(), nmax);
		for (int i = 0; i < limit; i++)
		{
			if ((i % 200000) == 0)
				System.out.println((int) ((double) i / (double) pointList.getNumberOfPoints() * 100) + "% complete : " + i
						+ "/" + limit);
			root.addPoint(pointList.getPoint(i));
			totalPointsWritten++;
		}
	}

	public void expand() throws IOException
	{
		expandNode(root);
	}

	public void expandNode(FileSystemOctreeNode node) throws IOException
	{ // depth-first recursion, so we limit the number of open output files to 8
		System.out.println(node.getSelfPath() + " " + convertBytesToMB(node.getDataFilePath().toFile().length()) + " MB");
		if (node.getNumPoints() > maxNumberOfPointsPerLeaf)
		{
			node.split();
			for (int i = 0; i < 8; i++)
				if (node.getChildren()[i] != null)
					expandNode(node.getChildren()[i]);
		}
	}

	public int getNumberOfNodes()
	{
		return getAllNodes().size();
	}

	public long getNumberOfBytes()
	{
		List<FileSystemOctreeNode> nodeList = getAllNonEmptyLeafNodes();
		long total = 0;
		for (FileSystemOctreeNode node : nodeList)
		{
			total += node.getDataFilePath().toFile().length();
		}
		return total;
	}

	public double convertBytesToMB(long bytes)
	{
		return (double) bytes / (double) (1024 * 1024);
	}

	public void writeStatistics(Path outputFilePath)
	{
		try
		{
			FileWriter writer = new FileWriter(outputFilePath.toFile());
			List<FileSystemOctreeNode> nodeList = getAllNonEmptyLeafNodes();
			for (FileSystemOctreeNode node : nodeList)
			{
				writer.write(String.valueOf(convertBytesToMB(node.getSelfPath().toFile().length())));
			}
			writer.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public List<FileSystemOctreeNode> getAllNodes()
	{
		List<FileSystemOctreeNode> nodeList = Lists.newArrayList();
		getAllNodes(root, nodeList);
		return nodeList;
	}

	void getAllNodes(FileSystemOctreeNode node, List<FileSystemOctreeNode> nodeList)
	{
		nodeList.add(node);
		for (int i = 0; i < 8; i++)
			if (node.getChildren()[i] != null)
				nodeList.add(node.getChildren()[i]);
	}

	public vtkUnstructuredGrid getAllNonEmptyLeavesAsUnstructuredGrid()
	{
		List<FileSystemOctreeNode> nodeList = getAllNonEmptyLeafNodes();
		vtkPoints points = new vtkPoints();
		vtkCellArray cells = new vtkCellArray();
		vtkStringArray paths = new vtkStringArray();
		for (FileSystemOctreeNode node : nodeList)
		{
			vtkHexahedron hex = new vtkHexahedron();
			for (int i = 0; i < 8; i++)
			{
				Vector3D crn = node.getCorner(i);
				int id = points.InsertNextPoint(crn.getX(), crn.getY(), crn.getZ());
				hex.GetPointIds().SetId(i, id);
			}
			cells.InsertNextCell(hex);
			String relativePath = "/" + node.getSelfPath().toString().replace(nearsdcRootDirectory.toString(), "");
			System.out.println(relativePath);
			paths.InsertNextValue(relativePath);
		}
		//
		vtkUnstructuredGrid grid = new vtkUnstructuredGrid();
		grid.SetPoints(points);
		grid.SetCells(new vtkHexahedron().GetCellType(), cells);
		grid.GetCellData().AddArray(paths);
		return grid;
	}

	public List<FileSystemOctreeNode> getAllNonEmptyLeafNodes()
	{
		List<FileSystemOctreeNode> nodeList = Lists.newArrayList();
		getAllNonEmptyLeafNodes(root, nodeList);
		return nodeList;
	}

	void getAllNonEmptyLeafNodes(FileSystemOctreeNode node, List<FileSystemOctreeNode> nodeList)
	{
		if (!node.isLeaf())
			for (int i = 0; i < 8; i++)
				getAllNonEmptyLeafNodes(node.getChildren()[i], nodeList);
		else if (node.getNumPoints() > 0)
			nodeList.add(node);
	}

	public void commit() throws IOException
	{
		streamManager.closeAllStreams();// close any files that are still open
		finalCommit(root);
	}

	void finalCommit(FileSystemOctreeNode node) throws IOException
	{
		if (!node.isLeaf())
			for (int i = 0; i < 8; i++)
				finalCommit(node.getChildren()[i]);
		else
		{
			File dataFile = node.getDataFilePath().toFile(); // clean up any data
																				// files with zero
																				// points
			if (dataFile.length() == 0l)
				dataFile.delete();
		}
	}

	public static void main(String[] args) throws IOException
	{
		if (args.length != 5)
		{
			System.out.println("Arguments:");
			System.out.println("   OLA L2 file directory");
			System.out.println("   Top-level nearsdc data output path, e.g. /project/nearsdc/data/");
			System.out.println(
					"   Path where tree is to be built, relative to nearsdc data path, e.g. GASKELL/RQ36_V3/OLA/tree");
			System.out.println("   Desired max file size (MB)");
			System.out.println("   Number of raw L2 files to process (-1 means all)");
			return;
		}

		Path olaL2FileDirectory = Paths.get(args[0]);
		Path nearsdcDataRootDirectory = Paths.get(args[1]);
		Path treeRootDirectory = Paths.get(args[2]);
		int dataFileMBLimit = Integer.valueOf(args[3]);
		int nFilesToProcess = Integer.valueOf(args[4]);
		System.out.println("OLA L2 file directory = " + olaL2FileDirectory);
		System.out.println("Nearsdc root data directory = " + nearsdcDataRootDirectory);
		System.out.println("Tree root directory = " + treeRootDirectory);
		System.out.println("Desired max file size = " + dataFileMBLimit + " MB");
		System.out.println();

		//
		NativeLibraryLoader.loadVtkLibrariesHeadless();
		int dataFileByteLimit = dataFileMBLimit * 1024 * 1024;
		int maxPointsPerLeaf = dataFileByteLimit / (8 * 4); // three doubles for
																				// scpos, three
																				// doubles for
																				// tgpos, one double
																				// for time, and one
																				// double for
																				// intensity
		int maxNumFiles = 32;
		//
		Configuration.setAPLVersion(true);
		ShapeModelBody body = ShapeModelBody.RQ36;
		ShapeModelType author = ShapeModelType.GASKELL;
		String version = "V3 Image";
		Bennu bennu = new Bennu(SmallBodyViewConfig.getSmallBodyConfig(body, author, version));
		BoundingBox bbox = new BoundingBox(bennu.getBoundingBox().getBounds());
		System.out.println("Shape model info:");
		System.out.println("  Body = " + body);
		System.out.println("  Author = " + author);
		System.out.println("  Version = \"" + version + "\"");
		System.out.println("Original bounding box = " + bbox);
		double bboxSizeIncrease = 0.05;
		bbox.increaseSize(bboxSizeIncrease);
		System.out.println("Bounding box diagonal length increase = " + bboxSizeIncrease);
		System.out.println("Rescaled bounding box = " + bbox);
		System.out.println();
		FileSystemOctreeGenerator tree = new FileSystemOctreeGenerator(nearsdcDataRootDirectory, treeRootDirectory,
				maxPointsPerLeaf, bbox, maxNumFiles);
		//
		List<File> fileList = Lists.newArrayList();
		Collection<File> fileCollection = FileUtils.listFiles(olaL2FileDirectory.toFile(),
				new WildcardFileFilter("OBJLIST*.l2"), null);
		fileCollection
				.addAll(FileUtils.listFiles(olaL2FileDirectory.toFile(), new WildcardFileFilter("g_0085*.l2"), null));
		for (File f : fileCollection)
		{
			System.out.println("Adding file " + f + " to the processing queue");
			fileList.add(f);
		}
		//
		Stopwatch sw = new Stopwatch();
		int numFiles = fileList.size();
		if (nFilesToProcess > -1)
			numFiles = nFilesToProcess;
		for (int i = 0; i < numFiles; i++)
		{
			sw.start();
			Path inputPath = Paths.get(fileList.get(i).toString());
			System.out.println("File " + (i + 1) + "/" + numFiles + ": " + inputPath);
			tree.addPointsFromFileToRoot(inputPath);
			System.out.println("Elapsed time = " + sw.elapsedTime(TimeUnit.SECONDS) + " s");
			System.out.println("Total points written so far = " + tree.totalPointsWritten);// TODO:
																														// close
																														// down
																														// all
																														// DataOutputStreams
			System.out.println(
					"Total MB written so far = " + tree.convertBytesToMB(tree.root.getDataFilePath().toFile().length()));
			System.out.println();
			sw.reset();
		}
		long rootFileSizeBytes = tree.root.getDataFilePath().toFile().length();

		sw.start();
		System.out.println("Expanding tree.");
		tree.expand();
		System.out.println("Done expanding tree. Time elapsed=" + sw.elapsedTime(TimeUnit.SECONDS) + " s");
		System.out.println("Cleaning up.");
		System.out.println();
		tree.commit(); // clean up any empty or open data files

		//
		vtkUnstructuredGrid grid = tree.getAllNonEmptyLeavesAsUnstructuredGrid();
		vtkUnstructuredGridWriter writer = new vtkUnstructuredGridWriter();
		writer.SetFileName(tree.fullyResolvedOutputDirectory.resolve("tree.vtk").toString());
		writer.SetFileTypeToBinary();
		writer.SetInputData(grid);
		writer.Write();

		System.out.println("Total # of leaves=" + grid.GetNumberOfCells());
		System.out.println("Total MB stored = " + tree.convertBytesToMB(tree.getNumberOfBytes()));
		System.out.println("Total MB initially copied = " + tree.convertBytesToMB(rootFileSizeBytes) + " MB");

		Path statisticsOutputPath = nearsdcDataRootDirectory.resolve(treeRootDirectory).resolve("stats");
		tree.writeStatistics(statisticsOutputPath);
		System.out.println("Statistics written to " + statisticsOutputPath);
	}

}
