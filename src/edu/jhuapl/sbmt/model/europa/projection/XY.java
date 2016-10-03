package edu.jhuapl.sbmt.model.europa.projection;

public class XY {
	public double x = 0.0;
	public double y = 0.0;
	public XY(){}
	public XY(double x1, double x2){ x = x1; y = x2;}
	public XY(XY v){ x = v.x; y = v.y;}
	public void create(double x1, double x2)
	{
		x = x1;
		y = x2;
	}
	public double dist(XY p)
	{
		double result = 0.0;
		
		double dx = p.x - x;
		double dy = p.y - y;
		result = Math.sqrt(dx*dx + dy*dy);
		
		return result;
	}
	public void copy(XY xy)
	{
		x = xy.x;
		y = xy.y;
	}
	public static double delta(XY xy1, XY xy2)
	{
		return Math.sqrt((xy1.x - xy2.x)*(xy1.x - xy2.x) + (xy1.y - xy2.y)*(xy1.y - xy2.y));
	}
	public static XY midPoint(XY xy1, XY xy2)
	{
		XY result = new XY();
		
		result.x = xy1.x + 0.50 * (xy2.x - xy1.x);
		result.y = xy1.y + 0.50 * (xy2.y - xy1.y);
		
		return result;
	}
}