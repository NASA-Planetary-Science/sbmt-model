package edu.jhuapl.sbmt.model.image.perspectiveImage.renderer;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import vtk.vtkActor;
import vtk.vtkFloatArray;
import vtk.vtkImageCanvasSource2D;
import vtk.vtkImageData;
import vtk.vtkPointData;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkPolyDataNormals;
import vtk.vtkProp;
import vtk.vtkProperty;
import vtk.vtkTexture;

import edu.jhuapl.saavtk.util.FillDetector;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.image.interfaces.IImagingInstrument;
import edu.jhuapl.sbmt.image.interfaces.ImageKeyInterface;
import edu.jhuapl.sbmt.image.model.PerspectiveImage;

public class PerspectiveImageRendererHelper2
{
	PerspectiveImage image;
	protected vtkImageData rawImage;
    private vtkImageData displayedImage;
    public vtkPolyData[] footprint;
//    public boolean[] footprintGenerated;
    vtkPolyData[] shiftedFootprint;
    private vtkActor footprintActor;
    private List<vtkProp> footprintActors = new ArrayList<vtkProp>();
    private List<vtkProp> footprintOfflimbActors = new ArrayList<vtkProp>();
//    vtkPolyData frustumPolyData;
    private vtkActor frustumActor;

    private vtkFloatArray textureCoords;
    private vtkPolyDataNormals normalsFilter;
    protected vtkTexture imageTexture;
    private vtkImageCanvasSource2D maskSource;
    public Frustum[] frusta;

    private boolean showFrustum = false;
    private boolean simulateLighting = false;

//    public double[] maxFrustumDepth;
//    public double[] minFrustumDepth;
    private boolean useDefaultFootprint = true;


    private double imageOpacity = 1.0;

    //helpers to the helper
    PerspectiveImageFootprintCacheOperator footprintCacheOperator;
    PerspectiveImageIlluminationOperator illuminationOperator;
    PerspectiveImageIntensityOperator intensityOperator;
    PerspectiveImageMaskingOperator maskingOperator;
    PerspectiveImagePixelScaleOperator pixelScaleOperator;
    PerspectiveImageFootprintBoundaryOperator boundaryOperator;
    PerspectiveImageFootprintRendererOperator footprintRendererOperator;
    PerspectiveImageFrustumRendererOperator frustumRendererOperator;

    // If true, then the footprint is generated by intersecting a frustum with the
    // asteroid.
    // This setting is used when generating the files on the server.
    // If false, then the footprint is downloaded from the server. This setting is
    // used by the GUI.
    private static boolean generateFootprint = true;

    private List<SmallBodyModel> smallBodyModels;

	public PerspectiveImageRendererHelper2(PerspectiveImage image, List<SmallBodyModel> smallBodyModels)
	{
		this.image = image;
		this.smallBodyModels = smallBodyModels;
		footprintCacheOperator = new PerspectiveImageFootprintCacheOperator();
		illuminationOperator = new PerspectiveImageIlluminationOperator(image);
		intensityOperator = new PerspectiveImageIntensityOperator(image);
		maskingOperator = new PerspectiveImageMaskingOperator(image);
		pixelScaleOperator = new PerspectiveImagePixelScaleOperator();
		boundaryOperator = new PerspectiveImageFootprintBoundaryOperator();
		footprintRendererOperator = new PerspectiveImageFootprintRendererOperator(image, smallBodyModels, footprintCacheOperator);
		frustumRendererOperator = new PerspectiveImageFrustumRendererOperator(image);

		 int numParts = smallBodyModels.size();
	    shiftedFootprint = new vtkPolyData[numParts];


        frusta = new Frustum[1];

        footprint = new vtkPolyData[numParts];
        footprint[0] = new vtkPolyData();
	}

