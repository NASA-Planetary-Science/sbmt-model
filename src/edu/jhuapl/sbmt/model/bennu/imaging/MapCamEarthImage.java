package edu.jhuapl.sbmt.model.bennu.imaging;

import java.io.IOException;

import vtk.vtkImageData;

import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.image.interfaces.ImageKeyInterface;

import nom.tam.fits.FitsException;

public class MapCamEarthImage extends MapCamImage
{
    public MapCamEarthImage(ImageKeyInterface key,
            SmallBodyModel smallBodyModel,
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
        ImageDataUtil.rotateImage(rawImage, -90);
    }
}
