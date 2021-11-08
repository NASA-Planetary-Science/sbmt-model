package edu.jhuapl.sbmt.model.bennu.spectra;

import java.util.Date;

import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.sbmt.image.SbmtInfoWindowManager;
import edu.jhuapl.sbmt.spectrum.controllers.standard.SpectrumColoringController;
import edu.jhuapl.sbmt.spectrum.controllers.standard.SpectrumResultsTableController;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.interfaces.ISpectrumSearchModel;
import edu.jhuapl.sbmt.spectrum.model.core.search.BaseSpectrumSearchModel;
import edu.jhuapl.sbmt.spectrum.model.core.search.SpectraHierarchicalSearchSpecification;
import edu.jhuapl.sbmt.spectrum.rendering.SpectraCollection;
import edu.jhuapl.sbmt.spectrum.rendering.SpectrumBoundaryCollection;
import edu.jhuapl.sbmt.spectrum.ui.search.SpectrumSearchPanel;

public class OREXSpectrumHypertreeSearchController
{
    private ISpectrumSearchModel model;
    private SpectrumSearchPanel panel;
    protected BasicSpectrumInstrument instrument;
    protected ModelManager modelManager;
    protected Renderer renderer;
    private SpectrumResultsTableController spectrumResultsTableController;
    private OREXSpectrumHypertreeSearchParametersController searchParametersController;
    private SpectrumColoringController coloringController;
    private BaseSpectrumSearchModel spectrumSearchModel;
    protected SpectraHierarchicalSearchSpecification spectraSpec;


    public OREXSpectrumHypertreeSearchController(Date imageSearchDefaultStartDate, Date imageSearchDefaultEndDate,
    		boolean hasHierarchicalSpectraSearch, double imageSearchDefaultMaxSpacecraftDistance, SpectraHierarchicalSearchSpecification spectraSpec,

    		ModelManager modelManager, SbmtInfoWindowManager infoPanelManager,
            PickManager pickManager, Renderer renderer, BasicSpectrumInstrument instrument, BaseSpectrumSearchModel model,
            double[] rgbMaxVals, int[] rgbIndices)
    {
    	this.modelManager = modelManager;
        this.renderer = renderer;

        this.spectrumSearchModel = model;
//        this.spectrumSearchModel.loadSearchSpecMetadata();
        SpectraCollection spectrumCollection = (SpectraCollection)modelManager.getModel(spectrumSearchModel.getSpectrumCollectionModelName()).get(0);
        SpectrumBoundaryCollection boundaryCollection = (SpectrumBoundaryCollection)modelManager.getModel(spectrumSearchModel.getSpectrumBoundaryCollectionModelName()).get(0);
        this.spectrumResultsTableController = new SpectrumResultsTableController(instrument, spectrumCollection, modelManager, boundaryCollection, spectrumSearchModel, renderer, infoPanelManager);
        this.spectrumResultsTableController.setSpectrumResultsPanel();

        this.searchParametersController = new OREXSpectrumHypertreeSearchParametersController(imageSearchDefaultStartDate, imageSearchDefaultEndDate, hasHierarchicalSpectraSearch, imageSearchDefaultMaxSpacecraftDistance, spectraSpec, spectrumSearchModel, pickManager, modelManager);
        this.searchParametersController.setupSearchParametersPanel();

        this.coloringController = new SpectrumColoringController(model, spectrumCollection, rgbMaxVals, rgbIndices);

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
