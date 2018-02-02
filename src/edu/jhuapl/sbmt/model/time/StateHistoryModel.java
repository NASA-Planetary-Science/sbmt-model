package edu.jhuapl.sbmt.model.time;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.ISODateTimeFormat;

import vtk.vtkActor;
import vtk.vtkActor2D;
import vtk.vtkAssembly;
import vtk.vtkBMPWriter;
import vtk.vtkCaptionActor2D;
import vtk.vtkCellArray;
import vtk.vtkConeSource;
import vtk.vtkCubeSource;
import vtk.vtkCylinderSource;
import vtk.vtkIdList;
import vtk.vtkJPEGWriter;
import vtk.vtkMatrix4x4;
import vtk.vtkPNGWriter;
import vtk.vtkPNMWriter;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkPolyDataMapper2D;
import vtk.vtkPostScriptWriter;
import vtk.vtkProp;
import vtk.vtkProperty;
import vtk.vtkScalarBarActor;
import vtk.vtkSphereSource;
import vtk.vtkTIFFWriter;
import vtk.vtkTextActor;
import vtk.vtkTransform;
import vtk.vtkUnsignedCharArray;
import vtk.vtkWindowToImageFilter;
import vtk.rendering.jogl.vtkJoglCanvasComponent;

import edu.jhuapl.saavtk.gui.Renderer;
import edu.jhuapl.saavtk.gui.Renderer.LightingType;
import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.util.BoundingBox;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.ConvertResourceToFile;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.Preferences;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.gui.time.StateHistoryPanel;
import edu.jhuapl.sbmt.util.TimeUtil;


public class StateHistoryModel extends AbstractModel implements PropertyChangeListener, TableModel, HasTime, ActionListener
{
    //Use approximate radius of largest solar system body as scale for surface intercept vector.
    private static final double JupiterScale = 75000;
    private double[] zero = {0.0, 0.0, 0.0};
    //  private double[] boresightOffset = {0.0, 100.0, 0.0};

    private double[] spacecraftFovOffset = {0.0, 0.0, 0.5};

    private double[] monolithBodyOffset = { 0.0, 0.0, 0.0 };
    //    private double[] monolithBodyBounds = { -0.09, 0.09, -0.04, 0.04, -0.01, 0.01 };
    private double[] monolithBodyBounds = { -9.0, 9.0, -4.0, 4.0, -1.0, 1.0 };
    //  private double[] monolithBodyOffset = { -3.0, 0.0, 0.0 };
    //    private double[] monolithBodyOffset = { 9.0, 4.0, 1.0 };
    private double markerRadius = 0.5;
    private double markerHeight = 0.5;
    private double[] markerOffset = { 0.0, 0.0, 0.0 };
    private double scalingFactor = 0.0;

    private double[] trajectoryColor = {0, 255, 255, 255};
    private double trajectoryLineThickness = 1;
    private double[] monolithColor = {0.2, 0.2, 0.2, 1.0};
    private double[] spacecraftMarkerColor = {0.0, 1.0, 0.0, 1.0};
    private double[] earthMarkerColor = {0.0, 0.0, 1.0, 1.0};
    private double[] sunMarkerColor = {1.0, 1.0, 0.0, 1.0};
    private boolean move;

    //    private double iconScale = 3.0;
    //    private double[] spacecraftColor = {1.0, 0.9, 0.1, 1.0};
    //    private double[] fovColor = {0.3, 0.3, 1.0, 1.0};

    private double iconScale = 10.0;
    private double[] spacecraftColor = {1.0, 0.7, 0.4, 1.0};
    private double[] fovColor = {0.3, 0.3, 1.0, 0.5};

    private double[] white = {1.0, 1.0, 1.0, 1.0};

    //    private double viewAngle = 30.0;

    private int RECON = 0;
    private int SWIRS = 1;
    private int instrument = SWIRS;
    private String[] instrumentNames = { "RECON", "SWIRS" };
    private double[] instrumentIFovs = { 10.0e-6, 150.0e-6 }; // in radians
    private int[] instrumentLines = { 128, 1 };
    private int[] instrumentLineSamples = { 9216, 480 };
    private int distanceOption;

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
    //    private Map<String, StateHistory> nameToFlybyStateHistory = new HashMap<String, StateHistory>();

    //    private List<String> trajectoryNames = new ArrayList<String>();
    //    private HashMap<String, Integer> nameToTrajectoryIndex = new HashMap<String, Integer>();
    //    private HashMap<String, Trajectory> nameToTrajectory = new HashMap<String, Trajectory>();
    //    private HashMap<Integer, Trajectory> indexToTrajectory = new HashMap<Integer, Trajectory>();
    //    private HashMap<Integer, Trajectory> cellIdToTrajectory = new HashMap<Integer, Trajectory>();
    //    private HashMap<vtkProp, Trajectory> propToTrajectory = new HashMap<vtkProp, Trajectory>();

    private Trajectory trajectory;
    private vtkPolyData trajectoryPolylines;
    private vtkActor trajectoryActor;
    private vtkPolyDataMapper trajectoryMapper = new vtkPolyDataMapper();


    //    private vtkCylinderSource spacecraftBoresight;
    private vtkPolyData spacecraftBody;
    private vtkCubeSource monolithBody;
    private vtkSphereSource spacecraftMarkerBody;
    private vtkCylinderSource earthMarkerBody;
    private vtkConeSource earthMarkerHead;
    private vtkSphereSource sunMarkerHead;
    private vtkConeSource sunMarker;
    private vtkConeSource spacecraftFov;
    private vtkConeSource spacecraftMarkerHead;

    //    private vtkActor spacecraftBoresightActor;
    private vtkCaptionActor2D spacecraftLabelActor;
    private vtkActor spacecraftBodyActor;
    private vtkActor monolithBodyActor;
    private vtkActor spacecraftFovActor;
    private vtkActor spacecraftMarkerActor;
    private vtkActor earthMarkerHeadActor;
    private vtkActor earthMarkerActor;
    private vtkActor sunMarkerHeadActor;
    private vtkActor sunMarkerActor;
    private vtkActor spacecraftMarkerHeadActor;
    private vtkAssembly sunAssembly;

    private ArrayList<vtkProp> stateHistoryActors = new ArrayList<vtkProp>();

    //    private boolean showTrajectories;
    private boolean showSpacecraft;
    private boolean showMonolith;
    private boolean showSpacecraftBody;
    private boolean showSpacecraftLabel;
    private boolean showSpacecraftFov;
    private boolean initialized =false;
    private String trajectoryName = ""; // default name and description fields
    private String description = "desc";
    private File path = null;
    final int lineLength = 121;
    private JFileChooser chooser;
    private JLabel frames, fromLabel, toLabel;
    private JSpinner numFrames;
    private double timeStep;

    private boolean showSpacecraftMarker;
    private boolean showEarthMarker;
    private boolean showSunMarker;
    private boolean showLighting;
    private boolean earthView;
    private boolean sunView;
    private ArrayList<String[]> timeArray = new ArrayList<>(3);

    private boolean visible; // able to be shown
    private boolean showing = false; // currently showing
    private double offset = offsetHeight;

    private double[] sunDirection = { 0.0, 1.0, 0.0 };
    public double[] getSunDirection() { return sunDirection; }

    private vtkCylinderSource testCylinder;
    private vtkActor testActor;

    //    // TODO do we need this?
    //    public enum StateHistorySource
    //    {
    //        CLIPPER {
    //            public String toString()
    //            {
    //                return "Europa Clipper Derived";
    //            }
    //        }
    //    }

    /**
     * A StateHistoryKey should be used to uniquely distinguish one trajectory from another.
     * No two trajectories will have the same values for the fields of this class.
     */
    public static class StateHistoryKey
    {
        public static final Random RAND = new Random();
        public Integer value;

        public StateHistoryKey(StateHistoryCollection runs)
        {
            value = RAND.nextInt(1000);
            while (runs.getKeys().contains(value)) {
                value = RAND.nextInt(1000);
            }
        }

        @Override
        public boolean equals(Object obj)
        {
            return value.equals(((StateHistoryKey)obj).value);
        }

    }

