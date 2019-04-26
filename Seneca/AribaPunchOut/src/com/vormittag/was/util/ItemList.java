package com.vormittag.was.util;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;

public class ItemList
{
	protected String company = null;
	protected String websiteId = null;
	protected String jndiName = null;
	protected String providerURL = null;
	protected String currentLocation = null;
	protected int decimalPrecisionPricing = 0;
	protected Set<String> locations = new TreeSet<String>();
	
	// Declare logging objects
	private static String className = ItemList.class.getName();
	private static Logger logger = Logger.getLogger(className);
	
	private ItemList() {}
	
	private ItemList(String company, String websiteId, String jndiName, String providerURL, int decimalPrecision)
	{
		this.company = company;
		this.websiteId = websiteId;
		this.jndiName = jndiName;
		this.providerURL = providerURL;
		this.decimalPrecisionPricing = decimalPrecision;
	}

	public ItemList(String company, String websiteId, String jndiName, String providerURL, String location, int decimalPrecision)
	{
		this.company = company;
		this.websiteId = websiteId;
		this.jndiName = jndiName;
		this.providerURL = providerURL;
		this.currentLocation = location;
		this.decimalPrecisionPricing = decimalPrecision;
		this.locations.add(location);
	}
	
	private void getItemDetail(Item itemDetail)
	{
		getItemDetail(itemDetail, "");
	}
	
	private void getItemDetail(Item itemDetail,String location)
	{
		getItemDetail(itemDetail, location, true);
	}
	
