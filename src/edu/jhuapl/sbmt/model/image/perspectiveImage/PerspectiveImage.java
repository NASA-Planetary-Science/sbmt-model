package edu.jhuapl.sbmt.model.image.perspectiveImage;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.base.Stopwatch;

import vtk.vtkFeatureEdges;
import vtk.vtkImageData;
import vtk.vtkPNGReader;
import vtk.vtkPolyData;
import vtk.vtkProp;
import vtk.vtkTexture;

import edu.jhuapl.saavtk.model.FileType;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.util.BoundingBox;
import edu.jhuapl.saavtk.util.DateTimeUtil;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.saavtk.util.IntensityRange;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.ObjUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.gui.image.model.CustomImageKeyInterface;
import edu.jhuapl.sbmt.model.image.IImagingInstrument;
import edu.jhuapl.sbmt.model.image.Image;
import edu.jhuapl.sbmt.model.image.ImageKeyInterface;
import edu.jhuapl.sbmt.model.image.ImageSource;
import edu.jhuapl.sbmt.util.BackPlanesPDS4XML;
import edu.jhuapl.sbmt.util.BackPlanesXml;
import edu.jhuapl.sbmt.util.BackPlanesXmlMeta;
import edu.jhuapl.sbmt.util.BackPlanesXmlMeta.BPMetaBuilder;
import edu.jhuapl.sbmt.util.BackplaneInfo;
import edu.jhuapl.sbmt.util.BackplanesLabel;
import edu.jhuapl.sbmt.util.VtkENVIReader;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.impl.FixedMetadata;
import crucible.crust.metadata.impl.gson.Serializers;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

/**
 * This class represents an abstract image of a spacecraft imager instrument.
 */
abstract public class PerspectiveImage extends Image implements PropertyChangeListener, BackPlanesPDS4XML, BackplanesLabel
{
    private static final SafeURLPaths SAFE_URL_PATHS = SafeURLPaths.instance();

    public static final float PDS_NA = -ImageDataUtil.FILL_CUTOFF;
    public static final String FRUSTUM1 = "FRUSTUM1";
    public static final String FRUSTUM2 = "FRUSTUM2";
    public static final String FRUSTUM3 = "FRUSTUM3";
    public static final String FRUSTUM4 = "FRUSTUM4";
    public static final String BORESIGHT_DIRECTION = "BORESIGHT_DIRECTION";
    public static final String UP_DIRECTION = "UP_DIRECTION";
    public static final String NUMBER_EXPOSURES = "NUMBER_EXPOSURES";
    public static final String START_TIME = "START_TIME";
    public static final String STOP_TIME = "STOP_TIME";
    public static final String SPACECRAFT_POSITION = "SPACECRAFT_POSITION";
    public static final String SUN_POSITION_LT = "SUN_POSITION_LT";
    public static final String DISPLAY_RANGE = "DISPLAY_RANGE";
    public static final String OFFLIMB_DISPLAY_RANGE = "OFFLIMB_DISPLAY_RANGE";
    public static final String TARGET_PIXEL_COORD = "TARGET_PIXEL_COORD";
    public static final String TARGET_ROTATION = "TARGET_ROTATION";
    public static final String TARGET_ZOOM_FACTOR = "TARGET_ZOOM_FACTOR";
    public static final String APPLY_ADJUSTMENTS = "APPLY_ADJUSTMENTS";

    public static final String SUMFILENAMES = "SumfileNames";
    public static final String INFOFILENAMES = "InfofileNames";

    public static final double[] bodyOrigin = { 0.0, 0.0, 0.0 };

    ///////////////////////
    // Pointing Properties
    ///////////////////////
    private String infoFileFullPath;
    private String sumFileFullPath;

    private double[][] spacecraftPositionOriginal = new double[1][3];
    private double[][] frustum1Original = new double[1][3];
    private double[][] frustum2Original = new double[1][3];
    private double[][] frustum3Original = new double[1][3];
    private double[][] frustum4Original = new double[1][3];
    private double[][] boresightDirectionOriginal = new double[1][3];
    private double[][] upVectorOriginal = new double[1][3];
    private double[][] sunPositionOriginal = new double[1][3];

    //////////////////////
    // Other properties
    //////////////////////
    private String imageName;
    protected int currentSlice = 0;
    private SmallBodyModel smallBodyModel;
    private String startTime = "";
    private String stopTime = "";
    protected int imageWidth;
    protected int imageHeight;
    private int imageDepth = 1;
    int numBackplanes = BackplaneInfo.values().length;

    private ModelManager modelManager;
    private String pngFileFullPath; // The actual path of the PNG image stored on the local disk (after downloading
                                    // from the server)
    private String fitFileFullPath; // The actual path of the FITS image stored on the local disk (after downloading
                                    // from the server)
    private String enviFileFullPath; // The actual path of the ENVI binary stored on the local disk (after
                                     // downloading from the server)
    private String labelFileFullPath;

    protected int fitFileImageExtension = 0; // Default is to use the primary FITS image.

    float[] minValue = new float[1];
    float[] maxValue = new float[1];
    private boolean loadPointingOnly;
    Stopwatch sw;

    PerspectiveImageBackplanesHelper backplanesHelper;
    PerspectiveImageOffsetCalculator imageOffsetCalculator;
    PerspectiveImageOfflimbPlaneHelper offlimbPlaneHelper;
    PerspectiveImageRendererHelper rendererHelper;


    public PerspectiveImage( //
            ImageKeyInterface key, //
            SmallBodyModel smallBodyModel, //
            boolean loadPointingOnly) throws FitsException, IOException //
    {
        this(key, smallBodyModel, null, loadPointingOnly, 0);
    }

    public PerspectiveImage( //
            ImageKeyInterface key, //
            SmallBodyModel smallBodyModel, //
            boolean loadPointingOnly, //
            int currentSlice) throws FitsException, IOException //
    {
        this(key, smallBodyModel, null, loadPointingOnly, currentSlice);
    }

    /**
     * If loadPointingOnly is true then only pointing information about this image
     * will be downloaded/loaded. The image itself will not be loaded. Used by
     * ImageBoundary to get pointing info.
     */
    public PerspectiveImage( //
            ImageKeyInterface key, //
            SmallBodyModel smallBodyModel, //
            ModelManager modelManager, //
            boolean loadPointingOnly) throws FitsException, IOException //
    {
        this(key, smallBodyModel, modelManager, loadPointingOnly, 0);
    }

  /**
     * If loadPointingOnly is true then only pointing information about this image
     * will be downloaded/loaded. The image itself will not be loaded. Used by
     * ImageBoundary to get pointing info.
     */
    protected PerspectiveImage( //
            ImageKeyInterface key, //
            SmallBodyModel smallBodyModel, //
            ModelManager modelManager, //
            boolean loadPointingOnly, //
            int currentSlice) throws FitsException, IOException //
    {
        super(key);
        this.currentSlice = currentSlice;
        this.smallBodyModel = smallBodyModel;
        this.modelManager = modelManager;
        this.loadPointingOnly = loadPointingOnly;

        this.backplanesHelper = new PerspectiveImageBackplanesHelper(this);
        this.imageOffsetCalculator = new PerspectiveImageOffsetCalculator(this);
        this.offlimbPlaneHelper = new PerspectiveImageOfflimbPlaneHelper(this);
        this.rendererHelper = new PerspectiveImageRendererHelper(this);

        initialize();
    }

    protected void initialize() throws FitsException, IOException
    {
        sw = Stopwatch.createUnstarted();
        sw.start();
//        footprint[0] = new vtkPolyData();
//        shiftedFootprint[0] = new vtkPolyData();

        if (key.getSource().equals(ImageSource.LOCAL_PERSPECTIVE))
        {
            loadImageInfoFromConfigFile();
        }

        if (!loadPointingOnly)
        {
            if (key.getSource().equals(ImageSource.LOCAL_PERSPECTIVE))
            {
                fitFileFullPath = initLocalFitFileFullPath();
                pngFileFullPath = initLocalPngFileFullPath();
                enviFileFullPath = initLocalEnviFileFullPath();
            }
            else
            {
                fitFileFullPath = initializeFitFileFullPath();
                pngFileFullPath = initializePngFileFullPath();
                enviFileFullPath = initializeEnviFileFullPath();
            }
        }

        if (key.getSource().equals(ImageSource.LOCAL_PERSPECTIVE))
        {
            infoFileFullPath = initLocalInfoFileFullPath();
            sumFileFullPath = initLocalSumfileFullPath();
        }
        else if (key.getSource().equals(ImageSource.SPICE) || key.getSource().equals(ImageSource.CORRECTED_SPICE))
        {
            infoFileFullPath = initializeInfoFileFullPath();
        }
        else if (key.getSource().equals(ImageSource.LABEL))
            setLabelFileFullPath(initializeLabelFileFullPath());
        else
            sumFileFullPath = initializeSumfileFullPath();
        imageDepth = loadNumSlices();
        if (imageDepth > 1)
            initSpacecraftStateVariables();
        if ((sumFileFullPath != null) && sumFileFullPath.endsWith("null"))
            sumFileFullPath = null;
        if ((infoFileFullPath != null) && infoFileFullPath.endsWith("null"))
            infoFileFullPath = null;
        loadPointing();

        if (!loadPointingOnly)
        {
            loadImage();
            imageOffsetCalculator.updateFrameAdjustments();
        }

        rendererHelper.initialize();
//        rendererHelper.maxFrustumDepth = new double[imageDepth];
//        rendererHelper.minFrustumDepth = new double[imageDepth];
    }

    private void initSpacecraftStateVariables()
    {
        int nslices = getImageDepth();
        spacecraftPositionOriginal = new double[nslices][3];
        frustum1Original = new double[nslices][3];
        frustum2Original = new double[nslices][3];
        frustum3Original = new double[nslices][3];
        frustum4Original = new double[nslices][3];
        sunPositionOriginal = new double[nslices][3];
        boresightDirectionOriginal = new double[nslices][3];
        upVectorOriginal = new double[nslices][3];
        rendererHelper.getFootprint().initSpacecraftStateVariables();
        rendererHelper.getFrustum().initSpacecraftStateVariables();
    }

    public void resetSpacecraftState()
    {
        copySpacecraftState();
        int nslices = getImageDepth();
        for (int i = 0; i < nslices; i++)
        {
        	rendererHelper.resetFrustaAndFootprint(i);
        }

        // offsetPixelCoordinates[0] = Double.MAX_VALUE;
        // offsetPixelCoordinates[1] = Double.MAX_VALUE;
        imageOffsetCalculator.resetInternalState();
        imageOffsetCalculator.updateFrameAdjustments();

        loadFootprint();
        calculateFrustum();
        deleteAdjustedImageInfo();
        // saveImageInfo();
    }

    public double getFocalLength()
    {
        return 0.0;
    }

    private double numberOfPixels = 0.0;

    public double getNumberOfPixels()
    {
        return numberOfPixels;
    }

    private double numberOfLines = 0.0;

    public double getNumberOfLines()
    {
        return numberOfLines;
    }

    public double getPixelWidth()
    {
        return 0.0;
    }

    public double getPixelHeight()
    {
        return 0.0;
    }

    public float getMinValue()
    {
        return minValue[currentSlice];
    }

    public float getMinValue(int slice)
    {
        return minValue[slice];
    }

    public void setMinValue(float minValue)
    {
        this.minValue[currentSlice] = minValue;
    }

    public float getMaxValue()
    {
        return maxValue[currentSlice];
    }

    public float getMaxValue(int slice)
    {
        return maxValue[slice];
    }

    public void setMaxValue(float maxValue)
    {
        this.maxValue[currentSlice] = maxValue;
    }

    public double[] getScalarRange(int slice)
    {
        double[] result = { minValue[slice], maxValue[slice] };
        return result;
    }

    public boolean shiftBands()
    {
        return false;
    }

    /**
     * Returns the number of spectra the image contains
     *
     * @return number of spectra
     */
    public int getNumberOfSpectralSegments()
    {
        return 0;
    }

    /**
     * For a multispectral image, returns an array of doubles containing the
     * wavelengths for each point on the image's spectrum.
     *
     * @return array of spectrum wavelengths
     */
    public double[] getSpectrumWavelengths(int segment)
    {
        return null;
    }

    /**
     * For a multispectral image, returns an array of doubles containing the
     * bandwidths for each point on the image's spectrum.
     *
     * @return array of spectrum wavelengths
     */
    public double[] getSpectrumBandwidths(int segment)
    {
        return null;
    }

    /**
     * For a multispectral image, returns an array of doubles containing the values
     * for each point on the image's spectrum.
     *
     * @return array of spectrum values
     */
    public double[] getSpectrumValues(int segment)
    {
        return null;
    }

    public String getSpectrumWavelengthUnits()
    {
        return null;
    }

    public String getSpectrumValueUnits()
    {
        return null;
    }

    /**
     * For a multispectral image, specify a region in pixel space over which to
     * calculate the spectrum values. The array is an Nx2 array of 2-dimensional
     * vertices in pixel coordinates. First index indicates the vertex, the second
     * index indicates which of the two pixel coordinates. A vertices array of
     * height 1 will specify a single pixel region. An array of h 2 will specify a
     * circular region where the first value is the center and the second value is a
     * point the circle. An array of size 3 or more will specify a polygonal region.
     *
     * @param vertices of region
     */
    public void setSpectrumRegion(double[][] vertices)
    {}

    /**
     * Return the default mask sizes as a 4 element integer array where the: first
     * element is the top mask size, second element is the right mask size, third
     * element is the bottom mask size, fourth element is the left mask size.
     *
     * @return
     */
    abstract protected int[] getMaskSizes();

