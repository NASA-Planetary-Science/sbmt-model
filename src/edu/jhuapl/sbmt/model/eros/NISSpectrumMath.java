package edu.jhuapl.sbmt.model.eros;

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

    static final public String[] derivedParameters = {
        "B36 - B05",
        "B01 - B05",
        "B52 - B36"
    };

    @Override
    public int getNumberOfBandsPerRawSpectrum()
    {
        return NIS.bandCenters.length;
    }

    @Override
    public String[] getDerivedParameters()
    {
        return derivedParameters;
    }




}
