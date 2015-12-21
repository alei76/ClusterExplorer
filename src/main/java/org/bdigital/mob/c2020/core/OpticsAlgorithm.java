package org.bdigital.mob.c2020.core;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.Polygon;
import gov.nasa.worldwind.util.measure.AreaMeasurer;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwindx.examples.ApplicationTemplate;

import java.awt.Font;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;


import org.apache.commons.cli.*;

import org.bdigital.mob.c2020.data.MongoDBConnector;
import org.bdigital.mob.c2020.geo.GeoPolygon;

import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICSXi;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.OPTICSModel;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AbstractDatabase;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk.SortTileRecursiveBulkSplit;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy.Iter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

//import org.apache.commons.math3.*;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Optics Algorithm
 * 
 * @author Joana Simoes (jsimoes@bdigital.org)
 * @version 0.1 31/07/2014
 */
public class OpticsAlgorithm extends ApplicationTemplate{
	
	private static String dbUrl;
	private static String dbPort;
	private static String dbName;
	private static String tweetsCollectionName;

	private static String locationField;
	private static String latField;
	private static String lonField;		
	private static String dbusername;
	private static String dbpasswd;
	
	private static String env= "MONGODB_PATH"; 
	private static String mongodbProperties="mongodb.properties";
	
	private static double epsilon = 1000;//500.0;
	private static double xi = 0.035;//0.03;
	private static int minPoints = 10;//100;
	private static ALG alg=ALG.DBSCAN;	
	private static String start="01/06/2014";
	private static String end="05/06/2014";
	
	private static double minx=180.0;
	private static double maxx=-180.0;
	private static double miny=90.0;			
	private static double maxy=-90.0;	
	private static double tolerance=3.0;
	
	private static int nLevels=0;
	
	public enum ALG {
	    DBSCAN, OPTICS, ALL, 
	}
	
	    public static class AppFrame extends ApplicationTemplate.AppFrame
	    {
	    	
	        public AppFrame()
	        {
	            super(true, true, true);

	    		MongoDBConnector mongoConnector = null;
	    		try {
	    			
	    		    System.out.println("Retrieving tweets \n");
	    			
	    			mongoConnector = new MongoDBConnector(dbUrl, dbPort, dbName, tweetsCollectionName, 
	    					locationField, latField, lonField, dbusername, dbpasswd);
	    	
	    			SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/M/yyyy",  Locale.ENGLISH);
	    			
	    			Date timestamp1 = dateFormatter.parse(start);
	    			Date timestamp2 = dateFormatter.parse(end);
	    			
	    			double[][] tweets = mongoConnector.getArrayTweets(timestamp1, timestamp2);
	    				    			
	    			if (alg==ALG.OPTICS || alg==ALG.ALL)
	    				runAlgorithm(tweets,ALG.OPTICS);
	    			if (alg==ALG.DBSCAN || alg==ALG.ALL)
	    				runAlgorithm(tweets,ALG.DBSCAN);;
	    			
	 	            this.getLayerPanel().update(this.getWwd());
	    		    
	    			LatLon p1=new LatLon(Position.fromDegrees(miny,minx,2000.0));
	    			LatLon p2=new LatLon(Position.fromDegrees(maxy,maxx,2000.0));		
	    	
	    			Sector boundingSector = Sector.boundingSector(p1,p2);
	    					
	    			LatLon centroid=boundingSector.getCentroid();
	    			Position pos=new Position(centroid.getLatitude(),
	    					centroid.getLongitude(),2000.0);
	    			
	    			
                    View view = this.getWwd().getView();
                    //Globe globe = this.getWwd().getModel().getGlobe();
                    if(view instanceof BasicOrbitView) {
                            BasicOrbitView bov = (BasicOrbitView)view;
                                                    bov.stopAnimations();
                                                    bov.addPanToAnimator(pos, 
                                                    		view.getHeading(), Angle.fromDegrees(55.0)/*view.getPitch()*/, 200000);
                    }
	    			
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}finally{
	    			if(mongoConnector != null){
	    				mongoConnector.closeConnection();
	    			}
	    		}
	    			            
	        }
	        
