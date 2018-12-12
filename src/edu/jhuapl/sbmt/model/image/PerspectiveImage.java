package edu.jhuapl.sbmt.model.image;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import vtk.vtkCell;
import vtk.vtkCellData;
import vtk.vtkDataArray;
import vtk.vtkImageCanvasSource2D;
import vtk.vtkImageData;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkProp;
import vtk.vtkProperty;
import vtk.vtkTexture;

import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.util.BoundingBox;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.IntensityRange;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.io.PerspectiveImageIO;
import edu.jhuapl.sbmt.model.image.io.PerspectiveImageIOSupportedFiletypes;
import edu.jhuapl.sbmt.model.pointing.BaseInfoFileIO;
import edu.jhuapl.sbmt.model.pointing.BaseLabelFileIO;
import edu.jhuapl.sbmt.model.pointing.BaseSumFileIO;
import edu.jhuapl.sbmt.model.pointing.InfoFileIO;
import edu.jhuapl.sbmt.model.pointing.LabelFileIO;
import edu.jhuapl.sbmt.model.pointing.SumFileIO;
import edu.jhuapl.sbmt.util.BackPlanesPDS4XML;
import edu.jhuapl.sbmt.util.BackPlanesXml;
import edu.jhuapl.sbmt.util.BackPlanesXmlMeta;
import edu.jhuapl.sbmt.util.BackPlanesXmlMeta.BPMetaBuilder;
import edu.jhuapl.sbmt.util.BackplaneInfo;
import edu.jhuapl.sbmt.util.BackplanesLabel;
import edu.jhuapl.sbmt.util.ImageDataUtil;
import edu.jhuapl.sbmt.util.VtkENVIReader;

import nom.tam.fits.FitsException;

/**
 * This class represents an abstract image of a spacecraft imager instrument.
 */
abstract public class PerspectiveImage extends Image implements PropertyChangeListener, BackPlanesPDS4XML, BackplanesLabel
{
    public static final String NUMBER_EXPOSURES = "NUMBER_EXPOSURES";
    public static final String SUMFILENAMES = "SumfileNames";
    public static final String INFOFILENAMES = "InfofileNames";
    public static final float PDS_NA = -ImageDataUtil.FILL_CUTOFF;


    public static final double[] bodyOrigin = { 0.0, 0.0, 0.0 };

    private SmallBodyModel smallBodyModel;
    public SmallBodyModel getSmallBodyModel() { return smallBodyModel; }

    private ModelManager modelManager;
    protected ModelManager getModelManager() { return modelManager; }

//    private vtkImageData rawImage;
//    private vtkImageData displayedImage;
    private int currentSlice = 0;

    private double rotation = 0.0;
    public double getRotation() { return rotation; }

    private String flip = "None";
    public String getFlip() { return flip; }

    private boolean useDefaultFootprint = true;
//    private vtkPolyData[] footprint = new vtkPolyData[1];
    private boolean[] footprintGenerated = new boolean[1];
//    private final vtkPolyData[] shiftedFootprint = new vtkPolyData[1];

//    private vtkActor footprintActor;
//    private List<vtkProp> footprintActors = new ArrayList<vtkProp>();

//    vtkPolyData frustumPolyData;
//    private vtkActor frustumActor;

//    private vtkPolyDataNormals normalsFilter;

//    private vtkFloatArray textureCoords;

    private boolean normalsGenerated = false;

    private double minIncidence = Double.MAX_VALUE;
    private double maxIncidence = -Double.MAX_VALUE;
    private double minEmission = Double.MAX_VALUE;
    private double maxEmission = -Double.MAX_VALUE;
    private double minPhase = Double.MAX_VALUE;
    private double maxPhase = -Double.MAX_VALUE;
    private double minHorizontalPixelScale = Double.MAX_VALUE;
    private double maxHorizontalPixelScale = -Double.MAX_VALUE;
    private double meanHorizontalPixelScale = 0.0;
    private double minVerticalPixelScale = Double.MAX_VALUE;
    private double maxVerticalPixelScale = -Double.MAX_VALUE;
    private double meanVerticalPixelScale = 0.0;

    private float[] minValue = new float[1];
    private float[] maxValue = new float[1];

    private int[] currentMask = new int[4];

    private IntensityRange[] displayedRange = new IntensityRange[1];
    private double imageOpacity = 1.0;

    private double[][] spacecraftPositionOriginal = new double[1][3];
    private double[][] frustum1Original = new double[1][3];
    private double[][] frustum2Original = new double[1][3];
    private double[][] frustum3Original = new double[1][3];
    private double[][] frustum4Original = new double[1][3];
    private double[][] boresightDirectionOriginal = new double[1][3];
    private double[][] upVectorOriginal = new double[1][3];
    private double[][] sunPositionOriginal = new double[1][3];

    protected double[][] spacecraftPositionAdjusted = new double[1][3];
    protected double[][] frustum1Adjusted = new double[1][3];
    protected double[][] frustum2Adjusted = new double[1][3];
    protected double[][] frustum3Adjusted = new double[1][3];
    protected double[][] frustum4Adjusted = new double[1][3];
    private double[][] boresightDirectionAdjusted = new double[1][3];
    private double[][] upVectorAdjusted = new double[1][3];
    private double[][] sunPositionAdjusted = new double[1][3];

    // location in pixel coordinates of the target origin for the adjusted frustum
    private double[] targetPixelCoordinates = { Double.MAX_VALUE, Double.MAX_VALUE };

    // offset in world coordinates of the adjusted frustum from the loaded frustum
    //    private double[] offsetPixelCoordinates = { Double.MAX_VALUE, Double.MAX_VALUE };

    private double[] zoomFactor = { 1.0 };

    private double[] rotationOffset = { 0.0 };

    // apply all frame adjustments if true
    private boolean[] applyFrameAdjustments = { true };



    private boolean showFrustum = false;
    private boolean simulateLighting = false;

    private String startTime = "";
    private String stopTime = "";

//    private vtkImageCanvasSource2D maskSource;

    private int imageWidth;
    private int imageHeight;
    protected int imageDepth = 1;
    private int numBands = BackplaneInfo.values().length;

    private String imageFileFullPath;
//    private String pngFileFullPath; // The actual path of the PNG image stored on the local disk (after downloading from the server)
//    private String fitFileFullPath; // The actual path of the FITS image stored on the local disk (after downloading from the server)
//    private String enviFileFullPath; // The actual path of the ENVI binary stored on the local disk (after downloading from the server)
    private String labelFileFullPath;
    private String infoFileFullPath;
    private String sumFileFullPath;
//    protected int fitFileImageExtension = 0; // Default is to use the primary FITS image.

//    protected vtkTexture imageTexture;

    // If true, then the footprint is generated by intersecting a frustum with the asteroid.
    // This setting is used when generating the files on the server.
    // If false, then the footprint is downloaded from the server. This setting is used by the GUI.
    private static boolean generateFootprint = true;

    private boolean loadPointingOnly;




    protected boolean transposeFITSData = false;
    private double numberOfPixels = 0.0;
    private double numberOfLines = 0.0;
    private static double[] origin = { 0.0, 0.0, 0.0 };
    private String imageName;

    private static final Vector3D i = new Vector3D(1.0, 0.0, 0.0);
    private static final Vector3D j = new Vector3D(0.0, 1.0, 0.0);
    private static final Vector3D k = new Vector3D(0.0, 0.0, 1.0);


    private double[] q = new double[4];
    private double[] cx = new double[3];
    private double[] cy = new double[3];
    private double[] cz = new double[3];

    private double focalLengthMillimeters = 100.0;
    private double npx = 4096.0;
    private double nln = 32.0;
    private double kmatrix00 = 1.0;
    private double kmatrix11 = 1.0;

    //For Label files
    private String targetName = null;
    private String instrumentId = null;
    private String filterName = null;
    private String objectName = null;

    private String startTimeString = null;
    private String stopTimeString = null;
    private double exposureDuration = 0.0;

    private String scTargetPositionString = null;
    private String targetSunPositionString = null;
    private String scOrientationString = null;
    private Rotation scOrientation = null;

//    /*
//     * For off-limb images
//     */
//    vtkPolyData offLimbPlane=null;
//    private vtkActor offLimbActor;
//    private vtkTexture offLimbTexture;
//    vtkPolyData offLimbBoundary=null;
//    private vtkActor offLimbBoundaryActor;
//    double offLimbFootprintDepth;
//    private boolean offLimbVisibility;
//    private boolean offLimbBoundaryVisibility;
//    OffLimbPlaneCalculator calculator = new OffLimbPlaneCalculator();
    IOfflimbRenderEngine offlimb;
    IVTKRenderEngine vtkRenderer;

    PerspectiveImageIOSupportedFiletypes imageFileType = PerspectiveImageIOSupportedFiletypes.FITS;

    /*
     * Pointing file interfaces - initialize to base versions, but allow children to override
     */
    InfoFileIO infoFileIO;
    SumFileIO sumFileIO;
    LabelFileIO labelFileIO;
    protected PerspectiveImageIO fileIO;


    public PerspectiveImage(ImageKey key,
            SmallBodyModel smallBodyModel,
            boolean loadPointingOnly, boolean transposeData) throws FitsException, IOException
    {
        this(key, smallBodyModel, null, loadPointingOnly, 0, transposeData);

    }


    public PerspectiveImage(ImageKey key,
            SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        this(key, smallBodyModel, null, loadPointingOnly);
    }

    public PerspectiveImage(ImageKey key,
            SmallBodyModel smallBodyModel,
            boolean loadPointingOnly, int currentSlice) throws FitsException, IOException
    {
        this(key, smallBodyModel, null, loadPointingOnly, currentSlice);
    }


    /**
     * If loadPointingOnly is true then only pointing information about this
     * image will be downloaded/loaded. The image itself will not be loaded.
     * Used by ImageBoundary to get pointing info.
     */
    public PerspectiveImage(ImageKey key,
            SmallBodyModel smallBodyModel,
            ModelManager modelManager,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        this(key, smallBodyModel, modelManager, loadPointingOnly, 0);
    }

    /**
     * If loadPointingOnly is true then only pointing information about this
     * image will be downloaded/loaded. The image itself will not be loaded.
     * Used by ImageBoundary to get pointing info.
     */
    public PerspectiveImage(ImageKey key,
            SmallBodyModel smallBodyModel,
            ModelManager modelManager,
            boolean loadPointingOnly, int currentSlice, boolean transposeData) throws FitsException, IOException
    {
        super(key);
        this.currentSlice = currentSlice;
        this.smallBodyModel = smallBodyModel;
        this.modelManager = modelManager;
        this.loadPointingOnly = loadPointingOnly;
        this.rotation = key.instrument != null ? key.instrument.rotation : 0.0;
        this.flip = key.instrument != null ? key.instrument.flip : "None";
        this.transposeFITSData = transposeData;
        this.fileIO = new PerspectiveImageIO(this);
        infoFileIO = new BaseInfoFileIO(this);
        sumFileIO = new BaseSumFileIO(this);
        labelFileIO = new BaseLabelFileIO();
        this.vtkRenderer = new PerspectiveImageVTKRenderEngine(this);
        initialize();
//        this.offlimb = new PerspectiveImageVTKOfflimbRenderEngine(this);
//        offlimb.setOffLimbFootprintVisibility(true);
//        offlimb.setOffLimbBoundaryVisibility(true);

    }

    /**
     * If loadPointingOnly is true then only pointing information about this
     * image will be downloaded/loaded. The image itself will not be loaded.
     * Used by ImageBoundary to get pointing info.
     */
    public PerspectiveImage(ImageKey key,
            SmallBodyModel smallBodyModel,
            ModelManager modelManager,
            boolean loadPointingOnly, int currentSlice) throws FitsException, IOException
    {
        super(key);
        this.currentSlice = currentSlice;
        this.smallBodyModel = smallBodyModel;
        this.modelManager = modelManager;
        this.loadPointingOnly = loadPointingOnly;
        this.rotation = key.instrument != null ? key.instrument.rotation : 0.0;
        this.flip = key.instrument != null ? key.instrument.flip : "None";
        this.fileIO = new PerspectiveImageIO(this);
        infoFileIO = new BaseInfoFileIO(this);
        sumFileIO = new BaseSumFileIO(this);
        labelFileIO = new BaseLabelFileIO();
//        System.out.println("PerspectiveImage: PerspectiveImage: key is " + key);
        this.vtkRenderer = new PerspectiveImageVTKRenderEngine(this);
        initialize();
//        this.offlimb = new PerspectiveImageVTKOfflimbRenderEngine(this);
//        offlimb.setOffLimbFootprintVisibility(true);
//        offlimb.setOffLimbBoundaryVisibility(true);

    }

