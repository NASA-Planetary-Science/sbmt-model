package edu.jhuapl.sbmt.model.phobos.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.math3.util.Pair;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import edu.jhuapl.sbmt.model.phobos.model.MEGANEDataModel;
import edu.jhuapl.sbmt.stateHistory.model.time.StateHistoryTimeModel;
import edu.jhuapl.sbmt.stateHistory.rendering.PlannedDataProperties;
import edu.jhuapl.sbmt.stateHistory.ui.DateTimeSpinner;

public class MEGANEPlotPanel extends JPanel implements PropertyChangeListener
{

	JFreeChart timeVersusAltitude;
	JFreeChart timeBelowAltitudeHistogram;
	private DateTimeSpinner startTimeSpinner, stopTimeSpinner;
    /**
     * Internal value for spinner size
     */
    private Dimension spinnerSize = new Dimension(400, 28);

	public MEGANEPlotPanel()
	{
		// TODO Auto-generated constructor stub
		initGUI();
	}

	public MEGANEPlotPanel(LayoutManager layout)
	{
		super(layout);
		// TODO Auto-generated constructor stub
	}

	private void initGUI()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel panel_2 = new JPanel();
	    panel_2.setBorder(null);
	    add(panel_2);
	    panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));

	    JLabel lblNewLabel_2 = new JLabel("Start Time:");
	    panel_2.add(lblNewLabel_2);

	    Component horizontalGlue_1 = Box.createHorizontalGlue();
	    panel_2.add(horizontalGlue_1);

		startTimeSpinner = new DateTimeSpinner();
        startTimeSpinner.setMinimumSize(spinnerSize);
        startTimeSpinner.setMaximumSize(spinnerSize);
        startTimeSpinner.setPreferredSize(spinnerSize);
        panel_2.add(startTimeSpinner);

        JPanel panel_3 = new JPanel();
        panel_3.setBorder(null);
        add(panel_3);
        panel_3.setLayout(new BoxLayout(panel_3, BoxLayout.X_AXIS));

        JLabel lblNewLabel_3 = new JLabel("Stop Time:");
        panel_3.add(lblNewLabel_3);

        Component horizontalGlue_2 = Box.createHorizontalGlue();
        panel_3.add(horizontalGlue_2);

        stopTimeSpinner = new DateTimeSpinner();
        stopTimeSpinner.setMinimumSize(spinnerSize);
        stopTimeSpinner.setMaximumSize(spinnerSize);
        stopTimeSpinner.setPreferredSize(spinnerSize);

        panel_3.add(stopTimeSpinner);
	}

	public void createPlotsFromModel(MEGANEDataModel model)
	{
		Double startTime = model.getStartTime();
		Double stopTime = model.getStopTime();
		Pair<Double, Double>[] timeAltitudePairs = model.getAltitudesForTimeWindowWithTimeStep(startTime, stopTime, 60);
		timeBelowAltitudeHistogram = ChartFactory.createHistogram("Time Below Altitude", "Altitude (Body radii)", "Fractional time @ Altitude",
																	model.calculateAltitudeOverTimeInBinsDataset(timeAltitudePairs),
																	PlotOrientation.VERTICAL,
																	false,
																	false, false);

		TimeSeries series = new TimeSeries("TimeVsAltitude");
		Calendar cal = Calendar.getInstance();
		for (Pair<Double, Double> pair : timeAltitudePairs)
		{
			Date dateForET = StateHistoryTimeModel.getDateForET(pair.getFirst());
			cal.setTime(dateForET);
            series.add(new FixedMillisecond(dateForET), pair.getSecond());
		}
		TimeSeriesCollection dataset = new TimeSeriesCollection(series);
		timeVersusAltitude = ChartFactory.createTimeSeriesChart("Time vs Altitude", "Time (sec)", "Altitude (m)", dataset, /*PlotOrientation.VERTICAL,*/ false, false, false);

		ChartPanel chart1 = new ChartPanel(timeVersusAltitude);
		chart1.setBorder(BorderFactory.createTitledBorder("Time vs Altitude"));
		((DateAxis)timeVersusAltitude.getXYPlot().getDomainAxis()).setDateFormatOverride(new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss"));
		add(chart1);
		ChartPanel chart2 = new ChartPanel(timeBelowAltitudeHistogram);
		chart2.setBorder(BorderFactory.createTitledBorder("Time Below Altitude"));
		add(chart2);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals(PlannedDataProperties.TIME_CHANGED))
		{
			ValueMarker currentTime = new ValueMarker((Double)(evt.getNewValue()));
			currentTime.setPaint(Color.black);

			timeVersusAltitude.getXYPlot().addDomainMarker(currentTime);
		}

	}

	/**
	 * @return the startTimeSpinner
	 */
	public DateTimeSpinner getStartTimeSpinner()
	{
		return startTimeSpinner;
	}

	/**
	 * @return the stopTimeSpinner
	 */
	public DateTimeSpinner getStopTimeSpinner()
	{
		return stopTimeSpinner;
	}

	/**
	 * @return the timeVersusAltitude
	 */
	public JFreeChart getTimeVersusAltitude()
	{
		return timeVersusAltitude;
	}

	/**
	 * @return the timeBelowAltitudeHistogram
	 */
	public JFreeChart getTimeBelowAltitudeHistogram()
	{
		return timeBelowAltitudeHistogram;
	}





}
