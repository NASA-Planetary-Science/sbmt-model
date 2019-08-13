package edu.jhuapl.sbmt.model.lidar;

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

import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import vtk.vtkCellArray;
import vtk.vtkDoubleArray;
import vtk.vtkIdList;
import vtk.vtkPoints;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.lidar.hyperoctree.hayabusa2.Hayabusa2RawLidarFile;
import edu.jhuapl.sbmt.model.image.Instrument;
import edu.jhuapl.sbmt.model.lidar.feature.FeatureAttr;
import edu.jhuapl.sbmt.model.lidar.feature.FeatureAttrBuilder;
import edu.jhuapl.sbmt.model.lidar.feature.VtkFeatureAttr;
import edu.jhuapl.sbmt.model.lidar.vtk.VtkLidarPointProvider;
import edu.jhuapl.sbmt.model.lidar.vtk.VtkLidarStruct;
import edu.jhuapl.sbmt.model.lidar.vtk.VtkLidarUniPainter;
import edu.jhuapl.sbmt.model.lidar.vtk.VtkUtil;
import edu.jhuapl.sbmt.util.TimeUtil;

class BinaryDataTask extends SwingWorker<Void, Void> implements PropertyChangeListener
{
	private final File refFile;
	private final LidarFileSpecManager refManager;
	private final LidarFileSpec refFileSpec;
	private final BodyViewConfig refBodyViewConfig;
	private ProgressMonitor progressMonitor;

	private VtkLidarPointProvider workLPP;
	private VtkLidarUniPainter<LidarFileSpec> workPainter;

	public BinaryDataTask(File aFile, LidarFileSpecManager aManager, LidarFileSpec aFileSpec,
			BodyViewConfig aBodyViewConfig)
	{
		refFile = aFile;
		refManager = aManager;
		refFileSpec = aFileSpec;
		refBodyViewConfig = aBodyViewConfig;
		progressMonitor = new ProgressMonitor(null, "Loading OLA Data", "", 0, 100);

		workLPP = null;
		workPainter = null;

		// Register for events of interest
		addPropertyChangeListener(this);
	}

	public void setProgressVal(int aVal)
	{
		setProgress(aVal);
	}

	@Override
	protected Void doInBackground() throws Exception
	{
		VtkLidarStruct tmpVLS = LidarFileSpecLoadUtil.loadBinaryLidarData(this, refFile, refBodyViewConfig);

		workLPP = new VtkLidarPointProvider(tmpVLS.vSrcP, tmpVLS.vTgtP);
		workPainter = new VtkLidarUniPainter<>(refManager, refFileSpec, tmpVLS);

		return null;
	}

	@Override
	protected void done()
	{
		refManager.markLidarLoadComplete(refFileSpec, workLPP, workPainter);
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		if ("progress" == evt.getPropertyName())
		{
			int progress = (Integer) evt.getNewValue();
			progressMonitor.setProgress(progress);
			String message = String.format("Completed %d%%.\n", progress);
			progressMonitor.setNote(message);
			if (progressMonitor.isCanceled() || isDone())
			{
				if (progressMonitor.isCanceled())
				{
					cancel(true);
				}
				else
				{
//                 taskOutput.append("Task completed.\n");
				}
			}
		}

	}
}

/**
 * Collection of utility methods used to load lidar data.
 * <P>
 * Currently the following lidar data file types are supported:
 * <UL>
 * <LI>ASCII lidar data files
 * <LI>Binary lidar data files
 * <LI>Hayabusa formatted files
 * </UL>
 *
 * @author lopeznr1
 */
public class LidarFileSpecLoadUtil
{
	/**
	 * Utility method that returns true if this is hayabusa specific data.
	 * <P>
	 * TODO: Due to the deficient design several aspects of this code is specific
	 * to an instrument. This should be rectified.
	 */
	@Deprecated
	public static boolean isHayabusaData(BodyViewConfig aBodyViewConfig)
	{
		return aBodyViewConfig.lidarInstrumentName == Instrument.LASER;
	}

