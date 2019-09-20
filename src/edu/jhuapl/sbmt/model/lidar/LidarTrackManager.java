package edu.jhuapl.sbmt.model.lidar;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.collect.ImmutableList;

import vtk.vtkActor;
import vtk.vtkProp;

import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.model.SaavtkItemManager;
import edu.jhuapl.saavtk.pick.DefaultPicker;
import edu.jhuapl.saavtk.pick.PickEvent;
import edu.jhuapl.saavtk.pick.PickUtil;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.Properties;
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
 * <LI>Support to apply a radial offset to all Tracks
 * <LI>Support to specify the point size to render Tracks
 * </UL>
 * <P>
 * Currently (VTK) rendering of Tracks is supported, however that capability
 * should eventually be removed and placed in a separate class/module.
 *
 * @author lopeznr1
 */
public class LidarTrackManager extends SaavtkItemManager<LidarTrack> implements LidarManager<LidarTrack>
{
	// Reference vars
	protected final PolyhedralModel refSmallBodyModel;

	// State vars
	private Map<LidarTrack, RenderProp> propM;
	private GroupColorProvider sourceGCP;
	private GroupColorProvider targetGCP;
	private double radialOffset;
	private double pointSize;

	// VTK vars
	private Map<LidarTrack, VtkLidarPainter<LidarTrack>> vPainterM;
	private Map<vtkProp, VtkLidarPainter<LidarTrack>> vActorToPainterM;
	private VtkPointPainter vPointPainter;

	/**
	 * Standard Constructor
	 *
	 * @param aSmallBodyModel
	 */
	public LidarTrackManager(PolyhedralModel aSmallBodyModel)
	{
		refSmallBodyModel = aSmallBodyModel;

		propM = new HashMap<>();
		sourceGCP = ColorWheelGroupColorProvider.Instance;
		targetGCP = ColorWheelGroupColorProvider.Instance;
		radialOffset = 0.0;
		pointSize = 2.0;

		vPainterM = new HashMap<>();
		vActorToPainterM = new HashMap<>();
		vPointPainter = new VtkPointPainter(this);

		setPointSize(2);
	}

	/**
	 * Returns the LidarPoint associated with the specified vtkActor and cellId.
	 * <P>
	 * Returns null if there is no LidarPoint corresponding the specified
	 * vtkActor / cellId.
	 *
	 * @param aActor The vtkActor associated with the pick action
	 * @param aCellId The cell id associated with the pick action
	 */
	public LidarPoint getLidarPointFromVtkPick(vtkActor aActor, int aCellId)
	{
		// Return the LidarPoint associated with the vPointPainter
		if (aActor == vPointPainter.getActor())
			return vPointPainter.getPoint();

		// Bail if aActor is not associated with a relevant painter
		VtkLidarPainter<LidarTrack> tmpPainter = vActorToPainterM.get(aActor);
		if (tmpPainter == null)
			return null;

		return tmpPainter.getLidarPointForCell(aCellId);
	}