    protected void initialize() throws FitsException, IOException
    {
//        footprint[0] = new vtkPolyData();
//        shiftedFootprint[0] = new vtkPolyData();
        displayedRange[0] = new IntensityRange(1,0);
        System.out.println("PerspectiveImage: initialize: " + key);
        switch (key.source)
        {
        case LOCAL_PERSPECTIVE:
            //TODO GENERALIZE
//            loadImageInfoFromConfigFile();
//            infoFileFullPath = initLocalInfoFileFullPath();
//            sumFileFullPath = initLocalSumfileFullPath();
            if (!loadPointingOnly)
            {
                if (imageFileType == PerspectiveImageIOSupportedFiletypes.FITS) imageFileFullPath = initLocalFitFileFullPath();
                if (imageFileType == PerspectiveImageIOSupportedFiletypes.ENVI) imageFileFullPath = initLocalEnviFileFullPath();
                if (imageFileType == PerspectiveImageIOSupportedFiletypes.PNG) imageFileFullPath = initLocalPngFileFullPath();

//                fitFileFullPath = initLocalFitFileFullPath();
//                pngFileFullPath = initLocalPngFileFullPath();
//                enviFileFullPath = initLocalEnviFileFullPath();
            }
            break;
        case SPICE:
        case CORRECTED_SPICE:
            infoFileFullPath = initializeInfoFileFullPath();
            break;
        case LABEL:
            setLabelFileFullPath(initializeLabelFileFullPath());
            break;
        default:
            sumFileIO = new BaseSumFileIO(this);
//            sumFileFullPath = sumFileIO.initLocalSumfileFullPath();
            sumFileFullPath = initializeSumfileFullPath();
            System.out.println("PerspectiveImage: initialize: sum file full path " + sumFileFullPath);
            break;
        }
        System.out.println("PerspectiveImage: initialize: load pointing only " + loadPointingOnly);
        if (!loadPointingOnly)
        {
            if (imageFileType == PerspectiveImageIOSupportedFiletypes.FITS) imageFileFullPath = initializeFitFileFullPath();
            if (imageFileType == PerspectiveImageIOSupportedFiletypes.ENVI) imageFileFullPath = initializeEnviFileFullPath();
            if (imageFileType == PerspectiveImageIOSupportedFiletypes.PNG) imageFileFullPath = initializePngFileFullPath();

//            fitFileFullPath = initializeFitFileFullPath();
//            pngFileFullPath = initializePngFileFullPath();
//            enviFileFullPath = initializeEnviFileFullPath();
        }


        //why is this called for Custom images with INFO files?
//        if (key.source.equals(ImageSource.LOCAL_PERSPECTIVE))
//        {
//            loadImageInfoFromConfigFile();
//        }
//
//        if (!loadPointingOnly)
//        {
//            if (key.source.equals(ImageSource.LOCAL_PERSPECTIVE))
//            {
//                fitFileFullPath = initLocalFitFileFullPath();
//                pngFileFullPath = initLocalPngFileFullPath();
//                enviFileFullPath = initLocalEnviFileFullPath();
//            }
//            else
//            {
//                fitFileFullPath = initializeFitFileFullPath();
//                pngFileFullPath = initializePngFileFullPath();
//                enviFileFullPath = initializeEnviFileFullPath();
//            }
//        }
//
//        if (key.source.equals(ImageSource.LOCAL_PERSPECTIVE))
//        {
//            infoFileFullPath = initLocalInfoFileFullPath();
//            sumFileFullPath = initLocalSumfileFullPath();
//        }
//        else if (key.source.equals(ImageSource.SPICE) || key.source.equals(ImageSource.CORRECTED_SPICE))
//        {
//            infoFileFullPath = initializeInfoFileFullPath();
//        }
//        else if (key.source.equals(ImageSource.LABEL))
//            setLabelFileFullPath(initializeLabelFileFullPath());
//        else
//            sumFileFullPath = initializeSumfileFullPath();
//        imageDepth = loadNumSlices();
        fileIO.loadNumSlices(imageFileFullPath, imageFileType);
//        if (getFitFileFullPath() != null)
//        {
//            fileIO.loadFromFile(getFitFileFullPath());
//        }
//        else
//        {
//            fileIO.loadFromFile(getEnviFileFullPath());
//        }

        if (imageDepth > 1)
            initSpacecraftStateVariables();

        loadPointing();

        if (!loadPointingOnly)
        {
            String name = initializeFitFileFullPath();
            fileIO.loadFromFile(name);
            loadImage();
            updateFrameAdjustments();
        }

//        maxFrustumDepth=new double[imageDepth];
//        minFrustumDepth=new double[imageDepth];
    }

    private void copySpacecraftState()
    {
        System.out.println("PerspectiveImage: copySpacecraftState: ");
        int nslices = getNumberBands();
//        System.out.println("PerspectiveImage: copySpacecraftState: number of slices " + nslices);
        for (int i = 0; i<nslices; i++)
        {
//            System.out.println("PerspectiveImage: copySpacecraftState: spacecraft pos orig " + spacecraftPositionOriginal[0][0] + " " + spacecraftPositionOriginal[0][1] + " " + spacecraftPositionOriginal[0][2]);
            spacecraftPositionAdjusted = MathUtil.copy(spacecraftPositionOriginal);
            frustum1Adjusted = MathUtil.copy(frustum1Original);
            frustum2Adjusted = MathUtil.copy(frustum2Original);
            frustum3Adjusted = MathUtil.copy(frustum3Original);
            frustum4Adjusted = MathUtil.copy(frustum4Original);
            boresightDirectionAdjusted = MathUtil.copy(boresightDirectionOriginal);
            upVectorAdjusted = MathUtil.copy(upVectorOriginal);
            sunPositionAdjusted = MathUtil.copy(sunPositionOriginal);
        }
    }

    public void resetSpacecraftState()
    {
        System.out.println("PerspectiveImage: resetSpacecraftState: resetting sc state");
        copySpacecraftState();
        int nslices = getNumberBands();
        for (int i = 0; i<nslices; i++)
        {
            vtkRenderer.getFrusta()[i] = null;
            footprintGenerated[i] = false;
        }

        //        offsetPixelCoordinates[0] = Double.MAX_VALUE;
        //        offsetPixelCoordinates[1] = Double.MAX_VALUE;
        targetPixelCoordinates[0] = Double.MAX_VALUE;
        targetPixelCoordinates[1] = Double.MAX_VALUE;
        rotationOffset[0] = 0.0;
        zoomFactor[0] = 1.0;

        updateFrameAdjustments();

        vtkRenderer.loadFootprint();
        vtkRenderer.calculateFrustum();
        infoFileIO.deleteAdjustedImageInfo();
        //        saveImageInfo();
    }

    public void saveImageInfo()
    {
        infoFileIO.saveImageInfo();
    }

    public void saveImageInfo(String filename)
    {
        infoFileIO.saveImageInfo(filename);
    }


    public void setTargetPixelCoordinates(double[] frustumCenterPixel)
    {
        //        System.out.println("setFrustumOffset(): " + frustumCenterPixel[1] + " " + frustumCenterPixel[0]);

        this.targetPixelCoordinates[0] = frustumCenterPixel[0];
        this.targetPixelCoordinates[1] = frustumCenterPixel[1];

        updateFrameAdjustments();

        vtkRenderer.loadFootprint();
        vtkRenderer.calculateFrustum();
        infoFileIO.saveImageInfo();
    }

    //    public void setPixelOffset(double[] pixelOffset)
    //    {
    ////        System.out.println("setFrustumOffset(): " + frustumCenterPixel[1] + " " + frustumCenterPixel[0]);
    //
    //        this.offsetPixelCoordinates[0] = pixelOffset[0];
    //        this.offsetPixelCoordinates[1] = pixelOffset[1];
    //
    //        updateFrameAdjustments();
    //
    //        loadFootprint();
    //        calculateFrustum();
    //        saveImageInfo();
    //    }

    public void setRotationOffset(double offset)
    {
        //        System.out.println("setRotationOffset(): " + offset);

        if (rotationOffset == null)
            rotationOffset = new double[1];

        rotationOffset[0] = offset;

        updateFrameAdjustments();

        vtkRenderer.loadFootprint();
        vtkRenderer.calculateFrustum();
        infoFileIO.saveImageInfo();
    }

    public void setZoomFactor(double offset)
    {
        //        System.out.println("setZoomFactor(): " + offset);

        if (zoomFactor == null)
        {
            zoomFactor = new double[1];
            zoomFactor[0] = 1.0;
        }

        zoomFactor[0] = offset;

        updateFrameAdjustments();

        vtkRenderer.loadFootprint();
        vtkRenderer.calculateFrustum();
        infoFileIO.saveImageInfo();
    }

    public void setApplyFrameAdjustments(boolean state)
    {
        //        System.out.println("setApplyFrameAdjustments(): " + state);
        applyFrameAdjustments[0] = state;
        updateFrameAdjustments();
        vtkRenderer.loadFootprint();
        vtkRenderer.calculateFrustum();
        infoFileIO.saveImageInfo();
    }

    public boolean getApplyFramedAdjustments() { return applyFrameAdjustments[0]; }


    private void updateFrameAdjustments()
    {
        System.out.println("PerspectiveImage: updateFrameAdjustments: updating frame adjustments");
        // adjust wrt the original spacecraft pointing direction, not the previous adjusted one
        copySpacecraftState();

        if (applyFrameAdjustments[0])
        {
            if (targetPixelCoordinates[0] != Double.MAX_VALUE && targetPixelCoordinates[1]  != Double.MAX_VALUE)
            {
                int height = getImageHeight();
                int width = getImageWidth();
                double line = height - 1 - targetPixelCoordinates[0];
                double sample = targetPixelCoordinates[1];

                double[] newTargetPixelDirection = getPixelDirection(sample, line);
                rotateTargetPixelDirectionToLocalOrigin(newTargetPixelDirection);
            }
            //            else if (offsetPixelCoordinates[0] != Double.MAX_VALUE && offsetPixelCoordinates[1]  != Double.MAX_VALUE)
            //            {
            //                int height = getImageHeight();
            //                int width = getImageWidth();
            //                double line = height - 1 - offsetPixelCoordinates[0];
            //                double sample = offsetPixelCoordinates[1];
            //
            //                double[] newOffsetPixelDirection = getPixelDirection(sample, line);
            //                rotateBoresightTo(newOffsetPixelDirection);
            //            }

            if (rotationOffset[0] != 0.0)
            {
                rotateFrameAboutTarget(rotationOffset[0]);
            }
            if (zoomFactor[0] != 1.0)
            {
                zoomFrame(zoomFactor[0]);
            }
        }

        //        int slice = getCurrentSlice();
        int nslices = getNumberBands();
        for (int slice = 0; slice<nslices; slice++)
        {
            vtkRenderer.getFrusta()[slice] = null;
            footprintGenerated[slice] = false;
        }
    }




    private void zoomFrame(double zoomFactor)
    {
        //        System.out.println("zoomFrame(" + zoomFactor + ")");
        //        Vector3D spacecraftPositionVector = new Vector3D(spacecraftPositionOriginal[currentSlice]);
        //        Vector3D spacecraftToOriginVector = spacecraftPositionVector.scalarMultiply(-1.0);
        //        Vector3D originPointingVector = spacecraftToOriginVector.normalize();
        //        double distance = spacecraftToOriginVector.getNorm();
        //        Vector3D deltaVector = originPointingVector.scalarMultiply(distance * (zoomFactor - 1.0));
        //        double[] delta = { deltaVector.getX(), deltaVector.getY(), deltaVector.getZ() };

        double zoomRatio = 1.0 / zoomFactor;

        int nslices = getNumberBands();
        for (int slice = 0; slice<nslices; slice++)
        {
            for (int i=0; i<3; i++)
                spacecraftPositionAdjusted[currentSlice][i] = spacecraftPositionOriginal[currentSlice][i] * zoomRatio;
            vtkRenderer.getFrusta()[slice] = null;
            footprintGenerated[slice] = false;
        }
    }


