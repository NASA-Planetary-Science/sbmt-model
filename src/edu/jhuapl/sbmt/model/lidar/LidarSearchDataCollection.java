package edu.jhuapl.sbmt.model.lidar;

import java.awt.Color;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialFitter;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.optim.nonlinear.vector.jacobian.LevenbergMarquardtOptimizer;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import vtk.vtkActor;
import vtk.vtkProp;
import vtk.vtkUnstructuredGrid;

import edu.jhuapl.saavtk.io.readers.IpwgPlyReader;
import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.model.PointInRegionChecker;
import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.pick.PickEvent;
import edu.jhuapl.saavtk.pick.PickUtil;
import edu.jhuapl.saavtk.util.ColorUtil;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.FileUtil;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.Point3D;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.lidar.BasicLidarPoint;
import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.lidar.hyperoctree.FSHyperPointWithFileTag;
import edu.jhuapl.sbmt.util.TimeUtil;
import edu.jhuapl.sbmt.util.gravity.Gravity;

public class LidarSearchDataCollection extends AbstractModel
{
	// Reference vars
	private PolyhedralModel refSmallBodyModel;

	// State vars
	protected List<Track> tracks;
	private ImmutableSet<Track> selectedTrackS;

	private Map<Track, Vector3D> translationM;
	private Map<Track, Double> errorM;
	private double radialOffset;

	// VTK vars
	private VtkPointPainter vPointPainter;
	private VtkTrackPainter vTrackPainter;
	private List<vtkProp> vActorL;

	protected List<LidarPoint> originalPoints = new ArrayList<>();
	protected Map<LidarPoint, Integer> originalPointsSourceFiles = Maps.newHashMap();

	private String dataSource;
	private double startDate;
	private double stopDate;
	private double minRange;
	private double maxRange;
	private TreeSet<Integer> cubeList;

	private double timeSeparationBetweenTracks = 10.0; // In seconds
	private int minTrackLength = 5;

	/**
	 * Standard Constructor
	 *
	 * @param aSmallBodyModel
	 */
	public LidarSearchDataCollection(PolyhedralModel aSmallBodyModel)
	{
		refSmallBodyModel = aSmallBodyModel;

		tracks = new ArrayList<>();
		selectedTrackS = ImmutableSet.of();

		translationM = new HashMap<>();
		errorM = new HashMap<>();
		radialOffset = 0.0;

		vPointPainter = new VtkPointPainter(this);
		vTrackPainter = new VtkTrackPainter(this);

		vActorL = new ArrayList<>();
		vActorL.add(vTrackPainter.getActor());
		vActorL.add(vPointPainter.getActor());

		setPointSize(2);
	}

	/**
	 * Returns the cumulative error for the specified Track.
	 * <P>
	 * The cumulative error is defined as the summation of all of the error for
	 * each LidarPoint associated with the Track.
	 *
	 * @param aTrack
	 * @return
	 */
	public double getTrackError(Track aTrack)
	{
		// Utilize the cached value
		Double tmpErr = errorM.get(aTrack);
		if (tmpErr != null)
			return tmpErr;

		// Calculate the error
		tmpErr = 0.0;
		int begIdx = aTrack.startId;
		int endIdx = aTrack.stopId;
		Vector3D tmpVect = getTranslation(aTrack);
		for (int c1 = begIdx; c1 <= endIdx; c1++)
		{
			LidarPoint tmpLP = getPoint(c1);

			double[] ptLidar = tmpLP.getTargetPosition().toArray();

			ptLidar = transformLidarPoint(tmpVect, ptLidar);
			double[] ptClosest = refSmallBodyModel.findClosestPoint(ptLidar);
			tmpErr += MathUtil.distance2Between(ptLidar, ptClosest);
		}

		// Update the cache and return the error
		errorM.put(aTrack, tmpErr);
		return tmpErr;
	}

	/**
	 * Method that returns the list of selected Tracks.
	 */
	public ImmutableList<Track> getSelectedTracks()
	{
		return selectedTrackS.asList();
	}

	/**
	 * Method that sets in the list of selected Tracks.
	 */
	public void setSelectedTracks(List<Track> aTrackL)
	{
		// Bail if the selection has not changed
		if (aTrackL.equals(selectedTrackS.asList()) == true)
			return;

		// Update our selection
		selectedTrackS = ImmutableSet.copyOf(aTrackL);
	}

	public double getOffsetScale()
	{
		BodyViewConfig tmpBodyViewConfig = (BodyViewConfig) refSmallBodyModel.getConfig();
		if (tmpBodyViewConfig.lidarOffsetScale <= 0.0)
			return refSmallBodyModel.getBoundingBoxDiagonalLength() / 1546.4224133453388;

		return tmpBodyViewConfig.lidarOffsetScale;
	}

	public Map<String, String> getLidarDataSourceMap()
	{
		BodyViewConfig tmpBodyViewConfig = (BodyViewConfig) refSmallBodyModel.getConfig();
		return tmpBodyViewConfig.lidarSearchDataSourceMap;
	}

	public void setLidarData(String dataSource, double startDate, double stopDate, TreeSet<Integer> cubeList,
			PointInRegionChecker pointInRegionChecker, double timeSeparationBetweenTracks, int minTrackLength)
			throws IOException, ParseException
	{
		runQuery(dataSource, startDate, stopDate, cubeList, pointInRegionChecker, timeSeparationBetweenTracks,
				minTrackLength);

		this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
	}

	public void setLidarData(String dataSource, double startDate, double stopDate, TreeSet<Integer> cubeList,
			PointInRegionChecker pointInRegionChecker, double timeSeparationBetweenTracks, int minTrackLength,
			double minRange, double maxRange) throws IOException, ParseException
	{
		runQuery(dataSource, startDate, stopDate, cubeList, pointInRegionChecker, timeSeparationBetweenTracks,
				minTrackLength, minRange, maxRange);

		this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
	}

