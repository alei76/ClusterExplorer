package org.bdigital.mob.c2020.geo;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Polygon;
import gov.nasa.worldwind.render.ShapeAttributes;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * GeoPolygon. Geo primitives and conversions between World Wind and JTS objects
 * 
 * @author Joana Simoes (jsimoes@bdigital.org)
 * @version 0.1 31/07/2014
 */
public class GeoPolygon {
	public  Geometry coordinates;
	public  String polygonID;
	
	public GeoPolygon(Geometry m, String ID){
//		coordinates = new Geometry();
		coordinates = m;
		polygonID = new String(ID);
	}

	public  String getPolygonID() {
		return polygonID;
	}

	public  void setPolygonID(String polygonID) {
		this.polygonID = polygonID;
	}

	public  Geometry getCoordinates() {
		return coordinates;
	}

	public  void setCoordinates(Geometry coordinates) {
		this.coordinates = coordinates;
	}

	public int getNumPoints() {
		return this.coordinates.getNumPoints();
	}
	
	public String getLatLongs(){
		String cleanedGeometry;
		String tmp = "";
		String[] coords;
		String[] instances;

		cleanedGeometry = (coordinates.toString().replace("LINESTRING (", ""));
		cleanedGeometry = (coordinates.toString().replace("POLYGON ((", ""));
		cleanedGeometry.replace("(", "");
		cleanedGeometry = cleanedGeometry.substring(0, cleanedGeometry.length()-1);
		
		instances = cleanedGeometry.split(", ");
		
		for(int i = 0; i<instances.length;i++){
			coords = instances[i].split(" ");
			for(int j = 0; j<coords.length-1;j+=2){
				tmp +=coords[j]+","+coords[j+1]+"\n";
				
			}
		}
		return tmp;
	}

	public String getStringifiedCoords() {
		String cleanedGeometry;
		String tmp = "";
		String[] coords;
		String[] instances;
		String strCoords = coordinates.toText();


		
		cleanedGeometry = strCoords;
		cleanedGeometry = cleanedGeometry.replace("LINESTRING (", "");
		cleanedGeometry = cleanedGeometry.replace("POLYGON ((", "");
		cleanedGeometry = cleanedGeometry.replace("(", "");
		cleanedGeometry = cleanedGeometry.replace(")", "");
		
		instances = cleanedGeometry.split(", ");
		
		for(int i = 0; i<instances.length;i++){
			coords = instances[i].split(" ");
			for(int j = 0; j<coords.length-1;j+=2){
				tmp +=coords[j+1].replace(")", "")+";"+coords[j]+",";
				
			}
		}

		
		return tmp;
	}
	
	public static GeoPolygon getConvexBoundary(Coordinate[] ptList) {
		
		GeometryFactory gf = new GeometryFactory();
				
		Geometry geometry = gf.createMultiPoint(ptList);
				
		final ConvexHull convexHull = new ConvexHull(geometry);
		Geometry mp = convexHull.getConvexHull();
		
		return (mp.getGeometryType()==
				"Polygon"? new GeoPolygon(mp, "NONE"): null);
	}	

	public static Material getRndColor() throws NoSuchAlgorithmException{
		
		Material m;
		int max=13;		

		Random rnd = new Random();

		int res = rnd.nextInt(max);		
		
		    switch (res) {
	            case 0:  m = gov.nasa.worldwind.render.Material.BLACK;
	                     break;
	            case 1:  m = gov.nasa.worldwind.render.Material.BLUE;
	            		 break;
	            case 2:  m = gov.nasa.worldwind.render.Material.CYAN;
       		 			break;
	            case 3:  m = gov.nasa.worldwind.render.Material.DARK_GRAY;
       		 			break;
	            case 4:  m = gov.nasa.worldwind.render.Material.GRAY;
       		 			break;
	            case 5:  m = gov.nasa.worldwind.render.Material.GREEN;
       		 			break;
	            case 6:  m = gov.nasa.worldwind.render.Material.LIGHT_GRAY;
       		 			break;
	            case 7:  m = gov.nasa.worldwind.render.Material.MAGENTA;
       		 			break;
	            case 8:  m = gov.nasa.worldwind.render.Material.ORANGE;
       		 			break;
	            case 9:  m = gov.nasa.worldwind.render.Material.PINK;
       		 			break;
	            case 10:  m = gov.nasa.worldwind.render.Material.RED;
       		 			break;
	            case 11:  m = gov.nasa.worldwind.render.Material.WHITE;
       		 			break;
	            case 12:  m = gov.nasa.worldwind.render.Material.YELLOW;
       		 			break;
       		 			
	            default: m = gov.nasa.worldwind.render.Material.YELLOW;
                break;               
		    }
            return m;
	}