	/**
	 * Utility method to load the lidar data associated with the specified
	 * LidarFileSpec into a {@link VtkLidarUniPainter}.
	 * <P>
	 * On success the loaded {@link VtkLidarUniPainter} will be installed into
	 * the provided LidarFileSpecManager.
	 *
	 * @param aManager
	 * @param aFileSpec
	 * @param aBodyViewConfig
	 * @throws IOException
	 */
	public static void initLidarData(LidarFileSpecManager aManager, LidarFileSpec aFileSpec,
			BodyViewConfig aBodyViewConfig) throws IOException
	{
		// Retrieve the file of interest
		File tmpFile = FileCache.getFileFromServer(SafeURLPaths.instance().getString(aFileSpec.getPath()));
		if (tmpFile == null)
			throw new IOException(aFileSpec.getPath() + " could not be loaded");

		// Load hayabusa lidar data
		if (isHayabusaData(aBodyViewConfig) == true)
		{
			VtkLidarStruct tmpVLS = LidarFileSpecLoadUtil.loadHayabusaDataFile(tmpFile);

			VtkLidarPointProvider tmpLPP = new VtkLidarPointProvider(tmpVLS.vSrcP, tmpVLS.vTgtP);
			VtkLidarUniPainter<LidarFileSpec> tmpPainter = new VtkLidarUniPainter<>(aManager, aFileSpec, tmpVLS);

			aManager.markLidarLoadComplete(aFileSpec, tmpLPP, tmpPainter);
		}

		// Load binary lidar data
		else if (aBodyViewConfig.lidarBrowseIsBinary == true)
		{
			BinaryDataTask binaryTask = new BinaryDataTask(tmpFile, aManager, aFileSpec, aBodyViewConfig);
			binaryTask.execute();
		}

		// Load ascii lidar data
		else
		{
			VtkLidarStruct tmpVLS = LidarFileSpecLoadUtil.loadAsciiLidarData(tmpFile, aBodyViewConfig);

			VtkLidarPointProvider tmpLPP = new VtkLidarPointProvider(tmpVLS.vSrcP, tmpVLS.vTgtP);
			VtkLidarUniPainter<LidarFileSpec> tmpPainter = new VtkLidarUniPainter<>(aManager, aFileSpec, tmpVLS);

			aManager.markLidarLoadComplete(aFileSpec, tmpLPP, tmpPainter);
		}
	}

	/**
	 * Utility helper method that will load a Hayabusa lidar data file into a
	 * {@link VtkLidarStruct}.
	 * <P>
	 * Returns the loaded {@link VtkLidarStruct}.
	 *
	 * @param aManager
	 * @param aFileSpec
	 * @param aFile
	 */
	private static VtkLidarStruct loadHayabusaDataFile(File aFile) throws IOException
	{
		// Delegate the loading of the file
		Hayabusa2RawLidarFile lidarFile = new Hayabusa2RawLidarFile(aFile.getAbsolutePath());

		// Delegate the creation of VtkLidarStruct
		return VtkUtil.formVtkLidarStruct(lidarFile.iterator());
	}

	/**
	 * Utility helper method that will load ASCII lidar data file into a
	 * {@link VtkLidarStruct}.
	 * <P>
	 * Returns the loaded {@link VtkLidarStruct}.
	 *
	 * @param aManager
	 * @param aFileSpec
	 * @param aFile
	 */
	private static VtkLidarStruct loadAsciiLidarData(File aFile, BodyViewConfig aBodyViewConfig) throws IOException
	{
		FeatureAttrBuilder intensityFAB = new FeatureAttrBuilder();
		vtkDoubleArray vRangeDA = new vtkDoubleArray();
		vtkDoubleArray vTimeDA = new vtkDoubleArray();
		vtkPoints vSrcP = new vtkPoints();
		vtkPoints vTgtP = new vtkPoints();
		vtkCellArray vSrcCA = new vtkCellArray();
		vtkCellArray vTgtCA = new vtkCellArray();
		int numPts;

		vtkIdList idList = new vtkIdList();
		idList.SetNumberOfIds(1);

		// Load the file
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

			vTgtP.InsertNextPoint(x, y, z);
			idList.SetId(0, numPts);
			vTgtCA.InsertNextCell(idList);
			vSrcP.InsertNextPoint(scx, scy, scz);
			vSrcCA.InsertNextCell(idList);

			// Keep track of features of interest
			// Range data
			double range;
			if (isRangeExplicitInData)
			{
				// Range is explicitly listed in data, get it
				range = Double.parseDouble(vals[rangeIndex]);
				if (isInMeters)
					range /= 1000.0;
			}
			else
			{
				// Range is not explicitly listed, derive it from lidar measurement
				// and sc positions
				range = Math.sqrt((x - scx) * (x - scx) + (y - scy) * (y - scy) + (z - scz) * (z - scz));
			}
			vRangeDA.InsertNextValue(range);

			// Received intensity data
			double irec = 0.0;
			if (intensityEnabled)
				irec = Double.parseDouble(vals[receivedIntensityIndex]);

			intensityFAB.addValue(irec);

			// Time data
			// We store the times in a vtk array. By storing in a vtk array, we
			// don't have to worry about java out of memory errors since java
			// doesn't know about c++ memory.
			// Time is either in ET or UTC
			double t;
			if (isTimeInET)
				t = Double.parseDouble(vals[timeIndex]);
			else
				t = TimeUtil.str2et(vals[timeIndex]);
			vTimeDA.InsertNextValue(t);

			++numPts;
		}
		in.close();

