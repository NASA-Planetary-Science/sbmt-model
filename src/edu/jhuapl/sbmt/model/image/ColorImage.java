package edu.jhuapl.sbmt.model.image;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import vtk.vtkActor;
import vtk.vtkGenericCell;
import vtk.vtkImageCanvasSource2D;
import vtk.vtkImageData;
import vtk.vtkImageMask;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkProperty;
import vtk.vtkTexture;
import vtk.vtksbCellLocator;

import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.model.ModelNames;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.IntensityRange;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.saavtk.util.VtkDataTypes;
import edu.jhuapl.sbmt.app.SbmtModelFactory;
import edu.jhuapl.sbmt.app.SmallBodyModel;
import edu.jhuapl.sbmt.util.ImageDataUtil;

import nom.tam.fits.FitsException;

public class ColorImage extends Image implements PropertyChangeListener
{
    private SmallBodyModel smallBodyModel;
    private PerspectiveImage redImage;
    private PerspectiveImage greenImage;
    private PerspectiveImage blueImage;
    private int redImageSlice;
    private int greenImageSlice;
    private int blueImageSlice;
    private vtkImageData colorImage;
    private vtkImageData displayedImage;
    private vtkTexture imageTexture;
    private vtkPolyData footprint;
    private vtkPolyData shiftedFootprint;
    private vtkActor footprintActor;
    private ArrayList<vtkProp> footprintActors = new ArrayList<vtkProp>();
    private float[][] redPixelData;
    private float[][] greenPixelData;
    private float[][] bluePixelData;
    private ColorImageKey colorKey;
    private Chromatism chromatism = Chromatism.POLY;
    private double redScale = 1.0;
    private double greenScale = 1.0;
    private double blueScale = 1.0;
    private IntensityRange redIntensityRange = new IntensityRange(0, 255);
    private IntensityRange greenIntensityRange = new IntensityRange(0, 255);
    private IntensityRange blueIntensityRange = new IntensityRange(0, 255);
    private double offset;
    private double imageOpacity = 1.0;
    private int imageWidth;
    private int imageHeight;
    private vtkImageCanvasSource2D maskSource;
    private int[] currentMask = new int[4];



    static public class NoOverlapException extends Exception
    {
        public NoOverlapException()
        {
            super("No overlap in 3 images");
        }
    }

    public static enum Chromatism { POLY, MONO_RED, MONO_GREEN, MONO_BLUE };

    public static class ColorImageKey
    {
        public PerspectiveImage.ImageKey redImageKey;
        public PerspectiveImage.ImageKey greenImageKey;
        public PerspectiveImage.ImageKey blueImageKey;

        public ColorImageKey(PerspectiveImage.ImageKey redImage, PerspectiveImage.ImageKey greenImage, PerspectiveImage.ImageKey blueImage)
        {
            this.redImageKey = redImage;
            this.greenImageKey = greenImage;
            this.blueImageKey = blueImage;
        }

        @Override
        public boolean equals(Object obj)
        {
            return redImageKey.equals(((ColorImageKey)obj).redImageKey) &&
            greenImageKey.equals(((ColorImageKey)obj).greenImageKey) &&
            blueImageKey.equals(((ColorImageKey)obj).blueImageKey);
        }

        @Override
        public String toString()
        {
            // Find the start and stop indices of number part of the name. Should be
            // the same for all 3 images.
            String name = new File(redImageKey.name).getName();
            char[] buf = name.toCharArray();
            int ind0 = -1;
            int ind1 = -1;
            for (int i = 0; i<buf.length; ++i)
            {
                if (Character.isDigit(buf[i]) && ind0 == -1)
                    ind0 = i;
                else if(!Character.isDigit(buf[i]) && ind0 >= 0)
                {
                    ind1 = i;
                    break;
                }
            }

            if (buf[ind0] == '0')
                ++ind0;

            return
            "R: " + new File(redImageKey.name).getName().substring(ind0, ind1) + ", " +
            "G: " + new File(greenImageKey.name).getName().substring(ind0, ind1) + ", " +
            "B: " + new File(blueImageKey.name).getName().substring(ind0, ind1);
        }
    }


