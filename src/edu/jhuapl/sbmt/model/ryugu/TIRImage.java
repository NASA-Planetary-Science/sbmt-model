package edu.jhuapl.sbmt.model.ryugu;

import java.io.File;
import java.io.IOException;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.image.interfaces.ImageKeyInterface;
import edu.jhuapl.sbmt.image.model.PerspectiveImage;
import nom.tam.fits.FitsException;
import vtk.vtkImageData;

public class TIRImage extends PerspectiveImage
{
 // Size of image after resampling. Before resampling image is 328x248 pixels.
    // MSI pixels are resampled to make them square. According to SPICE kernel msi15.ti,
    // MSI pixel size in degrees is 12.66/248 in Y; 16.74/328 in X. To square the
    // pixels, resample X to 12.66 * 328/16.74 = ~412.
    public static final int RESAMPLED_IMAGE_WIDTH = 344;
    public static final int RESAMPLED_IMAGE_HEIGHT = 260;

    public TIRImage(ImageKeyInterface key, SmallBodyModel smallBodyModel, boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }

    @Override
    public int[] getMaskSizes()
    {
        return new int[] { 0, 0, 0, 0 };
    }

    @Override
    protected String initializeFitFileFullPath()
    {
        return FileCache.getFileFromServer(getKey().getName() + ".fit").getAbsolutePath();
    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        ImageKeyInterface key = getKey();
        File keyFile = new File(key.getName());
        String sumFilename = keyFile.getParentFile().getParent()
        + "/infofiles/" + keyFile.getName() + ".INFO";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    protected void processRawImage(vtkImageData rawImage)
    {
        // Flip image along y axis. For some reason we need to do
        // this so the image is displayed properly.
        //ImageKey key = getKey();
        //if (key.source.equals(ImageSource.SPICE))
            //ImageDataUtil.rotateImage(rawImage, -90);
//        int[] dims = rawImage.GetDimensions();
//        int originalHeight = dims[1];
//
//        vtkImageReslice reslice = new vtkImageReslice();
//        reslice.SetInputData(rawImage);
//        reslice.SetInterpolationModeToLinear();
//        reslice.SetOutputSpacing((double)dims[0]/(double)RESAMPLED_IMAGE_WIDTH, (double)originalHeight/(double)RESAMPLED_IMAGE_HEIGHT, 1.0);
//        reslice.SetOutputOrigin(0.0, 0.0, 0.0);
//        reslice.SetOutputExtent(0, RESAMPLED_IMAGE_WIDTH-1, 0, RESAMPLED_IMAGE_HEIGHT-1, 0, 0);
//        reslice.Update();
//
//        vtkImageData resliceOutput = reslice.GetOutput();
//        rawImage.DeepCopy(resliceOutput);
//        rawImage.SetSpacing(1, 1, 1);
//
//        ImageDataUtil.rotateImage(rawImage, 90.0);
//        ImageDataUtil.flipImageXAxis(rawImage);
        ImageDataUtil.flipImageYAxis(rawImage);
    }
}
