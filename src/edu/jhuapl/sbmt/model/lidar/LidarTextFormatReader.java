package edu.jhuapl.sbmt.model.lidar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import edu.jhuapl.sbmt.lidar.BasicLidarPoint;
import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.util.TimeUtil;

/**
 * @author steelrj1
 *
 * Reader for LIDAR data presented in a text format, command separated.  This reader can handle the following formats:
 *
 * 3 Columns:       lidarX, lidarY, lidarZ                                                                  //LIDAR point only (xyz) (t and SC Pos 0)
 * 4 Columns:       lidarX, lidarY, lidarZ, albedo                                                          //LIDAR point (xyz) with intensity (t and SC Pos 0)
 * 4 Columns v2:    t, lidarX, lidarY, lidarZ                                                               //time (et or UTC string), LIDAR point (xyz)
 * 5 Columns:       t, lidarX, lidarY, lidarZ, albedo                                                       //time (et or UTC string), LIDAR point (xyz), albedo
 * 6 Columns:       lidarX, lidarY, lidarZ, spcx, spcy, spcz                                                //LIDAR point only (xyz), SC pos (xyz) (t is 0)
 * 7 Columns:       t, lidarX, lidarY, lidarZ, spcx, spcy, spcz                                             //time (et or UTC string), LIDAR point only (xyz), SC pos (xyz)
 * 8 Columns:       t, lidarX, lidarY, lidarZ, spcx, spcy, spcz, albedo                                     //time (et or UTC string), LIDAR point only (xyz), SC pos (xyz), albedo
 *
 */

enum LIDARTextInputType
{
    LIDAR_ONLY("Lidar Point Only"),
    LIDAR_WITH_INTENSITY("Lidar Point with Intensity (Albedo)"),
    TIME_WITH_LIDAR("Time plus Lidar Point"),
    TIME_LIDAR_ALBEDO("Time, Lidar, Albedo"),
    LIDAR_SC("Lidar and SC Points"),
    TIME_LIDAR_SC("Time, Lidar and SC Points"),
    TIME_LIDAR_SC_ALBEDO("Time, Lidar, SC, Albedo");

    String name;

    private LIDARTextInputType(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void parseLine(String line, List<LidarPoint> originalPoints, Map<LidarPoint,Integer> originalPointsSourceFiles, BufferedReader in, int fileId) throws IOException
    {
        String[] vals = line.trim().split("\\s+");

        double time = 0;
        double[] target = {0.0, 0.0, 0.0};
        double[] scpos = {0.0, 0.0, 0.0};
        double albedo = 0;

        switch (this)
        {
        case LIDAR_ONLY:
            target[0] = Double.parseDouble(vals[0]);
            target[1] = Double.parseDouble(vals[1]);
            target[2] = Double.parseDouble(vals[2]);
            break;
        case LIDAR_WITH_INTENSITY:
            target[0] = Double.parseDouble(vals[0]);
            target[1] = Double.parseDouble(vals[1]);
            target[2] = Double.parseDouble(vals[2]);
            albedo = Double.parseDouble(vals[3]);
            break;
        case TIME_WITH_LIDAR:
            time = getTime(vals, in);
            target[0] = Double.parseDouble(vals[1]);
            target[1] = Double.parseDouble(vals[2]);
            target[2] = Double.parseDouble(vals[3]);
            break;
        case TIME_LIDAR_ALBEDO:
            time = getTime(vals, in);
            target[0] = Double.parseDouble(vals[1]);
            target[1] = Double.parseDouble(vals[2]);
            target[2] = Double.parseDouble(vals[3]);
            albedo = Double.parseDouble(vals[4]);
            break;
        case LIDAR_SC:
            target[0] = Double.parseDouble(vals[0]);
            target[1] = Double.parseDouble(vals[1]);
            target[2] = Double.parseDouble(vals[2]);
            scpos[0] = Double.parseDouble(vals[3]);
            scpos[1] = Double.parseDouble(vals[4]);
            scpos[2] = Double.parseDouble(vals[5]);
            break;
        case TIME_LIDAR_SC:
            time = getTime(vals, in);
            target[0] = Double.parseDouble(vals[1]);
            target[1] = Double.parseDouble(vals[2]);
            target[2] = Double.parseDouble(vals[3]);
            scpos[0] = Double.parseDouble(vals[4]);
            scpos[1] = Double.parseDouble(vals[5]);
            scpos[2] = Double.parseDouble(vals[6]);
            break;
        case TIME_LIDAR_SC_ALBEDO:
            time = getTime(vals, in);
            target[0] = Double.parseDouble(vals[1]);
            target[1] = Double.parseDouble(vals[2]);
            target[2] = Double.parseDouble(vals[3]);
            scpos[0] = Double.parseDouble(vals[4]);
            scpos[1] = Double.parseDouble(vals[5]);
            scpos[2] = Double.parseDouble(vals[6]);
            albedo = Double.parseDouble(vals[7]);
            break;

        default:
            break;
        }
        LidarPoint pt=new BasicLidarPoint(target, scpos, time, albedo);
        originalPoints.add(pt);
        originalPointsSourceFiles.put(pt, fileId);
    }

    private double getTime(String[] vals, BufferedReader in) throws IOException
    {
        double time = 0;
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
        return time;
    }
}





public class LidarTextFormatReader
{
    private File file;
    private List<LidarPoint> originalPoints;
    private Map<LidarPoint,Integer> originalPointsSourceFiles;
    private List<Track> tracks;

