package edu.jhuapl.sbmt.model;

import java.awt.Dimension;
import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import com.beust.jcommander.internal.Lists;

import edu.jhuapl.saavtk.gui.panel.PolyhedralModelControlPanel;
import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.gui.util.IconUtil;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.model.ModelNames;
import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.pick.PickUtil;
import edu.jhuapl.sbmt.config.SmallBodyViewConfig;
import edu.jhuapl.sbmt.core.body.BodyViewConfig;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.core.config.IFeatureConfig;
import edu.jhuapl.sbmt.core.config.ISmallBodyViewConfig;
import edu.jhuapl.sbmt.core.util.PolyDataUtil2;
import edu.jhuapl.sbmt.core.util.PolyDataUtil2.PolyDataStatistics;
import edu.jhuapl.sbmt.image.config.BasemapImageConfig;
import edu.jhuapl.sbmt.image.interfaces.IPerspectiveImage;
import edu.jhuapl.sbmt.image.interfaces.IPerspectiveImageTableRepresentable;
import edu.jhuapl.sbmt.image.interfaces.ImageKeyInterface;
import edu.jhuapl.sbmt.image.model.BasemapImage;
import edu.jhuapl.sbmt.image.model.BasemapImageCollection;
import edu.jhuapl.sbmt.image.model.Image;

import glum.gui.GuiUtil;
import net.miginfocom.swing.MigLayout;

public class SmallBodyControlPanel extends PolyhedralModelControlPanel implements ItemListener
{
    private static final long serialVersionUID = 518373430237465750L;
    private static final String IMAGE_MAP_TEXT = "Show Image Map";
    private static final String EMPTY_SELECTION = "None";

    @Deprecated
    private final Map<String, ImageKeyInterface> imageMapKeysMap;
    private final Map<String, BasemapImage> basemaps;
    private final JCheckBox imageMapCheckBox;
    private final JComboBox<String> imageMapComboBox;
    private final JSpinner imageMapOpacitySpinner;
    private final JLabel opacityLabel;
    private final List<OpacityChangeListener> imageChangeListeners;
    private final List<BasemapOpacityChangeListener> basemapImageChangeListeners;
    private JButton basemapInfoButton;
    private List<ImageKeyInterface> imageMapKeys = Lists.newArrayList();
    private List<BasemapImage> basemapImages = Lists.newArrayList();

    public SmallBodyControlPanel(Renderer aRenderer, ModelManager modelManager, String bodyName)
    {
        super(aRenderer, modelManager, bodyName);
        this.imageMapKeysMap = new HashMap<>();
        this.basemaps = new HashMap<>();
        this.imageChangeListeners = new ArrayList<>();
        this.basemapImageChangeListeners = new ArrayList<>();

        SmallBodyModel smallBodyModel = (SmallBodyModel) modelManager.getPolyhedralModel();
        ISmallBodyViewConfig config = (SmallBodyViewConfig)smallBodyModel.getConfig();
        List<IFeatureConfig> basemapConfigs = ((List<IFeatureConfig>)config.getFeatureConfigs().get(BasemapImageConfig.class));
        if (basemapConfigs != null)
        {
	        BasemapImageConfig basemapConfig = (BasemapImageConfig)basemapConfigs.get(0);
	        basemapConfig.setConfig((BodyViewConfig)config);
	        imageMapKeys = basemapConfig.getImageMapKeys();
	        basemapImages = basemapConfig.getBasemapImages();
        }

        //try the new basemap version first; if that fails, look for older basemap config; otherwise don't present the UI
        if ((basemapImages != null) && !basemapImages.isEmpty())
        {
            imageMapCheckBox = configureImageMapCheckBox(smallBodyModel);
            imageMapComboBox = configureImageMapComboBox(smallBodyModel);
            opacityLabel = new JLabel("Image opacity");
            imageMapOpacitySpinner = createOpacitySpinner();
        }
        else if ((imageMapKeys != null) && !imageMapKeys.isEmpty())
        {
            imageMapCheckBox = configureImageMapCheckBox(smallBodyModel);
            imageMapComboBox = configureImageMapComboBoxOld(smallBodyModel);
            opacityLabel = new JLabel("Image opacity");
            imageMapOpacitySpinner = createOpacitySpinner();
        }
        else
        {
            imageMapCheckBox = null;
            imageMapComboBox = null;
            imageMapOpacitySpinner = null;
            opacityLabel = null;
        }

        initialize();
    }

