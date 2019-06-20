package edu.jhuapl.sbmt.model.lidar;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;

import com.google.common.base.Stopwatch;

import edu.jhuapl.saavtk.gui.ProgressBarSwingWorker;
import edu.jhuapl.saavtk.model.LidarDataSource;
import edu.jhuapl.saavtk.model.PointInRegionChecker;
import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.lidar.BasicLidarPoint;
import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.lidar.hyperoctree.FSHyperPointWithFileTag;
import edu.jhuapl.sbmt.lidar.hyperoctree.FSHyperTreeSkeleton;
import edu.jhuapl.sbmt.lidar.hyperoctree.hayabusa2.Hayabusa2LidarPoint;
import edu.jhuapl.sbmt.util.TimeUtil;

import glum.item.IdGenerator;
import glum.item.IncrIdGenerator;

/**
 * Class that provides a collection of utility methods needed to perform query
 * for lidar data.
 *
 * @author lopeznr1
 */
public class LidarQueryUtil
{
	/**
	 * Runs the specified (classic) query and installs formed Tracks into the
	 * specified LidarTrackManager.
	 * <P>
	 * A number of the methods in this class are derived from various derivatives
	 * of the class LidarSearchDataCollection (prior to 2019Mar24). The divergent
	 * poorly designed classes and corresponding methods have been refactored to
	 * various utility classes.
	 */
	public static void executeQueryClassic(LidarTrackManager aManager, LidarSearchParms aSearchParms,
			PointInRegionChecker aPointInRegionChecker) throws IOException
	{
		// Run the query and install the Tracks
		IdGenerator tmpIG = new IncrIdGenerator(1);
		List<LidarTrack> tmpTrackL = runQueryClassic(tmpIG, aSearchParms, aPointInRegionChecker);
		aManager.setAllItems(tmpTrackL);
	}

	/**
	 * Runs the specified (hyper tree) query and installs formed Tracks into the
	 * specified LidarTrackManager.
	 * <P>
	 * TODO: Add more details to the comments.
	 */
	public static void executeQueryHyperTree(LidarTrackManager aManager, LidarSearchParms aSearchParms,
			JComponent aParentComp, DataType aDataType, FSHyperTreeSkeleton aSkeleton,
			PointInRegionChecker aPointInRegionChecker) throws IOException
	{
		// In the old LidarSearchDataCollection class the cubeList came from a
		// predetermined set of cubes all of equal size.
		// Here it corresponds to the list of leaves of a hypertree that intersect
		// the bounding box of the user selection area.
		IdGenerator tmpIG = new IncrIdGenerator(1);
		List<LidarTrack> tmpTrackL = runQueryHyperTree(tmpIG, aSearchParms, aDataType, aSkeleton, aParentComp,
				aPointInRegionChecker);
		aManager.setAllItems(tmpTrackL);
	}

	/**
	 * TODO: Add documentation.
	 */
	public static Map<String, LidarDataSource> getLidarDataSourceMap(PolyhedralModel aPolyhedralModel)
	{
		BodyViewConfig tmpBodyViewConfig = (BodyViewConfig) aPolyhedralModel.getConfig();

		Map<String, String> namePathM = tmpBodyViewConfig.lidarSearchDataSourceMap;
		Map<String, LidarDataSource> retM = new LinkedHashMap<>();
		for (String aName : namePathM.keySet())
			retM.put(aName, new LidarDataSource(aName, namePathM.get(aName)));

		return retM;
	}

