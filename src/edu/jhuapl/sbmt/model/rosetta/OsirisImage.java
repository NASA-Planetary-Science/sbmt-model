package edu.jhuapl.sbmt.model.rosetta;

import java.io.File;
import java.io.IOException;

import nom.tam.fits.FitsException;

import vtk.vtkImageConstantPad;
import vtk.vtkImageData;
import vtk.vtkImageTranslateExtent;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.sbmt.app.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;
import edu.jhuapl.sbmt.util.ImageDataUtil;

public class OsirisImage extends PerspectiveImage
{
    public OsirisImage(ImageKey key,
            SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }

    @Override
    protected void processRawImage(vtkImageData rawImage)
    {
        // Flip image along y axis and rotate it. Only needed for NAC images.
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        if (keyFile.getName().startsWith("N"))
        {
            ImageDataUtil.flipImageYAxis(rawImage);
            ImageDataUtil.rotateImage(rawImage, 180.0);
        }

        if (key.name.contains("67P"))
        {
            return;
        }
        else // for lutetia
        {
            // If image is smaller than 2048x2048 we need to extend it to that size.
            // Therefore, the following pads the images with zero back to
            // original size. The vtkImageTranslateExtent first translates the cropped image
            // to its proper position in the original and the vtkImageConstantPad then pads
            // it with zero to size 2048x2048.
            int[] dims = rawImage.GetDimensions();
            if (dims[0] == 2048 && dims[1] == 2048)
                return;

            // Currently this correction only works with NAC images of size 1024x1024.
            // Other images don't align well with the shape model using this shift amount.
            int xshift = 559;
            int yshift = 575;

            vtkImageTranslateExtent translateExtent = new vtkImageTranslateExtent();
            translateExtent.SetInputData(rawImage);
            translateExtent.SetTranslation(xshift, yshift, 0);
            translateExtent.Update();

            vtkImageConstantPad pad = new vtkImageConstantPad();
            pad.SetInputConnection(translateExtent.GetOutputPort());
            pad.SetOutputWholeExtent(0, 2047, 0, 2047, 0, 0);
            pad.Update();

            vtkImageData padOutput = pad.GetOutput();
            rawImage.DeepCopy(padOutput);

            // shift origin back to zero
            rawImage.SetOrigin(0.0, 0.0, 0.0);
        }
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
        String sumFilename = keyFile.getParentFile().getParent() + "/infofiles/"
        + keyFile.getName() + ".INFO";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    protected String initializeSumfileFullPath()
    {
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        String sumFilename = keyFile.getParentFile().getParent() + "/sumfiles/"
        + keyFile.getName() + ".SUM";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    public int getFilter()
    {
        int filter = Integer.parseInt(getFilterName());

        switch(filter)
        {
        case 12:
            return 1;
        case 16:
            return 2;
        case 18:
            return 3;
        case 22:
            return 4;
        case 23:
            return 5;
        case 24:
            return 6;
        case 27:
            return 7;
        case 28:
            return 8;
        case 41:
            return 9;
        case 51:
            return 10;
        case 54:
            return 11;
        case 61:
            return 12; // Everything below here was added on 12/24/2015, nothing above was touched for backwards compatibility
        case 13:
            return 13;
        case 15:
            return 14;
        case 17:
            return 15;
        case 31:
            return 16;
        case 71:
            return 17;
        case 82:
            return 18;
        case 84:
            return 19;
        case 87:
            return 20;
        case 88:
            return 21; // Everything below here was added on 3/27/2016, nothing above was touched for backwards compatibility
        case 21:
            return 22;
        }

        return 0;
    }

    @Override
    public String getFilterName()
    {
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        String filename = keyFile.getName();

        return filename.substring(filename.length()-2, filename.length());
    }


    private String getCameraNameFromNumber(int num)
    {
        String name = null;
        if (num == 1)
            name = "NAC";
        else if (num == 2)
            name = "WAC";

        return name;
    }

    @Override
    public String getCameraName()
    {
        return getCameraNameFromNumber(getCamera());
    }

    @Override
    public int getCamera()
    {
        // Return the following:
        // 1 for NAC
        // 2 for WAC
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        if (keyFile.getName().startsWith("N"))
        {
            return 1;
        }
        else
        {
            return 2;
        }
    }
}
