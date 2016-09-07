package edu.jhuapl.sbmt.model.bennu;

import java.io.File;
import java.io.IOException;

import nom.tam.fits.FitsException;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.ImageSource;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;

public class MapCamImage extends PerspectiveImage
{
    public MapCamImage(ImageKey key,
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
        ImageKey key = getKey();
        return FileCache.getFileFromServer(key.name + ".fit").getAbsolutePath();
    }

    @Override
    protected String initializeLabelFileFullPath()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        ImageKey key = getKey();
        String result = null;

        // if the source is GASKELL, then return a null
        if (key.source == null || key.source != null && key.source == ImageSource.GASKELL)
            result = null;
        else
        {
            File keyFile = new File(key.name);
            String infodir = "infofiles";
            String pointingFileName = keyFile.getParentFile().getParent() + File.separator + infodir + File.separator + keyFile.getName() + ".INFO";

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
        if (key.source == null || key.source != null && key.source == ImageSource.SPICE)
            result = null;
        else
        {
            File keyFile = new File(key.name);
            String sumdir = "sumfiles";
            String sumFilename = keyFile.getParentFile().getParent() + File.separator + sumdir + File.separator + keyFile.getName() + ".SUM";

            try {
                result = FileCache.getFileFromServer(sumFilename).getAbsolutePath();
            } catch (Exception e) {
                result = null;
            }
        }

        return result;
    }
}