    @Override
	protected void addCustomControls(JPanel panel)
    {
        SmallBodyModel smallBodyModel = (SmallBodyModel) getModelManager().getPolyhedralModel();

        if (!basemapImages.isEmpty())
        {
        	if (basemaps.size() > 0)
        		imageMapOpacitySpinner.addChangeListener(new BasemapChangedListener());
        	else
        		imageMapOpacitySpinner.addChangeListener(this);
            opacityLabel.setEnabled(false);
            imageMapOpacitySpinner.setEnabled(false);
            panel.add(GuiUtil.createDivider(), "growx,h 4!,span,wrap");
            if (imageMapComboBox != null)
            {
                panel.add(new JLabel(IMAGE_MAP_TEXT), "wrap");
                JPanel comboBoxPanel = new JPanel(new MigLayout("wrap 2", "[grow]", ""));
                comboBoxPanel.add(imageMapComboBox);
                if (basemaps.size() > 0)
                {
	                basemapInfoButton = GuiUtil.formButton(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							JOptionPane.showMessageDialog(null, basemaps.get(imageMapComboBox.getSelectedItem()).getImageDescription(), "Basemap Properties", JOptionPane.INFORMATION_MESSAGE);

						}
					}, IconUtil.getActionInfo());
	                basemapInfoButton.setEnabled(false);
	                comboBoxPanel.add(basemapInfoButton, "wrap");
                }
                panel.add(comboBoxPanel);
            }
            else
            {
                panel.add(imageMapCheckBox, "wrap");
            }
            panel.add(opacityLabel, "gapleft 25, split 2");
            panel.add(imageMapOpacitySpinner, "wrap");
        }
    }

    @Deprecated
    private ImageKeyInterface getCurrentImageMapKey()
    {
        ImageKeyInterface result = null;

        if (imageMapComboBox != null)
        {
            Object selection = imageMapComboBox.getSelectedItem();
            if (selection != null)
            {
                result = imageMapKeysMap.get(selection);
            }
        }
        else if (!imageMapKeys.isEmpty())
        {
            result = imageMapKeysMap.values().iterator().next();
        }

        return result;
    }

    private BasemapImage getCurrentBasemap()
    {
    	BasemapImage image = null;
    	if (imageMapComboBox != null)
        {
            Object selection = imageMapComboBox.getSelectedItem();
            if (selection != null)
            {
                image = basemaps.get(selection);
            }
        }
        else if (!imageMapKeys.isEmpty())
        {
            image = basemaps.values().iterator().next();
        }

    	return image;
    }

    public JSpinner getImageMapOpacitySpinner()
    {
        return imageMapOpacitySpinner;
    }

    protected boolean isImageMapEnabled()
    {
        if (imageMapComboBox != null)
            return !EMPTY_SELECTION.equals(imageMapComboBox.getSelectedItem());
        return false;
//        return imageMapCheckBox.isSelected();
    }

    public JLabel getOpacityLabel()
    {
        return opacityLabel;
    }



    @Deprecated
    public void itemStateChanged(ItemEvent e)
    {
        PickUtil.setPickingEnabled(false);

        ItemSelectable selectedItem = e.getItemSelectable();
        if (selectedItem == imageMapCheckBox)
        {
            boolean show = imageMapCheckBox.isSelected();
            showImageMap(getCurrentImageMapKey(), show);
        }
        else if (selectedItem == this.imageMapComboBox)
        {
            String item = (String) e.getItem();
            boolean show = e.getStateChange() == ItemEvent.SELECTED;
            showImageMap(imageMapKeysMap.get(item), show);
        }
        else
        {
            PickUtil.setPickingEnabled(true);
        }
    }

    private final JComboBox<String> configureImageMapComboBox(SmallBodyModel model)
    {
        JComboBox<String> result = null;
//        List<BasemapImage> maps = model.getBasemaps();
        List<BasemapImage> maps = basemapImages;
        if (maps.size() > 0)
        {
            String[] allOptions = new String[maps.size() + 1];
            int index = 0;
            allOptions[index] = EMPTY_SELECTION;
            basemaps.put(EMPTY_SELECTION, null);
            for (; index < maps.size(); ++index)
            {
                BasemapImage key = maps.get(index);
                String name = key.getImageName();
                allOptions[index + 1] = name;
                basemaps.put(name, key);
            }
            result = new JComboBox<>(allOptions);
            result.addItemListener(new BasemapItemChangedListener());
        }

        return result;
    }

    @Deprecated
    private final JCheckBox configureImageMapCheckBox(SmallBodyModel model)
    {
        JCheckBox result = null;
        List<ImageKeyInterface> mapKeys = imageMapKeys; //model.getImageMapKeys();
        if (mapKeys.size() == 1)
        {
            ImageKeyInterface key = mapKeys.get(0);
            imageMapKeysMap.put(key.getOriginalName(), key);

            result = new JCheckBox();
            result.setText(IMAGE_MAP_TEXT);
            result.setSelected(false);
            result.addItemListener(this);
        }

        return result;
    }

    @Deprecated
    private final JComboBox<String> configureImageMapComboBoxOld(SmallBodyModel model)
    {
        JComboBox<String> result = null;
        List<ImageKeyInterface> mapKeys = imageMapKeys; //model.getImageMapKeys();
        if (mapKeys.size() > 1)
        {
            String[] allOptions = new String[mapKeys.size() + 1];
            int index = 0;
            allOptions[index] = EMPTY_SELECTION;
            imageMapKeysMap.put(EMPTY_SELECTION, null);
            for (; index < mapKeys.size(); ++index)
            {
                ImageKeyInterface key = mapKeys.get(index);
                String name = key.getOriginalName();
                allOptions[index + 1] = name;
                imageMapKeysMap.put(name, key);
            }
            result = new JComboBox<>(allOptions);
            result.addItemListener(this);
        }

        return result;
    }

    @Deprecated
    protected <G1 extends IPerspectiveImage & IPerspectiveImageTableRepresentable> void showImageMap(ImageKeyInterface key, boolean show)
    {
//        if (key == null) return;
//
//        try
//        {
//        	 ImageCollection imageCollection = (ImageCollection) getModelManager().getModel(ModelNames.IMAGES);
//             Image image = imageCollection.getImage(key);
//
//             if (show && image == null)
//             {
//                 // The first time this image is displayed, need to load it.
//                 setCursor(new Cursor(Cursor.WAIT_CURSOR));
//
//                 imageCollection.addImage(key);
//                 image = imageCollection.getImage(key);
//                 imageMapOpacitySpinner.setValue(image.getOpacity());
//                 image.addPropertyChangeListener(new OpacityChangeListener(image));
//             }
//
//             if (image != null)
//             {
//                 image.setVisible(show);
//             }
//             opacityLabel.setEnabled(show);
//             imageMapOpacitySpinner.setEnabled(show);
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//        finally
//        {
//            setCursor(Cursor.getDefaultCursor());
//        }
    }

    private <G1 extends IPerspectiveImage & IPerspectiveImageTableRepresentable> void showBasemap(BasemapImage image, boolean show)
    {
    	BasemapImageCollection<G1> imageCollection = (BasemapImageCollection<G1>) getModelManager().getModel(ModelNames.BASEMAPS);
    	clearBasemaps();
    	G1 perImage = imageCollection.addImage(image);
    	imageCollection.setImageMapped(perImage, show);
    }

    private <G1 extends IPerspectiveImage & IPerspectiveImageTableRepresentable> void clearBasemaps()
    {
    	BasemapImageCollection<G1> imageCollection = (BasemapImageCollection<G1>) getModelManager().getModel(ModelNames.BASEMAPS);
    	for (G1 image : imageCollection.getAllItems())
    		imageCollection.setImageMapped(image, false);
    }

    @Override
    public void stateChanged(@SuppressWarnings("unused") ChangeEvent e)
    {
//        ImageCollection imageCollection =
//                (ImageCollection)getModelManager().getModel(ModelNames.IMAGES);
//
//        double val = (Double)getImageMapOpacitySpinner().getValue();
//
//        ImageKeyInterface key = getCurrentImageMapKey();
//        if (key != null)
//        {
//            CylindricalImage image = (CylindricalImage)imageCollection.getImage(key);
//            image.setOpacity(val);
//        }
    }

    @Override
    protected void addAdditionalStatisticsToLabel()
    {
        PolyhedralModel smallBodyModel = getModelManager().getPolyhedralModel();
        Double refPotential = smallBodyModel.getReferencePotential();
        PolyDataStatistics stat = PolyDataUtil2.getPolyDataStatistics(smallBodyModel.getSmallBodyPolyData());
        String refPotentialString = refPotential != Double.MAX_VALUE ? String.valueOf(refPotential) : "(not available)";

        String newText =
                "&nbsp;&nbsp;&nbsp;Number of Edges: " + stat.numberEdges + " <sup>&nbsp;</sup><br>"
                + "&nbsp;&nbsp;&nbsp;Reference Potential: " + refPotentialString + " J/kg<sup>&nbsp;</sup><br>"
                + "&nbsp;&nbsp;&nbsp;Plate Area Standard Deviation: " + String.format("%.7g", 1.0e6 * stat.stdCellArea) + " m<sup>2</sup><br>"
                + "&nbsp;&nbsp;&nbsp;Edge Length Average: " + String.format("%.7g", 1.0e3 * stat.meanEdgeLength) + " m<sup>&nbsp;</sup><br>"
                + "&nbsp;&nbsp;&nbsp;Edge Length Minimum: " + String.format("%.7g", 1.0e3 * stat.minEdgeLength) + " m<sup>&nbsp;</sup><br>"
                + "&nbsp;&nbsp;&nbsp;Edge Length Maximum: " + String.format("%.7g", 1.0e3 * stat.maxEdgeLength) + " m<sup>&nbsp;</sup><br>"
                + "&nbsp;&nbsp;&nbsp;Edge Length Standard Deviation: " + String.format("%.7g", 1.0e3 * stat.stdEdgeLength) + " m<sup>&nbsp;</sup><br>"
                + "&nbsp;&nbsp;&nbsp;Is Surface Closed? " + (stat.isClosed ? "Yes" : "No") + " <sup>&nbsp;</sup><br>";
                if (stat.isClosed)
                {
                    newText += "&nbsp;&nbsp;&nbsp;Centroid:<sup>&nbsp;</sup><br>"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;[" + String.format("%.7g, %.7g, %.7g", stat.centroid[0], stat.centroid[1], stat.centroid[2]) + "] km<sup>&nbsp;</sup><br>"
                            + "&nbsp;&nbsp;&nbsp;Moment of Inertia Tensor Relative to Origin:<sup>&nbsp;</sup><br>"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;[" + String.format("%.7g, %.7g, %.7g", stat.inertiaWorld[0][0], stat.inertiaWorld[0][1], stat.inertiaWorld[0][2]) + "] <sup>&nbsp;</sup><br>"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;[" + String.format("%.7g, %.7g, %.7g", stat.inertiaWorld[1][0], stat.inertiaWorld[1][1], stat.inertiaWorld[1][2]) + "] <sup>&nbsp;</sup><br>"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;[" + String.format("%.7g, %.7g, %.7g", stat.inertiaWorld[2][0], stat.inertiaWorld[2][1], stat.inertiaWorld[2][2]) + "] <sup>&nbsp;</sup><br>"
                            + "&nbsp;&nbsp;&nbsp;Moment of Inertia Tensor Relative to Centroid:<sup>&nbsp;</sup><br>"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;[" + String.format("%.7g, %.7g, %.7g", stat.inertiaCOM[0][0], stat.inertiaCOM[0][1], stat.inertiaCOM[0][2]) + "] <sup>&nbsp;</sup><br>"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;[" + String.format("%.7g, %.7g, %.7g", stat.inertiaCOM[1][0], stat.inertiaCOM[1][1], stat.inertiaCOM[1][2]) + "] <sup>&nbsp;</sup><br>"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;[" + String.format("%.7g, %.7g, %.7g", stat.inertiaCOM[2][0], stat.inertiaCOM[2][1], stat.inertiaCOM[2][2]) + "] <sup>&nbsp;</sup><br>";
                }
        try
        {

            ((HTMLEditorKit)getStatisticsLabel().getEditorKit()).insertHTML(
                    (HTMLDocument)getStatisticsLabel().getDocument(),
                    getStatisticsLabel().getDocument().getLength(),
                    newText, 0, 0, null);

            final int originalScrollBarValue = getScrollPane().getVerticalScrollBar().getValue();
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    getScrollPane().getVerticalScrollBar().setValue(originalScrollBarValue);
                }
            });
        }
        catch (BadLocationException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static JSpinner createOpacitySpinner()
    {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 1.0, 0.1));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "0.00"));
        spinner.setPreferredSize(new Dimension(80, 21));
        return spinner;
    }

    protected class OpacityChangeListener implements PropertyChangeListener {
        private final Image image;

        OpacityChangeListener(Image image) {
            this.image = image;
            imageChangeListeners.add(this);
        }

        @Override
        public void propertyChange(@SuppressWarnings("unused") PropertyChangeEvent evt)
        {
            getImageMapOpacitySpinner().setValue(image.getOpacity());
        }

    }

    class BasemapChangedListener<G1 extends IPerspectiveImage & IPerspectiveImageTableRepresentable> implements ChangeListener
    {
        @Override
        public void stateChanged(@SuppressWarnings("unused") ChangeEvent e)
        {
            BasemapImageCollection<G1> imageCollection = (BasemapImageCollection<G1>) getModelManager().getModel(ModelNames.BASEMAPS);
            double val = (Double)getImageMapOpacitySpinner().getValue();
            imageCollection.setOpacity(val);
        }
    }

	protected class BasemapOpacityChangeListener<G1 extends IPerspectiveImage & IPerspectiveImageTableRepresentable> implements PropertyChangeListener
	{
		private BasemapImageCollection<G1> imageCollection;

		BasemapOpacityChangeListener(BasemapImageCollection<G1> imageCollection)
		{
			this.imageCollection = imageCollection;
			basemapImageChangeListeners.add(this);
		}

		@Override
		public void propertyChange(@SuppressWarnings("unused") PropertyChangeEvent evt)
		{
			getImageMapOpacitySpinner().setValue(imageCollection.getOpacity());
		}

	}

    class BasemapItemChangedListener<G1 extends IPerspectiveImage & IPerspectiveImageTableRepresentable> implements ItemListener
    {

        @Override
        public void itemStateChanged(ItemEvent e)
        {
        	if (e.getStateChange() != ItemEvent.SELECTED) return;
            PickUtil.setPickingEnabled(false);

            ItemSelectable selectedItem = e.getItemSelectable();
            /*if (selectedItem == imageMapCheckBox)
            {
                boolean show = imageMapCheckBox.isSelected();
                showBasemap(getCurrentBasemap(), show);
            }
            else*/ if (selectedItem == imageMapComboBox)
            {
                String item = (String) e.getItem();
                boolean show = e.getStateChange() == ItemEvent.SELECTED;
                if (item.equals("None"))
                {
                	clearBasemaps();
                	basemapInfoButton.setEnabled(false);
                	opacityLabel.setEnabled(false);
                    imageMapOpacitySpinner.setEnabled(false);
                }
                else
            	{
                	showBasemap(basemaps.get(item), show);
                	basemapInfoButton.setEnabled(true);
                	opacityLabel.setEnabled(true);
                    imageMapOpacitySpinner.setEnabled(true);
            	}
            }
            else
            {
                PickUtil.setPickingEnabled(true);
            }
        }
    }
}
