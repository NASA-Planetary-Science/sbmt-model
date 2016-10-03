package edu.jhuapl.sbmt.model.europa.util;

public class Pair<U,V> {
	public U first;
	public V second;
	public Pair()
	{
	}
	public Pair(U x1, V x2)
	{
		first = x1;
		second = x2;
	}
	public Pair(Pair<U,V> p)
	{
		first = p.first;
		second = p.second;
	}
}
