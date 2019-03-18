package edu.jhuapl.sbmt.model.ryugu.nirs3.atRyugu;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.joda.time.DateTime;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkDoubleArray;
import vtk.vtkIdList;
import vtk.vtkIdTypeArray;
import vtk.vtkPointLocator;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkProperty;
import vtk.vtkTriangle;
import vtk.vtksbCellLocator;

import edu.jhuapl.saavtk.colormap.Colormap;
import edu.jhuapl.saavtk.colormap.Colormaps;
import edu.jhuapl.saavtk.model.GenericPolyhedralModel;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.sbmt.client.ISmallBodyModel;
import edu.jhuapl.sbmt.model.bennu.InstrumentMetadata;
import edu.jhuapl.sbmt.model.bennu.SpectrumSearchSpec;
import edu.jhuapl.sbmt.model.bennu.otes.SpectraHierarchicalSearchSpecification;
import edu.jhuapl.sbmt.model.image.InfoFileReader;
import edu.jhuapl.sbmt.model.ryugu.nirs3.NIRS3;
import edu.jhuapl.sbmt.model.spectrum.BasicSpectrum;
import edu.jhuapl.sbmt.model.spectrum.ISpectralInstrument;
import edu.jhuapl.sbmt.model.spectrum.coloring.SpectrumColoringStyle;
import edu.jhuapl.sbmt.model.spectrum.statistics.SpectrumStatistics;
import edu.jhuapl.sbmt.model.spectrum.statistics.SpectrumStatistics.Sample;


public class NIRS3Spectrum extends BasicSpectrum
{
    boolean footprintGenerated = false;
    File infoFile, spectrumFile;
    double time;
    String extension = "";
    private SpectraHierarchicalSearchSpecification<SpectrumSearchSpec> specIO;
    private InstrumentMetadata<SpectrumSearchSpec> instrumentMetadata;

    public NIRS3Spectrum(String filename, ISmallBodyModel smallBodyModel,
            ISpectralInstrument instrument) throws IOException
    {
        super(filename, smallBodyModel, instrument);
        extension = FilenameUtils.getExtension(serverpath.toString());
        this.specIO = smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification();
        instrumentMetadata = specIO.getInstrumentMetadata("NIRS3");
    }

    public NIRS3Spectrum(String filename, ISmallBodyModel smallBodyModel,
            ISpectralInstrument instrument, boolean headless, boolean isCustom) throws IOException
    {
        super(filename, smallBodyModel, instrument, headless, isCustom);
        extension = FilenameUtils.getExtension(serverpath.toString());
        this.specIO = smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification();
        instrumentMetadata = specIO.getInstrumentMetadata("NIRS3");
    }

    @Override
    public void saveSpectrum(File file) throws IOException
    {
        throw new IOException("Not implemented.");
    }

    protected String getLocalInfoFilePathOnServer()
    {
        return Paths.get(getLocalSpectrumFilePathOnServer()).getParent().resolve(FilenameUtils.getBaseName(getLocalSpectrumFilePathOnServer()) + ".INFO").toString();
    }

    protected String getLocalSpectrumFilePathOnServer()
    {
    	System.out.println("NIRS3Spectrum: getLocalSpectrumFilePathOnServer: server path is " + serverpath);
        return serverpath;
    }


    protected String getInfoFilePathOnServer()
    {
        if (isCustomSpectra)
        {
        	System.out.println("NIRS3Spectrum: getInfoFilePathOnServer: custom file " + getLocalInfoFilePathOnServer());
            return getLocalInfoFilePathOnServer();
        }
        else
        {
            String spectrumPath = getSpectrumPathOnServer().substring(0, getSpectrumPathOnServer().lastIndexOf("/"));
//            System.out.println("OTESSpectrum: getInfoFilePathOnServer: spectrum path " + Paths.get(getSpectrumPathOnServer()).getParent().resolveSibling("infofiles-corrected"));
            return Paths.get(spectrumPath).getParent()
                    .resolveSibling("infofiles-corrected")
                    .resolve(FilenameUtils.getBaseName(getSpectrumPathOnServer()) + ".INFO")
                    .toString();
        }
    }

