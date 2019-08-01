package edu.jhuapl.sbmt.model.bennu.spectra.ovirs;

import edu.jhuapl.sbmt.spectrum.model.sbmtCore.spectra.math.SpectrumMath;

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

    @Override
    public String[] getDerivedParameters()
    {
        return new String[]{};
    }

}
