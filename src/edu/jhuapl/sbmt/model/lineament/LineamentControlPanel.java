package edu.jhuapl.sbmt.model.lineament;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.jhuapl.saavtk.gui.RadialOffsetChanger;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.model.ModelNames;

import net.miginfocom.swing.MigLayout;

public class LineamentControlPanel extends JPanel implements ItemListener, ChangeListener
{
    private JCheckBox lineamentCheckBox;
    private LineamentModel lineamentModel;
    private JLabel lineWidthLabel;
    private JSpinner lineWidthSpinner;

    public LineamentControlPanel(ModelManager modelManager)
    {
        setLayout(new MigLayout(
                "fillx",
                "",
                ""));

        lineamentModel = (LineamentModel)modelManager.getModel(ModelNames.LINEAMENT);

        lineamentCheckBox = new JCheckBox();
        lineamentCheckBox.setText("Show Lineaments");
        lineamentCheckBox.setSelected(false);
        lineamentCheckBox.addItemListener(this);

        JPanel lineWidthPanel = new JPanel();
        lineWidthLabel = new JLabel("Line Width");
        lineWidthSpinner = new JSpinner(new SpinnerNumberModel(1.0, 1.0, 100.0, 1.0));
        lineWidthSpinner.setEditor(new JSpinner.NumberEditor(lineWidthSpinner, "0"));
        lineWidthSpinner.setPreferredSize(new Dimension(80, 22));
        lineWidthSpinner.addChangeListener(this);
        String lineWidthTooltip = "The line width of the lineaments";
        lineWidthLabel.setToolTipText(lineWidthTooltip);
        lineWidthSpinner.setToolTipText(lineWidthTooltip);
        lineWidthPanel.add(lineWidthLabel);
        lineWidthPanel.add(lineWidthSpinner);

        RadialOffsetChanger radialChanger = new RadialOffsetChanger();
        radialChanger.setModel(lineamentModel);

        add(lineamentCheckBox, "wrap");
        add(lineWidthPanel, "wrap");
        add(radialChanger, "growx");
    }

    public void itemStateChanged(ItemEvent e)
    {
        if (e.getItemSelectable() == this.lineamentCheckBox)
        {
            if (e.getStateChange() == ItemEvent.DESELECTED)
                lineamentModel.setShowLineaments(false);
            else
                lineamentModel.setShowLineaments(true);
        }
    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
        if (e.getSource() == lineWidthSpinner)
        {
            double val = (Double)lineWidthSpinner.getValue();
            lineamentModel.setLineWidth(val);
        }
    }
}
