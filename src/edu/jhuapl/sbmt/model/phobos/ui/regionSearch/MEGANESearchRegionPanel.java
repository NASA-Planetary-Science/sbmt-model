package edu.jhuapl.sbmt.model.phobos.ui.regionSearch;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class MEGANESearchRegionPanel extends JPanel
{
	private JToggleButton selectRegionButton;
	private JButton clearRegionButton;
	private JButton submitButton;
//	private JFormattedTextField fromDistanceTextField;
//	private JFormattedTextField toDistanceTextField;
	private JFormattedTextField fromLongitudeTextField;
	private JFormattedTextField toLongitudeTextField;
	private JFormattedTextField fromLatitudeTextField;
	private JFormattedTextField toLatitudeTextField;
	private JFormattedTextField fromAltitudeTextField;
	private JFormattedTextField toAltitudeTextField;

	public MEGANESearchRegionPanel()
	{
		initGUI();
	}

	private void initGUI()
	{
		selectRegionButton = new JToggleButton("Select Region");
        clearRegionButton = new JButton("Clear Region");
        submitButton = new JButton("Search");

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
//        setBorder(BorderFactory.createTitledBorder("Filter by Region and Attributes"));

        fromLatitudeTextField = new JFormattedTextField();
        toLatitudeTextField = new JFormattedTextField();
        fromLongitudeTextField = new JFormattedTextField();
        toLongitudeTextField = new JFormattedTextField();
        fromAltitudeTextField = new JFormattedTextField();
        toAltitudeTextField = new JFormattedTextField();


        //latitude
        JPanel latitudePanel = getSearchFieldFor("S/C Latitude from", "-90", "90", fromLatitudeTextField, toLatitudeTextField);
        //longitude
        JPanel longitudePanel = getSearchFieldFor("S/C Longitude from", "-180", "180", fromLongitudeTextField, toLongitudeTextField);


        //altitude
        JPanel altitudePanel = getSearchFieldFor("S/C Altitude from", "0", "20", fromAltitudeTextField, toAltitudeTextField);

        JPanel attributePanel = new JPanel();
        attributePanel.setLayout(new BoxLayout(attributePanel, BoxLayout.Y_AXIS));
        attributePanel.add(latitudePanel);
        attributePanel.add(longitudePanel);
        attributePanel.add(altitudePanel);

        add(attributePanel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(selectRegionButton);
        buttonPanel.add(clearRegionButton);
        buttonPanel.add(submitButton);
        add(Box.createVerticalGlue());
        add(buttonPanel);
        setPreferredSize(new Dimension(400, 400));
        setMaximumSize(new Dimension(400, 400));
	}

	private JPanel getSearchFieldFor(String name, String minValue, String maxValue, JFormattedTextField fromField, JFormattedTextField toField)
	{
		JPanel panel = new JPanel();
        JLabel lblScDistanceFrom = new JLabel(name);
        panel.add(lblScDistanceFrom);

        fromField.setText(minValue);
        fromField.setMaximumSize(
                new Dimension(fromField.getWidth(), 20));
        fromField.setColumns(5);
        panel.add(fromField);

        panel.add(new JLabel("to"));

        toField.setText(maxValue);
        toField.setMaximumSize(
                new Dimension(toField.getWidth(), 20));
        toField.setColumns(5);
        panel.setMaximumSize(new Dimension(400, 30));
        panel.add(toField);
        return panel;
	}

	/**
	 * @return the selectRegionButton
	 */
	public JToggleButton getSelectRegionButton()
	{
		return selectRegionButton;
	}

	/**
	 * @return the clearRegionButton
	 */
	public JButton getClearRegionButton()
	{
		return clearRegionButton;
	}

	/**
	 * @return the submitButton
	 */
	public JButton getSubmitButton()
	{
		return submitButton;
	}
}