	protected void runQuery(String dataSource, double startDate, double stopDate, TreeSet<Integer> cubeList,
			PointInRegionChecker pointInRegionChecker, double timeSeparationBetweenTracks, int minTrackLength)
			throws IOException, ParseException
	{

		if (dataSource.equals(this.dataSource) && startDate == this.startDate && stopDate == this.stopDate
				&& cubeList.equals(this.cubeList) && timeSeparationBetweenTracks == this.getTimeSeparationBetweenTracks()
				&& minTrackLength == this.minTrackLength)
		{
			return;
		}

		// Make clones since otherwise the previous if statement might
		// evaluate to true even if something changed.
		this.dataSource = new String(dataSource);
		this.startDate = startDate;
		this.stopDate = stopDate;
		this.cubeList = (TreeSet<Integer>) cubeList.clone();
		this.setTimeSeparationBetweenTracks(timeSeparationBetweenTracks);
		this.minTrackLength = minTrackLength;

		double start = startDate;
		double stop = stopDate;

		originalPoints.clear();
		originalPointsSourceFiles.clear(); // this is only used when generating
														// file ids upon loading from local
														// disk

		int timeindex = 0;
		int xindex = 1;
		int yindex = 2;
		int zindex = 3;
		int scxindex = 4;
		int scyindex = 5;
		int sczindex = 6;

		for (Integer cubeid : cubeList)
		{
			String filename = getLidarDataSourceMap().get(dataSource) + "/" + cubeid + ".lidarcube";
			File file = FileCache.getFileFromServer(filename);

			int fileId;
			if (!localFileMap.containsValue(file.toString()))
			{
				fileId = localFileMap.size();
				localFileMap.put(fileId, file.toString());
			}
			else
				fileId = localFileMap.inverse().get(file.toString());

			if (file == null)
				continue;

			InputStream fs = new FileInputStream(file.getAbsolutePath());
			InputStreamReader isr = new InputStreamReader(fs);
			BufferedReader in = new BufferedReader(isr);

			String lineRead;
			while ((lineRead = in.readLine()) != null)
			{
				String[] vals = lineRead.trim().split("\\s+");

				double time = TimeUtil.str2et(vals[timeindex]);
				if (time < start || time > stop)
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

				// if this part of the code has been reached and the point-checker
				// is null then this is a time-only search, and the time criterion
				// has already been met (cf. continue statement a few lines above)
				if (pointInRegionChecker == null)
				{
					LidarPoint p = new BasicLidarPoint(target, scpos, time, range, 0);
					originalPoints.add(p);
					originalPointsSourceFiles.put(p, fileId);
					continue;
				}

				// here, the point is known to be within the specified time bounds,
				// and since the point checker exists the target coordinates are
				// filtered against
				if (pointInRegionChecker.checkPointIsInRegion(target))
				{
					LidarPoint p = new BasicLidarPoint(target, scpos, time, range, 0);
					originalPoints.add(p);
					originalPointsSourceFiles.put(p, fileId);
					continue;
				}
			}
			in.close();
		}

		computeTracks();
		removeTracksThatAreTooSmall();
	}

	protected void runQuery(String dataSource, double startDate, double stopDate, TreeSet<Integer> cubeList,
			PointInRegionChecker pointInRegionChecker, double timeSeparationBetweenTracks, int minTrackLength,
			double minRange, double maxRange) throws IOException, ParseException
	{

		if (dataSource.equals(this.dataSource) && startDate == this.startDate && stopDate == this.stopDate
				&& cubeList.equals(this.cubeList) && timeSeparationBetweenTracks == this.getTimeSeparationBetweenTracks()
				&& minTrackLength == this.minTrackLength && minRange == this.minRange && this.maxRange == this.maxRange)
		{
			return;
		}

		// Make clones since otherwise the previous if statement might
		// evaluate to true even if something changed.
		this.dataSource = new String(dataSource);
		this.startDate = startDate;
		this.stopDate = stopDate;
		this.cubeList = (TreeSet<Integer>) cubeList.clone();
		this.setTimeSeparationBetweenTracks(timeSeparationBetweenTracks);
		this.minTrackLength = minTrackLength;
		this.minRange = minRange;
		this.maxRange = maxRange;

		double start = startDate;
		double stop = stopDate;

		originalPoints.clear();
		originalPointsSourceFiles.clear(); // this is only used when generating
														// file ids upon loading from local
														// disk

		int timeindex = 0;
		int xindex = 1;
		int yindex = 2;
		int zindex = 3;
		int scxindex = 4;
		int scyindex = 5;
		int sczindex = 6;

		for (Integer cubeid : cubeList)
		{
			String filename = getLidarDataSourceMap().get(dataSource) + "/" + cubeid + ".lidarcube";
			File file = FileCache.getFileFromServer(filename);

			int fileId;
			if (!localFileMap.containsValue(file.toString()))
			{
				fileId = localFileMap.size();
				localFileMap.put(fileId, file.toString());
			}
			else
				fileId = localFileMap.inverse().get(file.toString());

			if (file == null)
				continue;

			InputStream fs = new FileInputStream(file.getAbsolutePath());
			InputStreamReader isr = new InputStreamReader(fs);
			BufferedReader in = new BufferedReader(isr);

			String lineRead;
			while ((lineRead = in.readLine()) != null)
			{
				String[] vals = lineRead.trim().split("\\s+");

				double time = TimeUtil.str2et(vals[timeindex]);
				if (time < start || time > stop)
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

				// if this part of the code has been reached and the point-checker
				// is null then this is a time-only search, and the time criterion
				// has already been met (cf. continue statement a few lines above)
				if (pointInRegionChecker == null)
				{
					LidarPoint p = new BasicLidarPoint(target, scpos, time, range, 0);
					originalPoints.add(p);
					originalPointsSourceFiles.put(p, fileId);
					continue;
				}

				// here, the point is known to be within the specified time bounds,
				// and since the point checker exists the target coordinates are
				// filtered against
				if (pointInRegionChecker.checkPointIsInRegion(target))
				{
					LidarPoint p = new BasicLidarPoint(target, scpos, time, range, 0);
					originalPoints.add(p);
					originalPointsSourceFiles.put(p, fileId);
					continue;
				}
			}
			in.close();
		}

		computeTracks();
		removeTracksThatAreTooSmall();
	}

