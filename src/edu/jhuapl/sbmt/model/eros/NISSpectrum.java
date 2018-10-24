package edu.jhuapl.sbmt.model.eros;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import vtk.vtkDoubleArray;
import vtk.vtkIdTypeArray;
import vtk.vtkPolyData;
import vtk.vtkTriangle;

import edu.jhuapl.saavtk.model.GenericPolyhedralModel;
import edu.jhuapl.saavtk.util.FileUtil;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.gui.eros.NISSearchPanel;
import edu.jhuapl.sbmt.model.spectrum.BasicSpectrum;
import edu.jhuapl.sbmt.model.spectrum.instruments.SpectralInstrument;

public class NISSpectrum extends BasicSpectrum
{

    static public final int DATE_TIME_OFFSET = 0;
    static public final int MET_OFFSET = 1;
    static public final int CURRENT_SEQUENCE_NUM_OFFSET = 1;
    static public final int DURATION_OFFSET = 3+2;
    static public final int MET_OFFSET_TO_MIDDLE_OFFSET = 4+2;
    static public final int CALIBRATED_GE_DATA_OFFSET = 96+2;
    static public final int CALIBRATED_GE_NOISE_OFFSET = 160+2;
    static public final int SPACECRAFT_POSITION_OFFSET = 224+2;
    static public final int FRUSTUM_OFFSET = 230+2;
    static public final int INCIDENCE_OFFSET = 242+2;
    static public final int EMISSION_OFFSET = 245+2;
    static public final int PHASE_OFFSET = 248+2;
    static public final int RANGE_OFFSET = 252+2;
    static public final int POLYGON_TYPE_FLAG_OFFSET = 258+2;
    static public final int NUMBER_OF_VERTICES_OFFSET = 259+2;
    static public final int POLYGON_START_COORDINATES_OFFSET = 260+2;

    double[] spectrumEros=new double[NIS.bandCentersLength];

    /**
     * Because instances of NISSpectrum can be expensive, we want there to be
     * no more than one instance of this class per image file on the server.
     * Hence this class was created to manage the creation and deletion of
     * NISSpectrums. Anyone needing a NISSpectrum should use this factory class to
     * create NISSpectrums and should NOT call the constructor directly.
     */
//    public static class NISSpectrumFactory
//    {
//        static private WeakHashMap<NISSpectrum, Object> spectra =
//            new WeakHashMap<NISSpectrum, Object>();
//
//        static /*public*/ NISSpectrum createSpectrum(String name, SmallBodyModel eros) throws IOException
//        {
//            for (NISSpectrum spectrum : spectra.keySet())
//            {
//                if (spectrum.getServerPath().equals(name))
//                    return spectrum;
//            }
//
//            NISSpectrum spectrum = new NISSpectrum(name, eros);
//            spectra.put(spectrum, null);
//            return spectrum;
//        }
//    }


    public NISSpectrum(String filename, SmallBodyModel smallBodyModel, SpectralInstrument instrument) throws IOException
    {
        super(filename, smallBodyModel, instrument);

        List<String> values = FileUtil.getFileWordsAsStringList(fullpath);

        dateTime = new DateTime(values.get(DATE_TIME_OFFSET), DateTimeZone.UTC);

        double metOffsetToMiddle = Double.parseDouble(values.get(MET_OFFSET_TO_MIDDLE_OFFSET));
        dateTime = dateTime.plusMillis((int)metOffsetToMiddle);

        duration = Double.parseDouble(values.get(DURATION_OFFSET));
        minIncidence = Double.parseDouble(values.get(INCIDENCE_OFFSET+1));
        maxIncidence = Double.parseDouble(values.get(INCIDENCE_OFFSET+2));
        minEmission= Double.parseDouble(values.get(EMISSION_OFFSET+1));
        maxEmission = Double.parseDouble(values.get(EMISSION_OFFSET+2));
        minPhase = Double.parseDouble(values.get(PHASE_OFFSET+1));
        maxPhase= Double.parseDouble(values.get(PHASE_OFFSET+2));
        range = Double.parseDouble(values.get(RANGE_OFFSET));
        polygon_type_flag = Short.parseShort(values.get(POLYGON_TYPE_FLAG_OFFSET));

        int footprintSize = Integer.parseInt(values.get(NUMBER_OF_VERTICES_OFFSET));
        for (int i=0; i<footprintSize; ++i)
        {
            int latIdx = POLYGON_START_COORDINATES_OFFSET + i*2;
            int lonIdx = POLYGON_START_COORDINATES_OFFSET + i*2 + 1;

            latLons.add(new LatLon(Double.parseDouble(values.get(latIdx)) * Math.PI / 180.0,
                                   (360.0-Double.parseDouble(values.get(lonIdx))) * Math.PI / 180.0));
        }


        for (int i=0; i<getNumberOfBands(); ++i)
        {
            // The following min and max clamps the value between 0 and 1.
            spectrum[i] = Math.min(1.0, Math.max(0.0, Double.parseDouble(values.get(CALIBRATED_GE_DATA_OFFSET + i))));
            spectrumEros[i] = Double.parseDouble(values.get(CALIBRATED_GE_NOISE_OFFSET + i));
        }

        for (int i=0; i<3; ++i)
            spacecraftPosition[i] = Double.parseDouble(values.get(SPACECRAFT_POSITION_OFFSET + i));
        for (int i=0; i<3; ++i)
            frustum1[i] = Double.parseDouble(values.get(FRUSTUM_OFFSET + i));
        for (int i=0; i<3; ++i)
            frustum2[i] = Double.parseDouble(values.get(FRUSTUM_OFFSET + 3 + i));
        for (int i=0; i<3; ++i)
            frustum3[i] = Double.parseDouble(values.get(FRUSTUM_OFFSET + 6 + i));
        for (int i=0; i<3; ++i)
            frustum4[i] = Double.parseDouble(values.get(FRUSTUM_OFFSET + 9 + i));
        MathUtil.vhat(frustum1, frustum1);
        MathUtil.vhat(frustum2, frustum2);
        MathUtil.vhat(frustum3, frustum3);
        MathUtil.vhat(frustum4, frustum4);

        frustumCenter=new double[3];
        for (int i=0; i<3; i++)
            frustumCenter[i]=frustum1[i]+frustum2[i]+frustum3[i]+frustum4[i];

        footprint = new vtkPolyData();
        shiftedFootprint = new vtkPolyData();
        footprintHeight=smallBodyModel.getMinShiftAmount();

        isToSunVectorShowing=false;
        double dx = MathUtil.vnorm(spacecraftPosition) + smallBodyModel.getBoundingBoxDiagonalLength();
        toSunVectorLength=dx;
        toSunUnitVector=NISSearchPanel.getToSunUnitVector(serverpath.replace("/NIS/2000/", ""));

//        spectrum=new double[getNumberOfBands()];
//        spectrumEros=new double[getNumberOfBands()];


    }

