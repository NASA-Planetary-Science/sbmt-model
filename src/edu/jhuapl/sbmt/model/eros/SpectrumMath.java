package edu.jhuapl.sbmt.model.eros;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vtk.vtkFunctionParser;

import edu.jhuapl.saavtk.util.Preferences;

public abstract class SpectrumMath
{

    protected List<vtkFunctionParser> userDefinedDerivedParameters = new ArrayList<vtkFunctionParser>();

    // A list of channels used in one of the user defined derived parameters
    protected List< List<String>> bandsPerUserDefinedDerivedParameters = new ArrayList<List<String>>();

    double[] spectralValues;

    public double evaluateUserDefinedDerivedParameters(int userDefinedParameter)
    {
        List<String> bands = bandsPerUserDefinedDerivedParameters.get(userDefinedParameter);
        for (String c : bands)
        {
            userDefinedDerivedParameters.get(userDefinedParameter).SetScalarVariableValue(
                    c, spectralValues[Integer.parseInt(c.substring(1))-1]);
        }

        return userDefinedDerivedParameters.get(userDefinedParameter).GetScalarResult();
    }

    public boolean setupUserDefinedDerivedParameter(
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
            if (bandNumber < 1 || bandNumber > getNumberOfBandsPerRawSpectrum())
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

    public abstract int getNumberOfBandsPerRawSpectrum();

    public boolean testUserDefinedDerivedParameter(String function)
    {
        vtkFunctionParser functionParser = new vtkFunctionParser();
        List<String> bands = new ArrayList<String>();

        return setupUserDefinedDerivedParameter(functionParser, function, bands);
    }

    public boolean addUserDefinedDerivedParameter(String function)
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

    public boolean editUserDefinedDerivedParameter(int index, String function)
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

    public void removeUserDefinedDerivedParameters(int index)
    {
        bandsPerUserDefinedDerivedParameters.remove(index);
        userDefinedDerivedParameters.remove(index);
        saveUserDefinedParametersToPreferences();
    }

    public List<vtkFunctionParser> getAllUserDefinedDerivedParameters()
    {
        return userDefinedDerivedParameters;
    }

    public void loadUserDefinedParametersfromPreferences()
    {
        String[] functions = Preferences.getInstance().getAsArray(Preferences.NIS_CUSTOM_FUNCTIONS, ";");
        if (functions != null)
        {
            for (String func : functions)
                addUserDefinedDerivedParameter(func);
        }
    }

    public void saveUserDefinedParametersToPreferences()
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

    public abstract String[] getDerivedParameters();
}
