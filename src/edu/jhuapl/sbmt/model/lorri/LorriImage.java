package edu.jhuapl.sbmt.model.lorri;

import java.io.File;
import java.io.IOException;

import nom.tam.fits.FitsException;

import vtk.vtkImageData;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.ImageSource;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;

public class LorriImage extends PerspectiveImage
{
    public LorriImage(ImageKey key,
            SmallBodyModel smallBodyModel,
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
        ImageDataUtil.rotateImage(rawImage, 180.0);
    }

    @Override
    protected int[] getMaskSizes()
    {
        return new int[]{0, 0, 0, 0};
    }

    @Override
    protected String initializeFitFileFullPath()
    {
        ImageKey key = getKey();
        return FileCache.getFileFromServer(key.name + ".fit").getAbsolutePath();
    }

    @Override
    protected String initializeLabelFileFullPath()
    {
        return null;
    }

//    protected String initializeInfoFileFullPath()
//    {
//        ImageKey key = getKey();
//        File keyFile = new File(key.name);
//        String pointingFileName = null;
//
//        if (key.source == ImageSource.WCS_CORRECTED)
//            pointingFileName = keyFile.getParentFile().getParent() + "/wcsinfofiles/" + keyFile.getName() + ".INFO";
//        else
//            pointingFileName = keyFile.getParentFile().getParent() + "/infofiles/" + keyFile.getName() + ".INFO";
//
//        return FileCache.getFileFromServer(pointingFileName).getAbsolutePath();
//    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        ImageKey key = getKey();
        String result = null;

        // if the source is GASKELL, then return a null
        if (key.source == null || key.source != null && (key.source == ImageSource.GASKELL || key.source == ImageSource.CORRECTED))
            result = null;
        else
        {
            File keyFile = new File(key.name);
            String infodir = key.source == ImageSource.CORRECTED_SPICE ? "infofiles-corrected" : "infofiles";
            String pointingFileName = keyFile.getParentFile().getParent() + "/" + infodir + "/" + keyFile.getName() + ".INFO";

            try {
                result = FileCache.getFileFromServer(pointingFileName).getAbsolutePath();
            } catch (Exception e) {
                result = null;
            }
        }

        return result;
    }

    @Override
    protected String initializeSumfileFullPath()
    {
        ImageKey key = getKey();
        String result = null;

        // if the source is SPICE, then return a null
        if (key.source == null || key.source != null && (key.source == ImageSource.SPICE || key.source == ImageSource.CORRECTED_SPICE))
            result = null;
        else
        {
            File keyFile = new File(key.name);
            String sumdir = key.source == ImageSource.CORRECTED ? "sumfiles-corrected" : "sumfiles";
            String sumFilename = keyFile.getParentFile().getParent() + "/" + sumdir + "/" + keyFile.getName() + ".SUM";

            try {
                result = FileCache.getFileFromServer(sumFilename).getAbsolutePath();
            } catch (Exception e) {
                result = null;
            }
        }

        return result;
    }
}
