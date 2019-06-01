package edu.jhuapl.sbmt.model.lidar;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
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
import edu.jhuapl.saavtk.pick.DefaultPicker;
import edu.jhuapl.saavtk.pick.PickEvent;
import edu.jhuapl.saavtk.pick.PickUtil;
import edu.jhuapl.saavtk.util.ColorUtil;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.util.TimeUtil;

import glum.item.ItemEventType;

/**
 * Class that provides management logic for a collection of lidar Tracks.
 * <P>
 * The following features are supported:
 * <UL>
 * <LI>Event handling
 * <LI>Management to collection of lidar Tracks
 * <LI>Support for track selection
 * <LI>Configuration of associated rendering properties.
 * <LI>Track offset translation
 * <LI>Track error calculation
 * <LI>Support to apply a radial offset to all Tracks
 * </UL>
 * <P>
 * Currently (VTK) rendering of Tracks is supported, however that capability
 * should eventually be removed and placed in a separate class/module.
 *
 * @author lopeznr1
 */
public class LidarTrackManager extends SaavtkItemManager<LidarTrack>
{
	// Reference vars
	protected final PolyhedralModel refSmallBodyModel;

	// State vars
	private Map<LidarTrack, TrackProp> propM;
	private double radialOffset;

	// VTK vars
	private VtkPointPainter vPointPainter;
	private VtkTrackPainter vTrackPainter;
	private List<vtkProp> vActorL;

	/**
	 * Standard Constructor
	 *
	 * @param aSmallBodyModel
	 */
	public LidarTrackManager(PolyhedralModel aSmallBodyModel)
	{
		refSmallBodyModel = aSmallBodyModel;

		propM = new HashMap<>();
		radialOffset = 0.0;

		vPointPainter = new VtkPointPainter(this);
		vTrackPainter = new VtkTrackPainter(this);

		vActorL = new ArrayList<>();
		vActorL.add(vTrackPainter.getActor());
		vActorL.add(vPointPainter.getActor());

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

		// Bail if aActor is not associated with a relevant VtkTrackPainter
		VtkTrackPainter tmpTrackPainter = getVtkTrackPainterForActor(aActor);
		if (tmpTrackPainter == null)
			return null;

		return tmpTrackPainter.getLidarPointFromCellId(aCellId);
	}

	/**
	 * Returns the color associated with the Track.
	 */
	public Color getColor(LidarTrack aTrack)
	{
		TrackProp tmpProp = propM.get(aTrack);
		if (tmpProp == null)
			return null;

		return tmpProp.color;
	}

	/**
	 * Returns true if the track is visible.
	 */
	public boolean getIsVisible(LidarTrack aTrack)
	{
		TrackProp tmpProp = propM.get(aTrack);
		if (tmpProp == null)
			return false;

		return tmpProp.isVisible;
	}

	/**
	 * Return the Track at the specified index.
	 *
	 * @param aIdx
	 * @return
	 */
	public LidarTrack getTrack(int aIdx)
	{
		return getAllItems().get(aIdx);
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
		TrackProp tmpProp = propM.get(aTrack);
		double tmpErr = tmpProp.errAmt;
		if (Double.isNaN(tmpErr) == false)
			return tmpErr;

		// Calculate the error
		tmpErr = 0.0;
		Vector3D tmpVect = getTranslation(aTrack);
		for (LidarPoint aLP : aTrack.getPointList())
		{
			double[] ptLidar = aLP.getTargetPosition().toArray();

			ptLidar = transformLidarPoint(tmpVect, ptLidar);
			double[] ptClosest = refSmallBodyModel.findClosestPoint(ptLidar);
			tmpErr += MathUtil.distance2Between(ptLidar, ptClosest);
		}

		// Update the cache and return the error
		tmpProp.errAmt = tmpErr;
		return tmpErr;
	}

	/**
	 * Returns the translation associated with the specified Track.
	 *
	 * @param aTrack The Track of interest.
	 */
	public Vector3D getTranslation(LidarTrack aTrack)
	{
		TrackProp tmpProp = propM.get(aTrack);
		if (tmpProp == null)
			return null;

		return tmpProp.translation;
	}

	/**
	 * Method that will set the list of Tracks to visible and set all other
	 * Tracks to invisible.
	 *
	 * @param aTrackL
	 */
	public void hideOtherTracksExcept(List<LidarTrack> aTrackL)
	{
		Set<LidarTrack> tmpSet = new HashSet<>(aTrackL);

		// Update the visibility flag on each Track
		for (LidarTrack aTrack : getAllItems())
		{
			TrackProp tmpProp = propM.get(aTrack);
			if (tmpProp == null)
				continue;

			boolean isVisible = tmpSet.contains(aTrack);
			tmpProp.isVisible = isVisible;
		}

		notifyListeners(this, ItemEventType.ItemsMutated);
		updateVtkVars();
	}