    public String getSpectrumPathOnServer()
    {
  		spec = instrumentMetadata.getSpecs().get(0);
        if (isCustomSpectra)
        {
            return serverpath;
        }
        else
        {
            return Paths.get(serverpath).getParent()
                    .resolve(FilenameUtils.getBaseName(serverpath) + "." + extension)
                    .toString();
        }
    }

    @Override
    public void generateFootprint()
    {
        if (!footprintGenerated)
        {
            readPointingFromInfoFile();
            readSpectrumFromFile();

            vtkPolyData tmp = smallBodyModel.computeFrustumIntersection(
                    spacecraftPosition, frustum1, frustum2, frustum3, frustum4);
            System.out.println("NIRS3Spectrum: generateFootprint: sc pos " + spacecraftPosition[0] + " " + spacecraftPosition[1] +
            		" " + spacecraftPosition[2]);

            if (tmp==null)
                return;

            Vector3D f1=new Vector3D(frustum1);
            Vector3D f2=new Vector3D(frustum2);
            Vector3D f3=new Vector3D(frustum3);
            Vector3D f4=new Vector3D(frustum4);
            Vector3D lookUnit=new Vector3D(1,f1,1,f2,1,f3,1,f4);

            double[] angles=new double[]{22.5,45,67.5};
            for (int i=0; i<angles.length; i++)

            {
            	System.out.println("NIRS3Spectrum: generateFootprint: angle " + angles[i]);
                vtkPolyData tmp2=new vtkPolyData();

	            Rotation rot=new Rotation(lookUnit, Math.toRadians(angles[i]));
	            Vector3D g1=rot.applyTo(f1);
	            Vector3D g2=rot.applyTo(f2);
	            Vector3D g3=rot.applyTo(f3);
	            Vector3D g4=rot.applyTo(f4);
	            System.out.println("NIRS3Spectrum: generateFootprint: g1 " + g1);
	            System.out.println("NIRS3Spectrum: generateFootprint: g2 " + g2);
	            System.out.println("NIRS3Spectrum: generateFootprint: g3 " + g3);
	            System.out.println("NIRS3Spectrum: generateFootprint: g4 " + g4);
	            vtksbCellLocator tree=new vtksbCellLocator();
	            tree.SetDataSet(tmp);
	            tree.SetTolerance(1e-12);
	            tree.BuildLocator();

	            vtkPointLocator ploc=new vtkPointLocator();
	            ploc.SetDataSet(tmp);
	            ploc.SetTolerance(1e-12);
	            ploc.BuildLocator();

	            tmp2 = PolyDataUtil.computeFrustumIntersection(tmp, tree, ploc, spacecraftPosition, g1.toArray(), g2.toArray(), g3.toArray(), g4.toArray());
                if (tmp2 == null)
                {
                	System.out.println("NIRS3Spectrum: generateFootprint: temp2 is null");
                    try
                    {
                        throw new Exception("Frustum intersection is null - this needs to be handled better");
                    }
                    catch (Exception e)
                    {
                        // TODO Auto-generated catch block
//                        e.printStackTrace();
                    }
                    continue;
                }
	            tmp.DeepCopy(tmp2);
            }

            if (tmp==null)
                return;

            vtkDoubleArray faceAreaFraction = new vtkDoubleArray();
            faceAreaFraction.SetName(faceAreaFractionArrayName);
            for (int c = 0; c < tmp.GetNumberOfCells(); c++)
            {
                vtkIdTypeArray originalIds = (vtkIdTypeArray) tmp
                        .GetCellData()
                        .GetArray(GenericPolyhedralModel.cellIdsArrayName);
                int originalId = originalIds.GetValue(c);
                vtkTriangle tri = (vtkTriangle) smallBodyModel
                        .getSmallBodyPolyData().GetCell(originalId); // tri
                                                                     // on
                                                                     // original
                                                                     // body
                                                                     // model
                vtkTriangle ftri = (vtkTriangle) tmp.GetCell(c); // tri on
                                                                 // footprint
                faceAreaFraction.InsertNextValue(
                        ftri.ComputeArea() / tri.ComputeArea());
            }
            tmp.GetCellData().AddArray(faceAreaFraction);

            // Need to clear out scalar data since if coloring data is being
            // shown,
            // then the color might mix-in with the image.
            tmp.GetCellData().SetScalars(null);
            tmp.GetPointData().SetScalars(null);

            footprint = new vtkPolyData();
            footprint.DeepCopy(tmp);

            shiftedFootprint = new vtkPolyData();
            shiftedFootprint.DeepCopy(tmp);
            PolyDataUtil.shiftPolyDataInMeanNormalDirection(
                    shiftedFootprint, footprintHeight);

            createSelectionPolyData();
            createSelectionActor();
            createToSunVectorPolyData();
            createToSunVectorActor();
            createOutlinePolyData();
            createOutlineActor();

        }
    }

