package edu.jhuapl.sbmt.model.lidar;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import com.google.common.base.Stopwatch;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkDoubleArray;
import vtk.vtkGeometryFilter;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkUnsignedCharArray;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.util.ColorUtil;
import edu.jhuapl.saavtk.util.DoublePair;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.saavtk.util.SaavtkLODActor;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.util.TimeUtil;

public class LidarDataPerUnit extends AbstractModel implements PropertyChangeListener
{
    protected vtkPolyData polydata;
    protected vtkPolyData polydataSc;
    protected vtkPoints originalPoints;
    protected vtkPoints originalPointsSc;
    protected List<vtkProp> actors = new ArrayList<vtkProp>();
    protected double startPercentage = 0.0;
    protected double stopPercentage = 1.0;
    protected vtkGeometryFilter geometryFilter;
    protected vtkGeometryFilter geometryFilterSc;
    protected String filepath;
    protected vtkDoubleArray times;
    protected vtkDoubleArray ranges;
    protected vtkActor actorSpacecraft;
    private ProgressMonitor progressMonitor;
    private BinaryDataTask binaryTask;
    private int count;
    protected double offsetMultiplier=1.;
    private int binaryRecordSize;
    private File file;
    private FileInputStream fs;
    String path;
    BodyViewConfig config;
    private int xIndex;
    private int yIndex;
    private int zIndex;
    private int scxIndex;
    private int scyIndex;
    private int sczIndex;
    private boolean isInMeters;
    private vtkPoints points;
    private vtkCellArray vert;
    private vtkIdList idList;
    private boolean intensityEnabled;
    private double minIntensity;
    private double maxIntensity;
    private int receivedIntensityIndex;
    private vtkPoints pointsSc;
    private vtkCellArray vertSc;
    private List<Double> intensityList;
    private int timeIndex;
    private vtkUnsignedCharArray colors;
    private Color baseColor;
    protected LidarLoadingListener listener;

