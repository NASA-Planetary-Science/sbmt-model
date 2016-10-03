package edu.jhuapl.sbmt.model.europa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;



public class ScalarRange implements ComboBoxModel
{
    // field names, column numbers, min and max parameters
    // XTsize (km) Incidence(rad)  Emission(rad)   SolarPhase(rad) Tlocal(h)
    private static String[] dataFields = { "XTsize (km)", "Incidence (rad)", "Emission (rad)", "SolarPhase (rad)", "Tlocal (h)" };
    private static int[] dataColumns = { 7, 8, 9, 10, 11 };
    private static double[] dataMin = { 0.0, 0.0, 0.0, 0.0, 0.0 };
    private static double[] dataMax = { 300.0, Math.PI, Math.PI, Math.PI, 24.0 };

    private Map<String, Double> defaultSurfaceDataMin;
    private Map<String, Double> defaultSurfaceDataMax;

    private Map<String, Double> passSurfaceDataMin;
    private Map<String, Double> passSurfaceDataMax;

    public ScalarRange()
    {
        super();
        calculateMinMax();
    }

    public String[] getDataFields() { return dataFields; }

    public int[] getDataColumns() { return dataColumns; }

    public Double getSurfaceDataMin(String fieldName, Map<String, List<Double>> surfaceDataColumns)
    {
        if (currentRangeType.equals("Image"))
            return calculateMin(surfaceDataColumns.get(fieldName));
        else if (currentRangeType.equals("Pass") && !passSurfaceDataMax.isEmpty())
            return passSurfaceDataMin.get(fieldName);
        else
            return defaultSurfaceDataMin.get(fieldName);
    }

    public Double getSurfaceDataMax(String fieldName, Map<String, List<Double>> surfaceDataColumns)
    {
        if (currentRangeType.equals("Image"))
            return calculateMax(surfaceDataColumns.get(fieldName));
        else if (currentRangeType.equals("Pass") && !passSurfaceDataMax.isEmpty())
            return passSurfaceDataMax.get(fieldName);
        else
            return defaultSurfaceDataMax.get(fieldName);
    }

    public void clearMinMax()
    {
        passSurfaceDataMin = new HashMap<String, Double>();
        passSurfaceDataMax = new HashMap<String, Double>();
    }

    public void minMaxSurfaceColumns(Map<String, List<Double>> surfaceColumns)
    {
        for (String field : surfaceColumns.keySet())
        {
            List<Double> values = surfaceColumns.get(field);

            Double min = calculateMin(values);
            Double passMin = passSurfaceDataMin.get(field);
            if (passMin == null || min < passMin)
            {
                passSurfaceDataMin.put(field, min);
                System.out.println(field + " new min" + min);
            }

            Double max = calculateMax(values);
            Double passMax = passSurfaceDataMax.get(field);
            if (passMax == null || max > passMax)
            {
                passSurfaceDataMax.put(field, max);
                System.out.println(field + " new max" + max);
            }
        }
    }

    private Double calculateMin(List<Double> values)
    {
        Double result = null;
        for (Double value : values)
        {
            if (result == null || value < result)
                result = value;
        }

        return result;
    }

    private Double calculateMax(List<Double> values)
    {
        Double result = null;
        for (Double value : values)
        {
            if (result == null || value > result)
                result = value;
        }

        return result;
    }

    public void calculateMinMax()
    {
        if (defaultSurfaceDataMin == null)
        {
            defaultSurfaceDataMin = new HashMap<String, Double>();
            defaultSurfaceDataMax = new HashMap<String, Double>();
            for (int i=0; i<dataFields.length; i++)
            {
                defaultSurfaceDataMin.put(dataFields[i], dataMin[i]);
                defaultSurfaceDataMax.put(dataFields[i], dataMax[i]);
            }
        }
    }


    private static String[] rangeTypes = { "Default", "Pass", "Image" };

    private String currentRangeType = "Default";

    public String getCurrentRangeType() { return currentRangeType; }

    @Override
    public Object getSelectedItem()
    {
        return currentRangeType;
    }

    @Override
    public void setSelectedItem(Object item)
    {
        currentRangeType = (String)item;
        System.out.println("Selected Range Type: " + currentRangeType);
    }

    @Override
    public void addListDataListener(ListDataListener arg0)
    {
        // TODO Auto-generated method stub

    }

        @Override
    public Object getElementAt(int index)
    {
        return rangeTypes[index];
    }

    @Override
    public int getSize()
    {
        return rangeTypes.length;
    }

    @Override
    public void removeListDataListener(ListDataListener arg0)
    {
        // TODO Auto-generated method stub

    }
}