	/**
	 * Sets the color associated with the specified Track.
	 */
	public void setColor(LidarTrack aTrack, Color aColor)
	{
		// Delegate
		List<LidarTrack> tmpL = ImmutableList.of(aTrack);
		setColor(tmpL, aColor);
	}

	/**
	 * Sets the color associated with the specified list of Tracks.
	 */
	public void setColor(List<LidarTrack> aTrackL, Color aColor)
	{
		for (LidarTrack aTrack : aTrackL)
		{
			TrackProp tmpProp = propM.get(aTrack);
			if (tmpProp == null)
				continue;

			tmpProp.color = aColor;
		}

		notifyListeners(this, ItemEventType.ItemsMutated);
		updateVtkVars();
	}

	/**
	 * Sets the specified Track to be visible.
	 *
	 * @param aTrack
	 * @param aBool True if the Track should be visible
	 */
	public void setIsVisible(LidarTrack aTrack, boolean aBool)
	{
		// Delegate
		List<LidarTrack> tmpL = ImmutableList.of(aTrack);
		setIsVisible(tmpL, aBool);
	}

	/**
	 * Sets the specified lists of Tracks to be visible.
	 *
	 * @param aTrackL
	 * @param aBool True if the Tracks should be visible
	 */
	public void setIsVisible(List<LidarTrack> aTrackL, boolean aBool)
	{
		for (LidarTrack aTrack : aTrackL)
		{
			TrackProp tmpProp = propM.get(aTrack);
			if (tmpProp == null)
				continue;

			tmpProp.isVisible = aBool;
		}

		notifyListeners(this, ItemEventType.ItemsMutated);
		updateVtkVars();
	}

	/**
	 * Sets in the baseline point size for all of the tracks
	 *
	 * @param aSize
	 */
	public void setPointSize(int aSize)
	{
		vTrackPainter.setPointSize(aSize);
		vPointPainter.setPointSize(aSize * 3.5);

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

		updateVtkVars();
	}

	/**
	 * Set in the translation amount for each of the specified Tracks.
	 *
	 * @param aTrackL The list of Tracks of interest.
	 * @param aVect The vector that defines the translation amount.
	 */
	public void setTranslation(List<LidarTrack> aTrackL, Vector3D aVect)
	{
		for (LidarTrack aTrack : aTrackL)
		{
			TrackProp tmpProp = propM.get(aTrack);
			if (tmpProp == null)
				continue;

			tmpProp.translation = aVect;
			tmpProp.errAmt = Double.NaN;
		}

		// Send out the appropriate notifications
		notifyListeners(this, ItemEventType.ItemsMutated);
		updateVtkVars();
	}

	@Override
	public void removeItems(List<LidarTrack> aTrackL)
	{
		for (LidarTrack aTrack : aTrackL)
			propM.remove(aTrack);

		// Delegate
		super.removeItems(aTrackL);

		updateVtkVars();
	}

	@Override
	public void setAllItems(List<LidarTrack> aTrackL)
	{
		// Clear state vars
		propM = new HashMap<>();
		radialOffset = 0.0;

		// Setup the initial props for all the Tracks
		int tmpIdx = 0;
		Color[] colorArr = ColorUtil.generateColors(aTrackL.size());
		for (LidarTrack aTrack : aTrackL)
		{
			TrackProp tmpProp = new TrackProp();
			tmpProp.color = colorArr[tmpIdx];
			tmpIdx++;

			propM.put(aTrack, tmpProp);
		}

		// Delegate
		super.setAllItems(aTrackL);

		updateVtkVars();
	}

	@Override
	public String getClickStatusBarText(vtkProp aProp, int aCellId, double[] aPickPosition)
	{
		// Bail if there is no VtkTrackPainter associated with the actor (aProp)
		VtkTrackPainter tmpTrackPainter = getVtkTrackPainterForActor(aProp);
		if (tmpTrackPainter == null)
			return "";

		try
		{
			// TODO: This is badly designed hack to prevent the program from
			// crashing by just sticking bad index checks in a silent try-catch
			// block. This is indicative of defective logic.
			LidarPoint tmpLP = tmpTrackPainter.getLidarPointFromCellId(aCellId);

			double et = tmpLP.getTime();
			double range = tmpLP.getSourcePosition().subtract(tmpLP.getTargetPosition()).getNorm() * 1000; // m
			return String.format("Lidar point acquired at " + TimeUtil.et2str(et) + ", ET = %f, unmodified range = %f m",
					et, range);
		}
		catch (Exception aExp)
		{
		}

		return "";
	}