	public void loadTrackAscii(File file, LIDARTextInputType type) throws IOException
	{
		LidarTextFormatReader textReader = new LidarTextFormatReader(file, localFileMap.inverse().get(file.toString()),
				originalPoints, originalPointsSourceFiles, tracks);
		textReader.process(type);
		initModelState();
//
//        InputStream fs = new FileInputStream(file.getAbsolutePath());
//        InputStreamReader isr = new InputStreamReader(fs);
//        BufferedReader in = new BufferedReader(isr);
//
//        Track track = new Track();
//        track.startId = originalPoints.size();
//
//        int fileId=localFileMap.inverse().get(file.toString());
//
//        String lineRead;
//        while ((lineRead = in.readLine()) != null)
//        {
//            String[] vals = lineRead.trim().split("\\s+");
//
//            double time = 0;
//            double[] target = {0.0, 0.0, 0.0};
//            double[] scpos = {0.0, 0.0, 0.0};
//
//            // The lines in the file may contain either 3, or greater columns.
//            // If 3, they are assumed to contain the lidar point only and time and spacecraft
//            // position are set to zero. If 4 or 5, they are assumed to contain time and lidar point
//            // and spacecraft position is set to zero. If 6, they are assumed to contain
//            // lidar position and spacecraft position and time is set to zero. If 7 or greater,
//            // they are assumed to contain time, lidar position, and spacecraft position.
//            // In the case of 5 columns, the last column is ignored and in the case of
//            // greater than 7 columns, columns 8 or higher are ignored.
//            if (vals.length == 4 || vals.length == 5 || vals.length >= 7)
//            {
//                try
//                {
//                    // First try to see if it's a double ET. Otherwise assume it's UTC.
//                    time = Double.parseDouble(vals[0]);
//                }
//                catch (NumberFormatException e)
//                {
//                    time = TimeUtil.str2et(vals[0]);
//                    if (time == -Double.MIN_VALUE)
//                    {
//                        in.close();
//                        throw new IOException("Error: Incorrect file format!");
//                    }
//                }
//                target[0] = Double.parseDouble(vals[1]);
//                target[1] = Double.parseDouble(vals[2]);
//                target[2] = Double.parseDouble(vals[3]);
//            }
//            if (vals.length >= 7)
//            {
//                scpos[0] = Double.parseDouble(vals[4]);
//                scpos[1] = Double.parseDouble(vals[5]);
//                scpos[2] = Double.parseDouble(vals[6]);
//            }
//            if (vals.length == 3 || vals.length == 6)
//            {
//                target[0] = Double.parseDouble(vals[0]);
//                target[1] = Double.parseDouble(vals[1]);
//                target[2] = Double.parseDouble(vals[2]);
//            }
//            if (vals.length == 6)
//            {
//                scpos[0] = Double.parseDouble(vals[3]);
//                scpos[1] = Double.parseDouble(vals[4]);
//                scpos[2] = Double.parseDouble(vals[5]);
//            }
//
//            if (vals.length < 3)
//            {
//                in.close();
//                throw new IOException("Error: Incorrect file format!");
//            }
//
//            LidarPoint pt=new BasicLidarPoint(target, scpos, time, 0);
//            originalPoints.add(pt);
//            originalPointsSourceFiles.put(pt, fileId);
//        }
//
//        in.close();
//
//        track.stopId = originalPoints.size() - 1;
//        tracks.add(track);
	}

	public void loadTrackBinary(File file) throws IOException
	{
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

		Track track = new Track();
		track.startId = originalPoints.size();

		int fileId = localFileMap.inverse().get(file.toString());

		while (true)
		{
			double time = 0;
			double[] target = { 0.0, 0.0, 0.0 };
			double[] scpos = { 0.0, 0.0, 0.0 };

			try
			{
				time = FileUtil.readDoubleAndSwap(in);
			}
			catch (EOFException e)
			{
				break;
			}

			try
			{
				target[0] = FileUtil.readDoubleAndSwap(in);
				target[1] = FileUtil.readDoubleAndSwap(in);
				target[2] = FileUtil.readDoubleAndSwap(in);
				scpos[0] = FileUtil.readDoubleAndSwap(in);
				scpos[1] = FileUtil.readDoubleAndSwap(in);
				scpos[2] = FileUtil.readDoubleAndSwap(in);
			}
			catch (IOException e)
			{
				in.close();
				throw e;
			}

			double range = 0; // TODO
			LidarPoint pt = new BasicLidarPoint(target, scpos, time, range, 0);
			originalPoints.add(pt);
			originalPointsSourceFiles.put(pt, fileId);
		}
		in.close();

		track.stopId = originalPoints.size() - 1;
		tracks.add(track);
		initModelState();
		track.registerSourceFileIndex(fileId, localFileMap);
	}

	public void loadTrackOlaL2(File file) throws IOException
	{
		Track track = new Track();
		track.startId = originalPoints.size();

		OLAL2File l2File = new OLAL2File(file.toPath());
		List<LidarPoint> pts = Lists.newArrayList();
		pts.addAll(l2File.read(1. / 1000.));
		int fileId = localFileMap.inverse().get(file.toString());
		for (int i = 0; i < pts.size(); i++)
			originalPointsSourceFiles.put(pts.get(i), fileId);
		originalPoints.addAll(pts);

		track.stopId = originalPoints.size() - 1;
		tracks.add(track);
		initModelState();
		track.registerSourceFileIndex(fileId, localFileMap);
	}

	public void loadTrackPLY(File file) throws IOException
	{
		Track track = new Track();
		track.startId = originalPoints.size();

		IpwgPlyReader reader = new IpwgPlyReader();
		reader.SetFileName(file.getAbsolutePath());
		reader.Update();
		vtkUnstructuredGrid polyData = reader.getOutputAsUnstructuredGrid();

		int fileId = localFileMap.inverse().get(file.toString());

		double range = 0; // TODO
		LidarPoint lidarPt = null;
		for (int i = 0; i < polyData.GetNumberOfPoints(); i += 10)
		{
			double[] pt = polyData.GetPoint(i);

			lidarPt = new FSHyperPointWithFileTag(pt[0], pt[1], pt[2], 0, 0, 0, 0, range,
					polyData.GetPointData().GetArray(0).GetTuple1(i), fileId);
			originalPointsSourceFiles.put(lidarPt, fileId);
			originalPoints.add(lidarPt);
		}

		track.stopId = originalPoints.size() - 1;
		tracks.add(track);
		initModelState();
		track.registerSourceFileIndex(fileId, localFileMap);
	}

	BiMap<Integer, String> localFileMap = HashBiMap.create();
	// List<int[]> fileBounds=Lists.newArrayList(); // for adding filenum
	// information to tracks later; length 3 -> lowerBound,upperBound,fileNum

