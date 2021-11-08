package edu.jhuapl.sbmt.model.bennu.imaging;

import java.io.IOException;
import java.util.List;

import vtk.vtkImageData;

import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.image.common.ImageKeyInterface;

import nom.tam.fits.FitsException;

public class MapCamV4Image extends MapCamImage
{
    public MapCamV4Image(ImageKeyInterface key,
    		List<SmallBodyModel> smallBodyModel,
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
    }
}
