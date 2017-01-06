package edu.jhuapl.sbmt.model.eros;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkDoubleArray;
import vtk.vtkFeatureEdges;
import vtk.vtkFunctionParser;
import vtk.vtkIdList;
import vtk.vtkIdTypeArray;
import vtk.vtkLine;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkProperty;
import vtk.vtkTriangle;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.model.GenericPolyhedralModel;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.FileUtil;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.Preferences;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.gui.eros.NISSearchPanel;

public class NISSpectrum extends AbstractModel implements PropertyChangeListener
{
    private String fullpath; // The actual path of the spectrum stored on the local disk (after downloading from the server)
    protected String serverpath; // The path of the spectrum as passed into the constructor. This is not the
       // same as fullpath but instead corresponds to the name needed to download
       // the file from the server (excluding the hostname).

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

    private DateTime dateTime;
    private double duration;
    private short polygon_type_flag;
    private double range;
    private List<LatLon> latLons = new ArrayList<LatLon>();
    private vtkPolyData footprint;
    private vtkPolyData shiftedFootprint;
    private vtkActor footprintActor;
    private vtkActor frustumActor;
    private List<vtkProp> footprintActors = new ArrayList<vtkProp>();
    private SmallBodyModel erosModel;
    private static final int numberOfBands = 64;
    private double[] spectrum = new double[numberOfBands];
    private double[] spectrumEros = new double[numberOfBands];
    private double[] spacecraftPosition = new double[3];
    private double[] frustum1 = new double[3];
    private double[] frustum2 = new double[3];
    private double[] frustum3 = new double[3];
    private double[] frustum4 = new double[3];
    private double minIncidence;
    private double maxIncidence;
    private double minEmission;
    private double maxEmission;
    private double minPhase;
    private double maxPhase;
    private boolean showFrustum = false;
    static private int[] channelsToColorBy = {0, 0, 0};
    static private double[] channelsColoringMinValue= {0.0, 0.0, 0.0};
    static private double[] channelsColoringMaxValue = {0.05, 0.05, 0.05};

    private double[] frustumCenter;

    private vtkActor selectionActor=new vtkActor();
    private vtkPolyData selectionPolyData=new vtkPolyData();
    boolean isSelected;
    double footprintHeight;

    private vtkActor outlineActor=new vtkActor();
    private vtkPolyData outlinePolyData=new vtkPolyData();
    boolean isOutlineShowing;

    private vtkActor toSunVectorActor=new vtkActor();
    private vtkPolyData toSunVectorPolyData=new vtkPolyData();
    boolean isToSunVectorShowing;
    double toSunVectorLength;

