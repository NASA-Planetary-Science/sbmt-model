package edu.jhuapl.sbmt.model.lidar;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
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

/**
 * Class which contains the logic to render a collection of lidar Tracks using
 * the VTK framework.
 * <P>
 * This class supports the following configurable state:
 * <UL>
 * <LI>List of tracks
 * <LI>Point size
 * </UL>
 */
public class VtkTrackPainter implements VtkPainter
{
	// Reference vars
	private LidarSearchDataCollection refModel;

	// State vars
	private ImmutableList<Track> trackL;
	private boolean isStale;

	// VTK vars
	private vtkActor actor;
	private vtkPolyData polydata;
	private vtkPolyDataMapper pointsMapper;

	// TODO: Unknown vars
	private float lightnessSpanBase = 0.5f;
	private List<Integer> displayedPointToOriginalPointMap = new ArrayList<Integer>();

	/**
	 * Standard Constructor
	 *
	 * @param aModel
	 */
	public VtkTrackPainter(LidarSearchDataCollection aModel)
	{
		refModel = aModel;

		trackL = ImmutableList.of();
		isStale = false;

		polydata = new vtkPolyData();
		VtkUtil.clearPolyData(polydata);

		pointsMapper = new vtkPolyDataMapper();
		pointsMapper.SetScalarModeToUseCellData();
		pointsMapper.SetInputData(polydata);

		actor = new SaavtkLODActor();
		actor.SetMapper(pointsMapper);
		((SaavtkLODActor) actor).setQuadricDecimatedLODMapper(polydata);
		actor.GetProperty().SetPointSize(2.0);
	}

	/**
	 * Returns the LidarPoint corresponding to the specified cellId.
	 *
	 * @param aCellId
	 */
	public LidarPoint getLidarPointFromCellId(int aCellId)
	{
		int tmpIdx = displayedPointToOriginalPointMap.get(aCellId);
		return refModel.getPoint(tmpIdx);
	}

	/**
	 * Returns the Track corresponding to the specified cellId.
	 *
	 * @param aCellId
	 */
	public Track getTrackFromCellId(int aCellId)
	{
		int tmpIdx = displayedPointToOriginalPointMap.get(aCellId);
		for (Track aTrack : trackL)
		{
			if (aTrack.containsId(tmpIdx) == true)
				return aTrack;
		}

		return null;
	}

	/**
	 * Sets in the point size associated with this VtkTrackGroup.
	 */
	public void setPointSize(int aSize)
	{
		actor.GetProperty().SetPointSize(aSize);
	}

	/**
	 * Sets in the Tracks that VtkTrackGroup will render.
	 */
	public void setTracks(Collection<Track> aTrackL)
	{
		trackL = ImmutableList.copyOf(aTrackL);

		markStale();
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

		displayedPointToOriginalPointMap.clear();

		for (Track aTrack : trackL)
		{
			int startId = aTrack.startId;
			int stopId = aTrack.stopId;
			if (aTrack.getIsVisible() == true)
			{
				// Variables to keep track of intensities
				double minIntensity = Double.POSITIVE_INFINITY;
				double maxIntensity = Double.NEGATIVE_INFINITY;
				List<Double> intensityList = new LinkedList<Double>();

				// Go through each point in the track
				Vector3D tmpTrans = refModel.getTranslation(aTrack);
				for (int i = startId; i <= stopId; i++)
				{
					LidarPoint tmpLP = refModel.getPoint(i);

					double[] pt = tmpLP.getTargetPosition().toArray();
					pt = refModel.transformLidarPoint(tmpTrans, pt);
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

					displayedPointToOriginalPointMap.add(i);
				}

				// Assign colors to each point in that track
				Color trackColor = aTrack.color;
				float[] trackHSL = ColorUtil.getHSLColorComponents(trackColor);
				Color plotColor;
				for (double intensity : intensityList)
				{
					plotColor = ColorUtil.scaleLightness(trackHSL, intensity, minIntensity, maxIntensity, lightnessSpanBase);
					colors.InsertNextTuple4(plotColor.getRed(), plotColor.getGreen(), plotColor.getBlue(),
							plotColor.getAlpha());
				}
			}
		}
		polydata.GetCellData().GetScalars().Modified();
		polydata.Modified();

		isStale = false;
	}

}
