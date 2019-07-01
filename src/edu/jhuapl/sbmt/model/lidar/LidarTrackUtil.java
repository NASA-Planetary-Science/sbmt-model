package edu.jhuapl.sbmt.model.lidar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.lidar.hyperoctree.FSHyperTreeSkeleton;
import edu.jhuapl.sbmt.util.TimeUtil;

import glum.item.IdGenerator;

/**
 * Utility class that provides for standardized way of creating Tracks.
 *
 * @author lopeznr1
 */
public class LidarTrackUtil
{
	/**
	 * Utility method to synthesize a Track.
	 * <P>
	 * Null will be returned if any of the following is true:
	 * <UL>
	 * <LI>Insufficient number of points.
	 * <LI>Time associated with the first or last lidar point is not valid.
	 * </UL>
	 *
	 * @param aIdGenerator
	 * @param aPointL
	 * @param aSourceS
	 * @return
	 */
	public static LidarTrack formTrack(IdGenerator aIdGenerator, List<LidarPoint> aPointL, Set<String> aSourceS,
			double aMinTrackLen)
	{
		// Ensure the minimum number of points constraint is satisfied
		if (aPointL.size() < aMinTrackLen)
			return null;

		// Ensure the time ranges are valid. Sometimes a track ends up with
		// bad times because the user cancelled the search.
		LidarPoint begPt = aPointL.get(0);
		LidarPoint endPt = aPointL.get(aPointL.size() - 1);
		String[] timeRangeArr = new String[] { TimeUtil.et2str(begPt.getTime()), TimeUtil.et2str(endPt.getTime()) };
		if (timeRangeArr[0].length() == 0 || timeRangeArr[1].length() == 0)
			return null;

		// Form the list of the relevant points
		LidarTrack retTrack = new LidarTrack(aIdGenerator.getNextId(), aPointL, ImmutableList.copyOf(aSourceS));
		return retTrack;
	}

	public static List<LidarTrack> formTracks(IdGenerator aIdGenerator, List<LidarPoint> aPointL,
			Map<LidarPoint, String> aPointSourceM, double aTimeSeparationBetweenTracks, int aMinTrackLen)
	{
		List<LidarTrack> retTrackL = new ArrayList<>();

		int size = aPointL.size();
		if (size == 0)
			return retTrackL;

		// Sort points in time order
		Collections.sort(aPointL);

		// Start keeping track of vars needed to form a Track
		Set<String> tmpSourceS = new LinkedHashSet<>();
		int begIdx = 0;

		LidarPoint prevPt = null;
		for (int i = 0; i < size; i++)
		{
			LidarPoint currPt = aPointL.get(i);
			if (prevPt != null && currPt.getTime() - prevPt.getTime() >= aTimeSeparationBetweenTracks)
			{
				// Synthesize a new Track
				int endIdx = i - 1;
				LidarTrack tmpTrack = LidarTrackUtil.formTrack(aIdGenerator, aPointL.subList(begIdx, endIdx + 1),
						tmpSourceS, aMinTrackLen);
				if (tmpTrack != null)
					retTrackL.add(tmpTrack);

				// Start tracking the vars needed to form a track
				tmpSourceS = new LinkedHashSet<>();
				begIdx = i;
			}

			String source = aPointSourceM.get(currPt);
			if (tmpSourceS.contains(source) == false)
				tmpSourceS.add(source);

			prevPt = currPt;
		}

		// Synthesize the last Track
		int endIdx = size - 1;
		LidarTrack tmpTrack = LidarTrackUtil.formTrack(aIdGenerator, aPointL.subList(begIdx, endIdx + 1), tmpSourceS,
				aMinTrackLen);
		if (tmpTrack != null)
			retTrackL.add(tmpTrack);

		return retTrackL;
	}

	public static List<LidarTrack> formTracks(IdGenerator aItGenerator, Map<Integer, Set<LidarPoint>> aFilesWithPointM,
			FSHyperTreeSkeleton aSkeleton, double aTimeSeparationBetweenTracks, int aMinTrackLen)
	{
		// Bail if there are no points
		List<LidarTrack> retTrackL = new ArrayList<>();
		if (aFilesWithPointM.size() == 0)
			return retTrackL;

		Map<Integer, String> tmpFileM = new HashMap<>(aSkeleton.getFileMap());

		// List that holds the LidarPoints which form a Track
		List<LidarPoint> workPtL = new ArrayList<>();

		Set<String> tmpSourceS = new LinkedHashSet<>();
		for (Integer aFileNum : aFilesWithPointM.keySet())
		{
			// Keep track of sources associated with the track
			String source = tmpFileM.get(aFileNum);
			if (tmpSourceS.contains(source) == false)
				tmpSourceS.add(source);

			// Get all current points and sort by time
			List<LidarPoint> pntsFromCurrFile = new ArrayList<>(aFilesWithPointM.get(aFileNum));
			Collections.sort(pntsFromCurrFile);

			LidarPoint prevPt = null;
			for (LidarPoint aLP : pntsFromCurrFile)
			{
				if (prevPt != null && aLP.getTime() - prevPt.getTime() >= aTimeSeparationBetweenTracks)
				{
					// Synthesize a new Track
					LidarTrack tmpTrack = LidarTrackUtil.formTrack(aItGenerator, workPtL, tmpSourceS, aMinTrackLen);
					if (tmpTrack != null)
						retTrackL.add(tmpTrack);

					// Keep track of sources associated with the track
					workPtL.clear();
					tmpSourceS = new LinkedHashSet<>();
					tmpSourceS.add(source);
				}

				workPtL.add(aLP);
				prevPt = aLP;
			}
		}

		// Synthesize the last Track
		LidarTrack tmpTrack = LidarTrackUtil.formTrack(aItGenerator, workPtL, tmpSourceS, aMinTrackLen);
		if (tmpTrack != null)
			retTrackL.add(tmpTrack);

		// sort tracks by their starting time
		Collections.sort(retTrackL, new Comparator<LidarTrack>() {
			public int compare(LidarTrack track1, LidarTrack track2)
			{
				double timeBeg1 = track1.getTimeBeg();
				double timeBeg2 = track2.getTimeBeg();
				return timeBeg1 > timeBeg2 ? 1 : timeBeg1 < timeBeg2 ? -1 : 0;
			}
		});

		return retTrackL;
	}

}
