package edu.jhuapl.sbmt.model.time;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

public class StandardFlybyStateHistory implements StateHistory, HasTime
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

    public StandardFlybyStateHistory()
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
}
