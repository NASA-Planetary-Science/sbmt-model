package edu.jhuapl.sbmt.model.rosetta;

import java.beans.PropertyChangeEvent;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

import edu.jhuapl.saavtk.gui.Renderer;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.saavtk.util.IdPair;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SbmtInfoWindowManager;
import edu.jhuapl.sbmt.client.SbmtSpectrumWindowManager;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.gui.image.ImagingSearchPanel;
import edu.jhuapl.sbmt.model.image.Image.ImageKey;
import edu.jhuapl.sbmt.model.image.ImageCollection;
import edu.jhuapl.sbmt.model.image.ImagingInstrument;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;
import edu.jhuapl.sbmt.model.image.PerspectiveImageBoundaryCollection;

public class OsirisImagingSearchPanel extends ImagingSearchPanel    // this class overrides the default jtable model for the results list in ImagingSearchPanel to include parameters for controlling off-limb rendering planes
{
    int hideOffLimbFootprintColumnIndex;

    public OsirisImagingSearchPanel(SmallBodyViewConfig smallBodyConfig,
            ModelManager modelManager, SbmtInfoWindowManager infoPanelManager,
            SbmtSpectrumWindowManager spectrumPanelManager,
            PickManager pickManager, Renderer renderer,
            ImagingInstrument instrument)
    {
        super(smallBodyConfig, modelManager, infoPanelManager, spectrumPanelManager,
                pickManager, renderer, instrument);
    }

    @Override
    protected void postInitComponents(ImagingInstrument instrument)
    {
        super.postInitComponents(instrument);

        // override the default jtable model

        String[] columnNames = {
                "Map",
                "Hide Ftprnt",
                "Hide OffLmb",
                "Frus",
                "Bndr",
                "Id",
                "Filename",
                "Date"
        };
        mapColumnIndex=0;
        hideFootprintColumnIndex=1;
        hideOffLimbFootprintColumnIndex=2;
        frusColumnIndex=3;
        bndrColumnIndex=4;
        idColumnIndex=5;
        filenameColumnIndex=6;
        dateColumnIndex=7;

        Object[][] data = new Object[0][8];
        resultList.setModel(new StructuresTableModel(data, columnNames));
        resultList.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resultList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resultList.setDefaultRenderer(String.class, new StringRenderer());
        resultList.getColumnModel().getColumn(mapColumnIndex).setPreferredWidth(31);
        resultList.getColumnModel().getColumn(hideFootprintColumnIndex).setPreferredWidth(69);
        resultList.getColumnModel().getColumn(hideOffLimbFootprintColumnIndex).setPreferredWidth(73);
        resultList.getColumnModel().getColumn(frusColumnIndex).setPreferredWidth(31);
        resultList.getColumnModel().getColumn(bndrColumnIndex).setPreferredWidth(31);
        resultList.getColumnModel().getColumn(idColumnIndex).setPreferredWidth(31);
        resultList.getColumnModel().getColumn(mapColumnIndex).setResizable(false);
        resultList.getColumnModel().getColumn(hideFootprintColumnIndex).setResizable(false);
        resultList.getColumnModel().getColumn(hideOffLimbFootprintColumnIndex).setResizable(false);
        resultList.getColumnModel().getColumn(frusColumnIndex).setResizable(false);
        resultList.getColumnModel().getColumn(bndrColumnIndex).setResizable(false);
        resultList.getColumnModel().getColumn(idColumnIndex).setResizable(false);
        resultList.addMouseListener(this);
        resultList.getModel().addTableModelListener(this);
    }


