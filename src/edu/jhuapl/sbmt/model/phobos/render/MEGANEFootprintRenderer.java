package edu.jhuapl.sbmt.model.phobos.render;

import java.awt.Color;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Vector;

import javax.swing.SwingUtilities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import edu.jhuapl.saavtk.color.provider.ColorProvider;
import edu.jhuapl.saavtk.colormap.Colormap;
import edu.jhuapl.saavtk.colormap.Colormaps;
import edu.jhuapl.saavtk.model.plateColoring.ColoringData;
import edu.jhuapl.saavtk.model.plateColoring.ColoringDataFactory;
import edu.jhuapl.saavtk.model.plateColoring.ColoringDataUtils;
import edu.jhuapl.saavtk.model.plateColoring.FacetColoringData;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.saavtk.util.file.Indexable;
import edu.jhuapl.saavtk.util.file.IndexableTuple;
import edu.jhuapl.saavtk2.polydata.select.PolyDataRemoveSelectedCells;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.model.phobos.model.MEGANEFootprint;
import edu.jhuapl.sbmt.model.phobos.model.MEGANEFootprintFacet;
import vtk.vtkActor;
import vtk.vtkFloatArray;
import vtk.vtkLookupTable;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkStringArray;
import vtk.vtkUnsignedCharArray;

public class MEGANEFootprintRenderer
{
	private vtkPolyData smallBodyPolyData;
	private SmallBodyModel smallBodyModel;
	private MEGANEFootprint footprint;
	private vtkActor footprintActor;
	private vtkPolyData footprintPolyData = new vtkPolyData();
//	private SimpleLogger logger = SimpleLogger.getInstance();
	private ImmutableList<ColoringData> allColoringData;
	private double minValue = Double.MAX_VALUE, maxValue = Double.MIN_VALUE;
	private PropertyChangeSupport pcs;
	private int i=0;
	private vtkStringArray idArray;
    private vtkStringArray stringArray;
    private vtkLookupTable lut;
    private ColorProvider gcp;
    private FacetColoringData[] plateDataInsidePolydata;
    private Colormap colormap;

	public MEGANEFootprintRenderer(MEGANEFootprint footprint, SmallBodyModel smallBodyModel, PropertyChangeSupport pcs)
	{
		this.smallBodyModel = smallBodyModel;
		this.footprint = footprint;
		this.footprintActor = new vtkActor();
		this.smallBodyPolyData = new vtkPolyData();
		this.pcs = pcs;
		smallBodyPolyData.DeepCopy(smallBodyModel.getSmallBodyPolyData());
//		logger.setLogFormat("%1$tF %1$tT.%1$tL %4$-7s %2$s %5$s%6$s%n");
		PolyDataRemoveSelectedCells removeCells = new PolyDataRemoveSelectedCells();
		removeCells.setIndicesToRemove(footprint.getCellIDs());
		footprintPolyData = removeCells.apply(smallBodyPolyData);
        allColoringData = getFootprintColoringData();	//this will use information from the footprint
		updateColorFromPlate();
	}

