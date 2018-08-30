package edu.jhuapl.sbmt.model.boundedobject.hyperoctree;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import edu.jhuapl.sbmt.lidar.DataOutputStreamPool;
import edu.jhuapl.sbmt.lidar.hyperoctree.HyperBox;
import edu.jhuapl.sbmt.lidar.hyperoctree.HyperException;
import edu.jhuapl.sbmt.lidar.hyperoctree.HyperException.HyperDimensionMismatchException;

public class BoundedObjectHyperTreeNode
{

    BoundedObjectHyperTreeNode parent;
    BoundedObjectHyperTreeNode[] children;
    Path path;
    HyperBox bbox;
    int maxPointsPerNode;
    DataOutputStreamPool pool;
    boolean isLeaf = true;
    private int numObjs = 0;

    public BoundedObjectHyperTreeNode(BoundedObjectHyperTreeNode parent, Path path,
            HyperBox bbox, int maxPoints, DataOutputStreamPool pool)
    {
        this.parent=parent;
        this.path=path;
        this.bbox=bbox;
        this.pool=pool;
        this.maxPointsPerNode=maxPoints;
        children=new BoundedObjectHyperTreeNode[(int)Math.pow(2, bbox.getDimension())];
        for (int i=0; i<children.length; i++)
            children[i]=null;
        path.toFile().mkdir();

        writeBoundsFile();
    }

    protected BoundedObjectHyperTreeNode createNewChild(int i)
    {
        try
        {
            return new BoundedObjectHyperTreeNode(this, getChildPath(i), getChildBounds(i), maxPointsPerNode, pool);
        }
        catch (HyperDimensionMismatchException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    protected HyperBox getChildBounds(int i) throws HyperDimensionMismatchException
    {
        double[] min=new double[getDimension()];
        double[] max=new double[getDimension()];
        for (int j=0; j<getDimension(); j++)
        {
            int whichBit=(int)Math.pow(2,j);
            if ((i&whichBit)>>j==0)
            {
                min[j]=bbox.getMin()[j];
                max[j]=bbox.getMid()[j];
            }
            else
            {
                min[j]=bbox.getMid()[j];
                max[j]=bbox.getMax()[j];
            }
        }
        return new HyperBox(min, max);
    }

    public int getDimension()
    {
        return bbox.getDimension();
    }

    public static HyperBoundedObject createNewBoundedObject(DataInputStream stream, int dim) throws HyperDimensionMismatchException
    {
        try
        {
            return new HyperBoundedObject(stream, dim);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    // TODO  implement adding of objects
    public boolean add(HyperBoundedObject obj) throws HyperException, IOException
    {
        if (!isLeaf) {
            for (int i=0; i<getNumberOfChildren(); i++)
                if (children[i].add(obj))
                    return true;
        } else {
//            if (isInside(obj)) {
                obj.write(pool.getStream(getDataFilePath()));
                numObjs ++;
                return true;
//            }
        }
        return false;

    }

    public int getNumberOfChildren()
    {
        return children.length;
    }

    public boolean isInside(HyperBoundedObject image) throws HyperException
    {
        return bbox.intersects(image.getBbox());
    }

    public int getNumberOfObjects()
    {
        return numObjs;
    }


    public void split() throws HyperException, IOException
    {
        pool.closeStream(getDataFilePath());
        for (int i=0; i<getNumberOfChildren(); i++)
            children[i]=createNewChild(i); // this creates a bounding box for where it is in comparison to its parent
        DataInputStream instream=new DataInputStream(new BufferedInputStream(new FileInputStream(getDataFilePath().toFile())));
        while (instream.available()>0) // for every object in the node
        {
            int dim = this.getDimension();
            HyperBoundedObject obj = createNewBoundedObject(instream, dim);

            for (int i=0; i<getNumberOfChildren() ; i++)
            {
                if (children[i].getBoundingBox().intersects(obj.getBbox()))
                {
                    children[i].add(obj);
                }
            }
        }

        instream.close();
        isLeaf=false;
        deleteDataFile();
    }

    void deleteDataFile() {
        getDataFilePath().toFile().delete();
    }

    private HyperBox getBoundingBox()
    {
        return bbox;
    }

    public boolean childExists(int i)
    {
        return children[i]!=null;
    }

    public BoundedObjectHyperTreeNode getChild(int i)
    {
        return children[i];
    }

    public boolean isLeaf()
    {
        return isLeaf;
    }

    public Path getPath()
    {
        return path;
    }

    public Path getChildPath(int i)
    {
        return path.resolve(String.valueOf(i));
    }

    public Path getDataFilePath()
    {
        return path.resolve("data");
    }

    public Path getBoundsFilePath()
    {
        return path.resolve("bounds");
    }

    public void writeBoundsFile()
    {
        try
        {
            DataOutputStream stream=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(getBoundsFilePath().toFile())));
            double[] min=bbox.getMin();
            double[] max=bbox.getMax();
            for (int i=0; i<bbox.getDimension(); i++)
            {
                stream.writeDouble(min[i]);
                stream.writeDouble(max[i]);
            }
            stream.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static double[] readBoundsFile(Path boundsFilePath, int dimension)

    {
        double[] bounds=new double[dimension*2];
        try
        {
            DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(boundsFilePath.toFile())));
            for (int i=0; i<dimension; i++)
            {
                bounds[2*i+0]=stream.readDouble();
                bounds[2*i+1]=stream.readDouble();
            }
            stream.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return bounds;
    }



}