		// Instantiate the VtkLidarStruct
		FeatureAttr timeFA = new VtkFeatureAttr(vTimeDA);
		FeatureAttr rangeFA = new VtkFeatureAttr(vRangeDA);
		FeatureAttr intensityFA = intensityFAB.build();

		return new VtkLidarStruct(timeFA, rangeFA, intensityFA, vSrcP, vSrcCA, vTgtP, vTgtCA);
	}

	/**
	 * Utility helper method that will load binary lidar data file into a
	 * {@link VtkLidarStruct}.
	 * <P>
	 * Returns the loaded {@link VtkLidarStruct}.
	 */
	protected static VtkLidarStruct loadBinaryLidarData(BinaryDataTask aTask, File aFile, BodyViewConfig aBodyViewConfig)
			throws IOException
	{
		vtkDoubleArray vRangeDA = new vtkDoubleArray();
		vtkDoubleArray vTimeDA = new vtkDoubleArray();
		vtkPoints vSrcP = new vtkPoints();
		vtkPoints vTgtP = new vtkPoints();
		vtkCellArray vSrcCA = new vtkCellArray();
		vtkCellArray vTgtCA = new vtkCellArray();

		vtkIdList idList = new vtkIdList();
		idList.SetNumberOfIds(1);

		// Load the file
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
			throw new IOException("Error reading: " + aFile);
		}

		byte[] utcArray = new byte[24];

		int numRecords = (int) (aFile.length() / binaryRecordSize);
		FeatureAttrBuilder intensityFAB = new FeatureAttrBuilder(numRecords);

		for (int aCount = 0; aCount < numRecords; aCount++)
		{
			aTask.setProgressVal(aCount * 100 / numRecords);
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
			vTgtP.InsertNextPoint(x, y, z);
			idList.SetId(0, aCount);
			vTgtCA.InsertNextCell(idList);

			// Keep track of features of interest
			// Range data
			vRangeDA.InsertNextValue(Math.sqrt((x - scx) * (x - scx) + (y - scy) * (y - scy) + (z - scz) * (z - scz)));

			// Received intensity
			double irec = 0.0;
			if (intensityEnabled)
			{
				int recIntensityOffset = aCount * binaryRecordSize + receivedIntensityIndex;
				irec = bb.getDouble(recIntensityOffset);
			}
			intensityFAB.addValue(irec);

			// assume no spacecraft position for now
			vSrcP.InsertNextPoint(scx, scy, scz);
			vSrcCA.InsertNextCell(idList);

			int timeoffset = aCount * binaryRecordSize + timeIndex;

			bb.position(timeoffset);
			bb.get(utcArray);
			String utc = new String(utcArray);

			// Time data
			// We store the times in a vtk array. By storing in a vtk array, we
			// don't have to worry about java out of memory errors since java
			// doesn't know about c++ memory.
			double t = TimeUtil.str2et(utc);
			vTimeDA.InsertNextValue(t);
		}
		fs.close();

		// Instantiate the VtkLidarStruct
		FeatureAttr timeFA = new VtkFeatureAttr(vTimeDA);
		FeatureAttr rangeFA = new VtkFeatureAttr(vRangeDA);
		FeatureAttr intensityFA = intensityFAB.build();

		aTask.setProgressVal(100);

		return new VtkLidarStruct(timeFA, rangeFA, intensityFA, vSrcP, vSrcCA, vTgtP, vTgtCA);
	}

}