    public String getImageName()
    {
        return new File(colorKey.redImageKey.name).getName();
    }

    public ColorImage(ColorImageKey key, SmallBodyModel smallBodyModel, ModelManager modelManager) throws FitsException, IOException, NoOverlapException
    {
        super(new ImageKey("FalseColorImage", ImageSource.FALSE_COLOR));
        this.colorKey = key;
        this.smallBodyModel = smallBodyModel;

        this.offset = getDefaultOffset();

        redImage = createImage(colorKey.redImageKey, smallBodyModel, modelManager);
        greenImage = createImage(colorKey.greenImageKey, smallBodyModel, modelManager);
        blueImage = createImage(colorKey.blueImageKey, smallBodyModel, modelManager);

        redImageSlice = colorKey.redImageKey.slice;
        greenImageSlice = colorKey.greenImageKey.slice;
        blueImageSlice = colorKey.blueImageKey.slice;

        int rslice = colorKey.redImageKey.slice;
        redPixelData = ImageDataUtil.vtkImageDataToArray2D(redImage.getRawImage(), rslice);

        int gslice = colorKey.greenImageKey.slice;
        greenPixelData = ImageDataUtil.vtkImageDataToArray2D(greenImage.getRawImage(), gslice);

        int bslice = colorKey.blueImageKey.slice;
        bluePixelData = ImageDataUtil.vtkImageDataToArray2D(blueImage.getRawImage(), bslice);

        colorImage = new vtkImageData();
        imageWidth = redImage.getImageWidth();
        imageHeight = redImage.getImageHeight();
        colorImage.SetDimensions(imageWidth, imageHeight, 1);
        colorImage.SetSpacing(1.0, 1.0, 1.0);
        colorImage.SetOrigin(0.0, 0.0, 0.0);
        colorImage.AllocateScalars(VtkDataTypes.VTK_UNSIGNED_CHAR, 3);

        shiftedFootprint = new vtkPolyData();

        computeFootprintAndColorImage();

        int[] masking = getMaskSizes();
        int topMask =    masking[0];
        int rightMask =  masking[1];
        int bottomMask = masking[2];
        int leftMask =   masking[3];
        for (int i=0; i<masking.length; ++i)
            currentMask[i] = masking[i];

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

        updateImageMask();
    }

    public vtkImageData getImage()
    {
        return displayedImage; // colorImage;
    }

    protected PerspectiveImage createImage(ImageKey key, SmallBodyModel smallBodyModel, ModelManager modelManager) throws FitsException, IOException
    {
        ImageCollection images = (ImageCollection)modelManager.getModel(ModelNames.IMAGES);
        PerspectiveImage result = (PerspectiveImage)images.getImage(key);
        if (result == null)
            result = (PerspectiveImage)SbmtModelFactory.createImage(key, smallBodyModel, false);
        return result;
    }

