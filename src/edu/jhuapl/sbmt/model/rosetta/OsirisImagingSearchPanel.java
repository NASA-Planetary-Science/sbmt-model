package edu.jhuapl.sbmt.model.rosetta;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

import com.jidesoft.swing.RangeSlider;

import edu.jhuapl.saavtk.gui.renderer.Renderer;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.saavtk.util.IdPair;
import edu.jhuapl.saavtk.util.IntensityRange;
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

public class OsirisImagingSearchPanel extends ImagingSearchPanel  implements ChangeListener // this class overrides the default jtable model for the results list in ImagingSearchPanel to include parameters for controlling off-limb rendering planes
{
    int showOffLimbFootprintColumnIndex;

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
                "Footprint",
                "OffLimb",
                "Frustum",
                "Bndry",
                "Id",
                "Filename",
                "Date"
        };
        mapColumnIndex=0;
        showFootprintColumnIndex=1;
        showOffLimbFootprintColumnIndex=2;
        frusColumnIndex=3;
        bndrColumnIndex=4;
        idColumnIndex=5;
        filenameColumnIndex=6;
        dateColumnIndex=7;

        Object[][] data = new Object[0][8];
        getResultList().setModel(new StructuresTableModel(data, columnNames));
        getResultList().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        getResultList().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        getResultList().setDefaultRenderer(String.class, new StringRenderer());
        getResultList().getColumnModel().getColumn(mapColumnIndex).setPreferredWidth(31);
        getResultList().getColumnModel().getColumn(showFootprintColumnIndex).setPreferredWidth(69);
        getResultList().getColumnModel().getColumn(showOffLimbFootprintColumnIndex).setPreferredWidth(73);
        getResultList().getColumnModel().getColumn(frusColumnIndex).setPreferredWidth(31);
        getResultList().getColumnModel().getColumn(bndrColumnIndex).setPreferredWidth(31);
        getResultList().getColumnModel().getColumn(idColumnIndex).setPreferredWidth(31);
        getResultList().getColumnModel().getColumn(mapColumnIndex).setResizable(true);
        getResultList().getColumnModel().getColumn(showFootprintColumnIndex).setResizable(true);
        getResultList().getColumnModel().getColumn(showOffLimbFootprintColumnIndex).setResizable(true);
        getResultList().getColumnModel().getColumn(frusColumnIndex).setResizable(true);
        getResultList().getColumnModel().getColumn(bndrColumnIndex).setResizable(true);
        getResultList().getColumnModel().getColumn(idColumnIndex).setResizable(true);
        getResultList().addMouseListener(this);
        getResultList().getModel().addTableModelListener(this);
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

        getResultList().getModel().removeTableModelListener(this);
        images.removePropertyChangeListener(this);
        boundaries.removePropertyChangeListener(this);

        // add the results to the list
        ((DefaultTableModel)getResultList().getModel()).setRowCount(results.size());
        int i=0;
        for (List<String> str : results)
        {

            String name = imageRawResults.get(i).get(0);
//            ImageKey key = new ImageKey(name.substring(0, name.length()-4), sourceOfLastQuery, instrument);
            ImageKey key = createImageKey(name.substring(0, name.length()-4), sourceOfLastQuery, instrument);
            if (images.containsImage(key))
            {
                PerspectiveImage image = (PerspectiveImage) images.getImage(key);
                ((OsirisImage)image).setOffLimbFootprintVisibility(false);   // hide off limb footprint by default
                getResultList().setValueAt(false, i, showOffLimbFootprintColumnIndex);   // hide off limb footprint by default
            }
            else
            {
                getResultList().setValueAt(false, i, showOffLimbFootprintColumnIndex);   // hide off limb footprint by default
            }

            ++i;
        }

        getResultList().getModel().addTableModelListener(this);
        images.addPropertyChangeListener(this);
        boundaries.addPropertyChangeListener(this);

        // Show the first set of boundaries
        this.resultIntervalCurrentlyShown = new IdPair(0, Integer.parseInt((String)this.getNumberOfBoundariesComboBox().getSelectedItem()));
        this.showImageBoundaries(resultIntervalCurrentlyShown);

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {

        // this code has basically been copied from ImageSearchPanel.propertyChange(...) to implement hiding and interacting with off-limb footprints

        if (Properties.MODEL_CHANGED.equals(evt.getPropertyName()))
        {
            getResultList().getModel().removeTableModelListener(this);
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
    //                getResultList().setValueAt(!((OsirisImage)image).offLimbFootprintIsVisible(), i, hideOffLimbFootprintColumnIndex);
                    ((OsirisImage)image).setOffLimbFootprintAlpha(alphaSlider.getAlphaValue());
                    if (depthSlider.activeImage!=null)
                        depthSlider.setValue(depthSlider.convertDepthToSliderValue(((OsirisImage)image).getOffLimbPlaneDepth()));
                }
                else
                {
                    getResultList().setValueAt(false, i, showOffLimbFootprintColumnIndex);
                }
            }
            getResultList().getModel().addTableModelListener(this);
            // Repaint the list in case the boundary colors has changed
            getResultList().repaint();
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

        if (e.getColumn() == mapColumnIndex)
        {
            int row = e.getFirstRow();
            String name = imageRawResults.get(row).get(0);
            String namePrefix = name.substring(0, name.length()-4);
            setOffLimbFootprintVisibility(namePrefix, false);   // set visibility to false if we are mapping or unmapping the image
            getResultList().setValueAt(false, row, showOffLimbFootprintColumnIndex);
        }

        if (e.getColumn() == showOffLimbFootprintColumnIndex)
        {
            int row = e.getFirstRow();
            String name = imageRawResults.get(row).get(0);
            String namePrefix = name.substring(0, name.length()-4);
            boolean visible = (Boolean)getResultList().getValueAt(row, showOffLimbFootprintColumnIndex);
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
            if (column == showFootprintColumnIndex || column == showOffLimbFootprintColumnIndex || column == frusColumnIndex)
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


    class DepthSlider extends JSlider
    {
        double depthMin,depthMax;
        OsirisImage activeImage;

        public DepthSlider()
        {
            setMinimum(0);
            setMaximum(100);
        }

        public void setDepthBounds(double depthMin, double depthMax)
        {
            this.depthMin=depthMin;
            this.depthMax=depthMax;
        }

        public void applyDepthToImage(OsirisImage image)
        {
            activeImage=image;
            depthMin=activeImage.getMinFrustumDepth(getCurrentSlice());
            depthMax=activeImage.getMaxFrustumDepth(getCurrentSlice());
            activeImage.loadOffLimbPlane(getDepthValue());
        }

        public double getDepthValue()
        {
            double depthNorm=(double)(getValue()-getMinimum())/(double)(getMaximum()-getMinimum());
            return depthMin+depthNorm*(depthMax-depthMin);
        }

        public int convertDepthToSliderValue(double depth)
        {
            double depthNorm=(activeImage.getOffLimbPlaneDepth()-depthMin)/(depthMax-depthMin);
            return (int)((double)(getMaximum()-getMinimum())*depthNorm);
        }

    }

    class AlphaSlider extends JSlider
    {
        public AlphaSlider()
        {
            setMinimum(0);
            setMaximum(100);
        }

        public void applyAlphaToImage(OsirisImage image)
        {
            image.setOffLimbFootprintAlpha(getAlphaValue());
        }

        public double getAlphaValue()
        {
            return (double)(getValue()-getMinimum())/(double)(getMaximum()-getMinimum());
        }
    }

    class ContrastSlider extends RangeSlider
    {
        public ContrastSlider()
        {
            setMinimum(0);
            setMaximum(255);
        }

        public void applyContrastToImage(OsirisImage image)
        {
            image.setDisplayedImageRange(new IntensityRange(getLowValue(), getHighValue()));
        }
    }

    DepthSlider depthSlider;
    AlphaSlider alphaSlider;
    ContrastSlider contrastSlider;

    @Override
    protected void populateMonochromePanel(JPanel panel)
    {
        super.populateMonochromePanel(panel);
        //
        depthSlider=new DepthSlider();
        depthSlider.addChangeListener(this);
        JLabel label=new JLabel();
        label.setText("Off-limb footprint depth");

        alphaSlider=new AlphaSlider();
        alphaSlider.addChangeListener(this);
        JLabel label2=new JLabel();
        label2.setText("Off-limb footprint transparency");

        contrastSlider=new ContrastSlider();
        contrastSlider.addChangeListener(this);
        JLabel label3=new JLabel();
        label3.setText("Image contrast");

        JPanel wholePanel=new JPanel(new GridLayout(3, 1));

        JPanel depthPanel=new JPanel();
        depthPanel.add(label,BorderLayout.NORTH);
        depthPanel.add(depthSlider,BorderLayout.CENTER);
        JPanel alphaPanel=new JPanel();
        alphaPanel.add(label2,BorderLayout.NORTH);
        alphaPanel.add(alphaSlider,BorderLayout.CENTER);
        JPanel contrastPanel=new JPanel();
        contrastPanel.add(label3, BorderLayout.NORTH);
        contrastPanel.add(contrastSlider, BorderLayout.CENTER);

        wholePanel.add(depthPanel);
        wholePanel.add(alphaPanel);
        wholePanel.add(contrastPanel);

        panel.add(wholePanel);
    }


    @Override
    public void stateChanged(ChangeEvent e)
    {
        if (getResultList().getSelectedRows().length==1)
        {
            String name = imageRawResults.get(getResultList().getSelectedRow()).get(0);
            ImageKey key = createImageKey(name.substring(0, name.length()-4), sourceOfLastQuery, instrument);
            ImageCollection images = (ImageCollection)modelManager.getModel(getImageCollectionModelName());
            OsirisImage image=(OsirisImage)images.getImage(key);
            if (image!=null)
            {
                if (e.getSource()==depthSlider && !depthSlider.getValueIsAdjusting())
                    depthSlider.applyDepthToImage(image);
                else if (e.getSource()==alphaSlider && !alphaSlider.getValueIsAdjusting())
                    alphaSlider.applyAlphaToImage(image);
                else if (e.getSource()==contrastSlider && !contrastSlider.getValueIsAdjusting())
                    contrastSlider.applyContrastToImage(image);
            }
        }
    }

}
