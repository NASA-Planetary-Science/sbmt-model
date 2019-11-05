package edu.jhuapl.sbmt.model.eros.nis;

import edu.jhuapl.sbmt.spectrum.model.sbmtCore.spectra.math.SpectrumMath;

/**
 * Contains pre-canned derived parameters for NIS that are used when choosing bands to color
 * @author steelrj1
 *
 */
public class NISSpectrumMath extends SpectrumMath
{

    private final static NISSpectrumMath spectrumMath=new NISSpectrumMath();

    public static NISSpectrumMath getSpectrumMath()
    {
        return spectrumMath;
    }

    private NISSpectrumMath()
    {

    }

    /**
     * Precanned band math formulas
     */
    static final public String[] derivedParameters = {
        "B36 - B05",
        "B01 - B05",
        "B52 - B36"
    };

    @Override
    public int getNumberOfBandsPerRawSpectrum()
    {
        return NIS.bandCentersLength;
    }

    @Override
    public String[] getDerivedParameters()
    {
        return derivedParameters;
    }




}
