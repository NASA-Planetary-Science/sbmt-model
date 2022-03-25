package edu.jhuapl.sbmt.model.phobos.ui.color;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Optional;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.jhuapl.saavtk.color.gui.EditGroupColorPanel;
import edu.jhuapl.saavtk.color.gui.SimplePanel;
import edu.jhuapl.saavtk.color.painter.ColorBarPainter;
import edu.jhuapl.saavtk.color.provider.GroupColorProvider;
import edu.jhuapl.saavtk.color.table.ColorTable;
import edu.jhuapl.saavtk.color.table.ColorTableUtil;
import edu.jhuapl.saavtk.colormap.Colormaps;
import edu.jhuapl.saavtk.feature.FeatureType;
import edu.jhuapl.sbmt.model.phobos.model.CumulativeMEGANECollection;
import edu.jhuapl.sbmt.model.phobos.model.MEGANECollection;

import glum.gui.GuiExeUtil;
import glum.gui.component.GComboBox;
import glum.gui.panel.CardPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Interface that defines the methods to allow configuration of ColorProviders
 * (used to render state history data).
 *
 * Originally made for Lidar by lopeznr1
 *
 * @author steelrj1
 */
public class MEGANEFootprintColorConfigPanel extends JPanel implements ActionListener
{
	// Ref vars
	private final ActionListener refListener;

	// GUI vars
	private MEGANEFootprintColorBarPanel colorMapPanel;
	private CardPanel<EditGroupColorPanel> colorPanel;
	private GComboBox<MEGANEFootprintColorMode> colorModeBox;
	private ColorBarPainter tmpCBP;
	private MEGANECollection rendererManager;

	/**
	 * Standard Constructor
	 */
	public MEGANEFootprintColorConfigPanel(ActionListener aListener, MEGANECollection rendererManager, CumulativeMEGANECollection cumulativeCollection)
	{
		refListener = aListener;
		this.rendererManager = rendererManager;
		setLayout(new MigLayout("", "", ""));

		JLabel tmpL = new JLabel("Colorize:");
		colorModeBox = new GComboBox<>(this, MEGANEFootprintColorMode.values());
		add(tmpL);
		add(colorModeBox, "pushx,wrap");

		Optional<ColorTable> defaultColoring = ColorTableUtil.getSystemColorTableList().stream().filter(color -> color.getName().equals(Colormaps.getDefaultColormapName())).findFirst();
		if (defaultColoring.isPresent())
			ColorTableUtil.setSystemColorTableDefault(defaultColoring.get());

		tmpCBP = new ColorBarPainter(rendererManager.getRenderer());

		colorMapPanel = new MEGANEFootprintColorBarPanel(this, rendererManager, cumulativeCollection, tmpCBP);
		colorPanel = new CardPanel<>();
		colorPanel.addCard(MEGANEFootprintColorMode.Simple, new SimplePanel(this, "Footprint", new Color(0.0f, 1.0f, 1.0f)));

		colorPanel.addCard(MEGANEFootprintColorMode.ColorMap, colorMapPanel);
		rendererManager.addListener((aSource, aEventType) -> {
			colorMapPanel.handleItemEvent(aSource, aEventType);
		});

		cumulativeCollection.addListener((aSource, aEventType) -> {
			colorMapPanel.handleItemEvent(aSource, aEventType);
		});

		add(colorPanel, "growx,span,w 50::,wrap 0");
		// Custom initialization code
		Runnable tmpRunnable = () -> {
			colorPanel.getActiveCard().activate(true);
		};
		GuiExeUtil.executeOnceWhenShowing(this, tmpRunnable);
	}

	public void showColorBar(boolean showColorBar)
	{
		if (showColorBar)
		{
			rendererManager.getRenderer().addVtkPropProvider(tmpCBP);
		}
		else
		{
			rendererManager.getRenderer().delVtkPropProvider(tmpCBP);
		}
	}

	/**
	 * Returns the GroupColorProvider that should be used to color data points
	 * associated with the lidar source (spacecraft).
	 */
	public GroupColorProvider getSourceGroupColorProvider()
	{
		EditGroupColorPanel tmpPanel = colorPanel.getActiveCard();
		if (tmpPanel instanceof SimplePanel)
		{
			return ((SimplePanel) tmpPanel).getGroupColorProvider();
		}
		return tmpPanel.getGroupColorProvider();
	}

	public FeatureType getFeatureType()
	{
		EditGroupColorPanel tmpPanel = colorPanel.getActiveCard();
		if (tmpPanel instanceof SimplePanel)
			return null;
		return ((MEGANEFootprintColorBarPanel)tmpPanel).getFeatureType();
	}

	/**
	 * Sets the ColorProviderMode which will be active
	 */
	public void setActiveMode(MEGANEFootprintColorMode aMode)
	{
		colorPanel.getActiveCard().activate(false);
		colorModeBox.setChosenItem(aMode);
		colorPanel.switchToCard(aMode);
		colorPanel.getActiveCard().activate(true);

	}

	@Override
	public void actionPerformed(ActionEvent aEvent)
	{
		Object source = aEvent.getSource();
		if (source == colorModeBox)
			doUpdateColorPanel();

		refListener.actionPerformed(new ActionEvent(this, 0, ""));
	}

	/**
	 * Helper method to properly update the colorPanel.
	 */
	private void doUpdateColorPanel()
	{
		colorPanel.getActiveCard().activate(false);
		MEGANEFootprintColorMode tmpCM = colorModeBox.getChosenItem();
		colorPanel.switchToCard(tmpCM);
		if (tmpCM == MEGANEFootprintColorMode.ColorMap)
			rendererManager.addListener(colorMapPanel);
		else
			rendererManager.delListener(colorMapPanel);
		colorPanel.getActiveCard().activate(true);
	}
}