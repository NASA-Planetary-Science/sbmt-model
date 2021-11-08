package edu.jhuapl.sbmt.model.mvic;

import java.io.File;
import java.io.IOException;
import java.util.List;

import vtk.vtkImageData;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.image.common.ImageKeyInterface;
import edu.jhuapl.sbmt.image.types.perspectiveImage.PerspectiveImage;

import nom.tam.fits.FitsException;

public class MVICJupiterImage extends PerspectiveImage
{
    public MVICJupiterImage(ImageKeyInterface key, List<SmallBodyModel> smallBodyModel,
            boolean loadPointingOnly) throws FitsException,
            IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }

    @Override
    protected void processRawImage(vtkImageData rawImage)
    {
        // Flip image along y axis and y axis. For some reason we need to do
        // this so the image is displayed properly.
        ImageDataUtil.flipImageYAxis(rawImage);
        ImageDataUtil.flipImageXAxis(rawImage);

        super.processRawImage(rawImage);
    }

    @Override
    public int[] getMaskSizes()
    {
        return new int[]{0, 0, 0, 0};
    }

    @Override
    protected String initializeFitFileFullPath()
    {
        ImageKeyInterface key = getKey();
        return FileCache.getFileFromServer(key.getName() + ".fit").getAbsolutePath();
    }

    public double getFocalLength() { return 657.5; }    // in mm

    public double getPixelWidth() { return 0.013; }    // in mm

    public double getPixelHeight() { return 0.013; }   // in mm

    @Override
    protected String initializeLabelFileFullPath()
    {
        ImageKeyInterface key = getKey();
        File keyFile = new File(key.getName());
        String sumFilename = keyFile.getParentFile().getParent() + "/labelfiles/"
        + keyFile.getName().split("\\.")[0] + ".lbl";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        ImageKeyInterface key = getKey();
        File keyFile = new File(key.getName());
        String sumFilename = keyFile.getParentFile().getParent() + "/infofiles/"
        + keyFile.getName() + ".INFO";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    protected String initializeSumfileFullPath()
    {
        ImageKeyInterface key = getKey();
        File keyFile = new File(key.getName());
        String sumFilename = keyFile.getParentFile().getParent() + "/sumfiles/"
        + keyFile.getName().split("\\.")[0] + ".SUM";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    protected vtkImageData createRawImage(int height, int width, int depth, float[][] array2D, float[][][] array3D)
    {
        return createRawImage(height, width, depth, false, array2D, array3D);
    }

//    protected vtkImageData createRawImage(int originalWidth, int originalHeight, float[][] array)
//    {
//        vtkImageData image = new vtkImageData();
//        image.SetScalarTypeToFloat();
//        image.SetDimensions(originalHeight, originalWidth, 1);
//        image.SetSpacing(1.0, 1.0, 1.0);
//        image.SetOrigin(0.0, 0.0, 0.0);
//        image.SetNumberOfScalarComponents(1);
//
//        float maxValue = -Float.MAX_VALUE;
//        float minValue = Float.MAX_VALUE;
//        for (int i=0; i<originalHeight; ++i)
//            for (int j=0; j<originalWidth; ++j)
//            {
//                image.SetScalarComponentFromDouble(i, originalWidth-1-j, 0, 0, array[i][j]);
//
//                if (array[i][j] > maxValue)
//                    maxValue = array[i][j];
//                if (array[i][j] < minValue)
//                    minValue = array[i][j];
//            }
//
//        setMaxValue(maxValue);
//        setMinValue(minValue);
//
//        return image;
//    }

}
