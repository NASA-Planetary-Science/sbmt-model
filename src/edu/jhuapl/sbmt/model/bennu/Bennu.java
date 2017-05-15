package edu.jhuapl.sbmt.model.bennu;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;

import edu.jhuapl.saavtk.model.ColoringInfo;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;

public class Bennu extends SmallBodyModel
{
    static private final String[] modelNames = {
            "ver64q",
            "ver128q",
            "ver256q",
            "ver512q"
    };

    static private final String[] modelFilesInPlateFormat = null;

    static private final String[] imageMap = null;


    static private final String[] coloringNames = {
            SlopeStr, ElevStr, GravAccStr, GravPotStr,
            "Facet Tilt",
            "Facet Tilt Direction",
            "Mean Tilt",
            "Tilt Variation",
            "Mean Tilt Direction",
            "Tilt Direction Variation",
            "Relative Tilt",
            "Relative Tilt Direction",
            "Maximum Relative Height"
    };

    static private final String[] coloringUnits = {
            SlopeUnitsStr, ElevUnitsStr, GravAccUnitsStr, GravPotUnitsStr,
            SlopeUnitsStr, // Facet Tilt
            SlopeUnitsStr, // Facet Tilt Direction
            SlopeUnitsStr, // Mean Tilt
            SlopeUnitsStr, // Tilt Variation
            SlopeUnitsStr, // Mean Tilt Direction
            SlopeUnitsStr, // Tilt Direction Variation
            SlopeUnitsStr, // Relative Tilt
            SlopeUnitsStr, // Relative Tilt Direction
            "km" // Maximum Relative Height
    };

    public Bennu(SmallBodyViewConfig config)
    {
        super(config,
                modelNames,
                getModelFiles(config),
                getColoringFiles(config.rootDirOnServer),
                coloringNames,
                coloringUnits,
                null,
                imageMap,
                ColoringValueType.CELLDATA,
                false);
    }

    private static final String[] getModelFiles(SmallBodyViewConfig config)
    {
        String[] paths = {
                config.rootDirOnServer + "/ver64q.vtk.gz",
                config.rootDirOnServer + "/ver128q.vtk.gz",
                config.rootDirOnServer + "/ver256q.vtk.gz",
                config.rootDirOnServer + "/ver512q.vtk.gz"
        };
        return paths;
    };

    private static final String[] getColoringFiles(String path)
    {
        return new String[] {
                path + "/Slope",
                path + "/Elevation",
                path + "/GravitationalAcceleration",
                path + "/GravitationalPotential",
                path + "/FacetTilt",
                path + "/FacetTiltDirection",
                path + "/MeanTilt",
                path + "/TiltVariation",
                path + "/MeanTiltDirection",
                path + "/TiltDirectionVariation",
                path + "/RelativeTilt",
                path + "/RelativeTiltDirection",
                path + "/MaximumRelativeHeight"
        };
    }

    @Override
    public double getDensity()
    {
        return getSmallBodyConfig().density;
    }

    @Override
    public double getRotationRate()
    {
        return getSmallBodyConfig().rotationRate;
    }

    @Override
    public String getServerPathToShapeModelFileInPlateFormat()
    {
        return modelFilesInPlateFormat[getModelResolution()];
    }

    @Override
    protected void loadColoringData() throws IOException {
        List<ColoringInfo> infoListCopy = Lists.newArrayList(getColoringInfoList());
        List<ColoringInfo> newInfoList = Lists.newArrayList();
        for (int index = 0; index < infoListCopy.size(); ++index) {
            ColoringInfo info = infoListCopy.get(index);
            if (tryLoad(info, Format.FIT)) {
                newInfoList.add(info);
                continue;
            }
            if (tryLoad(info, Format.TXT)) {
                newInfoList.add(info);
                continue;
            }
            System.err.println("Unable to load coloring data; disabling " + info.coloringName);
        }

        List<ColoringInfo> infoList = getColoringInfoList();
        infoList.clear();
        infoList.addAll(newInfoList);
   }

    private boolean tryLoad(ColoringInfo info, Format format) {
        info.format = format;
        List<ColoringInfo> infoList = getColoringInfoList();
        infoList.clear();
        infoList.add(info);
        try {
            super.loadColoringData();
            return true;
        } catch (@SuppressWarnings("unused") Exception e) {
            // Unable to load; report that by returning false.
            return false;
        }
    }
}
