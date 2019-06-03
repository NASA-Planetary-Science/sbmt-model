package edu.jhuapl.sbmt.model.lidar;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vtk.vtkProp;

import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;

import glum.item.ItemEventType;

/**
 * Class that provides management logic for a collection of lidar FileSpecs.
 * <P>
 * The following features are supported:
 * <UL>
 * <LI>Event handling
 * <LI>Management to collection of FileSpecs.
 * <LI>Support for FileSpec selection
 * <LI>Configuration of associated rendering properties.
 * </UL>
 * <P>
 * Currently (VTK) rendering of FileSpecs is supported, however that capability
 * should eventually be removed and placed in a separate class/module.
 *
 * @author lopeznr1
 */
public class LidarFileSpecManager extends SaavtkItemManager<LidarFileSpec>
{
	// Ref vars
	private final BodyViewConfig refBodyViewConfig;

	// State vars
	private double begPercent;
	private double endPercent;
	private double radialOffset;
	private boolean showSpacecraftPosition;

	// VTK vars
	private Map<LidarFileSpec, LidarDataPerUnit> drawM;
	private HashMap<vtkProp, LidarDataPerUnit> actorToFileMap = new HashMap<>();

	/**
	 * Standard Constructor
	 */
	public LidarFileSpecManager(SmallBodyModel aSmallBodyModel)
	{
		// TODO: Just pass the needed args
		refBodyViewConfig = (SmallBodyViewConfig) aSmallBodyModel.getSmallBodyConfig();

		begPercent = 0.0;
		endPercent = 1.0;
		radialOffset = 0.0;
		showSpacecraftPosition = true;

		drawM = new HashMap<>();
	}

	/**
	 * Returns the color associated with the specified LidarFileSpec.
	 */
	public Color getColor(LidarFileSpec aFileSpec)
	{
		LidarDataPerUnit tmpData = drawM.get(aFileSpec);
		if (tmpData == null)
			return null;

		return tmpData.getColor();
	}

	/**
	 * Returns the number of points associated with the specified LidarFileSpec.
	 * <P>
	 * Returns 0 if the file has not been loaded.
	 */
	public Integer getNumberOfPoints(LidarFileSpec aFileSpec)
	{
		LidarDataPerUnit tmpData = drawM.get(aFileSpec);
		if (tmpData == null)
			return 0;
		if (tmpData.isLoaded() == false)
			return 0;

		return tmpData.getNumberOfPoints();
	}

	/**
	 * Returns whether the specified LidarFileSpec is being rendered.
	 */
	public boolean getIsVisible(LidarFileSpec aFileSpec)
	{
		LidarDataPerUnit tmpData = drawM.get(aFileSpec);
		if (tmpData == null)
			return false;

		return tmpData.getIsVisible();
	}

	/**
	 * Returns whether the specified LidarFileSpec has been loaded.
	 */
	public boolean isLoaded(LidarFileSpec aFileSpec)
	{
		LidarDataPerUnit tmpData = drawM.get(aFileSpec);
		if (tmpData == null)
			return false;

		return tmpData.isLoaded();
	}

	/**
	 * Sets the color associated with the specified LidarFileSpec.
	 */
	public void setColor(List<LidarFileSpec> aFileSpecL, Color aColor)
	{
		for (LidarFileSpec aFileSpec : aFileSpecL)
		{
			// Skip to next if not loaded
			LidarDataPerUnit tmpData = drawM.get(aFileSpec);
			if (tmpData == null)
				continue;

			tmpData.setColor(aColor);
		}

		notifyListeners(this, ItemEventType.ItemsMutated);
		updateVtkVars();
	}

	/**
	 * Sets whether the specified LidarFileSpec should be rendered.
	 */
	public void setIsVisible(List<LidarFileSpec> aFileSpecL, boolean aBool)
	{
		// Delegate
		if (aBool == false)
			hideLidarData(aFileSpecL);
		else
			showLidarData(aFileSpecL);

		notifyListeners(this, ItemEventType.ItemsMutated);
		updateVtkVars();
	}

	// TODO: Add javadoc
	public void setPercentageShown(double aBegPercent, double aEndPercent)
	{
		begPercent = aBegPercent;
		endPercent = aEndPercent;

		for (LidarFileSpec aKey : drawM.keySet())
		{
			LidarDataPerUnit data = drawM.get(aKey);
			data.setPercentageShown(aBegPercent, aEndPercent);
			data.showPercentageShown();
		}

		updateVtkVars();
	}

