package edu.jhuapl.sbmt.model.image;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import vtk.vtkActor;
import vtk.vtkProp;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.ColorImage.ColorImageKey;
import edu.jhuapl.sbmt.model.image.ColorImage.NoOverlapException;

import nom.tam.fits.FitsException;

public class ColorImageCollection extends AbstractModel implements PropertyChangeListener
{
    private SmallBodyModel smallBodyModel;

    private ModelManager modelManager;

    private HashMap<ColorImage, List<vtkProp>> imageToActorsMap = new HashMap<ColorImage, List<vtkProp>>();

    private HashMap<vtkProp, ColorImage> actorToImageMap = new HashMap<vtkProp, ColorImage>();

    private Vector<ColorImage> loadedImages;
    private Vector<ColorImageKey> loadedImageKeys;

    public ColorImageCollection(SmallBodyModel smallBodyModel, ModelManager modelManager)
    {
        this.smallBodyModel = smallBodyModel;
        this.modelManager = modelManager;
        this.loadedImages = new Vector<ColorImage>();
        this.loadedImageKeys = new Vector<ColorImageKey>();
    }

    protected ColorImage createImage(ColorImageKey key,
            SmallBodyModel smallBodyModel) throws FitsException, IOException, NoOverlapException
    {
        return new ColorImage(key, smallBodyModel, modelManager);
    }

    public Set<ColorImage> getImages()
    {
        return imageToActorsMap.keySet();
    }

    private boolean containsKey(ColorImageKey key)
    {
        for (ColorImage image : imageToActorsMap.keySet())
        {
            if (image.getColorKey().equals(key))
                return true;
        }

        return false;
    }

    private ColorImage getImageFromKey(ColorImageKey key)
    {
        for (ColorImage image : imageToActorsMap.keySet())
        {
            if (image.getColorKey().equals(key))
                return image;
        }

        return null;
    }

    public void addImage(ColorImageKey key) throws IOException, FitsException, NoOverlapException
    {
        if (containsKey(key))
            return;

        ColorImage image = createImage(key, smallBodyModel);
        if (!loadedImageKeys.contains(key))
        {
            loadedImageKeys.add(key);
            loadedImages.add(image);
        }
        else
        {
            image = loadedImages.get(loadedImageKeys.indexOf(key));
        }

        smallBodyModel.addPropertyChangeListener(image);
        image.addPropertyChangeListener(this);

        imageToActorsMap.put(image, new ArrayList<vtkProp>());

        List<vtkProp> imagePieces = image.getProps();

        imageToActorsMap.get(image).addAll(imagePieces);

        for (vtkProp act : imagePieces)
            actorToImageMap.put(act, image);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void removeImage(ColorImageKey key)
    {
        ColorImage image = getImageFromKey(key);
//        loadedImages.remove(image);

        List<vtkProp> actors = imageToActorsMap.get(image);

        for (vtkProp act : actors)
            actorToImageMap.remove(act);

        imageToActorsMap.remove(image);

        image.removePropertyChangeListener(this);
        smallBodyModel.removePropertyChangeListener(image);
        //image.setShowFrustum(false);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        this.pcs.firePropertyChange(Properties.MODEL_REMOVED, null, image);
    }

    public void removeAllImages()
    {
        HashMap<ColorImage, List<vtkProp>> map = (HashMap<ColorImage, List<vtkProp>>)imageToActorsMap.clone();
        for (ColorImage image : map.keySet())
            removeImage(image.getColorKey());
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

    public ColorImage getImage(vtkActor actor)
    {
        return actorToImageMap.get(actor);
    }

    public ColorImage getImage(ColorImageKey key)
    {
        return getImageFromKey(key);
    }

    public boolean containsImage(ColorImageKey key)
    {
        return containsKey(key);
    }

//    public void setShowFrustums(boolean b)
//    {
//        for (ColorImage image : imageToActorsMap.keySet())
//            image.setShowFrustum(b);
//
//        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
//    }


    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        // Get image and show image name in status
        Image pickedImage = actorToImageMap.get(prop);
        if(pickedImage == null)
        {
            return "";
        }
        File file = new File(pickedImage.getImageName());
        String status = "Color Image " + file.getName();

        // Return status message for display
        return status;
    }

    public Vector<ColorImage> getLoadedImages()
    {
        return loadedImages;
    }

    public Vector<ColorImageKey> getLoadedImageKeys()
    {
        return loadedImageKeys;
    }
}
