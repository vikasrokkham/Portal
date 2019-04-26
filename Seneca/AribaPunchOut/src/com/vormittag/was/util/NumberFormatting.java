package com.vormittag.was.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NumberFormatting {
	public final static String DECIMAL = "DECIMAL";
	public final static String CURRENCY = "CURRENCY";
	public final static String PERCENTAGE = "PERCENTAGE";
	public final static String NUMBER = "NUMBER";
	public final static String DATE = "DATE";
	public final static String S2KDATE = "S2KDATE";
	public final static String S2KDATE2 = "S2KDATE2";
	public final static String VEBFROMDATE = "VEBFROMDATE";
	public final static String VEBTODATE = "VEBTODATE";
	public final static String VEBFOURFROMDATE = "VEBFOURFROMDATE";
	public final static String SENECAFROMDATE = "SENECAFROMDATE";
	public final static String DATETODOJO = "DATETODOJO";
	public final static String S2KTODOJO = "S2KTODOJO";
	public final static String DECIMALPRECISIONPRICING = "DECIMALPRECISIONPRICING";

	private static final String CURRENCY_FORMAT = "########0.00";
	private static final String PERCENTAGE_FORMAT = "0.00%";
	private static final String DECIMAL_FORMAT = "#,###,##0.00";
	private static final String NUMBER_FORMAT = "#,###,###";
	private static final String DATE_FORMAT = "MM/dd/yyyy";
	private static final String DATE_FORMAT_CONTRACT = "MM/dd/yy";
	private static final String S2KDATE_FORMAT = "yyyyMMdd";
	private static final String VEBDATE_FORMAT = "yyyy-MM-dd";
	private String DECIMALPRECISIONPRICING_FORMAT = "########0.0";	//This format is dynamic and allows N-number of decimal places
	private int numberOfDecimalPlaces = 1; //Default decimal place for the DECIMALPRECISIONPRICING_FORMAT

	private static DecimalFormat currencyFormat =
		new DecimalFormat(CURRENCY_FORMAT);
	private static DecimalFormat percentageFormat =
		new DecimalFormat(PERCENTAGE_FORMAT);
	private static DecimalFormat decimalFormat =
		new DecimalFormat(DECIMAL_FORMAT);
	private static DecimalFormat numberFormat =
		new DecimalFormat(NUMBER_FORMAT);
	private static SimpleDateFormat dateFormat =
			new SimpleDateFormat(DATE_FORMAT);
	private static SimpleDateFormat dateFormatContract =
		new SimpleDateFormat(DATE_FORMAT_CONTRACT);
	private static SimpleDateFormat s2kdateFormat =
		new SimpleDateFormat(S2KDATE_FORMAT);
	private static SimpleDateFormat vebdateFormat =
		new SimpleDateFormat(VEBDATE_FORMAT);
	
	public void setDecimalPrecision(int tmp){
		this.numberOfDecimalPlaces = tmp;
	}
	
	public int getDecimalPrecision(){
		return this.numberOfDecimalPlaces;
	}	
	
	public String format(String value, String expression) {

		String result = null;

		if (value != null) {	
				if(value==null || value.length()==0)
					value = "0.00";

				int len = value.length();

				//skip numbers with trailing percentages (two values in CICSSNA model
				if (value.substring(len-1).equalsIgnoreCase("%"))
					return value;

				if (expression.equals(CURRENCY)) {
					Double d = new Double(value);
					try {

						// use double to preserve decimal values
						result = currencyFormat.format(d.doubleValue());

					} //catch (Exception ex) {System.out.println(ex.toString());}
					catch (Exception e)
					{
						e.printStackTrace();
					}

				} else if(expression.equals(DECIMALPRECISIONPRICING)){
					Double d = new Double(value);
					try {
						//Default '1' decimal-place
						DecimalFormat decimalPrecisionFormat = new DecimalFormat(DECIMALPRECISIONPRICING_FORMAT);
						
						if(this.getDecimalPrecision() > 1){
							String baseDecimalFormat = DECIMALPRECISIONPRICING_FORMAT;
							for(int i=1; i<this.getDecimalPrecision(); i++){
								baseDecimalFormat = baseDecimalFormat + "0";
							}
							decimalPrecisionFormat = new DecimalFormat(baseDecimalFormat);							
						}else{}
						
						result = decimalPrecisionFormat.format(d.doubleValue());
						
					} //catch (Exception ex) {System.out.println(ex.toString());}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					
				} else if (expression.equals(PERCENTAGE)) {
					Double d = new Double(value);
					try {

					//cat.debug("\n*** percentage to format= "+ d);
					if (d != null && d.compareTo(new Double (0.0))!=0) // skip for empty rows
						result = percentageFormat.format((d.doubleValue()/100));


					} 
					catch (Exception e)
					{
						e.printStackTrace();
					}

				} else if (expression.equals(DECIMAL)) {
					Double d = new Double(value);
					try {

						result = decimalFormat.format(d.doubleValue());

					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

				}

				else if (expression.equals(NUMBER)) {
					Double d = new Double(value);
					try {
						//cat.debug ("D = " + d );

						// skip for empty rows
						if (d != null && d.compareTo(new Double (0.0))!=0)
							result = numberFormat.format(d);

					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

				}
						
				else if (expression.equals(DATE)) {
				try {
				
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				
				if (value.equalsIgnoreCase("0"))
					return "";
				Date dValue = sdf.parse(value);
				result = dateFormat.format(dValue);
				
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

				}	
				
				else if (expression.equals(VEBFROMDATE)) {
					try {
					//Using Ntive driver 
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");	
					//Using JT400	
					//SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
					
					if (value.equalsIgnoreCase("0"))
						return "";
					Date dValue = sdf.parse(value);
					result = dateFormat.format(dValue);
					
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

					}
				
				else if (expression.equals(S2KDATE)) { // yyyy-MM-dd to yyyyMMdd
					try {
					
					//SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
									
					if (value.equalsIgnoreCase("0"))
						return "0";
					Date dValue = sdf.parse(value);
					result = s2kdateFormat.format(dValue);
					
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

					}	
				
				else if (expression.equals(S2KDATE2)) { // MM/dd/yyyy to yyyyMMdd
					try {
					
					SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
									
					if (value.equalsIgnoreCase("0"))
						return "0";
					Date dValue = sdf.parse(value);
					result = s2kdateFormat.format(dValue);
					
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

					}
				
				else if (expression.equals(VEBTODATE)) {
					try {
					
					SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
					
					if (value.equalsIgnoreCase("0"))
						return "0";
					Date dValue = sdf.parse(value);
					result = vebdateFormat.format(dValue);
					
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

					}		
				
				else if (expression.equals(SENECAFROMDATE)) {
					try {
					
					SimpleDateFormat sdf = new SimpleDateFormat("MMddyy");
					if (value.equalsIgnoreCase("0"))
						return "0";
					Date dValue = sdf.parse(value);
					result = dateFormatContract.format(dValue);
					
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

					}

				else if (expression.equals(DATETODOJO)) {
					try {
					
					SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
					
					if (value.equalsIgnoreCase("0"))
						return "0";
					Date dValue = sdf.parse(value);
					result = vebdateFormat.format(dValue);
					
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

					}
				
				else if (expression.equals(S2KTODOJO)) {
					try {
					
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
					
					if (value.equalsIgnoreCase("0"))
						return "0";
					Date dValue = sdf.parse(value);
					result = vebdateFormat.format(dValue);
					
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

					}
			} else {

				result = value;

			}

		return result;
	}
}
