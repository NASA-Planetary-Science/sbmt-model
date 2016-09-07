package edu.jhuapl.sbmt.model.itokawa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import vtk.vtkImageConstantPad;
import vtk.vtkImageData;
import vtk.vtkImageMathematics;
import vtk.vtkImageReslice;
import vtk.vtkImageTranslateExtent;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.VtkDataTypes;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.ImageSource;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

public class AmicaImage extends PerspectiveImage
{
    //private static final int IMAGE_WIDTH = 1024;
    //private static final int IMAGE_HEIGHT = 1024;

    public AmicaImage(ImageKey key,
            SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }

    /**
     * Apply a flat field correction to the image. In the AMICA dataset, a flat field
     * file is provided for each of the filters. This AMICA image needs to be divided
     * by the flat field image to get the final better looking image. This function
     * downloads the appropriate flat field image and then divides the raw image by
     * the flat field and overwrites the raw image with this new ratio image.
     *
     * @param rawImage
     */
    private void doFlatFieldCorrection(vtkImageData rawImage)
    {
        // Download appropriate flat field depending on filter of current image
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        String imagesDir = keyFile.getParentFile().getPath();
        String flatFileName = "flat_" + getFilterName() + ".fit";
        String flatfile = FileCache.getFileFromServer(imagesDir + File.separator + flatFileName).getAbsolutePath();

        try
        {
            // Load the flat field file into a vtkImageData
            Fits f = new Fits(flatfile);
            BasicHDU h = f.getHDU(0);

            float[][] array = null;
            int[] axes = h.getAxes();
            int originalWidth = axes[1];
            int originalHeight = axes[0];

            Object data = h.getData().getData();
            if (data instanceof float[][])
            {
                array = (float[][])data;
            }
            f.getStream().close();

            vtkImageData flatImage = new vtkImageData();
            flatImage.SetDimensions(originalWidth, originalHeight, 1);
            flatImage.SetSpacing(1.0, 1.0, 1.0);
            flatImage.SetOrigin(0.0, 0.0, 0.0);
            flatImage.AllocateScalars(VtkDataTypes.VTK_FLOAT, 1);

            for (int i=0; i<originalHeight; ++i)
                for (int j=0; j<originalWidth; ++j)
                {
                    flatImage.SetScalarComponentFromDouble(j, originalHeight-1-i, 0, 0, array[i][j]);
                }

            // Finally construct the ratio image
            vtkImageMathematics divideFilter = new vtkImageMathematics();
            divideFilter.SetInput1Data(rawImage);
            divideFilter.SetInput2Data(flatImage);
            divideFilter.SetOperationToDivide();
            divideFilter.Update();

            vtkImageData divideOutput = divideFilter.GetOutput();
            rawImage.DeepCopy(divideOutput);
            divideFilter.Delete();
        }
        catch (FitsException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void processRawImage(vtkImageData rawImage)
    {
        int[] dims = rawImage.GetDimensions();
        if (dims[0] == 1024 && dims[1] == 1024)
        {
            doFlatFieldCorrection(rawImage);
            return;
        }

        // Some of the AMICA images were cropped from their original size
        // of 1024x1024. Therefore, the following pads the images with zero back to
        // original size. The vtkImageTranslateExtent first translates the cropped image
        // to its proper position in the original and the vtkImageConstantPad then pads
        // it with zero to size 1024x1024. For images that were binned need to resample
        // to 1024x1024.
        String filename = getFitFileFullPath();

        int binning = 1;
        //int topMask = 0;
        //int rightMask = 0;
        int bottomMask = 0;
        int leftMask = 0;

        try
        {
            Fits f = new Fits(filename);
            BasicHDU h = f.getHDU(0);

            binning = h.getHeader().getIntValue("BINNING");
            int startH = h.getHeader().getIntValue("START_H");
            //int startV = h.getHeader().getIntValue("START_V");
            //int lastH  = h.getHeader().getIntValue("LAST_H");
            int lastV  = h.getHeader().getIntValue("LAST_V");

            if (binning == 1)
            {
                //topMask = startV;
                //rightMask = 1023-lastH;
                bottomMask = 1023-lastV;
                leftMask = startH;
            }
            else if (binning == 2)
            {
                startH /= 2;
                //startV /= 2;
                //lastH = ((lastH + 1) / 2) - 1;
                lastV = ((lastV + 1) / 2) - 1;

                //topMask = startV;
                //rightMask = 511-lastH;
                bottomMask = 511-lastV;
                leftMask = startH;
            }
            else
            {
                // do nothing as images with binning higher than 2 have no masks
            }

            f.getStream().close();
        }
        catch (FitsException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        vtkImageTranslateExtent translateExtent = new vtkImageTranslateExtent();
        translateExtent.SetInputData(rawImage);
        translateExtent.SetTranslation(leftMask, bottomMask, 0);
        translateExtent.Update();

        vtkImageConstantPad pad = new vtkImageConstantPad();
        pad.SetInputConnection(translateExtent.GetOutputPort());
        if (binning == 1)
            pad.SetOutputWholeExtent(0, 1023, 0, 1023, 0, 0);
        else if (binning == 2)
            pad.SetOutputWholeExtent(0, 511, 0, 511, 0, 0);
        else if (binning == 4)
            pad.SetOutputWholeExtent(0, 255, 0, 255, 0, 0);
        else if (binning == 8)
            pad.SetOutputWholeExtent(0, 127, 0, 127, 0, 0);
        pad.Update();

        vtkImageData padOutput = pad.GetOutput();
        rawImage.DeepCopy(padOutput);

        // shift origin back to zero
        rawImage.SetOrigin(0.0, 0.0, 0.0);

        // Rescale image to 1024 by 1024 for binned images
        if (binning > 1)
        {
            if (binning == 2)
                rawImage.SetSpacing(2.0, 2.0, 1.0);
            else if (binning == 4)
                rawImage.SetSpacing(4.0, 4.0, 1.0);
            else if (binning == 8)
                rawImage.SetSpacing(8.0, 8.0, 1.0);

            vtkImageReslice resample = new vtkImageReslice();
            resample.SetInputData(rawImage);
            resample.InterpolateOff();
            resample.SetOutputExtent(0, 1023, 0, 1023, 0, 0);
            resample.SetOutputOrigin(0.0, 0.0, 0.0);
            resample.SetOutputSpacing(1.0, 1.0, 1.0);
            resample.Update();
            vtkImageData resampleOutput = resample.GetOutput();
            rawImage.DeepCopy(resampleOutput);
        }

        doFlatFieldCorrection(rawImage);
    }

    @Override
    public void generateBackplanesLabel(File imgName, File lblFileName) throws IOException
    {
        String lblFilename = getLabelFileFullPath();

        FileInputStream fs = new FileInputStream(lblFilename);
        InputStreamReader isr = new InputStreamReader(fs);
        BufferedReader in = new BufferedReader(isr);

        StringBuffer strbuf = new StringBuffer("");

        int[] croppedSize = getCroppedSize();

        int numBands = 16;
        String productName = null;

        // The following flag is used to ensure that two or more
        // consecutive blank lines are never output.
        boolean lastLineWasEmpty = false;

        String str;
        while ((str = in.readLine()) != null)
        {
            // The software name and version in the downloaded ddr is not correct
            if (str.startsWith("RECORD_TYPE") ||
                    str.startsWith("RECORD_BYTES") ||
                    str.startsWith("FILE_RECORDS") ||
                    str.startsWith("^HEADER") ||
                    str.startsWith("^IMAGE"))
            {
                continue;
            }

            if (str.startsWith("DATA_SET_ID"))
            {
                appendWithPadding(strbuf, "DATA_SET_ID                        = \"HAY-A-AMICA-3-AMICAGEOM-V1.0\"");
                continue;
            }

            if (str.startsWith("PRODUCT_NAME"))
            {
                String[] tokens = str.split("=");
                productName = tokens[1].trim().replaceAll("\"","");
                productName = productName.substring(0, productName.length()-4) + "_DDR.IMG";
                appendWithPadding(strbuf, "PRODUCT_NAME                       = \"" + productName + "\"");
                continue;
            }

            if (str.startsWith("PRODUCT_ID"))
            {
                String[] tokens = str.split("=");
                String name = tokens[1].trim().replaceAll("\"","");
                name = name.substring(0, name.length()-4) + "_DDR_IMG";
                appendWithPadding(strbuf, "PRODUCT_ID                         = \"" + name + "\"");
                continue;
            }

            if (str.startsWith("PRODUCT_CREATION_TIME"))
            {
                appendWithPadding(strbuf, "PRODUCT_TYPE                       = DDR");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date();
                String dateStr = sdf.format(date).replace(' ', 'T');
                appendWithPadding(strbuf, "PRODUCT_CREATION_TIME              = " + dateStr);
                continue;
            }

            if (str.startsWith("OBJECT"))
            {
                appendWithPadding(strbuf, "");
                appendWithPadding(strbuf, "/* This DDR label describes one data file:                               */");
                appendWithPadding(strbuf, "/* 1. A multiple-band backplane image file with wavelength-independent,  */");
                appendWithPadding(strbuf, "/* spatial pixel-dependent geometric and timing information.             */");
                appendWithPadding(strbuf, "");
                appendWithPadding(strbuf, "OBJECT                       = FILE");

                appendWithPadding(strbuf, "  ^IMAGE                     = \"" + imgName.getName() + "\"");

                appendWithPadding(strbuf, "  RECORD_TYPE                = FIXED_LENGTH");
                appendWithPadding(strbuf, "  RECORD_BYTES               = " + (croppedSize[1] * 4));
                appendWithPadding(strbuf, "  FILE_RECORDS               = " + (croppedSize[0] * numBands));
                appendWithPadding(strbuf, "");

                appendWithPadding(strbuf, "  OBJECT                     = IMAGE");
                appendWithPadding(strbuf, "    LINES                    = " + croppedSize[1]);
                appendWithPadding(strbuf, "    LINE_SAMPLES             = " + croppedSize[0]);
                appendWithPadding(strbuf, "    SAMPLE_TYPE              = IEEE_REAL");
                appendWithPadding(strbuf, "    SAMPLE_BITS              = 32");
                appendWithPadding(strbuf, "    CORE_NULL                = 16#F49DC5AE#"); // bit pattern of -1.0e32 in hex

                appendWithPadding(strbuf, "    BANDS                    = " + numBands);
                appendWithPadding(strbuf, "    BAND_STORAGE_TYPE        = BAND_SEQUENTIAL");
                appendWithPadding(strbuf, "    BAND_NAME                = (\"Pixel value\",");
                appendWithPadding(strbuf, "                                \"x coordinate of center of pixel, km\",");
                appendWithPadding(strbuf, "                                \"y coordinate of center of pixel, km\",");
                appendWithPadding(strbuf, "                                \"z coordinate of center of pixel, km\",");
                appendWithPadding(strbuf, "                                \"Latitude, deg\",");
                appendWithPadding(strbuf, "                                \"Longitude, deg\",");
                appendWithPadding(strbuf, "                                \"Distance from center of body, km\",");
                appendWithPadding(strbuf, "                                \"Incidence angle, deg\",");
                appendWithPadding(strbuf, "                                \"Emission angle, deg\",");
                appendWithPadding(strbuf, "                                \"Phase angle, deg\",");
                appendWithPadding(strbuf, "                                \"Horizontal pixel scale, km per pixel\",");
                appendWithPadding(strbuf, "                                \"Vertical pixel scale, km per pixel\",");
                appendWithPadding(strbuf, "                                \"Slope, deg\",");
                appendWithPadding(strbuf, "                                \"Elevation, m\",");
                appendWithPadding(strbuf, "                                \"Gravitational acceleration, m/s^2\",");
                appendWithPadding(strbuf, "                                \"Gravitational potential, J/kg\")");
                appendWithPadding(strbuf, "");
                appendWithPadding(strbuf, "  END_OBJECT                 = IMAGE");
                appendWithPadding(strbuf, "END_OBJECT                   = FILE");

                appendWithPadding(strbuf, "");
                appendWithPadding(strbuf, "END");

                break;
            }

            if (!lastLineWasEmpty || !str.trim().isEmpty())
                appendWithPadding(strbuf, str);

            lastLineWasEmpty = str.trim().isEmpty();
        }

        in.close();

//        return strbuf.toString();
        byte[] bytes = strbuf.toString().getBytes();
        OutputStream out = new FileOutputStream(lblFileName);
        out.write(bytes, 0, bytes.length);
        out.close();
    }

    public float[] cropBackplanes(float[] backplanes)
    {

        // The backplane should not include the extra parts added to the image so that
        // it has a size of 1024x1024. Therefore, create a new backplane array which only
        // includes the original image. Note that we still resample the image in cases where
        // the binning is greater than 1.

        int[] croppedSize = getCroppedSize();

        if (croppedSize[0] == 1024 && croppedSize[1] == 1024)
            return backplanes;

        float[] newBackplanes = new float[16 * croppedSize[0] * croppedSize[1]];

        int[] mask = getMaskSizes();
        int topMask =    mask[0];
        int rightMask =  mask[1];
        int bottomMask = mask[2];
        int leftMask =   mask[3];

        for (int i=0; i<1024; ++i)
            for (int j=0; j<1024; ++j)
            {
                if (i >= bottomMask && i <= 1024-1-topMask &&
                        j >= leftMask && j <= 1024-1-rightMask)
                {
                    for (int k = 0; k < 16; ++k)
                    {
                        int idx = k*croppedSize[0]*croppedSize[1] + (i-bottomMask)*croppedSize[0] + (j-leftMask);
                        newBackplanes[idx]  = backplanes[index(j,i,k)];
                    }
                }
            }

        return newBackplanes;
    }

    @Override
    public float[] generateBackplanes()
    {
        float[] backplanes = super.generateBackplanes();

        return cropBackplanes(backplanes);
    }

    /**
     * Get the size of the image without the mask added to it to make it 1024x1024
     * @return height in first element and width in second
     */
    public int[] getCroppedSize()
    {
        int[] mask = getMaskSizes();

        return new int[] {
                1024 - mask[1] - mask[3],
                1024 - mask[0] - mask[2]
        };
    }

    @Override
    protected int[] getMaskSizes()
    {
        String filename = getFitFileFullPath();

        try
        {
            Fits f = new Fits(filename);
            BasicHDU h = f.getHDU(0);

            int startH = h.getHeader().getIntValue("START_H");
            int startV = h.getHeader().getIntValue("START_V");
            int lastH  = h.getHeader().getIntValue("LAST_H");
            int lastV  = h.getHeader().getIntValue("LAST_V");

            f.getStream().close();

            return new int[]{startV, 1023-lastH, 1023-lastV, startH};
        }
        catch (FitsException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // Should never reach here
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
        ImageKey key = getKey();
        String imgLblFilename = key.name + ".lbl";
        return FileCache.getFileFromServer(imgLblFilename).getAbsolutePath();
    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        String sumFilename = keyFile.getParentFile().getParent()
        + "/infofiles/" + keyFile.getName() + ".INFO";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    protected String initializeSumfileFullPath()
    {
        ImageKey key = getKey();
        String sumfilesdir = "sumfiles";
        if (key.source == ImageSource.CORRECTED)
            sumfilesdir += "-corrected";
        File keyFile = new File(key.name);
        String sumFilename = keyFile.getParentFile().getParent()
        + "/" + sumfilesdir + "/N" + keyFile.getName().substring(3, 13) + ".SUM";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    public int getFilter()
    {
        String fitName = getFitFileFullPath();

        int ind1 = fitName.lastIndexOf('_');
        int ind2 = fitName.lastIndexOf('.');

        String filterName = fitName.substring(ind1+1, ind2);

        return getFilterNumberFromName(filterName);
    }

    @Override
    public String getFilterName()
    {
        return getFilterNameFromNumber(getFilter());
    }

    private int getFilterNumberFromName(String name)
    {
        int num = -1;
        if (name.equals("ul"))
            num = 1;
        else if (name.equals("b"))
            num = 2;
        else if (name.equals("v"))
            num = 3;
        else if (name.equals("w"))
            num = 4;
        else if (name.equals("x"))
            num = 5;
        else if (name.equals("p"))
            num = 6;
        else if (name.equals("zs"))
            num = 7;

        return num;
    }

    private String getFilterNameFromNumber(int num)
    {
        String name = "";
        if (num == 1)
            name = "ul";
        else if (num == 2)
            name = "b";
        else if (num == 3)
            name = "v";
        else if (num == 4)
            name = "w";
        else if (num == 5)
            name = "x";
        else if (num == 6)
            name = "p";
        else if (num == 7)
            name = "zs";

        return name;
    }
}
