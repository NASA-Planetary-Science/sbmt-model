package edu.jhuapl.sbmt.model.image.perspectiveImage;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

import vtk.vtkCell;
import vtk.vtkCellData;
import vtk.vtkDataArray;
import vtk.vtkImageCanvasSource2D;
import vtk.vtkImageData;
import vtk.vtkImageMapToColors;
import vtk.vtkImageMask;
import vtk.vtkImageReslice;
import vtk.vtkLookupTable;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataNormals;
import vtk.vtkPolyDataReader;
import vtk.vtkProp;
import vtk.vtkTexture;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.FillDetector;
import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.saavtk.util.IntensityRange;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.sbmt.core.image.IImagingInstrument;
import edu.jhuapl.sbmt.model.image.ImageKeyInterface;

class PerspectiveImageRendererHelper
{
	PerspectiveImage image;
	protected vtkImageData rawImage;
    private vtkImageData displayedImage;
    private PerspectiveImageFootprint footprint;
    private PerspectiveImageFrustum frustum;

    private vtkPolyDataNormals normalsFilter;

    private boolean normalsGenerated = false;
    private vtkImageCanvasSource2D maskSource;

    private boolean simulateLighting = false;

    // Always use accessors to use this field -- even within this class!
    private IntensityRange[] displayedRange = null;

    private int[] currentMask = new int[4];

    private double minIncidence = Double.MAX_VALUE;
    private double maxIncidence = -Double.MAX_VALUE;
    private double minEmission = Double.MAX_VALUE;
    private double maxEmission = -Double.MAX_VALUE;
    private double minPhase = Double.MAX_VALUE;
    private double maxPhase = -Double.MAX_VALUE;

    double minHorizontalPixelScale = Double.MAX_VALUE;
    double maxHorizontalPixelScale = -Double.MAX_VALUE;
    double meanHorizontalPixelScale = 0.0;
    double minVerticalPixelScale = Double.MAX_VALUE;
    double maxVerticalPixelScale = -Double.MAX_VALUE;
    double meanVerticalPixelScale = 0.0;

    private List<vtkProp> actors = new ArrayList<vtkProp>();


	public PerspectiveImageRendererHelper(PerspectiveImage image)
	{
		this.image = image;
		this.frustum = new PerspectiveImageFrustum(image);
		this.footprint = new PerspectiveImageFootprint(image);
//        int nslices = image.getImageDepth();
	}

	public void initialize()
	{
		footprint.initialize();
		frustum.initialize();
	}

	void resetFrustaAndFootprint(int slice)
    {
    	frustum.frusta[slice] = null;
        footprint.footprintGenerated[slice] = false;
    }

	public boolean[] getFootprintGenerated()
	{
		return footprint.getFootprintGenerated();
	}

	public void setFootprintGenerated(boolean footprintGenerated)
	{
		footprint.setFootprintGenerated(footprintGenerated);
	}

	public void setFootprintGenerated(boolean footprintGenerated, int slice)
	{
		footprint.setFootprintGenerated(footprintGenerated, slice);
	}

	public static boolean isGenerateFootprint()
	{
		return PerspectiveImageFootprint.isGenerateFootprint();
	}

	public double getOpacity()
    {
        return footprint.getOpacity();
    }

    public void setCurrentMask(int[] masking)
    {
        int topMask = masking[0];
        int rightMask = masking[1];
        int bottomMask = masking[2];
        int leftMask = masking[3];
        // Initialize the mask to black which masks out the image
        maskSource.SetDrawColor(0.0, 0.0, 0.0, 0.0);
        maskSource.FillBox(0, image.imageWidth - 1, 0, image.imageHeight - 1);
        // Create a square inside mask which passes through the image.
        maskSource.SetDrawColor(255.0, 255.0, 255.0, 255.0);
        maskSource.FillBox(leftMask, image.imageWidth - 1 - rightMask, bottomMask, image.imageHeight - 1 - topMask);
        maskSource.Update();

        image.firePropertyChange(Properties.MODEL_CHANGED, null, this);
        setDisplayedImageRange(null);

        for (int i = 0; i < masking.length; ++i)
            currentMask[i] = masking[i];
    }

    int[] getCurrentMask()
    {
        return currentMask.clone();
    }

    public boolean isNormalsGenerated()
    {
        return normalsGenerated;
    }