	/**
	 * Returns the current radial offset (of all Tracks).
	 */
	@Override
	public double getOffset()
	{
		return radialOffset;
	}

	@Override
	public List<vtkProp> getProps()
	{
		return vActorL;
	}

	/**
	 * Sets in the radial offset (of all Tracks).
	 */
	@Override
	public void setOffset(double aOffset)
	{
		// Update the radialOffset
		if (radialOffset == aOffset)
			return;
		radialOffset = aOffset;

		// Invalidate the cache vars
		for (TrackProp aProp : propM.values())
			aProp.errAmt = Double.NaN;

		// Send out the appropriate notifications
		notifyListeners(this, ItemEventType.ItemsMutated);
		updateVtkVars();
	}

	/**
	 * Helper method that takes the given lidar point and returns a corresponding
	 * point that takes into account the radial offset and the specified
	 * translation vector.
	 *
	 * @param aVect The translation vector of interest.
	 * @param aPt The lidar point of interest.
	 */
	protected double[] transformLidarPoint(Vector3D aVect, double[] aPt)
	{
		if (radialOffset != 0.0)
		{
			LatLon lla = MathUtil.reclat(aPt);
			lla = new LatLon(lla.lat, lla.lon, lla.rad + radialOffset);
			aPt = MathUtil.latrec(lla);
		}

		return new double[] { aPt[0] + aVect.getX(), aPt[1] + aVect.getY(), aPt[2] + aVect.getZ() };
	}

	/**
	 * Similar to previous function but specific to spacecraft position. The
	 * difference is that we calculate the radial offset we applied to the lidar
	 * and apply that offset to the spacecraft (rather than computing the radial
	 * offset directly for the spacecraft).
	 *
	 * @param aVect The translation vector of interest.
	 * @param scpos
	 * @param lidarPoint
	 * @return
	 */
	protected double[] transformScpos(Vector3D aVect, double[] scpos, double[] lidarPoint)
	{
		if (radialOffset != 0.0)
		{
			LatLon lla = MathUtil.reclat(lidarPoint);
			lla = new LatLon(lla.lat, lla.lon, lla.rad + radialOffset);
			double[] offsetLidarPoint = MathUtil.latrec(lla);

			scpos[0] += (offsetLidarPoint[0] - lidarPoint[0]);
			scpos[1] += (offsetLidarPoint[1] - lidarPoint[1]);
			scpos[2] += (offsetLidarPoint[2] - lidarPoint[2]);
		}

		return new double[] { scpos[0] + aVect.getX(), scpos[1] + aVect.getY(), scpos[2] + aVect.getZ() };
	}

	/**
	 * Helper method that returns the VtkTrackPainter corresponding to the
	 * specified actor.
	 */
	private VtkTrackPainter getVtkTrackPainterForActor(vtkProp aActor)
	{
		if (aActor == vTrackPainter.getActor())
			return vTrackPainter;

		return null;
	}

	/**
	 * Helper method that will update all relevant VTK vars.
	 * <P>
	 * A notification will be sent out to PropertyChange listeners of the
	 * {@link Properties#MODEL_CHANGED} event.
	 */
	private void updateVtkVars()
	{
		vPointPainter.updateVtkVars();
		vTrackPainter.updateVtkVars();

		// Notify our PropertyChangeListeners
		pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
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

		// Retrieve the selected lidar Point and corresponding Track
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
			// Bail if tmpActor is not associated with a relevant VtkTrackPainter
			VtkTrackPainter tmpTrackPainter = getVtkTrackPainterForActor(tmpActor);
			if (tmpTrackPainter == null)
				return;

			// Update the selected LidarPoint
			int tmpCellId = aPickEvent.getPickedCellId();
			tmpPoint = tmpTrackPainter.getLidarPointFromCellId(tmpCellId);

			// Determine the Track that was selected
			tmpTrack = tmpTrackPainter.getTrackFromCellId(tmpCellId);
			if (tmpTrack == null)
				return;

			// Update the VtkLidaPoint to reflect the selected point
			vPointPainter.setData(tmpPoint, tmpTrack);
		}

		// Determine if this is a modified action
		boolean isModifyKey = PickUtil.isModifyKey(aPickEvent.getMouseEvent());

		// Determine the Tracks that will be marked as selected
		List<LidarTrack> tmpL = getSelectedItems();
		tmpL = new ArrayList<>(tmpL);

		if (isModifyKey == false)
			tmpL = ImmutableList.of(tmpTrack);
		else if (tmpL.contains(tmpTrack) == false)
			tmpL.add(tmpTrack);

		// Update the selected Tracks
		setSelectedItems(tmpL);

		Object source = aPickEvent.getMouseEvent().getSource();
		notifyListeners(source, ItemEventType.ItemsSelected);

		updateVtkVars();
	}

}