    void init() throws IOException
    {
        progressMonitor = new ProgressMonitor(null, "Loading OLA Data", "", 0, 100);
        int[] xyzIndices = config.lidarBrowseXYZIndices;
        int[] scXyzIndices = config.lidarBrowseSpacecraftIndices;
        boolean isLidarInSphericalCoordinates = config.lidarBrowseIsLidarInSphericalCoordinates;
        boolean isSpacecraftInSphericalCoordinates = config.lidarBrowseIsSpacecraftInSphericalCoordinates;
        boolean isTimeInET = config.lidarBrowseIsTimeInET;
        timeIndex = config.lidarBrowseTimeIndex;
        int numberHeaderLines = config.lidarBrowseNumberHeaderLines;
        isInMeters = config.lidarBrowseIsInMeters;
        int rangeIndex = config.lidarBrowseRangeIndex;
        boolean isRangeExplicitInData = config.lidarBrowseIsRangeExplicitInData;
        int noiseIndex = config.lidarBrowseNoiseIndex;
        boolean isBinary = config.lidarBrowseIsBinary;
        binaryRecordSize = config.lidarBrowseBinaryRecordSize;
        receivedIntensityIndex = config.lidarBrowseReceivedIntensityIndex;
        intensityEnabled = config.lidarBrowseIntensityEnabled;

        if (config.lidarBrowseOrigPathRegex != null && !config.lidarBrowseOrigPathRegex.isEmpty()) {
            path = path.replaceAll(config.lidarBrowseOrigPathRegex, config.lidarBrowsePathTop);
        }
        file = FileCache.getFileFromServer(SafeURLPaths.instance().getString(path));

        if (file == null)
            throw new IOException(path + " could not be loaded");

        filepath = path;


        polydata = new vtkPolyData();
        points = new vtkPoints();
        vert = new vtkCellArray();
        polydata.SetPoints( points );
        polydata.SetVerts( vert );
        colors = new vtkUnsignedCharArray();
        colors.SetNumberOfComponents(4);
        polydata.GetCellData().SetScalars(colors);

        polydataSc = new vtkPolyData();
        pointsSc = new vtkPoints();
        vertSc = new vtkCellArray();
        polydataSc.SetPoints( pointsSc );
        polydataSc.SetVerts( vertSc );
        times = new vtkDoubleArray();
        ranges = new vtkDoubleArray();

        idList = new vtkIdList();
        idList.SetNumberOfIds(1);

        xIndex = xyzIndices[0];
        yIndex = xyzIndices[1];
        zIndex = xyzIndices[2];
        scxIndex = scXyzIndices[0];
        scyIndex = scXyzIndices[1];
        sczIndex = scXyzIndices[2];

        count = 0;

        fs = new FileInputStream(file.getAbsolutePath());

        // Set base color

        if (path.contains("_v2"))
        {
            baseColor = new Color(255, 255, 0);
        }
        else
        {
            baseColor = new Color(0, 0, 255);
        }

        // Variables to keep track of intensities
        minIntensity = Double.POSITIVE_INFINITY;
        maxIntensity = Double.NEGATIVE_INFINITY;
        intensityList = new LinkedList<Double>();

        // Parse data
        if (isBinary)
        {
            binaryTask = new BinaryDataTask();
            binaryTask.addPropertyChangeListener(this);
            binaryTask.execute();
        }
        else
        {   // ASCII-file
            InputStreamReader isr = new InputStreamReader(fs);
            BufferedReader in = new BufferedReader(isr);

            for (int i=0; i<numberHeaderLines; ++i)
                in.readLine();

            String line;

            while ((line = in.readLine()) != null)
            {
                String[] vals = line.trim().split("\\s+");

                // Don't include noise
                if (noiseIndex >=0 && vals[noiseIndex].equals("1"))
                    continue;

                // Parse lidar measured position
                double x=0, y=0, z=0;
                if(xIndex >= 0 && yIndex >= 0 && zIndex >= 0)
                {
                    x = Double.parseDouble(vals[xIndex]);
                    y = Double.parseDouble(vals[yIndex]);
                    z = Double.parseDouble(vals[zIndex]);
                }
                if(isLidarInSphericalCoordinates)
                {
                    // Convert from spherical to xyz
                    double[] xyz = MathUtil.latrec(new LatLon(y*Math.PI/180.0, x*Math.PI/180.0, z));
                    x = xyz[0];
                    y = xyz[1];
                    z = xyz[2];
                }

                // Parse spacecraft position
                double scx=0, scy=0, scz=0;
                if(scxIndex >= 0 && scyIndex >= 0 && sczIndex >= 0)
                {
                    scx = Double.parseDouble(vals[scxIndex]);
                    scy = Double.parseDouble(vals[scyIndex]);
                    scz = Double.parseDouble(vals[sczIndex]);
                }
                if (isSpacecraftInSphericalCoordinates)
                {
                    double[] xyz = MathUtil.latrec(new LatLon(scy*Math.PI/180.0, scx*Math.PI/180.0, scz));
                    scx = xyz[0];
                    scy = xyz[1];
                    scz = xyz[2];
                }

                // Convert distance units from m -> km
                if (isInMeters)
                {
                    x /= 1000.0;
                    y /= 1000.0;
                    z /= 1000.0;
                    scx /= 1000.0;
                    scy /= 1000.0;
                    scz /= 1000.0;
                }

                points.InsertNextPoint(x, y, z);
                idList.SetId(0, count);
                vert.InsertNextCell(idList);
                pointsSc.InsertNextPoint(scx, scy, scz);
                vertSc.InsertNextCell(idList);

                // Save range data
                double range;
                if(isRangeExplicitInData)
                {
                    // Range is explicitly listed in data, get it
                    range = Double.parseDouble(vals[rangeIndex]);
                    if(isInMeters)
                    {
                        range /= 1000.0;
                    }
                }
                else
                {
                    // Range is not explicitly listed, derive it from lidar measurement and sc positions
                    range = Math.sqrt((x-scx)*(x-scx) + (y-scy)*(y-scy) + (z-scz)*(z-scz));
                }
                ranges.InsertNextValue(range);

                // Extract the received intensity
                double irec = 0.0;
                if(intensityEnabled)
                {
                    irec = Double.parseDouble(vals[receivedIntensityIndex]);
                }

                // Add to list and keep track of min/max encountered so far
                minIntensity = (irec < minIntensity) ? irec : minIntensity;
                maxIntensity = (irec > maxIntensity) ? irec : maxIntensity;
                intensityList.add(irec);

                // We store the times in a vtk array. By storing in a vtk array, we don't have to
                // worry about java out of memory errors since java doesn't know about c++ memory.
                double t = 0;
                if(isTimeInET)
                {
                    // Read ET directly
                    t = Double.parseDouble(vals[timeIndex]);
                }
                else
                {
                    // Convert from UTC string to ET
                    t = TimeUtil.str2et(vals[timeIndex]);
                }
                times.InsertNextValue(t);

                ++count;
            }

            in.close();
            renderPoints();
            if (listener != null)
                listener.lidarLoadComplete(LidarDataPerUnit.this);
        }


    }

