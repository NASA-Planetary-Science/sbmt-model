package edu.jhuapl.sbmt.model.bennu.spectra;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.client.SbmtInfoWindowManager;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.OTESSearchModel;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.OTESSpectrum;
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.OVIRSSearchModel;
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.OVIRSSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.SpectraTypeFactory;
import edu.jhuapl.sbmt.spectrum.model.core.interfaces.SpectrumAppearanceListener;
import edu.jhuapl.sbmt.spectrum.rendering.SpectraCollection;

/**
 * A tabbed panel with browse and search options
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
        if (instrument.getDisplayName().equals(SpectraTypeFactory.findSpectraTypeForDisplayName("OTES").getDisplayName())) {


            OTESSearchModel model = new OTESSearchModel(modelManager, instrument);
            model.addAppearanceChangedListener(new SpectrumAppearanceListener<OTESSpectrum>()
			{

				@Override
				public void spectrumFootprintVisibilityChanged(OTESSpectrum spectrum, boolean isVisible)
				{
					spectrumCollection.addSpectrum((S)spectrum, false);
				}

				@Override
				public void spectrumBoundaryVisibilityChanged(OTESSpectrum spectrum, boolean isVisible)
				{
					// TODO Auto-generated method stub

				}
			});

            JComponent component = new OREXSpectrumSearchController<OTESSpectrum>(
                    smallBodyConfig.imageSearchDefaultStartDate, smallBodyConfig.imageSearchDefaultEndDate,
                    smallBodyConfig.hasHierarchicalSpectraSearch, smallBodyConfig.imageSearchDefaultMaxSpacecraftDistance, smallBodyConfig.hierarchicalSpectraSearchSpecification,
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

        	OVIRSSearchModel model = new OVIRSSearchModel(modelManager, instrument);
            model.addAppearanceChangedListener(new SpectrumAppearanceListener<OVIRSSpectrum>()
			{

				@Override
				public void spectrumFootprintVisibilityChanged(OVIRSSpectrum spectrum, boolean isVisible)
				{
					spectrumCollection.addSpectrum((S)spectrum, false);
//					spectrumCollection.setVisibility(spectrum, isVisible);
				}

				@Override
				public void spectrumBoundaryVisibilityChanged(OVIRSSpectrum spectrum, boolean isVisible)
				{

				}
			});

            JComponent component = new OREXSpectrumSearchController<OVIRSSpectrum>(smallBodyConfig.imageSearchDefaultStartDate, smallBodyConfig.imageSearchDefaultEndDate,
                    smallBodyConfig.hasHierarchicalSpectraSearch, smallBodyConfig.imageSearchDefaultMaxSpacecraftDistance, smallBodyConfig.hierarchicalSpectraSearchSpecification,
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