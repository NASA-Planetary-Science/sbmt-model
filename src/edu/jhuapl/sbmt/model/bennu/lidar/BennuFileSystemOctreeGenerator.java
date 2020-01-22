package edu.jhuapl.sbmt.model.bennu.lidar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import vtk.vtkUnstructuredGrid;
import vtk.vtkUnstructuredGridWriter;

import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.saavtk.util.BoundingBox;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.NativeLibraryLoader;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.lidar.FileSystemOctreeGenerator;
import edu.jhuapl.sbmt.model.bennu.shapeModel.Bennu;

public class BennuFileSystemOctreeGenerator
{

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
		NativeLibraryLoader.loadHeadlessVtkLibraries();
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
			System.out.println("Total points written so far = " + tree.getTotalPointsWritten());// TODO:
																														// close
																														// down
																														// all
																														// DataOutputStreams
			System.out.println(
					"Total MB written so far = " + tree.convertBytesToMB(tree.getDataFilePath().toFile().length()));
			System.out.println();
			sw.reset();
		}
		long rootFileSizeBytes = tree.getDataFilePath().toFile().length();

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
		writer.SetFileName(tree.getFullyResolvedOutputDirectory().resolve("tree.vtk").toString());
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
