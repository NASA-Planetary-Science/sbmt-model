package edu.jhuapl.sbmt.model.lidar;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import com.google.common.base.Stopwatch;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkDoubleArray;
import vtk.vtkGeometryFilter;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkUnsignedCharArray;
import vtk.vtkVertex;

import edu.jhuapl.saavtk.util.ColorUtil;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.SaavtkLODActor;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.lidar.hyperoctree.hayabusa2.Hayabusa2RawLidarFile;
import edu.jhuapl.sbmt.model.image.Instrument;
import edu.jhuapl.sbmt.util.TimeUtil;

public class LidarDataPerUnit implements PropertyChangeListener
{
	// Attributes
	private final BodyViewConfig refBodyViewConfig;
	private final LidarFileSpec refFileSpec;

	// State vars
	private boolean isLoaded;
	private boolean isVible;
	private Color colorGround;
	private Color colorSpace;
	private int numPts;

	private double startPercentage = 0.0;
	private double stopPercentage = 1.0;
	private double minIntensity;
	private double maxIntensity;

	// VTK vars
	private vtkPolyData polydata;
	private vtkPolyData polydataSc;
	private vtkPoints originalPoints;
	private vtkPoints originalPointsSc;
	private List<vtkProp> actors = new ArrayList<>();
	private vtkGeometryFilter geometryFilter;
	private vtkGeometryFilter geometryFilterSc;
	private vtkDoubleArray times;
	private vtkDoubleArray ranges;
	private vtkActor actorSpacecraft;
	private ProgressMonitor progressMonitor;
	private BinaryDataTask binaryTask;
	private vtkPoints points;
	private vtkCellArray vert;
	private vtkIdList idList;
	private vtkPoints pointsSc;
	private vtkCellArray vertSc;
	private List<Double> intensityList;
	private vtkUnsignedCharArray colors;
	private LidarLoadingListener listener;

	/**
	 * Standard Constructor
	 */
	public LidarDataPerUnit(BodyViewConfig aBodyViewConfig, LidarFileSpec aFileSpec, Color aColorGround,
			Color aColorSpace, LidarLoadingListener aListener) throws IOException
	{
		refBodyViewConfig = aBodyViewConfig;
		refFileSpec = aFileSpec;

		isLoaded = false;
		colorGround = aColorGround;
		colorSpace = aColorSpace;
		listener = aListener;

		init();
	}

	/**
	 * Returns the color of this object
	 */
	public Color getColor()
	{
		return colorGround;
	}

	/**
	 * Return whether this object should be rendered.
	 */
	public boolean getIsVisible()
	{
		return isVible;
	}

	/**
	 * Returns the number of points in this object
	 */
	public int getNumberOfPoints()
	{
		return numPts;
	}

	/**
	 * Returns true if this object has been fully loaded
	 */
	public boolean isLoaded()
	{
		return isLoaded;
	}

	/**
	 * Sets the color of this object.
	 */
	public void setColor(Color aColor)
	{
		colorGround = aColor;
	}

	/**
	 * Return whether this object should be rendered.
	 */
	public void setIsVisible(boolean aBool)
	{
		isVible = aBool;
	}

	void init() throws IOException
	{
		File file = FileCache.getFileFromServer(SafeURLPaths.instance().getString(refFileSpec.getPath()));
		if (file == null)
			throw new IOException(refFileSpec.getPath() + " could not be loaded");

		polydata = new vtkPolyData();
		points = new vtkPoints();
		vert = new vtkCellArray();
		polydata.SetPoints(points);
		polydata.SetVerts(vert);
		colors = new vtkUnsignedCharArray();
		colors.SetNumberOfComponents(4);
		polydata.GetCellData().SetScalars(colors);

		polydataSc = new vtkPolyData();
		pointsSc = new vtkPoints();
		vertSc = new vtkCellArray();
		polydataSc.SetPoints(pointsSc);
		polydataSc.SetVerts(vertSc);
		times = new vtkDoubleArray();
		ranges = new vtkDoubleArray();

		// Variables to keep track of intensities
		minIntensity = Double.POSITIVE_INFINITY;
		maxIntensity = Double.NEGATIVE_INFINITY;
		intensityList = new ArrayList<>();

		// Load hayabusa data and bail
		if (isHayabusaData() == true)
		{
			loadHayabusaDataFile(file);
			return;
		}

		idList = new vtkIdList();
		idList.SetNumberOfIds(1);

		progressMonitor = new ProgressMonitor(null, "Loading OLA Data", "", 0, 100);

		// Parse data
		boolean isBinary = refBodyViewConfig.lidarBrowseIsBinary;
		if (isBinary)
		{
			binaryTask = new BinaryDataTask(file);
			binaryTask.addPropertyChangeListener(this);
			binaryTask.execute();
		}
		else
		{
			loadAsciiLidarData(refBodyViewConfig, file);
		}
	}

