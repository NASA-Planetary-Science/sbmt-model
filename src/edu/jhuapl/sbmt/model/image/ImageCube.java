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
import vtk.vtkProp;
import vtk.vtkTexture;
import vtk.vtksbCellLocator;

import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.model.ModelNames;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.IntensityRange;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SbmtModelFactory;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.util.ImageDataUtil;

import nom.tam.fits.FitsException;

public class ImageCube extends PerspectiveImage implements PropertyChangeListener
{
//    private SmallBodyModel smallBodyModel;
    private List<PerspectiveImage> images;
    private int nimages;
    private List<Integer> imageSlices;
    private PerspectiveImage firstImage;
    private int firstSlice;
    private int firstImageIndex;

//    private vtkImageData colorImage;
    private vtkImageData rawImage;
    private vtkImageData displayedImage;
    private vtkTexture imageTexture;
    private vtkPolyData footprint;
    private Frustum firstFrustum;
    private vtkPolyData shiftedFootprint;
    private vtkActor footprintActor;
    private List<vtkProp> footprintActors = new ArrayList<vtkProp>();
    private float[][][] pixelData;
    private float[][] firstPixelData;

    private Chromatism chromatism = Chromatism.POLY;
    private double redScale = 1.0;
    private double greenScale = 1.0;
    private double blueScale = 1.0;
    private IntensityRange redIntensityRange;
    private IntensityRange greenIntensityRange;
    private IntensityRange blueIntensityRange;
    private double offset;
    private double imageOpacity = 1.0;
    private int imageWidth;
    private int imageHeight;
    private vtkImageCanvasSource2D maskSource;
    private int[] currentMask;



    static public class NoOverlapException extends RuntimeException
    {
        public NoOverlapException()
        {
            super("No overlap in 3 images");
        }
    }

    public static enum Chromatism { POLY, MONO_RED, MONO_GREEN, MONO_BLUE };

    public static class ImageCubeKey extends Image.ImageKey
    {
        public List<PerspectiveImage.ImageKey> imageKeys;
        public PerspectiveImage.ImageKey firstImageKey;

        public String labelFileFullPath;
        public String infoFileFullPath;
        public String sumFileFullPath;
        public int nimages;

        public ImageCubeKey(List<ImageKey> imageKeys, ImageKey firstImageKey,
                String labelFileFullPath, String infoFileFullPath, String sumFileFullPath)
        {
            super(firstImageKey.name + "-cube", firstImageKey.source, firstImageKey.fileType, firstImageKey.imageType, firstImageKey.instrument, firstImageKey.band, firstImageKey.slice);

            this.imageKeys = new ArrayList<ImageKey>(imageKeys);
            this.nimages = imageKeys.size();
            this.firstImageKey = firstImageKey;
            this.labelFileFullPath = labelFileFullPath;
            this.infoFileFullPath = infoFileFullPath;
            this.sumFileFullPath = sumFileFullPath;
        }

        @Override
        public boolean equals(Object obj)
        {
            ImageCubeKey objectCubeKey = (ImageCubeKey)obj;
            List<ImageKey> objectKeys = objectCubeKey.imageKeys;

            for (int i = 0; i < imageKeys.size(); i++)
            {
                ImageKey thisKey = this.imageKeys.get(i);
                ImageKey objectKey = objectKeys.get(i);
                if (!thisKey.equals(objectKey))
                    return false;
            }
            return true;
        }

        @Override
        public String toString()
        {
            // Find the start and stop indices of number part of the name. Should be
            // the same for all 3 images.
            String name = new File(firstImageKey.name).getName();
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
            if (ind1 == -1 || ind1 == ind0) ind1 = buf.length;

            String result = "";
            for (ImageKey key : imageKeys)
            {
                result = result + new File(key.name).getName().substring(ind0, ind1).toString() + ", ";
            }

            return result;
        }

        public String fileNameString()
        {
            // Find the start and stop indices of number part of the name. Should be
            // the same for all 3 images.
            String name = new File(firstImageKey.name).getName();
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
            if (ind1 == -1) ind1 = buf.length;
            if (buf[ind0] == '0')
                ++ind0;

            String result = "";
            for (ImageKey key : imageKeys)
                result = result + new File(key.name).getName().substring(ind0, ind1).toString() + "-";

            return result;
        }

        public PerspectiveImage.ImageKey getFirstImageKey()
        {
            return firstImageKey;
        }
}


    public String getImageName()
    {
        return new File(getImageCubeKey().fileNameString()).getName();
//        return new File(imageCubeKey.redImageKey.name).getName();
    }

