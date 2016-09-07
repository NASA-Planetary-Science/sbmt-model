package edu.jhuapl.sbmt.model.eros;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkDataArray;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkUnsignedCharArray;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.util.Properties;

public class LineamentModel extends AbstractModel
{
    private HashMap<Integer, Lineament> idToLineamentMap = new HashMap<Integer, Lineament>();
    private HashMap<Integer, Lineament> cellIdToLineamentMap = new HashMap<Integer, Lineament>();
    private vtkPolyData lineaments;
    private List<vtkProp> lineamentActors = new ArrayList<vtkProp>();
    private vtkActor lineamentActor;
    private int[] defaultColor = {255, 0, 255, 255}; // RGBA, default to purple

    public static class Lineament
    {
        public int cellId;
        public String name = "";
        public int id;
        public List<Double> lat = new ArrayList<Double>();
        public List<Double> lon = new ArrayList<Double>();
        public List<Double> rad = new ArrayList<Double>();
        //public List<Double> x = new ArrayList<Double>();
        //public List<Double> y = new ArrayList<Double>();
        //public List<Double> z = new ArrayList<Double>();
        //public BoundingBox bb = new BoundingBox();
    }

    private void initialize()
    {
        if (lineamentActor == null)
        {
            try
            {
                loadModel();

                createPolyData();

                vtkPolyDataMapper lineamentMapper = new vtkPolyDataMapper();
                lineamentMapper.SetInputData(lineaments);
                //lineamentMapper.SetResolveCoincidentTopologyToPolygonOffset();
                //lineamentMapper.SetResolveCoincidentTopologyPolygonOffsetParameters(-1000.0, -1000.0);

                lineamentActor = new vtkActor();
                lineamentActor.SetMapper(lineamentMapper);

                // By default do not show the lineaments
                //lineamentActors.add(lineamentActor);

            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("Number of lineaments: " + this.idToLineamentMap.size());
        }
    }

    private void loadModel() throws NumberFormatException, IOException
    {
        InputStream is = getClass().getResourceAsStream("/edu/jhuapl/sbmt/data/LinearFeatures.txt");
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader in = new BufferedReader(isr);

        String line;
        while ((line = in.readLine()) != null)
        {
            String [] tokens = line.split("\t");

            if (tokens.length < 5)
            {
                System.out.println(tokens.length);
                for (int i=0;i<tokens.length;++i)
                    System.out.println(tokens[i]);
                continue;
            }

            String name = tokens[0];
            Integer id = Integer.parseInt(tokens[1]);
            double lat = Double.parseDouble(tokens[2]) * Math.PI / 180.0;
            double lon = (360.0-Double.parseDouble(tokens[3])) * Math.PI / 180.0;
            double rad = Double.parseDouble(tokens[4]);

            if (!this.idToLineamentMap.containsKey(id))
            {
                this.idToLineamentMap.put(id, new Lineament());
            }

            Lineament lin = this.idToLineamentMap.get(id);
            lin.name = name;
            lin.id = id;
            lin.lat.add(lat);
            lin.lon.add(lon);
            lin.rad.add(rad);

            // Convert to xyz
            //double x = rad * Math.cos( lon ) * Math.cos( lat );
            //double y = rad * Math.sin( lon ) * Math.cos( lat );
            //double z = rad * Math.sin( lat );

            //lin.x.add(x);
            //lin.y.add(y);
            //lin.z.add(z);

            // Update the bounds of the lineaments
            //lin.bb.update(x, y, z);
        }

        in.close();
    }

    /*
    public List<Lineament> getLineamentsWithinBox(BoundingBox box)
    {
        List<Lineament> array = new ArrayList<Lineament>();
        for (Integer id : this.idToLineamentMap.keySet())
        {
            Lineament lin =    this.idToLineamentMap.get(id);
            if (lin.bb.intersects(box))
                array.add(lin);
        }
        return array;
    }
    */

    private void createPolyData()
    {
        lineaments = new vtkPolyData();
        vtkPoints points = new vtkPoints();
        vtkCellArray lines = new vtkCellArray();
        vtkUnsignedCharArray colors = new vtkUnsignedCharArray();

        colors.SetNumberOfComponents(4);

        vtkIdList idList = new vtkIdList();

        int c=0;
        int cellId = 0;
        for (Integer id : this.idToLineamentMap.keySet())
        {
            Lineament lin =    this.idToLineamentMap.get(id);
            lin.cellId = cellId;

            int size = lin.lat.size();
            idList.SetNumberOfIds(size);

            for (int i=0;i<size;++i)
            {
                double lat = lin.lat.get(i);
                double lon = lin.lon.get(i);
                double rad = lin.rad.get(i);
                double x = rad * Math.cos( lon ) * Math.cos( lat );
                double y = rad * Math.sin( lon ) * Math.cos( lat );
                double z = rad * Math.sin( lat );

                points.InsertNextPoint(x, y, z);
                idList.SetId(i, c);
                ++c;
            }

            lines.InsertNextCell(idList);
            colors.InsertNextTuple4(defaultColor[0],defaultColor[1],defaultColor[2],defaultColor[3]);

            cellIdToLineamentMap.put(cellId, lin);
            ++cellId;
        }

        lineaments.SetPoints(points);
        lineaments.SetLines(lines);
        lineaments.GetCellData().SetScalars(colors);
    }

    public Lineament getLineament(int cellId)
    {
        return this.cellIdToLineamentMap.get(cellId);
    }

    public void setLineamentColor(int cellId, int[] color)
    {
        lineaments.GetCellData().GetScalars().SetTuple4(cellId, color[0], color[1], color[2], color[3]);
        lineaments.Modified();
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void setsAllLineamentsColor(int[] color)
    {
        int numLineaments = this.cellIdToLineamentMap.size();
        vtkDataArray colors = lineaments.GetCellData().GetScalars();

        for (int i=0; i<numLineaments; ++i)
            colors.SetTuple4(i, color[0], color[1], color[2], color[3]);

        lineaments.Modified();
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void setMSIImageLineamentsColor(int cellId, int[] color)
    {
        int numLineaments = this.cellIdToLineamentMap.size();
        String name = cellIdToLineamentMap.get(cellId).name;
        vtkDataArray colors = lineaments.GetCellData().GetScalars();

        for (int i=0; i<numLineaments; ++i)
            if (cellIdToLineamentMap.get(i).name.equals(name))
                    colors.SetTuple4(i, color[0], color[1], color[2], color[3]);

        lineaments.Modified();
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void setOffset(double offset)
    {
        if (lineamentActors.isEmpty())
            initialize();

        int ptId=0;
        vtkPoints points = lineaments.GetPoints();

        for (Integer id : this.idToLineamentMap.keySet())
        {
            Lineament lin =    this.idToLineamentMap.get(id);

            int size = lin.lat.size();

            for (int i=0;i<size;++i)
            {
                double x = (lin.rad.get(i)+offset) * Math.cos( lin.lon.get(i) ) * Math.cos( lin.lat.get(i) );
                double y = (lin.rad.get(i)+offset) * Math.sin( lin.lon.get(i) ) * Math.cos( lin.lat.get(i) );
                double z = (lin.rad.get(i)+offset) * Math.sin( lin.lat.get(i) );
                points.SetPoint(ptId, x, y, z);
                ++ptId;
            }
        }

        lineaments.Modified();
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void setShowLineaments(boolean show)
    {
        if (show)
        {
            if (lineamentActors.isEmpty())
            {
                initialize();

                lineamentActors.add(lineamentActor);
                this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
            }
        }
        else
        {
            if (!lineamentActors.isEmpty())
            {
                lineamentActors.clear();
                this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
            }
        }

    }

    public List<vtkProp> getProps()
    {
        return lineamentActors;
    }

    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        LineamentModel.Lineament lin = getLineament(cellId);
        if (lin != null)
            return "Lineament " + lin.id + " mapped on MSI image " + lin.name + " contains " + lin.lat.size() + " vertices";
        else
            return "";
    }

    public void setLineWidth(double value)
    {
        if (lineamentActors.isEmpty())
            initialize();
        lineamentActor.GetProperty().SetLineWidth(value);
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }
}
