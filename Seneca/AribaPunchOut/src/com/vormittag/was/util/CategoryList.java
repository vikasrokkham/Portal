package com.vormittag.was.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import com.vormittag.util.S2KErrorLogger;

public class CategoryList
{
	protected String company = null;
	protected String websiteId = null;
	protected String parentCategory = null;
	protected String jndiName = null;
	protected String providerURL = null;

	public CategoryList() {}
	
	public CategoryList(String company, String websiteId, String parentCategory, String jndiName, String providerURL)
	{
		this.company = company;
		this.websiteId = websiteId;
		this.parentCategory = parentCategory;
		this.jndiName = jndiName;
		this.providerURL = providerURL;
	}

	//ML Version
	public RelatedCategory getCatInfo(int categoryID, String currentLanguageCode) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		RelatedCategory c = new RelatedCategory();
		
		// Run the query on the database
		try {
			Hashtable<String,String> env = new Hashtable<String,String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();
			
			sql = "select VEBCATG.SHORT_DESCRIPTION,VEBCATG.SMALL_IMAGE_REF,VEBCATGML.SHORT_DESCRIPTION as SHORT_DESCRIPTION_ML,VEBCATGML.SMALL_IMAGE_REF as SMALL_IMAGE_REF_ML " +
				"from VEBCATG " +
				"left outer join VEBCATGML on VEBCATG.COMPANY_NBR=VEBCATGML.COMPANY_NBR and VEBCATG.CATALOG_ID=VEBCATGML.CATALOG_ID and VEBCATG.CATEGORY_ID=VEBCATGML.CATEGORY_ID and VEBCATGML.LANGUAGE_CODE='"+currentLanguageCode+"' " +
				"where VEBCATG.COMPANY_NBR="+company+" and VEBCATG.CATALOG_ID="+websiteId+" and VEBCATG.CATEGORY_ID="+categoryID;
			sqlToExecute = "select VEBCATG.SHORT_DESCRIPTION,VEBCATG.SMALL_IMAGE_REF,VEBCATGML.SHORT_DESCRIPTION as SHORT_DESCRIPTION_ML,VEBCATGML.SMALL_IMAGE_REF as SMALL_IMAGE_REF_ML " +
				"from VEBCATG " +
				"left outer join VEBCATGML on VEBCATG.COMPANY_NBR=VEBCATGML.COMPANY_NBR and VEBCATG.CATALOG_ID=VEBCATGML.CATALOG_ID and VEBCATG.CATEGORY_ID=VEBCATGML.CATEGORY_ID and VEBCATGML.LANGUAGE_CODE=? " +
				"where VEBCATG.COMPANY_NBR=? and VEBCATG.CATALOG_ID=? and VEBCATG.CATEGORY_ID=?";
			
			//System.out.println("getCatInfo SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setString(1, currentLanguageCode);
			pstmt.setInt(2, Integer.parseInt(company));
			pstmt.setInt(3, Integer.parseInt(websiteId));
			pstmt.setInt(4, categoryID);
			
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				c.setCategoryID(categoryID);
				
				String short_description = rs.getString("SHORT_DESCRIPTION_ML");
				if (short_description != null) {
					short_description = short_description.trim();
				} else {
					//fallback to non-ml
					short_description = rs.getString("SHORT_DESCRIPTION");
					if (short_description != null) {
						short_description = short_description.trim();
					}
				}
				c.setCategoryName(short_description);
				
				String small_image_ref = rs.getString("SMALL_IMAGE_REF_ML");
				if (small_image_ref != null) {
					small_image_ref = small_image_ref.trim();
				} else {
					//fallback to non-ml
					small_image_ref = rs.getString("SMALL_IMAGE_REF");
					if (small_image_ref != null) {
						small_image_ref = small_image_ref.trim();
					}
				}
				c.setImage(small_image_ref);
			}
		}
		catch (Exception e)
		{
			c = new RelatedCategory();
			
			S2KErrorLogger logger = new S2KErrorLogger();
			logger.log(e, sql);
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
		
		return c;
	}

	//Old, non-ML	
	public RelatedCategory getCatInfo(int categoryID)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		RelatedCategory c = new RelatedCategory();
		
		// Run the query on the database
		try
		{
			Hashtable<String,String> env = new Hashtable<String,String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();
			
			sql = "select SHORT_DESCRIPTION,SMALL_IMAGE_REF from VEBCATG where COMPANY_NBR="+company+" and CATALOG_ID="+websiteId+" and CATEGORY_ID="+categoryID;
			sqlToExecute = "select SHORT_DESCRIPTION,SMALL_IMAGE_REF from VEBCATG where COMPANY_NBR=? and CATALOG_ID=? and CATEGORY_ID=?";
			
			//System.out.println("getCatInfo SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setInt(2, Integer.parseInt(websiteId));
			pstmt.setInt(3, categoryID);
			
			ResultSet rs = pstmt.executeQuery();
			if (rs.next())
			{
				c.setCategoryID(categoryID);
				c.setCategoryName(rs.getString("SHORT_DESCRIPTION"));
				c.setImage(rs.getString("SMALL_IMAGE_REF"));
			}
		}
		catch (Exception e)
		{
			c = new RelatedCategory();
			
			S2KErrorLogger logger = new S2KErrorLogger();
			logger.log(e, sql);
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
		
		return c;
	}

	public String getCompany()
	{
		return company;
	}
	public void setCompany(String company)
	{
		this.company = company;
	}
	public String getWebsiteId()
	{
		return websiteId;
	}
	public void setWebsiteId(String websiteId)
	{
		this.websiteId = websiteId;
	}
	public String getParentCategory()
	{
		return parentCategory;
	}
	public void setParentCategory(String parentCategory)
	{
		this.parentCategory = parentCategory;
	}
	public String getJndiName()
	{
		return jndiName;
	}
	public void setJndiName(String jndiName)
	{
		this.jndiName = jndiName;
	}
	public String getProviderURL()
	{
		return providerURL;
	}
	public void setProviderURL(String providerURL)
	{
		this.providerURL = providerURL;
	}
}