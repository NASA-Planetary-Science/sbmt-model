package edu.jhuapl.sbmt.model.ryugu.nirs3;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.google.common.collect.Lists;

import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.sbmt.client.ISmallBodyViewConfig;
import edu.jhuapl.sbmt.client.SbmtInfoWindowManager;
import edu.jhuapl.sbmt.spectrum.model.core.AbstractSpectrumSearchModel;
import edu.jhuapl.sbmt.spectrum.model.core.ISpectralInstrument;

import crucible.crust.metadata.api.Metadata;

public class NIRS3SearchModel extends AbstractSpectrumSearchModel
{
	String fileExtension = "";

    public NIRS3SearchModel(ISmallBodyViewConfig smallBodyConfig,
            ModelManager modelManager, SbmtInfoWindowManager infoPanelManager,
            PickManager pickManager, Renderer renderer,
            ISpectralInstrument instrument)
    {
        super(smallBodyConfig.hasHierarchicalSpectraSearch(), smallBodyConfig.hasHypertreeBasedSpectraSearch(),
        		smallBodyConfig.getHierarchicalSpectraSearchSpecification(), modelManager, pickManager,
                renderer, instrument);

        setRedMaxVal(0.00005);
        setGreenMaxVal(0.0001);
        setBlueMaxVal(0.002);

        setRedIndex(100);
        setGreenIndex(70);
        setBlueIndex(40);
    }

    @Override
    public void setSpectrumRawResults(List<List<String>> spectrumRawResults)
    {


        List<String> matchedImages=Lists.newArrayList();
        if (matchedImages.size() > 0)
            fileExtension = FilenameUtils.getExtension(matchedImages.get(0));
        for (List<String> res : results)
        {
            String basePath=FilenameUtils.getPath(res.get(0));
            String filename=FilenameUtils.getBaseName(res.get(0));

            Path infoFile=Paths.get(basePath).resolveSibling("infofiles-corrected/"+filename+".INFO");

            matchedImages.add(FilenameUtils.getBaseName(infoFile.toString()));
        }

        String[] formattedResults = new String[results.size()];

        // add the results to the list
        int i=0;
        for (String str : matchedImages)
        {
            formattedResults[i]=str;
            ++i;
        }

        for (String res : formattedResults)
        {
            List<String> result = new ArrayList<String>();
            result.add(res);
            this.results.add(result);
        }
    }

    @Override
    public String createSpectrumName(int index)
    {
        return getSpectrumRawResults().get(index).get(0);
//        return "/earth/hayabusa2/nirs3/spectra/"+FilenameUtils.getBaseName(currentSpectrumRaw)+".spect";
    }

    @Override
    public void populateSpectrumMetadata(String line)
    {
//        SpectraCollection collection = (SpectraCollection)getModelManager().getModel(ModelNames.SPECTRA);
//        for (int i=0; i<lines.size(); ++i)
//        {
//            OREXSearchSpec spectrumSpec = new OREXSearchSpec();
//            spectrumSpec.fromFile(lines.get(0));
//            collection.tagSpectraWithMetadata(createSpectrumName(i), spectrumSpec);
//        }
    }

	@Override
	public Metadata store()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void retrieve(Metadata source)
	{
		// TODO Auto-generated method stub

	}

}
