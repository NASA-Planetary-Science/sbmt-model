package edu.jhuapl.sbmt.model.phobos.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import com.beust.jcommander.internal.Lists;

public class CumulativeMEGANEFootprint extends MEGANEFootprint
{
	private List<MEGANEFootprint> footprints;
	private int index;
	private Set<Integer> combinedIndices = new HashSet<>();

	public CumulativeMEGANEFootprint(List<MEGANEFootprint> footprints)
	{
		super(0, 0, 0, 0, 0);
		this.footprints = Lists.newArrayList();
		this.footprints.addAll(footprints);
		facets = Lists.newArrayList();
		calculateCumulativeFootprint();
	}

	@Override
	public Vector<Integer> getCellIDs()
	{
		List<Integer> sortedCellIDs = Lists.newArrayList(combinedIndices);
		Collections.sort(sortedCellIDs);
		return new Vector<Integer>(sortedCellIDs);
	}

	public void setIndex(int index)
	{
		this.index = index;
	}

	/**
	 * @return the index
	 */
	public int getIndex()
	{
		return index;
	}

	public String getOriginalIndices()
	{
		return this.footprints.stream().map(fp -> fp.getDateTimeString()).collect(Collectors.joining(","));
	}

	private void calculateCumulativeFootprint()
	{
		//get the unified unique set of cell IDs for the cumulative footprint
		for (MEGANEFootprint footprint : footprints)
		{
			combinedIndices.addAll(footprint.getCellIDs());
		}

		for (MEGANEFootprint footprint : footprints)
		{
			for (Integer index : getCellIDs())
			{
				MEGANEFootprintFacet facet = null;
				Optional<MEGANEFootprintFacet> match = facets.stream().filter(fac ->  fac.getFacetID() == index ).findFirst();
				if (match.isPresent())
				{
					facet = match.get();
				}
				else
				{
					facet = new MEGANEFootprintFacet(index);
					facets.add(facet);
				}
				facet.addToComputedValue(footprint.getComputedValueAtFacet(index));
			}
		}
	}
}
