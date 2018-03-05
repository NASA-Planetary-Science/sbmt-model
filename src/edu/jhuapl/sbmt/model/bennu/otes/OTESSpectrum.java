package edu.jhuapl.sbmt.model.bennu.otes;

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

import edu.jhuapl.saavtk.model.GenericPolyhedralModel;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.InfoFileReader;
import edu.jhuapl.sbmt.model.spectrum.BasicSpectrum;
import edu.jhuapl.sbmt.model.spectrum.SpectralInstrument;


public class OTESSpectrum extends BasicSpectrum
{
    boolean footprintGenerated = false;
    File infoFile, spectrumFile;

    public OTESSpectrum(String filename, SmallBodyModel smallBodyModel,
            SpectralInstrument instrument) throws IOException
    {
        super(filename, smallBodyModel, instrument);
    }

    @Override
    public void saveSpectrum(File file) throws IOException
    {
        throw new IOException("Not implemented.");
    }

    protected String getInfoFilePathOnServer()
    {
        return Paths.get(getSpectrumPathOnServer()).getParent()
                .resolveSibling("infofiles-corrected")
                .resolve(FilenameUtils.getBaseName(getSpectrumPathOnServer()) + ".INFO")
                .toString();
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
                vtkPolyData tmp2=new vtkPolyData();

            Rotation rot=new Rotation(lookUnit, Math.toRadians(angles[i]));
            Vector3D g1=rot.applyTo(f1);
            Vector3D g2=rot.applyTo(f2);
            Vector3D g3=rot.applyTo(f3);
            Vector3D g4=rot.applyTo(f4);

            vtksbCellLocator tree=new vtksbCellLocator();
            tree.SetDataSet(tmp);
            tree.SetTolerance(1e-12);
            tree.BuildLocator();

            vtkPointLocator ploc=new vtkPointLocator();
            ploc.SetDataSet(tmp);
            ploc.SetTolerance(1e-12);
            ploc.BuildLocator();

            tmp2 = PolyDataUtil.computeFrustumIntersection(tmp, tree, ploc, spacecraftPosition, g1.toArray(), g2.toArray(), g3.toArray(), g4.toArray());
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
        infoFile = FileCache.getFileFromServer(getInfoFilePathOnServer());
        //
        InfoFileReader reader = new InfoFileReader(infoFile.getAbsolutePath());
        reader.read();
        //
        Vector3D origin = new Vector3D(reader.getSpacecraftPosition());
//                .scalarMultiply(1e-3);
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
        spectrumFile=FileCache.getFileFromServer(getSpectrumPathOnServer());
        //
        OTESSpectrumReader reader=new OTESSpectrumReader(spectrumFile.getAbsolutePath());
        reader.read();
        //
        spectrum=reader.getCalibratedRadiance();
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
        return OTES.bandCenters.length;
    }

    @Override
    public double[] getChannelColor()
    {
        double[] color = new double[3];
        for (int i=0; i<3; ++i)
        {
            double val = 0.0;
            if (channelsToColorBy[i] < instrument.getBandCenters().length)
                val = spectrum[channelsToColorBy[i]];
            else if (channelsToColorBy[i] < instrument.getBandCenters().length + instrument.getSpectrumMath().getDerivedParameters().length)
                val = evaluateDerivedParameters(channelsToColorBy[i]-instrument.getBandCenters().length);
            else
                val = instrument.getSpectrumMath().evaluateUserDefinedDerivedParameters(channelsToColorBy[i]-instrument.getBandCenters().length-instrument.getSpectrumMath().getDerivedParameters().length, spectrum);

            if (val < 0.0)
                val = 0.0;
            else if (val > 1.0)
                val = 1.0;

            double slope = 1.0 / (channelsColoringMaxValue[i] - channelsColoringMinValue[i]);
            color[i] = slope * (val - channelsColoringMinValue[i]);
        }
        return color;
    }


}
