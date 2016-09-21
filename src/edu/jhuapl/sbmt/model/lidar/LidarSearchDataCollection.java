package edu.jhuapl.sbmt.model.lidar;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialFitter;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.optim.nonlinear.vector.jacobian.LevenbergMarquardtOptimizer;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkUnsignedCharArray;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.model.PointInRegionChecker;
import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.util.ColorUtil;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.FileUtil;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.Point3D;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.saavtk.util.SaavtkLODActor;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.lidar.test.BasicLidarPoint;
import edu.jhuapl.sbmt.lidar.test.LidarPoint;
import edu.jhuapl.sbmt.util.TimeUtil;
import edu.jhuapl.sbmt.util.gravity.Gravity;

public class LidarSearchDataCollection extends AbstractModel
{
    public enum TrackFileType
    {
        TEXT,
        BINARY,
        OLA_LEVEL_2
    };

    private BodyViewConfig polyhedralModelConfig;
    private PolyhedralModel smallBodyModel;
    private vtkPolyData polydata;   // target points
    private vtkPolyData selectedPointPolydata;
    protected List<LidarPoint> originalPoints = new ArrayList<LidarPoint>();
    private List<vtkProp> actors = new ArrayList<vtkProp>();
    private vtkPolyDataMapper pointsMapper;
    private vtkPolyDataMapper selectedPointMapper;
    private vtkActor actor;
    private vtkActor selectedPointActor;
    private vtkPolyData emptyPolyData; // an empty polydata for resetting

    private vtkPolyData scPosPolyData;  // spacecraft points
    private vtkPolyDataMapper scPosMapper;
    private vtkActor scPosActor;

    protected double radialOffset = 0.0;
    protected double[] translation = {0.0, 0.0, 0.0};

    private String dataSource;
    private double startDate;
    private double stopDate;
    private TreeSet<Integer> cubeList;

    private int selectedPoint = -1;

    protected List<Track> tracks = new ArrayList<Track>();
    private double timeSeparationBetweenTracks = 10.0; // In seconds
    private int minTrackLength = 1;
    private int[] defaultColor = {0, 0, 255, 255};
    private List<Integer> displayedPointToOriginalPointMap = new ArrayList<Integer>();
    private boolean enableTrackErrorComputation = false;
    private double trackError;


    private boolean showSpacecraftPosition = false;

    public class Track
    {
        public int startId = -1;
        public int stopId = -1;
        public boolean hidden = false;
        public int[] color = defaultColor.clone(); // blue by default
        List<Integer> sourceFiles=Lists.newArrayList();
        public String[] timeRange=new String[]{"",""};
        List<Map<Integer,String>> fileMaps=Lists.newArrayList();

        public int getNumberOfPoints()
        {
            return stopId - startId + 1;
        }

        public boolean containsId(int id)
        {
            return startId >= 0 && stopId >=0 && id >= startId && id <= stopId;
        }

        public int getNumberOfSourceFiles()
        {
            return sourceFiles.size();
        }

        public String getSourceFileName(int i)
        {
            return fileMaps.get(i).get(sourceFiles.get(i));
        }

        public void registerSourceFileIndex(int fileNum, Map<Integer,String> fileMap)
        {
            if (!sourceFiles.contains(fileNum))
            {
                sourceFiles.add(fileNum);
                fileMaps.add(fileMap);
            }
        }

        public LidarPoint getPoint(int i)
        {
            return originalPoints.get(startId+i);
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + startId;
            result = prime * result + stopId;
            result = prime * result + Arrays.hashCode(timeRange);
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Track other = (Track) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (startId != other.startId)
                return false;
            if (stopId != other.stopId)
                return false;
            if (!Arrays.equals(timeRange, other.timeRange))
                return false;
            return true;
        }

        private LidarSearchDataCollection getOuterType()
        {
            return LidarSearchDataCollection.this;
        }

    }

    public LidarSearchDataCollection(PolyhedralModel smallBodyModel)
    {
        this.smallBodyModel = smallBodyModel;
        this.polyhedralModelConfig = (BodyViewConfig)smallBodyModel.getConfig();

        // Initialize an empty polydata for resetting
        emptyPolyData = new vtkPolyData();
        vtkPoints points = new vtkPoints();
        vtkCellArray vert = new vtkCellArray();
        emptyPolyData.SetPoints( points );
        emptyPolyData.SetVerts( vert );
        vtkUnsignedCharArray colors = new vtkUnsignedCharArray();
        colors.SetNumberOfComponents(4);
        emptyPolyData.GetCellData().SetScalars(colors);

        polydata = new vtkPolyData();
        polydata.DeepCopy(emptyPolyData);

        selectedPointPolydata = new vtkPolyData();
        selectedPointPolydata.DeepCopy(emptyPolyData);

        pointsMapper = new vtkPolyDataMapper();
        pointsMapper.SetScalarModeToUseCellData();
        pointsMapper.SetInputData(polydata);

        selectedPointMapper = new vtkPolyDataMapper();
        selectedPointMapper.SetInputData(selectedPointPolydata);

        actor = new SaavtkLODActor();
        actor.SetMapper(pointsMapper);
        ((SaavtkLODActor)actor).addQuadricDecimatedLODMapper(polydata);
        actor.GetProperty().SetPointSize(2.0);

        actors.add(actor);

        selectedPointActor = new SaavtkLODActor();
        selectedPointActor.SetMapper(selectedPointMapper);
        ((SaavtkLODActor)selectedPointActor).addQuadricDecimatedLODMapper(selectedPointPolydata);
        selectedPointActor.GetProperty().SetColor(0.1, 0.1, 1.0);
        selectedPointActor.GetProperty().SetPointSize(7.0);

        actors.add(selectedPointActor);


        scPosPolyData=new vtkPolyData();
        scPosPolyData.DeepCopy(emptyPolyData);
        scPosMapper=new vtkPolyDataMapper();
        scPosMapper.SetInputData(scPosPolyData);
        scPosActor=new vtkActor();
        scPosActor.SetMapper(scPosMapper);
        actors.add(scPosActor);
    }