    protected void renderPoints()
    {
     // Color each point based on base color scaled by intensity
        Color plotColor;
        float[] baseHSL = ColorUtil.getHSLColorComponents(baseColor);
        for(double intensity : intensityList)
        {
            plotColor = ColorUtil.scaleLightness(baseHSL, intensity, minIntensity, maxIntensity, 0.5f);
            colors.InsertNextTuple4(plotColor.getRed(), plotColor.getGreen(), plotColor.getBlue(), plotColor.getAlpha());
        }

        polydata.GetCellData().GetScalars().Modified();
        polydata.Modified();
        originalPoints = new vtkPoints();
        originalPoints.DeepCopy(points);
        originalPointsSc = new vtkPoints();
        originalPointsSc.DeepCopy(pointsSc);

        geometryFilter = new vtkGeometryFilter();
        geometryFilter.SetInputData(polydata);
        geometryFilter.PointClippingOn();
        geometryFilter.CellClippingOff();
        geometryFilter.ExtentClippingOff();
        geometryFilter.MergingOff();
        geometryFilter.SetPointMinimum(0);
        geometryFilter.SetPointMaximum(count);

        geometryFilterSc = new vtkGeometryFilter();
        geometryFilterSc.SetInputData(polydataSc);
        geometryFilterSc.PointClippingOn();
        geometryFilterSc.CellClippingOff();
        geometryFilterSc.ExtentClippingOff();
        geometryFilterSc.MergingOff();
        geometryFilterSc.SetPointMinimum(0);
        geometryFilterSc.SetPointMaximum(count);
        vtkPolyDataMapper pointsMapper = new vtkPolyDataMapper();
        pointsMapper.SetScalarModeToUseCellData();
        pointsMapper.SetInputConnection(geometryFilter.GetOutputPort());

        vtkActor actor = new SaavtkLODActor();
        actor.SetMapper(pointsMapper);
        vtkPolyDataMapper lodMapper = ((SaavtkLODActor)actor).setQuadricDecimatedLODMapper(geometryFilter.GetOutputPort());

        actor.GetProperty().SetPointSize(2.0);

        vtkPolyDataMapper pointsMapperSc = new vtkPolyDataMapper();
        pointsMapperSc.SetInputConnection(geometryFilterSc.GetOutputPort());

        actorSpacecraft = new SaavtkLODActor();
        actorSpacecraft.SetMapper(pointsMapperSc);
        ((SaavtkLODActor)actorSpacecraft).setQuadricDecimatedLODMapper(geometryFilterSc.GetOutputPort());
        actorSpacecraft.GetProperty().SetColor(0.0, 1.0, 0.0);
        // for Itokawa optimized lidar data, show in different color.
        if (path.contains("_v2"))
            actorSpacecraft.GetProperty().SetColor(1.0, 0.0, 1.0);

        actorSpacecraft.GetProperty().SetPointSize(2.0);

        actors.add(actor);
        actors.add(actorSpacecraft);
    }

    public void setPercentageShown(double startPercent, double stopPercent)
    {
        startPercentage = startPercent;
        stopPercentage = stopPercent;
    }