	        public void runAlgorithm(double[][] tweets, ALG cAlg) throws Exception{
	        	
    			RenderableLayer layer=new RenderableLayer();
    			layer.setName( (cAlg==ALG.OPTICS?"OPTICSxi Clusters":"DBSCAN Clusters") );
    			RenderableLayer layer2=new RenderableLayer();		    
    			layer2.setName("Level Labels");
    				    			
    			getClusters(tweets,layer,layer2,cAlg);
    				    		    		    
    			insertBeforeLayerName(this.getWwd(), layer, "MS Virtual Earth Aerial");
	        	
	        }
	        	        	        	        
	    }

	    public static void main(String[] args)
	    {
			readProperties();
	    	
	    	CommandLineParser parser = new DefaultParser();
	    	
	    	HelpFormatter formatter = new HelpFormatter();	    	
	    	// create Options object
	    	Options options = new Options();
	    	
	    	//options.addOption( "e", "epsilon", true, "do not hide entries starting with." );
	    	options.addOption( "h", "help", false, "display this help message." );

	    	options.addOption(OptionBuilder
	    			.withLongOpt( "guess" )
                    .withDescription( "suggests parameters for your dataset, based on an informated guess;" )
//                    .isRequired()
                    .create("g") );
	    		    	
	    	options.addOption(OptionBuilder
	    			.withLongOpt( "epsilon" )
                    .withDescription( "neighbourhood radius (DBSCAN) and maximum neighbourhood radius (OPTICS); e.g.: 500 (default);" )
                    .hasArg()
//                    .isRequired()
                    .create("e") );

	    	options.addOption( OptionBuilder.withLongOpt( "xi" )
                    .withDescription( "OPTICSxi contrast parameter: establishes the relative decrease in density; e.g.: 0.03 (default);" )
                    .hasArg()
                    //.isRequired()
                    .create("x") );

	    	options.addOption( OptionBuilder.withLongOpt( "minpts" )
                    .withDescription( "number of points required to form a cluster; e.g.: 100 (default);" )
                    .hasArg()
                    //.isRequired()
                    .create("m") );

	    	options.addOption( OptionBuilder.withLongOpt( "alg" )
                    .withDescription( "cluster algorithm to be used; possible values are \"optics\", \"dbscan\" (default) and \"all\"(both)" )
                    .hasArg()
                    .create("a") );	    	
	    	
	    	options.addOption( OptionBuilder.withLongOpt( "start" )
                    .withDescription( "start date; e.g.: \"01/06/2014\"" )
                    .hasArg()
                    .isRequired()
                    .create("s") );	    	

	    	options.addOption( OptionBuilder.withLongOpt( "end" )
                    .withDescription( "end date; e.g.: \"05/06/2014\"" )
                    .hasArg()
                    .isRequired()
                    .create("n") );	    	
	    	
	    	try {
	    		
	    	    // parse the command line arguments
	    	    CommandLine line = parser.parse( options, args );

	    	    if( line.hasOption( "h" ) ) {
	    	    	formatter.printHelp( "Clusterizer", options );
	    	    	System.exit(0);
	    	    }if( line.hasOption( "e" )) {    	    	
	    	    	epsilon=Double.parseDouble(line.getOptionValue( "e" ));
	    	        //System.out.println(epsilon);
	    	    }
	    	    if( line.hasOption( "m" ) ) {
	    	    	minPoints=Integer.parseInt(line.getOptionValue( "m" ));	    	    	
	    	    }
	    	    if( line.hasOption( "a" ) ) {
	    	    	String alg_=line.getOptionValue( "a" ).toString().toLowerCase();
	    	    	if (alg_.equals("dbscan")) alg=ALG.DBSCAN;
	    	    	else if (alg_.equals("optics")) alg=ALG.OPTICS;
	    	    	else if (alg_.equals("all")) alg=ALG.ALL;
	    	    	else throw new Exception("No such algorithm: \"" + alg_ + "\"");	    	    		    	    		    	    			
	    	    }	    	    
	    	    if( line.hasOption( "xi" ) ) {
	    	    	xi=Double.parseDouble(line.getOptionValue( "xi" ));
	    	    }
	    	    if( line.hasOption( "s" ) ) {
	    	    	start=line.getOptionValue( "s" ).toString();
	    	    }
	    	    if( line.hasOption( "n" ) ) {
	    	    	end=line.getOptionValue( "n" ).toString();
	    	    }
	    	    if( line.hasOption( "g" ) ) {
	    	    	suggestParameters(start,end);
	    	    	System.exit(0);
	    	    }
	    	    	    	    
		        ApplicationTemplate.start("clusters on NASA World Wind", AppFrame.class);
	    	    
	    	}
	    	catch( Exception /*org.apache.commons.cli.ParseException | NumberFormatException*/ exp) {
	    	    System.out.println( "Unexpected exception:" + exp.getMessage() );
    	    	formatter.printHelp( "Clusterizer", options );	    	    
	    	}  	
	        	        
	    }

