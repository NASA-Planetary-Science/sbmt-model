package edu.jhuapl.sbmt.model.bennu.spectra;

import java.util.Date;

import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.sbmt.client.SbmtInfoWindowManager;
import edu.jhuapl.sbmt.spectrum.controllers.standard.SpectrumColoringController;
import edu.jhuapl.sbmt.spectrum.controllers.standard.SpectrumResultsTableController;
import edu.jhuapl.sbmt.spectrum.controllers.standard.SpectrumSearchParametersController;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.search.BaseSpectrumSearchModel;
import edu.jhuapl.sbmt.spectrum.model.core.search.SpectraHierarchicalSearchSpecification;
import edu.jhuapl.sbmt.spectrum.rendering.SpectraCollection;
import edu.jhuapl.sbmt.spectrum.rendering.SpectrumBoundaryCollection;
import edu.jhuapl.sbmt.spectrum.ui.search.SpectrumSearchPanel;

public class OREXSpectrumSearchController<S extends BasicSpectrum>
{
    private SpectrumSearchPanel panel;
    protected SpectrumResultsTableController<S> spectrumResultsTableController;
    private SpectrumSearchParametersController searchParametersController;
    private SpectrumColoringController<S> coloringController;

    public OREXSpectrumSearchController(Date imageSearchDefaultStartDate, Date imageSearchDefaultEndDate,
    		boolean hasHierarchicalSpectraSearch, double imageSearchDefaultMaxSpacecraftDistance,
    		SpectraHierarchicalSearchSpecification spectraSpec,
    		ModelManager modelManager,
            SbmtInfoWindowManager infoPanelManager,
            PickManager pickManager, Renderer renderer, BasicSpectrumInstrument instrument, BaseSpectrumSearchModel<S> model)
    {
        SpectraCollection<S> spectrumCollection = (SpectraCollection<S>)modelManager.getModel(model.getSpectrumCollectionModelName());
        SpectrumBoundaryCollection<S> boundaryCollection = (SpectrumBoundaryCollection<S>)modelManager.getModel(model.getSpectrumBoundaryCollectionModelName());

        this.spectrumResultsTableController = new SpectrumResultsTableController<S>(instrument, spectrumCollection, modelManager, boundaryCollection, model, renderer, infoPanelManager);
        this.spectrumResultsTableController.setSpectrumResultsPanel();

        this.searchParametersController = new SpectrumSearchParametersController(imageSearchDefaultStartDate, imageSearchDefaultEndDate, hasHierarchicalSpectraSearch, imageSearchDefaultMaxSpacecraftDistance, spectraSpec, model, pickManager, modelManager);

        this.searchParametersController.setupSearchParametersPanel();

        this.coloringController = new SpectrumColoringController<S>(model, spectrumCollection, instrument.getRGBMaxVals(), instrument.getRGBDefaultIndices());

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
