package edu.jhuapl.sbmt.model.eros.nis.util;

import java.io.File;
import java.io.IOException;

import edu.jhuapl.sbmt.core.body.ISmallBodyModel;
import edu.jhuapl.sbmt.model.eros.nis.NIS;
import edu.jhuapl.sbmt.model.eros.nis.NISSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.search.SpectraHierarchicalSearchSpecification;
import edu.jhuapl.sbmt.spectrum.model.io.SpectrumInstrumentMetadataIO;

public class LocalNISSpectrum extends NISSpectrum
{
    private static NIS nis=new NIS();

    public LocalNISSpectrum(File nisFile, ISmallBodyModel smallBodyModel, SpectraHierarchicalSearchSpecification searchSpec)
            throws IOException
    {
        super(nisFile.getAbsolutePath(), (SpectrumInstrumentMetadataIO)searchSpec, smallBodyModel.getBoundingBoxDiagonalLength(), nis);
        serverpath=nisFile.toString();
    }

}
