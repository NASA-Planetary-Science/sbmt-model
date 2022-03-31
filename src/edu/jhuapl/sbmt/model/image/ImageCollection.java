package edu.jhuapl.sbmt.model.image;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import vtk.vtkActor;
import vtk.vtkProp;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SbmtModelFactory;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.perspectiveImage.PerspectiveImage;

import nom.tam.fits.FitsException;

public class ImageCollection extends AbstractModel implements PropertyChangeListener
{
    private SmallBodyModel smallBodyModel;
    private List<SmallBodyModel> smallBodyModels;

    private HashMap<Image, List<vtkProp>> imageToActorsMap = new HashMap<Image, List<vtkProp>>();

    private HashMap<vtkProp, Image> actorToImageMap = new HashMap<vtkProp, Image>();

//    private ModelManager modelManager;

    public ImageCollection(List<SmallBodyModel> smallBodyModels)
    {
        this.smallBodyModels = smallBodyModels;
        this.smallBodyModel = smallBodyModels.get(0);
//        this.modelManager = modelManager;
    }

    protected Image createImage(ImageKeyInterface key) throws FitsException, IOException
    {
//    	List<SmallBodyModel> smallBodyModels = modelManager.getModel(ModelNames.SMALL_BODY).stream().map(body -> { return (SmallBodyModel)body; }).toList();
        return SbmtModelFactory.createImage(key, smallBodyModels, false);
    }

    private boolean containsKey(ImageKeyInterface key)
    {
        for (Image image : imageToActorsMap.keySet())
        {
            if (image.getKey().equals(key))
                return true;
        }

        return false;
    }

    private Image getImageFromKey(ImageKeyInterface key)
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

    public boolean isImageMapped(Image image)
    {
    	return getImages().contains(image);
    }

    public boolean getVisibility(Image image)
    {
    	if (getImages().contains(image))
    	{
    		return image.isVisible();
    	}
    	return false;
    }

    public boolean getOfflimbVisibility(Image image)
    {
    	if (getImages().contains(image))
    	{
    		return image.isVisible();
    	}
    	return false;
    }

    public boolean getFrustumVisibility(Image image)
    {
    	if (getImages().contains(image))
    	{
    		return image.isFrustumShowing();
    	}
    	return false;
    }

    public void addImage(ImageKeyInterface key) throws FitsException, IOException
    {
        if (containsKey(key))
        {
            return;
        }

//        Stopwatch sw = Stopwatch.createUnstarted();
//        sw.start();
        Image image = createImage(key);
//        System.out.println("ImageCollection: addImage: created image in " + sw.elapsedMillis() + " ms");
        image.setBoundaryColor(new Color((int)(Math.random() * 0x1000000)));

        for (SmallBodyModel smallBodyModel : smallBodyModels)
        {
	        smallBodyModel.addPropertyChangeListener(image);
	        image.addPropertyChangeListener(this);
	//        System.out.println("ImageCollection: addImage: putting image in imageToActorsMap " + sw.elapsedMillis() + " ms");

	        imageToActorsMap.put(image, new ArrayList<vtkProp>());
	//        System.out.println("ImageCollection: addImage: getting props " + sw.elapsedMillis() + " ms");

	        List<vtkProp> imagePieces = image.getProps();
	//        System.out.println("ImageCollection: addImage: building actor to image map " + sw.elapsedMillis() + " ms");

	        imageToActorsMap.get(image).addAll(imagePieces);
	//        System.out.println("ImageCollection: addImage: building image to actor map " + sw.elapsedMillis() + " ms");

	        for (vtkProp act : imagePieces)
	            actorToImageMap.put(act, image);
        }
//        System.out.println("ImageCollection: addImage: firing listener " + sw.elapsedMillis() + " ms");
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, image);
//        System.out.println("ImageCollection: addImage: fired listener " + sw.elapsedMillis() + " ms");

    }

    public void removeImage(ImageKeyInterface key)
    {
        if (!containsKey(key))
            return;
        Image image = getImageFromKey(key);

        List<vtkProp> actors = imageToActorsMap.get(image);

        for (vtkProp act : actors)
        {
        	act.SetVisibility(0);
            actorToImageMap.remove(act);
        }

        imageToActorsMap.remove(image);

        image.removePropertyChangeListener(this);
        smallBodyModel.removePropertyChangeListener(image);
        image.imageAboutToBeRemoved();

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, image);
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
            if (image.getKey().getSource() == source)
                removeImage(image.getKey());
    }

    public List<vtkProp> getProps()
    {
        return new ArrayList<vtkProp>(actorToImageMap.keySet());
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_CHANGED.equals(evt.getPropertyName()))
            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, evt.getNewValue());
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
        String status = "Image " + file.getName();

        // Add on additional information if applicable
        if(pickedImage instanceof PerspectiveImage)
        {
            PerspectiveImage pi = (PerspectiveImage) pickedImage;
            double[] pickedPixel = pi.getPixelFromPoint(pickPosition);
            double[] pixelLocation = {pi.getImageHeight()-1-pickedPixel[0], pickedPixel[1]};
            status += ", " + pi.getClickStatusBarText(null, 0, pixelLocation);
        }

        // Return status message for display
        return status;
    }

    public String getImageName(vtkActor actor)
    {
        return actorToImageMap.get(actor).getImageName();
    }

    public Image getImage(vtkActor actor)
    {
        return actorToImageMap.get(actor);
    }

    public Image getImage(ImageKeyInterface key)
    {
        return getImageFromKey(key);
    }

    public boolean containsImage(ImageKeyInterface key)
    {
        return containsKey(key);
    }
}
