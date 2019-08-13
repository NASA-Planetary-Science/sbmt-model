package edu.jhuapl.sbmt.model.lidar;

import java.awt.Color;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import edu.jhuapl.sbmt.gui.lidar.color.ColorProvider;
import edu.jhuapl.sbmt.gui.lidar.color.ConstColorProvider;

/**
 * Object that contains the mutable renderable properties associated with a
 * lidar data.
 * <P>
 * Since this class is NOT to be exposed outside of this package there are no
 * publicly accessible methods and all access is package private.
 *
 * @author lopeznr1
 */
class RenderProp
{
	// Associated properties
	/** Defines if the lidar data should be visible. */
	boolean isVisible;
	/** Defines whether the installed ColorProvider is a custom ColorProvider */
	boolean isCustomCP;
	/** Defines the ColorProvider associated with a source points. */
	ColorProvider srcCP;
	/** Defines the ColorProvider associated with a target points. */
	ColorProvider tgtCP;
	/** Defines the (offset) translation vector associated with lidar data. */
	Vector3D translation;
	/** Defines the precomputed error associated with lidar data. */
	double errAmt;

	/**
	 * Standard Constructor
	 */
	RenderProp()
	{
		isVisible = true;
		isCustomCP = false;
		srcCP = new ConstColorProvider(Color.GREEN);
		tgtCP = new ConstColorProvider(Color.BLUE);
		translation = Vector3D.ZERO;
		errAmt = Double.NaN;
	}

}
