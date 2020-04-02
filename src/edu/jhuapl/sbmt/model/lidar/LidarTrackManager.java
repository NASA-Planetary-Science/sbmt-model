package edu.jhuapl.sbmt.model.lidar;

import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.collect.ImmutableList;

import vtk.vtkProp;

import edu.jhuapl.saavtk.gui.render.SceneChangeNotifier;
import edu.jhuapl.saavtk.gui.render.VtkPropProvider;
import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.pick.HookUtil;
import edu.jhuapl.saavtk.pick.PickListener;
import edu.jhuapl.saavtk.pick.PickMode;
import edu.jhuapl.saavtk.pick.PickTarget;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.saavtk.util.SaavtkLODActor;
import edu.jhuapl.sbmt.gui.lidar.color.ColorProvider;
import edu.jhuapl.sbmt.gui.lidar.color.ColorWheelGroupColorProvider;
import edu.jhuapl.sbmt.gui.lidar.color.GroupColorProvider;
import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.model.lidar.feature.FeatureAttr;
import edu.jhuapl.sbmt.model.lidar.feature.FeatureType;
import edu.jhuapl.sbmt.model.lidar.vtk.VtkLidarPainter;
import edu.jhuapl.sbmt.model.lidar.vtk.VtkLidarStruct;
import edu.jhuapl.sbmt.model.lidar.vtk.VtkLidarUniPainter;
import edu.jhuapl.sbmt.model.lidar.vtk.VtkPointPainter;
import edu.jhuapl.sbmt.model.lidar.vtk.VtkUtil;

import glum.item.BaseItemManager;
import glum.item.ItemEventType;

/**
 * Class that provides management logic for a collection of lidar Tracks.
 * <P>
 * The following features are supported:
 * <UL>
 * <LI>Event handling
 * <LI>Management to collection of LidarTracks
 * <LI>Support for LidarTrack selection
 * <LI>Configuration of associated rendering properties
 * <LI>Track offset translation
 * <LI>Track error calculation
 * <LI>Support to apply a radial offset to all items.
 * <LI>Support to specify the point size to rendered items.
 * </UL>
 * <P>
 * Currently (VTK) rendering of Tracks is supported, however that capability
 * should eventually be moved and placed in a separate class/module.
 *
 * @author lopeznr1
 */
