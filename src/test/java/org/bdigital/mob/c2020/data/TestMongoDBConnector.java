package org.bdigital.mob.c2020.data;

import static org.junit.Assert.assertEquals;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import junit.framework.Assert;

import org.bdigital.mob.c2020.data.MongoDBConnector;
import org.bdigital.mob.c2020.model.Tweet;
import org.junit.Test;


public class TestMongoDBConnector {

	
	/**
	 * Stores 2 tweets on different timestamps
	 * Does the query to extract just one of those stored tweets
	 * and asserts the correct extraction of the tweets
	 * @throws UnknownHostException
	 * @throws ParseException
	 */
	@Test
	public void getTweetsByDate() throws UnknownHostException, ParseException {

		String dbUrl = "localhost";
		String dbPort = "27017";
		String dbName = "tweets_db";
		String locationField = "geoLocation";	
		String latField = "latField";	
		String lonField = "lonField";	
		String dbusername = "test";
		String dbpasswd = "test";	
		String collectionName = "test";

		MongoDBConnector mongo = new MongoDBConnector(dbUrl, dbPort, dbName, collectionName,
				locationField, latField, lonField, dbusername, dbpasswd);
		
		
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/M/yyyy", Locale.ENGLISH);
		
		mongo.removeAllStoredTweets("I understand what Im doing");
		
		Date timestamp1 = dateFormatter.parse("10/06/2014");
		Date timestamp2 = dateFormatter.parse("01/06/2014");
	
		Tweet tweet1 = new Tweet(1L, 41.10, 2.14, "New message", "Eixample", timestamp1, "user1");
		Tweet tweet2 = new Tweet(2L, 42.05, 2.09, "New message2", "Barceloneta", timestamp2, "user2");
	
		mongo.storeTweet(tweet1);
		mongo.storeTweet(tweet2);
		
		Date startTimestamp = dateFormatter.parse("05/06/2014");
		Date endTimestamp = dateFormatter.parse("15/06/2014");
		
		ArrayList<Tweet> tweets = mongo.getStoredTweets(startTimestamp, endTimestamp);
		
		assertEquals(1, tweets.size());
		
		Tweet tweet = tweets.get(0);
		
		System.out.println(tweet);
		
		assertEquals(tweet1.getBaseID(), tweet.getBaseID());
		assertEquals(tweet1.getLat(), tweet.getLat());
		assertEquals(tweet1.getLon(), tweet.getLon());
		assertEquals(tweet1.getMessageContent(), tweet.getMessageContent());
		assertEquals(tweet1.getNeighborhoodId(), tweet.getNeighborhoodId());
		assertEquals(tweet1.getTimestamp(), tweet.getTimestamp());
		assertEquals(tweet1.getUserId(), tweet.getUserId());
		
		
		
	}

}