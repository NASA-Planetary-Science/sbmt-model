package edu.jhuapl.sbmt.model.lidar;

import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import edu.jhuapl.sbmt.gui.lidar.color.ColorProvider;
import edu.jhuapl.sbmt.gui.lidar.color.GroupColorProvider;
import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.model.lidar.feature.FeatureAttr;
import edu.jhuapl.sbmt.model.lidar.feature.FeatureType;

import glum.item.ItemManager;

/**
 * Interface that defines a collection of methods to manage, handle
 * notification, and customize display of a collection of lidar data objects.
 * <P>
 * Access to various rendering properties of the lidar data is provided. The
 * rendering properties supported are:
 * <UL>
 * <LI>Visibility
 * <LI>Coloring
 * <LI>Translations
 * <LI>Radial offset.
 * </UL>
 *
 * @author lopeznr1
 */
public interface LidarManager<G1> extends ItemManager<G1>
{
	/**
	 * Clears out any custom ColorProvider associated with the specified list of
	 * lidar objects.
	 *
	 * @param aItemL The list of objects of interest.
	 */
	public void clearCustomColorProvider(List<G1> aItemL);

	/**
	 * Returns the ColorProvider used to render the source points associated with
	 * the specified lidar object.
	 */
	public ColorProvider getColorProviderSource(G1 aItem);

	/**
	 * Returns the ColorProvider used to render the target points associated with
	 * the specified lidar object.
	 */
	public ColorProvider getColorProviderTarget(G1 aItem);

	/**
	 * Returns the FeatureAttr corresponding to aFeatureType for the specified
	 * lidar object.
	 */
	public FeatureAttr getFeatureAttrFor(G1 aItem, FeatureType aFeatureType);

	/**
	 * Returns whether the specified object is rendered.
	 */
	public boolean getIsVisible(G1 aItem);

	/**
	 * Returns the LidarPoint at the specified index for the specified lidar
	 * object.
	 * <P>
	 * The returned LidarPoint will be the original point without any position or
	 * radial offset applied.
	 */
	public LidarPoint getLidarPointAt(G1 aItem, int aIdx);

	/**
	 * Returns the (original) target position at the specified index for the
	 * specified lidar object.
	 */
	public Vector3D getTargetPosition(G1 aItem, int aIdx);

	/**
	 * Returns the current radial offset (of all lidar objects).
	 */
	public double getRadialOffset();

	/**
	 * Returns the translation associated with the specified lidar object.
	 */
	public Vector3D getTranslation(G1 aItem);

	/**
	 * Returns true if specified lidar object is associated with a custom
	 * ColorProvider.
	 */
	public boolean hasCustomColorProvider(G1 aItem);

	/**
	 * Sets in the custom ColorProvider for the specified list of lidar objects.
	 *
	 * @param aItemC The list of objects of interest.
	 * @param aSrcColorProvider The ColorProvider that should be used to colorize
	 * source data.
	 * @param aSrcColorProvider The ColorProvider that should be used to colorize
	 * target data.
	 */
	public void installCustomColorProviders(Collection<G1> aItemC, ColorProvider aSrcCP, ColorProvider aTgtCP);

	/**
	 * Sets the GroupColorProviders used to color the lidar data.
	 * <P>
	 * Note that the ColorProviders associated with all lidar data will be
	 * updated unless they have been customized via the method
	 * {@link #setColorProviders}.
	 * <P>
	 * Call {@link #clearCustomColorProvider} to allow the respective lidar data
	 * to be colorized with the GroupColorProviders.
	 */
	public void installGroupColorProviders(GroupColorProvider aSrcGCP, GroupColorProvider aTgtGCP);

	/**
	 * Sets whether the specified list of lidar objects should be rendered.
	 *
	 * @param aItemL The list of objects of interest.
	 * @param aBool True if the objects should be visible
	 */
	public void setIsVisible(List<G1> aItemL, boolean aBool);

	/**
	 * Method that will set the list of lidar objects to visible and set all
	 * other objects to not visible.
	 *
	 * @param aItemL The list of objects of interest. These will be the only ones
	 * that are made visible all others will be hidden.
	 */
	public void setOthersHiddenExcept(List<G1> aItemL);

	/**
	 * Sets in the radial offset (of all lidar objects).
	 */
	public void setRadialOffset(double aRadialOffset);

	/**
	 * Sets whether the source points will be shown (of all lidar objects).
	 */
	public void setShowSourcePoints(boolean aShowSourcePoints);

	/**
	 * Sets the translation associated with the specified lidar objects.
	 *
	 * @param aItemL The list of objects of interest.
	 * @param aVect The vector that defines the translation amount.
	 */
	public void setTranslation(Collection<G1> aItemC, Vector3D aVect);

}
