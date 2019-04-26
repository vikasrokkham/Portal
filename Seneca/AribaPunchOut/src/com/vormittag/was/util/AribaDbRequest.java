/*
 * Created on 12/9/14 by GHV.
 * Based on earlier code from Waytek & Joshen by John V.
 */
package com.vormittag.was.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.jdom.Content;
import org.jdom.DataConversionException;
import org.jdom.Element;

import com.vormittag.cxml.util.ParseRequest;
import com.vormittag.util.AribaOrder;
import com.vormittag.util.AribaOrderDetails;
import com.vormittag.util.AribaOrderDetailsLineNumberComparator;
import com.vormittag.util.ConfigCachePunchoutAriba;
import com.vormittag.util.ExceptionUtils;
import com.vormittag.util.OrderDetail;
import com.vormittag.util.OrderHeader;
import com.vormittag.util.PunchoutResponse;
import com.vormittag.util.SqlUtil;
/**
 * @author johnv
 * @author mwilson
 */
public class AribaDbRequest {
	
	private static final String cName = AribaDbRequest.class.getCanonicalName();
	
	private static final String SESSION_STATUS_PUNCHOUT_INIT		= "U",
								SESSION_STATUS_PUNCHOUT_SENT		= "N",
								SESSION_STATUS_PUNCHOUT_APPROVED	= "A";
	
	/*
	 * ORDER_STATUS CODES:
	 * 	A - Approved
	 *  C - Completed or set to C so I do not pick up for lost cart
	 *  I - Incomplete (this is the initial state)
	 *  P - Pending Approval
	 *  Q - Saved Quote
	 *  R - Return Authorization
	 *  S - Saved Cart
	 *  W - Waiting Approval
	 */
	public static final String STATUS_APPROVED = "A";
	public static final String STATUS_COMPLETED = "C";
	public static final String STATUS_INCOMPLETE = "I";
	public static final String STATUS_PENDING = "P";
	public static final String STATUS_QUOTE = "Q";
	public static final String STATUS_RETURN = "R";
	public static final String STATUS_SAVED = "S";
	public static final String STATUS_WAITING = "W";
	public static final String STATUS_PUNCHOUT_PENDING = "N";
	public static final String STATUS_PUNCHOUT_COMPLETED = "T";
	
	public static final String DEFAULT_ADDRESS_ID = "_DFLT_";
	private static Logger log = Logger.getLogger(cName);
	
	//NOTE: decimal precision will be 2 for this.
	private static final int DECIMAL_PRECISION = 2;
	private static final String CURRENT_LANGUAGE_CODE = "en";
	
	String theSession = null;
	String jndiName = null;
	String providerURL = null;
	String theUser = null;
	String company = null;
	String websiteId = null;
	String buyerCookie = null;
	
	public AribaDbRequest(String company, String websiteId, String jndiName, String providerURL, String session, String user, String buyerCookie) {
		super();
		this.company = company;
		this.websiteId = websiteId;
		this.jndiName = jndiName;
		this.providerURL = providerURL;
		this.theSession = session;
		this.theUser = user;
		this.buyerCookie = buyerCookie;
	}

	public AribaDbRequest(String jndiName,String providerURL,String session,String user) {
		this(null, null, jndiName, providerURL, session, user, null);
	}
	
	//OrderNumber will contain wither the Cart_key or order number depending on whether this is a Part/marketing or Lansa order
	public boolean completePunchOutOrder(AribaOrder order, String networkId, String addressId)
	{
		String cMethod = "completePunchOutOrder";
		log.entering(cName, cMethod);
		
		//set the customer and shipto numbers
		this.setCustomerAndShipToInfoForOrder(order, networkId, addressId);
		
		Connection conn = null;
		PreparedStatement stmt = null;
		String sql = null;
		
		boolean success = false;
		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY,"com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);

			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
		    conn = ds.getConnection();
		    
		    //if we need to update VEBSHIP, let's do it first.
		    if (order.needToUpdateShipTo())
		    {
			    sql =
			    	"UPDATE VEBSHIP SET "+
			    		"SHIPTO_NAME=?,"+
			    		"ADDRESS1=?,"+
			    		"ADDRESS2=?,"+
			    		"ADDRESS3=?,"+
			    		"CITY=?,"+
			    		"STATE=?,"+
			    		"ZIP_CODE=?,"+
			    		"COUNTRY=?,"+
			    		"UPDATED_BY=?,"+
			    		"DATE_UPDATED=?,"+
			    		"TIME_UPDATED=?,"+
			    		"SHIPTO_NBR=? "+
			    	"WHERE ORDER_NBR = ?";
			    
			    log.logp(Level.FINEST, cName, cMethod, sql);
			    
			    stmt = conn.prepareStatement(sql);
			    
			    int q = 0;
			    stmt.setString(++q, order.getName());
			    stmt.setString(++q, order.getAddress1());
			    stmt.setString(++q, order.getAddress2());
			    stmt.setString(++q, order.getAddress3());
			    stmt.setString(++q, order.getCity());
			    stmt.setString(++q, order.getState());
			    stmt.setString(++q, order.getZip());
			    stmt.setString(++q, order.getCountry());
			    stmt.setString(++q, "PUNCHOUT");
			    
			    long now = Calendar.getInstance().getTimeInMillis();
			    java.sql.Date currentDate = new java.sql.Date(now);
			    java.sql.Time currentTime = new java.sql.Time(now);
			    stmt.setDate(++q, currentDate);
			    stmt.setTime(++q, currentTime);
			    
			    stmt.setString(++q, order.getShipto_nbr());
			    stmt.setInt(++q, order.getOrderNumber());
			    
			    log.logp(Level.FINEST, cName, cMethod, 
			    	SqlUtil.magicPrintSQL(sql,
			    		order.getName(),
			    		order.getAddress1(),
			    		order.getAddress2(),
			    		order.getAddress3(),
			    		order.getCity(),
			    		order.getState(),
			    		order.getZip(),
			    		order.getCountry(),
			    		"PUNCHOUT",
			    		currentDate,
			    		currentTime,
			    		order.getShipto_nbr(),
			    		order.getOrderNumber()
			    	)
			    );
			    
			    int row_count = stmt.executeUpdate();
			    
			    log.logp(Level.FINEST, cName, cMethod, "updated "+row_count+" VEBSHIP records.");
			    
			    try
			    {
			    	stmt.close();
			    }
			    catch (SQLException e)
			    {
			    	//nothing to be done, but just want to be sure everything keeps flowing
			    }
		    }
		    
		    sql = 
		    	"SELECT "+
					"ORDER_STATUS,"+
					"PO_NUMBER"+
					(order.needToUpdateShipTo()?",CUSTOMER_NBR,SHIPTO_NBR":"")+
				" FROM VEBSESS "+
				"WHERE "+
					//"CART_KEY = ?";
					"WEB_ORDER = ? and "+
					"ORDER_STATUS = '"+SESSION_STATUS_PUNCHOUT_SENT+"'";
			
			stmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			
			int q = 0;
			
			stmt.setInt(++q, order.getOrderNumber());
			
			log.finer("Executing SQL: " + SqlUtil.magicPrintSQL(sql, order.getOrderNumber()));				
			//Update the record to complete the order
			
			//Execute the Sql here and then return whether or not I am successful
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next())
			{
				if (order.needToUpdateShipTo())
				{
					rs.updateString("CUSTOMER_NBR", order.getCustomer_nbr());
					rs.updateString("SHIPTO_NBR", order.getShipto_nbr());
				}
			 	rs.updateString("ORDER_STATUS", SESSION_STATUS_PUNCHOUT_APPROVED);
			 	rs.updateString("PO_NUMBER", order.getPoNumber());
			 	rs.updateRow();
			 	if (!rs.next())
			 	{
			 		success = true;
			 	}
			}
		} 	
		catch(SQLException sqle) {
			log.severe("from ArDbRequest.JAVA: "+sqle.getMessage() + "\n" + ExceptionUtils.getStackTrace(sqle)); 
		}
		catch(NamingException ne) { 
			log.severe(ne.getMessage() + "\n " + ExceptionUtils.getStackTrace(ne)); 
		}
		finally {
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlex) {
					// ignore -- as we can't do anything about it here
				}
				stmt = null;
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlex) {
					// ignore -- as we can't do anything about it here
				}
				conn = null;
			}
		}
		log.exiting(cName, cMethod);
		return success;	
	}
	
	public boolean checkAddressId(String addressID,String networkID){
		String cMethod = "checkAddressId";
		log.entering(cName, cMethod);
		Connection conn = null;
		PreparedStatement stmt = null;
		String sql = null;
		boolean validAddress = false;

		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY,"com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);

			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
		    conn = ds.getConnection();
			sql = "SELECT B1CUST,B1SHIP FROM VEBPUNXREF WHERE B1DEL='A' AND B1NETID=? AND B1ADDID=?";
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, networkID);
			stmt.setString(2, addressID);
			
			log.finer("Executing SQL: " + SqlUtil.magicPrintSQL(sql, networkID, addressID));
			
			ResultSet addressRecord = stmt.executeQuery();
			if (addressRecord.next()) {
				validAddress = true;
			}
			
		} catch(SQLException sqle) {
			log.severe("SQLEXCEPTION from AribaDbRequest.JAVA: "+sqle.getMessage() + "\n" + ExceptionUtils.getStackTrace(sqle));
		} catch(NamingException ne) {
			log.severe("NAMINGEXCEPTION from AribaDbRequest.JAVA: "+ne.getMessage() + "\n" + ExceptionUtils.getStackTrace(ne));
		} finally {
		    /*
		     * close any jdbc instances here that weren't
		     * explicitly closed during normal code path, so
		     * that we don't 'leak' resources...
		     */
		    if (stmt != null) {
		        try {
		        stmt.close();
		        } catch (SQLException sqlex) {
		            // ignore -- as we can't do anything about it here
		        }
		        stmt = null;
		    }
		    if (conn != null) {
		        try {
		        conn.close();
		        } catch (SQLException sqlex) {
		            // ignore -- as we can't do anything about it here
		        }
		        conn = null;
		    }
		}
		log.exiting(cName, cMethod);
		return validAddress;
	}

	public ArrayList<String> getCustShipTo(String addressID,String networkID){
		String cMethod = "getCustShipTo";
		log.entering(cName, cMethod);
		Connection conn = null;
		PreparedStatement stmt = null;
		String sql = null;
		ArrayList<String> customerInfo = new ArrayList<String>();
		
		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY,"com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);

			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
		    conn = ds.getConnection();
		    sql = "SELECT B1CUST,B1SHIP,B1CMP FROM VEBPUNXREF WHERE B1DEL='A' AND B1NETID=? AND B1ADDID=?";
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, networkID);
			stmt.setString(2, addressID);
			log.finer("Executing SQL: " + SqlUtil.magicPrintSQL(sql, networkID, addressID));
			ResultSet addressRecord = stmt.executeQuery();
			if(addressRecord.next()){
				customerInfo.add(addressRecord.getString("B1CUST").trim());
				customerInfo.add(addressRecord.getString("B1SHIP").trim());
				customerInfo.add(addressRecord.getString("B1CMP").trim());
			}
			else
				customerInfo.add("INVALID");
		} 	
		catch(SQLException sqle) { 
			System.out.println(sqle.getMessage());
		}
		catch(NamingException ne) {
			System.out.println(ne.getMessage());
		}
		finally {
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlex) {
					// ignore -- as we can't do anything about it here
				}
				stmt = null;
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlex) {
					// ignore -- as we can't do anything about it here
				}
				conn = null;
			}
		}
		log.exiting(cName, cMethod);
		return customerInfo;
	}
	public String getUser(String addressID,String networkID){
		String cMethod = "getUser";
		log.entering(cName, cMethod);
		Connection conn = null;
		PreparedStatement stmt = null;
		String sql = null;
		String userId = null;
		
		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY,"com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);

			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
		    conn = ds.getConnection();
		    sql = "SELECT B1USER FROM VEBPUNXREF WHERE B1DEL='A' AND B1NETID=? AND B1ADDID=?";
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, networkID);
			stmt.setString(2, addressID);
			log.finer("Executing SQL: " + SqlUtil.magicPrintSQL(sql, networkID, addressID));
			ResultSet addressRecord = stmt.executeQuery();
			if(addressRecord.next()){
				userId = addressRecord.getString("B1USER").trim();
				theUser = userId;		// NOTE: Side effect here! Intentionally updating object properties here. 
			}
			else
				userId = "INVALID";
		} 	
		catch(SQLException sqle) { 
			System.out.println(sqle.getMessage());
		}
		catch(NamingException ne) {
			System.out.println(ne.getMessage());
		}
		finally {
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlex) {
					// ignore -- as we can't do anything about it here
				}
				stmt = null;
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlex) {
					// ignore -- as we can't do anything about it here
				}
				conn = null;
			}
		}
		log.exiting(cName, cMethod);
		return userId;
	}

	//This is executed when the source=Ariba
	public void setPunchoutRequest(String postUrl){
		String cMethod = "setPunchoutRequest";
		log.entering(cName, cMethod);
		Connection conn = null;
		PreparedStatement stmt = null;
		String sql = null;
		Calendar calendar = Calendar.getInstance();
		Date date = calendar.getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
		sdf.setTimeZone(TimeZone.getTimeZone("EST"));
		String[] s = sdf.format(date).split("-");
		String mydate = s[0];
		String mytime = s[1];

		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY,"com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);

			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();
			sql = "INSERT INTO VEBPUNCH (PSESSION,WEBUSER,WDATE,WTIME,WBCOOKIE,WPURL) VALUES(?,?,?,?,?,?)";
			log.finest("GHV:WTF?Sess" + theSession);
			log.finest("GHV:WTF?User" + theUser);
			log.finest("GHV:WTF?Date" + mydate);
			log.finest("GHV:WTF?Time" + mytime);
			log.finest("GHV:WTF?Cook" + buyerCookie);
			String ss = (postUrl.length() >= 151)? postUrl.substring(0, 150) : postUrl;
			log.finest("GHV:WTF?pUrl" + ss);
			
			log.finer("GHV:Executing SQL: " + SqlUtil.magicPrintSQL(sql, theSession, theUser, mydate, mytime, buyerCookie, postUrl));
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, theSession);
			stmt.setString(2, theUser);
			stmt.setString(3, mydate);
			stmt.setString(4, mytime);
			stmt.setString(5, buyerCookie);
			stmt.setString(6, ss);
			log.finer("Executing SQL: " + SqlUtil.magicPrintSQL(sql, theSession, theUser, mydate, mytime, buyerCookie, ss));
			int success = stmt.executeUpdate();
			if (success != 1) {
				// Either DB2 is hosed, or something went wrong.
				log.info("SQL insert returned unexpected value " + success);
			}
		} 	
		catch(SQLException sqle) {
			log.severe(sqle.getMessage() + "\n" + ExceptionUtils.getStackTrace(sqle));
		}
		catch(NamingException ne) {
			log.severe(ne.getMessage() + "\n" + ExceptionUtils.getStackTrace(ne)); 
		}
		finally {
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlex) {
					// ignore -- as we can't do anything about it here
				}
				stmt = null;
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlex) {
					// ignore -- as we can't do anything about it here
				}
				conn = null;
			}
		}
		log.exiting(cName, cMethod);
	}
	
	//OrderNumber will contain wither the Cart_key or order number depending on whether this is a Part/marketing or Lansa order
	private boolean createOrderHeader(OrderHeader orderHeader) throws ParseException{
		String cMethod = "createOrderHeader";
		log.entering(cName, cMethod);
		boolean success = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ConfigCachePunchoutAriba cc = ConfigCachePunchoutAriba.getInstance();
		String sql = "";
		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY,"com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, cc.getProviderURL());

			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(cc.getJndiName()));
			conn = ds.getConnection();

			//Update the record to complete the order
			sql = "INSERT INTO VCORMHD (STATUS,COMPANY_NBR,NETWORK_ID,ORDER_ID,ORDER_VERSION,SHIP_ADDRESS_ID,SHIP_DELIVER_TO,SHIP_STREET,SHIP_CITY,SHIP_STATE," +
					"SHIP_ZIP,SHIP_COUNTRY,SHIP_EMAIL,BILL_ADDRESS_ID,BILL_DELIVER_TO,BILL_STREET,BILL_CITY,BILL_STATE,BILL_ZIP,BILL_COUNTRY,BILL_EMAIL,CONTACT_NAME," +
					"CUSTOMER_NBR,SHIPTO_NBR,ORDER_NBR,GE_PO_DATE) " +
					"values ('A',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
			int i = 1;
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(i++, orderHeader.getCompany());
			pstmt.setString(i++, orderHeader.getNetworkId());
			pstmt.setString(i++, orderHeader.getOrderId());
			pstmt.setInt(i++, orderHeader.getOrderVersion());
			pstmt.setString(i++, orderHeader.getShipAddressId());
			pstmt.setString(i++, this.truncate(orderHeader.getShipDeliverTo(), 30));
			pstmt.setString(i++, orderHeader.getShipStreet());
			pstmt.setString(i++, orderHeader.getShipCity());
			pstmt.setString(i++, orderHeader.getShipState());
			pstmt.setString(i++, orderHeader.getShipZip());
			pstmt.setString(i++, orderHeader.getShipCountry());
			pstmt.setString(i++, orderHeader.getShipEmail());
			pstmt.setString(i++, orderHeader.getBillAddressId());
			pstmt.setString(i++, this.truncate(orderHeader.getBillDeliverTo(), 30));
			pstmt.setString(i++, orderHeader.getBillStreet());
			pstmt.setString(i++, orderHeader.getBillCity());
			pstmt.setString(i++, orderHeader.getBillState());
			pstmt.setString(i++, orderHeader.getBillZip());
			pstmt.setString(i++, orderHeader.getBillCountry());
			pstmt.setString(i++, orderHeader.getBillEmail());
			pstmt.setString(i++, orderHeader.getContactName());
			pstmt.setString(i++, orderHeader.getCustNumber());
			pstmt.setString(i++, orderHeader.getShipNumber());
			pstmt.setDouble(i++, orderHeader.getOrderTotal());
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			Date parsed = format.parse(orderHeader.getPoDate());
			java.sql.Date sqlDate = new java.sql.Date(parsed.getTime());
			pstmt.setDate(i++, sqlDate);

			log.finer("Executing SQL: " + SqlUtil.magicPrintSQL(sql, orderHeader.getCompany(), orderHeader.getNetworkId(), orderHeader.getOrderId(), 
					orderHeader.getOrderVersion(), orderHeader.getShipAddressId(), truncate(orderHeader.getShipDeliverTo(), 30), orderHeader.getShipStreet(),
					orderHeader.getShipCity(), orderHeader.getShipState(), orderHeader.getShipZip(), orderHeader.getShipCountry(), orderHeader.getShipEmail(),
					orderHeader.getBillAddressId(), truncate(orderHeader.getBillDeliverTo(), 30), orderHeader.getBillStreet(), orderHeader.getBillCity(), orderHeader.getBillState(),
					orderHeader.getBillZip(), orderHeader.getBillCountry(), orderHeader.getBillEmail(), orderHeader.getContactName(), orderHeader.getCustNumber(),
					orderHeader.getShipNumber(), orderHeader.getOrderTotal(), sqlDate)); 

			//Execute the Sql here and then return whether or not I am successful
			pstmt.executeUpdate();
			success = true;
		} catch(SQLException sqle) {
			success = false;
			log.severe("SQLException from createOrderHeader(): "+sqle.getMessage() + "\n" + ExceptionUtils.getStackTrace(sqle));
		} 
		catch(NamingException ne) {
			success = false;
			ne.printStackTrace();
			log.severe("NamingException from createOrderHeader(): "+ne.getMessage() + "\n" + ExceptionUtils.getStackTrace(ne));
		} 
		finally {
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException sqlex) {
					// ignore -- as we can't do anything about it here
				}
				pstmt = null;
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlex) {
					// ignore -- as we can't do anything about it here
				}
				conn = null;
			}
		}
		log.exiting(cName, cMethod);
		return success;	
	}
	
	private String truncate(String input, int length) {
		if (input.length() > length) {
			input = input.substring(0,length);
		}
		return input;
	}