	/**
	 * Load a track from a file. This will replace all currently existing tracks
	 * with a single track.
	 *
	 * @param filename
	 * @throws IOException
	 */
	public void loadTracksFromFiles(File[] files, TrackFileType trackFileType) throws IOException
	{

		// originalPoints.clear();
		// tracks.clear();
		// fileBounds.clear();
		// localFileMap.clear();

		for (File file : files)
		{
			if (!localFileMap.containsValue(file.toString()))
				localFileMap.put(localFileMap.size(), file.toString());

			if (trackFileType == TrackFileType.BINARY)
			{
				loadTrackBinary(file);
				computeLoadedTracks();
			}
			else if (trackFileType == TrackFileType.PLY)
			{
				loadTrackPLY(file);
				computeLoadedTracks();
			}
			else if (trackFileType == TrackFileType.OLA_LEVEL_2)
			{
				loadTrackOlaL2(file);
				computeLoadedTracks();
			}
			else // variations on text input
			{
				loadTrackAscii(file, LIDARTextInputType.valueOf(trackFileType.name()));
				computeLoadedTracks();
			}
		}

		removeTracksThatAreTooSmall();
	}

	/**
	 * Returns the LidarPoint associated with the specified vtkActor and cellId.
	 * <P>
	 * Returns null if there is no LidarPoint corresponding the specified
	 * vtkActor / cellId.
	 *
	 * @param aActor The vtkActor associated with the pick action
	 * @param aCellId The cell id associated with the pick action
	 */
	public LidarPoint getLidarPointFromVtkPick(vtkActor aActor, int aCellId)
	{
		// Return the LidarPoint associated with the vPointPainter
		if (aActor == vPointPainter.getActor())
			return vPointPainter.getPoint();

		// Bail if aActor is not associated with a relevant VtkTrackPainter
		VtkTrackPainter tmpTrackPainter = getVtkTrackPainterForActor(aActor);
		if (tmpTrackPainter == null)
			return null;

		return tmpTrackPainter.getLidarPointFromCellId(aCellId);
	}

	/**
	 * Returns the LidarPoint at the specified index.
	 */
	public LidarPoint getPoint(int aIdx)
	{
		return originalPoints.get(aIdx);
	}

	/**
	 * Returns the list of Tracks
	 */
	public List<Track> getTracks()
	{
		return ImmutableList.copyOf(tracks);
	}

	/**
	 * Return the Track at the specified index.
	 *
	 * @param aIdx
	 * @return
	 */
	public Track getTrack(int aIdx)
	{
		return tracks.get(aIdx);
	}

	/**
	 * Returns the number of tracks in the model
	 */
	public int getNumberOfTracks()
	{
		return tracks.size();
	}

	public int getNumberOfVisibleTracks()
	{
		int numVisibleTracks = 0;
		for (Track aTrack : tracks)
			if (aTrack.getIsVisible() == true)
				++numVisibleTracks;

		return numVisibleTracks;
	}

	/**
	 * Method that will process the specified PickEvent.
	 * <P>
	 * The selected Tracks will be updated to reflect the PickEvent action.
	 *
	 * @param aPickEvent
	 */
	public void handlePickAction(PickEvent aPickEvent)
	{
		// Retrieve the selected lidar Point and corresponding Track
		Track tmpTrack = null;
		LidarPoint tmpPoint = null;
		vtkProp tmpActor = aPickEvent.getPickedProp();
		if (tmpActor == vPointPainter.getActor())
		{
			tmpPoint = vPointPainter.getPoint();
			tmpTrack = vPointPainter.getTrack();
		}
		else
		{
			// Bail if tmpActor is not associated with a relevant VtkTrackPainter
			VtkTrackPainter tmpTrackPainter = getVtkTrackPainterForActor(tmpActor);
			if (tmpTrackPainter == null)
				return;

			// Update the selected LidarPoint
			int tmpCellId = aPickEvent.getPickedCellId();
			tmpPoint = tmpTrackPainter.getLidarPointFromCellId(tmpCellId);

			// Determine the Track that was selected
			tmpTrack = tmpTrackPainter.getTrackFromCellId(tmpCellId);
			if (tmpTrack == null)
				return;

			// Update the VtkLidaPoint to reflect the selected point
			vPointPainter.setData(tmpPoint, tmpTrack);
		}

		// Determine if this is a modified action
		boolean isModifyKey = PickUtil.isModifyKey(aPickEvent.getMouseEvent());

		// Determine the Tracks that will be marked as selected
		List<Track> tmpL = getSelectedTracks();
		tmpL = new ArrayList<>(tmpL);

		if (isModifyKey == false)
			tmpL = ImmutableList.of(tmpTrack);
		else if (tmpL.contains(tmpTrack) == false)
			tmpL.add(tmpTrack);

		// Update the selected Tracks
		setSelectedTracks(tmpL);

		updateVtkVars();

		// Send out notification of the picked Track
		pcs.firePropertyChange(Properties.MODEL_PICKED, null, null);
	}

	/**
	 * Sets the selected LidarPoint
	 *
	 * @param aLidarPoint The LidarPoint of interest
	 * @param aLidarTrack The Track associated with the LidarPoint
	 */
	public void setSelectedPoint(LidarPoint aLidarPoint, Track aLidarTrack)
	{
		vPointPainter.setData(aLidarPoint, aLidarTrack);
		vPointPainter.updateVtkVars();

		pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
	}

	/**
	 * Helper method which will cause the LidarSearchDataCollection model to be
	 * properly initialized.
	 * <P>
	 * This method should be called after the track / point data has been
	 * updated. This method is responsible for initializing various internal
	 * state.
	 */
	protected void initModelState()
	{
		selectedTrackS = ImmutableSet.of();

		// Reset state vars
		translationM = new HashMap<>();
		for (Track aTrack : tracks)
			translationM.put(aTrack, Vector3D.ZERO);

		errorM = new HashMap<>();
		radialOffset = 0.0;

		// Reset VTK vars
		vPointPainter.setData(null, null);
		vTrackPainter.setTracks(tracks);

		// Setup the initial colors for all the Tracks
		assignInitialColorToTrack();

		updateVtkVars();
	}