	private void renderPoints(int aCount)
	{
		// Color each point based on base color scaled by intensity
		Color plotColor;
		float[] baseHSL = ColorUtil.getHSLColorComponents(colorGround);
		for (double intensity : intensityList)
		{
			plotColor = ColorUtil.scaleLightness(baseHSL, intensity, minIntensity, maxIntensity, 0.5f);
			colors.InsertNextTuple4(plotColor.getRed(), plotColor.getGreen(), plotColor.getBlue(), plotColor.getAlpha());
		}

		polydata.GetCellData().GetScalars().Modified();
		polydata.Modified();
		originalPoints = new vtkPoints();
		originalPoints.DeepCopy(points);
		originalPointsSc = new vtkPoints();
		originalPointsSc.DeepCopy(pointsSc);

		geometryFilter = new vtkGeometryFilter();
		geometryFilter.SetInputData(polydata);
		geometryFilter.PointClippingOn();
		geometryFilter.CellClippingOff();
		geometryFilter.ExtentClippingOff();
		geometryFilter.MergingOff();
		geometryFilter.SetPointMinimum(0);
		geometryFilter.SetPointMaximum(aCount);

		geometryFilterSc = new vtkGeometryFilter();
		geometryFilterSc.SetInputData(polydataSc);
		geometryFilterSc.PointClippingOn();
		geometryFilterSc.CellClippingOff();
		geometryFilterSc.ExtentClippingOff();
		geometryFilterSc.MergingOff();
		geometryFilterSc.SetPointMinimum(0);
		geometryFilterSc.SetPointMaximum(aCount);
		vtkPolyDataMapper pointsMapper = new vtkPolyDataMapper();
		pointsMapper.SetScalarModeToUseCellData();
		pointsMapper.SetInputConnection(geometryFilter.GetOutputPort());

		vtkActor actor = new SaavtkLODActor();
		actor.SetMapper(pointsMapper);
		vtkPolyDataMapper lodMapper = ((SaavtkLODActor) actor)
				.setQuadricDecimatedLODMapper(geometryFilter.GetOutputPort());

		actor.GetProperty().SetPointSize(2.0);

		vtkPolyDataMapper pointsMapperSc = new vtkPolyDataMapper();
		pointsMapperSc.SetInputConnection(geometryFilterSc.GetOutputPort());

		actorSpacecraft = new SaavtkLODActor();
		actorSpacecraft.SetMapper(pointsMapperSc);
		((SaavtkLODActor) actorSpacecraft).setQuadricDecimatedLODMapper(geometryFilterSc.GetOutputPort());
		double r = colorSpace.getRed() / 255.0;
		double g = colorSpace.getGreen() / 255.0;
		double b = colorSpace.getBlue() / 255.0;
		actorSpacecraft.GetProperty().SetColor(r, g, b);
		actorSpacecraft.GetProperty().SetPointSize(2.0);

		actors.add(actor);
		actors.add(actorSpacecraft);
	}

	public void setPercentageShown(double startPercent, double stopPercent)
	{
		startPercentage = startPercent;
		stopPercentage = stopPercent;
	}

	public void showPercentageShown()
	{
		double numberOfPoints = originalPoints.GetNumberOfPoints();
		int firstPointId = (int) (numberOfPoints * startPercentage);
		int lastPointId = (int) (numberOfPoints * stopPercentage) - 1;
		if (lastPointId < firstPointId)
		{
			lastPointId = firstPointId;
		}

		geometryFilter.SetPointMinimum(firstPointId);
		geometryFilter.SetPointMaximum(lastPointId);
		geometryFilter.Update();

		geometryFilterSc.SetPointMinimum(firstPointId);
		geometryFilterSc.SetPointMaximum(lastPointId);
		geometryFilterSc.Update();
	}

	public void setOffset(double offset)
	{
		// Retrieve the offsetMultipier
		double offsetMultiplier = 1.0;
		if (isHayabusaData() == true)
			offsetMultiplier = 10.0;

		vtkPoints points = polydata.GetPoints();

		int numberOfPoints = points.GetNumberOfPoints();

		for (int i = 0; i < numberOfPoints; ++i)
		{
			double[] pt = originalPoints.GetPoint(i);
			LatLon lla = MathUtil.reclat(pt);
			lla = new LatLon(lla.lat, lla.lon, lla.rad + offset * offsetMultiplier);
			pt = MathUtil.latrec(lla);
			points.SetPoint(i, pt);
		}

		polydata.Modified();
	}

