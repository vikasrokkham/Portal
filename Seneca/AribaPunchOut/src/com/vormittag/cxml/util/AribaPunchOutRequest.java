package com.vormittag.cxml.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.logging.Logger;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.JDOMException;

import com.vormittag.util.ExceptionUtils;

public class AribaPunchOutRequest extends HttpServlet implements Servlet {
	private static final String cName = AribaPunchOutRequest.class.getCanonicalName();
	private static Logger log = Logger.getLogger(cName);
	private static final long serialVersionUID = 1815524903742274704L;
	
	public AribaPunchOutRequest() {
		super();
	}

	/* (non-Java-doc)
	 * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest arg0, HttpServletResponse arg1)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String cMethod = "doGet";
		log.entering(cName, cMethod);
		doPost(req,resp);
		log.exiting(cName, cMethod);
	}

	/* (non-Java-doc)
	 * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest arg0, HttpServletResponse arg1)
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String cMethod = "doPost";
		log.entering(cName, cMethod);
		
		String session = req.getSession().getId();
    	ParseRequest parseRequest = new ParseRequest(false,session);
		try {
			InputStream in = req.getInputStream();
			resp.setContentType("text/xml; charset=\"utf-8\"");
			PrintStream xmlBack= new PrintStream(resp.getOutputStream());
			//XML Response is printed to the outputStream in send response which is called directly in ParseRequest class
			parseRequest.read(in,xmlBack);
		} catch(JDOMException jE) { 
			log.severe("JDOM Exception: "+jE.getMessage() + "\n" + ExceptionUtils.getStackTrace(jE)); 
		}	
		catch(Exception e) {
			log.severe("Exception: "+e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e)); 
		}
		log.exiting(cName, cMethod);
	}
}