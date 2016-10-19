package edu.jhuapl.sbmt.model.rosetta;

import edu.jhuapl.saavtk.util.IntensityRange;
import edu.jhuapl.sbmt.gui.image.ImageInfoPanel;
import edu.jhuapl.sbmt.model.image.Image;
import edu.jhuapl.sbmt.model.image.ImageCollection;
import edu.jhuapl.sbmt.model.image.PerspectiveImageBoundaryCollection;

public class OsirisImageInfoPanel extends ImageInfoPanel
{

    public OsirisImageInfoPanel(Image image, ImageCollection imageCollection,
            PerspectiveImageBoundaryCollection imageBoundaryCollection)
    {
        super(image, imageCollection, imageBoundaryCollection);
        IntensityRange range=((OsirisImage)image).getDisplayedRange();
        slider.setLowValue(range.min);
        slider.setHighValue(range.max);
    }

}
