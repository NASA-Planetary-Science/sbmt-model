package edu.jhuapl.sbmt.model.image;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.ImmutableSet;

import vtk.vtkActor;
import vtk.vtkProp;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.model.ModelNames;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SbmtModelFactory;
import edu.jhuapl.sbmt.client.SmallBodyModel;

import nom.tam.fits.FitsException;

public class PerspectiveImageBoundaryCollection extends AbstractModel implements PropertyChangeListener
{
    private HashMap<PerspectiveImageBoundary, List<vtkProp>> boundaryToActorsMap = new HashMap<PerspectiveImageBoundary, List<vtkProp>>();
    private HashMap<vtkProp, PerspectiveImageBoundary> actorToBoundaryMap = new HashMap<vtkProp, PerspectiveImageBoundary>();
    private SmallBodyModel smallBodyModel;
    // Create a buffer of initial boundary colors to use. We cycle through these colors when creating new boundaries
    private Color[] initialColors = {Color.RED, Color.PINK.darker(), Color.ORANGE.darker(),
            Color.GREEN.darker(), Color.MAGENTA, Color.CYAN.darker(), Color.BLUE,
            Color.GRAY, Color.DARK_GRAY, Color.BLACK};
    private int initialColorIndex = 0;
	private ModelManager modelManager;
	private ModelNames imgCollectionModelName;

    public PerspectiveImageBoundaryCollection(SmallBodyModel smallBodyModel, ModelManager modelManager, ModelNames imgCollectionModelName)
    {
        this.smallBodyModel = smallBodyModel;
        this.modelManager = modelManager;
        this.imgCollectionModelName = imgCollectionModelName;
    }

    protected PerspectiveImageBoundary createBoundary(
            ImageKeyInterface key,
            SmallBodyModel smallBodyModel) throws IOException, FitsException
    {
        ImageCollection images = (ImageCollection)modelManager.getModel(imgCollectionModelName);
    	PerspectiveImage image = (PerspectiveImage) images.getImage(key);
    	if(image ==null) {
            image = (PerspectiveImage)SbmtModelFactory.createImage(key, smallBodyModel, true);
    	}
        PerspectiveImageBoundary boundary = new PerspectiveImageBoundary(image, smallBodyModel);
        Color color = initialColors[initialColorIndex++];
        boundary.setBoundaryColor(color);
        image.setBoundaryColor(color);
        if (initialColorIndex >= initialColors.length)
            initialColorIndex = 0;
        return boundary;
    }

    private boolean containsKey(ImageKeyInterface key)
    {
        for (PerspectiveImageBoundary boundary : boundaryToActorsMap.keySet())
        {
            if (boundary.getKey().equals(key))
                return true;
        }

        return false;
    }

    private PerspectiveImageBoundary getBoundaryFromKey(ImageKeyInterface key)
    {
        for (PerspectiveImageBoundary boundary : boundaryToActorsMap.keySet())
        {
            if (boundary.getKey().equals(key))
                return boundary;
        }

        return null;
    }


    public void addBoundary(ImageKeyInterface key) throws FitsException, IOException
    {
        if (containsKey(key))
            return;

        PerspectiveImageBoundary boundary = createBoundary(key, smallBodyModel);

        smallBodyModel.addPropertyChangeListener(boundary);
        boundary.addPropertyChangeListener(this);

        boundaryToActorsMap.put(boundary, new ArrayList<vtkProp>());

        List<vtkProp> boundaryPieces = boundary.getProps();

        boundaryToActorsMap.get(boundary).addAll(boundaryPieces);

        for (vtkProp act : boundaryPieces)
            actorToBoundaryMap.put(act, boundary);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void removeBoundary(ImageKeyInterface key)
    {
        PerspectiveImageBoundary boundary = getBoundaryFromKey(key);

        if(boundary != null)
        {
            List<vtkProp> actors = boundaryToActorsMap.get(boundary);

            if(actors != null)
            {
                for (vtkProp act : actors)
                    actorToBoundaryMap.remove(act);
            }

            boundaryToActorsMap.remove(boundary);

            boundary.removePropertyChangeListener(this);
            smallBodyModel.removePropertyChangeListener(boundary);

            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }

    public void removeAllBoundaries()
    {
        HashMap<PerspectiveImageBoundary, List<vtkProp>> map = (HashMap<PerspectiveImageBoundary, List<vtkProp>>)boundaryToActorsMap.clone();
        for (PerspectiveImageBoundary boundary : map.keySet())
            removeBoundary(boundary.getKey());
    }

    public List<vtkProp> getProps()
    {
//        System.out
//                .println("PerspectiveImageBoundaryCollection: getProps: getting props, number is " + actorToBoundaryMap.keySet().size());
        return new ArrayList<vtkProp>(actorToBoundaryMap.keySet());
    }

    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        PerspectiveImageBoundary boundary = actorToBoundaryMap.get(prop);
        if(boundary == null)
        {
            return "";
        }
        File file = new File(boundary.getKey().getName());
        return "Boundary of image " + file.getName();
    }

    public String getBoundaryName(vtkActor actor)
    {
        return actorToBoundaryMap.get(actor).getKey().getName();
    }

    public ImmutableSet<ImageKeyInterface> getImageKeys()
    {
        ImmutableSet.Builder<ImageKeyInterface> builder = ImmutableSet.builder();
        for (PerspectiveImageBoundary boundary : boundaryToActorsMap.keySet())
        {
            builder.add(boundary.getKey());
        }
        return builder.build();
    }

    public PerspectiveImageBoundary getBoundary(vtkActor actor)
    {
        return actorToBoundaryMap.get(actor);
    }

    public PerspectiveImageBoundary getBoundary(ImageKeyInterface key)
    {
        return getBoundaryFromKey(key);
    }

    public boolean containsBoundary(ImageKeyInterface key)
    {
        return containsKey(key);
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_CHANGED.equals(evt.getPropertyName()))
        {
            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }
}
