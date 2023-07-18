package edu.jhuapl.sbmt.model.bennu.spectra.otes;

import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.search.BaseSpectrumSearchModel;

/**
 * OTES Search Model.  Small child class to give a concrete type to the BaseSpectrumSearchModel type.
 * @author steelrj1
 *
 */
public class OTESSearchModel extends BaseSpectrumSearchModel<OTESSpectrum>
{
    public OTESSearchModel(BasicSpectrumInstrument instrument)
    {
        super(instrument);
    }
}