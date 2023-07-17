package edu.jhuapl.sbmt.model.eros.nis.util;

import edu.jhuapl.sbmt.core.util.TimeUtil;

public class NisTime
{
    String timeString;
    double timeEt;

    public NisTime(String timeString)
    {
        this.timeString=timeString;
        timeEt=TimeUtil.str2et(timeString);
    }

    @Override
    public String toString()
    {
        return timeString;
    }

    public double toEt()
    {
        return timeEt;
    }
}
