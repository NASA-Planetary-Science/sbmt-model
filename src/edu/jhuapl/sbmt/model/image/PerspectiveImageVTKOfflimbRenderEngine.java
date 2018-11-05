package edu.jhuapl.sbmt.model.image;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import vtk.vtkActor;
import vtk.vtkPolyData;
import vtk.vtkTexture;

public class PerspectiveImageVTKOfflimbRenderEngine implements IOfflimbRenderEngine
{
    PerspectiveImage image;
    /*
     * For off-limb images
     */
    vtkPolyData offLimbPlane=null;
    private vtkActor offLimbActor;
    private vtkTexture offLimbTexture;
    vtkPolyData offLimbBoundary=null;
    private vtkActor offLimbBoundaryActor;
    double offLimbFootprintDepth;
    private boolean offLimbVisibility;
    private boolean offLimbBoundaryVisibility;
    OffLimbPlaneCalculator calculator = new OffLimbPlaneCalculator();

    public PerspectiveImageVTKOfflimbRenderEngine(PerspectiveImage image)
    {
        this.image = image;
    }


    /*
     * FOR OFF-LIMB IMAGES
     */

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.image.IOfflimbRenderEngine#loadOffLimbPlane()
     */
    @Override
    public void loadOffLimbPlane()
    {
        double[] spacecraftPosition=new double[3];
        double[] focalPoint=new double[3];
        double[] upVector=new double[3];
        image.getCameraOrientation(spacecraftPosition, focalPoint, upVector);
        this.offLimbFootprintDepth=new Vector3D(spacecraftPosition).getNorm();
        calculator.loadOffLimbPlane(image, offLimbFootprintDepth);
        offLimbActor=calculator.getOffLimbActor();
        offLimbBoundaryActor=calculator.getOffLimbBoundaryActor();

        // set initial visibilities
        if (offLimbActor != null)
        {
            offLimbActor.SetVisibility(offLimbVisibility?1:0);
            offLimbBoundaryActor.SetVisibility(offLimbBoundaryVisibility?1:0);
        }
    }


    /**
     * Set the distance of the off-limb plane from the camera position, along its look vector.
     * The associated polydata doesn't need to be regenerated every time this method is called since the body's shadow in frustum coordinates does not change with depth along the look axis.
     * The call to loadOffLimbPlane here does actually re-create the polydata, which should be unnecessary, and needs to be fixed in a future release.
     * @param footprintDepth
     */
    public void setOffLimbPlaneDepth(double footprintDepth)
    {
        this.offLimbFootprintDepth=footprintDepth;
        calculator.loadOffLimbPlane(image, offLimbFootprintDepth);
    }

    public void setOffLimbFootprintAlpha(double alpha)  // between 0-1
    {
        if (offLimbActor==null)
            loadOffLimbPlane();
        offLimbActor.GetProperty().SetOpacity(alpha);
    }


    public boolean offLimbFootprintIsVisible()
    {
        return offLimbVisibility;
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.image.IOfflimbRenderEngine#setOffLimbFootprintVisibility(boolean)
     */
    @Override
    public void setOffLimbFootprintVisibility(boolean visible)
    {

        offLimbVisibility=visible;
        offLimbBoundaryVisibility = visible;
        if (offLimbActor==null)
            loadOffLimbPlane();

        if (offLimbActor != null)
        {
            offLimbActor.SetVisibility(visible?1:0);
            offLimbBoundaryActor.SetVisibility(visible?1:0);
        }

        image.firePropertyChange();
    }

    /**
     * Set visibility of the off-limb footprint boundary
     *
     * Checks if offLimbActor has been instantiated; if not then call loadOffLimbPlane() before showing/hiding actors.
     *
     * @param visible
     */
    public void setOffLimbBoundaryVisibility(boolean visible)
    {

        offLimbBoundaryVisibility=visible;
        if (offLimbActor==null)
            loadOffLimbPlane();
        offLimbBoundaryActor.SetVisibility(visible?1:0);

        image.firePropertyChange();
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.image.IOfflimbRenderEngine#getOffLimbTexture()
     */
    @Override
    public vtkTexture getOffLimbTexture()
    {
        return offLimbTexture;
    }

    public void setOffLimbTexture(vtkTexture offLimbTexture)
    {
        this.offLimbTexture = offLimbTexture;
    }
    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.image.IOfflimbRenderEngine#getOffLimbPlaneDepth()
     */
    @Override
    public double getOffLimbPlaneDepth()
    {
        return offLimbFootprintDepth;
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