    @Override
    protected void setImageResults(List<List<String>> results)
    {
        super.setImageResults(results);

        // this is virtually a copy of ImageSearchPanel.setImageResults(...), but it is needed to implement hiding the off-limb footprints

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        ImageCollection images = (ImageCollection)modelManager.getModel(getImageCollectionModelName());
        PerspectiveImageBoundaryCollection boundaries = (PerspectiveImageBoundaryCollection)modelManager.getModel(getImageBoundaryCollectionModelName());

        resultList.getModel().removeTableModelListener(this);
        images.removePropertyChangeListener(this);
        boundaries.removePropertyChangeListener(this);

        // add the results to the list
        ((DefaultTableModel)resultList.getModel()).setRowCount(results.size());
        int i=0;
        for (List<String> str : results)
        {

            String name = imageRawResults.get(i).get(0);
//            ImageKey key = new ImageKey(name.substring(0, name.length()-4), sourceOfLastQuery, instrument);
            ImageKey key = createImageKey(name.substring(0, name.length()-4), sourceOfLastQuery, instrument);
            if (images.containsImage(key))
            {
                PerspectiveImage image = (PerspectiveImage) images.getImage(key);
                resultList.setValueAt(!((OsirisImage)image).offLimbFootprintIsVisible(), i, hideOffLimbFootprintColumnIndex);
            }
            else
            {
                resultList.setValueAt(false, i, hideOffLimbFootprintColumnIndex);
            }

            ++i;
        }

        resultList.getModel().addTableModelListener(this);
        images.addPropertyChangeListener(this);
        boundaries.addPropertyChangeListener(this);

        // Show the first set of boundaries
        this.resultIntervalCurrentlyShown = new IdPair(0, Integer.parseInt((String)this.numberOfBoundariesComboBox.getSelectedItem()));
        this.showImageBoundaries(resultIntervalCurrentlyShown);

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {

        // this code has basically been copied from ImageSearchPanel.propertyChange(...) to implement hiding and interacting with off-limb footprints

        if (Properties.MODEL_CHANGED.equals(evt.getPropertyName()))
        {
            resultList.getModel().removeTableModelListener(this);
            int size = imageRawResults.size();
            ImageCollection images = (ImageCollection)modelManager.getModel(getImageCollectionModelName());
            PerspectiveImageBoundaryCollection boundaries = (PerspectiveImageBoundaryCollection)modelManager.getModel(getImageBoundaryCollectionModelName());
            for (int i=0; i<size; ++i)
            {
                String name = imageRawResults.get(i).get(0);
//                ImageKey key = new ImageKey(name.substring(0, name.length()-4), sourceOfLastQuery, instrument);
                ImageKey key = createImageKey(name.substring(0, name.length()-4), sourceOfLastQuery, instrument);
                if (images.containsImage(key))
                {
                    PerspectiveImage image = (PerspectiveImage) images.getImage(key);
    //                System.out.println(((OsirisImage)image).offLimbFootprintIsVisible());
                    resultList.setValueAt(!((OsirisImage)image).offLimbFootprintIsVisible(), i, hideOffLimbFootprintColumnIndex);
                }
                else
                {
                    resultList.setValueAt(false, i, hideOffLimbFootprintColumnIndex);
                }
            }
            resultList.getModel().addTableModelListener(this);
            // Repaint the list in case the boundary colors has changed
            resultList.repaint();
        }

        super.propertyChange(evt);

    }

    protected void setOffLimbFootprintVisibility(String name, boolean visible)
    {
        List<ImageKey> keys = createImageKeys(name, sourceOfLastQuery, instrument);
        ImageCollection images = (ImageCollection)modelManager.getModel(getImageCollectionModelName());
        for (ImageKey key : keys)
        {
            if (images.containsImage(key))
            {
                OsirisImage image = (OsirisImage)images.getImage(key);
                image.setOffLimbFootprintVisibility(visible);
            }
        }
    }


    @Override
    public void tableChanged(TableModelEvent e)
    {
        // TODO Auto-generated method stub
        super.tableChanged(e);

        if (e.getColumn() == hideOffLimbFootprintColumnIndex)
        {
            int row = e.getFirstRow();
            String name = imageRawResults.get(row).get(0);
            String namePrefix = name.substring(0, name.length()-4);
            boolean visible = !(Boolean)resultList.getValueAt(row, hideOffLimbFootprintColumnIndex);
            setOffLimbFootprintVisibility(namePrefix, visible);
        }

    }


    public class StructuresTableModel extends DefaultTableModel
    {
        public StructuresTableModel(Object[][] data, String[] columnNames)
        {
            super(data, columnNames);
        }

        public boolean isCellEditable(int row, int column)
        {
            // Only allow editing the hide column if the image is mapped
            if (column == hideFootprintColumnIndex || column == hideOffLimbFootprintColumnIndex || column == frusColumnIndex)
            {
                String name = imageRawResults.get(row).get(0);
//                ImageKey key = new ImageKey(name.substring(0, name.length()-4), sourceOfLastQuery, instrument);
                ImageKey key = createImageKey(name.substring(0, name.length()-4), sourceOfLastQuery, instrument);
                ImageCollection images = (ImageCollection)modelManager.getModel(getImageCollectionModelName());
                return images.containsImage(key);
            }
            else
            {
                return column == mapColumnIndex || column == bndrColumnIndex;
            }
        }

        public Class<?> getColumnClass(int columnIndex)
        {
            if (columnIndex <= bndrColumnIndex)
                return Boolean.class;
            else
                return String.class;
        }
    }


}