    protected String initializeLabelFileFullPath() { return ((ImageCubeKey)getKey()).labelFileFullPath; }

    @Override
    protected String initLocalInfoFileFullPath()
    {
        return initializeInfoFileFullPath();
    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        System.out.println("ImageCube: initializeInfoFileFullPath: returning " + ((ImageCubeKey)getKey()).infoFileFullPath);
        return ((ImageCubeKey)getKey()).infoFileFullPath;
    }

    protected String initializeSumfileFullPath() { return ((ImageCubeKey)getKey()).sumFileFullPath; }

    protected String initializeFitFileFullPath() { return null; }
    protected String initializeEnviFileFullPath() {return null; }
    protected String initializePngFileFullPath() { return null; }

    public ImageCube(ImageCubeKey key, SmallBodyModel smallBodyModel, ModelManager modelManager) throws FitsException, IOException, NoOverlapException
    {
        super(key, smallBodyModel, modelManager, false);
    }

    protected vtkImageData loadRawImage() throws FitsException, IOException
    {
        ImageCubeKey imageCubeKey = (ImageCubeKey)getKey();

        footprintActors = new ArrayList<vtkProp>();
        chromatism = Chromatism.POLY;
        redScale = 1.0;
        greenScale = 1.0;
        blueScale = 1.0;
        imageOpacity = 1.0;
        currentMask = new int[4];

        this.offset = getDefaultOffset();


        images = new ArrayList<PerspectiveImage>();
        imageSlices = new ArrayList<Integer>();
        for (ImageKey key : imageCubeKey.imageKeys)
        {
            PerspectiveImage image = createImage(key, getSmallBodyModel(), getModelManager());
            images.add(image);
            imageSlices.add(0); // twupy1: Hardcoded image slice to 0 since image cubes are always made from single slice images.  This was causing problems otherwise.
            if (key.equals(imageCubeKey.firstImageKey))
            {
                firstImage = image;
                firstSlice = 0; // twupy1: Hardcoded image slice to 0 since image cubes are always made from single slice images.  This was causing problems otherwise.
                firstFrustum = image.getFrustum();
            }
        }

        int nkeys = images.size();
        pixelData = new float[nkeys][][];
        for (int i=0; i<nkeys; i++)
        {
            PerspectiveImage image = images.get(i);
            int slice = imageSlices.get(i);
            pixelData[i] = ImageDataUtil.vtkImageDataToArray2D(image.getRawImage(), slice);
            if (image.equals(firstImage))
            {
                firstPixelData = pixelData[i];
                firstImageIndex = i;
            }

        }

//        colorImage = new vtkImageData();
        imageWidth = firstImage.getImageWidth();
        imageHeight = firstImage.getImageHeight();
//        colorImage.SetDimensions(imageWidth, imageHeight, 1);
//        colorImage.SetSpacing(1.0, 1.0, 1.0);
//        colorImage.SetOrigin(0.0, 0.0, 0.0);
//        colorImage.AllocateScalars(VtkDataTypes.VTK_UNSIGNED_CHAR, 3);

        shiftedFootprint = new vtkPolyData();

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

        vtkImageData rawImage = computeFootprintAndImageCube();
        return rawImage;

//        return colorImage;
    }

    protected PerspectiveImage createImage(ImageKey key, SmallBodyModel smallBodyModel, ModelManager modelManager) throws FitsException, IOException
    {
        ImageCollection images = (ImageCollection)modelManager.getModel(ModelNames.IMAGES);
        PerspectiveImage result = (PerspectiveImage)images.getImage(key);
        if (result == null)
            result = (PerspectiveImage)SbmtModelFactory.createImage(key, smallBodyModel, false);
        return result;
    }

    protected int loadNumSlices()
    {
        return ((ImageCubeKey)getKey()).nimages;
    }


    protected vtkPolyData getFootprint(int defaultSlice)
    {
        return footprint;
    }

//    protected vtkImageData getDisplayedImage()
//    {
//        return displayedImage;
//    }

