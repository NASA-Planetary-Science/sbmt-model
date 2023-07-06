package edu.jhuapl.sbmt.model.phobos.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.swing.SwingUtilities;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ImmutableSet;

import vtk.vtkDoubleArray;
import vtk.vtkProp;

import edu.jhuapl.saavtk.color.provider.ColorProvider;
import edu.jhuapl.saavtk.color.provider.GroupColorProvider;
import edu.jhuapl.saavtk.color.provider.SimpleColorProvider;
import edu.jhuapl.saavtk.feature.FeatureAttr;
import edu.jhuapl.saavtk.feature.FeatureType;
import edu.jhuapl.saavtk.feature.VtkFeatureAttr;
import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.model.SaavtkItemManager;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.model.phobos.controllers.MEGANEDatabaseConnection;
import edu.jhuapl.sbmt.model.phobos.render.MEGANEFootprintRenderer;

import crucible.crust.logging.SimpleLogger;
import glum.item.ItemEventType;

public class MEGANECollection extends SaavtkItemManager<MEGANEFootprint> implements PropertyChangeListener
{
	private List<MEGANEFootprint> footprints;
	private SmallBodyModel smallBodyModel;
	private HashMap<MEGANEFootprint, MEGANEFootprintRenderer> footprintRenderers;
	private MEGANEDatabaseConnection dbConnection;
	private SimpleLogger logger = SimpleLogger.getInstance();
	private Map<MEGANEFootprint, MEGANEFootprintRenderProperties> propM;
	private Renderer renderer;

	public MEGANECollection(SmallBodyModel smallBodyModel)
	{
		logger.setLogFormat("%1$tF %1$tT.%1$tL %4$-7s %2$s %5$s%6$s%n");
		this.smallBodyModel = smallBodyModel;
		this.footprintRenderers = new HashMap<MEGANEFootprint, MEGANEFootprintRenderer>();
		propM = new HashMap<>();
	}

	public MEGANECollection(List<MEGANEFootprint> footprints, SmallBodyModel smallBodyModel)
	{
		this(smallBodyModel);
		this.footprints = footprints;
	}

	public void setFootprints(List<MEGANEFootprint> footprints)
	{
		this.footprints = footprints;
		setAllItems(footprints);
	}

	@Override
	public void setAllItems(Collection<MEGANEFootprint> aItemC)
	{
		// Clear relevant state vars
		propM = new HashMap<>();
		// Setup the initial props for all the items
		for (MEGANEFootprint aItem : aItemC)
		{
			MEGANEFootprintRenderProperties tmpProp = new MEGANEFootprintRenderProperties();
			tmpProp.isVisible = false;
			propM.put(aItem, tmpProp);
		}
		super.setAllItems(aItemC);
	}

	@Override
	public synchronized List<vtkProp> getProps()
	{
		List<vtkProp> props = Lists.newArrayList();
		if (footprints == null) return props;
		for (MEGANEFootprintRenderer renderer : footprintRenderers.values())
		{
			props.add(renderer.getProps());
		}
		return props;
	}

