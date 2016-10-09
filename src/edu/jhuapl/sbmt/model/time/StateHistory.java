package edu.jhuapl.sbmt.model.time;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

import vtk.vtkActor;
import vtk.vtkActor2D;
import vtk.vtkCaptionActor2D;
import vtk.vtkCellArray;
import vtk.vtkConeSource;
import vtk.vtkCoordinate;
import vtk.vtkCubeSource;
import vtk.vtkCylinderSource;
import vtk.vtkDataArray;
import vtk.vtkIdList;
import vtk.vtkLookupTable;
import vtk.vtkMatrix4x4;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkPolyDataMapper2D;
import vtk.vtkProp;
import vtk.vtkProperty;
import vtk.vtkScalarBarActor;
import vtk.vtkTextActor;
import vtk.vtkTextProperty;
import vtk.vtkUnsignedCharArray;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.ConvertResourceToFile;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.Preferences;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.europa.math.V3;
import edu.jhuapl.sbmt.model.europa.math.VectorOps;
import edu.jhuapl.sbmt.model.europa.time.TimeUtils;


public class StateHistory extends AbstractModel implements PropertyChangeListener, ListModel
{
    // constants
    private double fovDepthFudgeFactor = 2.0;
    private double[] zero = {0.0, 0.0, 0.0};
//  private double[] boresightOffset = {0.0, 100.0, 0.0};
    private double[] spacecraftFovOffset = {0.0, 0.0, 0.5};
    private double[] monolithBodyOffset = { 0.0, 0.0, 0.0 };
//    private double[] monolithBodyBounds = { -0.09, 0.09, -0.04, 0.04, -0.01, 0.01 };
    private double[] monolithBodyBounds = { -9.0, 9.0, -4.0, 4.0, -1.0, 1.0 };
//  private double[] monolithBodyOffset = { -3.0, 0.0, 0.0 };
//    private double[] monolithBodyOffset = { 9.0, 4.0, 1.0 };

    private double[] trajectoryColor = {0.0, 1.0, 1.0, 1.0};
    private double[] monolithColor = {0.2, 0.2, 0.2, 1.0};

//    private double iconScale = 3.0;
//    private double[] spacecraftColor = {1.0, 0.9, 0.1, 1.0};
//    private double[] fovColor = {0.3, 0.3, 1.0, 1.0};

    private double iconScale = 10.0;
  private double[] spacecraftColor = {1.0, 0.7, 0.4, 1.0};
  private double[] fovColor = {0.3, 0.3, 1.0, 0.5};

    private double[] white = {1.0, 1.0, 1.0, 1.0};

    private int RECON = 0;
    private int SWIRS = 1;
    private int instrument = SWIRS;
    private String[] instrumentNames = { "RECON", "SWIRS" };
    private double[] instrumentIFovs = { 10.0e-6, 150.0e-6 }; // in radians
    private int[] instrumentLines = { 128, 1 };
    private int[] instrumentLineSamples = { 9216, 480 };

    public double[] getFov(String instrumentName)
    {
        for (int i=0; i<instrumentNames.length; i++)
            if (instrumentNames[i].equals(instrumentName))
            {
                double ifov = instrumentIFovs[i];
                int lines = instrumentLines[i];
                int samples = instrumentLineSamples[i];
                double width = samples * ifov;
                double height = lines * ifov;
                double[] result = { width, height };
                return result;
            }
        return null;
    }

    public static final String RUN_NAMES = "RunNames"; // What name to give this image for display
    public static final String RUN_FILENAMES = "RunFilenames"; // Filename of image on disk

    // tables
    private Map<String, FlybyStateHistory> nameToFlybyStateHistory = new HashMap<String, FlybyStateHistory>();

    private List<String> trajectoryNames = new ArrayList<String>();
    private HashMap<String, Integer> nameToTrajectoryIndex = new HashMap<String, Integer>();
    private HashMap<String, Trajectory> nameToTrajectory = new HashMap<String, Trajectory>();
    private HashMap<Integer, Trajectory> indexToTrajectory = new HashMap<Integer, Trajectory>();
    private HashMap<Integer, Trajectory> cellIdToTrajectory = new HashMap<Integer, Trajectory>();
    private HashMap<vtkProp, Trajectory> propToTrajectory = new HashMap<vtkProp, Trajectory>();

    private int ntrajectories;
    private Trajectory trajectories[];
    private vtkPolyData trajectoryPolylines[];
    private vtkActor trajectoryActors[];


//    private vtkCylinderSource spacecraftBoresight;
    private vtkPolyData spacecraftBody;
    private vtkCubeSource monolithBody;
    private vtkConeSource spacecraftFov;

//    private vtkActor spacecraftBoresightActor;
    private vtkCaptionActor2D spacecraftLabelActor;
    private vtkActor spacecraftBodyActor;
    private vtkActor monolithBodyActor;
    private vtkActor spacecraftFovActor;

    private ArrayList<vtkProp> stateHistoryActors = new ArrayList<vtkProp>();

    private boolean showSpacecraft;
    private Set<String> visibleTrajectories;
    private double offset = offsetHeight;

    private vtkCylinderSource testCylinder;
    private vtkActor testActor;
    private void createTestPolyData()
    {
        testCylinder = new vtkCylinderSource();
        testCylinder.SetCenter(zero);
        testCylinder.SetRadius(300.0);
        testCylinder.SetHeight(600.0);
        testCylinder.SetResolution(20);
    }