    @Override
    public void generateFootprint()
    {
                if (!latLons.isEmpty())
                {
                    vtkPolyData tmp = smallBodyModel.computeFrustumIntersection(
                            spacecraftPosition, frustum1, frustum2, frustum3, frustum4);
                    if (tmp == null) return;
                    vtkDoubleArray faceAreaFraction = new vtkDoubleArray();
                    faceAreaFraction.SetName(faceAreaFractionArrayName);
                    Frustum frustum = new Frustum(getFrustumOrigin(),
                            getFrustumCorner(0), getFrustumCorner(1),
                            getFrustumCorner(2), getFrustumCorner(3));
                    for (int c = 0; c < tmp.GetNumberOfCells(); c++)
                    {
                        vtkIdTypeArray originalIds = (vtkIdTypeArray) tmp.GetCellData()
                                .GetArray(GenericPolyhedralModel.cellIdsArrayName);
                        int originalId = originalIds.GetValue(c);
                        vtkTriangle tri = (vtkTriangle) smallBodyModel
                                .getSmallBodyPolyData().GetCell(originalId); // tri on
                                                                             // original
                                                                             // body
                                                                             // model
                        vtkTriangle ftri = (vtkTriangle) tmp.GetCell(c); // tri on
                                                                         // footprint
                        faceAreaFraction.InsertNextValue(
                                ftri.ComputeArea() / tri.ComputeArea());
                    }
                    tmp.GetCellData().AddArray(faceAreaFraction);

                    if (tmp != null)
                    {
                        // Need to clear out scalar data since if coloring data is being
                        // shown,
                        // then the color might mix-in with the image.
                        tmp.GetCellData().SetScalars(null);
                        tmp.GetPointData().SetScalars(null);

                        footprint.DeepCopy(tmp);

                        shiftedFootprint.DeepCopy(tmp);
                        PolyDataUtil.shiftPolyDataInMeanNormalDirection(
                                shiftedFootprint, footprintHeight);

                        createSelectionPolyData();
                        createSelectionActor();
                        createToSunVectorPolyData();
                        createToSunVectorActor();
                        createOutlinePolyData();
                        createOutlineActor();
                    }
                }
            }



    //    private vtkPolyData loadFootprint()
//    {
//        String footprintFilename = serverpath.substring(0, serverpath.length()-4) + "_FOOTPRINT.VTK";
//        File file = FileCache.getFileFromServer(footprintFilename);
//
//        if (file == null)
//        {
//            return null;
//        }
//
//        vtkPolyDataReader footprintReader = new vtkPolyDataReader();
//        footprintReader.SetFileName(file.getAbsolutePath());
//        footprintReader.Update();
//
//        vtkPolyData polyData = new vtkPolyData();
//        polyData.DeepCopy(footprintReader.GetOutput());
//
//        return polyData;
//    }

    public double getRange()
    {
        return range;
    }

    public double getDuration()
    {
        return duration;
    }

    public short getPolygonTypeFlag()
    {
        return polygon_type_flag;
    }

    public double[] getSpectrumErrors()
    {
        return spectrumEros;
    }

  /*  public static String[] getDerivedParameters()
    {
        return derivedParameters;
    }*/


