package edu.jhuapl.sbmt.model.image;

import java.io.IOException;
import java.util.List;

import vtk.vtkImageData;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.perspectiveImage.PerspectiveImage;

import nom.tam.fits.FitsException;

public class CustomPerspectiveImage extends PerspectiveImage
{
    public CustomPerspectiveImage(ImageKeyInterface key, List<SmallBodyModel> smallBodyModel, boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }

    protected void initialize() throws FitsException, IOException
    {

        super.initialize();

//        setUseDefaultFootprint(true);
    }

    @Override
    protected void processRawImage(vtkImageData rawImage)
    {
        ImageKeyInterface key = getKey();
        if (key.getSource() == ImageSource.LOCAL_PERSPECTIVE)
        {
             super.processRawImage(rawImage);
        }
    }

    @Override
    public int[] getMaskSizes()
    {
        return new int[]{0, 0, 0, 0};
    }
}
