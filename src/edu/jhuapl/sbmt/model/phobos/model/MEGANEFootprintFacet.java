package edu.jhuapl.sbmt.model.phobos.model;

public class MEGANEFootprintFacet
{
	private double time;
	private int facetID;
	private double cosE;
	private double projectedArea;
	private double range;
	private double integrationTime;

	/**
	 * @param time
	 * @param facetID
	 * @param cosE
	 * @param projectedArea
	 * @param range
	 */
	public MEGANEFootprintFacet(double time, double facetID, double integrationTime, double cosE, double projectedArea, double range)
	{
		this.time = time;
		this.facetID = (int)facetID;
		this.cosE = cosE;
		this.projectedArea = projectedArea;
		this.range = range;
		this.integrationTime = integrationTime;
	}

	/**
	 * @return the time
	 */
	public double getTime()
	{
		return time;
	}

	/**
	 * @return the facetID
	 */
	public int getFacetID()
	{
		return facetID;
	}

	/**
	 * @return the cosE
	 */
	public double getCosE()
	{
		return cosE;
	}

	/**
	 * @return the projectedArea
	 */
	public double getProjectedArea()
	{
		return projectedArea;
	}

	/**
	 * @return the range
	 */
	public double getRange()
	{
		return range;
	}

	public double getComputedValue()
	{
		return getProjectedArea()/Math.pow(getRange(), 2);
	}

}