	public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
	{
		cellId = geometryFilter.GetPointMinimum() + cellId;
		String tmpPath = refFileSpec.getPath();
		if (tmpPath.toLowerCase().endsWith(".gz"))
			tmpPath = tmpPath.substring(0, tmpPath.length() - 3);
		File file = new File(tmpPath);

		String timeStr = TimeUtil.et2str(times.GetValue(cellId));

		return String.format(
				"Lidar point " + file.getName() + " acquired at " + timeStr + ", ET = %f, unmodified range = %f m",
				times.GetValue(cellId), ranges.GetValue(cellId) * 1000);
	}

	public List<vtkProp> getProps()
	{
		return actors;
	}

	public void setShowSpacecraftPosition(boolean show)
	{
		if (actorSpacecraft != null)
			actorSpacecraft.SetVisibility(show ? 1 : 0);
	}

	private void loadAsciiLidarData(BodyViewConfig aBodyViewConfig, File aFile) throws IOException
	{
		int[] xyzIndices = aBodyViewConfig.lidarBrowseXYZIndices;
		int[] scXyzIndices = aBodyViewConfig.lidarBrowseSpacecraftIndices;
		int xIndex = xyzIndices[0];
		int yIndex = xyzIndices[1];
		int zIndex = xyzIndices[2];
		int scxIndex = scXyzIndices[0];
		int scyIndex = scXyzIndices[1];
		int sczIndex = scXyzIndices[2];
		int timeIndex = aBodyViewConfig.lidarBrowseTimeIndex;
		int receivedIntensityIndex = aBodyViewConfig.lidarBrowseReceivedIntensityIndex;

		boolean isInMeters = aBodyViewConfig.lidarBrowseIsInMeters;
		boolean intensityEnabled = aBodyViewConfig.lidarBrowseIntensityEnabled;

		boolean isLidarInSphericalCoordinates = aBodyViewConfig.lidarBrowseIsLidarInSphericalCoordinates;
		boolean isSpacecraftInSphericalCoordinates = aBodyViewConfig.lidarBrowseIsSpacecraftInSphericalCoordinates;
		boolean isTimeInET = aBodyViewConfig.lidarBrowseIsTimeInET;
		int numberHeaderLines = aBodyViewConfig.lidarBrowseNumberHeaderLines;
		int rangeIndex = aBodyViewConfig.lidarBrowseRangeIndex;
		boolean isRangeExplicitInData = aBodyViewConfig.lidarBrowseIsRangeExplicitInData;
		int noiseIndex = aBodyViewConfig.lidarBrowseNoiseIndex;

		FileInputStream fs = new FileInputStream(aFile.getAbsolutePath());
		BufferedReader in = new BufferedReader(new InputStreamReader(fs));

		for (int i = 0; i < numberHeaderLines; ++i)
			in.readLine();

		String line;

		numPts = 0;
		while ((line = in.readLine()) != null)
		{
			String[] vals = line.trim().split("\\s+");

			// Don't include noise
			if (noiseIndex >= 0 && vals[noiseIndex].equals("1"))
				continue;

			// Parse lidar measured position
			double x = 0, y = 0, z = 0;
			if (xIndex >= 0 && yIndex >= 0 && zIndex >= 0)
			{
				x = Double.parseDouble(vals[xIndex]);
				y = Double.parseDouble(vals[yIndex]);
				z = Double.parseDouble(vals[zIndex]);
			}
			if (isLidarInSphericalCoordinates)
			{
				// Convert from spherical to xyz
				double[] xyz = MathUtil.latrec(new LatLon(y * Math.PI / 180.0, x * Math.PI / 180.0, z));
				x = xyz[0];
				y = xyz[1];
				z = xyz[2];
			}

			// Parse spacecraft position
			double scx = 0, scy = 0, scz = 0;
			if (scxIndex >= 0 && scyIndex >= 0 && sczIndex >= 0)
			{
				scx = Double.parseDouble(vals[scxIndex]);
				scy = Double.parseDouble(vals[scyIndex]);
				scz = Double.parseDouble(vals[sczIndex]);
			}
			if (isSpacecraftInSphericalCoordinates)
			{
				double[] xyz = MathUtil.latrec(new LatLon(scy * Math.PI / 180.0, scx * Math.PI / 180.0, scz));
				scx = xyz[0];
				scy = xyz[1];
				scz = xyz[2];
			}

			// Convert distance units from m -> km
			if (isInMeters)
			{
				x /= 1000.0;
				y /= 1000.0;
				z /= 1000.0;
				scx /= 1000.0;
				scy /= 1000.0;
				scz /= 1000.0;
			}

			points.InsertNextPoint(x, y, z);
			idList.SetId(0, numPts);
			vert.InsertNextCell(idList);
			pointsSc.InsertNextPoint(scx, scy, scz);
			vertSc.InsertNextCell(idList);

			// Save range data
			double range;
			if (isRangeExplicitInData)
			{
				// Range is explicitly listed in data, get it
				range = Double.parseDouble(vals[rangeIndex]);
				if (isInMeters)
				{
					range /= 1000.0;
				}
			}
			else
			{
				// Range is not explicitly listed, derive it from lidar measurement
				// and sc positions
				range = Math.sqrt((x - scx) * (x - scx) + (y - scy) * (y - scy) + (z - scz) * (z - scz));
			}
			ranges.InsertNextValue(range);

			// Extract the received intensity
			double irec = 0.0;
			if (intensityEnabled)
			{
				irec = Double.parseDouble(vals[receivedIntensityIndex]);
			}

			// Add to list and keep track of min/max encountered so far
			minIntensity = (irec < minIntensity) ? irec : minIntensity;
			maxIntensity = (irec > maxIntensity) ? irec : maxIntensity;
			intensityList.add(irec);

			// We store the times in a vtk array. By storing in a vtk array, we
			// don't have to worry about java out of memory errors since java
			// doesn't know about c++ memory.
			double t = 0;
			if (isTimeInET)
			{
				// Read ET directly
				t = Double.parseDouble(vals[timeIndex]);
			}
			else
			{
				// Convert from UTC string to ET
				t = TimeUtil.str2et(vals[timeIndex]);
			}
			times.InsertNextValue(t);

			++numPts;
		}

		in.close();
		renderPoints(numPts);
		if (listener != null)
			listener.lidarLoadComplete(LidarDataPerUnit.this);
		isLoaded = true;

	}