//	This is executed when I get a sessionId back in the header 
	private boolean checkRequest(){
		String cMethod = "checkRequest";
		log.entering(cName, cMethod);
		Connection conn = null;
		PreparedStatement stmt = null;
		boolean letMeIn = false;
		Calendar calendar = Calendar.getInstance();
		Date now = calendar.getTime();
		SimpleDateFormat dfmt = new SimpleDateFormat("yyyyMMdd-HHmmss");
		dfmt.setTimeZone(TimeZone.getTimeZone("EST"));

		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY,"com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);

			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
		    conn = ds.getConnection();
			String sql = "SELECT * FROM VEBPUNCH WHERE PSESSION = ?";
			stmt = conn.prepareStatement(sql);
			ResultSet aribaRecord = stmt.executeQuery();
			if(aribaRecord.next()){
				//Make sure this record has been created within the last 6 hours, if so return true
				String dateFromRecord = aribaRecord.getString("WDATE") + "-" + String.valueOf(aribaRecord.getInt("WTIME"));
				Date recordDate = dfmt.parse(dateFromRecord);
				
				long diff = now.getTime() - recordDate.getTime();
				if(diff < 6 * 3600000 && diff > 0) {
					letMeIn = true;
				}
				else {
					log.info("Expired Token detected. Rejecting punchout request: difference was " + diff);
				}
			}
			
		} 	
		catch(SQLException sqle) { 
			log.severe("from AribaDbRequest.JAVA: " + sqle.getMessage() + "\n" + ExceptionUtils.getStackTrace(sqle)); 
		}
		catch(NamingException ne) {
			log.severe(ne.getMessage() + "\n" + ExceptionUtils.getStackTrace(ne)); 
		} catch (ParseException e) {
			log.severe("Malformed Token detected. Rejecting punchout request: " + e.getMessage());
		}
		finally {
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlex) {
					// ignore -- as we can't do anything about it here
				}
				stmt = null;
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlex) {
					// ignore -- as we can't do anything about it here
				}
				conn = null;
			}
		}
		log.exiting(cName, cMethod);
		return letMeIn;
	}
	
	private boolean createOrderDetails(ArrayList<OrderDetail> lineItemDetails) {
		String cMethod = "createOrderDetails";
		log.entering(cName, cMethod);
		boolean success = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ConfigCachePunchoutAriba cc = ConfigCachePunchoutAriba.getInstance();
		String sql = "";
		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY,"com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, cc.getProviderURL());

			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(cc.getJndiName()));
			conn = ds.getConnection();

			//Update the record to complete the order
			sql = "INSERT INTO VCORMDE (STATUS,COMPANY_NBR,ORDER_ID,ORDER_VERSION,LINE_NUMBER,ORDER_QTY,ITEM_NBR,UNIT_PRICE,UNIT_OF_MEASURE) values ('A',?,?,?,?,?,?,?,?) ";

			pstmt = conn.prepareStatement(sql);

			for(int x =0;x<lineItemDetails.size();x++){
				OrderDetail orderDetail = new OrderDetail();
				orderDetail = (lineItemDetails.get(x));
				int i = 1;
				pstmt.setInt(i++, orderDetail.getCompany());
				pstmt.setString(i++, orderDetail.getOrderId());
				pstmt.setInt(i++, orderDetail.getOrderVersion());
				pstmt.setInt(i++, orderDetail.getLineNumber());
				pstmt.setInt(i++, orderDetail.getQuantity());
				pstmt.setString(i++, orderDetail.getItemNumber());
				pstmt.setDouble(i++, orderDetail.getUnitPrice());
				pstmt.setString(i++, orderDetail.getUnitOfMeasure());

				//Execute the Sql here and then return whether or not I am successful
				log.finer("Executing SQL: " + SqlUtil.magicPrintSQL(sql, orderDetail.getCompany(), orderDetail.getOrderId(), orderDetail.getOrderVersion(), 
						orderDetail.getLineNumber(), orderDetail.getQuantity(), orderDetail.getItemNumber(), orderDetail.getUnitPrice(), orderDetail.getUnitOfMeasure()));
				pstmt.executeUpdate();
			}
			success = true;
		} 
		catch(SQLException sqle) {
			success = false;
			log.severe("SQLException from createOrderDetails(): "+sqle.getMessage() + "\n" + ExceptionUtils.getStackTrace(sqle));
		} 
		catch(NamingException ne) {
			success = false;
			ne.printStackTrace();
			log.severe("NamingException from createOrderDetails(): "+ne.getMessage() + "\n" + ExceptionUtils.getStackTrace(ne));
		} 
		finally {
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException sqlex) {
					// ignore -- as we can't do anything about it here
				}
				pstmt = null;
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlex) {
					// ignore -- as we can't do anything about it here
				}
				conn = null;
			}
		}
		log.exiting(cName, cMethod);
		return success;	
	}
	
	public boolean itemsMatchExistingS2kOrder(AribaOrder order)
	{
		String methodName = "itemsMatchExistingS2kOrder";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement stmt = null;
		String sqlExecute = null;
		String sqlToPrint = null;
		
		//Set.equals will do the dirty work for us.
		List<AribaOrderDetails> dbItems = new ArrayList<AribaOrderDetails>();
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource) context.lookup(jndiName));
			
			conn = ds.getConnection();
			sqlExecute = "SELECT OUSEQ, OUITEM, OUUM, OUQTY FROM VCORMDE JOIN VEBSESS ON OUORD = CART_KEY AND OUCMP = COMPANY_NBR WHERE WEB_ORDER=?";
			sqlToPrint = "SELECT OUSEQ, OUITEM, OUUM, OUQTY FROM VCORMDE JOIN VEBSESS ON OUORD = CART_KEY AND OUCMP = COMPANY_NBR WHERE WEB_ORDER="+order.getOrderNumber();
			stmt = conn.prepareStatement(sqlExecute);
			stmt.setInt(1, order.getOrderNumber());
			
			log.logp(Level.FINER, cName, methodName, "Executing SQL: " + sqlToPrint);
			
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next())
			{
				int lineNumber = rs.getInt("OUSEQ");
				int quantity = rs.getBigDecimal("OUQTY").intValueExact();
				if (quantity == 0)
				{
					//don't ask me why, but this can happen.
					continue;
				}
				String itemNumber = rs.getString("OUITEM").trim();
				String unitOfMeasure = rs.getString("OUUM").trim();
				//TODO: handle duplicates
				dbItems.add(new AribaOrderDetails(lineNumber, itemNumber, unitOfMeasure, quantity));
			}
			Collections.sort(dbItems, new AribaOrderDetailsLineNumberComparator());
			
			for (AribaOrderDetails details : dbItems)
			{
				log.logp(Level.FINEST, cName, methodName, "db: "+details.toString());
			}
		}
		catch (SQLException sqle)
		{
			log.severe("SQLEXCEPTION from SciQuestDbRequest.JAVA: " + sqle.getMessage() + "\n" + ExceptionUtils.getStackTrace(sqle));
		}
		catch (NamingException ne)
		{
			log.severe("NAMINGEXCEPTION from SciQuestDbRequest.JAVA: " + ne.getMessage() + "\n" + ExceptionUtils.getStackTrace(ne));
		}
		finally
		{
			//close any jdbc instances here that weren't
			//explicitly closed during normal code path, so
			//that we don't 'leak' resources...
			if (stmt != null)
			{
				try
				{
					stmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}
				stmt = null;
			}
			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}
				conn = null;
			}
		}
		boolean match = order.getItemDetails().equals(dbItems);
		log.exiting(cName, methodName, match);
		return match;
	}
	
	private Collection<AribaOrder> validateItemsAndGetOrderNumbers(Element orderRequest, ParseRequest request, String poNumber)
	{
		String cMethod = "validateItemsAndGetOrderNumber";
		log.entering(cName, cMethod);
		
		//Vector<Integer> orderNumbers = new Vector<Integer>();
		
		Map<Integer, AribaOrder> orders = new LinkedHashMap<Integer, AribaOrder>();
		List<Content> itemsFromCXML = orderRequest.getChildren("ItemOut");
		
		if (itemsFromCXML.size() == 0)
		{
			request.setResponse(new PunchoutResponse(400, "Bad Request", "Error: no items in order request."));
			log.severe("Error: 400: Bad Request: no items in order request.");
			log.exiting(cName, cMethod, "none");
			return orders.values();
		}
		for (Content c : itemsFromCXML)
		{
			Element itemOut = ((Element)c);
			Element itemID = itemOut.getChild("ItemID");
			String itemDescription = itemOut.getChild("ItemDetail").getChildText("Description");
			int orderNumber = Integer.parseInt(itemID.getChild("SupplierPartAuxiliaryID").getText().trim());
			String itemNumber = itemID.getChild("SupplierPartID").getText().trim();
			int itemQuantity = new BigDecimal(itemOut.getAttribute("quantity").getValue().trim()).intValueExact();
			int lineNumber = 0;
			try
			{
				lineNumber = itemOut.getAttribute("lineNumber").getIntValue();
			}
			catch (DataConversionException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			log.finer("cxml item {orderNumber="+orderNumber+", itemNumber='"+itemNumber+"', itemQuantity="+itemQuantity+"}");
			
			if (itemQuantity < 1)
			{
				request.setResponse(new PunchoutResponse(400, "Bad Request", "Error: quantity cannot be less than 1. orderNumber="+orderNumber+", itemNumber="+itemNumber+", quantity="+itemQuantity+"."));
				log.severe("Error: 400: Bad Request: Error: quantity cannot be less than 1. orderNumber="+orderNumber+", itemNumber="+itemNumber+", quantity="+itemQuantity+".");
				log.exiting(cName, cMethod, "none");
				return orders.values();
			}
			
			if (orders.containsKey(orderNumber))
			{
				log.fine("putting <"+orderNumber+", <'"+itemNumber+"', "+itemQuantity+">>");
				orders.get(orderNumber).getItemDetails().add(new AribaOrderDetails(lineNumber, itemNumber, itemDescription, itemQuantity));
			}
			else
			{
				log.finer("new order number: "+orderNumber);
				log.fine("putting <"+orderNumber+", <'"+itemNumber+"', "+itemQuantity+">>");
				
				AribaOrder order = new AribaOrder(poNumber, orderNumber);
				order.getItemDetails().add(new AribaOrderDetails(lineNumber, itemNumber, itemDescription, itemQuantity));
				orders.put(orderNumber, order);
			}
		}
		
		orderNumberLoop:
		for (Integer orderNumber : orders.keySet())
		{
			AribaOrder orderItems = orders.get(orderNumber);
			int totalItemsFromCXML = orderItems.getItemDetails().size();
			
			if (totalItemsFromCXML == 0)
			{
				request.setResponse(new PunchoutResponse(400, "Bad Request", "Error: no items in order "+orderNumber+"."));
				log.severe("Error: 400: Bad Request: Error: no items in order "+orderNumber+".");
				//orderNumbers.clear();
				log.exiting(cName, cMethod, "none");
				return orders.values();
			}
			
			Connection conn = null;
			PreparedStatement stmt = null;
			String sqlExecute = null;
			String sqlToPrint = null;
			try
			{
				Hashtable<String, String> env = new Hashtable<String, String>();
				env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
				env.put(Context.PROVIDER_URL, providerURL);
				
				Context context = new InitialContext(env);
				DataSource ds = ((DataSource) context.lookup(jndiName));
				
				conn = ds.getConnection();
				sqlExecute = "SELECT OUSEQ, OUITEM, TRIM(OUDES1) || ' ' || TRIM(OUDES2) AS DESCRIPTION, OUQTY FROM VCORMDE JOIN VEBSESS ON OUORD = CART_KEY AND OUCMP = COMPANY_NBR WHERE WEB_ORDER=?";
				sqlToPrint = "SELECT OUSEQ, OUITEM, TRIM(OUDES1) || ' ' || TRIM(OUDES2) AS DESCRIPTION, OUQTY FROM VCORMDE JOIN VEBSESS ON OUORD = CART_KEY AND OUCMP = COMPANY_NBR WHERE WEB_ORDER="+orderNumber;
				stmt = conn.prepareStatement(sqlExecute);
				stmt.setInt(1, orderNumber);
				
				log.finer("Executing SQL: " + sqlToPrint);
				
				ResultSet rs = stmt.executeQuery();
				
				//test the items by name and quantity
				int totalItemsFromDB = 0;
				while (rs.next())
				{
					int quantityFromDB = (int)rs.getDouble("OUQTY");
					if (quantityFromDB == 0)
					{
						log.fine("item with quantity 0 detected.");
						continue;
					}
					totalItemsFromDB++;
					int lineNumberFromDb = rs.getInt("OUSEQ");
					String itemId = rs.getString("OUITEM").trim();
					Integer quantityFromCXML = null;
					int lineNumberFromCXML = 0;
					
					for (AribaOrderDetails itemDetails : orderItems.getItemDetails())
					{
						if (itemDetails.getItemNumber().equals(itemId))
						{
							quantityFromCXML = itemDetails.getQuantity();
							lineNumberFromCXML = itemDetails.getLineNumber();
							break;
						}
					}
					
					if (quantityFromCXML != null)
					{
						log.fine("quantity from DB from item '"+itemId+"'="+quantityFromDB+", quantity from CXML="+quantityFromCXML+".");
						
						if (quantityFromCXML != quantityFromDB)
						{
							request.setResponse(new PunchoutResponse(406, "Not Acceptable", "Quantity for item"+itemId+"does not match. cxml="+quantityFromCXML+" != "+quantityFromDB+"."));
							log.severe("406: Not Acceptable: Quantity for item "+itemId+" does not match. cxml="+quantityFromCXML+" != "+quantityFromDB+".");
							//orderNumbers.clear();
							break orderNumberLoop;
						}
						//if (lineNumberFromCXML != lineNumberFromDb)
							//this is an error, but we don't use this method any more so who cares?
					}
					else
					{
						request.setResponse(new PunchoutResponse(406, "Not Acceptable", "no matching item found for "+itemId+"."));
						log.severe("406: Not Acceptable: no matching item found for "+itemId+".");
						//orderNumbers.clear();
						break orderNumberLoop;
					}
				}
				if (totalItemsFromDB != totalItemsFromCXML)
				{
					request.setResponse(new PunchoutResponse(406, "Not Acceptable", "number of lines does not match. cxmlItemCount="+totalItemsFromCXML+", totalItemsFromDB="+totalItemsFromDB));
					log.severe("406: Not Acceptable: number of items does not match. cxmlItemCount="+totalItemsFromCXML+", totalItemsFromDB="+totalItemsFromDB);
					//orderNumbers.clear();
					break;
				}
			}
			catch (SQLException sqle)
			{
				log.severe("SQLEXCEPTION from SciQuestDbRequest.JAVA: " + sqle.getMessage() + "\n" + ExceptionUtils.getStackTrace(sqle));
			}
			catch (NamingException ne)
			{
				log.severe("NAMINGEXCEPTION from SciQuestDbRequest.JAVA: " + ne.getMessage() + "\n" + ExceptionUtils.getStackTrace(ne));
			}
			finally
			{
				//close any jdbc instances here that weren't
				//explicitly closed during normal code path, so
				//that we don't 'leak' resources...
				if (stmt != null)
				{
					try
					{
						stmt.close();
					}
					catch (SQLException sqlex)
					{
						// ignore -- as we can't do anything about it here
					}
					stmt = null;
				}
				if (conn != null)
				{
					try
					{
						conn.close();
					}
					catch (SQLException sqlex)
					{
						// ignore -- as we can't do anything about it here
					}
					conn = null;
				}
			}
			//since we have to iterate over the whole key set anyway, why not do this the dumb (and less memory/cpu intensive) way?
			//orderNumbers.add(orderNumber);
		}
		
		return orders.values();
	}
	
	public void setCustomerAndShipToInfoForOrder(AribaOrder order, String networkId, String addressId){
		String cMethod = "setCustomerAndShipToForOrder";
		log.entering(cName, cMethod);
		Connection conn = null;
		PreparedStatement stmt = null;
		String sql = null;

		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY,"com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);

			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
		    conn = ds.getConnection();
			sql = 
				"SELECT "+
					"RVCUST,"+
					"RVSHIP,"+
					"RVNAME,"+
					"RVADD1,"+
					"RVADD2,"+
					"RVADD3,"+
					"RVCITY,"+
					"RVST1,"+
					"RVMZIP,"+
					"RVCNTR,"+
					"RVDLOC,"+
					"RVEMAL,"+
					"RVSVIA,"+
					"RVMFON,"+
					"RVMFAX "+
				"FROM VEBPUNXREF "+
				"JOIN VARSHIP ON "+
					"VARSHIP.RVDEL = 'A' AND "+
					"VARSHIP.RVCMP = VEBPUNXREF.B1CMP AND "+
					"VARSHIP.RVCUST = VEBPUNXREF.B1CUST AND "+
					"VARSHIP.RVSHIP = VEBPUNXREF.B1SHIP "+
				"WHERE "+
					"VEBPUNXREF.B1DEL = 'A' AND "+
					"VEBPUNXREF.B1NETID = ? AND "+
					"VEBPUNXREF.B1ADDID = ?";
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, networkId);
			stmt.setString(2, addressId);
			
			log.finer("Executing SQL: " + SqlUtil.magicPrintSQL(sql, networkId, addressId));
			
			ResultSet addressRecord = stmt.executeQuery();
			if (addressRecord.next()) {
				order.setCustomer_nbr(addressRecord.getString("RVCUST"));
				order.setShipto_nbr(addressRecord.getString("RVSHIP"));
				order.setName(addressRecord.getString("RVNAME"));
				order.setAddress1(addressRecord.getString("RVADD1"));
				order.setAddress2(addressRecord.getString("RVADD2"));
				order.setAddress3(addressRecord.getString("RVADD3"));
				order.setCity(addressRecord.getString("RVCITY"));
				order.setState(addressRecord.getString("RVST1"));
				order.setZip(addressRecord.getString("RVMZIP"));
				order.setCountry(addressRecord.getString("RVCNTR"));
				order.setLocation(addressRecord.getString("RVDLOC"));
				order.setEmail(addressRecord.getString("RVEMAL"));
				order.setShipVia(addressRecord.getString("RVSVIA"));
				order.setPhoneNumber(addressRecord.getString("RVMFON"));
				order.setFaxNumber(addressRecord.getString("RVMFAX"));
			}
			
		} catch(SQLException sqle) {
			log.severe("SQLEXCEPTION from AribaDbRequest.JAVA: "+sqle.getMessage() + "\n" + ExceptionUtils.getStackTrace(sqle));
		} catch(NamingException ne) {
			log.severe("NAMINGEXCEPTION from AribaDbRequest.JAVA: "+ne.getMessage() + "\n" + ExceptionUtils.getStackTrace(ne));
		} finally {
		    /*
		     * close any jdbc instances here that weren't
		     * explicitly closed during normal code path, so
		     * that we don't 'leak' resources...
		     */
		    if (stmt != null) {
		        try {
		        stmt.close();
		        } catch (SQLException sqlex) {
		            // ignore -- as we can't do anything about it here
		        }
		        stmt = null;
		    }
		    if (conn != null) {
		        try {
		        conn.close();
		        } catch (SQLException sqlex) {
		            // ignore -- as we can't do anything about it here
		        }
		        conn = null;
		    }
		}
		log.exiting(cName, cMethod);
	}
	/*
	public boolean insertIntoVebcart(AribaOrder order, int cartKey)
	{
		boolean success = false;
		
		Connection conn = null;
		PreparedStatement stmt = null;
		String sqlExecute = null;
		String sqlToPrint = null;
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource) context.lookup(jndiName));
			
			conn = ds.getConnection();
			
			int lineNumber = 1;
			Iterator<AribaOrder.Details> it = order.getItemDetails().iterator();
			long now = Calendar.getInstance().getTimeInMillis();
			while (it.hasNext())
			//for (AribaOrder.Details item : order.getItemDetails())
			{
				AribaOrder.Details item = it.next();
				sqlExecute =
					"INSERT INTO VEBCART(LINE_STATUS,COMPANY_NBR,CART_KEY,LINE_NBR,ORDER_QTY,SHIPPABLE_QTY,BACKORDER_QTY,ITEM_NUMBER,DESCRIPTION_ONE,DESCRIPTION_TWO,UNIT_OF_MEASURE,TAXABLE,ITEM_PRICE,ITEM_DISCOUNT,ADDED_BY,  DATE_ADDED,TIME_ADDED,UPDATED_BY,DATE_UPDATED,TIME_UPDATED,PROMO_NUMBER,PROMO_ITEM_TYPE,ALT_LOCATION,LOCATION,PGM_PRICE,OVERRIDE_PRICE,PARENT_LINE_NBR,AVAILABLE_QTY,COMMENT,STYLE_ITEM_NBR,COUPON_OVERRIDE,RX_DEA222_YEAR,RX_DEA222_NUM,RX_DEA222_LINE,RX_DEA222_DATE,RX_CONTR_NUM,RX_CONTR_ID,RX_CONTR_PURCST,RX_LAST_PURCST,RX_CONTR_CB_VND,RX_CONTR_GEN_CB,RX_CONTR_PRIGRP,RX_HOLD_LICENSE,RX_HOLD_SOM,RX_HOLD_DEA222,RX_HOLD_FUT4,RX_HOLD_FUT5,RX_HOLD_FUT6,RX_HOLD_FUT7,RX_HOLD_FUT8,RX_HOLD_FUT9,RX_HOLD_FUT10)"+
					            " VALUES('A',"+     "1,"+       "?,"+    "?,"+    "?,"+     "?,"+         "?,"+         "?,"+       "?,"+           "?,"+           "?,"+           "?,"+   "?,"+      "?,"+         "'PUNCHOUT',?,"+      "?,"+      "'PUNCHOUT',?,"+        "?,"+        "?,"+        "?,"+           "?,"+        "?,"+    "?,"+     "?,"+          "?,"+           "?,"+         "?,"+   "?,"+          "?,"+           "?,"+          "?,"+         "?,"+          "?,"+          "?,"+        "?,"+       "?,"+           "?,"+          "?,"+           "?,"+           "?,"+           "?,"+           "?,"+       "?,"+          "?,"+        "?,"+        "?,"+        "?,"+        "?,"+        "?,"+        "?)";
				
				stmt = conn.prepareStatement(sqlExecute);
				int q = 0;
				//stmt.setString(++q, LINE_STATUS);//LINE_STATUS
				//stmt.setInt(++q, COMPANY_NBR);//COMPANY_NBR
				stmt.setInt(++q, cartKey);//CART_KEY
				stmt.setInt(++q, lineNumber++);//LINE_NBR
				stmt.setInt(++q, item.getQuantity());//ORDER_QTY
				stmt.setInt(++q, SHIPPABLE_QTY);//SHIPPABLE_QTY
				stmt.setInt(++q, BACKORDER_QTY);//BACKORDER_QTY
				stmt.setString(++q, item.getItemNumber());//ITEM_NUMBER
				stmt.setString(++q, DESCRIPTION_ONE);//DESCRIPTION_ONE
				stmt.setString(++q, DESCRIPTION_TWO);//DESCRIPTION_TWO
				stmt.setString(++q, item.getUnitOfMeasure());//UNIT_OF_MEASURE
				stmt.setString(++q, TAXABLE);//TAXABLE
				stmt.setBigDecimal(++q, ITEM_PRICE);//ITEM_PRICE
				stmt.setBigDecimal(++q, ITEM_DISCOUNT);//ITEM_DISCOUNT
				//stmt.setString(++q, ADDED_BY);//ADDED_BY
				stmt.setDate(++q, new java.sql.Date(now));//DATE_ADDED
				stmt.setTime(++q, new java.sql.Time(now));//TIME_ADDED
				//stmt.setString(++q, UPDATED_BY);//UPDATED_BY
				stmt.setDate(++q, new java.sql.Date(now));//DATE_UPDATED
				stmt.setTime(++q, new java.sql.Time(now));//TIME_UPDATED
				stmt.setInt(++q, PROMO_NUMBER);//PROMO_NUMBER
				stmt.setString(++q, PROMO_ITEM_TYPE);//PROMO_ITEM_TYPE
				stmt.setString(++q, ALT_LOCATION);//ALT_LOCATION
				stmt.setString(++q, LOCATION);//LOCATION
				stmt.setBigDecimal(++q, PGM_PRICE);//PGM_PRICE
				stmt.setString(++q, OVERRIDE_PRICE);//OVERRIDE_PRICE
				stmt.setInt(++q, PARENT_LINE_NBR);//PARENT_LINE_NBR
				stmt.setInt(++q, AVAILABLE_QTY);//AVAILABLE_QTY
				stmt.setString(++q, COMMENT);//COMMENT
				stmt.setString(++q, STYLE_ITEM_NBR);//STYLE_ITEM_NBR
				stmt.setString(++q, COUPON_OVERRIDE);//COUPON_OVERRIDE
				stmt.setInt(++q, RX_DEA222_YEAR);//RX_DEA222_YEAR
				stmt.setString(++q, RX_DEA222_NUM);//RX_DEA222_NUM
				stmt.setInt(++q, RX_DEA222_LINE);//RX_DEA222_LINE
				stmt.setInt(++q, RX_DEA222_DATE);//RX_DEA222_DATE
				stmt.setInt(++q, RX_CONTR_NUM);//RX_CONTR_NUM
				stmt.setString(++q, RX_CONTR_ID);//RX_CONTR_ID
				stmt.setBigDecimal(++q, RX_CONTR_PURCST);//RX_CONTR_PURCST
				stmt.setBigDecimal(++q, RX_LAST_PURCST);//RX_LAST_PURCST
				stmt.setString(++q, RX_CONTR_CB_VND);//RX_CONTR_CB_VND
				stmt.setString(++q, RX_CONTR_GEN_CB);//RX_CONTR_GEN_CB
				stmt.setString(++q, RX_CONTR_PRIGRP);//RX_CONTR_PRIGRP
				stmt.setString(++q, RX_HOLD_LICENSE);//RX_HOLD_LICENSE
				stmt.setString(++q, RX_HOLD_SOM);//RX_HOLD_SOM
				stmt.setString(++q, RX_HOLD_DEA222);//RX_HOLD_DEA222
				stmt.setString(++q, RX_HOLD_FUT4);//RX_HOLD_FUT4
				stmt.setString(++q, RX_HOLD_FUT5);//RX_HOLD_FUT5
				stmt.setString(++q, RX_HOLD_FUT6);//RX_HOLD_FUT6
				stmt.setString(++q, RX_HOLD_FUT7);//RX_HOLD_FUT7
				stmt.setString(++q, RX_HOLD_FUT8);//RX_HOLD_FUT8
				stmt.setString(++q, RX_HOLD_FUT9);//RX_HOLD_FUT9
				stmt.setString(++q, RX_HOLD_FUT10);//RX_HOLD_FUT10
				
				log.finer("Executing SQL: " + sqlToPrint);
				
				int rows = stmt.executeUpdate();
			}
		}
		catch (SQLException sqle)
		{
			log.severe("SQLEXCEPTION from SciQuestDbRequest.JAVA: " + sqle.getMessage() + "\n" + ExceptionUtils.getStackTrace(sqle));
		}
		catch (NamingException ne)
		{
			log.severe("NAMINGEXCEPTION from SciQuestDbRequest.JAVA: " + ne.getMessage() + "\n" + ExceptionUtils.getStackTrace(ne));
		}
		finally
		{
			//close any jdbc instances here that weren't
			//explicitly closed during normal code path, so
			//that we don't 'leak' resources...
			if (stmt != null)
			{
				try
				{
					stmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}
				stmt = null;
			}
			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}
				conn = null;
			}
		}
		return success;
	}
	
	public boolean insertIntoVebsess(AribaOrder order, int cartKey, int webOrder, int s2kOrder)
	{
		boolean success = false;
		
		Connection conn = null;
		PreparedStatement stmt = null;
		String sqlExecute = null;
		String sqlToPrint = null;
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource) context.lookup(jndiName));
			
			conn = ds.getConnection();
			
			int lineNumber = 1;
			Iterator<AribaOrder.Details> it = order.getItemDetails().iterator();
			long now = Calendar.getInstance().getTimeInMillis();
			while (it.hasNext())
			//for (AribaOrder.Details item : order.getItemDetails())
			{
				AribaOrder.Details item = it.next();
				sqlExecute =
					"INSERT INTO VEBSESS(COMPANY_NBR,CATALOG_ID,THIS_SESSION_ID,THIS_SEQUENCE,THIS_USER,THIS_SESSION_DATE,THIS_SESSION_TIME,LAST_SESSION_ID,LAST_SEQUENCE,LAST_USER,LAST_SESSION_DATE,LAST_SESSION_TIME,CART_KEY,WEB_ORDER,CUSTOMER_NBR,SHIPTO_NBR,ORDER_STATUS,IS_PRIVATE,CART_DESCRIPTION,PO_NUMBER,STATE,COUNTY,LOCAL1,LOCAL2,STATE_TAX_FLAG,COUNTY_TAX_FLAG,LOCAL1_TAX_FLAG,LOCAL2_TAX_FLAG,REQUESTED_DATE,SHIP_VIA,MATERIALTOTAL,DISCOUNT_AMOUNT,FREIGHT_CHARGE,HANDLING_CHARGE,TAX_AMOUNT,ORDER_TOTAL,DISCOUNT_PCT1,DISCOUNT_PCT2,DISCOUNT_PCT3,EMAIL_ADDRESS,ORDER_COMMENTS,PHONE_NUMBER,FAX_NUMBER,CREDIT_CARD#,CREDIT_CARD_EXP,S2K_ORDER,SHIP_VIA2,SHIP_VIA3,FREIGHT_CHARGE2,FREIGHT_CHARGE3,PROMO_CODE,PAYMENT_TERMS,LOCATION,BUYER,GC_TOTAL,BALANCE_DUE,SOURCE_ID,PUNCHOUT_POST_URL,PUNCHOUT_NET_ID,PUNCHOUT_ADDR_ID,PUNCHOUT_BUYER_COOKIE)"+
				                 "VALUES(?,"+       "?,"+      "?,"+           "?,"+         "?,"+     "?,"+             "?,"+             "?,"+           "?,"+         "?,"+     "?,"+             "?,"+             "?,"+    "?,"+     "?,"+        "?,"+      "?,"+        "?,"+      "?,"+            "?,"+     "?,"+ "?,"+  "?,"+  "?,"+  "?,"+          "?,"+           "?,"+           "?,"+           "?,"+          "?,"+    "?,"+         "?,"+           "?,"+          "?,"+           "?,"+      "?,"+       "?,"+         "?,"+         "?,"+         "?,"+         "?,"+          "?,"+        "?,"+      "?,"+        "?,"+           "?,"+     "?,"+     "?,"+     "?,"+           "?,"+           "?,"+      "?,"+         "?,"+    "?,"+ "?,"+    "?,"+       "?,"+     "?,"+             "?,"+           "?,"+            "?)";
				
				stmt = conn.prepareStatement(sqlExecute);
				int q = 0;
				stmt.setInt(++q, COMPANY_NBR);//COMPANY_NBR
				stmt.setInt(++q, CATALOG_ID);//CATALOG_ID
				stmt.setString(++q, THIS_SESSION_ID);//THIS_SESSION_ID
				stmt.setInt(++q, THIS_SEQUENCE);//THIS_SEQUENCE
				stmt.setString(++q, THIS_USER);//THIS_USER
				stmt.setDate(++q, THIS_SESSION_DATE);//THIS_SESSION_DATE
				stmt.setTime(++q, THIS_SESSION_TIME);//THIS_SESSION_TIME
				stmt.setString(++q, LAST_SESSION_ID);//LAST_SESSION_ID
				stmt.setInt(++q, LAST_SEQUENCE);//LAST_SEQUENCE
				stmt.setString(++q, LAST_USER);//LAST_USER
				stmt.setDate(++q, LAST_SESSION_DATE);//LAST_SESSION_DATE
				stmt.setTime(++q, LAST_SESSION_TIME);//LAST_SESSION_TIME
				stmt.setInt(++q, CART_KEY);//CART_KEY
				stmt.setInt(++q, WEB_ORDER);//WEB_ORDER
				stmt.setString(++q, CUSTOMER_NBR);//CUSTOMER_NBR
				stmt.setString(++q, SHIPTO_NBR);//SHIPTO_NBR
				stmt.setString(++q, ORDER_STATUS);//ORDER_STATUS
				stmt.setString(++q, IS_PRIVATE);//IS_PRIVATE
				stmt.setString(++q, CART_DESCRIPTION);//CART_DESCRIPTION
				stmt.setString(++q, PO_NUMBER);//PO_NUMBER
				stmt.setString(++q, STATE);//STATE
				stmt.setInt(++q, COUNTY);//COUNTY
				stmt.setInt(++q, LOCAL1);//LOCAL1
				stmt.setInt(++q, LOCAL2);//LOCAL2
				stmt.setString(++q, STATE_TAX_FLAG);//STATE_TAX_FLAG
				stmt.setString(++q, COUNTY_TAX_FLAG);//COUNTY_TAX_FLAG
				stmt.setString(++q, LOCAL1_TAX_FLAG);//LOCAL1_TAX_FLAG
				stmt.setString(++q, LOCAL2_TAX_FLAG);//LOCAL2_TAX_FLAG
				stmt.setDate(++q, REQUESTED_DATE);//REQUESTED_DATE
				stmt.setString(++q, SHIP_VIA);//SHIP_VIA
				stmt.setBigDecimal(++q, MATERIALTOTAL);//MATERIALTOTAL
				stmt.setBigDecimal(++q, DISCOUNT_AMOUNT);//DISCOUNT_AMOUNT
				stmt.setBigDecimal(++q, FREIGHT_CHARGE);//FREIGHT_CHARGE
				stmt.setBigDecimal(++q, HANDLING_CHARGE);//HANDLING_CHARGE
				stmt.setBigDecimal(++q, TAX_AMOUNT);//TAX_AMOUNT
				stmt.setBigDecimal(++q, ORDER_TOTAL);//ORDER_TOTAL
				stmt.setBigDecimal(++q, DISCOUNT_PCT1);//DISCOUNT_PCT1
				stmt.setBigDecimal(++q, DISCOUNT_PCT2);//DISCOUNT_PCT2
				stmt.setBigDecimal(++q, DISCOUNT_PCT3);//DISCOUNT_PCT3
				stmt.setString(++q, EMAIL_ADDRESS);//EMAIL_ADDRESS
				stmt.setString(++q, ORDER_COMMENTS);//ORDER_COMMENTS
				stmt.setString(++q, PHONE_NUMBER);//PHONE_NUMBER
				stmt.setString(++q, FAX_NUMBER);//FAX_NUMBER
				stmt.setString(++q, CREDIT_CARD#);//CREDIT_CARD#
				stmt.setInt(++q, CREDIT_CARD_EXP);//CREDIT_CARD_EXP
				stmt.setInt(++q, S2K_ORDER);//S2K_ORDER
				stmt.setString(++q, SHIP_VIA2);//SHIP_VIA2
				stmt.setString(++q, SHIP_VIA3);//SHIP_VIA3
				stmt.setBigDecimal(++q, FREIGHT_CHARGE2);//FREIGHT_CHARGE2
				stmt.setBigDecimal(++q, FREIGHT_CHARGE3);//FREIGHT_CHARGE3
				stmt.setString(++q, PROMO_CODE);//PROMO_CODE
				stmt.setString(++q, PAYMENT_TERMS);//PAYMENT_TERMS
				stmt.setString(++q, LOCATION);//LOCATION
				stmt.setString(++q, BUYER);//BUYER
				stmt.setBigDecimal(++q, GC_TOTAL);//GC_TOTAL
				stmt.setBigDecimal(++q, BALANCE_DUE);//BALANCE_DUE
				stmt.setInt(++q, SOURCE_ID);//SOURCE_ID
				stmt.setString(++q, PUNCHOUT_POST_URL);//PUNCHOUT_POST_URL
				stmt.setString(++q, PUNCHOUT_NET_ID);//PUNCHOUT_NET_ID
				stmt.setString(++q, PUNCHOUT_ADDR_ID);//PUNCHOUT_ADDR_ID
				stmt.setString(++q, PUNCHOUT_BUYER_COOKIE);//PUNCHOUT_BUYER_COOKIE
				
				log.finer("Executing SQL: " + sqlToPrint);
				
				int rows = stmt.executeUpdate();
			}
		}
		catch (SQLException sqle)
		{
			log.severe("SQLEXCEPTION from SciQuestDbRequest.JAVA: " + sqle.getMessage() + "\n" + ExceptionUtils.getStackTrace(sqle));
		}
		catch (NamingException ne)
		{
			log.severe("NAMINGEXCEPTION from SciQuestDbRequest.JAVA: " + ne.getMessage() + "\n" + ExceptionUtils.getStackTrace(ne));
		}
		finally
		{
			//close any jdbc instances here that weren't
			//explicitly closed during normal code path, so
			//that we don't 'leak' resources...
			if (stmt != null)
			{
				try
				{
					stmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}
				stmt = null;
			}
			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}
				conn = null;
			}
		}
		return success;
	}
	*/
	public Vector<Item> getItemInfo(AribaOrder order, String sessionID) {
		// Log the entrance into the method
		String methodName = "getItemInfo";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		Vector<Item> itemsOut = new Vector<Item>();
		
		String xRefCustomerNumber = getxRefCustomerNumber(order.getCustomer_nbr());
		
		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();
			
			sqlToExecute = "SELECT VCOITEM.ONCITM,VEBITEM.ITEM_NBR,VEBITEM.DESCRIPTION_ONE," +
				"VEBITEM.DESCRIPTION_TWO,VEBITEM.IS_STOCKING," +
				"VEBEXTI.WEB_DESCRIPTION_1,VEBEXTI.WEB_DESCRIPTION_2,VEBEXTI.IS_ACTIVE," +
				"VEBEXTIML.WEB_DESCRIPTION_1 as WEB_DESCRIPTION_1_ML,VEBEXTIML.WEB_DESCRIPTION_2 as WEB_DESCRIPTION_2_ML," +
				"VEBPRIC.UNIT_OF_MEASURE,VEBPRIC.UOM_DESCRIPTION,VEBPRIC.SUGGESTED_PRICE,VEBPRIC.UOM_MULTIPLIER " +
				"FROM VEBPRIC,VEBITEM " +
				"JOIN VEBEXTI ON VEBEXTI.ITEM_NBR=VEBITEM.ITEM_NBR " +
				"LEFT OUTER JOIN VEBEXTIML ON VEBEXTI.COMPANY_NBR=VEBEXTIML.COMPANY_NBR AND VEBEXTI.CATALOG_ID=VEBEXTIML.CATALOG_ID AND VEBEXTI.ITEM_NBR=VEBEXTIML.ITEM_NBR AND VEBEXTIML.LANGUAGE_CODE=? " +
				"JOIN VEBITMB ON VEBITMB.ITEM_NBR=VEBITEM.ITEM_NBR AND VEBITMB.COMPANY_NBR=? AND VEBITMB.LOCATION=? "+
				"LEFT OUTER JOIN VCOITEM ON VCOITEM.ONITEM=VEBITEM.ITEM_NBR AND VCOITEM.ONCMP=? AND VCOITEM.ONCUST=? "+
				"WHERE (VEBITEM.STATUS_FLAG='A' or VEBITEM.STATUS_FLAG='I') AND VEBITEM.COMPANY_NBR=? " +
				"AND VEBPRIC.COMPANY_NBR=? AND VEBITEM.ITEM_NBR=VEBPRIC.ITEM_NBR " +
				"AND VEBPRIC.LOCATION=? ";
				
			sqlToExecute += "AND VEBEXTI.COMPANY_NBR=? AND VEBEXTI.CATALOG_ID=? " +
				"AND (UPPER(VCOITEM.ONCITM)=? OR UPPER(VEBITEM.ITEM_NBR)=?) ";
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setString(1, CURRENT_LANGUAGE_CODE);
			pstmt.setInt(2, order.getCompany());
			pstmt.setString(3, order.getLocation());
			pstmt.setInt(4, order.getCompany());
			pstmt.setString(5, xRefCustomerNumber);
			pstmt.setInt(6, order.getCompany());
			pstmt.setInt(7, order.getCompany());
			pstmt.setString(8, order.getLocation());
			pstmt.setInt(9, order.getCompany());
			pstmt.setInt(10, order.getCompany());
			
			for (AribaOrderDetails cxmlItem : order.getItemDetails())
			{
				boolean haveItem = false;
				boolean isStyleSKU = false;
				Item item = new Item();
				
				//Get the itemNumber and quantity
				String itemNumber = cxmlItem.getItemNumber();
				int quantity = cxmlItem.getQuantity();

				sql = 	"SELECT VCOITEM.ONCITM,VEBITEM.ITEM_NBR,VEBITEM.DESCRIPTION_ONE," +
						"VEBITEM.DESCRIPTION_TWO,VEBITEM.IS_STOCKING," +
						"VEBEXTI.WEB_DESCRIPTION_1,VEBEXTI.WEB_DESCRIPTION_2,VEBEXTI.IS_ACTIVE," +
						"VEBEXTIML.WEB_DESCRIPTION_1 as WEB_DESCRIPTION_1_ML,VEBEXTIML.WEB_DESCRIPTION_2 as WEB_DESCRIPTION_2_ML," +
						"VEBPRIC.UNIT_OF_MEASURE,VEBPRIC.UOM_DESCRIPTION,VEBPRIC.SUGGESTED_PRICE,VEBPRIC.UOM_MULTIPLIER " +
						"FROM VEBPRIC,VEBITEM " +
						"JOIN VEBEXTI ON VEBEXTI.ITEM_NBR=VEBITEM.ITEM_NBR " +
						"LEFT OUTER JOIN VEBEXTIML ON VEBEXTI.COMPANY_NBR=VEBEXTIML.COMPANY_NBR AND VEBEXTI.CATALOG_ID=VEBEXTIML.CATALOG_ID AND VEBEXTI.ITEM_NBR=VEBEXTIML.ITEM_NBR AND VEBEXTIML.LANGUAGE_CODE='"+CURRENT_LANGUAGE_CODE+"' " +
						"JOIN VEBITMB ON VEBITMB.ITEM_NBR=VEBITEM.ITEM_NBR AND VEBITMB.COMPANY_NBR="+company+" AND VEBITMB.LOCATION="+order.getLocation()+" "+
						"LEFT OUTER JOIN VCOITEM ON VCOITEM.ONITEM=VEBITEM.ITEM_NBR AND VCOITEM.ONCMP="+company+" AND VCOITEM.ONCUST='"+xRefCustomerNumber+"' "+
						"WHERE (VEBITEM.STATUS_FLAG='A' or VEBITEM.STATUS_FLAG='I') AND VEBITEM.COMPANY_NBR="+company+" " +
						"AND VEBPRIC.COMPANY_NBR="+company+" AND VEBITEM.ITEM_NBR=VEBPRIC.ITEM_NBR " +
						"AND VEBPRIC.LOCATION='"+order.getLocation()+"' ";
						
				sql += "AND VEBEXTI.IS_ACTIVE='Y' AND VEBEXTI.COMPANY_NBR="+company+" AND VEBEXTI.CATALOG_ID="+websiteId+" " +
						"AND (UPPER(VCOITEM.ONCITM)='"+itemNumber.toUpperCase()+"' OR UPPER(VEBITEM.ITEM_NBR)='"+itemNumber.toUpperCase()+"') ";
				
				if (log.isLoggable(Level.FINE))
					log.logp(Level.FINE, cName, methodName, methodName+" SQL: "+sql);
				
				pstmt.setString(11, itemNumber.toUpperCase());
				pstmt.setString(12, itemNumber.toUpperCase());
				ResultSet items_rs = pstmt.executeQuery();
				
				/*
				 * Cycle through all the items in the resultset now
				 * I will append the unit of measure to a vector for each pass through and then set all the item object values 
				 * in the item object before I add the item object to the itemOut vector.  This will produce a vector of
				 * item objects that contains the pertinent item info and a vector of all the available unit of measure
				 */
				Vector<UnitOfMeasure> unitOfMeasure = new Vector<UnitOfMeasure>();
				while (items_rs.next())
				{
					haveItem = true;
					String isActive = items_rs.getString("IS_ACTIVE").trim();
					
					if (isActive.equals("N")){
						haveItem = false;
					}
					
					if (haveItem || isStyleSKU) {
						if (items_rs.getString("ONCITM") != null)
							item.setCustomerItem(items_rs.getString("ONCITM").trim());
						item.setItemNumber(items_rs.getString("ITEM_NBR").trim());
					}
					if (items_rs.getDouble("UOM_MULTIPLIER") == 1.00)
					{
						item.setSuggestedPrice(items_rs.getString("SUGGESTED_PRICE").trim());
						
					}
					
				}
				
				if (haveItem || isStyleSKU)
				{
					// Now I will just add the item details
					if (isStyleSKU)
						getItemDetail(item, order.getLocation(), 0);
					else
						getItemDetail(item, order.getLocation(), 0);
					
					// Make sure that the quantity follows minimum and multiple rules
					HashMap<String, String> minMult = item.getMinimumMultiple();
					int roundedMinQty = 0;
					if (minMult.get("Minimum") != null)
						roundedMinQty = Math.round(Float.parseFloat(minMult.get("Minimum")));
					
					/*
					System.out.println("Quick Order item: "+itemNumber);
					System.out.println("itemQty: "+itemQty);
					System.out.println("roundedMinQty: "+roundedMinQty);
					System.out.println("multiple? *"+minMult.get("Multiple")+"*");
					*/
					
					if(quantity < roundedMinQty)
					{
						
						String qtyMsg = "Item "+itemNumber+" quantity has been changed from "+quantity+
							" to "+roundedMinQty;
						item.setQoQtyMessage(qtyMsg);
						
						quantity = roundedMinQty;
					}
					else if(minMult.get("Multiple") != null && minMult.get("Multiple").equals("Y"))
					{
						//System.out.println("itemQty % roundedMinQty: "+itemQty % roundedMinQty);
						
						if(quantity % roundedMinQty > 0)
						{
							int tmp = (int)(Math.floor(quantity / roundedMinQty) * roundedMinQty) + roundedMinQty;
							
							String qtyMsg = "Item "+itemNumber+" quantity has been changed from "+
									quantity+" to "+String.valueOf(tmp);
							item.setQoQtyMessage(qtyMsg);
							
							quantity = tmp;
						}
					}
					
					//System.out.println("Finalized item qty: "+quantity);
					
					item.setQuantityForQuickOrder(Integer.toString(quantity));
					
					// Price the item here now, I need to know if it is restricted or inactive so I can treat it like an invalid item
					getWebPrice(item, order.getCustomer_nbr(), order.getShipto_nbr(), sessionID, order.getLocation());
					
					// Redmine 3293: We need to pass the item status of R to the page, so we can show the item as restricted, 
					// and also process whether the user can override it or not
					//if (item.getStatus().equals("R") || item.getStatus().equals("I"))
					if (item.getStatus().equals("I") || item.getItemType().equals("S"))
					{
						unitOfMeasure.add(new UnitOfMeasure());
						item.setItemNumber(itemNumber);
						item.setDescription1("ITEM NOT FOUND");
						item.setDescription2("");
						item.setQuantityForQuickOrder("0");
						item.setUomList(unitOfMeasure);
						//we want to handle this differently than we do in the portal
						//if we find an item number we don't sell, stop processing immediately.
						log.logp(Level.FINE, cName, methodName, "INVALID ITEM");
						itemsOut.add(item);
						return itemsOut;
					}
				}
				else
				{
					// Set the item number in the item object and the description to NOT FOUND
					unitOfMeasure.add(new UnitOfMeasure());
					item.setItemNumber(itemNumber);
					item.setDescription1("ITEM NOT FOUND");
					item.setDescription2("");
					item.setQuantityForQuickOrder("0");
					item.setUomList(unitOfMeasure);
					//we want to handle this differently than we do in the portal
					//if we find an item number we don't sell, stop processing immediately.
					log.logp(Level.FINE, cName, methodName, "ITEM NOT FOUND");
					itemsOut.add(item);
					return itemsOut;
				}
				String available = ((UnitOfMeasure)item.getUomList().firstElement()).getAvailableQty();
				if (item.isReplacement() && (available.length()==0 || Double.parseDouble(available) <= 0)) {
					AribaOrder newOrder = order.clone();
					newOrder.getItemDetails().add(new AribaOrderDetails(cxmlItem.getLineNumber(), item.getReplacementItem().trim(), quantity));
					
					Vector<Item> replacementItemVector = getItemInfo(newOrder, sessionID);
					
					if (replacementItemVector.size() > 0) {
						Item replacementItem = replacementItemVector.get(0);
						replacementItem.setDescription3("This item is a replacement for " + item.getItemNumber());
						
						replacementItem.setCxmlUom(cxmlItem.getUnitOfMeasure());
						
						itemsOut.add(replacementItem);
					}
				} else {
					item.setCxmlUom(cxmlItem.getUnitOfMeasure());
					itemsOut.add(item);
				}
				
				item.setItemLineNumber(cxmlItem.getLineNumber());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		return itemsOut;
	}
	
	private String getxRefCustomerNumber(String accountNumber)
	{
		// Log the entrance into the method
		String methodName = "getxRefCustomerNumber";
		log.entering(cName, methodName);
				
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "", xRefCustomerNum = "";
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = (DataSource)context.lookup(jndiName);
			conn = ds.getConnection();
			
			sql = "select SVCSTXRF from SVCUUDEF where SVCUUDEF.SVCMP="+company+" and SVCUUDEF.SVCUST='"+accountNumber+"'";
			sqlToExecute = "select SVCSTXRF from SVCUUDEF where SVCUUDEF.SVCMP=? and SVCUUDEF.SVCUST=?";
			
			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, "getxRefCustomerNumber SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setString(2, accountNumber);
	
			ResultSet rs = pstmt.executeQuery();
			if (rs.next())
			{
				if (!StringUtils.isEmpty(rs.getString("SVCSTXRF").trim()))
					xRefCustomerNum = rs.getString("SVCSTXRF").trim();
				else
					xRefCustomerNum = accountNumber;
			}
			else
				xRefCustomerNum = accountNumber;
		}
		catch (Exception e)
		{
			xRefCustomerNum = "";
			
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}
			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
		
		return xRefCustomerNum;
	}
	
	private boolean isStyleSKUItem(String itemNumber)
	{
		// Log the entrance into the method
		String methodName = "isStyleSKUItem";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		boolean isStyleSKUItem = false;
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = (DataSource)context.lookup(jndiName);
			conn = ds.getConnection();
			
			sql = "SELECT DISTINCT SKU_ITEM_NBR FROM VEBSTYITM JOIN VEBEXTI "+
				  "ON VEBSTYITM.STYLE_ITEM_NBR=VEBEXTI.ITEM_NBR AND VEBEXTI.COMPANY_NBR="+company+" AND VEBEXTI.CATALOG_ID="+websiteId+
				  "AND IS_ACTIVE='Y' AND VEBEXTI.ITEM_TYPE='S' "+
				  "WHERE VEBSTYITM.SKU_ITEM_NBR="+itemNumber+" AND VEBSTYITM.COMPANY_NBR="+company+" AND VEBSTYITM.CATALOG_ID="+websiteId;
			sqlToExecute = "SELECT DISTINCT SKU_ITEM_NBR FROM VEBSTYITM JOIN VEBEXTI "+
				  "ON VEBSTYITM.STYLE_ITEM_NBR=VEBEXTI.ITEM_NBR AND VEBEXTI.COMPANY_NBR=? AND VEBEXTI.CATALOG_ID=? "+
				  "AND IS_ACTIVE='Y' AND VEBEXTI.ITEM_TYPE='S' "+
				  "WHERE VEBSTYITM.SKU_ITEM_NBR=? AND VEBSTYITM.COMPANY_NBR=? AND VEBSTYITM.CATALOG_ID=?";
			
			int c=1;
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setString(c++, itemNumber);
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));

			ResultSet rs = pstmt.executeQuery();
			
			if (rs.next())
				isStyleSKUItem = true;
		}
		catch (Exception e)
		{
			isStyleSKUItem = false;
			System.err.println(sql);
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null){
				try{pstmt.close();}
				catch (SQLException sqlex){}
				pstmt = null;
			}
			if (conn != null){
				try{conn.close();}
				catch (SQLException sqlex){}
				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
		
		return isStyleSKUItem;
	}
	
	private void getItemDetail(Item itemDetail, String location, int orderGuide)
	{
		// Log the entrance into the method
		String methodName = "getItemDetail(Item, languageCode, checkActive, currentLanguageCode, orderGuide)";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		Vector<UnitOfMeasure> uomList = new Vector<UnitOfMeasure>();
		
		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = "select DESCRIPTION_ONE,DESCRIPTION_TWO,DESCRIPTION_THREE,IS_STOCKING,REPLACEMENT_ITEM," +
					"VEBEXTIML.WEB_DESCRIPTION_1 as WEB_DESCRIPTION_1_ML,VEBEXTIML.WEB_DESCRIPTION_2 as WEB_DESCRIPTION_2_ML,VEBEXTIML.BIG_IMAGE_REF as BIG_IMAGE_REF_ML,VEBEXTIML.SMALL_IMAGE_REF as SMALL_IMAGE_REF_ML," +
					"VEBEXTIML.BIG_ALT_TEXT as BIG_ALT_TEXT_ML,VEBEXTIML.SMALL_ALT_TEXT as SMALL_ALT_TEXT_ML,VEBEXTIML.EXTENDED_COMMENTS as EXTENDED_COMMENTS_ML,VEBEXTIML.META_KEYWORDS as META_KEYWORDS_ML," +
					"VEBEXTIML.META_DESCRIPTION as META_DESCRIPTION_ML,VEBEXTIML.PAGE_TITLE as PAGE_TITLE_ML,VEBEXTIML.SHORT_TEXT as SHORT_TEXT_ML," +
					"VEBEXTI.WEB_DESCRIPTION_1,VEBEXTI.WEB_DESCRIPTION_2,VEBEXTI.BIG_IMAGE_REF,VEBEXTI.SMALL_IMAGE_REF," +
					"VEBEXTI.BIG_ALT_TEXT,VEBEXTI.SMALL_ALT_TEXT,VEBEXTI.EXTENDED_COMMENTS,VEBEXTI.META_KEYWORDS," +
					"VEBEXTI.META_DESCRIPTION,VEBEXTI.PAGE_TITLE,VEBEXTI.SHORT_TEXT," +
					"VEBEXTI.CONTENT_KEY,VEBEXTI.ITEM_TYPE,VEBPRIC.OUR_PRICE," +
					"VEBPRIC.UNIT_OF_MEASURE,VEBITEM.BASE_UOM,VEBPRIC.UOM_DESCRIPTION,VEBPRIC.UOM_MULTIPLIER,VEBPRIC.SUGGESTED_PRICE,VEBITEM.MFG_STOCK_NO,VINMFGC.IRNAME,RELATED_CATEGORY1," +
					"RELATED_CATEGORY2,RELATED_CATEGORY3,HAZARD,HAZARD_SURCHARGE,HEAVY,OVERSIZED,REFRIGERATED,UNAVAILABLE,ANON_PRICE,VEBCATI.CATEGORY_ID,VEBITEM.STATUS_FLAG,VEBITMB.STATUS_FLAG as STATUS_FLAG_B," +
					"VEBSTYLE.SUGGESTED_PRICE as STYLE_SUGGESTED_PRICE "+
					"from VEBPRIC,VEBITEM " +
					"join VEBEXTI on  VEBITEM.ITEM_NBR=VEBEXTI.ITEM_NBR " +
					"left outer join VEBEXTIML on VEBEXTI.COMPANY_NBR=VEBEXTIML.COMPANY_NBR and VEBEXTI.CATALOG_ID=VEBEXTIML.CATALOG_ID and VEBEXTI.ITEM_NBR=VEBEXTIML.ITEM_NBR and VEBEXTIML.LANGUAGE_CODE='"+CURRENT_LANGUAGE_CODE+"' " +
					"join VEBITMB on VEBITMB.COMPANY_NBR="+company+" and VEBITMB.ITEM_NBR=VEBITEM.ITEM_NBR and VEBITMB.ITEM_NBR='"+itemDetail.getItemNumber()+"' and VEBITMB.LOCATION='"+location+"' "+
					"left outer join VEBCATI on VEBCATI.COMPANY_NBR="+company+" and VEBCATI.CATALOG_ID="+websiteId+" and VEBCATI.ITEM_NBR=VEBITEM.ITEM_NBR and VEBCATI.ITEM_NBR='"+itemDetail.getItemNumber()+"' and VEBCATI.PRIMARY_CATEGORY='Y' "+
					"left outer join VEBCATG on VEBCATG.COMPANY_NBR="+company+" and VEBCATG.CATALOG_ID="+websiteId+" and VEBCATG.IS_ACTIVE='Y' and VEBCATG.CATEGORY_ID=VEBCATI.CATEGORY_ID "+
					"left outer join VINMFGC on VINMFGC.IRCMP="+company+" and VINMFGC.IRCODE=VEBITEM.MFG_CODE "+
					"left outer join VEBSTYLE on VEBSTYLE.ITEM_NBR=VEBITEM.ITEM_NBR and VEBSTYLE.COMPANY_NBR="+company+" and VEBSTYLE.CATALOG_ID="+websiteId+" "+
					"where (VEBITEM.STATUS_FLAG='A' or VEBITEM.STATUS_FLAG='I') and VEBITEM.COMPANY_NBR="+company+" and VEBITEM.ITEM_NBR='"+itemDetail.getItemNumber()+"' " +
					"and VEBEXTI.COMPANY_NBR="+company+" and VEBEXTI.CATALOG_ID="+websiteId;
			
				sql += " and VEBEXTI.IS_ACTIVE='Y'";
								
				sql += " and VEBPRIC.COMPANY_NBR="+company+" " +
					"and VEBPRIC.ITEM_NBR=VEBITEM.ITEM_NBR and VEBPRIC.LOCATION='"+location+"' order by VEBPRIC.UOM_MULTIPLIER asc";
			
			sqlToExecute = "select DESCRIPTION_ONE,DESCRIPTION_TWO,DESCRIPTION_THREE,IS_STOCKING,REPLACEMENT_ITEM," +
					"VEBEXTIML.WEB_DESCRIPTION_1 as WEB_DESCRIPTION_1_ML,VEBEXTIML.WEB_DESCRIPTION_2 as WEB_DESCRIPTION_2_ML,VEBEXTIML.BIG_IMAGE_REF as BIG_IMAGE_REF_ML,VEBEXTIML.SMALL_IMAGE_REF as SMALL_IMAGE_REF_ML," +
					"VEBEXTIML.BIG_ALT_TEXT as BIG_ALT_TEXT_ML,VEBEXTIML.SMALL_ALT_TEXT as SMALL_ALT_TEXT_ML,VEBEXTIML.EXTENDED_COMMENTS as EXTENDED_COMMENTS_ML,VEBEXTIML.META_KEYWORDS as META_KEYWORDS_ML," +
					"VEBEXTIML.META_DESCRIPTION as META_DESCRIPTION_ML,VEBEXTIML.PAGE_TITLE as PAGE_TITLE_ML,VEBEXTIML.SHORT_TEXT as SHORT_TEXT_ML," +
					"VEBEXTI.WEB_DESCRIPTION_1,VEBEXTI.WEB_DESCRIPTION_2,VEBEXTI.BIG_IMAGE_REF,VEBEXTI.SMALL_IMAGE_REF," +
					"VEBEXTI.BIG_ALT_TEXT,VEBEXTI.SMALL_ALT_TEXT,VEBEXTI.EXTENDED_COMMENTS,VEBEXTI.META_KEYWORDS," +
					"VEBEXTI.META_DESCRIPTION,VEBEXTI.PAGE_TITLE,VEBEXTI.SHORT_TEXT," +
					"VEBEXTI.CONTENT_KEY,VEBEXTI.ITEM_TYPE,VEBPRIC.OUR_PRICE," +
					"VEBPRIC.UNIT_OF_MEASURE,VEBITEM.BASE_UOM,VEBPRIC.UOM_DESCRIPTION,VEBPRIC.UOM_MULTIPLIER,VEBPRIC.SUGGESTED_PRICE,VEBITEM.MFG_STOCK_NO,VINMFGC.IRNAME,RELATED_CATEGORY1," +
					"RELATED_CATEGORY2,RELATED_CATEGORY3,HAZARD,HAZARD_SURCHARGE,HEAVY,OVERSIZED,REFRIGERATED,UNAVAILABLE,ANON_PRICE,VEBCATI.CATEGORY_ID,VEBITEM.STATUS_FLAG,VEBITMB.STATUS_FLAG as STATUS_FLAG_B, " +
					"VEBSTYLE.SUGGESTED_PRICE as STYLE_SUGGESTED_PRICE "+
					"from VEBPRIC,VEBITEM " +
					"join VEBEXTI on  VEBITEM.ITEM_NBR=VEBEXTI.ITEM_NBR " +
					"left outer join VEBEXTIML on VEBEXTI.COMPANY_NBR=VEBEXTIML.COMPANY_NBR and VEBEXTI.CATALOG_ID=VEBEXTIML.CATALOG_ID and VEBEXTI.ITEM_NBR=VEBEXTIML.ITEM_NBR and VEBEXTIML.LANGUAGE_CODE=? " +
					"join VEBITMB on VEBITMB.COMPANY_NBR=? and VEBITMB.ITEM_NBR=VEBITEM.ITEM_NBR and VEBITMB.ITEM_NBR=? and VEBITMB.LOCATION=? "+
					"left outer join VEBCATI on VEBCATI.COMPANY_NBR=? and VEBCATI.CATALOG_ID=? and VEBCATI.ITEM_NBR=VEBITEM.ITEM_NBR and VEBCATI.ITEM_NBR=? and VEBCATI.PRIMARY_CATEGORY='Y' "+
					"left outer join VEBCATG on VEBCATG.COMPANY_NBR=? and VEBCATG.CATALOG_ID=? and VEBCATG.IS_ACTIVE='Y' and VEBCATG.CATEGORY_ID=VEBCATI.CATEGORY_ID "+
					"left outer join VINMFGC on VINMFGC.IRCMP=? and VINMFGC.IRCODE=VEBITEM.MFG_CODE "+
					"left outer join VEBSTYLE on VEBSTYLE.ITEM_NBR=VEBITEM.ITEM_NBR and VEBSTYLE.COMPANY_NBR=? and VEBSTYLE.CATALOG_ID=? "+
					"where (VEBITEM.STATUS_FLAG='A' or VEBITEM.STATUS_FLAG='I') and VEBITEM.COMPANY_NBR=? and VEBITEM.ITEM_NBR=? " +
					"and VEBEXTI.COMPANY_NBR=? and VEBEXTI.CATALOG_ID=? ";
					
				sqlToExecute += "and VEBEXTI.IS_ACTIVE='Y' ";
			
				sqlToExecute += "and VEBPRIC.COMPANY_NBR=? " +
					"and VEBPRIC.ITEM_NBR=VEBITEM.ITEM_NBR and VEBPRIC.LOCATION=? order by VEBPRIC.UOM_MULTIPLIER asc";
			
			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, methodName+" SQL: "+sql);

			int c = 1;
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setString(c++, CURRENT_LANGUAGE_CODE);
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setString(c++, itemDetail.getItemNumber());
			pstmt.setString(c++, location);
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setString(c++, itemDetail.getItemNumber());
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setString(c++, itemDetail.getItemNumber());
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setString(c++, location);
			
			ResultSet items_rs = pstmt.executeQuery();
			boolean setDefaultUoM = true;
			while (items_rs.next()) {
				UnitOfMeasure unitOfMeasure = new UnitOfMeasure();
				
				String web_description_1 = items_rs.getString("WEB_DESCRIPTION_1_ML");
				if (web_description_1 != null) {
					web_description_1 = web_description_1.trim();
				} else {
					//fallback to non-ml
					web_description_1 = items_rs.getString("WEB_DESCRIPTION_1");
					if (web_description_1 != null) {
						web_description_1 = web_description_1.trim();
					}
				}
				String web_description_2 = items_rs.getString("WEB_DESCRIPTION_2_ML");
				if (web_description_2 != null) {
					web_description_2 = web_description_2.trim();
				} else {
					//fallback to non-ml
					web_description_2 = items_rs.getString("WEB_DESCRIPTION_2");
					if (web_description_2 != null) {
						web_description_2 = web_description_2.trim();
					}
				}
				if (web_description_1 == null || web_description_1.equalsIgnoreCase("")) {
					itemDetail.setDescription1(items_rs.getString("DESCRIPTION_ONE").trim());
					itemDetail.setDescription2(items_rs.getString("DESCRIPTION_TWO").trim());
				} else {
					itemDetail.setDescription1(web_description_1);
					itemDetail.setDescription2(web_description_2);
				}

				itemDetail.setReplacementItem(items_rs.getString("REPLACEMENT_ITEM").trim());
				if (itemDetail.getReplacementItem().length() > 0)
					itemDetail.setReplacement(true);
				else
					itemDetail.setReplacement(false);
				
				itemDetail.setContentKey(items_rs.getString("CONTENT_KEY").trim());
				itemDetail.setIsActive(items_rs.getString("STATUS_FLAG").trim());
				
				String page_title = items_rs.getString("PAGE_TITLE_ML");
				if (page_title != null) {
					page_title = page_title.trim();
				} else {
					//fallback to non-ml
					page_title = items_rs.getString("PAGE_TITLE");
					if (page_title != null) {
						page_title = page_title.trim();
					}
				}
				itemDetail.setPageTitle(items_rs.getString("PAGE_TITLE").trim());
				
				String meta_keywords = items_rs.getString("META_KEYWORDS_ML");
				if (meta_keywords != null) {
					meta_keywords = meta_keywords.trim();
				} else {
					//fallback to non-ml
					meta_keywords = items_rs.getString("META_KEYWORDS");
					if (meta_keywords != null) {
						meta_keywords = meta_keywords.trim();
					}
				}
				itemDetail.setMetaKeywords(items_rs.getString("META_KEYWORDS").trim());
				
				String meta_description = items_rs.getString("META_DESCRIPTION_ML");
				if (meta_description != null) {
					meta_description = meta_description.trim();
				} else {
					//fallback to non-ml
					meta_description = items_rs.getString("META_DESCRIPTION");
					if (meta_description != null) {
						meta_description = meta_description.trim();
					}
				}
				itemDetail.setMetaDescription(items_rs.getString("META_DESCRIPTION").trim());
				
				itemDetail.setManufacturerItem(items_rs.getString("DESCRIPTION_THREE").trim());
				itemDetail.setStockItem(items_rs.getString("IS_STOCKING").trim());
				
				String big_image_ref = items_rs.getString("BIG_IMAGE_REF_ML");
				if (big_image_ref != null) {
					big_image_ref = big_image_ref.trim();
				} else {
					//fallback to non-ml
					big_image_ref = items_rs.getString("BIG_IMAGE_REF");
					if (big_image_ref != null) {
						big_image_ref = big_image_ref.trim();
					}
				}
				String small_image_ref = items_rs.getString("SMALL_IMAGE_REF_ML");
				if (small_image_ref != null) {
					small_image_ref = small_image_ref.trim();
				} else {
					//fallback to non-ml
					small_image_ref = items_rs.getString("SMALL_IMAGE_REF");
					if (small_image_ref != null) {
						small_image_ref = small_image_ref.trim();
					}
				}
				
				String fullImageName = big_image_ref.substring(0, big_image_ref.lastIndexOf('.')+1);
				String fullImageExt = big_image_ref.substring(big_image_ref.lastIndexOf('.')+1);
				
				String thumbImageName = small_image_ref.substring(0, small_image_ref.lastIndexOf('.')+1);
				String thumbImageExt = small_image_ref.substring(small_image_ref.lastIndexOf('.')+1);
				
				itemDetail.setFullImage(fullImageName.concat(fullImageExt.toLowerCase()));
				itemDetail.setThumbImage(thumbImageName.concat(thumbImageExt.toLowerCase()));
				
				String big_alt_text = items_rs.getString("BIG_ALT_TEXT_ML");
				if (big_alt_text != null) {
					big_alt_text = big_alt_text.trim();
				} else {
					//fallback to non-ml
					big_alt_text = items_rs.getString("BIG_ALT_TEXT");
					if (big_alt_text != null) {
						big_alt_text = big_alt_text.trim();
					}
				}
				
				String small_alt_text = items_rs.getString("SMALL_ALT_TEXT_ML");
				if (small_alt_text != null) {
					small_alt_text = small_alt_text.trim();
				} else {
					//fallback to non-ml
					small_alt_text = items_rs.getString("SMALL_ALT_TEXT");
					if (small_alt_text != null) {
						small_alt_text = small_alt_text.trim();
					}
				}
				itemDetail.setFullImageAltText(big_alt_text);
				itemDetail.setThumbImageAltText(small_alt_text);
				
				if (itemDetail.getExtDescription() == null || itemDetail.getExtDescription().equalsIgnoreCase("")) {
					Clob extended_comments = items_rs.getClob("EXTENDED_COMMENTS_ML");
					if (extended_comments == null) {
						extended_comments = items_rs.getClob("EXTENDED_COMMENTS");
					}
					itemDetail.setExtDescription(extended_comments.getSubString(1, (int)extended_comments.length()));
				}
				
				itemDetail.setItemType(items_rs.getString("ITEM_TYPE").trim());
				
				//Available qty is set in the uomList in the pricing routine
				//itemDetail.setAvailaleQuantity(items_rs.getInt("ONHAND_QTY"));
				
				unitOfMeasure.setUnitMearurePrice(items_rs.getString("OUR_PRICE").trim());
				unitOfMeasure.setUnitOfMeasure(items_rs.getString("UNIT_OF_MEASURE").trim());
				unitOfMeasure.setMultiplier(items_rs.getDouble("UOM_MULTIPLIER"));
				unitOfMeasure.setSuggestedPrice(items_rs.getString("SUGGESTED_PRICE").trim());
				
				if (setDefaultUoM) {
					itemDetail.setUnitMeasure(items_rs.getString("UNIT_OF_MEASURE").trim()); //This is set because it is used in the field name for adds
					itemDetail.setDefaultUOM(items_rs.getString("UNIT_OF_MEASURE").trim());
					//itemDetail.setCartUOM(items_rs.getString("UNIT_OF_MEASURE"));
					setDefaultUoM = false;
				}
				
				unitOfMeasure.setUnitOfMeasureDescription(items_rs.getString("UNIT_OF_MEASURE").trim()+" "+items_rs.getString("UOM_DESCRIPTION").trim());
				
				if (itemDetail.getItemType().equals("S") && items_rs.getString("STYLE_SUGGESTED_PRICE") != null)
				{
						unitOfMeasure.setUnitMearurePrice(items_rs.getString("STYLE_SUGGESTED_PRICE"));
						
						NumberFormatting nf = new NumberFormatting();
						nf.setDecimalPrecision(DECIMAL_PRECISION);
						
						unitOfMeasure.setDiscount(nf.format("0", "DECIMALPRECISIONPRICING"));
						unitOfMeasure.setAvailableQty("0");		
				}
				
				itemDetail.setManufacturerItem(items_rs.getString("MFG_STOCK_NO").trim());
				if (items_rs.getString("IRNAME") != null)
					itemDetail.setManufacturerName(items_rs.getString("IRNAME").trim());
				else
					itemDetail.setManufacturerName("");
				
				CategoryList cList = new CategoryList(company, websiteId, "", jndiName, providerURL);
				itemDetail.setRelatedCategory1(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY1"), CURRENT_LANGUAGE_CODE));
				itemDetail.setRelatedCategory2(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY2"), CURRENT_LANGUAGE_CODE));
				itemDetail.setRelatedCategory3(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY3"), CURRENT_LANGUAGE_CODE));
				
				//itemDetail.setProductComments(getProductComments(itemDetail.getItemNumber()));
				
				Clob short_text = items_rs.getClob("SHORT_TEXT_ML");
				if (short_text == null) {
					short_text = items_rs.getClob("SHORT_TEXT");
				}
				itemDetail.setProductComments(short_text.getSubString(1, (int)short_text.length()));
				
				itemDetail.setHazardFlag(items_rs.getString("HAZARD").trim());
				itemDetail.setHazardSurchargeFlag(items_rs.getString("HAZARD_SURCHARGE").trim());
				itemDetail.setHeavyFlag(items_rs.getString("HEAVY").trim());
				itemDetail.setOversizedFlag(items_rs.getString("OVERSIZED").trim());
				itemDetail.setRefrigeratedFlag(items_rs.getString("REFRIGERATED").trim());
				itemDetail.setUnavailableFlag(items_rs.getString("UNAVAILABLE").trim());
				itemDetail.setAnonPriceFlag(items_rs.getString("ANON_PRICE").trim());
				itemDetail.setItemCategory(String.valueOf(items_rs.getInt("CATEGORY_ID")));
				
				if (items_rs.getString("STATUS_FLAG").trim().equalsIgnoreCase("I"))
					itemDetail.setIsActive("I");
				else if (items_rs.getString("STATUS_FLAG_B").trim().equalsIgnoreCase("I"))
					itemDetail.setIsActive("I");
				else
					itemDetail.setIsActive(items_rs.getString("STATUS_FLAG").trim());
				
				//Add element to UnitOfMeasure List
				uomList.addElement(unitOfMeasure);
			}
			
			//Put the uomList into itemDetail
			itemDetail.setUomList(uomList);
			
			// Get the external links for this item
			itemDetail.setLinks(getLinks(itemDetail.getItemNumber()));
			
			// Get the minimum and multiple quantities for this item
			HashMap<String, String> minMultiple = this.getMinimumMultipleForItem(itemDetail);
			itemDetail.setMinimumMultiple(minMultiple);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
	}
	
	public void getWebPrice(Item itemDetail, String accountNumber, String shipToNumber, String sessionID, String location)
	{
		getWebPrice(itemDetail, accountNumber, shipToNumber, sessionID, false, location);
	}
	
	private void getWebPrice(Item itemDetail, String accountNumber, String shipToNumber, String sessionID, boolean checkMultiple, String location)
	{
		// Log the entrance into the method	
		String methodName = "getWebPrice";	
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";

		//Vector<UnitOfMeasure> uomList = new Vector<UnitOfMeasure>();
		NumberFormatting nf = new NumberFormatting();
		nf.setDecimalPrecision(DECIMAL_PRECISION);
		String qty = "1";
		if (itemDetail.getQtyInCart() != 0)
			qty = String.valueOf(itemDetail.getQtyInCart());
		else if (itemDetail.getQuantityForQuickOrder() != null && !itemDetail.getQuantityForQuickOrder().equals("0"))
			qty = itemDetail.getQuantityForQuickOrder();

		if (qty == null)
			qty = "1";
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);

			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();
			
			sqlToExecute = "call COINPRQ (?,?,?,?,?,?,?,?)";
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setInt(2, Integer.parseInt(websiteId));
			pstmt.setString(3, accountNumber);
			pstmt.setString(4, itemDetail.getItemNumber());
			pstmt.setInt(5, Integer.parseInt(qty));
			pstmt.setString(7, shipToNumber);
			pstmt.setString(8, location);
		
			// Get the price for each unit of measure that exists for this item
			String cartUOMPrice = "", defaultUOMPrice = "";
			for (UnitOfMeasure unitOfMeasure : itemDetail.getUomList())
			{
				sql = "call COINPRQ ("+company+","+websiteId+",'"+accountNumber+"','"+itemDetail.getItemNumber()+"',"+qty+",'"+unitOfMeasure.getUnitOfMeasure()+"','"+shipToNumber+"','"+location+"')";
				if (log.isLoggable(Level.FINE))	
	                log.logp(Level.FINE, cName, methodName, methodName+" SQL: "+sql);
				
				pstmt.setString(6, unitOfMeasure.getUnitOfMeasure());
				ResultSet items_rs = pstmt.executeQuery();
				if (items_rs.next())
				{
					if (log.isLoggable(Level.FINEST))
				    {
						 log.logp(Level.FINEST, cName, methodName, "items_rs.getString(1): "+items_rs.getString(1));
						 log.logp(Level.FINEST, cName, methodName, "items_rs.getString(2): "+items_rs.getString(2));
						 log.logp(Level.FINEST, cName, methodName, "items_rs.getString(3): "+items_rs.getString(3));
						 log.logp(Level.FINEST, cName, methodName, "items_rs.getString(4): "+items_rs.getString(4));
						 log.logp(Level.FINEST, cName, methodName, "items_rs.getString(5): "+items_rs.getString(5));
						 log.logp(Level.FINEST, cName, methodName, "items_rs.getString(6): "+items_rs.getString(6));
						 log.logp(Level.FINEST, cName, methodName, "items_rs.getString(7): "+items_rs.getString(7));
						 log.logp(Level.FINEST, cName, methodName, "items_rs.getString(8): "+items_rs.getString(8));
						 log.logp(Level.FINEST, cName, methodName, "items_rs.getString(9): "+items_rs.getString(9));
						 log.logp(Level.FINEST, cName, methodName, "items_rs.getString(10): "+items_rs.getString(10));
						 log.logp(Level.FINEST, cName, methodName, "items_rs.getString(11): "+items_rs.getString(11));
		            }
				
					// Redmine 3504: Fix item.price field bug
					// setCartUOM
					unitOfMeasure.setUnitMearurePrice(nf.format(items_rs.getString(2), "DECIMALPRECISIONPRICING"));
					if (!itemDetail.getCartUOM().equals("") && itemDetail.getCartUOM().trim().equalsIgnoreCase(unitOfMeasure.getUnitOfMeasure().trim()))
						cartUOMPrice = unitOfMeasure.getUnitMearurePrice();
						//itemDetail.setPrice(unitOfMeasure.getUnitMearurePrice());
					else if (!itemDetail.getDefaultUOM().equals("") && itemDetail.getDefaultUOM().trim().equalsIgnoreCase(unitOfMeasure.getUnitOfMeasure().trim()))
						defaultUOMPrice = unitOfMeasure.getUnitMearurePrice();
						//itemDetail.setPrice(unitOfMeasure.getUnitMearurePrice());
					
					unitOfMeasure.setDiscount(nf.format(items_rs.getString(3), "DECIMALPRECISIONPRICING"));
					
					String errorCode = items_rs.getString(5);
					itemDetail.setStatus(errorCode);
					//itemDetail.setDeaFlag(items_rs.getString(7).trim());
					
					unitOfMeasure.setErrorCode(errorCode);
					
					// Redmine 3504: Fix item.price field bug
					// Set the item object price field with the cart UOM price or the default UOM price, if they exist (cart UOM price takes priority)
					if (!cartUOMPrice.equals(""))
						itemDetail.setPrice(cartUOMPrice);
					else if (!defaultUOMPrice.equals(""))
						itemDetail.setPrice(defaultUOMPrice);
					
					//Set the Contract Data here 
					unitOfMeasure.setContractSource(items_rs.getString(7));
					unitOfMeasure.setContractNumber(items_rs.getString(8));
					unitOfMeasure.setContractTier(items_rs.getString(11));
					
					String contractExpiration = items_rs.getString(9);
					if (!contractExpiration.equals("0") && contractExpiration.length()<6)
						contractExpiration = "0"+contractExpiration;
					unitOfMeasure.setContractExpiration(nf.format(contractExpiration,"SENECAFROMDATE"));
					
					String contractEffectiveDate = items_rs.getString(10);
					if (!contractEffectiveDate.equals("0") && contractEffectiveDate.length()<6)
						contractEffectiveDate = "0"+contractEffectiveDate;
					unitOfMeasure.setContractEffDate(nf.format(contractEffectiveDate,"SENECAFROMDATE"));
					
					unitOfMeasure.setCost(String.valueOf(getItemCost(itemDetail.getItemNumber(), unitOfMeasure.getUnitOfMeasure(), location)));
					
					//Add this unitOfMeasure to the uomList to set back in itemDetail
					//uomList.addElement(unitOfMeasure);
					
					if (checkMultiple)
					{
						//HashMap<String, String> minMultiple = this.getMinimumMultipleForItem(itemDetail, this.locationNumber);
						HashMap<String, String> minMultiple = this.getMinimumMultipleForItem(itemDetail);
						itemDetail.setMinimumMultiple(minMultiple);
						//System.out.println("ss_getWebPrice.mm>"+itemDetail.getItemNumber()+"-minMultiple Size:"+minMultiple.size());	
					}
				
					// Redmine 3647: COINPRQ will now return -1 for available quantity if the item is not a stocking item 
					// for the selected inventory location
					double availQty = items_rs.getDouble(4);
					if (availQty >= 0.0)
					{
						if(unitOfMeasure.getAvailableQty() != null && unitOfMeasure.getAvailableQty().length()>0)
						{
							try
							{
								availQty += Double.parseDouble(unitOfMeasure.getAvailableQty());
							}
							catch (NumberFormatException n)
							{
								//ignore it, we're just going to set it
							}
						}
						//this should now also include the value we had in the uom object
						//I'd change the data type but I can't even begin to imagine all the places it's handled as a String.
						//the original author of this ought to be publicly shamed.
					    unitOfMeasure.setAvailableQty(Double.toString(availQty));
					}
				}
			}
			
			for (UnitOfMeasure unitOfMeasure : itemDetail.getUomList())
			{
				if(unitOfMeasure.getAvailableQty() != null && unitOfMeasure.getAvailableQty().length()>0)
				{
					if (Double.parseDouble(unitOfMeasure.getAvailableQty()) <= 0.0)
					{
						itemDetail.setStockItem("N");
					}
					else
					{
						itemDetail.setStockItem("Y");
					}
				}
			}
			
			//Put the new uomList back into itemDetail
			//itemDetail.setUomList(uomList);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
	    // Log the exit from the method
	    log.exiting(cName, methodName);
	}
	
	private void getWebPriceVEBPRIC(Item itemDetail, String accountNumber, String location)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		Vector<UnitOfMeasure> uomList = new Vector<UnitOfMeasure>();
		NumberFormatting nf = new NumberFormatting();
		nf.setDecimalPrecision(DECIMAL_PRECISION);
		//Use this formatter to format currency for USD...  $xx.xx
		//NumberFormat n = NumberFormat.getCurrencyInstance(Locale.US);
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);

			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sqlToExecute = "select OUR_PRICE,ONHAND_QTY from VEBPRIC,VEBITMB where VEBPRIC.COMPANY_NBR=? and VEBPRIC.ITEM_NBR=? and VEBPRIC.UNIT_OF_MEASURE=? and VEBPRIC.LOCATION=? and VEBITMB.COMPANY_NBR=? and VEBITMB.LOCATION=? and VEBITMB.ITEM_NBR=?";
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setString(2, itemDetail.getItemNumber());
			pstmt.setString(4, location);
			pstmt.setInt(5, Integer.parseInt(company));
			pstmt.setString(6, location);
			pstmt.setString(7, itemDetail.getItemNumber());
			
			for (int q = 0; q < itemDetail.getUomList().size(); q++)
			{
				UnitOfMeasure unitOfMeasure = new UnitOfMeasure();
				unitOfMeasure = (UnitOfMeasure)itemDetail.getUomList().elementAt(q);
				
				sql = "select OUR_PRICE,ONHAND_QTY from VEBPRIC,VEBITMB " +
					" where VEBPRIC.COMPANY_NBR="+company+" and VEBPRIC.ITEM_NBR='"+
					itemDetail.getItemNumber()+"' and VEBPRIC.UNIT_OF_MEASURE='"+
					unitOfMeasure.getUnitOfMeasure()+"' and VEBPRIC.LOCATION='"+location+
					"' and VEBITMB.COMPANY_NBR="+company+" and VEBITMB.LOCATION='"+location+
					"' and VEBITMB.ITEM_NBR='"+itemDetail.getItemNumber()+"'";
				
				//System.out.println("Pricing SQL = " + sql);
				
				pstmt.setString(3, unitOfMeasure.getUnitOfMeasure());
				ResultSet items_rs = pstmt.executeQuery();
				if (items_rs.next())
				{
					String price = nf.format(items_rs.getString("OUR_PRICE"), "DECIMALPRECISIONPRICING");//DECIMAL_PRECISION_PRICING_CHANGE
					unitOfMeasure.setUnitMearurePrice(price);
					unitOfMeasure.setDiscount("0.00");
					unitOfMeasure.setAvailableQty(items_rs.getString("ONHAND_QTY"));
					itemDetail.setPrice(price);
					itemDetail.setAvailaleQuantity(items_rs.getInt("ONHAND_QTY"));
				}			
				
				//Add this unitOfMeasure to the uomList to set back in itemDetail
				uomList.addElement(unitOfMeasure);			
			}				
		}
		catch (Exception e)
		{
			System.err.println(sql);
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
	}
	
	private ArrayList<ItemLinks> getLinks(String itemNumber)
	{
		// Log the entrance into the method
		String methodName = "getLinks";
		log.entering(cName, methodName);
				
		Connection conn = null;
		PreparedStatement pstmt = null;
		ArrayList<ItemLinks> links = new ArrayList<ItemLinks>();
		
		String sql = "select LINK_NAME,DESCRIPTION,LINK_TYPE,DISPLAY from VEBDLINK where COMPANY_NBR="+company+" and CATALOG_ID="+websiteId+" and ITEM_NBR='"+itemNumber+"'";
		String sqlToExecute = "select LINK_NAME,DESCRIPTION,LINK_TYPE,DISPLAY from VEBDLINK where COMPANY_NBR=? and CATALOG_ID=? and ITEM_NBR=?";
		
		log.logp(Level.FINE, cName, methodName, "getLinks SQL: "+sql);
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setString(1, company);
			pstmt.setInt(2, Integer.parseInt(websiteId));
			pstmt.setString(3, itemNumber);
	
			ResultSet items_rs = pstmt.executeQuery();
			while (items_rs.next())
			{
				ItemLinks itemLink = new ItemLinks();
				itemLink.setFile(items_rs.getString("LINK_NAME").trim());
				itemLink.setDescription(items_rs.getString("DESCRIPTION").trim());
				itemLink.setFlag(items_rs.getString("LINK_TYPE"));
				itemLink.setDisplay(items_rs.getString("DISPLAY").trim());
				links.add(itemLink);
			}
		}
		catch (Exception e)
		{
			links = new ArrayList<ItemLinks>();
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
		
		return links;
	}
	
	private HashMap<String, String> getMinimumMultipleForItem(Item itemDetail){
		// Log the entrance into the method	
		String methodName = "getMinimumMultipleForItem(Item itemDetail)";	
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		HashMap<String, String> minimumMultiple = new HashMap<String, String>();
		
		try{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = " SELECT ICINN from VINITEM WHERE ICCMP="+company+" and ICITEM="+itemDetail.getItemNumber();
			sqlToExecute = 	" SELECT ICINN from VINITEM " +
							" WHERE ICCMP=? and ICITEM=?";
			
			if (log.isLoggable(Level.FINEST))
				 log.logp(Level.FINEST, cName, methodName, "SQL: "+sql);
			
			int i=1;
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(i++, Integer.parseInt(company));
			pstmt.setString(i++, itemDetail.getItemNumber());
			
			ResultSet rs = pstmt.executeQuery();
			String minimumQuantity = "";
			
			if (rs.next()){
				if (rs.getDouble("ICINN")>0)
				{
					minimumQuantity = rs.getDouble("ICINN")+"";
					minimumMultiple.put("Minimum", minimumQuantity);
					minimumMultiple.put("Multiple", "Y");
					
					if (log.isLoggable(Level.FINEST))
						 log.logp(Level.FINEST, cName, methodName, "Minimum: "+minimumQuantity);
				}
			}
			
		}catch (Exception e){
			minimumMultiple = new HashMap<String, String>();
			e.printStackTrace();
			
		}finally{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
	    // Log the exit from the method
	    log.exiting(cName, methodName);
	    
		return minimumMultiple;
	}
	
	private double getItemCost(String itemNumber, String uomCode, String location)
	{
		// Log the entrance into the method
		String methodName = "getItemCost";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "", costColumn = "";
		double itemCost = 0.00;
		
		// S2K has a setting that determines which cost the user should see
		S2KCEE_Constants constants = new S2KCEE_Constants(company, jndiName, providerURL, "");
		S2KConstant constant = constants.getS2KConstants("OEIFPG");
		// This really gets the 25th character in the string
		char costType = constant.getAlphaField().charAt(24);

		if (log.isLoggable(Level.FINEST))
			log.logp(Level.FINEST, cName, methodName, "costType: "+costType);
		
		switch (costType)
		{
			// Last cost
			case 'L':
				costColumn = "IFLST";
			break;
			// Other cost
			case 'O':
				costColumn = "IFOTH";
			break;
			// Average cost
			case 'A':
			default:
				costColumn = "IFAVG";
			break;
		}
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = (DataSource)context.lookup(jndiName);
			conn = ds.getConnection();
			
			sql = "select "+costColumn+" as ITEM_COST,VEBPRIC.UOM_MULTIPLIER as MULT from VINITMB " +
					" join VEBPRIC on VEBPRIC.COMPANY_NBR="+company+" and VEBPRIC.ITEM_NBR='"+itemNumber+"' and VEBPRIC.LOCATION='"+location+"' and VEBPRIC.UNIT_OF_MEASURE='"+uomCode+"' "+
					" where VINITMB.IFCOMP="+company+" and VINITMB.IFLOC='"+location+"' and VINITMB.IFITEM='"+itemNumber+"' ";
			sqlToExecute = "select "+costColumn+" as ITEM_COST,VEBPRIC.UOM_MULTIPLIER as MULT from VINITMB " +
					" join VEBPRIC on VEBPRIC.COMPANY_NBR=? and VEBPRIC.ITEM_NBR=? and VEBPRIC.LOCATION=? and VEBPRIC.UNIT_OF_MEASURE=? "+
					" where VINITMB.IFCOMP=? and VINITMB.IFLOC=? and VINITMB.IFITEM=? ";
			
			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, "getItemCost SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setString(2, itemNumber);
			pstmt.setString(3, location);
			pstmt.setString(4, uomCode);
			pstmt.setInt(5, Integer.parseInt(company));
			pstmt.setString(6, location);
			pstmt.setString(7, itemNumber);
	
			ResultSet rs = pstmt.executeQuery();
			if (rs.next())
			{
				itemCost = rs.getDouble("ITEM_COST");
				double mult = rs.getDouble("MULT");
				if (mult <= 0.00)
					mult = 1.00;
				itemCost *= mult;
			}
		}
		catch (Exception e)
		{
			itemCost = 0.00;
			
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}
			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
		
		return itemCost;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public int getOrderNumber()
	{
		String increaseKeyValueFlag = "yes";
		String constantsKey = "webOrderNumberKey";
		S2KCEE_Constants constants = new S2KCEE_Constants(company, jndiName, providerURL, theUser);
		int orderNumber = constants.getConstantsValue(constantsKey, increaseKeyValueFlag);
		
		// Check to see if the order number already exists. If it does, get a new order number
		while (orderNumberExists(orderNumber))
		{
			orderNumber = constants.getConstantsValue(constantsKey, increaseKeyValueFlag);
		}
		
		return orderNumber;
	}
	
	private boolean orderNumberExists(int orderNumber)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		boolean orderExists = false;

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = "select * from VEBSESS where WEB_ORDER="+orderNumber;
			sqlToExecute = "select * from VEBSESS where WEB_ORDER=?";
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, orderNumber);
			
			ResultSet rs = pstmt.executeQuery();
			if (rs.next())
				orderExists = true;
		}
		catch (Exception e)
		{
			orderExists = false;
			System.err.println(sql);
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}

		return orderExists;
	}
	
	public int getCartId()
	{
		String increaseKeyValueFlag = "yes";
		String constantsKey = "cartIdKey";
		S2KCEE_Constants constants = new S2KCEE_Constants(company, jndiName, providerURL, theUser);
		int cartID = constants.getConstantsValue(constantsKey, increaseKeyValueFlag);
		
		while (cartIDExists(cartID))
		{
			cartID = constants.getConstantsValue(constantsKey, increaseKeyValueFlag);
		}
		
		return cartID;
	}
	
	private boolean cartIDExists(int cartID)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		boolean cartIDExists = false;

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = "select * from VEBSESS where CART_KEY="+cartID;
			sqlToExecute = "select * from VEBSESS where CART_KEY=?";
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, cartID);
			
			ResultSet rs = pstmt.executeQuery();
			if (rs.next())
				cartIDExists = true;
		}
		catch (Exception e)
		{
			cartIDExists = false;
			System.err.println(sql);
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}

		return cartIDExists;
	}
	
	public void createVEBSESSRecord(AribaOrder order, String orderStatus, String orderMessage, String email, String punchoutUrl, String punchoutCookie, String punchoutAddId, String punchoutNetId, int sequence)
	{
		// Log the entrance into the method
		String methodName = "createVEBSESSRecord(email)";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";

		Calendar calendar = new GregorianCalendar();
		Date date = calendar.getTime();
		java.sql.Date mydate = new java.sql.Date(date.getTime());
		java.sql.Time mytime = new java.sql.Time(date.getTime());

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = "INSERT INTO VEBSESS (COMPANY_NBR,CATALOG_ID,THIS_SESSION_ID,THIS_SEQUENCE,THIS_USER,THIS_SESSION_DATE,THIS_SESSION_TIME,"+
				"CART_KEY,WEB_ORDER,CUSTOMER_NBR,SHIPTO_NBR,ORDER_STATUS,IS_PRIVATE,CART_DESCRIPTION,EMAIL_ADDRESS," +
				"LOCATION,PUNCHOUT_POST_URL,PUNCHOUT_BUYER_COOKIE,PUNCHOUT_NET_ID,PUNCHOUT_ADDR_ID) VALUES ("+
				company+","+websiteId+",'"+this.theSession+"',"+sequence+",'"+theUser+"','"+mydate+"','"+mytime+"',"+order.getCartKey()+","+order.getOrderNumber()+",'"+
				order.getCustomer_nbr()+"','"+order.getShipto_nbr()+"','"+orderStatus+"','N','"+orderMessage.replace('\'', ' ')+"','"+email+"','"+order.getLocation()+","+
				punchoutUrl+","+punchoutCookie+","+punchoutAddId+","+punchoutNetId+"')";
			sqlToExecute = "INSERT INTO VEBSESS (COMPANY_NBR,CATALOG_ID,THIS_SESSION_ID," +
				"THIS_SEQUENCE,THIS_USER,THIS_SESSION_DATE,THIS_SESSION_TIME,CART_KEY,WEB_ORDER," +
				"CUSTOMER_NBR,SHIPTO_NBR,ORDER_STATUS,IS_PRIVATE,CART_DESCRIPTION,EMAIL_ADDRESS,LOCATION," +
				"PUNCHOUT_POST_URL,PUNCHOUT_BUYER_COOKIE,PUNCHOUT_NET_ID,PUNCHOUT_ADDR_ID) " +
				" VALUES (?,?,?,?,?,?,?,?,?,?,?,?,'N',?,?,?,?,?,?,?)";

			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, "createVEBSESSRecord(email) SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setInt(2, Integer.parseInt(websiteId));
			pstmt.setString(3, this.theSession);			
			pstmt.setInt(4, sequence);
			pstmt.setString(5, theUser);
			pstmt.setDate(6, mydate);
			pstmt.setTime(7, mytime);
			pstmt.setInt(8, order.getCartKey());
			pstmt.setInt(9, order.getOrderNumber());
			pstmt.setString(10, order.getCustomer_nbr());
			pstmt.setString(11, order.getShipto_nbr());
			pstmt.setString(12, orderStatus);
			pstmt.setString(13, orderMessage);
			pstmt.setString(14, email);
			pstmt.setString(15, order.getLocation());
			pstmt.setString(16, punchoutUrl);
			pstmt.setString(17, punchoutCookie);
			pstmt.setString(18, punchoutAddId);
			pstmt.setString(19, punchoutNetId);
			pstmt.executeUpdate();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}
			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
	}
	
	private void insertItem(Item item, int cartId, String customerNumber,
			String shipToNumber, int unitOfMeasureIndex, String reprice, String location)
	{
		insertItem(item, cartId, customerNumber, shipToNumber, unitOfMeasureIndex, reprice, 0, "", location, "N");
	}
	
	private void insertItem(Item item, int cartId, String customerNumber, String shipToNumber, int unitOfMeasureIndex, String reprice,
			int promoNumber, String itemType, String location, String overridePrice)
	{
		// Log the entrance into the method
		String methodName = "insertItem";
		log.entering(cName, methodName);
				
		if (log.isLoggable(Level.FINEST))
			log.logp(Level.FINEST, cName, methodName, "qty: "+item.getQtyInCart());
				
		
		if (item.getQtyInCart() > 0)
		{
			Connection conn = null;
			PreparedStatement pstmt = null;
			String sql = "", sqlToExecute = "";
			boolean doInsert = true;
			
			Calendar calendar = new GregorianCalendar();
			Date date = calendar.getTime();
			java.sql.Date mydate = new java.sql.Date(date.getTime());
			java.sql.Time mytime = new java.sql.Time(date.getTime());
			
			Vector<UnitOfMeasure> uomList = item.getUomList();
			UnitOfMeasure unitOfMeasure = (UnitOfMeasure)uomList.elementAt(unitOfMeasureIndex);
			
			// Only get the taxable flag is I am using S2K pricing integration
			String taxable = "N";
			if (reprice.equalsIgnoreCase("Y")) 
				taxable = getTaxFlag(item.getItemNumber(), customerNumber, shipToNumber);

			try
			{
				Hashtable<String, String> env = new Hashtable<String, String>();
				env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
				env.put(Context.PROVIDER_URL, providerURL);
				Context context = new InitialContext(env);
				DataSource ds = ((DataSource)context.lookup(jndiName));
				conn = ds.getConnection();
	

				//Customized items and quantity one items will be added to cart as individual line items
				if (!item.isQtyOneItem() && !item.isCustomizable())
				{
					// Check if the item/uom/promo combination already exists in the cart.
					sql = "select ORDER_QTY from vebcart where COMPANY_NBR="+company+" and CART_KEY="+cartId+" and ITEM_NUMBER='"+item.getItemNumber()+
						"' and UNIT_OF_MEASURE='"+unitOfMeasure.getUnitOfMeasure()+"' and PROMO_NUMBER="+promoNumber+" and PARENT_LINE_NBR="+item.getParentLineNumber();
					sqlToExecute = "select ORDER_QTY from vebcart where COMPANY_NBR=? and CART_KEY=? and ITEM_NUMBER=? and UNIT_OF_MEASURE=? and PROMO_NUMBER=? and PARENT_LINE_NBR=?";
					
					if (log.isLoggable(Level.FINE))
						log.logp(Level.FINE, cName, methodName, "insertItem SQL: "+sql);
					
					pstmt = conn.prepareStatement(sqlToExecute, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
					pstmt.setInt(1, Integer.parseInt(company));
					pstmt.setInt(2, cartId);
					pstmt.setString(3, item.getItemNumber());
					pstmt.setString(4, unitOfMeasure.getUnitOfMeasure());
					pstmt.setInt(5, promoNumber);
					pstmt.setInt(6,item.getParentLineNumber());
			
					ResultSet qty_rs = pstmt.executeQuery();
					if (qty_rs.next())
					{
						if (log.isLoggable(Level.FINEST))
							log.logp(Level.FINEST, cName, methodName, "promoNumber : "+promoNumber);
						
						if (promoNumber <= 0)
						{
							int newQty=0;
							// If there is no promo number, combine the two quantities together
							int originalQty = qty_rs.getInt("ORDER_QTY");
							
							newQty = originalQty + item.getQtyInCart();
							
							if (log.isLoggable(Level.FINEST))
								log.logp(Level.FINEST, cName, methodName, "updating ORDER_QTY to: "+newQty);
							
							qty_rs.updateInt("ORDER_QTY", newQty);
							qty_rs.updateRow();
						}
						else
						{
							if (log.isLoggable(Level.FINEST))
								log.logp(Level.FINEST, cName, methodName, "updating ORDER_QTY to: "+item.getQtyInCart());
							
							// If there is a promo number, the new quantity overwrites the old quantity
							qty_rs.updateInt("ORDER_QTY", item.getQtyInCart());
							qty_rs.updateRow();
						}
							
						// Since we combined the items, do not insert this item into a new VEBCART row
						doInsert = false;
					}
		
					// Reset the Prepared Statement to get ready for the insert statement below
					pstmt.close();
					pstmt = null;
				}
				
				if (log.isLoggable(Level.FINEST))
					log.logp(Level.FINEST, cName, methodName, "doInsert: "+doInsert);
				
				// Only insert the item if we did not combine it with an already existing item
				if (doInsert)
				{
					sql = "insert into VEBCART (LINE_STATUS,COMPANY_NBR,CART_KEY,LINE_NBR,ORDER_QTY,ITEM_NUMBER,DESCRIPTION_ONE,DESCRIPTION_TWO," +
						"UNIT_OF_MEASURE,TAXABLE,ITEM_PRICE,ITEM_DISCOUNT,ADDED_BY,DATE_ADDED,TIME_ADDED,UPDATED_BY,DATE_UPDATED,TIME_UPDATED," +
						"PROMO_NUMBER,PROMO_ITEM_TYPE,LOCATION,PGM_PRICE,OVERRIDE_PRICE,PARENT_LINE_NBR,COMMENT,STYLE_ITEM_NBR) values ('I',"+company+","+cartId+","+item.getItemLineNumber()+
						","+item.getQtyInCart()+",'"+item.getItemNumber()+"','"+item.getDescription1().replace('\'', ' ')+"','"+item.getDescription2().replace('\'', ' ')+
						"','"+unitOfMeasure.getUnitOfMeasure()+"','"+taxable+"',"+unitOfMeasure.getUnitMearurePrice()+","+unitOfMeasure.getDiscount()+",'"+
						theUser+"','"+mydate+"','"+mytime+"','"+theUser+"','"+mydate+"','"+mytime+"',"+promoNumber+",'"+itemType+"','"+location+"',"+
						unitOfMeasure.getUnitMearurePrice()+",'"+overridePrice+"',0,'"+item.getLineItemComments()+"','"+item.getStyleItemNumber()+"')";
					sqlToExecute = "insert into VEBCART (LINE_STATUS,COMPANY_NBR,CART_KEY,LINE_NBR,ORDER_QTY,ITEM_NUMBER,DESCRIPTION_ONE,DESCRIPTION_TWO," +
						"UNIT_OF_MEASURE,TAXABLE,ITEM_PRICE,ITEM_DISCOUNT,ADDED_BY,DATE_ADDED,TIME_ADDED,UPDATED_BY,DATE_UPDATED,TIME_UPDATED," +
						"PROMO_NUMBER,PROMO_ITEM_TYPE,LOCATION,PGM_PRICE,OVERRIDE_PRICE,PARENT_LINE_NBR,COMMENT,STYLE_ITEM_NBR) values ('I',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					
					if (log.isLoggable(Level.FINE))
						log.logp(Level.FINE, cName, methodName, "insertItem SQL: "+sql);
					
					pstmt = conn.prepareStatement(sqlToExecute);
					pstmt.setInt(1, Integer.parseInt(company));
					pstmt.setInt(2, cartId);
					pstmt.setInt(3, item.getItemLineNumber());
					pstmt.setInt(4, item.getQtyInCart());
					pstmt.setString(5, item.getItemNumber());
					pstmt.setString(6, item.getDescription1());
					pstmt.setString(7, item.getDescription2());
					pstmt.setString(8, unitOfMeasure.getUnitOfMeasure());
					pstmt.setString(9, taxable);
					pstmt.setDouble(10, Double.parseDouble(unitOfMeasure.getUnitMearurePrice()));
					pstmt.setDouble(11, Double.parseDouble(unitOfMeasure.getDiscount()));
					pstmt.setString(12, theUser);
					pstmt.setDate(13, mydate);
					pstmt.setTime(14, mytime);
					pstmt.setString(15, theUser);
					pstmt.setDate(16, mydate);
					pstmt.setTime(17, mytime);
					pstmt.setInt(18, promoNumber);
					pstmt.setString(19, itemType);
					pstmt.setString(20, location);
					pstmt.setDouble(21, Double.parseDouble(unitOfMeasure.getUnitMearurePrice()));
					pstmt.setString(22, overridePrice);
					pstmt.setInt(23,item.getParentLineNumber());
					pstmt.setString(24, item.getLineItemComments());
					pstmt.setString(25, item.getStyleItemNumber());
					pstmt.executeUpdate();
					
					if (item.getItemPersonalization().size()>0){
						pstmt.close();
						pstmt = null;
						
						sqlToExecute = "INSERT INTO VEBCRTPR (COMPANY_NBR, CART_KEY, LINE_NBR,TEMPLT_NAME,PRSNLZN_ID,PRSNLZN_VAL,"+
								"DFT_CHG_YN,TEMPLT_CLS,ADDED_BY,DATE_ADDED,TIME_ADDED,UPDATED_BY,DATE_UPDATED,TIME_UPDATED) "+
								"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
						
						for (ItemPersonalization ip:item.getItemPersonalization())
						{
							sql = "INSERT INTO VEBCRTPR (COMPANY_NBR, CART_KEY, LINE_NBR,TEMPLT_NAME,PRSNLZN_ID,PRSNLZN_VAL,"+
								  "DFT_CHG_YN,TEMPLT_CLS,ADDED_BY,DATE_ADDED,TIME_ADDED,UPDATED_BY,DATE_UPDATED,TIME_UPDATED) "+
								  "VALUES ("+company+","+cartId+","+item.getItemLineNumber()+",'"+ip.getTemplateName()+"',"+
								  ip.getPersonalizationId()+",'"+ip.getPersonalizationValue()+"','"+ip.getDefaultWasChanged()+"',"+
								  "'"+ip.getTemplateClass()+"','"+theUser+"','"+mydate+"','"+mytime+"','"+theUser+"','"+mydate+"','"+mytime+"')";	

							if (log.isLoggable(Level.FINE))
								log.logp(Level.FINE, cName, methodName, "insertItemPersonalization SQL: "+sql);
							
							int c=1;
							pstmt = conn.prepareStatement(sqlToExecute);
							pstmt.setInt(c++, Integer.parseInt(company));
							pstmt.setInt(c++, cartId);
							pstmt.setInt(c++, item.getItemLineNumber());
							pstmt.setString(c++, ip.getTemplateName());
							pstmt.setInt(c++, ip.getPersonalizationId());
							pstmt.setString(c++, ip.getPersonalizationValue());
							pstmt.setString(c++, ip.getDefaultWasChanged());
							pstmt.setString(c++, ip.getTemplateClass());
							pstmt.setString(c++, theUser);
							pstmt.setDate(c++, mydate);
							pstmt.setTime(c++, mytime);
							pstmt.setString(c++, theUser);
							pstmt.setDate(c++, mydate);
							pstmt.setTime(c++, mytime);							
							pstmt.executeUpdate();
						}
					}
					
					if (item.getItemSizing().size()>0) {
						pstmt.close();
						pstmt = null;	
						
						sqlToExecute = "INSERT INTO VEBCRTSZ (COMPANY_NBR,CART_KEY,LINE_NBR,SIZE_ID,SIZE_VAL,"+
								"ADDED_BY,DATE_ADDED,TIME_ADDED,UPDATED_BY,DATE_UPDATED,TIME_UPDATED) "+
								"VALUES (?,?,?,?,?,?,?,?,?,?,?)";
						
						for (ItemSizing is:item.getItemSizing()) 
						{
							sql = "INSERT INTO VEBCRTSZ (COMPANY_NBR, CART_KEY, LINE_NBR,SIZE_ID,SIZE_VAL,"+
									"ADDED_BY,DATE_ADDED,TIME_ADDED,UPDATED_BY,DATE_UPDATED,TIME_UPDATED) "+
									"VALUES ("+company+","+cartId+","+item.getItemLineNumber()+","+is.getSizeId()+",'"+is.getSizeValue()+"',"+
									"'"+theUser+"','"+mydate+"','"+mytime+"','"+theUser+"','"+mydate+"','"+mytime+"')";	
							
							if (log.isLoggable(Level.FINE))
								log.logp(Level.FINE, cName, methodName, "insertItemSizing SQL: "+sql);
							
							int c=1;
							pstmt = conn.prepareStatement(sqlToExecute);
							pstmt.setInt(c++, Integer.parseInt(company));
							pstmt.setInt(c++, cartId);
							pstmt.setInt(c++, item.getItemLineNumber());		
							pstmt.setInt(c++, is.getSizeId());
							pstmt.setString(c++, is.getSizeValue());
							pstmt.setString(c++, theUser);
							pstmt.setDate(c++, mydate);
							pstmt.setTime(c++, mytime);
							pstmt.setString(c++, theUser);
							pstmt.setDate(c++, mydate);
							pstmt.setTime(c++, mytime);							
							pstmt.executeUpdate();							
						}
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				/*
				 * close any jdbc instances here that weren't
				 * explicitly closed during normal code path, so
				 * that we don't 'leak' resources...
				 */
				if (pstmt != null)
				{
					try
					{
						pstmt.close();
					}
					catch (SQLException sqlex)
					{
						// ignore -- as we can't do anything about it here
					}
	
					pstmt = null;
				}
	
				if (conn != null)
				{
					try
					{
						conn.close();
					}
					catch (SQLException sqlex)
					{
						// ignore -- as we can't do anything about it here
					}
	
					conn = null;
				}
			}
			
			//insertAssociatedItem(item.getItemNumber(), item.getQtyInCart(), cartId, customerNumber, shipToNumber, unitOfMeasure.getUnitOfMeasure(), reprice, location);
		}
		// Log the exit from the method
		log.exiting(cName, methodName);
	}
	
	private String getTaxFlag(String itemNumber, String customerNumber, String shipToNumber)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";

		String taxable = "Y";
		// Pad the company if I need to
		String pCompany = company;
		if (pCompany.length() == 1)
			pCompany = "0" + pCompany;

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = "call CORTTXQ ('"+pCompany+"','"+customerNumber+"','"+shipToNumber+"','"+itemNumber+"')";
			sqlToExecute = "call CORTTXQ (?,?,?,?)";
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setString(1, pCompany);
			pstmt.setString(2, customerNumber);
			pstmt.setString(3, shipToNumber);
			pstmt.setString(4, itemNumber);
			
			ResultSet taxable_rs = pstmt.executeQuery();
			if (taxable_rs.next())
			{
				taxable = taxable_rs.getString(1);
			}
		}
		catch (Exception e)
		{
			System.err.println(sql);
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}

		return taxable;
	}
	
	//private void insertAssociatedItem(String itemNumber,int qty, int cartId, String customerNumber,
	//		String shipToNumber, String unitOfMeasure, String reprice, String location, int lineNumber)
	//{
	//	Vector<Item> vAssociatedItems = getAssociatedItems(company, customerNumber, shipToNumber, reprice, this.theSession, lineNumber, itemNumber, qty, unitOfMeasure, location); 
	//	
	//	for (Item aItem : vAssociatedItems)
	//		insertItem(aItem, cartId, customerNumber, shipToNumber, reprice, location);
	//}
	
	public void insertItem(Item item, int cartId, String customerNumber, String shipToNumber, String reprice, String location)
	{
		insertItem(item, cartId, customerNumber, shipToNumber, reprice, location, "N");
	}
	
	private void insertItem(Item item, int cartId, String customerNumber, String shipToNumber, String reprice, String location, String overridePrice)
	{
		// Log the entrance into the method
		String methodName = "insertItem";
		log.entering(cName, methodName);
		
		if (log.isLoggable(Level.FINEST))
			log.logp(Level.FINEST, cName, methodName, "qty: "+item.getQtyInCart());
		
		if (item.getQtyInCart() > 0)
		{
			Connection conn = null;
			PreparedStatement pstmt = null;

			String sql = "", sqlToExecute = "";
			boolean doInsert = true;
		
			Calendar calendar = new GregorianCalendar();
			Date date = calendar.getTime();
			java.sql.Date mydate = new java.sql.Date(date.getTime());
			java.sql.Time mytime = new java.sql.Time(date.getTime());
			
			// Only get the taxable flag is I am using S2K pricing integration
			String taxable = "N";
			if (reprice.equalsIgnoreCase("Y"))
				taxable = getTaxFlag(item.getItemNumber(), customerNumber, shipToNumber);
	
			try
			{
				Hashtable<String, String> env = new Hashtable<String, String>();
				env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
				env.put(Context.PROVIDER_URL, providerURL);
				Context context = new InitialContext(env);
				DataSource ds = (DataSource)context.lookup(jndiName);
				conn = ds.getConnection();
				
				//Customized items will be added to cart as individual line items
				if (!item.isQtyOneItem() && !item.isCustomizable())
				{				
					sql = "select ORDER_QTY from vebcart where COMPANY_NBR="+company+" AND CART_KEY="+cartId+" and ITEM_NUMBER='"+item.getItemNumber()+
						"' and UNIT_OF_MEASURE='"+item.getUnitMeasure()+"' and PARENT_LINE_NBR="+item.getParentLineNumber();
					sqlToExecute = "select ORDER_QTY from vebcart where COMPANY_NBR=? AND CART_KEY=? and ITEM_NUMBER=? and UNIT_OF_MEASURE=? and PARENT_LINE_NBR=?";
					
					if (log.isLoggable(Level.FINE))
						log.logp(Level.FINE, cName, methodName, "insertItem SQL: "+sql);
					
					pstmt = conn.prepareStatement(sqlToExecute, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
					pstmt.setInt(1, Integer.parseInt(company));
					pstmt.setInt(2, cartId);
					pstmt.setString(3, item.getItemNumber());
					pstmt.setString(4, item.getUnitMeasure());
					pstmt.setInt(5,item.getParentLineNumber());
					
					ResultSet qty_rs = pstmt.executeQuery();
					if (qty_rs.next())
					{
						if (log.isLoggable(Level.FINEST))
							log.logp(Level.FINEST, cName, methodName, "item.getCartPromoNumber(): "+item.getCartPromoNumber());
						
						if (item.getCartPromoNumber() <= 0)
						{
							int newQty=0;
							// If there is no promo number, combine the two quantities together
							int originalQty = qty_rs.getInt("ORDER_QTY");
							
							newQty = originalQty + item.getQtyInCart();
							
							if (log.isLoggable(Level.FINEST))
								log.logp(Level.FINEST, cName, methodName, "updating ORDER_QTY to: "+newQty);
							
							qty_rs.updateInt("ORDER_QTY", newQty);
							qty_rs.updateRow();
						}
						else
						{
							// If there is a promo number, the new quantity overwrites the old quantity
							if (log.isLoggable(Level.FINEST))
								log.logp(Level.FINEST, cName, methodName, "updating ORDER_QTY to: "+item.getQtyInCart());
							qty_rs.updateInt("ORDER_QTY", item.getQtyInCart());
							qty_rs.updateRow();
						}
						
						// Since we combined the items, do not insert this item into a new VEBCART row
						doInsert = false;
					}
		
					// Reset the Prepared Statement to get ready for the insert statement below
					pstmt.close();
					pstmt = null;
				}
				
				if (log.isLoggable(Level.FINEST))
					log.logp(Level.FINEST, cName, methodName, "doInsert: "+doInsert);
				
				// Only insert the item if we did not combine it with an already existing item
				if (doInsert)
				{
					sql = "insert into VEBCART (LINE_STATUS,COMPANY_NBR,CART_KEY,LINE_NBR,ORDER_QTY,ITEM_NUMBER,DESCRIPTION_ONE,DESCRIPTION_TWO,"+
						"UNIT_OF_MEASURE,TAXABLE,ITEM_PRICE,ITEM_DISCOUNT,ADDED_BY,DATE_ADDED,TIME_ADDED,UPDATED_BY,DATE_UPDATED,TIME_UPDATED,"+
						"PROMO_NUMBER,PROMO_ITEM_TYPE,LOCATION,PGM_PRICE,OVERRIDE_PRICE,PARENT_LINE_NBR,COMMENT,STYLE_ITEM_NBR) values ('I',"+company+","+cartId+
						","+item.getItemLineNumber()+","+item.getQtyInCart()+",'"+item.getItemNumber()+"','"+item.getDescription1().replace('\'', ' ')+"','"+
						item.getDescription2().replace('\'', ' ')+"','" + item.getUnitMeasure()+"','"+taxable+"',"+item.getPrice()+","+
						item.getDiscount()+",'"+theUser+"','"+mydate+"','"+mytime+"','"+theUser+"','"+mydate+"','"+mytime+"',"+item.getCartPromoNumber()+
						",'"+item.getCartPromoItemType()+"','"+location+"',"+item.getPrice()+",'"+overridePrice+"',"+item.getParentLineNumber()+",'"+
						item.getLineItemComments()+"','"+item.getStyleItemNumber()+"')";
					sqlToExecute = "insert into VEBCART (LINE_STATUS,COMPANY_NBR,CART_KEY,LINE_NBR,ORDER_QTY,ITEM_NUMBER,DESCRIPTION_ONE," +
						"DESCRIPTION_TWO,UNIT_OF_MEASURE,TAXABLE,ITEM_PRICE,ITEM_DISCOUNT,ADDED_BY,DATE_ADDED,TIME_ADDED,UPDATED_BY,DATE_UPDATED," +
						"TIME_UPDATED,PROMO_NUMBER,PROMO_ITEM_TYPE,LOCATION,PGM_PRICE,OVERRIDE_PRICE,PARENT_LINE_NBR,COMMENT,STYLE_ITEM_NBR) values ('I',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					
					if (log.isLoggable(Level.FINE))
						log.logp(Level.FINE, cName, methodName, "insertItem SQL: "+sql);
					
					pstmt = conn.prepareStatement(sqlToExecute);
					pstmt.setInt(1, Integer.parseInt(company));
					pstmt.setInt(2, cartId);
					pstmt.setInt(3, item.getItemLineNumber());
					pstmt.setInt(4, item.getQtyInCart());
					pstmt.setString(5, item.getItemNumber());
					pstmt.setString(6, item.getDescription1());
					pstmt.setString(7, item.getDescription2());
					pstmt.setString(8, item.getUnitMeasure());
					pstmt.setString(9, taxable);
					pstmt.setDouble(10, Double.parseDouble(item.getPrice()));
					pstmt.setDouble(11, Double.parseDouble(item.getDiscount()));
					pstmt.setString(12, theUser);
					pstmt.setDate(13, mydate);
					pstmt.setTime(14, mytime);
					pstmt.setString(15, theUser);
					pstmt.setDate(16, mydate);
					pstmt.setTime(17, mytime);
					pstmt.setInt(18, item.getCartPromoNumber());
					pstmt.setString(19, item.getCartPromoItemType());
					pstmt.setString(20, location);
					pstmt.setDouble(21, Double.parseDouble(item.getPrice()));
					pstmt.setString(22, overridePrice);
					pstmt.setInt(23, item.getParentLineNumber());
					pstmt.setString(24, item.getLineItemComments());
					pstmt.setString(25, item.getStyleItemNumber());
					pstmt.executeUpdate();	
					
					if (item.getItemPersonalization().size()>0){
						pstmt.close();
						pstmt = null;
						
						sqlToExecute = "INSERT INTO VEBCRTPR (COMPANY_NBR, CART_KEY, LINE_NBR,TEMPLT_NAME,PRSNLZN_ID,PRSNLZN_VAL,"+
								"DFT_CHG_YN,TEMPLT_CLS,ADDED_BY,DATE_ADDED,TIME_ADDED,UPDATED_BY,DATE_UPDATED,TIME_UPDATED) "+
								"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
						
						for (ItemPersonalization ip:item.getItemPersonalization())
						{
							sql = "INSERT INTO VEBCRTPR (COMPANY_NBR, CART_KEY, LINE_NBR,TEMPLT_NAME,PRSNLZN_ID,PRSNLZN_VAL,"+
								  "DFT_CHG_YN,TEMPLT_CLS,ADDED_BY,DATE_ADDED,TIME_ADDED,UPDATED_BY,DATE_UPDATED,TIME_UPDATED) "+
								  "VALUES ("+company+","+cartId+","+item.getItemLineNumber()+",'"+ip.getTemplateName()+"',"+
								  ip.getPersonalizationId()+",'"+ip.getPersonalizationValue()+"','"+ip.getDefaultWasChanged()+"',"+
								  "'"+ip.getTemplateClass()+"','"+theUser+"','"+mydate+"','"+mytime+"','"+theUser+"','"+mydate+"','"+mytime+"')";	
							
							if (log.isLoggable(Level.FINE))
								log.logp(Level.FINE, cName, methodName, "insertItemPersonalization SQL: "+sql);
							
							int c=1;
							pstmt = conn.prepareStatement(sqlToExecute);
							pstmt.setInt(c++, Integer.parseInt(company));
							pstmt.setInt(c++, cartId);
							pstmt.setInt(c++, item.getItemLineNumber());
							pstmt.setString(c++, ip.getTemplateName());
							pstmt.setInt(c++, ip.getPersonalizationId());
							pstmt.setString(c++, ip.getPersonalizationValue());
							pstmt.setString(c++, ip.getDefaultWasChanged());
							pstmt.setString(c++, ip.getTemplateClass());
							pstmt.setString(c++, theUser);
							pstmt.setDate(c++, mydate);
							pstmt.setTime(c++, mytime);
							pstmt.setString(c++, theUser);
							pstmt.setDate(c++, mydate);
							pstmt.setTime(c++, mytime);							
							pstmt.executeUpdate();
						}
					}
					
					if (item.getItemSizing().size()>0) {
						pstmt.close();
						pstmt = null;	
						
						sqlToExecute = "INSERT INTO VEBCRTSZ (COMPANY_NBR,CART_KEY,LINE_NBR,SIZE_ID,SIZE_VAL,"+
								"ADDED_BY,DATE_ADDED,TIME_ADDED,UPDATED_BY,DATE_UPDATED,TIME_UPDATED) "+
								"VALUES (?,?,?,?,?,?,?,?,?,?,?)";
						
						for (ItemSizing is:item.getItemSizing()) 
						{
							sql = "INSERT INTO VEBCRTSZ (COMPANY_NBR, CART_KEY, LINE_NBR,SIZE_ID,SIZE_VAL,"+
									"ADDED_BY,DATE_ADDED,TIME_ADDED,UPDATED_BY,DATE_UPDATED,TIME_UPDATED) "+
									"VALUES ("+company+","+cartId+","+item.getItemLineNumber()+","+is.getSizeId()+",'"+is.getSizeValue()+"',"+
									"'"+theUser+"','"+mydate+"','"+mytime+"','"+theUser+"','"+mydate+"','"+mytime+"')";	
							
							if (log.isLoggable(Level.FINE))
								log.logp(Level.FINE, cName, methodName, "insertItemSizing SQL: "+sql);
							
							int c=1;
							pstmt = conn.prepareStatement(sqlToExecute);
							pstmt.setInt(c++, Integer.parseInt(company));
							pstmt.setInt(c++, cartId);
							pstmt.setInt(c++, item.getItemLineNumber());		
							pstmt.setInt(c++, is.getSizeId());
							pstmt.setString(c++, is.getSizeValue());
							pstmt.setString(c++, theUser);
							pstmt.setDate(c++, mydate);
							pstmt.setTime(c++, mytime);
							pstmt.setString(c++, theUser);
							pstmt.setDate(c++, mydate);
							pstmt.setTime(c++, mytime);							
							pstmt.executeUpdate();							
						}
					}					
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				/*
				 * close any jdbc instances here that weren't
				 * explicitly closed during normal code path, so
				 * that we don't 'leak' resources...
				 */
				if (pstmt != null)
				{
					try
					{
						pstmt.close();
					}
					catch (SQLException sqlex)
					{
						// ignore -- as we can't do anything about it here
					}
	
					pstmt = null;
				}
				if (conn != null)
				{
					try
					{
						conn.close();
					}
					catch (SQLException sqlex)
					{
						// ignore -- as we can't do anything about it here
					}
	
					conn = null;
				}
			}
			
			//insertAssociatedItem(item.getItemNumber(), item.getQtyInCart(), cartId, customerNumber, shipToNumber, item.getUnitMeasure(), reprice, location, item.getItemLineNumber());
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
	}
	
	private CustomerStatus getCustomerStatus(String acctNumber, String shipToNumber, int cartId, double orderTotal, String currentLanguageCode)
	{
		// Log the entrance into the method
		String methodName = "getCustomerStatus";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		CustomerStatus custStatus = new CustomerStatus();		
		
		String DATE_FORMAT = "yyyyMMdd";
		java.text.SimpleDateFormat sdf =  new java.text.SimpleDateFormat(DATE_FORMAT);
		Date d1 = new Date();
		String orderDate = sdf.format(d1);
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();
			
			sql = "call EBCKWCQ ("+company+",'"+acctNumber+"','"+shipToNumber+"',"+cartId+","+orderTotal+",'"+orderDate+"','"+currentLanguageCode+"')";
			sqlToExecute = "call EBCKWCQ (?,?,?,?,?,?,?)";
			
			log.logp(Level.FINE, cName, methodName, methodName + " SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			int c = 1;
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setString(c++, acctNumber);
			pstmt.setString(c++, shipToNumber);
			pstmt.setInt(c++, cartId);
			pstmt.setDouble(c++, orderTotal);
			pstmt.setString(c++, orderDate);
			pstmt.setString(c++, currentLanguageCode);
			
			ResultSet rs = pstmt.executeQuery();
			if (rs.next())
			{
				custStatus.setAction(rs.getString("rs_Action").trim());
				custStatus.setHoldCode(rs.getString("rs_HoldCode").trim());
				custStatus.setMessage(rs.getString("rs_Message").trim());
				custStatus.setContact(rs.getString("rs_Contact").trim());
				custStatus.setPhone(rs.getString("rs_Phone").trim());
				custStatus.setEmail(rs.getString("rs_Email").trim());
				
				log.logp(Level.FINE, cName, methodName, methodName + " Action:"+custStatus.getAction());
				log.logp(Level.FINE, cName, methodName, methodName + " Hold Code:"+custStatus.getHoldCode());
				log.logp(Level.FINE, cName, methodName, methodName + " Message:"+custStatus.getMessage());
				log.logp(Level.FINE, cName, methodName, methodName + " Contact:"+custStatus.getContact());
				log.logp(Level.FINE, cName, methodName, methodName + " Phone:"+custStatus.getPhone());
				log.logp(Level.FINE, cName, methodName, methodName + " Email:"+custStatus.getEmail());
			}
		}
		catch (Exception e)
		{
			custStatus = new CustomerStatus();
			e.printStackTrace();
		}
		finally
		{
			if (pstmt != null){
				try{pstmt.close();}
				catch (SQLException sqlex){}
				pstmt = null;
			}

			if (conn != null){
				try{conn.close();}
				catch (SQLException sqlex){}
				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
		
		return custStatus;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Vector<Item> getAssociatedItems(String company, String customerNumber, String shipToNumber, String reprice, String sessionID, int lineNumber, String itemNumber, int qty, String uom, String location)
	{
		// Log the entrance into the method
		String methodName = "getAssociatedItems";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		Vector<Item> itemList = new Vector<Item>();	
			
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();		
			
			sql = "CALL COMITEMQ("+company+",'"+itemNumber+"','"+uom+"',"+qty+")";
			sqlToExecute = "CALL COMITEMQ(?,?,?,?)";
			
			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, "getAssociatedItems SQL: "+sql);
			
			int i = 1;
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(i++, Integer.parseInt(company));
			pstmt.setString(i++, itemNumber);
			pstmt.setString(i++, uom);
			pstmt.setInt(i++, qty);	
			
			ResultSet associatedItems_rs = pstmt.executeQuery();
			while (associatedItems_rs.next())
			{			
				Item item = new Item();
				item.setCompany(associatedItems_rs.getString("COMPANY".trim()));
				item.setItemNumber(associatedItems_rs.getString("ITEM").trim());
				item.setUnitMeasure(associatedItems_rs.getString("UNIT").trim());
				item.setItemQuantity(associatedItems_rs.getInt("QUANTITY"));
				item.setParentLineNumber(lineNumber);
				
				if (log.isLoggable(Level.FINEST))
				{
					log.logp(Level.FINEST, cName, methodName, "company: "+item.getCompany());
					log.logp(Level.FINEST, cName, methodName, "item number: "+item.getItemNumber());
					log.logp(Level.FINEST, cName, methodName, "uom: "+item.getUnitMeasure());
					log.logp(Level.FINEST, cName, methodName, "qty: "+item.getItemQuantity());
				}
					
				Vector[] associatedItemsIn = initializeTheVectorArray(2);
				associatedItemsIn[0].addElement(item.getItemNumber().trim());
				associatedItemsIn[1].addElement(item.getItemQuantity());
				
				Vector<Item> aItems = getItemInfo(associatedItemsIn,
						location,
						customerNumber,
						shipToNumber,
						reprice, sessionID,false,false);
				
				aItems.elementAt(0).setParentLineNumber(lineNumber);
				aItems.elementAt(0).setQtyInCart(item.getItemQuantity());
				aItems.elementAt(0).setUnitMeasure(item.getUnitMeasure());
				aItems.elementAt(0).setCompany(item.getCompany());				
				// Add to the vector
				itemList.addElement(aItems.elementAt(0));		
			}
		}
		catch (Exception e)
		{
			itemList = new Vector<Item>();
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't explicitly closed during normal code path, so that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}
			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
		
		return itemList;
	}
	
	@SuppressWarnings("rawtypes")
	private Vector[] initializeTheVectorArray(int xSize)
	{
		Vector[] vectorToInitialize = new Vector[xSize];
		for (int x = 0; x < xSize; x++)
			vectorToInitialize[x] = new Vector();

		return vectorToInitialize;
	}
	
	private Vector<Item> getItemInfo(Vector[] itemsIn, String location, String accountNumber,
			String shipToNumber, String s2kPricing, String sessionID, boolean checkActive, boolean checkStyleItem)
	{
		// Log the entrance into the method
		String methodName = "getItemInfo(Vector[])";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		Vector<Item> itemsOut = new Vector<Item>();
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();
			
			sqlToExecute = "select VCOITEM.ONCITM,VEBITEM.ITEM_NBR,VEBITEM.DESCRIPTION_ONE," +
				"VEBITEM.DESCRIPTION_TWO,VEBITEM.IS_STOCKING,VEBEXTI.WEB_DESCRIPTION_1," +
				"VEBEXTI.WEB_DESCRIPTION_2,VEBEXTI.IS_ACTIVE,VEBPRIC.UNIT_OF_MEASURE,VEBPRIC.UOM_DESCRIPTION " +
				" from VEBEXTI,VEBPRIC,VEBITEM " +
				" join VEBITMB on VEBITMB.COMPANY_NBR=? and VEBITMB.ITEM_NBR=VEBITEM.ITEM_NBR and VEBITMB.LOCATION=? "+
				" left outer join VCOITEM on VCOITEM.ONCMP=? and VCOITEM.ONCUST=? and VCOITEM.ONITEM=VEBITEM.ITEM_NBR"+
				" where (VEBITEM.STATUS_FLAG='A' or VEBITEM.STATUS_FLAG='I') and VEBITEM.COMPANY_NBR=? " +
				" and VEBPRIC.COMPANY_NBR=? and VEBITEM.ITEM_NBR=VEBPRIC.ITEM_NBR " +
				" and VEBPRIC.LOCATION=? ";
				
			sqlToExecute += " and VEBEXTI.COMPANY_NBR=? and VEBEXTI.CATALOG_ID=? " +
				" and VEBEXTI.ITEM_NBR=VEBITEM.ITEM_NBR " +
				" and (upper(VCOITEM.ONCITM)=? or upper(VEBITEM.ITEM_NBR)=?) ";
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setString(2, location);
			pstmt.setInt(3, Integer.parseInt(company));
			pstmt.setString(4, accountNumber);
			pstmt.setInt(5, Integer.parseInt(company));
			pstmt.setInt(6, Integer.parseInt(company));
			pstmt.setString(7, location);
			pstmt.setInt(8, Integer.parseInt(company));
			pstmt.setInt(9, Integer.parseInt(websiteId));
			
			for (int x = 0; x < itemsIn[0].size(); x++)
			{
				boolean haveItem = false;
				boolean isStyleSKU = false;
				Item item = new Item();
				
				//Get the itemNumber and quantity
				String itemNumber = itemsIn[0].elementAt(x).toString();
				String quantity = itemsIn[1].elementAt(x).toString();

				sql = "select VCOITEM.ONCITM,VEBITEM.ITEM_NBR,VEBITEM.DESCRIPTION_ONE," +
					"VEBITEM.DESCRIPTION_TWO,VEBITEM.IS_STOCKING,VEBEXTI.WEB_DESCRIPTION_1," +
					"VEBEXTI.WEB_DESCRIPTION_2,VEBEXTI.IS_ACTIVE,VEBPRIC.UNIT_OF_MEASURE,VEBPRIC.UOM_DESCRIPTION " +
					" from VEBEXTI,VEBPRIC,VEBITEM " +
					" join VEBITMB on VEBITMB.COMPANY_NBR="+company+" and VEBITMB.ITEM_NBR=VEBITEM.ITEM_NBR and VEBITMB.LOCATION="+location+
					" left outer join VCOITEM on VCOITEM.ONCMP="+company+" and VCOITEM.ONCUST='"+accountNumber+"' and VCOITEM.ONITEM=VEBITEM.ITEM_NBR"+
					" where (VEBITEM.STATUS_FLAG='A' or VEBITEM.STATUS_FLAG='I') and VEBITEM.COMPANY_NBR="+company+
					" and VEBPRIC.COMPANY_NBR="+company+
					" and VEBITEM.ITEM_NBR=VEBPRIC.ITEM_NBR and VEBPRIC.LOCATION="+location;
				
					sql += " and VEBEXTI.COMPANY_NBR="+company+
					" and VEBEXTI.CATALOG_ID="+websiteId+" and VEBEXTI.ITEM_NBR=VEBITEM.ITEM_NBR "+
					" and (upper(VCOITEM.ONCITM)='"+itemNumber.toUpperCase()+
					"' or upper(VEBITEM.ITEM_NBR)='"+itemNumber.toUpperCase()+"')";
				
				if (log.isLoggable(Level.FINE))
					log.logp(Level.FINE, cName, methodName, methodName+" SQL: "+sql);
				
				pstmt.setString(10, itemNumber.toUpperCase());
				pstmt.setString(11, itemNumber.toUpperCase());
				ResultSet items_rs = pstmt.executeQuery();
				
				/*
				 * Cycle through all the item sin the resultset now
				 * I will append the unit of measure to a vector for each pass through and then set all the item object values 
				 * in the item object before I add the item object to the itemOut vector.  This will produce a vector of
				 * item objects that contains the pertinant item info and a vector of all the available unit of measure
				 */
				Vector<UnitOfMeasure> unitOfMeasure = new Vector<UnitOfMeasure>();
				while (items_rs.next())
				{
					haveItem = true;
					String isActive = items_rs.getString("IS_ACTIVE").trim();
					
					if (checkActive && isActive.equals("N")){
						haveItem = false;
						
						if (checkStyleItem)
							isStyleSKU = isStyleSKUItem(itemNumber);
					}
					
					if (haveItem || isStyleSKU) {
						if (items_rs.getString("ONCITM") != null)
							item.setCustomerItem(items_rs.getString("ONCITM").trim());
						item.setItemNumber(items_rs.getString("ITEM_NBR").trim());
					}
				}
				
				if (haveItem || isStyleSKU)
				{
					// Now I will just add the item details
					
					if (isStyleSKU)
						getItemDetail(item, location, false);
					else	
						getItemDetail(item, location, checkActive);
					
					// Make sure that the quantity follows minimum and multiple rules
					HashMap<String, String> minMult = item.getMinimumMultiple();
					int itemQty = Integer.parseInt(quantity);
					int roundedMinQty = 0;
					if (minMult.get("Minimum") != null)
						roundedMinQty = Math.round(Float.parseFloat(minMult.get("Minimum")));
					
					if (log.isLoggable(Level.FINEST))
					{
						log.logp(Level.FINEST, cName, methodName, "Quick Order item: "+itemNumber);
						log.logp(Level.FINEST, cName, methodName, "itemQty: "+itemQty);
						log.logp(Level.FINEST, cName, methodName, "roundedMinQty: "+roundedMinQty);
						log.logp(Level.FINEST, cName, methodName, "multiple? *"+minMult.get("Multiple")+"*");
					}
					
					if(itemQty < roundedMinQty)
					{
						String qtyMsg = "Item "+itemNumber+" quantity has been changed from "+quantity+" to "+roundedMinQty;
						item.setQoQtyMessage(qtyMsg);
						
						quantity = String.valueOf(roundedMinQty);
					}
					else if(minMult.get("Multiple") != null && minMult.get("Multiple").equals("Y"))
					{
						if (log.isLoggable(Level.FINEST))
							log.logp(Level.FINEST, cName, methodName, "itemQty % roundedMinQty: "+itemQty % roundedMinQty);
						if(itemQty % roundedMinQty > 0)
						{
							int tmp = (int)(Math.floor(itemQty / roundedMinQty) * roundedMinQty) + roundedMinQty;
							String qtyMsg = "Item "+itemNumber+" quantity has been changed from "+itemQty+" to "+String.valueOf(tmp);
							item.setQoQtyMessage(qtyMsg);
							quantity = String.valueOf(tmp);
						}
					}
					if (log.isLoggable(Level.FINEST))
						log.logp(Level.FINEST, cName, methodName, "Finalized item qty: "+quantity);
					item.setQuantityForQuickOrder(quantity);
					
					// Price the item here now, I need to know if it is restricted or inactive so I can treat it like an invalid item
					if (s2kPricing.equals("Y"))
						getWebPrice(item, accountNumber, shipToNumber, sessionID, location);
					else
						getWebPriceVEBPRIC(item, accountNumber, location);

					// Redmine 3293: We need to pass the item status of R to the page, so we can show the item as restricted, 
					// and also process whether the user can override it or not
					//if (item.getStatus().equals("R") || item.getStatus().equals("I"))
					if (item.getStatus().equals("I"))
					{
						unitOfMeasure.add(new UnitOfMeasure());
						item.setItemNumber(itemNumber);
						item.setDescription1("ITEM NOT FOUND");
						item.setDescription2("");
						item.setQuantityForQuickOrder("0");
					}
				}
				else
				{
					// Set the item number in the item object and the description to NOT FOUND
					unitOfMeasure.add(new UnitOfMeasure());
					item.setItemNumber(itemNumber);
					item.setDescription1("ITEM NOT FOUND");
					item.setDescription2("");
					item.setQuantityForQuickOrder("0");
				}
				
				itemsOut.add(item);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
		
		return itemsOut;
	}
	
	private void getItemDetail(Item itemDetail, String location, boolean checkActive)
	{
		// Log the entrance into the method
		String methodName = "getItemDetail(Item, location, checkActive)";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		Vector<UnitOfMeasure> uomList = new Vector<UnitOfMeasure>();
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = "select DESCRIPTION_ONE,DESCRIPTION_TWO,DESCRIPTION_THREE,IS_STOCKING,REPLACEMENT_ITEM," +
				"WEB_DESCRIPTION_1,WEB_DESCRIPTION_2,VEBEXTI.BIG_IMAGE_REF,VEBEXTI.SMALL_IMAGE_REF," +
				"VEBEXTI.BIG_ALT_TEXT,VEBEXTI.SMALL_ALT_TEXT,VEBEXTI.EXTENDED_COMMENTS,VEBEXTI.META_KEYWORDS," +
				"VEBEXTI.META_DESCRIPTION,VEBEXTI.CONTENT_KEY,VEBEXTI.PAGE_TITLE,VEBEXTI.SHORT_TEXT,VEBPRIC.OUR_PRICE," +
				"VEBPRIC.UNIT_OF_MEASURE,VEBITEM.BASE_UOM,VEBPRIC.UOM_DESCRIPTION,VEBPRIC.UOM_MULTIPLIER,VEBITEM.MFG_STOCK_NO,VEBITEM.MFG_CODE,VINMFGC.IRNAME,RELATED_CATEGORY1," +
				"RELATED_CATEGORY2,RELATED_CATEGORY3,HAZARD,HAZARD_SURCHARGE,HEAVY,OVERSIZED,REFRIGERATED,UNAVAILABLE,ANON_PRICE,VEBCATI.CATEGORY_ID,VEBITEM.STATUS_FLAG, VEBITMB.STATUS_FLAG as STATUS_FLAG_B " +
				" from VEBEXTI,VEBPRIC,VEBITEM " +
				" join VEBITMB on VEBITMB.COMPANY_NBR="+company+" and VEBITMB.ITEM_NBR='"+itemDetail.getItemNumber()+"' and VEBITMB.LOCATION='"+location+"'"+
				" left outer join VEBCATI on VEBCATI.COMPANY_NBR="+company+" and VEBCATI.CATALOG_ID="+websiteId+" and VEBCATI.ITEM_NBR='"+itemDetail.getItemNumber()+"' and VEBCATI.PRIMARY_CATEGORY='Y' "+
				" left outer join VEBCATG on VEBCATG.COMPANY_NBR="+company+" and VEBCATG.CATALOG_ID="+websiteId+" and VEBCATG.IS_ACTIVE='Y' and VEBCATG.CATEGORY_ID=VEBCATI.CATEGORY_ID "+
				" left outer join VINMFGC on VINMFGC.IRCMP="+company+" and VINMFGC.IRCODE=VEBITEM.MFG_CODE "+
				"where (VEBITEM.STATUS_FLAG='A' or VEBITEM.STATUS_FLAG='I') and VEBITEM.COMPANY_NBR="+company+
				" and VEBITEM.ITEM_NBR='"+itemDetail.getItemNumber()+"' and VEBEXTI.COMPANY_NBR="+
				company+" and VEBEXTI.CATALOG_ID="+websiteId;
				
			if (checkActive)
				sql += " and VEBEXTI.IS_ACTIVE='Y' ";
				
				sql += " AND VEBITEM.ITEM_NBR=VEBEXTI.ITEM_NBR and VEBPRIC.COMPANY_NBR="+company+
				" AND VEBPRIC.ITEM_NBR=VEBITEM.ITEM_NBR and VEBPRIC.LOCATION="+location+" order by VEBPRIC.UOM_MULTIPLIER asc";
			
			sqlToExecute = "select DESCRIPTION_ONE,DESCRIPTION_TWO,DESCRIPTION_THREE,IS_STOCKING," +
				"REPLACEMENT_ITEM,WEB_DESCRIPTION_1,WEB_DESCRIPTION_2,VEBEXTI.BIG_IMAGE_REF," +
				"VEBEXTI.SMALL_IMAGE_REF,VEBEXTI.BIG_ALT_TEXT,VEBEXTI.SMALL_ALT_TEXT,VEBEXTI.EXTENDED_COMMENTS," +
				"VEBEXTI.META_KEYWORDS,VEBEXTI.META_DESCRIPTION,VEBEXTI.CONTENT_KEY,VEBEXTI.PAGE_TITLE,VEBEXTI.SHORT_TEXT," +
				"VEBPRIC.OUR_PRICE,VEBPRIC.UNIT_OF_MEASURE,VEBITEM.BASE_UOM,VEBPRIC.UOM_DESCRIPTION,VEBPRIC.UOM_MULTIPLIER,VEBITEM.MFG_STOCK_NO,VEBITEM.MFG_CODE,VINMFGC.IRNAME,RELATED_CATEGORY1," +
				"RELATED_CATEGORY2,RELATED_CATEGORY3,HAZARD,HAZARD_SURCHARGE,HEAVY,OVERSIZED,REFRIGERATED,UNAVAILABLE,ANON_PRICE,VEBCATI.CATEGORY_ID,VEBITEM.STATUS_FLAG, VEBITMB.STATUS_FLAG as STATUS_FLAG_B " +
				" from VEBEXTI,VEBPRIC,VEBITEM " +
				" join VEBITMB on VEBITMB.COMPANY_NBR=? and VEBITMB.ITEM_NBR=? and VEBITMB.LOCATION=? "+
				" left outer join VEBCATI on VEBCATI.COMPANY_NBR=? and VEBCATI.CATALOG_ID=? and VEBCATI.ITEM_NBR=? and VEBCATI.PRIMARY_CATEGORY='Y' "+
				" left outer join VEBCATG on VEBCATG.COMPANY_NBR=? and VEBCATG.CATALOG_ID=? and VEBCATG.IS_ACTIVE='Y' and VEBCATG.CATEGORY_ID=VEBCATI.CATEGORY_ID "+
				" left outer join VINMFGC on VINMFGC.IRCMP=? and VINMFGC.IRCODE=VEBITEM.MFG_CODE "+
				" where (VEBITEM.STATUS_FLAG='A' or VEBITEM.STATUS_FLAG='I') and VEBITEM.COMPANY_NBR=? and VEBITEM.ITEM_NBR=? " +
				" and VEBEXTI.COMPANY_NBR=? and VEBEXTI.CATALOG_ID=?";
			
			if (checkActive)
				sqlToExecute += " and VEBEXTI.IS_ACTIVE='Y' ";
				
				sqlToExecute += " and VEBITEM.ITEM_NBR=VEBEXTI.ITEM_NBR and VEBPRIC.COMPANY_NBR=? " +
					" and VEBPRIC.ITEM_NBR=VEBITEM.ITEM_NBR and VEBPRIC.LOCATION=? order by VEBPRIC.UOM_MULTIPLIER asc";
			
			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, methodName+" SQL: "+sql);

			int c = 1;
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setString(c++, itemDetail.getItemNumber());
			pstmt.setString(c++, location);
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setString(c++, itemDetail.getItemNumber());
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setString(c++, itemDetail.getItemNumber());
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setString(c++, location);
			
			ResultSet items_rs = pstmt.executeQuery();
			boolean setDefaultUoM = true;
			while (items_rs.next())
			{
				UnitOfMeasure unitOfMeasure = new UnitOfMeasure();
				if (items_rs.getString("WEB_DESCRIPTION_1") == null || items_rs.getString("WEB_DESCRIPTION_1").trim().equalsIgnoreCase(""))
				{
					itemDetail.setDescription1(items_rs.getString("DESCRIPTION_ONE").trim());
					itemDetail.setDescription2(items_rs.getString("DESCRIPTION_TWO").trim());
				}
				else
				{
					itemDetail.setDescription1(items_rs.getString("WEB_DESCRIPTION_1").trim());
					itemDetail.setDescription2(items_rs.getString("WEB_DESCRIPTION_2").trim());
				}

				itemDetail.setReplacementItem(items_rs.getString("REPLACEMENT_ITEM").trim());
				if (itemDetail.getReplacementItem().length() > 0)
					itemDetail.setReplacement(true);
				else
					itemDetail.setReplacement(false);
				
				if (items_rs.getString("STATUS_FLAG").trim().equalsIgnoreCase("I"))
					itemDetail.setIsActive("I");
				else if (items_rs.getString("STATUS_FLAG_B").trim().equalsIgnoreCase("I"))
					itemDetail.setIsActive("I");
				else
					itemDetail.setIsActive(items_rs.getString("STATUS_FLAG").trim());
				
				itemDetail.setContentKey(items_rs.getString("CONTENT_KEY").trim());
				itemDetail.setPageTitle(items_rs.getString("PAGE_TITLE").trim());
				itemDetail.setMetaKeywords(items_rs.getString("META_KEYWORDS").trim());
				itemDetail.setMetaDescription(items_rs.getString("META_DESCRIPTION").trim());
				itemDetail.setManufacturerItem(items_rs.getString("DESCRIPTION_THREE").trim());
				itemDetail.setStockItem(items_rs.getString("IS_STOCKING").trim());
				String fullImageName = items_rs.getString("BIG_IMAGE_REF").trim();
				String fullImageExt = items_rs.getString("BIG_IMAGE_REF").trim();
				String thumbImageName = items_rs.getString("SMALL_IMAGE_REF").trim();	
				String thumbImageExt = items_rs.getString("SMALL_IMAGE_REF").trim();
				
				fullImageName = fullImageName.substring(0, fullImageName.lastIndexOf('.')+1);
				fullImageExt = fullImageExt.substring(fullImageExt.lastIndexOf('.')+1);
				
				thumbImageName = thumbImageName.substring(0, thumbImageName.lastIndexOf('.')+1);
				thumbImageExt = thumbImageExt.substring(thumbImageExt.lastIndexOf('.')+1);
				
				itemDetail.setFullImage(fullImageName.concat(fullImageExt.toLowerCase()));
				itemDetail.setFullImageAltText(items_rs.getString("BIG_ALT_TEXT").trim());
				itemDetail.setThumbImage(thumbImageName.concat(thumbImageExt.toLowerCase()));
				itemDetail.setThumbImageAltText(items_rs.getString("SMALL_ALT_TEXT").trim());
				
				if (itemDetail.getExtDescription() == null || itemDetail.getExtDescription().equalsIgnoreCase(""))
				{
					Clob extDescription = items_rs.getClob("EXTENDED_COMMENTS");
					itemDetail.setExtDescription(extDescription.getSubString(1, (int)extDescription.length()));
				}
				
				//Available qty is set in the uomList in the pricing routine
				//itemDetail.setAvailaleQuantity(items_rs.getInt("ONHAND_QTY"));
				
				unitOfMeasure.setUnitMearurePrice(items_rs.getString("OUR_PRICE").trim());
				unitOfMeasure.setUnitOfMeasure(items_rs.getString("UNIT_OF_MEASURE").trim());
				unitOfMeasure.setMultiplier(items_rs.getDouble("UOM_MULTIPLIER"));
				
				if (setDefaultUoM) {
					itemDetail.setUnitMeasure(items_rs.getString("UNIT_OF_MEASURE").trim()); //This is set because it is used in the field name for adds
					itemDetail.setDefaultUOM(items_rs.getString("UNIT_OF_MEASURE").trim());
					//itemDetail.setCartUOM(items_rs.getString("UNIT_OF_MEASURE"));
					setDefaultUoM = false;
				}
				
				unitOfMeasure.setUnitOfMeasureDescription(items_rs.getString("UNIT_OF_MEASURE").trim()+" "+items_rs.getString("UOM_DESCRIPTION").trim());
				itemDetail.setManufacturerItem(items_rs.getString("MFG_STOCK_NO").trim());
				if (items_rs.getString("IRNAME") != null)
					itemDetail.setManufacturerName(items_rs.getString("IRNAME").trim());
				else
					itemDetail.setManufacturerName("");
				
				itemDetail.setPreferredVendor(items_rs.getString("MFG_CODE").trim());
				
				CategoryList cList = new CategoryList(company, websiteId, "", jndiName, providerURL);
				itemDetail.setRelatedCategory1(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY1")));
				itemDetail.setRelatedCategory2(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY2")));
				itemDetail.setRelatedCategory3(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY3")));
				
				//itemDetail.setProductComments(getProductComments(itemDetail.getItemNumber()));
				Clob shortDesc = items_rs.getClob("SHORT_TEXT");
				itemDetail.setProductComments(shortDesc.getSubString(1, (int)shortDesc.length()));
				
				itemDetail.setHazardFlag(items_rs.getString("HAZARD").trim());
				itemDetail.setHazardSurchargeFlag(items_rs.getString("HAZARD_SURCHARGE").trim());
				itemDetail.setHeavyFlag(items_rs.getString("HEAVY").trim());
				itemDetail.setOversizedFlag(items_rs.getString("OVERSIZED").trim());
				itemDetail.setRefrigeratedFlag(items_rs.getString("REFRIGERATED").trim());
				itemDetail.setUnavailableFlag(items_rs.getString("UNAVAILABLE").trim());
				itemDetail.setAnonPriceFlag(items_rs.getString("ANON_PRICE").trim());
				itemDetail.setItemCategory(String.valueOf(items_rs.getInt("CATEGORY_ID")));
				
				//Add element to UnitOfMeasure List
				uomList.addElement(unitOfMeasure);
			}
			
			//Put the uomList into itemDetail
			itemDetail.setUomList(uomList);
			
			// Get the external links for this item
			itemDetail.setLinks(getLinks(itemDetail.getItemNumber()));
			
			// Get the minimum and multiple quantities for this item
			HashMap<String, String> minMultiple = this.getMinimumMultipleForItem(itemDetail);
			itemDetail.setMinimumMultiple(minMultiple);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
	}
	
	private void repriceItems(AribaOrder order)
	{
		// Reprice the items
		repriceCartItems(order.getCustomer_nbr(), order.getLocation(), order.getCartKey());
		
		double materialCharge = getMaterial(order.getCartKey());
		double shippingCharge = getShipping(order.getCartKey());
		double handlingCharge = getHandling(order.getCustomer_nbr(), materialCharge);
		String promoCode = getPromoCode(order.getCartKey());
		Vector<String> discounts = getDiscounts(order.getCustomer_nbr(), order.getShipto_nbr(),
				order.getLocation(), promoCode, order.getCartKey());
		
		// Getting the tax information has 3 steps:
		// for punchout, we'll delete first instead of after
		deleteShippingRecord(order.getOrderNumber());
		// 1 - Write shipping info to VEBSHIP
		insertShipTo(order);
		// 2 - Call the tax calculation program
		afterReprice(order.getCustomer_nbr(), order.getShipto_nbr(), order.getLocation(),
				order.getCartKey(), order.getState(), materialCharge, shippingCharge,
				handlingCharge, discounts.get(0).toString(), discounts.get(1).toString(),
				discounts.get(2).toString(), discounts.get(3).toString(), order.getEmail());
		// 3 - Delete the VEBSHIP record we just wrote

		// Update the items in the cart
		updateCartItems(order.getCartKey());
	}
	
	private void repriceCartItems(String customerNumber, String location, int cartId)
	{
		// Log the entrance into the method
		String methodName = "repriceCartItems";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();
			
			sql = "call CODETLQ ("+company+","+websiteId+",'"+customerNumber+"','"+location+"',"+cartId+")";
			sqlToExecute = "call CODETLQ (?,?,?,?,?)";
			
			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, methodName+" SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setInt(2, Integer.parseInt(websiteId));
			pstmt.setString(3, customerNumber);
			pstmt.setString(4, location);
			pstmt.setInt(5, cartId);
			pstmt.executeQuery();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
	}
	
	private double getMaterial(int cartId)
	{
		// Log the entrance into the method
		String methodName = "getMaterial";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		BigDecimal materialCharge = new BigDecimal(0.00), subTotal = new BigDecimal(0.00);

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = "select ITEM_PRICE,ITEM_DISCOUNT,ORDER_QTY from vebcart where COMPANY_NBR=" + company + " AND CART_KEY=" + cartId;
			sqlToExecute = "select ITEM_PRICE,ITEM_DISCOUNT,ORDER_QTY from vebcart where COMPANY_NBR=? AND CART_KEY=?";
			
			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, methodName+" SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setInt(2, cartId);
			
			ResultSet items_rs = pstmt.executeQuery();
			while (items_rs.next()) {

				int orderQty = items_rs.getInt("ORDER_QTY");
				BigDecimal singleItemPrice = new BigDecimal(items_rs.getString("ITEM_PRICE").trim());
				BigDecimal theDiscount = new BigDecimal(items_rs.getString("ITEM_DISCOUNT").trim());
				
				if (log.isLoggable(Level.FINEST))
				{
					log.logp(Level.FINEST, cName, methodName, "orderQty: "+orderQty);
					log.logp(Level.FINEST, cName, methodName, "singleItemPrice: "+singleItemPrice.doubleValue());
					log.logp(Level.FINEST, cName, methodName, "theDiscount: "+theDiscount.doubleValue());
				}
				
				//Reading this probably will be a headache at first. Let me try to alleviate this by breaking it into steps.
				
				/**
				 * Step 1: Multiply the regular price of an item by its discount to get the 'money saved' per item for that item.
				 * 			Truncate after 4 decimal places.
				 * 
				 * 			For example, the item costs $8.49 and has a 9.5% discount. 8.49 * 0.095 = 0.80655.
				 * 				We then truncate this to 0.8065.
				 */
				BigDecimal discountAmount = singleItemPrice.multiply(theDiscount).setScale(4, RoundingMode.DOWN);
				if (log.isLoggable(Level.FINEST))
					log.logp(Level.FINEST, cName, methodName, "discountAmount: "+discountAmount.doubleValue());
				
				/**
				 * Step 2: Subtract the discount amount above from the regular price of an item to get the discount price of a single item.
				 * 			Truncate after 4 decimal places.
				 * 
				 * 			For example, 8.49 - 0.8065 = 7.6835
				 */
				BigDecimal discountedSingleItemPrice = singleItemPrice.subtract(discountAmount).setScale(4, RoundingMode.DOWN);
				if (log.isLoggable(Level.FINEST))
					log.logp(Level.FINEST, cName, methodName, "discountedSingleItemPrice: "+discountedSingleItemPrice.doubleValue());
				
				/**
				 * Step 3: Multiply the discounted price of the item by the quantity in the order.
				 */
				BigDecimal pretotal = discountedSingleItemPrice.multiply(new BigDecimal(orderQty));
				if (log.isLoggable(Level.FINEST))
					log.logp(Level.FINEST, cName, methodName, "pretotal: "+pretotal.doubleValue());
				
				/**
				 * Redmine 3499: This used to use decimalPrecisionPricing for the rounding scale, but it should always round to 2 decimal prices, so we get a proper price.
				 * Step 4: Round up to 2 decimal places
				 */
				BigDecimal lineTotal = pretotal.setScale(2, java.math.RoundingMode.HALF_UP);
				if (log.isLoggable(Level.FINEST))
					log.logp(Level.FINEST, cName, methodName, "lineTotal: "+lineTotal.doubleValue());
				
				materialCharge = materialCharge.add(lineTotal);
				if (log.isLoggable(Level.FINEST))
					log.logp(Level.FINEST, cName, methodName, "materialCharge: "+materialCharge.doubleValue());
			}
			
			subTotal = materialCharge;
			if (log.isLoggable(Level.FINEST))
				log.logp(Level.FINEST, cName, methodName, "subTotal: "+subTotal.doubleValue());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}

		if (log.isLoggable(Level.FINEST))
			log.logp(Level.FINEST, cName, methodName, "return value: "+subTotal.doubleValue());
		
		return subTotal.doubleValue();
	}
	
	private double getShipping(int cartID)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		double shippingCharge = 0.00;
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			// Get the discount from VARCUST
			sql = "select FREIGHT_CHARGE,FREIGHT_CHARGE2,FREIGHT_CHARGE3 from VEBSESS where VEBSESS.COMPANY_NBR="+company+" and VEBSESS.CATALOG_ID="+websiteId+" and VEBSESS.CART_KEY="+cartID;
			sqlToExecute = "select FREIGHT_CHARGE,FREIGHT_CHARGE2,FREIGHT_CHARGE3 from VEBSESS where " +
				"VEBSESS.COMPANY_NBR=? and VEBSESS.CATALOG_ID=? and VEBSESS.CART_KEY=?";
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setInt(2, Integer.parseInt(websiteId));
			pstmt.setInt(3, cartID);
			
			ResultSet rs = pstmt.executeQuery();
			if (rs.next())
			{
				shippingCharge += rs.getDouble("FREIGHT_CHARGE");
				shippingCharge += rs.getDouble("FREIGHT_CHARGE2");
				shippingCharge += rs.getDouble("FREIGHT_CHARGE3");
			}
		}
		catch (Exception e)
		{
			shippingCharge = 0.00;
			System.err.println(sql);
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}

		return shippingCharge;
	}
	
	private double getHandling(String customerNumber, double subTotal)
	{
		// Log the entrance into the method
		String methodName = "getHandling";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		double handlingCharge = 0.00;
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = (DataSource)context.lookup(jndiName);
			conn = ds.getConnection();

			// Call the handling charge stored procedure
			sql = "call COGTHCQ("+company+",'"+customerNumber+"',"+subTotal+")";
			sqlToExecute = "call COGTHCQ(?,?,?)";
			
			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, methodName+" SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setString(2, customerNumber);
			pstmt.setDouble(3, subTotal);
			
			ResultSet rs = pstmt.executeQuery();
			if (rs.next())
				handlingCharge = rs.getDouble(4);
		}
		catch (Exception e)
		{
			handlingCharge = 0.00;
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}
			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
		
		return handlingCharge;
	}
	
	private void deleteShippingRecord(int orderNumber)
	{
		// Log the entrance into the method
		String methodName = "deleteShippingRecord";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = "delete from VEBSHIP where CATALOG_ID="+websiteId+" and COMPANY_NBR="+company+" and ORDER_NBR="+orderNumber;
			sqlToExecute = "delete from VEBSHIP where CATALOG_ID=? and COMPANY_NBR=? and ORDER_NBR=?";

			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, methodName+" SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(websiteId));
			pstmt.setInt(2, Integer.parseInt(company));
			pstmt.setInt(3, orderNumber);
			pstmt.executeUpdate();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
	}
	
	public void insertShipTo(AribaOrder order)
	{
		// Log the entrance into the method
		String methodName = "insertShipTo";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";

		Calendar calendar = new GregorianCalendar();
		Date date = calendar.getTime();
		java.sql.Date mydate = new java.sql.Date(date.getTime());
		java.sql.Time mytime = new java.sql.Time(date.getTime());

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = "insert into VEBSHIP (CATALOG_ID,COMPANY_NBR,ORDER_NBR,SHIPTO_NAME,ADDRESS1,ADDRESS2,ADDRESS3,CITY,STATE,ZIP_CODE,COUNTRY," +
				"CREATED_BY,DATE_CREATED,TIME_CREATED,UPDATED_BY,DATE_UPDATED,TIME_UPDATED,SHIPTO_NBR) values (" +
				websiteId + "," + company + "," + order.getOrderNumber() + ",'" + order.getName() +
				"','" + order.getAddress1() + "','" + order.getAddress2() +
				"','" + order.getAddress3() + "','" + order.getCity() + "','" +
				order.getState() + "','" + order.getZip() + "','" +
				order.getCountry() + "','" + this.theUser + "','" + mydate + "','" + mytime +
				"','"+this.theUser+"','"+mydate+"','"+mytime+"','"+order.getShipto_nbr()+"')";
			sqlToExecute = "insert into VEBSHIP (CATALOG_ID,COMPANY_NBR,ORDER_NBR,SHIPTO_NAME," +
				"ADDRESS1,ADDRESS2,ADDRESS3,CITY,STATE,ZIP_CODE,COUNTRY,CREATED_BY,DATE_CREATED," +
				"TIME_CREATED,UPDATED_BY,DATE_UPDATED,TIME_UPDATED,SHIPTO_NBR) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, methodName+" SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(websiteId));
			pstmt.setInt(2, Integer.parseInt(company));
			pstmt.setInt(3, order.getOrderNumber());
			pstmt.setString(4, order.getName());
			pstmt.setString(5, order.getAddress1());
			pstmt.setString(6, order.getAddress2());
			pstmt.setString(7, order.getAddress3());
			pstmt.setString(8, order.getCity());
			pstmt.setString(9, order.getState());
			pstmt.setString(10, order.getZip());
			pstmt.setString(11, order.getCountry());
			pstmt.setString(12, this.theUser);
			pstmt.setDate(13, mydate);
			pstmt.setTime(14, mytime);
			pstmt.setString(15, this.theUser);
			pstmt.setDate(16, mydate);
			pstmt.setTime(17, mytime);
			pstmt.setString(18, order.getShipto_nbr());
			pstmt.executeUpdate();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
	}
	
	private Vector<String> getDiscounts(String accountNumber, String shipToNumber, String location, String promoCode, int cartID)
	{
		// Log the entrance into the method
		String methodName = "getDiscounts";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		Vector<String> discounts = new Vector<String>();

		Calendar calendar = new GregorianCalendar();
		Date date = calendar.getTime();
		java.sql.Date mydate = new java.sql.Date(date.getTime());
		
		try {
			Hashtable<String,String> env = new Hashtable<String,String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			boolean error = false;
			
			try {
				// Get the discount from VARCUST
				sql = "call EBDSCPQ ("+company+",'"+accountNumber+"','"+shipToNumber+"')";
				sqlToExecute = "call EBDSCPQ (?,?,?)";
				
				if (log.isLoggable(Level.FINE))
					log.logp(Level.FINE, cName, methodName, "getDiscounts discount SQL1: "+sql);	
				
				pstmt = conn.prepareStatement(sqlToExecute);
				pstmt.setInt(1, Integer.parseInt(company));
				pstmt.setString(2, accountNumber);
				pstmt.setString(3, shipToNumber);
				
				ResultSet discounts_rs = pstmt.executeQuery();
				
				if (discounts_rs.next())
				{
					String rtnError = discounts_rs.getString("rs_ErrFlg").trim();
					if (rtnError.equals(""))
						discounts.add(discounts_rs.getString("rs_DiscPct").trim());
					else {
						if (log.isLoggable(Level.FINE))
							log.logp(Level.FINE, cName, methodName, "There was an error returned from EBDSCPQ - "+rtnError);
						error = true;
					}
				}
				else
					discounts.add("0.000");
			}
			catch (Exception sqlEx)
			{
				if (log.isLoggable(Level.FINE))
					log.logp(Level.FINE, cName, methodName, "Failed to retrieve discount using procedure - EBDSCPQ");
				
				error = true;
			}
			finally
			{
				if (pstmt != null)  {
					try{pstmt.close();}
					catch (SQLException sqlex){}
					pstmt = null;
				}
			}
			
			if (error)
			{				
				// Get the discount from VARCUST
				sql = "select VARCUST.RMODSC from VARCUST where VARCUST.RMCMP="+company+" and VARCUST.RMCUST='"+accountNumber+"'";
				sqlToExecute = "select VARCUST.RMODSC from VARCUST where VARCUST.RMCMP=? and VARCUST.RMCUST=?";
				
				if (log.isLoggable(Level.FINE))
					log.logp(Level.FINE, cName, methodName, "getDiscounts discount SQL2: "+sql);
				
				pstmt = conn.prepareStatement(sqlToExecute);
				pstmt.setInt(1, Integer.parseInt(company));
				pstmt.setString(2, accountNumber);
				
				ResultSet discounts_rs = pstmt.executeQuery();
				if (discounts_rs.next()) {
					discounts.add(discounts_rs.getString("RMODSC").trim());
				} else {
					discounts.add("0.000");
				}
				
				// Reset the PreparedStatement object
				pstmt.close();
				pstmt = null;
			}
			
			//new stuff
			sql = "select DISC_STARTDATE,DISC_AMOUNT,DISC_DURATION " +
				"from VEBAPREF where COMPANY_NBR="+company+" and lower(USER_ID)='"+this.theUser.toLowerCase()+"' and DISC_STARTDATE<="+mydate;
			
			sqlToExecute = "select DISC_STARTDATE,DISC_AMOUNT,DISC_DURATION " +
				"from VEBAPREF where COMPANY_NBR=? and lower(USER_ID)=? and DISC_STARTDATE<=?";
			
			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, "getDiscounts discount SQL3: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			int q = 1;
			pstmt.setInt(q++, Integer.parseInt(company));
			pstmt.setString(q++, this.theUser.toLowerCase());
			pstmt.setDate(q++, mydate);
			
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				String startDateString = "";
				if (rs.getString("DISC_STARTDATE") != null && rs.getString("DISC_STARTDATE").trim().length() > 0) {
					startDateString = rs.getString("DISC_STARTDATE").trim();
					int duration = rs.getInt("DISC_DURATION");
					
					// Get the start date as a Calendar object
					DateFormat df = new SimpleDateFormat("MM/dd/yy");
					Date startDate = df.parse(startDateString);
					Calendar c = new GregorianCalendar();
					c.setTime(startDate);
					
					// Add the duration to the start date
					c.add(Calendar.DATE, duration);
					
					// Here we are comparing to the calendar we use for the SQL, which holds today's date
					// We want to make sure that the last day of the discount is AFTER today's date
					if (c.after(calendar)) {
						double discAmt = rs.getDouble("DISC_AMOUNT");
						//discAmt = discAmt / 100.0;
						
						// Format the discounts to the format that ARINTXQ is expecting
						DecimalFormat decFmt = new DecimalFormat("0.000");
						discounts.add(decFmt.format(discAmt));
					} else {
						discounts.add("0.000");
					}
				} else {
					discounts.add("0.000");
				}
			} else {
				discounts.add("0.000");
			}
			
			// The third discount slot is not currently used 
			discounts.add("0.000");
			
			// Get the discount associated with the coupon code
			discounts.add(String.valueOf(getCouponDiscountAmount(cartID)));
		}
		catch (Exception e)
		{
			// Create a Vector with 4 discounts of 0
			discounts = new Vector<String>();discounts.add("0.000");discounts.add("0.000");discounts.add("0.000");discounts.add("0.000");
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}

		// Log the exit from the method
		log.exiting(cName, methodName);
		
		return discounts;
	}
	
	private String getPromoCode(int cartID)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "", promoCode = "";

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = "select PROMO_CODE from VEBSESS where COMPANY_NBR="+company+" and CART_KEY="+cartID;
			sqlToExecute = "select PROMO_CODE from VEBSESS where COMPANY_NBR=? and CART_KEY=?";

			//System.out.println("getPromoCode SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setInt(2, cartID);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next())
				promoCode = rs.getString("PROMO_CODE").trim();
		}
		catch (Exception e)
		{
			promoCode = "";
			System.err.println(sql);
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		return promoCode;
	}
	
	private void updateCartItems(int number)
	{
		// Log the entrance into the method
		String methodName = "updateCartItems";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null, pstmt2 = null;
		String sql = "", sqlToExecute = "", sql2 = "", sqlToExecute2 = "";

		Calendar calendar = new GregorianCalendar();
		Date date = calendar.getTime();
		java.sql.Date mydate = new java.sql.Date(date.getTime());
		java.sql.Time mytime = new java.sql.Time(date.getTime());

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();
			
			//Get the items to update by company,cart id
			sql = "select OUSEQ,OUPRIC,OUITDC from VCORMDE where OUCMP="+company+" and OUORD="+number;
			sqlToExecute = "select OUSEQ,OUPRIC,OUITDC from VCORMDE where OUCMP=? and OUORD=?";

			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, "updateCartItems SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setInt(2, number);
			
			// Prepare the second SQL statement
			sqlToExecute2 = "select * from VEBCART where COMPANY_NBR=? and CART_KEY=? and LINE_NBR=? for update";
			pstmt2 = conn.prepareStatement(sqlToExecute2, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			pstmt2.setInt(1, Integer.parseInt(company));
			pstmt2.setInt(2, number);
			
			ResultSet itemsUpdate_rs = pstmt.executeQuery();
			//**********************************************************************************************************************************
			// Update each of the items in the list
			while (itemsUpdate_rs.next())
			{
				//System.out.println("itemsUpdate_rs row: " +itemsUpdate_rs.getRow());
				//System.out.println("itemsUpdate_rs cursor: " +itemsUpdate_rs.getCursorName());
				
				String price = itemsUpdate_rs.getString("OUPRIC").trim();
				String discount = itemsUpdate_rs.getString("OUITDC").trim();
				String lineNumber = itemsUpdate_rs.getString("OUSEQ").trim();

				sql2 = "select * from VEBCART where COMPANY_NBR="+company+" and CART_KEY="+number+" and LINE_NBR="+lineNumber+" for update";
				pstmt2.setInt(3, Integer.parseInt(lineNumber));
				
				if (log.isLoggable(Level.FINE))
					log.logp(Level.FINE, cName, methodName, "updateCartItems SQL 2: "+sql2);
				
				ResultSet lineItem_rs = pstmt2.executeQuery();
				if (lineItem_rs.next())
				{
					boolean priceisOverridden = lineItem_rs.getString("OVERRIDE_PRICE").trim().equals("Y");
					if (log.isLoggable(Level.FINEST))
						log.logp(Level.FINEST, cName, methodName, "priceisOverridden: "+priceisOverridden);
						
					lineItem_rs.updateString("UPDATED_BY", this.theUser);
					lineItem_rs.updateDate("DATE_UPDATED", mydate);
					lineItem_rs.updateTime("TIME_UPDATED", mytime);
					lineItem_rs.updateDouble("ITEM_DISCOUNT", Double.parseDouble(discount));
					if (!priceisOverridden)
					{
						lineItem_rs.updateDouble("ITEM_PRICE", Double.parseDouble(price));
						lineItem_rs.updateDouble("PGM_PRICE", Double.parseDouble(price));
					}
					
					if (log.isLoggable(Level.FINEST))
					{
						log.logp(Level.FINEST, cName, methodName, "update UPDATED_BY to: "+this.theUser);
						log.logp(Level.FINEST, cName, methodName, "update DATE_UPDATED to: "+mydate);
						log.logp(Level.FINEST, cName, methodName, "update TIME_UPDATED to: "+mytime);
						if (!priceisOverridden)
						{
							log.logp(Level.FINEST, cName, methodName, "update ITEM_PRICE to: "+price);
							log.logp(Level.FINEST, cName, methodName, "update PGM_PRICE to: "+price);
						}
						else
						{
							log.logp(Level.FINEST, cName, methodName, "ITEM_PRICE not updated due to priceisOverridden value");
							log.logp(Level.FINEST, cName, methodName, "PGM_PRICE not updated due to priceisOverridden value");
						}
						log.logp(Level.FINEST, cName, methodName, "update ITEM_DISCOUNT to: "+discount);
					}
					
					lineItem_rs.updateRow();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}
			if (pstmt2 != null)
			{
				try
				{
					pstmt2.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt2 = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
	}
	
	private double getCouponDiscountAmount(int cartID)
	{
		// Log the entrance into the method
		String methodName = "getCouponDiscountAmount";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		double discount = 0.00;

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = "select DISCOUNT_AMOUNT from VEBSESS where VEBSESS.COMPANY_NBR="+company+" and VEBSESS.CATALOG_ID="+websiteId+
				" and VEBSESS.CART_KEY="+cartID+" and VEBSESS.PROMO_CODE<>''";
			sqlToExecute = "select DISCOUNT_AMOUNT from VEBSESS where VEBSESS.COMPANY_NBR=? and VEBSESS.CATALOG_ID=? and VEBSESS.CART_KEY=? and VEBSESS.PROMO_CODE<>''";
			
			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, "getCouponDiscountAmount SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setInt(2, Integer.parseInt(websiteId));
			pstmt.setInt(3, cartID);
			
			ResultSet rs = pstmt.executeQuery();
			if (rs.next())
				discount = rs.getDouble("DISCOUNT_AMOUNT");
		}
		catch (Exception e)
		{
			discount = 0.00;
			
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}

		// Log the exit from the method
		log.exiting(cName, methodName);
		
		return discount;
	}
	
	private void afterReprice(String customerNumber, String shipToNumber, String location, int cartId, String state, double materialCharge, double freightCharge, double handlingCharge, String discount1, String discount2, String discount3, String discount4, String emailAddress)
	{
		// Log the entrance into the method
		String methodName = "afterReprice";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();
			
			sql = "call ARINTXQ ("+company+","+websiteId+",'"+customerNumber+"','"+shipToNumber+"',"+cartId+",'"+state+"',"+materialCharge+","+
					freightCharge+","+handlingCharge+","+discount1+","+discount2+","+discount3+",'"+location+"',"+discount4+")";
			sqlToExecute = "call ARINTXQ (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			
			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, methodName+" SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setInt(2, Integer.parseInt(websiteId));
			pstmt.setString(3, customerNumber);
			pstmt.setString(4, shipToNumber);
			pstmt.setInt(5, cartId);
			pstmt.setString(6, state);
			pstmt.setDouble(7, materialCharge);
			pstmt.setDouble(8, freightCharge);
			pstmt.setDouble(9, handlingCharge);
			pstmt.setDouble(10, Double.parseDouble(discount1));
			pstmt.setDouble(11, Double.parseDouble(discount2));
			pstmt.setDouble(12, Double.parseDouble(discount3));
			pstmt.setString(13, location);
			pstmt.setDouble(14, Double.parseDouble(discount4));
			
			ResultSet reprice_rs = pstmt.executeQuery();
			if (reprice_rs.next())
			{
				String taxAmount = reprice_rs.getString(1);
				String county = reprice_rs.getString(2);
				String local1 = reprice_rs.getString(3);
				String local2 = reprice_rs.getString(4);
				String stateTaxFlag = reprice_rs.getString(5);
				String countytaxFlag = reprice_rs.getString(6);
				String local1TaxFlag = reprice_rs.getString(7);
				String local2TaxFlag = reprice_rs.getString(8);

				// Close out the PreparedStatement
				pstmt.close();
				pstmt = null;
				
				// Calculate the Order Total here. I am only subtracting discount1 here 
				double orderTotal = (materialCharge + 
						Double.parseDouble(taxAmount) + freightCharge + 
						handlingCharge) - (Double.parseDouble(discount1) * materialCharge);
				
				String sOrderTotal = new NumberFormatting().format(String.valueOf(orderTotal), "CURRENCY");
				
				// update VEBSESS
				sql = "UPDATE VEBSESS SET TAX_AMOUNT="+taxAmount+",COUNTY="+county+",LOCAL1="+local1+
					",LOCAL2="+local2+",STATE_TAX_FLAG='"+stateTaxFlag+"',ORDER_TOTAL="+sOrderTotal+
					",COUNTY_TAX_FLAG='"+countytaxFlag+"',LOCAL1_TAX_FLAG='"+local1TaxFlag+
					"',LOCAL2_TAX_FLAG='"+local2TaxFlag+"',STATE='"+state+"',MATERIALTOTAL="+
					materialCharge+",HANDLING_CHARGE="+handlingCharge+
					",EMAIL_ADDRESS='"+emailAddress+"',DISCOUNT_PCT1="+discount1+
					",DISCOUNT_PCT2="+discount2+",DISCOUNT_PCT3="+discount3+
					" where VEBSESS.COMPANY_NBR="+company+
					" AND VEBSESS.CATALOG_ID="+websiteId+" AND VEBSESS.CART_KEY="+cartId;
				
				sqlToExecute = "UPDATE VEBSESS SET TAX_AMOUNT=?,COUNTY=?,LOCAL1=?,LOCAL2=?, " +
					"STATE_TAX_FLAG=?,ORDER_TOTAL=?,COUNTY_TAX_FLAG=?,LOCAL1_TAX_FLAG=?, " +
					"LOCAL2_TAX_FLAG=?,STATE=?,MATERIALTOTAL=?,HANDLING_CHARGE=?, " +
					"EMAIL_ADDRESS=?,DISCOUNT_PCT1=?,DISCOUNT_PCT2=?,DISCOUNT_PCT3=? " +
					"where VEBSESS.COMPANY_NBR=? AND VEBSESS.CATALOG_ID=? AND VEBSESS.CART_KEY=?";
				
				if (log.isLoggable(Level.FINE))
					log.logp(Level.FINE, cName, methodName, methodName+" SQL2: "+sql);
				
				pstmt = conn.prepareStatement(sqlToExecute);
				pstmt.setDouble(1, Double.parseDouble(taxAmount));
				pstmt.setInt(2, Integer.parseInt(county));
				pstmt.setInt(3, Integer.parseInt(local1));
				pstmt.setInt(4, Integer.parseInt(local2));
				pstmt.setString(5, stateTaxFlag);
				pstmt.setDouble(6, Double.parseDouble(sOrderTotal));
				pstmt.setString(7, countytaxFlag);
				pstmt.setString(8, local1TaxFlag);
				pstmt.setString(9, local2TaxFlag);
				pstmt.setString(10, state);
				pstmt.setDouble(11, materialCharge);
				pstmt.setDouble(12, handlingCharge);
				pstmt.setString(13, emailAddress);
				// discount in VEBSESS seems to be applied to order total to calculate material charge.
				// stored procedure COINOTSP is used to get tax, discount etc
				pstmt.setDouble(14, Double.parseDouble(discount1));
				pstmt.setDouble(15, Double.parseDouble(discount2));
				pstmt.setDouble(16, Double.parseDouble(discount3));
				pstmt.setInt(17, Integer.parseInt(company));
				pstmt.setInt(18, Integer.parseInt(websiteId));
				pstmt.setInt(19, cartId);
				pstmt.executeUpdate();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
	}
	
	public void updateVEBSESSRecordBeforeComplete(AribaOrder order, String paymentTerms, String requestedShipDate, int sequence)
	{
		// Log the entrance into the method
		String methodName = "updateVEBSESSRecordBeforeComplete";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";

		Calendar calendar = new GregorianCalendar();
		Date date = calendar.getTime();
		java.sql.Date mydate = new java.sql.Date(date.getTime());
		java.sql.Time mytime = new java.sql.Time(date.getTime());
		java.sql.Date shipDate = java.sql.Date.valueOf(requestedShipDate);
		
		/*
		 * ORDER_STATUS CODES:
		 * 	A - Approved
		 *  W - Waiting Approval
		 *  P - Pending Approval
		 *  I - Incomplete (this is the initial state)
		 *  C - Completed or set to C so I do not pick up for lost cart
		 *  S - Saved Cart
		 */
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			//First get "THIS" values from VEBSESS so I can move them into "LAST"
			sql = "SELECT * FROM VEBSESS WHERE COMPANY_NBR=" + company + " AND CATALOG_ID=" + websiteId + " AND CART_KEY=" + order.getCartKey();
			sqlToExecute = "SELECT * FROM VEBSESS WHERE COMPANY_NBR=? AND CATALOG_ID=? AND CART_KEY=?";
			
			pstmt = conn.prepareStatement(sqlToExecute, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setInt(2, Integer.parseInt(websiteId));
			pstmt.setInt(3, order.getCartKey());
			
			ResultSet vebsessRecord = pstmt.executeQuery();
			if (vebsessRecord.next())
			{
				// Move values into last here
				vebsessRecord.updateString("LAST_SESSION_ID", vebsessRecord.getString("THIS_SESSION_ID"));
				vebsessRecord.updateInt("LAST_SEQUENCE", vebsessRecord.getInt("THIS_SEQUENCE"));
				vebsessRecord.updateString("LAST_USER", vebsessRecord.getString("THIS_USER"));
				vebsessRecord.updateDate("LAST_SESSION_DATE", vebsessRecord.getDate("THIS_SESSION_DATE"));
				vebsessRecord.updateTime("LAST_SESSION_TIME", vebsessRecord.getTime("THIS_SESSION_TIME"));

				// Update THIS Values
				vebsessRecord.updateString("THIS_SESSION_ID", this.theSession);
				vebsessRecord.updateInt("THIS_SEQUENCE", sequence);
				vebsessRecord.updateString("THIS_USER", this.theUser);
				vebsessRecord.updateDate("THIS_SESSION_DATE", mydate);
				vebsessRecord.updateTime("THIS_SESSION_TIME", mytime);
				vebsessRecord.updateString("PO_NUMBER", order.getPoNumber());
				vebsessRecord.updateString("EMAIL_ADDRESS", order.getEmail());
				vebsessRecord.updateString("CART_DESCRIPTION", "BEFORE COMPLETION");
				vebsessRecord.updateString("ORDER_COMMENTS", "");
				vebsessRecord.updateString("PHONE_NUMBER", order.getPhoneNumber());
				vebsessRecord.updateString("FAX_NUMBER", order.getFaxNumber());
				vebsessRecord.updateString("PAYMENT_TERMS", paymentTerms);
				vebsessRecord.updateDate("REQUESTED_DATE", shipDate);
				
				if (log.isLoggable(Level.FINE))
				{
					log.logp(Level.FINE, cName, methodName, methodName+" customerNumber: "+order.getCustomer_nbr());
					log.logp(Level.FINE, cName, methodName, methodName+" shipToNumber: "+order.getShipto_nbr());
				}
				
				if (order.getShipto_nbr() != null && !order.getShipto_nbr().isEmpty())
					vebsessRecord.updateString("SHIPTO_NBR", order.getShipto_nbr());
				if (order.getCustomer_nbr() != null && !order.getCustomer_nbr().isEmpty())
					vebsessRecord.updateString("CUSTOMER_NBR", order.getCustomer_nbr());
				
				if (order.getState() != null && !order.getState().isEmpty())
					vebsessRecord.updateString("STATE", order.getState());
				
				// Send the updates to the database				
				vebsessRecord.updateRow();
			}
		}
		catch (Exception e)
		{
			System.err.println(sql);
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
	}
	
	public int getOrderNumberS2K()
	{
		// Log the entrance into the method
		String methodName = "getOrderNumberS2K";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		int orderNumber = 0;

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = "CALL CONXTOQ(" + company + ")";
			sqlToExecute = "CALL CONXTOQ(?)";
			
			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, "getOrderNumberS2K SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			pstmt.setInt(1, Integer.parseInt(company));
			
			ResultSet orderNumber_rs = pstmt.executeQuery();
			if (orderNumber_rs.next())
				orderNumber = orderNumber_rs.getInt(1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}
			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}

		// Log the exit from the method
		log.exiting(cName, methodName);
		
		return orderNumber;
	}
	
	public void insertS2KOrderNumber(int cartId, int s2kOrderNumber)
	{
		// Log the entrance into the method
		String methodName = "insertS2KOrderNumber";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = "update VEBSESS set S2K_ORDER="+s2kOrderNumber+" where COMPANY_NBR="+company+" and CATALOG_ID="+websiteId+" and CART_KEY="+cartId;
			sqlToExecute = "update VEBSESS set S2K_ORDER=? where COMPANY_NBR=? and CATALOG_ID=? and CART_KEY=? ";
			
			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, "insertS2KOrderNumber SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, s2kOrderNumber);
			pstmt.setInt(2, Integer.parseInt(company));
			pstmt.setInt(3, Integer.parseInt(websiteId));
			pstmt.setInt(4, cartId);
			pstmt.executeUpdate();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}
			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
	}
	
	public void updateShippingInfo(int cartID, ShippingOption shipVia)
	{
		String methodName = "updateShippingInfo";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		int c = 1;

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = "update VEBSESS set SHIP_VIA='"+shipVia.getShipViaCode()+"', FREIGHT_CHARGE="+shipVia.getAmount()+" where COMPANY_NBR="+company+" and CATALOG_ID="+websiteId+" and CART_KEY="+cartID;
			sqlToExecute = "update VEBSESS set SHIP_VIA=?, FREIGHT_CHARGE=? where COMPANY_NBR=? and CATALOG_ID=? and CART_KEY=?";
			
			log.logp(Level.FINER, cName, methodName, sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setString(c++, shipVia.getShipViaCode());
			pstmt.setDouble(c++, shipVia.getAmount());
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setInt(c++, cartID);
			pstmt.executeUpdate();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		log.exiting(cName, methodName);
	}
	
	public Vector<ShippingOption> getShippingOptions(String customerNumber, String shipToNumber, String locationNumber, String state, String country, int cartID, String shipVia, String reprice, String getAvail, String calcShipCharge, String userDefinedParam)
	{
		// Log the entrance into the method
		String methodName = "getShippingOptions";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		Vector<ShippingOption> options = new Vector<ShippingOption>();
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();
			
			sql = "CALL EBSVIAQ ("+company+","+websiteId+","+cartID+",'"+customerNumber+"','"+shipToNumber+"','"+locationNumber+"','"+state+"','','"+country+"','"+shipVia+"','"+reprice+"','"+getAvail+"','"+calcShipCharge+"','"+userDefinedParam+"')";
			sqlToExecute = "CALL EBSVIAQ(?,?,?,?,?,?,?,'',?,?,?,?,?,?)";
			
			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, "getShippingOptions SQL: "+sql);
			
			int c = 1;
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setInt(c++, cartID);
			pstmt.setString(c++, customerNumber);
			pstmt.setString(c++, shipToNumber);
			pstmt.setString(c++, locationNumber);
			pstmt.setString(c++, state);
			pstmt.setString(c++, country);
			pstmt.setString(c++, shipVia);
			pstmt.setString(c++, reprice);
			pstmt.setString(c++, getAvail);
			pstmt.setString(c++, calcShipCharge);
			pstmt.setString(c++, userDefinedParam);
			
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{
				ShippingOption o = new ShippingOption();
				
				if (rs.getString("rs_ErrorCode").trim().equals("N"))
				{
					o.setShippingOption("");
					o.setShipViaCode(rs.getString("rs_ShipVia").trim());
					o.setDescription(rs.getString("rs_ShipDesc").trim());
					o.setSelected((rs.getString("rs_ShipSelect").trim()).equals("Y"));
					o.setAmount(rs.getDouble("rs_ShipAmount"));
					o.setShipDate(rs.getString("rs_ShipDate"));
					options.addElement(o);
				}				
			}
		}
		catch (Exception e)
		{
			options = new Vector<ShippingOption>();
			e.printStackTrace();
		}
		finally
		{
			if (pstmt != null){
				try{pstmt.close();}
				catch (SQLException sqlex){}

				pstmt = null;
			}

			if (conn != null){
				try{conn.close();}
				catch (SQLException sqlex){}

				conn = null;
			}
		}

		// Log the exit from the method
		log.exiting(cName, methodName);
		
		return options;
	}
	
	public void completeOrder(int cartId, boolean isApprover, boolean isPunchoutOrder, int sequence)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";

		Calendar calendar = new GregorianCalendar();
		Date date = calendar.getTime();
		java.sql.Date mydate = new java.sql.Date(date.getTime());
		java.sql.Time mytime = new java.sql.Time(date.getTime());
		
		String orderStatus = "", orderMessage = "";		
		if (isPunchoutOrder)
		{
			orderStatus = "N";
			orderMessage = "PUNCHOUT PENDING";
		}
		else if (isApprover)
		{
			//This person does not require approval set ORDER_STATUS in VEBSESS to A
			orderStatus = STATUS_APPROVED;
			orderMessage = "APPROVED AND COMPLETED";
		}
		else
		{
			//This person requires approval
			orderStatus = STATUS_WAITING;
			orderMessage = "WAITING APPROVAL";
		}

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();
			
			sql = "select LINE_STATUS,UPDATED_BY,DATE_UPDATED,TIME_UPDATED from vebcart where COMPANY_NBR=" + company + " AND CART_KEY = " + cartId;
			sqlToExecute = "select LINE_STATUS,UPDATED_BY,DATE_UPDATED,TIME_UPDATED from vebcart where COMPANY_NBR=? AND CART_KEY=?";
			
			pstmt = conn.prepareStatement(sqlToExecute, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setInt(2, cartId);
			
			ResultSet qty_rs = pstmt.executeQuery();
			while (qty_rs.next())
			{
				qty_rs.updateString("LINE_STATUS", orderStatus);
				qty_rs.updateString("UPDATED_BY", this.theUser);
				qty_rs.updateDate("DATE_UPDATED", mydate);
				qty_rs.updateTime("TIME_UPDATED", mytime);
				qty_rs.updateRow();
			}
			
			//Update the VEBSESS Record here
			updateVEBSESSRecord(cartId, orderStatus, "N", orderMessage, sequence);
		}
		catch (Exception e)
		{
			System.err.println(sql);
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
	}
	
	private void updateVEBSESSRecord(int cartId, String orderStatus, String isPrivate, String cartDescription, int sequence)
	{
		// Log the entrance into the method
		String methodName = "updateVEBSESSRecord";
		log.entering(cName, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";

		Calendar calendar = new GregorianCalendar();
		Date date = calendar.getTime();
		java.sql.Date mydate = new java.sql.Date(date.getTime());
		java.sql.Time mytime = new java.sql.Time(date.getTime());
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			//First get "THIS" values from VEBSESS so I can move them into "LAST"
			sql = "SELECT * FROM VEBSESS WHERE COMPANY_NBR="+company+" AND CATALOG_ID="+websiteId+" AND CART_KEY="+cartId;
			sqlToExecute = "SELECT * FROM VEBSESS WHERE COMPANY_NBR=? AND CATALOG_ID=? AND CART_KEY=?";
			
			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, cName, methodName, "updateVEBSESSRecord SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setInt(2, Integer.parseInt(websiteId));
			pstmt.setInt(3, cartId);
			
			ResultSet vebsessRecord = pstmt.executeQuery();
			if (vebsessRecord.next())
			{
				if (log.isLoggable(Level.FINEST))
				{
					log.logp(Level.FINEST, cName, methodName, "updating cart ID: "+cartId);
					log.logp(Level.FINEST, cName, methodName, "updating LAST_SESSION_ID to: "+vebsessRecord.getString("THIS_SESSION_ID"));
					log.logp(Level.FINEST, cName, methodName, "updating LAST_SEQUENCE to: "+vebsessRecord.getString("THIS_SEQUENCE"));
					log.logp(Level.FINEST, cName, methodName, "updating LAST_USER to: "+vebsessRecord.getString("THIS_USER"));
					log.logp(Level.FINEST, cName, methodName, "updating LAST_SESSION_DATE to: "+vebsessRecord.getString("THIS_SESSION_DATE"));
					log.logp(Level.FINEST, cName, methodName, "updating LAST_SESSION_TIME to: "+vebsessRecord.getString("THIS_SESSION_TIME"));
					
					log.logp(Level.FINEST, cName, methodName, "updating THIS_SESSION_ID to: "+this.theSession);
					log.logp(Level.FINEST, cName, methodName, "updating THIS_SEQUENCE to: "+sequence);
					log.logp(Level.FINEST, cName, methodName, "updating THIS_USER to: "+this.theUser);
					log.logp(Level.FINEST, cName, methodName, "updating THIS_SESSION_DATE to: "+mydate);
					log.logp(Level.FINEST, cName, methodName, "updating THIS_SESSION_TIME to: "+mytime);
					log.logp(Level.FINEST, cName, methodName, "updating ORDER_STATUS to: "+orderStatus);
					log.logp(Level.FINEST, cName, methodName, "updating IS_PRIVATE to: "+isPrivate);
					log.logp(Level.FINEST, cName, methodName, "updating CART_DESCRIPTION to: "+cartDescription);
				}
				
				// Move values into last here
				vebsessRecord.updateString("LAST_SESSION_ID", vebsessRecord.getString("THIS_SESSION_ID"));
				vebsessRecord.updateInt("LAST_SEQUENCE", vebsessRecord.getInt("THIS_SEQUENCE"));
				vebsessRecord.updateString("LAST_USER", vebsessRecord.getString("THIS_USER"));
				vebsessRecord.updateDate("LAST_SESSION_DATE", vebsessRecord.getDate("THIS_SESSION_DATE"));
				vebsessRecord.updateTime("LAST_SESSION_TIME", vebsessRecord.getTime("THIS_SESSION_TIME"));

				// Update THIS Values
				vebsessRecord.updateString("THIS_SESSION_ID", this.theSession);
				vebsessRecord.updateInt("THIS_SEQUENCE", sequence);
				vebsessRecord.updateString("THIS_USER", this.theUser);
				vebsessRecord.updateDate("THIS_SESSION_DATE", mydate);
				vebsessRecord.updateTime("THIS_SESSION_TIME", mytime);
				vebsessRecord.updateString("ORDER_STATUS", orderStatus);
				vebsessRecord.updateString("IS_PRIVATE", isPrivate);
				vebsessRecord.updateString("CART_DESCRIPTION", cartDescription);
				vebsessRecord.updateRow();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				pstmt = null;
			}

			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}

				conn = null;
			}
		}
		
		// Log the exit from the method
		log.exiting(cName, methodName);
	}

	public Map<String, String> getUomTranslationMatrix(String networkId)
	{
		String cMethod = "getCustShipTo";
		log.entering(cName, cMethod);
		Map<String, String> uomTranslationMatrix = new HashMap<String, String>();
		Connection conn = null;
		PreparedStatement stmt = null;
		String sql = null;
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource) context.lookup(jndiName));
			conn = ds.getConnection();
			sql = "SELECT CXUM as UOM, CXCUM as DESC from CCXMLUMX where CXNETID=?";
			//NOTE: this table also has company - presumably as a key, but we do not use variable companies anywhere else. in theory, company should be loaded from VEBPUNXREF.
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, networkId);
			log.finer("Executing SQL: " + SqlUtil.magicPrintSQL(sql, networkId));
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				String uom = rs.getString("UOM").trim();
				String desc = rs.getString("DESC").trim();
				uomTranslationMatrix.put(desc, uom);
			}
		}
		catch (SQLException sqle)
		{
			System.out.println(sqle.getMessage());
		}
		catch (NamingException ne)
		{
			System.out.println(ne.getMessage());
		}
		finally
		{
			/*
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 */
			if (stmt != null)
			{
				try
				{
					stmt.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}
				stmt = null;
			}
			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException sqlex)
				{
					// ignore -- as we can't do anything about it here
				}
				conn = null;
			}
		}
		log.exiting(cName, cMethod);
		return uomTranslationMatrix;
	}
}