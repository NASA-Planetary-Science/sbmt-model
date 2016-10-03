package edu.jhuapl.sbmt.model.europa.projection;

//
//	The IJ class stores 2 integers. These could be used for anything
//	but are intended to represent (column, row) pairs or (x, y) pairs
//	in image space.
//

public class IJ {
	public int i = 0;
	public int j = 0;
	public IJ(){}
	public IJ(int i1, int j1){ i = i1; j = j1;}
	public IJ(IJ ij){ i = ij.i; j = ij.j;}
}
