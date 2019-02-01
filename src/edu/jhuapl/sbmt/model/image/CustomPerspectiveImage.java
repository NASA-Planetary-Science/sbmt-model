package edu.jhuapl.sbmt.model.image;

import java.io.IOException;

import vtk.vtkImageData;

import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.sbmt.client.SmallBodyModel;

import nom.tam.fits.FitsException;

public class CustomPerspectiveImage extends PerspectiveImage
{
    public CustomPerspectiveImage(ImageKeyInterface key, SmallBodyModel smallBodyModel, boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, null, loadPointingOnly);
    }

    protected void initialize() throws FitsException, IOException
    {

        super.initialize();

        setUseDefaultFootprint(true);
    }

    @Override
    public int getNumberBands()
    {
        return imageDepth;
    }

    @Override
    protected void processRawImage(vtkImageData rawImage)
    {
        ImageKeyInterface key = getKey();
        if (key.getSource() == ImageSource.LOCAL_PERSPECTIVE)
        {
                if (getFlip().equals("X"))
                {
                    ImageDataUtil.flipImageXAxis(rawImage);
                }
                else if (getFlip().equals("Y"))
                {
                    ImageDataUtil.flipImageYAxis(rawImage);
                }

                if (getRotation() != 0.0)
                    ImageDataUtil.rotateImage(rawImage, 360.0 - getRotation());
        }
    }

    @Override
    protected int[] getMaskSizes()
    {
        return new int[]{0, 0, 0, 0};
    }
}
