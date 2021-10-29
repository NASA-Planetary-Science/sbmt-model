package edu.jhuapl.sbmt.model.image.perspectiveImage.renderer;

public class PerspectiveImageFootprintBoundaryOperator
{

	public PerspectiveImageFootprintBoundaryOperator()
	{
		// TODO Auto-generated constructor stub
	}

//	//**********************
//	// Boundary
//	//**********************
//    vtkPolyData generateBoundary()
//    {
//        loadFootprint();
//
//        if (footprint[image.getCurrentSlice()].GetNumberOfPoints() == 0)
//            return null;
//
//        vtkFeatureEdges edgeExtracter = new vtkFeatureEdges();
//        edgeExtracter.SetInputData(footprint[image.getCurrentSlice()]);
//        edgeExtracter.BoundaryEdgesOn();
//        edgeExtracter.FeatureEdgesOff();
//        edgeExtracter.NonManifoldEdgesOff();
//        edgeExtracter.ManifoldEdgesOff();
//        edgeExtracter.Update();
//
//        vtkPolyData boundary = new vtkPolyData();
//        vtkPolyData edgeExtracterOutput = edgeExtracter.GetOutput();
//        boundary.DeepCopy(edgeExtracterOutput);
//
//        return boundary;
//    }
}
