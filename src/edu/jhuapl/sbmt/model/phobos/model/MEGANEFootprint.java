package edu.jhuapl.sbmt.model.phobos.model;

import java.util.List;
import java.util.Vector;

public class MEGANEFootprint
{
	private double dateTime;
	private double latDegrees;
	private double lonDegrees;
	private double altKm;
	private double normalizedAlt;
	private List<MEGANEFootprintFacet> facets;
	private boolean mapped;
	private String status;

	public MEGANEFootprint(double dateTime, double latDegrees, double lonDegrees, double altKm, double normalizedAlt)
	{
		this.dateTime = dateTime;
		this.latDegrees = latDegrees;
		this.lonDegrees = lonDegrees;
		this.altKm = altKm;
		this.normalizedAlt = normalizedAlt;
		this.status = "Unloaded";
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

	/**
	 * @return the facets
	 */
	public List<MEGANEFootprintFacet> getFacets()
	{
		return facets;
	}

	/**
	 * @param facets the facets to set
	 */
	public void setFacets(List<MEGANEFootprintFacet> facets)
	{
		this.facets = facets;
	}

	public Vector<Integer> getCellIDs()
	{
		Vector<Integer> cellIDs = new Vector<Integer>();
		for (MEGANEFootprintFacet facet : facets)
		{
			cellIDs.add(facet.getFacetID());
		}

		return cellIDs;
	}

	public double getSummedValue()
	{
		double total = 0;
		for (MEGANEFootprintFacet facet : facets)
		{
			total += facet.getComputedValue();
		}
		return total;
	}

	/**
	 * @return the mapped
	 */
	public boolean isMapped()
	{
		return mapped;
	}

	/**
	 * @param mapped the mapped to set
	 */
	public void setMapped(boolean mapped)
	{
		this.mapped = mapped;
	}

	/**
	 * @return the status
	 */
	public String getStatus()
	{
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status)
	{
		this.status = status;
	}
}
