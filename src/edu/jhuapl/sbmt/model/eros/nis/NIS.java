package edu.jhuapl.sbmt.model.eros.nis;

import java.io.IOException;

import edu.jhuapl.sbmt.client.ISmallBodyModel;
import edu.jhuapl.sbmt.query.QueryBase;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.SpectraType;
import edu.jhuapl.sbmt.spectrum.model.core.SpectraTypeFactory;
import edu.jhuapl.sbmt.spectrum.model.core.SpectrumInstrumentFactory;
import edu.jhuapl.sbmt.spectrum.model.io.SpectrumInstrumentMetadataIO;
import edu.jhuapl.sbmt.spectrum.model.sbmtCore.spectra.Spectrum;
import edu.jhuapl.sbmt.spectrum.model.sbmtCore.spectra.math.SpectrumMath;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.InstanceGetter;
import crucible.crust.metadata.impl.SettableMetadata;

public class NIS extends BasicSpectrumInstrument
{
    public static int bandCentersLength = 64;

    static
    {
        SpectrumInstrumentFactory.registerType("NIS", new NIS());
    }

    public NIS()
    {
        super("nm", "NIS", NisQuery.getInstance(), NISSpectrumMath.getSpectrumMath());
        bandCenters = new Double[]{
                816.2,  // 0
                837.8,  // 1
                859.4,  // 2
                881.0,  // 3
                902.7,  // 4
                924.3,  // 5
                945.9,  // 6
                967.5,  // 7
                989.1,  // 8
                1010.7, // 9
                1032.3, // 10
                1053.9, // 11
                1075.5, // 12
                1097.1, // 13
                1118.8, // 14
                1140.4, // 15
                1162.0, // 16
                1183.6, // 17
                1205.2, // 18
                1226.8, // 19
                1248.4, // 20
                1270.0, // 21
                1291.6, // 22
                1313.2, // 23
                1334.9, // 24
                1356.5, // 25
                1378.1, // 26
                1399.7, // 27
                1421.3, // 28
                1442.9, // 29
                1464.5, // 30
                1486.1, // 31
                1371.8, // 32
                1414.9, // 33
                1458.0, // 34
                1501.1, // 35
                1544.2, // 36
                1587.3, // 37
                1630.4, // 38
                1673.6, // 39
                1716.7, // 40
                1759.8, // 41
                1802.9, // 42
                1846.0, // 43
                1889.1, // 44
                1932.2, // 45
                1975.3, // 46
                2018.4, // 47
                2061.5, // 48
                2104.7, // 49
                2147.8, // 50
                2190.9, // 51
                2234.0, // 52
                2277.1, // 53
                2320.2, // 54
                2363.3, // 55
                2406.4, // 56
                2449.5, // 57
                2492.6, // 58
                2535.8, // 59
                2578.9, // 60
                2622.0, // 61
                2665.1, // 62
                2708.2  // 63
            };
    }


    // These values were taken from Table 1 of "Spectral properties and geologic
    // processes on Eros from combined NEAR NIS and MSI data sets"
    // by Noam Izenberg et. al.
//    public double[] bandCenters = {
//        816.2,  // 0
//        837.8,  // 1
//        859.4,  // 2
//        881.0,  // 3
//        902.7,  // 4
//        924.3,  // 5
//        945.9,  // 6
//        967.5,  // 7
//        989.1,  // 8
//        1010.7, // 9
//        1032.3, // 10
//        1053.9, // 11
//        1075.5, // 12
//        1097.1, // 13
//        1118.8, // 14
//        1140.4, // 15
//        1162.0, // 16
//        1183.6, // 17
//        1205.2, // 18
//        1226.8, // 19
//        1248.4, // 20
//        1270.0, // 21
//        1291.6, // 22
//        1313.2, // 23
//        1334.9, // 24
//        1356.5, // 25
//        1378.1, // 26
//        1399.7, // 27
//        1421.3, // 28
//        1442.9, // 29
//        1464.5, // 30
//        1486.1, // 31
//        1371.8, // 32
//        1414.9, // 33
//        1458.0, // 34
//        1501.1, // 35
//        1544.2, // 36
//        1587.3, // 37
//        1630.4, // 38
//        1673.6, // 39
//        1716.7, // 40
//        1759.8, // 41
//        1802.9, // 42
//        1846.0, // 43
//        1889.1, // 44
//        1932.2, // 45
//        1975.3, // 46
//        2018.4, // 47
//        2061.5, // 48
//        2104.7, // 49
//        2147.8, // 50
//        2190.9, // 51
//        2234.0, // 52
//        2277.1, // 53
//        2320.2, // 54
//        2363.3, // 55
//        2406.4, // 56
//        2449.5, // 57
//        2492.6, // 58
//        2535.8, // 59
//        2578.9, // 60
//        2622.0, // 61
//        2665.1, // 62
//        2708.2  // 63
//    };


    @Override
    public Spectrum getSpectrumInstance(String filename,
            ISmallBodyModel smallBodyModel) throws IOException
    {
        return new NISSpectrum(filename, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel, this);
    }


	@Override
	public double[] getRGBMaxVals()
	{
		return new double[] { 0.5, 0.5, 0.5 };
	}


	@Override
	public int[] getRGBDefaultIndices()
	{
		return new int[] { 1, 25, 50 };
	}


	@Override
	public String[] getDataTypeNames()
	{
		return new String[] {};
	}

	 //metadata interface
    private static final Key<NIS> NIS_KEY = Key.of("nis");
    private static final Key<String> spectraNameKey = Key.of("displayName");
    private static final Key<QueryBase> queryBaseKey = Key.of("queryBase");
    private static final Key<SpectrumMath> spectrumMathKey = Key.of("spectrumMath");
    private static final Key<Double[]> bandCentersKey = Key.of("bandCenters");
    private static final Key<String> bandCenterUnitKey = Key.of("bandCenterUnit");

    public static void initializeSerializationProxy()
	{
		InstanceGetter.defaultInstanceGetter().register(NIS_KEY, (metadata) -> {

			NIS inst = null;
			String displayName = metadata.get(spectraNameKey);
			SpectraType spectraType = SpectraTypeFactory.findSpectraTypeForDisplayName(displayName);
			QueryBase queryBase = spectraType.getQueryBase();
			SpectrumMath spectrumMath = spectraType.getSpectrumMath();
			Double[] bandCenters = spectraType.getBandCenters();
			String bandCenterUnit = spectraType.getBandCenterUnit();

			inst = new NIS();
			inst.bandCenterUnit = bandCenterUnit;
			inst.displayName = displayName;
			inst.queryBase = queryBase;
			inst.spectrumMath = spectrumMath;
			inst.bandCenters = bandCenters;

			return inst;
		},
	    NIS.class,
	    key -> {
			 SettableMetadata metadata = SettableMetadata.of(Version.of(1, 0));
			 metadata.put(spectraNameKey, key.getDisplayName());
//			 metadata.put(queryBaseKey, key.getQueryBase());
//			 metadata.put(spectrumMathKey, key.getSpectrumMath());
//			 metadata.put(bandCenterUnitKey, key.getBandCenterUnit());
			 return metadata;
		});
	}

}
