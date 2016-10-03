package edu.jhuapl.sbmt.model.europa.util;

public class Triplet<U,V,W> {
	public U first;
	public V second;
	public W third;
	public Triplet(){}
	public Triplet(U x1, V x2, W x3)
	{
		first = x1;
		second = x2;
		third = x3;
	}
	public Triplet(Triplet<U,V,W> t)
	{
		first = t.first;
		second = t.second;
		third = t.third;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
