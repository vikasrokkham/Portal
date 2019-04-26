package com.vormittag.was.util;

import java.util.HashMap;
import java.util.Vector;

public class Category implements Comparable<Object> {
	/**
	 * For deprecated fields and accessors, eventually migrate to use languageSpecificData.
	 */
	
	//Keyed by LanguageCode
	private HashMap<String, CategoryText> languageSpecificData = new HashMap<String, CategoryText>();
	private boolean isNewCategory = true;
	private String Exclude;
	private String parentCategoryId = "";
	private int Catg;
	private String Quickcode;
	private String Dsp_comments;
	

	/**
	 * Used in Category Maintenance for UI only.
	 */
	private String Catg_desc;

	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	private String image1;
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	private String image2;

	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	private String pageTitle = "";
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	private String metaData;
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	private String metaDescription;
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	private String altText1;
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	private String altText2;
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	private String comments;
	private String parentCode;
	private String parentQuickCode;
	private boolean isTerminal = true;
	private boolean hasAssignedItems = false;
	private int sequenceNumber = 190;
	private int relatedCategory1 = -1;
	private int relatedCategory2 = -1;
	private int relatedCategory3 = -1;
	private int productType = -1;
	private int categoryLevel;
	private Vector<Category> subCategories = new Vector<Category>();

	
	private String contentKey = "";
	private boolean primaryCategory = false;
	private String menuImage = "";
	private String menuContentKey = "";
	private String fullPath = "";

	//	Used for Collection.sort on vector of item objects
	public int compareTo(Object obj) {
		if (obj instanceof Category) {
			Category i = (Category)obj;
			int seqNumber = sequenceNumber;
			return seqNumber - i.getSequenceNumber();
		} else {
			return 0;
		}
	}

	public boolean equals(Object o) {
		if (o instanceof Category) {
			Category c = (Category)o;
			return this.Catg == c.getCatg();
		} else {
			return false;
		}
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}
	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
	public boolean hasAssignedItems() {
		return hasAssignedItems;
	}
	public void setHasAssignedItems(boolean hasAssignedItems) {
		this.hasAssignedItems = hasAssignedItems;
	}
	public boolean isTerminal() {
		return isTerminal;
	}
	public void setIsTerminal(boolean isTerminal) {
		this.isTerminal = isTerminal;
	}
	public Category() {
		super();
	}
	public String getParentQuickCode() {
		return parentQuickCode;
	}
	public void setParentQuickCode(String parentQuickCode) {
		this.parentQuickCode = parentQuickCode;
	}
	public String getParentCode() {
		return parentCode;
	}
	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}
	public int getCatg() {
		return Catg;
	}
	public void setCatg(int catg) {
		Catg = catg;
	}
	public String getDsp_comments() {
		return Dsp_comments;
	}
	public void setDsp_comments(String dsp_comments) {
		this.Dsp_comments = dsp_comments;
	}
	public String getExclude() {
		return Exclude;
	}
	public void setExclude(String exclude) {
		Exclude = exclude;
	}
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	public String getAltText1() {
		return altText1;
	}
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	public void setAltText1(String altText1) {
		this.altText1 = altText1;
	}
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	public String getAltText2() {
		return altText2;
	}
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	public void setAltText2(String altText2) {
		this.altText2 = altText2;
	}
	/**
	 * Used in Category Maintenance for UI only.
	 */
	public String getCatg_desc() {
		return Catg_desc;
	}
	/**
	 * Used in Category Maintenance for UI only.
	 */
	public void setCatg_desc(String catg_desc) {
		Catg_desc = catg_desc;
	}
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	public String getComments() {
		return comments;
	}
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	public void setComments(String comments) {
		this.comments = comments;
	}
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	public String getImage1() {
		return image1;
	}
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	public void setImage1(String image1) {
		this.image1 = image1;
	}
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	public String getImage2() {
		return image2;
	}
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	public void setImage2(String image2) {
		this.image2 = image2;
	}
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	public String getPageTitle() {
		return pageTitle;
	}
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	public String getMetaData() {
		return metaData;
	}
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	public void setMetaData(String metaData) {
		this.metaData = metaData;
	}
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	public String getMetaDescription() {
		return metaDescription;
	}
	/***
	 * @deprecated - replace with Multi-Lingual
	 * */
	public void setMetaDescription(String metaDescription) {
		this.metaDescription = metaDescription;
	}
	public String getQuickcode() {
		return Quickcode;
	}
	public void setQuickcode(String quickcode) {
		Quickcode = quickcode;
	}
	public int getRelatedCategory1() {
		return relatedCategory1;
	}
	public void setRelatedCategory1(int relatedCategory1) {
		this.relatedCategory1 = relatedCategory1;
	}
	public int getRelatedCategory2() {
		return relatedCategory2;
	}
	public void setRelatedCategory2(int relatedCategory2) {
		this.relatedCategory2 = relatedCategory2;
	}
	public int getRelatedCategory3() {
		return relatedCategory3;
	}
	public void setRelatedCategory3(int relatedCategory3) {
		this.relatedCategory3 = relatedCategory3;
	}
	public int getProductType() {
		return productType;
	}
	public void setProductType(int productType) {
		this.productType = productType;
	}
	public int getCategoryLevel() {
		return categoryLevel;
	}
	public void setCategoryLevel(int categoryLevel) {
		this.categoryLevel = categoryLevel;
	}
	public Vector<Category> getSubCategories() {
		return subCategories;
	}
	public void setSubCategories(Vector<Category> subCategories) {
		this.subCategories = subCategories;
	}
	public String getContentKey() {
		return contentKey;
	}
	public void setContentKey(String contentKey) {
		this.contentKey = contentKey;
	}
	public boolean isPrimaryCategory() {
		return primaryCategory;
	}
	public void setPrimaryCategory(boolean primaryCategory) {
		this.primaryCategory = primaryCategory;
	}
	public String getMenuImage() {
		return menuImage;
	}
	public void setMenuImage(String menuImage) {
		this.menuImage = menuImage;
	}
	public String getMenuContentKey() {
		return menuContentKey;
	}
	public void setMenuContentKey(String menuContentKey) {
		this.menuContentKey = menuContentKey;
	}
	public boolean isHasAssignedItems() {
		return hasAssignedItems;
	}
	public void setTerminal(boolean isTerminal) {
		this.isTerminal = isTerminal;
	}
	public String getFullPath(){
		return fullPath;
	}
	public void setFullPath(String fullPath){
		this.fullPath = fullPath;
	}
	public HashMap<String, CategoryText> getLanguageSpecificData() {
		return languageSpecificData;
	}
	public void setLanguageSpecificData(HashMap<String, CategoryText> languageSpecificData) {
		this.languageSpecificData = languageSpecificData;
	}
	public String getParentCategoryId() {
		return parentCategoryId;
	}
	public void setParentCategoryId(String parentCategoryId) {
		this.parentCategoryId = parentCategoryId;
	}
	public boolean isNewCategory() {
		return isNewCategory;
	}
	public void setNewCategory(boolean isNewCategory) {
		this.isNewCategory = isNewCategory;
	}
}