package edu.jhuapl.sbmt.model.lidar.vtk;

import java.util.Iterator;

import vtk.vtkCellArray;
import vtk.vtkDataObject;
import vtk.vtkDoubleArray;
import vtk.vtkGeometryFilter;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkUnsignedCharArray;
import vtk.vtkVertex;

import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.model.lidar.feature.FeatureAttr;
import edu.jhuapl.sbmt.model.lidar.feature.FeatureAttrBuilder;
import edu.jhuapl.sbmt.model.lidar.feature.VtkFeatureAttr;

/**
 * Collection of VTK based utility methods.
 *
 * @author lopeznr1
 */
public class VtkUtil
{
	// Constants
	// An empty PolyData used for resetting
	private static final vtkPolyData EmptyPolyData = formEmptyPolyData();

	/**
	 * Utility method used to reset a vtkPolyData
	 */
	public static void clearPolyData(vtkPolyData aPolyData)
	{
		aPolyData.DeepCopy(EmptyPolyData);
	}

	/**
	 * Utility method that will create a vtkGeometryFilter for the specified
	 * vtkDataObject.
	 */
	public static vtkGeometryFilter formGeometryFilter(vtkDataObject aVtkDataObject, int aNumPts)
	{
		vtkGeometryFilter retVtkGeometryFilter = new vtkGeometryFilter();
		retVtkGeometryFilter.SetInputData(aVtkDataObject);
		retVtkGeometryFilter.PointClippingOn();
		retVtkGeometryFilter.CellClippingOff();
		retVtkGeometryFilter.ExtentClippingOff();
		retVtkGeometryFilter.MergingOff();
		retVtkGeometryFilter.SetPointMinimum(0);
		retVtkGeometryFilter.SetPointMaximum(aNumPts);

		return retVtkGeometryFilter;
	}

	/**
	 * Utility method that will create a {@link VtkLidarStruct} for the specified
	 * parameters.
	 * <P>
	 * The returned {@link VtkLidarStruct} life cycle must be managed by the
	 * caller of this method.
	 *
	 * @param aPointIter The LidarPoints associated with the lidar data object.
	 */
	public static VtkLidarStruct formVtkLidarStruct(Iterator<LidarPoint> aPointIter)
	{
		FeatureAttrBuilder intensityFAB = new FeatureAttrBuilder();
		vtkDoubleArray vRangeDA = new vtkDoubleArray();
		vtkDoubleArray vTimeDA = new vtkDoubleArray();
		vtkPoints vSrcP = new vtkPoints();
		vtkPoints vTgtP = new vtkPoints();
		vtkCellArray vSrcCA = new vtkCellArray();
		vtkCellArray vTgtCA = new vtkCellArray();

		// Update the VTK state to reflect the provided LidarPoints
		while (aPointIter.hasNext())
		{
			LidarPoint pt = aPointIter.next();

			int id = vTgtP.InsertNextPoint(pt.getTargetPosition().toArray());
			vtkVertex v = new vtkVertex();
			v.GetPointIds().SetId(0, id);

			// Keep track of features of interest
			vTgtCA.InsertNextCell(v);
			vRangeDA.InsertNextValue(pt.getSourcePosition().subtract(pt.getTargetPosition()).getNorm());

			double irec = pt.getIntensityReceived();
			intensityFAB.addValue(irec);

			int id2 = vSrcP.InsertNextPoint(pt.getSourcePosition().toArray());
			vtkVertex v2 = new vtkVertex();
			v2.GetPointIds().SetId(0, id2);
			vSrcCA.InsertNextCell(v2);
			vTimeDA.InsertNextValue(pt.getTime());
		}

		// Instantiate the VtkLidarStruct
		FeatureAttr timeFA = new VtkFeatureAttr(vTimeDA);
		FeatureAttr rangeFA = new VtkFeatureAttr(vRangeDA);
		FeatureAttr intensityFA = intensityFAB.build();

		VtkLidarStruct retVLS = new VtkLidarStruct(timeFA, rangeFA, intensityFA, vSrcP, vSrcCA, vTgtP, vTgtCA);
		return retVLS;
	}

	/**
	 * Utility helper method that forms a vtkPolyData that is useful for
	 * resetting other vtkPolyDatas.
	 */
	private static vtkPolyData formEmptyPolyData()
	{
		vtkPolyData retPolyData;

		// Initialize an empty polydata for resetting
		retPolyData = new vtkPolyData();
		vtkPoints points = new vtkPoints();
		vtkCellArray vert = new vtkCellArray();
		retPolyData.SetPoints(points);
		retPolyData.SetVerts(vert);
		vtkUnsignedCharArray colors = new vtkUnsignedCharArray();
		colors.SetNumberOfComponents(4);
		retPolyData.GetCellData().SetScalars(colors);

		return retPolyData;
	}

}