    ///////////////////////////
    // Pointing methods
    ///////////////////////////
    protected void loadImageInfo( //
            String infoFilename, //
            int startSlice, // for loading multiple info files, the starting array index to put the info
                            // into
            boolean pad, // if true, will pad out the rest of the array with the same info
            String[] startTime, //
            String[] stopTime, //
            double[][] spacecraftPosition, //
            double[][] sunPosition, //
            double[][] frustum1, //
            double[][] frustum2, //
            double[][] frustum3, //
            double[][] frustum4, //
            double[][] boresightDirection, //
            double[][] upVector, //
            double[] targetPixelCoordinates, //
            boolean[] applyFrameAdjustments, //
            IntensityRange[] displayRange, //
            IntensityRange[] offlimbDisplayRange) throws NumberFormatException, IOException, FileNotFoundException //
    {
        if (infoFilename == null || infoFilename.endsWith("null"))
            throw new FileNotFoundException();

        boolean offset = true;

        FileInputStream fs = null;

        // look for an adjusted file first
        try
        {
            fs = new FileInputStream(infoFilename + ".adjusted");
        }
        catch (FileNotFoundException e)
        {
            fs = null;
        }

        // if no adjusted file exists, then load in the original unadjusted file
        if (fs == null)
        {
            // try {
            fs = new FileInputStream(infoFilename);
            // } catch (FileNotFoundException e) {
            // e.printStackTrace();
            // }
        }

        InputStreamReader isr = new InputStreamReader(fs);
        BufferedReader in = new BufferedReader(isr);

        // for multispectral images, the image slice being currently parsed
        int slice = startSlice - 1;

        String str;
        while ((str = in.readLine()) != null)
        {
            StringTokenizer st = new StringTokenizer(str);
            while (st.hasMoreTokens())
            {
                String token = st.nextToken();
                if (token == null)
                    continue;

                if (START_TIME.equals(token))
                {
                    st.nextToken();
                    startTime[0] = st.nextToken();
                }
                if (STOP_TIME.equals(token))
                {
                    st.nextToken();
                    stopTime[0] = st.nextToken();
                }
                // eventually, we should parse the number of exposures from the INFO file, for
                // now it is hard-coded -turnerj1
                // if (NUMBER_EXPOSURES.equals(token))
                // {
                // numberExposures = Integer.parseInt(st.nextToken());
                // if (numberExposures > 1)
                // {
                // spacecraftPosition = new double[numberExposures][3];
                // frustum1 = new double[numberExposures][3];
                // frustum2 = new double[numberExposures][3];
                // frustum3 = new double[numberExposures][3];
                // frustum4 = new double[numberExposures][3];
                // sunVector = new double[numberExposures][3];
                // boresightDirection = new double[numberExposures][3];
                // upVector = new double[numberExposures][3];
                // frusta = new Frustum[numberExposures];
                // footprint = new vtkPolyData[numberExposures];
                // footprintCreated = new boolean[numberExposures];
                // shiftedFootprint = new vtkPolyData[numberExposures];
                // }
                // }
                // For backwards compatibility with MSI images we use the endsWith function
                // rather than equals for FRUSTUM1, FRUSTUM2, FRUSTUM3, FRUSTUM4,
                // BORESIGHT_DIRECTION
                // and UP_DIRECTION since these are all prefixed with MSI_ in the info file.
                if (token.equals(TARGET_PIXEL_COORD))
                {
                    st.nextToken();
                    st.nextToken();
                    double x = Double.parseDouble(st.nextToken());
                    st.nextToken();
                    double y = Double.parseDouble(st.nextToken());
                    targetPixelCoordinates[0] = x;
                    targetPixelCoordinates[1] = y;
                }
                if (token.equals(TARGET_ROTATION))
                {
                    st.nextToken();
                    double x = Double.parseDouble(st.nextToken());
                    getRotationOffset()[0] = x;
                }
                if (token.equals(TARGET_ZOOM_FACTOR))
                {
                    st.nextToken();
                    double x = Double.parseDouble(st.nextToken());
                    getZoomFactor()[0] = x;
                }
                if (token.equals(APPLY_ADJUSTMENTS))
                {
                    st.nextToken();
                    offset = Boolean.parseBoolean(st.nextToken());
                    applyFrameAdjustments[0] = offset;
                }

                if (SPACECRAFT_POSITION.equals(token) || //
                        SUN_POSITION_LT.equals(token) || //
                        token.endsWith(FRUSTUM1) || //
                        token.endsWith(FRUSTUM2) || //
                        token.endsWith(FRUSTUM3) || //
                        token.endsWith(FRUSTUM4) || //
                        token.endsWith(BORESIGHT_DIRECTION) || //
                        token.endsWith(UP_DIRECTION)) //
                {
                    st.nextToken();
                    st.nextToken();
                    double x = Double.parseDouble(st.nextToken());
                    st.nextToken();
                    double y = Double.parseDouble(st.nextToken());
                    st.nextToken();
                    double z = Double.parseDouble(st.nextToken());
                    if (SPACECRAFT_POSITION.equals(token))
                    {
                        // SPACECRAFT_POSITION is assumed to be at the start of a frame, so increment
                        // slice count
                        slice++;
                        spacecraftPosition[slice][0] = x;
                        spacecraftPosition[slice][1] = y;
                        spacecraftPosition[slice][2] = z;
                    }
                    if (SUN_POSITION_LT.equals(token))
                    {
                        sunPosition[slice][0] = x;
                        sunPosition[slice][1] = y;
                        sunPosition[slice][2] = z;
                        // MathUtil.vhat(sunPosition[slice], sunPosition[slice]);
                    }
                    else if (token.endsWith(FRUSTUM1))
                    {
                        frustum1[slice][0] = x;
                        frustum1[slice][1] = y;
                        frustum1[slice][2] = z;
                        MathUtil.vhat(frustum1[slice], frustum1[slice]);
                    }
                    else if (token.endsWith(FRUSTUM2))
                    {
                        frustum2[slice][0] = x;
                        frustum2[slice][1] = y;
                        frustum2[slice][2] = z;
                        MathUtil.vhat(frustum2[slice], frustum2[slice]);
                    }
                    else if (token.endsWith(FRUSTUM3))
                    {
                        frustum3[slice][0] = x;
                        frustum3[slice][1] = y;
                        frustum3[slice][2] = z;
                        MathUtil.vhat(frustum3[slice], frustum3[slice]);
                    }
                    else if (token.endsWith(FRUSTUM4))
                    {
                        frustum4[slice][0] = x;
                        frustum4[slice][1] = y;
                        frustum4[slice][2] = z;
                        MathUtil.vhat(frustum4[slice], frustum4[slice]);
                    }
                    if (token.endsWith(BORESIGHT_DIRECTION))
                    {
                        boresightDirection[slice][0] = x;
                        boresightDirection[slice][1] = y;
                        boresightDirection[slice][2] = z;
                    }
                    if (token.endsWith(UP_DIRECTION))
                    {
                        upVector[slice][0] = x;
                        upVector[slice][1] = y;
                        upVector[slice][2] = z;
                    }
                }
                if (token.equals(DISPLAY_RANGE))
                {
                    st.nextToken();
                    st.nextToken();
                    int min = Integer.parseInt(st.nextToken());
                    st.nextToken();
                    int max = Integer.parseInt(st.nextToken());
                    st.nextToken();
                    displayRange[0] = new IntensityRange(min, max);
                }
                if (token.equals(OFFLIMB_DISPLAY_RANGE))
                {
                    st.nextToken();
                    st.nextToken();
                    int min = Integer.parseInt(st.nextToken());
                    st.nextToken();
                    int max = Integer.parseInt(st.nextToken());
                    st.nextToken();
                    offlimbDisplayRange[0] = new IntensityRange(min, max);
                }
            }
        }

        // once we've read in all the frames, pad out any additional missing frames
        if (pad)
        {
            int nslices = getImageDepth();
            for (int i = slice + 1; i < nslices; i++)
            {
                System.out.println("PerspectiveImage: loadImageInfo: num slices " + nslices + " and slice is " + slice + " and i is " + i + " and spacecraft pos length" + spacecraftPosition.length);

                spacecraftPosition[i][0] = spacecraftPosition[slice][0];
                spacecraftPosition[i][1] = spacecraftPosition[slice][1];
                spacecraftPosition[i][2] = spacecraftPosition[slice][2];

                sunPosition[i][0] = sunPosition[slice][0];
                sunPosition[i][1] = sunPosition[slice][1];
                sunPosition[i][2] = sunPosition[slice][2];

                frustum1[i][0] = frustum1[slice][0];
                frustum1[i][1] = frustum1[slice][1];
                frustum1[i][2] = frustum1[slice][2];

                frustum2[i][0] = frustum2[slice][0];
                frustum2[i][1] = frustum2[slice][1];
                frustum2[i][2] = frustum2[slice][2];

                frustum3[i][0] = frustum3[slice][0];
                frustum3[i][1] = frustum3[slice][1];
                frustum3[i][2] = frustum3[slice][2];

                frustum4[i][0] = frustum4[slice][0];
                frustum4[i][1] = frustum4[slice][1];
                frustum4[i][2] = frustum4[slice][2];

                boresightDirection[i][0] = boresightDirection[slice][0];
                boresightDirection[i][1] = boresightDirection[slice][1];
                boresightDirection[i][2] = boresightDirection[slice][2];

                upVector[slice][0] = upVector[slice][0];
                upVector[slice][1] = upVector[slice][1];
                upVector[slice][2] = upVector[slice][2];
            }
        }

        in.close();
    }

    public void saveImageInfo( //
            String infoFilename, //
            int slice, // currently, we only support single-frame INFO files
            String startTime, //
            String stopTime, //
            double[][] spacecraftPosition, //
            double[][] sunPosition, //
            double[][] frustum1, //
            double[][] frustum2, //
            double[][] frustum3, //
            double[][] frustum4, //
            double[][] boresightDirection, //
            double[][] upVector, //
            double[] targetPixelCoordinates, //
            double[] zoomFactor, //
            double[] rotationOffset, //
            boolean applyFrameAdjustments, //
            boolean flatten, //
            IntensityRange displayRange, //
            IntensityRange offLimbDisplayRange //
    ) throws NumberFormatException, IOException //
    {
        // for testing purposes only:
        // infoFilename = infoFilename + ".txt";
        // System.out.println("Saving infofile to: " + infoFilename + ".adjusted");

        FileOutputStream fs = null;

        // save out info file to cache with ".adjusted" appended to the name
        String suffix = flatten ? "" : ".adjusted";
        try
        {
            fs = new FileOutputStream(infoFilename + suffix);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return;
        }
        OutputStreamWriter osw = new OutputStreamWriter(fs);
        BufferedWriter out = new BufferedWriter(osw);

        out.write(String.format("%-22s= %s\n", START_TIME, startTime));
        out.write(String.format("%-22s= %s\n", STOP_TIME, stopTime));
        out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", SPACECRAFT_POSITION, spacecraftPosition[slice][0], spacecraftPosition[slice][1], spacecraftPosition[slice][2]));
        out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", BORESIGHT_DIRECTION, boresightDirection[slice][0], boresightDirection[slice][1], boresightDirection[slice][2]));
        out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", UP_DIRECTION, upVector[slice][0], upVector[slice][1], upVector[slice][2]));
        out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", FRUSTUM1, frustum1[slice][0], frustum1[slice][1], frustum1[slice][2]));
        out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", FRUSTUM2, frustum2[slice][0], frustum2[slice][1], frustum2[slice][2]));
        out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", FRUSTUM3, frustum3[slice][0], frustum3[slice][1], frustum3[slice][2]));
        out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", FRUSTUM4, frustum4[slice][0], frustum4[slice][1], frustum4[slice][2]));
        out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", SUN_POSITION_LT, sunPosition[slice][0], sunPosition[slice][1], sunPosition[slice][2]));
        out.write(String.format("%-22s= ( %16d , %16d )\n", DISPLAY_RANGE, displayRange.min, displayRange.max));
        out.write(String.format("%-22s= ( %16d , %16d )\n", OFFLIMB_DISPLAY_RANGE, offLimbDisplayRange.min, offLimbDisplayRange.max));

        boolean writeApplyAdustments = false;

        if (!flatten)
        {
            if (targetPixelCoordinates[0] != Double.MAX_VALUE && targetPixelCoordinates[1] != Double.MAX_VALUE)
            {
                out.write(String.format("%-22s= ( %1.16e , %1.16e )\n", TARGET_PIXEL_COORD, targetPixelCoordinates[0], targetPixelCoordinates[1]));
                writeApplyAdustments = true;
            }

            if (zoomFactor[0] != 1.0)
            {
                out.write(String.format("%-22s= %1.16e\n", TARGET_ZOOM_FACTOR, zoomFactor[0]));
                writeApplyAdustments = true;
            }

            if (rotationOffset[0] != 0.0)
            {
                out.write(String.format("%-22s= %1.16e\n", TARGET_ROTATION, rotationOffset[0]));
                writeApplyAdustments = true;
            }

            // only write out user-modified offsets if the image info has been modified
            if (writeApplyAdustments)
                out.write(String.format("%-22s= %b\n", APPLY_ADJUSTMENTS, applyFrameAdjustments));
        }

        out.close();
    }

    public String getLabelFileFullPath()
    {
        return labelFileFullPath;
    }

    public String getInfoFileFullPath()
    {
        return infoFileFullPath;
    }

    public String[] getInfoFilesFullPath()
    {
        String[] result = { infoFileFullPath };
        return result;
    }

    public String getSumfileFullPath()
    {
        return sumFileFullPath;
    }

    public String getLabelfileFullPath()
    {
        return getLabelFileFullPath();
    }

    protected void loadPointing() throws FitsException, IOException
    {
        if (key.getSource().equals(ImageSource.SPICE) || key.getSource().equals(ImageSource.CORRECTED_SPICE))
        {
            try
            {
                loadImageInfo();
            }
            catch (IOException ex)
            {
                System.out.println("INFO file not available");
                ex.printStackTrace();
            }
        }
        else if (key.getSource().equals(ImageSource.LABEL))
        {
            try
            {
                loadLabelFile();
            }
            catch (IOException ex)
            {
                System.out.println("LABEL file not available");
            }
        }
        else if (key.getSource().equals(ImageSource.LOCAL_PERSPECTIVE))
        {
            boolean loaded = false;
            try
            {
                loadAdjustedSumfile();
                loaded = true;
            }
            catch (FileNotFoundException e)
            {
                loaded = false;
            }
            if (!loaded)
            {
                try
                {
                    loadSumfile();
                    loaded = true;
                }
                catch (FileNotFoundException e)
                {
                    loaded = false;
                }
            }
            if (!loaded)
                this.loadImageInfo();
        }
        else
        {
            boolean loaded = false;
            try
            {
                loadAdjustedSumfile();
                loaded = true;
            }
            catch (FileNotFoundException e)
            {
                loaded = false;
            }
            if (!loaded)
            {
                try
                {
                    loadSumfile();
                    loaded = true;
                }
                catch (FileNotFoundException e)
                {
                    System.out.println("SUM file not available");
                    throw (e);
                }
            }
        }

        // copy loaded state values into the adjusted values
        copySpacecraftState();
        rendererHelper.getFrustum().updatePointing(this);
        rendererHelper.getFootprint().updatePointing(this);
    }

