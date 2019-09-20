package edu.jhuapl.sbmt.model.lidar;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import vtk.vtkProp;

import edu.jhuapl.saavtk.model.SaavtkItemManager;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.gui.lidar.color.ColorProvider;
import edu.jhuapl.sbmt.gui.lidar.color.ConstColorProvider;
import edu.jhuapl.sbmt.gui.lidar.color.ConstGroupColorProvider;
import edu.jhuapl.sbmt.gui.lidar.color.GroupColorProvider;
import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.model.lidar.feature.FeatureAttr;
import edu.jhuapl.sbmt.model.lidar.feature.FeatureType;
import edu.jhuapl.sbmt.model.lidar.vtk.VtkLidarPainter;
import edu.jhuapl.sbmt.model.lidar.vtk.VtkLidarPointProvider;

import glum.item.ItemEventType;

/**
 * Class that provides management logic for a collection of lidar FileSpecs.
 * <P>
 * The following features are supported:
 * <UL>
 * <LI>Event handling
 * <LI>Management of collection of FileSpecs.
 * <LI>Support for LidarFileSpec selection
 * <LI>Configuration of associated rendering properties.
 * <LI>Support to apply a radial offset to all Tracks
 * </UL>
 * <P>
 * Currently (VTK) rendering of FileSpecs is supported, however that capability
 * should eventually be removed and placed in a separate class/module.
 *
 * @author lopeznr1
 */
public class LidarFileSpecManager extends SaavtkItemManager<LidarFileSpec> implements LidarManager<LidarFileSpec>
{
	// Ref vars
	private final BodyViewConfig refBodyViewConfig;

	// State vars
	private Map<LidarFileSpec, RenderProp> propM;
	private GroupColorProvider souceGCP;
	private GroupColorProvider targeGCP;
	private double begPercent;
	private double endPercent;
	private double radialOffset;
	private double pointSize;
	private boolean showSourcePoints;

	// VTK vars
	private Map<LidarFileSpec, VtkLidarPointProvider> vAuxM;
	private Map<LidarFileSpec, VtkLidarPainter<LidarFileSpec>> vPainterM;
	private Map<vtkProp, VtkLidarPainter<LidarFileSpec>> vActorToPainterM;

	/**
	 * Standard Constructor
	 */
	public LidarFileSpecManager(SmallBodyModel aSmallBodyModel)
	{
		// TODO: Just pass the needed args
		refBodyViewConfig = (SmallBodyViewConfig) aSmallBodyModel.getSmallBodyConfig();

		propM = new HashMap<>();
		souceGCP = new ConstGroupColorProvider(new ConstColorProvider(Color.GREEN));
		targeGCP = new ConstGroupColorProvider(new ConstColorProvider(Color.BLUE));
		begPercent = 0.0;
		endPercent = 1.0;
		radialOffset = 0.0;
		pointSize = 2.0;
		showSourcePoints = true;

		vAuxM = new HashMap<>();
		vPainterM = new HashMap<>();
		vActorToPainterM = new HashMap<>();
	}

	/**
	 * Returns the number of points associated with the specified LidarFileSpec.
	 * <P>
	 * Returns 0 if the file has not been loaded.
	 */
	public Integer getNumberOfPoints(LidarFileSpec aFileSpec)
	{
		VtkLidarPointProvider tmpLPP = vAuxM.get(aFileSpec);
		if (tmpLPP == null)
			return 0;

		return tmpLPP.getNumberOfPoints();
	}

	/**
	 * Returns whether the specified LidarFileSpec has been loaded.
	 */
	public boolean isLoaded(LidarFileSpec aFileSpec)
	{
		VtkLidarPainter<?> tmpPainter = vPainterM.get(aFileSpec);
		if (tmpPainter == null)
			return false;

		return true;
	}

	/**
	 * Sets in the baseline point size for all of the lidar points.
	 *
	 * @param aSize
	 */
	public void setPointSize(double aPointSize)
	{
		pointSize = aPointSize;

		for (VtkLidarPainter<?> aPainter : vPainterM.values())
			aPainter.setPointSize(pointSize);

		pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
	}

