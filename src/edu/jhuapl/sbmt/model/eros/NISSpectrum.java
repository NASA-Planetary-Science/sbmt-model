package edu.jhuapl.sbmt.model.eros;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import vtk.vtkFunctionParser;
import vtk.vtkPolyData;

import edu.jhuapl.saavtk.util.FileUtil;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.Preferences;
import edu.jhuapl.sbmt.client.SmallBodyModel;

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



    static final public String[] derivedParameters = {
        "B36 - B05",
        "B01 - B05",
        "B52 - B36"
    };

    static private List<vtkFunctionParser> userDefinedDerivedParameters = new ArrayList<vtkFunctionParser>();

    // A list of channels used in one of the user defined derived parameters
    static private List< List<String>> bandsPerUserDefinedDerivedParameters = new ArrayList<List<String>>();

    static
    {
        loadUserDefinedParametersfromPreferences();
    }

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

        for (int i=0; i<numberOfBands; ++i)
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

    public static String[] getDerivedParameters()
    {
        return derivedParameters;
    }


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


    private double evaluateUserDefinedDerivedParameters(int userDefinedParameter)
    {
        List<String> bands = bandsPerUserDefinedDerivedParameters.get(userDefinedParameter);
        for (String c : bands)
        {
            userDefinedDerivedParameters.get(userDefinedParameter).SetScalarVariableValue(
                    c, spectrum[Integer.parseInt(c.substring(1))-1]);
        }

        return userDefinedDerivedParameters.get(userDefinedParameter).GetScalarResult();
    }

    private static boolean setupUserDefinedDerivedParameter(
            vtkFunctionParser functionParser, String function, List<String> bands)
    {
        functionParser.RemoveAllVariables();
        functionParser.SetFunction(function);

        // Find all variables in the expression of the form BXX where X is a digit
        // such as B01, b63, B10
        String patternString = "[Bb]\\d\\d";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(function);

        bands.clear();
        while(matcher.find())
        {
            String bandName = function.substring(matcher.start(), matcher.end());

            // Flag an error if user tries to create variable out of the range
            // of valid bands (only from 1 through 64 is allowed)
            int bandNumber = Integer.parseInt(bandName.substring(1));
            if (bandNumber < 1 || bandNumber > numberOfBands)
                return false;

            bands.add(bandName);
        }

        // First try to evaluate it to see if it's valid. Make sure to set
        // Replacement value on, so only syntax errors are flagged.
        // (Division by zero is not flagged).
        functionParser.SetReplacementValue(0.0);
        functionParser.ReplaceInvalidValuesOn();

        for (String c : bands)
            functionParser.SetScalarVariableValue(c, 0.0);
        if (functionParser.IsScalarResult() == 0)
            return false;

        return true;
    }

    public static boolean testUserDefinedDerivedParameter(String function)
    {
        vtkFunctionParser functionParser = new vtkFunctionParser();
        List<String> bands = new ArrayList<String>();

        return setupUserDefinedDerivedParameter(functionParser, function, bands);
    }

    public static boolean addUserDefinedDerivedParameter(String function)
    {
        vtkFunctionParser functionParser = new vtkFunctionParser();
        List<String> bands = new ArrayList<String>();

        boolean success = setupUserDefinedDerivedParameter(functionParser, function, bands);

        if (success)
        {
            bandsPerUserDefinedDerivedParameters.add(bands);
            userDefinedDerivedParameters.add(functionParser);
            saveUserDefinedParametersToPreferences();
        }

        return success;
    }

    public static boolean editUserDefinedDerivedParameter(int index, String function)
    {
        vtkFunctionParser functionParser = new vtkFunctionParser();
        List<String> bands = new ArrayList<String>();

        boolean success = setupUserDefinedDerivedParameter(functionParser, function, bands);

        if (success)
        {
            bandsPerUserDefinedDerivedParameters.set(index, bands);
            userDefinedDerivedParameters.set(index, functionParser);
            saveUserDefinedParametersToPreferences();
        }

        return success;
    }

    public static void removeUserDefinedDerivedParameters(int index)
    {
        bandsPerUserDefinedDerivedParameters.remove(index);
        userDefinedDerivedParameters.remove(index);
        saveUserDefinedParametersToPreferences();
    }

    public static List<vtkFunctionParser> getAllUserDefinedDerivedParameters()
    {
        return userDefinedDerivedParameters;
    }

    public static void loadUserDefinedParametersfromPreferences()
    {
        String[] functions = Preferences.getInstance().getAsArray(Preferences.NIS_CUSTOM_FUNCTIONS, ";");
        if (functions != null)
        {
            for (String func : functions)
                addUserDefinedDerivedParameter(func);
        }
    }

    public static void saveUserDefinedParametersToPreferences()
    {
        String functionList = "";
        int numUserDefineParameters = userDefinedDerivedParameters.size();
        for (int i=0; i<numUserDefineParameters; ++i)
        {
            functionList += userDefinedDerivedParameters.get(i).GetFunction();
            if (i < numUserDefineParameters-1)
                functionList += ";";
        }

        Preferences.getInstance().put(Preferences.NIS_CUSTOM_FUNCTIONS, functionList);
    }



    @Override
    public void saveSpectrum(File file) throws IOException
    {
        FileWriter fstream = new FileWriter(file);
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

        for (int i=0; i<userDefinedDerivedParameters.size(); ++i)
        {
            out.write(userDefinedDerivedParameters.get(i).GetFunction() + " = " + evaluateUserDefinedDerivedParameters(i) + nl);
        }

        out.close();
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
            else if (channelsToColorBy[i] < instrument.getBandCenters().length + derivedParameters.length)
                val = evaluateDerivedParameters(channelsToColorBy[i]-instrument.getBandCenters().length);
            else
                val = evaluateUserDefinedDerivedParameters(channelsToColorBy[i]-instrument.getBandCenters().length-derivedParameters.length);

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

}