    private void rotateFrameAboutTarget(double angleDegrees)
    {
        //        Vector3D axis = new Vector3D(boresightDirectionOriginal[currentSlice]);
        Vector3D axis = new Vector3D(spacecraftPositionAdjusted[currentSlice]);
        axis.normalize();
        axis.negate();
        Rotation rotation = new Rotation(axis, Math.toRadians(angleDegrees));

        //        int slice = getCurrentSlice();
        int nslices = getNumberBands();
        for (int slice = 0; slice<nslices; slice++)
        {
            MathUtil.rotateVector(frustum1Adjusted[slice], rotation, frustum1Adjusted[slice]);
            MathUtil.rotateVector(frustum2Adjusted[slice], rotation, frustum2Adjusted[slice]);
            MathUtil.rotateVector(frustum3Adjusted[slice], rotation, frustum3Adjusted[slice]);
            MathUtil.rotateVector(frustum4Adjusted[slice], rotation, frustum4Adjusted[slice]);
            MathUtil.rotateVector(boresightDirectionAdjusted[slice], rotation, boresightDirectionAdjusted[slice]);

            vtkRenderer.getFrusta()[slice] = null;
            footprintGenerated[slice] = false;
        }
    }

    public void moveTargetPixelCoordinates(double[] pixelDelta)
    {
        //        System.out.println("moveTargetPixelCoordinates(): " + pixelDelta[1] + " " + pixelDelta[0]);

        double height = (double)getImageHeight();
        double width = (double)getImageWidth();
        if (targetPixelCoordinates[0] == Double.MAX_VALUE || targetPixelCoordinates[1] == Double.MAX_VALUE)
        {
            targetPixelCoordinates = getPixelFromPoint(bodyOrigin);
            targetPixelCoordinates[0] = height - 1 - targetPixelCoordinates[0];
        }
        double line = this.targetPixelCoordinates[0] + pixelDelta[0];
        double sample = targetPixelCoordinates[1] + pixelDelta[1];
        double[] newFrustumCenterPixel = { line, sample };

        setTargetPixelCoordinates(newFrustumCenterPixel);
    }

    //    public void moveOffsetPixelCoordinates(double[] pixelDelta)
    //    {
    ////        System.out.println("moveOffsetPixelCoordinates(): " + pixelDelta[1] + " " + pixelDelta[0]);
    //
    //        double height = (double)getImageHeight();
    //        double width = (double)getImageWidth();
    //        if (offsetPixelCoordinates[0] == Double.MAX_VALUE || offsetPixelCoordinates[1] == Double.MAX_VALUE)
    //        {
    //            offsetPixelCoordinates[0] = 0.0;
    //            offsetPixelCoordinates[1] = 0.0;
    //        }
    //        double line = offsetPixelCoordinates[0] + pixelDelta[0];
    //        double sample = offsetPixelCoordinates[1] + pixelDelta[1];
    //        double[] newPixelOffset = { line, sample };
    //
    //        setPixelOffset(newPixelOffset);
    //    }

    public void moveRotationAngleBy(double rotationDelta)
    {
        //        System.out.println("moveRotationAngleBy(): " + rotationDelta);

        double newRotationOffset = rotationOffset[0] + rotationDelta;

        setRotationOffset(newRotationOffset);
    }

    public void moveZoomFactorBy(double zoomDelta)
    {
        //        System.out.println("moveZoomDeltaBy(): " + zoomDelta);

        double newZoomFactor = zoomFactor[0] * zoomDelta;

        setZoomFactor(newZoomFactor);
    }

    //    private void rotateBoresightDirectionTo(double[] newDirection)
    //    {
    //        Vector3D oldDirectionVector = new Vector3D(boresightDirectionOriginal[currentSlice]);
    //        Vector3D newDirectionVector = new Vector3D(newDirection);
    //
    //        Rotation rotation = new Rotation(oldDirectionVector, newDirectionVector);
    //
    //        int nslices = getNumberBands();
    //        for (int i = 0; i<nslices; i++)
    //        {
    //            MathUtil.rotateVector(frustum1Adjusted[i], rotation, frustum1Adjusted[i]);
    //            MathUtil.rotateVector(frustum2Adjusted[i], rotation, frustum2Adjusted[i]);
    //            MathUtil.rotateVector(frustum3Adjusted[i], rotation, frustum3Adjusted[i]);
    //            MathUtil.rotateVector(frustum4Adjusted[i], rotation, frustum4Adjusted[i]);
    //            MathUtil.rotateVector(boresightDirectionAdjusted[i], rotation, boresightDirectionAdjusted[i]);
    //
    //            frusta[i] = null;
    //            footprintGenerated[i] = false;
    //        }
    //
    ////        loadFootprint();
    ////        calculateFrustum();
    //    }


    private void rotateTargetPixelDirectionToLocalOrigin(double[] direction)
    {
        Vector3D directionVector = new Vector3D(direction);
        Vector3D spacecraftPositionVector = new Vector3D(spacecraftPositionOriginal[currentSlice]);
        Vector3D spacecraftToOriginVector = spacecraftPositionVector.scalarMultiply(-1.0);
        Vector3D originPointingVector = spacecraftToOriginVector.normalize();

        Rotation rotation = new Rotation(directionVector, originPointingVector);

        //        int slice = getCurrentSlice();
        int nslices = getNumberBands();
        for (int slice = 0; slice<nslices; slice++)
        {
            MathUtil.rotateVector(frustum1Adjusted[slice], rotation, frustum1Adjusted[slice]);
            MathUtil.rotateVector(frustum2Adjusted[slice], rotation, frustum2Adjusted[slice]);
            MathUtil.rotateVector(frustum3Adjusted[slice], rotation, frustum3Adjusted[slice]);
            MathUtil.rotateVector(frustum4Adjusted[slice], rotation, frustum4Adjusted[slice]);
            MathUtil.rotateVector(boresightDirectionAdjusted[slice], rotation, boresightDirectionAdjusted[slice]);

            vtkRenderer.getFrusta()[slice] = null;
            footprintGenerated[slice] = false;
        }
    }

    private void rotateBoresightTo(double[] direction)
    {
        Vector3D directionVector = new Vector3D(direction);
        Vector3D boresightVector = new Vector3D(getBoresightDirection());

        Rotation rotation = new Rotation(boresightVector, directionVector);

        int nslices = getNumberBands();
        for (int slice = 0; slice<nslices; slice++)
        {
            MathUtil.rotateVector(frustum1Adjusted[slice], rotation, frustum1Adjusted[slice]);
            MathUtil.rotateVector(frustum2Adjusted[slice], rotation, frustum2Adjusted[slice]);
            MathUtil.rotateVector(frustum3Adjusted[slice], rotation, frustum3Adjusted[slice]);
            MathUtil.rotateVector(frustum4Adjusted[slice], rotation, frustum4Adjusted[slice]);
            MathUtil.rotateVector(boresightDirectionAdjusted[slice], rotation, boresightDirectionAdjusted[slice]);

            vtkRenderer.getFrusta()[slice] = null;
            footprintGenerated[slice] = false;
        }
    }

    public void calculateFrustum()
    {
        System.out.println("PerspectiveImage: calculateFrustum: ");
        vtkRenderer.calculateFrustum();
    }


//    public void calculateFrustum()
//    {
//        //        System.out.println("recalculateFrustum()");
//        frustumPolyData = new vtkPolyData();
//
//        vtkPoints points = new vtkPoints();
//        vtkCellArray lines = new vtkCellArray();
//
//        vtkIdList idList = new vtkIdList();
//        idList.SetNumberOfIds(2);
//
//        double maxFrustumRayLength = MathUtil.vnorm(spacecraftPositionAdjusted[currentSlice]) + smallBodyModel.getBoundingBoxDiagonalLength();
//        double[] origin = spacecraftPositionAdjusted[currentSlice];
//        double[] UL = {origin[0]+frustum1Adjusted[currentSlice][0]*maxFrustumRayLength, origin[1]+frustum1Adjusted[currentSlice][1]*maxFrustumRayLength, origin[2]+frustum1Adjusted[currentSlice][2]*maxFrustumRayLength};
//        double[] UR = {origin[0]+frustum2Adjusted[currentSlice][0]*maxFrustumRayLength, origin[1]+frustum2Adjusted[currentSlice][1]*maxFrustumRayLength, origin[2]+frustum2Adjusted[currentSlice][2]*maxFrustumRayLength};
//        double[] LL = {origin[0]+frustum3Adjusted[currentSlice][0]*maxFrustumRayLength, origin[1]+frustum3Adjusted[currentSlice][1]*maxFrustumRayLength, origin[2]+frustum3Adjusted[currentSlice][2]*maxFrustumRayLength};
//        double[] LR = {origin[0]+frustum4Adjusted[currentSlice][0]*maxFrustumRayLength, origin[1]+frustum4Adjusted[currentSlice][1]*maxFrustumRayLength, origin[2]+frustum4Adjusted[currentSlice][2]*maxFrustumRayLength};
//
//        double minFrustumRayLength = MathUtil.vnorm(spacecraftPositionAdjusted[currentSlice]) - smallBodyModel.getBoundingBoxDiagonalLength();
//        maxFrustumDepth[currentSlice]=maxFrustumRayLength;  // a reasonable approximation for a max bound on the frustum depth
//        minFrustumDepth[currentSlice]=minFrustumRayLength;  // a reasonable approximation for a min bound on the frustum depth
//
//
//
//        points.InsertNextPoint(spacecraftPositionAdjusted[currentSlice]);
//        points.InsertNextPoint(UL);
//        points.InsertNextPoint(UR);
//        points.InsertNextPoint(LL);
//        points.InsertNextPoint(LR);
//
//        idList.SetId(0, 0);
//        idList.SetId(1, 1);
//        lines.InsertNextCell(idList);
//        idList.SetId(0, 0);
//        idList.SetId(1, 2);
//        lines.InsertNextCell(idList);
//        idList.SetId(0, 0);
//        idList.SetId(1, 3);
//        lines.InsertNextCell(idList);
//        idList.SetId(0, 0);
//        idList.SetId(1, 4);
//        lines.InsertNextCell(idList);
//
//        frustumPolyData.SetPoints(points);
//        frustumPolyData.SetLines(lines);
//
//
//        vtkPolyDataMapper frusMapper = new vtkPolyDataMapper();
//        frusMapper.SetInputData(frustumPolyData);
//
//        frustumActor.SetMapper(frusMapper);
//    }


    public void setPickedPosition(double[] position)
    {
        //System.out.println("PerspectiveImage.setPickedPosition(): " + position[0] + ", " + position[1] + ", " + position[2]);
        double[] pixelPosition = getPixelFromPoint(position);
        double[][] region = { { pixelPosition[0], pixelPosition[1] } };
        setSpectrumRegion(region);
    }

