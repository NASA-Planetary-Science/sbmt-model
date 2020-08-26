package edu.jhuapl.sbmt.model.rosetta;

import edu.jhuapl.saavtk.gui.StatusBar;
import edu.jhuapl.saavtk.util.IntensityRange;
import edu.jhuapl.sbmt.gui.image.ui.images.ImageInfoPanel;
import edu.jhuapl.sbmt.model.image.Image;
import edu.jhuapl.sbmt.model.image.ImageCollection;

public class OsirisImageInfoPanel extends ImageInfoPanel
{

    public OsirisImageInfoPanel(Image image, ImageCollection imageCollection,
             StatusBar statusBar)
    {
        super(image, imageCollection, statusBar);
        IntensityRange range=((OsirisImage)image).getDisplayedRange();
        slider.setLowValue(range.min);
        slider.setHighValue(range.max);
    }

}