    protected void readPointingFromInfoFile()
    {
    	if (!isCustomSpectra)
            infoFile = FileCache.getFileFromServer(getInfoFilePathOnServer());
        else
            infoFile = new File(getInfoFilePathOnServer());
        System.out.println("NIRS3Spectrum: readPointingFromInfoFile: info file is " + infoFile.getAbsolutePath());
        //
        InfoFileReader reader = new InfoFileReader(infoFile.getAbsolutePath());
        reader.read();
        //
        Vector3D origin = new Vector3D(reader.getSpacecraftPosition()); //.scalarMultiply(1e-3);
        Vector3D fovUnit = new Vector3D(reader.getFrustum2()).normalize(); // for whatever
                                                               // reason,
                                                               // frustum2
                                                               // contains the
                                                               // vector along
                                                               // the field of
                                                               // view cone
        Vector3D boresightUnit = new Vector3D(reader.getBoresightDirection()).normalize();
        Vector3D lookTarget = origin
                .add(boresightUnit.scalarMultiply(origin.getNorm()));
        double fovDeg = Math
                .toDegrees(Vector3D.angle(fovUnit, boresightUnit) * 2.);
        //
        toSunUnitVector = new Vector3D(reader.getSunPosition()).normalize();
        Frustum frustum = new Frustum(origin.toArray(), lookTarget.toArray(),
                boresightUnit.orthogonal().toArray(), fovDeg, fovDeg);
        frustum1 = frustum.ul;
        frustum2 = frustum.ur;
        frustum3 = frustum.lr;
        frustum4 = frustum.ll;
        spacecraftPosition = frustum.origin;

        this.dateTime=new DateTime(reader.getStartTime());

        // 1 double[] ul, // ordering is from
        // smallBodyModel.computeFrustumIntersection(spacecraftPosition,
        // frustum1, frustum2, frustum3, frustum4);
        // 2 double[] ur,
        // 3 double[] lr,
        // 4 double[] ll)

    }

    protected void readSpectrumFromFile()
    {
    	if (!isCustomSpectra)
            spectrumFile=FileCache.getFileFromServer(getSpectrumPathOnServer());
        else
            spectrumFile = new File(getSpectrumPathOnServer());
        //
        NIRS3SpectrumReader reader = new NIRS3SpectrumReader(spectrumFile.getAbsolutePath());
        reader.read();

        spectrum=reader.spectra.get(0).getSpectrum();
        xData = new NIRS3().getBandCenters();
        time = reader.spectra.get(0).getEt();
    }

