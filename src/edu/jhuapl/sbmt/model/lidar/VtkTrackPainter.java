package edu.jhuapl.sbmt.model.lidar;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.collect.ImmutableList;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkUnsignedCharArray;

import edu.jhuapl.saavtk.util.ColorUtil;
import edu.jhuapl.saavtk.util.SaavtkLODActor;
import edu.jhuapl.sbmt.lidar.LidarPoint;

import glum.item.ItemEventListener;
import glum.item.ItemEventType;

/**
 * Package private class which contains the logic to render a collection of
 * lidar Tracks using the VTK framework.
 * <P>
 * This class supports the following configurable state:
 * <UL>
 * <LI>List of tracks
 * <LI>Point size
 * </UL>
 *
 * @author lopeznr1
 */
class VtkTrackPainter implements ItemEventListener, VtkPainter
{
	// Reference vars
	private LidarTrackManager refManager;

	// State vars
	private ImmutableList<LidarTrack> trackL;
	private boolean isStale;

	// Lookup vars
	private List<LidarPoint> cellIdToPointLUL;
	private List<LidarTrack> cellIdToTrackLUL;

	// VTK vars
	private vtkActor actor;
	private vtkPolyData polydata;
	private vtkPolyDataMapper pointsMapper;

	// TODO: Unknown vars
	private float lightnessSpanBase = 0.5f;

	/**
	 * Standard Constructor
	 *
	 * @param aManager
	 */
	public VtkTrackPainter(LidarTrackManager aManager)
	{
		refManager = aManager;

		trackL = ImmutableList.of();
		isStale = false;

		cellIdToPointLUL = new ArrayList<>();
		cellIdToTrackLUL = new ArrayList<>();

		polydata = new vtkPolyData();
		VtkUtil.clearPolyData(polydata);

		pointsMapper = new vtkPolyDataMapper();
		pointsMapper.SetScalarModeToUseCellData();
		pointsMapper.SetInputData(polydata);

		actor = new SaavtkLODActor();
		actor.SetMapper(pointsMapper);
		((SaavtkLODActor) actor).setQuadricDecimatedLODMapper(polydata);
		actor.GetProperty().SetPointSize(2.0);

		// Register for events of interest
		refManager.addListener(this);
	}

	/**
	 * Returns the LidarPoint corresponding to the specified cellId.
	 *
	 * @param aCellId
	 */
	public LidarPoint getLidarPointFromCellId(int aCellId)
	{
		return cellIdToPointLUL.get(aCellId);
	}

	/**
	 * Returns the Track corresponding to the specified cellId.
	 *
	 * @param aCellId
	 */
	public LidarTrack getTrackFromCellId(int aCellId)
	{
		return cellIdToTrackLUL.get(aCellId);
	}

	/**
	 * Sets in the point size associated with this VtkTrackGroup.
	 */
	public void setPointSize(int aSize)
	{
		actor.GetProperty().SetPointSize(aSize);
	}

	@Override
	public void handleItemEvent(Object aSource, ItemEventType aEventType)
	{
		if (aEventType == ItemEventType.ItemsChanged)
		{
			trackL = refManager.getAllItems();
			markStale();
		}
		if (aEventType == ItemEventType.ItemsMutated)
		{
			markStale();
		}
	}

	@Override
	public vtkActor getActor()
	{
		return actor;
	}

	@Override
	public void markStale()
	{
		isStale = true;
	}

	@Override
	public void updateVtkVars()
	{
		// Bail if we are not stale
		if (isStale == false)
			return;

		VtkUtil.clearPolyData(polydata);

		vtkPoints points = polydata.GetPoints();
		vtkCellArray vert = polydata.GetVerts();
		vtkUnsignedCharArray colors = (vtkUnsignedCharArray) polydata.GetCellData().GetScalars();

		vtkIdList idList = new vtkIdList();
		idList.SetNumberOfIds(1);

		cellIdToPointLUL.clear();
		cellIdToTrackLUL.clear();

		for (LidarTrack aTrack : trackL)
		{
			// Skip to next if the Track is not visible
			if (refManager.getIsVisible(aTrack) == false)
				continue;

			// Variables to keep track of intensities
			double minIntensity = Double.POSITIVE_INFINITY;
			double maxIntensity = Double.NEGATIVE_INFINITY;
			List<Double> intensityList = new ArrayList<>();

			// Go through each point in the track
			Vector3D tmpTrans = refManager.getTranslation(aTrack);
			for (LidarPoint tmpLP : aTrack.getPointList())
			{
				double[] pt = tmpLP.getTargetPosition().toArray();
				pt = refManager.transformLidarPoint(tmpTrans, pt);
				int id = points.InsertNextPoint(pt);
				idList.SetId(0, id);
				vert.InsertNextCell(idList);

				// pt=originalPoints.get(i).getSourcePosition().toArray();
				// pt=transformLidarPoint(pt);
				// scPoints.InsertNextPoint(pt);
				// scVert.InsertNextCell(idList);

				double intensityReceived = tmpLP.getIntensityReceived();
				minIntensity = (intensityReceived < minIntensity) ? intensityReceived : minIntensity;
				maxIntensity = (intensityReceived > maxIntensity) ? intensityReceived : maxIntensity;
				intensityList.add(intensityReceived);

				cellIdToPointLUL.add(tmpLP);
				cellIdToTrackLUL.add(aTrack);
			}

			// Assign colors to each point in that track
			Color trackColor = refManager.getColor(aTrack);
			float[] trackHSL = ColorUtil.getHSLColorComponents(trackColor);
			Color plotColor;
			for (double intensity : intensityList)
			{
				plotColor = ColorUtil.scaleLightness(trackHSL, intensity, minIntensity, maxIntensity, lightnessSpanBase);
				colors.InsertNextTuple4(plotColor.getRed(), plotColor.getGreen(), plotColor.getBlue(),
						plotColor.getAlpha());
			}
		}
		polydata.GetCellData().GetScalars().Modified();
		polydata.Modified();

		isStale = false;
	}

}
