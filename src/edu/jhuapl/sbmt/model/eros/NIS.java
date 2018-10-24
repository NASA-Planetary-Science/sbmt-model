package edu.jhuapl.sbmt.model.eros;

import java.io.IOException;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.spectrum.Spectrum;
import edu.jhuapl.sbmt.model.spectrum.SpectrumInstrumentFactory;
import edu.jhuapl.sbmt.model.spectrum.instruments.BasicSpectrumInstrument;

public class NIS extends BasicSpectrumInstrument
{
    public static int bandCentersLength = 64;

    static
    {
        SpectrumInstrumentFactory.registerType("NIS", new NIS());
    }

    public NIS()
    {
        super("cm^-1", "NIS", NisQuery.getInstance(), NISSpectrumMath.getSpectrumMath());
        bandCenters = new double[]{
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
            SmallBodyModel smallBodyModel) throws IOException
    {
        return new NISSpectrum(filename, smallBodyModel, this);
    }



}
