package edu.jhuapl.sbmt.model.vesta;

import java.io.File;
import java.io.IOException;

import nom.tam.fits.FitsException;

import vtk.vtkImageData;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;

public class FcImage extends PerspectiveImage
{
    public FcImage(ImageKey key,
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
    }

    public int getCamera()
    {
        ImageKey key = getKey();
        String cameraId = new File(key.name).getName().substring(2, 3);
        return Integer.parseInt(cameraId);
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
        return FileCache.getFileFromServer(key.name + ".FIT").getAbsolutePath();
    }

    @Override
    protected String initializeLabelFileFullPath()
    {
        return null;
    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        String sumFilename = keyFile.getParentFile().getParent() + File.separator + "infofiles" + File.separator
        + keyFile.getName() + ".INFO";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    protected String initializeSumfileFullPath()
    {
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        String sumFilename = keyFile.getParentFile().getParent() + File.separator + "sumfiles" + File.separator
        + keyFile.getName().substring(0, 12).replace('B', 'A') + ".SUM";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    public int getFilter()
    {
        ImageKey key = getKey();
        String filterId = new File(key.name).getName().substring(25, 26);
        return Integer.parseInt(filterId);
    }

    @Override
    public String getCameraName()
    {
        return "FC" + getCamera();
    }
}
