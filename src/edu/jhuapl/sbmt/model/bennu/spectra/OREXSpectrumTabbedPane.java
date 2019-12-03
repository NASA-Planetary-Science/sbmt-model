package edu.jhuapl.sbmt.model.bennu.spectra;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.client.SbmtInfoWindowManager;
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
        //TODO I think these 2 blocks can be collapsed, since there is no difference here for OTES and OVIRS

        if (instrument.getDisplayName().equals(SpectraTypeFactory.findSpectraTypeForDisplayName("OTES").getDisplayName())) {


            BaseSpectrumSearchModel<S> model = new BaseSpectrumSearchModel<S>(modelManager, instrument);
            JComponent component = new OREXSpectrumSearchController<S>(
                    smallBodyConfig.imageSearchDefaultStartDate, smallBodyConfig.imageSearchDefaultEndDate,
                    smallBodyConfig.hasHierarchicalSpectraSearch, smallBodyConfig.imageSearchDefaultMaxSpacecraftDistance, smallBodyConfig.hierarchicalSpectraSearchSpecification.clone(),
                    modelManager, sbmtInfoWindowManager, pickManager, renderer, instrument, model).getPanel();



            addTab("Browse", component);
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

        	BaseSpectrumSearchModel<S> model = new BaseSpectrumSearchModel<S>(modelManager, instrument);
            JComponent component = new OREXSpectrumSearchController<S>(smallBodyConfig.imageSearchDefaultStartDate, smallBodyConfig.imageSearchDefaultEndDate,
                    smallBodyConfig.hasHierarchicalSpectraSearch, smallBodyConfig.imageSearchDefaultMaxSpacecraftDistance, smallBodyConfig.hierarchicalSpectraSearchSpecification.clone(),
                    modelManager, sbmtInfoWindowManager, pickManager, renderer, instrument, model).getPanel();
            addTab("Browse", component);
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