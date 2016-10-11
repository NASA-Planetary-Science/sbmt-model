package edu.jhuapl.sbmt.model.time;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import altwg.util.MathUtil;

public class StandardStateHistory implements StateHistory, HasTime
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

    public double[] getSpacecraftPosition()
    {
        State floor = getFloorEntry(time).getValue();
        State ceiling = getCeilingEntry(time).getValue();
        double[] floorPosition = floor.getSpacecraftPosition();
        double[] ceilingPosition = ceiling.getSpacecraftPosition();
        double floorTime = floor.getEphemerisTime();
        double ceilingTime = ceiling.getEphemerisTime();
        double timeDelta = ceilingTime - floorTime;
        double timeFraction = (time - floorTime) / timeDelta;
        double[] positionDelta = new double[3];
        MathUtil.vsub(ceilingPosition, floorPosition, positionDelta);
        double[] positionFraction = new double[3];
        MathUtil.vscl(timeFraction, positionDelta, positionFraction);
        double[] result = new double[3];
        MathUtil.vadd(floorPosition, positionFraction, result);
        return result;
    }

    public double[] getSunPosition()
    {
        State floor = getFloorEntry(time).getValue();
        State ceiling = getCeilingEntry(time).getValue();
        double[] floorPosition = floor.getSunPosition();
        double[] ceilingPosition = ceiling.getSunPosition();
        double floorTime = floor.getEphemerisTime();
        double ceilingTime = ceiling.getEphemerisTime();
        double timeDelta = ceilingTime - floorTime;
        double timeFraction = (time - floorTime) / timeDelta;
        double[] positionDelta = new double[3];
        MathUtil.vsub(ceilingPosition, floorPosition, positionDelta);
        double[] positionFraction = new double[3];
        MathUtil.vscl(timeFraction, positionDelta, positionFraction);
        double[] result = new double[3];
        MathUtil.vadd(floorPosition, positionFraction, result);
        return result;
    }

    public double[] getEarthPosition()
    {
        State floor = getFloorEntry(time).getValue();
        State ceiling = getCeilingEntry(time).getValue();
        double[] floorPosition = floor.getEarthPosition();
        double[] ceilingPosition = ceiling.getEarthPosition();
        double floorTime = floor.getEphemerisTime();
        double ceilingTime = ceiling.getEphemerisTime();
        double timeDelta = ceilingTime - floorTime;
        double timeFraction = (time - floorTime) / timeDelta;
        double[] positionDelta = new double[3];
        MathUtil.vsub(ceilingPosition, floorPosition, positionDelta);
        double[] positionFraction = new double[3];
        MathUtil.vscl(timeFraction, positionDelta, positionFraction);
        double[] result = new double[3];
        MathUtil.vadd(floorPosition, positionFraction, result);
        return result;
    }
}
