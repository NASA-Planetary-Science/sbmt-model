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
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.ImageCube.ImageCubeKey;

public class ImageCubeCollection extends AbstractModel implements PropertyChangeListener
{
    private SmallBodyModel smallBodyModel;

    private ModelManager modelManager;

    private HashMap<ImageCube, List<vtkProp>> imageToActorsMap = new HashMap<ImageCube, List<vtkProp>>();

    private HashMap<vtkProp, ImageCube> actorToImageMap = new HashMap<vtkProp, ImageCube>();

    public ImageCubeCollection(SmallBodyModel smallBodyModel, ModelManager modelManager)
    {
        this.smallBodyModel = smallBodyModel;
        this.modelManager = modelManager;
    }

    protected ImageCube createImage(ImageCubeKey key,
            SmallBodyModel smallBodyModel) throws FitsException, IOException, ImageCube.NoOverlapException
    {
        return new ImageCube(key, smallBodyModel, modelManager);
    }

    private boolean containsKey(ImageCubeKey key)
    {
        for (ImageCube image : imageToActorsMap.keySet())
        {
            if (image.getImageCubeKey().equals(key))
                return true;
        }

        return false;
    }

    private ImageCube getImageFromKey(ImageCubeKey key)
    {
        for (ImageCube image : imageToActorsMap.keySet())
        {
            if (image.getImageCubeKey().equals(key))
                return image;
        }

        return null;
    }

    public void addImage(ImageCubeKey key) throws IOException, FitsException, ImageCube.NoOverlapException
    {
        if (containsKey(key))
            return;

        ImageCube image = createImage(key, smallBodyModel);

        smallBodyModel.addPropertyChangeListener(image);
        image.addPropertyChangeListener(this);

        imageToActorsMap.put(image, new ArrayList<vtkProp>());

        List<vtkProp> imagePieces = image.getProps();

        imageToActorsMap.get(image).addAll(imagePieces);

        for (vtkProp act : imagePieces)
            actorToImageMap.put(act, image);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void removeImage(ImageCubeKey key)
    {
        ImageCube image = getImageFromKey(key);

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

    public Set<ImageCube> getImages()
    {
        return imageToActorsMap.keySet();
    }

    public void removeAllImages()
    {
        HashMap<ImageCube, List<vtkProp>> map = (HashMap<ImageCube, List<vtkProp>>)imageToActorsMap.clone();
        for (ImageCube image : map.keySet())
            removeImage(image.getImageCubeKey());
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

    public ImageCube getImage(vtkActor actor)
    {
        return actorToImageMap.get(actor);
    }

    public ImageCube getImage(ImageCubeKey key)
    {
        return getImageFromKey(key);
    }

    public boolean containsImage(ImageCubeKey key)
    {
        return containsKey(key);
    }

    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        // Get image and show image name in status
        Image pickedImage = actorToImageMap.get(prop);
        if(pickedImage == null)
        {
            return "";
        }
        File file = new File(pickedImage.getImageName());
        String status = "Image Cube " + file.getName();

        // Add on additional information if applicable
        if(pickedImage instanceof ImageCube)
        {
            ImageCube ic = (ImageCube) pickedImage;
            double[] pickedPixel = ic.getPixelFromPoint(pickPosition);
            double[] pixelLocation = {ic.getImageHeight()-1-pickedPixel[0], pickedPixel[1]};
            status += ", " + ic.getClickStatusBarText(null, 0, pixelLocation);
        }

        // Return status message for display
        return status;
    }

//    public void setShowFrustums(boolean b)
//    {
//        for (ColorImage image : imageToActorsMap.keySet())
//            image.setShowFrustum(b);
//
//        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
//    }
}
