package edu.jhuapl.sbmt.model.image.io;

import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import edu.jhuapl.sbmt.model.image.PerspectiveImage;

import nom.tam.fits.FitsException;

public class PerspectiveImageIO
{
    ENVIFileFormatIO enviIO;
    PNGFileFormatIO pngIO;
    FitsFileFormatIO fitsIO;
    String filePath;
    PerspectiveImage image;

    public PerspectiveImageIO(PerspectiveImage image)
    {
        this.image = image;
        this.enviIO = new ENVIFileFormatIO(image);
        this.pngIO = new PNGFileFormatIO(image);
        this.fitsIO = new FitsFileFormatIO(image);

    }

    public int loadNumSlices(String filename, PerspectiveImageIOSupportedFiletypes type)
    {
//        PerspectiveImageIOSupportedFiletypes type = PerspectiveImageIOSupportedFiletypes.getTypeForExtension(FilenameUtils.getExtension(filename));
        switch (type)
        {
        case PNG:
            return pngIO.loadNumSlices(filename);
        case ENVI:
            return enviIO.loadNumSlices(filename);
        case FITS:
            return fitsIO.loadNumSlices(filename);
        default:
            return 1;
        }
    }


    public void loadFromFile(String filename) throws FitsException, IOException
    {
        System.out.println("PerspectiveImageIO: loadFromFile: filename is " + filename);
        PerspectiveImageIOSupportedFiletypes type = PerspectiveImageIOSupportedFiletypes.getTypeForExtension(FilenameUtils.getExtension(filename));
        switch (type)
        {
        case PNG:
            pngIO.loadPngFile(filename);
            break;
        case ENVI:
            enviIO.loadEnviFile(filename);
            enviIO.loadNumSlices(filename);
        case FITS:
            fitsIO.loadFitsFiles(new String[] { filename }, true);
            fitsIO.loadNumSlices(filename);
        default:
            break;
        }
    }

    public void loadFromFiles(String[] filenames, boolean transposeData) throws FitsException, IOException
    {
        fitsIO.loadFitsFiles(filenames, transposeData);
        fitsIO.loadNumSlices(filenames[0]);
    }


    public ENVIFileFormatIO getEnviIO()
    {
        return enviIO;
    }


    public FitsFileFormatIO getFitsIO()
    {
        return fitsIO;
    }

}
