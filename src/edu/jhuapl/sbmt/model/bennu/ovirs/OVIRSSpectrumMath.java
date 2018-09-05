package edu.jhuapl.sbmt.model.bennu.ovirs;

import edu.jhuapl.sbmt.model.eros.SpectrumMath;

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
        return OVIRS.ovirsBandCenters.length;
    }

    @Override
    public String[] getDerivedParameters()
    {
        return new String[]{};
    }

}
