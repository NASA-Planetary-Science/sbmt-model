package edu.jhuapl.sbmt.model.dem;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nom.tam.fits.FitsException;

import vtk.vtkActor;
import vtk.vtkProp;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.model.ModelNames;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.dem.DEM.DEMKey;
import edu.jhuapl.sbmt.model.dem.DEMBoundaryCollection.DEMBoundary;

public class DEMCollection extends AbstractModel implements PropertyChangeListener
{
    private SmallBodyModel smallBodyModel;
    private ModelManager modelManager;

    private HashMap<DEM, List<vtkProp>> demToActorsMap = new HashMap<DEM, List<vtkProp>>();

    private HashMap<vtkProp, DEM> actorToDemMap = new HashMap<vtkProp, DEM>();

    private Map<DEMKey, Integer> demColorMap = new HashMap<DEMKey, Integer>();

    public DEMCollection(SmallBodyModel smallBodyModel, ModelManager modelManager)
    {
        this.smallBodyModel = smallBodyModel;
        this.modelManager = modelManager;
    }

    // Creates a DEM based on a key
    protected DEM createDEM(DEMKey key, SmallBodyModel smallBodyModel) throws FitsException, IOException
    {
        // Check to see if we've already created the DEM through its boundary
        DEMBoundaryCollection demBoundaryCollection = (DEMBoundaryCollection)modelManager.getModel(ModelNames.DEM_BOUNDARY);
        DEMBoundary demBoundary = demBoundaryCollection.getBoundary(key);
        if(demBoundary != null)
        {
            // If boundary exists, use the associated DEM to save time
            return demBoundary.getDEM();
        }
        else
        {
            // If boundary does not exist, then go ahead and create one from scratch (takes time)
            return new DEM(key);
        }
    }

    // Checks if key exists in map
    private boolean containsKey(DEMKey key)
    {
        for (DEM dem : demToActorsMap.keySet())
        {
            if (dem.getKey().equals(key))
                return true;
        }

        return false;
    }

    // Gets the DEM associated with the key, otherwise returns
    // null if key is not in map
    private DEM getDEMFromKey(DEMKey key)
    {
        for (DEM dem : demToActorsMap.keySet())
        {
            if (dem.getKey().equals(key))
                return dem;
        }

        return null;
    }

    public DEM getDEM(vtkActor actor)
    {
        return actorToDemMap.get(actor);
    }

    public DEM getDEM(DEMKey key)
    {
        return getDEMFromKey(key);
    }

    // Gets set of all DEMs stored in the map
    public Set<DEM> getImages()
    {
        return demToActorsMap.keySet();
    }

    public void addDEM(DEMKey key) throws FitsException, IOException
    {
        // Nothing to do if collection already contains this key
        if(containsKey(key))
        {
            return;
        }

        // Create the DEM
        DEM dem = createDEM(key, smallBodyModel);
        smallBodyModel.addPropertyChangeListener(dem);
        dem.addPropertyChangeListener(this);

        demToActorsMap.put(dem, new ArrayList<vtkProp>());

        List<vtkProp> demPieces = dem.getProps();

        demToActorsMap.get(dem).addAll(demPieces);

        for (vtkProp act : demPieces)
        {
            actorToDemMap.put(act, dem);
        }

        demColorMap.put(key, 0);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void removeDEM(DEMKey key)
    {
        // Nothing to remove if key does not exist in map
        if (!containsKey(key))
        {
            return;
        }

        DEM dem = getDEMFromKey(key);

        List<vtkProp> actors = demToActorsMap.get(dem);

        for (vtkProp act : actors)
        {
            actorToDemMap.remove(act);
        }

        demToActorsMap.remove(dem);

        dem.removePropertyChangeListener(this);
        smallBodyModel.removePropertyChangeListener(dem);
        dem.demAboutToBeRemoved();

        demColorMap.remove(key);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        this.pcs.firePropertyChange(Properties.MODEL_REMOVED, null, dem);
    }

    public void removeDEMs()
    {
        HashMap<DEM, List<vtkProp>> map = (HashMap<DEM, List<vtkProp>>)demToActorsMap.clone();
        for (DEM dem : map.keySet())
                removeDEM(dem.getKey());
    }

    public boolean containsDEM(DEMKey key)
    {
        return containsKey(key);
    }

    public List<vtkProp> getProps()
    {
        return new ArrayList<vtkProp>(actorToDemMap.keySet());
    }

    public Map<DEMKey, Integer> getColorMap()
    {
        return demColorMap;
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_CHANGED.equals(evt.getPropertyName()))
            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }
}
