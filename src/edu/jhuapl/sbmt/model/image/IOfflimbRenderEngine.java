package edu.jhuapl.sbmt.model.image;

import vtk.vtkActor;
import vtk.vtkTexture;


public interface IOfflimbRenderEngine
{

    /**
     * No-argument entry point into the off-limb geometry-creation implementation.
     * This will create an offlimbPlaneCalculator and create the actors for the
     * plane and the boundaries.
     */
    void loadOffLimbPlane();

    /**
     * Set visibility of the off-limb footprint
     *
     * Checks if offLimbActor has been instantiated; if not then call loadOffLimbPlane() before showing/hiding actors.
     *
     * @param visible
     */
    public void setOffLimbFootprintVisibility(boolean visible);

    public void setOffLimbBoundaryVisibility(boolean visible);

    public vtkTexture getOffLimbTexture();

    public double getOffLimbPlaneDepth();

    public vtkActor getOffLimbActor();

    public vtkActor getOffLimbBoundaryActor();

    public void setOffLimbFootprintAlpha(double alpha);

    public void setOffLimbPlaneDepth(double footprintDepth);

}
