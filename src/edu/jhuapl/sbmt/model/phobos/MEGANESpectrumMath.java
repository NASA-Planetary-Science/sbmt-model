package edu.jhuapl.sbmt.model.phobos;

import edu.jhuapl.sbmt.model.spectrum.math.SpectrumMath;

public class MEGANESpectrumMath extends SpectrumMath
{
    private static MEGANESpectrumMath spectrumMath=new MEGANESpectrumMath();

    public static MEGANESpectrumMath getInstance()
    {
        return spectrumMath;
    }

    private MEGANESpectrumMath()
    {
        // TODO Auto-generated constructor stub
    }

    @Override
    public int getNumberOfBandsPerRawSpectrum()
    {
        return MEGANE.bandCentersLength;
    }

    @Override
    public String[] getDerivedParameters()
    {
        return new String[]{};
    }

}
