package edu.jhuapl.sbmt.model.lidar.vtk;

import java.util.List;

import vtk.vtkProp;

import edu.jhuapl.saavtk.vtk.VtkResource;
import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.model.lidar.feature.FeatureAttr;
import edu.jhuapl.sbmt.model.lidar.feature.FeatureType;

/**
 * Interface that defines methods used to render lidar data via the VTK
 * framework.
 *
 * @author lopeznr1
 */
public interface VtkLidarPainter<G1> extends VtkResource
{
	/**
	 * Returns a short description string that should be used for display
	 * information.
	 *
	 * @param aCellId
	 * @param aTitle
	 */
	public String getDisplayInfoStr(int aCellId, String aTitle);

	/**
	 * Returns the FeatureAttr associated with the specified FeatureType
	 */
	public FeatureAttr getFeatureAttrFor(FeatureType aFeatureType);

	/**
	 * Returns the lidar data object associated with the specified cell.
	 */
	public G1 getLidarItemForCell(int aCellId);

	/**
	 * Returns the {@link LidarPoint} associated with the specified cell.
	 */
	public LidarPoint getLidarPointForCell(int aCellId);

	/**
	 * Returns the list of VtkProps used to render this painter.
	 */
	public List<vtkProp> getProps();

	/**
	 * Sets in whether selected lidar items should be highlighted.
	 */
	public void setHighlightSelection(boolean aBool);

	// TODO: Add comments, make more intutitive
	public void setPercentageShown(double aPercentBeg, double aPercentEnd);

	/**
	 * Sets in the point size used to render the individual lidar data points.
	 */
	public void setPointSize(double aPointSize);

	/**
	 * Configures the visible state of the lidar source points.
	 */
	public void setShowSourcePoints(boolean aBool);

}
