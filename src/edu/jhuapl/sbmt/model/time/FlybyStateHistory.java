package edu.jhuapl.sbmt.model.time;

import java.util.Map.Entry;

public interface FlybyStateHistory
{
    public Double getTime();

    public void setTime(Double time);

    public Double getTimeFraction();

    public void setTimeFraction(Double time);

    public Double getMinTime();
    public Double getMaxTime();

    public void put(FlybyState flybyState);

    public void put(Double time, FlybyState flybyState);

    public Entry<Double, FlybyState> getFloorEntry(Double time);

    public Entry<Double, FlybyState> getCeilingEntry(Double time);

    public FlybyState getValue(Double time);

    public FlybyState getCurrentValue();
}
