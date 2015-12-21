package org.bdigital.mob.c2020.geo;

import static org.junit.Assert.*;
import gov.nasa.worldwind.geom.Position;

import java.util.LinkedList;

import org.junit.Test;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class TestGeoPolygon {

	@Test
	public void testGetConvexBoundary()
	{	
	
		double[][] tweets=org.bdigital.mob.c2020.core.TestOpticsAlgorithm.createTweets();
		Coordinate[] ptList=new Coordinate[3];
		LinkedList<Position> list = new LinkedList<Position>();
				
		for (int j=0; j < 3; ++j){
			ptList[j]=new Coordinate(tweets[j][0],tweets[j][1]);
			System.out.println(ptList[j]);
			list.add(Position.fromDegrees(tweets[j][1],tweets[j][0],0.0));			
		}

		GeoPolygon poly=GeoPolygon.getConvexBoundary(ptList);
		
		GeometryFactory gf=new GeometryFactory();
		com.vividsolutions.jts.geom.Polygon aPoly=gf.createPolygon(poly.getCoordinates().getCoordinates());
		
		assertTrue(aPoly.getArea()>0);
		assertTrue(aPoly.intersects(gf.createPoint(ptList[0])));
		assertTrue(aPoly.intersects(gf.createPoint(ptList[1])));
		assertTrue(aPoly.intersects(gf.createPoint(ptList[2])));
		
	}	
	
	
}
