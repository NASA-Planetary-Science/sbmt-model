package edu.jhuapl.sbmt.model.bennu.spectra.otes;

import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.search.BaseSpectrumSearchModel;

public class OTESSearchModel extends BaseSpectrumSearchModel<OTESSpectrum>
{
    public OTESSearchModel(ModelManager modelManager, BasicSpectrumInstrument instrument)
    {
        super(modelManager, instrument);
    }
}