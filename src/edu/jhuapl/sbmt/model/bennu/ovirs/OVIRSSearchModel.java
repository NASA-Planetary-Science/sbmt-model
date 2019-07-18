package edu.jhuapl.sbmt.model.bennu.ovirs;

import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.google.common.collect.Lists;

import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.model.ModelNames;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.model.bennu.SpectrumSearchSpec;
import edu.jhuapl.sbmt.spectrum.model.core.AbstractSpectrumSearchModel;
import edu.jhuapl.sbmt.spectrum.model.core.ISpectralInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.SpectraCollection;

import crucible.crust.metadata.api.Metadata;

public class OVIRSSearchModel extends AbstractSpectrumSearchModel
{
    String fileExtension = "";

    public OVIRSSearchModel(BodyViewConfig smallBodyConfig, ModelManager modelManager,
            PickManager pickManager,
            Renderer renderer, ISpectralInstrument instrument)
    {
        super(smallBodyConfig.hasHierarchicalSpectraSearch, smallBodyConfig.hasHypertreeBasedSpectraSearch,
        		smallBodyConfig.hierarchicalSpectraSearchSpecification, modelManager, pickManager,
                renderer, instrument);

        setRedMaxVal(0.00005);
        setGreenMaxVal(0.0001);
        setBlueMaxVal(0.002);

        setRedIndex(736);
        setGreenIndex(500);
        setBlueIndex(50);
    }

    @Override
    public void setSpectrumRawResults(List<List<String>> spectrumRawResults)
    {
        //TODO This need to really be shifted to use classes and not string representation until the end
        List<String> matchedImages=Lists.newArrayList();
        if (matchedImages.size() > 0)
            fileExtension = FilenameUtils.getExtension(matchedImages.get(0));
        super.setSpectrumRawResults(spectrumRawResults);
//        fireResultsChanged();
//        fireResultsCountChanged(this.results.size());
    }

    @Override
    public String createSpectrumName(int index)
    {
        return getSpectrumRawResults().get(index).get(0);
    }

    @Override
    public void populateSpectrumMetadata(String line)
    {
        SpectraCollection collection = (SpectraCollection)getModelManager().getModel(ModelNames.SPECTRA);
        for (int i=0; i<results.size(); ++i)
        {
            SpectrumSearchSpec spectrumSpec = new SpectrumSearchSpec();
            spectrumSpec.fromFile(line);
            collection.tagSpectraWithMetadata(createSpectrumName(i), spectrumSpec);
        }
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