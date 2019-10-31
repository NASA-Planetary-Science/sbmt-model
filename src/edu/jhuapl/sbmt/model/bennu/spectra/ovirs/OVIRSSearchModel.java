package edu.jhuapl.sbmt.model.bennu.spectra.ovirs;


import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.search.BaseSpectrumSearchModel;

public class OVIRSSearchModel extends BaseSpectrumSearchModel<OVIRSSpectrum>
{
    public OVIRSSearchModel(ModelManager modelManager, BasicSpectrumInstrument instrument)
    {
        super(modelManager, instrument);
    }
}