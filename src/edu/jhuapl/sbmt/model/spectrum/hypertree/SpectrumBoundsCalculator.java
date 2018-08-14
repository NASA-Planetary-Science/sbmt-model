package edu.jhuapl.sbmt.model.spectrum.hypertree;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import vtk.vtkPolyData;

import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.NativeLibraryLoader;
import edu.jhuapl.sbmt.client.SbmtModelFactory;
import edu.jhuapl.sbmt.client.SbmtMultiMissionTool;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.model.bennu.otes.OTES;
import edu.jhuapl.sbmt.model.bennu.otes.OTESSpectrum;
import edu.jhuapl.sbmt.model.bennu.ovirs.OVIRS;
import edu.jhuapl.sbmt.model.eros.SpectrumStatistics;
import edu.jhuapl.sbmt.model.eros.SpectrumStatistics.Sample;
import edu.jhuapl.sbmt.model.image.ImageSource;
import edu.jhuapl.sbmt.model.image.InfoFileReader;
import edu.jhuapl.sbmt.model.spectrum.BasicSpectrum;
import edu.jhuapl.sbmt.model.spectrum.SpectralInstrument;
import edu.jhuapl.sbmt.query.QueryBase;
import edu.jhuapl.sbmt.query.fixedlist.FixedListQuery;
import edu.jhuapl.sbmt.query.fixedlist.FixedListSearchMetadata;
import edu.jhuapl.sbmt.tools.Authenticator;

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

        Configuration.setAPLVersion(true);
        SbmtMultiMissionTool.configureMission();

        // need password to access OREX data
        Authenticator.authenticate();

        System.setProperty("java.awt.headless", "true");
        NativeLibraryLoader.loadVtkLibrariesHeadless();
        SmallBodyViewConfig.initialize();

        // get earth model
        SmallBodyViewConfig config = SmallBodyViewConfig.getSmallBodyConfig(ShapeModelBody.EARTH, ShapeModelType.OREX);
        SmallBodyModel earth = SbmtModelFactory.createSmallBodyModel(config);

        SpectralInstrument instrument;
        if (args[2].toUpperCase().equals("OTES"))
            instrument = new OTES();
        else if (args[2].toUpperCase().equals("OVIRS"))
            instrument = new OVIRS();
        else {
            System.err.println("No spectral instrument named " + args[2]);
            return;
        }

        String filename = args[0];  // name of file to write to
        FileWriter fw;
        try
        {
            fw = new FileWriter(filename);

            BufferedWriter bw = new BufferedWriter(fw);

            String inputDir  = args[1]; // directory of this instruments spectral data (should contain infofiles-corrected, spectra, and spectrumlist.txt)
            QueryBase queryType = instrument.getQueryBase();
            List<List<String>> spectrafiles = new ArrayList<List<String>>();
            if (queryType instanceof FixedListQuery)
            {
                FixedListQuery query = (FixedListQuery)queryType;
                spectrafiles = instrument.getQueryBase().runQuery(FixedListSearchMetadata.of("Spectrum Search", "spectrumlist.txt", "spectra", inputDir, ImageSource.CORRECTED_SPICE)).getResultlist();
            }


            int iFile = 0;
            for (List<String> spectraFile : spectrafiles) {
                // get filename
                String thisFileName = spectraFile.get(0);

                // rename to spectrum file
//                String specFileName = thisFileName.replaceAll(".INFO", ".spect").replaceAll("infofiles/", "spectrum/");

                // create spectrum
                BasicSpectrum spectrum = new OTESSpectrum(thisFileName, earth, instrument);

                // get x, y, z bounds and time, emission, incidence, phase, s/c distance
//                String infoFile = thisFileName.replaceAll("spectrum", "infofiles-corrected").replaceAll(".spectra", ".INFO");
                String basePath = FilenameUtils.getPath(thisFileName);
                String fn = FilenameUtils.getBaseName(thisFileName);
                Path infoFile = Paths.get(basePath).resolveSibling("infofiles-corrected/"+fn+".INFO");
                InfoFileReader reader = new InfoFileReader(FileCache.getFileFromServer(infoFile.toString()).getAbsolutePath());
                reader.read();

                Vector3D origin = new Vector3D(reader.getSpacecraftPosition());
                Vector3D fovUnit = new Vector3D(reader.getFrustum2()).normalize(); // for whatever reason, frustum2 contains the vector along the field of view cone
                Vector3D boresightUnit = new Vector3D(reader.getBoresightDirection()).normalize();
                Vector3D lookTarget = origin
                        .add(boresightUnit.scalarMultiply(origin.getNorm()));


                double fovDeg = Math
                        .toDegrees(Vector3D.angle(fovUnit, boresightUnit) * 2.);
                Vector3D toSun = new Vector3D(reader.getSunPosition()).normalize();
                Frustum frustum = new Frustum(origin.toArray(), lookTarget.toArray(),
                        boresightUnit.orthogonal().toArray(), fovDeg, fovDeg);
                double[] frustum1 = frustum.ul;
                double[] frustum2 = frustum.ur;
                double[] frustum3 = frustum.lr;
                double[] frustum4 = frustum.ll;
                double[] spacecraftPosition = reader.getSpacecraftPosition();

                spectrum.generateFootprint();
//                double[] spacecraftPosition = spectrum.getSpacecraftPosition();
//                double[] toSun = spectrum.getToSunUnitVector();
//                Frustum frustum = new Frustum(spectrum.getFrustumOrigin(),
//                        spectrum.getFrustumCorner(0), spectrum.getFrustumCorner(1),
//                        spectrum.getFrustumCorner(2), spectrum.getFrustumCorner(3));
//                double[] frustum1 = frustum.ul;
//                double[] frustum2 = frustum.ur;
//                double[] frustum3 = frustum.lr;
//                double[] frustum4 = frustum.ll;

                List<Sample> sampleEmergenceAngle = SpectrumStatistics.sampleEmergenceAngle(spectrum, new Vector3D(spacecraftPosition));
                double em = SpectrumStatistics.getWeightedMean(sampleEmergenceAngle);
                List<Sample> sampleIncidenceAngle = SpectrumStatistics.sampleIncidenceAngle(spectrum, toSun);
                double inc = SpectrumStatistics.getWeightedMean(sampleIncidenceAngle);
                List<Sample> samplePhaseAngle = SpectrumStatistics.samplePhaseAngle( sampleIncidenceAngle, sampleEmergenceAngle);
                double ph = SpectrumStatistics.getWeightedMean(samplePhaseAngle);
                // TODO s/c distance
                double dist = 0;


                vtkPolyData tmp = earth.computeFrustumIntersection(
                        spacecraftPosition, frustum1, frustum2, frustum3, frustum4);
                if (tmp != null) {
                    double[] bbox = tmp.GetBounds();
                    System.out.println("file " + iFile++ + ": " + bbox[0] + ", " + bbox[1] + ", " + bbox[2] + ", " + bbox[3]);
                    // write min and max for x, y, z, time, emission, incidence, phase, distance.
                    bw.write(thisFileName + " " + bbox[0] + " " + bbox[1] + " " + bbox[2] + " " + bbox[3] + " " + bbox[4] + " " + bbox[5] +" " + reader.getStartTime() +
                            " " + reader.getStopTime() +" " + em +" " + em + " "+ inc+" " + inc + " "+ ph +" " + ph + " "+ dist + " " + dist +" \n");
                }


            }
            bw.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


    }
}