	/**
	 * Runs a query and returns the appropriate Tracks.
	 * <P>
	 * TODO: Add more details to the comments.
	 */
	public static List<LidarTrack> runQueryClassic(IdGenerator aIdGenerator, LidarSearchParms aSearchParms,
			PointInRegionChecker aPointInRegionChecker) throws IOException
	{
		Set<Integer> cubeSet = aSearchParms.getCubeSet();
		LidarDataSource dataSource = aSearchParms.getDataSource();
		double begTime = aSearchParms.getBegTime();
		double endTime = aSearchParms.getEndTime();
//		double minRange = aSearchParms.getMinRange();
//		double maxRange = aSearchParms.getMaxRange();
		double timeSeparationBetweenTracks = aSearchParms.getTimeSeparationBetweenTracks();
		int minTrackLen = aSearchParms.getMinTrackLen();

		List<LidarPoint> tmpPointL = new ArrayList<>();
		Map<LidarPoint, String> tmpPointSourceM = new HashMap<>();

		int timeindex = 0;
		int xindex = 1;
		int yindex = 2;
		int zindex = 3;
		int scxindex = 4;
		int scyindex = 5;
		int sczindex = 6;

		for (Integer cubeid : cubeSet)
		{
			String filename = dataSource.getPath() + "/" + cubeid + ".lidarcube";
			File file = FileCache.getFileFromServer(filename);
			String source = file.toString();

			InputStream fs = new FileInputStream(file.getAbsolutePath());
			InputStreamReader isr = new InputStreamReader(fs);
			BufferedReader in = new BufferedReader(isr);

			String lineRead;
			while ((lineRead = in.readLine()) != null)
			{
				String[] vals = lineRead.trim().split("\\s+");

				// Skip to next if the time constraint is not satisfied
				double time = TimeUtil.str2et(vals[timeindex]);
				if (time < begTime || time > endTime)
					continue;

				double[] scpos = new double[3];
				double[] target = new double[3];
				target[0] = Double.parseDouble(vals[xindex]);
				target[1] = Double.parseDouble(vals[yindex]);
				target[2] = Double.parseDouble(vals[zindex]);
				scpos[0] = Double.parseDouble(vals[scxindex]);
				scpos[1] = Double.parseDouble(vals[scyindex]);
				scpos[2] = Double.parseDouble(vals[sczindex]);
				double range = 0; // TODO

				// Skip to next if region constraint is not satisfied
				if (aPointInRegionChecker != null && aPointInRegionChecker.checkPointIsInRegion(target) == false)
					continue;

				LidarPoint tmpLP = new BasicLidarPoint(target, scpos, time, range, 0);
				tmpPointL.add(tmpLP);
				tmpPointSourceM.put(tmpLP, source);
			}
			in.close();
		}

		List<LidarTrack> retTrackL = LidarTrackUtil.formTracks(aIdGenerator, tmpPointL, tmpPointSourceM,
				timeSeparationBetweenTracks, minTrackLen);
		return retTrackL;
	}

