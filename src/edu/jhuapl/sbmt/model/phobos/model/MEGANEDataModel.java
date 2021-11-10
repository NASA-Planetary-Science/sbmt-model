package edu.jhuapl.sbmt.model.phobos.model;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.Pair;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.pointing.InstrumentPointing;
import edu.jhuapl.sbmt.pointing.spice.SpicePointingProvider;

import crucible.core.mechanics.providers.lockable.LockableEphemerisLinkEvaluationException;
import crucible.core.mechanics.providers.lockable.LockableFrameLinkEvaluationException;
import crucible.core.time.TimeSystems;
import crucible.core.time.UTCEpoch;

public class MEGANEDataModel
{
	SpicePointingProvider pointingProvider;
	SmallBodyModel smallBodyModel;
	double currentStartTime, currentStopTime;
	Date startDate, stopDate;
    protected static final TimeSystems DefaultTimeSystems = TimeSystems.builder().build();

	public MEGANEDataModel(SmallBodyModel smallBodyModel, Date startDate, Date stopDate)
	{
		this.smallBodyModel = smallBodyModel;
		this.startDate = startDate;
		this.stopDate = stopDate;
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-D'T'HH:mm:ss.SSS");
		UTCEpoch startEpoch = UTCEpoch.fromString(dateFormatter.format(startDate));
		UTCEpoch endEpoch = UTCEpoch.fromString(dateFormatter.format(stopDate));
		this.currentStartTime = DefaultTimeSystems.getTDB().getTime( DefaultTimeSystems.getUTC().getTSEpoch(startEpoch));
		this.currentStopTime = DefaultTimeSystems.getTDB().getTime( DefaultTimeSystems.getUTC().getTSEpoch(endEpoch));

//		this.currentStartTime = TimeUtil.str2et(startEpoch.toString());
//		this.currentStopTime = TimeUtil.str2et(endEpoch.toString());
	}

	public Pair<Double, Double>[] getAltitudesForTimeWindowWithTimeStep(double startTime, double endTime, double timeStep)
	{
		double[] intercept = new double[3];
		double[] spacecraftPosition = new double[3];
		double[] boresightDirection = new double[3];

		int numEntries = (int)((endTime-startTime)/timeStep) + 1;
		Pair<Double, Double>[] values = new Pair[numEntries];
		for (int i=0; i< numEntries; i++)
		{
			double time = startTime + i*timeStep;
//			UTCEpoch utcTime = getUTC(utcTimeString);
//	        double time = DefaultTimeSystems.getTDB().getTime(DefaultTimeSystems.getUTC().getTSEpoch(utcTime));
//			System.out.println("MEGANEDataModel: getAltitudesForTimeWindowWithTimeStep: time is " + TimeUtil.et2str(time) + " and time " + time);

			InstrumentPointing pointing = pointingProvider.provide(time + timeStep/2);
			try {
				spacecraftPosition[0] = pointing.getScPosition().getI();
				spacecraftPosition[1] = pointing.getScPosition().getJ();
				spacecraftPosition[2] = pointing.getScPosition().getK();
				boresightDirection[0] = pointing.getBoresight().getI();
				boresightDirection[1] = pointing.getBoresight().getJ();
				boresightDirection[2] = pointing.getBoresight().getK();
//				System.out.println("MEGANEDataModel: getAltitudesForTimeWindowWithTimeStep: sc pos " + new Vector3D(spacecraftPosition));

				double distance = pointing.getScPosition().getLength();
//				System.out.println("MEGANEDataModel: getAltitudesForTimeWindowWithTimeStep: distance " + distance);
				smallBodyModel.computeRayIntersection(spacecraftPosition, boresightDirection, intercept);
				double altitude = distance - new Vector3D(intercept).getNorm();
//				System.out.println("MEGANEDataModel: getAltitudesForTimeWindowWithTimeStep: sub s/c radius " + new Vector3D(intercept).getNorm());
//				System.out.println("MEGANEDataModel: getAltitudesForTimeWindowWithTimeStep: altitude " + altitude);
				double normalizedAltitude = altitude/(new Vector3D(intercept).getNorm());
//				System.out.println("MEGANEDataModel: getAltitudesForTimeWindowWithTimeStep: normalzied alt " + normalizedAltitude);
				values[i] = new Pair<Double, Double>(time, normalizedAltitude);
			}
			catch (LockableEphemerisLinkEvaluationException | LockableFrameLinkEvaluationException ex)
			{
//				System.out.println("MEGANEDataModel: NO DATA getAltitudesForTimeWindowWithTimeStep: time is " + TimeUtil.et2str(time));
//
//				System.out.println("MEGANEDataModel: getAltitudesForTimeWindowWithTimeStep: distance " + values[i-1].getSecond());
				values[i] = new Pair<Double, Double>(time, values[i-1].getSecond());
			}
		}
		return values;
	}

	public HistogramDataset calculateAltitudeOverTimeInBinsDataset(Pair<Double, Double>[] timeAltitudePairs)
	{
		int numBins = 160;
		HistogramDataset dataset = new HistogramDataset();
		dataset.setType(HistogramType.RELATIVE_FREQUENCY);
		List<Double> altitudeList = Arrays.stream(timeAltitudePairs).map(item -> item.getSecond()).collect(Collectors.toList());
		Double[] altitudeValues = new Double[altitudeList.size()];
		altitudeList.toArray(altitudeValues);
		dataset.addSeries("altitude", ArrayUtils.toPrimitive(altitudeValues), numBins, 0.4, 2.0);

		return dataset;
	}

	/**
	 * @param pointingProvider the pointingProvider to set
	 */
	public void setPointingProvider(SpicePointingProvider pointingProvider)
	{
		this.pointingProvider = pointingProvider;
	}

	/**
	 * @return the startTime
	 */
	public Double getStartTime()
	{
		return currentStartTime;
	}

	/**
	 * @param startTime the startTime to set
	 */
	public void setStartTime(Double startTime)
	{
		this.currentStartTime = startTime;
	}

	/**
	 * @return the stopTime
	 */
	public Double getStopTime()
	{
		return currentStopTime;
	}

	/**
	 * @param stopTime the stopTime to set
	 */
	public void setStopTime(Double stopTime)
	{
		this.currentStopTime = stopTime;
	}

	public double getTDBForDate(Date date)
	{
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-D'T'HH:mm:ss.SSS");
		UTCEpoch epoch = UTCEpoch.fromString(dateFormatter.format(date));
		return DefaultTimeSystems.getTDB().getTime( DefaultTimeSystems.getUTC().getTSEpoch(epoch));
	}

}
