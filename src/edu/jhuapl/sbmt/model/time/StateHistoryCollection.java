package edu.jhuapl.sbmt.model.time;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import vtk.vtkActor;
import vtk.vtkProp;

import edu.jhuapl.saavtk.gui.Renderer;
import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SbmtModelFactory;
//import edu.jhuapl.sbmt.client.ModelFactory;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.time.StateHistoryModel.StateHistoryKey;
import edu.jhuapl.sbmt.model.time.StateHistoryModel.StateHistorySource;

public class StateHistoryCollection extends AbstractModel implements PropertyChangeListener, HasTime
{
    private SmallBodyModel smallBodyModel;

    private List<StateHistoryModel> simRuns = new ArrayList<StateHistoryModel>();

    private StateHistoryModel currentRun = null;

    public StateHistoryCollection(SmallBodyModel smallBodyModel)
    {
        this.smallBodyModel = smallBodyModel;
    }

    protected StateHistoryModel createRun(StateHistoryKey key, SmallBodyModel smallBodyModel, Renderer renderer) // throws FitsException, IOException
    {
        try {
            return SbmtModelFactory.createStateHistory(key, smallBodyModel, renderer, false);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean containsKey(StateHistoryKey key)
    {
        for (StateHistoryModel run : simRuns)
        {
            if (run.getKey().equals(key))
                return true;
        }

        return false;
    }

    private StateHistoryModel getRunFromKey(StateHistoryKey key)
    {
        for (StateHistoryModel run : simRuns)
        {
            if (run.getKey().equals(key))
                return run;
        }

        return null;
    }

    public StateHistoryModel getCurrentRun()
    {
        return currentRun;
    }

    public void setCurrentRun(StateHistoryKey key)
    {
        StateHistoryModel run = getRunFromKey(key);
        if (run != null && run != currentRun)
        {
            currentRun = run;
        }

    }

    public void addRun(StateHistoryKey key, Renderer renderer)//  throws FitsException, IOException
    {

        if (containsKey(key))
        {
            this.currentRun = this.getRun(key);
            return;
        }

        StateHistoryModel run = createRun(key, smallBodyModel, renderer);

        // set the current run
        this.currentRun = run;

        run.addPropertyChangeListener(this);

        simRuns.add(run);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void removeRun(StateHistoryKey key)
    {
        if (!containsKey(key))
            return;

        StateHistoryModel run = getRunFromKey(key);
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
    public void removeRuns(StateHistorySource source)
    {
        for (StateHistoryModel run : simRuns)
            if (run.getKey().source == source)
                removeRun(run.getKey());
    }

    public void setShowTrajectories(boolean show)
    {
        for (StateHistoryModel run : simRuns)
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

    public StateHistoryModel getRun(vtkActor actor)
    {
        return currentRun;
    }

    public StateHistoryModel getRun(StateHistoryKey key)
    {
        return getRunFromKey(key);
    }

    public boolean containsRun(StateHistoryKey key)
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