    public enum StateHistorySource
    {
        CLIPPER {
            public String toString()
            {
                return "Europa Clipper Derived";
            }
        }
    }

    /**
     * An StateHistoryKey should be used to uniquely distinguish one image from another.
     * No two images will have the same values for the fields of this class.
     */
    public static class StateHistoryKey
    {
        public String name;

        public StateHistorySource source;

        public StateHistoryKey()
        {
        }

        public StateHistoryKey(String name, StateHistorySource source)
        {
            this.name = name;
            this.source = source;

        }

        @Override
        public boolean equals(Object obj)
        {
            return name.equals(((StateHistoryKey)obj).name) && source == ((StateHistoryKey)obj).source;
        }
    }

    protected final StateHistoryKey key;
    private SmallBodyModel smallBodyModel;

    private String currentTrajectoryName;
    private Trajectory currentTrajectory;

    private FlybyStateHistory currentFlybyStateHistory;

    public Trajectory getCurrentTrajectory()
    {
        return currentTrajectory;
    }

    public void setCurrentTrajectory(Trajectory currentTrajectory)
    {
        this.currentTrajectory = currentTrajectory;
        currentTrajectoryName = currentTrajectory.getName();
        currentFlybyStateHistory = nameToFlybyStateHistory.get(currentTrajectoryName);
        this.setTimeFraction(0.0);
    }

    public String getCurrentTrajectoryName()
    {
        return currentTrajectoryName;
    }

    public void setCurrentTrajectoryName(String currentTrajectoryName)
    {
        Trajectory trajectory = nameToTrajectory.get(currentTrajectoryName);
        setCurrentTrajectory(trajectory);
    }

    public void setCurrentTrajectoryIndex(Integer index)
    {
        if (index != null)
            setCurrentTrajectory(getTrajectoryByIndex(index));
    }



    static public StateHistory createStateHistory(StateHistoryKey key, SmallBodyModel smallBodyModel)
    {
        return new StateHistory(key, smallBodyModel);
    }

    public StateHistory(StateHistoryKey key, SmallBodyModel smallBodyModel)
    {
        this.key = key;
        this.smallBodyModel = smallBodyModel;

        initialize();

    }

    private List<String> passFileNames = new ArrayList<String>();



