package edu.jhuapl.sbmt.model.time;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

public class StandardFlybyStateHistory implements FlybyStateHistory, HasTime
{
    private NavigableMap<Double, FlybyState> timeToFlybyState = new TreeMap<Double, FlybyState>();

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

    public StandardFlybyStateHistory()
    {

    }

    public void put(FlybyState flybyState)
    {
        put(flybyState.getEphemerisTime(), flybyState);
    }

    public void put(Double time, FlybyState flybyState)
    {
        timeToFlybyState.put(time, flybyState);
    }

    public Entry<Double, FlybyState> getFloorEntry(Double time)
    {
        return timeToFlybyState.floorEntry(time);
    }

    public Entry<Double, FlybyState> getCeilingEntry(Double time)
    {
        return timeToFlybyState.ceilingEntry(time);
    }

    public FlybyState getValue(Double time)
    {
        // for now, just return floor
        return getFloorEntry(time).getValue();
    }

    public FlybyState getCurrentValue()
    {
        // for now, just return floor
        return getValue(getTime());
    }
}
