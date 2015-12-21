package org.bdigital.mob.c2020.data;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.bdigital.mob.c2020.model.Tweet;
import org.bdigital.mob.c2020.utils.Utils;
import org.json.simple.JSONObject;


import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;


/**
 * MongoDBConnector. Access to MongoDB instance with stored data
 * 
 * @author dsolans (dsolans@bdigital.org), doublebyte
 * @version 0.1 12/06/2014
 */
public class MongoDBConnector {
	private String dbUrl;
	private int dbPort;
	private String dbName;
	private String tweetsCollectionName;

	private Mongo mongo;
	private DB db;
	private DBCollection mongoDBcollectionTweets;
		
	//geo field stuff
	private String locationField;
	private String latField;
	private String lonField;	
	
	//authentication (optional)
	private String dbusername;
	private String dbpasswd;
	
	/**
	 * Constructor 
	 * @param dbUrl ip of the db
	 * @param dbPort port of the mongoDB ("27017" as default")
	 * @param dbName mongo DB name
	 * @param tweetsCollectionName name of the collection that contains the tweets
	 * @throws UnknownHostException
	 */
	@SuppressWarnings("deprecation")
	public MongoDBConnector(String dbUrl, String dbPort, String dbName,
			String tweetsCollectionName,
			String locationField, String latField, String lonField,
			String dbusername, String dbpasswd) throws UnknownHostException {
		super();
		this.dbUrl = dbUrl;
		this.dbPort = Integer.parseInt(dbPort);
		this.dbName = dbName;
		this.tweetsCollectionName = tweetsCollectionName;
		this.locationField = locationField;
		this.latField = latField;
		this.lonField = lonField;
		this.dbusername = dbusername;
		this.dbpasswd = dbpasswd;
		
		mongo = new Mongo(this.dbUrl, this.dbPort);
		db = mongo.getDB(this.dbName);
		
		boolean auth = db.authenticate(dbusername, dbpasswd.toCharArray());
		if (auth) {
		
			mongoDBcollectionTweets = db.getCollection(tweetsCollectionName);
			
		}	
	}

	/**
	 * Removes all the stored tweets
	 * 
	 * CAUTION!!!!
	 * Used just on the tests to remove previous results. 
	 * 
	 * DO NOT USE IT ON THE PRODUCTION DATABASE
	 * 
	 * Uses a password to ensure that the user understands that
	 *
	 * @param password
	 */
	public void removeAllStoredTweets(String password){
		if(password.equals("I understand what Im doing")){
			
			DBCursor cursor = mongoDBcollectionTweets.find();
			while (cursor.hasNext()) {
				mongoDBcollectionTweets.remove(cursor.next());
			}
		}
	}

	/**
	 * Extracts the stored tweets from a specific time-range and stores them in an array
	 * , which is the input of the ELKI algorithm
	 * @param startDate
	 * @param endDate
	 * @return array of doubles
	 */	
	public double[][] getArrayTweets(Date startDate, Date endDate){
		
		BasicDBObject andQuery = new BasicDBObject();
		List<BasicDBObject> obj = new ArrayList<BasicDBObject>();
		
		obj.add(new BasicDBObject("createdAt",new BasicDBObject("$gte", startDate.getTime()).append("$lt", endDate.getTime())));
		obj.add(new BasicDBObject(locationField,new BasicDBObject("$ne",null)));
		
		
		//optional geoquery to restrict to the bbox of Barcelona
	  final LinkedList<double[]> polygon = new LinkedList<double[]>();
	
		    // Create the shape.
		polygon.addLast(new double[] { 1.36068, 41.19564 });
		polygon.addLast(new double[] { 2.77827, 42.323299 });
		polygon.addLast(new double[] { 2.77827, 41.19564  });
		polygon.addLast(new double[] { 1.36068, 41.19564  });
		
		obj.add(new BasicDBObject(locationField, 
				new BasicDBObject("$geoWithin", new BasicDBObject("$polygon", polygon))));		
		
		
		andQuery.put("$and", obj);		
		
		DBObject nextTweet;
		DBCursor cursor = (DBCursor) mongoDBcollectionTweets.find(andQuery);//.limit(1000);		
		
		double[][] arTweets=new double[cursor.size()][2];
		
		int i=0;
		while (cursor.hasNext()) {
			try{	
				nextTweet = cursor.next();
						

				double lat = (double) ((DBObject) (nextTweet.get(locationField)))
						.get(latField);
				double lon = (double) ((DBObject) (nextTweet.get(locationField)))
						.get(lonField);
																
				
	  	      	arTweets[i][0]=lon;	      
	  	      	arTweets[i][1]=lat;
	  	      	++i;			
	  	      					
			}catch(Exception ex){
				//Error getting a tweet. Flow should continue for the rest of stored data
				System.out.println(ex);
				
			}
			
		}

		return arTweets;

	}	
	
	
	
