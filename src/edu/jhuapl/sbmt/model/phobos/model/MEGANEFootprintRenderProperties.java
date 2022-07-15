package edu.jhuapl.sbmt.model.phobos.model;

import java.awt.Color;

import edu.jhuapl.saavtk.color.provider.ColorProvider;
import edu.jhuapl.saavtk.color.provider.ConstColorProvider;



/**
 * Object that contains the mutable renderable properties associated with a
 * state history.
 * <P>
 * Since this class is NOT to be exposed outside of this package there are no
 * publicly accessible methods and all access is package private.
 *
 * Based off a file for Lidar originally made by lopeznr1
 *
 * @author steelrj1
 */
class MEGANEFootprintRenderProperties
{
	// Associated properties
	/** Defines if the lidar data should be visible. */
	boolean isVisible;
	/** Defines whether the installed ColorProvider is a custom ColorProvider */
	boolean isCustomCP;
	/** Defines the Simple ColorProvider associated with a state history */
	ColorProvider simpleCP;
	/** Defines the Feature based ColorProvider associated with a state history */
	ColorProvider featureCP;
	/** Defines the active ColorProvider associated with a state history */
	ColorProvider activeCP;
	ColorProvider lastActive;
	ColorProvider customCP;

	/**
	 * Standard Constructor
	 */
	MEGANEFootprintRenderProperties()
	{
		isVisible = true;
		isCustomCP = false;
		activeCP = new ConstColorProvider(new Color(0.0f, 1.0f, 1.0f));
		simpleCP = new ConstColorProvider(new Color(0.0f, 1.0f, 1.0f));
	}
}