	public List<vtkProp> getProps()
    {
//		System.out.println("PerspectiveImageRendererHelper2: getProps: footprintActor null " + (footprintActor == null) + " frustum actor " + (frustumActor == null));
        if (footprintActor == null)
        {
            loadFootprint();
            imageTexture = new vtkTexture();
            imageTexture.InterpolateOn();
            imageTexture.RepeatOff();
            imageTexture.EdgeClampOn();
            imageTexture.SetInputData(getDisplayedImage());
            for (int i=0; i<smallBodyModels.size(); i++)
            {
	            vtkPolyDataMapper footprintMapper = new vtkPolyDataMapper();
	            footprintMapper.SetInputData(shiftedFootprint[i]);
	            footprintMapper.Update();
	            vtkActor footprintActor = new vtkActor();
	            footprintActor.SetMapper(footprintMapper);
	            footprintActor.SetTexture(imageTexture);
	            vtkProperty footprintProperty = footprintActor.GetProperty();
	            footprintProperty.LightingOff();
	            footprintActors.add(footprintActor);
            }
        }
        if (frustumActor == null)
        {

            frustumRendererOperator.calculateFrustum();
            frustumActor = frustumRendererOperator.getFrustumActor();


            vtkProperty frustumProperty = frustumActor.GetProperty();
            frustumProperty.SetColor(0.0, 1.0, 0.0);
            frustumProperty.SetLineWidth(2.0);
            frustumActor.VisibilityOff();

            footprintActors.add(frustumActor);
        }

        // for offlimb
        footprintOfflimbActors.addAll(image.getOfflimbPlaneHelper().getProps());
        footprintActors.addAll(image.getOfflimbPlaneHelper().getProps());
        return footprintActors;
    }

	public void resetFrustaAndFootprint(int slice)
    {
		if (frustumRendererOperator.frusta == null || footprintRendererOperator.footprintGenerated == null) return;
    	frustumRendererOperator.frusta[slice] = null;
        footprintRendererOperator.footprintGenerated[slice] = false;
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_RESOLUTION_CHANGED.equals(evt.getPropertyName()))
        {
        	footprintRendererOperator.loadFootprint();

            illuminationOperator.propertyChange(evt);
            pixelScaleOperator.propertyChange(evt);

            image.firePropertyChange(Properties.MODEL_CHANGED, null, this);
        }
        else if (Properties.MODEL_CHANGED.equals(evt.getPropertyName()))
        {
        	footprintRendererOperator.loadFootprint();
        	shiftedFootprint = footprintRendererOperator.getShiftedFootprint();
        	footprint = footprintRendererOperator.getFootprint();
        }
    }

	//**********************
	// Raw image
	//**********************
    /**
     * Give oppurtunity to subclass to do some processing on the raw image such as
     * resizing, flipping, masking, etc.
     *
     * @param rawImage
     */
    public void processRawImage(vtkImageData rawImage)
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

    public vtkImageData createRawImage(int height, int width, int depth, float[][] array2D, float[][][] array3D)
    {
        return createRawImage(height, width, depth, true, array2D, array3D);
    }

    public vtkImageData createRawImage(int height, int width, int depth, boolean transpose, float[][] array2D, float[][][] array3D)
    {
        // Allocate enough room to store min/max value at each layer
        image.maxValue = new float[depth];
        image.minValue = new float[depth];

        IImagingInstrument instrument = image.getKey().getInstrument();

        FillDetector<Float> fillDetector = instrument != null ? instrument.getFillDetector(image) : ImageDataUtil.getDefaultFillDetector();

        return ImageDataUtil.createRawImage(height, width, depth, transpose, array2D, array3D, image.minValue, image.maxValue, fillDetector, null);
    }

    //**********************
  	// Footprint
  	//**********************
    public void loadFootprint()
    {
        vtkPolyData[] existingFootprints = footprintCacheOperator.checkForExistingFootprint(image.getPrerenderingFileNameBase());
        int modelRes = image.getSmallBodyModel().getModelResolution();
        if ((modelRes >=3) && (existingFootprints != null))
        {
        	int i=0;
        	for (vtkPolyData existingFootprint : existingFootprints)
        	{
	            footprint[0] = existingFootprint;

	            vtkPointData pointData = footprint[i].GetPointData();
	            pointData.SetTCoords(textureCoords);
	            PolyDataUtil.generateTextureCoordinates(getFrustum(), image.getImageWidth(), image.getImageHeight(), footprint[i]);
	            pointData.Delete();

	            shiftedFootprint[0].DeepCopy(footprint[i++]);
	            PolyDataUtil.shiftPolyDataInNormalDirection(shiftedFootprint[0], image.getOffset());
        	}
        }
        else
        {
        	footprintRendererOperator.loadFootprint();
        	shiftedFootprint = footprintRendererOperator.getShiftedFootprint();
        	footprint = footprintRendererOperator.getFootprint();
        }
        setCurrentMask(image.getCurrentMask());
    }

    //**********************
  	// Frustum
  	//**********************
	public Frustum getFrustum()
	{

		Frustum	frusta = new Frustum(image.getSpacecraftPositionAdjusted()[0],
					image.getFrustum1Adjusted()[0], image.getFrustum3Adjusted()[0],
					image.getFrustum4Adjusted()[0], image.getFrustum2Adjusted()[0]);
		return frusta;
	}

