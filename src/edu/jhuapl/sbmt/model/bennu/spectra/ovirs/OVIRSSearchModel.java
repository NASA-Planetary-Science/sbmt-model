package edu.jhuapl.sbmt.model.bennu.spectra.ovirs;


import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.sbmt.spectrum.model.core.search.BaseSpectrumSearchModel;
import edu.jhuapl.sbmt.spectrum.model.sbmtCore.spectra.ISpectralInstrument;

public class OVIRSSearchModel extends BaseSpectrumSearchModel
{
    public OVIRSSearchModel(ModelManager modelManager, ISpectralInstrument instrument)
    {
        super(modelManager, instrument);

        getColoringModel().setRedMaxVal(0.00005);
        getColoringModel().setGreenMaxVal(0.0001);
        getColoringModel().setBlueMaxVal(0.002);

        getColoringModel().setRedIndex(736);
        getColoringModel().setGreenIndex(500);
        getColoringModel().setBlueIndex(50);
    }
}