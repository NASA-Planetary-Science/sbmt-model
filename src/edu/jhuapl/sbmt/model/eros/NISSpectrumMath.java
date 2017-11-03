package edu.jhuapl.sbmt.model.eros;

public class NISSpectrumMath extends SpectrumMath
{


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
