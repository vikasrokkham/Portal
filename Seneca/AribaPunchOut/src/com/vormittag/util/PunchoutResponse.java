package com.vormittag.util;

import java.util.Date;

public class PunchoutResponse
{
	private String	timestamp	= "";
	private int		code;
	private String	text		= "";
	private String	message		= "";
	private String	payloadID	= "";
	
	public PunchoutResponse(int code, String text)
	{
		this(code, text, "");
	}
	
	public PunchoutResponse(int code, String text, String message)
	{
		this.timestamp = createTimestamp();
		this.payloadID = createPayloadId();
		this.code = code;
		this.text = text;
		this.message = message;
	}
	
	// Timestamp
	// The date and time the message was sent, in ISO 8601 format.
	// This value should not change for retry attempts.
	// The format is YYYY-MM-DDThh:mm:ss-hh:mm (for example, 1997-07-16T19:20:30+01:00).
	
	private String createTimestamp()
	{
		Date date = new Date();
		return date.toString();
	}
	
	// PayloadId
	// A unique number with respect to space and time, used for
	// logging purposes to identify documents that might have been
	// lost or had problems. This value should not change for retry attempts.
	// The recommended implementation is: datetime.process id.random number@hostname
	private String createPayloadId()
	{
		ConfigCachePunchoutAriba cc = ConfigCachePunchoutAriba.getInstance();
		long pseudoRandom = System.nanoTime(); // Close enough for government work...
		
		return this.timestamp + "." + Long.toString(pseudoRandom).substring(2, 6) + "." + Long.toOctalString(pseudoRandom).substring(2, 6) + "@" + cc.getDomain();
	}

	public String getTimestamp()
	{
		return timestamp;
	}

	public int getCode()
	{
		return code;
	}

	public String getText()
	{
		return text;
	}

	public String getMessage()
	{
		return message;
	}

	public String getPayloadID()
	{
		return payloadID;
	}
}
