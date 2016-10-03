package edu.jhuapl.sbmt.model.europa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.ListDataListener;

import vtk.vtkProp;



public class CsvSurfacePatch implements SurfacePatch
{
    // field names, column numbers, min and max parameters
    // XTsize (km) Incidence(rad)  Emission(rad)   SolarPhase(rad) Tlocal(h)
//    private static String[] dataFields = { "XTsize", "Incidence", "Emission", "SolarPhase", "Tlocal" };
//    private static int[] dataColumns = { 7, 8, 9, 10, 11 };
//    private static double[] dataMin = { 0.0, 0.0, 0.0, 0.0, 0.0 };
//    private static double[] dataMax = { 4.0, Math.PI, Math.PI, Math.PI, 24.0 };
//
//    private static Map<String, Double> surfaceDataMin;
//    private static Map<String, Double> surfaceDataMax;
//
//    private static void calculateMinMax()
//    {
//        if (surfaceDataMin == null)
//        {
//            surfaceDataMin = new HashMap<String, Double>();
//            surfaceDataMax = new HashMap<String, Double>();
//            for (int i=0; i<dataFields.length; i++)
//            {
//                surfaceDataMin.put(dataFields[i], dataMin[i]);
//                surfaceDataMax.put(dataFields[i], dataMax[i]);
//            }
//        }
//    }

    private ScalarRange scalarRange;

    private int id;
    private double area;
    private int boundarySize;
    private double[][] boundaryVertex;
    private int patchSize;
    private double[][] patchVertex;
    private int[][] patchIndex;

    private Map<String, List<Double>> surfaceDataColumns;


    private String currentDataType = "XTsize (km)";

    public CsvSurfacePatch()
    {
        scalarRange = new ScalarRange();
    }

    public String getName()
    {
        return Integer.toString(id);
    }

    public int getId()
    {
        return id;
    }

    public double getArea()
    {
        return area;
    }


    public Double getScalarValue(vtkProp prop, int cellId, double[] pickPosition)
    {
        Double result = null;
        List<Double> scalarValues = this.getSurfaceDataColumns().get(this.currentDataType);
        if (scalarValues != null)
        {
//            PolyDataUtil.interpolateWithinCell(polydata, pointdata, cellId, pickPosition, idList)
            result = scalarValues.get(cellId);
        }

        return result;

//        return (Double)getArea();
    }

    public int getBoundarySize()
    {
        return boundarySize;
    }

    public double[][] getBoundaryVertex()
    {
        return boundaryVertex;
    }

    public int getPatchSize()
    {
        return patchSize;
    }

    public double[][] getPatchVertex()
    {
        return patchVertex;
    }

    public int[][] getPatchIndex()
    {
        return patchIndex;
    }

    public Map<String, List<Double>> getSurfaceDataColumns()
    {
        return this.surfaceDataColumns;
    }

    public CsvSurfacePatch(int id, double area, int boundarySize,
            double[][] boundaryVertex, int patchSize,
            double[][] patchVertex, int[][] patchIndex,
            Map<String, List<Double>> surfaceDataColumns, ScalarRange scalarRange)
    {
        super();
        this.id = id;
        this.area = area;
        this.boundarySize = boundarySize;
        this.boundaryVertex = boundaryVertex;
        this.patchSize = patchSize;
        this.patchVertex = patchVertex;
        this.patchIndex = patchIndex;
        this.surfaceDataColumns = surfaceDataColumns;
        this.scalarRange = scalarRange;
        scalarRange.calculateMinMax();
    }

//    private static ScalarRange defaultScalarRange = new ScalarRange();

