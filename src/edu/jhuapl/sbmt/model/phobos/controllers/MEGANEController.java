package edu.jhuapl.sbmt.model.phobos.controllers;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerDateModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.axis.ValueAxis;

import com.beust.jcommander.internal.Lists;
import com.github.davidmoten.guavamini.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import vtk.vtkFloatArray;
import vtk.vtkPolyData;

import edu.jhuapl.saavtk.gui.dialog.CustomFileChooser;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.model.ModelNames;
import edu.jhuapl.saavtk.model.plateColoring.ColoringData;
import edu.jhuapl.saavtk.model.plateColoring.ColoringDataFactory;
import edu.jhuapl.saavtk.model.plateColoring.ColoringDataUtils;
import edu.jhuapl.saavtk.model.plateColoring.CustomizableColoringDataManager;
import edu.jhuapl.saavtk.model.plateColoring.FacetColoringData;
import edu.jhuapl.saavtk.model.plateColoring.LoadableColoringData;
import edu.jhuapl.saavtk.model.structure.AbstractEllipsePolygonModel;
import edu.jhuapl.saavtk.model.structure.CircleModel;
import edu.jhuapl.saavtk.model.structure.EllipseModel;
import edu.jhuapl.saavtk.model.structure.PlateUtil;
import edu.jhuapl.saavtk.model.structure.PolygonModel;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.saavtk.pick.PickManager.PickMode;
import edu.jhuapl.saavtk.structure.Ellipse;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.saavtk.util.file.IndexableTuple;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.perspectiveImage.PerspectiveImageFootprint;
import edu.jhuapl.sbmt.model.phobos.model.MEGANECollection;
import edu.jhuapl.sbmt.model.phobos.model.MEGANEDataModel;
import edu.jhuapl.sbmt.model.phobos.model.MEGANEFootprint;
import edu.jhuapl.sbmt.model.phobos.model.MEGANEFootprintFacet;
import edu.jhuapl.sbmt.model.phobos.ui.MEGANEPlotPanel;
import edu.jhuapl.sbmt.model.phobos.ui.MEGANESearchPanel;
import edu.jhuapl.sbmt.model.phobos.ui.structureSearch.MEGANEStructureCollection;
import edu.jhuapl.sbmt.pointing.spice.SpicePointingProvider;
import edu.jhuapl.sbmt.stateHistory.model.interfaces.ICalculatedPlateValues;
import edu.jhuapl.sbmt.stateHistory.model.interfaces.IFootprintConfinedPlateValues;
import edu.jhuapl.sbmt.stateHistory.model.interfaces.ITimeCalculatedPlateValues;
import edu.jhuapl.sbmt.stateHistory.model.liveColoring.LiveColorableManager;
import edu.jhuapl.sbmt.stateHistory.model.time.StateHistoryTimeModelChangedListener;
import edu.jhuapl.sbmt.stateHistory.model.time.TimeWindow;

import crucible.crust.logging.SimpleLogger;
import glum.item.ItemEventType;
import glum.task.SilentTask;
import glum.task.Task;

public class MEGANEController implements PropertyChangeListener
{
	private MEGANEPlotPanel plotPanel;
	private MEGANESearchPanel searchPanel;
	private MEGANEDataModel model;
	private CustomizableColoringDataManager coloringDataManager;
	private SmallBodyModel smallBodyModel;
	private JTabbedPane tabbedPane;
	private MEGANECollection collection;
	private MEGANEDatabaseConnection dbConnection;
	private MEGANEStructureCollection structureCollection;
	private PolygonModel polygons;
	private CircleModel circles;
	private EllipseModel ellipses;
	private AbstractEllipsePolygonModel selectionModel;
	private PickManager pickManager;
	private ModelManager modelManager;

