package edu.jhuapl.sbmt.model.lidar;

import java.awt.Color;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * Object that contains the mutable properties associated with a lidar Track.
 * <P>
 * Since this class is NOT to be exposed outside of this package there are no
 * publicly accessible methods and all access is package private.
 *
 * @author lopeznr1
 */
class TrackProp
{
	// Associated properties
	/** Defines if a track should be visible. */
	boolean isVisible;
	/** Defines the color associated with a track. */
	Color color;
	/** Defines the (offset) translation vector associated with a track. */
	Vector3D translation;
	/** Defines the precomputed error associated with a track. */
	double errAmt;

	/**
	 * Standard Constructor
	 */
	TrackProp()
	{
		isVisible = true;
		color = Color.BLUE;
		translation = Vector3D.ZERO;
		errAmt = Double.NaN;
	}

}