	/**
	 * Returns the cumulative error for the specified Track.
	 * <P>
	 * The cumulative error is defined as the summation of all of the error for
	 * each LidarPoint associated with the Track.
	 *
	 * @param aTrack
	 * @return
	 */
	public double getTrackError(LidarTrack aTrack)
	{
		// Utilize the cached value
		RenderProp tmpProp = propM.get(aTrack);
		double tmpErr = tmpProp.errAmt;
		if (Double.isNaN(tmpErr) == false)
			return tmpErr;

		// Calculate the error
		tmpErr = 0.0;
		Vector3D tmpVect = getTranslation(aTrack);
		for (LidarPoint aLP : aTrack.getPointList())
		{
			Vector3D targetV = aLP.getTargetPosition();

			double[] ptLidarArr = LidarGeoUtil.transformTarget(tmpVect, radialOffset, targetV).toArray();
			double[] ptClosest = refSmallBodyModel.findClosestPoint(ptLidarArr);
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

		pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
	}

	/**
	 * Sets the selected LidarPoint
	 *
	 * @param aLidarPoint The LidarPoint of interest
	 * @param aLidarTrack The Track associated with the LidarPoint
	 */
	public void setSelectedPoint(LidarPoint aLidarPoint, LidarTrack aLidarTrack)
	{
		vPointPainter.setData(aLidarPoint, aLidarTrack);
		notifyListeners(this, ItemEventType.ItemsSelected);

		List<LidarTrack> tmpL = ImmutableList.of();
		if (aLidarTrack != null)
			tmpL = ImmutableList.of(aLidarTrack);
		updateVtkVars(tmpL);
	}

	@Override
	public void clearCustomColorProvider(List<LidarTrack> aItemL)
	{
		Set<LidarTrack> tmpSet = new HashSet<>(aItemL);

		int tmpIdx = -1;
		int numItems = getNumItems();
		for (LidarTrack aItem : getAllItems())
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
		Set<LidarTrack> tmpSet = new HashSet<>(aItemL);

		// Update the visibility flag on each Track
		for (LidarTrack aItem : getAllItems())
		{
			RenderProp tmpProp = propM.get(aItem);
			if (tmpProp == null)
				continue;

			boolean isVisible = tmpSet.contains(aItem);
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
	public void removeItems(List<LidarTrack> aItemL)
	{
		// Remove relevant state and VTK mappings
		for (LidarTrack aTrack : aItemL)
		{
			propM.remove(aTrack);

			// Remove and release the resources associated with the track
			VtkLidarPainter<LidarTrack> tmpPainter = vPainterM.remove(aTrack);

			for (vtkProp aProp : tmpPainter.getProps())
				vActorToPainterM.remove(aProp);

			tmpPainter.vtkDispose();
		}

		// Delegate
		super.removeItems(aItemL);

		List<LidarTrack> tmpL = ImmutableList.of();
		updateVtkVars(tmpL);
	}

	@Override
	public void setAllItems(List<LidarTrack> aItemL)
	{
		// Clear relevant state vars
		propM = new HashMap<>();
		radialOffset = 0.0;

		// Setup the initial props for all the items
		int tmpIdx = 0;
		int numItems = aItemL.size();
		for (LidarTrack aItem : aItemL)
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
		vActorToPainterM = new HashMap<>();
		for (LidarTrack aItem : aItemL)
		{
			VtkLidarPainter<LidarTrack> tmpPainter = oldDrawM.remove(aItem);
			if (tmpPainter == null)
			{
				VtkLidarStruct tmpVLS = VtkUtil.formVtkLidarStruct(aItem.getPointList().iterator());
				tmpPainter = new VtkLidarUniPainter<>(this, aItem, tmpVLS);
			}
			tmpPainter.setHighlightSelection(true);

			vPainterM.put(aItem, tmpPainter);
			for (vtkProp aProp : tmpPainter.getProps())
				vActorToPainterM.put(aProp, tmpPainter);

			// Set in the hard coded configuration state
			tmpPainter.setShowSourcePoints(false);
			tmpPainter.setPercentageShown(0.0, 1.0);
		}

		// Manually dispose of the (remaining) old VtkPainters
		for (VtkLidarPainter<?> aPainter : oldDrawM.values())
			aPainter.vtkDispose();

		// Delegate
		super.setAllItems(aItemL);

		updateVtkVars(aItemL);
	}

	@Override
	public void setSelectedItems(List<LidarTrack> aItemL)
	{
		super.setSelectedItems(aItemL);

		// Selected items will be rendered with a different point size.
		// Force the painters to "update" their point size
		setPointSize(pointSize);
	}

	@Override
	public String getClickStatusBarText(vtkProp aProp, int aCellId, double[] aPickPosition)
	{
		// Bail if there is no painter associated with the actor (aProp)
		VtkLidarPainter<LidarTrack> tmpPainter = vActorToPainterM.get(aProp);
		if (tmpPainter == null)
			return "";

		// Custom title
		LidarTrack tmpItem = tmpPainter.getLidarItemForCell(aCellId);
		String tmpTitle = "(Trk " + tmpItem.getId() + ")";

		return tmpPainter.getDisplayInfoStr(aCellId, tmpTitle);
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

	/**
	 * TODO: We should be notified of the "DefaultPicker" through different means
	 * and not rely on unrelated third party handling...
	 */
	public void handleDefaultPickerManagement(DefaultPicker aDefaultPicker, ModelManager aModelManager)
	{
		aDefaultPicker.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent aEvent)
			{
				// Bail if not the right event type
				if (Properties.MODEL_PICKED.equals(aEvent.getPropertyName()) == false)
					return;

				// Bail if the picked item is not associated with our ItemManager
				PickEvent pickEvent = (PickEvent) aEvent.getNewValue();
				boolean isPass = aModelManager.getModel(pickEvent.getPickedProp()) == LidarTrackManager.this;
				if (isPass == false)
					return;

				// Delegate
				handlePickAction(pickEvent);
			}
		});
	}

	/**
	 * Helper method that will process the specified PickEvent.
	 * <P>
	 * The selected Tracks will be updated to reflect the PickEvent action.
	 *
	 * @param aPickEvent
	 */
	private void handlePickAction(PickEvent aPickEvent)
	{
		// Bail if the 1st button was not pushed
		if (aPickEvent.getMouseEvent().getButton() != 1)
			return;

		// Retrieve the selected lidar Point and corresponding track
		LidarTrack tmpTrack = null;
		LidarPoint tmpPoint = null;
		vtkProp tmpActor = aPickEvent.getPickedProp();
		if (tmpActor == vPointPainter.getActor())
		{
			tmpPoint = vPointPainter.getPoint();
			tmpTrack = vPointPainter.getTrack();
		}
		else
		{
			// Bail if tmpActor is not associated with a relevant painter
			VtkLidarPainter<LidarTrack> tmpPainter = vActorToPainterM.get(tmpActor);
			if (tmpPainter == null)
				return;

			// Determine the Track / Point that was selected
			int tmpCellId = aPickEvent.getPickedCellId();

			tmpTrack = tmpPainter.getLidarItemForCell(tmpCellId);
			tmpPoint = tmpPainter.getLidarPointForCell(tmpCellId);

			// Update the VtkPointPainter to reflect the selected point
			vPointPainter.setData(tmpPoint, tmpTrack);
		}

		// Determine if this is a modified action
		boolean isModifyKey = PickUtil.isModifyKey(aPickEvent.getMouseEvent());

		// Determine the Tracks that will be marked as selected
		List<LidarTrack> tmpL = new ArrayList<>(getSelectedItems());
		if (isModifyKey == false)
			tmpL = ImmutableList.of(tmpTrack);
		else if (getSelectedItems().contains(tmpTrack) == false)
			tmpL.add(tmpTrack);
		else
			tmpL.remove(tmpTrack);

		// Update the selected Tracks
		setSelectedItems(tmpL);

		Object source = aPickEvent.getMouseEvent().getSource();
		notifyListeners(source, ItemEventType.ItemsSelected);

		updateVtkVars(tmpL);
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

		// Notify our PropertyChangeListeners
		pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
	}

}