	private void loadBinaryLidarData(BinaryDataTask aBinaryDataTask, BodyViewConfig aBodyViewConfig, File aFile)
			throws IOException
	{
		int[] xyzIndices = aBodyViewConfig.lidarBrowseXYZIndices;
		int[] scXyzIndices = aBodyViewConfig.lidarBrowseSpacecraftIndices;
		int xIndex = xyzIndices[0];
		int yIndex = xyzIndices[1];
		int zIndex = xyzIndices[2];
		int scxIndex = scXyzIndices[0];
		int scyIndex = scXyzIndices[1];
		int sczIndex = scXyzIndices[2];

		int binaryRecordSize = aBodyViewConfig.lidarBrowseBinaryRecordSize;
		boolean intensityEnabled = aBodyViewConfig.lidarBrowseIntensityEnabled;
		boolean isInMeters = aBodyViewConfig.lidarBrowseIsInMeters;
		int timeIndex = aBodyViewConfig.lidarBrowseTimeIndex;
		int receivedIntensityIndex = aBodyViewConfig.lidarBrowseReceivedIntensityIndex;

		FileInputStream fs = new FileInputStream(aFile.getAbsolutePath());
		FileChannel channel = fs.getChannel();
		ByteBuffer bb = ByteBuffer.allocateDirect((int) aFile.length());
		bb.clear();
		bb.order(ByteOrder.LITTLE_ENDIAN);
		if (channel.read(bb) != aFile.length())
		{
			fs.close();
			throw new IOException("Error reading " + refFileSpec);
		}

		byte[] utcArray = new byte[24];

		int numRecords = (int) (aFile.length() / binaryRecordSize);
		Stopwatch sw = new Stopwatch();
		sw.start();
		for (int aCount = 0; aCount < numRecords; aCount++)
		{
			aBinaryDataTask.setProgressVal(aCount * 100 / numRecords);
			int xoffset = aCount * binaryRecordSize + xIndex;
			int yoffset = aCount * binaryRecordSize + yIndex;
			int zoffset = aCount * binaryRecordSize + zIndex;
			int scxoffset = aCount * binaryRecordSize + scxIndex;
			int scyoffset = aCount * binaryRecordSize + scyIndex;
			int sczoffset = aCount * binaryRecordSize + sczIndex;

			// Add lidar (x,y,z) and spacecraft (scx,scy,scz) data
			double x = bb.getDouble(xoffset);
			double y = bb.getDouble(yoffset);
			double z = bb.getDouble(zoffset);
			double scx = bb.getDouble(scxoffset);
			double scy = bb.getDouble(scyoffset);
			double scz = bb.getDouble(sczoffset);

			if (isInMeters)
			{
				x /= 1000.0;
				y /= 1000.0;
				z /= 1000.0;
				scx /= 1000.0;
				scy /= 1000.0;
				scz /= 1000.0;
			}
			points.InsertNextPoint(x, y, z);
			idList.SetId(0, aCount);
			vert.InsertNextCell(idList);

			// Save range data
			ranges.InsertNextValue(Math.sqrt((x - scx) * (x - scx) + (y - scy) * (y - scy) + (z - scz) * (z - scz)));

			// Extract the received intensity
			double irec = 0.0;
			if (intensityEnabled)
			{
				// Extract received intensity and keep track of min/max encountered
				// so far
				int recIntensityOffset = aCount * binaryRecordSize + receivedIntensityIndex;
				irec = bb.getDouble(recIntensityOffset);
			}

			// Add to list and keep track of min/max encountered so far
			minIntensity = (irec < minIntensity) ? irec : minIntensity;
			maxIntensity = (irec > maxIntensity) ? irec : maxIntensity;
			intensityList.add(irec);

			// assume no spacecraft position for now
			pointsSc.InsertNextPoint(scx, scy, scz);
			vertSc.InsertNextCell(idList);

			int timeoffset = aCount * binaryRecordSize + timeIndex;

			bb.position(timeoffset);
			bb.get(utcArray);
			String utc = new String(utcArray);

			// We store the times in a vtk array. By storing in a vtk array, we
			// don't have to worry about java out of memory errors since java
			// doesn't know about c++ memory.
			double t = TimeUtil.str2et(utc);
			times.InsertNextValue(t);
		}
		fs.close();

		numPts = numRecords;

		aBinaryDataTask.setProgressVal(100);
		renderPoints(numPts);
	}