    public boolean isLoading()
    {
        return false;
    }

    public double getOffsetScale()
    {
        if (polyhedralModelConfig.lidarOffsetScale <= 0.0)
        {
            return smallBodyModel.getBoundingBoxDiagonalLength()/1546.4224133453388;
        }
        else
        {
            return polyhedralModelConfig.lidarOffsetScale;
        }
    }

    public Map<String, String> getLidarDataSourceMap()
    {
        return polyhedralModelConfig.lidarSearchDataSourceMap;
    }


    public void setLidarData(
            String dataSource,
            double startDate,
            double stopDate,
            TreeSet<Integer> cubeList,
            PointInRegionChecker pointInRegionChecker,
            double timeSeparationBetweenTracks,
            int minTrackLength) throws IOException, ParseException
    {
        runQuery(
                dataSource,
                startDate,
                stopDate,
                cubeList,
                pointInRegionChecker,
                timeSeparationBetweenTracks,
                minTrackLength);

        selectPoint(-1);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }


    protected void runQuery(
            String dataSource,
            double startDate,
            double stopDate,
            TreeSet<Integer> cubeList,
            PointInRegionChecker pointInRegionChecker,
            double timeSeparationBetweenTracks,
            int minTrackLength) throws IOException, ParseException
    {

        if (dataSource.equals(this.dataSource) &&
                startDate == this.startDate &&
                stopDate == this.stopDate &&
                cubeList.equals(this.cubeList) &&
                timeSeparationBetweenTracks == this.timeSeparationBetweenTracks &&
                minTrackLength == this.minTrackLength)
        {
            return;
        }

        // Make clones since otherwise the previous if statement might
        // evaluate to true even if something changed.
        this.dataSource = new String(dataSource);
        this.startDate = startDate;
        this.stopDate = stopDate;
        this.cubeList = (TreeSet<Integer>)cubeList.clone();
        this.timeSeparationBetweenTracks = timeSeparationBetweenTracks;
        this.minTrackLength = minTrackLength;


        double start = startDate;
        double stop = stopDate;

        originalPoints.clear();


        int timeindex = 0;
        int xindex = 1;
        int yindex = 2;
        int zindex = 3;
        int scxindex = 4;
        int scyindex = 5;
        int sczindex = 6;

        for (Integer cubeid : cubeList)
        {
            String filename = getLidarDataSourceMap().get(dataSource) + "/" + cubeid + ".lidarcube";
            File file = FileCache.getFileFromServer(filename);

            if (file == null)
                continue;

            InputStream fs = new FileInputStream(file.getAbsolutePath());
            InputStreamReader isr = new InputStreamReader(fs);
            BufferedReader in = new BufferedReader(isr);

            String lineRead;
            while ((lineRead = in.readLine()) != null)
            {
                String[] vals = lineRead.trim().split("\\s+");

                double time = TimeUtil.str2et(vals[timeindex]);
                if (time < start || time > stop)
                    continue;

                double[] scpos = new double[3];
                double[] target = new double[3];
                target[0] = Double.parseDouble(vals[xindex]);
                target[1] = Double.parseDouble(vals[yindex]);
                target[2] = Double.parseDouble(vals[zindex]);
                scpos[0] = Double.parseDouble(vals[scxindex]);
                scpos[1] = Double.parseDouble(vals[scyindex]);
                scpos[2] = Double.parseDouble(vals[sczindex]);

                if (pointInRegionChecker==null) // if this part of the code has been reached and the point-checker is null then this is a time-only search, and the time criterion has already been met (cf. continue statement a few lines above)
                {
                    originalPoints.add(new BasicLidarPoint(target, scpos, time, 0));
                    continue;
                }


                if (pointInRegionChecker.checkPointIsInRegion(target))  // here, the point is known to be within the specified time bounds, and since the point checker exists the target coordinates are filtered against
                {
                    originalPoints.add(new BasicLidarPoint(target, scpos, time, 0));
                    continue;
                }
            }

            in.close();
        }

        // Sort points in time order
        Collections.sort(originalPoints);

        radialOffset = 0.0;
        translation[0] = translation[1] = translation[2] = 0.0;

        computeTracks();
        removeTracksThatAreTooSmall();

        assignInitialColorToTrack();

        updateTrackPolydata();
    }

    public void loadTrackAscii(File file) throws IOException
    {
        InputStream fs = new FileInputStream(file.getAbsolutePath());
        InputStreamReader isr = new InputStreamReader(fs);
        BufferedReader in = new BufferedReader(isr);

        Track track = new Track();
        track.startId = originalPoints.size();

        String lineRead;
        while ((lineRead = in.readLine()) != null)
        {
            String[] vals = lineRead.trim().split("\\s+");

            double time = 0;
            double[] target = {0.0, 0.0, 0.0};
            double[] scpos = {0.0, 0.0, 0.0};

            // The lines in the file may contain either 3, or greater columns.
            // If 3, they are assumed to contain the lidar point only and time and spacecraft
            // position are set to zero. If 4 or 5, they are assumed to contain time and lidar point
            // and spacecraft position is set to zero. If 6, they are assumed to contain
            // lidar position and spacecraft position and time is set to zero. If 7 or greater,
            // they are assumed to contain time, lidar position, and spacecraft position.
            // In the case of 5 columns, the last column is ignored and in the case of
            // greater than 7 columns, columns 8 or higher are ignored.
            if (vals.length == 4 || vals.length == 5 || vals.length >= 7)
            {
                try
                {
                    // First try to see if it's a double ET. Otherwise assume it's UTC.
                    time = Double.parseDouble(vals[0]);
                }
                catch (NumberFormatException e)
                {
                    time = TimeUtil.str2et(vals[0]);
                    if (time == -Double.MIN_VALUE)
                    {
                        in.close();
                        throw new IOException("Error: Incorrect file format!");
                    }
                }
                target[0] = Double.parseDouble(vals[1]);
                target[1] = Double.parseDouble(vals[2]);
                target[2] = Double.parseDouble(vals[3]);
            }
            if (vals.length >= 7)
            {
                scpos[0] = Double.parseDouble(vals[4]);
                scpos[1] = Double.parseDouble(vals[5]);
                scpos[2] = Double.parseDouble(vals[6]);
            }
            if (vals.length == 3 || vals.length == 6)
            {
                target[0] = Double.parseDouble(vals[0]);
                target[1] = Double.parseDouble(vals[1]);
                target[2] = Double.parseDouble(vals[2]);
            }
            if (vals.length == 6)
            {
                scpos[0] = Double.parseDouble(vals[3]);
                scpos[1] = Double.parseDouble(vals[4]);
                scpos[2] = Double.parseDouble(vals[5]);
            }

            if (vals.length < 3)
            {
                in.close();
                throw new IOException("Error: Incorrect file format!");
            }

            originalPoints.add(new BasicLidarPoint(target, scpos, time, 0));
        }

        in.close();

        track.stopId = originalPoints.size() - 1;
        tracks.add(track);
    }

