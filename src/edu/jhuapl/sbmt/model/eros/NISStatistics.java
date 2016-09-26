package edu.jhuapl.sbmt.model.eros;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import vtk.vtkPolyData;
import vtk.vtkProp;
import vtk.vtkTriangle;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.util.Frustum;

public class NISStatistics extends AbstractModel
{
    int nFaces;
    List<Sample> emergenceAngle;

    List<NISSpectrum> spectra=Lists.newArrayList();

    public NISStatistics(List<Sample> emergenceAngle, List<NISSpectrum> spectra)
    {
        this.emergenceAngle=emergenceAngle;
        this.spectra=spectra;

        nFaces=emergenceAngle.size();
    }

    @Override
    public List<vtkProp> getProps()
    {
        return null;    // props are stored by individual spectra
    }

    public int getNumberOfFaces()
    {
        return nFaces;
    }

    public List<NISSpectrum> getOriginalSpectra()
    {
        return spectra;
    }

    public List<Sample> sampleEmergenceAngle()
    {
        return emergenceAngle;
    }

    public Map<NISSpectrum, Integer> orderSpectraByMeanEmergenceAngle()
    {
        final List<Double> means=Lists.newArrayList();
        for (int i=0; i<spectra.size(); i++)
        {
            NISSpectrum spectrum=spectra.get(i);
            List<Sample> subSample=Lists.newArrayList();
            for (Sample sample : emergenceAngle)
                if (sample.parentSpectrum.equals(spectrum))
                    subSample.add(sample);
            means.add(getWeightedMean(subSample));
        }

        List<Integer> indices=Lists.newArrayList();
        for (int i=0; i<spectra.size(); i++)
            indices.add(i);
        Collections.sort(indices, new Comparator<Integer>() // sort indices by value of corresponding means
        {
            @Override
            public int compare(Integer o1, Integer o2)
            {
                return means.get(o1).compareTo(means.get(o2));
            }
        });

        Map<NISSpectrum, Integer> stackingMap=Maps.newHashMap();
        for (int i=0; i<indices.size(); i++)
            stackingMap.put(spectra.get(i), indices.get(i));
        return stackingMap;
    }


    public static class Sample
    {
        double value;
        double weight;
        NISSpectrum parentSpectrum;
    }

    public static double getMin(List<Sample> samples)
    {
        double min=Double.POSITIVE_INFINITY;
        for (int i=0; i<samples.size(); i++)
        {
            double th=samples.get(i).value;
            if (th<min)
                min=th;
        }
        return min;
    }

    public static double getMax(List<Sample> samples)
    {
        double max=Double.NEGATIVE_INFINITY;
        for (int i=0; i<samples.size(); i++)
        {
            double th=samples.get(i).value;
            if (th>max)
                max=th;
        }
        return max;
    }

    public static double getWeightedMean(List<Sample> samples)
    {
        double mean=0;
        double wtot=0;
        for (int i=0; i<samples.size(); i++)
        {
            mean+=samples.get(i).value*samples.get(i).weight;
            wtot+=samples.get(i).weight;
        }
        return mean/wtot;
    }

    public static double[] getValuesAsArray(List<Sample> samples)
    {
        double[] val=new double[samples.size()];
        for (int i=0; i<samples.size(); i++)
            val[i]=samples.get(i).value;
        return val;
    }

    public static double[] getWeightsAsArray(List<Sample> samples)
    {
        double[] wgt=new double[samples.size()];
        for (int i=0; i<samples.size(); i++)
            wgt[i]=samples.get(i).weight;
        return wgt;
    }

    public static double getWeightedVariance(List<Sample> samples)
    {
        double mean=getWeightedMean(samples);
        double val=0;
        double wtot=0;
        for (int i=0; i<samples.size(); i++)
        {
            val+=Math.pow((samples.get(i).value-mean)*samples.get(i).weight,2);
            wtot+=samples.get(i).weight;
        }
        return val/wtot;
    }

    public static double getWeightedSkewness(List<Sample> samples)
    {
        double mean=getWeightedMean(samples);
        double val=0;
        double wtot=0;
        for (int i=0; i<samples.size(); i++)
        {
            val+=Math.pow((samples.get(i).value-mean)*samples.get(i).weight, 3);
            wtot+=samples.get(i).weight;
        }
        return val/wtot/Math.pow(getWeightedVariance(samples),3./2.);
    }

    public static double getWeightedKurtosis(List<Sample> samples)
    {
        double mean=getWeightedMean(samples);
        double val=0;
        double wtot=0;
        for (int i=0; i<samples.size(); i++)
        {
            val+=Math.pow((samples.get(i).value-mean)*samples.get(i).weight, 4);
            wtot+=samples.get(i).weight;
        }
        return val/wtot/Math.pow(getWeightedVariance(samples),2);
    }

    public static List<Sample> sampleEmergenceAngle(NISSpectrum spectrum, vtkPolyData selectedFaces, Frustum frustum)
    {
        List<Sample> samples=Lists.newArrayList();
        Vector3D origin=new Vector3D(frustum.origin);
        for (int c=0; c<selectedFaces.GetNumberOfCells(); c++)
        {
            vtkTriangle tri=(vtkTriangle)selectedFaces.GetCell(c);
            double[] nml=new double[3];
            tri.ComputeNormal(tri.GetPoints().GetPoint(0), tri.GetPoints().GetPoint(1), tri.GetPoints().GetPoint(2), nml);
            double[] ctr=new double[3];
            tri.TriangleCenter(tri.GetPoints().GetPoint(0), tri.GetPoints().GetPoint(1), tri.GetPoints().GetPoint(2), ctr);
            Vector3D nmlVec=new Vector3D(nml).normalize();
            Vector3D ctrVec=new Vector3D(ctr);
            Vector3D toScVec=origin.subtract(ctrVec);
            //
            Sample sample=new Sample();
            sample.value=Math.acos(nmlVec.dotProduct(toScVec.normalize()));
            sample.weight=computeOverlapFraction(tri, frustum);
            sample.parentSpectrum=spectrum;
            samples.add(sample);
        }
        return samples;
    }

    private static double computeOverlapFraction(vtkTriangle surfaceTriangle, Frustum frustum)  // how much of a triangle is inside the frustum?
    {
        double overlap=0;
        int nPoints=3;  // this can be replaced with some other number if face subdivision is used
        for (int i=0; i<nPoints; i++)
        {
            double[] pt=surfaceTriangle.GetPoints().GetPoint(i);
            double[] uv=new double[2];
            frustum.computeTextureCoordinatesFromPoint(pt, 1, 1, uv, false);
            if (uv[0]>=0 && uv[0]<=1 && uv[1]>=0 && uv[1]<=1)
                overlap+=1./(double)nPoints;
        }
        return overlap;
    }

    public static List<Sample> sampleIncidentFlux(NISSpectrum spectrum, vtkPolyData selectedFaces)
    {
        // TODO: implement
        return null;
    }

}