    public double[] getPixelFromPoint(double[] pt)
    {
        double[] uv = new double[2];
        Frustum frustum = vtkRenderer.getFrustum();
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
     * Return the default mask sizes as a 4 element integer array where the:
     * first  element is the top    mask size,
     * second element is the right  mask size,
     * third  element is the bottom mask size,
     * fourth element is the left   mask size.
     * @return
     */
    abstract protected int[] getMaskSizes();

    protected String initializeFitFileFullPath() throws IOException { return initLocalFitFileFullPath(); }
    protected String initializeEnviFileFullPath() {return initLocalEnviFileFullPath(); }
    protected String initializePngFileFullPath() { return initializeLabelFileFullPath(); }

    protected String initLocalPngFileFullPath()
    {
        return getKey().name.endsWith(".png") ? getKey().name : null;
    }

    protected String initLocalFitFileFullPath()
    {
        System.out.println("PerspectiveImage: initLocalFitFileFullPath: ");
        String keyName = getKey().name;
        if(imageFileType == PerspectiveImageIOSupportedFiletypes.FITS || keyName.endsWith(".fit") || keyName.endsWith(".fits") ||
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
        return VtkENVIReader.isENVIFilename(getKey().name) ? getKey().name : null;
    }

    //TODO: FIX THIS
    protected String initializeLabelFileFullPath() { return labelFileIO.initLocalLabelFileFullPath(); } //return initLocalLabelFileFullPath(); }
    protected String initializeInfoFileFullPath() { return infoFileIO.initLocalInfoFileFullPath(); } //return initLocalInfoFileFullPath(); }
    protected String initializeSumfileFullPath() throws IOException { return sumFileIO.initLocalSumfileFullPath(); } //initLocalSumfileFullPath(); }

//    public String initLocalSumfileFullPath()
//    {
//        // TODO this is bad in that we read from the config file 3 times in this class
//
//        // Look in the config file and figure out which index this image
//        // corresponds to. The config file is located in the same folder
//        // as the image file
//        String configFilename = new File(getKey().name).getParent() + File.separator + "config.txt";
//        MapUtil configMap = new MapUtil(configFilename);
//        String[] imageFilenames = configMap.getAsArray(Image.IMAGE_FILENAMES);
//        for (int i=0; i<imageFilenames.length; ++i)
//        {
//            String filename = new File(getKey().name).getName();
//            if (filename.equals(imageFilenames[i]))
//            {
//                return new File(getKey().name).getParent() + File.separator + configMap.getAsArray(PerspectiveImage.SUMFILENAMES)[i];
//            }
//        }
//
//        return null;
//    }

    /**
     *  Give oppurtunity to subclass to do some processing on the raw
     *  image such as resizing, flipping, masking, etc.
     *
     * @param rawImage
     */
    protected void processRawImage(vtkImageData rawImage)
    {
        // By default do nothing
    }

    protected vtkImageData createRawImage(int height, int width, int depth, float[][] array2D, float[][][] array3D)
    {
        return createRawImage(height, width, depth, true, array2D, array3D);
    }

    public vtkImageData createRawImage(int height, int width, int depth, boolean transpose, float[][] array2D, float[][][] array3D)
    {
        // Allocate enough room to store min/max value at each layer
        maxValue = new float[depth];
        minValue = new float[depth];

        // Call
        return ImageDataUtil.createRawImage(height, width, depth, transpose, array2D, array3D, minValue, maxValue);
    }

    protected vtkImageData loadRawImage(String filename) throws FitsException, IOException
    {
        fileIO.loadFromFile(filename);
        return vtkRenderer.getRawImage();
    }

    protected vtkImageData loadRawImage(String[] filenames) throws FitsException, IOException
    {
        fileIO.loadFromFiles(filenames, false);
        return vtkRenderer.getRawImage();
    }

    protected vtkImageData loadRawImage(String[] filenames, boolean transpose) throws FitsException, IOException
    {
        fileIO.loadFromFiles(filenames, transpose);
        return vtkRenderer.getRawImage();
    }

    protected void loadImage() throws FitsException, IOException
    {
        System.out.println("PerspectiveImage: loadImage: ");
//        rawImage = loadRawImage(filename);
//        System.out.println("PerspectiveImage: loadImage: raw image is " + vtkRenderer.getRawImage());
        if (vtkRenderer.getRawImage() == null)
            return;

        processRawImage(vtkRenderer.getRawImage());

        int[] dims = vtkRenderer.getRawImage().GetDimensions();
        imageWidth = dims[0];
        imageHeight = dims[1];
        imageDepth = dims[2];

        int[] masking = getMaskSizes();
        int topMask =    masking[0];
        int rightMask =  masking[1];
        int bottomMask = masking[2];
        int leftMask =   masking[3];
        for (int i=0; i<masking.length; ++i)
            currentMask[i] = masking[i];

        vtkImageCanvasSource2D maskSource = vtkRenderer.getMaskSource();
        maskSource = new vtkImageCanvasSource2D();
        maskSource.SetScalarTypeToUnsignedChar();
        maskSource.SetNumberOfScalarComponents(1);
        //        maskSource.SetExtent(0, imageWidth-1, 0, imageHeight-1, 0, imageDepth-1);
        maskSource.SetExtent(0, imageWidth-1, 0, imageHeight-1, 0, 0);
        // Initialize the mask to black which masks out the image
        maskSource.SetDrawColor(0.0, 0.0, 0.0, 0.0);
        maskSource.FillBox(0, imageWidth-1, 0, imageHeight-1);
        // Create a square inside mask which passes through the image.
        maskSource.SetDrawColor(255.0, 255.0, 255.0, 255.0);
        maskSource.FillBox(leftMask, imageWidth-1-rightMask, bottomMask, imageHeight-1-topMask);
        maskSource.Update();

        vtkRenderer.setMaskSource(maskSource);

        for (int k=0; k<imageDepth; k++)
        {
            vtkRenderer.getFootprint()[k] = new vtkPolyData();
            //            displayedRange[k] = new IntensityRange(1,0);
            displayedRange[k] = new IntensityRange(0,255);
        }

        vtkRenderer.reset();

//        shiftedFootprint[0] = new vtkPolyData();
//        textureCoords = new vtkFloatArray();
//        normalsFilter = new vtkPolyDataNormals();

        if (imageFileType == PerspectiveImageIOSupportedFiletypes.PNG)
        {
            System.out.println("PerspectiveImage: loadImage: png");
            double[] scalarRange = vtkRenderer.getRawImage().GetScalarRange();
            minValue[0] = (float)scalarRange[0];
            maxValue[0] = (float)scalarRange[1];
            //            setDisplayedImageRange(new IntensityRange(0, 255));
            setDisplayedImageRange(null);
        }
        else
        {
            setDisplayedImageRange(null);
        }

//        if (getFitFileFullPath() != null)
//        {
//            System.out.println("PerspectiveImage: loadImage: fit file");
//            setDisplayedImageRange(null);
//        }
//        else if (getPngFileFullPath() != null)
//        {
//            System.out.println("PerspectiveImage: loadImage: png");
//            double[] scalarRange = vtkRenderer.getRawImage().GetScalarRange();
//            minValue[0] = (float)scalarRange[0];
//            maxValue[0] = (float)scalarRange[1];
//            //            setDisplayedImageRange(new IntensityRange(0, 255));
//            setDisplayedImageRange(null);
//        }
//        else if (getEnviFileFullPath() != null)
//        {
//            System.out.println("PerspectiveImage: loadImage: envi");
//            setDisplayedImageRange(null);
//        }
//        else
//        {
//            System.out.println("PerspectiveImage: loadImage: ????");
//            setDisplayedImageRange(null);
//        }

        //        setDisplayedImageRange(new IntensityRange(0, 255));
    }

//    protected int loadNumSlices()
//    {
//        fileIO.loadNumSlices(getFitFileFullPath());
//        if (getFitFileFullPath() != null)
//        {
//            try {
//                String filename = getFitFileFullPath();
//                Fits f = new Fits(filename);
//                BasicHDU<?> h = f.getHDU(fitFileImageExtension);
//
//                int[] fitsAxes = h.getAxes();
//                int fitsNAxes = fitsAxes.length;
//                int fitsDepth = fitsNAxes == 3 ? fitsAxes[1] : 1;
//
//                imageDepth = fitsDepth;
//            } catch (Exception e) { e.printStackTrace(); }
//        }
//        else if (getPngFileFullPath() != null)
//        {
//            // Do nothing for now
//        }
//        else if (getEnviFileFullPath() != null)
//        {
//            // Get the number of bands from the ENVI header
//            String name = getEnviFileFullPath();
//
//            String imageFile = null;
//            if (getKey().source == ImageSource.IMAGE_MAP)
//                imageFile = FileCache.getFileFromServer(name).getAbsolutePath();
//            else
//                imageFile = getKey().name;
//
//            VtkENVIReader reader = new VtkENVIReader();
//            reader.SetFileName(imageFile);
//            imageDepth = reader.getNumBands();
//            // for multislice images, set slice to middle slice
//            if (imageDepth > 1)
//                setCurrentSlice(imageDepth / 2);
//        }
//        return imageDepth;
//    }

    protected void loadPointing() throws FitsException, IOException
    {
        System.out.println("PerspectiveImage: loadPointing:");
        if (key.source.equals(ImageSource.SPICE) || key.source.equals(ImageSource.CORRECTED_SPICE))
        {
            try
            {
                infoFileIO.loadImageInfo();
            }
            catch(IOException ex)
            {
                System.out.println("INFO file not available");
                ex.printStackTrace();
            }
        }
        else if (key.source.equals(ImageSource.LABEL))
        {
            try
            {
                labelFileIO.loadLabelFile();
            }
            catch(IOException ex)
            {
                System.out.println("LABEL file not available");
            }
        }
        else if (key.source.equals(ImageSource.LOCAL_PERSPECTIVE))
        {
            boolean loaded = false;
            try {
                infoFileIO.loadAdjustedSumfile();
                loaded = true;
            } catch (FileNotFoundException e) {
                loaded = false;
            }
            if (!loaded)
            {
                try {
                    sumFileIO.loadSumfile();
                    loaded = true;
                }
                catch (FileNotFoundException e)
                {
                    loaded = false;
                }
            }
            if (!loaded)
                this.infoFileIO.loadImageInfo();
        }
        else
        {
            boolean loaded = false;
            try {
//                System.out.println("PerspectiveImage: loadPointing: loading adjusted sum file");
                infoFileIO.loadAdjustedSumfile();
                loaded = true;
            } catch (FileNotFoundException e) {
                loaded = false;
                e.printStackTrace();

            }
            if (!loaded)
            {
                try {
//                    System.out.println("PerspectiveImage: loadPointing: loading sum file");
                    sumFileIO.loadSumfile();
                    loaded = true;
                }
                catch (FileNotFoundException e)
                {
                    System.out.println("SUM file not available");
                    throw(e);
                }
            }
        }

        // copy loaded state values into the adjusted values
        copySpacecraftState();
    }

    public List<vtkProp> getProps()
    {
        System.out.println("PerspectiveImage: getProps: ");
        return vtkRenderer.getProps();
    }

//    public List<vtkProp> getProps()
//    {
//        //        System.out.println("getProps()");
//        if (footprintActor == null)
//        {
//            loadFootprint();
//
//            imageTexture = new vtkTexture();
//            imageTexture.InterpolateOn();
//            imageTexture.RepeatOff();
//            imageTexture.EdgeClampOn();
//            imageTexture.SetInputData(getDisplayedImage());
//
//            vtkPolyDataMapper footprintMapper = new vtkPolyDataMapper();
//            footprintMapper.SetInputData(shiftedFootprint[0]);
//            footprintMapper.Update();
//
//            footprintActor = new vtkActor();
//            footprintActor.SetMapper(footprintMapper);
//            footprintActor.SetTexture(imageTexture);
//            vtkProperty footprintProperty = footprintActor.GetProperty();
//            footprintProperty.LightingOff();
//
//            footprintActors.add(footprintActor);
//        }
//
//        if (frustumActor == null)
//        {
//            frustumActor = new vtkActor();
//
//            calculateFrustum();
//
//            vtkProperty frustumProperty = frustumActor.GetProperty();
//            frustumProperty.SetColor(0.0, 1.0, 0.0);
//            frustumProperty.SetLineWidth(2.0);
//            frustumActor.VisibilityOff();
//
//            footprintActors.add(frustumActor);
//        }
//
//        // for offlimb
//        vtkActor offLimbActor = offlimb.getOffLimbActor();
//        vtkActor offLimbBoundaryActor = offlimb.getOffLimbBoundaryActor();
//        if (offLimbActor == null) {
//            offlimb.loadOffLimbPlane();
//            if (footprintActors.contains(offLimbActor))
//                footprintActors.remove(offLimbActor);
//            footprintActors.add(offLimbActor);
//            if (footprintActors.contains(offLimbBoundaryActor))
//                footprintActors.remove(offLimbBoundaryActor);
//            footprintActors.add(offLimbBoundaryActor);
//        }
//
//
//        return footprintActors;
//    }

    public void setShowFrustum(boolean b)
    {
        showFrustum = b;

        if (showFrustum)
        {
            vtkRenderer.getFrustumActor().VisibilityOn();
        }
        else
        {
            vtkRenderer.getFrustumActor().VisibilityOff();
        }

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void setDisplayedImageRange(IntensityRange range)
    {
        System.out.println("PerspectiveImage: setDisplayedImageRange: ");
        vtkRenderer.setDisplayedImageRange(range);
    }

//    public void setDisplayedImageRange(IntensityRange range)
//    {
//        if (range == null || displayedRange[currentSlice].min != range.min || displayedRange[currentSlice].max != range.max)
//        {
//            //            displayedRange[currentSlice] = range != null ? range : new IntensityRange(0, 255);
//            if (range != null)
//                displayedRange[currentSlice] = range;
//
//            float minValue = getMinValue();
//            float maxValue = getMaxValue();
//            float dx = (maxValue-minValue)/255.0f;
//            float min = minValue + displayedRange[currentSlice].min*dx;
//            float max = minValue + displayedRange[currentSlice].max*dx;
//
//            // Update the displayed image
//            vtkLookupTable lut = new vtkLookupTable();
//            lut.SetTableRange(min, max);
//            lut.SetValueRange(0.0, 1.0);
//            lut.SetHueRange(0.0, 0.0);
//            lut.SetSaturationRange(0.0, 0.0);
//            //lut.SetNumberOfTableValues(402);
//            lut.SetRampToLinear();
//            lut.Build();
//
//            // for 3D images, take the current slice
//            vtkImageData image2D = rawImage;
//            if (imageDepth > 1)
//            {
//                vtkImageReslice slicer = new vtkImageReslice();
//                slicer.SetInputData(rawImage);
//                slicer.SetOutputDimensionality(2);
//                slicer.SetInterpolationModeToNearestNeighbor();
//                slicer.SetOutputSpacing(1.0, 1.0, 1.0);
//                slicer.SetResliceAxesDirectionCosines(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0);
//
//                slicer.SetOutputOrigin(0.0, 0.0, (double)currentSlice);
//                slicer. SetResliceAxesOrigin(0.0, 0.0, (double)currentSlice);
//
//                slicer.SetOutputExtent(0, imageWidth-1, 0, imageHeight-1, 0, 0);
//
//                slicer.Update();
//                image2D = slicer.GetOutput();
//            }
//
//            vtkImageMapToColors mapToColors = new vtkImageMapToColors();
//            mapToColors.SetInputData(image2D);
//            mapToColors.SetOutputFormatToRGBA();
//            mapToColors.SetLookupTable(lut);
//            mapToColors.Update();
//
//            vtkImageData mapToColorsOutput = mapToColors.GetOutput();
//            vtkImageData maskSourceOutput = maskSource.GetOutput();
//
//            vtkImageMask maskFilter = new vtkImageMask();
//            maskFilter.SetImageInputData(mapToColorsOutput);
//            maskFilter.SetMaskInputData(maskSourceOutput);
//            maskFilter.Update();
//
//            if (displayedImage == null)
//                displayedImage = new vtkImageData();
//            vtkImageData maskFilterOutput = maskFilter.GetOutput();
//            displayedImage.DeepCopy(maskFilterOutput);
//
//            maskFilter.Delete();
//            mapToColors.Delete();
//            lut.Delete();
//            mapToColorsOutput.Delete();
//            maskSourceOutput.Delete();
//            maskFilterOutput.Delete();
//
//            //vtkPNGWriter writer = new vtkPNGWriter();
//            //writer.SetFileName("fit.png");
//            //writer.SetInput(displayedImage);
//            //writer.Write();
//
//        }
//        // for offlimb
//        vtkTexture offLimbTexture = offlimb.getOffLimbTexture();
//        if (offLimbTexture==null)
//            offLimbTexture=new vtkTexture();
//        vtkImageData image=new vtkImageData();
//        image.DeepCopy(getDisplayedImage());
//        offLimbTexture.SetInputData(image);
//        offLimbTexture.Modified();
//
//        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
//
//    }

    //    private static void printpt(double[] p, String s)
    //    {
    //        System.out.println(s + " " + p[0] + " " + p[1] + " " + p[2]);
    //    }




    private void initSpacecraftStateVariables()
    {
        System.out.println(
                "PerspectiveImage: initSpacecraftStateVariables: ");
        int nslices = getNumberBands();
        spacecraftPositionOriginal = new double[nslices][3];
        frustum1Original = new double[nslices][3];
        frustum2Original = new double[nslices][3];
        frustum3Original = new double[nslices][3];
        frustum4Original = new double[nslices][3];
        sunPositionOriginal = new double[nslices][3];
        boresightDirectionOriginal = new double[nslices][3];
        upVectorOriginal = new double[nslices][3];
        vtkRenderer.resetFrustaAndFootprints(nslices);
//        frusta = new Frustum[nslices];
//        footprint = new vtkPolyData[nslices];
        footprintGenerated = new boolean[nslices];
        displayedRange = new IntensityRange[nslices];
    }



    public boolean containsLimb()
    {
        //TODO Speed this up: Determine if there is a limb without computing the entire backplane.

        float[] bp = generateBackplanes(true);
        if (bp == null)
            return true;
        else
            return false;
    }

    protected vtkPolyData getFootprint(int defaultSlice)
    {
        System.out.println("PerspectiveImage: getFootprint: ");
        return vtkRenderer.getFootprint(defaultSlice);
    }

    public void loadFootprint()
    {
        System.out.println("PerspectiveImage: loadFootprint: ");
        vtkRenderer.loadFootprint();
    }

//    protected vtkPolyData getFootprint(int defaultSlice)
//    {
//        return smallBodyModel.computeFrustumIntersection(spacecraftPositionAdjusted[defaultSlice],
//                frustum1Adjusted[defaultSlice], frustum3Adjusted[defaultSlice], frustum4Adjusted[defaultSlice], frustum2Adjusted[defaultSlice]);
//    }
//
//    public void loadFootprint()
//    {
//        if (generateFootprint)
//        {
//            vtkPolyData tmp = null;
//
//            if (!footprintGenerated[currentSlice])
//            {
//                if (useDefaultFootprint())
//                {
//                    int defaultSlice = getDefaultSlice();
//                    if (footprintGenerated[defaultSlice] == false)
//                    {
//                        footprint[defaultSlice] = getFootprint(defaultSlice);
//                        if (footprint[defaultSlice] == null)
//                            return;
//
//                        // Need to clear out scalar data since if coloring data is being shown,
//                        // then the color might mix-in with the image.
//                        footprint[defaultSlice].GetCellData().SetScalars(null);
//                        footprint[defaultSlice].GetPointData().SetScalars(null);
//
//                        footprintGenerated[defaultSlice] = true;
//                    }
//
//                    tmp = footprint[defaultSlice];
//
//                }
//                else
//                {
//                    tmp = smallBodyModel.computeFrustumIntersection(spacecraftPositionAdjusted[currentSlice],
//                            frustum1Adjusted[currentSlice], frustum3Adjusted[currentSlice], frustum4Adjusted[currentSlice], frustum2Adjusted[currentSlice]);
//                    if (tmp == null)
//                        return;
//
//                    // Need to clear out scalar data since if coloring data is being shown,
//                    // then the color might mix-in with the image.
//                    tmp.GetCellData().SetScalars(null);
//                    tmp.GetPointData().SetScalars(null);
//                }
//
//
//                //                vtkPolyDataWriter writer=new vtkPolyDataWriter();
//                //                writer.SetInputData(tmp);
//                //                writer.SetFileName("/Users/zimmemi1/Desktop/test.vtk");
//                //               writer.SetFileTypeToBinary();
//                //                writer.Write();
//
//                footprint[currentSlice].DeepCopy(tmp);
//
//                footprintGenerated[currentSlice] = true;
//            }
//
//            vtkPointData pointData = footprint[currentSlice].GetPointData();
//            pointData.SetTCoords(textureCoords);
//            PolyDataUtil.generateTextureCoordinates(getFrustum(), getImageWidth(), getImageHeight(), footprint[currentSlice]);
//            pointData.Delete();
//        }
//        else
//        {
//            int resolutionLevel = smallBodyModel.getModelResolution();
//
//            String footprintFilename = null;
//            File file = null;
//
//            if (key.source == ImageSource.SPICE || key.source == ImageSource.CORRECTED_SPICE)
//                footprintFilename = key.name + "_FOOTPRINT_RES" + resolutionLevel + "_PDS.VTP";
//            else
//                footprintFilename = key.name + "_FOOTPRINT_RES" + resolutionLevel + "_GASKELL.VTP";
//
//            file = FileCache.getFileFromServer(footprintFilename);
//
//            if (file == null || !file.exists())
//            {
//                System.out.println("Warning: " + footprintFilename + " not found");
//                return;
//            }
//
//            vtkXMLPolyDataReader footprintReader = new vtkXMLPolyDataReader();
//            footprintReader.SetFileName(file.getAbsolutePath());
//            footprintReader.Update();
//
//            vtkPolyData footprintReaderOutput = footprintReader.GetOutput();
//            footprint[currentSlice].DeepCopy(footprintReaderOutput);
//        }
//
//
//        shiftedFootprint[0].DeepCopy(footprint[currentSlice]);
//        PolyDataUtil.shiftPolyDataInNormalDirection(shiftedFootprint[0], getOffset());
//    }
//
//    public vtkPolyData generateBoundary()
//    {
//        loadFootprint();
//
//        if (footprint[currentSlice].GetNumberOfPoints() == 0)
//            return null;
//
//        vtkFeatureEdges edgeExtracter = new vtkFeatureEdges();
//        edgeExtracter.SetInputData(footprint[currentSlice]);
//        edgeExtracter.BoundaryEdgesOn();
//        edgeExtracter.FeatureEdgesOff();
//        edgeExtracter.NonManifoldEdgesOff();
//        edgeExtracter.ManifoldEdgesOff();
//        edgeExtracter.Update();
//
//        vtkPolyData boundary = new vtkPolyData();
//        vtkPolyData edgeExtracterOutput = edgeExtracter.GetOutput();
//        boundary.DeepCopy(edgeExtracterOutput);
//
//        return boundary;
//    }
//
//
//
//    private void computeCellNormals()
//    {
//        if (normalsGenerated == false)
//        {
//            normalsFilter.SetInputData(footprint[currentSlice]);
//            normalsFilter.SetComputeCellNormals(1);
//            normalsFilter.SetComputePointNormals(0);
//            //normalsFilter.AutoOrientNormalsOn();
//            //normalsFilter.ConsistencyOn();
//            normalsFilter.SplittingOff();
//            normalsFilter.Update();
//
//            if (footprint != null && footprint[currentSlice] != null)
//            {
//                vtkPolyData normalsFilterOutput = normalsFilter.GetOutput();
//                footprint[currentSlice].DeepCopy(normalsFilterOutput);
//                normalsGenerated = true;
//            }
//        }
//    }

    // Computes the incidence, emission, and phase at a point on the footprint with a given normal.
    // (I.e. the normal of the plate which the point is lying on).
    // The output is a 3-vector with the first component equal to the incidence,
    // the second component equal to the emission and the third component equal to
    // the phase.
    public double[] computeIlluminationAnglesAtPoint(
            double[] pt,
            double[] normal)
    {
        System.out.println(
                "PerspectiveImage: computeIlluminationAnglesAtPoint: ");
        double[] scvec = {
                spacecraftPositionAdjusted[currentSlice][0] - pt[0],
                spacecraftPositionAdjusted[currentSlice][1] - pt[1],
                spacecraftPositionAdjusted[currentSlice][2] - pt[2]};

        double[] sunVectorAdjusted = getSunVector();
        double incidence = MathUtil.vsep(normal, sunVectorAdjusted) * 180.0 / Math.PI;
        double emission = MathUtil.vsep(normal, scvec) * 180.0 / Math.PI;
        double phase = MathUtil.vsep(sunVectorAdjusted, scvec) * 180.0 / Math.PI;

        double[] angles = {incidence, emission, phase};

        return angles;
    }

    protected void computeIlluminationAngles()
    {
        System.out
                .println("PerspectiveImage: computeIlluminationAngles: ");
        if (footprintGenerated[currentSlice] == false)
            vtkRenderer.loadFootprint();

        vtkRenderer.computeCellNormals();

        int numberOfCells = vtkRenderer.getFootprint()[currentSlice].GetNumberOfCells();

        vtkPoints points = vtkRenderer.getFootprint()[currentSlice].GetPoints();
        vtkCellData footprintCellData = vtkRenderer.getFootprint()[currentSlice].GetCellData();
        vtkDataArray normals = footprintCellData.GetNormals();

        this.minEmission  =  Double.MAX_VALUE;
        this.maxEmission  = -Double.MAX_VALUE;
        this.minIncidence =  Double.MAX_VALUE;
        this.maxIncidence = -Double.MAX_VALUE;
        this.minPhase     =  Double.MAX_VALUE;
        this.maxPhase     = -Double.MAX_VALUE;

        for (int i=0; i<numberOfCells; ++i)
        {
            vtkCell cell = vtkRenderer.getFootprint()[currentSlice].GetCell(i);
            double[] pt0 = points.GetPoint( cell.GetPointId(0) );
            double[] pt1 = points.GetPoint( cell.GetPointId(1) );
            double[] pt2 = points.GetPoint( cell.GetPointId(2) );
            double[] centroid = {
                    (pt0[0] + pt1[0] + pt2[0]) / 3.0,
                    (pt0[1] + pt1[1] + pt2[1]) / 3.0,
                    (pt0[2] + pt1[2] + pt2[2]) / 3.0
            };
            double[] normal = normals.GetTuple3(i);

            double[] angles = computeIlluminationAnglesAtPoint(centroid, normal);
            double incidence = angles[0];
            double emission  = angles[1];
            double phase     = angles[2];

            if (incidence < minIncidence)
                minIncidence = incidence;
            if (incidence > maxIncidence)
                maxIncidence = incidence;
            if (emission < minEmission)
                minEmission = emission;
            if (emission > maxEmission)
                maxEmission = emission;
            if (phase < minPhase)
                minPhase = phase;
            if (phase > maxPhase)
                maxPhase = phase;
            cell.Delete();
        }

        points.Delete();
        footprintCellData.Delete();
        if (normals != null)
            normals.Delete();
    }

    protected void computePixelScale()
    {
        System.out.println("PerspectiveImage: computePixelScale: ");
        if (footprintGenerated[currentSlice] == false)
            vtkRenderer.loadFootprint();

        int numberOfPoints = vtkRenderer.getFootprint()[currentSlice].GetNumberOfPoints();

        vtkPoints points = vtkRenderer.getFootprint()[currentSlice].GetPoints();

        minHorizontalPixelScale = Double.MAX_VALUE;
        maxHorizontalPixelScale = -Double.MAX_VALUE;
        meanHorizontalPixelScale = 0.0;
        minVerticalPixelScale = Double.MAX_VALUE;
        maxVerticalPixelScale = -Double.MAX_VALUE;
        meanVerticalPixelScale = 0.0;

        double horizScaleFactor = 2.0 * Math.tan( MathUtil.vsep(frustum1Adjusted[currentSlice], frustum3Adjusted[currentSlice]) / 2.0 ) / imageHeight;
        double vertScaleFactor = 2.0 * Math.tan( MathUtil.vsep(frustum1Adjusted[currentSlice], frustum2Adjusted[currentSlice]) / 2.0 ) / imageWidth;

        double[] vec = new double[3];

        for (int i=0; i<numberOfPoints; ++i)
        {
            double[] pt = points.GetPoint(i);

            vec[0] = pt[0] - spacecraftPositionAdjusted[currentSlice][0];
            vec[1] = pt[1] - spacecraftPositionAdjusted[currentSlice][1];
            vec[2] = pt[2] - spacecraftPositionAdjusted[currentSlice][2];
            double dist = MathUtil.vnorm(vec);

            double horizPixelScale = dist * horizScaleFactor;
            double vertPixelScale = dist * vertScaleFactor;

            if (horizPixelScale < minHorizontalPixelScale)
                minHorizontalPixelScale = horizPixelScale;
            if (horizPixelScale > maxHorizontalPixelScale)
                maxHorizontalPixelScale = horizPixelScale;
            if (vertPixelScale < minVerticalPixelScale)
                minVerticalPixelScale = vertPixelScale;
            if (vertPixelScale > maxVerticalPixelScale)
                maxVerticalPixelScale = vertPixelScale;

            meanHorizontalPixelScale += horizPixelScale;
            meanVerticalPixelScale += vertPixelScale;
        }

        meanHorizontalPixelScale /= (double)numberOfPoints;
        meanVerticalPixelScale /= (double)numberOfPoints;

        points.Delete();
    }



    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_RESOLUTION_CHANGED.equals(evt.getPropertyName()))
        {
            vtkRenderer.loadFootprint();
            normalsGenerated = false;
            this.minEmission  =  Double.MAX_VALUE;
            this.maxEmission  = -Double.MAX_VALUE;
            this.minIncidence =  Double.MAX_VALUE;
            this.maxIncidence = -Double.MAX_VALUE;
            this.minPhase     =  Double.MAX_VALUE;
            this.maxPhase     = -Double.MAX_VALUE;
            this.minHorizontalPixelScale = Double.MAX_VALUE;
            this.maxHorizontalPixelScale = -Double.MAX_VALUE;
            this.minVerticalPixelScale = Double.MAX_VALUE;
            this.maxVerticalPixelScale = -Double.MAX_VALUE;
            this.meanHorizontalPixelScale = 0.0;
            this.meanVerticalPixelScale = 0.0;

            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }



    public void Delete()
    {
        vtkRenderer.getDisplayedImage().Delete();
        vtkRenderer.getRawImage().Delete();
        for (int i=0; i<imageDepth; i++)
        {
            // Footprints can be null if no frustum intersection is found
            if(vtkRenderer.getFootprint()[i] != null)
            {
                vtkRenderer.getFootprint()[i].Delete();
            }
            if(vtkRenderer.getShiftedFootprint(i) != null)
            {
                vtkRenderer.getShiftedFootprint(i).Delete();
            }
        }

        vtkRenderer.getTextureCoords().Delete();
        vtkRenderer.getNormalsFilter().Delete();
        vtkRenderer.getMaskSource().Delete();
    }

    public void getCameraOrientation(double[] spacecraftPosition,
            double[] focalPoint, double[] upVector)
    {
        System.out.println("PerspectiveImage: getCameraOrientation: ");
        for (int i=0; i<3; ++i)
        {
            spacecraftPosition[i] = this.spacecraftPositionAdjusted[currentSlice][i];
            upVector[i] = this.upVectorAdjusted[currentSlice][i];
        }

        // Normalize the direction vector
        double[] direction = new double[3];
        MathUtil.unorm(boresightDirectionAdjusted[currentSlice], direction);

        int cellId = smallBodyModel.computeRayIntersection(spacecraftPosition, direction, focalPoint);

        if (cellId < 0)
        {
            BoundingBox bb = new BoundingBox(vtkRenderer.getFootprint()[currentSlice].GetBounds());
            double[] centerPoint = bb.getCenterPoint();
            //double[] centerPoint = footprint[currentSlice].GetPoint(0);
            double distanceToCenter = MathUtil.distanceBetween(spacecraftPosition, centerPoint);

            focalPoint[0] = spacecraftPosition[0] + distanceToCenter*direction[0];
            focalPoint[1] = spacecraftPosition[1] + distanceToCenter*direction[1];
            focalPoint[2] = spacecraftPosition[2] + distanceToCenter*direction[2];
        }
//        System.out.println("PerspectiveImage: getCameraOrientation: focal point " + focalPoint[0] + " " + focalPoint[1] + " "  + focalPoint[2]);
//        System.out.println("PerspectiveImage: getCameraOrientation: sc pos " + spacecraftPosition[0] + " " + spacecraftPosition[1] + " "  + spacecraftPosition[2]);
    }

    /**
     * Same as previous but return a (4 element) quaternion instead.
     * First element is the scalar followed by the 3 element vector.
     * Also returns a rotation matrix.
     * @param spacecraftPosition
     * @param quaternion
     * @return Rotation matrix
     */
    public Rotation getCameraOrientation(double[] spacecraftPosition,
            double[] quaternion)
    {
        System.out.println("PerspectiveImage: getCameraOrientation: ");
        double[] cx = upVectorAdjusted[currentSlice];
        double[] cz = new double[3];
        MathUtil.unorm(boresightDirectionAdjusted[currentSlice], cz);

        double[] cy = new double[3];
        MathUtil.vcrss(cz, cx, cy);

        double[][] m = {
                {cx[0], cx[1], cx[2]},
                {cy[0], cy[1], cy[2]},
                {cz[0], cz[1], cz[2]}
        };

        Rotation rotation = new Rotation(m, 1.0e-6);

        for (int i=0; i<3; ++i)
            spacecraftPosition[i] = this.spacecraftPositionAdjusted[currentSlice][i];

        quaternion[0] = rotation.getQ0();
        quaternion[1] = rotation.getQ1();
        quaternion[2] = rotation.getQ2();
        quaternion[3] = rotation.getQ3();

        return rotation;
    }

    public Frustum getFrustum(int slice)
    {
        System.out.println("PerspectiveImage: getFrustum: ");
        return vtkRenderer.getFrustum(slice);
    }

//    public Frustum getFrustum(int slice)
//    {
//        if (useDefaultFootprint())
//        {
//            int defaultSlice = getDefaultSlice();
//            if (frusta[defaultSlice] == null)
//                frusta[defaultSlice] = new Frustum(spacecraftPositionAdjusted[defaultSlice], frustum1Adjusted[defaultSlice], frustum3Adjusted[defaultSlice], frustum4Adjusted[defaultSlice], frustum2Adjusted[defaultSlice]);
//            return frusta[defaultSlice];
//        }
//
//        if (frusta[slice] == null)
//            frusta[slice] = new Frustum(spacecraftPositionAdjusted[slice], frustum1Adjusted[slice], frustum3Adjusted[slice], frustum4Adjusted[slice], frustum2Adjusted[slice]);
//        return frusta[slice];
//    }




    @Override
    public LinkedHashMap<String, String> getProperties() throws IOException
    {
        LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
        if (vtkRenderer.getFootprint() == null || vtkRenderer.getFootprint()[currentSlice] == null)
            return properties;

        if (getMaxPhase() < getMinPhase())
        {
            this.computeIlluminationAngles();
            this.computePixelScale();
        }

        DecimalFormat df = new DecimalFormat("#.######");

        properties.put("Name", new File(getImageFileFullPath()).getName()); //TODO remove extension and possibly prefix
        properties.put("Start Time", getStartTime());
        properties.put("Stop Time", getStopTime());
        properties.put("Spacecraft Distance", df.format(getSpacecraftDistance()) + " km");
        properties.put("Spacecraft Position",
                df.format(spacecraftPositionAdjusted[currentSlice][0]) + ", " + df.format(spacecraftPositionAdjusted[currentSlice][1]) + ", " + df.format(spacecraftPositionAdjusted[currentSlice][2]) + " km");
        double[] quaternion = new double[4];
        double[] notused = new double[4];
        getCameraOrientation(notused, quaternion);
        properties.put("Spacecraft Orientation (quaternion)",
                "(" + df.format(quaternion[0]) + ", [" + df.format(quaternion[1]) + ", " + df.format(quaternion[2]) + ", " + df.format(quaternion[3]) + "])");
        double[] sunVectorAdjusted = getSunVector();
        properties.put("Sun Vector",
                df.format(sunVectorAdjusted[0]) + ", " + df.format(sunVectorAdjusted[1]) + ", " + df.format(sunVectorAdjusted[2]));
        if (getCameraName() != null)
            properties.put("Camera", getCameraName());
        if (getFilterName() != null)
            properties.put("Filter", getFilterName());

        // Note \u00B2 is the unicode superscript 2 symbol
        String ss2 = "\u00B2";
        properties.put("Footprint Surface Area", df.format(getSurfaceArea()) + " km" + ss2);

        // Note \u00B0 is the unicode degree symbol
        String deg = "\u00B0";
        properties.put("FOV", df.format(getHorizontalFovAngle())+deg + " x " + df.format(getVerticalFovAngle())+deg);

        properties.put("Minimum Incidence", df.format(getMinIncidence())+deg);
        properties.put("Maximum Incidence", df.format(getMaxIncidence())+deg);
        properties.put("Minimum Emission", df.format(getMinEmission())+deg);
        properties.put("Maximum Emission", df.format(getMaxIncidence())+deg);
        properties.put("Minimum Phase", df.format(getMinPhase())+deg);
        properties.put("Maximum Phase", df.format(getMaxPhase())+deg);
        properties.put("Minimum Horizontal Pixel Scale", df.format(1000.0*getMinimumHorizontalPixelScale()) + " meters/pixel");
        properties.put("Maximum Horizontal Pixel Scale", df.format(1000.0*getMaximumHorizontalPixelScale()) + " meters/pixel");
        properties.put("Minimum Vertical Pixel Scale", df.format(1000.0*getMinimumVerticalPixelScale()) + " meters/pixel");
        properties.put("Maximum Vertical Pixel Scale", df.format(1000.0*getMaximumVerticalPixelScale()) + " meters/pixel");

        return properties;
    }

    public void firePropertyChange()
    {
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void setCurrentMask(int[] masking)
    {
        System.out.println("PerspectiveImage: setCurrentMask: ");
        int topMask =    masking[0];
        int rightMask =  masking[1];
        int bottomMask = masking[2];
        int leftMask =   masking[3];
        // Initialize the mask to black which masks out the image
        vtkImageCanvasSource2D maskSource = vtkRenderer.getMaskSource();
        maskSource.SetDrawColor(0.0, 0.0, 0.0, 0.0);
        maskSource.FillBox(0, imageWidth-1, 0, imageHeight-1);
        // Create a square inside mask which passes through the image.
        maskSource.SetDrawColor(255.0, 255.0, 255.0, 255.0);
        maskSource.FillBox(leftMask, imageWidth-1-rightMask, bottomMask, imageHeight-1-topMask);
        maskSource.Update();

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        setDisplayedImageRange(null);

        for (int i=0; i<masking.length; ++i)
            currentMask[i] = masking[i];
    }




    @Override
    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        // Get default status message
        String status = super.getClickStatusBarText(prop, cellId, pickPosition);

        // Append raw pixel value information
        status += ", Raw Value = ";
        if(vtkRenderer.getRawImage() == null)
        {
            status += "Unavailable";
        }
        else
        {
            int ip0 = (int)Math.round(pickPosition[0]);
            int ip1 = (int)Math.round(pickPosition[1]);
            if (!vtkRenderer.getRawImage().GetScalarTypeAsString().contains("char"))
            {
                float[] pixelColumn = ImageDataUtil.vtkImageDataToArray1D(vtkRenderer.getRawImage(), imageHeight-1-ip0, ip1);
                status += pixelColumn[currentSlice];
            }
            else
            {
                status += "N/A";
            }
        }

        return status;
    }



//    /*
//     * FOR OFF-LIMB IMAGES
//     */
//
//    /**
//     * No-argument entry point into the off-limb geometry-creation implementation.
//     * This will create an offlimbPlaneCalculator and create the actors for the
//     * plane and the boundaries.
//     */
//    protected void loadOffLimbPlane()
//    {
//        double[] spacecraftPosition=new double[3];
//        double[] focalPoint=new double[3];
//        double[] upVector=new double[3];
//        this.getCameraOrientation(spacecraftPosition, focalPoint, upVector);
//        this.offLimbFootprintDepth=new Vector3D(spacecraftPosition).getNorm();
//        calculator.loadOffLimbPlane(this, offLimbFootprintDepth);
//        offLimbActor=calculator.getOffLimbActor();
//        offLimbBoundaryActor=calculator.getOffLimbBoundaryActor();
//
//        // set initial visibilities
//        if (offLimbActor != null)
//        {
//            offLimbActor.SetVisibility(offLimbVisibility?1:0);
//            offLimbBoundaryActor.SetVisibility(offLimbBoundaryVisibility?1:0);
//        }
//    }
//
//
//    /**
//     * Set the distance of the off-limb plane from the camera position, along its look vector.
//     * The associated polydata doesn't need to be regenerated every time this method is called since the body's shadow in frustum coordinates does not change with depth along the look axis.
//     * The call to loadOffLimbPlane here does actually re-create the polydata, which should be unnecessary, and needs to be fixed in a future release.
//     * @param footprintDepth
//     */
//    public void setOffLimbPlaneDepth(double footprintDepth)
//    {
//        this.offLimbFootprintDepth=footprintDepth;
//        calculator.loadOffLimbPlane(this, offLimbFootprintDepth);
//    }
//
//    public void setOffLimbFootprintAlpha(double alpha)  // between 0-1
//    {
//        if (offLimbActor==null)
//            loadOffLimbPlane();
//        offLimbActor.GetProperty().SetOpacity(alpha);
//    }
//
//
//    public boolean offLimbFootprintIsVisible()
//    {
//        return offLimbVisibility;
//    }
//
//    /**
//     * Set visibility of the off-limb footprint
//     *
//     * Checks if offLimbActor has been instantiated; if not then call loadOffLimbPlane() before showing/hiding actors.
//     *
//     * @param visible
//     */
//    public void setOffLimbFootprintVisibility(boolean visible)
//    {
//
//        offLimbVisibility=visible;
//        offLimbBoundaryVisibility = visible;
//        if (offLimbActor==null)
//            loadOffLimbPlane();
//
//        if (offLimbActor != null)
//        {
//            offLimbActor.SetVisibility(visible?1:0);
//            offLimbBoundaryActor.SetVisibility(visible?1:0);
//        }
//
//        pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
//    }
//
//    /**
//     * Set visibility of the off-limb footprint boundary
//     *
//     * Checks if offLimbActor has been instantiated; if not then call loadOffLimbPlane() before showing/hiding actors.
//     *
//     * @param visible
//     */
//    public void setOffLimbBoundaryVisibility(boolean visible)
//    {
//
//        offLimbBoundaryVisibility=visible;
//        if (offLimbActor==null)
//            loadOffLimbPlane();
//        offLimbBoundaryActor.SetVisibility(visible?1:0);
//
//        pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
//    }
//
//    public vtkTexture getOffLimbTexture()
//    {
//        return offLimbTexture;
//    }
//
//    public void setOffLimbTexture(vtkTexture offLimbTexture)
//    {
//        this.offLimbTexture = offLimbTexture;
//    }
//    public double getOffLimbPlaneDepth()
//    {
//        return offLimbFootprintDepth;
//    }



    //*********************
    //Getters and Setters
    //*********************

    public String getStartTime()
    {
        return startTime;
    }

    public String getStopTime()
    {
        return stopTime;
    }

    public double getMinimumHorizontalPixelScale()
    {
        return minHorizontalPixelScale;
    }

    public double getMaximumHorizontalPixelScale()
    {
        return maxHorizontalPixelScale;
    }

    public double getMeanHorizontalPixelScale()
    {
        return meanHorizontalPixelScale;
    }

    public double getMinimumVerticalPixelScale()
    {
        return minVerticalPixelScale;
    }

    public double getMaximumVerticalPixelScale()
    {
        return maxVerticalPixelScale;
    }

    public double getMeanVerticalPixelScale()
    {
        return meanVerticalPixelScale;
    }

    public double getSpacecraftDistance()
    {
        return MathUtil.vnorm(spacecraftPositionAdjusted[currentSlice]);
    }

    public double[] getZoomFactor()
    {
        return zoomFactor;
    }


    public double[] getRotationOffset()
    {
        return rotationOffset;
    }

    public vtkPolyData getFrustumPolyData()
    {
        return vtkRenderer.getFrustumPolyData();
    }

//    public vtkPolyData getFrustumPolyData()
//    {
//        return frustumPolyData;
//    }
//
//    /**
//     * The shifted footprint is the original footprint shifted slightly in the
//     * normal direction so that it will be rendered correctly and not obscured
//     * by the asteroid.
//     * @return
//     */
//    @Override
    public vtkPolyData getShiftedFootprint()
    {
        return vtkRenderer.getShiftedFootprint();
//        return shiftedFootprint[0];
    }

//    /**
//     * The original footprint whose cells exactly overlap the original asteroid.
//     * If rendered as is, it would interfere with the asteroid.
//     * Note: this is made public in this class for the benefit of backplane
//     * generators, which use it.
//     * @return
//     */
    @Override
    public vtkPolyData getUnshiftedFootprint()
    {
        return vtkRenderer.getUnshiftedFootprint();
//        return footprint[currentSlice];
    }

    public int getNumBackplanes()
    {
        return numBands;
    }

    public double getFocalLength() { return 0.0; }

    public double getNumberOfPixels() { return numberOfPixels; }

    public void setNumberOfPixels(double numberOfPixels)
    {
        this.numberOfPixels = numberOfPixels;
    }

    public double getNumberOfLines() { return numberOfLines; }

    public void setNumberOfLines(double numberOfLines)
    {
        this.numberOfLines = numberOfLines;
    }

    public double getPixelWidth() { return 0.0; }

    public double getPixelHeight() { return 0.0; }

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

    public boolean shiftBands() { return false; }

    public int getNumberBands()
    {
        //        return 1;
        return imageDepth;
    }

    /**
     * Returns the number of spectra the image contains
     *
     * @return number of spectra
     */
    public int getNumberOfSpectralSegments() { return 0; }

    /**
     * For a multispectral image, returns an array of doubles containing the wavelengths for each point
     * on the image's spectrum.
     *
     * @return array of spectrum wavelengths
     */
    public double[] getSpectrumWavelengths(int segment) { return null; }

    /**
     * For a multispectral image, returns an array of doubles containing the bandwidths for each point
     * on the image's spectrum.
     *
     * @return array of spectrum wavelengths
     */
    public double[] getSpectrumBandwidths(int segment) { return null; }

    /**
     * For a multispectral image, returns an array of doubles containing the values for each point
     * on the image's spectrum.
     *
     * @return array of spectrum values
     */
    public double[] getSpectrumValues(int segment) { return null; }

    public String getSpectrumWavelengthUnits() { return null; }

    public String getSpectrumValueUnits() { return null; }

    /**
     * For a multispectral image, specify a region in pixel space over which to calculate the spectrum values.
     * The array is an Nx2 array of 2-dimensional vertices in pixel coordinates.
     * First index indicates the vertex, the second index indicates which of the two pixel coordinates.
     * A vertices array of height 1 will specify a single pixel region. An array of h 2 will specify a circular
     * region where the first value is the center and the second value is a point the circle. An array of size
     * 3 or more will specify a polygonal region.
     *
     * @param vertices of region
     */
    public void setSpectrumRegion(double[][] vertices) { }

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

    public String getImageFileFullPath()
    {
        return imageFileFullPath;
    }

//    public String getPngFileFullPath()
//    {
//        return pngFileFullPath;
//    }
//
//    public String getFitFileFullPath()
//    {
//        return fitFileFullPath;
//    }
//
//    public String getEnviFileFullPath()
//    {
//        return enviFileFullPath;
//    }
//
//    public String getImageFileFullPath()
//    {
//        if(fitFileFullPath != null)
//        {
//            return fitFileFullPath;
//        }
//        else if(pngFileFullPath != null)
//        {
//            return pngFileFullPath;
//        }
//        else if(enviFileFullPath != null)
//        {
//            return enviFileFullPath;
//        }
//        else
//        {
//            return null;
//        }
//    }

    public String[] getFitFilesFullPath()
    {
//        String[] result = { fitFileFullPath };
        String[] result = { imageFileFullPath };
        return result;
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

    /**
     * Return the multispectral image's spectrum region in pixel space.
     *
     * @return array describing region over which the spectrum is calculated.
     */
    public double[][] getSpectrumRegion() { return null; }


    @Override
    public String getImageName()
    {
        if (imageName != null)
            return imageName;
        else
            return super.getImageName();
    }

    public vtkImageData getRawImage()
    {
        return vtkRenderer.getRawImage();
    }

    public void setRawImage(vtkImageData imageData)
    {
        vtkRenderer.setRawImage(imageData);
    }

    protected vtkImageData getDisplayedImage()
    {
        return vtkRenderer.getDisplayedImage();
    }

    public void setCurrentSlice(int slice)
    {
        this.currentSlice = slice;
    }

    public int getCurrentSlice()
    {
        return currentSlice;
    }

    public int getDefaultSlice() { return 0; }

    public void setUseDefaultFootprint(boolean useDefaultFootprint)
    {
        this.useDefaultFootprint = useDefaultFootprint;
        for (int i=0; i<getImageDepth(); i++)
        {
            footprintGenerated[i] = false;
        }
    }

    public boolean useDefaultFootprint() { return useDefaultFootprint; }

    public String getCurrentBand()
    {
        return Integer.toString(currentSlice);
    }

    public vtkTexture getTexture()
    {
        return vtkRenderer.getTexture();
//        return imageTexture;
    }

    public static void setGenerateFootprint(boolean b)
    {
        generateFootprint = b;
    }

    /**
     * Get filter as an integer id. Return -1 if no filter is available.
     * @return
     */
    public int getFilter()
    {
        return -1;
    }

    /**
     * Get filter name as string. By default cast filter id to string.
     * Return null if filter id is negative.
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
     * Return the camera id. We assign an integer id to each camera.
     * For example, if there are 2 cameras on the spacecraft, return
     * either 1 or 2. If there are 2 spacecrafts each with a single
     * camera, then also return either 1 or 2. Return -1 if camera is
     * not available.
     *
     * @return
     */
    public int getCamera()
    {
        return -1;
    }

    /**
     * Get camera name as string. By default cast camera id to string.
     * Return null if camera id is negative.
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

    public boolean isFrustumShowing()
    {
        return showFrustum;
    }

    public void setSimulateLighting(boolean b)
    {
        simulateLighting = b;
    }

    public boolean isSimulatingLighingOn()
    {
        return simulateLighting;
    }

    public double getMinIncidence()
    {
        return minIncidence;
    }

    public double getMaxIncidence()
    {
        return maxIncidence;
    }

    public double getMinEmission()
    {
        return minEmission;
    }

    public double getMaxEmission()
    {
        return maxEmission;
    }

    public double getMinPhase()
    {
        return minPhase;
    }

    public double getMaxPhase()
    {
        return maxPhase;
    }

    public IntensityRange getDisplayedRange()
    {
        return displayedRange[currentSlice];
    }

    public IntensityRange getDisplayedRange(int slice)
    {
        return displayedRange[slice];
    }

    public void setDisplayedImageRange()
    {
        setDisplayedImageRange(displayedRange[currentSlice]);
    }


    public String getTargetName()
    {
        return targetName;
    }


    public void setTargetName(String targetName)
    {
        this.targetName = targetName;
    }


    public String getObjectName()
    {
        return objectName;
    }


    public void setObjectName(String objectName)
    {
        this.objectName = objectName;
    }


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


    public double getExposureDuration()
    {
        return exposureDuration;
    }


    public void setExposureDuration(double exposureDuration)
    {
        this.exposureDuration = exposureDuration;
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


    public void setFilterName(String filterName)
    {
        this.filterName = filterName;
    }


    public String getInstrumentId()
    {
        return instrumentId;
    }


    public void setInstrumentId(String instrumentId)
    {
        this.instrumentId = instrumentId;
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


    public void setImageWidth(int imageWidth)
    {
        this.imageWidth = imageWidth;
    }


    public void setImageHeight(int imageHeight)
    {
        this.imageHeight = imageHeight;
    }


    public void setImageDepth(int imageDepth)
    {
        this.imageDepth = imageDepth;
    }

    public int[] getCurrentMask()
    {
        return currentMask.clone();
    }

    public void setLabelFileFullPath(String labelFileFullPath)
    {
        this.labelFileFullPath = labelFileFullPath;
    }

    public Frustum getFrustum()
    {
        return vtkRenderer.getFrustum();
//        return getFrustum(currentSlice);
    }

    /**
     *  Get the maximum FOV angle in degrees of the image (the max of either
     *  the horizontal or vetical FOV). I.e., return the
     *  angular separation in degrees between two corners of the frustum where the
     *  two corners are both on the longer side.
     *
     * @return
     */
    public double getMaxFovAngle()
    {
        return Math.max(getHorizontalFovAngle(), getVerticalFovAngle());
    }

    public double getHorizontalFovAngle()
    {
        double fovHoriz = MathUtil.vsep(frustum1Adjusted[currentSlice], frustum3Adjusted[currentSlice]) * 180.0 / Math.PI;
        return fovHoriz;
    }

    public double getVerticalFovAngle()
    {
        double fovVert = MathUtil.vsep(frustum1Adjusted[currentSlice], frustum2Adjusted[currentSlice]) * 180.0 / Math.PI;
        return fovVert;
    }

    public double[] getSpacecraftPosition()
    {
        return spacecraftPositionAdjusted[currentSlice];
    }

    public double[] getSunPosition()
    {
        return sunPositionAdjusted[currentSlice];
    }

    public double[] getSunVector()
    {
        double[] result = new double[3];
        MathUtil.vhat(sunPositionAdjusted[currentSlice], result);
        return result;
    }

    public double[] getBoresightDirection()
    {
        return boresightDirectionAdjusted[currentSlice];
    }

    public double[] getUpVector()
    {
        return upVectorAdjusted[currentSlice];
    }

    public double[] getPixelDirection(int sample, int line)
    {
        return getPixelDirection((double)sample, (double)line, currentSlice);
    }

    public double[] getPixelDirection(double sample, double line)
    {
        return getPixelDirection((double)sample, (double)line, currentSlice);
    }


    /**
     * Get the direction from the spacecraft of pixel with specified sample and line.
     * Note that sample is along image width and line is along image height.
     */
    public double[] getPixelDirection(double sample, double line, int slice)
    {
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
        double fracHeight = ((double)line / (double)(imageHeight-1));
        double[] left = {
                corner1[0] + fracHeight*vec13[0],
                corner1[1] + fracHeight*vec13[1],
                corner1[2] + fracHeight*vec13[2]
        };

        double fracWidth = ((double)sample / (double)(imageWidth-1));
        double[] dir = {
                left[0] + fracWidth*vec12[0],
                left[1] + fracWidth*vec12[1],
                left[2] + fracWidth*vec12[2]
        };
        dir[0] -= spacecraftPositionAdjusted[slice][0];
        dir[1] -= spacecraftPositionAdjusted[slice][1];
        dir[2] -= spacecraftPositionAdjusted[slice][2];
        MathUtil.unorm(dir, dir);

        return dir;
    }

    /**
     * Get point on surface that intersects a ray originating from spacecraft
     * in direction of pixel with specified sample and line.
     * Note that sample is along image width and line is along image height.
     * If there is no intersect point, null is returned.
     */
    public double[] getPixelSurfaceIntercept(int sample, int line)
    {
        double[] dir = getPixelDirection(sample, line);

        double[] intersectPoint = new double[3];

        int result = smallBodyModel.computeRayIntersection(spacecraftPositionAdjusted[currentSlice], dir, intersectPoint);

        if (result >= 0)
            return intersectPoint;
        else
            return null;
    }

    public void setVisible(boolean b)
    {
        vtkRenderer.getFootprintActor().SetVisibility(b ? 1 : 0);
        super.setVisible(b);
    }

    public double getDefaultOffset()
    {
        return 3.0*smallBodyModel.getMinShiftAmount();
    }

    public void imageAboutToBeRemoved()
    {
        setShowFrustum(false);
    }

    public int getNumberOfComponentsOfOriginalImage()
    {
        return vtkRenderer.getRawImage().GetNumberOfScalarComponents();
    }

    /**
     * Return surface area of footprint (unshifted) of image.
     * @return
     */
    public double getSurfaceArea()
    {
        return PolyDataUtil.getSurfaceArea(vtkRenderer.getFootprint()[currentSlice]);
    }

    public double getOpacity()
    {
        return imageOpacity;
    }

    public void setOpacity(double imageOpacity)
    {
        this.imageOpacity = imageOpacity;
        vtkProperty smallBodyProperty = vtkRenderer.getFootprintActor().GetProperty();
        smallBodyProperty.SetOpacity(imageOpacity);
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public double getMaxFrustumDepth(int slice)
    {
        return vtkRenderer.getMaxFrustumDepth(slice);
//        return maxFrustumDepth[slice];
    }

    public void setMaxFrustumDepth(int slice, double value)
    {
        vtkRenderer.setMaxFrustumDepth(slice, value);
    }

    public double getMinFrustumDepth(int slice)
    {
        return vtkRenderer.getMinFrustumDepth(slice);
//        return minFrustumDepth[slice];
    }

    public void setMinFrustumDepth(int slice, double value)
    {
        vtkRenderer.setMinFrustumDepth(slice, value);
    }



    public double[][] getSpacecraftPositionAdjusted()
    {
        return spacecraftPositionAdjusted;
    }


    public double[][] getFrustum1Adjusted()
    {
        return frustum1Adjusted;
    }


    public double[][] getFrustum2Adjusted()
    {
        return frustum2Adjusted;
    }


    public double[][] getFrustum3Adjusted()
    {
        return frustum3Adjusted;
    }


    public double[][] getFrustum4Adjusted()
    {
        return frustum4Adjusted;
    }


    //******************
    //IO Delegates
    //******************

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
        return targetPixelCoordinates;
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
        return boresightDirectionAdjusted;
    }


    public double[][] getUpVectorAdjusted()
    {
        return upVectorAdjusted;
    }


    public double[][] getSunPositionAdjusted()
    {
        return sunPositionAdjusted;
    }


//    public void setRawImage(vtkImageData rawImage)
//    {
//        this.rawImage = rawImage;
//    }


    public void setImageName(String imageName)
    {
        this.imageName = imageName;
    }


    public void setInfoFileIO(InfoFileIO infoFileIO)
    {
        this.infoFileIO = infoFileIO;
    }


    public void setSumFileIO(SumFileIO sumFileIO)
    {
        this.sumFileIO = sumFileIO;
    }


    public void setLabelFileIO(LabelFileIO labelFileIO)
    {
        this.labelFileIO = labelFileIO;
    }


    public InfoFileIO getInfoFileIO()
    {
        return infoFileIO;
    }


    public SumFileIO getSumFileIO()
    {
        return sumFileIO;
    }


    public LabelFileIO getLabelFileIO()
    {
        return labelFileIO;
    }

    public PerspectiveImageIO getFileIO()
    {
        return fileIO;
    }


    public IOfflimbRenderEngine getOfflimb()
    {
        return offlimb;
    }


    public boolean[] getFootprintGenerated()
    {
        return footprintGenerated;
    }


    public void setFootprintGenerated(boolean footprintGenerated)
    {
        this.footprintGenerated[getDefaultSlice()] = footprintGenerated;
    }

    public void setFootprintGenerated(boolean footprintGenerated, int slice)
    {
        this.footprintGenerated[slice] = footprintGenerated;
    }


    public boolean isNormalsGenerated()
    {
        return normalsGenerated;
    }


    public static boolean isGenerateFootprint()
    {
        return generateFootprint;
    }


    public void setNormalsGenerated(boolean normalsGenerated)
    {
        this.normalsGenerated = normalsGenerated;
    }


    public String getEnviHeaderAppend()
    {
        return "";
    }


    protected int index(int i, int j, int k)
    {
        return ((k * imageHeight + j) * imageWidth + i);
    }

    /**
     * Generate metadata to be used in PDS4 XML creation by parsing existing PDS3 label.
     * By default creates a bare-bones metadata class that only contains the
     * output XML filename.
     * Use this method to use an existing PDS3 label as the source metadata on which to
     * describe a new PDS4 product.
     */
    public BPMetaBuilder pds3ToXmlMeta(String pds3Fname, String outXmlFname) {
        return PerspectiveImageIOHelpers.pds3ToXmlMeta(pds3Fname, outXmlFname);
    }

    /**
     * Generate metadata to be used in PDS4 XML creation by parsing existing PDS4 label.
     * By default creates a bare-bones metdata class that only contains the output
     * XML filename.
     * Use this method to use an existing PDS4 label as the source metadata on which to
     * describe a new PDS4 product.
     */
    public BPMetaBuilder pds4ToXmlMeta(String pds4Fname, String outXmlFname) {
        return PerspectiveImageIOHelpers.pds4ToXmlMeta(pds4Fname, outXmlFname);
    }

    /**
     * Parse additional metadata from the fits file and add to the metaDataBuilder.
     * @throws FitsException
     */
    public BPMetaBuilder fitsToXmlMeta(File fitsFile, BPMetaBuilder metaDataBuilder)
            throws FitsException {
        return metaDataBuilder;
    }

    /**
     * Generate XML document from XmlMetadata
     * @param metaData - metadata to be used in populating XmlDoc
     * @param xmlTemplate - path to XML template file
     */
    public BackPlanesXml metaToXmlDoc(BackPlanesXmlMeta metaData, String xmlTemplate) {
        BackPlanesXml xmlLabel = new BackPlanesXml(metaData, xmlTemplate);
        return xmlLabel;
    }

    public static void appendWithPadding(StringBuffer strbuf, String str)
    {
        PerspectiveImageIOHelpers.appendWithPadding(strbuf, str);
    }

    /**
     * Generate PDS 3 format backplanes label file. This is the default
     * implementation for classes extending PerspectiveImage.
     *
     * @param imgName
     *            - pointer to the data File for which this label is being
     *            created
     * @param lblFileName
     *            - pointer to the output label file to be written, without file
     *            name extension. The extension is dependent on image type (e.g.
     *            MSI images are written as PDS 4 XML labels), and is assigned
     *            in the class implementing this function.
     * @throws IOException
     */
    public void generateBackplanesLabel(File imgName, File lblFileName) throws IOException
    {
        PerspectiveImageIOHelpers.generateBackplanesLabel(imgName, lblFileName, this);
    }

    public float[] generateBackplanes()
    {
        return generateBackplanes(false);
    }

    /**
     * If <code>returnNullIfContainsLimb</code> then return null if any ray
     * in the direction of a pixel in the image does not intersect the asteroid.
     * By setting this boolean to true, you can (usually) determine whether or not the
     * image contains a limb without having to compute the entire backplane. Note
     * that this is a bit of a hack and a better way is needed to quickly determine
     * if there is a limb.
     *
     * @param returnNullIfContainsLimb
     * @return
     */
    private float[] generateBackplanes(boolean returnNullIfContainsLimb)
    {
        return PerspectiveImageIOHelpers.generateBackplanes(returnNullIfContainsLimb, this);
    }


    @Override
    public void outputToOBJ(String filePath)
    {
        PerspectiveImageIOHelpers.outputToOBJ(filePath, this);
    }


}