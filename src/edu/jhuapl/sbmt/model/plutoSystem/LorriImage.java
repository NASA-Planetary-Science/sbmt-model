package edu.jhuapl.sbmt.model.plutoSystem;

import java.io.File;
import java.io.IOException;

import vtk.vtkImageData;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.core.pointing.PointingSource;
import edu.jhuapl.sbmt.image.interfaces.ImageKeyInterface;
import edu.jhuapl.sbmt.image.old.BasicPerspectiveImage;

import nom.tam.fits.FitsException;

public class LorriImage extends BasicPerspectiveImage
{
    public LorriImage(ImageKeyInterface key,
            SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }

    @Override
    protected void processRawImage(vtkImageData rawImage)
    {
        // Flip image along y axis. For some reason we need to do
        // this so the image is displayed properly, but only for
        // SPICE pointings, which are most likely not correct.
        PointingSource source = getKey().getSource();
        if (source == PointingSource.SPICE)
        {
            ImageDataUtil.flipImageYAxis(rawImage);
            ImageDataUtil.rotateImage(rawImage, 180.0);
        }

        super.processRawImage(rawImage);
    }

    @Override
    protected String getImageFileName(String imageName)
    {
        // If the proposed name does not include the extension, add .fits.
        if (!imageName.matches("^.*\\.[^\\\\.]*$"))
        {
            imageName += ".fit";
        }

        return imageName;
    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        ImageKeyInterface key = getKey();
        String result = null;

        // if the source is GASKELL, then return a null
        if (key.getSource() == null || key.getSource() != null && (key.getSource() == PointingSource.GASKELL || key.getSource() == PointingSource.CORRECTED))
            result = null;
        else
        {
            File keyFile = new File(key.getName());
            String infodir = key.getSource() == PointingSource.CORRECTED_SPICE ? "infofiles-corrected" : "infofiles";
            String pointingFileName = keyFile.getParentFile().getParent() + "/" + infodir + "/" + keyFile.getName() + ".INFO";

            try {
                result = FileCache.getFileFromServer(pointingFileName).getAbsolutePath();
            } catch (Exception e) {
                result = null;
            }
        }

        return result;
    }

}
