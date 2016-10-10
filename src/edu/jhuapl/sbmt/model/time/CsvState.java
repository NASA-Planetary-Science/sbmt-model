package edu.jhuapl.sbmt.model.time;

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
            System.out.println("State: " + utc + " = " + ephemerisTime);
        }

        if (ntokens > 3)
        {
            spacecraftPosition = new double[]
            {
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3])
            };
        }

        if (ntokens > 6)
        {
            spacecraftPosition = new double[]
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
