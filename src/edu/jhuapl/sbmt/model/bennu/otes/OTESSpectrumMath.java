package edu.jhuapl.sbmt.model.bennu.otes;

import edu.jhuapl.sbmt.model.eros.SpectrumMath;

public class OTESSpectrumMath extends SpectrumMath
{
    private static OTESSpectrumMath spectrumMath=new OTESSpectrumMath();

    public static OTESSpectrumMath getInstance()
    {
        return spectrumMath;
    }

    private OTESSpectrumMath()
    {
        // TODO Auto-generated constructor stub
    }

    @Override
    public int getNumberOfBandsPerRawSpectrum()
    {
        return OTES.bandCentersLength;
    }

    @Override
    public String[] getDerivedParameters()
    {
        return new String[]{};
    }

}