    int fileId = -1;

    public LidarTextFormatReader(File file, int fileID, List<LidarPoint> originalPoints, Map<LidarPoint,Integer> originalPointsSourceFiles, List<Track> tracks) throws NumberFormatException, IOException
    {
        this.file = file;
        this.fileId = fileID;
        this.originalPoints = originalPoints;
        this.originalPointsSourceFiles = originalPointsSourceFiles;
        this.tracks = tracks;
//        process();
    }

    public void process(LIDARTextInputType type) throws NumberFormatException, IOException
    {
        InputStream fs = new FileInputStream(file.getAbsolutePath());
        InputStreamReader isr = new InputStreamReader(fs);
        BufferedReader in = new BufferedReader(isr);

        Track track = new Track();
        track.startId = originalPoints.size();

        String lineRead;
        while ((lineRead = in.readLine()) != null)
        {
            type.parseLine(lineRead, originalPoints, originalPointsSourceFiles, in, fileId);
        }

        in.close();

        track.stopId = originalPoints.size() - 1;
        tracks.add(track);
    }


//    private void process() throws NumberFormatException, IOException
//    {
//        InputStream fs = new FileInputStream(file.getAbsolutePath());
//        InputStreamReader isr = new InputStreamReader(fs);
//        BufferedReader in = new BufferedReader(isr);
//
//        Track track = new Track();
//        track.startId = originalPoints.size();
//
////        int fileId=localFileMap.inverse().get(file.toString());
//
//        String lineRead;
//        while ((lineRead = in.readLine()) != null)
//        {
//            String[] vals = lineRead.trim().split("\\s+");
//
//            double time = 0;
//            double[] target = {0.0, 0.0, 0.0};
//            double[] scpos = {0.0, 0.0, 0.0};
//
//            // The lines in the file may contain either 3, or greater columns.
//            // If 3, they are assumed to contain the lidar point only and time and spacecraft
//            // position are set to zero. If 4 or 5, they are assumed to contain time and lidar point
//            // and spacecraft position is set to zero. If 6, they are assumed to contain
//            // lidar position and spacecraft position and time is set to zero. If 7 or greater,
//            // they are assumed to contain time, lidar position, and spacecraft position.
//            // In the case of 5 columns, the last column is ignored and in the case of
//            // greater than 7 columns, columns 8 or higher are ignored.
//            if (vals.length == 4 || vals.length == 5 || vals.length >= 7)
//            {
//                try
//                {
//                    // First try to see if it's a double ET. Otherwise assume it's UTC.
//                    time = Double.parseDouble(vals[0]);
//                }
//                catch (NumberFormatException e)
//                {
//                    time = TimeUtil.str2et(vals[0]);
//                    if (time == -Double.MIN_VALUE)
//                    {
//                        in.close();
//                        throw new IOException("Error: Incorrect file format!");
//                    }
//                }
//                target[0] = Double.parseDouble(vals[1]);
//                target[1] = Double.parseDouble(vals[2]);
//                target[2] = Double.parseDouble(vals[3]);
//            }
//            if (vals.length >= 7)
//            {
//                scpos[0] = Double.parseDouble(vals[4]);
//                scpos[1] = Double.parseDouble(vals[5]);
//                scpos[2] = Double.parseDouble(vals[6]);
//            }
//            if (vals.length == 3 || vals.length == 6)
//            {
//                target[0] = Double.parseDouble(vals[0]);
//                target[1] = Double.parseDouble(vals[1]);
//                target[2] = Double.parseDouble(vals[2]);
//            }
//            if (vals.length == 6)
//            {
//                scpos[0] = Double.parseDouble(vals[3]);
//                scpos[1] = Double.parseDouble(vals[4]);
//                scpos[2] = Double.parseDouble(vals[5]);
//            }
//
//            if (vals.length < 3)
//            {
//                in.close();
//                throw new IOException("Error: Incorrect file format!");
//            }
//
//            LidarPoint pt=new BasicLidarPoint(target, scpos, time, 0);
//            originalPoints.add(pt);
//            originalPointsSourceFiles.put(pt, fileId);
//        }
//
//        in.close();
//
//        track.stopId = originalPoints.size() - 1;
//        tracks.add(track);
//    }
}
