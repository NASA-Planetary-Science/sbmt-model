package edu.jhuapl.sbmt.model.lidar;

import java.awt.Color;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkUnsignedCharArray;

import edu.jhuapl.saavtk.util.SaavtkLODActor;
import edu.jhuapl.sbmt.lidar.LidarPoint;

/**
 * Class which contains the logic to render a single selected lidar point using
 * the VTK framework.
 * <P>
 * This class supports the following configurable state:
 * <UL>
 * <LI>Selected LidarPoint (index) and corresponding Track (index).
 * <LI>Point color
 * <LI>Point size
 * </UL>
 */
public class VtkPointPainter implements VtkPainter
{
	// Reference vars
	private LidarSearchDataCollection refModel;

	// State vars
	private LidarPoint lidarPoint;
	private Track lidarTrack;
	private Color pointColor;
	private boolean isStale;

	// VTK vars
	private vtkActor actor;
	private vtkPolyData polydata;
	private vtkPolyDataMapper pointsMapper;

	/**
	 * Standard Constructor
	 *
	 * @param aModel
	 */
	public VtkPointPainter(LidarSearchDataCollection aModel)
	{
		refModel = aModel;

		lidarPoint = null;
		lidarTrack = null;
		pointColor = new Color(0.1f, 0.1f, 1.0f);
		isStale = false;

		polydata = new vtkPolyData();
		VtkUtil.clearPolyData(polydata);

		pointsMapper = new vtkPolyDataMapper();
		pointsMapper.SetInputData(polydata);

		actor = new SaavtkLODActor();
		actor.SetMapper(pointsMapper);
		((SaavtkLODActor) actor).setQuadricDecimatedLODMapper(polydata);
		actor.GetProperty().SetColor(pointColor.getRed() / 255.0, pointColor.getGreen() / 255.0,
				pointColor.getBlue() / 255.0);
		actor.GetProperty().SetPointSize(1.0);
	}

	/**
	 * Returns the LidarPoint associated with this VtkPainter
	 */
	public LidarPoint getPoint()
	{
		return lidarPoint;
	}

	/**
	 * Returns the Track associated with this VtkPainter
	 */
	public Track getTrack()
	{
		return lidarTrack;
	}

	/**
	 * Sets in the LidarPoint and corresponding Track.
	 */
	public void setData(LidarPoint aLidarPoint, Track aLidarTrack)
	{
		lidarPoint = aLidarPoint;
		lidarTrack = aLidarTrack;

		markStale();
	}

	/**
	 * Sets in the point size associated with this VtkLidarPoint.
	 */
	public void setPointSize(double aSize)
	{
		actor.GetProperty().SetPointSize(aSize);
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

		if (lidarPoint != null && lidarTrack != null && lidarTrack.getIsVisible() == true)
		{
			Vector3D tmpTrans = refModel.getTranslation(lidarTrack);
			Vector3D tmpPos = lidarPoint.getTargetPosition();
			double[] targPosArr = refModel.transformLidarPoint(tmpTrans, tmpPos.toArray());

			int id1 = points.InsertNextPoint(targPosArr);
			vtkIdList idList = new vtkIdList();
			idList.InsertNextId(id1);

//			vtkPoints points = new vtkPoints();
//			points.InsertNextPoint(targPosArr);
//			polydata.SetPoints(points);

			vert.InsertNextCell(idList);
			colors.InsertNextTuple4(pointColor.getRed(), pointColor.getGreen(), pointColor.getBlue(), 255);
		}
//
//		polydata.Modified();

		isStale = false;
	}

}