	private vtkLookupTable updateColorFromPlate()
	{
		i=0;
		DecimalFormat formatter = new DecimalFormat("##.##");
		String coloringPlateName = "Projected Area/Range^2";
		//grab coloring data for plates in the footprint
		if (plateDataInsidePolydata == null)
			plateDataInsidePolydata = getColoringDataForFootprint();	//contains coloring data for each cell in this footprint
		colormap = Colormaps.getNewInstanceOfBuiltInColormap(Colormaps.getDefaultColormapName());
		ColoringData globalColoringData = allColoringData.get(0);
		colormap.setRangeMin(minValue);
		colormap.setRangeMax(maxValue);
//		colormap.setRangeMax(1.0E-3);
		colormap.setNumberOfLevels(32);
		//create and setup the LUT
		lut = new vtkLookupTable();
		lut.SetIndexedLookup(1);
        lut.SetNumberOfTableValues(plateDataInsidePolydata.length);
        lut.Build();

        //now populated the LUT using the coloring in the FacetColoringData

        idArray = new vtkStringArray();
        stringArray = new vtkStringArray();
		for (FacetColoringData coloringData : plateDataInsidePolydata)	//for each facet in the set of facets...
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					if (!(i%100 == 0)) return;
					double percentage = (double)i/(double)plateDataInsidePolydata.length*100;
					footprint.setStatus("LD: " + formatter.format(percentage) + "%");
					MEGANEFootprintRenderer.this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
				}
			});

			double[] coloringValuesFor = null;
			try
			{
				coloringValuesFor = coloringData.getColoringValuesFor(coloringPlateName);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Color c = getColorForValue(coloringValuesFor[0]);
			lut.SetTableValue(i, new double[] {((double)c.getRed())/255.0, ((double)c.getGreen())/255.0, ((double)c.getBlue())/255.0});
//			lut.SetAnnotation("" + i++, ""+ coloringData.getCellId());
			stringArray.InsertNextValue(""+ coloringData.getCellId());
			idArray.InsertNextValue("" + i++);
		}
		lut.SetAnnotations(idArray, stringArray);
		SwingUtilities.invokeLater(() -> {
			footprint.setStatus("Loaded");
			MEGANEFootprintRenderer.this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
		});

		Vector<Integer> cellIds = footprint.getCellIDs();
		vtkUnsignedCharArray cellData = new vtkUnsignedCharArray();
		cellData.SetNumberOfComponents(4);
		for (Integer cellId : cellIds)
		{
			double[] colorArray = lut.GetColor(cellIds.indexOf(cellId));
			cellData.InsertNextTuple4(colorArray[0]*255, colorArray[1]*255, colorArray[2]*255, 255);	//this needs to be the color for the cell
		}

		footprintPolyData.GetCellData().SetScalars(cellData);
		return lut;
	}

	/**
	 * For the given trajectory index (which in turn provides a time), returns the color of the trajectory at this time
	 * @param index	the index into the arraylist of times that make up this trajectory
	 * @return	the <pre>Color</pre> of the trajectory at this index
	 */
	private Color getColorForValue(double value)
	{
		if (gcp == null)
		{
			Color color = colormap.getColor(value);
			return color;
		}
		else
		{
			return gcp.getColor(minValue, maxValue, value);
		}
	}

	public void shiftFootprint()
	{
		smallBodyModel.shiftPolyLineInNormalDirection(footprintPolyData, 0.001);
	}

	public vtkProp getProps()
	{
		vtkPolyDataMapper footprintMapper = new vtkPolyDataMapper();
        footprintMapper.SetInputData(footprintPolyData);

        footprintMapper.Update();
        footprintActor.SetMapper(footprintMapper);
        if (footprint.isMapped()) footprintActor.SetVisibility(1);
        else footprintActor.SetVisibility(0);
        return footprintActor;
	}

	private FacetColoringData[] getColoringDataForFootprint()	//this uses the exact indicies of the footprint (see below for previous use)
    {
		Indexable<Integer> indexable = getFootprintIndexable();
        FacetColoringData[] data = new FacetColoringData[indexable.size()];
        for (int index = 0; index < indexable.size(); ++index)
        {
        	footprint.setStatus("Idx: " + (i+1) + "/" + indexable.size());
            int cellId = indexable.get(index);
            FacetColoringData facetData = new FacetColoringData(cellId, allColoringData);
            facetData.generateDataFromPolydata(smallBodyPolyData);
            data[index] = facetData;
        }
        return data;
    }

	private ImmutableList<ColoringData> getFootprintColoringData()	//needs to have coloring data for: cosE, projectedArea, range, ???
	{

		vtkFloatArray footprintValues = new vtkFloatArray();
		Vector<Integer> cellIds = footprint.getCellIDs();
		List<ColoringData> coloringData = Lists.newArrayList();

		footprintValues.SetNumberOfValues(smallBodyPolyData.GetNumberOfPolys());
		for (Integer cellId : cellIds)
		{
			List<MEGANEFootprintFacet> facets = footprint.getFacets().stream().filter(fp -> fp.getFacetID() == cellId).toList();
			if (facets.size() == 1)
			{
				MEGANEFootprintFacet facet = facets.get(0);
				double value = facet.getComputedValue();
				minValue = Math.min(minValue, value);
				maxValue = Math.max(maxValue, value);
				footprintValues.InsertValue((int)cellId, (float)value);
			}
		}

		IndexableTuple indexableTuple = ColoringDataUtils.createIndexableFromVtkArray(footprintValues);

		//this needs to have a name for each coloring set
		ColoringData data = ColoringDataFactory.of("Projected Area/Range^2", "units", (int)smallBodyPolyData.GetNumberOfCells(), List.of("Projected Area/Range^2"), false, indexableTuple);

		coloringData.add(data);

		return ImmutableList.copyOf(coloringData);
	}

	private Indexable<Integer> getFootprintIndexable()
	{
		return new Indexable<Integer>()
		{
			@Override
            public int size()
            {
                return footprint.getFacets().size();
            }

            @Override
            public Integer get(int index)
            {
                return footprint.getFacets().get(index).getFacetID();
            }
		};
	}

	public void setColoringProvider(ColorProvider gcp)
	{
		this.gcp = gcp;
		updateColorFromPlate();
	}
}