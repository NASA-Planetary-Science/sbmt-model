package edu.jhuapl.sbmt.model.spectrum;

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
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.spectrum.Spectrum.SpectrumKey;

import nom.tam.fits.FitsException;

public class SpectrumBoundaryCollection extends AbstractModel implements PropertyChangeListener
{
    private HashMap<SpectrumBoundary, List<vtkProp>> boundaryToActorsMap = new HashMap<SpectrumBoundary, List<vtkProp>>();
    private HashMap<vtkProp, SpectrumBoundary> actorToBoundaryMap = new HashMap<vtkProp, SpectrumBoundary>();
    private SmallBodyModel smallBodyModel;
    // Create a buffer of initial boundary colors to use. We cycle through these colors when creating new boundaries
    private Color[] initialColors = {Color.RED, Color.PINK.darker(), Color.ORANGE.darker(),
            Color.GREEN.darker(), Color.MAGENTA, Color.CYAN.darker(), Color.BLUE,
            Color.GRAY, Color.DARK_GRAY, Color.BLACK};
    private int initialColorIndex = 0;

    public SpectrumBoundaryCollection(SmallBodyModel smallBodyModel)
    {
        this.smallBodyModel = smallBodyModel;
    }

    protected SpectrumBoundary createBoundary(
            SpectrumKey key,
            SmallBodyModel smallBodyModel, SpectraCollection collection) throws IOException, FitsException
    {
        Spectrum spectrum = collection.getSpectrumFromKey(key);
        SpectrumBoundary boundary = new SpectrumBoundary(spectrum, smallBodyModel);
        boundary.setBoundaryColor(initialColors[initialColorIndex++]);
        if (initialColorIndex >= initialColors.length)
            initialColorIndex = 0;
        return boundary;
    }

    private boolean containsKey(SpectrumKey key)
    {
        for (SpectrumBoundary boundary : boundaryToActorsMap.keySet())
        {
            if (boundary.getKey().equals(key))
                return true;
        }

        return false;
    }

    private SpectrumBoundary getBoundaryFromKey(SpectrumKey key)
    {
        for (SpectrumBoundary boundary : boundaryToActorsMap.keySet())
        {
            if (boundary.getKey().equals(key))
                return boundary;
        }

        return null;
    }


    public void addBoundary(SpectrumKey key, SpectraCollection collection) throws FitsException, IOException
    {
        if (containsKey(key))
            return;

        SpectrumBoundary boundary = createBoundary(key, smallBodyModel, collection);

        smallBodyModel.addPropertyChangeListener(boundary);
        boundary.addPropertyChangeListener(this);

        boundaryToActorsMap.put(boundary, new ArrayList<vtkProp>());

        List<vtkProp> boundaryPieces = boundary.getProps();

        boundaryToActorsMap.get(boundary).addAll(boundaryPieces);

        for (vtkProp act : boundaryPieces)
            actorToBoundaryMap.put(act, boundary);
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void removeBoundary(SpectrumKey key)
    {
        SpectrumBoundary boundary = getBoundaryFromKey(key);

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
        HashMap<SpectrumBoundary, List<vtkProp>> map = (HashMap<SpectrumBoundary, List<vtkProp>>)boundaryToActorsMap.clone();
        for (SpectrumBoundary boundary : map.keySet())
            removeBoundary(boundary.getKey());
    }

    public List<vtkProp> getProps()
    {
        System.out.println("SpectrumBoundaryCollection: getProps: getting props, number is " + actorToBoundaryMap.keySet().size());
        return new ArrayList<vtkProp>(actorToBoundaryMap.keySet());
    }

    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        SpectrumBoundary boundary = actorToBoundaryMap.get(prop);
        if(boundary == null)
        {
            return "";
        }
        File file = new File(boundary.getKey().name);
        return "Boundary of image " + file.getName();
    }

    public String getBoundaryName(vtkActor actor)
    {
        return actorToBoundaryMap.get(actor).getKey().name;
    }

    public ImmutableSet<SpectrumKey> getSpectrumKeys()
    {
        ImmutableSet.Builder<SpectrumKey> builder = ImmutableSet.builder();
        for (SpectrumBoundary boundary : boundaryToActorsMap.keySet())
        {
            builder.add(boundary.getKey());
        }
        return builder.build();
    }

    public SpectrumBoundary getBoundary(vtkActor actor)
    {
        return actorToBoundaryMap.get(actor);
    }

    public SpectrumBoundary getBoundary(SpectrumKey key)
    {
        return getBoundaryFromKey(key);
    }

    public boolean containsBoundary(SpectrumKey key)
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
