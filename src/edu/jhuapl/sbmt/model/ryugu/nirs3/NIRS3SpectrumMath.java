package edu.jhuapl.sbmt.model.ryugu.nirs3;

import edu.jhuapl.sbmt.model.spectrum.math.SpectrumMath;

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

    @Override
    public String[] getDerivedParameters()
    {
        return new String[]{};
    }

}
