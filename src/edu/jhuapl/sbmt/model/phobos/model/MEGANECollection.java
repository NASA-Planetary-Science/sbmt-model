package edu.jhuapl.sbmt.model.phobos.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;

import javax.swing.SwingUtilities;

import com.beust.jcommander.internal.Lists;

import vtk.vtkProp;

import edu.jhuapl.saavtk.model.SaavtkItemManager;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.phobos.render.MEGANEFootprintRenderer;

import crucible.crust.logging.SimpleLogger;

public class MEGANECollection extends SaavtkItemManager<MEGANEFootprint> implements PropertyChangeListener
{
	private List<MEGANEFootprint> footprints;
	private SmallBodyModel smallBodyModel;
	private HashMap<MEGANEFootprint, MEGANEFootprintRenderer> footprintRenderers;
	private SimpleLogger logger = SimpleLogger.getInstance();

	public MEGANECollection(SmallBodyModel smallBodyModel)
	{
		logger.setLogFormat("%1$tF %1$tT.%1$tL %4$-7s %2$s %5$s%6$s%n");
		this.smallBodyModel = smallBodyModel;
		this.footprintRenderers = new HashMap<MEGANEFootprint, MEGANEFootprintRenderer>();
	}

	public MEGANECollection(List<MEGANEFootprint> footprints, SmallBodyModel smallBodyModel)
	{
		this(smallBodyModel);
		this.footprints = footprints;
	}

	public void setFootprints(List<MEGANEFootprint> footprints)
	{
		this.footprints = footprints;
//		for (MEGANEFootprint footprint : footprints)
//		{
//			if (footprintRenderers.get(footprint) == null)
//			{
//				Thread thread = new Thread(new Runnable()
//				{
//
//					@Override
//					public void run()
//					{
//						footprintRenderers.put(footprint, new MEGANEFootprintRenderer(footprint, smallBodyModel, MEGANECollection.this.pcs));
//						footprint.setStatus("Loaded");
//					}
//				});
//				thread.start();
//			}
//		}
		setAllItems(footprints);
	}

	@Override
	public synchronized List<vtkProp> getProps()
	{
		List<vtkProp> props = Lists.newArrayList();
		if (footprints == null) return props;
//		List<vtkProp> fpProps = footprintRenderers.values().stream().map( fp -> fp.getProps()).toList();
		for (MEGANEFootprintRenderer renderer : footprintRenderers.values())
		{
			props.add(renderer.getProps());
		}
		return props;
	}

	public void setFootprintMapped(MEGANEFootprint footprint, boolean isMapped)
	{
		footprint.setMapped(isMapped);
		if (footprintRenderers.get(footprint) == null)
		{
			Thread thread = new Thread(new Runnable()
			{

				@Override
				public void run()
				{
					footprint.setStatus("Loading...");
					MEGANEFootprintRenderer renderer = new MEGANEFootprintRenderer(footprint, smallBodyModel, MEGANECollection.this.pcs);
					footprintRenderers.put(footprint, renderer);
					renderer.getProps().SetVisibility(isMapped ? 1 : 0);
					footprint.setStatus("Loaded");
					SwingUtilities.invokeLater( () -> {
						renderer.shiftFootprint();
						pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
					});
				}
			});
			thread.start();
		}
		else
			this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
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

}