	public static void suggestParameters(String start, String end){
		
		MongoDBConnector mongoConnector = null;
		try {
			
			System.out.print("Analysing dataset... wait"+"\n");
			
			mongoConnector = new MongoDBConnector(dbUrl, dbPort, dbName, tweetsCollectionName,
					locationField, latField, lonField, dbusername, dbpasswd);	
			SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/M/yyyy",  Locale.ENGLISH);
			double[][] tweets = mongoConnector.getArrayTweets(dateFormatter.parse(start), dateFormatter.parse(end));
			
			//double maxx=-90.0, maxy=-180.0, minx=90.0, miny=180.0;
			ArrayList<Position> list = new ArrayList<Position>();
			
			
			for (int i=0; i < tweets.length; ++i){
				if (tweets[i][0]<minx) minx=tweets[i][0];
				else if (tweets[i][0]>maxx) maxx=tweets[i][0];
				
				if (tweets[i][1]<miny) miny=tweets[i][1];
				else if (tweets[i][1]>maxy) maxy=tweets[i][1];								
			}

			list.add(Position.fromDegrees(maxy,minx,500.0));
			list.add(Position.fromDegrees(maxy,maxx,500.0));
			list.add(Position.fromDegrees(miny,maxx,500.0));
			list.add(Position.fromDegrees(miny,minx,500.0));

			list.add(Position.fromDegrees(maxy,minx,500.0));
			
			/*
			System.out.print("maxx: " + maxx + "\n");
			System.out.print("minx: " + minx + "\n");
			System.out.print("maxy: " + maxy + "\n");
			System.out.print("miny: " + miny + "\n");
			*/
			
			AreaMeasurer am=new AreaMeasurer(list);
			
			Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);			
			Globe globe=m.getGlobe();
			
			System.out.print( tweets.length<1000?("Your dataset is too small: no suggestions;")
					:("suggested minpts: " + (int)(tweets.length/1000) + "\n"));
			
			System.out.print( am.getPerimeter(globe)<1000?("Your dataset is too small: no suggestions;")
					:("suggested epsilon: " + (int)(am.getPerimeter(globe)/100) + "\n"));
			
			
		} catch (Exception e) {
			e.printStackTrace();
			}finally{
				if(mongoConnector != null){
					mongoConnector.closeConnection();
				}
			}
		            		
	}
	    
	public static double computeZoomForExtent(Sector sector)
	{
	    Angle delta = sector.getDeltaLat();
	    if (sector.getDeltaLon().compareTo(delta) > 0)
	        delta = sector.getDeltaLon();
	    double arcLength = delta.radians * Earth.WGS84_EQUATORIAL_RADIUS;
	    double fieldOfView = Configuration.getDoubleValue(AVKey.FOV, 45.0);
	    return arcLength / (2 * Math.tan(fieldOfView / 2.0));
	}	
		
	//initializes Elki database
	public static Database createDatabase(double[][] tweets) throws IOException{
		
		// Setup parameters:
		ListParameterization params = new ListParameterization();
		DatabaseConnection dbc = new ArrayAdapterDatabaseConnection(tweets);

		// Pass an instance, not a class, as parameter.
		params.addParameter(
		    AbstractDatabase.Parameterizer.DATABASE_CONNECTION_ID, dbc);		
				
		params.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, 
				RStarTreeFactory.class);
		params.addParameter(
				RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SortTileRecursiveBulkSplit.class);
		// Add other parameters for the database here!	
		Database db = ClassGenericsUtil.parameterizeOrAbort(StaticArrayDatabase.class, params);
		db.initialize();	
		return db;
	}

	//initializes map from Elki database. The key is the DatabaseID the value is a point (as coordinates)
	public static HashMap<String, Coordinate> createMap(Database db) throws IOException{
	
		Relation<NumberVector<?>> vectors = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
		
		HashMap<String, Coordinate> dataMap = new HashMap<String, Coordinate>();						
	    for (DBIDIter iter = vectors.getDBIDs().iter(); iter.valid(); iter.advance()) {
	    	Coordinate pt=new Coordinate();
	    	pt.x=vectors.get(iter).doubleValue(0);
	    	pt.y=vectors.get(iter).doubleValue(1);	    	
	    	dataMap.put(DBIDUtil.toString(iter),pt);	    	
	    }			
	    	    
	    return dataMap;
	}

	public static ListParameterization parametrizeDBSCAN(){
		ListParameterization params2 = new ListParameterization();
		params2.addParameter(de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN.Parameterizer.MINPTS_ID, minPoints);
		params2.addParameter(de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN.Parameterizer.EPSILON_ID, epsilon);
		params2.addParameter(de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, 
				de.lmu.ifi.dbs.elki.distance.distancefunction.geo.LngLatDistanceFunction.class);
		
		return params2;
	}	
	
	public static ListParameterization parametrizeOPTICS(){
		ListParameterization params2 = new ListParameterization();
		params2.addParameter(OPTICSXi.XI_ID, xi);	
		params2.addParameter(de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS.MINPTS_ID, minPoints);//150
		params2.addParameter(de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS.EPSILON_ID, epsilon);//0.0125						
		params2.addParameter(de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICSXi.XIALG_ID, 
				de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS.class);	
		params2.addParameter(de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, 
				de.lmu.ifi.dbs.elki.distance.distancefunction.geo.LngLatDistanceFunction.class);			
		
		return params2;
	}
			
	private static ModifiableDBIDs recursiveBuild(de.lmu.ifi.dbs.elki.data.Cluster<OPTICSModel> clu,
			Hierarchy<de.lmu.ifi.dbs.elki.data.Cluster<OPTICSModel>> hier,
			HashMap<String, Coordinate> dataMap,ArrayList<Polygon> polys, int nhier
			) throws NoSuchAlgorithmException{	
	
	
	  ModifiableDBIDs ids = DBIDUtil.newHashSet(clu.getIDs());
	  
	  // Aggregate child ids recursively:
	  	  
	  if(hier != null) {
		nhier++;		  
		//System.out.println(nhier);
		  
	  final int numc = hier.numChildren(clu);
		if(numc > 0) {
	          for(Iter<de.lmu.ifi.dbs.elki.data.Cluster<OPTICSModel>> iter =
	        		  hier.iterChildren(clu); iter.valid(); iter.advance()) {
	        	  
	  	               final de.lmu.ifi.dbs.elki.data.Cluster<OPTICSModel> iclu 
	  	               = iter.get();
	  	               ids.addDBIDs(recursiveBuild(iclu,hier,dataMap,polys,nhier));
	          }
		}
	  }	  
        	  
		Coordinate[] coords=new Coordinate[ids.size()];
		
		int i=0;		
		
		double sumClusterAvgDist=0.0;
		
		double[] avgDist=new double[ids.size()];				
		
		for (DBIDIter iter = ids.iter(); 
					iter.valid(); iter.advance()) {
			
				coords[i]=dataMap.get(DBIDUtil.toString(iter));
				
				if (coords[i].x < minx) minx=coords[i].x;
				else if (coords[i].x > maxx) maxx=coords[i].x;
				if (coords[i].y < miny) miny=coords[i].y;
				else if (coords[i].y > maxy) maxy=coords[i].y;
				++i;
				
		}		

		for (i=0; i < coords.length; ++i){
			avgDist[i]=getAverageNgDistance(i, coords);
			sumClusterAvgDist+=avgDist[i];
		}
		
		
		double avgClusterAvgDist=sumClusterAvgDist/coords.length;
		ArrayList<Coordinate> coords2=new ArrayList<Coordinate>();
		
		for (i=0; i < avgDist.length; ++i){
			if ( avgDist[i]<avgClusterAvgDist*tolerance){
				coords2.add(coords[i]);
			}
		}
				
		Coordinate[] coords3 = coords2.toArray(new Coordinate[coords2.size()]);
		Polygon poly=GeoPolygon.getConvexBoundaryWW(coords3);
						
		if (poly!=null){
			poly.setValue("class", nhier);
								
			if (nhier> nLevels) nLevels=nhier;
			
			poly.setValue(AVKey.DISPLAY_NAME, "Level in the cluster hierarchy: " + nhier);
			
			polys.add(poly);
		}
					  
	  return ids;	
	}

	public static double getAverageNgDistance(int i, 
			Coordinate[] coords){
			
		double sum=0.0;
		int count=0;
		try{		

			if (i>0){
				sum+=coords[i].distance(coords[i-1]);
				count++;
			}
			if (i< coords.length-1){
				sum+=coords[i].distance(coords[i+1]);
				count++;
			}
				
		}
    	catch( Exception  exp) {
    	    System.out.println( "Unexpected exception:" + exp.getMessage() );
    	}  	
		return sum/count;
	}		
	
	public static double getAverageDistance(Coordinate coord, Coordinate[] coords){
		
		double sum=0.0;
		
		try{		
									
			for (int i=0; i < coords.length; ++i){
				sum+=coord.distance(coords[i]);			
			}
			
		}
    	catch( Exception  exp) {
    	    System.out.println( "Unexpected exception:" + exp.getMessage() );
    	}  	
		return sum/coords.length;
	}	
	
	public static double getAverageDistance(Coordinate coord, ModifiableDBIDs ids, HashMap<String, Coordinate> dataMap){
		
		double sum=0.0;
		
		try{		
			for (DBIDIter iter = ids.iter(); 
						iter.valid(); iter.advance()) 								
						sum+=coord.distance(dataMap.get(DBIDUtil.toString(iter)));
		}
    	catch( Exception  exp) {
    	    System.out.println( "Unexpected exception:" + exp.getMessage() );
    	}  	
		return sum/ids.size();
	}
	
	public static ArrayList<Polygon> runDBSCAN(Database db, HashMap<String,
			Coordinate> dataMap, RenderableLayer layer, RenderableLayer layer2) throws NoSuchAlgorithmException{
					
		ArrayList<Polygon> clusters=new ArrayList<Polygon>();
				
		ListParameterization params=parametrizeDBSCAN();
		de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN<DoubleVector,DoubleDistance> dbscan = 
		ClassGenericsUtil.parameterizeOrAbort(de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN.class,
				params);
							
		 Clustering<de.lmu.ifi.dbs.elki.data.model.Model> result = dbscan.run(db);	
			 		
		 ArrayList<Polygon> polys=new ArrayList<Polygon>();		
		   
			   for (de.lmu.ifi.dbs.elki.data.Cluster<?> cl : result.getAllClusters()) {
				   if (!cl.isNoise()){
					   
						  ModifiableDBIDs ids = DBIDUtil.newHashSet(cl.getIDs());
						    					        	  
						  Coordinate[] coords=new Coordinate[ids.size()];
						  int i=0;
														
							for (DBIDIter iter = ids.iter(); 
										iter.valid(); iter.advance()) {
								
									coords[i]=dataMap.get(DBIDUtil.toString(iter));
									
									if (coords[i].x < minx) minx=coords[i].x;
									else if (coords[i].x > maxx) maxx=coords[i].x;
									if (coords[i].y < miny) miny=coords[i].y;
									else if (coords[i].y > maxy) maxy=coords[i].y;
									++i;
									
							}		

							Polygon poly=GeoPolygon.getConcaveBoundaryWW(coords);
											
							if (poly!=null){								
								polys.add(poly);
							    layer.addRenderable(poly);	
							    
								Polygon pl=poly;
								Position p=Position.fromDegrees(
										pl.getSector().getCentroid().getLatitude().getDegrees(),
										pl.getSector().getCentroid().getLongitude().getDegrees(),500.0);							    
/*
								   Double maxLat= pl.getSector().getMaxLatitude().getDegrees();
								   Double maxLon= pl.getSector().getMaxLongitude().getDegrees();
								   Double minLat = pl.getSector().getMinLatitude().getDegrees();
								   Double minLon = pl.getSector().getMinLongitude().getDegrees();
								   			   
								if (minLon<minx) minx=minLon;				
								if (maxLon>maxx) maxx=maxLon;
								
								if (minLat<miny) miny=minLat;
								if (maxLat>maxy) maxy=maxLat;								
	*/							
								gov.nasa.worldwind.render.Material m=new 
										gov.nasa.worldwind.render.Material(Color.blue);
								poly.getAttributes().setInteriorMaterial(m);									
								
							    PointPlacemark pmStandard = new PointPlacemark(p);
							    PointPlacemarkAttributes pointAttribute = new PointPlacemarkAttributes();
							    pointAttribute.setImageColor(Color.red);
							    pointAttribute.setLabelFont(Font.decode("Verdana-Bold-22"));
							    pointAttribute.setLabelMaterial(Material.CYAN);
							    pmStandard.setLabelText("num points: " + ids.size()); 

								poly.setValue(AVKey.DISPLAY_NAME, "num points: " + ids.size());
							    
							    layer.addRenderable(poly);			   
							    layer2.addRenderable(pmStandard);							    
							    
							    clusters.add(poly);								
							}				   
							
				   }
			   }

		return clusters;			   
	}	
	
	public static ArrayList<Polygon> runOPTICS(Database db, HashMap<String,
			Coordinate> dataMap, RenderableLayer layer, RenderableLayer layer2) throws NoSuchAlgorithmException{
					
		ArrayList<Polygon> clusters=new ArrayList<Polygon>();
						
		ListParameterization params=parametrizeOPTICS();
		de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICSXi<DoubleDistance> 		
		 opticsxi = ClassGenericsUtil.parameterizeOrAbort(OPTICSXi.class, params);
		
		Clustering<OPTICSModel> result = opticsxi.run(db);			

		
		List<de.lmu.ifi.dbs.elki.data.Cluster<OPTICSModel>> topc = 
				result.getToplevelClusters();
		 Hierarchy<de.lmu.ifi.dbs.elki.data.Cluster<OPTICSModel>> hier
		 = result.getClusterHierarchy();
		 		
		   int nhier=-2;
		   ArrayList<Polygon> polys=new ArrayList<Polygon>();				
		   for(de.lmu.ifi.dbs.elki.data.Cluster<OPTICSModel> clu : topc)
				   recursiveBuild(clu,hier,dataMap,polys,nhier);
		   		   
		   //System.out.println("N levels: " + nLevels);
		   
		   // writing backwards, to have the outer clusters on the bottom
		   for (int i = polys.size()-2; i>=0; --i ){
			   
			    Integer tmp=(Integer)polys.get(i).getValue("class");			    
				double norm=(double)tmp*100.0/(double)nLevels/100.0;
									    
				Color c = getColorFromRamp(Color.black,Color.red,norm); 
								
				gov.nasa.worldwind.render.Material m=new 
						gov.nasa.worldwind.render.Material(c);
				polys.get(i).getAttributes().setInteriorMaterial(m);				
												
				Polygon pl=polys.get(i);
				Position p=Position.fromDegrees(
						pl.getSector().getCentroid().getLatitude().getDegrees(),
						pl.getSector().getCentroid().getLongitude().getDegrees(),500.0);
				/*
			   Double maxLat= pl.getSector().getMaxLatitude().getDegrees();
			   Double maxLon= pl.getSector().getMaxLongitude().getDegrees();
			   Double minLat = pl.getSector().getMinLatitude().getDegrees();
			   Double minLon = pl.getSector().getMinLongitude().getDegrees();
			   			   
				if (minLon<minx) minx=minLon;				
				if (maxLon>maxx) maxx=maxLon;
				
				if (minLat<miny) miny=minLat;
				if (maxLat>maxy) maxy=maxLat;								
				*/
			   PointPlacemark pmStandard = new PointPlacemark(p);
			   PointPlacemarkAttributes pointAttribute = new PointPlacemarkAttributes();
			   pointAttribute.setImageColor(Color.red);
			   pointAttribute.setLabelFont(Font.decode("Verdana-Bold-22"));
			   pointAttribute.setLabelMaterial(Material.CYAN);
			   pmStandard.setLabelText(polys.get(i).getValue("class").toString()); 

			   layer.addRenderable(polys.get(i));			   
			   layer2.addRenderable(pmStandard);
			   //TODO: remove this later
			   clusters.add(polys.get(i));
			   
		   }
		   
		   return clusters;
	}
	
	public static Color getColorFromRamp(Color color1, Color color2, double percent){
		  double inverse_percent = 1.0 - percent;
		  int redPart = (int)(color1.getRed()*percent + color2.getRed()*inverse_percent);
		  int greenPart = (int)(color1.getGreen()*percent + color2.getGreen()*inverse_percent);
		  int bluePart = (int)(color1.getBlue()*percent + color2.getBlue()*inverse_percent);
		  return new Color(redPart, greenPart, bluePart);
		}	
		
	
	public static ArrayList<Polygon> getClusters(double[][] tweets,
			RenderableLayer layer,RenderableLayer layer2, ALG cAlg) throws Exception{

		//initializes input structures
		Database db=createDatabase(tweets);
		HashMap<String, Coordinate> dataMap=createMap(db);				
		
		return (cAlg==ALG.OPTICS?runOPTICS(db,dataMap,layer,layer2):
			runDBSCAN(db,dataMap,layer,layer2));
	}	
	
	public void setXi(double inXi){
		this.xi=inXi;
	}
	
	public void setMinPoints(int inMinPts){
		this.minPoints=inMinPts;
	}
	
	private static void readProperties(){

        Properties props = new Properties();
        FileInputStream fis=null;
                
		try {
			
            String value = System.getenv(env);
            if (value != null) {
    			fis = new FileInputStream(value + "//"  + mongodbProperties);    			
            	
            } else {
    			fis = new FileInputStream(mongodbProperties);    			
            }
        						     
        //loading properties from properties file
			props.load(fis);
			
	        dbUrl = props.getProperty("dbUrl");
	        dbPort = props.getProperty("dbPort");
	        dbName = props.getProperty("dbName");	        
	        tweetsCollectionName=props.getProperty("tweetsCollectionName");
	        locationField=props.getProperty("locationField");
	        latField=props.getProperty("latField");
	        lonField=props.getProperty("lonField");
	        dbusername=props.getProperty("dbusername");
	        dbpasswd=props.getProperty("dbpasswd");	        
		} catch (Exception e) {
    	    System.out.println( "Could not read properties file: " + e.getMessage() );			
	    	System.exit(0);
		}


	}
	
	
}
