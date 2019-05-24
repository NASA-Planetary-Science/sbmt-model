package edu.jhuapl.sbmt.model.lidar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.collect.ImmutableList;

import vtk.vtkUnstructuredGrid;

import edu.jhuapl.saavtk.io.readers.IpwgPlyReader;
import edu.jhuapl.saavtk.util.FileUtil;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.sbmt.lidar.BasicLidarPoint;
import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.lidar.hyperoctree.FSHyperPointWithFileTag;
import edu.jhuapl.sbmt.util.TimeUtil;

import glum.item.IdGenerator;
import glum.item.IncrIdGenerator;

/**
 * Class that provides a collection of utility methods needed to serialize /
 * deserialize lidar data from a variety of file formats.
 *
 * @author lopeznr1
 */
public class LidarFileUtil
{
	/**
	 * Returns the file that should be used for the specified basename and index.
	 */
	public static File getFileForIndex(File aFolder, String aBaseName, int aIdx)
	{
		File retFile = new File(aFolder, aBaseName + aIdx + ".txt");
		return retFile;
	}

	/**
	 * Loads a list of LidarPoints from the specified text file.
	 *
	 * @See {@link LIDARTextInputType}
	 *
	 * @param aFile The file of interest.
	 * @return The list of the loaded LidarPoints.
	 * @throws IOException
	 */
	public static List<LidarPoint> loadLidarPointsFromAsciiFile(File aFile, TrackFileType aFileType) throws IOException
	{
		List<LidarPoint> retL = new ArrayList<>();

		LIDARTextInputType textInputType = LIDARTextInputType.valueOf(aFileType.name());
		try (BufferedReader aReader = new BufferedReader(new InputStreamReader(new FileInputStream(aFile))))
		{
			String lineRead;
			while ((lineRead = aReader.readLine()) != null)
			{
				LidarPoint tmpLP = textInputType.parseLine(lineRead);
				if (tmpLP != null)
					retL.add(tmpLP);
			}
		}

		return retL;
	}

	/**
	 * Loads a list of LidarPoints from the specified (raw binary) file.
	 *
	 * @param aFile The file of interest.
	 * @return The list of the loaded LidarPoints.
	 * @throws IOException
	 */
	public static List<LidarPoint> loadLidarPointsFromBinaryFile(File aFile) throws IOException
	{
		List<LidarPoint> retL = new ArrayList<>();

		DataInputStream aStream = new DataInputStream(new BufferedInputStream(new FileInputStream(aFile)));

		while (true)
		{
			double time = 0;
			double[] target = { 0.0, 0.0, 0.0 };
			double[] scpos = { 0.0, 0.0, 0.0 };

			try
			{
				time = FileUtil.readDoubleAndSwap(aStream);
			}
			catch (EOFException e)
			{
				break;
			}

			try
			{
				target[0] = FileUtil.readDoubleAndSwap(aStream);
				target[1] = FileUtil.readDoubleAndSwap(aStream);
				target[2] = FileUtil.readDoubleAndSwap(aStream);
				scpos[0] = FileUtil.readDoubleAndSwap(aStream);
				scpos[1] = FileUtil.readDoubleAndSwap(aStream);
				scpos[2] = FileUtil.readDoubleAndSwap(aStream);
			}
			catch (IOException e)
			{
				aStream.close();
				throw e;
			}

			double range = 0; // TODO

			LidarPoint tmpLP = new BasicLidarPoint(target, scpos, time, range, 0);
			retL.add(tmpLP);
		}
		aStream.close();

		return retL;
	}

	/**
	 * Loads a list of LidarPoints from the specified PLY file.
	 *
	 * @param aFile The file of interest.
	 * @return The list of the loaded LidarPoints.
	 * @throws IOException
	 */
	public static List<LidarPoint> loadLidarPointsFromPlyFile(File aFile, int aFileId) throws IOException
	{
		IpwgPlyReader reader = new IpwgPlyReader();
		reader.SetFileName(aFile.getAbsolutePath());
		reader.Update();
		vtkUnstructuredGrid polyData = reader.getOutputAsUnstructuredGrid();

		List<LidarPoint> retL = new ArrayList<>();

		double range = 0; // TODO
		for (int i = 0; i < polyData.GetNumberOfPoints(); i += 10)
		{
			double[] pt = polyData.GetPoint(i);

			LidarPoint tmpLP = new FSHyperPointWithFileTag(pt[0], pt[1], pt[2], 0, 0, 0, 0, range,
					polyData.GetPointData().GetArray(0).GetTuple1(i), aFileId);
			retL.add(tmpLP);
		}

		return retL;
	}

	/**
	 * Utility method that returns the Tracks loaded from the specified files.
	 *
	 * @throws IOException
	 */
	public static List<LidarTrack> loadLidarTracksFromFiles(LidarTrackManager aManager, File[] aFileArr,
			TrackFileType aTrackFileType) throws IOException
	{
		// Determine the starting id value that should be used
		int nextId = 1;
		for (LidarTrack aTrack : aManager.getAllItems())
		{
			if (aTrack.getId() >= nextId)
				nextId = aTrack.getId() + 1;
		}
		IdGenerator tmpIG = new IncrIdGenerator(nextId);

		// Load the files
		List<LidarTrack> retTrackL = new ArrayList<>();
		for (File aFile : aFileArr)
		{
			// Load the LidarPoints
			List<LidarPoint> tmpPointL;
			if (aTrackFileType == TrackFileType.BINARY)
				tmpPointL = loadLidarPointsFromBinaryFile(aFile);
			else if (aTrackFileType == TrackFileType.PLY)
				tmpPointL = loadLidarPointsFromPlyFile(aFile, 0);
			else if (aTrackFileType == TrackFileType.OLA_LEVEL_2)
				tmpPointL = loadLidarPointsFromOla2File(aFile);
			else // variations on text input
				tmpPointL = loadLidarPointsFromAsciiFile(aFile, aTrackFileType);

			List<String> tmpSourceL = ImmutableList.of(aFile.toString());

			LidarTrack tmpTrack = new LidarTrack(tmpIG.getNextId(), tmpPointL, tmpSourceL);
			retTrackL.add(tmpTrack);
		}

		return retTrackL;
	}

