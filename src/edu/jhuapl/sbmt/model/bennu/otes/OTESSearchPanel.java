package edu.jhuapl.sbmt.model.bennu.otes;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.google.common.collect.Lists;

import edu.jhuapl.saavtk.gui.Renderer;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.saavtk.util.IdPair;
import edu.jhuapl.sbmt.client.SbmtInfoWindowManager;
import edu.jhuapl.sbmt.gui.spectrum.SpectrumSearchPanel;
import edu.jhuapl.sbmt.model.spectrum.SpectralInstrument;

public class OTESSearchPanel extends SpectrumSearchPanel
{

    public OTESSearchPanel(ModelManager modelManager,
            SbmtInfoWindowManager infoPanelManager, PickManager pickManager,
            Renderer renderer, SpectralInstrument instrument)
    {
        super(modelManager, infoPanelManager, pickManager, renderer, instrument);
        // TODO Auto-generated constructor stub


        setupComboBoxes();
    }

    @Override
    protected void setSpectrumSearchResults(List<List<String>> results)
    {

        spectrumResultsLabelText = results.size() + " spectra matched";
        resultsLabel.setText(spectrumResultsLabelText);

        List<String> matchedImages=Lists.newArrayList();
        for (List<String> res : results)
        {
            //String path = NisQuery.getNisPath(res);
            //matchedImages.add(path);

            String basePath=FilenameUtils.getPath(res.get(0));
            String filename=FilenameUtils.getBaseName(res.get(0));

            Path infoFile=Paths.get(basePath).resolveSibling("infofiles-corrected/"+filename+".INFO");
//            File file=FileCache.getFileFromServer("/"+infoFile.toString());

            matchedImages.add(FilenameUtils.getBaseName(infoFile.toString()));

        }


        spectrumRawResults = matchedImages;

        String[] formattedResults = new String[results.size()];

        // add the results to the list
        int i=0;
        for (String str : matchedImages)
        {
            //String fileNum=str.substring(9,str.length()-5);
            //System.out.println(fileNum);
//            String strippedFileName=str.replace("/NIS/2000/", "");
//            String detailedTime=nisFileToObservationzTimeMap.get(strippedFileName);
//            formattedResults[i] = new String(
 //                   fileNum
//                    + ", day: " + str.substring(10, 13) + "/" + str.substring(5, 9)+" ("+detailedTime+")"
//                    );
            formattedResults[i]=str;//FilenameUtils.getBaseName(str);
            ++i;
        }

        resultList.setListData(formattedResults);


        // Show the first set of footprints
        this.resultIntervalCurrentlyShown = new IdPair(0, Integer.parseInt((String)this.numberOfFootprintsComboBox.getSelectedItem()));
        this.showFootprints(resultIntervalCurrentlyShown);
    }

    @Override
    public String createSpectrumName(String currentSpectrumRaw)
    {
        return "/earth/osirisrex/otes/spectra/"+FilenameUtils.getBaseName(currentSpectrumRaw)+".cal";
    }


}
