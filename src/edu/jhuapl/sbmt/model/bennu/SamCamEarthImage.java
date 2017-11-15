package edu.jhuapl.sbmt.model.bennu;

import java.io.IOException;

import vtk.vtkImageData;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.util.ImageDataUtil;

import nom.tam.fits.FitsException;

public class SamCamEarthImage extends MapCamImage
{
    public SamCamEarthImage(ImageKey key,
            SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }

    @Override
    protected void processRawImage(vtkImageData rawImage)
    {
        // Flip image along X axis. For some reason we need to do
        // this so the image is displayed properly.
        ImageDataUtil.flipImageXAxis(rawImage);
    }
}
