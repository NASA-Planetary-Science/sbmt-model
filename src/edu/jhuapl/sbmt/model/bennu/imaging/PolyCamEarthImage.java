package edu.jhuapl.sbmt.model.bennu.imaging;

import java.io.IOException;

import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.image.interfaces.ImageKeyInterface;
import nom.tam.fits.FitsException;
import vtk.vtkImageData;

public class PolyCamEarthImage extends PolyCamImage
{
    public PolyCamEarthImage(ImageKeyInterface key, SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }

    @Override
    protected void processRawImage(vtkImageData rawImage)
    {
        // Flip image along y axis. For some reason we need to do
        // this so the image is displayed properly.
        ImageDataUtil.flipImageYAxis(rawImage);
        ImageDataUtil.rotateImage(rawImage, 90);
    }
}