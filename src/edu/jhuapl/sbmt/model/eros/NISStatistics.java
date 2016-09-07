package edu.jhuapl.sbmt.model.eros;

import java.util.List;

import com.google.common.collect.Lists;

import vtk.vtkProp;

import edu.jhuapl.saavtk.model.AbstractModel;

public class NISStatistics extends AbstractModel
{
    int nFaces;
    double[] th;
    double minth,maxth;

    List<vtkProp> props=Lists.newArrayList();
    List<NISSpectrum> spectra=Lists.newArrayList();

    public NISStatistics(double[] th, List<NISSpectrum> spectra)
    {
        this.th=th;
        nFaces=th.length;
        this.spectra=spectra;

        minth=Double.POSITIVE_INFINITY;
        maxth=Double.NEGATIVE_INFINITY;
        for (int i=0; i<th.length; i++)
        {
            if (th[i]<minth)
                minth=th[i];
            if (th[i]>maxth)
                maxth=th[i];
        }
}

    @Override
    public List<vtkProp> getProps()
    {
        return props;
    }

    public int getNumberOfFaces()
    {
        return nFaces;
    }

    public double[] getTheta()
    {
        return th;
    }

    public double getMinTheta()
    {
        return minth;
    }

    public double getMaxTheta()
    {
        return maxth;
    }

    public List<NISSpectrum> getOriginalSpectra()
    {
        return spectra;
    }

}
