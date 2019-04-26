/*
 * Created on Nov 6, 2006
 */
package com.vormittag.cxml.util;

import java.net.*;
//import java.io.BufferedInputStream;
import java.io.FileOutputStream;
//import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * @author johnv
 */
public class PostXML {
	
	public PostXML() {
		super();
	}

	public static void main(String[] args) {
		ParseRequest parseRequest = new ParseRequest(false, null);
		String xmlText = "<!DOCTYPE cXML SYSTEM \"http://xml.cXML.org/schemas/cXML/1.2.014/cXML.dtd\">" +
			"<cXML payloadID=\"456778-199@acme.com\" xml:lang=\"en-US\"" +
			"timestamp=\"2001-03-12T18:39:09-08:00\">" +
			"<Header>" +
			"<From>" +
			"<Credential domain=\"NetworkId\">" +
			"<Identity>AN0100000123</Identity>" +
			"</Credential>" +
			"</From>" +
			"<To>" +
			"<Credential domain=\"NetworkId\">"+
			"<Identity>AN01000000001</Identity>" +
			"</Credential>" +
			"</To>" +
			"<Sender>" +
			"<Credential domain=\"NetworkId\">"+
			"<Identity>AN0100000123</Identity>" +
			"<SharedSecret>abracadabra</SharedSecret>" +
			"</Credential>" +
			"<UserAgent>Our Download Application, v1.0</UserAgent>" +
			"</Sender>" +
			"</Header>" +
			"<Request>" +
			"<ProfileRequest />" +
			"</Request>" +
			"</cXML>";
		String url = "https://service.ariba.com/service/transaction/cxml.asp";
		//String url = "https://service.ariba.com/ANCXMLDispatcher.aw/ad/cxml400Test";
		try {
			URL server = new URL(url);
		    HttpURLConnection c = (HttpURLConnection)server.openConnection();
		    c.setRequestMethod("POST");
		    c.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
		    c.setDoOutput(true);
		    c.setDoInput(true);
		    OutputStreamWriter out = new OutputStreamWriter(c.getOutputStream(),
		    	"UTF8");
		    out.write(xmlText);	
		    FileOutputStream fos = new FileOutputStream("c:\\t.tmp");
		    FileOutputStream fos2 = new FileOutputStream("c:\\t3.tmp");
			PrintStream outPrint = new PrintStream(fos2);
			
			//******************************************************************************************
			 // read response and write to System.out
	        System.out.println("Reading Gateway's response:");
	        System.out.println();
	        InputStream in2 = c.getInputStream();
	        
	        //******************************************************************************************
		    parseRequest.read(in2,outPrint);
	        fos.close();
		    out.close();
		    c.disconnect();
		} catch(Exception e) { System.out.println("Exception = "+e.getMessage());}
	}
}
