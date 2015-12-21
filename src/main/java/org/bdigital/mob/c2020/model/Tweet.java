package org.bdigital.mob.c2020.model;

import java.util.Date;


/**
 * Tweet. Tweet Pojo
 * 
 * @author dsolans (dsolans@bdigital.org)
 * @version 0.1 12/06/2014
 */
public class Tweet {
	private Long baseID;
	private Double lat;
	private Double lon;
	private String messageContent;
	private String neighborhoodId;
	private Date timestamp;
	private String userId;
	
	public Tweet(Long baseID, Double lat, Double lon, String messageContent,
			String neighborhoodId, Date timestamp, String userId) {
		super();
		this.baseID = baseID;
		this.lat = lat;
		this.lon = lon;
		this.messageContent = messageContent;
		this.neighborhoodId = neighborhoodId;
		this.timestamp = timestamp;
		this.userId = userId;
	}
	
	
	public Long getBaseID() {
		return baseID;
	}
	public void setBaseID(Long baseID) {
		this.baseID = baseID;
	}
	public Double getLat() {
		return lat;
	}
	public void setLat(Double lat) {
		this.lat = lat;
	}
	public Double getLon() {
		return lon;
	}
	public void setLon(Double lon) {
		this.lon = lon;
	}
	public String getMessageContent() {
		return messageContent;
	}
	public void setMessageContent(String messageContent) {
		this.messageContent = messageContent;
	}
	public String getNeighborhoodId() {
		return neighborhoodId;
	}
	public void setNeighborhoodId(String neighborhoodId) {
		this.neighborhoodId = neighborhoodId;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}


	@Override
	public String toString() {
		return "Tweet [baseID=" + baseID + ", lat=" + lat + ", lon=" + lon
				+ ", messageContent=" + messageContent + ", neighborhoodId="
				+ neighborhoodId + ", timestamp=" + timestamp + ", userId="
				+ userId + "]";
	}
	
}