    public static List<SurfacePatch> parseCsvFiles(File areaFile, File boundaryFile, File patchFile, ScalarRange defaultScalarRange)
    {
        List<SurfacePatch> result = new ArrayList<SurfacePatch>();

        try {
            BufferedReader areaReader = new BufferedReader(new FileReader(areaFile));
            BufferedReader boundaryReader = new BufferedReader(new FileReader(boundaryFile));
            BufferedReader patchReader = new BufferedReader(new FileReader(patchFile));

            String areaLine = null;
            String boundaryLine = null;
            String patchLine = null;

            while ((areaLine = areaReader.readLine()) != null)
            {
                if (areaLine.startsWith("#") || areaLine.length() <= 1)
                    continue;

                String areaTokens[] = areaLine.split("\\s");
                int imageIndex = Integer.parseInt(areaTokens[0].trim());
                double area = Double.parseDouble(areaTokens[2].trim());

                List<double[]> boundaryList = new ArrayList<double[]>();

                List<double[]> patchVertexList = new ArrayList<double[]>();
                List<int[]> patchIndexList = new ArrayList<int[]>();

                // initialize the surface data columns
                Map<String, List<Double>> surfaceDataColumns = new LinkedHashMap<String, List<Double>>();
                for (String fieldName : defaultScalarRange.getDataFields())
                    surfaceDataColumns.put(fieldName, new ArrayList<Double>());

                // read in the boundary from the boundary file
                String prevBoundaryLine = "";
                do
                {
                    // input files have extra lines and some duplicate lines, which we filter out here
                    if (boundaryLine != null && !boundaryLine.startsWith("#") && boundaryLine.length() > 1 && !boundaryLine.equals(prevBoundaryLine))
                    {
                        String boundaryTokens[] = boundaryLine.split(",");
                        int imageKey = Integer.parseInt(boundaryTokens[0].trim());
                        if (imageKey > imageIndex)
                            break;

                        double x = Double.parseDouble(boundaryTokens[2]);
                        double y = Double.parseDouble(boundaryTokens[3]);
                        double z = Double.parseDouble(boundaryTokens[4]);

                        double[] vertex = { x, y, z };
                        boundaryList.add(vertex);

                        prevBoundaryLine = boundaryLine;
                    }
                } while ((boundaryLine = boundaryReader.readLine()) != null);


                int boundarySize = boundaryList.size();
                double[][] boundaryVertex = (double[][])boundaryList.toArray(new double[boundarySize][]);

                int ipatch = -1;
                // read in the patch from the patch file
                do
                {
                    if (patchLine != null && !patchLine.startsWith("#"))
                    {
                        String patchTokens[] = patchLine.split(",");
                        int imageKey = Integer.parseInt(patchTokens[0].trim());
                        if (imageKey > imageIndex)
                        {
                            ipatch = -1;
                            break;
                        }

                        double x = Double.parseDouble(patchTokens[4]);
                        double y = Double.parseDouble(patchTokens[5]);
                        double z = Double.parseDouble(patchTokens[6]);
                        double[] vertex = { x, y, z };
                        patchVertexList.add(vertex);

                        int j = Integer.parseInt(patchTokens[3]);

//                      int i = Integer.parseInt(patchTokens[2]);
                      // hack due to error (?) in patch index value
                        if (j == 0)
                            ipatch++;
                        int i = ipatch;

                        int[] index = { i, j };
                        patchIndexList.add(index);

                        // populate the data fields for each column
                        for (int k=0; k<defaultScalarRange.getDataFields().length; k++)
                        {
                            String fieldName = defaultScalarRange.getDataFields()[k];
                            int fieldColumn = defaultScalarRange.getDataColumns()[k];
                            Double fieldValue = Double.parseDouble(patchTokens[fieldColumn]);
//                            if (fieldName.equals("XTsize"))
//                                fieldValue = Math.log10(fieldValue);
                            surfaceDataColumns.get(fieldName).add(fieldValue);
                        }

//                        double crossTrackSize = Double.parseDouble(patchTokens[7]);
//                        double normalizedCrossTrackSize = (crossTrackSize - 4.0) / 4.0;
//                        patchCrossTrackSizeList.add(normalizedCrossTrackSize);
                    }
                } while ((patchLine = patchReader.readLine()) != null);


                int patchSize = patchVertexList.size();
                double[][] patchVertex = (double[][])patchVertexList.toArray(new double[patchSize][]);
                int[][] patchIndex = (int[][])patchIndexList.toArray(new int[patchSize][]);

//                Double[] xtrackSize = (Double[])patchCrossTrackSizeList.toArray(new Double[patchSize]);
//                double[] crossTrackSize = new double[patchSize];

//                for (int i=0; i<patchSize; i++)
//                    crossTrackSize[i] = xtrackSize[i];


//                // patch vertices TEST
//                int patchSize = 100;
//                double[][] patchVertex = new double[patchSize][3];
//                int[][] patchIndex = new int[patchSize][2];
//
//                // calculate scalar fields TEST
//                double[] crossTrackSize = new double[patchSize];
//
//                double x = 0.0;
//                double y = 0.0;
//                double z = 0.0;
//
//                int vertex = 0;
//                for(int i = 0; i < 10; i++)
//                {
//                  x = i * 10.0 + imageIndex * 200.0;
//                  for(int j = 0; j < 10; j++)
//                  {
//                      y = j * 10.0;
//                      patchVertex[vertex][0] = x;
//                      patchVertex[vertex][1] = y;
//                      patchVertex[vertex][2] = z;
//
//                      patchIndex[vertex][0] = i;
//                      patchIndex[vertex][1] = j;
//
//                      vertex++;
//
//                      // crossTrackSize test
//                      crossTrackSize[10*i + j] = (i * j) / 100.0;
//                  }
//                }

                SurfacePatch surfacePatch = new CsvSurfacePatch(imageIndex, area, boundarySize, boundaryVertex, patchSize, patchVertex, patchIndex, surfaceDataColumns, defaultScalarRange);
                result.add(surfacePatch);
            }

            areaReader.close();
            boundaryReader.close();
            patchReader.close();

        } catch (Exception e) { e.printStackTrace(); }

        return result;
    }

