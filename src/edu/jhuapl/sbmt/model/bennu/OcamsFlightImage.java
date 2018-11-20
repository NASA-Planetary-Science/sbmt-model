package edu.jhuapl.sbmt.model.bennu;

import java.io.IOException;

import vtk.vtkImageData;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.BasicPerspectiveImage;
import edu.jhuapl.sbmt.util.ImageDataUtil;

import nom.tam.fits.FitsException;

public class OcamsFlightImage extends BasicPerspectiveImage
{
    public static OcamsFlightImage of(ImageKey key, SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        return new OcamsFlightImage(key, smallBodyModel, loadPointingOnly);
    }

    protected OcamsFlightImage(ImageKey key, SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }

    @Override
    protected void processRawImage(vtkImageData rawImage)
    {
        // For some reason we need to do
        // this so the image is displayed properly.
        ImageDataUtil.flipImageXAxis(rawImage);
        ImageDataUtil.rotateImage(rawImage, 180);
    }

}
