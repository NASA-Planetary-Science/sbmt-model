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
    private final boolean transposeFITSData;
    Stopwatch sw;

    PerspectiveImageBackplanesHelper backplanesHelper;
    PerspectiveImageOffsetCalculator imageOffsetCalculator;
    PerspectiveImageOfflimbPlaneHelper offlimbPlaneHelper;
    PerspectiveImageRendererHelper rendererHelper;


    public PerspectiveImage( //
            ImageKeyInterface key, //
            SmallBodyModel smallBodyModel, //
            boolean loadPointingOnly, //
            boolean transposeData) throws FitsException, IOException //
    {
        this(key, smallBodyModel, null, loadPointingOnly, 0, transposeData);
    }

    public PerspectiveImage( //
            ImageKeyInterface key, //
            SmallBodyModel smallBodyModel, //
            boolean loadPointingOnly) throws FitsException, IOException //
    {
        this(key, smallBodyModel, null, loadPointingOnly, 0, true);
    }

    public PerspectiveImage( //
            ImageKeyInterface key, //
            SmallBodyModel smallBodyModel, //
            boolean loadPointingOnly, //
            int currentSlice) throws FitsException, IOException //
    {
        this(key, smallBodyModel, null, loadPointingOnly, currentSlice, true);
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
        this(key, smallBodyModel, modelManager, loadPointingOnly, 0, true);
    }

    /**
     * If loadPointingOnly is true then only pointing information about this
     * image will be downloaded/loaded. The image itself will not be loaded.
     * Used by ImageBoundary to get pointing info.
     */
    public PerspectiveImage( //
            ImageKeyInterface key, //
            SmallBodyModel smallBodyModel, //
            ModelManager modelManager, //
            boolean loadPointingOnly, //
            int currentSlice) throws FitsException, IOException //
    {
        this(key, smallBodyModel, modelManager, loadPointingOnly, currentSlice, true);
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
            int currentSlice, //
            boolean transposeData) throws FitsException, IOException //
    {
        super(key);
        this.currentSlice = currentSlice;
        this.smallBodyModel = smallBodyModel;
        this.modelManager = modelManager;
        this.loadPointingOnly = loadPointingOnly;

        this.transposeFITSData = transposeData;

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

        rendererHelper.maxFrustumDepth = new double[imageDepth];
        rendererHelper.minFrustumDepth = new double[imageDepth];
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
        rendererHelper.frusta = new Frustum[nslices];
        rendererHelper.footprint = new vtkPolyData[nslices];
        rendererHelper.footprintGenerated = new boolean[nslices];
    }

    public void resetSpacecraftState()
    {
        copySpacecraftState();
        int nslices = getImageDepth();
        for (int i = 0; i < nslices; i++)
        {
            rendererHelper.frusta[i] = null;
            rendererHelper.footprintGenerated[i] = false;
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
            imageFile = imageFile.substring(imageFile.indexOf("file://") + 7);
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
                    byte[][] arrayB = ((byte[][][]) data)[1];

                    // Override the default setup used for other 3-d images.
                    fitsDepth = 1;
                    fitsHeight = arrayB.length;
                    fitsWidth = arrayB[0].length;

                    array2D = new float[fitsHeight][fitsWidth];

                    for (int i = 0; i < fitsHeight; ++i)
                        for (int j = 0; j < fitsWidth; ++j)
                        {
                            array2D[i][j] = arrayB[i][j];
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
        setRawImage(createRawImage(fitsHeight, fitsWidth, fitsDepth, transposeFITSData, array2D, array3D));
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
        ObjUtil.writePolyDataToObj(rendererHelper.shiftedFootprint[0], getDisplayedImage(), footprintFilePath, headerString);
        // write footprint boundary to obj lines
        vtkFeatureEdges edgeFilter = new vtkFeatureEdges();
        edgeFilter.SetInputData(rendererHelper.shiftedFootprint[0]);
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
        ObjUtil.writePolyDataToObj(rendererHelper.frustumPolyData, frustumFilePath, frustumFileHeader);
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
    }

    //////////////////
    // Other methods
    //////////////////
    public String getPrerenderingFileNameBase()
    {
        String imageName = getKey().getName();

        // TODO this needs work. The location will be in general different depending on
        // whether the image is in the cache or a custom image.
        // For now, check whether the instrument is defined. Cached images will have
        // this, custom images will not. In the custom case, just
        // look up one level.
        IImagingInstrument instrument = getKey().getInstrument();
        String topPath = instrument != null ? smallBodyModel.serverPath("", instrument.getInstrumentName()) : FileCache.instance().getFile(imageName).getParent();
        if (instrument == null)
        {
        	topPath = topPath.split(".sbmt")[1];
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
            BoundingBox bb = new BoundingBox(rendererHelper.footprint[currentSlice].GetBounds());
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
        if (rendererHelper.getFootprint() == null || rendererHelper.getFootprint(currentSlice) == null)
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
        properties.put("Maximum Emission", df.format(getMaxIncidence()) + deg);
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
		return rendererHelper.getUnshiftedFootprint();
	}

	@Override
	public vtkPolyData getShiftedFootprint()
	{
		return rendererHelper.getShiftedFootprint();
	}

	public void calculateFrustum()
	{
		rendererHelper.calculateFrustum();
	}



	public int getNumberOfComponentsOfOriginalImage()
	{
		return rendererHelper.getNumberOfComponentsOfOriginalImage();
	}

	public void loadFootprint()
	{
		rendererHelper.loadFootprint();
	}

	public boolean useDefaultFootprint()
	{
		return rendererHelper.useDefaultFootprint();
	}

	public void setUseDefaultFootprint(boolean useDefaultFootprint)
	{
		rendererHelper.setUseDefaultFootprint(useDefaultFootprint);
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
		rendererHelper.setShowFrustum(b);
	}

	public boolean isFrustumShowing()
	{
		return rendererHelper.isFrustumShowing();
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
		return rendererHelper.getFootprint(defaultSlice);
	}

	public Frustum getFrustum(int slice)
	{
		return rendererHelper.getFrustum(slice);
	}

	public Frustum getFrustum()
	{
		return rendererHelper.getFrustum();
	}

	public void setRawImage(vtkImageData rawImage)
	{
		rendererHelper.setRawImage(rawImage);
	}

	public void setMaxFrustumDepth(int slice, double value)
	{
		rendererHelper.setMaxFrustumDepth(slice, value);
	}

	public void setMinFrustumDepth(int slice, double value)
	{
		rendererHelper.setMinFrustumDepth(slice, value);
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
		PerspectiveImageRendererHelper.setGenerateFootprint(b);
	}

	public vtkImageData getImageWithDisplayedRange(IntensityRange range, boolean offlimb)
	{
		return rendererHelper.getImageWithDisplayedRange(range, offlimb);
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

}

//Moving currently commented out sections that are handled with helper classes to down here for now

///////////////////////
// Rendering properties
///////////////////////
//protected vtkImageData rawImage;
//private vtkImageData displayedImage;
//private boolean useDefaultFootprint = true;
//private vtkPolyData[] footprint = new vtkPolyData[1];
//boolean[] footprintGenerated = new boolean[1];
//private final vtkPolyData[] shiftedFootprint = new vtkPolyData[1];
//private vtkActor footprintActor;
//private List<vtkProp> footprintActors = new ArrayList<vtkProp>();
//vtkPolyData frustumPolyData;
//private vtkActor frustumActor;
//private vtkPolyDataNormals normalsFilter;
//private vtkFloatArray textureCoords;
//private boolean normalsGenerated = false;
//private double minIncidence = Double.MAX_VALUE;
//private double maxIncidence = -Double.MAX_VALUE;
//private double minEmission = Double.MAX_VALUE;
//private double maxEmission = -Double.MAX_VALUE;
//private double minPhase = Double.MAX_VALUE;
//private double maxPhase = -Double.MAX_VALUE;
//private double minHorizontalPixelScale = Double.MAX_VALUE;
//private double maxHorizontalPixelScale = -Double.MAX_VALUE;
//private double meanHorizontalPixelScale = 0.0;
//private double minVerticalPixelScale = Double.MAX_VALUE;
//private double maxVerticalPixelScale = -Double.MAX_VALUE;
//private double meanVerticalPixelScale = 0.0;
//
//// Always use accessors to use this field -- even within this class!
//private IntensityRange[] displayedRange = null;
//private double imageOpacity = 1.0;
//protected vtkTexture imageTexture;
//
//// If true, then the footprint is generated by intersecting a frustum with the
//// asteroid.
//// This setting is used when generating the files on the server.
//// If false, then the footprint is downloaded from the server. This setting is
//// used by the GUI.
//private static boolean generateFootprint = true;
//private int[] currentMask = new int[4];
//
//Frustum[] frusta = new Frustum[1];
//
//private boolean showFrustum = false;
//private boolean simulateLighting = false;
//private vtkImageCanvasSource2D maskSource;
//public double[] maxFrustumDepth;
//public double[] minFrustumDepth;

////////////
// Offset properties
///////////
//protected double[][] spacecraftPositionAdjusted = new double[1][3];
//protected double[][] frustum1Adjusted = new double[1][3];
//protected double[][] frustum2Adjusted = new double[1][3];
//protected double[][] frustum3Adjusted = new double[1][3];
//protected double[][] frustum4Adjusted = new double[1][3];
//private double[][] boresightDirectionAdjusted = new double[1][3];
//private double[][] upVectorAdjusted = new double[1][3];
//private double[][] sunPositionAdjusted = new double[1][3];
//
//// location in pixel coordinates of the target origin for the adjusted frustum
//private double[] targetPixelCoordinates = { Double.MAX_VALUE, Double.MAX_VALUE };

//// offset in world coordinates of the adjusted frustum from the loaded frustum
//// private double[] offsetPixelCoordinates = { Double.MAX_VALUE,
//// Double.MAX_VALUE };
//
//private double[] zoomFactor = { 1.0 };
//
//private double[] rotationOffset = { 0.0 };
//private double[] pitchOffset = { 0.0 };
//private double[] yawOffset = { 0.0 };
//private double sampleOffset = 0.0;
//private double lineOffset = 0.0;
//
//// apply all frame adjustments if true
//private boolean[] applyFrameAdjustments = { true };

/////////////////////////
//// off-limb images properties
////////////////////////
//vtkPolyData offLimbPlane = null;
//private vtkActor offLimbActor;
//private vtkTexture offLimbTexture;
//vtkPolyData offLimbBoundary = null;
//private vtkActor offLimbBoundaryActor;
//double offLimbFootprintDepth;
//private boolean offLimbVisibility;
//private boolean offLimbBoundaryVisibility;
//OffLimbPlaneCalculator calculator = new OffLimbPlaneCalculator();
// Always use accessors to use this field -- even within this class!
//private IntensityRange offLimbDisplayedRange = null;
//private boolean contrastSynced = false; // by default, the contrast of offlimb is not synced with on limb
//private Color offLimbBoundaryColor = Color.RED; // default


////////////////////////////////
// Image offset methods
////////////////////////////////


//private void copySpacecraftState()
//{
//  int nslices = getImageDepth();
//  for (int i = 0; i < nslices; i++)
//  {
//      spacecraftPositionAdjusted = MathUtil.copy(spacecraftPositionOriginal);
//      frustum1Adjusted = MathUtil.copy(frustum1Original);
//      frustum2Adjusted = MathUtil.copy(frustum2Original);
//      frustum3Adjusted = MathUtil.copy(frustum3Original);
//      frustum4Adjusted = MathUtil.copy(frustum4Original);
//      boresightDirectionAdjusted = MathUtil.copy(boresightDirectionOriginal);
//      upVectorAdjusted = MathUtil.copy(upVectorOriginal);
//      sunPositionAdjusted = MathUtil.copy(sunPositionOriginal);
//  }
//}

//public void setTargetPixelCoordinates(double[] frustumCenterPixel)
//{
//    // System.out.println("setFrustumOffset(): " + frustumCenterPixel[1] + " " +
//    // frustumCenterPixel[0]);
//
//    this.targetPixelCoordinates[0] = frustumCenterPixel[0];
//    this.targetPixelCoordinates[1] = frustumCenterPixel[1];
//    setApplyFrameAdjustments(true);
//}

// public void setPixelOffset(double[] pixelOffset)
// {
//// System.out.println("setFrustumOffset(): " + frustumCenterPixel[1] + " " +
// frustumCenterPixel[0]);
//
// this.offsetPixelCoordinates[0] = pixelOffset[0];
// this.offsetPixelCoordinates[1] = pixelOffset[1];
//
// updateFrameAdjustments();
//
// loadFootprint();
// calculateFrustum();
// saveImageInfo();
// }

//public void setLineOffset(double offset)
//{
//    lineOffset = offset;
//    setApplyFrameAdjustments(true);
//}
//
//public void setSampleOffset(double offset)
//{
//    sampleOffset = offset;
//    setApplyFrameAdjustments(true);
//}
//
//public void setRotationOffset(double offset)
//{
//    // System.out.println("setRotationOffset(): " + offset);
//
//    if (rotationOffset == null)
//        rotationOffset = new double[1];
//
//    rotationOffset[0] = offset;
//    setApplyFrameAdjustments(true);
//}
//
//public void setYawOffset(double offset)
//{
//    // System.out.println("setRotationOffset(): " + offset);
//
//    if (yawOffset == null)
//        yawOffset = new double[1];
//
//    yawOffset[0] = offset;
//    setApplyFrameAdjustments(true);
//}
//
//public void setPitchOffset(double offset)
//{
//    // System.out.println("setRotationOffset(): " + offset);
//
//    if (pitchOffset == null)
//        pitchOffset = new double[1];
//
//    pitchOffset[0] = offset;
//    setApplyFrameAdjustments(true);
//}
//
//public void setZoomFactor(double offset)
//{
//    // System.out.println("setZoomFactor(): " + offset);
//
//    if (zoomFactor == null)
//    {
//        zoomFactor = new double[1];
//        zoomFactor[0] = 1.0;
//    }
//
//    zoomFactor[0] = offset;
//    setApplyFrameAdjustments(true);
//}

//public void setApplyFrameAdjustments(boolean state)
//{
//    // System.out.println("setApplyFrameAdjustments(): " + state);
//    applyFrameAdjustments[0] = state;
//    updateFrameAdjustments();
//    loadFootprint();
//    calculateFrustum();
//    saveImageInfo();
//}
//
//public boolean getApplyFramedAdjustments()
//{
//    return applyFrameAdjustments[0];
//}

//private void updateFrameAdjustments()
//{
//    // adjust wrt the original spacecraft pointing direction, not the previous
//    // adjusted one
//    copySpacecraftState();
//
//    if (applyFrameAdjustments[0])
//    {
//        if (targetPixelCoordinates[0] != Double.MAX_VALUE && targetPixelCoordinates[1] != Double.MAX_VALUE)
//        {
//            int height = getImageHeight();
//            double line = height - 1 - targetPixelCoordinates[0];
//            double sample = targetPixelCoordinates[1];
//
//            double[] newTargetPixelDirection = getPixelDirection(sample, line);
//            rotateTargetPixelDirectionToLocalOrigin(newTargetPixelDirection);
//        }
//        // else if (offsetPixelCoordinates[0] != Double.MAX_VALUE &&
//        // offsetPixelCoordinates[1] != Double.MAX_VALUE)
//        // {
//        // int height = getImageHeight();
//        // int width = getImageWidth();
//        // double line = height - 1 - offsetPixelCoordinates[0];
//        // double sample = offsetPixelCoordinates[1];
//        //
//        // double[] newOffsetPixelDirection = getPixelDirection(sample, line);
//        // rotateBoresightTo(newOffsetPixelDirection);
//        // }
//
//        if (sampleOffset != 0 || lineOffset != 0)
//        	translateSpacecraftInImagePlane(sampleOffset, lineOffset);
//        else
//        	translateSpacecraftInImagePlane(0, 0);
//
////        if (yawOffset[0] != 0.0)
////        {
////        	rotateFrameAboutYawAxis(yawOffset[0]);
////        }
////
////        if (pitchOffset[0] != 0.0)
////        {
////        	rotateFrameAboutPitchAxis(pitchOffset[0]);
////
////        }
//
//        if (rotationOffset[0] != 0.0)
//        {
//            rotateFrameAboutTarget(rotationOffset[0]);
//        }
//        if (zoomFactor[0] != 1.0)
//        {
//            zoomFrame(zoomFactor[0]);
//        }
//    }
//
//    // int slice = getCurrentSlice();
//    int nslices = getImageDepth();
//    for (int slice = 0; slice < nslices; slice++)
//    {
//        frusta[slice] = null;
//        footprintGenerated[slice] = false;
//    }
//}
//
//private void zoomFrame(double zoomFactor)
//{
//    // System.out.println("zoomFrame(" + zoomFactor + ")");
//    // Vector3D spacecraftPositionVector = new
//    // Vector3D(spacecraftPositionOriginal[currentSlice]);
//    // Vector3D spacecraftToOriginVector =
//    // spacecraftPositionVector.scalarMultiply(-1.0);
//    // Vector3D originPointingVector = spacecraftToOriginVector.normalize();
//    // double distance = spacecraftToOriginVector.getNorm();
//    // Vector3D deltaVector = originPointingVector.scalarMultiply(distance *
//    // (zoomFactor - 1.0));
//    // double[] delta = { deltaVector.getX(), deltaVector.getY(), deltaVector.getZ()
//    // };
//
//    double zoomRatio = 1.0 / zoomFactor;
//    if (zoomRatio < 1.0)
//	{
//    	zoomRatio = 1.0;
//    	return;
//	}
//    int nslices = getImageDepth();
//    for (int slice = 0; slice < nslices; slice++)
//    {
//        double[][] surfacePoint = new double[nslices][3];
//
//        for (int i = 0; i < 3; i++)
//        {
//        	surfacePoint[currentSlice][i] = spacecraftPositionOriginal[currentSlice][i] + boresightDirectionOriginal[currentSlice][i];
//        	spacecraftPositionAdjusted[currentSlice][i] = surfacePoint[currentSlice][i] - boresightDirectionOriginal[currentSlice][i] * zoomRatio;
//
////            spacecraftPositionAdjusted[currentSlice][i] = spacecraftPositionOriginal[currentSlice][i] * zoomRatio;
////            boresightDirectionAdjusted[currentSlice][i] = boresightDirectionOriginal[currentSlice][i] * zoomRatio;
//        }
//        frusta[slice] = null;
//        footprintGenerated[slice] = false;
//    }
//}
//
//private void rotateFrameAboutPitchAxis(double angleDegrees)
//{
//	int nslices = getImageDepth();
//    for (int slice = 0; slice < nslices; slice++)
//    {
//    	double[] vout = new double[] { 0.0, 0.0, 0.0 };
//    	MathUtil.vsub(frustum1Adjusted[slice], frustum2Adjusted[slice], vout);
//    	MathUtil.unorm(vout, vout);
//    	Rotation rotation = new Rotation(new Vector3D(vout), Math.toRadians(angleDegrees), RotationConvention.VECTOR_OPERATOR);
//    	MathUtil.rotateVector(frustum1Adjusted[slice], rotation, frustum1Adjusted[slice]);
//        MathUtil.rotateVector(frustum2Adjusted[slice], rotation, frustum2Adjusted[slice]);
//        MathUtil.rotateVector(frustum3Adjusted[slice], rotation, frustum3Adjusted[slice]);
//        MathUtil.rotateVector(frustum4Adjusted[slice], rotation, frustum4Adjusted[slice]);
//        MathUtil.rotateVector(boresightDirectionAdjusted[slice], rotation, boresightDirectionAdjusted[slice]);
//    }
//
//}
//
//
//private void rotateFrameAboutYawAxis(double angleDegrees)
//{
//	int nslices = getImageDepth();
//    for (int slice = 0; slice < nslices; slice++)
//    {
//    	double[] vout = new double[] { 0.0, 0.0, 0.0 };
//    	MathUtil.vsub(frustum1Adjusted[slice], frustum3Adjusted[slice], vout);
//    	MathUtil.unorm(vout, vout);
//    	Rotation rotation = new Rotation(new Vector3D(vout), Math.toRadians(angleDegrees), RotationConvention.VECTOR_OPERATOR);
//    	MathUtil.rotateVector(frustum1Adjusted[slice], rotation, frustum1Adjusted[slice]);
//        MathUtil.rotateVector(frustum2Adjusted[slice], rotation, frustum2Adjusted[slice]);
//        MathUtil.rotateVector(frustum3Adjusted[slice], rotation, frustum3Adjusted[slice]);
//        MathUtil.rotateVector(frustum4Adjusted[slice], rotation, frustum4Adjusted[slice]);
//        MathUtil.rotateVector(boresightDirectionAdjusted[slice], rotation, boresightDirectionAdjusted[slice]);
//    }
//}
//
//private void rotateFrameAboutTarget(double angleDegrees)
//{
//     Vector3D axis = new Vector3D(boresightDirectionOriginal[currentSlice]);
////    Vector3D axis = new Vector3D(spacecraftPositionAdjusted[currentSlice]);
////    axis.normalize();
////    axis.negate();
//    Rotation rotation = new Rotation(axis, Math.toRadians(angleDegrees), RotationConvention.VECTOR_OPERATOR);
//
//    // int slice = getCurrentSlice();
//    int nslices = getImageDepth();
//    for (int slice = 0; slice < nslices; slice++)
//    {
//        MathUtil.rotateVector(frustum1Adjusted[slice], rotation, frustum1Adjusted[slice]);
//        MathUtil.rotateVector(frustum2Adjusted[slice], rotation, frustum2Adjusted[slice]);
//        MathUtil.rotateVector(frustum3Adjusted[slice], rotation, frustum3Adjusted[slice]);
//        MathUtil.rotateVector(frustum4Adjusted[slice], rotation, frustum4Adjusted[slice]);
//        MathUtil.rotateVector(boresightDirectionAdjusted[slice], rotation, boresightDirectionAdjusted[slice]);
//
//        frusta[slice] = null;
//        footprintGenerated[slice] = false;
//    }
//}
//
//private void translateSpacecraftInImagePlane(double sampleDelta, double lineDelta)
//{
//	int nslices = getImageDepth();
//
//    for (int slice = 0; slice < nslices; slice++)
//    {
//    	double[] sampleAxis = new double[] { 0.0, 0.0, 0.0 };
//    	MathUtil.vsub(frustum1Adjusted[slice], frustum2Adjusted[slice], sampleAxis);
//    	MathUtil.unorm(sampleAxis, sampleAxis);
//    	double[] lineAxis = new double[] { 0.0, 0.0, 0.0 };
//    	MathUtil.vsub(frustum1Adjusted[slice], frustum3Adjusted[slice], lineAxis);
//    	MathUtil.unorm(lineAxis, lineAxis);
//    	MathUtil.vscl(sampleDelta, sampleAxis, sampleAxis);
//    	MathUtil.vadd(spacecraftPositionAdjusted[slice], sampleAxis, spacecraftPositionAdjusted[slice]);
//    	MathUtil.vscl(lineDelta, lineAxis, lineAxis);
//    	MathUtil.vadd(spacecraftPositionAdjusted[slice], lineAxis, spacecraftPositionAdjusted[slice]);
//    }
//}
//
//public void moveTargetPixelCoordinates(double[] pixelDelta)
//{
//     System.out.println("moveTargetPixelCoordinates(): " + pixelDelta[1] + " " +
//     pixelDelta[0]);
//     System.out.println("PerspectiveImage: moveTargetPixelCoordinates: current target pixel coords " + targetPixelCoordinates[0] + " " + targetPixelCoordinates[1]);
//    double height = (double) getImageHeight();
//    if (targetPixelCoordinates[0] == Double.MAX_VALUE || targetPixelCoordinates[1] == Double.MAX_VALUE)
//    {
//        targetPixelCoordinates = getPixelFromPoint(bodyOrigin);
//        targetPixelCoordinates[0] = height - 1 - targetPixelCoordinates[0];
//    }
//    System.out.println("PerspectiveImage: moveTargetPixelCoordinates: current target pixel coords 2 " + targetPixelCoordinates[0] + " " + targetPixelCoordinates[1]);
//
//    double line = this.targetPixelCoordinates[0] + pixelDelta[0];
//    double sample = targetPixelCoordinates[1] + pixelDelta[1];
//    double[] newFrustumCenterPixel = { line, sample };
//    System.out.println("moveTargetPixelCoordinates(): " + newFrustumCenterPixel[1] + " " + newFrustumCenterPixel[0]);
//    setTargetPixelCoordinates(newFrustumCenterPixel);
//}
//
//// public void moveOffsetPixelCoordinates(double[] pixelDelta)
//// {
////// System.out.println("moveOffsetPixelCoordinates(): " + pixelDelta[1] + " " +
//// pixelDelta[0]);
////
//// double height = (double)getImageHeight();
//// double width = (double)getImageWidth();
//// if (offsetPixelCoordinates[0] == Double.MAX_VALUE ||
//// offsetPixelCoordinates[1] == Double.MAX_VALUE)
//// {
//// offsetPixelCoordinates[0] = 0.0;
//// offsetPixelCoordinates[1] = 0.0;
//// }
//// double line = offsetPixelCoordinates[0] + pixelDelta[0];
//// double sample = offsetPixelCoordinates[1] + pixelDelta[1];
//// double[] newPixelOffset = { line, sample };
////
//// setPixelOffset(newPixelOffset);
//// }
//
//public void movePitchAngleBy(double rotationDelta)
//{
//	double newPitchOffset = pitchOffset[0] + rotationDelta;
//	setPitchOffset(newPitchOffset);
//}
//
//public void moveYawAngleBy(double rotationDelta)
//{
//	double newYawOffset = yawOffset[0] + rotationDelta;
//	setYawOffset(newYawOffset);
//}

//public void moveLineOffsetBy(double offset)
//{
//	setLineOffset(lineOffset + offset);
//}
//
//public void moveSampleOffsetBy(double offset)
//{
//	setSampleOffset(sampleOffset + offset);
//}
//
//
///**
// * This adjusts the roll angle about the boresight direction
// * @param rotationDelta
// */
//public void moveRotationAngleBy(double rotationDelta)
//{
//    // System.out.println("moveRotationAngleBy(): " + rotationDelta);
//
//    double newRotationOffset = rotationOffset[0] + rotationDelta;
//
//    setRotationOffset(newRotationOffset);
//}
//
//public void moveZoomFactorBy(double zoomDelta)
//{
//    // System.out.println("moveZoomDeltaBy(): " + zoomDelta);
//
//    double newZoomFactor = zoomFactor[0] * zoomDelta;
//
//    setZoomFactor(newZoomFactor);
//}

// private void rotateBoresightDirectionTo(double[] newDirection)
// {
// Vector3D oldDirectionVector = new
// Vector3D(boresightDirectionOriginal[currentSlice]);
// Vector3D newDirectionVector = new Vector3D(newDirection);
//
// Rotation rotation = new Rotation(oldDirectionVector, newDirectionVector);
//
// int nslices = getNumberBands();
// for (int i = 0; i<nslices; i++)
// {
// MathUtil.rotateVector(frustum1Adjusted[i], rotation, frustum1Adjusted[i]);
// MathUtil.rotateVector(frustum2Adjusted[i], rotation, frustum2Adjusted[i]);
// MathUtil.rotateVector(frustum3Adjusted[i], rotation, frustum3Adjusted[i]);
// MathUtil.rotateVector(frustum4Adjusted[i], rotation, frustum4Adjusted[i]);
// MathUtil.rotateVector(boresightDirectionAdjusted[i], rotation,
// boresightDirectionAdjusted[i]);
//
// frusta[i] = null;
// footprintGenerated[i] = false;
// }
//
//// loadFootprint();
//// calculateFrustum();
// }

//private void rotateTargetPixelDirectionToLocalOrigin(double[] direction)
//{
//    Vector3D directionVector = new Vector3D(direction);
//    Vector3D spacecraftPositionVector = new Vector3D(spacecraftPositionOriginal[currentSlice]);
//    Vector3D spacecraftToOriginVector = spacecraftPositionVector.scalarMultiply(-1.0);
//    Vector3D originPointingVector = spacecraftToOriginVector.normalize();
//
//    Rotation rotation = new Rotation(directionVector, originPointingVector);
//
//    // int slice = getCurrentSlice();
//    int nslices = getImageDepth();
//    for (int slice = 0; slice < nslices; slice++)
//    {
//        MathUtil.rotateVector(frustum1Adjusted[slice], rotation, frustum1Adjusted[slice]);
//        MathUtil.rotateVector(frustum2Adjusted[slice], rotation, frustum2Adjusted[slice]);
//        MathUtil.rotateVector(frustum3Adjusted[slice], rotation, frustum3Adjusted[slice]);
//        MathUtil.rotateVector(frustum4Adjusted[slice], rotation, frustum4Adjusted[slice]);
//        MathUtil.rotateVector(boresightDirectionAdjusted[slice], rotation, boresightDirectionAdjusted[slice]);
//
//        frusta[slice] = null;
//        footprintGenerated[slice] = false;
//    }
//}


///////////////////////////////////
// Rendering methods
///////////////////////////////////
//public void calculateFrustum()
//{
//    if (frustumActor == null)
//        return;
//    // System.out.println("recalculateFrustum()");
//    frustumPolyData = new vtkPolyData();
//
//    vtkPoints points = new vtkPoints();
//    vtkCellArray lines = new vtkCellArray();
//
//    vtkIdList idList = new vtkIdList();
//    idList.SetNumberOfIds(2);
//
//    double[][] frustum1Adjusted = getFrustum1Adjusted();
//    double[][] frustum2Adjusted = getFrustum2Adjusted();
//    double[][] frustum3Adjusted = getFrustum3Adjusted();
//    double[][] frustum4Adjusted = getFrustum4Adjusted();
//    double[][] spacecraftPositionAdjusted = getSpacecraftPositionAdjusted();
//    double maxFrustumRayLength = MathUtil.vnorm(spacecraftPositionAdjusted[currentSlice]) + smallBodyModel.getBoundingBoxDiagonalLength();
//    double[] origin = spacecraftPositionAdjusted[currentSlice];
//    double[] UL = { origin[0] + frustum1Adjusted[currentSlice][0] * maxFrustumRayLength, origin[1] + frustum1Adjusted[currentSlice][1] * maxFrustumRayLength, origin[2] + frustum1Adjusted[currentSlice][2] * maxFrustumRayLength };
//    double[] UR = { origin[0] + frustum2Adjusted[currentSlice][0] * maxFrustumRayLength, origin[1] + frustum2Adjusted[currentSlice][1] * maxFrustumRayLength, origin[2] + frustum2Adjusted[currentSlice][2] * maxFrustumRayLength };
//    double[] LL = { origin[0] + frustum3Adjusted[currentSlice][0] * maxFrustumRayLength, origin[1] + frustum3Adjusted[currentSlice][1] * maxFrustumRayLength, origin[2] + frustum3Adjusted[currentSlice][2] * maxFrustumRayLength };
//    double[] LR = { origin[0] + frustum4Adjusted[currentSlice][0] * maxFrustumRayLength, origin[1] + frustum4Adjusted[currentSlice][1] * maxFrustumRayLength, origin[2] + frustum4Adjusted[currentSlice][2] * maxFrustumRayLength };
//
//    double minFrustumRayLength = MathUtil.vnorm(spacecraftPositionAdjusted[currentSlice]) - smallBodyModel.getBoundingBoxDiagonalLength();
//    maxFrustumDepth[currentSlice] = maxFrustumRayLength; // a reasonable approximation for a max bound on the frustum depth
//    minFrustumDepth[currentSlice] = minFrustumRayLength; // a reasonable approximation for a min bound on the frustum depth
//
//    points.InsertNextPoint(spacecraftPositionAdjusted[currentSlice]);
//    points.InsertNextPoint(UL);
//    points.InsertNextPoint(UR);
//    points.InsertNextPoint(LL);
//    points.InsertNextPoint(LR);
//
//    idList.SetId(0, 0);
//    idList.SetId(1, 1);
//    lines.InsertNextCell(idList);
//    idList.SetId(0, 0);
//    idList.SetId(1, 2);
//    lines.InsertNextCell(idList);
//    idList.SetId(0, 0);
//    idList.SetId(1, 3);
//    lines.InsertNextCell(idList);
//    idList.SetId(0, 0);
//    idList.SetId(1, 4);
//    lines.InsertNextCell(idList);
//
//    frustumPolyData.SetPoints(points);
//    frustumPolyData.SetLines(lines);
//
//    vtkPolyDataMapper frusMapper = new vtkPolyDataMapper();
//    frusMapper.SetInputData(frustumPolyData);
//
//    frustumActor.SetMapper(frusMapper);
//}
//
//public vtkImageData getRawImage()
//{
//    return rawImage;
//}
//
//public vtkImageData getDisplayedImage()
//{
//    return displayedImage;
//}
//
//public void setUseDefaultFootprint(boolean useDefaultFootprint)
//{
//    this.useDefaultFootprint = useDefaultFootprint;
//    for (int i = 0; i < getImageDepth(); i++)
//    {
//        footprintGenerated[i] = false;
//    }
//}
//
//public boolean useDefaultFootprint()
//{
//    return useDefaultFootprint;
//}
//
//public vtkTexture getTexture()
//{
//    return imageTexture;
//}
//
//public static void setGenerateFootprint(boolean b)
//{
//    generateFootprint = b;
//}
//
//public List<vtkProp> getProps()
//{
//
//    // System.out.println("getProps()");
//    if (footprintActor == null)
//    {
//        loadFootprint();
//
//        imageTexture = new vtkTexture();
//        imageTexture.InterpolateOn();
//        imageTexture.RepeatOff();
//        imageTexture.EdgeClampOn();
//        imageTexture.SetInputData(getDisplayedImage());
//
//        vtkPolyDataMapper footprintMapper = new vtkPolyDataMapper();
//        footprintMapper.SetInputData(shiftedFootprint[0]);
//        footprintMapper.Update();
//        footprintActor = new vtkActor();
//        footprintActor.SetMapper(footprintMapper);
//        footprintActor.SetTexture(imageTexture);
//        vtkProperty footprintProperty = footprintActor.GetProperty();
//        footprintProperty.LightingOff();
//
//        footprintActors.add(footprintActor);
//    }
//
//    if (frustumActor == null)
//    {
//        frustumActor = new vtkActor();
//
//        calculateFrustum();
//
//        vtkProperty frustumProperty = frustumActor.GetProperty();
//        frustumProperty.SetColor(0.0, 1.0, 0.0);
//        frustumProperty.SetLineWidth(2.0);
//        frustumActor.VisibilityOff();
//
//        footprintActors.add(frustumActor);
//    }
//
//    // for offlimb
//    getOffLimbTexture();
//    footprintActors.addAll(offlimbPlaneHelper.getProps());
////    if (offLimbActor == null && offLimbTexture != null)
////    {
////        loadOffLimbPlane();
////        if (footprintActors.contains(offLimbActor))
////            footprintActors.remove(offLimbActor);
////        footprintActors.add(offLimbActor);
////        if (footprintActors.contains(offLimbBoundaryActor))
////            footprintActors.remove(offLimbBoundaryActor);
////        footprintActors.add(offLimbBoundaryActor);
////    }
//
//    return footprintActors;
//}
//
//public void setShowFrustum(boolean b)
//{
//    showFrustum = b;
//
//    if (showFrustum)
//    {
//        frustumActor.VisibilityOn();
//    }
//    else
//    {
//        frustumActor.VisibilityOff();
//    }
//
//    this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, this);
//}
//
//public boolean isFrustumShowing()
//{
//    return showFrustum;
//}
//
//public void setSimulateLighting(boolean b)
//{
//    simulateLighting = b;
//}
//
//public boolean isSimulatingLighingOn()
//{
//    return simulateLighting;
//}
//
//public double getMinIncidence()
//{
//    return minIncidence;
//}
//
//public double getMaxIncidence()
//{
//    return maxIncidence;
//}
//
//public double getMinEmission()
//{
//    return minEmission;
//}
//
//public double getMaxEmission()
//{
//    return maxEmission;
//}
//
//public double getMinPhase()
//{
//    return minPhase;
//}
//
//public double getMaxPhase()
//{
//    return maxPhase;
//}
//
//public IntensityRange getDisplayedRange()
//{
//    return getDisplayedRange(currentSlice);
//}
//
///**
// * This getter lazily initializes the range field as necessary to
// * ensure this returns a valid, non-null range as long as the argument
// * is in range for this image.
// *
// * @param slice the number of the slice whose displayed range to return.
// */
//public IntensityRange getDisplayedRange(int slice)
//{
//    int nslices = getImageDepth();
//
//    Preconditions.checkArgument(slice < nslices);
//
//    if (displayedRange == null)
//    {
//        displayedRange = new IntensityRange[nslices];
//    }
//    if (displayedRange[slice] == null)
//    {
//        displayedRange[slice] = new IntensityRange(0, 255);
//    }
//
//    return displayedRange[slice];
//}
//
///**
// * Set the displayed image range of the currently selected slice of the image.
// * As a side-effect, this method also MAYBE CREATES the displayed image.
// *
// * @param range the new displayed range of the image. If null is passed,
// */
//public void setDisplayedImageRange(IntensityRange range)
//{
//    if (rawImage != null)
//    {
//        if (rawImage.GetNumberOfScalarComponents() > 1)
//        {
//            displayedImage = rawImage;
//            return;
//        }
//
//    }
//
//    IntensityRange displayedRange = getDisplayedRange(currentSlice);
//    if (range == null || displayedRange.min != range.min || displayedRange.max != range.max)
//    {
//        if (range != null)
//        {
//            this.displayedRange[currentSlice] = range;
//            saveImageInfo();
//        }
//
//        if (rawImage != null)
//        {
//            vtkImageData img = getImageWithDisplayedRange(range, false);
//
//            if (displayedImage == null)
//                displayedImage = new vtkImageData();
//            displayedImage.DeepCopy(img);
//        }
//    }
//
//    this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, this);
//}
//
//public vtkPolyData getFootprint(int defaultSlice)
//{
//    if (footprint[0].GetNumberOfPoints() > 0)
//        return footprint[0];
//    // first check the cache
//    vtkPolyData existingFootprint = checkForExistingFootprint();
//    if (existingFootprint != null)
//        return existingFootprint;
//    else
//    {
//        vtkPolyData footprint = smallBodyModel.computeFrustumIntersection(getSpacecraftPositionAdjusted()[defaultSlice],
//        																	getFrustum1Adjusted()[defaultSlice],
//        																	getFrustum3Adjusted()[defaultSlice],
//        																	getFrustum4Adjusted()[defaultSlice],
//        																	getFrustum2Adjusted()[defaultSlice]);
////        System.out.println("PerspectiveImage: getFootprint: footprint creation " + sw.elapsedMillis());
//        return footprint;
//    }
//}
//
//public void loadFootprint()
//{
//    vtkPolyData existingFootprint = checkForExistingFootprint();
//    if (existingFootprint != null)
//    {
//    	System.out.println("PerspectiveImage: loadFootprint: existing footprint");
//        footprint[0] = existingFootprint;
//
//        vtkPointData pointData = footprint[currentSlice].GetPointData();
//        pointData.SetTCoords(textureCoords);
////        System.out.println("PerspectiveImage: loadFootprint: setting texture coords " + sw.elapsedMillis());
//        PolyDataUtil.generateTextureCoordinates(getFrustum(), getImageWidth(), getImageHeight(), footprint[currentSlice]);
////        System.out.println("PerspectiveImage: loadFootprint: set texture coords " + sw.elapsedMillis());
//        pointData.Delete();
//
//        shiftedFootprint[0].DeepCopy(footprint[currentSlice]);
//        PolyDataUtil.shiftPolyDataInNormalDirection(shiftedFootprint[0], getOffset());
//        return;
//    }
//
//    if (generateFootprint)
//    {
//    	System.out.println("PerspectiveImage: loadFootprint: generate footprint true");
//        vtkPolyData tmp = null;
//
//        if (!footprintGenerated[currentSlice])
//        {
//        	System.out.println("PerspectiveImage: loadFootprint: footprint not generated");
//            if (useDefaultFootprint())
//            {
//            	System.out.println("PerspectiveImage: loadFootprint: using default footprint");
//                int defaultSlice = getDefaultSlice();
//                if (footprintGenerated[defaultSlice] == false)
//                {
//                    footprint[defaultSlice] = getFootprint(defaultSlice);
//                    if (footprint[defaultSlice] == null)
//                        return;
//
//                    // Need to clear out scalar data since if coloring data is being shown,
//                    // then the color might mix-in with the image.
//                    footprint[defaultSlice].GetCellData().SetScalars(null);
//                    footprint[defaultSlice].GetPointData().SetScalars(null);
//
//                    footprintGenerated[defaultSlice] = true;
//                }
//
//                tmp = footprint[defaultSlice];
//
//            }
//            else
//            {
//            	System.out.println("PerspectiveImage: loadFootprint: computing new intersection");
//                tmp = smallBodyModel.computeFrustumIntersection(getSpacecraftPositionAdjusted()[currentSlice],
//                												getFrustum1Adjusted()[currentSlice],
//                												getFrustum3Adjusted()[currentSlice],
//                												getFrustum4Adjusted()[currentSlice],
//                												getFrustum2Adjusted()[currentSlice]);
//                if (tmp == null)
//                    return;
//
//                // Need to clear out scalar data since if coloring data is being shown,
//                // then the color might mix-in with the image.
//                tmp.GetCellData().SetScalars(null);
//                tmp.GetPointData().SetScalars(null);
//            }
//
//            // vtkPolyDataWriter writer=new vtkPolyDataWriter();
//            // writer.SetInputData(tmp);
//            // writer.SetFileName("/Users/zimmemi1/Desktop/test.vtk");
//            // writer.SetFileTypeToBinary();
//            // writer.Write();
//
//            footprint[currentSlice].DeepCopy(tmp);
//
//            footprintGenerated[currentSlice] = true;
//        }
//        System.out.println("PerspectiveImage: loadFootprint: footprint generated");
//        vtkPointData pointData = footprint[currentSlice].GetPointData();
//        pointData.SetTCoords(textureCoords);
//        PolyDataUtil.generateTextureCoordinates(getFrustum(), getImageWidth(), getImageHeight(), footprint[currentSlice]);
//        pointData.Delete();
//    }
//    else
//    {
//    	System.out.println("PerspectiveImage: loadFootprint: fetching from server, generate footprint false");
//        int resolutionLevel = smallBodyModel.getModelResolution();
//
//        String footprintFilename = null;
//        File file = null;
//
//        if (key.getSource() == ImageSource.SPICE || key.getSource() == ImageSource.CORRECTED_SPICE)
//            footprintFilename = key.getName() + "_FOOTPRINT_RES" + resolutionLevel + "_PDS.VTP";
//        else
//            footprintFilename = key.getName() + "_FOOTPRINT_RES" + resolutionLevel + "_GASKELL.VTP";
//
//        file = FileCache.getFileFromServer(footprintFilename);
//
//        if (file == null || !file.exists())
//        {
//            System.out.println("Warning: " + footprintFilename + " not found");
//            return;
//        }
//
//        vtkXMLPolyDataReader footprintReader = new vtkXMLPolyDataReader();
//        footprintReader.SetFileName(file.getAbsolutePath());
//        footprintReader.Update();
//
//        vtkPolyData footprintReaderOutput = footprintReader.GetOutput();
//        footprint[currentSlice].DeepCopy(footprintReaderOutput);
//    }
//
//    shiftedFootprint[0].DeepCopy(footprint[currentSlice]);
//    PolyDataUtil.shiftPolyDataInNormalDirection(shiftedFootprint[0], getOffset());
//    vtkPolyDataWriter writer = new vtkPolyDataWriter();
//    writer.SetInputData(footprint[0]);
////    System.out.println("PerspectiveImage: loadFootprint: fit file full path " + getFitFileFullPath());
//    String intersectionFileName = getPrerenderingFileNameBase() + "_frustumIntersection.vtk";
//    File file = FileCache.instance().getFile(intersectionFileName);
////    System.out.println("PerspectiveImage: loadFootprint: saving to " + intersectionFileName);
//    writer.SetFileName(file.getPath());
//    writer.SetFileTypeToBinary();
//    writer.Write();
//}
//
//public double getMinimumHorizontalPixelScale()
//{
//    return minHorizontalPixelScale;
//}
//
//public double getMaximumHorizontalPixelScale()
//{
//    return maxHorizontalPixelScale;
//}
//
//public double getMeanHorizontalPixelScale()
//{
//    return meanHorizontalPixelScale;
//}
//
//public double getMinimumVerticalPixelScale()
//{
//    return minVerticalPixelScale;
//}
//
//public double getMaximumVerticalPixelScale()
//{
//    return maxVerticalPixelScale;
//}
//
//public double getMeanVerticalPixelScale()
//{
//    return meanVerticalPixelScale;
//}
//
//// Computes the incidence, emission, and phase at a point on the footprint with
//// a given normal.
//// (I.e. the normal of the plate which the point is lying on).
//// The output is a 3-vector with the first component equal to the incidence,
//// the second component equal to the emission and the third component equal to
//// the phase.
//public double[] computeIlluminationAnglesAtPoint(double[] pt, double[] normal)
//{
//	double[][] spacecraftPositionAdjusted = getSpacecraftPositionAdjusted();
//    double[] scvec = {
//            spacecraftPositionAdjusted[currentSlice][0] - pt[0],
//            spacecraftPositionAdjusted[currentSlice][1] - pt[1],
//            spacecraftPositionAdjusted[currentSlice][2] - pt[2] };
//
//    double[] sunVectorAdjusted = getSunVector();
//    double incidence = MathUtil.vsep(normal, sunVectorAdjusted) * 180.0 / Math.PI;
//    double emission = MathUtil.vsep(normal, scvec) * 180.0 / Math.PI;
//    double phase = MathUtil.vsep(sunVectorAdjusted, scvec) * 180.0 / Math.PI;
//
//    double[] angles = { incidence, emission, phase };
//
//    return angles;
//}
//
//public void propertyChange(PropertyChangeEvent evt)
//{
//    if (Properties.MODEL_RESOLUTION_CHANGED.equals(evt.getPropertyName()))
//    {
//        loadFootprint();
//        normalsGenerated = false;
//        this.minEmission = Double.MAX_VALUE;
//        this.maxEmission = -Double.MAX_VALUE;
//        this.minIncidence = Double.MAX_VALUE;
//        this.maxIncidence = -Double.MAX_VALUE;
//        this.minPhase = Double.MAX_VALUE;
//        this.maxPhase = -Double.MAX_VALUE;
//        this.minHorizontalPixelScale = Double.MAX_VALUE;
//        this.maxHorizontalPixelScale = -Double.MAX_VALUE;
//        this.minVerticalPixelScale = Double.MAX_VALUE;
//        this.maxVerticalPixelScale = -Double.MAX_VALUE;
//        this.meanHorizontalPixelScale = 0.0;
//        this.meanVerticalPixelScale = 0.0;
//
//        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, this);
//    }
//}
//
///**
// * The shifted footprint is the original footprint shifted slightly in the
// * normal direction so that it will be rendered correctly and not obscured by
// * the asteroid.
// *
// * @return
// */
//@Override
//public vtkPolyData getShiftedFootprint()
//{
//    return shiftedFootprint[0];
//}
//
///**
// * The original footprint whose cells exactly overlap the original asteroid. If
// * rendered as is, it would interfere with the asteroid. Note: this is made
// * public in this class for the benefit of backplane generators, which use it.
// *
// * @return
// */
//@Override
//public vtkPolyData getUnshiftedFootprint()
//{
//    return footprint[currentSlice];
//}
//
//public void Delete()
//{
//    displayedImage.Delete();
//    rawImage.Delete();
//
//    for (int i = 0; i < footprint.length; i++)
//    {
//        // Footprints can be null if no frustum intersection is found
//        if (footprint[i] != null)
//        {
//            footprint[i].Delete();
//        }
//    }
//
//    for (int i = 0; i < shiftedFootprint.length; i++)
//    {
//        if (shiftedFootprint[i] != null)
//        {
//            shiftedFootprint[i].Delete();
//        }
//    }
//
//    textureCoords.Delete();
//    normalsFilter.Delete();
//    maskSource.Delete();
//}
//
//public Frustum getFrustum(int slice)
//{
//    if (useDefaultFootprint())
//    {
//        int defaultSlice = getDefaultSlice();
//        if (frusta[defaultSlice] == null)
//            frusta[defaultSlice] = new Frustum(getSpacecraftPositionAdjusted()[defaultSlice], getFrustum1Adjusted()[defaultSlice], getFrustum3Adjusted()[defaultSlice], getFrustum4Adjusted()[defaultSlice], getFrustum2Adjusted()[defaultSlice]);
//        return frusta[defaultSlice];
//    }
//
//    if (frusta[slice] == null)
//        frusta[slice] = new Frustum(getSpacecraftPositionAdjusted()[slice], getFrustum1Adjusted()[slice], getFrustum3Adjusted()[slice], getFrustum4Adjusted()[slice], getFrustum2Adjusted()[slice]);
//    return frusta[slice];
//}
//
//public Frustum getFrustum()
//{
//    return getFrustum(currentSlice);
//}
//
//public int getNumberOfComponentsOfOriginalImage()
//{
//    return rawImage.GetNumberOfScalarComponents();
//}
//
//public void setRawImage(vtkImageData rawImage)
//{
//    this.rawImage = rawImage;
//}
//
//public double getMaxFrustumDepth(int slice)
//{
//    return maxFrustumDepth[slice];
//}
//
//public double getMinFrustumDepth(int slice)
//{
//    return minFrustumDepth[slice];
//}
//
//vtkImageData getImageWithDisplayedRange(IntensityRange range, boolean offlimb)
//{
//    float minValue = getMinValue();
//    float maxValue = getMaxValue();
//    float dx = (maxValue - minValue) / 255.0f;
//
//    float min = minValue;
//    float max = maxValue;
//    if (!offlimb)
//    {
//        IntensityRange displayedRange = getDisplayedRange(currentSlice);
//        min = minValue + displayedRange.min * dx;
//        max = minValue + displayedRange.max * dx;
//    }
//    else
//    {
//        IntensityRange offLimbDisplayedRange = getOffLimbDisplayedRange();
//        min = minValue + offLimbDisplayedRange.min * dx;
//        max = minValue + offLimbDisplayedRange.max * dx;
//    }
//
//    // Update the displayed image
//    vtkLookupTable lut = new vtkLookupTable();
//    lut.SetTableRange(min, max);
//    lut.SetValueRange(0.0, 1.0);
//    lut.SetHueRange(0.0, 0.0);
//    lut.SetSaturationRange(0.0, 0.0);
//    // lut.SetNumberOfTableValues(402);
//    lut.SetRampToLinear();
//    lut.Build();
//
//    // for 3D images, take the current slice
//    vtkImageData image2D = rawImage;
//    if (getImageDepth() > 1)
//    {
//        vtkImageReslice slicer = new vtkImageReslice();
//        slicer.SetInputData(rawImage);
//        slicer.SetOutputDimensionality(2);
//        slicer.SetInterpolationModeToNearestNeighbor();
//        slicer.SetOutputSpacing(1.0, 1.0, 1.0);
//        slicer.SetResliceAxesDirectionCosines(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0);
//
//        slicer.SetOutputOrigin(0.0, 0.0, (double) currentSlice);
//        slicer.SetResliceAxesOrigin(0.0, 0.0, (double) currentSlice);
//
//        slicer.SetOutputExtent(0, imageWidth - 1, 0, imageHeight - 1, 0, 0);
//
//        slicer.Update();
//        image2D = slicer.GetOutput();
//    }
//
//    vtkImageMapToColors mapToColors = new vtkImageMapToColors();
//    mapToColors.SetInputData(image2D);
//    mapToColors.SetOutputFormatToRGBA();
//    mapToColors.SetLookupTable(lut);
//    mapToColors.Update();
//
//    vtkImageData mapToColorsOutput = mapToColors.GetOutput();
//    vtkImageData maskSourceOutput = maskSource.GetOutput();
//
//    vtkImageMask maskFilter = new vtkImageMask();
//    maskFilter.SetImageInputData(mapToColorsOutput);
//    maskFilter.SetMaskInputData(maskSourceOutput);
//    maskFilter.Update();
//
//    vtkImageData maskFilterOutput = maskFilter.GetOutput();
//    mapToColors.Delete();
//    lut.Delete();
//    mapToColorsOutput.Delete();
//    maskSourceOutput.Delete();
//    maskFilter.Delete();
//    return maskFilterOutput;
//}
//
///**
// * Give oppurtunity to subclass to do some processing on the raw image such as
// * resizing, flipping, masking, etc.
// *
// * @param rawImage
// */
//protected void processRawImage(vtkImageData rawImage)
//{
//	if (getFlip().equals("X"))
//    {
//        ImageDataUtil.flipImageXAxis(rawImage);
//    }
//    else if (getFlip().equals("Y"))
//    {
//        ImageDataUtil.flipImageYAxis(rawImage);
//    }
//    if (getRotation() != 0.0)
//        ImageDataUtil.rotateImage(rawImage, 360.0 - getRotation());
//}
//
//protected vtkImageData createRawImage(int height, int width, int depth, float[][] array2D, float[][][] array3D)
//{
//    return createRawImage(height, width, depth, true, array2D, array3D);
//}
//
//public vtkImageData createRawImage(int height, int width, int depth, boolean transpose, float[][] array2D, float[][][] array3D)
//{
//    // Allocate enough room to store min/max value at each layer
//    maxValue = new float[depth];
//    minValue = new float[depth];
//
//    // Call
//    return ImageDataUtil.createRawImage(height, width, depth, transpose, array2D, array3D, minValue, maxValue);
//}
//
//private vtkPolyData checkForExistingFootprint()
//{
//    String intersectionFileName = getPrerenderingFileNameBase() + "_frustumIntersection.vtk.gz";
//    if (FileCache.isFileGettable(intersectionFileName))
//    {
////        System.out.println(
////                "PerspectiveImage: checkForExistingFootprint: getting from server");
//        File file = FileCache.getFileFromServer(intersectionFileName);
////        System.out.println("PerspectiveImage: checkForExistingFootprint: exists locally " + file.getAbsolutePath());
//        vtkPolyDataReader reader = new vtkPolyDataReader();
////        reader.SetFileName(file.getPath().replaceFirst("\\.[^\\.]*$", ""));	//This is wrong.  The old code was stripping off .gz from the intersection name.  This now further removes .vtk which is bad.
//        reader.SetFileName(file.getAbsolutePath()); // now just reads in the file path as it should.
//        reader.Update();
//        vtkPolyData footprint = reader.GetOutput();
//        return footprint;
//    }
//    return null;
//}
//
//public vtkPolyData generateBoundary()
//{
//    loadFootprint();
//
//    if (footprint[currentSlice].GetNumberOfPoints() == 0)
//        return null;
//
//    vtkFeatureEdges edgeExtracter = new vtkFeatureEdges();
//    edgeExtracter.SetInputData(footprint[currentSlice]);
//    edgeExtracter.BoundaryEdgesOn();
//    edgeExtracter.FeatureEdgesOff();
//    edgeExtracter.NonManifoldEdgesOff();
//    edgeExtracter.ManifoldEdgesOff();
//    edgeExtracter.Update();
//
//    vtkPolyData boundary = new vtkPolyData();
//    vtkPolyData edgeExtracterOutput = edgeExtracter.GetOutput();
//    boundary.DeepCopy(edgeExtracterOutput);
//
//    return boundary;
//}
//
//private void computeCellNormals()
//{
//    if (normalsGenerated == false)
//    {
//        normalsFilter.SetInputData(footprint[currentSlice]);
//        normalsFilter.SetComputeCellNormals(1);
//        normalsFilter.SetComputePointNormals(0);
//        // normalsFilter.AutoOrientNormalsOn();
//        // normalsFilter.ConsistencyOn();
//        normalsFilter.SplittingOff();
//        normalsFilter.Update();
//
//        if (footprint != null && footprint[currentSlice] != null)
//        {
//            vtkPolyData normalsFilterOutput = normalsFilter.GetOutput();
//            footprint[currentSlice].DeepCopy(normalsFilterOutput);
//            normalsGenerated = true;
//        }
//    }
//}
//
//protected void computeIlluminationAngles()
//{
//    if (footprintGenerated[currentSlice] == false)
//        loadFootprint();
//
//    computeCellNormals();
//
//    int numberOfCells = footprint[currentSlice].GetNumberOfCells();
//
//    vtkPoints points = footprint[currentSlice].GetPoints();
//    vtkCellData footprintCellData = footprint[currentSlice].GetCellData();
//    vtkDataArray normals = footprintCellData.GetNormals();
//
//    this.minEmission = Double.MAX_VALUE;
//    this.maxEmission = -Double.MAX_VALUE;
//    this.minIncidence = Double.MAX_VALUE;
//    this.maxIncidence = -Double.MAX_VALUE;
//    this.minPhase = Double.MAX_VALUE;
//    this.maxPhase = -Double.MAX_VALUE;
//
//    for (int i = 0; i < numberOfCells; ++i)
//    {
//        vtkCell cell = footprint[currentSlice].GetCell(i);
//        double[] pt0 = points.GetPoint(cell.GetPointId(0));
//        double[] pt1 = points.GetPoint(cell.GetPointId(1));
//        double[] pt2 = points.GetPoint(cell.GetPointId(2));
//        double[] centroid = {
//                (pt0[0] + pt1[0] + pt2[0]) / 3.0,
//                (pt0[1] + pt1[1] + pt2[1]) / 3.0,
//                (pt0[2] + pt1[2] + pt2[2]) / 3.0
//        };
//        double[] normal = normals.GetTuple3(i);
//
//        double[] angles = computeIlluminationAnglesAtPoint(centroid, normal);
//        double incidence = angles[0];
//        double emission = angles[1];
//        double phase = angles[2];
//
//        if (incidence < minIncidence)
//            minIncidence = incidence;
//        if (incidence > maxIncidence)
//            maxIncidence = incidence;
//        if (emission < minEmission)
//            minEmission = emission;
//        if (emission > maxEmission)
//            maxEmission = emission;
//        if (phase < minPhase)
//            minPhase = phase;
//        if (phase > maxPhase)
//            maxPhase = phase;
//        cell.Delete();
//    }
//
//    points.Delete();
//    footprintCellData.Delete();
//    if (normals != null)
//        normals.Delete();
//}
//
//protected void computePixelScale()
//{
//	double[][] spacecraftPositionAdjusted = getSunPositionAdjusted();
//	double[][] frustum1Adjusted = getFrustum1Adjusted();
//	double[][] frustum2Adjusted = getFrustum2Adjusted();
//	double[][] frustum3Adjusted = getFrustum3Adjusted();
//    if (footprintGenerated[currentSlice] == false)
//        loadFootprint();
//
//    int numberOfPoints = footprint[currentSlice].GetNumberOfPoints();
//
//    vtkPoints points = footprint[currentSlice].GetPoints();
//
//    minHorizontalPixelScale = Double.MAX_VALUE;
//    maxHorizontalPixelScale = -Double.MAX_VALUE;
//    meanHorizontalPixelScale = 0.0;
//    minVerticalPixelScale = Double.MAX_VALUE;
//    maxVerticalPixelScale = -Double.MAX_VALUE;
//    meanVerticalPixelScale = 0.0;
//
//    double horizScaleFactor = 2.0 * Math.tan(MathUtil.vsep(frustum1Adjusted[currentSlice], frustum3Adjusted[currentSlice]) / 2.0) / imageHeight;
//    double vertScaleFactor = 2.0 * Math.tan(MathUtil.vsep(frustum1Adjusted[currentSlice], frustum2Adjusted[currentSlice]) / 2.0) / imageWidth;
//
//    double[] vec = new double[3];
//
//    for (int i = 0; i < numberOfPoints; ++i)
//    {
//        double[] pt = points.GetPoint(i);
//
//        vec[0] = pt[0] - spacecraftPositionAdjusted[currentSlice][0];
//        vec[1] = pt[1] - spacecraftPositionAdjusted[currentSlice][1];
//        vec[2] = pt[2] - spacecraftPositionAdjusted[currentSlice][2];
//        double dist = MathUtil.vnorm(vec);
//
//        double horizPixelScale = dist * horizScaleFactor;
//        double vertPixelScale = dist * vertScaleFactor;
//
//        if (horizPixelScale < minHorizontalPixelScale)
//            minHorizontalPixelScale = horizPixelScale;
//        if (horizPixelScale > maxHorizontalPixelScale)
//            maxHorizontalPixelScale = horizPixelScale;
//        if (vertPixelScale < minVerticalPixelScale)
//            minVerticalPixelScale = vertPixelScale;
//        if (vertPixelScale > maxVerticalPixelScale)
//            maxVerticalPixelScale = vertPixelScale;
//
//        meanHorizontalPixelScale += horizPixelScale;
//        meanVerticalPixelScale += vertPixelScale;
//    }
//
//    meanHorizontalPixelScale /= (double) numberOfPoints;
//    meanVerticalPixelScale /= (double) numberOfPoints;
//
//    points.Delete();
//}
//
//public double getOpacity()
//{
//    return imageOpacity;
//}
//
//public void setOpacity(double imageOpacity)
//{
//    this.imageOpacity = imageOpacity;
//    vtkProperty smallBodyProperty = footprintActor.GetProperty();
//    smallBodyProperty.SetOpacity(imageOpacity);
//    this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, this);
//}
//
//public void setCurrentMask(int[] masking)
//{
//    int topMask = masking[0];
//    int rightMask = masking[1];
//    int bottomMask = masking[2];
//    int leftMask = masking[3];
//    // Initialize the mask to black which masks out the image
//    maskSource.SetDrawColor(0.0, 0.0, 0.0, 0.0);
//    maskSource.FillBox(0, imageWidth - 1, 0, imageHeight - 1);
//    // Create a square inside mask which passes through the image.
//    maskSource.SetDrawColor(255.0, 255.0, 255.0, 255.0);
//    maskSource.FillBox(leftMask, imageWidth - 1 - rightMask, bottomMask, imageHeight - 1 - topMask);
//    maskSource.Update();
//
//    this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, this);
//    setDisplayedImageRange(null);
//
//    for (int i = 0; i < masking.length; ++i)
//        currentMask[i] = masking[i];
//}
//
//public int[] getCurrentMask()
//{
//    return currentMask.clone();
//}

//public boolean[] getFootprintGenerated()
//{
//  return footprintGenerated;
//}
//
//public void setFootprintGenerated(boolean footprintGenerated)
//{
//  this.footprintGenerated[getDefaultSlice()] = footprintGenerated;
//}
//
//public void setFootprintGenerated(boolean footprintGenerated, int slice)
//{
//  this.footprintGenerated[slice] = footprintGenerated;
//}
//
//public boolean isNormalsGenerated()
//{
//  return normalsGenerated;
//}
//
//public static boolean isGenerateFootprint()
//{
//  return generateFootprint;
//}
//
//public void setNormalsGenerated(boolean normalsGenerated)
//{
//  this.normalsGenerated = normalsGenerated;
//}
///**
// * Return surface area of footprint (unshifted) of image.
// *
// * @return
// */
//public double getSurfaceArea()
//{
//    return PolyDataUtil.getSurfaceArea(footprint[currentSlice]);
//}
//public void setVisible(boolean b)
//{
//    footprintActor.SetVisibility(b ? 1 : 0);
//    super.setVisible(b);
//}


/////////////////////
// Backplane methods
/////////////////////

//protected void appendWithPadding(StringBuffer strbuf, String str)
//{
//    strbuf.append(str);
//
//    int length = str.length();
//    while (length < 78)
//    {
//        strbuf.append(' ');
//        ++length;
//    }
//
//    strbuf.append("\r\n");
//}

///**
// * Generate PDS 3 format backplanes label file. This is the default
// * implementation for classes extending PerspectiveImage.
// *
// * @param imgName - pointer to the data File for which this label is being
// *            created
// * @param lblFileName - pointer to the output label file to be written, without
// *            file name extension. The extension is dependent on image type
// *            (e.g. MSI images are written as PDS 4 XML labels), and is assigned
// *            in the class implementing this function.
// * @throws IOException
// */
//public void generateBackplanesLabel(File imgName, File lblFileName) throws IOException
//{
//    StringBuffer strbuf = new StringBuffer("");
//
//    int numBands = 16;
//
//    appendWithPadding(strbuf, "PDS_VERSION_ID               = PDS3");
//    appendWithPadding(strbuf, "");
//
//    appendWithPadding(strbuf, "PRODUCT_TYPE                 = DDR");
//    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//    Date date = new Date();
//    String dateStr = sdf.format(date).replace(' ', 'T');
//    appendWithPadding(strbuf, "PRODUCT_CREATION_TIME        = " + dateStr);
//    appendWithPadding(strbuf, "PRODUCER_INSTITUTION_NAME    = \"APPLIED PHYSICS LABORATORY\"");
//    appendWithPadding(strbuf, "SOFTWARE_NAME                = \"Small Body Mapping Tool\"");
//    appendWithPadding(strbuf, "SHAPE_MODEL                  = \"" + smallBodyModel.getModelName() + "\"");
//
//    appendWithPadding(strbuf, "");
//    appendWithPadding(strbuf, "/* This DDR label describes one data file:                               */");
//    appendWithPadding(strbuf, "/* 1. A multiple-band backplane image file with wavelength-independent,  */");
//    appendWithPadding(strbuf, "/* spatial pixel-dependent geometric and timing information.             */");
//    appendWithPadding(strbuf, "");
//    appendWithPadding(strbuf, "OBJECT                       = FILE");
//
//    appendWithPadding(strbuf, "  ^IMAGE                     = \"" + imgName.getName() + "\"");
//
//    appendWithPadding(strbuf, "  RECORD_TYPE                = FIXED_LENGTH");
//    appendWithPadding(strbuf, "  RECORD_BYTES               = " + (imageHeight * 4));
//    appendWithPadding(strbuf, "  FILE_RECORDS               = " + (imageWidth * numBands));
//    appendWithPadding(strbuf, "");
//
//    appendWithPadding(strbuf, "  OBJECT                     = IMAGE");
//    appendWithPadding(strbuf, "    LINES                    = " + imageHeight);
//    appendWithPadding(strbuf, "    LINE_SAMPLES             = " + imageWidth);
//    appendWithPadding(strbuf, "    SAMPLE_TYPE              = IEEE_REAL");
//    appendWithPadding(strbuf, "    SAMPLE_BITS              = 32");
//    appendWithPadding(strbuf, "    CORE_NULL                = 16#F49DC5AE#"); // bit pattern of -1.0e32 in hex
//
//    appendWithPadding(strbuf, "    BANDS                    = " + numBands);
//    appendWithPadding(strbuf, "    BAND_STORAGE_TYPE        = BAND_SEQUENTIAL");
//    appendWithPadding(strbuf, "    BAND_NAME                = (\"Pixel value\",");
//    appendWithPadding(strbuf, "                                \"x coordinate of center of pixel, km\",");
//    appendWithPadding(strbuf, "                                \"y coordinate of center of pixel, km\",");
//    appendWithPadding(strbuf, "                                \"z coordinate of center of pixel, km\",");
//    appendWithPadding(strbuf, "                                \"Latitude, deg\",");
//    appendWithPadding(strbuf, "                                \"Longitude, deg\",");
//    appendWithPadding(strbuf, "                                \"Distance from center of body, km\",");
//    appendWithPadding(strbuf, "                                \"Incidence angle, deg\",");
//    appendWithPadding(strbuf, "                                \"Emission angle, deg\",");
//    appendWithPadding(strbuf, "                                \"Phase angle, deg\",");
//    appendWithPadding(strbuf, "                                \"Horizontal pixel scale, km per pixel\",");
//    appendWithPadding(strbuf, "                                \"Vertical pixel scale, km per pixel\",");
//    appendWithPadding(strbuf, "                                \"Slope, deg\",");
//    appendWithPadding(strbuf, "                                \"Elevation, m\",");
//    appendWithPadding(strbuf, "                                \"Gravitational acceleration, m/s^2\",");
//    appendWithPadding(strbuf, "                                \"Gravitational potential, J/kg\")");
//    appendWithPadding(strbuf, "");
//    appendWithPadding(strbuf, "  END_OBJECT                 = IMAGE");
//    appendWithPadding(strbuf, "END_OBJECT                   = FILE");
//
//    appendWithPadding(strbuf, "");
//    appendWithPadding(strbuf, "END");
//
//    // return strbuf.toString();
//    byte[] bytes = strbuf.toString().getBytes();
//    OutputStream out = new FileOutputStream(lblFileName.getAbsolutePath() + ".lbl");
//    out.write(bytes, 0, bytes.length);
//    out.close();
//}
//public float[] generateBackplanes()
//{
//  return generateBackplanes(false);
//}

///**
//* If <code>returnNullIfContainsLimb</code> then return null if any ray in the
//* direction of a pixel in the image does not intersect the asteroid. By setting
//* this boolean to true, you can (usually) determine whether or not the image
//* contains a limb without having to compute the entire backplane. Note that
//* this is a bit of a hack and a better way is needed to quickly determine if
//* there is a limb.
//*
//* @param returnNullIfContainsLimb
//* @return
//*/
//private float[] generateBackplanes(boolean returnNullIfContainsLimb)
//{
//  // We need to use cell normals not point normals for the calculations
//  vtkDataArray normals = null;
//  if (!returnNullIfContainsLimb)
//      normals = smallBodyModel.getCellNormals();
//
//  float[] data = new float[numBackplanes * imageHeight * imageWidth];
//
//  vtksbCellLocator cellLocator = smallBodyModel.getCellLocator();
//
//  // vtkPoints intersectPoints = new vtkPoints();
//  // vtkIdList intersectCells = new vtkIdList();
//  vtkGenericCell cell = new vtkGenericCell();
//
//  // For each pixel in the image we need to compute the vector
//  // from the spacecraft pointing in the direction of that pixel.
//  // To do this, for each row in the image compute the left and
//  // right vectors of the entire row. Then for each pixel in
//  // the row use the two vectors from either side to compute
//  // the vector of that pixel.
//  double[] corner1 = {
//          spacecraftPositionAdjusted[currentSlice][0] + frustum1Adjusted[currentSlice][0],
//          spacecraftPositionAdjusted[currentSlice][1] + frustum1Adjusted[currentSlice][1],
//          spacecraftPositionAdjusted[currentSlice][2] + frustum1Adjusted[currentSlice][2]
//  };
//  double[] corner2 = {
//          spacecraftPositionAdjusted[currentSlice][0] + frustum2Adjusted[currentSlice][0],
//          spacecraftPositionAdjusted[currentSlice][1] + frustum2Adjusted[currentSlice][1],
//          spacecraftPositionAdjusted[currentSlice][2] + frustum2Adjusted[currentSlice][2]
//  };
//  double[] corner3 = {
//          spacecraftPositionAdjusted[currentSlice][0] + frustum3Adjusted[currentSlice][0],
//          spacecraftPositionAdjusted[currentSlice][1] + frustum3Adjusted[currentSlice][1],
//          spacecraftPositionAdjusted[currentSlice][2] + frustum3Adjusted[currentSlice][2]
//  };
//  double[] vec12 = {
//          corner2[0] - corner1[0],
//          corner2[1] - corner1[1],
//          corner2[2] - corner1[2]
//  };
//  double[] vec13 = {
//          corner3[0] - corner1[0],
//          corner3[1] - corner1[1],
//          corner3[2] - corner1[2]
//  };
//
//  double horizScaleFactor = 2.0 * Math.tan(MathUtil.vsep(frustum1Adjusted[currentSlice], frustum3Adjusted[currentSlice]) / 2.0) / imageHeight;
//  double vertScaleFactor = 2.0 * Math.tan(MathUtil.vsep(frustum1Adjusted[currentSlice], frustum2Adjusted[currentSlice]) / 2.0) / imageWidth;
//
//  double scdist = MathUtil.vnorm(spacecraftPositionAdjusted[currentSlice]);
//
//  for (int i = 0; i < imageHeight; ++i)
//  {
//      // Compute the vector on the left of the row.
//      double fracHeight = ((double) i / (double) (imageHeight - 1));
//      double[] left = {
//              corner1[0] + fracHeight * vec13[0],
//              corner1[1] + fracHeight * vec13[1],
//              corner1[2] + fracHeight * vec13[2]
//      };
//
//      for (int j = 0; j < imageWidth; ++j)
//      {
//          // If we're just trying to know if there is a limb, we
//          // only need to do intersections around the boundary of
//          // the backplane, not the interior pixels.
//          if (returnNullIfContainsLimb)
//          {
//              if (j == 1 && i > 0 && i < imageHeight - 1)
//              {
//                  j = imageWidth - 2;
//                  continue;
//              }
//          }
//
//          double fracWidth = ((double) j / (double) (imageWidth - 1));
//          double[] vec = {
//                  left[0] + fracWidth * vec12[0],
//                  left[1] + fracWidth * vec12[1],
//                  left[2] + fracWidth * vec12[2]
//          };
//          vec[0] -= spacecraftPositionAdjusted[currentSlice][0];
//          vec[1] -= spacecraftPositionAdjusted[currentSlice][1];
//          vec[2] -= spacecraftPositionAdjusted[currentSlice][2];
//          MathUtil.unorm(vec, vec);
//
//          double[] lookPt = {
//                  spacecraftPositionAdjusted[currentSlice][0] + 2.0 * scdist * vec[0],
//                  spacecraftPositionAdjusted[currentSlice][1] + 2.0 * scdist * vec[1],
//                  spacecraftPositionAdjusted[currentSlice][2] + 2.0 * scdist * vec[2]
//          };
//
//          // cellLocator.IntersectWithLine(spacecraftPosition, lookPt, intersectPoints,
//          // intersectCells);
//          double tol = 1e-6;
//          double[] t = new double[1];
//          double[] x = new double[3];
//          double[] pcoords = new double[3];
//          int[] subId = new int[1];
//          int[] cellId = new int[1];
//          int result = cellLocator.IntersectWithLine(spacecraftPositionAdjusted[currentSlice], lookPt, tol, t, x, pcoords, subId, cellId, cell);
//
//          // if (intersectPoints.GetNumberOfPoints() == 0)
//          // System.out.println(i + " " + j + " " + intersectPoints.GetNumberOfPoints());
//
//          // int numberOfPoints = intersectPoints.GetNumberOfPoints();
//
//          if (result > 0)
//          {
//              // If we're just trying to know if there is a limb, do not
//              // compute the values of the backplane (It will crash since
//              // we don't have normals of the asteroid itself)
//              if (returnNullIfContainsLimb)
//                  continue;
//
//              // double[] closestPoint = intersectPoints.GetPoint(0);
//              // int closestCell = intersectCells.GetId(0);
//              double[] closestPoint = x;
//              int closestCell = cellId[0];
//              double closestDist = MathUtil.distanceBetween(closestPoint, getSpacecraftPositionAdjusted()[currentSlice]);
//
//              /*
//               * // compute the closest point to the spacecraft of all the intersecting
//               * points. if (numberOfPoints > 1) { for (int k=1; k<numberOfPoints; ++k) {
//               * double[] pt = intersectPoints.GetPoint(k); double dist =
//               * GeometryUtil.distanceBetween(pt, spacecraftPosition); if (dist < closestDist)
//               * { closestDist = dist; closestCell = intersectCells.GetId(k); closestPoint =
//               * pt; } } }
//               */
//
//              LatLon llr = MathUtil.reclat(closestPoint);
//              double lat = llr.lat * 180.0 / Math.PI;
//              double lon = llr.lon * 180.0 / Math.PI;
//              if (lon < 0.0)
//                  lon += 360.0;
//
//              double[] normal = normals.GetTuple3(closestCell);
//              double[] illumAngles = computeIlluminationAnglesAtPoint(closestPoint, normal);
//
//              double horizPixelScale = closestDist * horizScaleFactor;
//              double vertPixelScale = closestDist * vertScaleFactor;
//
//              double[] coloringValues;
//              try
//              {
//                  coloringValues = smallBodyModel.getAllColoringValues(closestPoint);
//              }
//              catch (@SuppressWarnings("unused") IOException e)
//              {
//                  coloringValues = new double[] {};
//              }
//              int colorValueSize = coloringValues.length;
//
//              data[index(j, i, BackplaneInfo.PIXEL.ordinal())] = (float) rawImage.GetScalarComponentAsFloat(j, i, 0, 0);
//              data[index(j, i, BackplaneInfo.X.ordinal())] = (float) closestPoint[0];
//              data[index(j, i, BackplaneInfo.Y.ordinal())] = (float) closestPoint[1];
//              data[index(j, i, BackplaneInfo.Z.ordinal())] = (float) closestPoint[2];
//              data[index(j, i, BackplaneInfo.LAT.ordinal())] = (float) lat;
//              data[index(j, i, BackplaneInfo.LON.ordinal())] = (float) lon;
//              data[index(j, i, BackplaneInfo.DIST.ordinal())] = (float) llr.rad;
//              data[index(j, i, BackplaneInfo.INC.ordinal())] = (float) illumAngles[0];
//              data[index(j, i, BackplaneInfo.EMI.ordinal())] = (float) illumAngles[1];
//              data[index(j, i, BackplaneInfo.PHASE.ordinal())] = (float) illumAngles[2];
//              data[index(j, i, BackplaneInfo.HSCALE.ordinal())] = (float) horizPixelScale;
//              data[index(j, i, BackplaneInfo.VSCALE.ordinal())] = (float) vertPixelScale;
//              data[index(j, i, BackplaneInfo.SLOPE.ordinal())] = colorValueSize > 0 ? (float) coloringValues[0] : 0.0F; // slope
//              data[index(j, i, BackplaneInfo.EL.ordinal())] = colorValueSize > 1 ? (float) coloringValues[1] : 0.0F; // elevation
//              data[index(j, i, BackplaneInfo.GRAVACC.ordinal())] = colorValueSize > 2 ? (float) coloringValues[2] : 0.0F; // grav acc;
//              data[index(j, i, BackplaneInfo.GRAVPOT.ordinal())] = colorValueSize > 3 ? (float) coloringValues[3] : 0.0F; // grav pot
//          }
//          else
//          {
//              if (returnNullIfContainsLimb)
//                  return null;
//
//              data[index(j, i, 0)] = (float) rawImage.GetScalarComponentAsFloat(j, i, 0, 0);
//              for (int k = 1; k < numBackplanes; ++k)
//                  data[index(j, i, k)] = PDS_NA;
//          }
//      }
//  }
//
//  return data;
//}

//public int index(int i, int j, int k)
//{
//  return ((k * imageHeight + j) * imageWidth + i);
//}
///**
//* Generate metadata to be used in PDS4 XML creation by parsing existing PDS3
//* label. By default creates a bare-bones metadata class that only contains the
//* output XML filename. Use this method to use an existing PDS3 label as the
//* source metadata on which to describe a new PDS4 product.
//*/
//public BPMetaBuilder pds3ToXmlMeta(String pds3Fname, String outXmlFname)
//{
// BPMetaBuilder metaDataBuilder = new BackPlanesXmlMeta.BPMetaBuilder(outXmlFname);
// return metaDataBuilder;
//}
//
///**
//* Generate metadata to be used in PDS4 XML creation by parsing existing PDS4
//* label. By default creates a bare-bones metdata class that only contains the
//* output XML filename. Use this method to use an existing PDS4 label as the
//* source metadata on which to describe a new PDS4 product.
//*/
//public BPMetaBuilder pds4ToXmlMeta(String pds4Fname, String outXmlFname)
//{
// BPMetaBuilder metaDataBuilder = new BackPlanesXmlMeta.BPMetaBuilder(outXmlFname);
// return metaDataBuilder;
//}
//
///**
//* Parse additional metadata from the fits file and add to the metaDataBuilder.
//*
//* @throws FitsException
//*/
//public BPMetaBuilder fitsToXmlMeta(File fitsFile, BPMetaBuilder metaDataBuilder) throws FitsException
//{
// return metaDataBuilder;
//}
//
///**
//* Generate XML document from XmlMetadata
//*
//* @param metaData - metadata to be used in populating XmlDoc
//* @param xmlTemplate - path to XML template file
//*/
//public BackPlanesXml metaToXmlDoc(BackPlanesXmlMeta metaData, String xmlTemplate)
//{
// BackPlanesXml xmlLabel = new BackPlanesXml(metaData, xmlTemplate);
// return xmlLabel;
//}


///*
//* FOR OFF-LIMB IMAGES
//*/
//
///**
//* No-argument entry point into the off-limb geometry-creation implementation.
//* This will create an offlimbPlaneCalculator and create the actors for the
//* plane and the boundaries.
//*/
//protected void loadOffLimbPlane()
//{
// double[] spacecraftPosition = new double[3];
// double[] focalPoint = new double[3];
// double[] upVector = new double[3];
// this.getCameraOrientation(spacecraftPosition, focalPoint, upVector);
// this.offLimbFootprintDepth = new Vector3D(spacecraftPosition).getNorm();
// calculator.loadOffLimbPlane(this, offLimbFootprintDepth);
// offLimbActor = calculator.getOffLimbActor();
// offLimbBoundaryActor = calculator.getOffLimbBoundaryActor();
// offLimbTexture = calculator.getOffLimbTexture();
// // set initial visibilities
// if (offLimbActor != null)
// {
//     offLimbActor.SetVisibility(offLimbVisibility ? 1 : 0);
//     offLimbBoundaryActor.SetVisibility(offLimbBoundaryVisibility ? 1 : 0);
// }
//}
//
///**
//* Set the distance of the off-limb plane from the camera position, along its
//* look vector. The associated polydata doesn't need to be regenerated every
//* time this method is called since the body's shadow in frustum coordinates
//* does not change with depth along the look axis. The call to loadOffLimbPlane
//* here does actually re-create the polydata, which should be unnecessary, and
//* needs to be fixed in a future release.
//*
//* @param footprintDepth
//*/
//public void setOffLimbPlaneDepth(double footprintDepth)
//{
// this.offLimbFootprintDepth = footprintDepth;
// calculator.loadOffLimbPlane(this, offLimbFootprintDepth);
//}
//
//public void setOffLimbFootprintAlpha(double alpha) // between 0-1
//{
// if (offLimbActor == null)
//     loadOffLimbPlane();
// offLimbActor.GetProperty().SetOpacity(alpha);
//}
//
//public boolean offLimbFootprintIsVisible()
//{
// return offLimbVisibility;
//}
//
///**
//* Set visibility of the off-limb footprint
//*
//* Checks if offLimbActor has been instantiated; if not then call
//* loadOffLimbPlane() before showing/hiding actors.
//*
//* @param visible
//*/
//public void setOffLimbFootprintVisibility(boolean visible)
//{
//
// offLimbVisibility = visible;
// offLimbBoundaryVisibility = visible;
// if (offLimbVisibility && offLimbActor == null)
//     loadOffLimbPlane();
//
// if (offLimbActor != null)
// {
//     offLimbActor.SetVisibility(visible ? 1 : 0);
//     offLimbBoundaryActor.SetVisibility(visible ? 1 : 0);
// }
//
// pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
//}
//
///**
//* Set visibility of the off-limb footprint boundary
//*
//* Checks if offLimbActor has been instantiated; if not then call
//* loadOffLimbPlane() before showing/hiding actors.
//*
//* @param visible
//*/
//public void setOffLimbBoundaryVisibility(boolean visible)
//{
//
// offLimbBoundaryVisibility = visible;
// if (offLimbActor == null)
//     loadOffLimbPlane();
// offLimbBoundaryActor.SetVisibility(visible ? 1 : 0);
//
// pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
//}
//
//public vtkTexture getOffLimbTexture()
//{
// if (offLimbTexture == null)
// { // if offlimbtexture is null, initialize it.
//     vtkImageData image = new vtkImageData();
//     image.DeepCopy(getDisplayedImage());
//     offLimbTexture = new vtkTexture();
//     offLimbTexture.SetInputData(image);
//     offLimbTexture.Modified();
// }
// return offLimbTexture;
//}
//
//public void setOffLimbTexture(vtkTexture offLimbTexture)
//{
// this.offLimbTexture = offLimbTexture;
//}
//
//public double getOffLimbPlaneDepth()
//{
// return offLimbFootprintDepth;
//}
//
//public void setContrastSynced(boolean selected)
//{
// this.contrastSynced = selected;
// if (contrastSynced)
// {
//     // if we just changed this to true, update the values to match
//     offLimbDisplayedRange = getDisplayedRange();
//     setOfflimbImageRange(offLimbDisplayedRange);
//     pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
// }
//}
//
//public boolean isContrastSynced()
//{
// return contrastSynced;
//}
//
//public void setOfflimbBoundaryColor(Color color)
//{
// this.offLimbBoundaryColor = color;
// offLimbBoundaryActor.GetProperty().SetColor(color.getRed() / 255., color.getGreen() / 255., color.getBlue() / 255.);
// offLimbBoundaryActor.Modified();
// pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
//}
//
//public Color getOfflimbBoundaryColor()
//{
// return offLimbBoundaryColor;
//}
//public IntensityRange getOffLimbDisplayedRange()
//{
//if (offLimbDisplayedRange == null)
//{
//   offLimbDisplayedRange = new IntensityRange(0, 255);
//}
//
//return offLimbDisplayedRange;
//}
//
//public void setOfflimbImageRange(IntensityRange intensityRange)
//{
//
//IntensityRange displayedRange = getOffLimbDisplayedRange();
//if (intensityRange == null || displayedRange.min != intensityRange.min || displayedRange.max != intensityRange.max)
//{
//   if (intensityRange != null)
//   {
//       offLimbDisplayedRange = intensityRange;
//       saveImageInfo();
//   }
//
//   if (rawImage != null)
//   {
//       vtkImageData image = getImageWithDisplayedRange(intensityRange, true);
//
//       if (offLimbTexture == null && !Configuration.isHeadless())
//           offLimbTexture = new vtkTexture();
//       if (offLimbTexture != null)
//       {
//           offLimbTexture.SetInputData(image);
//           image.Delete();
//           offLimbTexture.Modified();
//       }
//   }
//
//   this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, this);
//}
//
//}