    @Override
    public List<vtkProp> getProps()
    {
        if (footprintActor == null)
        {
            generateFootprint();

            vtkPolyDataMapper footprintMapper = new vtkPolyDataMapper();
            footprintMapper.SetInputData(shiftedFootprint);
            // footprintMapper.SetResolveCoincidentTopologyToPolygonOffset();
            // footprintMapper.SetResolveCoincidentTopologyPolygonOffsetParameters(-.002,
            // -2.0);
            footprintMapper.Update();

            footprintActor = new vtkActor();
            footprintActor.SetMapper(footprintMapper);
            vtkProperty footprintProperty = footprintActor.GetProperty();
            System.out.println("NIRS3Spectrum: getProps: getting channel color");
            double[] color = getChannelColor();
            footprintProperty.SetColor(color[0], color[1], color[2]);
            footprintProperty.SetLineWidth(2.0);
            footprintProperty.LightingOff();

            footprintActors.add(footprintActor);

            /*
             * // Compute the bounding edges of this surface vtkFeatureEdges
             * edgeExtracter = new vtkFeatureEdges();
             * edgeExtracter.SetInput(shiftedFootprint);
             * edgeExtracter.BoundaryEdgesOn(); edgeExtracter.FeatureEdgesOff();
             * edgeExtracter.NonManifoldEdgesOff();
             * edgeExtracter.ManifoldEdgesOff(); edgeExtracter.Update();
             *
             * vtkPolyDataMapper edgeMapper = new vtkPolyDataMapper();
             * edgeMapper.SetInputConnection(edgeExtracter.GetOutputPort());
             * edgeMapper.ScalarVisibilityOff();
             * //edgeMapper.SetResolveCoincidentTopologyToPolygonOffset();
             * //edgeMapper.SetResolveCoincidentTopologyPolygonOffsetParameters(
             * -.004, -4.0); edgeMapper.Update();
             *
             * vtkActor edgeActor = new vtkActor();
             * edgeActor.SetMapper(edgeMapper);
             * edgeActor.GetProperty().SetColor(0.0, 0.39, 0.0);
             * edgeActor.GetProperty().SetLineWidth(2.0);
             * edgeActor.GetProperty().LightingOff();
             * footprintActors.add(edgeActor);
             */
        }

        if (frustumActor == null)
        {
            vtkPolyData frus = new vtkPolyData();

            vtkPoints points = new vtkPoints();
            vtkCellArray lines = new vtkCellArray();

            vtkIdList idList = new vtkIdList();
            idList.SetNumberOfIds(2);

            double dx = MathUtil.vnorm(spacecraftPosition)
                    + smallBodyModel.getBoundingBoxDiagonalLength();
            double[] origin = spacecraftPosition;
            double[] UL = { origin[0] + frustum1[0] * dx,
                    origin[1] + frustum1[1] * dx,
                    origin[2] + frustum1[2] * dx };
            double[] UR = { origin[0] + frustum2[0] * dx,
                    origin[1] + frustum2[1] * dx,
                    origin[2] + frustum2[2] * dx };
            double[] LL = { origin[0] + frustum3[0] * dx,
                    origin[1] + frustum3[1] * dx,
                    origin[2] + frustum3[2] * dx };
            double[] LR = { origin[0] + frustum4[0] * dx,
                    origin[1] + frustum4[1] * dx,
                    origin[2] + frustum4[2] * dx };

            points.InsertNextPoint(spacecraftPosition);
            points.InsertNextPoint(UL);
            points.InsertNextPoint(UR);
            points.InsertNextPoint(LL);
            points.InsertNextPoint(LR);

            idList.SetId(0, 0);
            idList.SetId(1, 1);
            lines.InsertNextCell(idList);
            idList.SetId(0, 0);
            idList.SetId(1, 2);
            lines.InsertNextCell(idList);
            idList.SetId(0, 0);
            idList.SetId(1, 3);
            lines.InsertNextCell(idList);
            idList.SetId(0, 0);
            idList.SetId(1, 4);
            lines.InsertNextCell(idList);

            frus.SetPoints(points);
            frus.SetLines(lines);

/*            vtkPolyDataWriter writer = new vtkPolyDataWriter();
            writer.SetInputData(frus);
            writer.SetFileName("/Users/zimmemi1/Desktop/otes/"
                    + UUID.randomUUID() + ".vtk");
            writer.SetFileTypeToBinary();
            writer.Write();*/

            vtkPolyDataMapper frusMapper = new vtkPolyDataMapper();
            frusMapper.SetInputData(frus);

            frustumActor = new vtkActor();
            frustumActor.SetMapper(frusMapper);
            vtkProperty frustumProperty = frustumActor.GetProperty();
            frustumProperty.SetColor(0.0, 1.0, 0.0);
            frustumProperty.SetLineWidth(2.0);
            frustumActor.VisibilityOff();

            footprintActors.add(frustumActor);
        }

        footprintActors.add(selectionActor);
        footprintActors.add(toSunVectorActor);
        footprintActors.add(outlineActor);

        return footprintActors;
    }

