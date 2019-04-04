package edu.jhuapl.sbmt.model.spectrum;

import java.io.IOException;

import edu.jhuapl.sbmt.client.ISmallBodyModel;
import edu.jhuapl.sbmt.model.spectrum.math.SpectrumMath;
import edu.jhuapl.sbmt.query.IQueryBase;

import crucible.crust.metadata.api.Metadata;

public interface ISpectralInstrument //extends StorableAsMetadata<ISpectralInstrument>
{
	public Metadata store();
    public double[] getBandCenters();
    public String getBandCenterUnit();
    public String getDisplayName();
    public IQueryBase getQueryBase();
//    public DatabaseQueryBase getDatabaseQueryBase();
    public SpectrumMath getSpectrumMath();
    public Spectrum getSpectrumInstance(String filename, ISmallBodyModel smallBodyModel) throws IOException;


//    static void retrieve(Metadata source)
//    {
//        displayName = read(spectraNameKey, source);
//        SpectraType spectraType = SpectraType.findSpectraTypeForDisplayName(displayName);
//        this.queryBase = spectraType.getQueryBase();
//        this.spectrumMath = spectraType.getSpectrumMath();
//        this.bandCenters = spectraType.getBandCenters();
//        this.bandCenterUnit = spectraType.getBandCenterUnit();
//    }
}
