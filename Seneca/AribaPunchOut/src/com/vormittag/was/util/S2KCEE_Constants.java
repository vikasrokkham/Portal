package com.vormittag.was.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;

public class S2KCEE_Constants
{
	private String company = null;
	private String jndiName = null;
	private String providerURL = null;
	private String userName = null;

	// Declare logging objects
	static String className = S2KCEE_Constants.class.getName();
	public static Logger logger = Logger.getLogger(className);
		
	public S2KCEE_Constants(String company, String jndiName, String providerURL, String userName)
	{
		this.company = company;
		this.jndiName = jndiName;
		this.providerURL = providerURL;
		this.userName = userName;
	}

	public int getConstantsValue(String constantsKey, String increaseKeyValueFlag)
	{
		// Log the entrance into the method
		String methodName = "getConstantsValue";
		logger.entering(className, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute="";
		int keyValue = 1;
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sqlToExecute = "call EBNXTOQ (?,?,?)";
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setString(2, constantsKey);
			
			if(increaseKeyValueFlag.equalsIgnoreCase("yes"))
			{
				pstmt.setString(3, "Y");
				sql = "call EBNXTOQ ("+Integer.parseInt(company)+","+constantsKey+",Y)"; 
			}
			else
			{
				pstmt.setString(3, "N");
				sql = "call EBNXTOQ ("+Integer.parseInt(company)+","+constantsKey+",N)"; 
			}
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, methodName+" SQL: "+sql);
			
			ResultSet storedProcedure_rs = pstmt.executeQuery();
			if(storedProcedure_rs.next())
				keyValue = storedProcedure_rs.getInt(1);
			
			if (logger.isLoggable(Level.FINEST))
				logger.logp(Level.FINEST, className, methodName, "keyValue: "+keyValue);
		}
		catch (Exception e)
		{
			keyValue = 1;
			
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
		logger.exiting(className, methodName);

		return keyValue;
	}
	
	/**
	 * Generic method to query any of the S2K UDFs
	 * 
	 * @param file
	 * 	The name of the file you want to get UDFs for. Typically this is in all capital letters
	 * @param key1
	 * 	First UDF key for the file.
	 * @param key2
	 * 	Second UDF key for the file. An empty string should be used if the file does not have a second key.
	 * @param key3
	 * 	Third UDF key for the file. An empty string should be used if the file does not have a third key.
	 * @param key4
	 * 	Fourth UDF key for the file. An empty string should be used if the file does not have a fourth key.
	 * @param key5
	 * 	Fifth UDF key for the file. An empty string should be used if the file does not have a fifth key.
	 * @param fieldNumber
	 * 	The field number you would like to retrieve. If 0, all fields matching the file name and the 5 keys will be returned.
	 * @return
	 * 	A <code>java.util.ArrayList</code> of <code>com.vormittag.share.UserDefinedField</code> objects containing the UDF data
	 */
	/*public ArrayList<UserDefinedField> getUserDefinedFields (String file, String key1, String key2, String key3, String key4, String key5, int fieldNumber)
	{
		// Log the entrance into the method
		String methodName = "getUserDefinedFields";
		logger.entering(className, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		ArrayList<UserDefinedField> udfs = new ArrayList<UserDefinedField>();
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = "select UDFBFILD,UDFBVALUE,UDFBVALUEN,UDFBVALUED,UDFBVALUEI from VUDDTA_PV where VUDDTA_PV.UDFBCMP="+company+
					" and VUDDTA_PV.UDFBFILE='"+file+"' and VUDDTA_PV.UDFBKEY1='"+key1+"' and VUDDTA_PV.UDFBKEY2='"+key2+"' and VUDDTA_PV.UDFBKEY3='"+key3+
					"' and VUDDTA_PV.UDFBKEY4='"+key4+"' and VUDDTA_PV.UDFBKEY5='"+key5+"' ";
			sqlToExecute = "select UDFBFILD,UDFBVALUE,UDFBVALUEN,UDFBVALUED,UDFBVALUEI from VUDDTA_PV where VUDDTA_PV.UDFBCMP=? and VUDDTA_PV.UDFBFILE=? " +
					"and VUDDTA_PV.UDFBKEY1=? and VUDDTA_PV.UDFBKEY2=? and VUDDTA_PV.UDFBKEY3=? and VUDDTA_PV.UDFBKEY4=? and VUDDTA_PV.UDFBKEY5=? ";
			// Only check the field number if it was specified
			if (fieldNumber > 0)
			{
				sql += " and VUDDTA_PV.UDFBFILD="+fieldNumber;
				sqlToExecute += " and VUDDTA_PV.UDFBFILD=? ";
			}
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, methodName+" SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setString(2, file);
			pstmt.setString(3, key1);
			pstmt.setString(4, key2);
			pstmt.setString(5, key3);
			pstmt.setString(6, key4);
			pstmt.setString(7, key5);
			if (fieldNumber > 0)
				pstmt.setInt(8, fieldNumber);
			
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{
				UserDefinedField udf = new UserDefinedField();
				udf.setFieldNumber(rs.getInt("UDFBFILD"));
				udf.setFieldValueString(rs.getString("UDFBVALUE").trim());
				udf.setFieldValueNumeric(rs.getDouble("UDFBVALUEN"));
				udf.setFieldValueDate(rs.getInt("UDFBVALUED"));
				udf.setFieldValueISODate(rs.getDate("UDFBVALUEI"));
				udfs.add(udf);
				
				if (logger.isLoggable(Level.FINEST))
				{
					logger.logp(Level.FINEST, className, methodName, "udf field number: "+udf.getFieldNumber());
					logger.logp(Level.FINEST, className, methodName, "udf field value string: "+udf.getFieldValueString());
					logger.logp(Level.FINEST, className, methodName, "udf field value numeric: "+udf.getFieldValueNumeric());
					logger.logp(Level.FINEST, className, methodName, "udf field value date: "+udf.getFieldValueDate());
					logger.logp(Level.FINEST, className, methodName, "udf field value ISO date: "+udf.getFieldValueISODate());
				}
			}
		}
		catch (Exception e)
		{
			udfs = new ArrayList<UserDefinedField>();
			
			e.printStackTrace();
		}
		finally
		{
			
			 * close any jdbc instances here that weren't
			 * explicitly closed during normal code path, so
			 * that we don't 'leak' resources...
			 
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
		logger.exiting(className, methodName);
		
		return udfs;
	}*/
	