    private void computeFootprintAndColorImage() throws NoOverlapException
    {
        Frustum redFrustum = redImage.getFrustum(redImageSlice);
        Frustum greenFrustum = greenImage.getFrustum(greenImageSlice);
        Frustum blueFrustum = blueImage.getFrustum(blueImageSlice);

        double[] redRange = redImage.getScalarRange(redImageSlice);
        double[] greenRange = greenImage.getScalarRange(greenImageSlice);
        double[] blueRange = blueImage.getScalarRange(blueImageSlice);

        double redfullRange = redRange[1] - redRange[0];
        double reddx = redfullRange / 255.0f;
        double redmin = redRange[0] + redIntensityRange.min*reddx;
        double redmax = redRange[0] + redIntensityRange.max*reddx;
        double redstretchRange = redmax - redmin;

        double greenfullRange = greenRange[1] - greenRange[0];
        double greendx = greenfullRange / 255.0f;
        double greenmin = greenRange[0] + greenIntensityRange.min*greendx;
        double greenmax = greenRange[0] + greenIntensityRange.max*greendx;
        double greenstretchRange = greenmax - greenmin;

        double bluefullRange = blueRange[1] - blueRange[0];
        double bluedx = bluefullRange / 255.0f;
        double bluemin = blueRange[0] + blueIntensityRange.min*bluedx;
        double bluemax = blueRange[0] + blueIntensityRange.max*bluedx;
        double bluestretchRange = bluemax - bluemin;

        ArrayList<Frustum> frustums = new ArrayList<Frustum>();
        frustums.add(redFrustum);
        frustums.add(greenFrustum);
        frustums.add(blueFrustum);

        footprint = smallBodyModel.computeMultipleFrustumIntersection(frustums);

        if (footprint == null)
            throw new NoOverlapException();

        // Need to clear out scalar data since if coloring data is being shown,
        // then the color might mix-in with the image.
        footprint.GetCellData().SetScalars(null);
        footprint.GetPointData().SetScalars(null);

        int IMAGE_WIDTH = redImage.getImageWidth();
        int IMAGE_HEIGHT = redImage.getImageHeight();

        PolyDataUtil.generateTextureCoordinates(redFrustum, IMAGE_WIDTH, IMAGE_HEIGHT, footprint);

        shiftedFootprint.DeepCopy(footprint);
        PolyDataUtil.shiftPolyDataInNormalDirection(shiftedFootprint, offset);

        // Now compute a color image with each channel one of these images.
        // To do that go through each pixel of the red image, and intersect a ray into the asteroid in
        // the direction of that pixel. Then compute the texture coordinates that the intersection
        // point would have for the green and blue images. Do linear interpolation in
        // the green and blue images to compute the green and blue channels.

        vtksbCellLocator cellLocator = new vtksbCellLocator();
        cellLocator.SetDataSet(footprint);
        cellLocator.CacheCellBoundsOn();
        cellLocator.AutomaticOn();
        //cellLocator.SetMaxLevel(10);
        //cellLocator.SetNumberOfCellsPerNode(15);
        cellLocator.BuildLocator();

        vtkGenericCell cell = new vtkGenericCell();

        double[] spacecraftPosition = redFrustum.origin;
        double[] frustum1 = redFrustum.ul;
        double[] frustum2 = redFrustum.lr;
        double[] frustum3 = redFrustum.ur;

        double[] corner1 = {
                spacecraftPosition[0] + frustum1[0],
                spacecraftPosition[1] + frustum1[1],
                spacecraftPosition[2] + frustum1[2]
        };
        double[] corner2 = {
                spacecraftPosition[0] + frustum2[0],
                spacecraftPosition[1] + frustum2[1],
                spacecraftPosition[2] + frustum2[2]
        };
        double[] corner3 = {
                spacecraftPosition[0] + frustum3[0],
                spacecraftPosition[1] + frustum3[1],
                spacecraftPosition[2] + frustum3[2]
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


        double scdist = MathUtil.vnorm(spacecraftPosition);

        for (int i=0; i<IMAGE_HEIGHT; ++i)
        {
            // Compute the vector on the left of the row.
            double fracHeight = ((double)i / (double)(IMAGE_HEIGHT-1));
            double[] left = {
                    corner1[0] + fracHeight*vec13[0],
                    corner1[1] + fracHeight*vec13[1],
                    corner1[2] + fracHeight*vec13[2]
            };

            for (int j=0; j<IMAGE_WIDTH; ++j)
            {
                double fracWidth = ((double)j / (double)(IMAGE_WIDTH-1));
                double[] vec = {
                        left[0] + fracWidth*vec12[0],
                        left[1] + fracWidth*vec12[1],
                        left[2] + fracWidth*vec12[2]
                };
                vec[0] -= spacecraftPosition[0];
                vec[1] -= spacecraftPosition[1];
                vec[2] -= spacecraftPosition[2];
                MathUtil.unorm(vec, vec);

                double[] lookPt = {
                        spacecraftPosition[0] + 2.0*scdist*vec[0],
                        spacecraftPosition[1] + 2.0*scdist*vec[1],
                        spacecraftPosition[2] + 2.0*scdist*vec[2]
                };

                double tol = 1e-6;
                double[] t = new double[1];
                double[] x = new double[3];
                double[] pcoords = new double[3];
                int[] subId = new int[1];
                int[] cellId = new int[1];
                int result = cellLocator.IntersectWithLine(spacecraftPosition, lookPt, tol, t, x, pcoords, subId, cellId, cell);

                if (result > 0)
                {
                    double redValue = redPixelData[j][i];

                    double[] uv = new double[2];

                    greenFrustum.computeTextureCoordinatesFromPoint(x, IMAGE_WIDTH, IMAGE_HEIGHT, uv, true);
                    double greenValue = ImageDataUtil.interpolateWithinImage(
                            greenPixelData,
                            IMAGE_WIDTH,
                            IMAGE_HEIGHT,
                            uv[1],
                            uv[0]);

                    blueFrustum.computeTextureCoordinatesFromPoint(x, IMAGE_WIDTH, IMAGE_HEIGHT, uv, true);
                    double blueValue = ImageDataUtil.interpolateWithinImage(
                            bluePixelData,
                            IMAGE_WIDTH,
                            IMAGE_HEIGHT,
                            uv[1],
                            uv[0]);

                    if (redValue < redmin)
                        redValue = redmin;
                    if (redValue > redmax)
                        redValue = redmax;

                    if (greenValue < greenmin)
                        greenValue = greenmin;
                    if (greenValue > greenmax)
                        greenValue = greenmax;

                    if (blueValue < bluemin)
                        blueValue = bluemin;
                    if (blueValue > bluemax)
                        blueValue = bluemax;

                    double redComponent = 255.0 * redScale * (redValue - redmin) / redstretchRange;
                    double greenComponent = 255.0 * greenScale * (greenValue - greenmin) / greenstretchRange;
                    double blueComponent = 255.0 * blueScale * (blueValue - bluemin) / bluestretchRange;

                    if (this.chromatism == Chromatism.MONO_RED)
                        greenComponent = blueComponent = redComponent;
                    else if (this.chromatism == Chromatism.MONO_GREEN)
                        blueComponent = redComponent = greenComponent;
                    else if (this.chromatism == Chromatism.MONO_BLUE)
                        greenComponent = redComponent = blueComponent;

                    colorImage.SetScalarComponentFromFloat(j, i, 0, 0, redComponent);
                    colorImage.SetScalarComponentFromFloat(j, i, 0, 1, greenComponent);
                    colorImage.SetScalarComponentFromFloat(j, i, 0, 2, blueComponent);
                }
            }
        }

        colorImage.Modified();
    }

    @Override
    public List<vtkProp> getProps()
    {
        if (footprintActor == null)
        {
            imageTexture = new vtkTexture();
            imageTexture.InterpolateOn();
            imageTexture.RepeatOff();
            imageTexture.EdgeClampOn();
//            texture.SetInput(colorImage);
            imageTexture.SetInputData(displayedImage);

            vtkPolyDataMapper footprintMapper = new vtkPolyDataMapper();
            footprintMapper.SetInputData(shiftedFootprint);
            footprintMapper.Update();

            footprintActor = new vtkActor();
            footprintActor.SetMapper(footprintMapper);
            footprintActor.SetTexture(imageTexture);
            vtkProperty footprintProperty = footprintActor.GetProperty();
            footprintProperty.LightingOff();

            footprintActors.add(footprintActor);
        }
        return footprintActors;
    }

    public ColorImageKey getColorKey()
    {
        return colorKey;
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_RESOLUTION_CHANGED.equals(evt.getPropertyName()))
        {
            try
            {
                computeFootprintAndColorImage();

                this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public PerspectiveImage getRedImage()
    {
        return redImage;
    }

    public PerspectiveImage getGreenImage()
    {
        return greenImage;
    }

    public PerspectiveImage getBlueImage()
    {
        return blueImage;
    }

    /**
     * Currently, just call updateImageMask
     */
    @Override
    public void setDisplayedImageRange(IntensityRange range)
    {
        if (range == null)
        {
            updateImageMask();
        }
        else
            setDisplayedImageRange(1.0, range);
    }

    public void setDisplayedImageRange(double scale, IntensityRange range)
    {
        setDisplayedImageRange(scale, range, scale, range, scale, range, Chromatism.POLY);
    }

    public void setDisplayedImageRange(IntensityRange redRange, IntensityRange greenRange, IntensityRange blueRange)
    {
        setDisplayedImageRange(1.0, redRange, 1.0, greenRange, 1.0, blueRange, Chromatism.POLY);
    }

    public void setDisplayedImageRange(double redScale, IntensityRange redRange, double greenScale, IntensityRange greenRange, double blueScale, IntensityRange blueRange, Chromatism chromatism)
    {
        this.chromatism = chromatism;
        this.redScale = redScale;
        this.greenScale = greenScale;
        this.blueScale = blueScale;
        redIntensityRange = redRange;
        greenIntensityRange = greenRange;
        blueIntensityRange = blueRange;

        try
        {
            computeFootprintAndColorImage();

            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
        catch (NoOverlapException e)
        {
            e.printStackTrace();
        }
    }

    public double getOpacity()
    {
        return imageOpacity;
    }

    public void setOpacity(double imageOpacity)
    {
        this.imageOpacity  = imageOpacity;
        vtkProperty smallBodyProperty = footprintActor.GetProperty();
        smallBodyProperty.SetOpacity(imageOpacity);
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void setVisible(boolean b)
    {
        footprintActor.SetVisibility(b ? 1 : 0);
        super.setVisible(b);
    }

    public double getDefaultOffset()
    {
        return 4.0*smallBodyModel.getMinShiftAmount();
    }

    public void setOffset(double offset)
    {
        this.offset = offset;

        shiftedFootprint.DeepCopy(footprint);
        PolyDataUtil.shiftPolyDataInNormalDirection(shiftedFootprint, offset);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public double getOffset()
    {
        return offset;
    }

    public void setCurrentMask(int[] masking)
    {
        int topMask =    masking[0];
        int rightMask =  masking[1];
        int bottomMask = masking[2];
        int leftMask =   masking[3];
        // Initialize the mask to black which masks out the image
        maskSource.SetDrawColor(0.0, 0.0, 0.0, 0.0);
        maskSource.FillBox(0, imageWidth-1, 0, imageHeight-1);
        // Create a square inside mask which passes through the image.
        maskSource.SetDrawColor(255.0, 255.0, 255.0, 255.0);
        maskSource.FillBox(leftMask, imageWidth-1-rightMask, bottomMask, imageHeight-1-topMask);
        maskSource.Update();

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        updateImageMask();

        for (int i=0; i<masking.length; ++i)
            currentMask[i] = masking[i];
    }

    public int[] getCurrentMask()
    {
        return currentMask.clone();
    }


    protected int[] getMaskSizes()
    {
        return new int[]{0, 0, 0, 0};
    }

    @Override
    public vtkTexture getTexture()
    {
        return imageTexture;
    }

    @Override
    public LinkedHashMap<String, String> getProperties() throws IOException
    {
//        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
//        result.put("pi", Double.toString(Math.PI));
//        result.put("e", Double.toString(Math.E));
//        return result;
        return redImage.getProperties();
    }

    public void updateImageMask()
    {
        vtkImageData maskSourceOutput = maskSource.GetOutput();

        vtkImageMask maskFilter = new vtkImageMask();
        maskFilter.SetImageInputData(colorImage);
        maskFilter.SetMaskInputData(maskSourceOutput);
        maskFilter.Update();

        if (displayedImage == null)
            displayedImage = new vtkImageData();
        vtkImageData maskFilterOutput = maskFilter.GetOutput();
        displayedImage.DeepCopy(maskFilterOutput);

        maskFilter.Delete();
        maskSourceOutput.Delete();
        maskFilterOutput.Delete();

        //vtkPNGWriter writer = new vtkPNGWriter();
        //writer.SetFileName("fit.png");
        //writer.SetInput(displayedImage);
        //writer.Write();

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    @Override
    public int getNumberOfComponentsOfOriginalImage()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * Implement Image abstract methods
     */



}
