package edu.jhuapl.sbmt.model.vesta;

import java.io.File;
import java.io.IOException;

import vtk.vtkImageData;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.ImageKeyInterface;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;

import nom.tam.fits.FitsException;

public class FcImage extends PerspectiveImage
{
    public FcImage(ImageKeyInterface key,
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
        ImageKeyInterface key = getKey();
        String cameraId = new File(key.getName()).getName().substring(2, 3);
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
        ImageKeyInterface key = getKey();
        return FileCache.getFileFromServer(key.getName() + ".FIT").getAbsolutePath();
    }

    @Override
    protected String initializeLabelFileFullPath()
    {
        return null;
    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        ImageKeyInterface key = getKey();
        File keyFile = new File(key.getName());
        String sumFilename = keyFile.getParentFile().getParent() + File.separator + "infofiles" + File.separator
        + keyFile.getName() + ".INFO";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    protected String initializeSumfileFullPath()
    {
        ImageKeyInterface key = getKey();
        File keyFile = new File(key.getName());
        String sumFilename = keyFile.getParentFile().getParent() + File.separator + "sumfiles" + File.separator
        + keyFile.getName().substring(0, 12).replace('B', 'A') + ".SUM";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    public int getFilter()
    {
        ImageKeyInterface key = getKey();
        String filterId = new File(key.getName()).getName().substring(25, 26);
        return Integer.parseInt(filterId);
    }

    @Override
    public String getCameraName()
    {
        return "FC" + getCamera();
    }
}
