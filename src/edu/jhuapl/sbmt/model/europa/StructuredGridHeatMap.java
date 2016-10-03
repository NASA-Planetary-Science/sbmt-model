package edu.jhuapl.sbmt.model.europa;

import vtk.vtkActor;
import vtk.vtkColorTransferFunction;
import vtk.vtkDataArray;
import vtk.vtkDataSetMapper;
import vtk.vtkLookupTable;
import vtk.vtkProperty;
import vtk.vtkStructuredGrid;
import vtk.vtkUnsignedCharArray;

public class StructuredGridHeatMap
{
    vtkDataSetMapper heatmapMapper = new vtkDataSetMapper();
    vtkActor heatmapActor = new vtkActor();
    private vtkStructuredGrid polyData;
    private vtkLookupTable lut;
    private int tableSize;

    double[] orange = { 1.0, 0.7, 0.0 };

    public StructuredGridHeatMap(vtkStructuredGrid polyData)
    {
        this.polyData = polyData;
//        this.polyData.DeepCopy(polyData);
//        this.lut = new vtkLookupTable();
        this.tableSize = polyData.GetNumberOfCells();
//        this.tableSize = polyData.GetNumberOfPoints();
    }

    public StructuredGridHeatMap makeDefaultCLUT()
    {
        double[] range = {0.0, 1.0};
//
        this.lut = new vtkLookupTable();
        this.lut.SetTableRange(range);
//        this.lut.SetHueRange(0.0, 1.0);
        lut.SetNumberOfColors(8);
        double opacity = 1.0;
        lut.SetTableValue(0, 1.0, 1.0, 0.0, opacity);
        lut.SetTableValue(1, 1.0, 0.75, 0.0, opacity);
        lut.SetTableValue(2, 1.0, 0.5, 0.0, opacity);
        lut.SetTableValue(3, 1.0, 0.0, 0.25, opacity);
        lut.SetTableValue(4, 1.0, 0.0, 0.5, opacity);
        lut.SetTableValue(5, 1.0, 0.0, 0.75, opacity);
        lut.SetTableValue(6, 1.0, 0.0, 1.0, opacity);
        lut.SetTableValue(7, 0.75, 0.0, 1.0, opacity);
        ((vtkLookupTable)heatmapMapper.GetLookupTable()).ForceBuild();
        return this;
    }

    public StructuredGridHeatMap makeCLUTFromCTF(vtkColorTransferFunction ctf)
    {
//       Nothing Here Yet
       return this;
    }


    public void setupMapperAndActor()
    {
//        vtkLookupTable lookupTable = new vtkLookupTable();

//        this.heatmapMapper.UseLookupTableScalarRangeOn();
//        this.heatmapMapper.SetLookupTable(this.lut);

        this.heatmapMapper.SetInputData(this.polyData);

//        this.heatmapMapper.SetScalarModeToUseCellData();
//        this.heatmapMapper.SetScalarModeToUsePointData();

        this.heatmapMapper.Update();

        this.heatmapActor.SetMapper(this.heatmapMapper);
        vtkProperty smallBodyProperty = this.heatmapActor.GetProperty();
        smallBodyProperty.SetInterpolationToGouraud();
//        smallBodyProperty.SetColor(orange);

//        smallBodyProperty.SetRepresentationToWireframe();

        smallBodyProperty.SetRepresentationToSurface();
        smallBodyProperty.SetBackfaceCulling(0);

//        smallBodyProperty.SetOpacity(0.9);

    }

//    public void createHeatMap(vtkDataArray scalars)
    public void createHeatMap(double[] scalars)
    {
        vtkDataArray array = null;
        vtkUnsignedCharArray colors = new vtkUnsignedCharArray();
        mapScalarsThroughLookupTable(scalars, this.lut, colors);

        array = colors;
        this.polyData.GetCellData().SetScalars(array);
//        this.polyData.GetPointData().SetScalars(array);

    }

    private void mapScalarsThroughLookupTable(double[] scalars,
                                             vtkLookupTable lut,
                                             vtkUnsignedCharArray colors)
    {
//        double nullValue = scalars.GetRange()[0];
        double nullValue = 0.0;
        int numberValues = scalars.length;
        colors.SetNumberOfComponents(3);
        colors.SetNumberOfTuples(numberValues);
        double[] rgb = new double[3];
        for (int i=0; i<numberValues; ++i)
        {
//            double v = scalars.GetTuple1(i);
            double v = scalars[i];
            if (v != nullValue)
            {
                lut.GetColor(v, rgb);
                colors.SetTuple3(i, 255.0*rgb[0], 255.0*rgb[1], 255.0*rgb[2]);
//                colors.SetTuple3(i, 255.0, 255.0, 255.0*0.0);
//                System.out.print("Scalar Value: " + v);
//                System.out.print("RGB Value: " + rgb[0] + " " + rgb[1] + " " + rgb[2] + "\r");
            }
            else
            {
                // Map null values to white
                colors.SetTuple3(i, 255.0, 255.0, 255.0);
            }
        }
    }

    public int getLUTSize()
    {
        return this.tableSize;
    }

    public vtkLookupTable getLUT()
    {
        return this.lut;
    }

    public vtkDataSetMapper getHeatMapMapper()
    {
        return this.heatmapMapper;
    }

    public vtkActor getHeatMapActor()
    {
        return this.heatmapActor;
    }

    public void setOpacity(double opacity)
    {
        if (opacity <= 1.0 && opacity >= 0.0)
            this.heatmapActor.GetProperty().SetOpacity(opacity);
    }

    public void setInterpolationToGouraud()
    {
        this.heatmapActor.GetProperty().SetInterpolationToGouraud();
    }

}