    public void loadTrackBinary(File file) throws IOException
    {
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

        Track track = new Track();
        track.startId = originalPoints.size();

        while (true)
        {
            double time = 0;
            double[] target = {0.0, 0.0, 0.0};
            double[] scpos = {0.0, 0.0, 0.0};

            try
            {
                time = FileUtil.readDoubleAndSwap(in);
            }
            catch(EOFException e)
            {
                break;
            }

            try
            {
                target[0] = FileUtil.readDoubleAndSwap(in);
                target[1] = FileUtil.readDoubleAndSwap(in);
                target[2] = FileUtil.readDoubleAndSwap(in);
                scpos[0] = FileUtil.readDoubleAndSwap(in);
                scpos[1] = FileUtil.readDoubleAndSwap(in);
                scpos[2] = FileUtil.readDoubleAndSwap(in);
            }
            catch(IOException e)
            {
                in.close();
                throw e;
            }

            originalPoints.add(new BasicLidarPoint(target, scpos, time, 0));
        }

        in.close();

        track.stopId = originalPoints.size() - 1;
        tracks.add(track);
    }

    private void skip(DataInputStream in, int n) throws IOException
    {
        for (int i = 0; i < n; ++i)
        {
            in.readByte();
        }
    }

    public void loadTrackOlaL2(File file) throws IOException
    {
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

        Track track = new Track();
        track.startId = originalPoints.size();

        while (true)
        {
            double time = 0;
            double[] target = {0.0, 0.0, 0.0};
            double[] scpos = {0.0, 0.0, 0.0};
            double intensityReceived = 0;
            boolean noise = false;

            try
            {
                in.readByte();
            }
            catch(EOFException e)
            {
                break;
            }

            try
            {
                skip(in, 17 + 8 + 24);
                time = FileUtil.readDoubleAndSwap(in);
                skip(in, 8 + 2 * 3);
                short flagStatus = MathUtil.swap(in.readShort());
                noise = ((flagStatus == 0 || flagStatus == 1) ? false : true);
                skip(in, 8 + 8 * 3);
                intensityReceived = FileUtil.readDoubleAndSwap(in);
                target[0] = FileUtil.readDoubleAndSwap(in) / 1000.0;
                target[1] = FileUtil.readDoubleAndSwap(in) / 1000.0;
                target[2] = FileUtil.readDoubleAndSwap(in) / 1000.0;
                skip(in, 8 * 3);
                scpos[0] = FileUtil.readDoubleAndSwap(in) / 1000.0;
                scpos[1] = FileUtil.readDoubleAndSwap(in) / 1000.0;
                scpos[2] = FileUtil.readDoubleAndSwap(in) / 1000.0;
            }
            catch(IOException e)
            {
                in.close();
                throw e;
            }

            if (!noise)
                originalPoints.add(new BasicLidarPoint(target, scpos, time, intensityReceived));
        }

        in.close();

        track.stopId = originalPoints.size() - 1;
        tracks.add(track);

    }

    BiMap<Integer, String> localFileMap=HashBiMap.create();
    List<int[]> fileBounds=Lists.newArrayList();    // for adding filenum information to tracks later; length 3 -> lowerBound,upperBound,fileNum

    /**
     * Load a track from a file. This will replace all currently existing tracks
     * with a single track.
     * @param filename
     * @throws IOException
     */
    public void loadTracksFromFiles(File[] files, TrackFileType trackFileType) throws IOException
    {
        originalPoints.clear();
        tracks.clear();
        fileBounds.clear();
        localFileMap.clear();

        int oldBounds=0;
        for (File file : files)
        {
            List<Track> oldTracks=Lists.newArrayList();
            oldTracks.addAll(tracks);

            if (trackFileType == TrackFileType.TEXT)
                loadTrackAscii(file);
            else if (trackFileType == TrackFileType.BINARY)
                loadTrackBinary(file);
            else
                loadTrackOlaL2(file);

            if (!localFileMap.containsValue(file.toString()))
                localFileMap.put(localFileMap.size(), file.toString());

            fileBounds.add(new int[]{oldBounds,originalPoints.size()-1,localFileMap.inverse().get(file.toString())});
            oldBounds=originalPoints.size();
        }

        //timeSeparationBetweenTracks = Double.MAX_VALUE;
        //radialOffset = 0.0;
        //translation[0] = translation[1] = translation[2] = 0.0;

        //int startTrack=tracks.size();
        computeTracks();
        removeTracksThatAreTooSmall();
        //int endTrack=tracks.size();

        assignInitialColorToTrack();

        //for (int i=startTrack; i<endTrack; i++)
        for (int i=0; i<tracks.size(); i++)
        {
            Track track=tracks.get(i);
            for (int f=0; f<fileBounds.size(); f++)
            {
                int[] bounds=fileBounds.get(f);
                if (track.startId>=bounds[0] && track.stopId<=bounds[1])
                    tracks.get(i).registerSourceFileIndex(bounds[2], localFileMap);
            }
        }

        updateTrackPolydata();


    }