    public void showPercentageShown()
    {
        double numberOfPoints = originalPoints.GetNumberOfPoints();
        int firstPointId = (int)(numberOfPoints * startPercentage);
        int lastPointId = (int)(numberOfPoints * stopPercentage) - 1;
        if (lastPointId < firstPointId)
        {
            lastPointId = firstPointId;
        }

        geometryFilter.SetPointMinimum(firstPointId);
        geometryFilter.SetPointMaximum(lastPointId);
        geometryFilter.Update();

        geometryFilterSc.SetPointMinimum(firstPointId);
        geometryFilterSc.SetPointMaximum(lastPointId);
        geometryFilterSc.Update();

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);

    }

    public LidarDataPerUnit(String path,
            BodyViewConfig polyhedralModelConfig,
            LidarLoadingListener listener) throws IOException
    {
        this.path=path;
        this.config=polyhedralModelConfig;
        this.listener = listener;
        init();
    }

    public DoublePair getPercentageShown()
    {
        return new DoublePair(startPercentage, stopPercentage);
    }

    public void setOffset(double offset)
    {
        vtkPoints points = polydata.GetPoints();

        int numberOfPoints = points.GetNumberOfPoints();

        for (int i=0;i<numberOfPoints;++i)
        {
            double[] pt = originalPoints.GetPoint(i);
            LatLon lla = MathUtil.reclat(pt);
            lla = new LatLon(lla.lat, lla.lon, lla.rad + offset*offsetMultiplier);
            pt = MathUtil.latrec(lla);
            points.SetPoint(i, pt);
        }

        polydata.Modified();
    }

    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        cellId = geometryFilter.GetPointMinimum() + cellId;
        String filepath2 = filepath;
        if (filepath2.toLowerCase().endsWith(".gz"))
            filepath2 = filepath2.substring(0, filepath2.length()-3);
        File file = new File(filepath2);

        String timeStr = TimeUtil.et2str(times.GetValue(cellId));

        return String.format("Lidar point " + file.getName() + " acquired at " + timeStr +
                ", ET = %f, unmodified range = %f m", times.GetValue(cellId),
                ranges.GetValue(cellId)*1000);
    }

    public List<vtkProp> getProps()
    {
        return actors;
    }

    public void setShowSpacecraftPosition(boolean show)
    {
        if (actorSpacecraft != null)
            actorSpacecraft.SetVisibility(show ? 1 : 0);
    }

    class BinaryDataTask extends SwingWorker<Void, Void>
    {

        @Override
        protected Void doInBackground() throws Exception
        {
            FileChannel channel = fs.getChannel();
            ByteBuffer bb = ByteBuffer.allocateDirect((int) file.length());
            bb.clear();
            bb.order(ByteOrder.LITTLE_ENDIAN);
            if (channel.read(bb) != file.length())
            {
                fs.close();
                throw new IOException("Error reading " + path);
            }

            byte[] utcArray = new byte[24];

            int numRecords = (int) (file.length() / binaryRecordSize);
            Stopwatch sw = new Stopwatch();
            sw.start();
            for (count = 0; count < numRecords; ++count)
            {
                setProgress(count*100/numRecords);
                int xoffset = count*binaryRecordSize + xIndex;
                int yoffset = count*binaryRecordSize + yIndex;
                int zoffset = count*binaryRecordSize + zIndex;
                int scxoffset = count*binaryRecordSize + scxIndex;
                int scyoffset = count*binaryRecordSize + scyIndex;
                int sczoffset = count*binaryRecordSize + sczIndex;

                // Add lidar (x,y,z) and spacecraft (scx,scy,scz) data
                double x = bb.getDouble(xoffset);
                double y = bb.getDouble(yoffset);
                double z = bb.getDouble(zoffset);
                double scx = bb.getDouble(scxoffset);
                double scy = bb.getDouble(scyoffset);
                double scz = bb.getDouble(sczoffset);

                if (isInMeters)
                {
                    x /= 1000.0;
                    y /= 1000.0;
                    z /= 1000.0;
                    scx /= 1000.0;
                    scy /= 1000.0;
                    scz /= 1000.0;
                }
                points.InsertNextPoint(x, y, z);
                idList.SetId(0, count);
                vert.InsertNextCell(idList);

                // Save range data
                ranges.InsertNextValue(Math.sqrt((x-scx)*(x-scx) + (y-scy)*(y-scy) + (z-scz)*(z-scz)));

                // Extract the received intensity
                double irec = 0.0;
                if(intensityEnabled)
                {
                    // Extract received intensity and keep track of min/max encountered so far
                    int recIntensityOffset = count*binaryRecordSize + receivedIntensityIndex;
                    irec = bb.getDouble(recIntensityOffset);
                }

                // Add to list and keep track of min/max encountered so far
                minIntensity = (irec < minIntensity) ? irec : minIntensity;
                maxIntensity = (irec > maxIntensity) ? irec : maxIntensity;
                intensityList.add(irec);

                // assume no spacecraft position for now
                pointsSc.InsertNextPoint(scx, scy, scz);
                vertSc.InsertNextCell(idList);

                int timeoffset = count*binaryRecordSize + timeIndex;

                bb.position(timeoffset);
                bb.get(utcArray);
                String utc = new String(utcArray);

                // We store the times in a vtk array. By storing in a vtk array, we don't have to
                // worry about java out of memory errors since java doesn't know about c++ memory.
                double t = TimeUtil.str2et(utc);
                times.InsertNextValue(t);
            }
            setProgress(100);
            fs.close();
            renderPoints();

            return null;

        }

        @Override
        protected void done()
        {
            // TODO Auto-generated method stub
            super.done();
            if (listener != null)
                listener.lidarLoadComplete(LidarDataPerUnit.this);
        }

    }

    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName() ) {
            int progress = (Integer) evt.getNewValue();
            progressMonitor.setProgress(progress);
            String message =
                String.format("Completed %d%%.\n", progress);
            progressMonitor.setNote(message);
            if (progressMonitor.isCanceled() || binaryTask.isDone()) {
                if (progressMonitor.isCanceled()) {
                    binaryTask.cancel(true);
                } else {
//                    taskOutput.append("Task completed.\n");
                }
            }
        }

    }
}

