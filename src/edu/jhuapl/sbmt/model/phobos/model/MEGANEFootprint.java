package edu.jhuapl.sbmt.model.phobos.model;

import java.util.List;
import java.util.Optional;
import java.util.Vector;

import edu.jhuapl.sbmt.core.util.TimeUtil;

public class MEGANEFootprint
{
	private double dateTime;
	private double latRadians;
	private double lonRadians;
	private double altKm;
	private double normalizedAlt;
	protected List<MEGANEFootprintFacet> facets;
	private boolean mapped;
	private String status;
	private double signalContribution = -1;

	public MEGANEFootprint(double dateTime, double latDegrees, double lonDegrees, double altKm, double normalizedAlt)
	{
		this.dateTime = dateTime;
		this.latRadians = latDegrees;
		this.lonRadians = lonDegrees;
		this.altKm = altKm;
		this.normalizedAlt = normalizedAlt;
		this.status = "Unloaded";
	}

	public MEGANEFootprint(String line)
	{
		String[] parts = line.split(",");
		this.dateTime = Double.parseDouble(parts[0]);
		this.latRadians = Double.parseDouble(parts[1]);
		this.lonRadians = Double.parseDouble(parts[2]);
		this.altKm = Double.parseDouble(parts[3]);
		this.normalizedAlt = Double.parseDouble(parts[4]);
		this.signalContribution = Double.parseDouble(parts[5]);
		this.status = "Unloaded";
	}

	public double getDateTime()
	{
		return dateTime;
	}

	public String getDateTimeString()
	{
		String timeString = TimeUtil.et2str(dateTime);
		return timeString.substring(0, timeString.lastIndexOf("."));
	}

	/**
	 * @return the latDegrees
	 */
	public double getLatRadians()
	{
		return latRadians;
	}

	/**
	 * @return the lonDegrees
	 */
	public double getLonRadians()
	{
		return lonRadians;
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

	public void setSignalContribution(double contribution)
	{
		this.signalContribution = contribution;
	}

	public double getSignalContribution()
	{
		return signalContribution;
	}

	public double getComputedValueAtFacet(Integer index)
	{
		double value = 0;
		Optional<MEGANEFootprintFacet> match = facets.stream().filter(facet ->  facet.getFacetID() == index ).findFirst();
		if (match.isPresent()) value = match.get().getComputedValue();
		return value;
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

	public String toCSV()
	{
		return String.format("%s, %s, %s, %s, %s, %s",
				dateTime, Math.toDegrees(latRadians), Math.toDegrees(lonRadians), altKm, normalizedAlt, signalContribution);
	}

	@Override
	public String toString()
	{
		return String.format("MEGANEFootprint [dateTime=%s, latDegrees=%s, lonDegrees=%s, altKm=%s, normalizedAlt=%s, signalContribution=%s]",
				dateTime, Math.toDegrees(latRadians), Math.toDegrees(lonRadians), altKm, normalizedAlt, signalContribution);
	}
}
