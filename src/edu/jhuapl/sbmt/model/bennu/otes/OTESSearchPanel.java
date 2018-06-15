package edu.jhuapl.sbmt.model.bennu.otes;

import java.text.DecimalFormat;
import java.util.List;

import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.SpinnerNumberModel;

import org.apache.commons.io.FilenameUtils;

import com.google.common.collect.Lists;

import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.saavtk.util.IdPair;
import edu.jhuapl.sbmt.client.SbmtInfoWindowManager;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.gui.spectrum.SpectrumSearchController;
import edu.jhuapl.sbmt.model.spectrum.SpectralInstrument;

public class OTESSearchPanel extends SpectrumSearchController
{
    String fileExtension = "";

    public OTESSearchPanel(SmallBodyViewConfig smallBodyConfig, ModelManager modelManager,
            SbmtInfoWindowManager infoPanelManager, PickManager pickManager,
            Renderer renderer, SpectralInstrument instrument)
    {
        super(smallBodyConfig, modelManager, infoPanelManager, pickManager, renderer, instrument);

        setupComboBoxes();
        setColoringComboBox();

        List<JSpinner> spinners=Lists.newArrayList(view.getBlueMaxSpinner(), view.getBlueMinSpinner(), view.getRedMaxSpinner(), view.getRedMinSpinner(),
                view.getGreenMaxSpinner(), view.getGreenMinSpinner());

        for (JSpinner spinner : spinners)
        {
            spinner.setModel(new SpinnerNumberModel(Double.valueOf(0.0d), null, null, Double.valueOf(0.0000001d)));
            NumberEditor editor = (NumberEditor)spinner.getEditor();
            DecimalFormat format = editor.getFormat();
            format.setMinimumFractionDigits(8);
        }

        view.getRedMaxSpinner().setValue(0.000001);
        view.getGreenMaxSpinner().setValue(0.000001);
        view.getBlueMaxSpinner().setValue(0.000001);

        view.getRedComboBox().setSelectedIndex(50);
        view.getGreenComboBox().setSelectedIndex(100);
        view.getBlueComboBox().setSelectedIndex(150);
    }

    @Override
    protected void setSpectrumSearchResults(List<List<String>> results)
    {
        view.getResultsLabel().setText(results.size() + " spectra matched");

        List<String> matchedImages=Lists.newArrayList();
        if (matchedImages.size() > 0)
        fileExtension = FilenameUtils.getExtension(matchedImages.get(0));
        for (List<String> res : results)
        {
            String basePath=FilenameUtils.getPath(res.get(0));
            String filename=FilenameUtils.getBaseName(res.get(0));
//            Path infoFile=Paths.get(basePath).resolveSibling("infofiles-corrected/"+filename+".INFO");

            matchedImages.add(basePath + filename + "." + FilenameUtils.getExtension(res.get(0)));
//            matchedImages.add(FilenameUtils.getBaseName(infoFile.toString()));
        }

        model.setSpectrumRawResults(matchedImages);

        String[] formattedResults = new String[results.size()];

        // add the results to the list
        int i=0;
        for (String str : matchedImages)
        {
            formattedResults[i]=FilenameUtils.getBaseName(str) + "." + FilenameUtils.getExtension(str);
            ++i;
        }

        view.getResultList().setListData(formattedResults);

        // Show the first set of footprints
        model.setResultIntervalCurrentlyShown(new IdPair(0, Integer.parseInt((String)view.getNumberOfFootprintsComboBox().getSelectedItem())));
        this.showFootprints(model.getResultIntervalCurrentlyShown());
    }

    @Override
    public String createSpectrumName(int index)
    {
        return model.getSpectrumRawResults().get(index); // + fileExtension;
//        System.out.println("OTESSearchPanel: createSpectrumName: " + currentSpectrumRaw);
//        return "/earth/osirisrex/otes/spectra/"+currentSpectrumRaw+".spect";
    }
}
