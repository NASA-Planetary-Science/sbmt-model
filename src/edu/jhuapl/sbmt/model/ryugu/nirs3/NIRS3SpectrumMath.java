package edu.jhuapl.sbmt.model.ryugu.nirs3;

import edu.jhuapl.sbmt.spectrum.model.sbmtCore.spectra.math.SpectrumMath;

/**
 * Contains pre-canned derived parameters for NIRS3 that are used when choosing bands to color
 * @author steelrj1
 *
 */
public class NIRS3SpectrumMath extends SpectrumMath
{
    private static NIRS3SpectrumMath spectrumMath=new NIRS3SpectrumMath();

    public static NIRS3SpectrumMath getInstance()
    {
        return spectrumMath;
    }

    private NIRS3SpectrumMath()
    {
        // TODO Auto-generated constructor stub
    }

    @Override
    public int getNumberOfBandsPerRawSpectrum()
    {
        return NIRS3.bandCentersLength;
    }

    /**
     * Precanned band math formulas
     */
    @Override
    public String[] getDerivedParameters()
    {
        return new String[]{};
    }

}
