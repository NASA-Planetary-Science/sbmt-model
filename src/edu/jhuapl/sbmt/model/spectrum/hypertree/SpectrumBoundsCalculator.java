package edu.jhuapl.sbmt.model.spectrum.hypertree;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import vtk.vtkPolyData;

import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.NativeLibraryLoader;
import edu.jhuapl.sbmt.client.SbmtModelFactory;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.model.image.InfoFileReader;

/**
 * Generate an input file for the Spectrum hypertree search.  This will be a file
 * that has a list of spectrum filenames, and their bounding boxes.
 * spectrum1.spect bbxmin, bbxmax, bbymin, bbymax, bbzmin, bbzmax, tmin, tmax
 * @author osheacm1
 *
 */
public class SpectrumBoundsCalculator
{


    public static void main(String[] args) throws IOException {

        NativeLibraryLoader.loadVtkLibrariesHeadless();
        Configuration.setAPLVersion(true);
        SmallBodyViewConfig.initialize();

        SmallBodyViewConfig config = SmallBodyViewConfig.getSmallBodyConfig(ShapeModelBody.EARTH, ShapeModelType.OREX);
        SmallBodyModel earth = SbmtModelFactory.createSmallBodyModel(config);

        String filename = args[0];  // name of file to write to
        FileWriter fw;
        try
        {
            fw = new FileWriter(filename);

            BufferedWriter bw = new BufferedWriter(fw);

            String inputDir  = args[1]; // directory of info files
            File[] infoFiles = new File(inputDir).listFiles();

            int iFile = 0;
            for (File infoFile : infoFiles) {
                // get filename
                String thisFileName = infoFile.getAbsolutePath();
                // rename to spectrum file
                String specFileName = thisFileName.replaceAll(".INFO", ".spect").replaceAll("infofiles/", "spectrum/");



                // get x, y, z bounds and time, emission, incidence, phase, s/c distance
                InfoFileReader reader = new InfoFileReader(thisFileName);
                reader.read();

                Vector3D origin = new Vector3D(reader.getSpacecraftPosition());
                Vector3D fovUnit = new Vector3D(reader.getFrustum2()).normalize(); // for whatever reason, frustum2 contains the vector along the field of view cone
                Vector3D boresightUnit = new Vector3D(reader.getBoresightDirection()).normalize();
                Vector3D lookTarget = origin
                        .add(boresightUnit.scalarMultiply(origin.getNorm()));


                double fovDeg = Math
                        .toDegrees(Vector3D.angle(fovUnit, boresightUnit) * 2.);
                Vector3D toSunUnitVector = new Vector3D(reader.getSunPosition()).normalize();
                Frustum frustum = new Frustum(origin.toArray(), lookTarget.toArray(),
                        boresightUnit.orthogonal().toArray(), fovDeg, fovDeg);
                double[] frustum1 = frustum.ul;
                double[] frustum2 = frustum.ur;
                double[] frustum3 = frustum.lr;
                double[] frustum4 = frustum.ll;
                double[] spacecraftPosition = frustum.origin;


                //        double[] intersectPoint = new double[3];
                //          int boresightInterceptFaceID = earth.computeRayIntersection(origin.toArray(), boresightUnit.toArray(), intersectPoint);
                //          double[] interceptNormal = earth.getCellNormals().GetTuple3(boresightInterceptFaceID);
                //          vtkCellCenters centers = new vtkCellCenters();
                //          centers.SetInputData(earth.getSmallBodyPolyData());
                //          centers.VertexCellsOn();
                //          centers.Update();
                //          double[] center = centers.GetOutput().GetPoint(boresightInterceptFaceID);
                //
                ////          double[] center = earth.getSmallBodyPolyData().GetPoint(boresightInterceptFaceID);
                //
                //          System.out.println("OTESSpectrum: readPointingFromInfoFile: intercept normal " + interceptNormal[0] + " " + interceptNormal[1] + " " + interceptNormal[2]);
                //          System.out.println("OTESSpectrum: readPointingFromInfoFile: center " + intersectPoint[0] + " " + intersectPoint[1] + " " + intersectPoint[2]);
                //
                //          Vector3D nmlVec=new Vector3D(interceptNormal).normalize();
                //          Vector3D ctrVec=new Vector3D(intersectPoint).normalize();
                //          Vector3D toScVec=new Vector3D(spacecraftPosition).subtract(ctrVec);
                //          double emissionAngle = Math.toDegrees(Math.acos(nmlVec.dotProduct(toScVec.normalize())));
                //          System.out.println("emission angle: " + emissionAngle);


                vtkPolyData tmp = earth.computeFrustumIntersection(
                        spacecraftPosition, frustum1, frustum2, frustum3, frustum4);
                if (tmp != null) {
                    double[] bbox = tmp.GetBounds();
                    System.out.println("file " + iFile++ + ": " + bbox[0] + ", " + bbox[1] + ", " + bbox[2] + ", " + bbox[3]);
                    bw.write(specFileName + " " + bbox[0] + " " + bbox[1] + " " + bbox[2] + " " + bbox[3] + " " + bbox[4] + " " + bbox[5] +" " + reader.getStartTime() + " " + reader.getStopTime()+", 0, 0, 0, 0\n");
                }


            }
            bw.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /////////////////

    }
}
