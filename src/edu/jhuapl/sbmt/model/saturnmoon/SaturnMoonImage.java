package edu.jhuapl.sbmt.model.saturnmoon;

import java.io.File;
import java.io.IOException;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.ImageKeyInterface;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;

import nom.tam.fits.FitsException;

public class SaturnMoonImage extends PerspectiveImage
{
    public SaturnMoonImage(ImageKeyInterface key,
            SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
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
        + keyFile.getName() + ".SUM";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }
}
