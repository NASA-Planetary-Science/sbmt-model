package edu.jhuapl.sbmt.model.lidar;

import java.awt.Color;
import java.awt.event.InputEvent;
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

import edu.jhuapl.saavtk.gui.render.SceneChangeNotifier;
import edu.jhuapl.saavtk.gui.render.VtkPropProvider;
import edu.jhuapl.saavtk.pick.HookUtil;
import edu.jhuapl.saavtk.pick.PickListener;
import edu.jhuapl.saavtk.pick.PickMode;
import edu.jhuapl.saavtk.pick.PickTarget;
import edu.jhuapl.saavtk.util.SaavtkLODActor;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.gui.lidar.color.ColorProvider;
import edu.jhuapl.sbmt.gui.lidar.color.ConstColorProvider;
import edu.jhuapl.sbmt.gui.lidar.color.ConstGroupColorProvider;
import edu.jhuapl.sbmt.gui.lidar.color.GroupColorProvider;
import edu.jhuapl.sbmt.lidar.BasicLidarPoint;
import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.model.lidar.feature.FeatureAttr;
import edu.jhuapl.sbmt.model.lidar.feature.FeatureType;
import edu.jhuapl.sbmt.model.lidar.vtk.VtkLidarPainter;
import edu.jhuapl.sbmt.model.lidar.vtk.VtkLidarPointProvider;
import edu.jhuapl.sbmt.model.lidar.vtk.VtkPointPainter;

import glum.item.BaseItemManager;
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
 * <LI>Support to apply a radial offset to all items.
 * <LI>Support to specify the point size to rendered items.
 * </UL>
 * <P>
 * Currently (VTK) rendering of FileSpecs is supported, however that capability
 * should eventually be moved and placed in a separate class/module.
 *
 * @author lopeznr1
 */