	public MEGANEController(MEGANECollection collection, SmallBodyModel smallBodyModel, ModelManager modelManager, PickManager pickManager)
	{
		this.smallBodyModel = smallBodyModel;
		this.collection = collection;
		this.modelManager = modelManager;
		this.coloringDataManager = (CustomizableColoringDataManager)smallBodyModel.getColoringDataManager();
		this.pickManager = pickManager;

		polygons = (PolygonModel)modelManager.getModel(ModelNames.POLYGON_STRUCTURES);
		circles = (CircleModel)modelManager.getModel(ModelNames.CIRCLE_STRUCTURES);
		ellipses = (EllipseModel)modelManager.getModel(ModelNames.ELLIPSE_STRUCTURES);
        selectionModel = (AbstractEllipsePolygonModel)modelManager.getModel(ModelNames.CIRCLE_SELECTION);


		this.structureCollection = new MEGANEStructureCollection(polygons, circles, ellipses);

		collection.addListener((aSource, aEventType) -> {

			if (aEventType != ItemEventType.ItemsSelected) return;
			updateButtonState();

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
		searchPanel = new MEGANESearchPanel(collection, structureCollection, modelManager);
		searchPanel.getDatabaseLoadButton().addActionListener(e -> {
			File file = CustomFileChooser.showOpenDialog(searchPanel, "Choose Database");
			this.dbConnection = new MEGANEDatabaseConnection(file.getAbsolutePath());
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
		});

		searchPanel.getTableView().getLoadSpectrumButton().addActionListener(e -> {

		});

		searchPanel.getTableView().getSaveSpectrumButton().addActionListener(e -> {

		});

		searchPanel.getTableView().getShowSpectrumButton().addActionListener(e -> {
			ImmutableSet<MEGANEFootprint> selectedFootprints = collection.getSelectedItems();
			if (selectedFootprints.size() == 0) return;
			for (MEGANEFootprint fp : selectedFootprints)
			{
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

		searchPanel.getFilterPanel().getSearchCirclePanel().getSelectRegionButton().addActionListener(e -> {

			if (searchPanel.getFilterPanel().getSearchCirclePanel().getSelectRegionButton().isSelected())
	            pickManager.setPickMode(PickMode.CIRCLE_SELECTION);
	        else
	            pickManager.setPickMode(PickMode.DEFAULT);
		});

		searchPanel.getFilterPanel().getSearchCirclePanel().getClearRegionButton().addActionListener(e -> {

	        selectionModel.removeAllStructures();
		});

		searchPanel.getFilterPanel().getSearchCirclePanel().getSubmitButton().addActionListener(e -> {

			if (selectionModel.getNumItems() == 0) return;
			Ellipse region = selectionModel.getItem(0);
			Task tmpTask = new SilentTask();
			vtkPolyData structureFacetInformation = PlateUtil.formUnifiedStructurePolyData(tmpTask, selectionModel, List.of(region));

			ImmutableList<Integer> cellIdList = smallBodyModel.getClosestCellList(structureFacetInformation);

			try
			{
				List<MEGANEFootprint> filteredFootprints = dbConnection.getFootprintsForFacets(cellIdList);
				collection.setFootprints(filteredFootprints);
			}
			catch (SQLException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});

		searchPanel.getFilterPanel().getSearchStructurePanel().getSubmitButton().addActionListener(e -> {

			vtkPolyData structureFacetInformation = structureCollection.getStructureFacetInformation();
			ImmutableList<Integer> cellIdList = smallBodyModel.getClosestCellList(structureFacetInformation);

			try
			{
				List<MEGANEFootprint> filteredFootprints = dbConnection.getFootprintsForFacets(cellIdList);
				collection.setFootprints(filteredFootprints);
			}
			catch (SQLException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
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
				if (facetColoring == null) return values;
				if (footprint.isVisible() == false) return values;
				for (FacetColoringData coloringData : footprint.getFacetColoringDataForFootprint())
				{
					int index = coloringData.getCellId();
					double valueToCalculate = values.GetValue(index) + 1;
					values.SetValue(index, valueToCalculate);
				}
				return values;
			}

			@Override
			public void setFacetColoringDataForFootprint(PerspectiveImageFootprint footprint)
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
		ICalculatedPlateValues timePerIndexCalculator = LiveColorableManager.getCalculatedPlateValuesFor("Time Per Facet");
		int numberElements = smallBodyModel.getCellNormals().GetNumberOfTuples();
		timePerIndexCalculator.setNumberOfValues(numberElements);
		timePerIndexCalculator.setNumberOfDimensions(1);
		timePerIndexCalculator.getValues().FillComponent(0, 0);
		vtkFloatArray timePerFacetArray = timePerIndexCalculator.getValues();
		IndexableTuple indexableTuple = ColoringDataUtils.createIndexableFromVtkArray(timePerFacetArray);
		ColoringData coloringData = ColoringDataFactory.of("Time Per Facet", "sec", timePerFacetArray.GetNumberOfTuples(), Arrays.asList("Time"), false, indexableTuple);
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
				ColoringData coloringData = ColoringDataFactory.of("Time Per Facet", "sec", valuesAtTime.GetNumberOfTuples(), Arrays.asList("Time"), false, indexableTuple);
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

	public class MEGANEDatabaseConnection {
		private Connection database;
		private final String dbName;
		private SimpleLogger logger;

		public MEGANEDatabaseConnection(String dbName) {
			this.dbName = dbName;
			logger = SimpleLogger.getInstance();
		}

		public void openDatabase() {
			if (database != null)
				return;

			try {
				database = DriverManager.getConnection("jdbc:sqlite:" + dbName);
//				logger.log(Level.INFO, String.format("Opened %s", dbName));
			} catch (SQLException e) {
				logger.log(Level.WARNING, e.getLocalizedMessage());
				e.printStackTrace();
			}
		}

		public List<MEGANEFootprint> getFootprints() throws SQLException
		{
			List<MEGANEFootprint> footprints = Lists.newArrayList();
			Statement st = null;
	        ResultSet rs = null;
	        Object o = null;
	        String expression = "SELECT * FROM observingGeometry ORDER BY tdb";

	        st = database.createStatement();         // statement objects can be reused with

	        // repeated calls to execute but we
	        // choose to make a new one each time
	        rs = st.executeQuery(expression);    // run the query

	        for (; rs.next(); )
	        {
	            List<Double> nextRow = new ArrayList<Double>();
	            for (int i = 0; i < 5; ++i)
	            {
	                o = rs.getObject(i + 1);    // Is SQL the first column is indexed with 1 not 0
	                nextRow.add((Double)o);
	            }

	            MEGANEFootprint footprint = new MEGANEFootprint(nextRow.get(0), nextRow.get(1), nextRow.get(2), nextRow.get(3), nextRow.get(4));
	            footprint.setFacets(getFacets(footprint.getDateTime()));
	            footprints.add(footprint);

	            //System.out.println(" ");
	        }

	        return footprints;
		}

		public List<MEGANEFootprint> getFootprintsForFacets(ImmutableList<Integer> cellIds) throws SQLException
		{
			List<MEGANEFootprint> footprints = Lists.newArrayList();

			Statement st = null;
	        ResultSet rs = null;
	        Object o = null;
	        String expression = "SELECT * FROM observingGeometry ORDER BY tdb";

	        st = database.createStatement();         // statement objects can be reused with

	        // repeated calls to execute but we
	        // choose to make a new one each time
	        rs = st.executeQuery(expression);    // run the query

	        for (; rs.next(); )
	        {
	            List<Double> nextRow = new ArrayList<Double>();
	            for (int i = 0; i < 5; ++i)
	            {
	                o = rs.getObject(i + 1);    // Is SQL the first column is indexed with 1 not 0
	                nextRow.add((Double)o);
	            }

	            List<MEGANEFootprintFacet> facets = getFacets(nextRow.get(0));
	            List<Integer> facetIds = facets.stream().map(facet -> facet.getFacetID()).toList();
	            List<Integer> facetCopy = Lists.newArrayList(facetIds);
	            boolean overlappingFacets = facetCopy.retainAll(cellIds);
	            if (overlappingFacets == true && facetCopy.size() == 0) continue;


	            MEGANEFootprint footprint = new MEGANEFootprint(nextRow.get(0), nextRow.get(1), nextRow.get(2), nextRow.get(3), nextRow.get(4));
	            footprint.setFacets(getFacets(footprint.getDateTime()));
	            footprints.add(footprint);
	        }


			return footprints;
		}

		public List<MEGANEFootprintFacet> getFacets(double time) throws SQLException
		{
			List<MEGANEFootprintFacet> facets = Lists.newArrayList();

			Statement st = null;
	        ResultSet rs = null;
	        Object o = null;
	        String expression = "SELECT * FROM facetObs WHERE tdb=" + time + " ORDER BY id";

	        st = database.createStatement();         // statement objects can be reused with

	        // repeated calls to execute but we
	        // choose to make a new one each time
	        rs = st.executeQuery(expression);    // run the query

	        for (; rs.next(); )
	        {
	            List<Object> nextRow = new ArrayList<Object>();
	            for (int i = 0; i < 5; ++i)
	            {
	                o = rs.getObject(i + 1);    // Is SQL the first column is indexed with 1 not 0
	                nextRow.add(o);
	            }

	            if ((Double)nextRow.get(3) < 0) continue;
	            MEGANEFootprintFacet facet = new MEGANEFootprintFacet(time, (Integer)nextRow.get(0), (Double)nextRow.get(2), (Double)nextRow.get(3), (Double)nextRow.get(4));
	            facets.add(facet);
	        }


			return facets;
		}

		private void executeSQL(String sql) {
			if (!sql.trim().endsWith(";"))
				sql = String.format("%s;", sql.trim());
			logger.log(Level.FINE, sql);
			try (Statement stmt = database.createStatement()) {
				stmt.execute(sql);
			} catch (SQLException e) {
				logger.log(Level.SEVERE, "Cannot execute query " + sql);
				logger.log(Level.SEVERE, e.getLocalizedMessage());
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

}