	private void loadHayabusaDataFile(File aFile)
	{
		Hayabusa2RawLidarFile lidarFile = new Hayabusa2RawLidarFile(aFile.getAbsolutePath());
		Iterator<LidarPoint> it = lidarFile.iterator();
		numPts = 0;
		while (it.hasNext())
		{
			LidarPoint pt = it.next();
			int id = points.InsertNextPoint(pt.getTargetPosition().toArray());
			vtkVertex v = new vtkVertex();
			v.GetPointIds().SetId(0, id);
			;
			vert.InsertNextCell(v);
			ranges.InsertNextValue(pt.getSourcePosition().subtract(pt.getTargetPosition()).getNorm());
			// Add to list and keep track of min/max encountered so far
			double irec = pt.getIntensityReceived();
			minIntensity = (irec < minIntensity) ? irec : minIntensity;
			maxIntensity = (irec > maxIntensity) ? irec : maxIntensity;
			intensityList.add(irec);
			int id2 = pointsSc.InsertNextPoint(pt.getSourcePosition().toArray());
			vtkVertex v2 = new vtkVertex();
			v2.GetPointIds().SetId(0, id2);
			vertSc.InsertNextCell(v2);
			times.InsertNextValue(pt.getTime());
			numPts++;
		}

		renderPoints(numPts);
		if (listener != null)
			listener.lidarLoadComplete(this);
		isLoaded = true;
	}

	/**
	 * Helper method that returns true if this is hayabusa specific data.
	 * <P>
	 * TODO: Due to the deficient design several aspects of this code is specific
	 * to an instrument. This should be rectified.
	 */
	@Deprecated
	private boolean isHayabusaData()
	{
		return refBodyViewConfig.lidarInstrumentName == Instrument.LASER;
	}

	class BinaryDataTask extends SwingWorker<Void, Void>
	{
		private final File file;

		public BinaryDataTask(File aFile)
		{
			file = aFile;
		}

		public void setProgressVal(int aVal)
		{
			setProgress(aVal);
		}

		@Override
		protected Void doInBackground() throws Exception
		{
			loadBinaryLidarData(this, refBodyViewConfig, file);
			return null;
		}

		@Override
		protected void done()
		{
			if (listener != null)
				listener.lidarLoadComplete(LidarDataPerUnit.this);
			isLoaded = true;
		}

	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		if ("progress" == evt.getPropertyName())
		{
			int progress = (Integer) evt.getNewValue();
			progressMonitor.setProgress(progress);
			String message = String.format("Completed %d%%.\n", progress);
			progressMonitor.setNote(message);
			if (progressMonitor.isCanceled() || binaryTask.isDone())
			{
				if (progressMonitor.isCanceled())
				{
					binaryTask.cancel(true);
				}
				else
				{
//                    taskOutput.append("Task completed.\n");
				}
			}
		}

	}
}
