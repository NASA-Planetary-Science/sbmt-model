package edu.jhuapl.sbmt.model.phobos.controllers;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.axis.ValueAxis;

import com.github.davidmoten.guavamini.Preconditions;

import vtk.vtkFloatArray;

import edu.jhuapl.saavtk.model.plateColoring.ColoringData;
import edu.jhuapl.saavtk.model.plateColoring.ColoringDataFactory;
import edu.jhuapl.saavtk.model.plateColoring.ColoringDataUtils;
import edu.jhuapl.saavtk.model.plateColoring.CustomizableColoringDataManager;
import edu.jhuapl.saavtk.model.plateColoring.FacetColoringData;
import edu.jhuapl.saavtk.model.plateColoring.LoadableColoringData;
import edu.jhuapl.saavtk.util.file.IndexableTuple;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.perspectiveImage.PerspectiveImageFootprint;
import edu.jhuapl.sbmt.model.phobos.model.MEGANEDataModel;
import edu.jhuapl.sbmt.model.phobos.ui.MEGANEPlotPanel;
import edu.jhuapl.sbmt.pointing.spice.SpicePointingProvider;
import edu.jhuapl.sbmt.stateHistory.model.interfaces.ICalculatedPlateValues;
import edu.jhuapl.sbmt.stateHistory.model.interfaces.IFootprintConfinedPlateValues;
import edu.jhuapl.sbmt.stateHistory.model.interfaces.ITimeCalculatedPlateValues;
import edu.jhuapl.sbmt.stateHistory.model.liveColoring.LiveColorableManager;
import edu.jhuapl.sbmt.stateHistory.model.time.StateHistoryTimeModelChangedListener;
import edu.jhuapl.sbmt.stateHistory.model.time.TimeWindow;

public class MEGANEController implements PropertyChangeListener
{
	private MEGANEPlotPanel plotPanel;
	private MEGANEDataModel model;
	private CustomizableColoringDataManager coloringDataManager;
	private SmallBodyModel smallBodyModel;

