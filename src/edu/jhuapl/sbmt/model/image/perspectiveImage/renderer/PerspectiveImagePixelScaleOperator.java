package edu.jhuapl.sbmt.model.image.perspectiveImage.renderer;

import java.beans.PropertyChangeEvent;

import vtk.vtkPoints;
import vtk.vtkPolyData;

import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.core.rendering.PerspectiveImage;

public class PerspectiveImagePixelScaleOperator
{

    public double minHorizontalPixelScale = Double.MAX_VALUE;
    public double maxHorizontalPixelScale = -Double.MAX_VALUE;
    public double meanHorizontalPixelScale = 0.0;
    public double minVerticalPixelScale = Double.MAX_VALUE;
    public double maxVerticalPixelScale = -Double.MAX_VALUE;
    public double meanVerticalPixelScale = 0.0;

	public PerspectiveImagePixelScaleOperator()
	{
		// TODO Auto-generated constructor stub
	}

	public void computePixelScale(PerspectiveImage image)
    {
    	double[][] spacecraftPositionAdjusted = image.getSpacecraftPositionAdjusted();
        minHorizontalPixelScale = Double.MAX_VALUE;
        maxHorizontalPixelScale = -Double.MAX_VALUE;
        meanHorizontalPixelScale = 0.0;
        minVerticalPixelScale = Double.MAX_VALUE;
        maxVerticalPixelScale = -Double.MAX_VALUE;
        meanVerticalPixelScale = 0.0;


    	vtkPolyData[] footprints = image.getRendererHelper().getFootprint();

    	for (int currentSlice = 0; currentSlice<footprints.length; currentSlice++)
    	{
//	        if (footprintGenerated[currentSlice] == false)
//	            loadFootprint();

	        int numberOfPoints = (int)footprints[currentSlice].GetNumberOfPoints();
	        vtkPoints points = footprints[currentSlice].GetPoints();

	        double horizScaleFactor = 2.0 * Math.tan(MathUtil.vsep(image.getFrustum1Adjusted()[0], image.getFrustum3Adjusted()[0]) / 2.0) / image.getImageHeight();
	        double vertScaleFactor = 2.0 * Math.tan(MathUtil.vsep(image.getFrustum1Adjusted()[0], image.getFrustum2Adjusted()[0]) / 2.0) / image.getImageWidth();

	        double[] vec = new double[3];

	        for (int i = 0; i < numberOfPoints; ++i)
	        {
	            double[] pt = points.GetPoint(i);

	            vec[0] = pt[0] - spacecraftPositionAdjusted[0][0];
	            vec[1] = pt[1] - spacecraftPositionAdjusted[0][1];
	            vec[2] = pt[2] - spacecraftPositionAdjusted[0][2];
	            double dist = MathUtil.vnorm(vec);

	            double horizPixelScale = dist * horizScaleFactor;
	            double vertPixelScale = dist * vertScaleFactor;

	            if (horizPixelScale < minHorizontalPixelScale)
	                minHorizontalPixelScale = horizPixelScale;
	            if (horizPixelScale > maxHorizontalPixelScale)
	                maxHorizontalPixelScale = horizPixelScale;
	            if (vertPixelScale < minVerticalPixelScale)
	                minVerticalPixelScale = vertPixelScale;
	            if (vertPixelScale > maxVerticalPixelScale)
	                maxVerticalPixelScale = vertPixelScale;

	            meanHorizontalPixelScale += horizPixelScale;
	            meanVerticalPixelScale += vertPixelScale;
	        }

	        meanHorizontalPixelScale /= (double) numberOfPoints;
	        meanVerticalPixelScale /= (double) numberOfPoints;
	        points.Delete();
    	}
    }



    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_RESOLUTION_CHANGED.equals(evt.getPropertyName()))
        {
            this.minHorizontalPixelScale = Double.MAX_VALUE;
            this.maxHorizontalPixelScale = -Double.MAX_VALUE;
            this.minVerticalPixelScale = Double.MAX_VALUE;
            this.maxVerticalPixelScale = -Double.MAX_VALUE;
            this.meanHorizontalPixelScale = 0.0;
            this.meanVerticalPixelScale = 0.0;
        }
    }
}
