package edu.jhuapl.sbmt.model.bennu;

import java.io.IOException;

import vtk.vtkImageData;

import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.BasicPerspectiveImage;
import edu.jhuapl.sbmt.model.image.ImageKeyInterface;
import edu.jhuapl.sbmt.model.image.ImageSource;
import edu.jhuapl.sbmt.model.image.ImageType;

import nom.tam.fits.FitsException;

public class OcamsFlightImage extends BasicPerspectiveImage
{
    public static OcamsFlightImage of(ImageKeyInterface key, SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        return new OcamsFlightImage(key, smallBodyModel, loadPointingOnly);
    }

    protected OcamsFlightImage(ImageKeyInterface key, SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }

    @Override
    protected void processRawImage(vtkImageData rawImage)
    {
        // For some reason we need to do
        // this so the image is displayed properly.
        if (key.getSource() == ImageSource.SPICE)
        {
        	if (key.getImageType() != ImageType.NAVCAM_FLIGHT_IMAGE)
        	{
        		ImageDataUtil.flipImageYAxis(rawImage);
        		ImageDataUtil.rotateImage(rawImage, 90);
        	}
        	else
        	{
        		ImageDataUtil.flipImageYAxis(rawImage);
        	}
        }
        else
        {
            ImageDataUtil.flipImageXAxis(rawImage);
            ImageDataUtil.rotateImage(rawImage, 180);
        }
    }

}
