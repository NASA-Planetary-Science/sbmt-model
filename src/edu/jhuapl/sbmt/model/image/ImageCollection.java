package edu.jhuapl.sbmt.model.image;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import nom.tam.fits.FitsException;

import vtk.vtkActor;
import vtk.vtkProp;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SbmtModelFactory;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.Image.ImageKey;

public class ImageCollection extends AbstractModel implements PropertyChangeListener
{
    private SmallBodyModel smallBodyModel;

    private HashMap<Image, List<vtkProp>> imageToActorsMap = new HashMap<Image, List<vtkProp>>();

    private HashMap<vtkProp, Image> actorToImageMap = new HashMap<vtkProp, Image>();

    public ImageCollection(SmallBodyModel smallBodyModel)
    {
        this.smallBodyModel = smallBodyModel;
    }

    protected Image createImage(ImageKey key, SmallBodyModel smallBodyModel) throws FitsException, IOException
    {
        return SbmtModelFactory.createImage(key, smallBodyModel, false);
    }

    private boolean containsKey(ImageKey key)
    {
        for (Image image : imageToActorsMap.keySet())
        {
            if (image.getKey().equals(key))
                return true;
        }

        return false;
    }

    private Image getImageFromKey(ImageKey key)
    {
        for (Image image : imageToActorsMap.keySet())
        {
            if (image.getKey().equals(key))
                return image;
        }

        return null;
    }

    public Set<Image> getImages()
    {
        return imageToActorsMap.keySet();
    }

    public void addImage(ImageKey key) throws FitsException, IOException
    {
        if (containsKey(key))
            return;

        Image image = createImage(key, smallBodyModel);

        smallBodyModel.addPropertyChangeListener(image);
        image.addPropertyChangeListener(this);

        imageToActorsMap.put(image, new ArrayList<vtkProp>());

        List<vtkProp> imagePieces = image.getProps();

        imageToActorsMap.get(image).addAll(imagePieces);

        for (vtkProp act : imagePieces)
            actorToImageMap.put(act, image);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void removeImage(ImageKey key)
    {
        if (!containsKey(key))
            return;

        Image image = getImageFromKey(key);

        List<vtkProp> actors = imageToActorsMap.get(image);

        for (vtkProp act : actors)
            actorToImageMap.remove(act);

        imageToActorsMap.remove(image);

        image.removePropertyChangeListener(this);
        smallBodyModel.removePropertyChangeListener(image);
        image.imageAboutToBeRemoved();

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        this.pcs.firePropertyChange(Properties.MODEL_REMOVED, null, image);
    }

    /**
     * Remove all images of the specified source
     * @param source
     */
    public void removeImages(ImageSource source)
    {
        HashMap<Image, List<vtkProp>> map = (HashMap<Image, List<vtkProp>>)imageToActorsMap.clone();
        for (Image image : map.keySet())
            if (image.getKey().source == source)
                removeImage(image.getKey());
    }

    public List<vtkProp> getProps()
    {
        return new ArrayList<vtkProp>(actorToImageMap.keySet());
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_CHANGED.equals(evt.getPropertyName()))
            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        File file = new File(actorToImageMap.get(prop).getImageName());
        return "Image " + file.getName();
    }

    public String getImageName(vtkActor actor)
    {
        return actorToImageMap.get(actor).getImageName();
    }

    public Image getImage(vtkActor actor)
    {
        return actorToImageMap.get(actor);
    }

    public Image getImage(ImageKey key)
    {
        return getImageFromKey(key);
    }

    public boolean containsImage(ImageKey key)
    {
        return containsKey(key);
    }
}
