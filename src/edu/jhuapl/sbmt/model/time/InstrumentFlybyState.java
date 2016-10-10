package edu.jhuapl.sbmt.model.time;

public class InstrumentFlybyState extends InstrumentState implements State
{

    public InstrumentFlybyState()
    {
        // TODO Auto-generated constructor stub
    }

    public InstrumentFlybyState(InstrumentState st)
    {
        super(st);
        // TODO Auto-generated constructor stub
    }

    public double getEphemerisTime()
    {
        return t;
    }

    public String getUtc()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public double getViewingAngle()
    {
        return viewAngle;
    }
    public double getRollAngle()
    {
        return rollAngle;
    }
    public double getCrossTrackPixelSpacingKm()
    {
        return pixelSpacingKm;
    }

    public double getAlongTrackPixelSpacing()
    {
        return 0;
    }

    public double getSolarIncidenceAngle()
    {
        return incAngleDeg;
    }

    public double getEmissionAngle()
    {
        return emsAngleDeg;
    }

    public double getPhaseAngDeg()
    {
        return phaseAngDeg;
    }
    public double getSpacecraftAltitude()
    {
        return scAlt;
    }
    public double[] getSurfaceIntercept()
    {
        double[] result = { surfaceIntercept.X1(), surfaceIntercept.X2(), surfaceIntercept.X3() };
        return result;
    }
    public double[] getSurfaceInterceptLatLon()
    {
        double[] result = { surfacePt.lat, surfacePt.lon };
        return result;
    }
    public double[] getSubSolarPointLatLon()
    {
        double[] result = { subPt.lat, subPt.lon };
        return result;
    }
    public double[] getSpacecraftPosition()
    {
        double[] result = { scPos.X1(), scPos.X2(), scPos.X3() };
        return result;
    }
    public double[] getSpacecraftVelocity()
    {
        double[] result = { scVel.X1(), scVel.X2(), scVel.X3() };
        return result;
    }
    public double[] getSpacecraftXAxis()
    {
        double[] result = { xAxis.X1(), xAxis.X2(), xAxis.X3() };
        return result;
    }
    public double[] getSpacecraftYAxis()
    {
        double[] result = { yAxis.X1(), yAxis.X2(), yAxis.X3() };
        return result;
    }
    public double[] getSpacecraftZAxis()
    {
        double[] result = { zAxis.X1(), zAxis.X2(), zAxis.X3() };
        return result;
    }
    public double[] getSunPosition()
    {
        double[] result = { sunVec.X1(), sunVec.X2(), sunVec.X3() };
        return result;
    }

    public double getLocalSolarTime()
    {
        return 0;
    }

    public float getFrameScore()
    {
        return totalScore;
    }
    public int getImageNumber()
    {
        return imageNumber;
    }
    public int getFrameNumber()
    {
        return frameNumber;
    }

    public State parseStateString(String ststring)
    {
        InstrumentFlybyState result = new InstrumentFlybyState();
        InstrumentState.readStateString(result, ststring);
        return result;
    }



}