	protected void computeLoadedTracks()
	{
		System.out.println("LidarSearchDataCollection: computeLoadedTracks: number of tracks " + tracks.size());
		for (Track track : tracks)
		{
			computeTrack(track);
		}
	}

	private void computeTrack(Track track)
	{
		int size = originalPoints.size();
		if (size == 0)
			return;
		track.registerSourceFileIndex(originalPointsSourceFiles.get(originalPoints.get(track.startId)), localFileMap);
		double t0 = originalPoints.get(track.startId).getTime();
		double t1 = originalPoints.get(track.stopId).getTime();
		track.timeRange = new String[] { TimeUtil.et2str(t0), TimeUtil.et2str(t1) };
	}

	protected void computeTracks()
	{
		tracks.clear();
		initModelState();

		int size = originalPoints.size();
		if (size == 0)
			return;

		// Sort points in time order
		Collections.sort(originalPoints);

		double prevTime = originalPoints.get(0).getTime();
		Track track = new Track();
		track.registerSourceFileIndex(originalPointsSourceFiles.get(originalPoints.get(0)), localFileMap);
		track.startId = 0;
		tracks.add(track);
		for (int i = 1; i < size; ++i)
		{
			double currentTime = originalPoints.get(i).getTime();
			if (currentTime - prevTime >= getTimeSeparationBetweenTracks())
			{
				track.stopId = i - 1;
				double t0 = originalPoints.get(track.startId).getTime();
				double t1 = originalPoints.get(track.stopId).getTime();
				track.timeRange = new String[] { TimeUtil.et2str(t0), TimeUtil.et2str(t1) };

				track = new Track();
				track.registerSourceFileIndex(originalPointsSourceFiles.get(originalPoints.get(i)), localFileMap);
				track.startId = i;

				tracks.add(track);
			}

			prevTime = currentTime;

		}

		track.stopId = size - 1;
		double t0 = originalPoints.get(track.startId).getTime();
		double t1 = originalPoints.get(track.stopId).getTime();
		track.timeRange = new String[] { TimeUtil.et2str(t0), TimeUtil.et2str(t1) };

		// Reset internal state vars
		initModelState();
	}

	/**
	 * If transformPoint is true, then the lidar points (not scpos) are
	 * translated using the current radial offset and translation before being
	 * saved out. If false, the original points are saved out unmodified.
	 *
	 * @param aTrack
	 * @param aOutfile
	 * @param transformPoint
	 * @throws IOException
	 */
	public void saveTrack(Track aTrack, File aOutfile, boolean transformPoint) throws IOException
	{
		FileWriter fstream = new FileWriter(aOutfile);
		BufferedWriter out = new BufferedWriter(fstream);

		int startId = aTrack.startId;
		int stopId = aTrack.stopId;

		String newline = System.getProperty("line.separator");

		Vector3D tmpVect = getTranslation(aTrack);
		for (int i = startId; i <= stopId; ++i)
		{
			LidarPoint pt = originalPoints.get(i);
			double[] target = pt.getTargetPosition().toArray();
			double[] scpos = pt.getSourcePosition().toArray();
			if (transformPoint)
			{
				target = transformLidarPoint(tmpVect, target);
				scpos = transformScpos(tmpVect, scpos, target);
			}

			String timeString = TimeUtil.et2str(pt.getTime());

			out.write(timeString + " " + target[0] + " " + target[1] + " " + target[2] + " " + scpos[0] + " " + scpos[1]
					+ " " + scpos[2] + " "
					+ MathUtil.distanceBetween(pt.getSourcePosition().toArray(), pt.getTargetPosition().toArray())
					+ newline);
		}

		out.close();
	}

	public void saveAllVisibleTracksToFolder(File folder, boolean transformPoint) throws IOException
	{
		int tmpIdx = -1;
		for (Track aTrack : tracks)
		{
			tmpIdx++;

			// Skip to next if Track is not visible
			if (aTrack.getIsVisible() == false)
				continue;

			File file = new File(folder.getAbsolutePath(), "track" + tmpIdx + ".txt");
			saveTrack(aTrack, file, transformPoint);
		}
	}

	public void saveAllVisibleTracksToSingleFile(File aFile, boolean transformPoint) throws IOException
	{
		FileWriter fstream = new FileWriter(aFile);
		BufferedWriter out = new BufferedWriter(fstream);

		String newline = System.getProperty("line.separator");

		for (Track aTrack : tracks)
		{
			// Skip to next if Track is not visible
			if (aTrack.getIsVisible() == false)
				continue;

			// Save each individual lidar point
			int startId = aTrack.startId;
			int stopId = aTrack.stopId;
			Vector3D tmpVect = getTranslation(aTrack);
			for (int i = startId; i <= stopId; ++i)
			{
				LidarPoint pt = originalPoints.get(i);
				double[] target = pt.getTargetPosition().toArray();
				double[] scpos = pt.getSourcePosition().toArray();
				if (transformPoint)
				{
					target = transformLidarPoint(tmpVect, target);
					scpos = transformScpos(tmpVect, scpos, target);
				}

				String timeString = TimeUtil.et2str(pt.getTime());

				out.write(timeString + " " + target[0] + " " + target[1] + " " + target[2] + " " + scpos[0] + " " + scpos[1]
						+ " " + scpos[2] + " "
						+ MathUtil.distanceBetween(pt.getSourcePosition().toArray(), pt.getTargetPosition().toArray())
						+ newline);
			}
		}
		out.close();
	}

	/**
	 * Helper method that will initialize all the Tracks with a default color.
	 */
	private void assignInitialColorToTrack()
	{
		int tmpIdx = 0;
		Color[] colorArr = ColorUtil.generateColors(tracks.size());
		for (Track aTrack : tracks)
		{
			aTrack.color = colorArr[tmpIdx];
			tmpIdx++;
		}
	}

	/**
	 * Sets the color associated with the specified Track.
	 */
	public void setTrackColor(Track aTrack, Color aColor)
	{
		// Delegate
		List<Track> tmpL = ImmutableList.of(aTrack);
		setTrackColor(tmpL, aColor);
	}

	/**
	 * Sets the color associated with the specified list of Tracks.
	 */
	public void setTrackColor(List<Track> aTrackL, Color aColor)
	{
		for (Track aTrack : aTrackL)
		{
			aTrack.color = aColor;
			getVtkTrackPainter(aTrack).markStale();
		}
		vPointPainter.markStale();

		updateVtkVars();
	}