    public double[] getNormalizedSurfaceData(String fieldName)
    {

        double min = scalarRange.getSurfaceDataMin(fieldName, surfaceDataColumns);
        double max = scalarRange.getSurfaceDataMax(fieldName, surfaceDataColumns);
        double range = max - min;

        List<Double> surfaceData = getSurfaceDataColumns().get(fieldName);

        int size = surfaceData.size();
        double[] result = new double[size];

        for (int i=0; i<size; i++)
        {
            Double value = surfaceData.get(i);
            // normalize to range of 0.0 to 1.0 from min to max value
            result[i] = (value - min) / range;
        }

        return result;
    }

    public double[] getSurfaceData(String fieldName)
    {
        List<Double> surfaceData = getSurfaceDataColumns().get(fieldName);

        int size = surfaceData.size();
        double[] result = new double[size];

        for (int i=0; i<size; i++)
            result[i] = surfaceData.get(i);

        return result;
    }

    public double[] getSurfaceDataRange(String fieldName)
    {
        double min = scalarRange.getSurfaceDataMin(fieldName, surfaceDataColumns);
        double max = scalarRange.getSurfaceDataMax(fieldName, surfaceDataColumns);
        double[] result = { min, max };

        return result;
    }

    public String getCurrentDataType()
    {
        return this.currentDataType;
    }

    @Override
    public Object getSelectedItem()
    {
        return getCurrentDataType();
    }

    @Override
    public void setSelectedItem(Object item)
    {
        this.currentDataType = (String)item;
        System.out.println("Selected Type: " + currentDataType);
    }

    @Override
    public void addListDataListener(ListDataListener arg0)
    {
        // TODO Auto-generated method stub

    }

        @Override
    public Object getElementAt(int index)
    {
        return scalarRange.getDataFields()[index];
    }

    @Override
    public int getSize()
    {
        return scalarRange.getDataFields().length;
    }

    @Override
    public void removeListDataListener(ListDataListener arg0)
    {
        // TODO Auto-generated method stub

    }
    public String toString()
    {
        return id + ": " + area;
//        return id + ", " +
//        ", " + area +
//        ", " + boundarySize +
//        ", " + boundaryVertex +
//        ", " + patchSize +
//        ", " + patchVertex +
//        ", " + patchIndex +
//        ", " + crossTrackSize +
//        ", " + incidence +
//        ", " + emission +
//        ", " + solarPhase +
//        ", " + localTime;
    }

}


