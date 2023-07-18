package edu.jhuapl.sbmt.model.ryugu.nirs3;

import edu.jhuapl.sbmt.model.ryugu.nirs3.atRyugu.NIRS3Spectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.search.BaseSpectrumSearchModel;

/**
 * NIS Search Model. Small child class to give a concrete type to the
 * BaseSpectrumSearchModel type.
 *
 * @author steelrj1
 *
 */
public class NIRS3SearchModel extends BaseSpectrumSearchModel<NIRS3Spectrum>
{
	public NIRS3SearchModel(BasicSpectrumInstrument instrument)
	{
		super(instrument);
	}
}