	/**
	 * Method that will set the list of Tracks to visible and set all other
	 * Tracks to invisible.
	 *
	 * @param aTrackL
	 */
	public void hideOtherTracksExcept(List<Track> aTrackL)
	{
		Set<Track> tmpSet = new HashSet<>(aTrackL);

		// Update the visibility flag on each Track
		for (Track aTrack : tracks)
		{
			boolean isVisible = tmpSet.contains(aTrack);
			aTrack.isVisible = isVisible;

			getVtkTrackPainter(aTrack).markStale();
		}
		vPointPainter.markStale();

		updateVtkVars();
	}

	/**
	 * Sets the specified Track to be visible.
	 *
	 * @param aTrack
	 * @param aBool True if the Track should be visible
	 */
	public void setTrackVisible(Track aTrack, boolean aBool)
	{
		// Delegate
		List<Track> tmpL = ImmutableList.of(aTrack);
		setTrackVisible(tmpL, aBool);
	}

	/**
	 * Sets the specified lists of Tracks to be visible.
	 *
	 * @param aTrackL
	 * @param aBool True if the Tracks should be visible
	 */
	public void setTrackVisible(List<Track> aTrackL, boolean aBool)
	{
		for (Track aTrack : aTrackL)
		{
			aTrack.isVisible = aBool;
			getVtkTrackPainter(aTrack).markStale();
		}
		vPointPainter.markStale();

		updateVtkVars();
	}

	/**
	 * Helper method that takes the given lidar point and returns a corresponding
	 * point that takes into account the radial offset and the specified
	 * translation vector.
	 *
	 * @param aVect The translation vector of interest.
	 * @param aPt The lidar point of interest.
	 */
	protected double[] transformLidarPoint(Vector3D aVect, double[] aPt)
	{
		if (radialOffset != 0.0)
		{
			LatLon lla = MathUtil.reclat(aPt);
			lla = new LatLon(lla.lat, lla.lon, lla.rad + radialOffset);
			aPt = MathUtil.latrec(lla);
		}

		return new double[] { aPt[0] + aVect.getX(), aPt[1] + aVect.getY(), aPt[2] + aVect.getZ() };
	}

	/**
	 * Similar to previous function but specific to spacecraft position. The
	 * difference is that we calculate the radial offset we applied to the lidar
	 * and apply that offset to the spacecraft (rather than computing the radial
	 * offset directly for the spacecraft).
	 *
	 * @param aVect The translation vector of interest.
	 * @param scpos
	 * @param lidarPoint
	 * @return
	 */
	private double[] transformScpos(Vector3D aVect, double[] scpos, double[] lidarPoint)
	{
		if (radialOffset != 0.0)
		{
			LatLon lla = MathUtil.reclat(lidarPoint);
			lla = new LatLon(lla.lat, lla.lon, lla.rad + radialOffset);
			double[] offsetLidarPoint = MathUtil.latrec(lla);

			scpos[0] += (offsetLidarPoint[0] - lidarPoint[0]);
			scpos[1] += (offsetLidarPoint[1] - lidarPoint[1]);
			scpos[2] += (offsetLidarPoint[2] - lidarPoint[2]);
		}

		return new double[] { scpos[0] + aVect.getX(), scpos[1] + aVect.getY(), scpos[2] + aVect.getZ() };
	}

	/**
	 * Helper method that returns the VtkTrackPainter corresponding to the
	 * specified actor.
	 */
	private VtkTrackPainter getVtkTrackPainterForActor(vtkProp aActor)
	{
		if (aActor == vTrackPainter.getActor())
			return vTrackPainter;

		return null;
	}

	/**
	 * Helper method that returns the VtkTrackPainter corresponding to the
	 * specified Track.
	 */
	private VtkTrackPainter getVtkTrackPainter(Track aTrack)
	{
		return vTrackPainter;
	}

	/**
	 * Helper method that will update all relevant VTK vars.
	 * <P>
	 * A notification will be sent out to PropertyChange listeners of the
	 * {@link Properties#MODEL_CHANGED} event.
	 */
	private void updateVtkVars()
	{
		vPointPainter.updateVtkVars();
		vTrackPainter.updateVtkVars();

		// Notify our listeners
		pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
	}

	/**
	 * Helper method that will remove Tracks that are too small. A Track is
	 * defined as being too small if the number of points in the track is less
	 * than the minTrackLength.
	 * <P>
	 * A side effect is that the translationArr field will be reinitialized (all
	 * translation vectors set to Zero).
	 */
	protected void removeTracksThatAreTooSmall()
	{
		for (int i = tracks.size() - 1; i >= 0; --i)
		{
			if (tracks.get(i).getNumberOfPoints() < minTrackLength)
			{
				tracks.remove(i);
			}
		}

		initModelState();
	}

	public void removeAllLidarData()
	{
		tracks.clear();
		selectedTrackS = ImmutableSet.of();

		translationM.clear();
		errorM.clear();

		vPointPainter.setData(null, null);
		vTrackPainter.setTracks(tracks);

		originalPoints.clear();
//        fileBounds.clear();
		localFileMap.clear();

		this.dataSource = null;
		this.startDate = -Double.MIN_VALUE;
		this.stopDate = -Double.MIN_VALUE;
		this.cubeList = null;

		updateVtkVars();
	}

	@Override
	public String getClickStatusBarText(vtkProp aProp, int aCellId, double[] aPickPosition)
	{
		// Bail if there is no data
		if (tracks.isEmpty() == true || originalPoints.isEmpty() == true)
			return "";

		// Bail if there is no VtkTrackPainter associated with the actor (aProp)
		VtkTrackPainter tmpTrackPainter = getVtkTrackPainterForActor(aProp);
		if (tmpTrackPainter == null)
			return "";

		try
		{
			// TODO: This is badly designed hack to prevent the program from
			// crashing by just sticking bad index checks in a silent try-catch
			// block. This is indicative of defective logic.
			LidarPoint tmpLP = tmpTrackPainter.getLidarPointFromCellId(aCellId);

			double et = tmpLP.getTime();
			double range = tmpLP.getSourcePosition().subtract(tmpLP.getTargetPosition()).getNorm() * 1000; // m
			return String.format("Lidar point acquired at " + TimeUtil.et2str(et) + ", ET = %f, unmodified range = %f m",
					et, range);
		}
		catch (Exception aExp)
		{
		}

		return "";
	}

