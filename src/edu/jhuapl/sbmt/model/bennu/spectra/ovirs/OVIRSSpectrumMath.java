package edu.jhuapl.sbmt.model.bennu.spectra.ovirs;

import edu.jhuapl.sbmt.spectrum.model.sbmtCore.spectra.math.SpectrumMath;

/**
 * Contains pre-canned derived parameters for OVIRS that are used when choosing bands to color
 * @author steelrj1
 *
 */
public class OVIRSSpectrumMath extends SpectrumMath
{
    private static OVIRSSpectrumMath spectrumMath=new OVIRSSpectrumMath();

    public static OVIRSSpectrumMath getInstance()
    {
        return spectrumMath;
    }

    private OVIRSSpectrumMath()
    {
        // TODO Auto-generated constructor stub
    }

    @Override
    public int getNumberOfBandsPerRawSpectrum()
    {
        return OVIRS.bandCentersLength;
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
