package edu.jhuapl.sbmt.model.eros;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import vtk.vtkProp;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.bennu.otes.OTES;
import edu.jhuapl.sbmt.model.bennu.otes.OTESSpectrum;
import edu.jhuapl.sbmt.model.bennu.ovirs.OVIRS;
import edu.jhuapl.sbmt.model.bennu.ovirs.OVIRSSpectrum;
import edu.jhuapl.sbmt.model.ryugu.nirs3.NIRS3;
import edu.jhuapl.sbmt.model.ryugu.nirs3.NIRS3Spectrum;
import edu.jhuapl.sbmt.model.spectrum.SpectralInstrument;
import edu.jhuapl.sbmt.model.spectrum.Spectrum;

public class SpectraCollection extends AbstractModel implements PropertyChangeListener
{
    private HashMap<Spectrum, List<vtkProp>> spectraActors = new HashMap<Spectrum, List<vtkProp>>();

    private HashMap<String, Spectrum> fileToSpectrumMap = new HashMap<String, Spectrum>();

    private HashMap<vtkProp, String> actorToFileMap = new HashMap<vtkProp, String>();
    private SmallBodyModel shapeModel;

    boolean selectAll=false;
    final double minFootprintSeparation=0.001;
    double footprintSeparation=0.001;

    Map<Spectrum,Integer> ordinals=Maps.newHashMap();
    final static int defaultOrdinal=0;

    public SpectraCollection(SmallBodyModel eros)
    {
        this.shapeModel = eros;
    }