    public HashMap<String, String> getProperties() throws IOException
    {
        HashMap<String, String> properties = new LinkedHashMap<String, String>();

        String name = new File(this.fullpath).getName();
        properties.put("Name", name.substring(0, name.length()-4));

        properties.put("Date", dateTime.toString());

        properties.put("Day of Year", (new File(this.fullpath)).getParentFile().getName());

        //properties.put("Year", (new File(this.fullpath)).getParentFile().getParentFile().getName());

        properties.put("MET", (new File(this.fullpath)).getName().substring(2,11));

        properties.put("Duration", Double.toString(duration) + " seconds");

        String polygonTypeStr = "Missing value";
        switch(this.polygon_type_flag)
        {
        case 0:
            polygonTypeStr = "Full (all vertices on shape)";
            break;
        case 1:
            polygonTypeStr = "Partial (single contiguous set of vertices on shape)";
            break;
        case 2:
            polygonTypeStr = "Degenerate (multiple contiguous sets of vertices on shape)";
            break;
        case 3:
            polygonTypeStr = "Empty (no vertices on shape)";
            break;
        }
        properties.put("Polygon Type", polygonTypeStr);

        // Note \u00B0 is the unicode degree symbol
        String deg = "\u00B0";
        properties.put("Minimum Incidence", Double.toString(minIncidence)+deg);
        properties.put("Maximum Incidence", Double.toString(maxIncidence)+deg);
        properties.put("Minimum Emission", Double.toString(minEmission)+deg);
        properties.put("Maximum Emission", Double.toString(maxIncidence)+deg);
        properties.put("Minimum Phase", Double.toString(minPhase)+deg);
        properties.put("Maximum Phase", Double.toString(maxPhase)+deg);

        properties.put("Range", this.range + " km");
        properties.put("Spacecraft Position (km)",
                spacecraftPosition[0] + " " + spacecraftPosition[1] + " " + spacecraftPosition[2]);

        return properties;
    }


    public double getMinIncidence()
    {
        return minIncidence;
    }

    public double getMaxIncidence()
    {
        return maxIncidence;
    }

    public double getMinEmission()
    {
        return minEmission;
    }

    public double getMaxEmission()
    {
        return maxEmission;
    }

    public double getMinPhase()
    {
        return minPhase;
    }

    public double getMaxPhase()
    {
        return maxPhase;
    }


    @Override
    public void saveSpectrum(File file) throws IOException
    {
/*        FileWriter fstream = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fstream);

        String nl = System.getProperty("line.separator");

        HashMap<String,String> properties = getProperties();
        for (String key : properties.keySet())
        {
            String value = properties.get(key);

            // Replace unicode degrees symbol (\u00B0) with text ' deg'
            value = value.replace("\u00B0", " deg");

            out.write(key + " = " + value + nl);
        }

        out.write(nl + nl + "Band Wavelength(nm) Reflectance" + nl);
        for (int i=0; i<instrument.getBandCenters().length; ++i)
        {
            out.write((i+1) + " " + instrument.getBandCenters()[i] + " " + spectrum[i] + nl);
        }

        out.write(nl + nl + "Derived Values" + nl);
        for (int i=0; i<derivedParameters.length; ++i)
        {
            out.write(derivedParameters[i] + " = " + evaluateDerivedParameters(i) + nl);
        }

        for (int i=0; i<spectrumMath.userDefinedDerivedParameters.size(); ++i)
        {
            out.write(spectrumMath.userDefinedDerivedParameters.get(i).GetFunction() + " = " + spectrumMath.evaluateUserDefinedDerivedParameters(i) + nl);
        }

        out.close();*/
    }


    @Override
    public double[] getChannelColor()
    {
        double[] color = new double[3];
        for (int i=0; i<3; ++i)
        {
            double val = 0.0;
            if (channelsToColorBy[i] < instrument.getBandCenters().length)
                val = spectrum[channelsToColorBy[i]];
            else if (channelsToColorBy[i] < instrument.getBandCenters().length + instrument.getSpectrumMath().getDerivedParameters().length)
                val = evaluateDerivedParameters(channelsToColorBy[i]-instrument.getBandCenters().length);
            else
                val = instrument.getSpectrumMath().evaluateUserDefinedDerivedParameters(channelsToColorBy[i]-instrument.getBandCenters().length-instrument.getSpectrumMath().getDerivedParameters().length, spectrum);

            if (val < 0.0)
                val = 0.0;
            else if (val > 1.0)
                val = 1.0;

            double slope = 1.0 / (channelsColoringMaxValue[i] - channelsColoringMinValue[i]);
            color[i] = slope * (val - channelsColoringMinValue[i]);
        }

        return color;
    }

    @Override
    public double evaluateDerivedParameters(int channel)
    {
        switch(channel)
        {
        case 0:
            return spectrum[35] - spectrum[4];
        case 1:
            return spectrum[0] - spectrum[4];
        case 2:
            return spectrum[51] - spectrum[35];
        default:
            return 0.0;
        }
    }



    @Override
    public int getNumberOfBands()
    {
        return NIS.bandCentersLength;
    }

}