public class LidarFileSpecManager extends BaseItemManager<LidarFileSpec>
		implements LidarManager<LidarFileSpec>, PickListener, VtkPropProvider
{
	// Reference vars
	private final SceneChangeNotifier refSceneChangeNotifier;
	private final BodyViewConfig refBodyViewConfig;

	// State vars
	private Map<LidarFileSpec, RenderProp> propM;
	private GroupColorProvider sourceGCP;
	private GroupColorProvider targetGCP;
	private double begPercent;
	private double endPercent;
	private double radialOffset;
	private double pointSize;
	private boolean showSourcePoints;

	// VTK vars
	private Map<LidarFileSpec, VtkLidarPointProvider> vAuxM;
	private Map<LidarFileSpec, VtkLidarPainter<LidarFileSpec>> vPainterM;
	private VtkPointPainter<LidarFileSpec> vPointPainter;

	/**
	 * Standard Constructor
	 */
	public LidarFileSpecManager(SceneChangeNotifier aSceneChangeNotifier, SmallBodyViewConfig aBodyViewConfig)
	{
		// TODO: Just pass the needed args
		refSceneChangeNotifier = aSceneChangeNotifier;
		refBodyViewConfig = aBodyViewConfig;

		propM = new HashMap<>();
		sourceGCP = new ConstGroupColorProvider(new ConstColorProvider(Color.GREEN));
		targetGCP = new ConstGroupColorProvider(new ConstColorProvider(Color.BLUE));
		begPercent = 0.0;
		endPercent = 1.0;
		radialOffset = 0.0;
		pointSize = 2.0;
		showSourcePoints = true;

		vAuxM = new HashMap<>();
		vPainterM = new HashMap<>();
		vPointPainter = new VtkPointPainter<>(this);
	}

	/**
	 * Returns the number of points associated with the specified LidarFileSpec.
	 * <P>
	 * Returns 0 if the file has not been loaded.
	 */
	public Integer getNumberOfPoints(LidarFileSpec aItem)
	{
		int numPoints = aItem.getNumPoints();
		if (numPoints > 0)
			return numPoints;

		VtkLidarPointProvider tmpLPP = vAuxM.get(aItem);
		if (tmpLPP == null)
			return 0;

		return tmpLPP.getNumberOfPoints();
	}

	/**
	 * Returns whether the specified LidarFileSpec has been loaded.
	 */
	public boolean isLoaded(LidarFileSpec aItem)
	{
		VtkLidarPainter<?> tmpPainter = vPainterM.get(aItem);
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

		notifyVtkStateChange();
	}

	@Override
	public void clearCustomColorProvider(List<LidarFileSpec> aItemL)
	{
		Set<LidarFileSpec> tmpItemS = new HashSet<>(aItemL);

		int tmpIdx = -1;
		int numItems = getNumItems();
		for (LidarFileSpec aItem : getAllItems())
		{
			tmpIdx++;

			// Skip to next if not in aItemL
			if (tmpItemS.contains(aItem) == false)
				continue;

			// Skip to next if no RenderProp
			RenderProp tmpProp = propM.get(aItem);
			if (tmpProp == null)
				continue;

			// Skip to next if not custom ColorProvider
			if (tmpProp.isCustomCP == false)
				continue;

			tmpProp.isCustomCP = false;
			tmpProp.srcCP = sourceGCP.getColorProviderFor(aItem, tmpIdx, numItems);
			tmpProp.tgtCP = targetGCP.getColorProviderFor(aItem, tmpIdx, numItems);
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
		VtkLidarPointProvider tmpLPP = vAuxM.get(aItem);

		// TODO: Note, time, intensity, range will be NaN
		return new BasicLidarPoint(tmpLPP.getTargetPosition(aIdx), tmpLPP.getSourcePosition(aIdx), Double.NaN, Double.NaN,
				Double.NaN);
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
		sourceGCP = aSrcGCP;
		targetGCP = aTgtGCP;

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

			tmpProp.srcCP = sourceGCP.getColorProviderFor(aItem, tmpIdx, numItems);
			tmpProp.tgtCP = targetGCP.getColorProviderFor(aItem, tmpIdx, numItems);
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
		Set<LidarFileSpec> tmpItemS = new HashSet<>(aItemL);

		// Update the visibility flag on each item
		for (LidarFileSpec aItem : getAllItems())
		{
			RenderProp tmpProp = propM.get(aItem);
			if (tmpProp == null)
				continue;

			boolean isVisible = tmpItemS.contains(aItem);
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
	public void setAllItems(Collection<LidarFileSpec> aItemC)
	{
		// Clear relevant state vars
		propM = new HashMap<>();
		radialOffset = 0.0;

		// Setup the initial props for all the items
		int tmpIdx = 0;
		int numItems = aItemC.size();
		for (LidarFileSpec aItem : aItemC)
		{
			ColorProvider tmpSrcCP = sourceGCP.getColorProviderFor(aItem, tmpIdx, numItems);
			ColorProvider tmpTgtCP = targetGCP.getColorProviderFor(aItem, tmpIdx, numItems);

			RenderProp tmpProp = new RenderProp();
			tmpProp.isVisible = false;
			tmpProp.srcCP = tmpSrcCP;
			tmpProp.tgtCP = tmpTgtCP;
			tmpIdx++;

			propM.put(aItem, tmpProp);
		}

		// Delegate
		super.setAllItems(aItemC);

		updateVtkVars(aItemC);
	}

	@Override
	public void setSelectedItems(Collection<LidarFileSpec> aItemC)
	{
		super.setSelectedItems(aItemC);

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

		retL.add(vPointPainter.getActor());

		return retL;
	}

	@Override
	public void handlePickAction(InputEvent aEvent, PickMode aMode, PickTarget aPrimaryTarg, PickTarget aSurfaceTarg)
	{
		// Respond only to primary actions
		if (aMode != PickMode.ActivePri)
			return;

		// Retrieve the selected item (and corresponding point)
		LidarFileSpec tmpItem = null;
		LidarPoint tmpPoint = null;
		vtkProp tmpActor = aPrimaryTarg.getActor();
		if (tmpActor == vPointPainter.getActor())
		{
			tmpPoint = vPointPainter.getPoint();
			tmpItem = vPointPainter.getItem();
		}
		else
		{
			// Bail if no associated painter
			VtkLidarPainter<LidarFileSpec> tmpPainter = getPainterFor(aPrimaryTarg);
			if (tmpPainter == null)
				return;

			// Determine the item / point that was selected
			int tmpCellId = aPrimaryTarg.getCellId();
			tmpItem = tmpPainter.getLidarItemForCell(tmpCellId);
			tmpPoint = tmpPainter.getLidarPointForCell(tmpCellId);

			// Update the VtkPointPainter to reflect the selected point
			vPointPainter.setData(tmpItem, tmpPoint);
		}

		// Update the selection
		HookUtil.updateSelection(this, aEvent, tmpItem);

		Object source = aEvent.getSource();
		notifyListeners(source, ItemEventType.ItemsSelected);

		Set<LidarFileSpec> tmpItemS = getSelectedItems();
		updateVtkVars(tmpItemS);
	}

	@Override
	public void setShowSourcePoints(boolean aShowSourcePoints)
	{
		showSourcePoints = aShowSourcePoints;

		for (VtkLidarPainter<?> aPainter : vPainterM.values())
			aPainter.setShowSourcePoints(aShowSourcePoints);

		updateVtkVars(getAllItems());
	}

	/**
	 * Notification method that the lidar data associated with the FileSpec has
	 * been loaded. The provided VtkLidarDataPainter will contain the loaded
	 * state.
	 */
	protected void markLidarLoadComplete(LidarFileSpec aItem, VtkLidarPointProvider aLidarPointProvider,
			VtkLidarPainter<LidarFileSpec> aPainter)
	{
		vAuxM.put(aItem, aLidarPointProvider);

		vPainterM.put(aItem, aPainter);

		aPainter.setShowSourcePoints(showSourcePoints);
		aPainter.setPercentageShown(begPercent, endPercent);

		notifyListeners(this, ItemEventType.ItemsMutated);
		notifyVtkStateChange();
	}

	/**
	 * Helper method that returns the {@link VtkLidarPainter} associated with the
	 * specified {@link vtkProp}. Returns null if the {@link vtkProp} did not
	 * originate from this manager.
	 */
	@SuppressWarnings("unchecked")
	private VtkLidarPainter<LidarFileSpec> getPainterFor(PickTarget aPickTarg)
	{
		// Bail if tmpProp is not the right type
		vtkProp tmpProp = aPickTarg.getActor();
		if (tmpProp instanceof SaavtkLODActor == false)
			return null;

		// Retrieve the painter and return it if we are the associated manager
		VtkLidarPainter<?> tmpPainter = ((SaavtkLODActor) tmpProp).getAssocModel(VtkLidarPainter.class);
		if (tmpPainter != null && tmpPainter.getManager() == this)
			return (VtkLidarPainter<LidarFileSpec>) tmpPainter;

		return null;
	}

	/**
	 * Helper method to load the lidar data into a VtkLidarDataPainter.
	 * <P>
	 * The actual loading of the lidar data may happen asynchronously.
	 */
	private void loadVtkPainter(LidarFileSpec aItem)
	{
		// Bail if the corresponding VTK data has already been created
		VtkLidarPainter<?> tmpData = vPainterM.get(aItem);
		if (tmpData != null)
			return;

		try
		{
			LidarFileSpecLoadUtil.initLidarData(this, aItem, refBodyViewConfig);
		}
		catch (IOException aExp)
		{
			aExp.printStackTrace();
		}
	}

	/**
	 * Helper method that notifies the system that our internal VTK state has
	 * been changed.
	 */
	private void notifyVtkStateChange()
	{
		refSceneChangeNotifier.notifySceneChange();
	}

	/**
	 * Helper method that will update all relevant VTK vars.
	 */
	private void updateVtkVars(Collection<LidarFileSpec> aUpdateC)
	{
		vPointPainter.vtkUpdateState();
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

		notifyVtkStateChange();
	}

}
