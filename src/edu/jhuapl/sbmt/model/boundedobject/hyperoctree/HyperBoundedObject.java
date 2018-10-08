package edu.jhuapl.sbmt.model.boundedobject.hyperoctree;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import edu.jhuapl.sbmt.lidar.hyperoctree.HyperBox;
import edu.jhuapl.sbmt.lidar.hyperoctree.HyperException.HyperDimensionMismatchException;

public class HyperBoundedObject
{

    protected String name;
    private int fileNum;
    protected HyperBox bbox;
    int dim; // number of things to search on (i.e. for images, it is 4: x, y, z, time)

    public HyperBoundedObject(DataInputStream stream, int dim) throws HyperDimensionMismatchException, IOException
    {
        this.dim = dim;
        read(stream);
    }

    public HyperBoundedObject(String objName, int objId, HyperBox objBBox)
    {
        name = objName;
        setFileNum(objId);
        bbox = objBBox;
        dim = objBBox.getDimension();
    }


    // TODO fix the read/write functions based on file format

    public void read(DataInputStream inputStream) throws IOException, HyperDimensionMismatchException
    {
        double[] data = new double[dim * 2]; // min and max for each dimension
//        double[] data = new double[8];
        for (int i=0; i<data.length; i++)
            data[i]=inputStream.readDouble();
        setFileNum(inputStream.readInt());

        double[] mins = new double[dim];
        double[] maxs = new double[dim];
        for (int i = 0; i < dim; i++) {
            mins[i] = data[i*2];
            maxs[i] = data[i*2 + 1];
        }
        bbox = new HyperBox(mins, maxs);
//        bbox = new HyperBox(new double[]{data[0], data[2], data[4], data[6]}, new double[]{data[1], data[3], data[5], data[7]});

    }

    public void write(DataOutputStream outputStream) throws IOException
    {
        double[] data = getData();
        for (int i=0; i<data.length; i++)
            outputStream.writeDouble(data[i]);
        outputStream.writeInt(getFileNum());
    }

    private double[] getData()
    {
        return bbox.getBounds();
    }

    public int getSizeInBytes()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public double getCoordinate(int i)
    {
        return getData()[i];
    }

    public HyperBox getBbox()
    {
        return bbox;
    }

    public int getFileNum()
    {
        return fileNum;
    }

    public void setFileNum(int fileNum)
    {
        this.fileNum = fileNum;
    }

}
