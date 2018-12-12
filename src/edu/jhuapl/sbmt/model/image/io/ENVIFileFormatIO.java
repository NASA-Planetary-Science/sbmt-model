package edu.jhuapl.sbmt.model.image.io;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import vtk.vtkImageData;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.sbmt.model.image.ImageSource;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;
import edu.jhuapl.sbmt.util.ImageDataUtil;
import edu.jhuapl.sbmt.util.VtkENVIReader;

public class ENVIFileFormatIO
{
    PerspectiveImage image;

    public ENVIFileFormatIO(PerspectiveImage image)
    {
        this.image = image;
    }


    protected void loadEnviFile(String name)
    {
        String imageFile = null;
        if (image.getKey().source == ImageSource.IMAGE_MAP)
            imageFile = FileCache.getFileFromServer(name).getAbsolutePath();
        else
            imageFile = image.getKey().name;
        if (image.getRawImage() == null)
            image.setRawImage(new vtkImageData());

        VtkENVIReader reader = new VtkENVIReader();
        reader.SetFileName(imageFile);
        reader.Update();
        image.getRawImage().DeepCopy(reader.GetOutput());
        image.setMinValue(reader.getMinValues()[0]);
        image.setMaxValue(reader.getMaxValues()[0]);
    }

    public void exportAsEnvi(
            String enviFilename, // no extensions
            String interleaveType, // "bsq", "bil", or "bip"
            boolean hostByteOrder) throws IOException
    {
        // Check if interleave type is recognized
        switch(interleaveType){
        case "bsq":
        case "bil":
        case "bip":
            break;
        default:
            System.out.println("Interleave type " + interleaveType + " unrecognized, aborting exportAsEnvi()");
            return;
        }

        // Create output stream for header (.hdr) file
        FileOutputStream fs = null;
        try {
            fs = new FileOutputStream(enviFilename + ".hdr");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        OutputStreamWriter osw = new OutputStreamWriter(fs);
        BufferedWriter out = new BufferedWriter(osw);

        // Write the fields of the header
        out.write("ENVI\n");
        out.write("samples = " + image.getImageWidth() + "\n");
        out.write("lines = " + image.getImageHeight() + "\n");
        out.write("bands = " + image.getImageDepth() + "\n");
        out.write("header offset = " + "0" + "\n");
        out.write("data type = " + "4" + "\n"); // 1 = byte, 2 = int, 3 = signed int, 4 = float
        out.write("interleave = " + interleaveType + "\n"); // bsq = band sequential, bil = band interleaved by line, bip = band interleaved by pixel
        out.write("byte order = "); // 0 = host(intel, LSB first), 1 = network (IEEE, MSB first)
        if(hostByteOrder){
            // Host byte order
            out.write("0" + "\n");
        }else{
            // Network byte order
            out.write("1" + "\n");
        }
        out.write(getEnviHeaderAppend());
        out.close();

        // Configure byte buffer & endianess
        ByteBuffer bb = ByteBuffer.allocate(4*image.getImageWidth()*image.getImageHeight()*image.getImageDepth()); // 4 bytes per float
        if(hostByteOrder){
            // Little Endian = LSB stored first
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }else{
            // Big Endian = MSB stored first
            bb.order(ByteOrder.BIG_ENDIAN);
        }

        // Write pixels to byte buffer
        // Remember, VTK origin is at bottom left while ENVI origin is at top left
        float[][][] imageData = ImageDataUtil.vtkImageDataToArray3D(image.getRawImage());
        switch(interleaveType)
        {
        case "bsq":
            // Band sequential: col, then row, then depth
            for(int depth = 0; depth < image.getImageDepth(); depth++)
            {
                //for(int row = imageHeight-1; row >= 0; row--)
                for(int row=0; row < image.getImageHeight(); row ++)
                {
                    for(int col = 0; col < image.getImageWidth(); col++)
                    {
                        bb.putFloat(imageData[depth][row][col]);
                    }
                }
            }
            break;
        case "bil":
            // Band interleaved by line: col, then depth, then row
            //for(int row=imageHeight-1; row >= 0; row--)
            for(int row=0; row < image.getImageHeight(); row ++)
            {
                for(int depth=0; depth < image.getImageDepth(); depth++)
                {
                    for(int col=0; col < image.getImageWidth(); col++)
                    {
                        bb.putFloat(imageData[depth][row][col]);
                    }
                }
            }
            break;
        case "bip":
            // Band interleaved by pixel: depth, then col, then row
            //for(int row=imageHeight-1; row >= 0; row--)
            for(int row=0; row < image.getImageHeight(); row ++)
            {
                for(int col=0; col < image.getImageWidth(); col++)
                {
                    for(int depth=0; depth < image.getImageDepth(); depth++)
                    {
                        bb.putFloat(imageData[depth][row][col]);
                    }
                }
            }
            break;
        }

        // Create output stream and write contents of byte buffer
        FileChannel fc = null;
        try {
            fc = new FileOutputStream(enviFilename).getChannel();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        bb.flip(); // flip() is a misleading name, nothing is being flipped.  Buffer end is set to curr pos and curr pos set to beginning.
        fc.write(bb);
        fc.close();
    }

    protected String getEnviHeaderAppend()
    {
        return image.getEnviHeaderAppend();
    }

    public int loadNumSlices(String filename)
    {
        // Get the number of bands from the ENVI header
//        String name = getEnviFileFullPath();

        String imageFile = null;
        if (image.getKey().source == ImageSource.IMAGE_MAP)
            imageFile = FileCache.getFileFromServer(filename).getAbsolutePath();
        else
            imageFile = image.getKey().name;
        if (imageFile == null) return 1;

        VtkENVIReader reader = new VtkENVIReader();
        reader.SetFileName(imageFile);
        image.setImageDepth(reader.getNumBands());
        // for multislice images, set slice to middle slice
        if (image.getImageDepth() > 1)
            image.setCurrentSlice(image.getImageDepth() / 2);

        return image.getImageDepth();
    }

}