    // These values were taken from Table 1 of "Spectral properties and geologic
    // processes on Eros from combined NEAR NIS and MSI data sets"
    // by Noam Izenberg et. al.
    static final public double[] bandCenters = {
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


    public NISSpectrum(String filename, SmallBodyModel eros) throws IOException
    {
        // Download the spectrum.
        this(FileCache.getFileFromServer(filename), eros);
        this.serverpath = filename;
    }

    public NISSpectrum(File nisFile, SmallBodyModel eros) throws IOException
    {
        this.erosModel = eros;

        String filename = nisFile.getAbsolutePath();
        this.fullpath = filename;

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
        footprintHeight=eros.getMinShiftAmount();

        isToSunVectorShowing=false;
        double dx = MathUtil.vnorm(spacecraftPosition) + erosModel.getBoundingBoxDiagonalLength();
        toSunVectorLength=dx;

    }

    public vtkPolyData getSelectionPolyData()
    {
        return selectionPolyData;
    }

    public static final String faceAreaFractionArrayName="faceAreaFraction";

    public void generateFootprint()
    {
        if (!latLons.isEmpty())
        {
            vtkPolyData tmp = erosModel.computeFrustumIntersection(spacecraftPosition,
                    frustum1, frustum2, frustum3, frustum4);

            vtkDoubleArray faceAreaFraction=new vtkDoubleArray();
            faceAreaFraction.SetName(faceAreaFractionArrayName);
            Frustum frustum=new Frustum(getFrustumOrigin(), getFrustumCorner(0), getFrustumCorner(1), getFrustumCorner(2), getFrustumCorner(3));
            for (int c=0; c<tmp.GetNumberOfCells(); c++)
            {
                int originalCellId=((vtkIdTypeArray)tmp.GetCellData().GetArray(GenericPolyhedralModel.cellIdsArrayName)).GetValue(c);
                vtkTriangle tri=(vtkTriangle)erosModel.getSmallBodyPolyData().GetCell(originalCellId);
                // XXX: TODO: uncomment this
    //            faceAreaFraction.InsertNextValue(PolyDataUtil.computeOverlapFraction(tri, frustum));
            }
            tmp.GetCellData().AddArray(faceAreaFraction);

            if (tmp != null)
            {
                // Need to clear out scalar data since if coloring data is being shown,
                // then the color might mix-in with the image.
                tmp.GetCellData().SetScalars(null);
                tmp.GetPointData().SetScalars(null);

                footprint.DeepCopy(tmp);

                shiftedFootprint.DeepCopy(tmp);
                PolyDataUtil.shiftPolyDataInMeanNormalDirection(shiftedFootprint, footprintHeight);

                createSelectionPolyData();
                createSelectionActor();
                createToSunVectorPolyData();
                createToSunVectorActor();
                createOutlinePolyData();
                createOutlineActor();
            }
        }
    }


    private void createSelectionPolyData()
    {
        vtkFeatureEdges edgeFilter=new vtkFeatureEdges();
        edgeFilter.SetInputData(getShiftedFootprint());
        edgeFilter.BoundaryEdgesOn();
        edgeFilter.FeatureEdgesOff();
        edgeFilter.ManifoldEdgesOff();
        edgeFilter.NonManifoldEdgesOff();
        edgeFilter.Update();
        selectionPolyData.DeepCopy(edgeFilter.GetOutput());
    }

    private void createSelectionActor()
    {
        vtkPolyDataMapper mapper=new vtkPolyDataMapper();
        mapper.SetInputData(selectionPolyData);
        mapper.Update();
        selectionActor.SetMapper(mapper);
        selectionActor.VisibilityOff();
        selectionActor.GetProperty().EdgeVisibilityOn();
        selectionActor.GetProperty().SetEdgeColor(0.5,1,0.5);
        selectionActor.GetProperty().SetLineWidth(5);
    }


    private void createOutlinePolyData()
    {
        vtkFeatureEdges edgeFilter=new vtkFeatureEdges();
        edgeFilter.SetInputData(getShiftedFootprint());
        edgeFilter.BoundaryEdgesOn();
        edgeFilter.FeatureEdgesOff();
        edgeFilter.ManifoldEdgesOff();
        edgeFilter.NonManifoldEdgesOff();
        edgeFilter.Update();
        outlinePolyData.DeepCopy(edgeFilter.GetOutput());
    }

    private void createOutlineActor()
    {
        vtkPolyDataMapper mapper=new vtkPolyDataMapper();
        mapper.SetInputData(outlinePolyData);
        mapper.Update();
        outlineActor.SetMapper(mapper);
        outlineActor.VisibilityOff();
        outlineActor.GetProperty().EdgeVisibilityOn();
        outlineActor.GetProperty().SetEdgeColor(0.4,0.4,1);
        outlineActor.GetProperty().SetLineWidth(2);
    }

    private void createToSunVectorPolyData()
    {
        Vector3D toSunVec=NISSearchPanel.getToSunUnitVector(serverpath.replace("/NIS/2000/", ""));
        vtkPoints points=new vtkPoints();
        vtkCellArray cells=new vtkCellArray();
        Vector3D footprintCenter=new Vector3D(getUnshiftedFootprint().GetCenter());
        int id1=points.InsertNextPoint(footprintCenter.toArray());
        int id2=points.InsertNextPoint(footprintCenter.add(toSunVec.normalize().scalarMultiply(toSunVectorLength)).toArray());
        vtkLine line=new vtkLine();
        line.GetPointIds().SetId(0, id1);
        line.GetPointIds().SetId(1, id2);
        cells.InsertNextCell(line);
        toSunVectorPolyData.SetPoints(points);
        toSunVectorPolyData.SetLines(cells);
    }

    private void createToSunVectorActor()
    {
        vtkPolyDataMapper mapper=new vtkPolyDataMapper();
        mapper.SetInputData(toSunVectorPolyData);
        mapper.Update();
        toSunVectorActor.SetMapper(mapper);
        toSunVectorActor.VisibilityOff();
        toSunVectorActor.GetProperty().SetColor(1,1,0.5);
    }

    public void setSelected()
    {
        isSelected=true;
        selectionActor.VisibilityOn();
        selectionActor.Modified();
    }

    public void setUnselected()
    {
        isSelected=false;
        selectionActor.VisibilityOff();
        selectionActor.Modified();
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

    void shiftFootprintToHeight(double h)
    {
        vtkPolyData tmp = erosModel.computeFrustumIntersection(spacecraftPosition,
                frustum1, frustum2, frustum3, frustum4);
        shiftedFootprint.DeepCopy(tmp);
        PolyDataUtil.shiftPolyDataInMeanNormalDirection(shiftedFootprint,h);
        createSelectionPolyData();
        createOutlinePolyData();
        //
        if (isSelected)
            selectionActor.VisibilityOn();
        //
        ((vtkPolyDataMapper)footprintActor.GetMapper()).SetInputData(shiftedFootprint);
        footprintActor.GetMapper().Update();
        ((vtkPolyDataMapper)selectionActor.GetMapper()).SetInputData(selectionPolyData);
        selectionActor.GetMapper().Update();
        ((vtkPolyDataMapper)outlineActor.GetMapper()).SetInputData(selectionPolyData);
        outlineActor.GetMapper().Update();

        //
        footprintHeight=h;
    }

    public List<vtkProp> getProps()
    {
        if (footprintActor == null && !latLons.isEmpty())
        {
            generateFootprint();

            vtkPolyDataMapper footprintMapper = new vtkPolyDataMapper();
            footprintMapper.SetInputData(shiftedFootprint);
            //footprintMapper.SetResolveCoincidentTopologyToPolygonOffset();
            //footprintMapper.SetResolveCoincidentTopologyPolygonOffsetParameters(-.002, -2.0);
            footprintMapper.Update();

            footprintActor = new vtkActor();
            footprintActor.SetMapper(footprintMapper);
            vtkProperty footprintProperty = footprintActor.GetProperty();
            double[] color = getChannelColor();
            footprintProperty.SetColor(color[0], color[1], color[2]);
            footprintProperty.SetLineWidth(2.0);
            footprintProperty.LightingOff();

            footprintActors.add(footprintActor);

            /*
            // Compute the bounding edges of this surface
            vtkFeatureEdges edgeExtracter = new vtkFeatureEdges();
            edgeExtracter.SetInput(shiftedFootprint);
            edgeExtracter.BoundaryEdgesOn();
            edgeExtracter.FeatureEdgesOff();
            edgeExtracter.NonManifoldEdgesOff();
            edgeExtracter.ManifoldEdgesOff();
            edgeExtracter.Update();

            vtkPolyDataMapper edgeMapper = new vtkPolyDataMapper();
            edgeMapper.SetInputConnection(edgeExtracter.GetOutputPort());
            edgeMapper.ScalarVisibilityOff();
            //edgeMapper.SetResolveCoincidentTopologyToPolygonOffset();
            //edgeMapper.SetResolveCoincidentTopologyPolygonOffsetParameters(-.004, -4.0);
            edgeMapper.Update();

            vtkActor edgeActor = new vtkActor();
            edgeActor.SetMapper(edgeMapper);
            edgeActor.GetProperty().SetColor(0.0, 0.39, 0.0);
            edgeActor.GetProperty().SetLineWidth(2.0);
            edgeActor.GetProperty().LightingOff();
            footprintActors.add(edgeActor);
            */
        }

        if (frustumActor == null)
        {
            vtkPolyData frus = new vtkPolyData();

            vtkPoints points = new vtkPoints();
            vtkCellArray lines = new vtkCellArray();

            vtkIdList idList = new vtkIdList();
            idList.SetNumberOfIds(2);

            double dx = MathUtil.vnorm(spacecraftPosition) + erosModel.getBoundingBoxDiagonalLength();
            double[] origin = spacecraftPosition;
            double[] UL = {origin[0]+frustum1[0]*dx, origin[1]+frustum1[1]*dx, origin[2]+frustum1[2]*dx};
            double[] UR = {origin[0]+frustum2[0]*dx, origin[1]+frustum2[1]*dx, origin[2]+frustum2[2]*dx};
            double[] LL = {origin[0]+frustum3[0]*dx, origin[1]+frustum3[1]*dx, origin[2]+frustum3[2]*dx};
            double[] LR = {origin[0]+frustum4[0]*dx, origin[1]+frustum4[1]*dx, origin[2]+frustum4[2]*dx};

            points.InsertNextPoint(spacecraftPosition);
            points.InsertNextPoint(UL);
            points.InsertNextPoint(UR);
            points.InsertNextPoint(LL);
            points.InsertNextPoint(LR);

            idList.SetId(0, 0);
            idList.SetId(1, 1);
            lines.InsertNextCell(idList);
            idList.SetId(0, 0);
            idList.SetId(1, 2);
            lines.InsertNextCell(idList);
            idList.SetId(0, 0);
            idList.SetId(1, 3);
            lines.InsertNextCell(idList);
            idList.SetId(0, 0);
            idList.SetId(1, 4);
            lines.InsertNextCell(idList);

            frus.SetPoints(points);
            frus.SetLines(lines);


            vtkPolyDataMapper frusMapper = new vtkPolyDataMapper();
            frusMapper.SetInputData(frus);

            frustumActor = new vtkActor();
            frustumActor.SetMapper(frusMapper);
            vtkProperty frustumProperty = frustumActor.GetProperty();
            frustumProperty.SetColor(0.0, 1.0, 0.0);
            frustumProperty.SetLineWidth(2.0);
            frustumActor.VisibilityOff();

            footprintActors.add(frustumActor);
        }

        footprintActors.add(selectionActor);
        footprintActors.add(toSunVectorActor);
        footprintActors.add(outlineActor);

        return footprintActors;
    }

    public void setShowFrustum(boolean b)
    {
        showFrustum = b;

        if (showFrustum)
        {
            frustumActor.VisibilityOn();
        }
        else
        {
            frustumActor.VisibilityOff();
        }

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public boolean isFrustumShowing()
    {
        return showFrustum;
    }

    public void setShowToSunVector(boolean b)
    {
        isToSunVectorShowing=b;
        if (isToSunVectorShowing)
            toSunVectorActor.VisibilityOn();
        else
            toSunVectorActor.VisibilityOff();
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public boolean isToSunVectorShowing()
    {
        return isToSunVectorShowing;
    }

    public void setShowOutline(boolean b)
    {
        isOutlineShowing=b;
        if (isOutlineShowing)
            outlineActor.VisibilityOn();
        else
            outlineActor.VisibilityOff();
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public boolean isOutlineShowing()
    {
        return isOutlineShowing;
    }

    public String getServerPath()
    {
        return serverpath;
    }

    public double getRange()
    {
        return range;
    }

    public double getDuration()
    {
        return duration;
    }

    public DateTime getDateTime()
    {
        return dateTime;
    }

    public short getPolygonTypeFlag()
    {
        return polygon_type_flag;
    }

    public double[] getSpectrum()
    {
        return spectrum;
    }

    public double[] getSpectrumErrors()
    {
        return spectrumEros;
    }

    public double[] getBandCenters()
    {
        return bandCenters;
    }

    public static String[] getDerivedParameters()
    {
        return derivedParameters;
    }

    public double getMinFootprintHeight()
    {
        return erosModel.getMinShiftAmount();
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

    static public void setChannelColoring(
            int[] channels, double[] mins, double[] maxs)
    {
        for (int i=0; i<3; ++i)
        {
            channelsToColorBy[i]        = channels[i];
            channelsColoringMinValue[i] = mins[i];
            channelsColoringMaxValue[i] = maxs[i];
        }
    }

    public void updateChannelColoring()
    {
        vtkProperty footprintProperty = footprintActor.GetProperty();
        double[] color = getChannelColor();
        footprintProperty.SetColor(color[0], color[1], color[2]);
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

    public double[] getSpacecraftPosition()
    {
        return spacecraftPosition;
    }

    public double[] getFrustumCenter()
    {
        return frustumCenter;
    }

    public double[] getFrustumCorner(int i)
    {
        switch (i)
        {
        case 0:
            return frustum1;
        case 1:
            return frustum2;
        case 2:
            return frustum3;
        case 3:
            return frustum4;
        }
        return null;
    }

    public double[] getFrustumOrigin()
    {
        return spacecraftPosition;
    }

    private double evaluateDerivedParameters(int channel)
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

    private double[] getChannelColor()
    {
        double[] color = new double[3];
        for (int i=0; i<3; ++i)
        {
            double val = 0.0;
            if (channelsToColorBy[i] < bandCenters.length)
                val = spectrum[channelsToColorBy[i]];
            else if (channelsToColorBy[i] < bandCenters.length + derivedParameters.length)
                val = evaluateDerivedParameters(channelsToColorBy[i]-bandCenters.length);
            else
                val = evaluateUserDefinedDerivedParameters(channelsToColorBy[i]-bandCenters.length-derivedParameters.length);

            if (val < 0.0)
                val = 0.0;
            else if (val > 1.0)
                val = 1.0;

            double slope = 1.0 / (channelsColoringMaxValue[i] - channelsColoringMinValue[i]);
            color[i] = slope * (val - channelsColoringMinValue[i]);
        }

        return color;
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_RESOLUTION_CHANGED.equals(evt.getPropertyName()))
        {
            System.out.println("updating nis image");
            generateFootprint();
            setUnselected();

            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }

    /**
     * The shifted footprint is the original footprint shifted slightly in the
     * normal direction so that it will be rendered correctly and not obscured
     * by the asteroid.
     * @return
     */
    public vtkPolyData getShiftedFootprint()
    {
        return shiftedFootprint;
    }

    /**
     * The original footprint whose cells exactly overlap the original asteroid.
     * If rendered as is, it would interfere with the asteroid.
     * @return
     */
    public vtkPolyData getUnshiftedFootprint()
    {
        return footprint;
    }

    public void Delete()
    {
        footprint.Delete();
        shiftedFootprint.Delete();
    }

    public String getFullPath()
    {
        return fullpath;
    }

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
        for (int i=0; i<bandCenters.length; ++i)
        {
            out.write((i+1) + " " + bandCenters[i] + " " + spectrum[i] + nl);
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
}