public class LidarTrackManager extends BaseItemManager<LidarTrack>
		implements LidarManager<LidarTrack>, PickListener, VtkPropProvider
{
	// Reference vars
	private final SceneChangeNotifier refSceneChangeNotifier;
	protected final PolyhedralModel refSmallBody;

	// State vars
	private Map<LidarTrack, RenderProp> propM;
	private GroupColorProvider sourceGCP;
	private GroupColorProvider targetGCP;
	private double radialOffset;
	private double pointSize;
	private boolean showSourcePoints;

	// VTK vars
	private Map<LidarTrack, VtkLidarPainter<LidarTrack>> vPainterM;
	private VtkPointPainter<LidarTrack> vPointPainter;

	/**
	 * Standard Constructor
	 *
	 * @param aSmallBody
	 */
	public LidarTrackManager(SceneChangeNotifier aSceneChangeNotifier, PolyhedralModel aSmallBody)
	{
		refSceneChangeNotifier = aSceneChangeNotifier;
		refSmallBody = aSmallBody;

		propM = new HashMap<>();
		sourceGCP = ColorWheelGroupColorProvider.Instance;
		targetGCP = ColorWheelGroupColorProvider.Instance;
		radialOffset = 0.0;
		pointSize = 2.0;
		showSourcePoints = false;

		vPainterM = new HashMap<>();
		vPointPainter = new VtkPointPainter<LidarTrack>(this);

		setPointSize(2);
	}

	/**
	 * Returns the LidarPoint associated with the specified {@link PickTarget}.
	 * <P>
	 * Returns null if there is no LidarPoint corresponding to the associated
	 * vtkActor / cellId.
	 */
	public LidarPoint getLidarPointFrom(PickTarget aPickTarget)
	{
		// Return the LidarPoint associated with the vPointPainter
		if (aPickTarget.getActor() == vPointPainter.getActor())
			return vPointPainter.getPoint();

		// Bail if aActor has no associated painter
		VtkLidarPainter<LidarTrack> tmpPainter = getPainterFor(aPickTarget);
		if (tmpPainter == null)
			return null;

		// Delegate
		return tmpPainter.getLidarPointForCell(aPickTarget.getCellId());
	}

	/**
	 * Returns the cumulative error for the specified Track.
	 * <P>
	 * The cumulative error is defined as the summation of all of the error for
	 * each LidarPoint associated with the Track.
	 *
	 * @param aItem
	 * @return
	 */
	public double getTrackError(LidarTrack aItem)
	{
		// Utilize the cached value
		RenderProp tmpProp = propM.get(aItem);
		double tmpErr = tmpProp.errAmt;
		if (Double.isNaN(tmpErr) == false)
			return tmpErr;

		// Calculate the error
		tmpErr = 0.0;
		Vector3D tmpVect = getTranslation(aItem);
		for (LidarPoint aLP : aItem.getPointList())
		{
			Vector3D targetV = aLP.getTargetPosition();

			double[] ptLidarArr = LidarGeoUtil.transformTarget(tmpVect, radialOffset, targetV).toArray();
			double[] ptClosest = refSmallBody.findClosestPoint(ptLidarArr);
			tmpErr += MathUtil.distance2Between(ptLidarArr, ptClosest);
		}

		// Update the cache and return the error
		tmpProp.errAmt = tmpErr;
		return tmpErr;
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
		vPointPainter.setPointSize(pointSize * 3.5);

		// Send out the appropriate notifications
		notifyListeners(this, ItemEventType.ItemsMutated);

		notifyVtkStateChange();
	}

	/**
	 * Sets the selected LidarPoint
	 *
	 * @param aItem The item associated with the LidarPoint
	 * @param aPoint The LidarPoint of interest
	 */
	public void setSelectedPoint(LidarTrack aItem, LidarPoint aPoint)
	{
		vPointPainter.setData(aItem, aPoint);
		notifyListeners(this, ItemEventType.ItemsSelected);

		List<LidarTrack> tmpItemL = ImmutableList.of();
		if (aItem != null)
			tmpItemL = ImmutableList.of(aItem);
		updateVtkVars(tmpItemL);
	}

	@Override
	public void clearCustomColorProvider(List<LidarTrack> aItemL)
	{
		Set<LidarTrack> tmpItemS = new HashSet<>(aItemL);

		int tmpIdx = -1;
		int numItems = getNumItems();
		for (LidarTrack aItem : getAllItems())
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
	public ColorProvider getColorProviderSource(LidarTrack aItem)
	{
		RenderProp tmpProp = propM.get(aItem);
		if (tmpProp == null)
			return null;

		return tmpProp.srcCP;
	}

	@Override
	public ColorProvider getColorProviderTarget(LidarTrack aItem)
	{
		RenderProp tmpProp = propM.get(aItem);
		if (tmpProp == null)
			return null;

		return tmpProp.tgtCP;
	}

	@Override
	public FeatureAttr getFeatureAttrFor(LidarTrack aItem, FeatureType aFeatureType)
	{
		VtkLidarPainter<?> tmpPainter = vPainterM.get(aItem);
		if (tmpPainter == null)
			return null;

		return tmpPainter.getFeatureAttrFor(aFeatureType);
	}

	@Override
	public boolean getIsVisible(LidarTrack aItem)
	{
		RenderProp tmpProp = propM.get(aItem);
		if (tmpProp == null)
			return false;

		return tmpProp.isVisible;
	}

	@Override
	public LidarPoint getLidarPointAt(LidarTrack aItem, int aIdx)
	{
		return aItem.getPointList().get(aIdx);
	}

	@Override
	public Vector3D getTargetPosition(LidarTrack aItem, int aIdx)
	{
		return getLidarPointAt(aItem, aIdx).getTargetPosition();
	}

	@Override
	public double getRadialOffset()
	{
		return radialOffset;
	}

	@Override
	public Vector3D getTranslation(LidarTrack aItem)
	{
		RenderProp tmpProp = propM.get(aItem);
		if (tmpProp == null)
			return null;

		return tmpProp.translation;
	}

	@Override
	public boolean hasCustomColorProvider(LidarTrack aItem)
	{
		RenderProp tmpProp = propM.get(aItem);
		if (tmpProp == null)
			return false;

		return tmpProp.isCustomCP;
	}

	@Override
	public void installCustomColorProviders(Collection<LidarTrack> aItemC, ColorProvider aSrcCP, ColorProvider aTgtCP)
	{
		for (LidarTrack aItem : aItemC)
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
		for (LidarTrack aItem : getAllItems())
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
	public void setIsVisible(List<LidarTrack> aItemL, boolean aBool)
	{
		for (LidarTrack aItem : aItemL)
		{
			RenderProp tmpProp = propM.get(aItem);
			if (tmpProp == null)
				continue;

			tmpProp.isVisible = aBool;
		}

		notifyListeners(this, ItemEventType.ItemsMutated);
		updateVtkVars(aItemL);
	}

	@Override
	public void setOthersHiddenExcept(List<LidarTrack> aItemL)
	{
		Set<LidarTrack> tmpItemS = new HashSet<>(aItemL);

		// Update the visibility flag on each item
		for (LidarTrack aItem : getAllItems())
		{
			RenderProp tmpProp = propM.get(aItem);
			if (tmpProp == null)
				continue;

			boolean isVisible = tmpItemS.contains(aItem);
			tmpProp.isVisible = isVisible;
		}

		notifyListeners(this, ItemEventType.ItemsMutated);
		updateVtkVars(aItemL);
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
	public void setShowSourcePoints(boolean aShowSourcePoints)
	{
		showSourcePoints = aShowSourcePoints;

		for (VtkLidarPainter<?> aPainter : vPainterM.values())
			aPainter.setShowSourcePoints(aShowSourcePoints);

		updateVtkVars(getAllItems());
	}

	@Override
	public void setTranslation(Collection<LidarTrack> aItemC, Vector3D aVect)
	{
		for (LidarTrack aItem : aItemC)
		{
			RenderProp tmpProp = propM.get(aItem);
			if (tmpProp == null)
				continue;

			tmpProp.translation = aVect;
			tmpProp.errAmt = Double.NaN;
		}

		// Send out the appropriate notifications
		notifyListeners(this, ItemEventType.ItemsMutated);
		updateVtkVars(aItemC);
	}

	@Override
	public void removeItems(Collection<LidarTrack> aItemC)
	{
		// Remove relevant state and VTK mappings
		for (LidarTrack aItem : aItemC)
		{
			propM.remove(aItem);

			// Remove and release the resources associated with the track
			VtkLidarPainter<LidarTrack> tmpPainter = vPainterM.remove(aItem);
			tmpPainter.vtkDispose();
		}

		// Delegate
		super.removeItems(aItemC);

		List<LidarTrack> tmpL = ImmutableList.of();
		updateVtkVars(tmpL);
		notifyVtkStateChange();
	}

	@Override
	public void setAllItems(Collection<LidarTrack> aItemC)
	{
		// Clear relevant state vars
		propM = new HashMap<>();
		radialOffset = 0.0;

		// Setup the initial props for all the items
		int tmpIdx = 0;
		int numItems = aItemC.size();
		for (LidarTrack aItem : aItemC)
		{
			ColorProvider tmpSrcCP = sourceGCP.getColorProviderFor(aItem, tmpIdx, numItems);
			ColorProvider tmpTgtCP = targetGCP.getColorProviderFor(aItem, tmpIdx, numItems);

			RenderProp tmpProp = new RenderProp();
			tmpProp.srcCP = tmpSrcCP;
			tmpProp.tgtCP = tmpTgtCP;
			tmpIdx++;

			propM.put(aItem, tmpProp);
		}

		// Update vPainterM and vActorToPainterM to reflect the installed Tracks
		Map<LidarTrack, VtkLidarPainter<LidarTrack>> oldDrawM = vPainterM;

		vPainterM = new HashMap<>();
		for (LidarTrack aItem : aItemC)
		{
			VtkLidarPainter<LidarTrack> tmpPainter = oldDrawM.remove(aItem);
			if (tmpPainter == null)
			{
				VtkLidarStruct tmpVLS = VtkUtil.formVtkLidarStruct(aItem.getPointList().iterator());
				tmpPainter = new VtkLidarUniPainter<>(this, aItem, tmpVLS);
			}
			tmpPainter.setHighlightSelection(true);
			tmpPainter.setShowSourcePoints(showSourcePoints);

			vPainterM.put(aItem, tmpPainter);

			// Set in the hard coded configuration state
			tmpPainter.setPercentageShown(0.0, 1.0);
		}

		// Manually dispose of the (remaining) old VtkPainters
		for (VtkLidarPainter<?> aPainter : oldDrawM.values())
			aPainter.vtkDispose();

		// Delegate
		super.setAllItems(aItemC);

		updateVtkVars(aItemC);
		notifyVtkStateChange();
	}

	@Override
	public void setSelectedItems(Collection<LidarTrack> aItemC)
	{
		super.setSelectedItems(aItemC);

		// Selected items will be rendered with a different point size.
		// Force the painters to "update" their point size
		setPointSize(pointSize);
	}

	@Override
	public List<vtkProp> getProps()
	{
		List<vtkProp> retL = new ArrayList<>();

		for (LidarTrack aItem : getAllItems())
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
		LidarTrack tmpItem = null;
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
			VtkLidarPainter<LidarTrack> tmpPainter = getPainterFor(aPrimaryTarg);
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

		Set<LidarTrack> tmpItemS = getSelectedItems();
		updateVtkVars(tmpItemS);
	}

	/**
	 * Helper method that returns the {@link VtkLidarPainter} associated with the
	 * specified {@link vtkProp}. Returns null if the {@link vtkProp} did not
	 * originate from this manager.
	 */
	@SuppressWarnings("unchecked")
	private VtkLidarPainter<LidarTrack> getPainterFor(PickTarget aPickTarg)
	{
		// Bail if aProp is not the right type
		vtkProp tmpProp = aPickTarg.getActor();
		if (tmpProp instanceof SaavtkLODActor == false)
			return null;

		// Retrieve the painter and return it if we are the associated manager
		VtkLidarPainter<?> tmpPainter = ((SaavtkLODActor) tmpProp).getAssocModel(VtkLidarPainter.class);
		if (tmpPainter != null && tmpPainter.getManager() == this)
			return (VtkLidarPainter<LidarTrack>) tmpPainter;

		return null;
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
	 * <P>
	 * A notification will be sent out to PropertyChange listeners of the
	 * {@link Properties#MODEL_CHANGED} event.
	 */
	private void updateVtkVars(Collection<LidarTrack> aUpdateC)
	{
		vPointPainter.vtkUpdateState();
		for (LidarTrack aItem : aUpdateC)
		{
			// Skip to next if no installed painter
			VtkLidarPainter<?> tmpPainter = vPainterM.get(aItem);
			if (tmpPainter == null)
				continue;

			tmpPainter.vtkUpdateState();
		}

		notifyVtkStateChange();
	}

}
