package edu.jhuapl.sbmt.model.time;

public interface FlybyState
{
    /** Image */
    public int getImageNumber();

    /** Frame */
    public int getFrameNumber();

    /** Ephemeris Time */
    public double getEphemerisTime();

    /** UTC Time */
    public String getUtc();

    /** View Angle (deg) */
    public double getViewingAngle();

    /** Roll Angle (deg) */
    public double getRollAngle();

    /** Spacecraft Altitude (km) */
    public double getSpacecraftAltitude();

    /** Intercept XYZ (km) - derived from SurfaceIntercept */
    public double[] getSurfaceIntercept();

    /** Intercept Lat-Lon (deg) */
    public double[] getSurfaceInterceptLatLon();

    /** Cross Track Spacing (km) */
    public double getCrossTrackPixelSpacingKm();

    /** Along Track Spacing (km) */
    public double getAlongTrackPixelSpacing();

    /** Solar Incidence Angle (deg) */
    public double getSolarIncidenceAngle();

    /** Emission Angle (deg) */
    public double getEmissionAngle();

    /** Local Solar Time (deg) */
    public double getLocalSolarTime();

    /** Spacecraft Position (km) */
    public double[] getSpacecraftPosition();

    /** Spacecraft Velocity (km/s) */
    public double[] getSpacecraftVelocity();

    /** Sun Position (km) */
    public double[] getSunPosition();

    /** Sub Solar Point Lat-Lon (deg) */
    public double[] getSubSolarPointLatLon();

    /** Frame Score */
    public float getFrameScore();

    /** SC X Axis */
    public double[] getSpacecraftXAxis();
    /** SC Y Axis */
    public double[] getSpacecraftYAxis();
    /** SC Z Axis */
    public double[] getSpacecraftZAxis();
}
