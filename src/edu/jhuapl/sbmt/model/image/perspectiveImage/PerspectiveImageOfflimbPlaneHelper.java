package edu.jhuapl.sbmt.model.image.perspectiveImage;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import vtk.vtkActor;
import vtk.vtkImageData;
import vtk.vtkPolyData;
import vtk.vtkProp;
import vtk.vtkTexture;

import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.IntensityRange;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.model.image.OffLimbPlaneCalculator;

class PerspectiveImageOfflimbPlaneHelper
{
	PerspectiveImage image;
    /*
     * For off-limb images
     */
    vtkPolyData offLimbPlane = null;
    private vtkActor offLimbActor;
    private vtkTexture offLimbTexture;
    vtkPolyData offLimbBoundary = null;
    private vtkActor offLimbBoundaryActor;
    double offLimbFootprintDepth;
    private boolean offLimbVisibility;
    private boolean offLimbBoundaryVisibility;
    private Color offLimbBoundaryColor = Color.RED; // default
    // Always use accessors to use this field -- even within this class!
    private IntensityRange offLimbDisplayedRange = null;
    private boolean contrastSynced = false; // by default, the contrast of offlimb is not synced with on limb
    OffLimbPlaneCalculator calculator = new OffLimbPlaneCalculator();


	public PerspectiveImageOfflimbPlaneHelper(PerspectiveImage image)
	{
		this.image = image;
	}

	public List<vtkProp> getProps()
    {
		List<vtkProp> footprintActors = new ArrayList<vtkProp>();
		getOffLimbTexture();
        if (offLimbActor == null && offLimbTexture != null)
        {
            loadOffLimbPlane();
            if (footprintActors.contains(offLimbActor))
                footprintActors.remove(offLimbActor);
            footprintActors.add(offLimbActor);
            if (footprintActors.contains(offLimbBoundaryActor))
                footprintActors.remove(offLimbBoundaryActor);
            footprintActors.add(offLimbBoundaryActor);
        }
        return footprintActors;
    }

    /*
     * FOR OFF-LIMB IMAGES
     */

    /**
     * No-argument entry point into the off-limb geometry-creation implementation.
     * This will create an offlimbPlaneCalculator and create the actors for the
     * plane and the boundaries.
     */
    void loadOffLimbPlane()
    {
        double[] spacecraftPosition = new double[3];
        double[] focalPoint = new double[3];
        double[] upVector = new double[3];
        image.getCameraOrientation(spacecraftPosition, focalPoint, upVector);
        this.offLimbFootprintDepth = new Vector3D(spacecraftPosition).getNorm();
        calculator.loadOffLimbPlane(image, offLimbFootprintDepth);
        offLimbActor = calculator.getOffLimbActor();
        offLimbBoundaryActor = calculator.getOffLimbBoundaryActor();
        offLimbTexture = calculator.getOffLimbTexture();
        // set initial visibilities
        if (offLimbActor != null)
        {
            offLimbActor.SetVisibility(offLimbVisibility ? 1 : 0);
            offLimbBoundaryActor.SetVisibility(offLimbBoundaryVisibility ? 1 : 0);
        }
    }

    /**
     * Set the distance of the off-limb plane from the camera position, along its
     * look vector. The associated polydata doesn't need to be regenerated every
     * time this method is called since the body's shadow in frustum coordinates
     * does not change with depth along the look axis. The call to loadOffLimbPlane
     * here does actually re-create the polydata, which should be unnecessary, and
     * needs to be fixed in a future release.
     *
     * @param footprintDepth
     */
    void setOffLimbPlaneDepth(double footprintDepth)
    {
        this.offLimbFootprintDepth = footprintDepth;
        calculator.loadOffLimbPlane(image, offLimbFootprintDepth);
    }

    void setOffLimbFootprintAlpha(double alpha) // between 0-1
    {
        if (offLimbActor == null)
            loadOffLimbPlane();
        offLimbActor.GetProperty().SetOpacity(alpha);
    }

