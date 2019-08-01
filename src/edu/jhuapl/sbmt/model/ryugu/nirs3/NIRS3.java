package edu.jhuapl.sbmt.model.ryugu.nirs3;

import java.io.IOException;

import edu.jhuapl.sbmt.client.ISmallBodyModel;
import edu.jhuapl.sbmt.model.ryugu.nirs3.atRyugu.NIRS3Spectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.SpectrumInstrumentFactory;
import edu.jhuapl.sbmt.spectrum.model.sbmtCore.spectra.Spectrum;

public class NIRS3 extends BasicSpectrumInstrument
{
	public static int bandCentersLength = 128;


    static
    {
        SpectrumInstrumentFactory.registerType("NIRS3", new NIRS3());
    }

    @Override
    public Spectrum getSpectrumInstance(String filename,
            ISmallBodyModel smallBodyModel)
            throws IOException
    {
        return new NIRS3Spectrum(filename, smallBodyModel, this);
    }


	@Override
	public double[] getRGBMaxVals()
	{
		return new double[] { 0.00005, 0.0001, 0.002 };
	}


	@Override
	public int[] getRGBDefaultIndices()
	{
		return new int[] {100, 70, 40};
	}


	@Override
	public String[] getDataTypeNames()
	{
		// TODO Auto-generated method stub
		return new String[] { };
	}

    public NIRS3()
    {
        super("cm^-1", "NIRS3", NIRS3Query.getInstance(), NIRS3SpectrumMath.getInstance());
        bandCenters = new Double[]{
        		1249.11, // 0
                1267.65, // 1
                1286.19, // 2
                1304.71, // 3
                1323.22, // 4
                1341.73, // 5
                1360.22, // 6
                1378.71, // 7
                1397.18, // 8
                1415.65, // 9
                1434.11, // 10
                1452.55, // 11
                1470.99, // 12
                1489.42, // 13
                1507.83, // 14
                1526.24, // 15
                1544.64, // 16
                1563.03, // 17
                1581.40, // 18
                1599.77, // 19
                1618.13, // 20
                1636.48, // 21
                1654.82, // 22
                1673.15, // 23
                1691.47, // 24
                1709.78, // 25
                1728.08, // 26
                1746.37, // 27
                1764.65, // 28
                1782.92, // 29
                1801.18, // 30
                1819.43, // 31
                1837.67, // 32
                1855.90, // 33
                1874.13, // 34
                1892.34, // 35
                1910.54, // 36
                1928.74, // 37
                1946.92, // 38
                1965.09, // 39
                1983.26, // 40
                2001.41, // 41
                2019.55, // 42
                2037.69, // 43
                2055.81, // 44
                2073.93, // 45
                2092.03, // 46
                2110.13, // 47
                2128.21, // 48
                2146.29, // 49
                2164.36, // 50
                2182.41, // 51
                2200.46, // 52
                2218.50, // 53
                2236.52, // 54
                2254.54, // 55
                2272.55, // 56
                2290.55, // 57
                2308.54, // 58
                2326.51, // 59
                2344.48, // 60
                2362.44, // 61
                2380.39, // 62
                2398.33, // 63
                2416.26, // 64
                2434.18, // 65
                2452.09, // 66
                2469.99, // 67
                2487.88, // 68
                2505.77, // 69
                2523.64, // 70
                2541.50, // 71
                2559.35, // 72
                2577.19, // 73
                2595.03, // 74
                2612.85, // 75
                2630.66, // 76
                2648.46, // 77
                2666.26, // 78
                2684.04, // 79
                2701.82, // 80
                2719.58, // 81
                2737.33, // 82
                2755.08, // 83
                2772.81, // 84
                2790.54, // 85
                2808.26, // 86
                2825.96, // 87
                2843.66, // 88
                2861.34, // 89
                2879.02, // 90
                2896.69, // 91
                2914.34, // 92
                2931.99, // 93
                2949.63, // 94
                2967.26, // 95
                2984.88, // 96
                3002.48, // 97
                3020.08, // 98
                3037.67, // 99
                3055.25, // 100
                3072.82, // 101
                3090.38, // 102
                3107.93, // 103
                3125.47, // 104
                3143.00, // 105
                3160.52, // 106
                3178.03, // 107
                3195.53, // 108
                3213.03, // 109
                3230.51, // 110
                3247.98, // 111
                3265.44, // 112
                3282.89, // 113
                3300.34, // 114
                3317.77, // 115
                3335.19, // 116
                3352.61, // 117
                3370.01, // 118
                3387.40, // 119
                3404.79, // 120
                3422.16, // 121
                3439.53, // 122
                3456.88, // 123
                3474.23, // 124
                3491.57, // 125
                3508.89, // 126
                3526.21 // 127
        };
    }




}
