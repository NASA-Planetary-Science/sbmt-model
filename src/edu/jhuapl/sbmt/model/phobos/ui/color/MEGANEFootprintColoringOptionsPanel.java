package edu.jhuapl.sbmt.model.phobos.ui.color;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import com.google.common.collect.ImmutableList;

import edu.jhuapl.saavtk.gui.panel.JComboBoxWithItemState;
import edu.jhuapl.saavtk.model.plateColoring.ColoringData;
import edu.jhuapl.saavtk.model.plateColoring.ColoringDataManager;
import edu.jhuapl.saavtk.model.plateColoring.LoadableColoringData;
import edu.jhuapl.saavtk.util.DownloadableFileManager.StateListener;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.FileStateListenerTracker;

public class MEGANEFootprintColoringOptionsPanel extends JPanel
{
	JComboBoxWithItemState<String> plateColorings = new JComboBoxWithItemState<String>();

	public MEGANEFootprintColoringOptionsPanel(ColoringDataManager coloringDataManager)
	{
		initUI(coloringDataManager);
	}

	private void initUI(ColoringDataManager coloringDataManager)
	{
		configureColoringPanel(coloringDataManager);
	}

	protected final Map<JComboBoxWithItemState<?>, FileStateListenerTracker> listenerTrackers = new HashMap<>();

	/**
	 *
	 */
	private void configureColoringPanel(ColoringDataManager coloringDataManager)
	{
		setBorder(new TitledBorder(null, "Coloring Options", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		//footprint coloring options
		JLabel footprintColoringLabel = new JLabel("Footprint Coloring: ");

		JPanel footprintColoringPanel = new JPanel();
		footprintColoringPanel.setLayout(new BoxLayout(footprintColoringPanel, BoxLayout.X_AXIS));
		footprintColoringPanel.add(footprintColoringLabel);

		ImmutableList<Integer> resolutions = coloringDataManager.getResolutions();
		int newResolutionLevel = 0;
		int numberElements = resolutions.size() > newResolutionLevel ? resolutions.get(newResolutionLevel) : -1;

		// Store the current selection and number of items in the combo box.
        int previousSelection = plateColorings.getSelectedIndex();

        // Clear the current content.
        plateColorings.setSelectedIndex(-1);
        plateColorings.removeAllItems();

        synchronized (this.listenerTrackers)
        {
            // Get rid of current file access state listeners.
            FileStateListenerTracker boxListeners = listenerTrackers.get(plateColorings);
            if (boxListeners == null)
            {
                boxListeners = FileStateListenerTracker.of(FileCache.instance());
                listenerTrackers.put(plateColorings, boxListeners);
            }
            else
            {
                boxListeners.removeAllStateChangeListeners();
            }
            // Add one item for blank (no coloring).
            plateColorings.addItem("");
            for (String name : coloringDataManager.getNames())
            {
                // Re-add the current colorings.
            	plateColorings.addItem(name);
                if (!coloringDataManager.has(name, numberElements))
                {
                    // This coloring is not available at this resolution. List it but grey it out.
                	plateColorings.setEnabled(name, false);
                }
                else
                {
                	ColoringData coloringData = coloringDataManager.get(name, numberElements);
                    if (coloringData instanceof LoadableColoringData)
                    {
                        String urlString = ((LoadableColoringData) coloringData).getFileId();

                        plateColorings.setEnabled(name, FileCache.instance().isAccessible(urlString));
                        StateListener listener = e -> {
                            plateColorings.setEnabled(name, e.isAccessible());
                        };
                        boxListeners.addStateChangeListener(urlString, listener);
                    }
                }
            }

            int numberColorings = plateColorings.getItemCount();
            int selection = 0;
            if (previousSelection < numberColorings)
            {
                // A coloring was replaced/edited. Re-select the current selection.
                selection = previousSelection;
            }

            plateColorings.setSelectedIndex(selection);
        }

        footprintColoringPanel.add(plateColorings);
		footprintColoringPanel.add(Box.createHorizontalGlue());

		add(footprintColoringPanel);
	}
}