    public void setNormalsGenerated(boolean normalsGenerated)
    {
        this.normalsGenerated = normalsGenerated;
    }

    public void imageAboutToBeRemoved()
    {
        frustum.setShowFrustum(false);
    }

    int getNumberOfComponentsOfOriginalImage()
    {
        return rawImage.GetNumberOfScalarComponents();
    }

    /**
     * Return surface area of footprint (unshifted) of image.
     *
     * @return
     */
    double getSurfaceArea()
    {
        return PolyDataUtil.getSurfaceArea(footprint.getFootprint(image.currentSlice));
    }

    public void setOpacity(double imageOpacity)
    {
        footprint.setOpacity(imageOpacity);
    }

    double getMinFrustumDepth(int slice)
    {
        return frustum.getMinFrustumDepth(slice);
    }

    double getMaxFrustumDepth(int slice)
    {
        return frustum.getMaxFrustumDepth(slice);
    }

    public void setVisible(boolean b)
    {
        footprint.setVisible(b);
    }

    void Delete()
    {
        displayedImage.Delete();
        rawImage.Delete();
        footprint.Delete();
        normalsFilter.Delete();
        maskSource.Delete();
    }

    void computeCellNormals()
    {
        if (normalsGenerated == false)
        {
        	vtkPolyData[] fprint = footprint.getFootprint();
        	int currentSlice = image.currentSlice;
            normalsFilter.SetInputData(fprint[currentSlice]);
            normalsFilter.SetComputeCellNormals(1);
            normalsFilter.SetComputePointNormals(0);
            // normalsFilter.AutoOrientNormalsOn();
            // normalsFilter.ConsistencyOn();
            normalsFilter.SplittingOff();
            normalsFilter.Update();

            if (fprint != null && fprint[currentSlice] != null)
            {
                vtkPolyData normalsFilterOutput = normalsFilter.GetOutput();
                fprint[currentSlice].DeepCopy(normalsFilterOutput);
                normalsGenerated = true;
            }
        }
    }

    // Computes the incidence, emission, and phase at a point on the footprint with
    // a given normal.
    // (I.e. the normal of the plate which the point is lying on).
    // The output is a 3-vector with the first component equal to the incidence,
    // the second component equal to the emission and the third component equal to
    // the phase.
    double[] computeIlluminationAnglesAtPoint(double[] pt, double[] normal)
    {
    	int currentSlice = image.currentSlice;
    	double[][] spacecraftPositionAdjusted = image.getSpacecraftPositionAdjusted();
        double[] scvec = {
        		spacecraftPositionAdjusted[currentSlice][0] - pt[0],
        		spacecraftPositionAdjusted[currentSlice][1] - pt[1],
        		spacecraftPositionAdjusted[currentSlice][2] - pt[2] };

        double[] sunVectorAdjusted = image.getSunVector();
        double incidence = MathUtil.vsep(normal, sunVectorAdjusted) * 180.0 / Math.PI;
        double emission = MathUtil.vsep(normal, scvec) * 180.0 / Math.PI;
        double phase = MathUtil.vsep(sunVectorAdjusted, scvec) * 180.0 / Math.PI;

        double[] angles = { incidence, emission, phase };

        return angles;
    }

