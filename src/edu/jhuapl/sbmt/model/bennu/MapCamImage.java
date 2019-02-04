package edu.jhuapl.sbmt.model.bennu;

import java.io.File;
import java.io.IOException;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.ImageKeyInterface;
import edu.jhuapl.sbmt.model.image.ImageSource;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;

import nom.tam.fits.FitsException;

public class MapCamImage extends PerspectiveImage
{
    public MapCamImage(ImageKeyInterface key,
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
        return FileCache.getFileFromServer(key.getName() + ".fit").getAbsolutePath();
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
        ImageKeyInterface key = getKey();
        String result = null;

        // if the source is GASKELL, then return a null
        if (key.getSource() == null || key.getSource() != null && key.getSource() == ImageSource.GASKELL)
            result = null;
        else
        {
            File keyFile = new File(key.getName());
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
        ImageKeyInterface key = getKey();
        String result = null;

        // if the source is SPICE, then return a null
        if (key.getSource() == null || key.getSource() != null && key.getSource() == ImageSource.SPICE)
            result = null;
        else
        {
            File keyFile = new File(key.getName());
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