	/**
	 * Get a single S2K Constants entry from VXACONT
	 * 
	 * @param constantsKey
	 *  The Constants Key for VXACONT
	 * @return
	 * 	A <code>com.vormittag.util.S2KConstant</code> object filled with the following information from the VXACONT record:<br/>
	 * 	<ul>
	 * 		<li>The entire alpha work field, as a single String</li>
	 * 		<li>An ArrayList of all 30 Constants number fields</li>
	 * 		<li>The entire Mods Work Field, as a single String</li>
	 * 	</ul><br/>
	 * 	If no record was found, the object will be empty.
	 */
	public S2KConstant getS2KConstants(String constantsKey)
	{
		// Log the entrance into the method
		String methodName = "getS2KConstants";
		logger.entering(className, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute="";
		S2KConstant constant = new S2KConstant();
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();

			sql = "select * from VXACONT where VXACONT.XACMP="+company+" and VXACONT.XAKEY='"+constantsKey+"'";
			sqlToExecute = "select * from VXACONT where VXACONT.XACMP=? and VXACONT.XAKEY=?";
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setString(2, constantsKey);
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, methodName+" SQL: "+sql);
			
			ResultSet rs = pstmt.executeQuery();
			if(rs.next())
			{
				constant.setAlphaField(rs.getString("XAALPH").trim());
				constant.setModsWorkField(rs.getString("XAMODS").trim());
				
				for (int i = 1; i <= 30; i++)
				{
					if (logger.isLoggable(Level.FINEST))
						logger.logp(Level.FINEST, className, methodName, "fetching value for column: "+("XANO"+StringUtils.leftPad(String.valueOf(i), 2, '0')));
					
					constant.getNumericConstants().add(i-1, rs.getDouble("XANO"+StringUtils.leftPad(String.valueOf(i), 2, '0')));
				}
			}
		}
		catch (Exception e)
		{
			constant = new S2KConstant();
			
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
		logger.exiting(className, methodName);

		return constant;
	}
}