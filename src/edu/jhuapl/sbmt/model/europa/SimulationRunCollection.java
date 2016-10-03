package edu.jhuapl.sbmt.model.europa;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import vtk.vtkActor;
import vtk.vtkProp;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SbmtModelFactory;
//import edu.jhuapl.sbmt.client.ModelFactory;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.europa.SimulationRun.SimulationRunKey;
import edu.jhuapl.sbmt.model.europa.SimulationRun.SimulationRunSource;

public class SimulationRunCollection extends AbstractModel implements PropertyChangeListener, HasTime
{
    private SmallBodyModel smallBodyModel;

    private List<SimulationRun> simRuns = new ArrayList<SimulationRun>();

    private SimulationRun currentRun = null;

    public SimulationRunCollection(SmallBodyModel smallBodyModel)
    {
        this.smallBodyModel = smallBodyModel;
    }

    protected SimulationRun createRun(SimulationRunKey key, SmallBodyModel smallBodyModel) // throws FitsException, IOException
    {
        try {
            return SbmtModelFactory.createSimulationRun(key, smallBodyModel, false);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean containsKey(SimulationRunKey key)
    {
        for (SimulationRun run : simRuns)
        {
            if (run.getKey().equals(key))
                return true;
        }

        return false;
    }

    private SimulationRun getRunFromKey(SimulationRunKey key)
    {
        for (SimulationRun run : simRuns)
        {
            if (run.getKey().equals(key))
                return run;
        }

        return null;
    }

    public SimulationRun getCurrentRun()
    {
        return currentRun;
    }

    public void setCurrentRun(SimulationRunKey key)
    {
        SimulationRun run = getRunFromKey(key);
        if (run != null && run != currentRun)
        {
            currentRun = run;
        }

    }

    public void addRun(SimulationRunKey key)//  throws FitsException, IOException
    {

        if (containsKey(key))
        {
            this.currentRun = this.getRun(key);
            return;
        }

        SimulationRun run = createRun(key, smallBodyModel);

        // set the current run
        this.currentRun = run;

        run.addPropertyChangeListener(this);

        simRuns.add(run);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void removeRun(SimulationRunKey key)
    {
        if (!containsKey(key))
            return;

        SimulationRun run = getRunFromKey(key);
        simRuns.remove(run);

        // change the current run to the first on the list
//        this.currentRun = simRuns.get(0);
        this.currentRun = null;

        run.removePropertyChangeListener(this);
        run.imageAboutToBeRemoved();

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        this.pcs.firePropertyChange(Properties.MODEL_REMOVED, null, run);
    }

    /**
     * Remove all images of the specified source
     * @param source
     */
    public void removeRuns(SimulationRunSource source)
    {
        for (SimulationRun run : simRuns)
            if (run.getKey().source == source)
                removeRun(run.getKey());
    }

    public void setShowTrajectories(boolean show)
    {
        for (SimulationRun run : simRuns)
            run.setShowTrajectories(show);
    }

    public ArrayList<vtkProp> getProps()
    {
        if (currentRun != null)
            return currentRun.getProps();
        else
            return new ArrayList<vtkProp>();
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_CHANGED.equals(evt.getPropertyName()))
            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        if (currentRun != null)
            return currentRun.getClickStatusBarText(prop, cellId, pickPosition);
        else
            return "No simulation run selected";
    }

    public String getRunName(vtkActor actor)
    {
        if (currentRun != null)
            return currentRun.getKey().name;
        else
            return "No simulation run selected";
    }

    public SimulationRun getRun(vtkActor actor)
    {
        return currentRun;
    }

    public SimulationRun getRun(SimulationRunKey key)
    {
        return getRunFromKey(key);
    }

    public boolean containsRun(SimulationRunKey key)
    {
        return containsKey(key);
    }

    public void setTimeFraction(Double timeFraction)
    {
        if (currentRun != null)
           currentRun.setTimeFraction(timeFraction);
    }

    public Double getTimeFraction()
    {
        if (currentRun!= null)
            return currentRun.getTimeFraction();
        else
            return null;
    }

    public void setOffset(double offset)
    {
        if (currentRun != null)
            currentRun.setOffset(offset);
    }

    public double getOffset()
    {
        if (currentRun!= null)
            return currentRun.getOffset();
        else
            return 0.0;
    }

}