    private void initialize()
    {
        if (trajectoryActors == null)
        {
            try
            {
                loadStateHistory();

                loadAreaCalculationCollection();

                createTrajectoryPolyData();

                trajectoryActors = new vtkActor[ntrajectories];

                for (int itraj=0; itraj<ntrajectories; itraj++)
                {
                    Trajectory traj = trajectories[itraj];
                    vtkPolyDataMapper trajectoryMapper = new vtkPolyDataMapper();
                    trajectoryMapper.SetInputData(trajectoryPolylines[itraj]);

                    vtkActor actor = new vtkActor();
                    trajectoryActors[itraj] = actor;
                    trajectoryActors[itraj].SetMapper(trajectoryMapper);
                    this.propToTrajectory.put(actor, traj);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("Created " + this.nameToTrajectory.size() + "trajectories");
        }

        if (spacecraftBodyActor == null)
        {
            try
            {
                createSpacecraftPolyData();

//                vtkPolyDataMapper spacecraftBoresightMapper = new vtkPolyDataMapper();
//                spacecraftBoresightMapper.SetInput(spacecraftBoresight.GetOutput());
//                spacecraftBoresightActor = new vtkActor();
//                spacecraftBoresightActor.SetMapper(spacecraftBoresightMapper);
//                spacecraftBoresightActor.GetProperty().SetDiffuseColor(spacecraftColor);
//                spacecraftBoresightActor.GetProperty().SetSpecularColor(white);
//                spacecraftBoresightActor.GetProperty().SetSpecular(0.5);
//                spacecraftBoresightActor.GetProperty().SetSpecularPower(100.0);
//                spacecraftBoresightActor.GetProperty().ShadingOn();
//                spacecraftBoresightActor.GetProperty().SetInterpolationToPhong();

                vtkPolyDataMapper monolithBodyMapper = new vtkPolyDataMapper();
                monolithBodyMapper.SetInputData(monolithBody.GetOutput());
                monolithBodyActor = new vtkActor();
                monolithBodyActor.SetMapper(monolithBodyMapper);
                monolithBodyActor.GetProperty().SetDiffuseColor(monolithColor);
                monolithBodyActor.GetProperty().SetSpecularColor(white);
                monolithBodyActor.GetProperty().SetSpecular(0.8);
                monolithBodyActor.GetProperty().SetSpecularPower(80.0);
                monolithBodyActor.GetProperty().ShadingOn();
                monolithBodyActor.GetProperty().SetInterpolationToPhong();

                vtkPolyDataMapper spacecraftBodyMapper = new vtkPolyDataMapper();
                spacecraftBodyMapper.SetInputData(spacecraftBody);
                spacecraftBodyActor = new vtkActor();
                spacecraftBodyActor.SetMapper(spacecraftBodyMapper);
                spacecraftBodyActor.GetProperty().SetDiffuseColor(spacecraftColor);
                spacecraftBodyActor.GetProperty().SetSpecularColor(white);
                spacecraftBodyActor.GetProperty().SetSpecular(0.8);
                spacecraftBodyActor.GetProperty().SetSpecularPower(80.0);
                spacecraftBodyActor.GetProperty().ShadingOn();
                spacecraftBodyActor.GetProperty().SetInterpolationToFlat();

                spacecraftLabelActor = new vtkCaptionActor2D();
                spacecraftLabelActor.SetCaption("Hello");
//                spacecraftLabelActor.GetProperty().SetColor(1.0, 1.0, 1.0);
                spacecraftLabelActor.GetCaptionTextProperty().SetColor(1.0, 1.0, 1.0);
                spacecraftLabelActor.GetCaptionTextProperty().SetJustificationToLeft();
                spacecraftLabelActor.GetCaptionTextProperty().BoldOff();
                spacecraftLabelActor.GetCaptionTextProperty().ShadowOff();
//                spacecraftLabelActor.GetCaptionTextProperty().ItalicOff();

                spacecraftLabelActor.SetPosition(0.0, 0.0);
                spacecraftLabelActor.SetWidth(0.2);
//                spacecraftLabelActor.SetPosition2(30.0, 20.0);
                spacecraftLabelActor.SetBorder(0);
                spacecraftLabelActor.SetLeader(0);
                spacecraftLabelActor.VisibilityOn();


                vtkPolyDataMapper spacecraftFovMapper = new vtkPolyDataMapper();
                spacecraftFovMapper.SetInputData(spacecraftFov.GetOutput());
                spacecraftFovActor = new vtkActor();
                spacecraftFovActor.SetMapper(spacecraftFovMapper);
                spacecraftFovActor.GetProperty().SetDiffuseColor(fovColor);
                spacecraftFovActor.GetProperty().SetSpecularColor(white);
                spacecraftFovActor.GetProperty().SetSpecular(0.5);
                spacecraftFovActor.GetProperty().SetSpecularPower(100.0);
                spacecraftFovActor.GetProperty().SetOpacity(fovColor[3]);
                spacecraftFovActor.GetProperty().ShadingOn();
                spacecraftFovActor.GetProperty().SetInterpolationToPhong();

                // By default do not show the trajectories
                //trajectoryActors.add(trajectoryActor);

            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            System.out.println("Created spacecraft actor");
        }

        if (timeBarActor == null)
            setupTimeBar();

        if (scalarBarActor == null)
            setupScalarBar();

//        if (testActor == null)
//        {
//            this.createTestPolyData();
//            vtkPolyDataMapper spacecraftMapper = new vtkPolyDataMapper();
//            spacecraftMapper.SetInput(spacecraftCylinder.GetOutput());
//
//            testActor = new vtkActor();
//            testActor.SetMapper(spacecraftMapper);
//        }
    }

    private AreaCalculationCollection areaCalculationList = null;
    public AreaCalculationCollection getAreaCalculationCollection()
    {
        return areaCalculationList;
    }

    private void loadAreaCalculationCollection()
    {
        String runName = getKey().name;
        File missionFile = new File(runName);
        String runDirName = missionFile.getParent();

        areaCalculationList = new StandardAreaCalculationCollection(runDirName, this, smallBodyModel);
    }

    private void loadStateHistory()
    {
        File missionFile = new File(getKey().name);
        String runDirName = missionFile.getParent();
        File runDir = new File(runDirName);
        File[] runFiles = runDir.listFiles();
        Integer firstIndex = null;

        System.out.println("Loading Run: " + runDirName);
        trajectoryNames.clear();
        passFileNames.clear();

        nameToTrajectory.clear();
        nameToTrajectoryIndex.clear();
        indexToTrajectory.clear();
        cellIdToTrajectory.clear();
        propToTrajectory.clear();

        if (areaCalculationList != null && areaCalculationList.getCurrentValue() != null)
            areaCalculationList.getCurrentValue().initialize();

        nameToFlybyStateHistory.clear();

        ntrajectories = 0;
        trajectories = new Trajectory[ntrajectories];
        trajectoryPolylines = new vtkPolyData[ntrajectories];
        trajectoryActors = new vtkActor[ntrajectories];

//        patches = new SurfacePatch[ntrajectories];

//        patchPolylines = new vtkPolyData[npatches];
//        patchActors = new vtkActor[npatches];

        for (File file : runFiles)
        {
            String runName = file.getName();
            String[] runNameTokens = runName.split("\\.");
            if (runNameTokens.length == 2 && runName.endsWith(".csv") && (
//                    runName.startsWith("TOPO-MONOPass") ||
//                    runName.startsWith("TOPO-STEREOPass") ||
                    runName.startsWith("SWIRS-HIPass") ||
//                    runName.startsWith("SWIRS-LOPass") ||
//                    runName.startsWith("ThermalImagerPass") ||
                    runName.startsWith("RECONPass")
                  )
               )
            {
                System.out.println("  pass " + runName);
                passFileNames.add(runName);
                ntrajectories++;
            }
        }

        trajectories = new Trajectory[ntrajectories];

        try {
            // iterate over the trajectories
            for (int itraj=0; itraj<ntrajectories; itraj++)
            {
                String dataFileName = passFileNames.get(itraj);
                String name = dataFileName.split("\\.")[0];
                BufferedReader in = new BufferedReader(new FileReader(runDirName + File.separator + dataFileName));

                if (firstIndex == null)
                    firstIndex = 0;

                trajectoryNames.add(name);
                trajectories[itraj] = new StandardTrajectory();

                // fill in the Trajectory parameters
                trajectories[itraj].setName(name);
                trajectories[itraj].setId(itraj);

                nameToTrajectory.put(name, trajectories[itraj]);
                nameToTrajectoryIndex.put(name, itraj);
                indexToTrajectory.put(itraj, trajectories[itraj]);

                // create a new history instance and add it to the Map
                FlybyStateHistory history = new StandardFlybyStateHistory();
                this.nameToFlybyStateHistory.put(name, history);

                // discard first line of column headings
                in.readLine();

                String line;
                while ((line = in.readLine()) != null)
                {
                    // parse line of file
                    FlybyState flybyState = new InstrumentFlybyState().parseStateString(line);

                    // add to history
                    history.put(flybyState);

                    double[] spacecraftPosition = flybyState.getSpacecraftPosition();

                    trajectories[itraj].getX().add(spacecraftPosition[0]);
                    trajectories[itraj].getY().add(spacecraftPosition[1]);
                    trajectories[itraj].getZ().add(spacecraftPosition[2]);
                }
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.setCurrentTrajectoryIndex(firstIndex);
    }


    private AreaCalculation areaCalculation;

    public AreaCalculation getAreaCalculation()
    {
        return areaCalculation;
    }

    public void setAreaCalculation(AreaCalculation areaCalculation)
    {
        this.areaCalculation = areaCalculation;

        System.out.println("Loading Area Calculation Actors: " + areaCalculation.getName());
        areaCalculation.initialize();
//        setShowPatches(new HashSet<String>(areaCalculation.getPatchNames()));
    }


    private void createTrajectoryPolyData()
    {
        trajectoryPolylines = new vtkPolyData[ntrajectories];

        int cellId = 0;
        vtkIdList idList = new vtkIdList();

        for (int itraj=0; itraj<ntrajectories; itraj++)
        {
            vtkPoints points = new vtkPoints();
            vtkCellArray lines = new vtkCellArray();
            vtkUnsignedCharArray colors = new vtkUnsignedCharArray();
            colors.SetNumberOfComponents(4);

            Trajectory traj =  trajectories[itraj];
            traj.setCellId(cellId);

            int size = traj.getX().size();
            idList.SetNumberOfIds(size);

            for (int i=0;i<size;++i)
            {
                Double x = traj.getX().get(i);
                Double y = traj.getY().get(i);
                Double z = traj.getZ().get(i);

                points.InsertNextPoint(x, y, z);
                idList.SetId(i, i);
            }

            lines.InsertNextCell(idList);
            colors.InsertNextTuple4(255.0 * trajectoryColor[0], 255.0 * trajectoryColor[1], 255.0 * trajectoryColor[2], 255.0 * trajectoryColor[3]);

            cellIdToTrajectory.put(cellId, traj);
            ++cellId;

            vtkPolyData trajectoryPolyline = new vtkPolyData();
            trajectoryPolyline.SetPoints(points);
            trajectoryPolyline.SetLines(lines);
            trajectoryPolyline.GetCellData().SetScalars(colors);

            trajectoryPolylines[itraj] = trajectoryPolyline;
        }
    }

    public static final double offsetHeight = 2.0;


    private void initializeSpacecraftBody(File modelFile)
    {
        try
        {
//            vtkPolyData vtkData = new vtkPolyData();
//            vtkData.ShallowCopy(PolyDataUtil.loadShapeModel(modelFile.getAbsolutePath()));
            vtkPolyData vtkData = PolyDataUtil.loadShapeModel(modelFile.getAbsolutePath());
            spacecraftBody = vtkData;
            vtkPolyDataMapper spacecraftBodyMapper = new vtkPolyDataMapper();
            spacecraftBodyMapper.SetInputData(vtkData);

            spacecraftBodyActor = new vtkActor();
            spacecraftBodyActor.SetMapper(spacecraftBodyMapper);
            vtkProperty spacecraftBodyProperty =  spacecraftBodyActor.GetProperty();
//            spacecraftBodyProperty.SetInterpolationToFlat();
//            spacecraftBodyProperty.SetOpacity(0.1);
//            spacecraftBodyProperty.SetSpecular(.1);
//            spacecraftBodyProperty.SetSpecularPower(100);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void createSpacecraftPolyData()
    {
//      spacecraftBoresight = new vtkCylinderSource();
//      spacecraftBoresight.SetCenter(cylinderOffset);
//      spacecraftBoresight.SetRadius(3.0);
//      spacecraftBoresight.SetHeight(200.0);
//      spacecraftBoresight.SetResolution(20);

        monolithBody = new vtkCubeSource();
        monolithBody.SetCenter(zero);
        monolithBody.SetBounds(monolithBodyBounds);
        monolithBody.SetCenter(monolithBodyOffset);
        monolithBody.Update();

        String spacecraftFileName = "/edu/jhuapl/sbmt/data/cassini-9k.stl";
        initializeSpacecraftBody(ConvertResourceToFile.convertResourceToRealFile(this, spacecraftFileName, Configuration.getApplicationDataDir()));

        spacecraftFov = new vtkConeSource();
        spacecraftFov.SetDirection(0.0, 0.0, -1.0);
        spacecraftFov.SetRadius(0.5);
        spacecraftFov.SetCenter(spacecraftFovOffset);
        spacecraftFov.SetHeight(1.0);
        spacecraftFov.SetResolution(4);
        spacecraftFov.Update();
    }

    public Trajectory getTrajectoryByCellId(int cellId)
    {
        return this.cellIdToTrajectory.get(cellId);
    }

    public Trajectory getTrajectory(vtkProp prop)
    {
        return this.propToTrajectory.get(prop);
    }

    public SurfacePatch getSurfacePatch(vtkProp prop)
    {
        SurfacePatch result = null;
        if (getAreaCalculation() != null)
            result = getAreaCalculation().getPatch(prop);
        return result;
    }

    public Double getSurfacePatchValue(vtkProp prop, int cellId, double[] pickPosition)
    {
//        System.out.println("Surface Patch Value at " + prop.GetVTKId() + ", " + cellId + ", (" + pickPosition[0] + ", " + pickPosition[1] + ", " + pickPosition[2] + ")");
        Double result = null;
        if (getAreaCalculation() != null)
            result = getAreaCalculation().getSurfacePatchValue(prop, cellId, pickPosition);
        return result;
    }

    public Trajectory getTrajectoryByIndex(int index)
    {
        return this.indexToTrajectory.get(index);
    }

    public Trajectory getTrajectoryByName(String name)
    {
        return nameToTrajectory.get(name);
    }

    public void setTrajectoryColor(int cellId, int[] color)
    {
//        Trajectory traj = cellIdToTrajectory.get(cellId);
//        trajectoryPolyline.GetCellData().GetScalars().SetTuple4(cellId, color[0], color[1], color[2], color[3]);
//        trajectoryPolyline.Modified();
//        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void setsAllTrajectoriesColor(int[] color)
    {
//        int numTrajectories = this.cellIdToTrajectory.size();
//        vtkDataArray colors = trajectoryPolyline.GetCellData().GetScalars();
//
//        for (int i=0; i<numTrajectories; ++i)
//            colors.SetTuple4(i, color[0], color[1], color[2], color[3]);
//
//        trajectoryPolyline.Modified();
//        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }



    public void setPatchColor(int cellId, int[] color)
    {
    }

    public void setsAllPatchesColor(int[] color)
    {
    }


    private Double time;
    public Double getTime()
    {
        return time;
    }

    public void setTime(Double time)
    {
        this.time = time;
        if (currentFlybyStateHistory != null)
            currentFlybyStateHistory.setTime(time);
//        System.out.println("CurrentTime of " + currentTrajectoryId + " is " + time);
    }

    public Double getTimeFraction()
    {
        if (currentFlybyStateHistory != null)
            return currentFlybyStateHistory.getTimeFraction();
        else
            return null;
    }

    public static final double europaRadius = 1560.8;
    public static final double fovWidthFudge = 1.3;

    public void setTimeFraction(double timeFraction)
    {
        if (currentFlybyStateHistory != null && spacecraftBodyActor != null)
        {
            // set the time
            currentFlybyStateHistory.setTimeFraction(timeFraction);
            setTime(currentFlybyStateHistory.getTime());

            if (timeBarActor != null)
            {
                updateTimeBarValue(getTime());
            }

            // get the current FlybyState
            FlybyState state = currentFlybyStateHistory.getCurrentValue();
            double[] spacecraftPosition = state.getSpacecraftPosition();
            double spacecraftRotationX = state.getRollAngle();
            double spacecraftRotationY = state.getViewingAngle();

            double velocity[] = state.getSpacecraftVelocity();
            double speed = Math.sqrt(velocity[0]*velocity[0] + velocity[1]*velocity[1] + velocity[2]*velocity[2]);

            double[] p1 = { spacecraftPosition[0]-velocity[0], spacecraftPosition[1]-velocity[1], spacecraftPosition[2]-velocity[2] };
            double[] p2 = { spacecraftPosition[0]+velocity[0], spacecraftPosition[1]+velocity[1], spacecraftPosition[2]+velocity[2] };

            V3 v1 = new V3(p1);
            V3 v2 = new V3(p2);
            double groundSpeed = VectorOps.AngularSep(v1, v2) * europaRadius * 0.50;

            double distance = Math.sqrt(spacecraftPosition[0]*spacecraftPosition[0] + spacecraftPosition[1]*spacecraftPosition[1] + spacecraftPosition[2]*spacecraftPosition[2]);
            double altitude = distance - europaRadius;

            String speedText = String.format("%7.1f km %7.3f km/sec   .", altitude, groundSpeed);

//            System.out.println("Speed: " + speed + ", Ground Speed: " + groundSpeed);

            // hardcoded to RECON # pixels for now

            double fovDepth = state.getSpacecraftAltitude() * fovDepthFudgeFactor;

//            double[] fovTargetPoint = state.getSurfaceIntercept();
//            double[] fovTargetPoint = { 0.0, 0.0, 0.0 };
//            double[] fovDelta = { fovTargetPoint[0] - spacecraftPosition[0],  fovTargetPoint[1] - spacecraftPosition[1], fovTargetPoint[2] - spacecraftPosition[2] };
//            double fovDepth = Math.sqrt(fovDelta[0]*fovDelta[0] + fovDelta[1]*fovDelta[1] + fovDelta[2]*fovDelta[2]);

            double fovWidth = fovDepth * instrumentIFovs[instrument] * instrumentLineSamples[instrument] * fovWidthFudge;
            double fovHeight = fovDepth * instrumentIFovs[instrument] * instrumentLines[instrument];

            // this is to make the FOV a rectangle shape rather than a diamond
            double spacecraftRotationZ = Math.toRadians(45.0);

            // set the current orientation
            double[] xaxis = state.getSpacecraftXAxis();
            double[] yaxis = state.getSpacecraftYAxis();
            double[] zaxis = state.getSpacecraftZAxis();

            // create spacecraft matrices
            vtkMatrix4x4 spacecraftBodyMatrix = new vtkMatrix4x4();
            vtkMatrix4x4 spacecraftIconMatrix = new vtkMatrix4x4();
            vtkMatrix4x4 fovMatrix = new vtkMatrix4x4();
            vtkMatrix4x4 fovRotateXMatrix = new vtkMatrix4x4();
            vtkMatrix4x4 fovRotateYMatrix = new vtkMatrix4x4();
            vtkMatrix4x4 fovRotateZMatrix = new vtkMatrix4x4();
            vtkMatrix4x4 fovScaleMatrix = new vtkMatrix4x4();
            vtkMatrix4x4 spacecraftInstrumentMatrix = new vtkMatrix4x4();

            // set to identity
            spacecraftBodyMatrix.Identity();
            spacecraftIconMatrix.Identity();
            fovMatrix.Identity();
            fovRotateXMatrix.Identity();
            fovRotateYMatrix.Identity();
            fovRotateZMatrix.Identity();

            // set body orientation matrix
            for (int i=0; i<3; i++)
            {
                spacecraftBodyMatrix.SetElement(i, 0, xaxis[i]);
                spacecraftBodyMatrix.SetElement(i, 1, yaxis[i]);
                spacecraftBodyMatrix.SetElement(i, 2, zaxis[i]);
            }

            // create the icon matrix, which is just the body matrix scaled by a factor
            for (int i=0; i<3; i++)
                spacecraftIconMatrix.SetElement(i, i, iconScale);
            spacecraftIconMatrix.Multiply4x4(spacecraftIconMatrix, spacecraftBodyMatrix, spacecraftIconMatrix);

            // rotate the FOV about the Z axis
            double sinRotZ = Math.sin(spacecraftRotationZ);
            double cosRotZ = Math.cos(spacecraftRotationZ);
            fovRotateZMatrix.SetElement(0, 0, cosRotZ);
            fovRotateZMatrix.SetElement(1, 1, cosRotZ);
            fovRotateZMatrix.SetElement(0, 1, sinRotZ);
            fovRotateZMatrix.SetElement(1, 0, -sinRotZ);

            // scale the FOV
            fovScaleMatrix.SetElement(0, 0, fovHeight);
            fovScaleMatrix.SetElement(1, 1, fovWidth);
            fovScaleMatrix.SetElement(2, 2, fovDepth);

            // rotate the FOV about the Y axis
            double sinRotY = Math.sin(spacecraftRotationY);
            double cosRotY = Math.cos(spacecraftRotationY);
            fovRotateYMatrix.SetElement(0, 0, cosRotY);
            fovRotateYMatrix.SetElement(2, 2, cosRotY);
            fovRotateYMatrix.SetElement(0, 2, sinRotY);
            fovRotateYMatrix.SetElement(2, 0, -sinRotY);

            fovMatrix.Multiply4x4(fovScaleMatrix, fovRotateZMatrix, spacecraftInstrumentMatrix);
            fovMatrix.Multiply4x4(fovRotateYMatrix, spacecraftInstrumentMatrix, spacecraftInstrumentMatrix);

//            spacecraftFovMatrix.Multiply4x4(fovRotateYMatrix, fovRotateZMatrix, spacecraftInstrumentMatrix);

            fovMatrix.Multiply4x4(spacecraftBodyMatrix, spacecraftInstrumentMatrix, fovMatrix);

            // set translation
            for (int i=0; i<3; i++)
            {
                spacecraftBodyMatrix.SetElement(i, 3, spacecraftPosition[i]);
                spacecraftIconMatrix.SetElement(i, 3, spacecraftPosition[i]);
                fovMatrix.SetElement(i, 3, spacecraftPosition[i]);
            }

//            spacecraftBoresightActor.SetUserMatrix(matrix);

            monolithBodyActor.SetUserMatrix(spacecraftBodyMatrix);

            spacecraftBodyActor.SetUserMatrix(spacecraftIconMatrix);

            spacecraftLabelActor.SetAttachmentPoint(spacecraftPosition);
            spacecraftLabelActor.SetCaption(speedText);

            spacecraftFovActor.SetUserMatrix(fovMatrix);
//            spacecraftFovActor.SetUserMatrix(spacecraftBodyMatrix);

//            spacecraftBoresight.Modified();
            monolithBody.Modified();
            spacecraftBody.Modified();
            spacecraftFov.Modified();

            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }

    public void setOffset(double offset)
    {
        this.offset = offset;
        areaCalculation.setOffset(offset);
        this.updateActorVisibility();
        Set<String> visiblePatches = new HashSet<String>();
        for (int i=0; i<areaCalculation.getSize(); i++)
            visiblePatches.add(areaCalculation.getValue(i).getName());
        setShowPatches(visiblePatches);
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public double getOffset()
    {
        return offset;
    }

    public void setShowSpacecraft(boolean show)
    {
        this.showSpacecraft = show;
        updateActorVisibility();
//        if (show)
//        {
////            stateHistoryActors.add(spacecraftBoresightActor);
//
//            stateHistoryActors.add(monolithBodyActor);
//            stateHistoryActors.add(spacecraftBodyActor);
//            stateHistoryActors.add(spacecraftLabelActor);
//            stateHistoryActors.add(spacecraftFovActor);
//        }
//        else
//        {
////            stateHistoryActors.remove(spacecraftBoresightActor);
//
//            stateHistoryActors.remove(monolithBodyActor);
//            stateHistoryActors.remove(spacecraftBodyActor);
//            stateHistoryActors.remove(spacecraftLabelActor);
//            stateHistoryActors.remove(spacecraftFovActor);
//        }
    }

    public void updateActorVisibility()
    {
        stateHistoryActors.clear();

        if (showSpacecraft)
        {
//            stateHistoryActors.add(spacecraftBoresightActor);
            stateHistoryActors.add(monolithBodyActor);
            stateHistoryActors.add(spacecraftBodyActor);
            stateHistoryActors.add(spacecraftLabelActor);
            stateHistoryActors.add(spacecraftFovActor);
        }

        if (visibleTrajectories != null)
            for (int itraj=0; itraj<ntrajectories; itraj++)
            {
                if (visibleTrajectories.contains(trajectories[itraj].getName()))
                    stateHistoryActors.add(trajectoryActors[itraj]);
            }

        if (areaCalculation != null)
        {
            areaCalculation.initializePatches();
            stateHistoryActors.addAll(areaCalculation.getVisibleActors());
        }

        if (showTimeBar)
        {
            stateHistoryActors.add(timeBarActor);
            stateHistoryActors.add(timeBarTextActor);
        }

        if (showScalarBar)
        {
            stateHistoryActors.add(scalarBarActor);
        }

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void setShowTrajectories(boolean show)
    {
        if (show)
        {
            if (stateHistoryActors.isEmpty())
            {
                initialize();

                for (int itraj=0; itraj<ntrajectories; itraj++)
                    stateHistoryActors.add(trajectoryActors[itraj]);
                if (this.showSpacecraft)
                {
//                    stateHistoryActors.add(spacecraftBoresightActor);
                    stateHistoryActors.add(monolithBodyActor);
                    stateHistoryActors.add(spacecraftBodyActor);
                    stateHistoryActors.add(spacecraftLabelActor);
                    stateHistoryActors.add(spacecraftFovActor);
                }
                this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
            }
        }
        else
        {
            if (!stateHistoryActors.isEmpty())
            {
                stateHistoryActors.clear();
                this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
            }
        }

    }

    public void setShowTrajectories(Set<String> trajectoryNames)
    {
        this.visibleTrajectories = trajectoryNames;
        updateActorVisibility();
    }

    public void setShowPatches(Set<String> patchNames)
    {
        if (areaCalculation != null)
        {
            areaCalculation.setShowPatches(patchNames);
            updateActorVisibility();
        }
    }

    public ArrayList<vtkProp> getProps()
    {
        return stateHistoryActors;
    }

    //
    // data display legends
    //

    // variables related to the scale bar (note the scale bar is different
    // from the scalar bar)
    private vtkPolyData timeBarPolydata;
    private vtkPolyDataMapper2D timeBarMapper;
    private vtkActor2D timeBarActor;
    private vtkTextActor timeBarTextActor;
    private int timeBarWidthInPixels = 0;
    private double timeBarValue = -1.0;
    private boolean showTimeBar = true;
    private boolean showScalarBar = true;

    // variables related to the scalar bar
    private vtkScalarBarActor scalarBarActor;
//    private int coloringIndex = -1;
    private int coloringIndex = 1;

    public void setColoringIndex(int index) throws IOException
    {
        if (coloringIndex != index)
        {
            coloringIndex = index;
        }
    }

    public int getColoringIndex()
    {
        return coloringIndex;
    }

    public void updateScalarBar()
    {
//        initializeActorsAndMappers();
//
//        loadColoringData();

        vtkDataArray array = null;

        if (this.areaCalculation != null)
        {
            int index = areaCalculation.getCurrentIndex();
            SurfacePatch currentPatch = areaCalculation.getCurrentPatch();
            ScalarRange currentRange = areaCalculation.getScalarRange();

            if (coloringIndex >= 0)
            {
    //            ColoringInfo info = coloringInfo.get(coloringIndex);
    //            array = info.coloringValues;
    //            String title = info.coloringName;
    //            if (!info.coloringUnits.isEmpty())
    //                title += " (" + info.coloringUnits + ")";
    //            String title = "Scalar Bar";
//                String title = currentRange.getCurrentRangeType();
                String title = currentPatch.getCurrentDataType();
                scalarBarActor.SetTitle(title);
            }
            if (coloringIndex < 0)
            {
                showScalarBar = false;
            }
            else
            {
                showScalarBar = true;

    //            ColoringInfo info = coloringInfo.get(coloringIndex);
                if (this.areaCalculation != null)
                {
                    System.out.println("Heat map index: " + index);
                    PolyDataHeatMap patchHeatMap = areaCalculation.getPatchHeatMapActor(index);
//                    vtkPolyDataMapper heatMapper = patchHeatMap.getMapper();
//                    scalarBarActor.SetLookupTable(heatMapper.GetLookupTable());
                    scalarBarActor.SetLookupTable(patchHeatMap.getLookupTable());
                }
            }

            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }

    private void setupScalarBar()
    {
        scalarBarActor = new vtkScalarBarActor();
        vtkCoordinate coordinate = scalarBarActor.GetPositionCoordinate();
        coordinate.SetCoordinateSystemToNormalizedViewport();
        coordinate.SetValue(0.2, 0.01);
        scalarBarActor.SetOrientationToHorizontal();
        scalarBarActor.SetWidth(0.6);
        scalarBarActor.SetHeight(0.1275);
        vtkTextProperty tp = new vtkTextProperty();
        tp.SetFontSize(10);
        scalarBarActor.SetTitleTextProperty(tp);
        scalarBarActor.SetLookupTable(new vtkLookupTable());
    }

    private void setupTimeBar()
    {
        timeBarPolydata = new vtkPolyData();
        vtkPoints points = new vtkPoints();
        vtkCellArray polys = new vtkCellArray();
        timeBarPolydata.SetPoints(points);
        timeBarPolydata.SetPolys(polys);

        points.SetNumberOfPoints(4);

        vtkIdList idList = new vtkIdList();
        idList.SetNumberOfIds(4);
        for (int i=0; i<4; ++i)
            idList.SetId(i, i);
        polys.InsertNextCell(idList);

        timeBarMapper = new vtkPolyDataMapper2D();
        timeBarMapper.SetInputData(timeBarPolydata);

        timeBarActor = new vtkActor2D();
        timeBarActor.SetMapper(timeBarMapper);

        timeBarTextActor = new vtkTextActor();

        stateHistoryActors.add(timeBarActor);
        stateHistoryActors.add(timeBarTextActor);

        timeBarActor.GetProperty().SetColor(0.0, 0.0, 0.0);
        timeBarActor.GetProperty().SetOpacity(0.0);
        timeBarTextActor.GetTextProperty().SetColor(1.0, 1.0, 1.0);
        timeBarTextActor.GetTextProperty().SetJustificationToCentered();
        timeBarTextActor.GetTextProperty().BoldOn();

//        timeBarActor.VisibilityOff();
//        timeBarTextActor.VisibilityOff();
        timeBarActor.VisibilityOn();
        timeBarTextActor.VisibilityOn();

        showTimeBar = Preferences.getInstance().getAsBoolean(Preferences.SHOW_SCALE_BAR, true);
    }

    public void updateTimeBarPosition(int windowWidth, int windowHeight)
    {
        vtkPoints points = timeBarPolydata.GetPoints();

        int newTimeBarWidthInPixels = (int)Math.min(0.75*windowWidth, 200.0);

        timeBarWidthInPixels = newTimeBarWidthInPixels;
        int timeBarHeight = timeBarWidthInPixels/9;
        int buffer = timeBarWidthInPixels/20;
        int x = buffer; // lower left corner x
//        int x = windowWidth - timeBarWidthInPixels - buffer; // lower right corner x
        int y = buffer; // lower left corner y

        points.SetPoint(0, x, y, 0.0);
        points.SetPoint(1, x+timeBarWidthInPixels, y, 0.0);
        points.SetPoint(2, x+timeBarWidthInPixels, y+timeBarHeight, 0.0);
        points.SetPoint(3, x, y+timeBarHeight, 0.0);

        timeBarTextActor.SetPosition(x+timeBarWidthInPixels/2, y+2);
        timeBarTextActor.GetTextProperty().SetFontSize(timeBarHeight-4);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void updateTimeBarValue(double time)
    {
        timeBarValue = time;
//        timeBarTextActor.SetInput(String.format("%.2f sec", timeBarValue));
        String utcValue = TimeUtils.et2UTCString(timeBarValue);
        timeBarTextActor.SetInput(utcValue.trim());

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void setShowTimeBar(boolean enabled)
    {
        this.showTimeBar = enabled;
        // The following forces the scale bar to be redrawn.
//        timeBarValue = -1.0;
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        // Note that we call firePropertyChange *twice*. Not really sure why.
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public boolean getShowTimeBar()
    {
        return showTimeBar;
    }

    public void setShowScalarBar(boolean enabled)
    {
        this.showScalarBar = enabled;
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        // Note that we call firePropertyChange *twice*. Not really sure why.
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public boolean getShowScalarBar()
    {
        return showScalarBar;
    }

    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
//        StateHistory.Trajectory traj = getTrajectoryByCellId(cellId);
        Trajectory traj = getTrajectory(prop);
        if (traj != null)
        {
            // putting selection code here until we get around to implementing a FlybyPicker -turnerj1
//            this.setCurrentTrajectoryId(traj.id);
            return "Trajectory " + traj.getId() + " = " + traj.getName() + " contains " + traj.getX().size() + " vertices";
        }
        else
        {
            SurfacePatch patch = getSurfacePatch(prop);
            if (patch != null)
            {
                String dataType = patch.getCurrentDataType();
                if (this.getAreaCalculation() != null)
                {
                    Double value = getAreaCalculation().getSurfacePatchValue(prop, cellId, pickPosition);
                    if (value != null)
                    {
                        return dataType + ": " + value;
                    }
                }
            }
        }

        return "";
    }


    public StateHistoryKey getKey()
    {
        return key;
    }

    public String getRunName()
    {
        return new File(key.name).getName();
    }

    public void imageAboutToBeRemoved()
    {
        // By default do nothing. Let subclasses handle this.
    }

    @Override
    public void propertyChange(PropertyChangeEvent arg0)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void addListDataListener(ListDataListener l)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public Object getElementAt(int index)
    {
        return trajectoryNames.get(index);
    }

    @Override
    public int getSize()
    {

        return trajectoryNames.size();
    }

    @Override
    public void removeListDataListener(ListDataListener l)
    {
        // TODO Auto-generated method stub

    }


}
