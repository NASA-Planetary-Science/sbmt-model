package edu.jhuapl.sbmt.model.bennu.spectra;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.sbmt.core.body.BodyViewConfig;
import edu.jhuapl.sbmt.image.model.SbmtInfoWindowManager;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.OTESSearchModel;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.OTESSpectrum;
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.OVIRSSearchModel;
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.OVIRSSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.SpectraTypeFactory;
import edu.jhuapl.sbmt.spectrum.model.core.search.BaseSpectrumSearchModel;
import edu.jhuapl.sbmt.spectrum.rendering.SpectraCollection;

/**
 * A tabbed panel with browse and search options for OREX.  Contains Browse and Search tabs (once enabled)
 * @author osheacm1
 *
 */
public class OREXSpectrumTabbedPane<S extends BasicSpectrum> extends JTabbedPane
{
    public OREXSpectrumTabbedPane(
            BodyViewConfig smallBodyConfig,
            ModelManager modelManager,
            SbmtInfoWindowManager sbmtInfoWindowManager, PickManager pickManager,
            Renderer renderer, BasicSpectrumInstrument instrument, SpectraCollection<S> spectrumCollection)
    {
        setBorder(BorderFactory.createEmptyBorder());

        BaseSpectrumSearchModel<S> model = new BaseSpectrumSearchModel<S>(modelManager, instrument);
        JComponent component = new OREXSpectrumSearchController<S>(
                smallBodyConfig.imageSearchDefaultStartDate, smallBodyConfig.imageSearchDefaultEndDate,
                smallBodyConfig.hasHierarchicalSpectraSearch, smallBodyConfig.hasHypertreeBasedSpectraSearch,
                smallBodyConfig.imageSearchDefaultMaxSpacecraftDistance, null,
                smallBodyConfig.hierarchicalSpectraSearchSpecification.clone(),
                modelManager, sbmtInfoWindowManager, pickManager, renderer, instrument, model).getPanel();

        addTab("Browse", component);

        if (instrument.getDisplayName().equals(SpectraTypeFactory.findSpectraTypeForDisplayName("OTES").getDisplayName())) {

            if (smallBodyConfig.hasHypertreeBasedSpectraSearch)
            {
    			OTESSearchModel model2 = new OTESSearchModel(modelManager, instrument);
    			JComponent component2 = new OREXSpectrumSearchController<OTESSpectrum>(smallBodyConfig.imageSearchDefaultStartDate, smallBodyConfig.imageSearchDefaultEndDate,
    					false, smallBodyConfig.hasHypertreeBasedSpectraSearch, smallBodyConfig.imageSearchDefaultMaxSpacecraftDistance, new String[] {"L2", "L3"},
    					smallBodyConfig.hierarchicalSpectraSearchSpecification,
    					modelManager, sbmtInfoWindowManager, pickManager, renderer, instrument, model2).getPanel();
    			addTab("Search", component2);

            }

//            if (smallBodyConfig.hasHypertreeBasedSpectraSearch)
//            {
//            	OREXSpectrumHypertreeSearchController controller =
//                		new OREXSpectrumHypertreeSearchController(smallBodyConfig.imageSearchDefaultStartDate, smallBodyConfig.imageSearchDefaultEndDate,
//                													smallBodyConfig.hasHierarchicalSpectraSearch, smallBodyConfig.imageSearchDefaultMaxSpacecraftDistance, smallBodyConfig.hierarchicalSpectraSearchSpecification, modelManager, sbmtInfoWindowManager, pickManager, renderer, instrument, model);
//
//            	addTab("Search", controller.getPanel());
//            }
        }
        else if (instrument.getDisplayName().equals(SpectraTypeFactory.findSpectraTypeForDisplayName("OVIRS").getDisplayName())) {

            if (smallBodyConfig.hasHypertreeBasedSpectraSearch)
            {
    			OVIRSSearchModel model3 = new OVIRSSearchModel(modelManager, instrument);
    			JComponent component3 = new OREXSpectrumSearchController<OVIRSSpectrum>(smallBodyConfig.imageSearchDefaultStartDate, smallBodyConfig.imageSearchDefaultEndDate,
    					false, smallBodyConfig.hasHypertreeBasedSpectraSearch, smallBodyConfig.imageSearchDefaultMaxSpacecraftDistance, new String[] {"SA16l3escireff", "SA27l3csci", "SA29l3esciradf"},
    					smallBodyConfig.hierarchicalSpectraSearchSpecification,
    					modelManager, sbmtInfoWindowManager, pickManager, renderer, instrument, model3).getPanel();
    			addTab("Search", component3);

            }
//            if (smallBodyConfig.hasHypertreeBasedSpectraSearch)
//            {
//            	OREXSpectrumHypertreeSearchController controller
//             		= new OREXSpectrumHypertreeSearchController(smallBodyConfig.imageSearchDefaultStartDate, smallBodyConfig.imageSearchDefaultEndDate,
// 						smallBodyConfig.hasHierarchicalSpectraSearch, smallBodyConfig.imageSearchDefaultMaxSpacecraftDistance, smallBodyConfig.hierarchicalSpectraSearchSpecification, modelManager, sbmtInfoWindowManager, pickManager, renderer, instrument, model);
//
//            	addTab("Search", controller.getPanel());
//            }
        }


    }
}