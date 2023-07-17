package edu.jhuapl.sbmt.model.phobos.controllers;

import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jfree.chart.axis.ValueAxis;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import vtk.vtkFloatArray;

import edu.jhuapl.saavtk.gui.dialog.CustomFileChooser;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.model.ModelNames;
import edu.jhuapl.saavtk.model.plateColoring.ColoringData;
import edu.jhuapl.saavtk.model.plateColoring.ColoringDataFactory;
import edu.jhuapl.saavtk.model.plateColoring.ColoringDataUtils;
import edu.jhuapl.saavtk.model.plateColoring.CustomizableColoringDataManager;
import edu.jhuapl.saavtk.model.plateColoring.FacetColoringData;
import edu.jhuapl.saavtk.model.plateColoring.LoadableColoringData;
import edu.jhuapl.saavtk.model.structure.PolygonModel;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.saavtk.pick.PickManager.PickMode;
import edu.jhuapl.saavtk.structure.Structure;
import edu.jhuapl.saavtk.structure.StructureManager;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.saavtk.util.file.IndexableTuple;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.image.model.PerspectiveFootprint;
import edu.jhuapl.sbmt.model.phobos.model.CumulativeMEGANECollection;
import edu.jhuapl.sbmt.model.phobos.model.CumulativeMEGANEFootprint;
import edu.jhuapl.sbmt.model.phobos.model.MEGANECollection;
import edu.jhuapl.sbmt.model.phobos.model.MEGANEDataModel;
import edu.jhuapl.sbmt.model.phobos.model.MEGANEFootprint;
import edu.jhuapl.sbmt.model.phobos.model.MEGANESearchModel;
import edu.jhuapl.sbmt.model.phobos.ui.MEGANEPlotPanel;
import edu.jhuapl.sbmt.model.phobos.ui.MEGANESearchPanel;
import edu.jhuapl.sbmt.pointing.spice.SpicePointingProvider;
import edu.jhuapl.sbmt.query.filter.model.DynamicFilterValues;
import edu.jhuapl.sbmt.query.filter.model.FilterType;
import edu.jhuapl.sbmt.query.filter.model.FilterTypeUnit;
import edu.jhuapl.sbmt.stateHistory.model.interfaces.ICalculatedPlateValues;
import edu.jhuapl.sbmt.stateHistory.model.interfaces.IFootprintConfinedPlateValues;
import edu.jhuapl.sbmt.stateHistory.model.interfaces.ITimeCalculatedPlateValues;
import edu.jhuapl.sbmt.stateHistory.model.liveColoring.LiveColorableManager;
import edu.jhuapl.sbmt.stateHistory.model.time.StateHistoryTimeModelChangedListener;
import edu.jhuapl.sbmt.stateHistory.model.time.TimeWindow;

import glum.item.ItemEventType;

public class MEGANEController implements PropertyChangeListener
{
	private MEGANEPlotPanel plotPanel;
	private MEGANESearchPanel searchPanel;
	private MEGANEDataModel model;
	private CustomizableColoringDataManager coloringDataManager;
	private SmallBodyModel smallBodyModel;
	private JTabbedPane tabbedPane;
	private MEGANECollection collection;
	private CumulativeMEGANECollection cumulativeCollection;
	private MEGANEDatabaseConnection dbConnection;
	private PickManager pickManager;
	private MEGANESearchModel searchModel;
	private MEGANEFootprintColoringOptionsController coloringController;

