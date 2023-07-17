package edu.jhuapl.sbmt.model.lineament;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

import vtk.vtkProp;

import edu.jhuapl.saavtk.gui.dialog.ColorChooser;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.model.ModelNames;
import edu.jhuapl.saavtk.popup.PopupMenu;

public class LineamentPopupMenu extends PopupMenu
{
    private enum ColoringType
    {
        ONE_LINEAMENT,
        LINEAMENTS_PER_IMAGE,
        ALL_LINEAMENTS,
    }

    private LineamentModel model;
    private LineamentModel.Lineament lineament;
    private Component invoker;

    public LineamentPopupMenu(ModelManager modelManager)
    {
        this.model = (LineamentModel)modelManager.getModel(ModelNames.LINEAMENT);

        JMenuItem mi;
        mi = new JMenuItem(new ChangeLineamentColorAction(ColoringType.ONE_LINEAMENT));
        mi.setText("Change color of this lineament only");
        this.add(mi);
        mi = new JMenuItem(new ChangeLineamentColorAction(ColoringType.LINEAMENTS_PER_IMAGE));
        mi.setText("Change color of lineaments on this MSI image");
        this.add(mi);
        mi = new JMenuItem(new ChangeLineamentColorAction(ColoringType.ALL_LINEAMENTS));
        mi.setText("Change color of all lineaments");
        this.add(mi);

    }

    private class ChangeLineamentColorAction extends AbstractAction
    {
        ColoringType coloringType;

        public ChangeLineamentColorAction(ColoringType type)
        {
            this.coloringType = type;
        }

        public void actionPerformed(ActionEvent actionEvent)
        {
            Color color = ColorChooser.showColorChooser(invoker);

            if (color == null)
                return;

            int[] c = new int[4];
            c[0] = color.getRed();
            c[1] = color.getGreen();
            c[2] = color.getBlue();
            c[3] = color.getAlpha();

            switch(coloringType)
            {
            case ONE_LINEAMENT:
                model.setLineamentColor(lineament.cellId, c);
                break;
            case LINEAMENTS_PER_IMAGE:
                model.setMSIImageLineamentsColor(lineament.cellId, c);
                break;
            case ALL_LINEAMENTS:
                model.setsAllLineamentsColor(c);
                break;
            }
        }
    }

    public void showPopup(MouseEvent e, vtkProp pickedProp, int pickedCellId,
            double[] pickedPosition)
    {
        LineamentModel.Lineament lin = model.getLineament(pickedCellId);
        if (lin != null)
        {
            invoker = e.getComponent();
            lineament = lin;
            super.show(invoker, e.getX(), e.getY());
        }
    }
}