    void computeIlluminationAngles()
    {
    	int currentSlice = image.getCurrentSlice();
    	vtkPolyData currentFootprint = footprint.getFootprint()[currentSlice];
        if (footprint.getFootprintGenerated()[currentSlice] == false)
            footprint.loadFootprint();

        computeCellNormals(); 

        int numberOfCells = currentFootprint.GetNumberOfCells();

        vtkPoints points = currentFootprint.GetPoints();
        vtkCellData footprintCellData = currentFootprint.GetCellData();
        vtkDataArray normals = footprintCellData.GetNormals();

        this.minEmission = Double.MAX_VALUE;
        this.maxEmission = -Double.MAX_VALUE;
        this.minIncidence = Double.MAX_VALUE;
        this.maxIncidence = -Double.MAX_VALUE;
        this.minPhase = Double.MAX_VALUE;
        this.maxPhase = -Double.MAX_VALUE;

        for (int i = 0; i < numberOfCells; ++i)
        {
            vtkCell cell = currentFootprint.GetCell(i);
            double[] pt0 = points.GetPoint(cell.GetPointId(0));
            double[] pt1 = points.GetPoint(cell.GetPointId(1));
            double[] pt2 = points.GetPoint(cell.GetPointId(2));
            double[] centroid = {
                    (pt0[0] + pt1[0] + pt2[0]) / 3.0,
                    (pt0[1] + pt1[1] + pt2[1]) / 3.0,
                    (pt0[2] + pt1[2] + pt2[2]) / 3.0
            };
            double[] normal = normals.GetTuple3(i);

            double[] angles = computeIlluminationAnglesAtPoint(centroid, normal);
            double incidence = angles[0];
            double emission = angles[1];
            double phase = angles[2];

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

    void computePixelScale()
    {
    	double[][] spacecraftPositionAdjusted = image.getSpacecraftPositionAdjusted();
    	int currentSlice = image.currentSlice;
    	vtkPolyData currentFootprint = footprint.getFootprint()[currentSlice];
        if (footprint.getFootprintGenerated()[currentSlice] == false)
            footprint.loadFootprint();

        int numberOfPoints = currentFootprint.GetNumberOfPoints();

        vtkPoints points = currentFootprint.GetPoints();

        minHorizontalPixelScale = Double.MAX_VALUE;
        maxHorizontalPixelScale = -Double.MAX_VALUE;
        meanHorizontalPixelScale = 0.0;
        minVerticalPixelScale = Double.MAX_VALUE;
        maxVerticalPixelScale = -Double.MAX_VALUE;
        meanVerticalPixelScale = 0.0;

        double horizScaleFactor = 2.0 * Math.tan(MathUtil.vsep(image.getFrustum1Adjusted()[currentSlice], image.getFrustum3Adjusted()[currentSlice]) / 2.0) / image.imageHeight;
        double vertScaleFactor = 2.0 * Math.tan(MathUtil.vsep(image.getFrustum1Adjusted()[currentSlice], image.getFrustum2Adjusted()[currentSlice]) / 2.0) / image.imageWidth;

        double[] vec = new double[3];

        for (int i = 0; i < numberOfPoints; ++i)
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

        meanHorizontalPixelScale /= (double) numberOfPoints;
        meanVerticalPixelScale /= (double) numberOfPoints;

        points.Delete();
    }

    float[][][] convertvtkImageToArray3D(vtkImageData image)
    {
    	return ImageDataUtil.vtkImageDataToArray3D(rawImage);
    }

//    public void loadFootprint()
//    {
//    	int currentSlice = image.currentSlice;
//    	double[][] spacecraftPositionAdjusted = image.getSpacecraftPositionAdjusted();
//    	double[][] frustum1Adjusted = image.getFrustum1Adjusted();
//    	double[][] frustum2Adjusted = image.getFrustum2Adjusted();
//    	double[][] frustum3Adjusted = image.getFrustum3Adjusted();
//    	double[][] frustum4Adjusted = image.getFrustum4Adjusted();
////    	System.out.println("PerspectiveImageRendererHelper: loadFootprint: spacecraftPos adjusted " + new Vector3D(spacecraftPositionAdjusted[0]));
////    	System.out.println("PerspectiveImageRendererHelper: loadFootprint: frus1 adjusted " + new Vector3D(frustum1Adjusted[0]));
////    	System.out.println("PerspectiveImageRendererHelper: loadFootprint: frus2 adjusted " + new Vector3D(frustum2Adjusted[0]));
////    	System.out.println("PerspectiveImageRendererHelper: loadFootprint: frus3 adjusted " + new Vector3D(frustum3Adjusted[0]));
////    	System.out.println("PerspectiveImageRendererHelper: loadFootprint: frus4 adjusted " + new Vector3D(frustum4Adjusted[0]));
//        vtkPolyData existingFootprint = checkForExistingFootprint();
//        if (existingFootprint != null)
//        {
////        	System.out.println("PerspectiveImage: loadFootprint: existing footprint");
//            footprint[0] = existingFootprint;
//
//            vtkPointData pointData = footprint[currentSlice].GetPointData();
//            pointData.SetTCoords(textureCoords);
////            System.out.println("PerspectiveImage: loadFootprint: setting texture coords " + sw.elapsedMillis());
//            PolyDataUtil.generateTextureCoordinates(getFrustum(), image.getImageWidth(), image.getImageHeight(), footprint[currentSlice]);
////            System.out.println("PerspectiveImage: loadFootprint: set texture coords " + sw.elapsedMillis());
//            pointData.Delete();
//
//            shiftedFootprint[0].DeepCopy(footprint[currentSlice]);
//            PolyDataUtil.shiftPolyDataInNormalDirection(shiftedFootprint[0], image.getOffset());
//            return;
//        }
//
//        if (generateFootprint)
//        {
////        	System.out.println("PerspectiveImage: loadFootprint: generate footprint true");
//            vtkPolyData tmp = null;
//
//            if (!footprintGenerated[currentSlice] || (existingFootprint == null))
//            {
////            	System.out.println("PerspectiveImage: loadFootprint: footprint not generated");
//                if (useDefaultFootprint())
//                {
////                	System.out.println("PerspectiveImage: loadFootprint: using default footprint");
//                    int defaultSlice = image.getDefaultSlice();
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
//                    tmp = image.getSmallBodyModel().computeFrustumIntersection(spacecraftPositionAdjusted[currentSlice], frustum1Adjusted[currentSlice], frustum3Adjusted[currentSlice], frustum4Adjusted[currentSlice], frustum2Adjusted[currentSlice]);
//                    if (tmp == null)
//                        return;
//
//                    // Need to clear out scalar data since if coloring data is being shown,
//                    // then the color might mix-in with the image.
//                    tmp.GetCellData().SetScalars(null);
//                    tmp.GetPointData().SetScalars(null);
//                }
//
//                // vtkPolyDataWriter writer=new vtkPolyDataWriter();
//                // writer.SetInputData(tmp);
//                // writer.SetFileName("/Users/zimmemi1/Desktop/test.vtk");
//                // writer.SetFileTypeToBinary();
//                // writer.Write();
//
//                footprint[currentSlice].DeepCopy(tmp);
//
//                footprintGenerated[currentSlice] = true;
//            }
////            System.out.println("PerspectiveImage: loadFootprint: footprint generated, setting texture coords "+  image.getImageWidth() + " " + image.getImageHeight());
////            System.out.println("PerspectiveImageRendererHelper: loadFootprint: frustum " + getFrustum());
//            vtkPointData pointData = footprint[currentSlice].GetPointData();
//            pointData.SetTCoords(textureCoords);
//            PolyDataUtil.generateTextureCoordinates(getFrustum(), image.getImageWidth(), image.getImageHeight(), footprint[currentSlice]);
//            pointData.Delete();
//        }
//        else
//        {
//        	ImageKeyInterface key = image.getKey();
////        	System.out.println("PerspectiveImage: loadFootprint: fetching from server, generate footprint false");
//            int resolutionLevel = image.getSmallBodyModel().getModelResolution();
//
//            String footprintFilename = null;
//            File file = null;
//
//            if (key.getSource() == ImageSource.SPICE || key.getSource() == ImageSource.CORRECTED_SPICE)
//                footprintFilename = key.getName() + "_FOOTPRINT_RES" + resolutionLevel + "_PDS.VTP";
//            else
//                footprintFilename = key.getName() + "_FOOTPRINT_RES" + resolutionLevel + "_GASKELL.VTP";
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
//        shiftedFootprint[0].DeepCopy(footprint[currentSlice]);
//        PolyDataUtil.shiftPolyDataInNormalDirection(shiftedFootprint[0], image.getOffset());
//
//        String intersectionFileName = image.getPrerenderingFileNameBase() + "_frustumIntersection.vtk";
//        saveToDisk(FileCache.instance().getFile(intersectionFileName).getPath(), footprint[0]);
//
//        setFootprintGenerated(true);
//    }
//
//    private void saveToDisk(String filename, vtkPolyData imagePolyData)
//    {
//        new File(filename).getParentFile().mkdirs();
//        vtkPolyDataWriter writer = new vtkPolyDataWriter();
//        writer.SetInputData(imagePolyData);
//        writer.SetFileName(new File(filename).toString());
//        writer.SetFileTypeToBinary();
//        writer.Write();
//    }
//
//    vtkPolyData generateBoundary()
//    {
//        loadFootprint();
//
//        if (footprint[image.currentSlice].GetNumberOfPoints() == 0)
//            return null;
//
//        vtkFeatureEdges edgeExtracter = new vtkFeatureEdges();
//        edgeExtracter.SetInputData(footprint[image.currentSlice]);
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
//    void setUseDefaultFootprint(boolean useDefaultFootprint)
//    {
//        this.useDefaultFootprint = useDefaultFootprint;
//        for (int i = 0; i < image.getImageDepth(); i++)
//        {
//            footprintGenerated[i] = false;
//        }
//    }
//
//    boolean useDefaultFootprint()
//    {
//        return useDefaultFootprint;
//    }
//
//    void setShowFrustum(boolean b)
//    {
//        showFrustum = b;
//
//        if (showFrustum)
//        {
//            frustumActor.VisibilityOn();
//        }
//        else
//        {
//            frustumActor.VisibilityOff();
//        }
//
//        image.firePropertyChange(Properties.MODEL_CHANGED, null, this);
//    }
//
//    boolean isFrustumShowing()
//    {
//        return showFrustum;
//    }

    void setSimulateLighting(boolean b)
    {
        simulateLighting = b;
    }

    boolean isSimulatingLighingOn()
    {
        return simulateLighting;
    }

    /**
     * Set the displayed image range of the currently selected slice of the image.
     * As a side-effect, this method also MAYBE CREATES the displayed image.
     *
     * @param range the new displayed range of the image. If null is passed,
     */
    void setDisplayedImageRange(IntensityRange range)
    {
//    	System.out.println("PerspectiveImageRendererHelper: setDisplayedImageRange: ");
    	int currentSlice = image.currentSlice;
        if (rawImage != null)
        {
            if (rawImage.GetNumberOfScalarComponents() > 1)
            {
                displayedImage = rawImage;
                return;
            }
        }

        IntensityRange displayedRange = getDisplayedRange(currentSlice);
        if (range == null || displayedRange.min != range.min || displayedRange.max != range.max)
        {
            if (range != null)
            {
                this.displayedRange[currentSlice] = range;
                image.saveImageInfo();
            }

            if (rawImage != null)
            {
                vtkImageData img = getImageWithDisplayedRange(range, false);
                if (displayedImage == null)
                    displayedImage = new vtkImageData();
                displayedImage.DeepCopy(img);
            }
        }

        image.firePropertyChange(Properties.MODEL_CHANGED, null, this);
    }

    vtkImageData getImageWithDisplayedRange(IntensityRange range, boolean offlimb)
    {
    	int currentSlice = image.currentSlice;
        float minValue = image.getMinValue();
        float maxValue = image.getMaxValue();
        float dx = (maxValue - minValue) / 255.0f;

        float min = minValue;
        float max = maxValue;
        if (!offlimb)
        {
            IntensityRange displayedRange = getDisplayedRange(currentSlice);
            min = minValue + displayedRange.min * dx;
            max = minValue + displayedRange.max * dx;
        }
        else
        {
            IntensityRange offLimbDisplayedRange = image.getOfflimbPlaneHelper().getOffLimbDisplayedRange();
            min = minValue + offLimbDisplayedRange.min * dx;
            max = minValue + offLimbDisplayedRange.max * dx;
        }

        // Update the displayed image
        vtkLookupTable lut = new vtkLookupTable();
        lut.SetTableRange(min, max);
        lut.SetValueRange(0.0, 1.0);
        lut.SetHueRange(0.0, 0.0);
        lut.SetSaturationRange(0.0, 0.0);
        // lut.SetNumberOfTableValues(402);
        lut.SetRampToLinear();
        lut.Build();

        // for 3D images, take the current slice
        vtkImageData image2D = rawImage;
        if (image.getImageDepth() > 1)
        {
            vtkImageReslice slicer = new vtkImageReslice();
            slicer.SetInputData(rawImage);
            slicer.SetOutputDimensionality(2);
            slicer.SetInterpolationModeToNearestNeighbor();
            slicer.SetOutputSpacing(1.0, 1.0, 1.0);
            slicer.SetResliceAxesDirectionCosines(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0);

            slicer.SetOutputOrigin(0.0, 0.0, (double) currentSlice);
            slicer.SetResliceAxesOrigin(0.0, 0.0, (double) currentSlice);

            slicer.SetOutputExtent(0, image.imageWidth - 1, 0, image.imageHeight - 1, 0, 0);

            slicer.Update();
            image2D = slicer.GetOutput();
        }

        vtkImageMapToColors mapToColors = new vtkImageMapToColors();
        mapToColors.SetInputData(image2D);
        mapToColors.SetOutputFormatToRGBA();
        mapToColors.SetLookupTable(lut);
        mapToColors.Update();

        vtkImageData mapToColorsOutput = mapToColors.GetOutput();
        vtkImageData maskSourceOutput = maskSource.GetOutput();

        vtkImageMask maskFilter = new vtkImageMask();
        maskFilter.SetImageInputData(mapToColorsOutput);
        maskFilter.SetMaskInputData(maskSourceOutput);
        maskFilter.Update();

        vtkImageData maskFilterOutput = maskFilter.GetOutput();
        mapToColors.Delete();
        lut.Delete();
        mapToColorsOutput.Delete();
        maskSourceOutput.Delete();
        maskFilter.Delete();
        return maskFilterOutput;
    }

    /**
     * This getter lazily initializes the range field as necessary to
     * ensure this returns a valid, non-null range as long as the argument
     * is in range for this image.
     *
     * @param slice the number of the slice whose displayed range to return.
     */
    IntensityRange getDisplayedRange(int slice)
    {
//    	System.out.println("PerspectiveImageRendererHelper: getDisplayedRange: ");
        int nslices = image.getImageDepth();

        Preconditions.checkArgument(slice < nslices);

        if (displayedRange == null)
        {
            displayedRange = new IntensityRange[nslices];
        }
        if (displayedRange[slice] == null)
        {
            displayedRange[slice] = new IntensityRange(0, 255);
        }

        return displayedRange[slice];
    }

	vtkImageData getRawImage()
	{
		return rawImage;
	}

	vtkImageData getDisplayedImage()
	{
		return displayedImage;
	}

    vtkTexture getTexture()
    {
        return footprint.getTexture();
    }

    List<vtkProp> getProps()
    {
//        if (footprintActor == null)
//        {
////        	System.out.println("PerspectiveImageRendererHelper: getProps: loading footprint actor");
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
////        	System.out.println("PerspectiveImageRendererHelper: getProps: loading frustum actor");
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

    	actors.addAll(footprint.getProps());
    	//TODO make color dynamic later?
    	frustum.setColor(Color.green);
    	actors.addAll(frustum.getProps());
        // for offlimb
        actors.addAll(image.getOfflimbPlaneHelper().getProps());

        return actors;
    }

    double getMinIncidence()
    {
        return minIncidence;
    }

    double getMaxIncidence()
    {
        return maxIncidence;
    }

    double getMinEmission()
    {
        return minEmission;
    }

    double getMaxEmission()
    {
        return maxEmission;
    }

    double getMinPhase()
    {
        return minPhase;
    }

    double getMaxPhase()
    {
        return maxPhase;
    }

    IntensityRange getDisplayedRange()
    {
        return getDisplayedRange(image.currentSlice);
    }

//    public vtkPolyData getFootprint(int defaultSlice)
//    {
//        if (footprint[0] != null && footprint[0].GetNumberOfPoints() > 0)
//            return footprint[0];
//        // first check the cache
//        vtkPolyData existingFootprint = checkForExistingFootprint();
//        if (existingFootprint != null)
//        {
//            return existingFootprint;
//        }
//        else
//        {
//            vtkPolyData footprint = image.getSmallBodyModel().computeFrustumIntersection(image.getSpacecraftPositionAdjusted()[defaultSlice], image.getFrustum1Adjusted()[defaultSlice], image.getFrustum3Adjusted()[defaultSlice], image.getFrustum4Adjusted()[defaultSlice], image.getFrustum2Adjusted()[defaultSlice]);
//            return footprint;
//        }
//    }

    vtkPolyData checkForExistingFootprint()
    {
//    	if (getFootprintGenerated()[image.getCurrentSlice()] == false) return null;
        String intersectionFileName = image.getPrerenderingFileNameBase() + "_frustumIntersection.vtk.gz";
        File file = null;
        try
        {
        	file = FileCache.getFileFromServer(intersectionFileName);
        }
        catch (Exception e)
        {
        	file = new File(SafeURLPaths.instance().getString(intersectionFileName));
        	if (file.exists())
            {
            	vtkPolyDataReader reader = new vtkPolyDataReader();
                reader.SetFileName(file.getAbsolutePath());
                reader.Update();
                vtkPolyData footprint = reader.GetOutput();
                return footprint;
            }
        	else
        	{
        		return null;
        	}
        }
        if (file != null)
        {
        	vtkPolyDataReader reader = new vtkPolyDataReader();
            reader.SetFileName(file.getAbsolutePath());
            reader.Update();
            vtkPolyData footprint = reader.GetOutput();
            return footprint;
        }
        return null;
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_RESOLUTION_CHANGED.equals(evt.getPropertyName()))
        {
            footprint.loadFootprint();
            normalsGenerated = false;
            this.minEmission = Double.MAX_VALUE;
            this.maxEmission = -Double.MAX_VALUE;
            this.minIncidence = Double.MAX_VALUE;
            this.maxIncidence = -Double.MAX_VALUE;
            this.minPhase = Double.MAX_VALUE;
            this.maxPhase = -Double.MAX_VALUE;
            this.minHorizontalPixelScale = Double.MAX_VALUE;
            this.maxHorizontalPixelScale = -Double.MAX_VALUE;
            this.minVerticalPixelScale = Double.MAX_VALUE;
            this.maxVerticalPixelScale = -Double.MAX_VALUE;
            this.meanHorizontalPixelScale = 0.0;
            this.meanVerticalPixelScale = 0.0;

            image.firePropertyChange(Properties.MODEL_CHANGED, null, this);
        }
    }


	void setRawImage(vtkImageData rawImage)
	{
		this.rawImage = rawImage;
	}

    void initializeMaskingAfterLoad()
    {
//    	System.out.println("PerspectiveImageRendererHelper: initializeMaskingAfterLoad: ");
    	int[] masking = image.getMaskSizes();
        int topMask = masking[0];
        int rightMask = masking[1];
        int bottomMask = masking[2];
        int leftMask = masking[3];
        for (int i = 0; i < masking.length; ++i)
            currentMask[i] = masking[i];

//        System.out.println("PerspectiveImageRendererHelper: initializeMaskingAfterLoad: masking top " + topMask + " r: " + rightMask + " b: " + bottomMask + " l: " + leftMask);
        maskSource = new vtkImageCanvasSource2D();
        maskSource.SetScalarTypeToUnsignedChar();
        maskSource.SetNumberOfScalarComponents(1);
        // maskSource.SetExtent(0, imageWidth-1, 0, imageHeight-1, 0, imageDepth-1);
        maskSource.SetExtent(0, image.imageWidth - 1, 0, image.imageHeight - 1, 0, 0);
        // Initialize the mask to black which masks out the image
        maskSource.SetDrawColor(0.0, 0.0, 0.0, 0.0);
        maskSource.FillBox(0, image.imageWidth - 1, 0, image.imageHeight - 1);
        // Create a square inside mask which passes through the image.
        maskSource.SetDrawColor(255.0, 255.0, 255.0, 255.0);
        maskSource.FillBox(leftMask, image.imageWidth - 1 - rightMask, bottomMask, image.imageHeight - 1 - topMask);
        maskSource.Update();

        footprint.initializeMaskingAfterLoad();
        normalsFilter = new vtkPolyDataNormals();
    }

    /**
     * Give oppurtunity to subclass to do some processing on the raw image such as
     * resizing, flipping, masking, etc.
     *
     * @param rawImage
     */
    void processRawImage(vtkImageData rawImage)
    {
        ImageKeyInterface key = image.getKey();

        if (key.getFlip().equals("X"))
        {
            ImageDataUtil.flipImageXAxis(rawImage);
        }
        else if (key.getFlip().equals("Y"))
        {
            ImageDataUtil.flipImageYAxis(rawImage);
        }
        if (key.getRotation() != 0.0)
            ImageDataUtil.rotateImage(rawImage, 360.0 - key.getRotation());
    }

    vtkImageData createRawImage(int height, int width, int depth, float[][] array2D, float[][][] array3D)
    {
        return createRawImage(height, width, depth, true, array2D, array3D);
    }

    vtkImageData createRawImage(int height, int width, int depth, boolean transpose, float[][] array2D, float[][][] array3D)
    {
        // Allocate enough room to store min/max value at each layer
        image.maxValue = new float[depth];
        image.minValue = new float[depth];

        IImagingInstrument instrument = image.getKey().getInstrument();

        FillDetector<Float> fillDetector = instrument != null ? instrument.getFillDetector(image) : ImageDataUtil.getDefaultFillDetector();

        return ImageDataUtil.createRawImage(height, width, depth, transpose, array2D, array3D, image.minValue, image.maxValue, fillDetector, null);
    }

	public PerspectiveImageFootprint getFootprint()
	{
		return footprint;
	}

	public PerspectiveImageFrustum getFrustum()
	{
		return frustum;
	}
}