    private void loadImageInfo() throws NumberFormatException, IOException
    {
        String[] infoFileNames = getInfoFilesFullPath();
        // for (String name : infoFileNames) System.out.println("PerspectiveImage:
        // loadImageInfo: name is " + name);
        if (infoFileNames == null)
            System.out.println("infoFileNames is null");

        int nfiles = infoFileNames.length;

        // if (nslices > 1)
        // initSpacecraftStateVariables();

        boolean pad = nfiles > 1;

        for (int k = 0; k < nfiles; k++)
        {
            String[] start = new String[1];
            String[] stop = new String[1];
            boolean[] ato = new boolean[1];
            ato[0] = true;

            // System.out.println("Loading image: " + infoFileNames[k]);

            IntensityRange[] displayRange = new IntensityRange[1];
            IntensityRange[] offLimbDisplayRange = new IntensityRange[1];

            loadImageInfo( //
                    infoFileNames[k], //
                    k, //
                    pad, //
                    start, //
                    stop, //
                    spacecraftPositionOriginal, //
                    sunPositionOriginal, //
                    frustum1Original, //
                    frustum2Original, //
                    frustum3Original, //
                    frustum4Original, //
                    boresightDirectionOriginal, //
                    upVectorOriginal, //
                    getTargetPixelCoordinates(), //
                    ato, //
                    displayRange, //
                    offLimbDisplayRange);

            // should startTime and stopTime be an array? -turnerj1
            startTime = start[0];
            stopTime = stop[0];
            imageOffsetCalculator.applyFrameAdjustments[0] = ato[0];

            if (displayRange[0] != null)
            {
                setDisplayedImageRange(displayRange[0]);
            }
            if (offLimbDisplayRange[0] != null)
            {
                setOfflimbImageRange(offLimbDisplayRange[0]);
            }

            // updateFrustumOffset();

            // printpt(frustum1, "pds frustum1 ");
            // printpt(frustum2, "pds frustum2 ");
            // printpt(frustum3, "pds frustum3 ");
            // printpt(frustum4, "pds frustum4 ");
        }
    }

    private void deleteAdjustedImageInfo()
    {
        String[] infoFileNames = getInfoFilesFullPath();

        int nfiles = infoFileNames.length;

        for (int k = 0; k < nfiles; k++)
        {
            boolean[] ato = new boolean[1];
            ato[0] = true;

            deleteAdjustedImageInfo(infoFileNames[k]);
        }
    }

    private void loadAdjustedSumfile() throws NumberFormatException, IOException
    {
        // Looks for either SUM or INFO files with the following priority scheme:
        // - if a SUM file is specified, look first for an adjusted INFO file, then look
        // for the SUM file
        // - if an INFO file is specified, look first for an adjusted INFO file, the the
        // INFO file
        String filePath = getSumfileFullPath();
        if (filePath != null && filePath.endsWith("SUM"))
        	filePath = filePath.substring(0, filePath.length()-FilenameUtils.getExtension(filePath).length()) + "INFO";
        else
            filePath = "";

        String[] start = new String[1];
        String[] stop = new String[1];
        boolean[] ato = new boolean[1];
        ato[0] = true;

        IntensityRange[] displayRange = new IntensityRange[1];
        IntensityRange[] offLimbDisplayRange = new IntensityRange[1];

        loadImageInfo( //
                filePath, //
                0, //
                false, //
                start, //
                stop, //
                spacecraftPositionOriginal, //
                sunPositionOriginal, //
                frustum1Original, //
                frustum2Original, //
                frustum3Original, //
                frustum4Original, //
                boresightDirectionOriginal, //
                upVectorOriginal, //
                getTargetPixelCoordinates(), //
                ato, //
                displayRange, //
                offLimbDisplayRange);

        // should startTime and stopTime be an array? -turnerj1
        startTime = start[0];
        stopTime = stop[0];
        imageOffsetCalculator.applyFrameAdjustments[0] = ato[0];

        if (displayRange[0] != null)
        {
            setDisplayedImageRange(displayRange[0]);
        }
        if (offLimbDisplayRange[0] != null)
        {
            setOfflimbImageRange(offLimbDisplayRange[0]);
        }

    }

