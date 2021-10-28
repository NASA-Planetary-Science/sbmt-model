package edu.jhuapl.sbmt.model.image.perspectiveImage.renderer;

import vtk.vtkImageCanvasSource2D;

import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.model.image.perspectiveImage.PerspectiveImage;

public class PerspectiveImageMaskingOperator
{
	private int[] currentMask = new int[4];
	private vtkImageCanvasSource2D maskSource;
	PerspectiveImage image;

	public PerspectiveImageMaskingOperator(PerspectiveImage image)
	{
		this.image = image;
	}

    void initializeMaskingAfterLoad()
    {
    	int[] masking = image.getMaskSizes();
        int topMask = masking[0];
        int rightMask = masking[1];
        int bottomMask = masking[2];
        int leftMask = masking[3];
        for (int i = 0; i < masking.length; ++i)
            currentMask[i] = masking[i];

        maskSource = new vtkImageCanvasSource2D();
        maskSource.SetScalarTypeToUnsignedChar();
        maskSource.SetNumberOfScalarComponents(1);
        // maskSource.SetExtent(0, imageWidth-1, 0, imageHeight-1, 0, imageDepth-1);
        maskSource.SetExtent(0, image.getImageWidth() - 1, 0, image.getImageHeight() - 1, 0, 0);
        // Initialize the mask to black which masks out the image
        maskSource.SetDrawColor(0.0, 0.0, 0.0, 0.0);
        maskSource.FillBox(0, image.getImageWidth() - 1, 0, image.getImageHeight() - 1);
        // Create a square inside mask which passes through the image.
        maskSource.SetDrawColor(255.0, 255.0, 255.0, 255.0);
        maskSource.FillBox(leftMask, image.getImageWidth() - 1 - rightMask, bottomMask, image.getImageHeight() - 1 - topMask);
        maskSource.Update();

//        for (int k = 0; k < image.getImageDepth(); k++)
//        {
//            footprint[k] = new vtkPolyData();
//        }

//        shiftedFootprint[0] = new vtkPolyData();
//        textureCoords = new vtkFloatArray();
//        normalsFilter = new vtkPolyDataNormals();
    }

    public void setCurrentMask(int[] masking)
    {
        int topMask = masking[0];
        int rightMask = masking[1];
        int bottomMask = masking[2];
        int leftMask = masking[3];
        // Initialize the mask to black which masks out the image
        maskSource.SetDrawColor(0.0, 0.0, 0.0, 0.0);
        maskSource.FillBox(0, image.getImageWidth() - 1, 0, image.getImageHeight() - 1);
        // Create a square inside mask which passes through the image.
        maskSource.SetDrawColor(255.0, 255.0, 255.0, 255.0);
        maskSource.FillBox(leftMask, image.getImageWidth() - 1 - rightMask, bottomMask, image.getImageHeight() - 1 - topMask);
        maskSource.Update();

        image.firePropertyChange(Properties.MODEL_CHANGED, null, this);
        setDisplayedImageRange(null);

        for (int i = 0; i < masking.length; ++i)
            currentMask[i] = masking[i];
    }

    int[] getCurrentMask()
    {
        return currentMask.clone();
    }

//    public void propertyChange(PropertyChangeEvent evt)
//    {
//        if (Properties.MODEL_RESOLUTION_CHANGED.equals(evt.getPropertyName()))
//        {
//            loadFootprint();
//            normalsGenerated = false;
//            this.minEmission = Double.MAX_VALUE;
//            this.maxEmission = -Double.MAX_VALUE;
//            this.minIncidence = Double.MAX_VALUE;
//            this.maxIncidence = -Double.MAX_VALUE;
//            this.minPhase = Double.MAX_VALUE;
//            this.maxPhase = -Double.MAX_VALUE;
//            this.minHorizontalPixelScale = Double.MAX_VALUE;
//            this.maxHorizontalPixelScale = -Double.MAX_VALUE;
//            this.minVerticalPixelScale = Double.MAX_VALUE;
//            this.maxVerticalPixelScale = -Double.MAX_VALUE;
//            this.meanHorizontalPixelScale = 0.0;
//            this.meanVerticalPixelScale = 0.0;
//
//            image.firePropertyChange(Properties.MODEL_CHANGED, null, this);
//        }
//    }

}