    private vtkImageData computeFootprintAndImageCube() throws NoOverlapException
    {
//        redIntensityRange = new IntensityRange(0, 255);
//        greenIntensityRange = new IntensityRange(0, 255);
//        blueIntensityRange = new IntensityRange(0, 255);

        List<IntensityRange> intensityRanges = new ArrayList<IntensityRange>();
        List<Frustum> frustums = new ArrayList<Frustum>();
        nimages = images.size();
        System.out.println("ImageCube: computeFootprintAndImageCube: number of images " + nimages);
        double[] mins = new double[nimages];
        double[] maxes = new double[nimages];
        double[] stretchRanges = new double[nimages];
        for (int i=0; i<nimages; i++)
        {
            PerspectiveImage image = images.get(i);

            int slice = imageSlices.get(i);
            Frustum frustum = image.getFrustum(slice);
            frustums.add(frustum);

            IntensityRange intensityRange = new IntensityRange(0, 255);
            intensityRanges.add(intensityRange);

            double[] range = image.getScalarRange(slice);
            double fullRange = range[1] - range[0];
            double dx = fullRange / 255.0f;
            mins[i] = range[0] + intensityRange.min * dx;
            maxes[i] = range[0] + intensityRange.max * dx;
            stretchRanges[i] = maxes[i] - mins[i];
        }

//        double[] redRange = redImage.getScalarRange(redImageSlice);
//        double[] greenRange = greenImage.getScalarRange(greenImageSlice);
//        double[] blueRange = blueImage.getScalarRange(blueImageSlice);

//        double redfullRange = redRange[1] - redRange[0];
//        double reddx = redfullRange / 255.0f;
//        double redmin = redRange[0] + redIntensityRange.min*reddx;
//        double redmax = redRange[0] + redIntensityRange.max*reddx;
//        double redstretchRange = redmax - redmin;
//
//        double greenfullRange = greenRange[1] - greenRange[0];
//        double greendx = greenfullRange / 255.0f;
//        double greenmin = greenRange[0] + greenIntensityRange.min*greendx;
//        double greenmax = greenRange[0] + greenIntensityRange.max*greendx;
//        double greenstretchRange = greenmax - greenmin;
//
//        double bluefullRange = blueRange[1] - blueRange[0];
//        double bluedx = bluefullRange / 255.0f;
//        double bluemin = blueRange[0] + blueIntensityRange.min*bluedx;
//        double bluemax = blueRange[0] + blueIntensityRange.max*bluedx;
//        double bluestretchRange = bluemax - bluemin;

        footprint = getSmallBodyModel().computeMultipleFrustumIntersection(frustums);

        if (footprint == null)
            throw new NoOverlapException();

        // Need to clear out scalar data since if coloring data is being shown,
        // then the color might mix-in with the image.
        footprint.GetCellData().SetScalars(null);
        footprint.GetPointData().SetScalars(null);

        int imageWidth = firstImage.getImageWidth();
        int imageHeight = firstImage.getImageHeight();

        PolyDataUtil.generateTextureCoordinates(firstFrustum, imageWidth, imageHeight, footprint);

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

        double[] spacecraftPosition = firstFrustum.origin;
//        double[] frustum1 = firstFrustum.ul;
//        double[] frustum2 = firstFrustum.ll;
//        double[] frustum3 = firstFrustum.ur;

        double[] frustum1 = firstFrustum.ul;
        double[] frustum2 = firstFrustum.lr;
        double[] frustum3 = firstFrustum.ur;

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

        float[][][] array3D = new float[imageHeight][nimages][imageWidth];

        for (int i=0; i<imageHeight; ++i)
        {
            // Compute the vector on the left of the row.
            double fracHeight = ((double)i / (double)(imageHeight-1));
            double[] left = {
                    corner1[0] + fracHeight*vec13[0],
                    corner1[1] + fracHeight*vec13[1],
                    corner1[2] + fracHeight*vec13[2]
            };

            for (int j=0; j<imageWidth; ++j)
            {
                double fracWidth = ((double)j / (double)(imageWidth-1));
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
                    for (int k=0; k<nimages; ++k)
                    {

                        double[] uv = new double[2];

                        Frustum frustum = frustums.get(k);
                        float[][] pixels = pixelData[k];
                        double value = 0.0;

                        if (k == firstImageIndex)
                        {
                            value = pixelData[firstImageIndex][j][i];
                        }
                        else
                        {
                            frustum.computeTextureCoordinatesFromPoint(x, imageWidth, imageHeight, uv, true);
                            value = ImageDataUtil.interpolateWithinImage(
                                    pixels,
                                    imageWidth,
                                    imageHeight,
                                    uv[1],
                                    uv[0]);
                        }

                        if (value < mins[k])
                            value = mins[k];
                        if (value > maxes[k])
                            value = maxes[k];
// Note: should make this a user-selectable option -turnerj1
//                        double component = 255.0 * redScale * (value - mins[k]) / stretchRanges[k];
                        double component = value;
//                        System.out.println("Band: " + k + " Value: " + component);
                        array3D[i][k][j] = (float)component;
                    }
                }
            }
        }

//        colorImage.Modified();

        rawImage = createRawImage(imageHeight, imageWidth, nimages, null, array3D);
        return rawImage;

    }