	@Override
	public void clearCustomColorProvider(List<LidarFileSpec> aItemL)
	{
		Set<LidarFileSpec> tmpSet = new HashSet<>(aItemL);

		int tmpIdx = -1;
		int numItems = getNumItems();
		for (LidarFileSpec aItem : getAllItems())
		{
			tmpIdx++;

			// Skip to next if not in aItemL
			if (tmpSet.contains(aItem) == false)
				continue;

			// Skip to next if no RenderProp
			RenderProp tmpProp = propM.get(aItem);
			if (tmpProp == null)
				continue;

			// Skip to next if not custom ColorProvider
			if (tmpProp.isCustomCP == false)
				continue;

			tmpProp.isCustomCP = false;
			tmpProp.srcCP = souceGCP.getColorProviderFor(aItem, tmpIdx, numItems);
			tmpProp.tgtCP = targeGCP.getColorProviderFor(aItem, tmpIdx, numItems);
		}

		notifyListeners(this, ItemEventType.ItemsMutated);
		updateVtkVars(aItemL);
	}

	@Override
	public ColorProvider getColorProviderSource(LidarFileSpec aItem)
	{
		RenderProp tmpProp = propM.get(aItem);
		if (tmpProp == null)
			return null;

		return tmpProp.srcCP;
	}

	@Override
	public ColorProvider getColorProviderTarget(LidarFileSpec aItem)
	{
		RenderProp tmpProp = propM.get(aItem);
		if (tmpProp == null)
			return null;

		return tmpProp.tgtCP;
	}

	@Override
	public FeatureAttr getFeatureAttrFor(LidarFileSpec aItem, FeatureType aFeatureType)
	{
		VtkLidarPainter<?> tmpPainter = vPainterM.get(aItem);
		if (tmpPainter == null)
			return null;

		return tmpPainter.getFeatureAttrFor(aFeatureType);
	}

	@Override
	public boolean getIsVisible(LidarFileSpec aItem)
	{
		RenderProp tmpProp = propM.get(aItem);
		if (tmpProp == null)
			return false;

		return tmpProp.isVisible;
	}

	@Override
	public LidarPoint getLidarPointAt(LidarFileSpec aItem, int aIdx)
	{
		// TODO: Unsupported at this time
		throw new UnsupportedOperationException();
	}

	@Override
	public Vector3D getTargetPosition(LidarFileSpec aItem, int aIdx)
	{
		return new Vector3D(vAuxM.get(aItem).getTargetPosition(aIdx));
	}

	@Override
	public double getRadialOffset()
	{
		return radialOffset;
	}

	@Override
	public Vector3D getTranslation(LidarFileSpec aItem)
	{
		return Vector3D.ZERO;
	}

	@Override
	public boolean hasCustomColorProvider(LidarFileSpec aItem)
	{
		RenderProp tmpProp = propM.get(aItem);
		if (tmpProp == null)
			return false;

		return tmpProp.isCustomCP;
	}

	@Override
	public void installCustomColorProviders(Collection<LidarFileSpec> aItemC, ColorProvider aSrcCP, ColorProvider aTgtCP)
	{
		for (LidarFileSpec aItem : aItemC)
		{
			// Skip to next if no RenderProp
			RenderProp tmpProp = propM.get(aItem);
			if (tmpProp == null)
				continue;

			tmpProp.isCustomCP = true;
			tmpProp.srcCP = aSrcCP;
			tmpProp.tgtCP = aTgtCP;
		}

		notifyListeners(this, ItemEventType.ItemsMutated);
		updateVtkVars(aItemC);
	}

