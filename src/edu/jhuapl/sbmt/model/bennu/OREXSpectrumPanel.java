package edu.jhuapl.sbmt.model.bennu;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.client.SbmtInfoWindowManager;
import edu.jhuapl.sbmt.model.bennu.otes.OTESSearchModel;
import edu.jhuapl.sbmt.model.bennu.otes.OTESSearchPanel;
import edu.jhuapl.sbmt.model.bennu.ovirs.OVIRSSearchModel;
import edu.jhuapl.sbmt.model.bennu.ovirs.OVIRSSearchPanel;
import edu.jhuapl.sbmt.spectrum.model.core.ISpectralInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.SpectraTypeFactory;

/**
 * A tabbed panel with browse and search options
 * @author osheacm1
 *
 */
public class OREXSpectrumPanel extends JTabbedPane
{
    public OREXSpectrumPanel(
            BodyViewConfig smallBodyConfig,
            ModelManager modelManager,
            SbmtInfoWindowManager sbmtInfoWindowManager, PickManager pickManager,
            Renderer renderer, ISpectralInstrument instrument)
    {
        setBorder(BorderFactory.createEmptyBorder());

        if (instrument.getDisplayName().equals(SpectraTypeFactory.findSpectraTypeForDisplayName("OTES").getDisplayName())) {

            OTESSearchModel model = new OTESSearchModel(smallBodyConfig, modelManager, pickManager, renderer, instrument);

            OREXSpectrumHypertreeSearchController controller =
            		new OREXSpectrumHypertreeSearchController(smallBodyConfig.imageSearchDefaultStartDate, smallBodyConfig.imageSearchDefaultEndDate,
            													smallBodyConfig.hasHierarchicalSpectraSearch, smallBodyConfig.imageSearchDefaultMaxSpacecraftDistance, modelManager, sbmtInfoWindowManager, pickManager, renderer, instrument, model);

            JComponent component = new OTESSearchPanel(
                    smallBodyConfig, modelManager, sbmtInfoWindowManager, pickManager, renderer, instrument, true).getView();



            addTab("Browse", component);
            if (smallBodyConfig.hasHypertreeBasedSpectraSearch)
            	addTab("Search", controller.getPanel());
        }
        else if (instrument.getDisplayName().equals(SpectraTypeFactory.findSpectraTypeForDisplayName("OVIRS").getDisplayName())) {

        	OVIRSSearchModel model = new OVIRSSearchModel(smallBodyConfig, modelManager, pickManager, renderer, instrument);

            OREXSpectrumHypertreeSearchController controller
            	= new OREXSpectrumHypertreeSearchController(smallBodyConfig.imageSearchDefaultStartDate, smallBodyConfig.imageSearchDefaultEndDate,
						smallBodyConfig.hasHierarchicalSpectraSearch, smallBodyConfig.imageSearchDefaultMaxSpacecraftDistance, modelManager, sbmtInfoWindowManager, pickManager, renderer, instrument, model);

            JComponent component = new OVIRSSearchPanel(
                    smallBodyConfig, modelManager, sbmtInfoWindowManager, pickManager, renderer, instrument, true).getView();
            addTab("Browse", component);
            if (smallBodyConfig.hasHypertreeBasedSpectraSearch)
            	addTab("Search", controller.getPanel());
        }


    }
}