    /**
     * Return the track with the specified trackId
     *
     * @param trackId
     * @return
     */
    public Track getTrack(int trackId)
    {
        return tracks.get(trackId);
    }

    public int getTrackIdFromPointId(int pointId)
    {
        pointId = displayedPointToOriginalPointMap.get(pointId);
        for (int i=0; i<tracks.size(); ++i)
        {
            if (getTrack(i).containsId(pointId))
                return i;
        }

        return -1;
    }

    public int getNumberOfTracks()
    {
        return tracks.size();
    }

    public int getNumberOfVisibleTracks()
    {
        int numVisibleTracks = 0;
        int numTracks = getNumberOfTracks();
        for (int i=0; i<numTracks; ++i)
            if (!getTrack(i).hidden)
                ++numVisibleTracks;

        return numVisibleTracks;
    }

/*    int nextId=0;
    Map<Integer, Integer> trackIds=Maps.newHashMap(); // map from hash codes to ids

    private Integer getTrackId(Track track)
    {
        int hash=track.hashCode();
        if (trackIds.containsKey(hash))
            return trackIds.get(hash);
        else
        {
            trackIds.put(hash, nextId);
            nextId++;
            return trackIds.get(hash);
        }
    }*/

    protected void computeTracks()
    {
        tracks.clear();

        int size = originalPoints.size();
        if (size == 0)
            return;

        double prevTime = originalPoints.get(0).getTime();
        Track track = new Track();
        track.startId = 0;
        tracks.add(track);

        for (int i=1; i<size; ++i)
        {
            double currentTime = originalPoints.get(i).getTime();

            if (currentTime - prevTime >= timeSeparationBetweenTracks)
            {
              //  System.out.println(currentTime-prevTime);
                track.stopId = i-1;
                double t0 = originalPoints.get(track.startId).getTime();
                double t1 = originalPoints.get(track.stopId).getTime();
                track.timeRange=new String[]{TimeUtil.et2str(t0),TimeUtil.et2str(t1)};

                track = new Track();
                track.startId = i;

                tracks.add(track);
            }

            prevTime = currentTime;
        }
        tracks.remove(tracks.size()-1); // last one is always empty so remove it

        track.stopId = size-1;
        /*double t0 = originalPoints.get(track.startId).getTime();
        double t1 = originalPoints.get(track.stopId).getTime();
        track.timeRange=new String[]{TimeUtil.et2str(t0),TimeUtil.et2str(t1)};
        tracks.add(track);*/


    }

    /**
     * If transformPoint is true, then the lidar points (not scpos) are translated using the current
     * radial offset and translation before being saved out. If false, the original points
     * are saved out unmodified.
     *
     * @param trackId
     * @param outfile
     * @param transformPoint
     * @throws IOException
     */
    public void saveTrack(int trackId, File outfile, boolean transformPoint) throws IOException
    {
        FileWriter fstream = new FileWriter(outfile);
        BufferedWriter out = new BufferedWriter(fstream);

        int startId = tracks.get(trackId).startId;
        int stopId = tracks.get(trackId).stopId;

        String newline = System.getProperty("line.separator");

        for (int i=startId; i<=stopId; ++i)
        {
            LidarPoint pt = originalPoints.get(i);
            double[] target = pt.getTargetPosition().toArray();
            double[] scpos = pt.getSourcePosition().toArray();
            if (transformPoint)
            {
                target = transformLidarPoint(target);
                scpos = transformScpos(scpos, target);
            }

            String timeString = TimeUtil.et2str(pt.getTime());

            out.write(timeString + " " +
                    target[0] + " " +
                    target[1] + " " +
                    target[2] + " " +
                    scpos[0] + " " +
                    scpos[1] + " " +
                    scpos[2] + " " +
                    MathUtil.distanceBetween(pt.getSourcePosition().toArray(), pt.getTargetPosition().toArray()) + newline);
        }

        out.close();
    }

    public void saveAllVisibleTracksToFolder(File folder, boolean transformPoint) throws IOException
    {
        int numTracks = getNumberOfTracks();
        for (int i=0; i<numTracks; ++i)
        {
            if (!getTrack(i).hidden)
            {
                File file = new File(folder.getAbsolutePath(), "track" + i + ".txt");
                saveTrack(i, file, transformPoint);
            }
        }
    }

    public void saveAllVisibleTracksToSingleFile(File file, boolean transformPoint) throws IOException
    {
        FileWriter fstream = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fstream);

        String newline = System.getProperty("line.separator");