	/**
	 * Runs a query and returns the appropriate Tracks.
	 * <P>
	 * TODO: Add more details to the comments.
	 * <P>
	 * The field aCubeList corresponds to the list of leaves of an hypertree /
	 * octree that intersect the bounding box of the user selection area.
	 */
	public static List<LidarTrack> runQueryHyperTree(IdGenerator aIdGenerator, LidarSearchParms aSearchParms,
			DataType aDataType, FSHyperTreeSkeleton aSkeleton, Component aParentComp,
			PointInRegionChecker aPointInRegionChecker) throws IOException
	{
		Set<Integer> cubeSet = aSearchParms.getCubeSet();
		double begTime = aSearchParms.getBegTime();
		double endTime = aSearchParms.getEndTime();
		double minRange = aSearchParms.getMinRange();
		double maxRange = aSearchParms.getMaxRange();
		double timeSeparationBetweenTracks = aSearchParms.getTimeSeparationBetweenTracks();
		int minTrackLen = aSearchParms.getMinTrackLen();

		Map<Integer, Set<LidarPoint>> filesWithPointM = new HashMap<>();

		ProgressBarSwingWorker dataLoader = new ProgressBarSwingWorker(aParentComp,
				"Loading lidar datapoints (" + cubeSet.size() + " individual chunks)", false) {
			@Override
			protected Void doInBackground() throws Exception
			{
				Stopwatch sw = new Stopwatch();
				sw.start();

				int cnt = 0;
				for (Integer cidx : cubeSet)
				{
					Path leafPath = aSkeleton.getNodeById(cidx).getPath();

					// System.out.println("Hayabusa2LidarSearchDataCollection:
					// setLidarData: Loading data partition
					// "+(cnt+1)+"/"+cubeList.size()+" (id="+cidx+")
					// \""+leafPath+"\"");
					Path dataFilePath = leafPath.resolve("data");
					File dataFile = FileCache.getFileFromServer(dataFilePath.toString());
					if (!dataFile.exists())
						dataFile = FileCache.getFileFromServer(FileCache.FILE_PREFIX + dataFilePath.toString());
					List<LidarPoint> pts = readDataFile(aDataType, dataFile, aPointInRegionChecker,
							new double[] { begTime, endTime });
					for (int i = 0; i < pts.size(); i++)
					{
						FSHyperPointWithFileTag currPt = (FSHyperPointWithFileTag) pts.get(i);

						// Skip to next if the range constraint is not satisfied
						boolean isPass = true;
						isPass &= Double.isNaN(minRange) == true || currPt.getRangeToSC() > minRange;
						isPass &= Double.isNaN(maxRange) == true || currPt.getRangeToSC() < maxRange;
						if (isPass == false)
							continue;

						int fileNum = currPt.getFileNum();

						// Add the LidarPoint into filesWithPointM
						Set<LidarPoint> tmpS = filesWithPointM.get(fileNum);
						if (tmpS == null)
						{
							tmpS = new HashSet<>();
							filesWithPointM.put(fileNum, tmpS);
						}

						if (tmpS.contains(currPt) == false)
							tmpS.add(currPt);
					}
					//
					cnt++;
					double progressPercentage = ((double) cnt / (double) cubeSet.size() * 100);
					setProgress((int) progressPercentage);
					if (isCancelled())
						break;
				}

				cancel(true);

				// System.out.println("Data Reading Time="+sw.elapsedMillis()+"
				// ms");
				sw.reset();
				sw.start();

				return null;
			}

		};
		dataLoader.executeDialog();
		// System.out.println("Hayabusa2LidarSearchDataCollection: setLidarData:
		// before while loop");

		List<LidarTrack> retTrackL = LidarTrackUtil.formTracks(aIdGenerator, filesWithPointM, aSkeleton,
				timeSeparationBetweenTracks, minTrackLen);
		return retTrackL;
	}

	// TODO: Add javadoc
	private static List<LidarPoint> readDataFile(DataType aDataType, File aInputFile,
			PointInRegionChecker aPointInRegionChecker, double[] aTimeLimitArr)
	{
		List<LidarPoint> retPointL = new ArrayList<>();
		try
		{
			DataInputStream stream = new DataInputStream(new FileInputStream(aInputFile));
			while (true)
			{
				LidarPoint tmpLP;
				if (aDataType == DataType.Hayabusa)
					tmpLP = new Hayabusa2LidarPoint(stream);
				else if (aDataType == DataType.Ola)
					tmpLP = new FSHyperPointWithFileTag(stream);
//				else if (aDataType == DataType.Mola)
//					tmpLP = new MolaFSHyperPoint(stream);
				else
					throw new IOException("Unsupported DataType: " + aDataType);

				// Skip to next if the time constraint is not satisfied
				if (tmpLP.getTime() < aTimeLimitArr[0] || tmpLP.getTime() > aTimeLimitArr[1])
					continue;

				// Skip to next if region constraint is not satisfied
				if (aPointInRegionChecker != null
						&& aPointInRegionChecker.checkPointIsInRegion(tmpLP.getTargetPosition().toArray()) == false)
					continue;

				retPointL.add(tmpLP);
			}
		}
		catch (IOException aExp)
		{
			if (!aExp.getClass().equals(EOFException.class))
				aExp.printStackTrace();
		}

		// // vtkPolyDataWriter writer=new vtkPolyDataWriter();
		// writer.SetFileName("/Users/zimmemi1/Desktop/test.vtk");
		// writer.SetFileTypeToBinary(); writer.SetInputData(polyData);
		// writer.Write();
		return retPointL;
	}

	/**
	 * Enum that describes the type of data to process.
	 */
	public enum DataType
	{
		Hayabusa,

		Mola,

		Ola,
	}

}
