package edu.jhuapl.sbmt.model.spectrum;

import edu.jhuapl.sbmt.model.bennu.otes.OTES;
import edu.jhuapl.sbmt.model.bennu.otes.OTESQuery;
import edu.jhuapl.sbmt.model.bennu.otes.OTESSpectrumMath;
import edu.jhuapl.sbmt.model.bennu.ovirs.OVIRS;
import edu.jhuapl.sbmt.model.bennu.ovirs.OVIRSQuery;
import edu.jhuapl.sbmt.model.bennu.ovirs.OVIRSSpectrumMath;
import edu.jhuapl.sbmt.model.eros.NIS;
import edu.jhuapl.sbmt.model.eros.NISSpectrumMath;
import edu.jhuapl.sbmt.model.eros.NisQuery;
import edu.jhuapl.sbmt.model.eros.SpectrumMath;
import edu.jhuapl.sbmt.model.ryugu.nirs3.NIRS3;
import edu.jhuapl.sbmt.model.ryugu.nirs3.NIRS3Query;
import edu.jhuapl.sbmt.model.ryugu.nirs3.NIRS3SpectrumMath;
import edu.jhuapl.sbmt.query.QueryBase;

public enum SpectraType implements ISpectraType
{
    OTES_SPECTRA("OTES", OTESQuery.getInstance(), OTESSpectrumMath.getInstance(), "cm^-1", OTES.otesBandCenters),
    OVIRS_SPECTRA("OVIRS", OVIRSQuery.getInstance(), OVIRSSpectrumMath.getInstance(), "um", OVIRS.ovirsBandCenters),
    NIS_SPECTRA("NIS", NisQuery.getInstance(), NISSpectrumMath.getSpectrumMath(), "cm^-1", NIS.nisBandCenters),
    NIRS3_SPECTRA("NIRS3", NIRS3Query.getInstance(), NIRS3SpectrumMath.getInstance(), "cm^-1", NIRS3.nirs3BandCenters);

    private QueryBase queryBase;
    private SpectrumMath spectrumMath;
    private String displayName;
    private double[] bandCenters;
    private String bandCenterUnit;

    private SpectraType(String displayName, QueryBase queryBase, SpectrumMath spectrumMath, String bandCenterUnit, double[] bandCenters)
    {
        this.displayName = displayName;
        this.queryBase = queryBase;
        this.spectrumMath = spectrumMath;
        this.bandCenterUnit = bandCenterUnit;
        System.out.println("SpectraType: SpectraType: band centers " + bandCenters);
        this.bandCenters = bandCenters;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public QueryBase getQueryBase()
    {
        return queryBase;
    }

    @Override
    public SpectrumMath getSpectrumMath()
    {
        return spectrumMath;
    }

    @Override
    public double[] getBandCenters()
    {
        return bandCenters;
    }

    @Override
    public String getBandCenterUnit()
    {
        return bandCenterUnit;
    }

    public static SpectraType findSpectraTypeForDisplayName(String displayName)
    {
        SpectraType type = null;
        for (SpectraType spectra : values())
        {
            if (spectra.getDisplayName().equals(displayName))
                return type;
        }

        return type;
    }


}
