package edu.jhuapl.sbmt.model.lidar;

import vtk.vtkCellArray;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkUnsignedCharArray;

public class VtkUtil
{
	// Constants
	// An empty PolyData used for resetting
	private static final vtkPolyData EmptyPolyData = formEmptyPolyData();

	/**
	 * Method used to reset a vtkPolyData
	 */
	public static void clearPolyData(vtkPolyData aPolyData)
	{
		aPolyData.DeepCopy(EmptyPolyData);
	}

	/**
	 * Forms a vtkPolyData that is useful for resetting other vtkPolyDatas.
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
