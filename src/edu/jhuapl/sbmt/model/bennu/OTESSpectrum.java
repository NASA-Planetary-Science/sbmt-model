package edu.jhuapl.sbmt.model.bennu;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.InfoFileReader;
import edu.jhuapl.sbmt.model.spectrum.BasicSpectrum;
import edu.jhuapl.sbmt.model.spectrum.SpectralInstrument;

public class OTESSpectrum extends BasicSpectrum
{
    boolean footprintGenerated=false;
    File infoFile;

    public OTESSpectrum(String filename, SmallBodyModel smallBodyModel,
            SpectralInstrument instrument) throws IOException
    {
        super(filename, smallBodyModel, instrument);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void saveSpectrum(File file) throws IOException
    {
        throw new IOException("Not implemented.");
    }

    protected String getInfoFileServerPath()
    {
        return getServerPath()+"/"+getInfoFilePathRelativeToSpectrumFile();
    }

    protected String getInfoFilePathRelativeToSpectrumFile()
    {
        return "/../infofiles/"+FilenameUtils.getBaseName(getServerPath())+".info";
    }


    @Override
    public void generateFootprint()
    {
        if (!footprintGenerated)
        {

        }
    }

    protected Frustum readPointingFromInfoFile()
    {
        infoFile=FileCache.getFileFromServer(getInfoFileServerPath());
        //
        InfoFileReader reader=new InfoFileReader();
        reader.setFileName(infoFile.getAbsolutePath());
        reader.read();
        double[] origin=reader.getSpacecraftPosition();
        double[] fovVec=reader.getFrustum2();   // for whatever reason, frustum2 contains the vector along the field of view cone
        double[] boresight=reader.getBoresightDirection();
        Rotation rotation=new Rotation(new Vector3D(boresight), Math.PI/2.);
        double[] n=fovVec;
        double[] w=rotation.applyTo(new Vector3D(n)).toArray();
        double[] s=rotation.applyTo(new Vector3D(w)).toArray();
        double[] e=rotation.applyTo(new Vector3D(s)).toArray(); // don't worry about possible loss of precision from repeated application of the same rotation, for now

        return new Frustum(origin, n, e, w, s);    // the field of view is circular, so just let n,e,w,s be the corners of the frustum, with the understanding that the fov CIRCUMSCRIBES the frustum
    }


}