    public void reshiftFootprints()
    {
        for (Spectrum spectrum : ordinals.keySet())
        {
            spectrum.shiftFootprintToHeight(footprintSeparation*(1+ordinals.get(spectrum)));
            //System.out.println(ordinals.get(spectrum)+" "+spectrum.isSelected);
        }
        //System.out.println();
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED,null,null);
    }

    public void setOrdinal(Spectrum spectrum, int ordinal)
    {
        //System.out.println(spectrum);
        if (ordinals.containsKey(spectrum))
            ordinals.remove(spectrum);
        ordinals.put(spectrum, ordinal);
        //System.out.println(ordinals);
    }

    public void clearOrdinals()
    {
        ordinals.clear();
    }

    public void setFootprintSeparation(double val)
    {
        footprintSeparation=val;
        reshiftFootprints();
    }

    public void increaseFootprintSeparation(double val)
    {
        footprintSeparation+=val;
        reshiftFootprints();
    }

    public void decreaseFootprintSeparation(double val)
    {
        footprintSeparation-=val;
        if (footprintSeparation<minFootprintSeparation)
            footprintSeparation=minFootprintSeparation;
        reshiftFootprints();
    }

    public double getFootprintSeparation()
    {
        return footprintSeparation;
    }

    public double getMinFootprintSeparation()
    {
        return minFootprintSeparation;
    }


    public Spectrum addSpectrum(String path, SpectralInstrument instrument) throws IOException
    {
        if (fileToSpectrumMap.containsKey(path))
            return fileToSpectrumMap.get(path);

        //NISSpectrum spectrum = NISSpectrum.NISSpectrumFactory.createSpectrum(path, erosModel);
        //NISSpectrum spectrum = new NISSpectrum(path, erosModel, instrument);

        Spectrum spectrum=null;
        try
        {
        if (instrument instanceof NIS)
        {
            spectrum=new NISSpectrum(path, shapeModel, instrument);
        }
        else if (instrument instanceof OTES)
        {
            spectrum=new OTESSpectrum(path, shapeModel, instrument);
        }
        else if (instrument instanceof OVIRS)
        {
            spectrum=new OVIRSSpectrum(path, shapeModel, instrument);
        }
        else if (instrument instanceof NIRS3)
        {
            spectrum=new NIRS3Spectrum(path, shapeModel, instrument);
        }
        else throw new Exception(instrument.getDisplayName()+" not supported");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        shapeModel.addPropertyChangeListener(spectrum);
        spectrum.addPropertyChangeListener(this);

        fileToSpectrumMap.put(path, spectrum);
        spectraActors.put(spectrum, new ArrayList<vtkProp>());

        List<vtkProp> props = spectrum.getProps();

        /*
        for (vtkProp p : props)
        {
            vtkActor a=(vtkActor)p;
            vtkPolyDataMapper m=(vtkPolyDataMapper)a.GetMapper();
            System.out.println(m);
            vtkPolyData polyData=m.GetInput();
            System.out.println(polyData.GetNumberOfCells());
        }*/

        spectraActors.get(spectrum).addAll(props);

        for (vtkProp act : props)
            actorToFileMap.put(act, path);

        select(spectrum);
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        return spectrum;
    }

    public void removeSpectrum(String path)
    {
        Spectrum spectrum = fileToSpectrumMap.get(path);
        spectrum.setUnselected();

        List<vtkProp> actors = spectraActors.get(spectrum);

        for (vtkProp act : actors)
            actorToFileMap.remove(act);

        spectraActors.remove(spectrum);

        fileToSpectrumMap.remove(path);

        spectrum.removePropertyChangeListener(this);
        shapeModel.removePropertyChangeListener(spectrum);
        spectrum.setShowFrustum(false);

        ordinals.remove(spectrum);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        this.pcs.firePropertyChange(Properties.MODEL_REMOVED, null, spectrum);
    }

    public void removeAllSpectra()
    {
        HashMap<String, Spectrum> map = (HashMap<String, Spectrum>)fileToSpectrumMap.clone();
        for (String path : map.keySet())
            removeSpectrum(path);
    }

    public void toggleSelect(Spectrum spectrum)
    {
        if (spectrum.isSelected())
            spectrum.setUnselected();
        else
            spectrum.setSelected();
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED,null,null);
        selectAll=false;
    }

    public void select(Spectrum spectrum)
    {
        spectrum.setSelected();
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED,null,null);
        selectAll=false;
    }

    public void deselect(Spectrum spectrum)
    {
        spectrum.setUnselected();
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED,null,null);
        selectAll=false;
    }

    public void toggleSelectAll()
    {
        if (!selectAll) // we're not in "select all" mode so go ahead and select all actors
        {
            for (Spectrum spectrum : fileToSpectrumMap.values())
                spectrum.setSelected();
            selectAll=true;
        }
        else
        {
            for (Spectrum spectrum : fileToSpectrumMap.values())
                spectrum.setUnselected();
            selectAll=false;
        }
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED,null,null);
    }

    public void deselectAll()
    {
        for (Spectrum spectrum : fileToSpectrumMap.values())
            spectrum.setUnselected();
        selectAll=false;
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED,null,null);
    }

    public List<Spectrum> getSelectedSpectra()
    {
        List<Spectrum> spectra=Lists.newArrayList();
        for (Spectrum s : fileToSpectrumMap.values())
            if (s.isSelected())
                spectra.add(s);
        return spectra;
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        String filename = actorToFileMap.get(prop);
        Spectrum spectrum = this.fileToSpectrumMap.get(filename);
//        System.out.println("SpectraCollection: getClickStatusBarText: time is " + ((OTESSpectrum)spectrum).getTime());
        if (spectrum==null)
            return "";
        return spectrum.getInstrument().getDisplayName() + " spectrum " + filename.substring(16, 25) + " acquired at " + spectrum.getDateTime().toString() /*+ "(SCLK: " + ((OTESSpectrum)spectrum).getTime() + ")"*/;
    }

    public String getSpectrumName(vtkProp actor)
    {
        return actorToFileMap.get(actor);
    }

    public Spectrum getSpectrum(String file)
    {
        return fileToSpectrumMap.get(file);
    }

    public boolean containsSpectrum(String file)
    {
        return fileToSpectrumMap.containsKey(file);
    }

    public void setChannelColoring(int[] channels, double[] mins, double[] maxs, SpectralInstrument instrument)
    {
        for (String file : this.fileToSpectrumMap.keySet())
        {
            Spectrum spectrum=this.fileToSpectrumMap.get(file);
            if (spectrum.getInstrument() == instrument)
            {
                spectrum.setChannelColoring(channels, mins, maxs);
                spectrum.updateChannelColoring();
            }
        }

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    @Override
    public List<vtkProp> getProps()
    {
        List<vtkProp> allProps=Lists.newArrayList();
        for (Spectrum s : spectraActors.keySet())
            allProps.addAll(spectraActors.get(s));
        return allProps;
    }
}