	/**
	 * Loads a list of LidarPoints from the specified Ola2 file.
	 *
	 * @param aFile The file of interest.
	 * @return The list of the loaded LidarPoints.
	 * @throws IOException
	 */
	public static List<LidarPoint> loadLidarPointsFromOla2File(File aFile) throws IOException
	{
		OLAL2File l2File = new OLAL2File(aFile.toPath());
		List<LidarPoint> retL = new ArrayList<>();
		retL.addAll(l2File.read(1. / 1000.));

		return retL;
	}

	/**
	 * Save track as binary file. Each record consists of 7 double precision
	 * values as follows:
	 * <UL>
	 * <LI>1. ET
	 * <LI>2. X target
	 * <LI>3. Y target
	 * <LI>4. Z target
	 * <LI>5. X sc pos
	 * <LI>6. Y sc pos
	 * <LI>7. Z sc pos
	 * </UL>
	 * If transformPoint is true, then the lidar points (not scpos) are
	 * translated using the current radial offset and translation before being
	 * saved out. If false, the original points are saved out unmodified.
	 *
	 * @param aManager
	 * @param aFile
	 * @param aTrack
	 * @param transformPoint
	 * @throws IOException
	 */
	public static void saveTrackToBinaryFile(LidarTrackManager aManager, File aFile, LidarTrack aTrack,
			boolean transformPoint) throws IOException
	{
		aFile = new File(aFile.getAbsolutePath() + ".bin");
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(aFile)));

		Vector3D tmpVect = aManager.getTranslation(aTrack);
		for (LidarPoint aLP : aTrack.getPointList())
		{
			double[] target = aLP.getTargetPosition().toArray();
			double[] scpos = aLP.getSourcePosition().toArray();
			if (transformPoint)
			{
				target = aManager.transformLidarPoint(tmpVect, target);
				scpos = aManager.transformScpos(tmpVect, scpos, target);
			}

			FileUtil.writeDoubleAndSwap(out, aLP.getTime());
			FileUtil.writeDoubleAndSwap(out, target[0]);
			FileUtil.writeDoubleAndSwap(out, target[1]);
			FileUtil.writeDoubleAndSwap(out, target[2]);
			FileUtil.writeDoubleAndSwap(out, scpos[0]);
			FileUtil.writeDoubleAndSwap(out, scpos[1]);
			FileUtil.writeDoubleAndSwap(out, scpos[2]);
		}

		out.close();
	}

	/**
	 * Saves the list of Tracks to the specified folder. Each file will be saved
	 * to a separate file.
	 */
	public static void saveTracksToFolder(LidarTrackManager aManager, File aFolder, List<LidarTrack> aTrackL,
			String aBaseName, boolean transformPoint) throws IOException
	{
		for (int aIdx = 0; aIdx < aTrackL.size(); aIdx++)
		{
			File tmpFile = getFileForIndex(aFolder, aBaseName, aIdx);

			LidarTrack tmpTrack = aTrackL.get(aIdx);
			List<LidarTrack> tmpTrackL = ImmutableList.of(tmpTrack);
			saveTracksToTextFile(aManager, tmpFile, tmpTrackL, transformPoint);
		}
	}

	/**
	 * Saves the list of Tracks to the specified file.
	 * <P>
	 * If transformPoint is true, then the lidar points (not scpos) are
	 * translated using the current radial offset and translation before being
	 * saved out. If false, the original points are saved out unmodified.
	 *
	 * @param aManager
	 * @param aFile
	 * @param aTrackL
	 * @param transformPoint
	 *
	 * @throws IOException
	 */
	public static void saveTracksToTextFile(LidarTrackManager aManager, File aFile, List<LidarTrack> aTrackL,
			boolean transformPoint) throws IOException
	{
		FileWriter fstream = new FileWriter(aFile);
		BufferedWriter out = new BufferedWriter(fstream);

		String newline = System.getProperty("line.separator");

		for (LidarTrack aTrack : aTrackL)
		{
			// Save each individual lidar point
			Vector3D tmpVect = aManager.getTranslation(aTrack);
			for (LidarPoint aLP : aTrack.getPointList())
			{
				double[] target = aLP.getTargetPosition().toArray();
				double[] scpos = aLP.getSourcePosition().toArray();
				if (transformPoint)
				{
					target = aManager.transformLidarPoint(tmpVect, target);
					scpos = aManager.transformScpos(tmpVect, scpos, target);
				}

				String timeString = TimeUtil.et2str(aLP.getTime());

				out.write(timeString + " " + target[0] + " " + target[1] + " " + target[2] + " " + scpos[0] + " " + scpos[1]
						+ " " + scpos[2] + " "
						+ MathUtil.distanceBetween(aLP.getSourcePosition().toArray(), aLP.getTargetPosition().toArray())
						+ newline);
			}
		}
		out.close();
	}

}
