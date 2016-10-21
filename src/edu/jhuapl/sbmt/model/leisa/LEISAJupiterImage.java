package edu.jhuapl.sbmt.model.leisa;

import java.io.File;
import java.io.IOException;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import vtk.vtkImageData;

import edu.jhuapl.saavtk.model.FileType;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;
import edu.jhuapl.sbmt.util.ImageDataUtil;

public class LEISAJupiterImage extends PerspectiveImage
{
    public static final int INITIAL_BAND = 127;

    private double[][] spectrumWavelengths;
    private double[][] spectrumBandwidths;
    private double[][] spectrumValues;
    private double[][] spectrumRegion;

    public ImageKey getKey()
    {
        ImageKey key = super.getKey();
        key.slice = getCurrentSlice();
        key.band = getCurrentBand();
        return key;
    }

    public LEISAJupiterImage(ImageKey key, SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException,
            IOException
    {
        super(key, smallBodyModel, loadPointingOnly, INITIAL_BAND);
    }

    protected void initialize() throws FitsException, IOException
    {
        // initialize the spectrum wavelengths
        // note that spectrum segment 1 is 200 bands while spectrum segment 2 is 56 bands
        spectrumWavelengths = new double[2][];
        spectrumWavelengths[0] = new double[200];
        spectrumWavelengths[1] = new double[56];

        // initialize the spectrum wavelength bandwidths
        spectrumBandwidths = new double[2][];
        spectrumBandwidths[0] = new double[200];
        spectrumBandwidths[1] = new double[56];

        // initialize the spectrum values
        spectrumValues = new double[2][];
        spectrumValues[0] = new double[200];
        spectrumValues[1] = new double[56];

        super.initialize();

        // calclulate image dimensions after the image has been loaded
        double centerI = (this.getImageHeight() - 1) / 2.0;
        double centerJ = (this.getImageWidth() - 1) / 2.0;
        double[][] region = { { centerI, centerJ } };

        setUseDefaultFootprint(true);

        this.setSpectrumRegion(region);
    }

    @Override
    protected void processRawImage(vtkImageData rawImage)
    {
        // Flip image along y axis and y axis. For some reason we need to do
        // this so the image is displayed properly.
//        ImageDataUtil.flipImageYAxis(rawImage);
        ImageDataUtil.flipImageXAxis(rawImage);
    }

    public int getDefaultSlice() { return INITIAL_BAND; }

    public boolean shiftBands() { return true; }

    protected int getNumberBands()
    {
        return 256;
    }

    protected int loadNumSlices()
    {
        return getNumberBands();
    }

    public double[] getPixelDirection(double sample, double line)
    {
        return getPixelDirection((double)sample, (double)line, 127);
    }

    public int getNumberOfSpectralSegments() { return 2; }

    public double[] getSpectrumWavelengths(int segment) { return spectrumWavelengths[segment]; }

    public double[] getSpectrumBandwidths(int segment) { return spectrumBandwidths[segment]; }

   public double[] getSpectrumValues(int segment) { return spectrumValues[segment]; }

    public String getSpectrumWavelengthUnits() { return "micrometers"; }

    public String getSpectrumValueUnits() { return "erg/s/cm^2/A/sr x 10^12"; }


    protected void loadImageCalibrationData(Fits f) throws FitsException, IOException
    {
        // load in calibration info
            BasicHDU wl = f.getHDU(1);

            int wlAxes[] = wl.getAxes();
            int wlNAxes = wlAxes.length;
            int wlHeight = wlAxes[0];
            int wlWidth = wlAxes[1];
            int wlDepth = 2;

            Object wldata = wl.getData().getData();
            float[][][] wlarray = null;

            // for 3D arrays we consider the second axis the "spectral" axis
            if (wldata instanceof float[][][])
            {
                wlarray = (float[][][])wldata;

//              System.out.println("Wavelength Center Image Detected: " + wlarray.length + "x" + wlarray[0].length + "x" + wlarray[0][0].length);
              for (int i=0; i<256; i++)
              {
                  double averageWavelength = 0.0;
                  double averageWavewidth = 0.0;
                  for (int j=0; j<256; j++)
                  {
                      averageWavelength += wlarray[0][i][j];
                      averageWavewidth += wlarray[1][i][j];
                  }
                  averageWavelength /= 256.0;
                  averageWavewidth /= 256.0;

                  // place wavelengths in either spectrum segment 1 or 2 depending on band
                  if (i < 200)
                  {
                      spectrumWavelengths[0][i] = averageWavelength;
                      spectrumBandwidths[0][i] = averageWavewidth;
                  }
                  else
                  {
                      spectrumWavelengths[1][i-200] = averageWavelength;
                      spectrumBandwidths[1][i-200] = averageWavewidth;
                  }

//                  System.out.println("Band " + i  + ": " + averageWavelength + ", " + averageWavewidth);
              }
            }
    }


    private double clamp(double value, double min, double max)
    {
        if (value < min)
            return min;
        else if (value > max)
            return max;
        else
            return value;
    }

    @Override
    public void setSpectrumRegion(double[][] spectrumRegion)
    {
//        System.out.println("Setting spectrum region: " + spectrumRegion[0][0] + ", " + spectrumRegion[0][1]);
        this.spectrumRegion = spectrumRegion;

        // calculate the spectrum values
        vtkImageData im = this.getRawImage();

        if (im != null)
        {
            int x = (int)Math.round(spectrumRegion[0][0]);
            int y = (int)Math.round(spectrumRegion[0][1]);
            float[] pixelColumn = ImageDataUtil.vtkImageDataToArray1D(im, x, y);

            // there are occasional garbage values (extremely large or negative) so I'm clamping these for now. -turnerj1
            for (int i=0; i<200; i++)
            {
                spectrumValues[0][i] = clamp(1.0e-12 * (double)pixelColumn[i], 0.0, 1000.0);
            }

            for (int i=200; i<256; i++)
            {
                spectrumValues[1][i-200] = clamp(1.0e-12 * (double)pixelColumn[i], 0.0, 1000.0);
            }

            this.pcs.firePropertyChange(Properties.SPECTRUM_REGION_CHANGED, null, null);
        }
    }

    @Override
    public double[][] getSpectrumRegion()
    {
        return this.spectrumRegion;
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

    protected double getFocalLength() { return 657.5; }    // in mm

    protected double getPixelWidth() { return 0.013; }    // in mm

    protected double getPixelHeight() { return 0.013; }   // in mm

    @Override
    protected String initializeLabelFileFullPath()
    {
        return null;
//        ImageKey key = getKey();
//        File keyFile = new File(key.name);
//        String sumFilename = keyFile.getParentFile().getParent() + "/labelfiles/"
//        + keyFile.getName().split("\\.")[0] + ".lbl";
//        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        ImageKey key = getKey();

        // if the file type is SUM, then return a null
        if (key.fileType != null && key.fileType == FileType.SUM)
            return null;

        File keyFile = new File(key.name);
        String sumFilename = keyFile.getParentFile().getParent() + "/infofiles/"
        + keyFile.getName() + ".INFO";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    protected String initializeSumfileFullPath()
    {
        ImageKey key = getKey();
        // if the file type is not SUM, then return null
        if (key.fileType == null || key.fileType != FileType.SUM)
            return null;

        File keyFile = new File(key.name);
        String sumFilename = keyFile.getParentFile().getParent() + "/sumfiles/"
        + keyFile.getName().split("\\.")[0] + ".SUM";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    protected vtkImageData createRawImage(int height, int width, int depth, float[][] array2D, float[][][] array3D)
    {
        return createRawImage(height, width, depth, false, array2D, array3D);
    }

    @Override
    public float[] getRawPixelValue(int p0, int p1)
    {
        if(getRawImage() == null)
        {
            return null;
        }
        else
        {
            float[] pixelColumn = ImageDataUtil.vtkImageDataToArray1D(getRawImage(), getImageHeight()-1-p0, p1);
            return new float[] {pixelColumn[getCurrentSlice()]};
        }
    }
}
