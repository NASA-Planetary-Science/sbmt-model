package edu.jhuapl.sbmt.model.bennu.spectra.otes;

import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.search.BaseSpectrumSearchModel;

public class OTESSearchModel extends BaseSpectrumSearchModel
{
    public OTESSearchModel(ModelManager modelManager, BasicSpectrumInstrument instrument)
    {
        super(modelManager, instrument);

        getColoringModel().setRedMaxVal(0.000007);
        getColoringModel().setGreenMaxVal(0.000007);
        getColoringModel().setBlueMaxVal(0.000007);

        getColoringModel().setRedIndex(50);
        getColoringModel().setGreenIndex(100);
        getColoringModel().setBlueIndex(150);
    }
}