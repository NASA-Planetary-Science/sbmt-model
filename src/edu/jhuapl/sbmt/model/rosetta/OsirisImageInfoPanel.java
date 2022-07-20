package edu.jhuapl.sbmt.model.rosetta;

import edu.jhuapl.saavtk.status.LegacyStatusHandler;
import edu.jhuapl.saavtk.util.IntensityRange;
import edu.jhuapl.sbmt.core.image.Image;
import edu.jhuapl.sbmt.gui.image.ui.images.ImageInfoPanel;
import edu.jhuapl.sbmt.model.image.ImageCollection;

public class OsirisImageInfoPanel extends ImageInfoPanel
{

    public OsirisImageInfoPanel(Image image, ImageCollection imageCollection,
    							LegacyStatusHandler aStatusHandler)
    {

        super(image, imageCollection, aStatusHandler);
        IntensityRange range=((OsirisImage)image).getDisplayedRange();
//        slider.setLowValue(range.min);
//        slider.setHighValue(range.max);
    }

}