    protected final StateHistoryKey key;
    private SmallBodyModel smallBodyModel;
    private Renderer renderer;

    private StateHistory currentFlybyStateHistory;
    private DateTime startTime;
    private DateTime endTime;



    static public StateHistoryModel createStateHistory(StateHistoryKey key, DateTime start, DateTime end, SmallBodyModel smallBodyModel, Renderer renderer)
    {
        return new StateHistoryModel(key, start, end, smallBodyModel, renderer);
    }

    public StateHistoryModel(StateHistoryKey key, DateTime start, DateTime end, SmallBodyModel smallBodyModel, Renderer renderer)
    {
        this.key = key;
        this.smallBodyModel = smallBodyModel;
        this.renderer = renderer;
        this.startTime = start;
        this.endTime = end;

        initialize();

    }

    public StateHistoryModel(StateHistoryKey key, SmallBodyModel smallBodyModel, Renderer renderer)
    {
        this.key = key;
        this.renderer = renderer;
        this.smallBodyModel = smallBodyModel;
    }

    private List<String> passFileNames = new ArrayList<String>();



    private void initialize()
    {
        initialized = true;
        BoundingBox bb = smallBodyModel.getBoundingBox();
        double width = Math.max((bb.xmax-bb.xmin), Math.max((bb.ymax-bb.ymin), (bb.zmax-bb.zmin)));
        scalingFactor = 30.62*width + -0.0002237;
        if (trajectory == null) {
            trajectory = new StandardTrajectory();
        }
        if (trajectoryActor == null)
        {
            try
            {
                trajectoryPolylines = new vtkPolyData();
                trajectoryActor = new vtkActor();

                createTrajectoryPolyData();

                trajectoryActor = new vtkActor();
                trajectoryMapper.SetInputData(trajectoryPolylines);

                vtkActor actor = new vtkActor();
                trajectoryActor = actor;
                trajectoryActor.SetMapper(trajectoryMapper);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (spacecraftBodyActor == null)
        {
            try
            {
                createSpacecraftPolyData();
                // boresight
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

                // monolith
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
                monolithBodyActor.GetProperty().SetColor(1, 0, 0);

                // spacecraft icon body
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

                // spacecraft label
                spacecraftLabelActor = new vtkCaptionActor2D();
                spacecraftLabelActor.SetCaption("");
                //                spacecraftLabelActor.GetProperty().SetColor(1.0, 1.0, 1.0);
                spacecraftLabelActor.GetCaptionTextProperty().SetColor(1.0, 1.0, 1.0);
                spacecraftLabelActor.GetCaptionTextProperty().SetJustificationToLeft();
                spacecraftLabelActor.GetCaptionTextProperty().BoldOff();
                spacecraftLabelActor.GetCaptionTextProperty().ShadowOff();
                //                spacecraftLabelActor.GetCaptionTextProperty().ItalicOff();
                spacecraftLabelActor.SetPosition(0.0, 0.0);
                spacecraftLabelActor.SetWidth(0.2);
                spacecraftLabelActor.SetHeight(.6);
                //                spacecraftLabelActor.SetPosition2(30.0, 20.0);
                spacecraftLabelActor.SetBorder(0);
                spacecraftLabelActor.SetLeader(0);
                spacecraftLabelActor.VisibilityOn();

                // instrument FOV
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

                // spacecraft position marker
                vtkPolyDataMapper spacecraftMarkerMapper = new vtkPolyDataMapper();
                spacecraftMarkerMapper.SetInputData(spacecraftMarkerBody.GetOutput());
                spacecraftMarkerActor = new vtkActor();
                spacecraftMarkerActor.SetMapper(spacecraftMarkerMapper);
                spacecraftMarkerActor.GetProperty().SetDiffuseColor(spacecraftMarkerColor);
                spacecraftMarkerActor.GetProperty().SetSpecularColor(white);
                spacecraftMarkerActor.GetProperty().SetSpecular(0.8);
                spacecraftMarkerActor.GetProperty().SetSpecularPower(80.0);
                spacecraftMarkerActor.GetProperty().ShadingOn();
                spacecraftMarkerActor.GetProperty().SetInterpolationToPhong();

                // earth position marker
                vtkPolyDataMapper earthMarkerMapper = new vtkPolyDataMapper();
                earthMarkerMapper.SetInputData(earthMarkerBody.GetOutput());
                earthMarkerActor = new vtkActor();
                earthMarkerActor.SetMapper(earthMarkerMapper);
                earthMarkerActor.GetProperty().SetDiffuseColor(earthMarkerColor);
                earthMarkerActor.GetProperty().SetSpecularColor(white);
                earthMarkerActor.GetProperty().SetSpecular(0.8);
                earthMarkerActor.GetProperty().SetSpecularPower(80.0);
                earthMarkerActor.GetProperty().ShadingOn();
                earthMarkerActor.GetProperty().SetInterpolationToPhong();

                //earthMarkerActor.RotateWXYZ(90, 1, 0, 0);

                // earth position arrowhead marker
                vtkPolyDataMapper earthMarkerHeadMapper = new vtkPolyDataMapper();
                earthMarkerHeadMapper.SetInputData(earthMarkerHead.GetOutput());
                earthMarkerHeadActor = new vtkActor();
                earthMarkerHeadActor.SetMapper(earthMarkerHeadMapper);
                earthMarkerHeadActor.GetProperty().SetDiffuseColor(earthMarkerColor);
                earthMarkerHeadActor.GetProperty().SetSpecularColor(white);
                earthMarkerHeadActor.GetProperty().SetSpecular(0.8);
                earthMarkerHeadActor.GetProperty().SetSpecularPower(80.0);
                earthMarkerHeadActor.GetProperty().ShadingOn();
                earthMarkerHeadActor.GetProperty().SetInterpolationToPhong();

                // sun position marker
                vtkPolyDataMapper sunMarkerMapper = new vtkPolyDataMapper();
                sunMarkerMapper.SetInputData(sunMarkerHead.GetOutput());
                sunMarkerHeadActor = new vtkActor();
                sunMarkerHeadActor.SetMapper(sunMarkerMapper);
                sunMarkerHeadActor.GetProperty().SetDiffuseColor(sunMarkerColor);
                sunMarkerHeadActor.GetProperty().SetSpecularColor(white);
                sunMarkerHeadActor.GetProperty().SetSpecular(0.8);
                sunMarkerHeadActor.GetProperty().SetSpecularPower(80.0);
                sunMarkerHeadActor.GetProperty().ShadingOn();
                sunMarkerHeadActor.GetProperty().SetInterpolationToFlat();
                sunMarkerHeadActor.GetProperty().SetRepresentationToSurface();
                //                sunMarkerActor.GetProperty().SetLighting(false);

                //                vtkVertexGlyphFilter filter = new vtkVertexGlyphFilter();
                //                filter.AddInputData(sunMarkerHead.GetOutput());
                //                vtkPolyDataMapper sunFilterMapper = new vtkPolyDataMapper();
                //                sunFilterMapper.SetInputConnection(filter.GetOutputPort());
                //                sunFilterActor

                vtkPolyDataMapper sunMapper = new vtkPolyDataMapper();
                sunMapper.SetInputData(sunMarker.GetOutput());
                sunMarkerActor = new vtkActor();
                sunMarkerActor.SetMapper(sunMapper);
                sunMarkerActor.GetProperty().SetDiffuseColor(sunMarkerColor);
                sunMarkerActor.GetProperty().SetSpecularColor(white);
                sunMarkerActor.GetProperty().SetSpecular(0.8);
                sunMarkerActor.GetProperty().SetSpecularPower(80.0);
                sunMarkerActor.GetProperty().ShadingOn();
                sunMarkerActor.GetProperty().SetInterpolationToFlat();
                sunMarkerActor.GetProperty().SetRepresentationToSurface();

                sunAssembly = new vtkAssembly();
                sunAssembly.AddPart(sunMarkerActor);
                sunAssembly.AddPart(sunMarkerHeadActor);


                // spacecraft position marker
                vtkPolyDataMapper spacecrafterMarkerHeadMapper = new vtkPolyDataMapper();
                spacecrafterMarkerHeadMapper.SetInputData(spacecraftMarkerHead.GetOutput());
                spacecraftMarkerHeadActor = new vtkActor();
                spacecraftMarkerHeadActor.SetMapper(spacecrafterMarkerHeadMapper);
                spacecraftMarkerHeadActor.GetProperty().SetDiffuseColor(spacecraftMarkerColor);
                spacecraftMarkerHeadActor.GetProperty().SetSpecularColor(white);
                spacecraftMarkerHeadActor.GetProperty().SetSpecular(0.1);
                spacecraftMarkerHeadActor.GetProperty().SetSpecularPower(80.0);
                spacecraftMarkerHeadActor.GetProperty().ShadingOn();
                spacecraftMarkerHeadActor.GetProperty().SetInterpolationToPhong();

                // By default do not show the trajectories
                //trajectoryActors.add(trajectoryActor);

            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            //            System.out.println("Created spacecraft actors");
            showSpacecraftMarker = false;
            showSpacecraft = false;
            updateActorVisibility();
        }

        if (timeBarActor == null)
            setupTimeBar();
    }



    private void createTrajectoryPolyData()
    {
        trajectoryPolylines = new vtkPolyData();

        int cellId = 0;
        vtkIdList idList = new vtkIdList();
        vtkPoints points = new vtkPoints();
        vtkCellArray lines = new vtkCellArray();
        vtkUnsignedCharArray colors = new vtkUnsignedCharArray();
        colors.SetNumberOfComponents(4);

        Trajectory traj =  trajectory;
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
        colors.InsertNextTuple4(trajectoryColor[0], trajectoryColor[1], trajectoryColor[2], trajectoryColor[3]);

        vtkPolyData trajectoryPolyline = new vtkPolyData();
        trajectoryPolyline.SetPoints(points);
        trajectoryPolyline.SetLines(lines);
        trajectoryPolyline.GetCellData().SetScalars(colors);

        trajectoryPolylines = trajectoryPolyline;

        trajectoryMapper.SetInputData(trajectoryPolyline);
        trajectoryMapper.Modified();
        trajectoryActor.GetProperty().SetLineWidth(trajectoryLineThickness);
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

        //Scale subsolar and subearth point markers to body size
        BoundingBox bb = smallBodyModel.getBoundingBox();
        double width = Math.max((bb.xmax-bb.xmin), Math.max((bb.ymax-bb.ymin), (bb.zmax-bb.zmin)));
        markerRadius = 0.02 * width;
        markerHeight = markerRadius * 3.0;

        spacecraftMarkerBody = new vtkSphereSource();
        spacecraftMarkerBody.SetRadius(markerRadius);
        spacecraftMarkerBody.SetCenter(markerOffset);
        spacecraftMarkerBody.SetPhiResolution(10);
        spacecraftMarkerBody.SetThetaResolution(10);
        spacecraftMarkerBody.Update();

        earthMarkerBody = new vtkCylinderSource();
        earthMarkerBody.SetRadius(markerRadius*.75);
        earthMarkerBody.SetCenter(markerOffset);
        earthMarkerBody.SetHeight(markerRadius);
        earthMarkerBody.SetResolution(50);
        //        earthMarkerBody.SetPhiResolution(10);
        //        earthMarkerBody.SetThetaResolution(10);
        earthMarkerBody.Update();

        earthMarkerHead = new vtkConeSource();
        earthMarkerHead.SetRadius(markerRadius);
        earthMarkerHead.SetHeight(markerHeight);
        earthMarkerHead.SetCenter(0, 0, 0);
        earthMarkerHead.SetResolution(50);
        earthMarkerHead.Update();

        sunMarkerHead = new vtkSphereSource();
        sunMarkerHead.SetRadius(markerRadius/16.0);
        //sunMarkerHead.SetHeight(markerHeight);
        sunMarkerHead.SetCenter(markerHeight/2.0, 0, 0);
        //sunMarkerHead.SetResolution(50);
        sunMarkerHead.Update();

        sunMarker = new vtkConeSource();
        sunMarker.SetRadius(markerRadius);
        sunMarker.SetHeight(markerHeight);
        sunMarker.SetCenter(0, 0, 0);
        sunMarker.SetResolution(50);
        sunMarker.Update();

        spacecraftMarkerHead = new vtkConeSource();
        spacecraftMarkerHead.SetRadius(markerRadius);
        spacecraftMarkerHead.SetHeight(markerHeight);
        spacecraftMarkerHead.SetCenter(0, 0, 0);
        spacecraftMarkerHead.SetResolution(50);
        spacecraftMarkerHead.Update();
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
    }

    public Double getTimeFraction()
    {
        if (currentFlybyStateHistory != null)
            return currentFlybyStateHistory.getTimeFraction();
        else
            return null;
    }

    public Double getPeriod()
    {
        if (currentFlybyStateHistory != null)
            return currentFlybyStateHistory.getPeriod();
        else
            return 0.0;
    }

    //    public static final double europaRadius = 1560.8;
    //    public static final double fovWidthFudge = 1.3;

    public void setTimeFraction(Double timeFraction)
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
            State state = currentFlybyStateHistory.getCurrentValue();
            double[] spacecraftPosition = currentFlybyStateHistory.getSpacecraftPosition();

            //            double spacecraftRotationX = state.getRollAngle();
            //            double spacecraftRotationY = state.getViewingAngle();

            double[] sunPosition = currentFlybyStateHistory.getSunPosition();
            double[] sunMarkerPosition = new double[3];
            sunDirection = new double[3];
            double[] sunViewpoint = new double[3];
            double[] sunViewDirection = new double[3];
            MathUtil.unorm(sunPosition, sunDirection);
            MathUtil.vscl(JupiterScale, sunDirection, sunViewpoint);
            MathUtil.vscl(-1.0, sunDirection, sunViewDirection);
            int result = smallBodyModel.computeRayIntersection(sunViewpoint, sunViewDirection, sunMarkerPosition);


            // toggle for lighting - Alex W
            if (timeFraction >= 0.0 && showLighting)
            {
                renderer.setFixedLightDirection(sunDirection);
                renderer.setLighting(LightingType.FIXEDLIGHT);
                updateActorVisibility();
            }
            else
                renderer.setLighting(LightingType.LIGHT_KIT);


            double[] earthPosition = currentFlybyStateHistory.getEarthPosition();
            double[] earthMarkerPosition = new double[3];
            double[] earthDirection = new double[3];
            double[] earthViewpoint = new double[3];
            double[] earthViewDirection = new double[3];
            MathUtil.unorm(earthPosition, earthDirection);
            MathUtil.vscl(JupiterScale, earthDirection, earthViewpoint);
            MathUtil.vscl(-1.0, earthDirection, earthViewDirection);
            result = smallBodyModel.computeRayIntersection(earthViewpoint, earthViewDirection, earthMarkerPosition);

            //double[] spacecraftPosition = currentFlybyStateHistory.getSpacecraftPosition();
            double[] spacecraftMarkerPosition = new double[3];
            double[] spacecraftDirection = new double[3];
            double[] spacecraftViewpoint = new double[3];
            double[] spacecraftViewDirection = new double[3];
            MathUtil.unorm(spacecraftPosition, spacecraftDirection);
            MathUtil.vscl(JupiterScale, spacecraftDirection, spacecraftViewpoint);
            MathUtil.vscl(-1.0, spacecraftDirection, spacecraftViewDirection);
            result = smallBodyModel.computeRayIntersection(spacecraftViewpoint, spacecraftViewDirection, spacecraftMarkerPosition);

            //rotates sun pointer to point in direction of sun - Alex W
            double[] zAxis = {1,0,0};
            double[] sunPos = currentFlybyStateHistory.getSunPosition();
            double[] sunPosDirection = new double[3];
            MathUtil.unorm(sunPos, sunPosDirection);
            double[] rotationAxisSun = new double[3];
            MathUtil.vcrss(sunPosDirection, zAxis, rotationAxisSun);
            double rotationAngleSun = ((180.0/Math.PI)*MathUtil.vsep(zAxis, sunPosDirection));

            vtkTransform sunMarkerTransform = new vtkTransform();
            //sunMarkerTransform.PostMultiply();
            sunMarkerTransform.Translate(sunMarkerPosition);
            sunMarkerTransform.RotateWXYZ(-rotationAngleSun, rotationAxisSun[0], rotationAxisSun[1], rotationAxisSun[2]);
            sunAssembly.SetUserTransform(sunMarkerTransform);


            //rotates earth pointer to point in direction of earth - Alex W
            double[] earthPos = currentFlybyStateHistory.getEarthPosition();
            double[] earthPosDirection = new double[3];
            MathUtil.unorm(earthPos, earthPosDirection);
            double[] rotationAxisEarth = new double[3];
            MathUtil.vcrss(earthPosDirection, zAxis, rotationAxisEarth);

            double rotationAngleEarth = ((180.0/Math.PI)*MathUtil.vsep(zAxis, earthPosDirection));

            vtkTransform earthMarkerTransform = new vtkTransform();
            earthMarkerTransform.Translate(earthMarkerPosition);
            earthMarkerTransform.RotateWXYZ(-rotationAngleEarth, rotationAxisEarth[0], rotationAxisEarth[1], rotationAxisEarth[2]);
            earthMarkerHeadActor.SetUserTransform(earthMarkerTransform);


            //rotates spacecraft pointer to point in direction of spacecraft - Alex W
            double[] spacecraftPos = spacecraftMarkerPosition;
            double[] spacecraftPosDirection = new double[3];
            MathUtil.unorm(spacecraftPos, spacecraftPosDirection);
            double[] rotationAxisSpacecraft = new double[3];
            MathUtil.vcrss(spacecraftPosDirection, zAxis, rotationAxisSpacecraft);

            double rotationAngleSpacecraft = ((180.0/Math.PI)*MathUtil.vsep(zAxis, spacecraftPosDirection));

            vtkTransform spacecraftMarkerTransform = new vtkTransform();
            spacecraftMarkerTransform.Translate(spacecraftPos);
            spacecraftMarkerTransform.RotateWXYZ(-rotationAngleSpacecraft, rotationAxisSpacecraft[0], rotationAxisSpacecraft[1], rotationAxisSpacecraft[2]);
            spacecraftMarkerHeadActor.SetUserTransform(spacecraftMarkerTransform);

            // set camera to earth, spacecraft, or sun views - Alex W
            if(earthView)
            {
                double[] focalpoint = {0,0,0};
                double[] upVector = {0,1,0};
                double[] newEarthPos = new double[3];
                MathUtil.unorm(earthPos, newEarthPos);
                MathUtil.vscl(scalingFactor, newEarthPos, newEarthPos);
                renderer.setCameraOrientation(newEarthPos, renderer.getCameraFocalPoint(), upVector, renderer.getCameraViewAngle());
            }
            else if(move)
            {
                double[] focalpoint = {0,0,0};
                double[] upVector = {0,1,0};
                renderer.setCameraOrientation(currentFlybyStateHistory.getSpacecraftPosition(), renderer.getCameraFocalPoint(), upVector, renderer.getCameraViewAngle());
            }
            else if(sunView)
            {
                double[] focalpoint = {0,0,0};
                double[] upVector = {0,1,0};
                double[] newSunPos = new double[3];
                MathUtil.unorm(sunPos, newSunPos);
                MathUtil.vscl(scalingFactor, newSunPos, newSunPos);
                renderer.setCameraOrientation(newSunPos, renderer.getCameraFocalPoint(), upVector, renderer.getCameraViewAngle());
            }

            double velocity[] = state.getSpacecraftVelocity();

            double speed = Math.sqrt(velocity[0]*velocity[0] + velocity[1]*velocity[1] + velocity[2]*velocity[2]);

            //            double[] p1 = { spacecraftPosition[0]-velocity[0], spacecraftPosition[1]-velocity[1], spacecraftPosition[2]-velocity[2] };
            //            double[] p2 = { spacecraftPosition[0]+velocity[0], spacecraftPosition[1]+velocity[1], spacecraftPosition[2]+velocity[2] };
            //
            //            V3 v1 = new V3(p1);
            //            V3 v2 = new V3(p2);
            //            double groundSpeed = VectorOps.AngularSep(v1, v2) * europaRadius * 0.50;
            //
            //            double distance = Math.sqrt(spacecraftPosition[0]*spacecraftPosition[0] + spacecraftPosition[1]*spacecraftPosition[1] + spacecraftPosition[2]*spacecraftPosition[2]);
            //            double altitude = distance - europaRadius;

            //            String speedText = String.format("%7.1f km %7.3f km/sec   .", altitude, groundSpeed);

            // calculates distance from the spacecraft to the surface or origin depending on which toggle is selected - Alex W

            double radius = Math.sqrt(spacecraftPosition[0]*spacecraftPosition[0] + spacecraftPosition[1]*spacecraftPosition[1] + spacecraftPosition[2]*spacecraftPosition[2]);
            result = smallBodyModel.computeRayIntersection(spacecraftViewpoint, spacecraftViewDirection, spacecraftMarkerPosition);
            double smallBodyRadius = Math.sqrt(spacecraftMarkerPosition[0]*spacecraftMarkerPosition[0] + spacecraftMarkerPosition[1]*spacecraftMarkerPosition[1] + spacecraftMarkerPosition[2]*spacecraftMarkerPosition[2]);
            if(distanceOption==1)
            {
                radius = radius - smallBodyRadius;
            }

            String speedText = String.format("%7.1f km %7.3f km/sec   .", radius, speed);
            //String speedText = String.format("%7.1f km       ", radius);
            //              System.out.println(speed);

            //            System.out.println("Speed: " + speed + ", Ground Speed: " + groundSpeed);

            // hardcoded to RECON # pixels for now

            //            double fovDepth = state.getSpacecraftAltitude() * fovDepthFudgeFactor;

            //            double[] fovTargetPoint = state.getSurfaceIntercept();
            //            double[] fovTargetPoint = { 0.0, 0.0, 0.0 };
            //            double[] fovDelta = { fovTargetPoint[0] - spacecraftPosition[0],  fovTargetPoint[1] - spacecraftPosition[1], fovTargetPoint[2] - spacecraftPosition[2] };
            //            double fovDepth = Math.sqrt(fovDelta[0]*fovDelta[0] + fovDelta[1]*fovDelta[1] + fovDelta[2]*fovDelta[2]);

            //            double fovWidth = fovDepth * instrumentIFovs[instrument] * instrumentLineSamples[instrument] * fovWidthFudge;
            //            double fovHeight = fovDepth * instrumentIFovs[instrument] * instrumentLines[instrument];

            // this is to make the FOV a rectangle shape rather than a diamond
            //            double spacecraftRotationZ = Math.toRadians(45.0);

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

            vtkMatrix4x4 sunMarkerMatrix = new vtkMatrix4x4();
            vtkMatrix4x4 earthMarkerMatrix = new vtkMatrix4x4();

            vtkMatrix4x4 spacecraftInstrumentMatrix = new vtkMatrix4x4();

            // set to identity
            spacecraftBodyMatrix.Identity();
            spacecraftIconMatrix.Identity();
            fovMatrix.Identity();
            fovRotateXMatrix.Identity();
            fovRotateYMatrix.Identity();
            fovRotateZMatrix.Identity();
            sunMarkerMatrix.Identity();
            earthMarkerMatrix.Identity();
            //fovMatrix = new vtkMatrix4x4

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
            //            double sinRotZ = Math.sin(spacecraftRotationZ);
            //            double cosRotZ = Math.cos(spacecraftRotationZ);
            //            fovRotateZMatrix.SetElement(0, 0, cosRotZ);
            //            fovRotateZMatrix.SetElement(1, 1, cosRotZ);
            //            fovRotateZMatrix.SetElement(0, 1, sinRotZ);
            //            fovRotateZMatrix.SetElement(1, 0, -sinRotZ);

            // scale the FOV
            //            fovScaleMatrix.SetElement(0, 0, fovHeight);
            //            fovScaleMatrix.SetElement(1, 1, fovWidth);
            //            fovScaleMatrix.SetElement(2, 2, fovDepth);

            // rotate the FOV about the Y axis
            //            double sinRotY = Math.sin(spacecraftRotationY);
            //            double cosRotY = Math.cos(spacecraftRotationY);
            //            fovRotateYMatrix.SetElement(0, 0, cosRotY);
            //            fovRotateYMatrix.SetElement(2, 2, cosRotY);
            //            fovRotateYMatrix.SetElement(0, 2, sinRotY);
            //            fovRotateYMatrix.SetElement(2, 0, -sinRotY);
            //
            //            fovMatrix.Multiply4x4(fovScaleMatrix, fovRotateZMatrix, spacecraftInstrumentMatrix);
            //            fovMatrix.Multiply4x4(fovRotateYMatrix, spacecraftInstrumentMatrix, spacecraftInstrumentMatrix);
            //
            ////            spacecraftFovMatrix.Multiply4x4(fovRotateYMatrix, fovRotateZMatrix, spacecraftInstrumentMatrix);
            //
            //            fovMatrix.Multiply4x4(spacecraftBodyMatrix, spacecraftInstrumentMatrix, fovMatrix);

            // set translation
            for (int i=0; i<3; i++)
            {
                spacecraftBodyMatrix.SetElement(i, 3, spacecraftPosition[i]);
                spacecraftIconMatrix.SetElement(i, 3, spacecraftPosition[i]);
                fovMatrix.SetElement(i, 3, spacecraftPosition[i]);

                sunMarkerMatrix.SetElement(i, 3, sunMarkerPosition[i]);
                earthMarkerMatrix.SetElement(i, 3, earthMarkerPosition[i]);
            }

            //            spacecraftBoresightActor.SetUserMatrix(matrix);

            monolithBodyActor.SetUserMatrix(spacecraftBodyMatrix);

            spacecraftBodyActor.SetUserMatrix(spacecraftIconMatrix);

            spacecraftLabelActor.SetAttachmentPoint(spacecraftPosition);
            spacecraftLabelActor.SetCaption(speedText);

            spacecraftFovActor.SetUserMatrix(fovMatrix);
            //            spacecraftFovActor.SetUserMatrix(spacecraftBodyMatrix);

            spacecraftMarkerActor.SetUserMatrix(spacecraftBodyMatrix);
            earthMarkerActor.SetUserMatrix(earthMarkerMatrix);
            //            earthMarkerHeadActor.SetUserMatrix(earthMarkerMatrix);
            //sunMarkerActor.SetUserMatrix(sunMarkerMatrix);

            //            spacecraftBoresight.Modified();
            monolithBody.Modified();
            spacecraftBody.Modified();
            spacecraftFov.Modified();
            spacecraftMarkerBody.Modified();
            earthMarkerHead.Modified();
            earthMarkerBody.Modified();
            sunAssembly.Modified();

            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }

    //    public void setOffset(double offset)
    //    {
    //        this.offset = offset;
    //        areaCalculation.setOffset(offset);
    //        this.updateActorVisibility();
    //        Set<String> visiblePatches = new HashSet<String>();
    //        for (int i=0; i<areaCalculation.getSize(); i++)
    //            visiblePatches.add(areaCalculation.getValue(i).getName());
    //        setShowPatches(visiblePatches);
    //        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    //    }

    //    public double getOffset()
    //    {
    //        return offset;
    //    }

    public void setShowSpacecraft(boolean show)
    {
        this.showSpacecraft = show;
        //        this.showMonolith = show;
        //        this.showSpacecraftBody = show;
        this.showSpacecraftFov = show;
        this.showSpacecraftLabel = show;
        updateActorVisibility();
    }

    //updated method so that it is togglable for pointers, trajectory etc. - Alex W
    public void updateActorVisibility()
    {
        stateHistoryActors.clear();
        initialize();

        if (visible)
        {
            stateHistoryActors.add(trajectoryActor);
        }

        //      stateHistoryActors.add(spacecraftBoresightActor);

        if (showMonolith)
            stateHistoryActors.add(monolithBodyActor);
        if (showSpacecraftBody)
            stateHistoryActors.add(spacecraftBodyActor);
        if (showSpacecraftLabel)
            stateHistoryActors.add(spacecraftLabelActor);
        if (showSpacecraftFov)
            stateHistoryActors.add(spacecraftFovActor);
        if (showSpacecraft)
            stateHistoryActors.add(spacecraftMarkerActor);
        if (showEarthMarker && !(time == 0.0))
            stateHistoryActors.add(earthMarkerHeadActor);
        if (showSunMarker && !(time == 0.0))
            stateHistoryActors.add(sunAssembly);
        if (showSpacecraftMarker)
            stateHistoryActors.add(spacecraftMarkerHeadActor);

        if (showTimeBar)
        {
            stateHistoryActors.add(timeBarActor);
            stateHistoryActors.add(timeBarTextActor);
        }

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
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
    private boolean showScalarBar = false;

    // variables related to the scalar bar
    private vtkScalarBarActor scalarBarActor;
    //    private int coloringIndex = -1;
    private int coloringIndex = 1;

    // sets the visibility of actors, called in StateHistoryPanel and gets the toggle info in it - Alex W
    public void setActorVisibility(String actorName, boolean visibility)
    {
        //maps the actors+data
        if(actorName.equalsIgnoreCase("Earth"))
            showEarthMarker = visibility;
        if(actorName.equalsIgnoreCase("Sun"))
            showSunMarker = visibility;
        if(actorName.equalsIgnoreCase("SpacecraftMarker"))
            showSpacecraftMarker = visibility;
        if(actorName.equalsIgnoreCase("Spacecraft"))
        {
            setShowSpacecraft(visibility);
        }
        if(actorName.equalsIgnoreCase("Trajectory"))
            if(visibility)
            {
                this.visible = true;
            }
            else
            {
                this.visible = false;
            }
        if(actorName.equalsIgnoreCase("Lighting"))
            showLighting = visibility;
        updateActorVisibility();
    }

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
        int x = buffer + 20; // lower left corner x
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
        //        String utcValue = TimeUtils.et2UTCString(timeBarValue);
        String utcValue =TimeUtil.et2str(timeBarValue).substring(0, 23);
        //        System.out.println(utcValue);
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
        Trajectory traj = trajectory;
        if (traj != null)
        {
            // putting selection code here until we get around to implementing a FlybyPicker -turnerj1
            //            this.setCurrentTrajectoryId(traj.id);
            return "Trajectory " + traj.getId() + " = " + traj.getName() + " contains " + traj.getX().size() + " vertices";
        }
        //        else
        //        {
        //            SurfacePatch patch = getSurfacePatch(prop);
        //            if (patch != null)
        //            {
        //                String dataType = patch.getCurrentDataType();
        //                if (this.getAreaCalculation() != null)
        //                {
        //                    Double value = getAreaCalculation().getSurfacePatchValue(prop, cellId, pickPosition);
        //                    if (value != null)
        //                    {
        //                        return dataType + ": " + value;
        //                    }
        //                }
        //            }
        //        }

        return "";
    }

    public boolean getInitialized(){
        return initialized;
    }


    public StateHistoryKey getKey()
    {
        return key;
    }

    public String getTrajectoryName(){
        if (trajectoryName == null || trajectoryName.equals("")) {
            return defaultTrajectoryName();
        }
        return trajectoryName;
    }

    public String defaultTrajectoryName() {
        return startTime.toString() + "_" + endTime.toString();
    }

    // returns sun position - Alex W
    public double[] getSunPosition()
    {
        return currentFlybyStateHistory.getSunPosition();
    }

    // sets the renderer to move along the spacecraft trajectory - Alex W
    public void setSpacecraftMovement(boolean move)
    {
        this.move = move;
        if(move)
        {
            double[] focalpoint = {0,0,0};
            double[] upVector = {0,1,0};
            renderer.setCameraOrientation(currentFlybyStateHistory.getSpacecraftPosition(), renderer.getCameraFocalPoint(), upVector, 30);
        }
    }

    // sets the renderer to the earth position and plays the animation with camera fixed to earth - Alex W
    public void setEarthView(boolean move)
    {
        this.earthView = move;
        if(earthView)
        {
            double[] focalpoint = {0,0,0};
            double[] upVector = {0,1,0};
            double[] earthPos = currentFlybyStateHistory.getEarthPosition();
            double[] newEarthPos = new double[3];
            MathUtil.unorm(earthPos, newEarthPos);
            MathUtil.vscl(scalingFactor, newEarthPos, newEarthPos);
            renderer.setCameraOrientation(newEarthPos, renderer.getCameraFocalPoint(), upVector, 5);
        }
    }

    // sets the renderer to the sun position and plays the animation with camera fixed to sun - Alex W
    public void setSunView(boolean move)
    {
        this.sunView = move;
        if(sunView)
        {
            double[] focalpoint = {0,0,0};
            double[] upVector = {0,1,0};
            double[] sunPos = currentFlybyStateHistory.getSunPosition();
            double[] newSunPos = new double[3];
            MathUtil.unorm(sunPos, newSunPos);
            MathUtil.vscl(scalingFactor, newSunPos, newSunPos);
            renderer.setCameraOrientation(newSunPos, renderer.getCameraFocalPoint(), upVector, 5);
        }
    }

    //returns renderer - Alex W
    public Renderer getRenderer()
    {
        return renderer;
    }

    // sets view angle - Alex W
    public void setViewAngle(double angle)
    {
        renderer.setCameraViewAngle(angle);
    }

    // set the earth pointer size - Alex W
    public void setEarthPointerSize(int radius)
    {
        double rad = markerRadius * (2.66e-4 * Math.pow((double)radius,2) + 1e-4*(double)radius + .33);//markerRadius * ((4.5/100.0)*(double)radius + 0.5);
        double height = markerHeight * (2.66e-4 * Math.pow((double)radius,2) + 1e-4*(double)radius + .33);
        earthMarkerHead.SetRadius(rad);
        earthMarkerHead.SetHeight(height);
        earthMarkerHead.Update();
        earthMarkerHead.Modified();
        updateActorVisibility();
    }

    // set the sun pointer size - Alex W
    public void setSunPointerSize(int radius)
    {
        double scale = (2.66e-4 * Math.pow((double)radius,2) + 1e-4*(double)radius + .33);
        sunAssembly.SetScale(scale);
        sunMarkerHead.Update();
        sunMarkerHead.Modified();
        updateActorVisibility();
    }

    // set the spacecraft pointer size - Alex W
    public void setSpacecraftPointerSize(int radius)
    {
        double rad = markerRadius *(2.66e-4 * Math.pow((double)radius,2) + 1e-4*(double)radius + .33);
        double height = markerHeight * (2.66e-4 * Math.pow((double)radius,2) + 1e-4*(double)radius + .33);
        spacecraftMarkerHead.SetRadius(rad);
        spacecraftMarkerHead.SetHeight(height);
        spacecraftMarkerHead.Update();
        spacecraftMarkerHead.Modified();
        updateActorVisibility();
    }

    // set the distance text from center or surface to the spacecraft - Alex W
    public void setDistanceText(String option)
    {
        if(option.equals("Distance to Center"))
        {
            distanceOption = 0;
        }
        else if(option.equals("Distance to Surface"))
        {
            distanceOption = 1;
        }
        State state = currentFlybyStateHistory.getCurrentValue();

        double[] spacecraftPosition = currentFlybyStateHistory.getSpacecraftPosition();
        double velocity[] = state.getSpacecraftVelocity();
        double speed = Math.sqrt(velocity[0]*velocity[0] + velocity[1]*velocity[1] + velocity[2]*velocity[2]);

        double[] spacecraftMarkerPosition = new double[3];
        double[] spacecraftDirection = new double[3];
        double[] spacecraftViewpoint = new double[3];
        double[] spacecraftViewDirection = new double[3];
        MathUtil.unorm(spacecraftPosition, spacecraftDirection);
        MathUtil.vscl(JupiterScale, spacecraftDirection, spacecraftViewpoint);
        MathUtil.vscl(-1.0, spacecraftDirection, spacecraftViewDirection);

        double radius = Math.sqrt(spacecraftPosition[0]*spacecraftPosition[0] + spacecraftPosition[1]*spacecraftPosition[1] + spacecraftPosition[2]*spacecraftPosition[2]);
        double result = smallBodyModel.computeRayIntersection(spacecraftViewpoint, spacecraftViewDirection, spacecraftMarkerPosition);
        double smallBodyRadius = Math.sqrt(spacecraftMarkerPosition[0]*spacecraftMarkerPosition[0] + spacecraftMarkerPosition[1]*spacecraftMarkerPosition[1] + spacecraftMarkerPosition[2]*spacecraftMarkerPosition[2]);
        if(distanceOption==1)
        {
            radius = radius - smallBodyRadius;
        }

        String speedText = String.format("%7.1f km %7.3f km/sec   .", radius, speed);
        spacecraftLabelActor.SetCaption(speedText);
        spacecraftLabelActor.Modified();
        updateActorVisibility();
    }

    // set time of animation - Alex W
    public boolean setInputTime(DateTime dt, StateHistoryPanel panel)
    {
        try
        {

            String dtString = dt.toString().substring(0, 23);
            String start = timeArray.get(0)[0];
            String end = timeArray.get(0)[1];

            if(dtString.compareTo(start) < 0 || dtString.compareTo(end) > 0)
            {
                JOptionPane.showMessageDialog(null, "Entered time is outside the range of the selected interval.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            Interval interval1 = new Interval(startTime, dt);
            Interval interval2 = new Interval(startTime, endTime);

            org.joda.time.Duration duration1 = interval1.toDuration();
            org.joda.time.Duration duration2 = interval2.toDuration();

            BigDecimal num1 = new BigDecimal(duration1.getMillis());
            BigDecimal num2 = new BigDecimal(duration2.getMillis());
            BigDecimal tf = num1.divide(num2,50,RoundingMode.UP);
            setTimeFraction(Double.parseDouble(tf.toString()));
            panel.setTimeSlider(Double.parseDouble(tf.toString()));
        }
        catch (Exception e)
        {

        }
        return true;
    }

    // toggle the visibility of trajectories - Alex W
    public void showTrajectory(boolean show)
    {
        if(show)
        {
            for(int i = 0; i<stateHistoryActors.size(); i++)
            {
                if(trajectoryActor.equals(stateHistoryActors.get(i)))
                {
                    stateHistoryActors.get(i).VisibilityOn();
                    stateHistoryActors.get(i).Modified();
                }
            }
            showing = true;
        }
        else
        {
            for(int i = 0; i<stateHistoryActors.size(); i++)
            {
                if(trajectoryActor.equals(stateHistoryActors.get(i)))
                {
                    stateHistoryActors.get(i).VisibilityOff();
                    stateHistoryActors.get(i).Modified();
                }

            }
            showing = false;
        }

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }


    /***
     *  goes to the server and creates a new time history for the body with the given time range.
     *
     * @param panel - main panel
     * @param length - length of interval
     * @param name - name of interval
     * @return -1 if error thrown on creation, 1 if successfully created
     */
    // returns
    public int createNewTimeInterval(StateHistoryPanel panel, double length, String name)
    {

        // gets the history file from the server
        SmallBodyViewConfig config = (SmallBodyViewConfig) smallBodyModel.getConfig();
        path = FileCache.getFileFromServer(config.timeHistoryFile);

        // removes the time zone from the time
        String startString = startTime.toString().substring(0,23);
        String endString = endTime.toString().substring(0,23);

        // searches the file for the specified times
        String queryStart = readString(lineLength);
        String queryEnd = readString((int)getBinaryFileLength()*lineLength-lineLength);

        // error checking
        if(startTime.compareTo(endTime) > 0)
        {
            JOptionPane.showMessageDialog(null, "The entered times are not in the correct order.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return -1;
        }
        if(startString.compareTo(queryStart) < 0 || endString.compareTo(queryEnd) > 0)
        {
            JOptionPane.showMessageDialog(null, "One or more of the query times are out of range of the available data.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return -1;
        }

        // get start and stop positions in file
        int positionStart = binarySearch(1, (int) getBinaryFileLength(), startString, false);
        int positionEnd = binarySearch(1, (int) getBinaryFileLength(), endString, true);

        // check length of time
        if(readString(positionStart).compareTo(readString(positionEnd)) == 0)
        {
            JOptionPane.showMessageDialog(null, "The queried time interval is too small.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return -1;
        }

        //sets the default name to "startTime_endTime"
        if(name.equals(""))
        {
            name = readString(positionStart) + "_" + readString(positionEnd);
        }

        // check length of interval
        if(length > 10.0)
        {
            int result = JOptionPane.showConfirmDialog(null, "The interval you selected is longer than 10 days and may take a while to generate. \nAre you sure you want to create it?");
            if(result == JOptionPane.CANCEL_OPTION || result == JOptionPane.NO_OPTION)
                return -1;
        }

        // creates the trajectory
        Trajectory temp = new StandardTrajectory();
        StateHistory history = new StandardStateHistory();

        this.currentFlybyStateHistory = history;

        //  reads the binary file and writes the data to a CSV file
        String[] timeSet = new String[2];
        timeArray.add(timeSet);
        for(int i = positionStart; i <= positionEnd; i+=lineLength)
        {
            int[] position = new int[12];
            for(int j = 0; j<position.length; j++)
            {
                position[j] = i + 25 + (j * 8);
            }
            State flybyState = new CsvState(readString(i),
                    readBinary(position[0]), readBinary(position[1]), readBinary(position[2]),
                    readBinary(position[3]), readBinary(position[4]), readBinary(position[5]),
                    readBinary(position[6]), readBinary(position[7]), readBinary(position[8]),
                    readBinary(position[9]), readBinary(position[10]), readBinary(position[11]));

            // add to history
            history.put(flybyState);

            double[] spacecraftPosition = flybyState.getSpacecraftPosition();

            temp.getX().add(spacecraftPosition[0]);
            temp.getY().add(spacecraftPosition[1]);
            temp.getZ().add(spacecraftPosition[2]);

            if(com.mysql.jdbc.StringUtils.isNullOrEmpty(timeArray.get(0)[0]))
            {
                timeArray.get(0)[0] = flybyState.getUtc();
            }
            timeArray.get(0)[1] = flybyState.getUtc();

        }

        setCurrentTrajectory(temp);
        createTrajectoryPolyData();

        trajectoryMapper.SetInputData(trajectoryPolylines);

        vtkActor actor = new vtkActor();
        trajectoryActor = actor;
        trajectoryActor.SetMapper(trajectoryMapper);
        trajectoryActor.GetProperty().SetLineWidth(trajectoryLineThickness);
        setTimeFraction(0.0);
        try
        {
            panel.initializeRunList();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        initialize();
        spacecraftBody.Modified();
        trajectoryActor.Modified();
        return 1;
    }

    private void setCurrentTrajectory(Trajectory temp)
    {
        trajectory = temp;
    }

    private BlockingQueue<AnimationFrame> animationFrameQueue;


    // starts the process for creating the movie frames
    public void saveAnimation(StateHistoryPanel panel, String start, String end)
    {
        frames = new JLabel("Number of Frames: ");
        fromLabel = new JLabel("Start: " + start);
        toLabel = new JLabel("End: " + end);
        numFrames = new JSpinner();
        SpinnerNumberModel model = new SpinnerNumberModel(100, 1, null, 1);
        numFrames.setModel(model);
        chooser = new JFileChooser();
        chooser.setDialogTitle("Export Movie Frames as PNG");
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setApproveButtonText("Export");

        JPanel comp1 = (JPanel) chooser.getComponent(4);
        JPanel time = (JPanel) comp1.getComponent(0);

        time.setLayout(new GridLayout(0, 2));
        time.add(frames);
        time.add(numFrames);
        time.add(fromLabel);
        time.add(toLabel);

        int result = chooser.showSaveDialog(JOptionPane.getFrameForComponent(panel));

        if(result == JFileChooser.CANCEL_OPTION || result == JFileChooser.ERROR_OPTION)
        {
            return;
        }

        File file = chooser.getSelectedFile();
        String path = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(File.separator));
        String base = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf(File.separator));
        String ext = ".png";

        int frameNum = (Integer) numFrames.getValue();
        timeStep = 1.0/ (double) frameNum;

        animationFrameQueue = new LinkedBlockingQueue<AnimationFrame>();

        // creates the frames with the data necessary to take the images
        for(double i = 0; i <= frameNum; i++)
        {
            String index = String.format("%03d",  (int)i);
            File f = new File(path+base+"_Frame_"+index+ext);
            double tf = i/(double)frameNum;
            AnimationFrame frame = createAnimationFrameWithTimeFraction(tf, f, 250, panel);
            animationFrameQueue.add(frame);
        }

        this.actionPerformed(null);
    }

    // creates animation frame with data to move the camera
    public AnimationFrame createAnimationFrameWithTimeFraction(double tf, File file, int delay, StateHistoryPanel panel)
    {
        AnimationFrame result = new AnimationFrame();
        result.timeFraction = tf;
        result.file = file;
        result.delay = delay;
        result.panel = panel;

        return result;
    }

    // saves a view to a file
    public static void saveToFile(File file, vtkJoglCanvasComponent renWin)
    {
        if (file != null)
        {
            try
            {
                // The following line is needed due to some weird threading
                // issue with JOGL when saving out the pixel buffer. Note release
                // needs to be called at the end.
                renWin.getComponent().getContext().makeCurrent();

                renWin.getVTKLock().lock();
                vtkWindowToImageFilter windowToImage = new vtkWindowToImageFilter();
                windowToImage.SetInput(renWin.getRenderWindow());

                String filename = file.getAbsolutePath();
                if (filename.toLowerCase().endsWith("bmp"))
                {
                    vtkBMPWriter writer = new vtkBMPWriter();
                    writer.SetFileName(filename);
                    writer.SetInputConnection(windowToImage.GetOutputPort());
                    writer.Write();
                }
                else if (filename.toLowerCase().endsWith("jpg") ||
                        filename.toLowerCase().endsWith("jpeg"))
                {
                    vtkJPEGWriter writer = new vtkJPEGWriter();
                    writer.SetFileName(filename);
                    writer.SetInputConnection(windowToImage.GetOutputPort());
                    writer.Write();
                }
                else if (filename.toLowerCase().endsWith("png"))
                {
                    vtkPNGWriter writer = new vtkPNGWriter();
                    writer.SetFileName(filename);
                    writer.SetInputConnection(windowToImage.GetOutputPort());
                    writer.Write();
                }
                else if (filename.toLowerCase().endsWith("pnm"))
                {
                    vtkPNMWriter writer = new vtkPNMWriter();
                    writer.SetFileName(filename);
                    writer.SetInputConnection(windowToImage.GetOutputPort());
                    writer.Write();
                }
                else if (filename.toLowerCase().endsWith("ps"))
                {
                    vtkPostScriptWriter writer = new vtkPostScriptWriter();
                    writer.SetFileName(filename);
                    writer.SetInputConnection(windowToImage.GetOutputPort());
                    writer.Write();
                }
                else if (filename.toLowerCase().endsWith("tif") ||
                        filename.toLowerCase().endsWith("tiff"))
                {
                    vtkTIFFWriter writer = new vtkTIFFWriter();
                    writer.SetFileName(filename);
                    writer.SetInputConnection(windowToImage.GetOutputPort());
                    writer.SetCompressionToNoCompression();
                    writer.Write();
                }
                renWin.getVTKLock().unlock();
            }
            finally
            {
                renWin.getComponent().getContext().release();
            }
        }
    }

    // sets the renderer to the information in the animation frame
    private void setAnimationFrame(AnimationFrame frame)
    {
        setTimeFraction(frame.timeFraction);
        frame.panel.setTimeSlider(frame.timeFraction);
    }

    // called repeatedly to update the renderer and take the picture
    @Override
    public void actionPerformed(ActionEvent e)
    {
        AnimationFrame frame = animationFrameQueue.peek();
        if (frame != null)
        {
            if (frame.staged && frame.file != null)
            {
                saveToFile(frame.file, renderer.getRenderWindowPanel());
                animationFrameQueue.remove();
            }
            else
            {
                setAnimationFrame(frame);
                frame.staged = true;
            }

            Timer timer = new Timer(frame.delay, this);
            timer.setRepeats(false);
            timer.start();
        }

    }

    // binary searches the binary file for a time for the new time interval feature - Alex W
    private int binarySearch(int first, int last, String target, boolean pos)
    {
        if(first > last)
        {
            if(pos)
            {
                return (last + 1) * lineLength;
            }
            return (last) * lineLength;
        }
        else
        {
            int middle = (first+last)/2;
            int compResult = target.compareTo(readString((middle) * lineLength));
            if(compResult == 0)
                return (middle) * lineLength;
            else if(compResult < 0)
                return binarySearch(first, middle - 1, target, pos);
            else
                return binarySearch(middle + 1, last, target, pos);
        }
    }

    // gets the number of lines of a binary file, needed for the binary search - Alex W
    private long getBinaryFileLength()
    {
        long length = 0;
        try
        {
            RandomAccessFile fileStore = new RandomAccessFile(path, "r");
            length = fileStore.length()/lineLength;
            fileStore.close();
        }
        catch (Exception e)
        {
            return length;
        }
        return length;
    }

    // reads binary that represents a String - Alex W
    private String readString(int postion)
    {
        String string = "";
        try
        {
            RandomAccessFile fileStore = new RandomAccessFile(path, "r");
            fileStore.seek(postion);
            string = fileStore.readUTF();
            fileStore.close();
        }
        catch (Exception e)
        {
            return "";
        }
        return string;
    }

    // reads binary that represents a double - Alex W
    private double readBinary(int postion)
    {
        double num = 0;
        try
        {
            RandomAccessFile fileStore = new RandomAccessFile(path, "r");
            fileStore.seek(postion);
            num = fileStore.readDouble();
            fileStore.close();
        }
        catch (Exception e)
        {
            return 0;
        }
        return  num;
    }

    // gets the start and end times of a trajectory - Alex W
    public String[] getIntervalTime()
    {
        return timeArray.get(0);
    }

    @Override
    public int getRowCount()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getColumnCount()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void addTableModelListener(TableModelListener l)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeTableModelListener(TableModelListener l)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // TODO Auto-generated method stub

    }

    public DateTime getStartTime()
    {
        return startTime;
    }
    public DateTime getEndTime()
    {
        return endTime;
    }

    public double[] getTrajectoryColor()
    {
        return trajectoryColor;
    }

    public void setTrajectoryColor(double[] color)
    {
        this.trajectoryColor = color;
        // recreate poly data with new color
        createTrajectoryPolyData();
        if (showing) {
            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }

    public void saveIntervalToFile(String fileName)
    {
        SmallBodyViewConfig config = (SmallBodyViewConfig) smallBodyModel.getConfig();
        // writes the header for the new history
        try
        {
            FileWriter writer = new FileWriter(fileName);
            writer.append(config.getShapeModelName());
            writer.append(',');
            writer.append('\n');

            // Create header of name, description, color
            writer.append(trajectoryName + ',');
            writer.append(description + ',');
            for (double colorElement : trajectoryColor) {
                writer.append(Double.toString(colorElement));
                writer.append(',');
            }
            writer.append(Double.toString(trajectoryLineThickness));
            writer.append('\n');

            // header of column names for each entry
            writer.append("#UTC");
            writer.append(',');
            writer.append(" Sun x");
            writer.append(',');
            writer.append(" Sun y");
            writer.append(',');
            writer.append(" Sun z");
            writer.append(',');
            writer.append(" Earth x");
            writer.append(',');
            writer.append(" Earth y");
            writer.append(',');
            writer.append(" Earth z");
            writer.append(',');
            writer.append(" SC x");
            writer.append(',');
            writer.append(" SC y");
            writer.append(',');
            writer.append(" SC z");
            writer.append(',');
            writer.append(" SCV x");
            writer.append(',');
            writer.append(" SCV y");
            writer.append(',');
            writer.append(" SCV z");
            writer.append('\n');
            writer.flush();
            writer.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // get each flyby state in currentFlybyStateHistory, and write to CSV
        Set<Double> keySet = currentFlybyStateHistory.getAllKeys();
        for (Double key : keySet) {
            CsvState history = (CsvState)currentFlybyStateHistory.getValue(key);
            history.writeToCSV(fileName);
        }

    }


    public StateHistoryModel loadStateHistoryFromFile(File runFile)
    {
        Integer firstIndex = null;
        String runDirName = runFile.getAbsolutePath();

        initialize();

        try
        {
            String runName = runFile.getName();
            if (runName.endsWith(".csv"))
            {
                BufferedReader in = new BufferedReader(new FileReader(runFile));
                String beforeParse = in.readLine();
                String input = beforeParse.substring(0, beforeParse.indexOf(','));
                if(input.equals(smallBodyModel.getConfig().getShapeModelName())){
                    passFileNames.add(runName);
                }
                in.close();
            }
        }
        catch (Exception e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }


        try {
            BufferedReader in = new BufferedReader(new FileReader(runDirName));

            if (firstIndex == null)
                firstIndex = 0;

            trajectory = new StandardTrajectory();
            // fill in the Trajectory parameters
            trajectory.setId(0);

            // create a new history instance and add it to the Map
            StateHistory history = new StandardStateHistory();

            // discard first line of body name
            in.readLine();

            // get name, desc, color form second line
            String info = in.readLine();
            String[] data = info.split(",");
            trajectory.setName(data[0]);
            setTrajectoryName(data[0]);
            setDescription(data[1]);
            setTrajectoryColor(new double[]{Double.parseDouble(data[2]), Double.parseDouble(data[3]),
                                            Double.parseDouble(data[4]),Double.parseDouble(data[5])});
            setTrajectoryLineThickness(Double.parseDouble(data[6]));


            // discard third line of headers
            in.readLine();

            String line;
            String[] timeSet = new String[2];
            timeArray.add(timeSet);
            while ((line = in.readLine()) != null)
            {
                // parse line of file
                State flybyState = new CsvState(line);

                // add to history
                history.put(flybyState);

                double[] spacecraftPosition = flybyState.getSpacecraftPosition();

                trajectory.getX().add(spacecraftPosition[0]);
                trajectory.getY().add(spacecraftPosition[1]);
                trajectory.getZ().add(spacecraftPosition[2]);

                if(com.mysql.jdbc.StringUtils.isNullOrEmpty(timeArray.get(0)[0])){
                    timeArray.get(0)[0] = flybyState.getUtc();
                }
                timeArray.get(0)[1] = flybyState.getUtc();
            }
            in.close();

            this.currentFlybyStateHistory = history;


        } catch (Exception e) {
            e.printStackTrace();
        }

        DateTime start = ISODateTimeFormat.dateTimeParser().parseDateTime(timeArray.get(0)[0]);
        DateTime stop = ISODateTimeFormat.dateTimeParser().parseDateTime(timeArray.get(0)[1]);
        this.startTime = start;
        this.endTime = stop;


        // set up vtk stuff
        createTrajectoryPolyData();
        trajectoryMapper.SetInputData(trajectoryPolylines);
        trajectoryActor.SetMapper(trajectoryMapper);
        setTimeFraction(0.0);


        initialize();
        spacecraftBody.Modified();
        trajectoryActor.Modified();
        return this;

    }

    public void setTrajectoryName(String name)
    {
        trajectoryName = name;
    }


    public String getDescription() {
        return description;
    }
    public void setDescription(String desc)
    {
        description = desc;
    }

    public void setTrajectoryLineThickness(double value)
    {
        this.trajectoryLineThickness = value;
        // recreate poly data with new thickness
        trajectoryActor.GetProperty().SetLineWidth(trajectoryLineThickness);
        createTrajectoryPolyData();
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null , null);
    }

    public double getTrajectoryLineThickness()
    {
        return trajectoryLineThickness;
    }



}

// class for the animation frame - Alex W
class AnimationFrame
{
    public boolean staged;
    public boolean saved;
    public int delay;
    public double timeFraction;
    public File file;
    public StateHistoryPanel panel;

}
