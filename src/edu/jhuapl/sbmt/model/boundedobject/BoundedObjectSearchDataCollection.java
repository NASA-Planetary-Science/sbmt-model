package edu.jhuapl.sbmt.model.boundedobject;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import vtk.vtkProp;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.model.PolyhedralModel;

public class BoundedObjectSearchDataCollection extends AbstractModel
{

    private List<vtkProp> actors = new ArrayList<vtkProp>();

    public BoundedObjectSearchDataCollection(PolyhedralModel smallBodyModel)
    {

    }

    public boolean isLoading()
    {
        return false;
    }

//    public double getOffsetScale()
//    {
//        t
//    }



    public void setData() {

    }


    protected void runQuery()
    {

    }


    private void skip(DataInputStream in, int n) throws IOException
    {
        for (int i = 0; i < n; ++i)
        {
            in.readByte();
        }
    }

    BiMap<Integer, String> localFileMap=HashBiMap.create();


    public void hideObject(int trackId, boolean hide)
    {

    }

    public void hideOtherObjectsExcept(int trackId)
    {

    }

    public void hideAllObjects()
    {

    }

    public void showAllObjects()
    {

    }

    public boolean isObjectHidden(int trackId)
    {
        // TODO
        return true;
    }

    private int getDisplayPointIdFromOriginalPointId(int ptId)
    {
        // TOOD
        return 1;
    }




    private void removeObject(int trackId)
    {

    }


    /**
     * select a point
     * @param ptId point id which must be id of a displayed point, not an original point
     */
    public void selectObject(int ptId)
    {

    }

    public void updateSelectedObject()
    {

    }

    @Override
    public List<vtkProp> getProps()
    {
       return actors;
    }

//    public double[] getSelectedObject()
//    {
//
//    }


}