	@Override
	public List<vtkProp> getProps()
	{
		return vActorL;
	}

	/**
	 * Sets in the radial offset of all Tracks in this model.
	 */
	public void setOffset(double aOffset)
	{
		// Update the radialOffset
		if (radialOffset == aOffset)
			return;
		radialOffset = aOffset;

		// Invalidate the cache vars
		errorM = new HashMap<>();

		// Invalidate the VTK render vars
		vPointPainter.markStale();
		vTrackPainter.markStale();

		updateVtkVars();
	}

	/**
	 * Returns the translation associated with the specified Track.
	 *
	 * @param aTrack The Track of interest.
	 */
	public Vector3D getTranslation(Track aTrack)
	{
		return translationM.get(aTrack);
	}

	/**
	 * Set in the translation amount for each of the specified Tracks.
	 *
	 * @param aTrackL The list of Tracks of interest.
	 * @param aVect The vector that defines the translation amount.
	 */
	public void setTranslation(List<Track> aTrackL, Vector3D aVect)
	{
		for (Track aTrack : aTrackL)
		{
			errorM.remove(aTrack);

			translationM.put(aTrack, aVect);

			getVtkTrackPainter(aTrack).markStale();
		}
		vPointPainter.markStale();

		updateVtkVars();
	}

	/**
	 * Sets in the baseline point size for all of the tracks
	 *
	 * @param aSize
	 */
	public void setPointSize(int aSize)
	{
		vTrackPainter.setPointSize(aSize);
		vPointPainter.setPointSize(aSize * 3.5);

		pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
	}

	public int getNumberOfPoints()
	{
		return originalPoints.size();
	}