	public MEGANEController(SmallBodyModel smallBodyModel)
	{
		this.smallBodyModel = smallBodyModel;
		this.coloringDataManager = (CustomizableColoringDataManager)smallBodyModel.getColoringDataManager();
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss.SSS");
		Date startDate = null;
		Date stopDate = null;
		try
		{
			startDate = dateFormatter.parse("2026-Apr-01 00:00:00.000");
			stopDate = dateFormatter.parse("2026-May-30 23:59:00.000");
		}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		model = new MEGANEDataModel(smallBodyModel, startDate, stopDate);
		plotPanel = new MEGANEPlotPanel();

		SpinnerDateModel spinnerDateModel = new SpinnerDateModel(startDate, null, null, Calendar.DAY_OF_MONTH);
        plotPanel.getStartTimeSpinner().setModel(spinnerDateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(plotPanel.getStartTimeSpinner(), "yyyy-MMM-dd HH:mm:ss.SSS");
        plotPanel.getStartTimeSpinner().setEditor(dateEditor);
        plotPanel.getStartTimeSpinner().addChangeListener(new ChangeListener()
		{

			@Override
			public void stateChanged(ChangeEvent e)
			{
				ValueAxis timeVersusAltitudeXAxis = plotPanel.getTimeVersusAltitude().getXYPlot().getDomainAxis();
				plotPanel.getTimeVersusAltitude().getXYPlot().getDomainAxis().setRange(model.getTDBForDate((Date)plotPanel.getStartTimeSpinner().getModel().getValue()), timeVersusAltitudeXAxis.getUpperBound());

//				ValueAxis altitudeHistogramXAxis = plotPanel.getTimeVersusAltitude().getXYPlot().getDomainAxis();
//				plotPanel.getTimeVersusAltitude().getXYPlot().getDomainAxis().setRange(model.getTDBForDate((Date)plotPanel.getStartTimeSpinner().getModel().getValue()), timeVersusAltitudeXAxis.getUpperBound());


			}


		});


        spinnerDateModel = new SpinnerDateModel(stopDate, null, null, Calendar.DAY_OF_MONTH);
        plotPanel.getStopTimeSpinner().setModel(spinnerDateModel);
		dateEditor = new JSpinner.DateEditor(plotPanel.getStopTimeSpinner(), "yyyy-MMM-dd HH:mm:ss.SSS");
        plotPanel.getStopTimeSpinner().setEditor(dateEditor);

        plotPanel.getStopTimeSpinner().addChangeListener(new ChangeListener()
		{

			@Override
			public void stateChanged(ChangeEvent e)
			{
				ValueAxis timeVersusAltitudeXAxis = plotPanel.getTimeVersusAltitude().getXYPlot().getDomainAxis();
				plotPanel.getTimeVersusAltitude().getXYPlot().getDomainAxis().setRange(timeVersusAltitudeXAxis.getLowerBound(), model.getTDBForDate((Date)plotPanel.getStopTimeSpinner().getModel().getValue()));
			}
		});

        initializeCalculatedPlateColorings();

	}

	private void initializeCalculatedPlateColorings()
	{
		LiveColorableManager.addICalculatedPlateValues("Time Per Facet", new IFootprintConfinedPlateValues()
		{
			private FacetColoringData[] facetColoring;
			private PerspectiveImageFootprint footprint;
			private vtkFloatArray values = new vtkFloatArray();

			@Override
			public vtkFloatArray getValues()
			{
				return values;
			}

			@Override
			public vtkFloatArray getPlateValuesForTime(double time)
			{
				Preconditions.checkArgument(values.GetNumberOfComponents() != 0);
				Preconditions.checkArgument(values.GetNumberOfTuples() != 0);
//				System.out.println(
//						"MEGANEController.initializeCalculatedPlateColorings().new IFootprintConfinedPlateValues() {...}: getPlateValuesForTime: facet coloring " + facetColoring);
				if (facetColoring == null) return values;
				if (footprint.isVisible() == false) return values;
				for (FacetColoringData coloringData : footprint.getFacetColoringDataForFootprint())
				{
					int index = coloringData.getCellId();
//					System.out.println(
//							"MEGANEController.initializeCalculatedPlateColorings().new IFootprintConfinedPlateValues() {...}: getPlateValuesForTime: index " + index);
					double valueToCalculate = values.GetValue(index) + 1;
//					System.out.println(
//							"MEGANEController.initializeCalculatedPlateColorings().new IFootprintConfinedPlateValues() {...}: getPlateValuesForTime: indx " + index + " value to calc " + valueToCalculate);
					values.SetValue(index, valueToCalculate);
				}
				return values;
			}

			@Override
			public void setFacetColoringDataForFootprint(PerspectiveImageFootprint footprint)
			{
				this.footprint = footprint;
//				System.out.println(
//						"MEGANEController.initializeCalculatedPlateColorings().new IFootprintConfinedPlateValues() {...}: setFacetColoringDataForFootprint: updating footprint " + footprint);
				facetColoring = footprint.getFacetColoringDataForFootprint();
			}

			@Override
			public void setNumberOfDimensions(int numDimensions)
			{
				values.SetNumberOfComponents(numDimensions);
			}

			@Override
			public void setNumberOfValues(int numValues)
			{
				values.SetNumberOfTuples(numValues);
			}
		});
		ICalculatedPlateValues timePerIndexCalculator = LiveColorableManager.getCalculatedPlateValuesFor("Time Per Facet");
		int numberElements = smallBodyModel.getCellNormals().GetNumberOfTuples();
		timePerIndexCalculator.setNumberOfValues(numberElements);
		timePerIndexCalculator.setNumberOfDimensions(1);
		timePerIndexCalculator.getValues().FillComponent(0, 0);
		vtkFloatArray timePerFacetArray = timePerIndexCalculator.getValues();
		IndexableTuple indexableTuple = ColoringDataUtils.createIndexableFromVtkArray(timePerFacetArray);
//		System.out.println("MEGANEController: initializeCalculatedPlateColorings: adding time per facet coloring, num entries " + indexableTuple.size());
		ColoringData coloringData = ColoringDataFactory.of("Time Per Facet", "sec", timePerFacetArray.GetNumberOfTuples(), Arrays.asList("Time"), false, indexableTuple);
		LoadableColoringData loadableColoringData = ColoringDataFactory.of(coloringData, "MEGANE-TimePerFacet");
//		System.out.println("MEGANEController: initializeCalculatedPlateColorings: loadable coloring data " + loadableColoringData);
		try
		{
			loadableColoringData.save();
		}
		catch (IOException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		coloringDataManager.addCustom(loadableColoringData);

		LiveColorableManager.timeModel.addTimeModelChangeListener(new StateHistoryTimeModelChangedListener()
		{

			@Override
			public void timeWindowChanged(TimeWindow twindow)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void timeFractionChanged(double fraction)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void timeChanged(double et)
			{
//				Logger.getAnonymousLogger().log(Level.INFO, "Starting in MEGANE");
				vtkFloatArray valuesAtTime = ((ITimeCalculatedPlateValues)timePerIndexCalculator).getPlateValuesForTime(et);
//				Logger.getAnonymousLogger().log(Level.INFO, "Making INdexabletuple");
				IndexableTuple indexableTuple = ColoringDataUtils.createIndexableFromVtkArray(valuesAtTime);
//				Logger.getAnonymousLogger().log(Level.INFO, "Making coloring data");
				ColoringData coloringData = ColoringDataFactory.of("Time Per Facet", "sec", valuesAtTime.GetNumberOfTuples(), Arrays.asList("Time"), false, indexableTuple);
//				Logger.getAnonymousLogger().log(Level.INFO, "Making Loadable");
				try
				{
					LoadableColoringData loadableColoringData = ColoringDataFactory.of(coloringData, "MEGANE-TimePerFacet");

					loadableColoringData.save();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				coloringDataManager.replaceCustom("Time Per Facet", coloringData);
//				Logger.getAnonymousLogger().log(Level.INFO, "Ending in MEGANE");
			}

			@Override
			public void fractionDisplayedChanged(double minFractionDisplayed, double maxFractionDisplayed)
			{
				// TODO Auto-generated method stub

			}
		});
	}



	public JPanel getPanel()
	{
		JPanel panel = new JPanel();
		panel.add(plotPanel);

		return panel;

	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals("SPICEPROVIDER"))
		{
			Runnable runner = new Runnable()
			{

				@Override
				public void run()
				{
					model.setPointingProvider((SpicePointingProvider)(evt.getNewValue()));
					plotPanel.createPlotsFromModel(model);
				}
			};
			Thread thread = new Thread(runner);
			thread.start();
		}

	}

}
