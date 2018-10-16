package edu.jhuapl.sbmt.model.spectrum;

import java.io.IOException;
import java.util.Hashtable;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.spectrum.instruments.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.model.spectrum.instruments.SpectralInstrument;

public class SpectrumInstrumentFactory
{
    static Hashtable<String, BasicSpectrumInstrument> spectralInstruments = new Hashtable<String, BasicSpectrumInstrument>();

    static public void registerType(String name, BasicSpectrumInstrument spectralInstrument)
    {
        spectralInstruments.put(name, spectralInstrument);
    }

    static public BasicSpectrumInstrument getInstrumentForName(String name)
    {
        return spectralInstruments.get(name);
    }

    static public Spectrum getSpectrumForName(String instrumentName, String filename,
            SmallBodyModel smallBodyModel) throws IOException
    {
        SpectralInstrument instrument = getInstrumentForName(instrumentName);
        return instrument.getSpectrumInstance(filename, smallBodyModel);
    }

}