//    public Frustum getFrustum(int slice)
//    {
//    	int sliceToUse = slice;
//        if (useDefaultFootprint()) sliceToUse = image.getDefaultSlice();
//
//        if (frusta[sliceToUse] == null)
//            frusta[sliceToUse] = new Frustum(image.getSpacecraftPositionAdjusted()[sliceToUse],
//            							image.getFrustum1Adjusted()[sliceToUse],
//            							image.getFrustum3Adjusted()[sliceToUse],
//            							image.getFrustum4Adjusted()[sliceToUse],
//            							image.getFrustum2Adjusted()[sliceToUse]);
//        return frusta[sliceToUse];
//    }
//
//    public Frustum getFrustum()
//    {
//        return getFrustum(image.getCurrentSlice());
//    }

	//**********************
	// Opacity
	//**********************

    public void setOpacity(double imageOpacity)
    {
        this.imageOpacity = imageOpacity;
        vtkProperty smallBodyProperty = footprintActor.GetProperty();
        smallBodyProperty.SetOpacity(imageOpacity);
        image.firePropertyChange(Properties.MODEL_CHANGED, null, this);
    }

	//**********************
	// Masking
	//**********************
    public void initializeMaskingAfterLoad()
    {
    	maskSource = maskingOperator.initializeMaskingAfterLoad();

        for (int k = 0; k < image.getImageDepth(); k++)
        {
            footprint[k] = new vtkPolyData();
        }

        shiftedFootprint[0] = new vtkPolyData();
        textureCoords = new vtkFloatArray();
        normalsFilter = new vtkPolyDataNormals();
        displayedImage = intensityOperator.getDisplayedImage();
    }

    public void setCurrentMask(int[] masking)
    {
    	maskingOperator.setCurrentMask(masking);
    	intensityOperator.setDisplayedImageRange(null, maskSource);
    	displayedImage = intensityOperator.getDisplayedImage();
    	image.firePropertyChange(Properties.MODEL_CHANGED, null, image);
    }

	//**********************
	// Getters/Setters
	//**********************

    public boolean[] getFootprintGenerated()
    {
    	return footprintRendererOperator.footprintGenerated;
//        return footprintGenerated;
    }



    public static boolean isGenerateFootprint()
    {
        return generateFootprint;
    }

    public void imageAboutToBeRemoved()
    {
        setShowFrustum(false);
    }

    public int getNumberOfComponentsOfOriginalImage()
    {
        return rawImage.GetNumberOfScalarComponents();
    }

    /**
     * Return surface area of footprint (unshifted) of image.
     *
     * @return
     */
    public double getSurfaceArea()
    {
        return PolyDataUtil.getSurfaceArea(footprint[image.getCurrentSlice()]);
    }

    public double getOpacity()
    {
        return imageOpacity;
    }



    public void setVisible(boolean b)
    {
    	for (vtkProp footprintActor : footprintActors)
    	{
    		if (!footprintOfflimbActors.contains(footprintActor) && footprintActor != frustumActor)
    			footprintActor.SetVisibility(b ? 1 : 0);
    	}
    }

    /**
     * The shifted footprint is the original footprint shifted slightly in the
     * normal direction so that it will be rendered correctly and not obscured by
     * the asteroid.
     *
     * @return
     */
    public vtkPolyData getShiftedFootprint()
    {
        return shiftedFootprint[0];
    }

    /**
     * The original footprint whose cells exactly overlap the original asteroid. If
     * rendered as is, it would interfere with the asteroid. Note: this is made
     * public in this class for the benefit of backplane generators, which use it.
     *
     * @return
     */
    public vtkPolyData getUnshiftedFootprint()
    {
        return footprint[image.getCurrentSlice()];
    }

    public void Delete()
    {
        displayedImage.Delete();
        rawImage.Delete();

        for (int i = 0; i < footprint.length; i++)
        {
            // Footprints can be null if no frustum intersection is found
            if (footprint[i] != null)
            {
                footprint[i].Delete();
            }
        }

        for (int i = 0; i < shiftedFootprint.length; i++)
        {
            if (shiftedFootprint[i] != null)
            {
                shiftedFootprint[i].Delete();
            }
        }

        textureCoords.Delete();
        normalsFilter.Delete();
        maskSource.Delete();
    }

    public void setUseDefaultFootprint(boolean useDefaultFootprint)
    {
        this.useDefaultFootprint = useDefaultFootprint;
        for (int i = 0; i < image.getImageDepth(); i++)
        {
            footprintRendererOperator.footprintGenerated[i] = false;
        }
    }

    public boolean useDefaultFootprint()
    {
        return useDefaultFootprint;
    }

    public void setShowFrustum(boolean b)
    {
    	if (frustumActor != null)
    	{
	        showFrustum = b;

	        if (showFrustum)
	        {
	            frustumActor.VisibilityOn();
	        }
	        else
	        {
	            frustumActor.VisibilityOff();
	        }
    	}

        image.firePropertyChange(Properties.MODEL_CHANGED, null, frustumActor);
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

	public vtkImageData getRawImage()
	{
		return rawImage;
	}

	public vtkImageData getDisplayedImage()
	{
		return displayedImage;
	}

    public vtkTexture getTexture()
    {
        return imageTexture;
    }

	public vtkPolyData[] getFootprint()
	{
		return footprintRendererOperator.getFootprint();
	}

	public void setFootprint(vtkPolyData[] footprint)
	{
		this.footprint = footprint;
	}

	public void setRawImage(vtkImageData rawImage)
	{
		this.rawImage = rawImage;
	}

    static void setGenerateFootprint(boolean b)
    {
        generateFootprint = b;
    }



	/**
	 * @return the footprintCacheOperator
	 */
	public PerspectiveImageFootprintCacheOperator getFootprintCacheOperator()
	{
		return footprintCacheOperator;
	}

	/**
	 * @return the illuminationOperator
	 */
	public PerspectiveImageIlluminationOperator getIlluminationOperator()
	{
		return illuminationOperator;
	}

	/**
	 * @return the intensityOperator
	 */
	public PerspectiveImageIntensityOperator getIntensityOperator()
	{
		return intensityOperator;
	}

	/**
	 * @return the maskingOperator
	 */
	public PerspectiveImageMaskingOperator getMaskingOperator()
	{
		return maskingOperator;
	}

	/**
	 * @return the pixelScaleOperator
	 */
	public PerspectiveImagePixelScaleOperator getPixelScaleOperator()
	{
		return pixelScaleOperator;
	}

	/**
	 * @return the boundaryOperator
	 */
	public PerspectiveImageFootprintBoundaryOperator getBoundaryOperator()
	{
		return boundaryOperator;
	}

	/**
	 * @return the footprintRendererOperator
	 */
	public PerspectiveImageFootprintRendererOperator getFootprintRendererOperator()
	{
		return footprintRendererOperator;
	}

	/**
	 * @return the frustumRendererOperator
	 */
	public PerspectiveImageFrustumRendererOperator getFrustumRendererOperator()
	{
		return frustumRendererOperator;
	}
}