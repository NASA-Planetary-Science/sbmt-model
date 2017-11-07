package edu.jhuapl.sbmt.model.time;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import edu.jhuapl.sbmt.util.TimeUtil;

public class CsvState implements State
{
    private String utc;
    private double ephemerisTime;

    private double[] spacecraftPosition;
    private double[] spacecraftVelocity;
    private double[] earthPosition;
    private double[] sunPosition;

    private double[] spacecraftXAxis;
    private double[] spacecraftYAxis;
    private double[] spacecraftZAxis;

    //writes the binary data to a CSV file and sets the data to the correct variables
    public CsvState(String utc, double sunPosX, double sunPosY, double sunPosZ,
                                double earthPosX, double earthPosY, double earthPosZ,
                                double spacecraftPosX, double spacecraftPosY, double spacecraftPosZ,
                                double spacecraftVelX, double spacecraftVelY, double spacecraftVelZ)
    {
        this.utc = utc;
        ephemerisTime = TimeUtil.str2et(utc);

        //System.out.println("Time: " + utc + "  " + earthPosY);

        sunPosition = new double[] {sunPosX, sunPosY, sunPosZ};
        earthPosition = new double[] {earthPosX, earthPosY, earthPosZ};
        spacecraftPosition = new double[] {spacecraftPosX, spacecraftPosY, spacecraftPosZ};
        spacecraftVelocity = new double[] {spacecraftVelX, spacecraftVelY, spacecraftVelZ};
        spacecraftXAxis = new double[] { 1.0, 0.0, 0.0 };
        spacecraftYAxis = new double[] { 0.0, 1.0, 0.0 };
        spacecraftZAxis = new double[] { 0.0, 0.0, 1.0 };
    }

    public void writeToCSV(String path)
    {
        try
        {
            File file = new File(path);
            FileWriter in = new FileWriter(file, true);

            in.append(utc);
            in.append(',');
            in.append(Double.toString(sunPosition[0]));
            in.append(',');
            in.append(Double.toString(sunPosition[1]));
            in.append(',');
            in.append(Double.toString(sunPosition[2]));
            in.append(',');
            in.append(Double.toString(earthPosition[0]));
            in.append(',');
            in.append(Double.toString(earthPosition[1]));
            in.append(',');
            in.append(Double.toString(earthPosition[2]));
            in.append(',');
            in.append(Double.toString(spacecraftPosition[0]));
            in.append(',');
            in.append(Double.toString(spacecraftPosition[1]));
            in.append(',');
            in.append(Double.toString(spacecraftPosition[2]));
            in.append(',');
            in.append(Double.toString(spacecraftVelocity[0]));
            in.append(',');
            in.append(Double.toString(spacecraftVelocity[1]));
            in.append(',');
            in.append(Double.toString(spacecraftVelocity[2]));
            in.append('\n');

            in.flush();
            in.close();
        }
        catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    // reads in a csv line and adds the data to the correct variables
    public CsvState(String line)
    {
        // initial values
        spacecraftPosition = new double[] { 0.0, 0.0, 0.0 };
        spacecraftVelocity = new double[] { 0.0, 0.0, 0.0 };
        earthPosition = new double[] { 0.0, 0.0, 0.0 };
        sunPosition = new double[] { 0.0, 0.0, 0.0 };
        spacecraftXAxis = new double[] { 1.0, 0.0, 0.0 };
        spacecraftYAxis = new double[] { 0.0, 1.0, 0.0 };
        spacecraftZAxis = new double[] { 0.0, 0.0, 1.0 };

        String[] parts = line.split(",");
        int ntokens = parts.length;

        if (ntokens > 0)
        {
            utc = parts[0].trim();
            ephemerisTime = TimeUtil.str2et(utc);
//            System.out.println("State: " + utc + " = " + ephemerisTime);
        }

        if (ntokens > 3)
        {
            sunPosition = new double[]
            {
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3])
            };
        }

        if (ntokens > 6)
        {
            earthPosition = new double[]
            {
                Double.parseDouble(parts[4]),
                Double.parseDouble(parts[5]),
                Double.parseDouble(parts[6])
            };
        }

        if (ntokens > 9)
        {
            spacecraftPosition = new double[]
            {
                Double.parseDouble(parts[7]),
                Double.parseDouble(parts[8]),
                Double.parseDouble(parts[9])
            };
        }

        if (ntokens > 12)
        {
            spacecraftVelocity = new double[]
            {
                Double.parseDouble(parts[10]),
                Double.parseDouble(parts[11]),
                Double.parseDouble(parts[12])
            };
        }
    }

    @Override
    public int getImageNumber()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getFrameNumber()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getEphemerisTime()
    {
        return ephemerisTime;
    }

    @Override
    public String getUtc()
    {
        return utc;
    }

    @Override
    public double getViewingAngle()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getRollAngle()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getSpacecraftAltitude()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double[] getSurfaceIntercept()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double[] getSurfaceInterceptLatLon()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getCrossTrackPixelSpacingKm()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getAlongTrackPixelSpacing()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getSolarIncidenceAngle()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getEmissionAngle()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getLocalSolarTime()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double[] getSpacecraftPosition()
    {
        return spacecraftPosition;
    }

    @Override
    public double[] getSpacecraftVelocity()
    {
        return spacecraftVelocity;
    }

    @Override
    public double[] getSunPosition()
    {
        return sunPosition;
    }

    @Override
    public double[] getEarthPosition()
    {
        return earthPosition;
    }

    @Override
    public double[] getSubSolarPointLatLon()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public float getFrameScore()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double[] getSpacecraftXAxis()
    {
        return spacecraftXAxis;
    }

    @Override
    public double[] getSpacecraftYAxis()
    {
        return spacecraftYAxis;
    }

    @Override
    public double[] getSpacecraftZAxis()
    {
        return spacecraftZAxis;
    }

}
