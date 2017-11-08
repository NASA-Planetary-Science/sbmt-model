package edu.jhuapl.sbmt.model.time;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import altwg.util.MathUtil;

public class StandardStateHistory implements StateHistory
{
    private NavigableMap<Double, State> timeToFlybyState = new TreeMap<Double, State>();

    private Double time;

    public Double getTime()
    {
        return time;
    }

    public void setTime(Double time)
    {
        this.time = time;
    }

    public Double getMinTime()
    {
        return timeToFlybyState.firstKey();
    }

    public Double getMaxTime()
    {
        return timeToFlybyState.lastKey();
    }

    public Double getTimeFraction()
    {
        double min = getMinTime();
        double max = getMaxTime();
        double time = getTime();
        double result = (time - min) / (max - min);
        return result;
    }

    public void setTimeFraction(Double timeFraction)
    {
        double min = getMinTime();
        double max = getMaxTime();
        double time = min + timeFraction * (max - min);
        setTime(time);
    }

    public StandardStateHistory()
    {

    }

    public void put(State flybyState)
    {
        put(flybyState.getEphemerisTime(), flybyState);
    }

    public void put(Double time, State flybyState)
    {
        timeToFlybyState.put(time, flybyState);
    }

    public Entry<Double, State> getFloorEntry(Double time)
    {
        return timeToFlybyState.floorEntry(time);
    }

    public Entry<Double, State> getCeilingEntry(Double time)
    {
        return timeToFlybyState.ceilingEntry(time);
    }

    public State getValue(Double time)
    {
        // for now, just return floor
        return getFloorEntry(time).getValue();
    }

    public State getCurrentValue()
    {
        // for now, just return floor
        return getValue(getTime());
    }

    public Double getPeriod()
    {
        return getMaxTime() - getMinTime();
    }

    public double[] getSpacecraftPosition()
    {
        State floor = getFloorEntry(time).getValue();
        State ceiling = getCeilingEntry(time).getValue();
        double[] floorPosition = floor.getSpacecraftPosition();
        double[] ceilingPosition = ceiling.getSpacecraftPosition();
        double floorTime = floor.getEphemerisTime();
        double ceilingTime = ceiling.getEphemerisTime();
//        System.out.println("Floor: " + floorTime + " Ceiling: " + ceilingTime);

        return interpolateDouble(floorPosition, ceilingPosition, floorTime, ceilingTime, time);
    }

    public double[] getSunPosition()
    {
        State floor = getFloorEntry(time).getValue();
        State ceiling = getCeilingEntry(time).getValue();
        double[] floorPosition = floor.getSunPosition();
        double[] ceilingPosition = ceiling.getSunPosition();
        double floorTime = floor.getEphemerisTime();
        double ceilingTime = ceiling.getEphemerisTime();

        return interpolateDouble(floorPosition, ceilingPosition, floorTime, ceilingTime, time);
    }

    public double[] getEarthPosition()
    {
        State floor = getFloorEntry(time).getValue();
        State ceiling = getCeilingEntry(time).getValue();
        double[] floorPosition = floor.getEarthPosition();
        double[] ceilingPosition = ceiling.getEarthPosition();
        double floorTime = floor.getEphemerisTime();
        double ceilingTime = ceiling.getEphemerisTime();

        return interpolateDouble(floorPosition, ceilingPosition, floorTime, ceilingTime, time);
    }


    private double[] interpolateDouble(double[] floorPosition, double[] ceilingPosition, double floorTime, double ceilingTime, double time)
    {
        double timeDelta = ceilingTime - floorTime;
        if (timeDelta < epsilon)
        {
            return floorPosition;
        }
        else
        {
            //System.out.println(floorPosition[0] + " " + floorPosition[1] + " " + floorPosition[2]);
            //System.out.println(ceilingPosition[0] + " " + ceilingPosition[1] + " " + ceilingPosition[2]);
            double timeFraction = (time - floorTime) / timeDelta;
            double[] positionDelta = new double[3];
            MathUtil.vsub(ceilingPosition, floorPosition, positionDelta);
            double[] positionFraction = new double[3];
            MathUtil.vscl(timeFraction, positionDelta, positionFraction);
            double[] result = new double[3];
//            System.out.println("Time: " + time + " FloorTime: " + floorTime + " timeDelta: " + timeDelta);
//            System.out.println("TF: " + timeFraction);
            MathUtil.vadd(floorPosition, positionFraction, result);
            //System.out.println(result[0] + " " + result[1] + " " + result[2]);
            return result;
        }
    }

    @Override
    public Set<Double> getAllKeys()
    {
        return timeToFlybyState.keySet();

    }

}
