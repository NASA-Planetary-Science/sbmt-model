package edu.jhuapl.sbmt.model.bennu;

import java.util.Date;

import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.sbmt.client.SbmtInfoWindowManager;
import edu.jhuapl.sbmt.spectrum.controllers.SpectrumColoringController;
import edu.jhuapl.sbmt.spectrum.controllers.SpectrumResultsTableController;
import edu.jhuapl.sbmt.spectrum.model.core.AbstractSpectrumSearchModel;
import edu.jhuapl.sbmt.spectrum.model.core.ISpectralInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.ISpectrumSearchModel;
import edu.jhuapl.sbmt.spectrum.model.core.SpectraCollection;
import edu.jhuapl.sbmt.spectrum.ui.SpectrumSearchPanel;

public class OREXSpectrumHypertreeSearchController
{
    private ISpectrumSearchModel model;
    private SpectrumSearchPanel panel;
    protected ISpectralInstrument instrument;
    protected ModelManager modelManager;
    protected Renderer renderer;
    private SpectrumResultsTableController spectrumResultsTableController;
    private OREXSpectrumHypertreeSearchParametersController searchParametersController;
    private SpectrumColoringController coloringController;
    private AbstractSpectrumSearchModel spectrumSearchModel;


    public OREXSpectrumHypertreeSearchController(Date imageSearchDefaultStartDate, Date imageSearchDefaultEndDate, boolean hasHierarchicalSpectraSearch, double imageSearchDefaultMaxSpacecraftDistance, ModelManager modelManager,
            SbmtInfoWindowManager infoPanelManager,
            PickManager pickManager, Renderer renderer, ISpectralInstrument instrument, AbstractSpectrumSearchModel model)
    {
    	this.modelManager = modelManager;
        this.renderer = renderer;

        this.spectrumSearchModel = model;
        this.spectrumSearchModel.loadSearchSpecMetadata();
        SpectraCollection spectrumCollection = (SpectraCollection)modelManager.getModel(spectrumSearchModel.getSpectrumCollectionModelName());
        this.spectrumResultsTableController = new SpectrumResultsTableController(instrument, spectrumCollection, spectrumSearchModel, renderer, infoPanelManager);
        this.spectrumResultsTableController.setSpectrumResultsPanel();

        this.searchParametersController = new OREXSpectrumHypertreeSearchParametersController(imageSearchDefaultStartDate, imageSearchDefaultEndDate, hasHierarchicalSpectraSearch, imageSearchDefaultMaxSpacecraftDistance, spectrumSearchModel, pickManager);
        this.searchParametersController.setupSearchParametersPanel();

        this.coloringController = new SpectrumColoringController(model);

        init();
    }

    public void init()
    {
        panel = new SpectrumSearchPanel();
        panel.addSubPanel(searchParametersController.getPanel());
        panel.addSubPanel(spectrumResultsTableController.getPanel());
        panel.addSubPanel(coloringController.getPanel());

        panel.addAncestorListener(new AncestorListener()
		{

			@Override
			public void ancestorRemoved(AncestorEvent event)
			{
				spectrumResultsTableController.removeResultListener();

			}

			@Override
			public void ancestorMoved(AncestorEvent event)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void ancestorAdded(AncestorEvent event)
			{
				spectrumResultsTableController.addResultListener();
			}
		});
    }

    public JPanel getPanel()
    {
        return panel;
    }
}
