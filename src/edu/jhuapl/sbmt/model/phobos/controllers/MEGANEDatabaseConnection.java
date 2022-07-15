package edu.jhuapl.sbmt.model.phobos.controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import org.apache.commons.lang3.tuple.Pair;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ImmutableList;

import edu.jhuapl.sbmt.model.phobos.model.MEGANEFootprint;
import edu.jhuapl.sbmt.model.phobos.model.MEGANEFootprintFacet;

import crucible.crust.logging.SimpleLogger;

public class MEGANEDatabaseConnection
{
	/**
	 *
	 */
	private final MEGANEController meganeController;
	private Connection database;
	private final String dbName;
	private SimpleLogger logger;
	private int index = 1;

	public MEGANEDatabaseConnection(MEGANEController meganeController, String dbName)
	{
		this.meganeController = meganeController;
		this.dbName = dbName;
		logger = SimpleLogger.getInstance();
	}

	public void openDatabase()
	{
		if (database != null) return;

		try
		{
			database = DriverManager.getConnection("jdbc:sqlite:" + dbName);
		}
		catch (SQLException e)
		{
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

		st = database.createStatement(); // statement objects can be reused with

		// repeated calls to execute but we choose to make a new one each time
		rs = st.executeQuery(expression); // run the query
		for (; rs.next();)
		{
			List<Double> nextRow = new ArrayList<Double>();
			for (int i = 0; i < 5; ++i)
			{
				o = rs.getObject(i + 1); // Is SQL the first column is indexed with 1 not 0
				nextRow.add((Double) o);
			}

			MEGANEFootprint footprint = new MEGANEFootprint(nextRow.get(0), nextRow.get(1), nextRow.get(2),
					nextRow.get(3), nextRow.get(4));
			footprints.add(footprint);
		}

		return footprints;
	}

	public List<MEGANEFootprint> getFootprintsForSQLParameters(String sqlString) throws SQLException
	{
		List<MEGANEFootprint> footprints = Lists.newArrayList();
		Statement st = null;
		ResultSet rs = null;
		Object o = null;
		String expression = "SELECT * FROM observingGeometry WHERE " + sqlString + " ORDER BY tdb";

		st = database.createStatement(); // statement objects can be reused with

		// repeated calls to execute but we choose to make a new one each time
		rs = st.executeQuery(expression); // run the query

		for (; rs.next();)
		{
			List<Double> nextRow = new ArrayList<Double>();
			for (int i = 0; i < 5; ++i)
			{
				o = rs.getObject(i + 1); // Is SQL the first column is indexed with 1 not 0
				nextRow.add((Double) o);
			}

			MEGANEFootprint footprint = new MEGANEFootprint(nextRow.get(0), nextRow.get(1), nextRow.get(2),
					nextRow.get(3), nextRow.get(4));
			footprints.add(footprint);
		}

		return footprints;
	}

	public List<MEGANEFootprint> getFootprintsForFacets2(ImmutableList<Integer> cellIds, String facetObsString,
			String obsGeomString, Function<String, Void> statusUpdater) throws SQLException
	{
		// find the matching facets ids and get their times, so the footprints can be fetched
		List<MEGANEFootprint> footprints = Lists.newArrayList();
		// double integrationTime = 180.0;
		Statement st = null;
		ResultSet rs = null;
		Object o = null;
		String cellIDValues = "(";
		for (int i = 0; i < cellIds.size(); i++)
		{
			cellIDValues += cellIds.get(i);
			if (i + 1 < cellIds.size())
				cellIDValues += ",";
		}
		cellIDValues += ")";
		// String facetExpression = "SELECT tdb FROM facetObs WHERE
		// integrationTime=" + integrationTime;
		String facetExpression = "SELECT tdb FROM facetObs WHERE ";
		if (!facetObsString.isEmpty())
			facetExpression += facetObsString;

		if (cellIds.size() != 0)
		{
			facetExpression += " AND id IN " + cellIDValues;
		}

		// System.out.println("MEGANEController.MEGANEDatabaseConnection: getFootprintsForFacets2: facet obs string " + facetExpression);
		st = database.createStatement(); // statement objects can be reused with

		// repeated calls to execute but we choose to make a new one each time
		SwingUtilities.invokeLater(() ->
		{
			statusUpdater.apply("Running query...");
		});
		rs = st.executeQuery(facetExpression); // run the query

		HashSet<Double> times = new HashSet<Double>();
		String timeValues = "(";
		for (; rs.next();)
		{
			times.add((Double) rs.getObject(1));
		}
		// System.out.println("MEGANEController.MEGANEDatabaseConnection: getFootprintsForFacets2: number of times fetched " + times.size());
		Iterator<Double> iterator = times.iterator();
		// for (int i=0; i<times.size(); i++)
		while (iterator.hasNext())
		{
			timeValues += iterator.next();
			if (iterator.hasNext())
				timeValues += ",";
		}
		timeValues += ")";
		String expression;
		if (!obsGeomString.isEmpty())
		{
			expression = "SELECT * FROM observingGeometry WHERE tdb IN " + timeValues + " AND (" + obsGeomString
					+ ") ORDER BY tdb";
		}
		else
		{
			expression = "SELECT * FROM observingGeometry WHERE tdb IN " + timeValues + " ORDER BY tdb";
		}

		st = database.createStatement(); // statement objects can be reused with repeated calls to execute but we choose to make a new one each time
		rs = st.executeQuery(expression); // run the query
		Pair<Double, Double> currentSignalRange = this.meganeController.getSearchModel().getCurrentSignalContributionRange();

		index = 1;
		for (; rs.next();)
		{
			SwingUtilities.invokeLater(() ->
			{
				statusUpdater.apply("Processing row " + index++ + "...");
			});
			List<Double> nextRow = new ArrayList<Double>();
			for (int i = 0; i < 5; ++i)
			{
				o = rs.getObject(i + 1); // Is SQL the first column is indexed
											// with 1 not 0
				nextRow.add((Double) o);
			}

			MEGANEFootprint footprint = new MEGANEFootprint(nextRow.get(0), nextRow.get(1), nextRow.get(2),
					nextRow.get(3), nextRow.get(4));
			if (currentSignalRange != null && cellIds.size() > 0)
			{
				footprint.setFacets(getFacets(footprint.getDateTime(), new Function<String, Void>()
				{
					@Override
					public Void apply(String t)
					{
						SwingUtilities.invokeLater(() ->
						{
							footprint.setStatus(t);
						});
						return null;
					}
				}));
				SwingUtilities.invokeLater(() -> { footprint.setStatus("Unloaded"); });
				double low = currentSignalRange.getLeft();
				double high = currentSignalRange.getRight();
				double totalContribution = footprint.getSummedValue();
				double inStructureSum = 0;
				List<MEGANEFootprintFacet> matchingIndices = footprint.getFacets().stream()
						.filter(facet -> cellIds.contains(facet.getFacetID())).toList();
				for (MEGANEFootprintFacet facet : matchingIndices)
				{
					inStructureSum += facet.getComputedValue();
				}
				double signalCont = (inStructureSum * 100 / totalContribution);
				footprint.setSignalContribution(signalCont);
				if (signalCont >= low && signalCont <= high)
					footprints.add(footprint);
			}
			else
				footprints.add(footprint);
		}
		SwingUtilities.invokeLater(() -> { statusUpdater.apply("Ready."); });
		return footprints;
	}

	public List<MEGANEFootprintFacet> getFacets(double time, Function<String, Void> statusUpdater) throws SQLException
	{
		List<MEGANEFootprintFacet> facets = Lists.newArrayList();
		statusUpdater.apply("Getting facets");
		Statement st = null;
		ResultSet rs = null;
		Object o = null;
		String expression = "SELECT * FROM facetObs WHERE tdb=" + time + " ORDER BY id";
		String intTime = this.meganeController.getSearchModel().getCurrentIntegrationTime();
		if (!intTime.isEmpty())
			expression = "SELECT * FROM facetObs WHERE tdb=" + time + " AND " + intTime + " ORDER BY id";

		st = database.createStatement(); // statement objects can be reused with
		statusUpdater.apply("Querying..");

		rs = st.executeQuery(expression); // run the query
		int index = 1;
		statusUpdater.apply("Indexing");
		List<Object> nextRow;
		for (; rs.next();)
		{
			nextRow = new ArrayList<Object>();
			for (int i = 0; i < 8; ++i)
			{
				o = rs.getObject(i + 1); // Is SQL the first column is indexed with 1 not 0
				nextRow.add(o);
			}
			statusUpdater.apply("Idx " + index++ + rs.getFetchSize());
			facets.add(new MEGANEFootprintFacet(time, (Integer) nextRow.get(0), (Double) nextRow.get(2),
					(Double) nextRow.get(5), (Double) nextRow.get(6), (Double) nextRow.get(7)));
		}

		return facets;
	}

	private void executeSQL(String sql)
	{
		if (!sql.trim().endsWith(";"))
			sql = String.format("%s;", sql.trim());
		logger.log(Level.FINE, sql);
		try (Statement stmt = database.createStatement())
		{
			stmt.execute(sql);
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, "Cannot execute query " + sql);
			logger.log(Level.SEVERE, e.getLocalizedMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
}