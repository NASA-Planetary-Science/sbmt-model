package edu.jhuapl.sbmt.model.phobos.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.phobos.controllers.MEGANEController.MEGANEDatabaseConnection;
import edu.jhuapl.sbmt.model.phobos.render.MEGANEFootprintRenderer;

import crucible.crust.logging.SimpleLogger;
import glum.item.ItemEventType;

public class CumulativeMEGANECollection extends SaavtkItemManager<CumulativeMEGANEFootprint> implements PropertyChangeListener
{
	private List<CumulativeMEGANEFootprint> cumulativeFootprints;
	private SmallBodyModel smallBodyModel;
	private HashMap<CumulativeMEGANEFootprint, MEGANEFootprintRenderer> cumulativeFootprintRenderers;
	private MEGANEDatabaseConnection dbConnection;
	private SimpleLogger logger = SimpleLogger.getInstance();
	private Renderer renderer;
	private Map<CumulativeMEGANEFootprint, MEGANEFootprintRenderProperties> propM;


	public CumulativeMEGANECollection(SmallBodyModel smallBodyModel)
	{
		logger.setLogFormat("%1$tF %1$tT.%1$tL %4$-7s %2$s %5$s%6$s%n");
		this.smallBodyModel = smallBodyModel;
		this.cumulativeFootprints = Lists.newArrayList();
		this.cumulativeFootprintRenderers = new HashMap<CumulativeMEGANEFootprint, MEGANEFootprintRenderer>();
	}

	public CumulativeMEGANECollection(List<CumulativeMEGANEFootprint> footprints, SmallBodyModel smallBodyModel)
	{
		this(smallBodyModel);
		this.cumulativeFootprints = footprints;
	}

	public void setFootprints(List<CumulativeMEGANEFootprint> footprints)
	{
		this.cumulativeFootprints = footprints;
		setAllItems(footprints);
	}

	public void addCumulativeFootprint(CumulativeMEGANEFootprint footprint)
	{
		cumulativeFootprints.add(footprint);
		setAllItems(cumulativeFootprints);
	}

	@Override
	public void setAllItems(Collection<CumulativeMEGANEFootprint> aItemC)
	{
		// Clear relevant state vars
		propM = new HashMap<>();
		// Setup the initial props for all the items
		for (CumulativeMEGANEFootprint aItem : aItemC)
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
		if (cumulativeFootprints == null) return props;
		for (MEGANEFootprintRenderer renderer : cumulativeFootprintRenderers.values())
		{
			props.add(renderer.getProps());
		}
		return props;
	}

	public void setFootprintMapped(CumulativeMEGANEFootprint footprint, boolean isMapped)
	{
		footprint.setMapped(isMapped);
		propM.get(footprint).isVisible = isMapped;
		if (cumulativeFootprintRenderers.get(footprint) == null)
		{
			Thread thread = new Thread(new Runnable()
			{

				@Override
				public void run()
				{
					footprint.setStatus("Loading...");
					MEGANEFootprintRenderer renderer = new MEGANEFootprintRenderer(footprint, smallBodyModel, CumulativeMEGANECollection.this.pcs);
					cumulativeFootprintRenderers.put(footprint, renderer);
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

	public void setRenderer(Renderer renderer)
	{
		this.renderer = renderer;
	}

	public Renderer getRender()
	{
		return renderer;
	}

	public void saveSelectedToFile(File file, HashMap<String, List<String>> metadata) throws IOException
	{
		ImmutableSet<CumulativeMEGANEFootprint> selectedFootprints = getSelectedItems();
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write("## Saved Footprints file.  Search parameters:");
		writer.newLine();
		for (String key : metadata.keySet())
		{
			writer.write("##" + key + " = " + metadata.get(key));
			writer.newLine();
		}
		writer.write("Date (ET), Center Lat (deg), Center Lon (deg), Altitude (km), Normalized Altitude");
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
		for (CumulativeMEGANEFootprint aItem : getAllItems())
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
	public void refreshColoring(CumulativeMEGANEFootprint segment)
	{
		MEGANEFootprintRenderer renderer = cumulativeFootprintRenderers.get(segment);
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
}