	@Override
	public void installGroupColorProviders(GroupColorProvider aSrcGCP, GroupColorProvider aTgtGCP)
	{
		souceGCP = aSrcGCP;
		targeGCP = aTgtGCP;

		int tmpIdx = -1;
		int numItems = getNumItems();
		for (LidarFileSpec aItem : getAllItems())
		{
			tmpIdx++;

			// Skip to next if no RenderProp
			RenderProp tmpProp = propM.get(aItem);
			if (tmpProp == null)
				continue;

			// Skip to next if custom
			if (tmpProp.isCustomCP == true)
				continue;

			tmpProp.srcCP = souceGCP.getColorProviderFor(aItem, tmpIdx, numItems);
			tmpProp.tgtCP = targeGCP.getColorProviderFor(aItem, tmpIdx, numItems);
		}

		notifyListeners(this, ItemEventType.ItemsMutated);
		updateVtkVars(getAllItems());
	}

	@Override
	public void setIsVisible(List<LidarFileSpec> aItemL, boolean aBool)
	{
		for (LidarFileSpec aItem : aItemL)
		{
			RenderProp tmpProp = propM.get(aItem);
			if (tmpProp == null)
				continue;

			tmpProp.isVisible = aBool;

			if (aBool == true)
				loadVtkPainter(aItem);
		}

		notifyListeners(this, ItemEventType.ItemsMutated);
		updateVtkVars(aItemL);
	}

	@Override
	public void setOthersHiddenExcept(List<LidarFileSpec> aItemL)
	{
		Set<LidarFileSpec> tmpSet = new HashSet<>(aItemL);

		// Update the visibility flag on each Track
		for (LidarFileSpec aItem : getAllItems())
		{
			RenderProp tmpProp = propM.get(aItem);
			if (tmpProp == null)
				continue;

			boolean isVisible = tmpSet.contains(aItem);
			tmpProp.isVisible = isVisible;

			if (isVisible == true)
				loadVtkPainter(aItem);
		}

		notifyListeners(this, ItemEventType.ItemsMutated);
		updateVtkVars(getAllItems());
	}

	@Override
	public void setRadialOffset(double aRadialOffset)
	{
		// Update the radialOffset
		if (radialOffset == aRadialOffset)
			return;
		radialOffset = aRadialOffset;

		// Invalidate the cache vars
		for (RenderProp aProp : propM.values())
			aProp.errAmt = Double.NaN;

		// Send out the appropriate notifications
		notifyListeners(this, ItemEventType.ItemsMutated);
		updateVtkVars(getAllItems());
	}

	@Override
	public void setTranslation(Collection<LidarFileSpec> aItemC, Vector3D aVect)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAllItems(List<LidarFileSpec> aItemL)
	{
		// Clear relevant state vars
		propM = new HashMap<>();
		radialOffset = 0.0;

		// Setup the initial props for all the items
		int tmpIdx = 0;
		int numItems = aItemL.size();
		for (LidarFileSpec aItem : aItemL)
		{
			ColorProvider tmpSrcCP = souceGCP.getColorProviderFor(aItem, tmpIdx, numItems);
			ColorProvider tmpTgtCP = targeGCP.getColorProviderFor(aItem, tmpIdx, numItems);

			RenderProp tmpProp = new RenderProp();
			tmpProp.isVisible = false;
			tmpProp.srcCP = tmpSrcCP;
			tmpProp.tgtCP = tmpTgtCP;
			tmpIdx++;

			propM.put(aItem, tmpProp);
		}

		// Delegate
		super.setAllItems(aItemL);

		updateVtkVars(aItemL);
	}

	@Override
	public void setSelectedItems(List<LidarFileSpec> aItemL)
	{
		super.setSelectedItems(aItemL);

		// Selected items will be rendered with a different point size.
		// Force the painters to "update" their point size
		setPointSize(pointSize);
	}

	// TODO: Add javadoc
	public void setPercentageShown(double aBegPercent, double aEndPercent)
	{
		begPercent = aBegPercent;
		endPercent = aEndPercent;

		for (VtkLidarPainter<?> aPainter : vPainterM.values())
			aPainter.setPercentageShown(aBegPercent, aEndPercent);

		updateVtkVars(getAllItems());
	}

