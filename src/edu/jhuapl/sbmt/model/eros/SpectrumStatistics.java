package edu.jhuapl.sbmt.model.eros;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import vtk.vtkDoubleArray;
import vtk.vtkPolyData;
import vtk.vtkProp;
import vtk.vtkTriangle;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.sbmt.model.spectrum.Spectrum;

public class SpectrumStatistics extends AbstractModel
{
    int nFaces;
    List<Sample> emergenceAngle;
    List<Sample> incidenceAngle;
    List<Sample> phaseAngle;
    List<Sample> irradiance;

    List<Spectrum> spectra=Lists.newArrayList();

    public SpectrumStatistics(List<Sample> emergenceAngle, List<Sample> incidenceAngle, List<Sample> phaseAngle, List<Sample> irradiance, List<Spectrum> spectra)
    {
        this.emergenceAngle=emergenceAngle;
        this.incidenceAngle=incidenceAngle;
        this.phaseAngle=phaseAngle;
        this.irradiance=irradiance;
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

    public List<Spectrum> getOriginalSpectra()
    {
        return spectra;
    }

    public List<Sample> getEmergenceAngleSamples()
    {
        return emergenceAngle;
    }

    public List<Sample> getIncidenceAngleSamples()
    {
        return incidenceAngle;
    }

    public List<Sample> getIrradianceSamples()
    {
        return irradiance;
    }

    public List<Sample> getPhaseAngleSamples()
    {
        return phaseAngle;
    }

    public Map<Spectrum, Integer> orderSpectraByMeanEmergenceAngle()
    {
        final List<Double> means=Lists.newArrayList();
        for (int i=0; i<spectra.size(); i++)
        {
            Spectrum spectrum=spectra.get(i);
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

        Map<Spectrum, Integer> stackingMap=Maps.newHashMap();
        for (int i=0; i<indices.size(); i++)
            stackingMap.put(spectra.get(i), indices.get(i));
        return stackingMap;
    }


    public static class Sample
    {
        public double value;
        public double weight;
        Spectrum parentSpectrum;
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

    public static List<Sample> sampleEmergenceAngle(Spectrum spectrum, Vector3D scPos)
    {
        vtkPolyData footprint=spectrum.getUnshiftedFootprint();
        List<Sample> samples=Lists.newArrayList();
        vtkDoubleArray overlapFraction=(vtkDoubleArray)footprint.GetCellData().GetArray(Spectrum.faceAreaFractionArrayName);
        for (int c=0; c<footprint.GetNumberOfCells(); c++)
        {
            vtkTriangle tri=(vtkTriangle)footprint.GetCell(c);
            double[] nml=new double[3];
            tri.ComputeNormal(tri.GetPoints().GetPoint(0), tri.GetPoints().GetPoint(1), tri.GetPoints().GetPoint(2), nml);
            double[] ctr=new double[3];
            tri.TriangleCenter(tri.GetPoints().GetPoint(0), tri.GetPoints().GetPoint(1), tri.GetPoints().GetPoint(2), ctr);
            Vector3D nmlVec=new Vector3D(nml).normalize();
            Vector3D ctrVec=new Vector3D(ctr);
            Vector3D toScVec=scPos.subtract(ctrVec);
            //
            Sample sample=new Sample();
            sample.value=Math.toDegrees(Math.acos(nmlVec.dotProduct(toScVec.normalize())));
            sample.weight=overlapFraction.GetValue(c);
            sample.parentSpectrum=spectrum;
            samples.add(sample);
        }
        return samples;
    }

    // TODO: incorporate occlusion in a meaningful way
    public static List<Sample> sampleIncidenceAngle(Spectrum spectrum, Vector3D toSunVector)//, double[] illuminationFactors)
    {
        vtkPolyData footprint=spectrum.getUnshiftedFootprint();
        List<Sample> samples=Lists.newArrayList();
        vtkDoubleArray overlapFraction=(vtkDoubleArray)footprint.GetCellData().GetArray(Spectrum.faceAreaFractionArrayName);
        for (int c=0; c<footprint.GetNumberOfCells(); c++)
        {
            vtkTriangle tri=(vtkTriangle)footprint.GetCell(c);
            double[] nml=new double[3];
            tri.ComputeNormal(tri.GetPoints().GetPoint(0), tri.GetPoints().GetPoint(1), tri.GetPoints().GetPoint(2), nml);
            double[] ctr=new double[3];
            tri.TriangleCenter(tri.GetPoints().GetPoint(0), tri.GetPoints().GetPoint(1), tri.GetPoints().GetPoint(2), ctr);
            Vector3D nmlVec=new Vector3D(nml).normalize();
            //
            Sample sample=new Sample();
//            if (illuminationFactors[c]!=0)
//            {
                sample.value=Math.toDegrees(Math.acos(nmlVec.dotProduct(toSunVector.normalize())));
                sample.weight=overlapFraction.GetValue(c);
//            }
            sample.parentSpectrum=spectrum;
            samples.add(sample);
        }
        return samples;
    }

    public static List<Sample> samplePhaseAngle(List<Sample> incidenceAngle, List<Sample> emergenceAngle)
    {
        List<Sample> samples=Lists.newArrayList();
        for (int i=0; i<incidenceAngle.size(); i++)
        {
//            if (incidenceAngle.get(i).value!=Double.NaN)
//            {
                Sample sample=new Sample();
                sample.value=FastMath.abs(incidenceAngle.get(i).value-emergenceAngle.get(i).value);
                sample.weight=1;
                samples.add(sample);
//            }
        }
        return samples;
    }

    public static List<Sample> sampleIrradiance(Spectrum spectrum, double[] illuminationFactors)
    {
        vtkPolyData footprint=spectrum.getUnshiftedFootprint();
        List<Sample> samples=Lists.newArrayList();
        vtkDoubleArray overlapFraction=(vtkDoubleArray)footprint.GetCellData().GetArray(Spectrum.faceAreaFractionArrayName);
        for (int c=0; c<footprint.GetNumberOfCells(); c++)
        {
            vtkTriangle tri=(vtkTriangle)footprint.GetCell(c);
                Sample sample=new Sample();
                sample.value=illuminationFactors[c];
                sample.weight=overlapFraction.GetValue(c);
                sample.parentSpectrum=spectrum;
                samples.add(sample);
        }
        return samples;

    }


/*    public static List<Sample> removeNans(List<Sample> samples)
    {
        List<Sample> result=Lists.newArrayList();
        for (int i=0; i<samples.size(); i++)
            if (!Double.isNaN(samples.get(i).value))
                result.add(samples.get(i));
        return result;
    }*/

}