	private void getItemDetail(Item itemDetail, String location, boolean checkActive)
	{
		// Log the entrance into the method
		String methodName = "getItemDetail(Item, location, checkActive)";
		logger.entering(className, methodName);
		
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
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, methodName+" SQL: "+sql);

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
		logger.exiting(className, methodName);
	}
	
	private void getItemDetail(Item itemDetail, String location, String currentLanguageCode)
	{
		getItemDetail(itemDetail, location, true, currentLanguageCode, 0);
	}
	private void getItemDetail(Item itemDetail, String location, boolean checkActive, String currentLanguageCode, int orderGuide)
	{
		// Log the entrance into the method
		String methodName = "getItemDetail(Item, languageCode, checkActive, currentLanguageCode, orderGuide)";
		logger.entering(className, methodName);
		
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
					"left outer join VEBEXTIML on VEBEXTI.COMPANY_NBR=VEBEXTIML.COMPANY_NBR and VEBEXTI.CATALOG_ID=VEBEXTIML.CATALOG_ID and VEBEXTI.ITEM_NBR=VEBEXTIML.ITEM_NBR and VEBEXTIML.LANGUAGE_CODE='"+currentLanguageCode+"' " +
					"join VEBITMB on VEBITMB.COMPANY_NBR="+company+" and VEBITMB.ITEM_NBR=VEBITEM.ITEM_NBR and VEBITMB.ITEM_NBR='"+itemDetail.getItemNumber()+"' and VEBITMB.LOCATION='"+location+"' "+
					"left outer join VEBCATI on VEBCATI.COMPANY_NBR="+company+" and VEBCATI.CATALOG_ID="+websiteId+" and VEBCATI.ITEM_NBR=VEBITEM.ITEM_NBR and VEBCATI.ITEM_NBR='"+itemDetail.getItemNumber()+"' and VEBCATI.PRIMARY_CATEGORY='Y' "+
					"left outer join VEBCATG on VEBCATG.COMPANY_NBR="+company+" and VEBCATG.CATALOG_ID="+websiteId+" and VEBCATG.IS_ACTIVE='Y' and VEBCATG.CATEGORY_ID=VEBCATI.CATEGORY_ID "+
					"left outer join VINMFGC on VINMFGC.IRCMP="+company+" and VINMFGC.IRCODE=VEBITEM.MFG_CODE "+
					"left outer join VEBSTYLE on VEBSTYLE.ITEM_NBR=VEBITEM.ITEM_NBR and VEBSTYLE.COMPANY_NBR="+company+" and VEBSTYLE.CATALOG_ID="+websiteId+" "+
					"where (VEBITEM.STATUS_FLAG='A' or VEBITEM.STATUS_FLAG='I') and VEBITEM.COMPANY_NBR="+company+" and VEBITEM.ITEM_NBR='"+itemDetail.getItemNumber()+"' " +
					"and VEBEXTI.COMPANY_NBR="+company+" and VEBEXTI.CATALOG_ID="+websiteId;
			
			if (checkActive)
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
					
			if (checkActive)
				sqlToExecute += "and VEBEXTI.IS_ACTIVE='Y' ";
			
				sqlToExecute += "and VEBPRIC.COMPANY_NBR=? " +
					"and VEBPRIC.ITEM_NBR=VEBITEM.ITEM_NBR and VEBPRIC.LOCATION=? order by VEBPRIC.UOM_MULTIPLIER asc";
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, methodName+" SQL: "+sql);

			int c = 1;
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setString(c++, currentLanguageCode);
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
						nf.setDecimalPrecision(decimalPrecisionPricing);
						
						unitOfMeasure.setDiscount(nf.format("0", "DECIMALPRECISIONPRICING"));
						unitOfMeasure.setAvailableQty("0");		
				}
				
				itemDetail.setManufacturerItem(items_rs.getString("MFG_STOCK_NO").trim());
				if (items_rs.getString("IRNAME") != null)
					itemDetail.setManufacturerName(items_rs.getString("IRNAME").trim());
				else
					itemDetail.setManufacturerName("");
				
				CategoryList cList = new CategoryList(company, websiteId, "", jndiName, providerURL);
				itemDetail.setRelatedCategory1(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY1"), currentLanguageCode));
				itemDetail.setRelatedCategory2(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY2"), currentLanguageCode));
				itemDetail.setRelatedCategory3(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY3"), currentLanguageCode));
				
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
		logger.exiting(className, methodName);
	}
	
	//ML Version
	private void getItemDetail(Item itemDetail, String location, String accountNumber, String shipToNumber, String currentLanguageCode) {
		
		// Log the entrance into the method
		String methodName = "getItemDetail(Item, location, accountNumber, shipToNumber, languageCode)";
		logger.entering(className, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		Vector uomList = new Vector();
		
		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = (DataSource)context.lookup(jndiName);
			conn = ds.getConnection();

			sql = "select DESCRIPTION_ONE,DESCRIPTION_TWO,DESCRIPTION_THREE,IS_STOCKING," +
					"VEBEXTI.WEB_DESCRIPTION_1,VEBEXTI.WEB_DESCRIPTION_2,VEBEXTI.META_KEYWORDS," +
					"VEBEXTI.META_DESCRIPTION,VEBEXTI.BIG_IMAGE_REF,VEBEXTI.SMALL_IMAGE_REF," +
					"VEBEXTI.BIG_ALT_TEXT,VEBEXTI.SMALL_ALT_TEXT,VEBEXTI.EXTENDED_COMMENTS," +
					"VEBEXTI.CONTENT_KEY,VEBEXTI.PAGE_TITLE,VEBEXTI.SHORT_TEXT,VEBEXTI.ITEM_TYPE," +
					"VEBEXTIML.WEB_DESCRIPTION_1 as WEB_DESCRIPTION_1_ML,VEBEXTIML.WEB_DESCRIPTION_2 as WEB_DESCRIPTION_2_ML,VEBEXTIML.META_KEYWORDS as META_KEYWORDS_ML," +
					"VEBEXTIML.META_DESCRIPTION as META_DESCRIPTION_ML,VEBEXTIML.BIG_IMAGE_REF as BIG_IMAGE_REF_ML,VEBEXTIML.SMALL_IMAGE_REF as SMALL_IMAGE_REF_ML," +
					"VEBEXTIML.BIG_ALT_TEXT as BIG_ALT_TEXT_ML,VEBEXTIML.SMALL_ALT_TEXT as SMALL_ALT_TEXT_ML,VEBEXTIML.EXTENDED_COMMENTS as EXTENDED_COMMENTS_ML," +
					"VEBEXTIML.PAGE_TITLE as PAGE_TITLE_ML,VEBEXTIML.SHORT_TEXT as SHORT_TEXT_ML," +
					"VEBPRIC.OUR_PRICE,VEBPRIC.UNIT_OF_MEASURE,VEBPRIC.SUGGESTED_PRICE," +
					"VEBPRIC.UOM_DESCRIPTION,VEBPRIC.UOM_MULTIPLIER,VEBITEM.MFG_STOCK_NO,VEBITEM.MFG_CODE,VINMFGC.IRNAME,RELATED_CATEGORY1,RELATED_CATEGORY2," +
					"RELATED_CATEGORY3,HAZARD,HAZARD_SURCHARGE,HEAVY,OVERSIZED,REFRIGERATED,UNAVAILABLE,ANON_PRICE,VEBCATI.CATEGORY_ID,VEBSTYLE.SUGGESTED_PRICE as STYLE_SUGGESTED_PRICE,VEBSTYLE.DISP_FORMAT," +
					"VEBITEM.STATUS_FLAG,VEBITMB.STATUS_FLAG as STATUS_FLAG_B "+
					"from VEBPRIC,VEBITEM " +
					"join VEBEXTI on VEBITEM.ITEM_NBR=VEBEXTI.ITEM_NBR "+
					"left outer join VEBEXTIML on VEBEXTI.COMPANY_NBR=VEBEXTIML.COMPANY_NBR and VEBEXTI.CATALOG_ID=VEBEXTIML.CATALOG_ID and VEBEXTI.ITEM_NBR=VEBEXTIML.ITEM_NBR and VEBEXTIML.LANGUAGE_CODE='"+currentLanguageCode+"' " +
					"join VEBITMB on VEBITMB.COMPANY_NBR="+company+" and VEBITMB.ITEM_NBR=VEBITEM.ITEM_NBR and VEBITMB.ITEM_NBR='"+itemDetail.getItemNumber()+"' and VEBITMB.LOCATION='"+location+"' "+
					"left outer join VEBCATI on VEBCATI.COMPANY_NBR="+company+" and VEBCATI.CATALOG_ID="+websiteId+" and VEBCATI.ITEM_NBR=VEBITEM.ITEM_NBR and VEBCATI.ITEM_NBR='"+itemDetail.getItemNumber()+"' and VEBCATI.PRIMARY_CATEGORY='Y' "+
					"left outer join VEBCATG on VEBCATG.COMPANY_NBR="+company+" and VEBCATG.CATALOG_ID="+websiteId+" and VEBCATG.IS_ACTIVE='Y' and VEBCATG.CATEGORY_ID=VEBCATI.CATEGORY_ID "+
					"left outer join VINMFGC on VINMFGC.IRCMP="+company+" and VINMFGC.IRCODE=VEBITEM.MFG_CODE "+
					"left outer join VEBSTYLE on VEBSTYLE.ITEM_NBR=VEBITEM.ITEM_NBR and VEBSTYLE.COMPANY_NBR="+company+" and VEBSTYLE.CATALOG_ID="+websiteId+" "+
					"where (VEBITEM.STATUS_FLAG='A' or VEBITEM.STATUS_FLAG='I') and VEBITEM.COMPANY_NBR="+company+" and VEBITEM.ITEM_NBR='"+itemDetail.getItemNumber()+"' " +
					"and VEBEXTI.COMPANY_NBR="+company+" and VEBEXTI.CATALOG_ID="+websiteId+" and VEBEXTI.IS_ACTIVE='Y' " +
					"and VEBPRIC.COMPANY_NBR="+company+" " +
					"and VEBPRIC.ITEM_NBR=VEBITEM.ITEM_NBR and VEBPRIC.LOCATION='"+location+"' order by VEBPRIC.UOM_MULTIPLIER asc";
			
			sqlToExecute = "select DESCRIPTION_ONE,DESCRIPTION_TWO,DESCRIPTION_THREE,IS_STOCKING," +
				"VEBEXTI.WEB_DESCRIPTION_1,VEBEXTI.WEB_DESCRIPTION_2,VEBEXTI.META_KEYWORDS," +
				"VEBEXTI.META_DESCRIPTION,VEBEXTI.BIG_IMAGE_REF,VEBEXTI.SMALL_IMAGE_REF," +
				"VEBEXTI.BIG_ALT_TEXT,VEBEXTI.SMALL_ALT_TEXT,VEBEXTI.EXTENDED_COMMENTS," +
				"VEBEXTI.CONTENT_KEY,VEBEXTI.PAGE_TITLE,VEBEXTI.SHORT_TEXT,VEBEXTI.ITEM_TYPE," +
				"VEBEXTIML.WEB_DESCRIPTION_1 as WEB_DESCRIPTION_1_ML,VEBEXTIML.WEB_DESCRIPTION_2 as WEB_DESCRIPTION_2_ML,VEBEXTIML.META_KEYWORDS as META_KEYWORDS_ML," +
				"VEBEXTIML.META_DESCRIPTION as META_DESCRIPTION_ML,VEBEXTIML.BIG_IMAGE_REF as BIG_IMAGE_REF_ML,VEBEXTIML.SMALL_IMAGE_REF as SMALL_IMAGE_REF_ML," +
				"VEBEXTIML.BIG_ALT_TEXT as BIG_ALT_TEXT_ML,VEBEXTIML.SMALL_ALT_TEXT as SMALL_ALT_TEXT_ML,VEBEXTIML.EXTENDED_COMMENTS as EXTENDED_COMMENTS_ML," +
				"VEBEXTIML.PAGE_TITLE as PAGE_TITLE_ML,VEBEXTIML.SHORT_TEXT as SHORT_TEXT_ML," +
				"VEBPRIC.OUR_PRICE,VEBPRIC.UNIT_OF_MEASURE,VEBPRIC.SUGGESTED_PRICE," +
				"VEBPRIC.UOM_DESCRIPTION,VEBPRIC.UOM_MULTIPLIER,VEBITEM.MFG_STOCK_NO,VEBITEM.MFG_CODE,VINMFGC.IRNAME,RELATED_CATEGORY1,RELATED_CATEGORY2," +
				"RELATED_CATEGORY3,HAZARD,HAZARD_SURCHARGE,HEAVY,OVERSIZED,REFRIGERATED,UNAVAILABLE,ANON_PRICE,VEBCATI.CATEGORY_ID,VEBSTYLE.SUGGESTED_PRICE as STYLE_SUGGESTED_PRICE,VEBSTYLE.DISP_FORMAT," +
				"VEBITEM.STATUS_FLAG,VEBITMB.STATUS_FLAG as STATUS_FLAG_B "+
				"from VEBPRIC,VEBITEM " +
				"join VEBEXTI on VEBITEM.ITEM_NBR=VEBEXTI.ITEM_NBR "+
				"left outer join VEBEXTIML on VEBEXTI.COMPANY_NBR=VEBEXTIML.COMPANY_NBR and VEBEXTI.CATALOG_ID=VEBEXTIML.CATALOG_ID and VEBEXTI.ITEM_NBR=VEBEXTIML.ITEM_NBR and VEBEXTIML.LANGUAGE_CODE=? " +
				"join VEBITMB on VEBITMB.COMPANY_NBR=? and VEBITMB.ITEM_NBR=VEBITEM.ITEM_NBR and VEBITMB.ITEM_NBR=? and VEBITMB.LOCATION=? "+
				"left outer join VEBCATI on VEBCATI.COMPANY_NBR=? and VEBCATI.CATALOG_ID=? and VEBCATI.ITEM_NBR=VEBITEM.ITEM_NBR and VEBCATI.ITEM_NBR=? and VEBCATI.PRIMARY_CATEGORY='Y' "+
				"left outer join VEBCATG on VEBCATG.COMPANY_NBR=? and VEBCATG.CATALOG_ID=? and VEBCATG.IS_ACTIVE='Y' and VEBCATG.CATEGORY_ID=VEBCATI.CATEGORY_ID "+
				"left outer join VINMFGC on VINMFGC.IRCMP=? and VINMFGC.IRCODE=VEBITEM.MFG_CODE "+
				"left outer join VEBSTYLE on VEBSTYLE.ITEM_NBR=VEBITEM.ITEM_NBR and VEBSTYLE.COMPANY_NBR=? and VEBSTYLE.CATALOG_ID=? "+
				"where (VEBITEM.STATUS_FLAG='A' or VEBITEM.STATUS_FLAG='I') and VEBITEM.COMPANY_NBR=? and VEBITEM.ITEM_NBR=? " +
				"and VEBEXTI.COMPANY_NBR=? and VEBEXTI.CATALOG_ID=? and VEBEXTI.IS_ACTIVE='Y' " +
				"and VEBPRIC.COMPANY_NBR=? " +
				"and VEBPRIC.ITEM_NBR=VEBITEM.ITEM_NBR and VEBPRIC.LOCATION=? order by VEBPRIC.UOM_MULTIPLIER asc";
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, methodName+" SQL: "+sql);
			
			int c = 1;
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setString(c++, currentLanguageCode);
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
				itemDetail.setPageTitle(page_title);
				
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
				itemDetail.setMetaKeywords(meta_keywords);
				
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
				itemDetail.setMetaDescription(meta_description);
				itemDetail.setManufacturerItem(items_rs.getString("DESCRIPTION_THREE").trim());
				itemDetail.setStockItem(items_rs.getString("IS_STOCKING").trim());
				itemDetail.setContentKey(items_rs.getString("CONTENT_KEY").trim());
				
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
				itemDetail.setFullImageAltText(big_alt_text);
				
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
				itemDetail.setThumbImageAltText(items_rs.getString("SMALL_ALT_TEXT").trim());
				
				if (itemDetail.getExtDescription() == null || itemDetail.getExtDescription().equalsIgnoreCase("")) {
					Clob extended_comments = items_rs.getClob("EXTENDED_COMMENTS_ML");
					if (extended_comments == null) {
						//fallback to non-ml
						extended_comments = items_rs.getClob("EXTENDED_COMMENTS");
					}
					itemDetail.setExtDescription(extended_comments.getSubString(1, (int)extended_comments.length()));
				}
				
				//Available qty is set in the uomList in the pricing routine
				//itemDetail.setAvailaleQuantity(items_rs.getInt("ONHAND_QTY"));
				
				unitOfMeasure.setUnitMearurePrice(items_rs.getString("OUR_PRICE").trim());
				unitOfMeasure.setUnitOfMeasure(items_rs.getString("UNIT_OF_MEASURE").trim());
				unitOfMeasure.setMultiplier(items_rs.getDouble("UOM_MULTIPLIER"));
				unitOfMeasure.setSuggestedPrice(items_rs.getString("SUGGESTED_PRICE").trim());
				
				if (setDefaultUoM)
				{
					itemDetail.setUnitMeasure(items_rs.getString("UNIT_OF_MEASURE").trim()); //This is set because it is used in the field name for adds
					itemDetail.setDefaultUOM(items_rs.getString("UNIT_OF_MEASURE").trim());
					itemDetail.setSuggestedPrice(items_rs.getString("SUGGESTED_PRICE").trim());
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
				itemDetail.setItemType(items_rs.getString("ITEM_TYPE"));
				
				if (itemDetail.getItemType().equals("S") && items_rs.getString("STYLE_SUGGESTED_PRICE") != null)
				{
					unitOfMeasure.setUnitMearurePrice(items_rs.getString("STYLE_SUGGESTED_PRICE"));
					
					NumberFormatting nf = new NumberFormatting();
					nf.setDecimalPrecision(decimalPrecisionPricing);
					
					unitOfMeasure.setDiscount(nf.format("0", "DECIMALPRECISIONPRICING"));
					unitOfMeasure.setAvailableQty("0");
					
					itemDetail.setStyleDspFormat(items_rs.getString("DISP_FORMAT").trim());
				}
				
				CategoryList cList = new CategoryList(company, websiteId, "", jndiName, providerURL);
				itemDetail.setRelatedCategory1(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY1"), currentLanguageCode));
				itemDetail.setRelatedCategory2(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY2"), currentLanguageCode));
				itemDetail.setRelatedCategory3(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY3"), currentLanguageCode));
				
				//itemDetail.setProductComments(getProductComments(itemDetail.getItemNumber()));
				Clob short_text = items_rs.getClob("SHORT_TEXT_ML");
				if (short_text == null) {
					//fallback to non-ml
					short_text = items_rs.getClob("SHORT_TEXT");
				}
				itemDetail.setProductComments(short_text.getSubString(1, (int)short_text.length()));
				
				itemDetail.setHazardFlag(items_rs.getString("HAZARD").trim());
				itemDetail.setHazardSurchargeFlag(items_rs.getString("HAZARD_SURCHARGE").trim());
				itemDetail.setHeavyFlag(items_rs.getString("HEAVY").trim());
				itemDetail.setOversizedFlag(items_rs.getString("OVERSIZED").trim());
				itemDetail.setRefrigeratedFlag(items_rs.getString("REFRIGERATED").trim());
				itemDetail.setUnavailableFlag(items_rs.getString("UNAVAILABLE").trim().equals("Y")?"Y":"N");
				itemDetail.setAnonPriceFlag(items_rs.getString("ANON_PRICE").trim());
				itemDetail.setItemCategory(String.valueOf(items_rs.getInt("CATEGORY_ID")));
				//itemDetail.setSuggestedPrice(items_rs.getString("SUGGESTED_PRICE").trim());
				
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
			
			// Get the customer item numbers for this item
			itemDetail.setCustomerItem(getCustomerItemNumber(itemDetail.getItemNumber(), accountNumber));
			
			//Get the promo numbers for this item here using the same connection
			//PromoUtilities pu = new PromoUtilities(company, websiteId, jndiName, providerURL);
			//itemDetail.setPromoNumbers(pu.getPromoNumberForItem(itemDetail, accountNumber, shipToNumber, location));
			
			// Get the external links for this item
			//itemDetail.setLinks(getLinks(itemDetail.getItemNumber()));
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
		logger.exiting(className, methodName);
	}
	
	//non-ML version
	private void getItemDetail(Item itemDetail, String location, String accountNumber, String shipToNumber)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		Vector uomList = new Vector();
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = (DataSource)context.lookup(jndiName);
			conn = ds.getConnection();

			sql = "select DESCRIPTION_ONE,DESCRIPTION_TWO,DESCRIPTION_THREE,IS_STOCKING," +
				"VEBEXTI.WEB_DESCRIPTION_1,VEBEXTI.WEB_DESCRIPTION_2,VEBEXTI.META_KEYWORDS," +
				"VEBEXTI.META_DESCRIPTION,VEBEXTI.BIG_IMAGE_REF,VEBEXTI.SMALL_IMAGE_REF," +
				"VEBEXTI.BIG_ALT_TEXT,VEBEXTI.SMALL_ALT_TEXT,VEBEXTI.EXTENDED_COMMENTS," +
				"VEBEXTI.CONTENT_KEY,VEBEXTI.PAGE_TITLE,VEBEXTI.SHORT_TEXT,VEBPRIC.OUR_PRICE,VEBPRIC.UNIT_OF_MEASURE," +
				"VEBPRIC.UOM_DESCRIPTION,VEBPRIC.UOM_MULTIPLIER,VEBITEM.MFG_STOCK_NO,VINMFGC.IRNAME,RELATED_CATEGORY1,RELATED_CATEGORY2," +
				"RELATED_CATEGORY3,HAZARD,HAZARD_SURCHARGE,HEAVY,OVERSIZED,REFRIGERATED,UNAVAILABLE,ANON_PRICE,VEBCATI.CATEGORY_ID " +
				" from VEBEXTI,VEBPRIC,VEBITEM " +
				" join VEBITMB on VEBITMB.COMPANY_NBR="+company+" and VEBITMB.ITEM_NBR='"+itemDetail.getItemNumber()+"' and VEBITMB.LOCATION="+location+
				" left outer join VEBCATI on VEBCATI.COMPANY_NBR="+company+" and VEBCATI.CATALOG_ID="+websiteId+" and VEBCATI.ITEM_NBR='"+itemDetail.getItemNumber()+"' and VEBCATI.PRIMARY_CATEGORY='Y' "+
				" left outer join VEBCATG on VEBCATG.COMPANY_NBR="+company+" and VEBCATG.CATALOG_ID="+websiteId+" and VEBCATG.IS_ACTIVE='Y' and VEBCATG.CATEGORY_ID=VEBCATI.CATEGORY_ID "+
				" left outer join VINMFGC on VINMFGC.IRCMP="+company+" and VINMFGC.IRCODE=VEBITEM.MFG_CODE "+
				" where VEBITEM.STATUS_FLAG='A' and VEBITEM.COMPANY_NBR="+company+
				" and VEBITEM.ITEM_NBR='"+itemDetail.getItemNumber()+"' and VEBEXTI.COMPANY_NBR="+
				company+" and VEBEXTI.CATALOG_ID="+websiteId+" and VEBEXTI.IS_ACTIVE='Y' AND " +
				" VEBITEM.ITEM_NBR=VEBEXTI.ITEM_NBR and VEBPRIC.COMPANY_NBR="+company+
				" AND VEBPRIC.ITEM_NBR=VEBITEM.ITEM_NBR and VEBPRIC.LOCATION="+location+" order by VEBPRIC.UOM_MULTIPLIER asc";
			
			sqlToExecute = "select DESCRIPTION_ONE,DESCRIPTION_TWO,DESCRIPTION_THREE,IS_STOCKING," +
				"VEBEXTI.WEB_DESCRIPTION_1,VEBEXTI.WEB_DESCRIPTION_2,VEBEXTI.META_KEYWORDS," +
				"VEBEXTI.META_DESCRIPTION,VEBEXTI.BIG_IMAGE_REF,VEBEXTI.SMALL_IMAGE_REF," +
				"VEBEXTI.BIG_ALT_TEXT,VEBEXTI.SMALL_ALT_TEXT,VEBEXTI.EXTENDED_COMMENTS," +
				"VEBEXTI.CONTENT_KEY,VEBEXTI.PAGE_TITLE,VEBEXTI.SHORT_TEXT,VEBPRIC.OUR_PRICE,VEBPRIC.UNIT_OF_MEASURE," +
				"VEBPRIC.UOM_DESCRIPTION,VEBPRIC.UOM_MULTIPLIER,VEBITEM.MFG_STOCK_NO,VINMFGC.IRNAME,RELATED_CATEGORY1,RELATED_CATEGORY2," +
				"RELATED_CATEGORY3,HAZARD,HAZARD_SURCHARGE,HEAVY,OVERSIZED,REFRIGERATED,UNAVAILABLE,ANON_PRICE,VEBCATI.CATEGORY_ID " +
				" from VEBEXTI,VEBPRIC,VEBITEM " +
				" join VEBITMB on VEBITMB.COMPANY_NBR=? and VEBITMB.ITEM_NBR=? and VEBITMB.LOCATION=? "+
				" left outer join VEBCATI on VEBCATI.COMPANY_NBR=? and VEBCATI.CATALOG_ID=? and VEBCATI.ITEM_NBR=? and VEBCATI.PRIMARY_CATEGORY='Y' "+
				" left outer join VEBCATG on VEBCATG.COMPANY_NBR=? and VEBCATG.CATALOG_ID=? and VEBCATG.IS_ACTIVE='Y' and VEBCATG.CATEGORY_ID=VEBCATI.CATEGORY_ID "+
				" left outer join VINMFGC on VINMFGC.IRCMP=? and VINMFGC.IRCODE=VEBITEM.MFG_CODE "+
				" where VEBITEM.STATUS_FLAG='A' and VEBITEM.COMPANY_NBR=? and VEBITEM.ITEM_NBR=? " +
				" and VEBEXTI.COMPANY_NBR=? and VEBEXTI.CATALOG_ID=? and VEBEXTI.IS_ACTIVE='Y' " +
				" and VEBITEM.ITEM_NBR=VEBEXTI.ITEM_NBR and VEBPRIC.COMPANY_NBR=? " +
				" and VEBPRIC.ITEM_NBR=VEBITEM.ITEM_NBR and VEBPRIC.LOCATION=? order by VEBPRIC.UOM_MULTIPLIER asc";
			
			//System.out.println("getItemDetail: "+sql);
			
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
				
				if (setDefaultUoM)
				{
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
			
			// Get the customer item numbers for this item
			itemDetail.setCustomerItem(getCustomerItemNumber(itemDetail.getItemNumber(), accountNumber));
			
			//Get the promo numbers for this item here using the same connection
			//PromoUtilities pu = new PromoUtilities(company, websiteId, jndiName, providerURL);
			//itemDetail.setPromoNumbers(pu.getPromoNumberForItem(itemDetail, accountNumber, shipToNumber, location));
			
			// Get the external links for this item
			//itemDetail.setLinks(getLinks(itemDetail.getItemNumber()));
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
	
	//ML Version
	private Vector<Item> getItemInfo(Vector[] itemsIn, String location, String accountNumber, String shipToNumber, String s2kPricing, String sessionID, boolean checkActive, boolean checkStyleItem, boolean doReplacements, String currentLanguageCode) {
		// Log the entrance into the method
		String methodName = "getItemInfo";
		logger.entering(className, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		Vector<Item> itemsOut = new Vector<Item>();
		
		String xRefCustomerNumber = getxRefCustomerNumber(accountNumber);
		
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
			pstmt.setString(1, currentLanguageCode);
			pstmt.setInt(2, Integer.parseInt(company));
			pstmt.setString(3, location);
			pstmt.setInt(4, Integer.parseInt(company));
			pstmt.setString(5, xRefCustomerNumber);
			pstmt.setInt(6, Integer.parseInt(company));
			pstmt.setInt(7, Integer.parseInt(company));
			pstmt.setString(8, location);
			pstmt.setInt(9, Integer.parseInt(company));
			pstmt.setInt(10, Integer.parseInt(websiteId));
			
			for (int x = 0; x < itemsIn[0].size(); x++)
			{
				boolean haveItem = false;
				boolean isStyleSKU = false;
				Item item = new Item();
				
				//Get the itemNumber and quantity
				String itemNumber = itemsIn[0].elementAt(x).toString();
				String quantity = itemsIn[1].elementAt(x).toString();

				sql = 	"SELECT VCOITEM.ONCITM,VEBITEM.ITEM_NBR,VEBITEM.DESCRIPTION_ONE," +
						"VEBITEM.DESCRIPTION_TWO,VEBITEM.IS_STOCKING," +
						"VEBEXTI.WEB_DESCRIPTION_1,VEBEXTI.WEB_DESCRIPTION_2,VEBEXTI.IS_ACTIVE," +
						"VEBEXTIML.WEB_DESCRIPTION_1 as WEB_DESCRIPTION_1_ML,VEBEXTIML.WEB_DESCRIPTION_2 as WEB_DESCRIPTION_2_ML," +
						"VEBPRIC.UNIT_OF_MEASURE,VEBPRIC.UOM_DESCRIPTION,VEBPRIC.SUGGESTED_PRICE,VEBPRIC.UOM_MULTIPLIER " +
						"FROM VEBPRIC,VEBITEM " +
						"JOIN VEBEXTI ON VEBEXTI.ITEM_NBR=VEBITEM.ITEM_NBR " +
						"LEFT OUTER JOIN VEBEXTIML ON VEBEXTI.COMPANY_NBR=VEBEXTIML.COMPANY_NBR AND VEBEXTI.CATALOG_ID=VEBEXTIML.CATALOG_ID AND VEBEXTI.ITEM_NBR=VEBEXTIML.ITEM_NBR AND VEBEXTIML.LANGUAGE_CODE='"+currentLanguageCode+"' " +
						"JOIN VEBITMB ON VEBITMB.ITEM_NBR=VEBITEM.ITEM_NBR AND VEBITMB.COMPANY_NBR="+company+" AND VEBITMB.LOCATION="+location+" "+
						"LEFT OUTER JOIN VCOITEM ON VCOITEM.ONITEM=VEBITEM.ITEM_NBR AND VCOITEM.ONCMP="+company+" AND VCOITEM.ONCUST='"+xRefCustomerNumber+"' "+
						"WHERE (VEBITEM.STATUS_FLAG='A' or VEBITEM.STATUS_FLAG='I') AND VEBITEM.COMPANY_NBR="+company+" " +
						"AND VEBPRIC.COMPANY_NBR="+company+" AND VEBITEM.ITEM_NBR=VEBPRIC.ITEM_NBR " +
						"AND VEBPRIC.LOCATION='"+location+"' ";
						
				sql += "AND VEBEXTI.IS_ACTIVE='Y' AND VEBEXTI.COMPANY_NBR="+company+" AND VEBEXTI.CATALOG_ID="+websiteId+" " +
						"AND (UPPER(VCOITEM.ONCITM)='"+itemNumber.toUpperCase()+"' OR UPPER(VEBITEM.ITEM_NBR)='"+itemNumber.toUpperCase()+"') ";
				
				if (logger.isLoggable(Level.FINE))
					logger.logp(Level.FINE, className, methodName, methodName+" SQL: "+sql);
				
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
					if (items_rs.getDouble("UOM_MULTIPLIER") == 1.00)
					{
						item.setSuggestedPrice(items_rs.getString("SUGGESTED_PRICE").trim());
						
					}
					
				}
				
				if (haveItem || isStyleSKU)
				{
					// Now I will just add the item details
					if (isStyleSKU)
						getItemDetail(item, location, false, currentLanguageCode, 0);
					else
						getItemDetail(item, location, checkActive, currentLanguageCode, 0);
					
					// Make sure that the quantity follows minimum and multiple rules
					HashMap<String, String> minMult = item.getMinimumMultiple();
					int itemQty = Integer.parseInt(quantity);
					int roundedMinQty = 0;
					if (minMult.get("Minimum") != null)
						roundedMinQty = Math.round(Float.parseFloat(minMult.get("Minimum")));
					
					/*
					System.out.println("Quick Order item: "+itemNumber);
					System.out.println("itemQty: "+itemQty);
					System.out.println("roundedMinQty: "+roundedMinQty);
					System.out.println("multiple? *"+minMult.get("Multiple")+"*");
					*/
					
					if(itemQty < roundedMinQty)
					{
						
						String qtyMsg = "Item "+itemNumber+" quantity has been changed from "+quantity+
							" to "+roundedMinQty;
						item.setQoQtyMessage(qtyMsg);
						
						quantity = String.valueOf(roundedMinQty);
					}
					else if(minMult.get("Multiple") != null && minMult.get("Multiple").equals("Y"))
					{
						//System.out.println("itemQty % roundedMinQty: "+itemQty % roundedMinQty);
						
						if(itemQty % roundedMinQty > 0)
						{
							int tmp = (int)(Math.floor(itemQty / roundedMinQty) * roundedMinQty) + roundedMinQty;
							
							String qtyMsg = "Item "+itemNumber+" quantity has been changed from "+
								itemQty+" to "+String.valueOf(tmp);
							item.setQoQtyMessage(qtyMsg);
							
							quantity = String.valueOf(tmp);
						}
					}
					
					//System.out.println("Finalized item qty: "+quantity);
					
					item.setQuantityForQuickOrder(quantity);
					
					// Price the item here now, I need to know if it is restricted or inactive so I can treat it like an invalid item
					if (s2kPricing.equals("Y"))
						getWebPrice(item, accountNumber, shipToNumber, sessionID);
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
						item.setUomList(unitOfMeasure);
					}
					
					if (item.getItemType().equals("S"))
						item.setQuantityForQuickOrder("0");
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
				}
			
				if (doReplacements && item.isReplacement() && Double.parseDouble(((UnitOfMeasure)item.getUomList().firstElement()).getAvailableQty()) <= 0) {
					Vector[] replacementLookupItem = initializeTheVectorArray(2);
					
					replacementLookupItem[0].addElement(item.getReplacementItem().trim());
					replacementLookupItem[1].addElement(quantity);
					
					Vector<Item> replacementItemVector = getItemInfo(replacementLookupItem, location, accountNumber, shipToNumber, s2kPricing, sessionID, checkActive, checkStyleItem, currentLanguageCode);
					
					if (replacementItemVector.size() > 0) {
						Item replacementItem = replacementItemVector.get(0);
						replacementItem.setDescription3("This item is a replacement for " + item.getItemNumber());
						itemsOut.add(replacementItem);
					}
				} else {
					itemsOut.add(item);
				}
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
		
		return itemsOut;
	}
	
	//ML Version
	private Vector<Item> getItemInfo(Vector<String>[] itemsIn, String location, String accountNumber, String shipToNumber, String s2kPricing, String sessionID, String currentLanguageCode) {
		return getItemInfo(itemsIn,location,accountNumber,shipToNumber,s2kPricing,sessionID,true,false,currentLanguageCode);
	}
	
	//ML Version - Wrapper without specifying doReplacements for backwards compatibility. 
	// Defaults doReplacements to false, since that's what happened in the past.
	private Vector<Item> getItemInfo(Vector[] itemsIn, String location, String accountNumber, String shipToNumber, String s2kPricing, String sessionID, boolean checkActive, boolean checkStyleItem, String currentLanguageCode) {
		return getItemInfo(itemsIn, location, accountNumber, shipToNumber, s2kPricing, sessionID, checkActive, checkStyleItem, false, currentLanguageCode);
	}
	
	//Old, non-ML
	private Vector<Item> getItemInfo(Vector<String>[] itemsIn, String location, String accountNumber,
			String shipToNumber, String s2kPricing, String sessionID){
		return getItemInfo(itemsIn,location,accountNumber,shipToNumber,s2kPricing,sessionID,true,false);
	}
	
	//Old, non-ML
	private Vector<Item> getItemInfo(Vector[] itemsIn, String location, String accountNumber,
			String shipToNumber, String s2kPricing, String sessionID, boolean checkActive, boolean checkStyleItem)
	{
		// Log the entrance into the method
		String methodName = "getItemInfo(Vector[])";
		logger.entering(className, methodName);
		
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
				
				if (logger.isLoggable(Level.FINE))
					logger.logp(Level.FINE, className, methodName, methodName+" SQL: "+sql);
				
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
					
					if (logger.isLoggable(Level.FINEST))
					{
						logger.logp(Level.FINEST, className, methodName, "Quick Order item: "+itemNumber);
						logger.logp(Level.FINEST, className, methodName, "itemQty: "+itemQty);
						logger.logp(Level.FINEST, className, methodName, "roundedMinQty: "+roundedMinQty);
						logger.logp(Level.FINEST, className, methodName, "multiple? *"+minMult.get("Multiple")+"*");
					}
					
					if(itemQty < roundedMinQty)
					{
						String qtyMsg = "Item "+itemNumber+" quantity has been changed from "+quantity+" to "+roundedMinQty;
						item.setQoQtyMessage(qtyMsg);
						
						quantity = String.valueOf(roundedMinQty);
					}
					else if(minMult.get("Multiple") != null && minMult.get("Multiple").equals("Y"))
					{
						if (logger.isLoggable(Level.FINEST))
							logger.logp(Level.FINEST, className, methodName, "itemQty % roundedMinQty: "+itemQty % roundedMinQty);
						if(itemQty % roundedMinQty > 0)
						{
							int tmp = (int)(Math.floor(itemQty / roundedMinQty) * roundedMinQty) + roundedMinQty;
							String qtyMsg = "Item "+itemNumber+" quantity has been changed from "+itemQty+" to "+String.valueOf(tmp);
							item.setQoQtyMessage(qtyMsg);
							quantity = String.valueOf(tmp);
						}
					}
					if (logger.isLoggable(Level.FINEST))
						logger.logp(Level.FINEST, className, methodName, "Finalized item qty: "+quantity);
					item.setQuantityForQuickOrder(quantity);
					
					// Price the item here now, I need to know if it is restricted or inactive so I can treat it like an invalid item
					if (s2kPricing.equals("Y"))
						getWebPrice(item, accountNumber, shipToNumber, sessionID);
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
		logger.exiting(className, methodName);
		
		return itemsOut;
	}
	
	private void getWebPrice(Item itemDetail, String accountNumber, String shipToNumber, String sessionID)
	{
		getWebPrice(itemDetail, accountNumber, shipToNumber, sessionID, false);
	}
	
	private void getWebPrice(Item itemDetail, String accountNumber, String shipToNumber, String sessionID, boolean checkMultiple)
	{
		// Log the entrance into the method	
		String methodName = "getWebPrice";	
		logger.entering(className, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";

		//Vector<UnitOfMeasure> uomList = new Vector<UnitOfMeasure>();
		NumberFormatting nf = new NumberFormatting();
		nf.setDecimalPrecision(decimalPrecisionPricing);
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
			
			for (String location : this.locations)
			{
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
					if (logger.isLoggable(Level.FINE))	
		                logger.logp(Level.FINE, className, methodName, methodName+" SQL: "+sql);
					
					pstmt.setString(6, unitOfMeasure.getUnitOfMeasure());
					ResultSet items_rs = pstmt.executeQuery();
					if (items_rs.next())
					{
						if (logger.isLoggable(Level.FINEST))
					    {
							 logger.logp(Level.FINEST, className, methodName, "items_rs.getString(1): "+items_rs.getString(1));
							 logger.logp(Level.FINEST, className, methodName, "items_rs.getString(2): "+items_rs.getString(2));
							 logger.logp(Level.FINEST, className, methodName, "items_rs.getString(3): "+items_rs.getString(3));
							 logger.logp(Level.FINEST, className, methodName, "items_rs.getString(4): "+items_rs.getString(4));
							 logger.logp(Level.FINEST, className, methodName, "items_rs.getString(5): "+items_rs.getString(5));
							 logger.logp(Level.FINEST, className, methodName, "items_rs.getString(6): "+items_rs.getString(6));
							 logger.logp(Level.FINEST, className, methodName, "items_rs.getString(7): "+items_rs.getString(7));
							 logger.logp(Level.FINEST, className, methodName, "items_rs.getString(8): "+items_rs.getString(8));
							 logger.logp(Level.FINEST, className, methodName, "items_rs.getString(9): "+items_rs.getString(9));
							 logger.logp(Level.FINEST, className, methodName, "items_rs.getString(10): "+items_rs.getString(10));
							 logger.logp(Level.FINEST, className, methodName, "items_rs.getString(11): "+items_rs.getString(11));
			            }
					
						if (location.equals(this.currentLocation))
						{
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
							//the original author of this ought to be privately shamed.
						    unitOfMeasure.setAvailableQty(Double.toString(availQty));
						}
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
	    logger.exiting(className, methodName);
	}

	//	itemDetail is passed by reference by default so ther is no need to return anything
	private void getWebPriceVEBPRIC(Item itemDetail, String accountNumber, String location)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		Vector<UnitOfMeasure> uomList = new Vector<UnitOfMeasure>();
		NumberFormatting nf = new NumberFormatting();
		nf.setDecimalPrecision(decimalPrecisionPricing);
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

	@SuppressWarnings("rawtypes")
	private Vector[] initializeTheVectorArray(int xSize)
	{
		Vector[] vectorToInitialize = new Vector[xSize];
		for (int x = 0; x < xSize; x++)
			vectorToInitialize[x] = new Vector();

		return vectorToInitialize;
	}
	
	private ArrayList<ItemLinks> getLinks(String itemNumber)
	{
		// Log the entrance into the method
		String methodName = "getLinks";
		logger.entering(className, methodName);
				
		Connection conn = null;
		PreparedStatement pstmt = null;
		ArrayList<ItemLinks> links = new ArrayList<ItemLinks>();
		
		String sql = "select LINK_NAME,DESCRIPTION,LINK_TYPE,DISPLAY from VEBDLINK where COMPANY_NBR="+company+" and CATALOG_ID="+websiteId+" and ITEM_NBR='"+itemNumber+"'";
		String sqlToExecute = "select LINK_NAME,DESCRIPTION,LINK_TYPE,DISPLAY from VEBDLINK where COMPANY_NBR=? and CATALOG_ID=? and ITEM_NBR=?";
		
		logger.logp(Level.FINE, className, methodName, "getLinks SQL: "+sql);
		
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
		logger.exiting(className, methodName);
		
		return links;
	}
	
	private HashMap<String, String> getMinimumMultipleForItem(Item itemDetail, String location){
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

			sql = " SELECT * from VUDDTA_PV WHERE UDFBCMP="+company+" and UDFBFILE='VINITMB' and UDFBKEY1="+location+" and UDFBKEY2="+itemDetail.getItemNumber();
			sqlToExecute = 	" SELECT * from VUDDTA_PV " +
							" WHERE UDFBCMP=? and UDFBFILE='VINITMB' and UDFBKEY1=? and UDFBKEY2=?";
			
			//System.out.println("getMinimumMultipleForItem SQL: "+sql);
			
			int i=1;
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(i++, Integer.parseInt(company));
			pstmt.setString(i++, location);
			pstmt.setString(i++, itemDetail.getItemNumber());
			
			ResultSet rs = pstmt.executeQuery();
			String minimumQuantity = "";
			String inMultiples = "";
			
			while (rs.next()){
				int quantityIndicator = rs.getInt("UDFBFILD");
				if(quantityIndicator == 1){
					minimumQuantity = rs.getDouble("UDFBVALUEN")+"";
					minimumMultiple.put("Minimum", minimumQuantity);
				}else if(quantityIndicator == 2){
					inMultiples = rs.getString("UDFBVALUE").trim();
					minimumMultiple.put("Multiple", inMultiples);
				}else{}
				
				//System.out.println("\t\tMinimum: "+minimumQuantity+" Multiple: "+inMultiples);
			}
			
		}catch (Exception e){
			minimumMultiple = new HashMap<String, String>();
			System.err.println(sql);
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
		
		return minimumMultiple;
	}	

	private HashMap<String, String> getMinimumMultipleForItem(Item itemDetail){
		// Log the entrance into the method	
		String methodName = "getMinimumMultipleForItem(Item itemDetail)";	
		logger.entering(className, methodName);
		
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
			
			if (logger.isLoggable(Level.FINEST))
				 logger.logp(Level.FINEST, className, methodName, "SQL: "+sql);
			
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
					
					if (logger.isLoggable(Level.FINEST))
						 logger.logp(Level.FINEST, className, methodName, "Minimum: "+minimumQuantity);
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
	    logger.exiting(className, methodName);
	    
		return minimumMultiple;
	}	
	
	private String getCustomerItemNumber(String itemNumber, String accountNumber)
	{
		// Log the entrance into the method
		String methodName = "getCustomerItemNumber";
		logger.entering(className, methodName);
					
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "", cItem = "";
		
		String xRefCustomerNumber = getxRefCustomerNumber(accountNumber);
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = (DataSource)context.lookup(jndiName);
			conn = ds.getConnection();
			
			sql = "select ONCITM from VCOITEM where VCOITEM.ONCMP="+company+" and VCOITEM.ONCUST='"+xRefCustomerNumber+"' and VCOITEM.ONITEM='"+itemNumber+"'";
			sqlToExecute = "select ONCITM from VCOITEM where VCOITEM.ONCMP=? and VCOITEM.ONCUST=? and VCOITEM.ONITEM=?";
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, "getCustomerItemNumber SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setString(2, xRefCustomerNumber);
			pstmt.setString(3, itemNumber);
	
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{
				if (!cItem.equals(""))
					cItem += ",";
				
				cItem += rs.getString("ONCITM").trim();
			}
		}
		catch (Exception e)
		{
			cItem = "";
			
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
		
		return cItem;
	}	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Vector<Item> getAssociatedItems(String company, String customerNumber, String shipToNumber, String reprice, String sessionID, int lineNumber, String itemNumber, int qty, String uom)
	{
		// Log the entrance into the method
		String methodName = "getAssociatedItems";
		logger.entering(className, methodName);
		
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
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, "getAssociatedItems SQL: "+sql);
			
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
				
				if (logger.isLoggable(Level.FINEST))
				{
					logger.logp(Level.FINEST, className, methodName, "company: "+item.getCompany());
					logger.logp(Level.FINEST, className, methodName, "item number: "+item.getItemNumber());
					logger.logp(Level.FINEST, className, methodName, "uom: "+item.getUnitMeasure());
					logger.logp(Level.FINEST, className, methodName, "qty: "+item.getItemQuantity());
				}
					
				Vector[] associatedItemsIn = initializeTheVectorArray(2);
				associatedItemsIn[0].addElement(item.getItemNumber().trim());
				associatedItemsIn[1].addElement(item.getItemQuantity());
				
				Vector<Item> aItems = getItemInfo(associatedItemsIn,
						currentLocation,
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
		logger.exiting(className, methodName);
		
		return itemList;
	}	

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Vector<Item> getAssociatedItems(String company, String customerNumber, String shipToNumber, String reprice, String sessionID, int lineNumber, String itemNumber, int qty, String uom,String currentLanguageCode)
	{
		// Log the entrance into the method
		String methodName = "getAssociatedItems";
		logger.entering(className, methodName);
		
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
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, "getAssociatedItems SQL: "+sql);
			
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
				
				if (logger.isLoggable(Level.FINEST))
				{
					logger.logp(Level.FINEST, className, methodName, "company: "+item.getCompany());
					logger.logp(Level.FINEST, className, methodName, "item number: "+item.getItemNumber());
					logger.logp(Level.FINEST, className, methodName, "uom: "+item.getUnitMeasure());
					logger.logp(Level.FINEST, className, methodName, "qty: "+item.getItemQuantity());
				}
					
				Vector[] associatedItemsIn = initializeTheVectorArray(2);
				associatedItemsIn[0].addElement(item.getItemNumber().trim());
				associatedItemsIn[1].addElement(item.getItemQuantity());
				
				Vector<Item> aItems = getItemInfo(associatedItemsIn,
						currentLocation,
						customerNumber,
						shipToNumber,
						reprice, sessionID,false,false,currentLanguageCode);
				
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
		logger.exiting(className, methodName);
		
		return itemList;
	}
	
	/*//non-ML version
	private ArrayList<Item> getCrossSellItems(int cartID, CustomerAccount account, String s2kPricing, String sessionID, String userID, int maxResults)
	{
		// Log the entrance into the method
		String methodName = "getCrossSellItems";
		logger.entering(className, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "" , sqlToExecute = "";
		ArrayList<Item> items = new ArrayList<Item>();
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = (DataSource)context.lookup(jndiName);
			conn = ds.getConnection();

			sql = "select VEBIXREF.REFERENCE_ITEM " +
				" from VEBIXREF " +
				" join VEBCART on VEBCART.COMPANY_NBR="+company+" and VEBCART.CART_KEY="+cartID+
				" and VEBCART.ITEM_NUMBER=VEBIXREF.ITEM_NBR and VEBCART.LINE_NBR=" +
				" (select max(LINE_NBR) from VEBCART where VEBCART.COMPANY_NBR="+company+" and CART_KEY="+cartID+" and PARENT_LINE_NBR<=0) "+
				" where VEBIXREF.COMPANY_NBR="+company+" and VEBIXREF.CATALOG_ID="+websiteId+
				" and VEBIXREF.REFERENCE_TYPE='C'"+
				" order by VEBIXREF.SEQUENCE_NBR ";
			sqlToExecute = "select VEBIXREF.REFERENCE_ITEM " +
				" from VEBIXREF " +
				" join VEBCART on VEBCART.COMPANY_NBR=? and VEBCART.CART_KEY=?"+
				" and VEBCART.ITEM_NUMBER=VEBIXREF.ITEM_NBR and VEBCART.LINE_NBR=" +
				" (select max(LINE_NBR) from VEBCART where VEBCART.COMPANY_NBR=? and CART_KEY=? and PARENT_LINE_NBR<=0) "+
				" where VEBIXREF.COMPANY_NBR=? and VEBIXREF.CATALOG_ID=?"+
				" and VEBIXREF.REFERENCE_TYPE='C'"+
				" order by VEBIXREF.SEQUENCE_NBR ";
			
			if (maxResults > 0)
			{
				sql += " fetch first "+maxResults+" rows only";
				sqlToExecute += " fetch first "+maxResults+" rows only";
			}
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, methodName+" SQL: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setInt(2, cartID);
			pstmt.setInt(3, Integer.parseInt(company));
			pstmt.setInt(4, cartID);
			pstmt.setInt(5, Integer.parseInt(company));
			pstmt.setInt(6, Integer.parseInt(websiteId));
	
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{
				String itemNumber = rs.getString("REFERENCE_ITEM").trim();
				
				// Get the item details for the cross-sell item number
				Vector<Item> itemList = getItemDetailForItemInParm(itemNumber, currentLocation, account.getAccountNumber(), account.getShipToNumber());
				Item item = new Item();
				if (itemList.size() > 0)
				{
					item = itemList.get(0);
				
					// Price the cross-sell items, un-comment this if we need prices
					if (s2kPricing.equalsIgnoreCase("Y"))
						getWebPrice(item, account.getAccountNumber(), account.getShipToNumber(), sessionID);
					else
						getWebPriceVEBPRIC(item, account.getAccountNumber(), currentLocation);
					
					// Get the list of order guides
					OrderGuideUtilities orderGuideUtil = new OrderGuideUtilities(company, websiteId, jndiName, providerURL, userID);
					item.setOrderGuides(orderGuideUtil.getOrderGuides(account, item.getItemNumber()));
					
					// Add the item to the list
					items.add(item);
				}
			}
		}
		catch (Exception e)
		{
			items = new ArrayList<Item>();
			
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
		
		return items;
	}
	
	//ML Version
	private ArrayList<Item> getCrossSellItems(int cartID, CustomerAccount account, String s2kPricing, String sessionID, String userID, int maxResults, String currentLanguageCode)
	{
		// Log the entrance into the method
		String methodName = "getCrossSellItems";
		logger.entering(className, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "" , sqlToExecute = "";
		ArrayList<Item> items = new ArrayList<Item>();
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = (DataSource)context.lookup(jndiName);
			conn = ds.getConnection();

			sql = "select ITEM_NUMBER,STYLE_ITEM_NBR from vebcart where VEBCART.COMPANY_NBR="+company+" and CART_KEY="+cartID+" and VEBCART.LINE_NBR="+
				  "(select max(LINE_NBR) from VEBCART where VEBCART.COMPANY_NBR="+company+" and CART_KEY="+cartID+" and PARENT_LINE_NBR<=0)";
			sqlToExecute = "select ITEM_NUMBER,STYLE_ITEM_NBR from vebcart where VEBCART.COMPANY_NBR=? and CART_KEY=? and VEBCART.LINE_NBR="+
					  "(select max(LINE_NBR) from VEBCART where VEBCART.COMPANY_NBR=? and CART_KEY=? and PARENT_LINE_NBR<=0)";
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, methodName+" SQL 1: "+sql);
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setInt(2, cartID);
			pstmt.setInt(3, Integer.parseInt(company));
			pstmt.setInt(4, cartID);
			
			ResultSet rs = pstmt.executeQuery();
			
			String itemNumber = "";
			if (rs.next()) {
				if (rs.getString("STYLE_ITEM_NBR") != null && !rs.getString("STYLE_ITEM_NBR").trim().equals(""))
					itemNumber = rs.getString("STYLE_ITEM_NBR").trim();
				else
					itemNumber = rs.getString("ITEM_NUMBER");
			}
			
			pstmt.close();
			pstmt = null;
			
			if (!itemNumber.equals(""))
			{
				sql = "select VEBIXREF.REFERENCE_ITEM " +
					" from VEBIXREF " +
					" where VEBIXREF.COMPANY_NBR="+company+" and VEBIXREF.CATALOG_ID="+websiteId+
					" and VEBIXREF.REFERENCE_TYPE='C'"+" and VEBIXREF.ITEM_NBR='"+itemNumber+"'"+
					" order by VEBIXREF.SEQUENCE_NBR ";
				sqlToExecute = "select VEBIXREF.REFERENCE_ITEM " +
					" from VEBIXREF " +
					" where VEBIXREF.COMPANY_NBR=? and VEBIXREF.CATALOG_ID=?"+
					" and VEBIXREF.REFERENCE_TYPE='C' and VEBIXREF.ITEM_NBR=?"+
					" order by VEBIXREF.SEQUENCE_NBR ";
				
				if (maxResults > 0)
				{
					sql += " fetch first "+maxResults+" rows only";
					sqlToExecute += " fetch first "+maxResults+" rows only";
				}
				
				if (logger.isLoggable(Level.FINE))
					logger.logp(Level.FINE, className, methodName, methodName+" SQL 2: "+sql);
				
				pstmt = conn.prepareStatement(sqlToExecute);
				pstmt.setInt(1, Integer.parseInt(company));
				pstmt.setInt(2, Integer.parseInt(websiteId));
				pstmt.setString(3, itemNumber);
		
				rs = pstmt.executeQuery();
				while (rs.next())
				{
					String refItemNumber = rs.getString("REFERENCE_ITEM").trim();
					
					// Get the item details for the cross-sell item number
					Vector<Item> itemList = getItemDetailForItemInParm(refItemNumber, currentLocation, account.getAccountNumber(), account.getShipToNumber(),currentLanguageCode);
					Item item = new Item();
					if (itemList.size() > 0)
					{
						item = itemList.get(0);
					
						if (!item.getItemType().equals("S"))
						{
							// Price the cross-sell items, un-comment this if we need prices
							if (s2kPricing.equalsIgnoreCase("Y"))
							{
								boolean checkMultiple = account.getMinimumMultiple().equals("Y")?true:false;
								getWebPrice(item, account.getAccountNumber(), account.getShipToNumber(), sessionID,checkMultiple);
							}
							else
								getWebPriceVEBPRIC(item, account.getAccountNumber(), currentLocation);
						}
						
						// Get the list of order guides
						OrderGuideUtilities orderGuideUtil = new OrderGuideUtilities(company, websiteId, jndiName, providerURL, userID);
						item.setOrderGuides(orderGuideUtil.getOrderGuides(account, item.getItemNumber()));
						
						// Add the item to the list
						items.add(item);
					}
				}
			}
		}
		catch (Exception e)
		{
			items = new ArrayList<Item>();
			
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
		
		return items;
	}*/
	
	// Non-ML version
	private Vector<Item> getItemDetailForItemInParm(String itemNumber, String location, String accountNumber, String shipToNumber)
	{
		return getItemDetailForItemInParm(itemNumber, location, accountNumber, shipToNumber, "", "", true);
	}
	private Vector<Item> getItemDetailForItemInParm(String itemNumber, String location, String accountNumber, String shipToNumber, String s2kPricing, String sessionID)
	{
		return getItemDetailForItemInParm(itemNumber, location, accountNumber, shipToNumber, s2kPricing, sessionID, true);
	}
	
	//ML Version
	private Vector<Item> getItemDetailForItemInParm(String itemNumber, String location, String accountNumber, String shipToNumber, String currentLanguageCode)
	{
		return getItemDetailForItemInParm(itemNumber, location, accountNumber, shipToNumber, "", "", currentLanguageCode, true);
	}
	private Vector<Item> getItemDetailForItemInParm(String itemNumber, String location, String accountNumber, String shipToNumber, String s2kPricing, String sessionID, String currentLanguageCode)
	{
		return getItemDetailForItemInParm(itemNumber, location, accountNumber, shipToNumber, s2kPricing, sessionID, currentLanguageCode, true);
	}
	
	// Non-ML version
	private Vector<Item> getItemDetailForItemInParm(String itemNumber, String location, String accountNumber, String shipToNumber, String s2kPricing,
			String sessionID, boolean mustBeWebActive)
	{
		// Log the entrance into the method
		String methodName = "getItemDetailForItemInParm";
		logger.entering(className, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "", webActiveSQL = "";
		Vector<Item> itemList = new Vector<Item>();
		Item itemDetail = new Item();
		Vector<UnitOfMeasure> uomList = new Vector<UnitOfMeasure>();

		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();
			
			if (mustBeWebActive)
				webActiveSQL = " and VEBEXTI.IS_ACTIVE='Y' ";
			
			sql = "select DESCRIPTION_ONE,DESCRIPTION_TWO,DESCRIPTION_THREE,IS_STOCKING," +
				"WEB_DESCRIPTION_1,WEB_DESCRIPTION_2,VEBEXTI.META_KEYWORDS,VEBEXTI.META_DESCRIPTION," +
				"VEBEXTI.BIG_IMAGE_REF,VEBEXTI.SMALL_IMAGE_REF,VEBEXTI.BIG_ALT_TEXT,VEBEXTI.SMALL_ALT_TEXT," +
				"VEBEXTI.EXTENDED_COMMENTS,VEBEXTI.CONTENT_KEY,VEBEXTI.PAGE_TITLE,VEBEXTI.SHORT_TEXT,VEBPRIC.OUR_PRICE," +
				"VEBPRIC.UNIT_OF_MEASURE,VEBPRIC.UOM_DESCRIPTION,VEBPRIC.UOM_MULTIPLIER,VEBITEM.MFG_STOCK_NO," +
				"VINMFGC.IRNAME,VEBITEM.REPLACEMENT_ITEM,VEBITEM.ITEM_NBR,VEBCATI.CATEGORY_ID,VEBCATG.RELATED_CATEGORY1," +
				"VEBCATG.RELATED_CATEGORY2,VEBCATG.RELATED_CATEGORY3,HAZARD,HAZARD_SURCHARGE,HEAVY,OVERSIZED,REFRIGERATED," +
				"UNAVAILABLE,ANON_PRICE,VEBCATI.CATEGORY_ID,VEBITEM.STATUS_FLAG,VEBMFGC.URL,VEBMFGC.NOFOLLOW "+
				" from VEBITEM "+
				" join VEBEXTI on VEBEXTI.COMPANY_NBR="+company+" and VEBEXTI.CATALOG_ID="+websiteId+webActiveSQL+" and VEBEXTI.ITEM_NBR=VEBITEM.ITEM_NBR "+
				" join VEBPRIC on VEBPRIC.COMPANY_NBR="+company+" AND VEBPRIC.ITEM_NBR=VEBITEM.ITEM_NBR and VEBPRIC.LOCATION="+location+
				" join VEBITMB on VEBITMB.COMPANY_NBR="+company+" and VEBITMB.ITEM_NBR='"+itemNumber.toUpperCase()+"' and VEBITMB.LOCATION="+location+"' "+
				" left outer join VEBCATI on VEBCATI.COMPANY_NBR="+company+" and VEBCATI.CATALOG_ID="+websiteId+" and VEBCATI.ITEM_NBR='"+itemNumber.toUpperCase()+"' and VEBCATI.PRIMARY_CATEGORY='Y' "+
				" left outer join VEBCATG on VEBCATG.COMPANY_NBR="+company+" and VEBCATG.CATALOG_ID="+websiteId+" and VEBCATG.IS_ACTIVE='Y' and VEBCATG.CATEGORY_ID=VEBCATI.CATEGORY_ID "+
				" left outer join VINMFGC on VINMFGC.IRCMP="+company+" and VINMFGC.IRCODE=VEBITEM.MFG_CODE "+
				" left outer join VEBMFGC on VEBMFGC.COMPANY_NBR="+company+" and VEBMFGC.MFG_CODE=VEBITEM.MFG_CODE "+
				" where (VEBITEM.STATUS_FLAG='A' or VEBITEM.STATUS_FLAG='I') and VEBITEM.COMPANY_NBR="+company+" and VEBITEM.ITEM_NBR='"+itemNumber.toUpperCase()+
				" order by VEBPRIC.UOM_MULTIPLIER asc";
			sqlToExecute = "select DESCRIPTION_ONE,DESCRIPTION_TWO,DESCRIPTION_THREE,IS_STOCKING," +
				"WEB_DESCRIPTION_1,WEB_DESCRIPTION_2,VEBEXTI.META_KEYWORDS,VEBEXTI.META_DESCRIPTION," +
				"VEBEXTI.BIG_IMAGE_REF,VEBEXTI.SMALL_IMAGE_REF,VEBEXTI.BIG_ALT_TEXT,VEBEXTI.SMALL_ALT_TEXT," +
				"VEBEXTI.EXTENDED_COMMENTS,VEBEXTI.CONTENT_KEY,VEBEXTI.PAGE_TITLE,VEBEXTI.SHORT_TEXT,VEBPRIC.OUR_PRICE,VEBPRIC.UNIT_OF_MEASURE," +
				"VEBPRIC.UOM_DESCRIPTION,VEBPRIC.UOM_MULTIPLIER,VEBITEM.MFG_STOCK_NO,VINMFGC.IRNAME,VEBITEM.REPLACEMENT_ITEM,VEBITEM.ITEM_NBR," +
				"VEBCATI.CATEGORY_ID,VEBCATG.RELATED_CATEGORY1,VEBCATG.RELATED_CATEGORY2,VEBCATG.RELATED_CATEGORY3,HAZARD,HAZARD_SURCHARGE,HEAVY," +
				"OVERSIZED,REFRIGERATED,UNAVAILABLE,ANON_PRICE,VEBCATI.CATEGORY_ID,VEBITEM.STATUS_FLAG,VEBMFGC.URL,VEBMFGC.NOFOLLOW "+
				" from VEBITEM " +
				" join VEBEXTI on VEBEXTI.COMPANY_NBR=? and VEBEXTI.CATALOG_ID=? "+webActiveSQL+" and VEBEXTI.ITEM_NBR=VEBITEM.ITEM_NBR "+
				" join VEBPRIC on VEBPRIC.COMPANY_NBR=? AND VEBPRIC.ITEM_NBR=VEBITEM.ITEM_NBR and VEBPRIC.LOCATION=? "+
				" join VEBITMB on VEBITMB.COMPANY_NBR=? and VEBITMB.ITEM_NBR=? and VEBITMB.LOCATION=? "+
				" left outer join VEBCATI on VEBCATI.COMPANY_NBR=? and VEBCATI.CATALOG_ID=? and VEBCATI.ITEM_NBR=? and VEBCATI.PRIMARY_CATEGORY='Y' "+
				" left outer join VEBCATG on VEBCATG.COMPANY_NBR=? and VEBCATG.CATALOG_ID=? and VEBCATG.IS_ACTIVE='Y' and VEBCATG.CATEGORY_ID=VEBCATI.CATEGORY_ID "+
				" left outer join VINMFGC on VINMFGC.IRCMP=? and VINMFGC.IRCODE=VEBITEM.MFG_CODE "+
				" left outer join VEBMFGC on VEBMFGC.COMPANY_NBR=? and VEBMFGC.MFG_CODE=VEBITEM.MFG_CODE "+
				" where (VEBITEM.STATUS_FLAG='A' or VEBITEM.STATUS_FLAG='I') AND VEBITEM.COMPANY_NBR=? and VEBITEM.ITEM_NBR=? "+
				" order by VEBPRIC.UOM_MULTIPLIER asc";
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, methodName+" SQL: "+sql);
			
			int c = 1;
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setString(c++, location);
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setString(c++, itemNumber.toUpperCase());
			pstmt.setString(c++, location);
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setString(c++, itemNumber.toUpperCase());
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setString(c++, itemNumber.toUpperCase());
			
			ResultSet items_rs = pstmt.executeQuery();
			boolean setDefaultUoM = true;
			while (items_rs.next())
			{
				UnitOfMeasure unitOfMeasure = new UnitOfMeasure();
				itemDetail.setItemNumber(items_rs.getString("ITEM_NBR").trim().trim());
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
				
				Clob shortDesc = items_rs.getClob("SHORT_TEXT");
				itemDetail.setProductComments(shortDesc.getSubString(1, (int)shortDesc.length()));
				
				itemDetail.setIsActive(items_rs.getString("STATUS_FLAG").trim());
				
				// Available qty is set in the uomList in the pricing routine
				//itemDetail.setAvailaleQuantity(items_rs.getInt("ONHAND_QTY"));
				
				unitOfMeasure.setUnitMearurePrice(items_rs.getString("OUR_PRICE"));
				unitOfMeasure.setUnitOfMeasure(items_rs.getString("UNIT_OF_MEASURE").trim());
				unitOfMeasure.setMultiplier(items_rs.getDouble("UOM_MULTIPLIER"));
				
				if (setDefaultUoM)
				{
					itemDetail.setUnitMeasure(items_rs.getString("UNIT_OF_MEASURE").trim()); //This is set because it is used in the field name for adds
					itemDetail.setDefaultUOM(items_rs.getString("UNIT_OF_MEASURE").trim());
					//itemDetail.setCartUOM(items_rs.getString("UNIT_OF_MEASURE"));
					setDefaultUoM = false;
				}
				
				unitOfMeasure.setUnitOfMeasureDescription(items_rs.getString("UNIT_OF_MEASURE").trim()+" "+items_rs.getString("UOM_DESCRIPTION").trim());
				itemDetail.setManufacturerItem(items_rs.getString("MFG_STOCK_NO"));
				if (items_rs.getString("IRNAME") != null)
					itemDetail.setManufacturerName(items_rs.getString("IRNAME"));
				else
					itemDetail.setManufacturerName("");
				
				if (items_rs.getString("URL") != null)
				{
					itemDetail.setMfgURL(items_rs.getString("URL").trim());
					itemDetail.setNofollow(items_rs.getString("NOFOLLOW").trim());
				}
				else
				{
					itemDetail.setMfgURL("");
					itemDetail.setNofollow("N");
				}
				
				itemDetail.setReplacementItem(items_rs.getString("REPLACEMENT_ITEM").trim());
				if (itemDetail.getReplacementItem().equalsIgnoreCase(""))
					itemDetail.setReplacement(false);
				else
					itemDetail.setReplacement(true);
				
				itemDetail.setHazardFlag(items_rs.getString("HAZARD").trim());
				itemDetail.setHazardSurchargeFlag(items_rs.getString("HAZARD_SURCHARGE").trim());
				itemDetail.setHeavyFlag(items_rs.getString("HEAVY").trim());
				itemDetail.setOversizedFlag(items_rs.getString("OVERSIZED").trim());
				itemDetail.setRefrigeratedFlag(items_rs.getString("REFRIGERATED").trim());
				itemDetail.setUnavailableFlag(items_rs.getString("UNAVAILABLE").trim());
				itemDetail.setAnonPriceFlag(items_rs.getString("ANON_PRICE").trim());
				itemDetail.setItemCategory(String.valueOf(items_rs.getInt("CATEGORY_ID")));
				
				CategoryList cList = new CategoryList(company, websiteId, "", jndiName, providerURL);
				itemDetail.setRelatedCategory1(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY1")));
				itemDetail.setRelatedCategory2(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY2")));
				itemDetail.setRelatedCategory3(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY3")));
				itemDetail.setItemCategory(String.valueOf(items_rs.getInt("CATEGORY_ID")));
				
				// Add element to UnitOfMeasure List
				uomList.addElement(unitOfMeasure);
			}
			
			// Put the uomList into itemDetail
			itemDetail.setUomList(uomList);
			
			// Get the customer item numbers for this item
			itemDetail.setCustomerItem(getCustomerItemNumber(itemDetail.getItemNumber(), accountNumber));
			
			// Get the external links for this item
			itemDetail.setLinks(getLinks(itemDetail.getItemNumber()));
			
			// Price the item
			/*
			if (s2kPricing.equalsIgnoreCase("Y"))
				getWebPrice(itemDetail, accountNumber, shipToNumber, sessionID);
			else
				getWebPriceVEBPRIC(itemDetail, accountNumber, location);
			*/
			
			// Put the itemDetail into the list as long as we got at least one UOM from the item
			if (itemDetail.getUomList().size() > 0)
				itemList.add(itemDetail);
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
		logger.exiting(className, methodName);
		
		return itemList;
	}
	
	// ML Version
	private Vector<Item> getItemDetailForItemInParm(String itemNumber, String location, String accountNumber, String shipToNumber, String s2kPricing, String sessionID,
			String currentLanguageCode, boolean mustBeWebActive)
	{
		// Log the entrance into the method
		String methodName = "getItemDetailForItemInParm (ML version)";
		logger.entering(className, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "", webActiveSQL = "";
		Vector<Item> itemList = new Vector<Item>();
		Item itemDetail = new Item();
		Vector<UnitOfMeasure> uomList = new Vector<UnitOfMeasure>();

		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();
			
			if (mustBeWebActive)
				webActiveSQL = " and VEBEXTI.IS_ACTIVE='Y' ";
			
			sql = "select DESCRIPTION_ONE,DESCRIPTION_TWO,DESCRIPTION_THREE,IS_STOCKING,VEBEXTI.CONTENT_KEY," +
				"VEBEXTI.WEB_DESCRIPTION_1,VEBEXTI.WEB_DESCRIPTION_2,VEBEXTI.META_KEYWORDS,VEBEXTI.META_DESCRIPTION," +
				"VEBEXTI.BIG_IMAGE_REF,VEBEXTI.SMALL_IMAGE_REF,VEBEXTI.BIG_ALT_TEXT,VEBEXTI.SMALL_ALT_TEXT," +
				"VEBEXTI.EXTENDED_COMMENTS,VEBEXTI.PAGE_TITLE,VEBEXTI.SHORT_TEXT," +
				"VEBEXTIML.WEB_DESCRIPTION_1 as WEB_DESCRIPTION_1_ML,VEBEXTIML.WEB_DESCRIPTION_2 as WEB_DESCRIPTION_2_ML,VEBEXTIML.META_KEYWORDS as META_KEYWORDS_ML,VEBEXTIML.META_DESCRIPTION as META_DESCRIPTION_ML," +
				"VEBEXTIML.BIG_IMAGE_REF as BIG_IMAGE_REF_ML,VEBEXTIML.SMALL_IMAGE_REF as SMALL_IMAGE_REF_ML,VEBEXTIML.BIG_ALT_TEXT as BIG_ALT_TEXT_ML,VEBEXTIML.SMALL_ALT_TEXT as SMALL_ALT_TEXT_ML," +
				"VEBEXTIML.EXTENDED_COMMENTS as EXTENDED_COMMENTS_ML,VEBEXTIML.PAGE_TITLE as PAGE_TITLE_ML,VEBEXTIML.SHORT_TEXT as SHORT_TEXT_ML," +
				"VEBPRIC.OUR_PRICE,VEBPRIC.UNIT_OF_MEASURE,VEBPRIC.UOM_DESCRIPTION,VEBPRIC.UOM_MULTIPLIER,VEBPRIC.SUGGESTED_PRICE as ITEM_SUGGESTED_PRICE,VEBITEM.MFG_STOCK_NO," +
				"VINMFGC.IRNAME,VEBITEM.REPLACEMENT_ITEM,VEBITEM.ITEM_NBR,VEBCATI.CATEGORY_ID,VEBCATG.RELATED_CATEGORY1," +
				"VEBCATG.RELATED_CATEGORY2,VEBCATG.RELATED_CATEGORY3,HAZARD,HAZARD_SURCHARGE,HEAVY,OVERSIZED,REFRIGERATED," +
				"UNAVAILABLE,ANON_PRICE,VEBCATI.CATEGORY_ID,VEBITEM.STATUS_FLAG,VEBEXTI.ITEM_TYPE as ITEM_TYPE,VEBSTYLE.SUGGESTED_PRICE as STYLE_SUGGESTED_PRICE,VEBMFGC.URL,VEBMFGC.NOFOLLOW "+
				" from VEBITEM "+
				" join VEBEXTI on VEBEXTI.COMPANY_NBR="+company+webActiveSQL+" and VEBEXTI.CATALOG_ID="+websiteId+" and VEBEXTI.ITEM_NBR=VEBITEM.ITEM_NBR "+
				"left outer join VEBEXTIML on VEBEXTI.COMPANY_NBR=VEBEXTIML.COMPANY_NBR and VEBEXTI.CATALOG_ID=VEBEXTIML.CATALOG_ID and VEBEXTI.ITEM_NBR=VEBEXTIML.ITEM_NBR and VEBEXTIML.LANGUAGE_CODE='"+currentLanguageCode+"' " +
				" join VEBPRIC on VEBPRIC.COMPANY_NBR="+company+" AND VEBPRIC.ITEM_NBR=VEBITEM.ITEM_NBR and VEBPRIC.LOCATION="+location+
				" join VEBITMB on VEBITMB.COMPANY_NBR="+company+" and VEBITMB.ITEM_NBR='"+itemNumber.toUpperCase()+"' and VEBITMB.LOCATION="+location+" "+
				" left outer join VEBCATI on VEBCATI.COMPANY_NBR="+company+" and VEBCATI.CATALOG_ID="+websiteId+" and VEBCATI.ITEM_NBR='"+itemNumber.toUpperCase()+"' and VEBCATI.PRIMARY_CATEGORY='Y' "+
				" left outer join VEBCATG on VEBCATG.COMPANY_NBR="+company+" and VEBCATG.CATALOG_ID="+websiteId+" and VEBCATG.IS_ACTIVE='Y' and VEBCATG.CATEGORY_ID=VEBCATI.CATEGORY_ID "+
				" left outer join VINMFGC on VINMFGC.IRCMP="+company+" and VINMFGC.IRCODE=VEBITEM.MFG_CODE "+
				" left outer join VEBMFGC on VEBMFGC.COMPANY_NBR="+company+" and VEBMFGC.MFG_CODE=VEBITEM.MFG_CODE "+
				" left outer join VEBSTYLE on VEBSTYLE.ITEM_NBR=VEBITEM.ITEM_NBR and VEBSTYLE.COMPANY_NBR="+company+" and VEBSTYLE.CATALOG_ID="+websiteId+
				" where (VEBITEM.STATUS_FLAG = 'A' or VEBITEM.STATUS_FLAG='I') and VEBITEM.COMPANY_NBR="+company+" and VEBITEM.ITEM_NBR='"+itemNumber.toUpperCase()+"'"+
				" order by VEBPRIC.UOM_MULTIPLIER asc";
			sqlToExecute = "select DESCRIPTION_ONE,DESCRIPTION_TWO,DESCRIPTION_THREE,IS_STOCKING,VEBEXTI.CONTENT_KEY," +
				"VEBEXTI.WEB_DESCRIPTION_1,VEBEXTI.WEB_DESCRIPTION_2,VEBEXTI.META_KEYWORDS,VEBEXTI.META_DESCRIPTION," +
				"VEBEXTI.BIG_IMAGE_REF,VEBEXTI.SMALL_IMAGE_REF,VEBEXTI.BIG_ALT_TEXT,VEBEXTI.SMALL_ALT_TEXT," +
				"VEBEXTI.EXTENDED_COMMENTS,VEBEXTI.PAGE_TITLE,VEBEXTI.SHORT_TEXT," +
				"VEBEXTIML.WEB_DESCRIPTION_1 as WEB_DESCRIPTION_1_ML,VEBEXTIML.WEB_DESCRIPTION_2 as WEB_DESCRIPTION_2_ML,VEBEXTIML.META_KEYWORDS as META_KEYWORDS_ML,VEBEXTIML.META_DESCRIPTION as META_DESCRIPTION_ML," +
				"VEBEXTIML.BIG_IMAGE_REF as BIG_IMAGE_REF_ML,VEBEXTIML.SMALL_IMAGE_REF as SMALL_IMAGE_REF_ML,VEBEXTIML.BIG_ALT_TEXT as BIG_ALT_TEXT_ML,VEBEXTIML.SMALL_ALT_TEXT as SMALL_ALT_TEXT_ML," +
				"VEBEXTIML.EXTENDED_COMMENTS as EXTENDED_COMMENTS_ML,VEBEXTIML.PAGE_TITLE as PAGE_TITLE_ML,VEBEXTIML.SHORT_TEXT as SHORT_TEXT_ML," +
				"VEBPRIC.OUR_PRICE,VEBPRIC.UNIT_OF_MEASURE,VEBPRIC.UOM_DESCRIPTION,VEBPRIC.UOM_MULTIPLIER,VEBPRIC.SUGGESTED_PRICE as ITEM_SUGGESTED_PRICE,VEBITEM.MFG_STOCK_NO," +
				"VINMFGC.IRNAME,VEBITEM.REPLACEMENT_ITEM,VEBITEM.ITEM_NBR,VEBCATI.CATEGORY_ID,VEBCATG.RELATED_CATEGORY1," +
				"VEBCATG.RELATED_CATEGORY2,VEBCATG.RELATED_CATEGORY3,HAZARD,HAZARD_SURCHARGE,HEAVY,OVERSIZED,REFRIGERATED," +
				"UNAVAILABLE,ANON_PRICE,VEBCATI.CATEGORY_ID,VEBITEM.STATUS_FLAG,VEBEXTI.ITEM_TYPE as ITEM_TYPE,VEBSTYLE.SUGGESTED_PRICE as STYLE_SUGGESTED_PRICE,VEBMFGC.URL,VEBMFGC.NOFOLLOW "+
				" from VEBITEM "+
				" join VEBEXTI on VEBEXTI.COMPANY_NBR=?"+webActiveSQL+" and VEBEXTI.CATALOG_ID=? and VEBEXTI.ITEM_NBR=VEBITEM.ITEM_NBR "+
				"left outer join VEBEXTIML on VEBEXTI.COMPANY_NBR=VEBEXTIML.COMPANY_NBR and VEBEXTI.CATALOG_ID=VEBEXTIML.CATALOG_ID and VEBEXTI.ITEM_NBR=VEBEXTIML.ITEM_NBR and VEBEXTIML.LANGUAGE_CODE=? " +
				" join VEBPRIC on VEBPRIC.COMPANY_NBR=? AND VEBPRIC.ITEM_NBR=VEBITEM.ITEM_NBR and VEBPRIC.LOCATION=? "+
				" join VEBITMB on VEBITMB.COMPANY_NBR=? and VEBITMB.ITEM_NBR=? and VEBITMB.LOCATION=? "+
				" left outer join VEBCATI on VEBCATI.COMPANY_NBR=? and VEBCATI.CATALOG_ID=? and VEBCATI.ITEM_NBR=? and VEBCATI.PRIMARY_CATEGORY='Y' "+
				" left outer join VEBCATG on VEBCATG.COMPANY_NBR=? and VEBCATG.CATALOG_ID=? and VEBCATG.IS_ACTIVE='Y' and VEBCATG.CATEGORY_ID=VEBCATI.CATEGORY_ID "+
				" left outer join VINMFGC on VINMFGC.IRCMP=? and VINMFGC.IRCODE=VEBITEM.MFG_CODE "+
				" left outer join VEBMFGC on VEBMFGC.COMPANY_NBR=? and VEBMFGC.MFG_CODE=VEBITEM.MFG_CODE "+
				" left outer join VEBSTYLE on VEBSTYLE.ITEM_NBR=VEBITEM.ITEM_NBR and VEBSTYLE.COMPANY_NBR=? and VEBSTYLE.CATALOG_ID=?"+
				" where (VEBITEM.STATUS_FLAG = 'A' or VEBITEM.STATUS_FLAG='I') AND VEBITEM.COMPANY_NBR=? and VEBITEM.ITEM_NBR=? "+
				" order by VEBPRIC.UOM_MULTIPLIER asc";
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, methodName+" SQL: "+sql);
			
			int c = 1;
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setString(c++, currentLanguageCode);
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setString(c++, location);
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setString(c++, itemNumber.toUpperCase());
			pstmt.setString(c++, location);
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setString(c++, itemNumber.toUpperCase());
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setInt(c++, Integer.parseInt(websiteId));
			pstmt.setInt(c++, Integer.parseInt(company));
			pstmt.setString(c++, itemNumber.toUpperCase());
			
			ResultSet items_rs = pstmt.executeQuery();
			boolean setDefaultUoM = true;
			while (items_rs.next()) {
				UnitOfMeasure unitOfMeasure = new UnitOfMeasure();
				itemDetail.setItemNumber(items_rs.getString("ITEM_NBR").trim().trim());
				
				String web_description_1 = items_rs.getString("WEB_DESCRIPTION_1_ML");
				if (web_description_1 != null) {
					web_description_1 = web_description_1.trim();
				} else {
					//fallback to non-ml
					web_description_1 = items_rs.getString("WEB_DESCRIPTION_1").trim();
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
				itemDetail.setPageTitle(page_title);
				
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
				itemDetail.setMetaKeywords(meta_keywords);
				
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
				itemDetail.setIsActive(items_rs.getString("STATUS_FLAG").trim());
				itemDetail.setManufacturerItem(items_rs.getString("DESCRIPTION_THREE").trim());
				itemDetail.setStockItem(items_rs.getString("IS_STOCKING").trim());
				itemDetail.setContentKey(items_rs.getString("CONTENT_KEY").trim());
				
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
						big_alt_text = big_alt_text .trim();
					}
				}
				String small_alt_text = items_rs.getString("SMALL_ALT_TEXT_ML");
				if (small_alt_text != null) {
					small_alt_text = small_alt_text.trim();
				} else {
					//fallback to non-ml
					small_alt_text = items_rs.getString("SMALL_ALT_TEXT");
					if (small_alt_text != null) {
						small_alt_text = small_alt_text .trim();
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
				
				Clob short_text = items_rs.getClob("SHORT_TEXT_ML");
				if (short_text == null) {
					short_text = items_rs.getClob("SHORT_TEXT");
				}
				itemDetail.setProductComments(short_text.getSubString(1, (int)short_text.length()));
				
				// Available qty is set in the uomList in the pricing routine
				//itemDetail.setAvailaleQuantity(items_rs.getInt("ONHAND_QTY"));
				
				unitOfMeasure.setUnitMearurePrice(items_rs.getString("OUR_PRICE"));
				unitOfMeasure.setUnitOfMeasure(items_rs.getString("UNIT_OF_MEASURE").trim());
				unitOfMeasure.setMultiplier(items_rs.getDouble("UOM_MULTIPLIER"));
				unitOfMeasure.setSuggestedPrice(items_rs.getString("ITEM_SUGGESTED_PRICE").trim());

				if (setDefaultUoM) {
					itemDetail.setUnitMeasure(items_rs.getString("UNIT_OF_MEASURE").trim()); //This is set because it is used in the field name for adds
					itemDetail.setDefaultUOM(items_rs.getString("UNIT_OF_MEASURE").trim());
					itemDetail.setSuggestedPrice(items_rs.getString("ITEM_SUGGESTED_PRICE").trim());
					//itemDetail.setCartUOM(items_rs.getString("UNIT_OF_MEASURE"));
					setDefaultUoM = false;
				}
				
				unitOfMeasure.setUnitOfMeasureDescription(items_rs.getString("UNIT_OF_MEASURE").trim()+" "+items_rs.getString("UOM_DESCRIPTION").trim());
				itemDetail.setManufacturerItem(items_rs.getString("MFG_STOCK_NO"));
				if (items_rs.getString("IRNAME") != null)
					itemDetail.setManufacturerName(items_rs.getString("IRNAME"));
				else
					itemDetail.setManufacturerName("");
				
				if (items_rs.getString("URL") != null)
				{
					itemDetail.setMfgURL(items_rs.getString("URL").trim());
					itemDetail.setNofollow(items_rs.getString("NOFOLLOW").trim());
				}
				else
				{
					itemDetail.setMfgURL("");
					itemDetail.setNofollow("N");
				}
				
				itemDetail.setItemType(items_rs.getString("ITEM_TYPE"));
				
				if (itemDetail.getItemType().equals("S") && items_rs.getString("STYLE_SUGGESTED_PRICE") != null)
					unitOfMeasure.setUnitMearurePrice(items_rs.getString("STYLE_SUGGESTED_PRICE"));
				
				itemDetail.setReplacementItem(items_rs.getString("REPLACEMENT_ITEM").trim());
				if (itemDetail.getReplacementItem().equalsIgnoreCase(""))
					itemDetail.setReplacement(false);
				else
					itemDetail.setReplacement(true);
				
				itemDetail.setHazardFlag(items_rs.getString("HAZARD").trim());
				itemDetail.setHazardSurchargeFlag(items_rs.getString("HAZARD_SURCHARGE").trim());
				itemDetail.setHeavyFlag(items_rs.getString("HEAVY").trim());
				itemDetail.setOversizedFlag(items_rs.getString("OVERSIZED").trim());
				itemDetail.setRefrigeratedFlag(items_rs.getString("REFRIGERATED").trim());
				itemDetail.setUnavailableFlag(items_rs.getString("UNAVAILABLE").trim().equals("Y")?"Y":"N");
				itemDetail.setAnonPriceFlag(items_rs.getString("ANON_PRICE").trim());
				itemDetail.setItemCategory(String.valueOf(items_rs.getInt("CATEGORY_ID")));
				
				CategoryList cList = new CategoryList(company, websiteId, "", jndiName, providerURL);
				itemDetail.setRelatedCategory1(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY1"), currentLanguageCode));
				itemDetail.setRelatedCategory2(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY2"), currentLanguageCode));
				itemDetail.setRelatedCategory3(cList.getCatInfo(items_rs.getInt("RELATED_CATEGORY3"), currentLanguageCode));
				itemDetail.setItemCategory(String.valueOf(items_rs.getInt("CATEGORY_ID")));
				
				
				// Add element to UnitOfMeasure List
				uomList.addElement(unitOfMeasure);
			}
			
			// Put the uomList into itemDetail
			itemDetail.setUomList(uomList);
			
			// Get the customer item numbers for this item
			itemDetail.setCustomerItem(getCustomerItemNumber(itemDetail.getItemNumber(), accountNumber));
			
			// Get the external links for this item
			//itemDetail.setLinks(getLinks(itemDetail.getItemNumber()));
			
			// Price the item
			/*
			if (s2kPricing.equalsIgnoreCase("Y"))
				getWebPrice(itemDetail, accountNumber, shipToNumber, sessionID);
			else
				getWebPriceVEBPRIC(itemDetail, accountNumber, location);
			*/
			
			// Put the itemDetail into the list as long as we got at least one UOM from the item
			if (itemDetail.getUomList().size() > 0)
				itemList.add(itemDetail);
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
		logger.exiting(className, methodName);
		
		return itemList;
	}
	
	private double getItemCost(String itemNumber, String uomCode, String location)
	{
		// Log the entrance into the method
		String methodName = "getItemCost";
		logger.entering(className, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "", costColumn = "";
		double itemCost = 0.00;
		
		// S2K has a setting that determines which cost the user should see
		S2KCEE_Constants constants = new S2KCEE_Constants(company, jndiName, providerURL, "");
		S2KConstant constant = constants.getS2KConstants("OEIFPG");
		// This really gets the 25th character in the string
		char costType = constant.getAlphaField().charAt(24);

		if (logger.isLoggable(Level.FINEST))
			logger.logp(Level.FINEST, className, methodName, "costType: "+costType);
		
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
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, "getItemCost SQL: "+sql);
			
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
		logger.exiting(className, methodName);
		
		return itemCost;
	}
	
	private Item checkItemRestriction(Item item, String customerNumber, String shiptoNumber)
	{
		// Log the entrance into the method
		String methodName = "checkItemRestriction";
		logger.entering(className, methodName);
		
		// Prepare the inputs
		ArrayList<Item> items = new ArrayList<Item>();
		items.add(item);
		
		// Call the method that does the actual work
		items = new ArrayList<Item>(checkItemRestriction(items, customerNumber, shiptoNumber));
		
		// Log the exit from the method
		logger.exiting(className, methodName);
		
		// Return the output	
		return item;
	}
	
	private List<Item> checkItemRestriction(List<Item> items, String customerNumber, String shiptoNumber)
	{
		// Log the entrance into the method
		String methodName = "checkItemRestriction";
		logger.entering(className, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = (DataSource)context.lookup(jndiName);
			conn = ds.getConnection();
			
			sqlToExecute = "call COINRSQ(?,?,?,?)";
			
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(1, Integer.parseInt(company));
			pstmt.setString(2, customerNumber);
			pstmt.setString(4, shiptoNumber);

			for (Item item : items)
			{
				pstmt.setString(3, item.getItemNumber());
				
				sql = "call COINRSQ("+company+",'"+customerNumber+"','"+item.getItemNumber()+"','"+shiptoNumber+"')";
				if (logger.isLoggable(Level.FINE))
					logger.logp(Level.FINE, className, methodName, "getItemCost SQL: "+sql);
				
				ResultSet rs = pstmt.executeQuery();
				if (rs.next())
					item.setStatus(rs.getString(1).trim());
				else
					item.setStatus("");
				
				if (logger.isLoggable(Level.FINEST))
					logger.logp(Level.FINEST, className, methodName, "item status after COINRSQ call: "+item.getStatus());
			}
		}
		catch (Exception e)
		{
			for (Item item : items)
			{
				item.setStatus("");
			}
			
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
		
		return items;
	}
	
	private boolean isStyleSKUItem(String itemNumber)
	{
		// Log the entrance into the method
		String methodName = "isStyleSKUItem";
		logger.entering(className, methodName);
		
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
		logger.exiting(className, methodName);
		
		return isStyleSKUItem;
	}	
	
	private ItemCustomizationSetting getItemCustomizationSetting(String itemNumber)
	{
		// Log the entrance into the method
		String methodName = "getItemCustomizationSetting";
		logger.entering(className, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		ItemCustomizationSetting itemCustSetting = new ItemCustomizationSetting();	
			
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();		
			
			sql = "CALL EBPERSQ("+company+","+websiteId+",'"+itemNumber+"')";
			sqlToExecute = "CALL EBPERSQ(?,?,?)";
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, "getItemCustomizationSetting SQL: "+sql);
			
			int i = 1;
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(i++, Integer.parseInt(company));
			pstmt.setInt(i++, Integer.parseInt(websiteId));
			pstmt.setString(i++, itemNumber);
			
			ResultSet rs = pstmt.executeQuery();
			if (rs.next())
			{			
				itemCustSetting.setTemplateName(rs.getString("rs_Template").trim());
				itemCustSetting.setSizing(rs.getString("rs_Sizing").trim());
				itemCustSetting.setPersonalization(rs.getString("rs_Prsnlztn").trim());
				itemCustSetting.setQuantityOne(rs.getString("rs_QtyOne").trim());
				itemCustSetting.setClassRequired(rs.getString("rs_ClassReq").trim());
				
				if (logger.isLoggable(Level.FINEST))
				{
					logger.logp(Level.FINEST, className, methodName, "Template Name: "+itemCustSetting.getTemplateName());
					logger.logp(Level.FINEST, className, methodName, "Sizing: "+itemCustSetting.getSizing());
					logger.logp(Level.FINEST, className, methodName, "Personalization: "+itemCustSetting.getPersonalization());
					logger.logp(Level.FINEST, className, methodName, "Quantity One: "+itemCustSetting.getQuantityOne());
					logger.logp(Level.FINEST, className, methodName, "Class Required: "+itemCustSetting.getClassRequired());
				}			
			}
		}
		catch (Exception e){
			itemCustSetting = new ItemCustomizationSetting();
			
			e.printStackTrace();
		}
		finally{
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
		logger.exiting(className, methodName);
		
		return itemCustSetting;
	}	
	
	/*private ArrayList<ItemSizingTemplate> getItemSizingTemplate(String itemNumber, String currentLanguageCode)
	{
		// Log the entrance into the method
		String methodName = "getItemSizingTemplate";
		logger.entering(className, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		ArrayList<ItemSizingTemplate> itemSizingTemplate = new ArrayList<ItemSizingTemplate>();	
			
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();		
			
			sql = "CALL EBSZTPLQ("+company+","+websiteId+",'"+itemNumber+"',"+currentLanguageCode+"')";
			sqlToExecute = "CALL EBSZTPLQ(?,?,?,?)";
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, "getItemSizingTemplate SQL: "+sql);
			
			int i = 1;
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(i++, Integer.parseInt(company));
			pstmt.setInt(i++, Integer.parseInt(websiteId));
			pstmt.setString(i++, itemNumber);
			pstmt.setString(i++, currentLanguageCode);
			
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{			
				ItemSizingTemplate sizingTemplate = new ItemSizingTemplate();
				sizingTemplate.setSizeId(rs.getInt("rs_SizeID"));
				sizingTemplate.setSizeDescription(rs.getString("rs_SizeDesc").trim());
				sizingTemplate.setDigits(rs.getInt("rs_Digits"));
				sizingTemplate.setDecimal(rs.getInt("rs_Decimals"));
				sizingTemplate.setRequired(rs.getString("rs_Required").trim());
				sizingTemplate.setSortOrder(rs.getInt("rs_SortOrder"));
				sizingTemplate.setMinValue(rs.getDouble("rs_MinValue"));
				sizingTemplate.setMaxValue(rs.getDouble("rs_MaxValue"));
				
				if (logger.isLoggable(Level.FINEST))
				{
					logger.logp(Level.FINEST, className, methodName, "Size Id: "+sizingTemplate.getSizeId());
					logger.logp(Level.FINEST, className, methodName, "Size Description: "+sizingTemplate.getSizeDescription());
					logger.logp(Level.FINEST, className, methodName, "Digits: "+sizingTemplate.getDigits());
					logger.logp(Level.FINEST, className, methodName, "Decimals: "+sizingTemplate.getDecimal());
					logger.logp(Level.FINEST, className, methodName, "Required: "+sizingTemplate.getRequired());
					logger.logp(Level.FINEST, className, methodName, "Sort Order: "+sizingTemplate.getSortOrder());
					logger.logp(Level.FINEST, className, methodName, "Min Value: "+sizingTemplate.getMinValue());
					logger.logp(Level.FINEST, className, methodName, "Max Value: "+sizingTemplate.getMaxValue());
				}	
				
				itemSizingTemplate.add(sizingTemplate);
			}
			
			Collections.sort(itemSizingTemplate);
		}
		catch (Exception e){
			itemSizingTemplate = new ArrayList<ItemSizingTemplate>();
			
			e.printStackTrace();
		}
		finally{
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
		logger.exiting(className, methodName);
		
		return itemSizingTemplate;
	}	
	
	private ArrayList<ItemPersonalizationTemplate> getItemPersonalizationTemplate(String itemNumber, String customerNumber, String templateClass, String currentLanguageCode)
	{
		// Log the entrance into the method
		String methodName = "getItemPersonalizationTemplate";
		logger.entering(className, methodName);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "", sqlToExecute = "";
		ArrayList<ItemPersonalizationTemplate> itemPersonalizationTemplate = new ArrayList<ItemPersonalizationTemplate>();	
			
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			Context context = new InitialContext(env);
			DataSource ds = ((DataSource)context.lookup(jndiName));
			conn = ds.getConnection();		
			
			sql = "CALL EBPRTPLQ("+company+","+websiteId+",'"+itemNumber+"','"+customerNumber+"','"+templateClass.toUpperCase()+"'"+currentLanguageCode+"')";
			sqlToExecute = "CALL EBPRTPLQ(?,?,?,?,?,?)";
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, "getItemPersonalizationTemplate SQL: "+sql);
			
			int i = 1;
			pstmt = conn.prepareStatement(sqlToExecute);
			pstmt.setInt(i++, Integer.parseInt(company));
			pstmt.setInt(i++, Integer.parseInt(websiteId));
			pstmt.setString(i++, itemNumber);
			pstmt.setString(i++, customerNumber);
			pstmt.setString(i++, templateClass.toUpperCase());
			pstmt.setString(i++, currentLanguageCode);
			
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{			
				ItemPersonalizationTemplate personalizationTemplate = new ItemPersonalizationTemplate();
				personalizationTemplate.setTemplateName(rs.getString("rs_Template").trim());
				personalizationTemplate.setTemplateClass(rs.getString("rs_TmpltCls").trim());
				personalizationTemplate.setPersonalizationId(rs.getInt("rs_PrsnlzID"));
				personalizationTemplate.setPersonalizationDescription(rs.getString("rs_PrsnlzVal").trim());
				personalizationTemplate.setRequired(rs.getString("rs_Required").trim());
				personalizationTemplate.setMaxLength(rs.getInt("rs_MaxLength"));
				personalizationTemplate.setDefaultValue(rs.getString("rs_DftValue").trim());
				personalizationTemplate.setDefaultOverrideAllowed(rs.getString("rs_DftOvrAlw").trim());
				personalizationTemplate.setSortOrder(rs.getInt("rs_SortOrder"));
				personalizationTemplate.setErrorCode(rs.getString("rs_ErrCode").trim());
				personalizationTemplate.setMessage(rs.getString("rs_Message").trim());
				
				if (logger.isLoggable(Level.FINEST))
				{
					logger.logp(Level.FINEST, className, methodName, "Template Name: "+personalizationTemplate.getTemplateName());
					logger.logp(Level.FINEST, className, methodName, "Template Class: "+personalizationTemplate.getTemplateClass());
					logger.logp(Level.FINEST, className, methodName, "Personalization Id: "+personalizationTemplate.getPersonalizationId());
					logger.logp(Level.FINEST, className, methodName, "Personalization Description: "+personalizationTemplate.getPersonalizationDescription());
					logger.logp(Level.FINEST, className, methodName, "Required: "+personalizationTemplate.getRequired());
					logger.logp(Level.FINEST, className, methodName, "Max Length: "+personalizationTemplate.getMaxLength());
					logger.logp(Level.FINEST, className, methodName, "Default Value: "+personalizationTemplate.getDefaultValue());
					logger.logp(Level.FINEST, className, methodName, "Default Override Allowed: "+personalizationTemplate.getDefaultOverrideAllowed());					
					logger.logp(Level.FINEST, className, methodName, "Sort Order: "+personalizationTemplate.getSortOrder());
					logger.logp(Level.FINEST, className, methodName, "Error Code: "+personalizationTemplate.getErrorCode());
					logger.logp(Level.FINEST, className, methodName, "Message: "+personalizationTemplate.getMessage());					
				}	
				
				itemPersonalizationTemplate.add(personalizationTemplate);
			}
			
			Collections.sort(itemPersonalizationTemplate);
		}
		catch (Exception e){
			itemPersonalizationTemplate = new ArrayList<ItemPersonalizationTemplate>();	
			
			e.printStackTrace();
		}
		finally{
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
		logger.exiting(className, methodName);
		
		return itemPersonalizationTemplate;
	}*/
	
	private String getxRefCustomerNumber(String accountNumber)
	{
		// Log the entrance into the method
		String methodName = "getxRefCustomerNumber";
		logger.entering(className, methodName);
				
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
			
			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, methodName, "getxRefCustomerNumber SQL: "+sql);
			
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
		logger.exiting(className, methodName);
		
		return xRefCustomerNum;
	}	
}