//    @Override
//    public List<vtkProp> getProps()
//    {
//        if (footprintActor == null)
//        {
//            imageTexture = new vtkTexture();
//            imageTexture.InterpolateOn();
//            imageTexture.RepeatOff();
//            imageTexture.EdgeClampOn();
////            texture.SetInput(colorImage);
//            imageTexture.SetInputData(getDisplayedImage());
//
//            vtkPolyDataMapper footprintMapper = new vtkPolyDataMapper();
//            footprintMapper.SetInputData(shiftedFootprint);
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
//        return footprintActors;
//    }

    public ImageCubeKey getImageCubeKey()
    {
        return (ImageCubeKey)getKey();
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_RESOLUTION_CHANGED.equals(evt.getPropertyName()))
        {
            try
            {
                computeFootprintAndImageCube();

                this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

//    public PerspectiveImage getRedImage()
//    {
//        return redImage;
//    }
//
//    public PerspectiveImage getGreenImage()
//    {
//        return greenImage;
//    }
//
//    public PerspectiveImage getBlueImage()
//    {
//        return blueImage;
//    }
//
    protected void processRawImage(vtkImageData rawImage)
    {
        ImageDataUtil.flipImageYAxis(rawImage);
    }
    /**
     * Currently, just call updateImageMask
     */
//    @Override
//    public void setDisplayedImageRange(IntensityRange range)
//    {
//        if (range == null)
//        {
//            updateImageMask();
//        }
//        else
//            setDisplayedImageRange(1.0, range);
//    }

//    public void setDisplayedImageRange(double scale, IntensityRange range)
//    {
//        setDisplayedImageRange(scale, range, scale, range, scale, range, Chromatism.POLY);
//    }
//
//    public void setDisplayedImageRange(IntensityRange redRange, IntensityRange greenRange, IntensityRange blueRange)
//    {
//        setDisplayedImageRange(1.0, redRange, 1.0, greenRange, 1.0, blueRange, Chromatism.POLY);
//    }
//
//    public void setDisplayedImageRange(double redScale, IntensityRange redRange, double greenScale, IntensityRange greenRange, double blueScale, IntensityRange blueRange, Chromatism chromatism)
//    {
//        this.chromatism = chromatism;
//        this.redScale = redScale;
//        this.greenScale = greenScale;
//        this.blueScale = blueScale;
//        redIntensityRange = redRange;
//        greenIntensityRange = greenRange;
//        blueIntensityRange = blueRange;
//
//        try
//        {
//            computeFootprintAndImageCube();
//
//            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
//        }
//        catch (NoOverlapException e)
//        {
//            e.printStackTrace();
//        }
//    }

//    public double getOpacity()
//    {
//        return imageOpacity;
//    }

//    public void setOpacity(double imageOpacity)
//    {
//        this.imageOpacity  = imageOpacity;
//        vtkProperty smallBodyProperty = footprintActor.GetProperty();
//        smallBodyProperty.SetOpacity(imageOpacity);
//        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
//    }

//    public void setVisible(boolean b)
//    {
//        footprintActor.SetVisibility(b ? 1 : 0);
//        super.setVisible(b);
//    }

    public double getDefaultOffset()
    {
        return 4.0* this.getSmallBodyModel().getMinShiftAmount();
    }

//    public void setOffset(double offset)
//    {
//        this.offset = offset;
//
//        shiftedFootprint.DeepCopy(footprint);
//        PolyDataUtil.shiftPolyDataInNormalDirection(shiftedFootprint, offset);
//
//        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
//    }
//
//    public double getOffset()
//    {
//        return offset;
//    }

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

//    @Override
//    public vtkTexture getTexture()
//    {
//        return imageTexture;
//    }

    @Override
    public LinkedHashMap<String, String> getProperties() throws IOException
    {
//        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
//        result.put("pi", Double.toString(Math.PI));
//        result.put("e", Double.toString(Math.E));
//        return result;
        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>(firstImage.getProperties());
        result.put("Name", getImageCubeKey().fileNameString());

        return result;
    }

    public void updateImageMask()
    {
        vtkImageData maskSourceOutput = maskSource.GetOutput();

        vtkImageMask maskFilter = new vtkImageMask();
        maskFilter.SetImageInputData(rawImage);
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

    @Override
    protected String getEnviHeaderAppend()
    {
        String appendString = "band names = {";
        for(int i=0; i<images.size(); i++)
        {
            if(i > 0)
            {
                appendString += ", ";
            }
            appendString += images.get(i).getImageName();
        }
        appendString += "}";
        return appendString;
    }
}