        for (Track track : tracks)
        {
            if (!track.hidden)
            {
                int startId = track.startId;
                int stopId = track.stopId;

                for (int i=startId; i<=stopId; ++i)
                {
                    LidarPoint pt = originalPoints.get(i);
                    double[] target = pt.getTargetPosition().toArray();
                    double[] scpos = pt.getSourcePosition().toArray();
                    if (transformPoint)
                    {
                        target = transformLidarPoint(target);
                        scpos = transformScpos(scpos, target);
                    }

                    String timeString = TimeUtil.et2str(pt.getTime());

                    out.write(timeString + " " +
                            target[0] + " " +
                            target[1] + " " +
                            target[2] + " " +
                            scpos[0] + " " +
                            scpos[1] + " " +
                            scpos[2] + " " +
                            MathUtil.distanceBetween(pt.getSourcePosition().toArray(), pt.getTargetPosition().toArray()) + newline);
                }
            }
        }
        out.close();
    }

    protected void assignInitialColorToTrack()
    {
        Color[] colors = ColorUtil.generateColors(tracks.size());
        int[] color = new int[4];
        int i = 0;

        for (Track track : tracks)
        {
            color[0] = colors[i].getRed();
            color[1] = colors[i].getGreen();
            color[2] = colors[i].getBlue();
            color[3] = colors[i].getAlpha();

            track.color = color.clone();

            ++i;
        }
    }

    public void setTrackColor(int trackId, Color color)
    {
        Track track = tracks.get(trackId);
        track.color[0] = color.getRed();
        track.color[1] = color.getGreen();
        track.color[2] = color.getBlue();
        track.color[3] = color.getAlpha();
        updateTrackPolydata();
    }

    public int[] getTrackColor(int trackId)
    {
        return tracks.get(trackId).color.clone();
    }

    public void setColorAllTracks(Color color)
    {
        defaultColor[0] = color.getRed();
        defaultColor[1] = color.getGreen();
        defaultColor[2] = color.getBlue();
        defaultColor[3] = color.getAlpha();

        for (Track track : tracks)
        {
            track.color = defaultColor.clone();
        }

        updateTrackPolydata();
    }

    public void hideTrack(int trackId, boolean hide)
    {
        tracks.get(trackId).hidden = hide;
        updateTrackPolydata();
        selectedPoint=-1;
        updateSelectedPoint();
    }

    public void hideOtherTracksExcept(int trackId)
    {
        Track trackToHide = tracks.get(trackId);
        for (Track track : tracks)
        {
            if (track != trackToHide)
                track.hidden = true;
        }

        updateTrackPolydata();
        selectedPoint=-1;
        updateSelectedPoint();
    }

    public void hideAllTracks()
    {
        for (Track track : tracks)
        {
            track.hidden = true;
        }

        updateTrackPolydata();
        selectedPoint=-1;
        updateSelectedPoint();
    }

    public void showAllTracks()
    {
        for (Track track : tracks)
        {
            track.hidden = false;
        }

        updateTrackPolydata();
        selectedPoint=-1;
        updateSelectedPoint();
    }

    public boolean isTrackHidden(int trackId)
    {
        return tracks.get(trackId).hidden;
    }

    private int getDisplayPointIdFromOriginalPointId(int ptId)
    {
        return displayedPointToOriginalPointMap.indexOf(ptId);
    }

    private double[] transformLidarPoint(double[] pt)
    {
        if (radialOffset != 0.0)
        {
            LatLon lla = MathUtil.reclat(pt);
            lla.rad += radialOffset;
            pt = MathUtil.latrec(lla);
        }

        return new double[]{pt[0]+translation[0], pt[1]+translation[1], pt[2]+translation[2]};
    }

    /**
     * Similar to previous function but specific to spacecraft position. The difference is
     * that we calculate the radial offset we applied to the lidar and apply that offset
     * to the spacecraft (rather than computing the radial offset directly for the spacecraft).
     *
     * @param scpos
     * @param lidarPoint
     * @return
     */
    private double[] transformScpos(double[] scpos, double[] lidarPoint)
    {
        if (radialOffset != 0.0)
        {
            LatLon lla = MathUtil.reclat(lidarPoint);
            lla.rad += radialOffset;
            double[] offsetLidarPoint = MathUtil.latrec(lla);

            scpos[0] += (offsetLidarPoint[0]-lidarPoint[0]);
            scpos[1] += (offsetLidarPoint[1]-lidarPoint[1]);
            scpos[2] += (offsetLidarPoint[2]-lidarPoint[2]);
        }

        return new double[]{scpos[0]+translation[0], scpos[1]+translation[1], scpos[2]+translation[2]};
    }

    protected void updateTrackPolydata()
    {
        // Place the points into polydata
        polydata.DeepCopy(emptyPolyData);
        scPosPolyData.DeepCopy(emptyPolyData);

        vtkPoints points = polydata.GetPoints();
        vtkCellArray vert = polydata.GetVerts();
        vtkUnsignedCharArray colors = (vtkUnsignedCharArray)polydata.GetCellData().GetScalars();

        vtkPoints scPoints=scPosPolyData.GetPoints();
        vtkCellArray scVert=scPosPolyData.GetVerts();
        vtkUnsignedCharArray scColors=(vtkUnsignedCharArray)scPosPolyData.GetCellData().GetScalars();

        vtkIdList idList = new vtkIdList();
        idList.SetNumberOfIds(1);

        displayedPointToOriginalPointMap.clear();
        int count = 0;

        int numTracks = getNumberOfTracks();
        for (int j=0; j<numTracks; ++j)
        {
            Track track = getTrack(j);
            int startId = track.startId;
            int stopId = track.stopId;
            if (!track.hidden)
            {
                // Variables to keep track of intensities
                double minIntensity = Double.POSITIVE_INFINITY;
                double maxIntensity = Double.NEGATIVE_INFINITY;
                List<Double> intensityList = new LinkedList<Double>();

                // Go through each point in the track
                for (int i=startId; i<=stopId; ++i)
                {

                    double[] pt = originalPoints.get(i).getTargetPosition().toArray();
                    pt = transformLidarPoint(pt);
                    points.InsertNextPoint(pt);
                    idList.SetId(0, count);
                    vert.InsertNextCell(idList);

                    pt=originalPoints.get(i).getSourcePosition().toArray();
                    pt=transformLidarPoint(pt);
                    scPoints.InsertNextPoint(pt);
                    scVert.InsertNextCell(idList);

                    double intensityReceived = originalPoints.get(i).getIntensityReceived();
                    minIntensity = (intensityReceived < minIntensity) ? intensityReceived : minIntensity;
                    maxIntensity = (intensityReceived > maxIntensity) ? intensityReceived : maxIntensity;
                    intensityList.add(intensityReceived);

                    displayedPointToOriginalPointMap.add(i);
                    ++count;
                }

                // Assign colors to each point in that track
                Color trackColor = new Color(track.color[0], track.color[1], track.color[2], track.color[3]);
                float[] trackHSL = ColorUtil.getHSLColorComponents(trackColor);
                Color plotColor;
                for(double intensity : intensityList)
                {
                    plotColor = ColorUtil.scaleLightness(trackHSL, intensity, minIntensity, maxIntensity);
                    colors.InsertNextTuple4(plotColor.getRed(), plotColor.getGreen(), plotColor.getBlue(), plotColor.getAlpha());
                    scColors.InsertNextTuple4(plotColor.getRed(), plotColor.getGreen(), plotColor.getBlue(), plotColor.getAlpha());
                }
            }
        }
        polydata.GetCellData().GetScalars().Modified();
        polydata.Modified();

        if (!showSpacecraftPosition)
            scPosActor.VisibilityOff();
        else
        {
            scPosActor.VisibilityOn();
            scPosPolyData.GetCellData().GetScalars().Modified();
            scPosPolyData.Modified();
        }

        if (enableTrackErrorComputation)
            computeTrackError();

        pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    private void removeTrack(int trackId)
    {
        Track track = tracks.get(trackId);
        int trackSize = track.getNumberOfPoints();

        for (int i=track.stopId; i>=track.startId; --i)
            originalPoints.remove(i);

        tracks.remove(trackId);

        // Go through all tracks that follow the deleted track and shift
        // all the start and stop ids down by the size of the deleted track
        int numberOfTracks = tracks.size();
        for (int i=trackId; i<numberOfTracks; ++i)
        {
            track = tracks.get(i);
            track.startId -= trackSize;
            track.stopId -= trackSize;
        }
    }

    protected void removeTracksThatAreTooSmall()
    {
        for (int i=tracks.size()-1; i>=0; --i)
        {
            if (tracks.get(i).getNumberOfPoints() < minTrackLength) {
                removeTrack(i);
            }
        }
    }

    public void removeAllLidarData()
    {
        polydata.DeepCopy(emptyPolyData);
        originalPoints.clear();
        tracks.clear();
        fileBounds.clear();
        localFileMap.clear();

        this.dataSource = null;
        this.startDate = -Double.MIN_VALUE;
        this.stopDate = -Double.MIN_VALUE;
        this.cubeList = null;

        selectPoint(-1);

        pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public List<vtkProp> getProps()
    {
        return actors;
    }

    /**
     *  Returns whether or not <tt>prop</tt> is the prop used for the actual data points
     *  as opposed the selection prop which is used for showing only the selected point.
     * @param prop
     * @return
     */
    public boolean isDataPointsProp(vtkProp prop)
    {
        return prop == actor || prop == scPosActor;
    }

    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        if (!originalPoints.isEmpty() && !tracks.isEmpty())
        {
            cellId = displayedPointToOriginalPointMap.get(cellId);
            double et = originalPoints.get(cellId).getTime();
            /*double[] target = originalPoints.get(cellId).getTargetPosition().toArray();
            double[] scpos = originalPoints.get(cellId).getSourcePosition().toArray();
            double range_m = Math.sqrt(
                    (target[0]-scpos[0])*(target[0]-scpos[0]) +
                    (target[1]-scpos[1])*(target[1]-scpos[1]) +
                    (target[2]-scpos[2])*(target[2]-scpos[2]))*1000;*/
            double range=originalPoints.get(cellId).getRange()*1000;    // m
            return String.format("Lidar point acquired at " + TimeUtil.et2str(et) +
                    ", ET = %f, unmodified range = %f m", et, range);
        }

        return "";
    }

    public void setOffset(double offset)
    {
        if (offset == radialOffset)
            return;

        radialOffset = offset;

        updateTrackPolydata();
        updateSelectedPoint();
    }

    public void setTranslation(double[] translation)
    {
        if (this.translation[0] == translation[0] && this.translation[1] == translation[1] && this.translation[2] == translation[2])
            return;

        this.translation[0] = translation[0];
        this.translation[1] = translation[1];
        this.translation[2] = translation[2];

        updateTrackPolydata();
        updateSelectedPoint();
    }

    public double[] getTranslation()
    {
        return this.translation;
    }

    public void setPointSize(int size)
    {
        if (actor != null)
        {
            actor.GetProperty().SetPointSize(size);

            pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }

    public int getNumberOfPoints()
    {
        return originalPoints.size();
    }

    public double getTimeOfPoint(int i)
    {
        return originalPoints.get(i).getTime();
    }

    /** It is useful to fit a line to the track. The following function computes
     * the parameters of such a line, namely, a point on the line
     * and a vector pointing in the direction of the line.
     * Note that the returned fittedLinePoint is the point on the line closest to
     * the first point of the track.
     */
    private void fitLineToTrack(int trackId, double[] fittedLinePoint, double[] fittedLineDirection)
    {
        Track track = tracks.get(trackId);
        int startId = track.startId;
        int stopId = track.stopId;

        if (startId == stopId)
            return;

        try
        {
            double t0 = originalPoints.get(startId).getTime();

            double[] lineStartPoint = new double[3];
            for (int j=0; j<3; ++j)
            {
                PolynomialFitter fitter = new PolynomialFitter(new LevenbergMarquardtOptimizer());
                for (int i=startId; i<=stopId; ++i)
                {
                    LidarPoint lp = originalPoints.get(i);
                    double[] target = transformLidarPoint(lp.getTargetPosition().toArray());
                    fitter.addObservedPoint(1.0, lp.getTime()-t0, target[j]);
                }

                PolynomialFunction fitted = new PolynomialFunction(fitter.fit(new double[2]));
                fittedLineDirection[j] = fitted.getCoefficients()[1];
                lineStartPoint[j] = fitted.value(0.0);
            }
            MathUtil.vhat(fittedLineDirection, fittedLineDirection);

            // Set the fittedLinePoint to the point on the line closest to first track point
            // as this makes it easier to do distance computations along the line.
            double[] dist = new double[1];
            double[] target = transformLidarPoint(originalPoints.get(startId).getTargetPosition().toArray());
            MathUtil.nplnpt(lineStartPoint, fittedLineDirection, target, fittedLinePoint, dist);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private double distanceOfClosestPointOnLineToStartOfLine(double[] point, int trackId, double[] fittedLinePoint, double[] fittedLineDirection)
    {
        Track track = tracks.get(trackId);
        if (track.startId == track.stopId)
            return 0.0;

        double[] pnear = new double[3];
        double[] dist = new double[1];
        MathUtil.nplnpt(fittedLinePoint, fittedLineDirection, point, pnear, dist);

        return MathUtil.distanceBetween(pnear, fittedLinePoint);
    }

    /**
     * Run gravity program on specified track and return potential, acceleration,
     * and elevation as function of distance and time.
     * @param trackId
     * @throws Exception
     */
    public void getGravityDataForTrack(
            int trackId,
            List<Double> potential,
            List<Double> acceleration,
            List<Double> elevation,
            List<Double> distance,
            List<Double> time) throws Exception
    {
        Track track = tracks.get(trackId);

        if (originalPoints.size() == 0 || track.startId < 0 || track.stopId < 0)
            throw new IOException();

        // Run the gravity program
        int startId = tracks.get(trackId).startId;
        int stopId = tracks.get(trackId).stopId;
        List<double[]> xyzPointList = new ArrayList<double[]>();
        for (int i=startId; i<=stopId; ++i)
        {
            LidarPoint pt = originalPoints.get(i);
            double[] target = pt.getTargetPosition().toArray();
            target = transformLidarPoint(target);
            xyzPointList.add(target);
        }
        List<Point3D> accelerationVector = new ArrayList<Point3D>();
        Gravity.getGravityAtPoints(
                xyzPointList,
                smallBodyModel.getDensity(),
                smallBodyModel.getRotationRate(),
                smallBodyModel.getReferencePotential(),
                smallBodyModel.getSmallBodyPolyData(),
                elevation,
                acceleration,
                accelerationVector,
                potential);

        double[] fittedLinePoint = new double[3];
        double[] fittedLineDirection = new double[3];
        fitLineToTrack(trackId, fittedLinePoint, fittedLineDirection);
        for (int i=track.startId; i<=track.stopId; ++i)
        {
            double[] point = originalPoints.get(i).getTargetPosition().toArray();
            point = transformLidarPoint(point);
            double dist = distanceOfClosestPointOnLineToStartOfLine(point, trackId, fittedLinePoint, fittedLineDirection);
            distance.add(dist);
            time.add(originalPoints.get(i).getTime());
        }
    }


    /**
     * select a point
     * @param ptId point id which must be id of a displayed point, not an original point
     */
    public void selectPoint(int ptId)
    {
        if (ptId >= 0)
            selectedPoint = displayedPointToOriginalPointMap.get(ptId);
        else
            selectedPoint = -1;

        selectedPointPolydata.DeepCopy(emptyPolyData);
        vtkPoints points = selectedPointPolydata.GetPoints();
        vtkCellArray vert = selectedPointPolydata.GetVerts();
        vtkUnsignedCharArray colors = (vtkUnsignedCharArray)selectedPointPolydata.GetCellData().GetScalars();

        if (ptId >= 0)
        {
            vtkIdList idList = new vtkIdList();
            int id1=points.InsertNextPoint(polydata.GetPoints().GetPoint(ptId));
            idList.InsertNextId(id1);

            if (showSpacecraftPosition)
            {
                int id2=points.InsertNextPoint(scPosPolyData.GetPoints().GetPoint(ptId));
                idList.InsertNextId(id2);
            }

            vert.InsertNextCell(idList);

            colors.InsertNextTuple4(0, 0, 255, 255);
        }

        selectedPointPolydata.Modified();

        pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void updateSelectedPoint()
    {
        int ptId = -1;
        if (selectedPoint >= 0)
            ptId = getDisplayPointIdFromOriginalPointId(selectedPoint);

        if (ptId < 0)
        {
            selectedPointPolydata.DeepCopy(emptyPolyData);
        }
        else
        {
            vtkPoints points=new vtkPoints();
            points.InsertNextPoint(polydata.GetPoints().GetPoint(ptId));
            if (showSpacecraftPosition)
                points.InsertNextPoint(scPosPolyData.GetPoints().GetPoint(ptId));
            selectedPointPolydata.SetPoints(points);
        }

        selectedPointPolydata.Modified();

        pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public double[] getSelectedPoint()
    {
        if (selectedPoint >= 0)
            return originalPoints.get(selectedPoint).getTargetPosition().toArray().clone();

        return null;
    }

    public int getNumberOfPointsPerTrack(int trackId)
    {
        return tracks.get(trackId).getNumberOfPoints();
    }

    public void setEnableTrackErrorComputation(boolean enable)
    {
        enableTrackErrorComputation = enable;
        if (enable)
            computeTrackError();
    }

    private void computeTrackError()
    {
        trackError = 0.0;

        vtkPoints points = polydata.GetPoints();
        int numberOfPoints = points.GetNumberOfPoints();
        double[] pt = new double[3];
        for (int i=0; i<numberOfPoints; ++i)
        {
            points.GetPoint(i, pt);
            double[] closestPt = smallBodyModel.findClosestPoint(pt);
            trackError += MathUtil.distance2Between(pt, closestPt);
        }

        if (numberOfPoints > 0)
            trackError /= (double)numberOfPoints;

        trackError = Math.sqrt(trackError);
    }

    public double getTrackError()
    {
        return trackError;
    }

    private double[] getCentroidOfTrack(int trackId)
    {
        Track track = tracks.get(trackId);
        int startId = track.startId;
        int stopId = track.stopId;

        double[] centroid = {0.0, 0.0, 0.0};
        for (int i=startId; i<=stopId; ++i)
        {
            LidarPoint lp = originalPoints.get(i);
            double[] target = transformLidarPoint(lp.getTargetPosition().toArray());
            centroid[0] += target[0];
            centroid[1] += target[1];
            centroid[2] += target[2];
        }

        int trackSize = track.getNumberOfPoints();
        if (trackSize > 0)
        {
            centroid[0] /= trackSize;
            centroid[1] /= trackSize;
            centroid[2] /= trackSize;
        }

        return centroid;
    }

    /** It is useful to fit a plane to the track. The following function computes
     * the parameters of such a plane, namely, a point on the plane
     * and a vector pointing in the normal direction of the plane.
     * Note that the returned pointOnPlane is the point on the plane closest to
     * the centroid of the track.
     */
    private void fitPlaneToTrack(int trackId, double[] pointOnPlane, RealMatrix planeOrientation)
    {
        Track track = tracks.get(trackId);
        int startId = track.startId;
        int stopId = track.stopId;

        if (startId == stopId)
            return;

        try
        {
            double[] centroid = getCentroidOfTrack(trackId);

            // subtract out the centroid from the track
            int trackSize = track.getNumberOfPoints();
            double[][] points = new double[3][trackSize];
            for (int i=startId,j=0; i<=stopId; ++i,++j)
            {
                LidarPoint lp = originalPoints.get(i);
                double[] target = transformLidarPoint(lp.getTargetPosition().toArray());
                points[0][j] = target[0] - centroid[0];
                points[1][j] = target[1] - centroid[1];
                points[2][j] = target[2] - centroid[2];
            }
            RealMatrix pointMatrix = new Array2DRowRealMatrix(points, false);

            // Now do SVD on this matrix
            SingularValueDecomposition svd = new SingularValueDecomposition(pointMatrix);
            RealMatrix u = svd.getU();

            planeOrientation.setSubMatrix(u.copy().getData(), 0, 0);

            pointOnPlane[0] = centroid[0];
            pointOnPlane[1] = centroid[1];
            pointOnPlane[2] = centroid[2];
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * This function takes a lidar track, fits a plane through it and reorients the track into
     * a new coordinate system such that the fitted plane is the XY plane in the new coordinate
     * system, and saves the reoriented track and the transformation to a file.

     * @param trackId
     * @param outfile - save reoriented track to this file
     * @param rotationMatrixFile - save transformation matrix to this file
     * @throws IOException
     */
    public void reprojectedTrackOntoFittedPlane(int trackId, File outfile, File rotationMatrixFile) throws IOException
    {
        Track track = tracks.get(trackId);
        int startId = track.startId;
        int stopId = track.stopId;

        if (startId == stopId)
            return;

        double[] pointOnPlane = new double[3];
        RealMatrix planeOrientation = new Array2DRowRealMatrix(3, 3);

        fitPlaneToTrack(trackId, pointOnPlane, planeOrientation);
        planeOrientation = new LUDecomposition(planeOrientation).getSolver().getInverse();

        FileWriter fstream = new FileWriter(outfile);
        BufferedWriter out = new BufferedWriter(fstream);

        String newline = System.getProperty("line.separator");

        for (int i=startId; i<=stopId; ++i)
        {
            LidarPoint lp = originalPoints.get(i);
            double[] target = transformLidarPoint(lp.getTargetPosition().toArray());

            target[0] = target[0] - pointOnPlane[0];
            target[1] = target[1] - pointOnPlane[1];
            target[2] = target[2] - pointOnPlane[2];

            target = planeOrientation.operate(target);

            out.write(TimeUtil.et2str(lp.getTime()) + " " +
                    target[0] + " " +
                    target[1] + " " +
                    target[2] + newline);
        }

        out.close();


        // Also save out the orientation matrix.
        fstream = new FileWriter(rotationMatrixFile);
        out = new BufferedWriter(fstream);

        double[][] data = planeOrientation.getData();
        out.write(data[0][0] + " " + data[0][1] + " " + data[0][2] + " " + pointOnPlane[0] + "\n");
        out.write(data[1][0] + " " + data[1][1] + " " + data[1][2] + " " + pointOnPlane[1] + "\n");
        out.write(data[2][0] + " " + data[2][1] + " " + data[2][2] + " " + pointOnPlane[2] + "\n");

        out.close();
    }

    /**
     * Save track as binary file. Each record consists of 7 double precision values as follows:
     * 1. ET
     * 2. X target
     * 3. Y target
     * 4. Z target
     * 5. X sc pos
     * 6. Y sc pos
     * 7. Z sc pos
     * If transformPoint is true, then the lidar points (not scpos) are translated using the current
     * radial offset and translation before being saved out. If false, the original points
     * are saved out unmodified.
     *
     * @param trackId
     * @param outfile
     * @param transformPoint
     * @throws IOException
     */
    public void saveTrackBinary(int trackId, File outfile, boolean transformPoint) throws IOException
    {
        outfile = new File(outfile.getAbsolutePath() + ".bin");
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outfile)));

        int startId = tracks.get(trackId).startId;
        int stopId = tracks.get(trackId).stopId;

        for (int i=startId; i<=stopId; ++i)
        {
            LidarPoint pt = originalPoints.get(i);
            double[] target = pt.getTargetPosition().toArray();
            double[] scpos = pt.getSourcePosition().toArray();
            if (transformPoint)
            {
                target = transformLidarPoint(target);
                scpos = transformScpos(scpos, target);
            }

            FileUtil.writeDoubleAndSwap(out, pt.getTime());
            FileUtil.writeDoubleAndSwap(out, target[0]);
            FileUtil.writeDoubleAndSwap(out, target[1]);
            FileUtil.writeDoubleAndSwap(out, target[2]);
            FileUtil.writeDoubleAndSwap(out, scpos[0]);
            FileUtil.writeDoubleAndSwap(out, scpos[1]);
            FileUtil.writeDoubleAndSwap(out, scpos[2]);
        }

        out.close();
    }


    public void setShowSpacecraftPosition(boolean show)
    {
        showSpacecraftPosition = show;
        updateTrackPolydata();
        //selectedPoint=-1;
        updateSelectedPoint();
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }
}