    @Override
    public int getNumberOfBands()
    {
        return NIRS3.bandCentersLength;
    }

    @Override
    public double[] getChannelColor()
    {
        if (coloringStyle == SpectrumColoringStyle.EMISSION_ANGLE)
        {
            List<Sample> sampleEmergenceAngle = SpectrumStatistics.sampleEmergenceAngle(this, new Vector3D(spacecraftPosition));
            Colormap colormap = Colormaps.getNewInstanceOfBuiltInColormap("OREX Scalar Ramp");
            colormap.setRangeMin(0.0);  //was 5.4
            colormap.setRangeMax(90.00); //was 81.7

            Color color2 = colormap.getColor(SpectrumStatistics.getWeightedMean(sampleEmergenceAngle));
                    double[] color = new double[3];
            color[0] = color2.getRed()/255.0;
            color[1] = color2.getGreen()/255.0;
            color[2] = color2.getBlue()/255.0;
            return color;
        }
        else
        {
	        double[] color = new double[3];
	        for (int i=0; i<3; ++i)
	        {
	            double val = 0.0;
	            System.out.println("NIRS3Spectrum: getChannelColor: channel " + channelsToColorBy[i]);
	            if (channelsToColorBy[i] < instrument.getBandCenters().length)
	            {
//	            	System.out.println("NIRS3Spectrum: getChannelColor: color 1");
	                val = spectrum[channelsToColorBy[i]];
	            }
	            else if (channelsToColorBy[i] < instrument.getBandCenters().length + instrument.getSpectrumMath().getDerivedParameters().length)
	            {
//	            	System.out.println("NIRS3Spectrum: getChannelColor: color 2");
	                val = evaluateDerivedParameters(channelsToColorBy[i]-instrument.getBandCenters().length);
	            }
	            else
	            {
//	            	System.out.println("NIRS3Spectrum: getChannelColor: color 3");
	                val = instrument.getSpectrumMath().evaluateUserDefinedDerivedParameters(channelsToColorBy[i]-instrument.getBandCenters().length-instrument.getSpectrumMath().getDerivedParameters().length, spectrum);
	            }
//	            System.out.println("NIRS3Spectrum: getChannelColor: val is " + val);
	            if (val < 0.0)
	                val = 0.0;
	            else if (val > 1.0)
	                val = 1.0;

	            System.out.println("NIRS3Spectrum: getChannelColor: minmax diff " + (channelsColoringMaxValue[i] - channelsColoringMinValue[i]));
	            double slope = 1.0 / (channelsColoringMaxValue[i] - channelsColoringMinValue[i]);
	            color[i] = slope * (val - channelsColoringMinValue[i]);
	        }
	        System.out.println("NIRS3Spectrum: getChannelColor: returning " + color[0] + " " + color[1] + " " + color[2]);
	        return color;
        }
    }


}
