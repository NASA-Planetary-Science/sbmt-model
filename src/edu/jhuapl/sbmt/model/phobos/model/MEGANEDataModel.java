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

import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.pointing.InstrumentPointing;
import edu.jhuapl.sbmt.pointing.spice.SpicePointingProvider;
import picante.mechanics.providers.lockable.LockableEphemerisLinkEvaluationException;
import picante.mechanics.providers.lockable.LockableFrameLinkEvaluationException;
import picante.time.TimeSystems;
import picante.time.UTCEpoch;

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

			InstrumentPointing pointing = pointingProvider.provide(time + timeStep/2);
			try {
				spacecraftPosition[0] = pointing.getScPosition().getI();
				spacecraftPosition[1] = pointing.getScPosition().getJ();
				spacecraftPosition[2] = pointing.getScPosition().getK();
				boresightDirection[0] = pointing.getBoresight().getI();
				boresightDirection[1] = pointing.getBoresight().getJ();
				boresightDirection[2] = pointing.getBoresight().getK();

				double distance = pointing.getScPosition().getLength();
				smallBodyModel.computeRayIntersection(spacecraftPosition, boresightDirection, intercept);
				double altitude = distance - new Vector3D(intercept).getNorm();
				double normalizedAltitude = altitude/(new Vector3D(intercept).getNorm());
				values[i] = new Pair<Double, Double>(time, normalizedAltitude);
			}
			catch (LockableEphemerisLinkEvaluationException | LockableFrameLinkEvaluationException ex)
			{
				if (i - 1 == -1) values[i] = new Pair<Double, Double>(time, 0.0);
				else values[i] = new Pair<Double, Double>(time, values[i-1].getSecond());
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
