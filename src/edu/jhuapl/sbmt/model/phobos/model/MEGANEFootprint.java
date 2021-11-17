package edu.jhuapl.sbmt.model.phobos.model;

public class MEGANEFootprint
{
	private double dateTime;
	private double latDegrees;
	private double lonDegrees;
	private double altKm;
	private double normalizedAlt;

	public MEGANEFootprint(double dateTime, double latDegrees, double lonDegrees, double altKm, double normalizedAlt)
	{
		this.dateTime = dateTime;
		this.latDegrees = latDegrees;
		this.lonDegrees = lonDegrees;
		this.altKm = altKm;
		this.normalizedAlt = normalizedAlt;
	}

	public double getDateTime()
	{
		return dateTime;
	}

	/**
	 * @return the latDegrees
	 */
	public double getLatDegrees()
	{
		return latDegrees;
	}

	/**
	 * @return the lonDegrees
	 */
	public double getLonDegrees()
	{
		return lonDegrees;
	}

	/**
	 * @return the altKm
	 */
	public double getAltKm()
	{
		return altKm;
	}

	/**
	 * @return the normalizedAlt
	 */
	public double getNormalizedAlt()
	{
		return normalizedAlt;
	}
}
