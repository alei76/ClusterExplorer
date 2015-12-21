package org.bdigital.mob.c2020.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utils. Utils functionalities.
 * 
 * @author dsolans (dsolans@bdigital.org)
 * @version 0.1 22/05/2014
 */
public class Utils {
	
	public static Date parseJSONDate(String jsonDate) throws ParseException {
		final String TWITTER = "EEE MMM dd HH:mm:ss ZZZZZ yyyy";
		SimpleDateFormat sf = new SimpleDateFormat(TWITTER, Locale.ENGLISH);
		sf.setLenient(true);
		return sf.parse(jsonDate);
	}
	
	
	public static Date parseStringifiedDate(String stringifiedDate) throws ParseException{

		SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
		sf.setLenient(true);
		return sf.parse(stringifiedDate);
	}


	
	
}
