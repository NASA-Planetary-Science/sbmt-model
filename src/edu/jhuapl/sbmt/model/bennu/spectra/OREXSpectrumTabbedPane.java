package edu.jhuapl.sbmt.model.bennu.spectra;

import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.client.SbmtInfoWindowManager;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.OTESSearchModel;
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.OVIRSSearchModel;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.SpectraTypeFactory;
import edu.jhuapl.sbmt.spectrum.model.core.interfaces.SpectrumAppearanceListener;
import edu.jhuapl.sbmt.spectrum.model.sbmtCore.spectra.SpectrumColoringStyle;
import edu.jhuapl.sbmt.spectrum.rendering.SpectraCollection;

/**
 * A tabbed panel with browse and search options
 * @author osheacm1
 *
 */
public class OREXSpectrumTabbedPane extends JTabbedPane
{
    public OREXSpectrumTabbedPane(
            BodyViewConfig smallBodyConfig,
            ModelManager modelManager,
            SbmtInfoWindowManager sbmtInfoWindowManager, PickManager pickManager,
            Renderer renderer, BasicSpectrumInstrument instrument, SpectraCollection spectrumCollection)
    {
        setBorder(BorderFactory.createEmptyBorder());
        if (instrument.getDisplayName().equals(SpectraTypeFactory.findSpectraTypeForDisplayName("OTES").getDisplayName())) {


            OTESSearchModel model = new OTESSearchModel(modelManager, instrument);
            model.addAppearanceChangedListener(new SpectrumAppearanceListener()
			{

				@Override
				public void spectrumFootprintVisbilityChanged(BasicSpectrum spectrum, boolean isVisible)
				{
					SpectrumColoringStyle style = SpectrumColoringStyle.getStyleForName(model.getColoringModel().getSpectrumColoringStyleName());
					try
					{
						spectrumCollection.addSpectrum(spectrum, style);
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				@Override
				public void spectrumBoundaryVisibilityChanged(BasicSpectrum spectrum, boolean isVisible)
				{
					// TODO Auto-generated method stub

				}
			});




            OREXSpectrumHypertreeSearchController controller =
            		new OREXSpectrumHypertreeSearchController(smallBodyConfig.imageSearchDefaultStartDate, smallBodyConfig.imageSearchDefaultEndDate,
            													smallBodyConfig.hasHierarchicalSpectraSearch, smallBodyConfig.imageSearchDefaultMaxSpacecraftDistance, smallBodyConfig.hierarchicalSpectraSearchSpecification, modelManager, sbmtInfoWindowManager, pickManager, renderer, instrument, model);

            JComponent component = new OREXSpectrumSearchController(
                    smallBodyConfig.imageSearchDefaultStartDate, smallBodyConfig.imageSearchDefaultEndDate,
                    smallBodyConfig.hasHierarchicalSpectraSearch, smallBodyConfig.imageSearchDefaultMaxSpacecraftDistance, smallBodyConfig.hierarchicalSpectraSearchSpecification,
                    modelManager, sbmtInfoWindowManager, pickManager, renderer, instrument, model).getPanel();



            addTab("Browse", component);
            if (smallBodyConfig.hasHypertreeBasedSpectraSearch)
            	addTab("Search", controller.getPanel());
        }
        else if (instrument.getDisplayName().equals(SpectraTypeFactory.findSpectraTypeForDisplayName("OVIRS").getDisplayName())) {

        	OVIRSSearchModel model = new OVIRSSearchModel(modelManager, instrument);
            model.addAppearanceChangedListener(new SpectrumAppearanceListener()
			{

				@Override
				public void spectrumFootprintVisbilityChanged(BasicSpectrum spectrum, boolean isVisible)
				{
					SpectrumColoringStyle style = SpectrumColoringStyle.getStyleForName(model.getColoringModel().getSpectrumColoringStyleName());
					try
					{
						spectrumCollection.addSpectrum(spectrum, style);
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

				@Override
				public void spectrumBoundaryVisibilityChanged(BasicSpectrum spectrum, boolean isVisible)
				{
					// TODO Auto-generated method stub

				}
			});

            OREXSpectrumHypertreeSearchController controller
            	= new OREXSpectrumHypertreeSearchController(smallBodyConfig.imageSearchDefaultStartDate, smallBodyConfig.imageSearchDefaultEndDate,
						smallBodyConfig.hasHierarchicalSpectraSearch, smallBodyConfig.imageSearchDefaultMaxSpacecraftDistance, smallBodyConfig.hierarchicalSpectraSearchSpecification, modelManager, sbmtInfoWindowManager, pickManager, renderer, instrument, model);

            JComponent component = new OREXSpectrumSearchController(smallBodyConfig.imageSearchDefaultStartDate, smallBodyConfig.imageSearchDefaultEndDate,
                    smallBodyConfig.hasHierarchicalSpectraSearch, smallBodyConfig.imageSearchDefaultMaxSpacecraftDistance, smallBodyConfig.hierarchicalSpectraSearchSpecification,
                    modelManager, sbmtInfoWindowManager, pickManager, renderer, instrument, model).getPanel();
            addTab("Browse", component);
            if (smallBodyConfig.hasHypertreeBasedSpectraSearch)
            	addTab("Search", controller.getPanel());
        }


    }
}