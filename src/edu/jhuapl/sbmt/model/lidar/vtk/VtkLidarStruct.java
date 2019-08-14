package edu.jhuapl.sbmt.model.lidar.vtk;

import vtk.vtkCellArray;
import vtk.vtkPoints;

import edu.jhuapl.sbmt.model.lidar.feature.FeatureAttr;

/**
 * Intermediate object used to hold VTK state associated with lidar data.
 * <P>
 * While this class is immutable - the field members are not! This class is
 * intended to be used as intermediate staging step while instantiating lidar
 * data.
 * <P>
 * The object that creates this intermediate struct is responsible for
 * management of the life cycle of the underlying objects.
 *
 * @author lopeznr1
 */
public class VtkLidarStruct
{
	public final FeatureAttr timeFA;
	public final FeatureAttr rangeFA;
	public final FeatureAttr intensityFA;

	public final vtkPoints vSrcP;
	public final vtkCellArray vSrcCA;
	public final vtkPoints vTgtP;
	public final vtkCellArray vTgtCA;

	/**
	 * Standard Constructor
	 */
	public VtkLidarStruct(FeatureAttr aTimeFA, FeatureAttr aRangeFA, FeatureAttr aIntensityFA, vtkPoints aSrcP,
			vtkCellArray aSrcCA, vtkPoints aTgtP, vtkCellArray aTgtCA)
	{
		timeFA = aTimeFA;
		rangeFA = aRangeFA;
		intensityFA = aIntensityFA;

		vSrcP = aSrcP;
		vSrcCA = aSrcCA;
		vTgtP = aTgtP;
		vTgtCA = aTgtCA;
	}

}
