package edu.jhuapl.sbmt.model.europa.pds;

import java.util.Vector;

public class CompressedImage {
	Vector<ImageRowData> rowData = null;
	int width=0;
	int height=0;
	public CompressedImage(int w, int h)
	{
		if ((w > 0) && (h > 0))
		{
			width = w;
			height = h;
		}
	}
	public void addRow(ImageRowData row)
	{
		if (rowData == null)
		{
			rowData = new Vector<ImageRowData>();
		}
		
		rowData.add(row);
	}
	public long getSample(int row, int col)
	{
		long result = ImageRowData.noDataValue;
		
		if (rowData != null)
		{
			if ((row >= 0) && (row < height) && (row < rowData.size()))
			{
				ImageRowData r = rowData.get(row);
				if ((col >= 0) && (col < width) && (col < r.rowLength))
					result = r.getRowValue(col);
			}
		}
		
		return result;
	}
}