	public static GeoPolygon WW2geoPolygon(Polygon p) throws NoSuchAlgorithmException{
		
		//Iterable<? extends LatLon> borderPositions = p.getOuterBoundary();
		
		ArrayList<Coordinate> coords=new ArrayList<Coordinate>();
		
		Iterator<? extends LatLon> itr = p.getOuterBoundary().iterator(); 
		while(itr.hasNext()) {
			LatLon ll=new LatLon(itr.next());			
			coords.add(new Coordinate(ll.asDegreesArray()[1],ll.asDegreesArray()[0]));
		}

		Coordinate[] coords2=new Coordinate[coords.size()];
		
		int i=0;
	    Iterator<Coordinate> itr2 = coords.iterator();
	    while(itr2.hasNext()){
	    	coords2[i]=itr2.next();
	    	++i;
	    }
	   		
		GeometryFactory gf = new GeometryFactory();		
		Geometry geometry = gf.createPolygon(coords2);		
		
		return new GeoPolygon(geometry,"NONE");		
	
	}
	
	public static Polygon geoPolygon2WW(GeoPolygon p) throws NoSuchAlgorithmException{
			 				 
		Coordinate[] coords=p.getCoordinates().getCoordinates();
		LinkedList<Position> list = new LinkedList<Position>();
			 			 
		for (int i=0; i < coords.length; ++i)			
			list.add(Position.fromDegrees(coords[i].y,coords[i].x,500.0));
    
		Polygon poly=new Polygon(list);
		
		//poly.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
	 
		ShapeAttributes normalAttributes = new BasicShapeAttributes();

		normalAttributes.setOutlineWidth(2);           
		normalAttributes.setOutlineOpacity(0.6);
		normalAttributes.setDrawInterior(true);
		//normalAttributes.setOutlineMaterial(Material.RED);		
		normalAttributes.setDrawOutline(true);
		normalAttributes.setInteriorOpacity(0.6);
	 				 
		normalAttributes.setInteriorMaterial(getRndColor());
		 
		poly.setAttributes(normalAttributes);
										
		return poly;
	}
	
	public static Polygon getConvexBoundaryWW(Coordinate[] ptList) throws NoSuchAlgorithmException {
		
		GeometryFactory gf = new GeometryFactory();
				
		Geometry geometry = gf.createMultiPoint(ptList);
				
		final ConvexHull convexHull = new ConvexHull(geometry);
		Geometry mp = convexHull.getConvexHull();
		
		return (mp.getGeometryType()==
				"Polygon"? geoPolygon2WW(new GeoPolygon(mp, "NONE")): null);		
	}		

	public static Polygon getConcaveBoundaryWW(Coordinate[] ptList) throws NoSuchAlgorithmException {
		
		GeometryFactory gf = new GeometryFactory();
				
		Geometry geometry = gf.createMultiPoint(ptList);
				
		final ConcaveHull concaveHull = new ConcaveHull(geometry,/*0.002*/0.005);
		Geometry mp = concaveHull.getConcaveHull();
		
		return (mp.getGeometryType()==
				"Polygon"? geoPolygon2WW(new GeoPolygon(mp, "NONE")): null);		
	}		
	
}
