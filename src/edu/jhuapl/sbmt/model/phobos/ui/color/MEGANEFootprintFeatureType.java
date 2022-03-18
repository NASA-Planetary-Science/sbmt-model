package edu.jhuapl.sbmt.model.phobos.ui.color;

import com.google.common.collect.ImmutableSet;

import edu.jhuapl.saavtk.feature.FeatureType;

/**
 * @author steelrj1
 *
 */
public class MEGANEFootprintFeatureType
{
	// Constants
	public static final FeatureType Signal = new FeatureType("Footprint Signal", null, 1.0);

	/** Provides access to all of the available MEGANE Footprint {@link FeatureType}s. */
	public static final ImmutableSet<FeatureType> FullSet = ImmutableSet.of(Signal);
}
