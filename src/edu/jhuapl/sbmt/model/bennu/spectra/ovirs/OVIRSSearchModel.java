package edu.jhuapl.sbmt.model.bennu.spectra.ovirs;


import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.search.BaseSpectrumSearchModel;

/**
 * OVIRS Search Model.  Small child class to give a concrete type to the BaseSpectrumSearchModel type.
 * @author steelrj1
 *
 */
public class OVIRSSearchModel extends BaseSpectrumSearchModel<OVIRSSpectrum>
{
    public OVIRSSearchModel(BasicSpectrumInstrument instrument)
    {
        super(instrument);
    }
}