	// TODO: Add javadoc
	public void setShowSourcePoints(boolean aShowSourcePoints)
	{
		showSourcePoints = aShowSourcePoints;

		for (VtkLidarPainter<?> aPainter : vPainterM.values())
			aPainter.setShowSourcePoints(aShowSourcePoints);

		updateVtkVars(getAllItems());
	}

	@Override
	public String getClickStatusBarText(vtkProp aProp, int aCellId, double[] aPickPosition)
	{
		// Bail if there is no painter associated with the actor (aProp)
		VtkLidarPainter<LidarFileSpec> tmpPainter = vActorToPainterM.get(aProp);
		if (tmpPainter == null)
			return "";

		// Custom title
		LidarFileSpec tmpItem = tmpPainter.getLidarItemForCell(aCellId);
		String tmpPath = tmpItem.getPath();
		if (tmpPath.toLowerCase().endsWith(".gz"))
			tmpPath = tmpPath.substring(0, tmpPath.length() - 3);
		File tmpFile = new File(tmpPath);

		return tmpPainter.getDisplayInfoStr(aCellId, tmpFile.getName());
	}

	@Override
	public List<vtkProp> getProps()
	{
		List<vtkProp> retL = new ArrayList<>();

		for (LidarFileSpec aItem : getAllItems())
		{
			// Skip to next if the item is not rendered
			RenderProp tmpProp = propM.get(aItem);
			if (tmpProp == null || tmpProp.isVisible == false)
				continue;

			// Skip to next if no corresponding painter
			VtkLidarPainter<?> tmpPainter = vPainterM.get(aItem);
			if (tmpPainter == null)
				continue;

			retL.addAll(tmpPainter.getProps());
		}

		return retL;
	}

	/**
	 * Notification method that the lidar data associated with aFileSpec has been
	 * loaded. The provided VtkLidarDataPainter will contain the loaded state.
	 */
	protected void markLidarLoadComplete(LidarFileSpec aFileSpec, VtkLidarPointProvider aLidarPointProvider,
			VtkLidarPainter<LidarFileSpec> aPainter)
	{
		vAuxM.put(aFileSpec, aLidarPointProvider);

		vPainterM.put(aFileSpec, aPainter);
		for (vtkProp prop : aPainter.getProps())
			vActorToPainterM.put(prop, aPainter);

		aPainter.setShowSourcePoints(showSourcePoints);
		aPainter.setPercentageShown(begPercent, endPercent);

		notifyListeners(this, ItemEventType.ItemsMutated);
		pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
	}

	/**
	 * Helper method to load the lidar data into a VtkLidarDataPainter.
	 * <P>
	 * The actual loading of the lidar data may happen asynchronously.
	 */
	private void loadVtkPainter(LidarFileSpec aFileSpec)
	{
		// Bail if the corresponding VTK data has already been created
		VtkLidarPainter<?> tmpData = vPainterM.get(aFileSpec);
		if (tmpData != null)
			return;

		try
		{
			LidarFileSpecLoadUtil.initLidarData(this, aFileSpec, refBodyViewConfig);
		}
		catch (IOException aExp)
		{
			aExp.printStackTrace();
		}
	}

	/**
	 * Helper method that will update all relevant VTK vars.
	 * <P>
	 * A notification will be sent out to PropertyChange listeners of the
	 * {@link Properties#MODEL_CHANGED} event.
	 */
	private void updateVtkVars(Collection<LidarFileSpec> aUpdateC)
	{
		for (LidarFileSpec aItem : aUpdateC)
		{
			// Skip to next if no installed painter
			VtkLidarPainter<?> tmpPainter = vPainterM.get(aItem);
			if (tmpPainter == null)
				continue;

			tmpPainter.vtkUpdateState();
		}

		for (VtkLidarPainter<?> aPainter : vPainterM.values())
			aPainter.vtkUpdateState();

		// Notify our PropertyChangeListeners
		pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
	}

}