	// TODO: Add javadoc
	public void setShowSpacecraftPosition(boolean show)
	{
		showSpacecraftPosition = show;

		for (LidarFileSpec aKey : drawM.keySet())
		{
			LidarDataPerUnit data = drawM.get(aKey);
			data.setShowSpacecraftPosition(show);
		}

		updateVtkVars();
	}

	@Override
	public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
	{
		LidarDataPerUnit data = actorToFileMap.get(prop);
		return data != null ? data.getClickStatusBarText(prop, cellId, pickPosition) : "";
	}

	@Override
	public List<vtkProp> getProps()
	{
		List<vtkProp> retL = new ArrayList<>();

		for (LidarDataPerUnit aPainter : drawM.values())
		{
			if (aPainter.getIsVisible() == false)
				continue;

			for (vtkProp aProp : aPainter.getProps())
				retL.add(aProp);
		}

		return retL;
	}

	@Override
	public void setOffset(double offset)
	{
		radialOffset = offset;

		for (LidarFileSpec aKey : drawM.keySet())
		{
			LidarDataPerUnit data = drawM.get(aKey);
			data.setOffset(offset);
		}

		updateVtkVars();
	}

	// TODO: Add javadoc
	private void showLidarData(List<LidarFileSpec> aFileSpecL)
	{
		for (LidarFileSpec aFileSpec : aFileSpecL)
		{
			// Skip to next if VTK data has already been created and is visible
			LidarDataPerUnit tmpData = drawM.get(aFileSpec);
			if (tmpData != null)
			{
				tmpData.setIsVisible(true);
				continue;
			}

			try
			{
				// TODO: [1] Color should not be specified here
				// Select the color
				Color colorGround = Color.BLUE;
				Color colorSpace = Color.GREEN;

				// TODO: The below is a brittle way of handling specialized coloring
				// for Itokawa optimized lidar data, show in different color.
				String tmpPath = aFileSpec.getPath();
				if (refBodyViewConfig.lidarBrowseOrigPathRegex != null
						&& !refBodyViewConfig.lidarBrowseOrigPathRegex.isEmpty())
					tmpPath = tmpPath.replaceAll(refBodyViewConfig.lidarBrowseOrigPathRegex,
							refBodyViewConfig.lidarBrowsePathTop);
				if (tmpPath.contains("_v2") == true)
				{
					colorGround = Color.YELLOW;
					colorSpace = Color.MAGENTA;
				}

				// TODO: [2] tmpData is not used again.
				tmpData = new LidarDataPerUnit(refBodyViewConfig, aFileSpec, colorGround, colorSpace,
						new LidarLoadingListener() {
							@Override
							public void lidarLoadComplete(LidarDataPerUnit aData)
							{
								drawM.put(aFileSpec, aData);
								for (vtkProp prop : aData.getProps())
									actorToFileMap.put(prop, aData);

								aData.setShowSpacecraftPosition(showSpacecraftPosition);

								aData.setOffset(radialOffset);
								aData.setPercentageShown(begPercent, endPercent);
								aData.showPercentageShown();

								aData.setIsVisible(true);

								notifyListeners(this, ItemEventType.ItemsMutated);
								pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
							}
						});

			}
			catch (IOException aExp)
			{
				aExp.printStackTrace();
			}

			pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
		}
	}

	private void hideLidarData(List<LidarFileSpec> aFileSpecL)
	{
		for (LidarFileSpec aFileSpec : aFileSpecL)
		{
			LidarDataPerUnit tmpPainter = drawM.get(aFileSpec);
			if (tmpPainter == null)
				continue;

			tmpPainter.setIsVisible(false);
		}
	}

	/**
	 * Helper method that will update all relevant VTK vars.
	 * <P>
	 * A notification will be sent out to PropertyChange listeners of the
	 * {@link Properties#MODEL_CHANGED} event.
	 */
	private void updateVtkVars()
	{
//		vPointPainter.updateVtkVars();
//		vTrackPainter.updateVtkVars();

		// Notify our PropertyChangeListeners
		pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
	}

}