	/**
	 * Extracts the stored tweets from a specific time-range
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public ArrayList<Tweet> getStoredTweets(Date startDate, Date endDate){

		BasicDBObject query = new BasicDBObject("createdAt", //
				new BasicDBObject("$gte", startDate).append("$lt", endDate));

		System.out.println(query);
		DBCursor cursor = (DBCursor) mongoDBcollectionTweets.find(query);
		
		
		
		//DBCursor cursor = (DBCursor) mongoDBcollectionTweets.find();
		ArrayList<Tweet> tweets = new ArrayList<Tweet>();

		DBObject nextTweet;
		Long baseID;
		Double lat;
		Double lon;
		String messageContent;
		String neighborhoodId;
		Date timestamp;
		String userId;

		Tweet tweet;

		while (cursor.hasNext()) {
			try{

				nextTweet = cursor.next();
	
				baseID = (Long) nextTweet.get("baseID");
				lat = (Double) ((DBObject) (nextTweet.get("geoposition")))
						.get("lat");
				lon = (Double) ((DBObject) (nextTweet.get("geoposition")))
						.get("lng");
				messageContent = (String) nextTweet.get("messageContent");
				neighborhoodId = (String) nextTweet.get("neighborhoodId");
				timestamp = (Date) nextTweet
						.get("createdAt");
				userId = (String) nextTweet.get("userId");
	
				tweet = new Tweet(baseID, lat, lon, messageContent, neighborhoodId,
						timestamp, userId);
	
				tweets.add(tweet);
			}catch(Exception ex){
				//Error getting a tweet. Flow should continue for the rest of stored data
			}
		}

		return tweets;

	}

	/**
	 * Stores a new object on the tweets collection
	 * @param tweet
	 */
	public void storeTweet(Tweet tweet) {
		BasicDBObject doc = new BasicDBObject("baseID", tweet.getBaseID())
				.append("messageContent", tweet.getMessageContent())
				.append("neighborhoodId", tweet.getNeighborhoodId())
				.append("createdAt", tweet.getTimestamp())
				.append("userId", tweet.getUserId());

		JSONObject jO = new JSONObject();
		jO.put("lat", tweet.getLat());
		jO.put("lng", tweet.getLon());

		doc.append("geoposition", jO);

		mongoDBcollectionTweets.insert(doc);
	}

	/**
	 * Gets all the stored tweets. 
	 * Could take a while when reading the checkins collections from cicerone
	 * @return
	 */
	public ArrayList<Tweet> getAllStoredTweets(){
		DBCursor cursor = (DBCursor) mongoDBcollectionTweets.find();
		ArrayList<Tweet> tweets = new ArrayList<Tweet>();

		DBObject nextTweet;
		Long baseID;
		Double lat;
		Double lon;
		String messageContent;
		String neighborhoodId;
		Date timestamp;
		String userId;

		Tweet tweet;

		while (cursor.hasNext()) {

			nextTweet = cursor.next();

			baseID = (Long) nextTweet.get("baseID");

			lat = (Double) ((DBObject) (nextTweet.get(locationField)))
					.get(latField);
			lon = (Double) ((DBObject) (nextTweet.get(locationField)))
					.get(lonField);
			
			messageContent = (String) nextTweet.get("messageContent");
			neighborhoodId = (String) nextTweet.get("neighborhoodId");
			timestamp = (Date) nextTweet
					.get("createdAt");
			userId = (String) nextTweet.get("userId");

			tweet = new Tweet(baseID, lat, lon, messageContent, neighborhoodId,
					timestamp, userId);

			tweets.add(tweet);
		}

		return tweets;
	}
	
	/**
	 * Closes the connection with the remote database
	 */
	public void closeConnection(){
		mongo.close();
	}

	/**
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MongoDBConnector [dbUrl=" + dbUrl + ", dbPort=" + dbPort
				+ ", dbName=" + dbName + ", tweetsCollectionName="
				+ tweetsCollectionName + "]";
		
	}
	
	
}