    void saveImageInfo()
    {
        String[] infoFileNames = getInfoFilesFullPath();
        String sumFileName = this.getSumfileFullPath();

        // int slice = getCurrentSlice();
        // System.out.println("Saving current slice: " + slice);
        try
        {
            int nfiles = infoFileNames.length;
            for (int fileindex = 0; fileindex < nfiles; fileindex++)
            {
                String filename = infoFileNames[fileindex];
                if (filename == null || filename.endsWith("/null"))
                	filename = sumFileName.substring(0, sumFileName.length()-FilenameUtils.getExtension(sumFileName).length()) + "INFO";

                int slice = this.getImageDepth() / 2;

                saveImageInfo( //
                        filename, //
                        slice, //
                        startTime, //
                        stopTime, //
                        spacecraftPositionOriginal, //
                        sunPositionOriginal, //
                        frustum1Original, //
                        frustum2Original, //
                        frustum3Original, //
                        frustum4Original, //
                        boresightDirectionOriginal, //
                        upVectorOriginal, //
                        getTargetPixelCoordinates(), //
                        getZoomFactor(), //
                        getRotationOffset(), //
                        imageOffsetCalculator.applyFrameAdjustments[0], //
                        false, //
                        getDisplayedRange(), //
                        getOffLimbDisplayedRange());
            }
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Saves adjusted image info out to an INFO file, folding in the adjusted
     * values, so no adjustment keywords appear.
     *
     * @param infoFileName
     */
    public void saveImageInfo(String infoFileName)
    {
        int slice = (getImageDepth() - 1) / 2;
        try
        {
            saveImageInfo( //
                    infoFileName, //
                    slice, //
                    startTime, //
                    stopTime, //
                    getSpacecraftPositionAdjusted(), //
                    getSunPositionAdjusted(), //
                    getFrustum1Adjusted(), //
                    getFrustum2Adjusted(), //
                    getFrustum3Adjusted(), //
                    getFrustum4Adjusted(), //
                    getBoresightDirectionAdjusted(), //
                    getUpVectorAdjusted(), //
                    getTargetPixelCoordinates(), //
                    getZoomFactor(), //
                    getRotationOffset(), //
                    imageOffsetCalculator.applyFrameAdjustments[0], //
                    true, //
                    getDisplayedRange(), //
                    getOffLimbDisplayedRange());
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Sometimes Bob Gaskell sumfiles contain numbers of the form .1192696009D+03
     * rather than .1192696009E+03 (i.e. a D instead of an E). This function
     * replaces D's with E's.
     *
     * @param s
     * @return
     */
    private void replaceDwithE(String[] s)
    {
        for (int i = 0; i < s.length; ++i)
            s[i] = s[i].replace('D', 'E');
    }

    protected void loadSumfile( //
            String sumfilename, //
            String[] startTime, //
            String[] stopTime, //
            double[][] spacecraftPosition, //
            double[][] sunVector, //
            double[][] frustum1, //
            double[][] frustum2, //
            double[][] frustum3, //
            double[][] frustum4, //
            double[][] boresightDirection, //
            double[][] upVector) throws IOException //
    {
        if (sumfilename == null)
            throw new FileNotFoundException();

        FileInputStream fs = new FileInputStream(sumfilename);
        InputStreamReader isr = new InputStreamReader(fs);
        BufferedReader in = new BufferedReader(isr);

        // for multispectral images, the image slice being currently parsed
        int slice = 0;

        in.readLine();

        String datetime = in.readLine().trim();
        datetime = DateTimeUtil.convertDateTimeFormat(datetime);
        startTime[0] = datetime;
        stopTime[0] = datetime;

        String[] tmp = in.readLine().trim().split("\\s+");
        double npx = Integer.parseInt(tmp[0]);
        double nln = Integer.parseInt(tmp[1]);

        tmp = in.readLine().trim().split("\\s+");
        replaceDwithE(tmp);
        double focalLengthMillimeters = Double.parseDouble(tmp[0]);

        tmp = in.readLine().trim().split("\\s+");
        replaceDwithE(tmp);
        spacecraftPosition[slice][0] = -Double.parseDouble(tmp[0]);
        spacecraftPosition[slice][1] = -Double.parseDouble(tmp[1]);
        spacecraftPosition[slice][2] = -Double.parseDouble(tmp[2]);

        double[] cx = new double[3];
        double[] cy = new double[3];
        double[] cz = new double[3];
        double[] sz = new double[3];

        tmp = in.readLine().trim().split("\\s+");
        replaceDwithE(tmp);
        cx[0] = Double.parseDouble(tmp[0]);
        cx[1] = Double.parseDouble(tmp[1]);
        cx[2] = Double.parseDouble(tmp[2]);

        tmp = in.readLine().trim().split("\\s+");
        replaceDwithE(tmp);
        cy[0] = Double.parseDouble(tmp[0]);
        cy[1] = Double.parseDouble(tmp[1]);
        cy[2] = Double.parseDouble(tmp[2]);

        tmp = in.readLine().trim().split("\\s+");
        replaceDwithE(tmp);
        cz[0] = Double.parseDouble(tmp[0]);
        cz[1] = Double.parseDouble(tmp[1]);
        cz[2] = Double.parseDouble(tmp[2]);

        tmp = in.readLine().trim().split("\\s+");
        replaceDwithE(tmp);
        sz[0] = Double.parseDouble(tmp[0]);
        sz[1] = Double.parseDouble(tmp[1]);
        sz[2] = Double.parseDouble(tmp[2]);

        tmp = in.readLine().trim().split("\\s+");
        replaceDwithE(tmp);
        double kmatrix00 = Math.abs(Double.parseDouble(tmp[0]));
        double kmatrix11 = Math.abs(Double.parseDouble(tmp[4]));

        // Here we calculate the image width and height using the K-matrix values.
        // This is used only when the constructor of this function was called with
        // loadPointingOnly set to true. When set to false, the image width and
        // and height is set in the loadImage function (after this function is called
        // and will overwrite these values here--though they should not be different).
        // But when in pointing-only mode, the loadImage function is not called so
        // we therefore set the image width and height here since some functions need
        // it.
        imageWidth = (int) npx;
        imageHeight = (int) nln;
        if (kmatrix00 > kmatrix11)
            imageHeight = (int) Math.round(nln * (kmatrix00 / kmatrix11));
        else if (kmatrix11 > kmatrix00)
            imageWidth = (int) Math.round(npx * (kmatrix11 / kmatrix00));

        double[] cornerVector = new double[3];
        double fov1 = Math.atan(npx / (2.0 * focalLengthMillimeters * kmatrix00));
        double fov2 = Math.atan(nln / (2.0 * focalLengthMillimeters * kmatrix11));
        cornerVector[0] = -Math.tan(fov1);
        cornerVector[1] = -Math.tan(fov2);
        cornerVector[2] = 1.0;

        double fx = cornerVector[0];
        double fy = cornerVector[1];
        double fz = cornerVector[2];
        frustum3[slice][0] = fx * cx[0] + fy * cy[0] + fz * cz[0];
        frustum3[slice][1] = fx * cx[1] + fy * cy[1] + fz * cz[1];
        frustum3[slice][2] = fx * cx[2] + fy * cy[2] + fz * cz[2];

        fx = -cornerVector[0];
        fy = cornerVector[1];
        fz = cornerVector[2];
        frustum4[slice][0] = fx * cx[0] + fy * cy[0] + fz * cz[0];
        frustum4[slice][1] = fx * cx[1] + fy * cy[1] + fz * cz[1];
        frustum4[slice][2] = fx * cx[2] + fy * cy[2] + fz * cz[2];

        fx = cornerVector[0];
        fy = -cornerVector[1];
        fz = cornerVector[2];
        frustum1[slice][0] = fx * cx[0] + fy * cy[0] + fz * cz[0];
        frustum1[slice][1] = fx * cx[1] + fy * cy[1] + fz * cz[1];
        frustum1[slice][2] = fx * cx[2] + fy * cy[2] + fz * cz[2];

        fx = -cornerVector[0];
        fy = -cornerVector[1];
        fz = cornerVector[2];
        frustum2[slice][0] = fx * cx[0] + fy * cy[0] + fz * cz[0];
        frustum2[slice][1] = fx * cx[1] + fy * cy[1] + fz * cz[1];
        frustum2[slice][2] = fx * cx[2] + fy * cy[2] + fz * cz[2];

        MathUtil.vhat(frustum1[slice], frustum1[slice]);
        MathUtil.vhat(frustum2[slice], frustum2[slice]);
        MathUtil.vhat(frustum3[slice], frustum3[slice]);
        MathUtil.vhat(frustum4[slice], frustum4[slice]);

        MathUtil.vhat(cz, boresightDirection[slice]);
        MathUtil.vhat(cx, upVector[slice]);
        MathUtil.vhat(sz, sunVector[slice]);

        in.close();
    }

    private void deleteAdjustedImageInfo(String filePath)
    {
        // Deletes for either SUM or INFO files with the following priority scheme:
        // - if a SUM file is specified, look first for an adjusted INFO file, then look
        // for the SUM file
        // - if an INFO file is specified, look first for an adjusted INFO file, the the
        // INFO file

        if (filePath == null || filePath.endsWith("null"))
        {
            filePath = getSumfileFullPath();
            if (filePath != null && filePath.endsWith("SUM"))
                filePath = filePath.substring(0, filePath.length()-FilenameUtils.getExtension(filePath).length()) + "INFO";
            else
                filePath = "";
        }

        // look for an adjusted file first
        try
        {
            File f = new File(filePath + ".adjusted");
            if (f.exists())
                f.delete();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //
    // Label (.lbl) file parsing methods
    //

    private static final Vector3D i = new Vector3D(1.0, 0.0, 0.0);
    private static final Vector3D j = new Vector3D(0.0, 1.0, 0.0);
    private static final Vector3D k = new Vector3D(0.0, 0.0, 1.0);

    private String targetName = null;
    private String instrumentId = null;
    private String filterName = null;
    private String objectName = null;

    private String startTimeString = null;
    private String stopTimeString = null;

    private String scTargetPositionString = null;
    private String targetSunPositionString = null;
    private String scOrientationString = null;
    private Rotation scOrientation = null;
    private double[] q = new double[4];
    private double[] cx = new double[3];
    private double[] cy = new double[3];
    private double[] cz = new double[3];

    private double focalLengthMillimeters = 100.0;
    private double npx = 4096.0;
    private double nln = 32.0;
    private double kmatrix00 = 1.0;
    private double kmatrix11 = 1.0;

    private void parseLabelKeyValuePair( //
            String key, //
            String value, //
            String[] startTime, //
            String[] stopTime, //
            double[] spacecraftPosition, //
            double[] sunVector, //
            double[] frustum1, //
            double[] frustum2, //
            double[] frustum3, //
            double[] frustum4, //
            double[] boresightDirection, //
            double[] upVector) throws IOException //
    {
        System.out.println("Label file key: " + key + " = " + value);

        if (key.equals("TARGET_NAME"))
            targetName = value;
        else if (key.equals("INSTRUMENT_ID"))
            instrumentId = value;
        else if (key.equals("FILTER_NAME"))
            filterName = value;
        else if (key.equals("OBJECT"))
            objectName = value;
        else if (key.equals("LINE_SAMPLES"))
        {
            if (objectName.equals("EXTENSION_CALGEOM_IMAGE"))
                numberOfPixels = Double.parseDouble(value);
        }
        else if (key.equals("LINES"))
        {
            if (objectName.equals("EXTENSION_CALGEOM_IMAGE"))
                numberOfLines = Double.parseDouble(value);
        }
        else if (key.equals("START_TIME"))
        {
            startTimeString = value;
            startTime[0] = startTimeString;
        }
        else if (key.equals("STOP_TIME"))
        {
            stopTimeString = value;
            stopTime[0] = stopTimeString;
        }
        else if (key.equals("SC_TARGET_POSITION_VECTOR"))
        {
            scTargetPositionString = value;
            String p[] = scTargetPositionString.split(",");
            spacecraftPosition[0] = Double.parseDouble(p[0].trim().split("\\s+")[0].trim());
            spacecraftPosition[1] = Double.parseDouble(p[1].trim().split("\\s+")[0].trim());
            spacecraftPosition[2] = Double.parseDouble(p[2].trim().split("\\s+")[0].trim());
        }
        else if (key.equals("TARGET_SUN_POSITION_VECTOR"))
        {
            targetSunPositionString = value;
            String p[] = targetSunPositionString.split(",");
            sunVector[0] = -Double.parseDouble(p[0].trim().split("\\s+")[0].trim());
            sunVector[1] = -Double.parseDouble(p[1].trim().split("\\s+")[0].trim());
            sunVector[2] = -Double.parseDouble(p[2].trim().split("\\s+")[0].trim());
        }
        else if (key.equals("QUATERNION"))
        {
            scOrientationString = value;
            String qstr[] = scOrientationString.split(",");
            q[0] = Double.parseDouble(qstr[0].trim().split("\\s+")[0].trim());
            q[1] = Double.parseDouble(qstr[1].trim().split("\\s+")[0].trim());
            q[2] = Double.parseDouble(qstr[2].trim().split("\\s+")[0].trim());
            q[3] = Double.parseDouble(qstr[3].trim().split("\\s+")[0].trim());
            scOrientation = new Rotation(q[0], q[1], q[2], q[3], false);
        }

    }

    protected void loadLabelFile( //
            String labelFileName, //
            String[] startTime, //
            String[] stopTime, //
            double[][] spacecraftPosition, //
            double[][] sunVector, //
            double[][] frustum1, //
            double[][] frustum2, //
            double[][] frustum3, //
            double[][] frustum4, //
            double[][] boresightDirection, //
            double[][] upVector) throws IOException //
    {
        System.out.println(labelFileName);

        // for multispectral images, the image slice being currently parsed
        int slice = 0;

        // open a file input stream
        FileInputStream fs = new FileInputStream(labelFileName);
        InputStreamReader isr = new InputStreamReader(fs);
        BufferedReader in = new BufferedReader(isr);

        //
        // Parse each line of the stream and process each key-value pair,
        // merging multiline numeric ("vector") values into a single-line
        // string. Multi-line quoted strings are ignored.
        //
        boolean inStringLiteral = false;
        boolean inVector = false;
        List<String> vector = new ArrayList<String>();
        String key = null;
        String value = null;
        String line = null;
        while ((line = in.readLine()) != null)
        {
            if (line.length() == 0)
                continue;

            // for now, multi-line quoted strings are ignored (i.e. treated as comments)
            if (line.trim().equals("\""))
            {
                inStringLiteral = false;
                continue;
            }

            if (inStringLiteral)
                continue;

            // terminate a multi-line numeric value (a "vector")
            if (line.trim().equals(")"))
            {
                inVector = false;
                value = "";
                for (String element : vector)
                    value = value + element;

                parseLabelKeyValuePair( //
                        key, //
                        value, //
                        startTime, //
                        stopTime, //
                        spacecraftPosition[slice], //
                        sunVector[slice], //
                        frustum1[slice], //
                        frustum2[slice], //
                        frustum3[slice], //
                        frustum4[slice], //
                        boresightDirection[slice], //
                        upVector[slice]);

                vector.clear();
                continue;
            }

            // add a line to the current vector
            if (inVector)
            {
                vector.add(line.trim());
                continue;
            }

            // extract key value pair
            String tokens[] = line.split("=");
            if (tokens.length < 2)
                continue;

            key = tokens[0].trim();
            value = tokens[1].trim();

            // detect and ignore comments
            if (value.equals("\""))
            {
                inStringLiteral = true;
                continue;
            }

            // start to accumulate numeric vector values
            if (value.equals("("))
            {
                inVector = true;
                continue;
            }

            if (value.startsWith("("))
                value = stripBraces(value);
            else
                value = stripQuotes(value);

            parseLabelKeyValuePair( //
                    key, //
                    value, //
                    startTime, //
                    stopTime, //
                    spacecraftPosition[slice], //
                    sunVector[slice], //
                    frustum1[slice], //
                    frustum2[slice], //
                    frustum3[slice], //
                    frustum4[slice], //
                    boresightDirection[slice], //
                    upVector[slice]);

        }

        in.close();

        //
        // calculate image projection from the parsed parameters
        //
        this.focalLengthMillimeters = getFocalLength();
        this.npx = getNumberOfPixels();
        this.nln = getNumberOfLines();
        this.kmatrix00 = 1.0 / getPixelWidth();
        this.kmatrix11 = 1.0 / getPixelHeight();

        Vector3D boresightVector3D = scOrientation.applyTo(i);
        boresightDirection[slice][0] = cz[0] = boresightVector3D.getX();
        boresightDirection[slice][1] = cz[1] = boresightVector3D.getY();
        boresightDirection[slice][2] = cz[2] = boresightVector3D.getZ();

        Vector3D upVector3D = scOrientation.applyTo(j);
        upVector[slice][0] = cy[0] = upVector3D.getX();
        upVector[slice][1] = cy[1] = upVector3D.getY();
        upVector[slice][2] = cy[2] = upVector3D.getZ();

        Vector3D leftVector3D = scOrientation.applyTo(k);
        cx[0] = -leftVector3D.getX();
        cx[1] = -leftVector3D.getY();
        cx[2] = -leftVector3D.getZ();

        // double kmatrix00 = Math.abs(Double.parseDouble(tmp[0]));
        // double kmatrix11 = Math.abs(Double.parseDouble(tmp[4]));

        // Here we calculate the image width and height using the K-matrix values.
        // This is used only when the constructor of this function was called with
        // loadPointingOnly set to true. When set to false, the image width and
        // and height is set in the loadImage function (after this function is called
        // and will overwrite these values here--though they should not be different).
        // But when in pointing-only mode, the loadImage function is not called so
        // we therefore set the image width and height here since some functions need
        // it.
        imageWidth = (int) npx;
        imageHeight = (int) nln;
        // if (kmatrix00 > kmatrix11)
        // imageHeight = (int)Math.round(nln * (kmatrix00 / kmatrix11));
        // else if (kmatrix11 > kmatrix00)
        // imageWidth = (int)Math.round(npx * (kmatrix11 / kmatrix00));

        double[] cornerVector = new double[3];
        double fov1 = Math.atan(npx / (2.0 * focalLengthMillimeters * kmatrix00));
        double fov2 = Math.atan(nln / (2.0 * focalLengthMillimeters * kmatrix11));
        cornerVector[0] = -Math.tan(fov1);
        cornerVector[1] = -Math.tan(fov2);
        cornerVector[2] = 1.0;

        double fx = cornerVector[0];
        double fy = cornerVector[1];
        double fz = cornerVector[2];
        frustum3[slice][0] = fx * cx[0] + fy * cy[0] + fz * cz[0];
        frustum3[slice][1] = fx * cx[1] + fy * cy[1] + fz * cz[1];
        frustum3[slice][2] = fx * cx[2] + fy * cy[2] + fz * cz[2];

        fx = -cornerVector[0];
        fy = cornerVector[1];
        fz = cornerVector[2];
        frustum4[slice][0] = fx * cx[0] + fy * cy[0] + fz * cz[0];
        frustum4[slice][1] = fx * cx[1] + fy * cy[1] + fz * cz[1];
        frustum4[slice][2] = fx * cx[2] + fy * cy[2] + fz * cz[2];

        fx = cornerVector[0];
        fy = -cornerVector[1];
        fz = cornerVector[2];
        frustum1[slice][0] = fx * cx[0] + fy * cy[0] + fz * cz[0];
        frustum1[slice][1] = fx * cx[1] + fy * cy[1] + fz * cz[1];
        frustum1[slice][2] = fx * cx[2] + fy * cy[2] + fz * cz[2];

        fx = -cornerVector[0];
        fy = -cornerVector[1];
        fz = cornerVector[2];
        frustum2[slice][0] = fx * cx[0] + fy * cy[0] + fz * cz[0];
        frustum2[slice][1] = fx * cx[1] + fy * cy[1] + fz * cz[1];
        frustum2[slice][2] = fx * cx[2] + fy * cy[2] + fz * cz[2];

        MathUtil.vhat(frustum1[slice], frustum1[slice]);
        MathUtil.vhat(frustum2[slice], frustum2[slice]);
        MathUtil.vhat(frustum3[slice], frustum3[slice]);
        MathUtil.vhat(frustum4[slice], frustum4[slice]);

    }

    private String stripQuotes(String input)
    {
        String result = input;
        if (input.startsWith("\""))
            result = result.substring(1);
        if (input.endsWith("\""))
            result = result.substring(0, input.length() - 2);
        return result;
    }

    private String stripBraces(String input)
    {
        String result = input;
        if (input.startsWith("("))
            result = result.substring(1);
        if (input.endsWith(")"))
            result = result.substring(0, input.length() - 2);
        return result;
    }

    private void loadSumfile() throws NumberFormatException, IOException
    {
        String[] start = new String[1];
        String[] stop = new String[1];

        loadSumfile( //
                getSumfileFullPath(), //
                start, //
                stop, //
                spacecraftPositionOriginal, //
                sunPositionOriginal, //
                frustum1Original, //
                frustum2Original, //
                frustum3Original, //
                frustum4Original, //
                boresightDirectionOriginal, //
                upVectorOriginal);

        startTime = start[0];
        stopTime = stop[0];

        // printpt(frustum1, "gas frustum1 ");
        // printpt(frustum2, "gas frustum2 ");
        // printpt(frustum3, "gas frustum3 ");
        // printpt(frustum4, "gas frustum4 ");
    }

    private void loadLabelFile() throws NumberFormatException, IOException
    {
        System.out.println("Loading label (.lbl) file...");
        String[] start = new String[1];
        String[] stop = new String[1];

        loadLabelFile( //
                getLabelFileFullPath(), //
                start, //
                stop, //
                spacecraftPositionOriginal, //
                sunPositionOriginal, //
                frustum1Original, //
                frustum2Original, //
                frustum3Original, //
                frustum4Original, //
                boresightDirectionOriginal, //
                upVectorOriginal);

        startTime = start[0];
        stopTime = stop[0];

    }

    public void setLabelFileFullPath(String labelFileFullPath)
    {
        this.labelFileFullPath = labelFileFullPath;
    }

    ////////////////////////
    // I/O Methods
    ////////////////////////
    public void exportAsEnvi( //
            String enviFilename, // no extensions
            String interleaveType, // "bsq", "bil", or "bip"
            boolean hostByteOrder //
    ) throws IOException //
    {
        // Check if interleave type is recognized
        switch (interleaveType)
        {
        case "bsq":
        case "bil":
        case "bip":
            break;
        default:
            System.out.println("Interleave type " + interleaveType + " unrecognized, aborting exportAsEnvi()");
            return;
        }

        // Create output stream for header (.hdr) file
        FileOutputStream fs = null;
        try
        {
            fs = new FileOutputStream(enviFilename + ".hdr");
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return;
        }
        OutputStreamWriter osw = new OutputStreamWriter(fs);
        BufferedWriter out = new BufferedWriter(osw);

        // Write the fields of the header
        out.write("ENVI\n");
        out.write("samples = " + imageWidth + "\n");
        out.write("lines = " + imageHeight + "\n");
        out.write("bands = " + getImageDepth() + "\n");
        out.write("header offset = " + "0" + "\n");
        out.write("data type = " + "4" + "\n"); // 1 = byte, 2 = int, 3 = signed int, 4 = float
        out.write("interleave = " + interleaveType + "\n"); // bsq = band sequential, bil = band interleaved by line, bip = band interleaved
                                                            // by pixel
        out.write("byte order = "); // 0 = host(intel, LSB first), 1 = network (IEEE, MSB first)
        if (hostByteOrder)
        {
            // Host byte order
            out.write("0" + "\n");
        }
        else
        {
            // Network byte order
            out.write("1" + "\n");
        }
        out.write(getEnviHeaderAppend());
        out.close();

        // Configure byte buffer & endianess
        ByteBuffer bb = ByteBuffer.allocate(4 * imageWidth * imageHeight * getImageDepth()); // 4 bytes per float
        if (hostByteOrder)
        {
            // Little Endian = LSB stored first
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        else
        {
            // Big Endian = MSB stored first
            bb.order(ByteOrder.BIG_ENDIAN);
        }

        // Write pixels to byte buffer
        // Remember, VTK origin is at bottom left while ENVI origin is at top left
        float[][][] imageData = ImageDataUtil.vtkImageDataToArray3D(getRawImage());
        switch (interleaveType)
        {
        case "bsq":
            // Band sequential: col, then row, then depth
            for (int depth = 0; depth < getImageDepth(); depth++)
            {
                // for(int row = imageHeight-1; row >= 0; row--)
                for (int row = 0; row < imageHeight; row++)
                {
                    for (int col = 0; col < imageWidth; col++)
                    {
                        bb.putFloat(imageData[depth][row][col]);
                    }
                }
            }
            break;
        case "bil":
            // Band interleaved by line: col, then depth, then row
            // for(int row=imageHeight-1; row >= 0; row--)
            for (int row = 0; row < imageHeight; row++)
            {
                for (int depth = 0; depth < getImageDepth(); depth++)
                {
                    for (int col = 0; col < imageWidth; col++)
                    {
                        bb.putFloat(imageData[depth][row][col]);
                    }
                }
            }
            break;
        case "bip":
            // Band interleaved by pixel: depth, then col, then row
            // for(int row=imageHeight-1; row >= 0; row--)
            for (int row = 0; row < imageHeight; row++)
            {
                for (int col = 0; col < imageWidth; col++)
                {
                    for (int depth = 0; depth < getImageDepth(); depth++)
                    {
                        bb.putFloat(imageData[depth][row][col]);
                    }
                }
            }
            break;
        }

        // Create output stream and write contents of byte buffer
        try (FileOutputStream stream = new FileOutputStream(enviFilename))
        {
            FileChannel fc = stream.getChannel();
            bb.flip(); // flip() is a misleading name, nothing is being flipped. Buffer end is set to
                       // curr pos and curr pos set to beginning.
            fc.write(bb);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    public String getEnviHeaderAppend()
    {
        return "";
    }

    protected String initializeFitFileFullPath() throws IOException
    {
        return initLocalFitFileFullPath();
    }

    protected String initializeEnviFileFullPath()
    {
        return initLocalEnviFileFullPath();
    }

    protected String initializePngFileFullPath()
    {
    	return initLocalPngFileFullPath();
    }

    protected String initLocalPngFileFullPath()
    {
        String name = getKey().getName().endsWith(".png") ? getKey().getName() : null;
        if (name == null)
            return name;
        if (name.startsWith("file://"))
            return name.substring(name.indexOf("file://") + 7);
        else
            return name;
    }

    protected String initLocalFitFileFullPath()
    {
        String keyName = getKey().getName();
        if (keyName.endsWith(".fit") || keyName.endsWith(".fits") ||
                keyName.endsWith(".FIT") || keyName.endsWith(".FITS"))
        {
            // Allowed fit file extensions for getKey().name
            return keyName;
        }
        else
        {
            return null;
        }
    }

    protected String initLocalEnviFileFullPath()
    {
        return VtkENVIReader.isENVIFilename(getKey().getName()) ? getKey().getName() : null;
    }

    protected String initializeLabelFileFullPath()
    {
        return initLocalLabelFileFullPath();
    }

    protected String initializeInfoFileFullPath()
    {
        return initLocalInfoFileFullPath();
    }

    protected String initializeSumfileFullPath() throws IOException
    {
        return initLocalSumfileFullPath();
    }

    protected String initLocalLabelFileFullPath()
    {
        return null;
    }

    private List<CustomImageKeyInterface> getCustomImageMetadata() throws IOException
    {
        final Key<List<CustomImageKeyInterface>> customImagesKey = Key.of("customImages");
        String file;
        if (getKey().getName().startsWith("file://"))
            file = getKey().getName().substring(7, getKey().getName().lastIndexOf("/"));
        else
            file = new File(getKey().getImageFilename()).getParent();
        String configFilename = file + File.separator + "config.txt";
        FixedMetadata metadata = Serializers.deserialize(new File(configFilename), "CustomImages");
        return metadata.get(customImagesKey);

    }

    protected <T> T read(Key<T> key, Metadata configMetadata)
    {
        T value = configMetadata.get(key);
        if (value != null)
            return value;
        return null;
    }

    protected String initLocalInfoFileFullPath()
    {
        List<CustomImageKeyInterface> images;
        try
        {
            images = getCustomImageMetadata();
            for (ImageKeyInterface info : images)
            {
                String filename = new File(getKey().getName()).getName();
                if (filename.equals(info.getImageFilename()))
                {
                    if (info.getFileType() == FileType.SUM)
                        return null;
                    String string = new File(getKey().getName()).getParent() + File.separator + info.getPointingFile();
                    if (getKey().getName().startsWith("file:/"))
                        return string.substring(5);
                    else
                        return string;
                }
            }
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    protected String initLocalSumfileFullPath()
    {

        List<CustomImageKeyInterface> images;
        try
        {
            images = getCustomImageMetadata();
            for (CustomImageKeyInterface info : images)
            {
                String filename = new File(getKey().getName()).getName();
                if (filename.equals(info.getImageFilename()))
                {
                    if (info.getFileType() == FileType.INFO)
                        return null;

                    String string = new File(getKey().getName()).getParent() + File.separator + info.getPointingFile();
                    if (getKey().getName().startsWith("file:/"))
                        return string.substring(5);
                    else
                        return string;
                }
            }
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    private void loadImageInfoFromConfigFile()
    {
        List<CustomImageKeyInterface> images;
        try
        {
            images = getCustomImageMetadata();
            for (CustomImageKeyInterface info : images)
            {
                String filename = new File(getKey().getName()).getName();
                if (filename.equals(info.getImageFilename()))
                {
                    imageName = info.getImageFilename();
                }
            }
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

//        // Look in the config file and figure out which index this image
//        // corresponds to. The config file is located in the same folder
//        // as the image file
//        String configFilename = new File(getKey().name).getParent() + File.separator + "config.txt";
//        MapUtil configMap = new MapUtil(configFilename);
//        String[] imageFilenames = configMap.getAsArray(IMAGE_FILENAMES);
//        for (int i=0; i<imageFilenames.length; ++i)
//        {
//            String filename = new File(getKey().name).getName();
//            if (filename.equals(imageFilenames[i]))
//            {
//                imageName = configMap.getAsArray(Image.IMAGE_NAMES)[i];
//                break;
//            }
//        }
    }

    public String getPngFileFullPath()
    {
        return pngFileFullPath;
    }

    public String getFitFileFullPath()
    {
        return fitFileFullPath;
    }

    public String getEnviFileFullPath()
    {
        return enviFileFullPath;
    }

    public String getImageFileFullPath()
    {
        if (fitFileFullPath != null)
        {
            return fitFileFullPath;
        }
        else if (pngFileFullPath != null)
        {
            return pngFileFullPath;
        }
        else if (enviFileFullPath != null)
        {
            return enviFileFullPath;
        }
        else
        {
            return null;
        }
    }

    public String[] getFitFilesFullPath()
    {
        String[] result = { fitFileFullPath };
        return result;
    }

    protected void loadPngFile()
    {
        String name = getPngFileFullPath();

        String imageFile = null;
        if (getKey().getSource() == ImageSource.IMAGE_MAP)
            imageFile = FileCache.getFileFromServer(name).getAbsolutePath();
        else
            imageFile = getKey().getName();
        if (imageFile.startsWith("file://"))
        	imageFile = SafeURLPaths.instance().getString(imageFile.substring(imageFile.indexOf("file://") + 7));
//            imageFile = imageFile.substring(imageFile.indexOf("file://") + 7);
        if (getRawImage() == null)
            setRawImage(new vtkImageData());

        vtkPNGReader reader = new vtkPNGReader();
        reader.SetFileName(imageFile);
        reader.Update();
        getRawImage().DeepCopy(reader.GetOutput());
    }

    protected void loadFitsFiles() throws FitsException, IOException
    {
        // TODO: maybe make this more efficient if possible

        String[] filenames = getFitFilesFullPath();
        String filename = filenames[0];

        float[][] array2D = null;
        float[][][] array3D = null;
        double[][][] array3Ddouble = null;

        int[] fitsAxes = null;
        int fitsNAxes = 0;
        // height is axis 0
        int fitsHeight = 0;
        // for 2D pixel arrays, width is axis 1, for 3D pixel arrays, width axis is 2
        int fitsWidth = 0;
        // for 2D pixel arrays, depth is 0, for 3D pixel arrays, depth axis is 1
        int fitsDepth = 0;

        // single file images (e.g. LORRI and LEISA)
        if (filenames.length == 1)
        {
            try (Fits f = new Fits(filename))
            {
                BasicHDU<?> h = f.getHDU(fitFileImageExtension);

                fitsAxes = h.getAxes();
                fitsNAxes = fitsAxes.length;
                fitsHeight = fitsAxes[0];
                fitsWidth = fitsNAxes == 3 ? fitsAxes[2] : fitsAxes[1];
                fitsDepth = fitsNAxes == 3 ? fitsAxes[1] : 1;

                Object data = h.getData().getData();

                // for 3D arrays we consider the second axis the "spectral" axis
                if (data instanceof float[][][])
                {
                    if (shiftBands())
                    {
                        array3D = new float[fitsHeight][fitsWidth][fitsDepth];
                        for (int i = 0; i < fitsHeight; ++i)
                            for (int j = 0; j < fitsWidth; ++j)
                                for (int k = 0; k < fitsDepth; ++k)
                                {
                                    int w = i + j - fitsDepth / 2;
                                    if (w >= 0 && w < fitsHeight)
                                        array3D[w][j][k] = ((float[][][]) data)[i][j][k];
                                }

                    }
                    else
                        array3D = (float[][][]) data;

                    // System.out.println("3D pixel array detected: " + array3D.length + "x" +
                    // array3D[0].length + "x" + array3D[0][0].length);
                }
                else if (data instanceof double[][][])
                {
                    array3Ddouble = new double[fitsHeight][fitsWidth][fitsDepth];
                    if (shiftBands())
                    {
                        for (int i = 0; i < fitsHeight; ++i)
                            for (int j = 0; j < fitsWidth; ++j)
                                for (int k = 0; k < fitsDepth; ++k)
                                {
                                    int w = i + j - fitsDepth / 2;
                                    if (w >= 0 && w < fitsHeight)
                                        array3Ddouble[w][j][k] = ((double[][][]) data)[i][j][k];
                                }

                    }
                    else
                    {
                        for (int i = 0; i < fitsHeight; ++i)
                            for (int j = 0; j < fitsWidth; ++j)
                                for (int k = 0; k < fitsDepth; ++k)
                                {
                                    array3Ddouble[i][j][k] = ((double[][][]) data)[i][j][k];
                                }

                    }

                    // System.out.println("3D pixel array detected: " + array3D.length + "x" +
                    // array3D[0].length + "x" + array3D[0][0].length);
                }
                else if (data instanceof float[][])
                {
                    array2D = (float[][]) data;
                }
                else if (data instanceof short[][])
                {
                    short[][] arrayS = (short[][]) data;
                    array2D = new float[fitsHeight][fitsWidth];

                    for (int i = 0; i < fitsHeight; ++i)
                        for (int j = 0; j < fitsWidth; ++j)
                        {
                            array2D[i][j] = arrayS[i][j];
                        }
                }
                else if (data instanceof double[][])
                {
                    double[][] arrayDouble = (double[][]) data;
                    array2D = new float[fitsHeight][fitsWidth];

                    for (int i = 0; i < fitsHeight; ++i)
                        for (int j = 0; j < fitsWidth; ++j)
                        {
                            array2D[i][j] = (float) arrayDouble[i][j];
                        }
                }
                else if (data instanceof byte[][])
                {
                    byte[][] arrayB = (byte[][]) data;
                    array2D = new float[fitsHeight][fitsWidth];

                    for (int i = 0; i < fitsHeight; ++i)
                        for (int j = 0; j < fitsWidth; ++j)
                        {
                            array2D[i][j] = arrayB[i][j] & 0xFF;
                        }
                }
                // WARNING: THIS IS A TOTAL HACK TO SUPPORT DART LUKE TEST IMAGES:
                else if (data instanceof byte[][][])
                {
                    // DART LUKE images are color: 3-d slab with the 3rd
                    // dimension being RGB, but the first test images are
                    // monochrome. Thus, in order to process the images, making
                    // this temporary hack.
                    byte[][][] arrayB = (byte[][][]) data;

                    // Override the default setup used for other 3-d images.
                    fitsDepth = 1;
                    fitsHeight = arrayB[0].length;
                    fitsWidth = arrayB[0][0].length;

                    array2D = new float[fitsHeight][fitsWidth];

                    for (int i = 0; i < fitsHeight; ++i)
                        for (int j = 0; j < fitsWidth; ++j)
                        {
                            array2D[i][j] = arrayB[0][i][j] & 0xFF;
                        }
                }
                else
                {
                    System.out.println("Data type not supported: " + data.getClass().getCanonicalName());
                    return;
                }

                // load in calibration info
                loadImageCalibrationData(f);
            }
        }
        // for multi-file images (e.g. MVIC)
        else if (filenames.length > 1)
        {
            fitsDepth = filenames.length;
            fitsAxes = new int[3];
            fitsAxes[2] = fitsDepth;
            fitsNAxes = 3;

            for (int k = 0; k < fitsDepth; k++)
            {
                try (Fits f = new Fits(filenames[k]))
                {
                    BasicHDU<?> h = f.getHDU(fitFileImageExtension);

                    int[] multiImageAxes = h.getAxes();
                    int multiImageNAxes = multiImageAxes.length;

                    if (multiImageNAxes > 2)
                    {
                        System.out.println("Multi-file images must be 2D.");
                        return;
                    }

                    // height is axis 0, width is axis 1
                    fitsHeight = fitsAxes[0] = multiImageAxes[0];
                    fitsWidth = fitsAxes[2] = multiImageAxes[1];

                    if (array3D == null)
                        array3D = new float[fitsHeight][fitsDepth][fitsWidth];

                    Object data = h.getData().getData();

                    if (data instanceof float[][])
                    {
                        // NOTE: could performance be improved if depth was the first index and the
                        // entire 2D array could be assigned to a each slice? -turnerj1
                        for (int i = 0; i < fitsHeight; ++i)
                            for (int j = 0; j < fitsWidth; ++j)
                            {
                                array3D[i][k][j] = ((float[][]) data)[i][j];
                            }
                    }
                    else if (data instanceof short[][])
                    {
                        short[][] arrayS = (short[][]) data;

                        for (int i = 0; i < fitsHeight; ++i)
                            for (int j = 0; j < fitsWidth; ++j)
                            {
                                array3D[i][k][j] = arrayS[i][j];
                            }
                    }
                    else if (data instanceof byte[][])
                    {
                        byte[][] arrayB = (byte[][]) data;

                        for (int i = 0; i < fitsHeight; ++i)
                            for (int j = 0; j < fitsWidth; ++j)
                            {
                                array3D[i][k][j] = arrayB[i][j] & 0xFF;
                            }
                    }
                    else
                    {
                        System.out.println("Data type not supported!");
                        return;
                    }
                }
            }
        }
        IImagingInstrument instrument = key.getInstrument();
        boolean isTranspose = instrument != null ? instrument.isTranspose() : true;

        setRawImage(createRawImage(fitsHeight, fitsWidth, fitsDepth, isTranspose, array2D, array3D));
    }

    protected void loadEnviFile()
    {
        String name = getEnviFileFullPath();

        String imageFile = null;
        if (getKey().getSource() == ImageSource.IMAGE_MAP)
            imageFile = FileCache.getFileFromServer(name).getAbsolutePath();
        else
            imageFile = getKey().getName();

        if (getRawImage() == null)
            setRawImage(new vtkImageData());
        if (imageFile.startsWith("file://"))
            imageFile = imageFile.substring(imageFile.indexOf("file://") + 7);
        if (imageFile.startsWith("file:/"))
            imageFile = imageFile.substring(imageFile.indexOf("file:/") + 6);
        System.out.println("PerspectiveImage: loadEnviFile: image file is " + imageFile);
        VtkENVIReader reader = new VtkENVIReader();
        reader.SetFileName(imageFile);
        reader.Update();
        getRawImage().DeepCopy(reader.GetOutput());
        minValue = reader.getMinValues();
        maxValue = reader.getMaxValues();
    }

    protected vtkImageData loadRawImage() throws FitsException, IOException
    {
        if (getFitFileFullPath() != null)
            loadFitsFiles();
        else if (getPngFileFullPath() != null)
            loadPngFile();
        else if (getEnviFileFullPath() != null)
            loadEnviFile();

        return getRawImage();
    }

    protected int loadNumSlices()
    {
        int imageDepth = getImageDepth();
        if (getFitFileFullPath() != null)
        {
            String filename = getFitFileFullPath();
            try (Fits f = new Fits(filename))
            {
                BasicHDU<?> h = f.getHDU(fitFileImageExtension);

                int[] fitsAxes = h.getAxes();
                int fitsNAxes = fitsAxes.length;
                int fitsDepth = fitsNAxes == 3 ? fitsAxes[1] : 1;

                imageDepth = fitsDepth;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else if (getPngFileFullPath() != null)
        {
            // Do nothing for now
        }
        else if (getEnviFileFullPath() != null)
        {
            // Get the number of bands from the ENVI header
            String name = getEnviFileFullPath();

            String imageFile = null;
            if (getKey().getSource() == ImageSource.IMAGE_MAP)
                imageFile = FileCache.getFileFromServer(name).getAbsolutePath();
            else
                imageFile = getKey().getName();

            if (imageFile.startsWith("file://"))
                imageFile = imageFile.substring(imageFile.indexOf("file://") + 7);
            if (imageFile.startsWith("file:/"))
                imageFile = imageFile.substring(imageFile.indexOf("file:/") + 6);
            VtkENVIReader reader = new VtkENVIReader();
            reader.SetFileName(imageFile);
            imageDepth = reader.getNumBands();
            // for multislice images, set slice to middle slice
            if (imageDepth > 1)
                setCurrentSlice(imageDepth / 2);
        }

        return imageDepth;
    }

    @Override
    public void outputToOBJ(String filePath)
    {
        // write image to obj triangles w/ texture map based on displayed image
        Path footprintFilePath = Paths.get(filePath);
        String headerString = "start time " + getStartTime() + " end time " + getStopTime();
        ObjUtil.writePolyDataToObj(rendererHelper.getFootprint().shiftedFootprint[0], getDisplayedImage(), footprintFilePath, headerString);
        // write footprint boundary to obj lines
        vtkFeatureEdges edgeFilter = new vtkFeatureEdges();
        edgeFilter.SetInputData(rendererHelper.getFootprint().shiftedFootprint[0]);
        edgeFilter.Update();
        Path basedir = Paths.get(filePath).getParent();
        String filename = Paths.get(filePath).getFileName().toString();
        Path boundaryFilePath = basedir.resolve("bnd_" + filename);
        ObjUtil.writePolyDataToObj(edgeFilter.GetOutput(), boundaryFilePath);
        //
        Path frustumFilePath = basedir.resolve("frst_" + filename);
        double[] spacecraftPosition = new double[3];
        double[] focalPoint = new double[3];
        double[] upVector = new double[3];
        getCameraOrientation(spacecraftPosition, focalPoint, upVector);
        String frustumFileHeader = "Camera position=" + new Vector3D(spacecraftPosition) + " Camera focal point=" + new Vector3D(focalPoint) + " Camera up vector=" + new Vector3D(upVector);
        ObjUtil.writePolyDataToObj(rendererHelper.getFrustum().frustumPolyData, frustumFilePath, frustumFileHeader);
    }

    protected void loadImageCalibrationData(Fits f) throws FitsException, IOException
    {
        // to be overridden by subclasses that load calibration data
    }

    protected void loadImage() throws FitsException, IOException
    {
        setRawImage(loadRawImage());

        if (getRawImage() == null)
            return;

        processRawImage(getRawImage());

        int[] dims = getRawImage().GetDimensions();
        imageWidth = dims[0];
        imageHeight = dims[1];
        imageDepth = dims[2];

        rendererHelper.initializeMaskingAfterLoad();

//        int[] masking = getMaskSizes();
//        int topMask = masking[0];
//        int rightMask = masking[1];
//        int bottomMask = masking[2];
//        int leftMask = masking[3];
//        for (int i = 0; i < masking.length; ++i)
//            currentMask[i] = masking[i];
//
//        maskSource = new vtkImageCanvasSource2D();
//        maskSource.SetScalarTypeToUnsignedChar();
//        maskSource.SetNumberOfScalarComponents(1);
//        // maskSource.SetExtent(0, imageWidth-1, 0, imageHeight-1, 0, imageDepth-1);
//        maskSource.SetExtent(0, imageWidth - 1, 0, imageHeight - 1, 0, 0);
//        // Initialize the mask to black which masks out the image
//        maskSource.SetDrawColor(0.0, 0.0, 0.0, 0.0);
//        maskSource.FillBox(0, imageWidth - 1, 0, imageHeight - 1);
//        // Create a square inside mask which passes through the image.
//        maskSource.SetDrawColor(255.0, 255.0, 255.0, 255.0);
//        maskSource.FillBox(leftMask, imageWidth - 1 - rightMask, bottomMask, imageHeight - 1 - topMask);
//        maskSource.Update();
//
//        for (int k = 0; k < getImageDepth(); k++)
//        {
//            footprint[k] = new vtkPolyData();
//        }
//
//        shiftedFootprint[0] = new vtkPolyData();
//        textureCoords = new vtkFloatArray();
//        normalsFilter = new vtkPolyDataNormals();

        // See notes on redmine issue #2046. Changing this check to require
        // explicitly that the FITS file name be null AND the PNG file name
        // not null
        if (getFitFileFullPath() == null && getPngFileFullPath() != null)
        {
            double[] scalarRange = getRawImage().GetScalarRange();
            minValue[0] = (float) scalarRange[0];
            maxValue[0] = (float) scalarRange[1];
        }
        setDisplayedImageRange(null);
        setOfflimbImageRange(null);
        rendererHelper.getFootprint().updatePointing(this);
    }

    //////////////////
    // Other methods
    //////////////////
    public String getPrerenderingFileNameBase()
    {
        String imageName = getKey().getName();

        IImagingInstrument instrument = getKey().getInstrument();
        String topPath = instrument != null ? smallBodyModel.serverPath("", instrument.getInstrumentName()) : FileCache.instance().getFile(imageName).getParent();
        if (instrument == null)
        {
        	String cachePath = FileCache.instance().getFile("").getParentFile().getParentFile().getAbsolutePath().replace("\\", "\\\\");
        	topPath = topPath.split(cachePath)[1];
        }
        String result = SAFE_URL_PATHS.getString(topPath, "support", key.getSource().name(), FilenameUtils.getBaseName(imageName) + "_" + smallBodyModel.getModelResolution());
        return result;
    }

    /**
     * Return the multispectral image's spectrum region in pixel space.
     *
     * @return array describing region over which the spectrum is calculated.
     */
    public double[][] getSpectrumRegion()
    {
        return null;
    }

    public void setPickedPosition(double[] position)
    {
        // System.out.println("PerspectiveImage.setPickedPosition(): " + position[0] +
        // ", " + position[1] + ", " + position[2]);
        double[] pixelPosition = getPixelFromPoint(position);
        double[][] region = { { pixelPosition[0], pixelPosition[1] } };
        setSpectrumRegion(region);
    }

    public double[] getPixelFromPoint(double[] pt)
    {
        double[] uv = new double[2];
        Frustum frustum = getFrustum();
        frustum.computeTextureCoordinatesFromPoint(pt, getImageWidth(), getImageHeight(), uv, false);

        double[] pixel = new double[2];
        pixel[0] = uv[0] * getImageHeight();
        pixel[1] = uv[1] * getImageWidth();

        return pixel;
    }

    public double getPixelDistance(double[] pt1, double[] pt2)
    {
        double[] pixel1 = getPixelFromPoint(pt1);
        double[] pixel2 = getPixelFromPoint(pt2);

        return MathUtil.distanceBetween(pixel1, pixel2);
    }

    /**
     * Get filter as an integer id. Return -1 if no filter is available.
     *
     * @return
     */
    public int getFilter()
    {
        return -1;
    }

    /**
     * Get filter name as string. By default cast filter id to string. Return null
     * if filter id is negative.
     *
     * @return
     */
    public String getFilterName()
    {
        int filter = getFilter();
        if (filter < 0)
            return null;
        else
            return String.valueOf(filter);
    }

    /**
     * Return the camera id. We assign an integer id to each camera. For example, if
     * there are 2 cameras on the spacecraft, return either 1 or 2. If there are 2
     * spacecrafts each with a single camera, then also return either 1 or 2. Return
     * -1 if camera is not available.
     *
     * @return
     */
    public int getCamera()
    {
        return -1;
    }

    /**
     * Get camera name as string. By default cast camera id to string. Return null
     * if camera id is negative.
     *
     * @return
     */
    public String getCameraName()
    {
        int camera = getCamera();
        if (camera < 0)
            return null;
        else
            return String.valueOf(camera);
    }

    public int getImageWidth()
    {
        return imageWidth;
    }

    public int getImageHeight()
    {
        return imageHeight;
    }

    public int getImageDepth()
    {
        return imageDepth;
    }

    public void setCurrentSlice(int slice)
    {
        this.currentSlice = slice;
    }

    public int getCurrentSlice()
    {
        return currentSlice;
    }

    public int getDefaultSlice()
    {
        return 0;
    }

    public String getCurrentBand()
    {
        return Integer.toString(currentSlice);
    }

    public boolean containsLimb()
    {
        // TODO Speed this up: Determine if there is a limb without computing the entire
        // backplane.

        float[] bp = backplanesHelper.generateBackplanes(true);
        if (bp == null)
            return true;
        else
            return false;
    }

    public String getStartTime()
    {
        return startTime;
    }

    public String getStopTime()
    {
        return stopTime;
    }

    public double getSpacecraftDistance()
    {
        return MathUtil.vnorm(getSpacecraftPositionAdjusted()[currentSlice]);
    }

    public void getCameraOrientation(double[] spacecraftPosition, double[] focalPoint, double[] upVector)
    {

        for (int i = 0; i < 3; ++i)
        {
            spacecraftPosition[i] = getSpacecraftPositionAdjusted()[currentSlice][i];
            upVector[i] = getUpVectorAdjusted()[currentSlice][i];
        }

        // Normalize the direction vector
        double[] direction = new double[3];
        MathUtil.unorm(getBoresightDirectionAdjusted()[currentSlice], direction);

        int cellId = smallBodyModel.computeRayIntersection(spacecraftPosition, direction, focalPoint);

        if (cellId < 0)
        {
            BoundingBox bb = new BoundingBox(rendererHelper.getFootprint().footprint[currentSlice].GetBounds());
            double[] centerPoint = bb.getCenterPoint();
            // double[] centerPoint = footprint[currentSlice].GetPoint(0);
            double distanceToCenter = MathUtil.distanceBetween(spacecraftPosition, centerPoint);

            focalPoint[0] = spacecraftPosition[0] + distanceToCenter * direction[0];
            focalPoint[1] = spacecraftPosition[1] + distanceToCenter * direction[1];
            focalPoint[2] = spacecraftPosition[2] + distanceToCenter * direction[2];
        }
    }

    /**
     * Same as previous but return a (4 element) quaternion instead. First element
     * is the scalar followed by the 3 element vector. Also returns a rotation
     * matrix.
     *
     * @param spacecraftPosition
     * @param quaternion
     * @return Rotation matrix
     */
    public Rotation getCameraOrientation(double[] spacecraftPosition, double[] quaternion)
    {
        double[] cx = getUpVectorAdjusted()[currentSlice];
        double[] cz = new double[3];
        MathUtil.unorm(getBoresightDirectionAdjusted()[currentSlice], cz);

        double[] cy = new double[3];
        MathUtil.vcrss(cz, cx, cy);

        double[][] m = {
                { cx[0], cx[1], cx[2] },
                { cy[0], cy[1], cy[2] },
                { cz[0], cz[1], cz[2] }
        };

        Rotation rotation = new Rotation(m, 1.0e-6);

        for (int i = 0; i < 3; ++i)
            spacecraftPosition[i] = getSpacecraftPositionAdjusted()[currentSlice][i];

        quaternion[0] = rotation.getQ0();
        quaternion[1] = rotation.getQ1();
        quaternion[2] = rotation.getQ2();
        quaternion[3] = rotation.getQ3();

        return rotation;
    }


    /**
     * Get the maximum FOV angle in degrees of the image (the max of either the
     * horizontal or vetical FOV). I.e., return the angular separation in degrees
     * between two corners of the frustum where the two corners are both on the
     * longer side.
     *
     * @return
     */
    public double getMaxFovAngle()
    {
        return Math.max(getHorizontalFovAngle(), getVerticalFovAngle());
    }

    public double getHorizontalFovAngle()
    {
        double fovHoriz = MathUtil.vsep(getFrustum1Adjusted()[currentSlice], getFrustum3Adjusted()[currentSlice]) * 180.0 / Math.PI;
        return fovHoriz;
    }

    public double getVerticalFovAngle()
    {
        double fovVert = MathUtil.vsep(getFrustum1Adjusted()[currentSlice], getFrustum2Adjusted()[currentSlice]) * 180.0 / Math.PI;
        return fovVert;
    }

    public double[] getSpacecraftPosition()
    {
        return getSpacecraftPositionAdjusted()[currentSlice];
    }

    public double[] getSunPosition()
    {
        return getSunPositionAdjusted()[currentSlice];
    }

    public double[] getSunVector()
    {
        double[] result = new double[3];
        MathUtil.vhat(getSunPositionAdjusted()[currentSlice], result);
        return result;
    }

    public double[] getBoresightDirection()
    {
        return getBoresightDirectionAdjusted()[currentSlice];
    }

    public double[] getUpVector()
    {
        return getUpVectorAdjusted()[currentSlice];
    }

    public double[] getPixelDirection(int sample, int line)
    {
        return getPixelDirection((double) sample, (double) line, currentSlice);
    }

    public double[] getPixelDirection(double sample, double line)
    {
        return getPixelDirection((double) sample, (double) line, currentSlice);
    }

    /**
     * Get the direction from the spacecraft of pixel with specified sample and
     * line. Note that sample is along image width and line is along image height.
     */
    public double[] getPixelDirection(double sample, double line, int slice)
    {
    	double[][] spacecraftPositionAdjusted = getSpacecraftPositionAdjusted();
    	double[][] frustum1Adjusted = getFrustum1Adjusted();
    	double[][] frustum2Adjusted = getFrustum2Adjusted();
    	double[][] frustum3Adjusted = getFrustum3Adjusted();

        double[] corner1 = {
                spacecraftPositionAdjusted[slice][0] + frustum1Adjusted[slice][0],
                spacecraftPositionAdjusted[slice][1] + frustum1Adjusted[slice][1],
                spacecraftPositionAdjusted[slice][2] + frustum1Adjusted[slice][2]
        };
        double[] corner2 = {
                spacecraftPositionAdjusted[slice][0] + frustum2Adjusted[slice][0],
                spacecraftPositionAdjusted[slice][1] + frustum2Adjusted[slice][1],
                spacecraftPositionAdjusted[slice][2] + frustum2Adjusted[slice][2]
        };
        double[] corner3 = {
                spacecraftPositionAdjusted[slice][0] + frustum3Adjusted[slice][0],
                spacecraftPositionAdjusted[slice][1] + frustum3Adjusted[slice][1],
                spacecraftPositionAdjusted[slice][2] + frustum3Adjusted[slice][2]
        };
        double[] vec12 = {
                corner2[0] - corner1[0],
                corner2[1] - corner1[1],
                corner2[2] - corner1[2]
        };
        double[] vec13 = {
                corner3[0] - corner1[0],
                corner3[1] - corner1[1],
                corner3[2] - corner1[2]
        };

        // Compute the vector on the left of the row.
        double fracHeight = ((double) line / (double) (imageHeight - 1));
        double[] left = {
                corner1[0] + fracHeight * vec13[0],
                corner1[1] + fracHeight * vec13[1],
                corner1[2] + fracHeight * vec13[2]
        };

        double fracWidth = ((double) sample / (double) (imageWidth - 1));
        double[] dir = {
                left[0] + fracWidth * vec12[0],
                left[1] + fracWidth * vec12[1],
                left[2] + fracWidth * vec12[2]
        };
        dir[0] -= spacecraftPositionAdjusted[slice][0];
        dir[1] -= spacecraftPositionAdjusted[slice][1];
        dir[2] -= spacecraftPositionAdjusted[slice][2];
        MathUtil.unorm(dir, dir);

        return dir;
    }

    /**
     * Get point on surface that intersects a ray originating from spacecraft in
     * direction of pixel with specified sample and line. Note that sample is along
     * image width and line is along image height. If there is no intersect point,
     * null is returned.
     */
    public double[] getPixelSurfaceIntercept(int sample, int line)
    {
        double[] dir = getPixelDirection(sample, line);

        double[] intersectPoint = new double[3];

        int result = smallBodyModel.computeRayIntersection(getSpacecraftPositionAdjusted()[currentSlice], dir, intersectPoint);

        if (result >= 0)
            return intersectPoint;
        else
            return null;
    }

    public double getDefaultOffset()
    {
        return 3.0 * smallBodyModel.getMinShiftAmount();
    }

    public void imageAboutToBeRemoved()
    {
        setShowFrustum(false);
    }

    @Override
    public LinkedHashMap<String, String> getProperties() throws IOException
    {
        LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
        if (rendererHelper.getFootprint() == null || rendererHelper.getFootprint().getFootprint(currentSlice) == null)
            return properties;

        if (getMaxPhase() < getMinPhase())
        {
            this.computeIlluminationAngles();
            this.computePixelScale();
        }

        DecimalFormat df = new DecimalFormat("#.######");

        properties.put("Name", new File(getImageFileFullPath()).getName()); // TODO remove extension and possibly prefix
        properties.put("Start Time", getStartTime());
        properties.put("Stop Time", getStopTime());
        properties.put("Spacecraft Distance", df.format(getSpacecraftDistance()) + " km");
        properties.put("Spacecraft Position", df.format(getSpacecraftPositionAdjusted()[currentSlice][0]) + ", " + df.format(getSpacecraftPositionAdjusted()[currentSlice][1]) + ", " + df.format(getSpacecraftPositionAdjusted()[currentSlice][2]) + " km");
        double[] quaternion = new double[4];
        double[] notused = new double[4];
        getCameraOrientation(notused, quaternion);
        properties.put("Spacecraft Orientation (quaternion)", "(" + df.format(quaternion[0]) + ", [" + df.format(quaternion[1]) + ", " + df.format(quaternion[2]) + ", " + df.format(quaternion[3]) + "])");
        double[] sunVectorAdjusted = getSunVector();
        properties.put("Sun Vector", df.format(sunVectorAdjusted[0]) + ", " + df.format(sunVectorAdjusted[1]) + ", " + df.format(sunVectorAdjusted[2]));
        if (getCameraName() != null)
            properties.put("Camera", getCameraName());
        if (getFilterName() != null)
            properties.put("Filter", getFilterName());

        // Note \u00B2 is the unicode superscript 2 symbol
        String ss2 = "\u00B2";
        properties.put("Footprint Surface Area", df.format(getSurfaceArea()) + " km" + ss2);

        // Note \u00B0 is the unicode degree symbol
        String deg = "\u00B0";
        properties.put("FOV", df.format(getHorizontalFovAngle()) + deg + " x " + df.format(getVerticalFovAngle()) + deg);

        properties.put("Minimum Incidence", df.format(getMinIncidence()) + deg);
        properties.put("Maximum Incidence", df.format(getMaxIncidence()) + deg);
        properties.put("Minimum Emission", df.format(getMinEmission()) + deg);
        properties.put("Maximum Emission", df.format(getMaxEmission()) + deg);
        properties.put("Minimum Phase", df.format(getMinPhase()) + deg);
        properties.put("Maximum Phase", df.format(getMaxPhase()) + deg);
        properties.put("Minimum Horizontal Pixel Scale", df.format(1000.0 * getMinimumHorizontalPixelScale()) + " meters/pixel");
        properties.put("Maximum Horizontal Pixel Scale", df.format(1000.0 * getMaximumHorizontalPixelScale()) + " meters/pixel");
        properties.put("Minimum Vertical Pixel Scale", df.format(1000.0 * getMinimumVerticalPixelScale()) + " meters/pixel");
        properties.put("Maximum Vertical Pixel Scale", df.format(1000.0 * getMaximumVerticalPixelScale()) + " meters/pixel");

        return properties;
    }

    public void firePropertyChange()
    {
        // with significant property changes, the offlimb plane needs to be recalculated
        offlimbPlaneHelper.calculator.loadOffLimbPlane(this, offlimbPlaneHelper.offLimbFootprintDepth);
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, this);
    }

    @Override
    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        // Get default status message
        String status = super.getClickStatusBarText(prop, cellId, pickPosition);

        // Append raw pixel value information
        status += ", Raw Value = ";
        if (getRawImage() == null)
        {
            status += "Unavailable";
        }
        else
        {
            int ip0 = (int) Math.round(pickPosition[0]);
            int ip1 = (int) Math.round(pickPosition[1]);
            if (!getRawImage().GetScalarTypeAsString().contains("char"))
            {
                float[] pixelColumn = ImageDataUtil.vtkImageDataToArray1D(getRawImage(), imageHeight - 1 - ip0, ip1);
                status += pixelColumn[currentSlice];
            }
            else
            {
                status += "N/A";
            }
        }

        return status;
    }

    // Getters/setters

    public String getStartTimeString()
    {
        return startTimeString;
    }

    public void setStartTimeString(String startTimeString)
    {
        this.startTimeString = startTimeString;
    }

    public String getStopTimeString()
    {
        return stopTimeString;
    }

    public void setStopTimeString(String stopTimeString)
    {
        this.stopTimeString = stopTimeString;
    }

    public String getScTargetPositionString()
    {
        return scTargetPositionString;
    }

    public void setScTargetPositionString(String scTargetPositionString)
    {
        this.scTargetPositionString = scTargetPositionString;
    }

    public String getTargetSunPositionString()
    {
        return targetSunPositionString;
    }

    public void setTargetSunPositionString(String targetSunPositionString)
    {
        this.targetSunPositionString = targetSunPositionString;
    }

    public String getScOrientationString()
    {
        return scOrientationString;
    }

    public void setScOrientationString(String scOrientationString)
    {
        this.scOrientationString = scOrientationString;
    }

    public Rotation getScOrientation()
    {
        return scOrientation;
    }

    public void setScOrientation(Rotation scOrientation)
    {
        this.scOrientation = scOrientation;
    }

    public double[][] getSpacecraftPositionAdjusted()
    {
        return imageOffsetCalculator.spacecraftPositionAdjusted;
    }

    public double[] getQ()
    {
        return q;
    }

    public void setQ(double[] q)
    {
        this.q = q;
    }

    public double[] getCx()
    {
        return cx;
    }

    public void setCx(double[] cx)
    {
        this.cx = cx;
    }

    public double[] getCy()
    {
        return cy;
    }

    public void setCy(double[] cy)
    {
        this.cy = cy;
    }

    public double[] getCz()
    {
        return cz;
    }

    public void setCz(double[] cz)
    {
        this.cz = cz;
    }

    public double getFocalLengthMillimeters()
    {
        return focalLengthMillimeters;
    }

    public void setFocalLengthMillimeters(double focalLengthMillimeters)
    {
        this.focalLengthMillimeters = focalLengthMillimeters;
    }

    public double getNpx()
    {
        return npx;
    }

    public void setNpx(double npx)
    {
        this.npx = npx;
    }

    public double getNln()
    {
        return nln;
    }

    public void setNln(double nln)
    {
        this.nln = nln;
    }

    public double getKmatrix00()
    {
        return kmatrix00;
    }

    public void setKmatrix00(double kmatrix00)
    {
        this.kmatrix00 = kmatrix00;
    }

    public double getKmatrix11()
    {
        return kmatrix11;
    }

    public void setKmatrix11(double kmatrix11)
    {
        this.kmatrix11 = kmatrix11;
    }

    public double[][] getFrustum1Adjusted()
    {
        return imageOffsetCalculator.frustum1Adjusted;
    }

    public double[][] getFrustum2Adjusted()
    {
        return imageOffsetCalculator.frustum2Adjusted;
    }

    public double[][] getFrustum3Adjusted()
    {
        return imageOffsetCalculator.frustum3Adjusted;
    }

    public double[][] getFrustum4Adjusted()
    {
        return imageOffsetCalculator.frustum4Adjusted;
    }

    // ******************
    // IO Delegates
    // ******************

    public void setSpacecraftPositionOriginal(double[][] spacecraftPositionOriginal)
    {
        this.spacecraftPositionOriginal = spacecraftPositionOriginal;
    }

    public void setFrustum1Original(double[][] frustum1Original)
    {
        this.frustum1Original = frustum1Original;
    }

    public void setFrustum2Original(double[][] frustum2Original)
    {
        this.frustum2Original = frustum2Original;
    }

    public void setFrustum3Original(double[][] frustum3Original)
    {
        this.frustum3Original = frustum3Original;
    }

    public void setFrustum4Original(double[][] frustum4Original)
    {
        this.frustum4Original = frustum4Original;
    }

    public void setSunPositionOriginal(double[][] sunPositionOriginal)
    {
        this.sunPositionOriginal = sunPositionOriginal;
    }

    public double[][] getSunPositionOriginal()
    {
        return sunPositionOriginal;
    }

    public double[][] getSpacecraftPositionOriginal()
    {
        return spacecraftPositionOriginal;
    }

    public double[][] getFrustum1Original()
    {
        return frustum1Original;
    }

    public double[][] getFrustum2Original()
    {
        return frustum2Original;
    }

    public double[][] getFrustum3Original()
    {
        return frustum3Original;
    }

    public double[][] getFrustum4Original()
    {
        return frustum4Original;
    }

    public double[][] getBoresightDirectionOriginal()
    {
        return boresightDirectionOriginal;
    }

    public double[][] getUpVectorOriginal()
    {
        return upVectorOriginal;
    }

    public double[] getTargetPixelCoordinates()
    {
        return imageOffsetCalculator.targetPixelCoordinates;
    }

    public void setStartTime(String startTime)
    {
        this.startTime = startTime;
    }

    public void setStopTime(String stopTime)
    {
        this.stopTime = stopTime;
    }

    public double[][] getBoresightDirectionAdjusted()
    {
        return imageOffsetCalculator.boresightDirectionAdjusted;
    }

    public double[][] getUpVectorAdjusted()
    {
        return imageOffsetCalculator.upVectorAdjusted;
    }

    public double[][] getSunPositionAdjusted()
    {
        return imageOffsetCalculator.sunPositionAdjusted;
    }

    public String getObjectName()
    {
        return objectName;
    }

    public void setObjectName(String objectName)
    {
        this.objectName = objectName;
    }

    public double[] getZoomFactor()
    {
        return imageOffsetCalculator.zoomFactor;
    }

    public double[] getRotationOffset()
    {
        return imageOffsetCalculator.rotationOffset;
    }

    public void setImageWidth(int imageWidth)
    {
        this.imageWidth = imageWidth;
    }

    public void setImageHeight(int imageHeight)
    {
        this.imageHeight = imageHeight;
    }

    public void setNumberOfLines(double numberOfLines)
    {
        this.numberOfLines = numberOfLines;
    }

    public void setImageDepth(int imageDepth)
    {
        this.imageDepth = imageDepth;
    }

    public void setNumberOfPixels(double numberOfPixels)
    {
        this.numberOfPixels = numberOfPixels;
    }

    public void setInstrumentId(String instrumentId)
    {
        this.instrumentId = instrumentId;
    }



    public void setFilterName(String filterName)
    {
        this.filterName = filterName;
    }

    public void setTargetName(String targetName)
    {
        this.targetName = targetName;
    }

    public void setImageName(String imageName)
    {
        this.imageName = imageName;
    }

//    public void setInfoFileIO(InfoFileIO infoFileIO)
//    {
//        this.infoFileIO = infoFileIO;
//    }
//
//
//    public void setSumFileIO(SumFileIO sumFileIO)
//    {
//        this.sumFileIO = sumFileIO;
//    }
//
//
//    public void setLabelFileIO(LabelFileIO labelFileIO)
//    {
//        this.labelFileIO = labelFileIO;
//    }
//
//
//    public InfoFileIO getInfoFileIO()
//    {
//        return infoFileIO;
//    }
//
//
//    public SumFileIO getSumFileIO()
//    {
//        return sumFileIO;
//    }
//
//
//    public LabelFileIO getLabelFileIO()
//    {
//        return labelFileIO;
//    }
//
//    public PerspectiveImageIO getFileIO()
//    {
//        return fileIO;
//    }

	String getModelName()
	{
		return smallBodyModel.getModelName();
	}

    public SmallBodyModel getSmallBodyModel()
    {
        return smallBodyModel;
    }

    public int getNumBackplanes()
    {
        return numBackplanes;
    }

    protected ModelManager getModelManager()
    {
        return modelManager;
    }

    @Override
    public String getImageName()
    {
        if (imageName != null)
            return imageName;
        else
            return super.getImageName();
    }

	void firePropertyChange(String propertyName, Object oldValue, Object newValue)
	{
		this.pcs.firePropertyChange(propertyName, oldValue, newValue);
	}

	public PerspectiveImageOffsetCalculator getImageOffsetCalculator()
	{
		return imageOffsetCalculator;
	}

	public PerspectiveImageOfflimbPlaneHelper getOfflimbPlaneHelper()
	{
		return offlimbPlaneHelper;
	}

	public PerspectiveImageBackplanesHelper getBackplanesHelper()
	{
		return backplanesHelper;
	}

	public PerspectiveImageRendererHelper getRendererHelper()
	{
		return rendererHelper;
	}

	///////////////////////////////
	/// Backplane delegate methods
	///////////////////////////////
	public BPMetaBuilder pds3ToXmlMeta(String pds3Fname, String outXmlFname)
	{
		return backplanesHelper.pds3ToXmlMeta(pds3Fname, outXmlFname);
	}

	public BPMetaBuilder pds4ToXmlMeta(String pds4Fname, String outXmlFname)
	{
		return backplanesHelper.pds4ToXmlMeta(pds4Fname, outXmlFname);
	}

	public BPMetaBuilder fitsToXmlMeta(File fitsFile, BPMetaBuilder metaDataBuilder) throws FitsException
	{
		return backplanesHelper.fitsToXmlMeta(fitsFile, metaDataBuilder);
	}

	public BackPlanesXml metaToXmlDoc(BackPlanesXmlMeta metaData, String xmlTemplate)
	{
		return backplanesHelper.metaToXmlDoc(metaData, xmlTemplate);
	}

	public void generateBackplanesLabel(File imgName, File lblFileName) throws IOException
	{
		backplanesHelper.generateBackplanesLabel(imgName, lblFileName);
	}

	public void appendWithPadding(StringBuffer strbuf, String str)
	{
		backplanesHelper.appendWithPadding(strbuf, str);
	}

	public float[] generateBackplanes()
	{
		return backplanesHelper.generateBackplanes();
	}

	public int index(int i, int j, int k)
	{
		return backplanesHelper.index(i, j, k);
	}

	///////////////////////////////////////
	/// Offset calculator delegate methods
	///////////////////////////////////////
	public void moveLineOffsetBy(double offset)
	{
		imageOffsetCalculator.moveLineOffsetBy(offset);
	}

	public void moveSampleOffsetBy(double offset)
	{
		imageOffsetCalculator.moveSampleOffsetBy(offset);
	}

	public void moveRotationAngleBy(double offset)
	{
		imageOffsetCalculator.moveRotationAngleBy(offset);
	}

	public void moveZoomFactorBy(double offset)
	{
		imageOffsetCalculator.moveZoomFactorBy(offset);
	}

	public void setTargetPixelCoordinates(double[] frustumCenterPixel)
	{
		imageOffsetCalculator.setTargetPixelCoordinates(frustumCenterPixel);
	}

	private void copySpacecraftState()
	{
		imageOffsetCalculator.copySpacecraftState();
	}

	public void setLineOffset(double offset)
	{
		imageOffsetCalculator.setLineOffset(offset);
	}

	public void setSampleOffset(double offset)
	{
		imageOffsetCalculator.setSampleOffset(offset);
	}

	public void setRotationOffset(double offset)
	{
		imageOffsetCalculator.setRotationOffset(offset);
	}

	public void setYawOffset(double offset)
	{
		imageOffsetCalculator.setPitchOffset(offset);
	}

	public void setPitchOffset(double offset)
	{
		imageOffsetCalculator.setPitchOffset(offset);
	}

	public void setZoomFactor(double offset)
	{
		imageOffsetCalculator.setZoomFactor(offset);
	}

	/////////////////////////////
	/// Offlimb delegate methods
	/////////////////////////////
	public void setOffLimbBoundaryVisibility(boolean visible)
	{
		offlimbPlaneHelper.setOffLimbBoundaryVisibility(visible);
	}

	public void setOffLimbFootprintVisibility(boolean visible)
	{
		offlimbPlaneHelper.setOffLimbFootprintVisibility(visible);
	}

	public boolean offLimbFootprintIsVisible()
	{
		return offlimbPlaneHelper.offLimbFootprintIsVisible();
	}

	public void setOffLimbPlaneDepth(double footprintDepth)
	{
		offlimbPlaneHelper.setOffLimbPlaneDepth(footprintDepth);
	}

	public void setOffLimbFootprintAlpha(double alpha)
	{
		offlimbPlaneHelper.setOffLimbFootprintAlpha(alpha);
	}

	public IntensityRange getOffLimbDisplayedRange()
	{
		return offlimbPlaneHelper.getOffLimbDisplayedRange();
	}

	public boolean isContrastSynced()
	{
		return offlimbPlaneHelper.isContrastSynced();
	}

	public Color getOfflimbBoundaryColor()
	{
		return offlimbPlaneHelper.getOfflimbBoundaryColor();
	}

	public void setOfflimbBoundaryColor(Color color)
	{
		offlimbPlaneHelper.setOfflimbBoundaryColor(color);
	}

	public void setOfflimbImageRange(IntensityRange intensityRange)
	{
		offlimbPlaneHelper.setOfflimbImageRange(intensityRange);
	}

	public void setContrastSynced(boolean selected)
	{
		offlimbPlaneHelper.setContrastSynced(selected);
	}

	public vtkTexture getOffLimbTexture()
	{
		return offlimbPlaneHelper.getOffLimbTexture();
	}

	/////////////////////////////////////
	/// Renderer Helper delegate methods
	/////////////////////////////////////
	public void Delete()
	{
		rendererHelper.Delete();
	}

	@Override
	public vtkPolyData getUnshiftedFootprint()
	{
		return rendererHelper.getFootprint().getUnshiftedFootprint();
	}

	@Override
	public vtkPolyData getShiftedFootprint()
	{
		return rendererHelper.getFootprint().getShiftedFootprint();
	}

	public void calculateFrustum()
	{
		rendererHelper.getFrustum().calculateFrustum();
	}



	public int getNumberOfComponentsOfOriginalImage()
	{
		return rendererHelper.getNumberOfComponentsOfOriginalImage();
	}

	public void loadFootprint()
	{
		rendererHelper.getFootprint().loadFootprint();
	}

	public boolean useDefaultFootprint()
	{
		return rendererHelper.getFootprint().useDefaultFootprint();
	}

	public void setUseDefaultFootprint(boolean useDefaultFootprint)
	{
		rendererHelper.getFootprint().setUseDefaultFootprint(useDefaultFootprint);
	}

	public void setSimulateLighting(boolean b)
	{
		rendererHelper.setSimulateLighting(b);
	}

	public boolean isSimulatingLighingOn()
	{
		return rendererHelper.isSimulatingLighingOn();
	}

	public void setShowFrustum(boolean b)
	{
		rendererHelper.getFrustum().setShowFrustum(b);
	}

	public boolean isFrustumShowing()
	{
		return rendererHelper.getFrustum().isFrustumShowing();
	}

	public double getMaxFrustumDepth(int slice)
	{
		return rendererHelper.getMaxFrustumDepth(slice);
	}

	public double getMinFrustumDepth(int slice)
	{
		return rendererHelper.getMinFrustumDepth(slice);
	}

	public void setDisplayedImageRange(IntensityRange range)
	{
		rendererHelper.setDisplayedImageRange(range);
	}

	public vtkImageData getRawImage()
	{
		return rendererHelper.getRawImage();
	}

	public vtkImageData getDisplayedImage()
	{
		return rendererHelper.getDisplayedImage();
	}

	public vtkTexture getTexture()
	{
		return rendererHelper.getTexture();
	}

	public List<vtkProp> getProps()
	{
		return rendererHelper.getProps();
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		rendererHelper.propertyChange(evt);
	}

	public double getMinIncidence()
	{
		return rendererHelper.getMinIncidence();
	}

	public double getMaxIncidence()
	{
		return rendererHelper.getMaxIncidence();
	}

	public double getMinEmission()
	{
		return rendererHelper.getMinEmission();
	}

	public double getMaxEmission()
	{
		return rendererHelper.getMaxEmission();
	}

	public double getMinPhase()
	{
		return rendererHelper.getMinPhase();
	}

	public double getMaxPhase()
	{
		return rendererHelper.getMaxPhase();
	}

	public IntensityRange getDisplayedRange()
	{
		return rendererHelper.getDisplayedRange();
	}

	public IntensityRange getDisplayedRange(int slice)
	{
		return rendererHelper.getDisplayedRange(slice);
	}

	public vtkPolyData getFootprint(int defaultSlice)
	{
		return rendererHelper.getFootprint().getFootprint(defaultSlice);
	}

	public Frustum getFrustum(int slice)
	{
		return rendererHelper.getFrustum().getFrustum(slice);
	}

	public Frustum getFrustum()
	{
		return rendererHelper.getFrustum().getFrustum();
	}

	public void setRawImage(vtkImageData rawImage)
	{
		rendererHelper.setRawImage(rawImage);
	}

	public void setMaxFrustumDepth(int slice, double value)
	{
		rendererHelper.getFrustum().setMaxFrustumDepth(slice, value);
	}

	public void setMinFrustumDepth(int slice, double value)
	{
		rendererHelper.getFrustum().setMinFrustumDepth(slice, value);
	}

	public double getMinimumHorizontalPixelScale()
	{
		return rendererHelper.minHorizontalPixelScale;
	}

	public double getMaximumHorizontalPixelScale()
	{
		return rendererHelper.maxHorizontalPixelScale;
	}

	public double getMeanHorizontalPixelScale()
	{
		return rendererHelper.meanHorizontalPixelScale;
	}

	public double getMinimumVerticalPixelScale()
	{
		return rendererHelper.minVerticalPixelScale;
	}

	public double getMaximumVerticalPixelScale()
	{
		return rendererHelper.maxVerticalPixelScale;
	}

	public double getMeanVerticalPixelScale()
	{
		return rendererHelper.meanVerticalPixelScale;
	}

	public static void setGenerateFootprint(boolean b)
	{
		PerspectiveImageFootprint.setGenerateFootprint(b);
	}

	public vtkImageData getImageWithDisplayedRange(IntensityRange range, boolean offlimb)
	{
		return rendererHelper.getImageWithDisplayedRange(range, offlimb);
	}

	private vtkPolyData checkForExistingFootprint()
	{
		return rendererHelper.getFootprint().checkForExistingFootprint();
	}

//	private vtkPolyData generateBoundary()
//	{
//		return rendererHelper.getFootprint().generateBoundary();
//	}

	private void computeCellNormals()
	{
		rendererHelper.computeCellNormals();
	}

	public double[] computeIlluminationAnglesAtPoint(double[] pt, double[] normal)
	{
		return rendererHelper.computeIlluminationAnglesAtPoint(pt, normal);
	}

	public void computeIlluminationAngles()
	{
		rendererHelper.computeIlluminationAngles();
	}

	public void computePixelScale()
	{
		rendererHelper.computePixelScale();
	}

	public double getOpacity()
    {
        return rendererHelper.getOpacity();
    }

    public void setOpacity(double imageOpacity)
    {
    	rendererHelper.setOpacity(imageOpacity);
    }

	public void setCurrentMask(int[] masking)
	{
		rendererHelper.setCurrentMask(masking);
	}

	public int[] getCurrentMask()
	{
		return rendererHelper.getCurrentMask();
	}

	protected void processRawImage(vtkImageData rawImage)
	{
		rendererHelper.processRawImage(rawImage);
	}

    protected vtkImageData createRawImage(int height, int width, int depth, float[][] array2D, float[][][] array3D)
    {
    	return rendererHelper.createRawImage(height, width, depth, array2D, array3D);
    }

    public vtkImageData createRawImage(int height, int width, int depth, boolean transpose, float[][] array2D, float[][][] array3D)
    {
    	return rendererHelper.createRawImage(height, width, depth, transpose, array2D, array3D);
    }

	public boolean[] getFootprintGenerated()
	{
		return rendererHelper.getFootprintGenerated();
	}

	public void setFootprintGenerated(boolean footprintGenerated)
	{
		rendererHelper.setFootprintGenerated(footprintGenerated);
	}

	public void setFootprintGenerated(boolean footprintGenerated, int slice)
	{
		rendererHelper.setFootprintGenerated(footprintGenerated, slice);
	}

	public boolean isNormalsGenerated()
	{
		return rendererHelper.isNormalsGenerated();
	}

	public static boolean isGenerateFootprint()
	{
		return PerspectiveImageRendererHelper.isGenerateFootprint();
	}

	public void setNormalsGenerated(boolean normalsGenerated)
	{
		rendererHelper.setNormalsGenerated(normalsGenerated);
	}

	public double getSurfaceArea()
	{
		return rendererHelper.getSurfaceArea();
	}

	public void setVisible(boolean b)
	{
		rendererHelper.setVisible(b);
		super.setVisible(b);
	}

	@Override
	public void setBoundaryVisibility(boolean isVisible)
	{
		super.setBoundaryVisibility(isVisible);
		rendererHelper.getFootprint().setBoundaryVisible(isVisible);
		firePropertyChange(Properties.MODEL_CHANGED, null, this);
	}

	@Override
	public void setBoundaryColor(Color color)
	{
		super.setBoundaryColor(color);
		rendererHelper.getFootprint().setBoundaryColor(color);
		firePropertyChange(Properties.MODEL_CHANGED, null, this);
	}

}