	public void setFootprintMapped(MEGANEFootprint footprint, boolean isMapped)
	{
		footprint.setMapped(isMapped);
		propM.get(footprint).isVisible = isMapped;
		if (footprintRenderers.get(footprint) == null)
		{
			Thread thread = new Thread(new Runnable()
			{

				@Override
				public void run()
				{
					footprint.setStatus("Loading...");
					if (footprint.getFacets() == null)
					{
						try
						{
							footprint.setFacets(dbConnection.getFacets(footprint.getDateTime(), new Function<String, Void>()
							{

								@Override
								public Void apply(String t)
								{
									SwingUtilities.invokeLater(() -> {footprint.setStatus(t);});

									return null;
								}
							}));
						}
						catch (SQLException e1)
						{
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					MEGANEFootprintRenderer renderer = new MEGANEFootprintRenderer(footprint, smallBodyModel, MEGANECollection.this.pcs);
					footprintRenderers.put(footprint, renderer);
					renderer.getProps().SetVisibility(isMapped ? 1 : 0);
					footprint.setStatus("Loaded");
					SwingUtilities.invokeLater( () -> {
						renderer.shiftFootprint();
						pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
						notifyListeners(footprint, ItemEventType.ItemsMutated);
					});
				}
			});
			thread.start();
		}
		else
		{
			this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
			notifyListeners(footprint, ItemEventType.ItemsMutated);
		}

	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		// TODO Auto-generated method stub

	}

	public boolean isFootprintMapped(MEGANEFootprint footprint)
	{
		return footprint.isMapped();
	}

	public String getStatus(MEGANEFootprint footprint)
	{
		return footprint.getStatus();
	}

	/**
	 * @param dbConnection the dbConnection to set
	 */
	public void setDbConnection(MEGANEDatabaseConnection dbConnection)
	{
		this.dbConnection = dbConnection;
	}

	public void saveSelectedToFile(File file, HashMap<String, List<String>> metadata) throws IOException
	{
		ImmutableSet<MEGANEFootprint> selectedFootprints = getSelectedItems();
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write("## Saved Footprints file.  Search parameters:");
		writer.newLine();
		for (String key : metadata.keySet())
		{
			writer.write("##" + key + " = " + metadata.get(key));
			writer.newLine();
		}
		writer.write("## Date (ET), Center Lat (deg), Center Lon (deg), Altitude (km), Normalized Altitude, Signal Contribution");
		writer.newLine();
		for (MEGANEFootprint footprint : selectedFootprints)
		{
			writer.write(footprint.toCSV());
			writer.newLine();
		}
		writer.close();
	}

	public void installGroupColorProviders(GroupColorProvider aSrcGCP)
	{
		int tmpIdx = -1;
		for (MEGANEFootprint aItem : getAllItems())
		{
			tmpIdx++;

			// Skip to next if no RenderProp
			MEGANEFootprintRenderProperties tmpProp = propM.get(aItem);
			if (tmpProp == null)
				return;

			// Skip to next if custom
			if (tmpProp.isCustomCP == true)
				return;

			ColorProvider provider = aSrcGCP.getColorProviderFor(aItem, tmpIdx, getNumItems());
			if (provider instanceof SimpleColorProvider)
			{
				tmpProp.simpleCP = provider;

				if (tmpProp.customCP == null)
				{
					tmpProp.activeCP = tmpProp.simpleCP;
				}
				else
				{
					tmpProp.activeCP = tmpProp.customCP;
				}

			}
			else
			{
				tmpProp.featureCP =  provider;
				tmpProp.activeCP = tmpProp.featureCP;
			}
			refreshColoring(aItem);

		}
	}

	public ColorProvider getColorProviderForFootprint(MEGANEFootprint footprint)
	{
		return propM.get(footprint).activeCP;
	}

	/**
	 * @param segment
	 */
	public void refreshColoring(MEGANEFootprint segment)
	{
		MEGANEFootprintRenderer renderer = footprintRenderers.get(segment);
		if (renderer == null)
			return;
		MEGANEFootprintRenderProperties tmpProp = propM.get(segment);
		if (tmpProp.isVisible == true)
			renderer.setColoringProvider(tmpProp.activeCP);
		SwingUtilities.invokeLater(() -> {
			this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, renderer);
		});
	}

	public FeatureAttr getFeatureAttrFor(MEGANEFootprint footprint, FeatureType aFeatureType)
	{
		vtkDoubleArray dataArray = new vtkDoubleArray();
		List<MEGANEFootprintFacet> facets = footprint.getFacets();

		for (int i=0; i<facets.size(); i++)
		{
			dataArray.InsertNextValue(facets.get(i).getComputedValue());
		}
		return new VtkFeatureAttr(dataArray);
	}

	/**
	 * @return the renderer
	 */
	public Renderer getRenderer()
	{
		return renderer;
	}

	public void setRenderer(Renderer renderer)
	{
		this.renderer = renderer;
	}

//	public boolean hasCustomColor(MEGANEFootprint footprint)
//	{
//		return propM.get(footprint).customCP != null;
//	}
//
//	public void clearCustomColor(Collection<MEGANEFootprint> footprints)
//	{
//		for (MEGANEFootprint footprint : footprints)
//		{
//			propM.get(footprint).customCP = null;
//			propM.get(footprint).activeCP = propM.get(footprint).lastActive;
//			refreshColoring(footprint);
//		}
//	}
//	//coloring
//	public void installCustomColorProvider(Collection<MEGANEFootprint> aItemC, ColorProvider colorProvider)
//	{
//		for (MEGANEFootprint aItem : aItemC)
//		{
//			MEGANEFootprintRenderProperties tmpProp = propM.get(aItem);
//			tmpProp.lastActive = tmpProp.activeCP;
//			tmpProp.customCP = colorProvider;
//			tmpProp.activeCP = colorProvider;
//			propM.put(aItem, tmpProp);
//			refreshColoring(aItem);
//		}
//	}
}