    boolean offLimbFootprintIsVisible()
    {
        return offLimbVisibility;
    }

    /**
     * Set visibility of the off-limb footprint
     *
     * Checks if offLimbActor has been instantiated; if not then call
     * loadOffLimbPlane() before showing/hiding actors.
     *
     * @param visible
     */
    void setOffLimbFootprintVisibility(boolean visible)
    {

        offLimbVisibility = visible;
        offLimbBoundaryVisibility = visible;
        if (offLimbVisibility && offLimbActor == null)
            loadOffLimbPlane();

        if (offLimbActor != null)
        {
            offLimbActor.SetVisibility(visible ? 1 : 0);
            offLimbBoundaryActor.SetVisibility(visible ? 1 : 0);
        }

        image.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    /**
     * Set visibility of the off-limb footprint boundary
     *
     * Checks if offLimbActor has been instantiated; if not then call
     * loadOffLimbPlane() before showing/hiding actors.
     *
     * @param visible
     */
    void setOffLimbBoundaryVisibility(boolean visible)
    {

        offLimbBoundaryVisibility = visible;
        if (offLimbActor == null)
            loadOffLimbPlane();
        offLimbBoundaryActor.SetVisibility(visible ? 1 : 0);

        image.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    vtkTexture getOffLimbTexture()
    {
        if (offLimbTexture == null)
        { // if offlimbtexture is null, initialize it.
            vtkImageData imageData = new vtkImageData();
            imageData.DeepCopy(image.getDisplayedImage());
            offLimbTexture = new vtkTexture();
            offLimbTexture.SetInputData(imageData);
            offLimbTexture.Modified();
        }
        return offLimbTexture;
    }

    void setOffLimbTexture(vtkTexture offLimbTexture)
    {
        this.offLimbTexture = offLimbTexture;
    }

    double getOffLimbPlaneDepth()
    {
        return offLimbFootprintDepth;
    }

    void setContrastSynced(boolean selected)
    {
        this.contrastSynced = selected;
        if (contrastSynced)
        {
            // if we just changed this to true, update the values to match
            offLimbDisplayedRange = image.getDisplayedRange();
            setOfflimbImageRange(offLimbDisplayedRange);
            image.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }

    boolean isContrastSynced()
    {
        return contrastSynced;
    }

    void setOfflimbBoundaryColor(Color color)
    {
        this.offLimbBoundaryColor = color;
        offLimbBoundaryActor.GetProperty().SetColor(color.getRed() / 255., color.getGreen() / 255., color.getBlue() / 255.);
        offLimbBoundaryActor.Modified();
        image.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    Color getOfflimbBoundaryColor()
    {
        return offLimbBoundaryColor;
    }

    IntensityRange getOffLimbDisplayedRange()
    {
        if (offLimbDisplayedRange == null)
        {
            offLimbDisplayedRange = new IntensityRange(0, 255);
        }

        return offLimbDisplayedRange;
    }

    void setOfflimbImageRange(IntensityRange intensityRange)
    {

        IntensityRange displayedRange = getOffLimbDisplayedRange();
        if (intensityRange == null || displayedRange.min != intensityRange.min || displayedRange.max != intensityRange.max)
        {
            if (intensityRange != null)
            {
                offLimbDisplayedRange = intensityRange;
                image.saveImageInfo();
            }

            if (image.getRawImage() != null)
            {
                vtkImageData imageData = image.getImageWithDisplayedRange(intensityRange, true);

                if (offLimbTexture == null && !Configuration.isHeadless())
                    offLimbTexture = new vtkTexture();
                if (offLimbTexture != null)
                {
                    offLimbTexture.SetInputData(imageData);
                    imageData.Delete();
                    offLimbTexture.Modified();
                }
            }

            image.firePropertyChange(Properties.MODEL_CHANGED, null, this);
        }

    }

	public vtkActor getOffLimbActor()
	{
		return offLimbActor;
	}

	public vtkActor getOffLimbBoundaryActor()
	{
		return offLimbBoundaryActor;
	}
}