	/**
	 * It is useful to fit a line to the track. The following function computes
	 * the parameters of such a line, namely, a point on the line and a vector
	 * pointing in the direction of the line. Note that the returned
	 * fittedLinePoint is the point on the line closest to the first point of the
	 * track.
	 */
	private void fitLineToTrack(Track aTrack, double[] fittedLinePoint, double[] fittedLineDirection)
	{
		int startId = aTrack.startId;
		int stopId = aTrack.stopId;
		if (startId == stopId)
			return;

		Vector3D tmpVect = getTranslation(aTrack);
		try
		{
			double t0 = originalPoints.get(startId).getTime();

			double[] lineStartPoint = new double[3];
			for (int j = 0; j < 3; ++j)
			{
				PolynomialFitter fitter = new PolynomialFitter(new LevenbergMarquardtOptimizer());
				for (int i = startId; i <= stopId; ++i)
				{
					LidarPoint lp = originalPoints.get(i);
					double[] target = transformLidarPoint(tmpVect, lp.getTargetPosition().toArray());
					fitter.addObservedPoint(1.0, lp.getTime() - t0, target[j]);
				}

				PolynomialFunction fitted = new PolynomialFunction(fitter.fit(new double[2]));
				fittedLineDirection[j] = fitted.getCoefficients()[1];
				lineStartPoint[j] = fitted.value(0.0);
			}
			MathUtil.vhat(fittedLineDirection, fittedLineDirection);

			// Set the fittedLinePoint to the point on the line closest to first
			// track point
			// as this makes it easier to do distance computations along the line.
			double[] dist = new double[1];
			double[] target = transformLidarPoint(tmpVect, originalPoints.get(startId).getTargetPosition().toArray());
			MathUtil.nplnpt(lineStartPoint, fittedLineDirection, target, fittedLinePoint, dist);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private double distanceOfClosestPointOnLineToStartOfLine(double[] point, Track aTrack, double[] fittedLinePoint,
			double[] fittedLineDirection)
	{
		if (aTrack.startId == aTrack.stopId)
			return 0.0;

		double[] pnear = new double[3];
		double[] dist = new double[1];
		MathUtil.nplnpt(fittedLinePoint, fittedLineDirection, point, pnear, dist);

		return MathUtil.distanceBetween(pnear, fittedLinePoint);
	}

	/**
	 * Run gravity program on specified track and return potential, acceleration,
	 * and elevation as function of distance and time.
	 *
	 * @param aTrack
	 * @throws Exception
	 */
	public void getGravityDataForTrack(Track aTrack, List<Double> potential, List<Double> acceleration,
			List<Double> elevation, List<Double> distance, List<Double> time) throws Exception
	{
		if (originalPoints.size() == 0 || aTrack.startId < 0 || aTrack.stopId < 0)
			throw new IOException();

		// Run the gravity program
		Vector3D tmpVect = getTranslation(aTrack);
		int startId = aTrack.startId;
		int stopId = aTrack.stopId;
		List<double[]> xyzPointList = new ArrayList<double[]>();
		for (int i = startId; i <= stopId; ++i)
		{
			LidarPoint pt = originalPoints.get(i);
			double[] target = pt.getTargetPosition().toArray();
			target = transformLidarPoint(tmpVect, target);
			xyzPointList.add(target);
		}
		List<Point3D> accelerationVector = new ArrayList<Point3D>();
		Gravity.getGravityAtPoints(xyzPointList, refSmallBodyModel.getDensity(), refSmallBodyModel.getRotationRate(),
				refSmallBodyModel.getReferencePotential(), refSmallBodyModel.getSmallBodyPolyData(), elevation,
				acceleration, accelerationVector, potential);

		double[] fittedLinePoint = new double[3];
		double[] fittedLineDirection = new double[3];
		fitLineToTrack(aTrack, fittedLinePoint, fittedLineDirection);
		for (int i = aTrack.startId; i <= aTrack.stopId; ++i)
		{
			double[] point = originalPoints.get(i).getTargetPosition().toArray();
			point = transformLidarPoint(tmpVect, point);
			double dist = distanceOfClosestPointOnLineToStartOfLine(point, aTrack, fittedLinePoint, fittedLineDirection);
			distance.add(dist);
			time.add(originalPoints.get(i).getTime());
		}
	}

	private double[] getCentroidOfTrack(Track aTrack)
	{
		Vector3D tmpVect = getTranslation(aTrack);
		int startId = aTrack.startId;
		int stopId = aTrack.stopId;

		double[] centroid = { 0.0, 0.0, 0.0 };
		for (int i = startId; i <= stopId; ++i)
		{
			LidarPoint lp = originalPoints.get(i);
			double[] target = transformLidarPoint(tmpVect, lp.getTargetPosition().toArray());
			centroid[0] += target[0];
			centroid[1] += target[1];
			centroid[2] += target[2];
		}

		int trackSize = aTrack.getNumberOfPoints();
		if (trackSize > 0)
		{
			centroid[0] /= trackSize;
			centroid[1] /= trackSize;
			centroid[2] /= trackSize;
		}

		return centroid;
	}

	/**
	 * It is useful to fit a plane to the track. The following function computes
	 * the parameters of such a plane, namely, a point on the plane and a vector
	 * pointing in the normal direction of the plane. Note that the returned
	 * pointOnPlane is the point on the plane closest to the centroid of the
	 * track.
	 */
	private void fitPlaneToTrack(Track aTrack, double[] pointOnPlane, RealMatrix planeOrientation)
	{
		int startId = aTrack.startId;
		int stopId = aTrack.stopId;
		if (startId == stopId)
			return;

		Vector3D tmpVect = getTranslation(aTrack);
		try
		{
			double[] centroid = getCentroidOfTrack(aTrack);

			// subtract out the centroid from the track
			int trackSize = aTrack.getNumberOfPoints();
			double[][] points = new double[3][trackSize];
			for (int i = startId, j = 0; i <= stopId; ++i, ++j)
			{
				LidarPoint lp = originalPoints.get(i);
				double[] target = transformLidarPoint(tmpVect, lp.getTargetPosition().toArray());
				points[0][j] = target[0] - centroid[0];
				points[1][j] = target[1] - centroid[1];
				points[2][j] = target[2] - centroid[2];
			}
			RealMatrix pointMatrix = new Array2DRowRealMatrix(points, false);

			// Now do SVD on this matrix
			SingularValueDecomposition svd = new SingularValueDecomposition(pointMatrix);
			RealMatrix u = svd.getU();

			planeOrientation.setSubMatrix(u.copy().getData(), 0, 0);

			pointOnPlane[0] = centroid[0];
			pointOnPlane[1] = centroid[1];
			pointOnPlane[2] = centroid[2];
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * This function takes a lidar track, fits a plane through it and reorients
	 * the track into a new coordinate system such that the fitted plane is the
	 * XY plane in the new coordinate system, and saves the reoriented track and
	 * the transformation to a file.
	 *
	 * @param aTrack The Track of interest.
	 * @param outfile - save reoriented track to this file
	 * @param rotationMatrixFile - save transformation matrix to this file
	 * @throws IOException
	 */
	public void reprojectedTrackOntoFittedPlane(Track aTrack, File outfile, File rotationMatrixFile) throws IOException
	{
		int startId = aTrack.startId;
		int stopId = aTrack.stopId;
		if (startId == stopId)
			return;

		double[] pointOnPlane = new double[3];
		RealMatrix planeOrientation = new Array2DRowRealMatrix(3, 3);

		fitPlaneToTrack(aTrack, pointOnPlane, planeOrientation);
		planeOrientation = new LUDecomposition(planeOrientation).getSolver().getInverse();

		FileWriter fstream = new FileWriter(outfile);
		BufferedWriter out = new BufferedWriter(fstream);

		String newline = System.getProperty("line.separator");

		Vector3D tmpVect = getTranslation(aTrack);
		for (int i = startId; i <= stopId; ++i)
		{
			LidarPoint lp = originalPoints.get(i);
			double[] target = transformLidarPoint(tmpVect, lp.getTargetPosition().toArray());

			target[0] = target[0] - pointOnPlane[0];
			target[1] = target[1] - pointOnPlane[1];
			target[2] = target[2] - pointOnPlane[2];

			target = planeOrientation.operate(target);

			out.write(TimeUtil.et2str(lp.getTime()) + " " + target[0] + " " + target[1] + " " + target[2] + newline);
		}

		out.close();

		// Also save out the orientation matrix.
		fstream = new FileWriter(rotationMatrixFile);
		out = new BufferedWriter(fstream);

		double[][] data = planeOrientation.getData();
		out.write(data[0][0] + " " + data[0][1] + " " + data[0][2] + " " + pointOnPlane[0] + "\n");
		out.write(data[1][0] + " " + data[1][1] + " " + data[1][2] + " " + pointOnPlane[1] + "\n");
		out.write(data[2][0] + " " + data[2][1] + " " + data[2][2] + " " + pointOnPlane[2] + "\n");

		out.close();
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
	 * @param aTrack
	 * @param outfile
	 * @param transformPoint
	 * @throws IOException
	 */
	public void saveTrackBinary(Track aTrack, File outfile, boolean transformPoint) throws IOException
	{
		outfile = new File(outfile.getAbsolutePath() + ".bin");
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outfile)));

		Vector3D tmpVect = getTranslation(aTrack);
		int startId = aTrack.startId;
		int stopId = aTrack.stopId;
		for (int i = startId; i <= stopId; ++i)
		{
			LidarPoint pt = originalPoints.get(i);
			double[] target = pt.getTargetPosition().toArray();
			double[] scpos = pt.getSourcePosition().toArray();
			if (transformPoint)
			{
				target = transformLidarPoint(tmpVect, target);
				scpos = transformScpos(tmpVect, scpos, target);
			}

			FileUtil.writeDoubleAndSwap(out, pt.getTime());
			FileUtil.writeDoubleAndSwap(out, target[0]);
			FileUtil.writeDoubleAndSwap(out, target[1]);
			FileUtil.writeDoubleAndSwap(out, target[2]);
			FileUtil.writeDoubleAndSwap(out, scpos[0]);
			FileUtil.writeDoubleAndSwap(out, scpos[1]);
			FileUtil.writeDoubleAndSwap(out, scpos[2]);
		}

		out.close();
	}

	public double getTimeSeparationBetweenTracks()
	{
		return timeSeparationBetweenTracks;
	}

	public void setTimeSeparationBetweenTracks(double timeSeparationBetweenTracks)
	{
		this.timeSeparationBetweenTracks = timeSeparationBetweenTracks;
	}

	public int getMinTrackLength()
	{
		return minTrackLength;
	}

	public void setMinTrackLength(int value)
	{
		this.minTrackLength = value;
	}

}