	public MEGANEController(MEGANECollection collection, CumulativeMEGANECollection cumulativeCollection, SmallBodyModel smallBodyModel, ModelManager modelManager, PickManager pickManager)
	{
		this.smallBodyModel = smallBodyModel;
		this.collection = collection;
		this.cumulativeCollection = cumulativeCollection;
		this.coloringDataManager = (CustomizableColoringDataManager)smallBodyModel.getColoringDataManager();
		this.pickManager = pickManager;
		this.searchModel = new MEGANESearchModel(modelManager, smallBodyModel, dbConnection);
		coloringController = new MEGANEFootprintColoringOptionsController(collection, cumulativeCollection);

		DynamicFilterValues<Structure> dynamicValues = new DynamicFilterValues<Structure>()
		{
			@Override
			public ArrayList<Structure> getCurrentValues()
			{
				PolygonModel polygonModel = (PolygonModel)modelManager.getModel(ModelNames.POLYGON_STRUCTURES);
				var circleModel = (StructureManager<?>)modelManager.getModel(ModelNames.CIRCLE_STRUCTURES);
				var ellipseModel = (StructureManager<?>)modelManager.getModel(ModelNames.ELLIPSE_STRUCTURES);

				ArrayList<Structure> structures = new ArrayList<Structure>();
				structures.addAll(polygonModel.getAllItems());
				structures.addAll(circleModel.getAllItems());
				structures.addAll(ellipseModel.getAllItems());
				Structure[] structuresArray = new Structure[structures.size()];
				structures.toArray(structuresArray);
				return structures;
			}
		};

		//facetObs filters

		FilterType.create("Incidence Angle", Optional.of(FilterTypeUnit.DEGREES), Double.class, Pair.of(0.0, 180.0), "incidence", true);
		FilterType.create("Emission Angle",  Optional.of(FilterTypeUnit.DEGREES), Double.class, Pair.of(0.0, 180.0), "emission", true);
		FilterType.create("SC Distance",  Optional.of(FilterTypeUnit.KM), Double.class, Pair.of(0.0, 1000.0), "range", true);
//		FilterType.create("SC Altitude",  Optional.of(FilterTypeUnit.KM), Double.class, Pair.of(0.0, 1000.0), "altitude");
//		FilterType.create("Limb", String.class, new String[] {"with only", "without only", "with or without"}, "limbType");
		FilterType.create("Image Pointing", String.class, new String[] {"SPC Derived", "SPICE Derivied"}, "Pointing", true);
		LocalDateTime startTime = LocalDateTime.of(2026, 6, 20, 0, 0);
		LocalDateTime endTime = LocalDateTime.of(2026, 6, 21, 0, 0);
		FilterType.create("Time Window", Optional.of(FilterTypeUnit.provide("Time Window")), LocalDateTime.class, Pair.of(startTime, endTime), "tdb", true);
		FilterType.create("Integration Time", /*Optional.of(FilterTypeUnit.SECONDS),*/ String.class, new String[] {"60.0", "180.0"}, "integrationTime", true);
		FilterType.create("cosE", Optional.of(FilterTypeUnit.NONE), Double.class, Pair.of(-1.0, 1.0), "cosE", true);
		FilterType.create("Projected Area", Optional.of(FilterTypeUnit.NONE), Double.class, Pair.of(0.0, 1.0), "lat", true);

		//observing geometry filters
		FilterType.create("Latitude", Optional.of(FilterTypeUnit.DEGREES), Double.class, Pair.of(-90.0, 90.0), "lat", true);
		FilterType.create("Longitude", Optional.of(FilterTypeUnit.DEGREES), Double.class, Pair.of(-180.0, 180.0), "lon", true);
		FilterType.create("Altitude", Optional.of(FilterTypeUnit.KM), Double.class, Pair.of(0.0, 10000.0), "alt", true);
		FilterType.create("Normalized Alt", Optional.of(FilterTypeUnit.NONE), Double.class, Pair.of(0.0, 1.0), "lat", true);

		//other filters
		FilterType.provideDynamic("Structures", Structure.class, dynamicValues, "Structure", false);
		FilterType.create("Signal Contribution", Optional.of(FilterTypeUnit.NONE), Double.class, Pair.of(40.0, 60.0), "signal", true);

		FilterType<String> integrationTimeFilter;
		FilterType<LocalDateTime> initialTimeWindow;
		FilterType<Double> signalContribution;
		try
		{
			integrationTimeFilter = FilterType.createInstance("Integration Time");
			integrationTimeFilter.setSelectedRangeValue("60.0");
			integrationTimeFilter.setEnabled(true);
			searchModel.getNonNumericFilterModel().addFilter(integrationTimeFilter);

			initialTimeWindow = FilterType.createInstance("Time Window");
			initialTimeWindow.setEnabled(false);
			searchModel.getTimeWindowModel().addFilter(initialTimeWindow);

			signalContribution = FilterType.createInstance("Signal Contribution");
			signalContribution.setRangeMin(50.0);
			signalContribution.setRangeMax(100.0);
			signalContribution.setEnabled(false);
			searchModel.getNumericFilterModel().addFilter(signalContribution);

		}
		catch (CloneNotSupportedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		collection.addListener((aSource, aEventType) -> {

			if (aEventType == ItemEventType.ItemsSelected)
			{
				updateButtonState();
				searchPanel.setEnabled(collection.getAllItems().size() > 0);
			}
			else if (aEventType == ItemEventType.ItemsMutated)
			{
				boolean oneMapped = false;
				for (MEGANEFootprint footprint : collection.getAllItems())
				{
					if (footprint.isMapped() == true)
					{
						oneMapped = true;
						break;
					}
				}
				coloringController.showColorBar(oneMapped);
			}
		});

		cumulativeCollection.addListener((aSource, aEventType) -> {

			if (aEventType == ItemEventType.ItemsMutated)
			{
				boolean oneMapped = false;
				for (CumulativeMEGANEFootprint footprint : cumulativeCollection.getAllItems())
				{
					if (footprint.isMapped() == true)
					{
						oneMapped = true;
						break;
					}
				}
				coloringController.showColorBar(oneMapped);
			}
		});

		collection.addPropertyChangeListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(Properties.MODEL_CHANGED))
					updateButtonState();
			}
		});

		setupPanel();
		initializeCalculatedPlateColorings();
		searchPanel.addAncestorListener(new AncestorListener()
		{

			@Override
			public void ancestorRemoved(AncestorEvent event)
			{
				searchModel.getNonNumericFilterModel().setSelectedItems(List.of());
				searchModel.getNumericFilterModel().setSelectedItems(List.of());
				searchModel.getTimeWindowModel().setSelectedItems(List.of());
			}

			@Override
			public void ancestorMoved(AncestorEvent event)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void ancestorAdded(AncestorEvent event)
			{
				// TODO Auto-generated method stub

			}
		});
	}

	private void setupPanel()
	{
		setupSearchPanel();
		setupPlotPanel();
		tabbedPane = new JTabbedPane();
		tabbedPane.add("Search", searchPanel);
		tabbedPane.add("Plots", plotPanel);

	}

	private void setupSearchPanel()
	{
		searchPanel = new MEGANESearchPanel(collection, cumulativeCollection, searchModel);	//TODO fix the use of the model here
		searchPanel.setEnabled(false);
		searchPanel.getDatabaseLoadButton().addActionListener(e -> {
			File file = CustomFileChooser.showOpenDialog(searchPanel, "Choose Database");
			this.dbConnection = new MEGANEDatabaseConnection(this, file.getAbsolutePath());
			searchPanel.setEnabled(true);

			searchModel.setDbConnection(dbConnection);
			collection.setDbConnection(dbConnection);
			this.dbConnection.openDatabase();
			try
			{
				List<MEGANEFootprint> footprints = this.dbConnection.getFootprints();
				collection.setFootprints(footprints);
				searchPanel.getTableView().getResultsLabel().setText(footprints.size() + " Results");
			}
			catch (SQLException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			searchPanel.getDatabaseNameLabel().setText(file.getName());
			searchPanel.getDatabaseNameLabel().setForeground(Color.black);
		});

		searchPanel.getTableView().getLoadSpectrumButton().addActionListener(e -> {
			File fileToOpen = CustomFileChooser.showOpenDialog(plotPanel, "Load CSV file...");
			if (fileToOpen == null) return;
			try
			{
				BufferedReader reader = new BufferedReader(new FileReader(fileToOpen));
				Stream<String> lines = reader.lines();
				List<MEGANEFootprint> fprints = Lists.newArrayList();
				for (String line : lines.toList())
				{
					if (line.startsWith("#")) continue;
					MEGANEFootprint footprint = new MEGANEFootprint(line);
					fprints.add(footprint);
				}
				collection.setFootprints(fprints);
				reader.close();
			}
			catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});

		searchPanel.getTableView().getSaveSpectrumButton().addActionListener(e -> {

			File filename = CustomFileChooser.showSaveDialog(plotPanel, "Save to...");
			if (FilenameUtils.getExtension(filename.getAbsolutePath()).equals("")) filename = new File(filename + ".csv");
			try
			{
				HashMap<String, List<String>> metadata = searchModel.getMetadata();
				collection.saveSelectedToFile(filename, metadata);
			}
			catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});

		searchPanel.getTableView().getAddSpectrumButton().addActionListener(e -> {

			ImmutableSet<MEGANEFootprint> selectedFootprints = collection.getSelectedItems();
			if (selectedFootprints.size() == 0) return;
			CumulativeMEGANEFootprint cumulativeFootprint = new CumulativeMEGANEFootprint(selectedFootprints.asList());
			cumulativeCollection.addCumulativeFootprint(cumulativeFootprint);
			updateButtonState();
		});

		searchPanel.getUpdateColorsButton().addActionListener(e -> {

			ImmutableSet<MEGANEFootprint> selectedFootprints = collection.getSelectedItems();
			if (selectedFootprints.size() == 0) return;
			JFrame frame = new JFrame("Adjust Color");
			frame.getContentPane().add(coloringController.getView());
			frame.setSize(new Dimension(300, 400));
			frame.setVisible(true);
		});

		searchPanel.getTableView().getShowSpectrumButton().addActionListener(e -> {
			ImmutableSet<MEGANEFootprint> selectedFootprints = collection.getSelectedItems();
			if (selectedFootprints.size() == 0) return;
			for (MEGANEFootprint fp : selectedFootprints)
			{
				if (fp.getCellIDs().isEmpty())
				{
					try
					{
						fp.setFacets(dbConnection.getFacets(fp.getDateTime(), new Function<String, Void>()
						{

							@Override
							public Void apply(String t)
							{
								SwingUtilities.invokeLater(() -> {fp.setStatus(t);});
								return null;
							}
						}));
					}
					catch (SQLException e1)
					{
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				collection.setFootprintMapped(fp, true);
			}
			updateButtonState();
		});

		searchPanel.getTableView().getHideSpectrumButton().addActionListener(e -> {
			ImmutableSet<MEGANEFootprint> selectedFootprints = collection.getSelectedItems();
			if (selectedFootprints.size() == 0) return;
			for (MEGANEFootprint fp : selectedFootprints)
			{
				collection.setFootprintMapped(fp, false);
			}
			updateButtonState();
		});

		searchPanel.getFilterPanel().getSelectRegionButton().addActionListener(e -> {

			if (searchPanel.getFilterPanel().getSelectRegionButton().isSelected())
	            pickManager.setPickMode(PickMode.CIRCLE_SELECTION);
	        else
	            pickManager.setPickMode(PickMode.DEFAULT);
		});

		searchPanel.getFilterPanel().getClearRegionButton().addActionListener(e -> {

	        searchModel.removeRegion();
		});

		searchPanel.getFilterPanel().getSubmitButton().addActionListener(e -> {

			Thread thread = new Thread(new Runnable()
			{

				@Override
				public void run()
				{
					try
					{
						collection.setFootprints(searchModel.performSearch(new Function<String, Void>()
						{

							@Override
							public Void apply(String t)
							{
								SwingUtilities.invokeLater(() -> {searchPanel.setStatus(t);});
								return null;
							}
						}));
						searchPanel.getTableView().getResultsLabel().setText(collection.getNumItems() + " Results");
					}
					catch (SQLException e1)
					{
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

				}
			});
			thread.start();

		});

		searchPanel.getFilterPanel().getFilterTables().getAddButton().addActionListener(e -> {

			try {
				FilterType selectedItem = (FilterType)searchPanel.getFilterPanel().getFilterTables().getFilterCombo().getSelectedItem();
				FilterType filter = FilterType.createInstance(selectedItem.name());
				if (filter.getType() == Double.class )
					searchModel.getNumericFilterModel().addFilter(filter);	//need to put Integration time (which is a choice) into nonnumeric
				else if (filter.getType() == LocalDateTime.class)
					searchModel.getTimeWindowModel().addFilter(filter);
				else
					searchModel.getNonNumericFilterModel().addFilter(filter);
			}
			catch (CloneNotSupportedException cnse)
			{
				cnse.printStackTrace();
			}

		});

		searchPanel.getFilterPanel().getFilterTables().getRemoveFilterButton().addActionListener(e -> {

			searchModel.getNumericFilterModel().removeItems(searchModel.getNumericFilterModel().getSelectedItems());
			searchModel.getNonNumericFilterModel().removeItems(searchModel.getNonNumericFilterModel().getSelectedItems());
			searchModel.getTimeWindowModel().removeItems(searchModel.getTimeWindowModel().getSelectedItems());

		});
	}

	private void setupPlotPanel()
	{

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
	}

	private void updateButtonState()
	{
		ImmutableSet<MEGANEFootprint> selectedItems = collection.getSelectedItems();
		searchPanel.getTableView().getSaveSpectrumButton().setEnabled(selectedItems.size() >= 1);
		boolean allMapped = true;
		for (MEGANEFootprint footprint : selectedItems)
		{
			if (footprint.isMapped() == false) allMapped = false;
		}
		searchPanel.getTableView().getHideSpectrumButton().setEnabled((selectedItems.size() > 0) && allMapped);
		searchPanel.getTableView().getShowSpectrumButton().setEnabled((selectedItems.size() > 0) && !allMapped);
		searchPanel.getTableView().getAddSpectrumButton().setEnabled(selectedItems.size() > 0);
		searchPanel.getUpdateColorsButton().setEnabled(((selectedItems.size() > 0) && allMapped) || (cumulativeCollection.getAllItems().size() > 0));
	}

	private void initializeCalculatedPlateColorings()
	{
		LiveColorableManager.addICalculatedPlateValues("Time Per Facet (on the fly)", new IFootprintConfinedPlateValues()
		{
			private FacetColoringData[] facetColoring;
			private PerspectiveFootprint footprint;
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
				if (facetColoring == null) return values;
				if (footprint.isVisible() == false) return values;
				for (FacetColoringData coloringData : footprint.getFacetColoringDataForFootprint())
				{
					long index = coloringData.getCellId();
					double valueToCalculate = values.GetValue(index) + 1;
					values.SetValue(index, (float)valueToCalculate);
				}
				return values;
			}

			@Override
			public void setFacetColoringDataForFootprint(PerspectiveFootprint footprint)
			{
				this.footprint = footprint;
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
		ICalculatedPlateValues timePerIndexCalculator = LiveColorableManager.getCalculatedPlateValuesFor("Time Per Facet (on the fly)");
		int numberElements = (int)smallBodyModel.getCellNormals().GetNumberOfTuples();
		timePerIndexCalculator.setNumberOfValues(numberElements);
		timePerIndexCalculator.setNumberOfDimensions(1);
		timePerIndexCalculator.getValues().FillComponent(0, 0);
		vtkFloatArray timePerFacetArray = timePerIndexCalculator.getValues();
		IndexableTuple indexableTuple = ColoringDataUtils.createIndexableFromVtkArray(timePerFacetArray);
		ColoringData coloringData = ColoringDataFactory.of("Time Per Facet (on the fly)", "sec", (int)timePerFacetArray.GetNumberOfTuples(), Arrays.asList("Time"), false, indexableTuple);
		LoadableColoringData loadableColoringData = ColoringDataFactory.of(coloringData, "MEGANE-TimePerFacet");
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
				vtkFloatArray valuesAtTime = ((ITimeCalculatedPlateValues)timePerIndexCalculator).getPlateValuesForTime(et);
				IndexableTuple indexableTuple = ColoringDataUtils.createIndexableFromVtkArray(valuesAtTime);
				ColoringData coloringData = ColoringDataFactory.of("Time Per Facet (on the fly)", "sec", (int)valuesAtTime.GetNumberOfTuples(), Arrays.asList("Time"), false, indexableTuple);
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
				coloringDataManager.replaceCustom("Time Per Facet (on the fly)", coloringData);
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
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(tabbedPane);

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

	/**
	 * @return the searchModel
	 */
	public MEGANESearchModel getSearchModel()
	{
		return searchModel;
	}
}
