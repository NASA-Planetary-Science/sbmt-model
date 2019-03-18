package edu.jhuapl.sbmt.model.lidar;

import vtk.vtkActor;

/**
 * Interface that provides the structure used to render and process model
 * relevant components via the VTK framework.
 */
public interface VtkPainter
{
	/**
	 * Returns the vtkActor associated with this VtkPainter.
	 */
	public vtkActor getActor();

	/**
	 * Notification method that the VTK state is stale and needs to be refreshed.
	 */
	public void markStale();

	/**
	 * Method to update internal vtk state.
	 */
	public void updateVtkVars();

}
