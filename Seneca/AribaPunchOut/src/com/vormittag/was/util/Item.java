/*
 * Created on Aug 23, 2006
 */
package com.vormittag.was.util;

import java.lang.Comparable;
import java.util.ArrayList;
import java.util.Vector;
import java.util.HashMap;

public class Item implements Comparable<Object>, java.io.Serializable
{
	static final long serialVersionUID=111111;
	
	private String overrideDesc1 = "";
	private String overrideDesc2 = "";
	private String description1 = "";
	private String description2 = "";
	private String description3 = "";
	private String contentKey = "";
	private String pageTitle = "";
	private String metaKeywords = "";
	private String metaDescription = "";
	private String fullImage = "";
	private String fullImageAltText = "";
	private String thumbImage = "";
	private String thumbImageAltText = "";
	private ArrayList<ItemImage> itemImages = new ArrayList<ItemImage>(); 
	private String extDescription = "";
	private String itemNumber = "";
	private String priceCode = "";
	private String price = "0.00";
	private String discount = "0.00";
	private String webDiscountFlag;
	private int availaleQuantity = 0;
	private String unitMeasure = "";
	private String manufacturerName = "";
	private String manufacturerItem = "";
	private String errorFlag;
	private int qtyInCart;
	private int itemLineNumber;
	private int parentLineNumber = 0;
	private int buildToQuantity;
	private String processFunction;  //This is used to remove or add for insertTheItems
	private int sequenceNumber;
	private Vector<UnitOfMeasure> uomList = new Vector<UnitOfMeasure>();  //This is used to store the available UOM for each item in the Quick Order 
	private String quantityForQuickOrder;
	private boolean isReplacement = false;
	private String originalItemNumber = "";
	private String unitOfMeasureDescription = "";
	private String isStockItem = "Y";
	private String contractSource = "";
	private String contractNumber = "";
	private String contractTier = "";
	private String contractExpiration = "";
	private String mfgStockNumber = "";
	private String mfgCode = "";
	private String mfgName = "";
	private String latex = "";
	private String hazardous = "";
	private String defaultUOM = "";
	private String customerItem = "";
	private String cartUOM = "";
	private String status = "";
	private String stockingUM;
	private String stockingPiece;
	private String stockingPackaging;
	private String sellingUM;
	private String sellingPiece;
	private String sellingPackaging;
	private String purchasingUM;
	private String purchasingPiece;
	private String purchasingPackaging;
	private String otherUM;
	private String otherPiece;
	private String otherPackaging;
	private String cubicMeasure;
	private String masterPackQuantity;
	private String innerPackQuantity;
	private String weight;
	private String length;
	private String height;
	private String width;
	private String company="";
	private String division;
	private String divName;
	private String inventoryClass;
	private String className;
	private Vector<Object> location;
	private String replacementItem;
	private String preferredVendor;
	private String preferredVendorName;
	private String vendorName;
	private String isActive = "";
	private String type = "";
	private Vector<Object> quantityLocations = new Vector<Object>();
	private String qOnHandSum = "";
	private String qCommittedSum = "";
	private String qAvailableSum = "";
	private String currency;
	private String priceMatrix;
	private String suggestedPrice = "0.00";
	private String priceLevel01;
	private String priceLevel02;
	private String priceLevel03;
	private String priceLevel04;
	private String priceLevel05;
	private String priceLevel06;
	private String priceLevel07;
	private String priceLevel08;
	private String priceLevel09;
	private String priceLevel10;
	private Vector<Object> qtyBreaksList = new Vector<Object>();
	private String upcCode = "";
	private RelatedCategory relatedCategory1 = new RelatedCategory();
	private RelatedCategory relatedCategory2 = new RelatedCategory();
	private RelatedCategory relatedCategory3 = new RelatedCategory();
	private String itemCategory = "";
	private String manufacturerIcon = "";
	private ArrayList<Object> categoryIDs = new ArrayList<Object>();
	private int itemQuantity = 0;
	private String productComments = "";
	private String deaFlag = "N";
	private ArrayList<Object> externalFiles = new ArrayList<Object>();
	private String hazardFlag = "";
	private String heavyFlag = "";
	private String hazardSurchargeFlag = "";
	private String oversizedFlag = "";
	private String refrigeratedFlag = "";
	private String anonPriceFlag = "";
	private ArrayList<ItemLinks> links = new ArrayList<ItemLinks>();
	private Vector<Object> promoNumbers = new Vector<Object>(); 
	private double pointValue = 0.00;
	private double getQty = 0.00;
	private int cartPromoNumber = 0;
	private String cartPromoItemType = "";
	private String unavailableFlag = "N";
	private String newItemFlag = "N";
	private String featuredItemFlag = "N";
	private String historyItem = "N";
	private String altLocation = "N";
	private Vector<OrderGuide> orderGuides = new Vector<OrderGuide>();
	private HashMap<Double, String> priceBreaks = new HashMap<Double, String>();
	private HashMap<String, String> minimumMultiple = new HashMap<String, String>();
	private String qoQtyMessage = "";
	private HashMap<String, Object> itemAttributes = new HashMap<String, Object>();
	private String lineItemComments = "";
	private Integer matchedSearchCount = 0;
	private String itemType = "";
	private String styleItemNumber = "";
	private ArrayList<SelectedVariant> selectedStyleVariants = new ArrayList<SelectedVariant>();
	private String styleDspFormat = "D";
	private boolean qtyOneItem = false;
	private boolean customizable = false;
	private ArrayList<ItemPersonalization> itemPersonalization = new ArrayList<ItemPersonalization>();
	private ArrayList<ItemSizing> itemSizing = new ArrayList<ItemSizing>();
	private ItemCustomizationSetting itemCustomizationSetting = new ItemCustomizationSetting();
	private String mfgURL = "";
	private String nofollow = "N";
	private String cxmlUom = "EA";
	
	// Used for Collection.sort on vector of item objects
	public int compareTo(Object obj)
	{
		Item i = (Item)obj;
		int seqNumber = sequenceNumber;
		return seqNumber - i.getSequenceNumber();
	}
	
	public boolean equals(Object o)
	{
		if (o instanceof Item)
		{
			Item i = (Item)o;
			return i.getItemNumber().equals(itemNumber);
		}
		else
			return false;
	}

	public int hashCode()
	{
		return super.hashCode();
	}

	public String getUpcCode() {
		return upcCode;
	}
	public void setUpcCode(String upcCode) {
		this.upcCode = upcCode;
	}
	public String getCompany() {
		return company;
	}
	public void setCompany(String company) {
		this.company = company;
	}
	public String getCubicMeasure() {
		return cubicMeasure;
	}
	public void setCubicMeasure(String cubicMeasure) {
		this.cubicMeasure = cubicMeasure;
	}
	public String getHeight() {
		return height;
	}
	public void setHeight(String height) {
		this.height = height;
	}
	public String getInnerPackQuantity() {
		return innerPackQuantity;
	}
	public void setInnerPackQuantity(String innerPackQuantity) {
		this.innerPackQuantity = innerPackQuantity;
	}
	public String getLength() {
		return length;
	}
	public void setLength(String length) {
		this.length = length;
	}
	public String getMasterPackQuantity() {
		return masterPackQuantity;
	}
	public void setMasterPackQuantity(String masterPackQuantity) {
		this.masterPackQuantity = masterPackQuantity;
	}
	public String getWeight() {
		return weight;
	}
	public void setWeight(String weight) {
		this.weight = weight;
	}
	public String getWidth() {
		return width;
	}
	public void setWidth(String width) {
		this.width = width;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getCartUOM() {
		return cartUOM;
	}
	public void setCartUOM(String cartUOM) {
		this.cartUOM = cartUOM;
	}
	public String getCustomerItem() {
		return customerItem;
	}
	public void setCustomerItem(String customerItem) {
		this.customerItem = customerItem;
	}
	public String getDefaultUOM() {
		return defaultUOM;
	}
	public void setDefaultUOM(String defaultUOM) {
		this.defaultUOM = defaultUOM;
	}
	public String getHazardous() {
		return hazardous;
	}
	public void setHazardous(String hazardous) {
		this.hazardous = hazardous;
	}
	public String getLatex() {
		return latex;
	}
	public void setLatex(String latex) {
		this.latex = latex;
	}
	public String getMfgStockNumber() {
		return mfgStockNumber;
	}
	public void setMfgStockNumber(String mfgStockNumber) {
		this.mfgStockNumber = mfgStockNumber;
	}
	public String getContractExpiration() {
		return contractExpiration;
	}
	public void setContractExpiration(String contractExpiration) {
		this.contractExpiration = contractExpiration;
	}
	public String getContractNumber() {
		return contractNumber;
	}
	public void setContractNumber(String contractNumber) {
		this.contractNumber = contractNumber;
	}
	public String getContractSource() {
		return contractSource;
	}
	public void setContractSource(String contractSource) {
		this.contractSource = contractSource;
	}
	public String getContractTier() {
		return contractTier;
	}
	public void setContractTier(String contractTier) {
		this.contractTier = contractTier;
	}
	public String getIsStockItem() {
		return isStockItem;
	}
	public void setIsStockItem(String isStockItem) {
		this.isStockItem = isStockItem;
	}
	public String isStockItem() {
		return isStockItem;
	}
	public void setStockItem(String isStockItem) {
		this.isStockItem = isStockItem;
	}
	public String getUnitOfMeasureDescription() {
		return unitOfMeasureDescription;
	}
	public void setUnitOfMeasureDescription(String unitOfMeasureDescription) {
		this.unitOfMeasureDescription = unitOfMeasureDescription;
	}
	public String getOriginalItemNumber() {
		return originalItemNumber;
	}
	public void setOriginalItemNumber(String originalItemNumber) {
		this.originalItemNumber = originalItemNumber;
	}
	public boolean isReplacement() {
		return isReplacement;
	}
	public void setReplacement(boolean isReplacement) {
		this.isReplacement = isReplacement;
	}
	public String getManufacturerItem() {
		return manufacturerItem;
	}
	public void setManufacturerItem(String manufacturerItem) {
		this.manufacturerItem = manufacturerItem;
	}
	public String getManufacturerName() {
		return manufacturerName;
	}
	public void setManufacturerName(String manufacturerName) {
		this.manufacturerName = manufacturerName;
	}
	public String getQuantityForQuickOrder() {
		return quantityForQuickOrder;
	}
	public void setQuantityForQuickOrder(String quantityForQuickOrder) {
		this.quantityForQuickOrder = quantityForQuickOrder;
	}
	public int getSequenceNumber() {
		return sequenceNumber;
	}
	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
	public Vector<UnitOfMeasure> getUomList()
	{
		return uomList;
	}
	public void setUomList(Vector<UnitOfMeasure> uomList)
	{
		this.uomList = uomList;
	}
	public String getProcessFunction() {
		return processFunction;
	}
	public void setProcessFunction(String processFunction) {
		this.processFunction = processFunction;
	}
	public int getBuildToQuantity() {
		return buildToQuantity;
	}
	public void setBuildToQuantity(int buildToQuantity) {
		this.buildToQuantity = buildToQuantity;
	}
	public String getUnitMeasure() {
		return unitMeasure;
	}
	public void setUnitMeasure(String unitMeasure) {
		this.unitMeasure = unitMeasure;
	}
	/**
	 * @return Returns the itemLineNumber.
	 */
	public int getItemLineNumber() {
		return itemLineNumber;
	}
	/**
	 * @param itemLineNumber The itemLineNumber to set.
	 */
	public void setItemLineNumber(int itemLineNumber) {
		this.itemLineNumber = itemLineNumber;
	}
	/**
	 * @return Returns the parentLineNumber.
	 */
	public int getParentLineNumber() {
		return parentLineNumber;
	}
	/**
	 * @param parentLineNumber The parentLineNumber to set.
	 */
	public void setParentLineNumber(int parentLineNumber) {
		this.parentLineNumber = parentLineNumber;
	}	
	/**
	 * @return Returns the qtyInCart.
	 */
	public int getQtyInCart() {
		return qtyInCart;
	}
	/**
	 * @param qtyInCart The qtyInCart to set.
	 */
	public void setQtyInCart(int qtyInCart) {
		this.qtyInCart = qtyInCart;
	}
	/**
	 * @return Returns the availaleQuantity.
	 */
	public int getAvailaleQuantity() {
		return availaleQuantity;
	}
	/**
	 * @param availaleQuantity The availaleQuantity to set.
	 */
	public void setAvailaleQuantity(int availaleQuantity) {
		this.availaleQuantity = availaleQuantity;
	}
	/**
	 * @return Returns the discount.
	 */
	public String getDiscount() {
		return discount;
	}
	/**
	 * @param discount The discount to set.
	 */
	public void setDiscount(String discount) {
		this.discount = discount;
	}
	/**
	 * @return Returns the errorFlag.
	 */
	public String getErrorFlag() {
		return errorFlag;
	}
	/**
	 * @param errorFlag The errorFlag to set.
	 */
	public void setErrorFlag(String errorFlag) {
		this.errorFlag = errorFlag;
	}
	/**
	 * @return Returns the price.
	 */
	public String getPrice() {
		return price;
	}
	/**
	 * @param price The price to set.
	 */
	public void setPrice(String price) {
		this.price = price;
	}
	/**
	 * @return Returns the priceCode.
	 */
	public String getPriceCode() {
		return priceCode;
	}
	/**
	 * @param priceCode The priceCode to set.
	 */
	public void setPriceCode(String priceCode) {
		this.priceCode = priceCode;
	}
	/**
	 * @return Returns the webDiscountFlag.
	 */
	public String getWebDiscountFlag() {
		return webDiscountFlag;
	}
	/**
	 * @param webDiscountFlag The webDiscountFlag to set.
	 */
	public void setWebDiscountFlag(String webDiscountFlag) {
		this.webDiscountFlag = webDiscountFlag;
	}
	/**
	 * @return Returns the itemNumber.
	 */
	public String getItemNumber() {
		return itemNumber;
	}
	/**
	 * @param itemNumber The itemNumber to set.
	 */
	public void setItemNumber(String itemNumber) {
		this.itemNumber = itemNumber;
	}
	/**
	 * @return Returns the description1.
	 */
	public String getDescription1() {
		return description1;
	}
	/**
	 * @param description1 The description1 to set.
	 */
	public void setDescription1(String description1) {
		this.description1 = description1;
	}
	/**
	 * @return Returns the description2.
	 */
	public String getDescription2() {
		return description2;
	}
	/**
	 * @param description2 The description2 to set.
	 */
	public void setDescription2(String description2) {
		this.description2 = description2;
	}
	/**
	 * @return Returns the extDescription.
	 */
	public String getExtDescription() {
		return extDescription;
	}
	/**
	 * @param extDescription The extDescription to set.
	 */
	public void setExtDescription(String extDescription) {
		this.extDescription = extDescription;
	}
	/**
	 * @return Returns the fullImage.
	 */
	public String getFullImage() {
		return fullImage;
	}
	/**
	 * @param fullImage The fullImage to set.
	 */
	public void setFullImage(String fullImage) {
		this.fullImage = fullImage;
	}
	public String getFullImageAltText()
	{
		return fullImageAltText;
	}
	public void setFullImageAltText(String fullImageAltText)
	{
		this.fullImageAltText = fullImageAltText;
	}
	public String getThumbImageAltText()
	{
		return thumbImageAltText;
	}
	public void setThumbImageAltText(String thumbImageAltText)
	{
		this.thumbImageAltText = thumbImageAltText;
	}
	/**
	 * @return Returns the overrideDesc1.
	 */
	public String getOverrideDesc1() {
		return overrideDesc1;
	}
	/**
	 * @param overrideDesc1 The overrideDesc1 to set.
	 */
	public void setOverrideDesc1(String overrideDesc1) {
		this.overrideDesc1 = overrideDesc1;
	}
	/**
	 * @return Returns the overrideDesc2.
	 */
	public String getOverrideDesc2() {
		return overrideDesc2;
	}
	/**
	 * @param overrideDesc2 The overrideDesc2 to set.
	 */
	public void setOverrideDesc2(String overrideDesc2) {
		this.overrideDesc2 = overrideDesc2;
	}
	/**
	 * @return Returns the thumbImage.
	 */
	public String getThumbImage() {
		return thumbImage;
	}
	/**
	 * @param thumbImage The thumbImage to set.
	 */
	public void setThumbImage(String thumbImage) {
		this.thumbImage = thumbImage;
	}
	public ArrayList<ItemImage> getItemImages()
	{
		return itemImages;
	}
	public void setItemImages(ArrayList<ItemImage> itemImages)
	{
		this.itemImages = itemImages;
	}
	public String getOtherPackaging() {
		return otherPackaging;
	}
	public void setOtherPackaging(String otherPackaging) {
		this.otherPackaging = otherPackaging;
	}	
	public String getOtherUM() {
		return otherUM;
	}
	public void setOtherUM(String otherUM) {
		this.otherUM = otherUM;
	}
	public String getPurchasingPackaging() {
		return purchasingPackaging;
	}
	public void setPurchasingPackaging(String purchasingPackaging) {
		this.purchasingPackaging = purchasingPackaging;
	}	
	public String getPurchasingUM() {
		return purchasingUM;
	}
	public void setPurchasingUM(String purchasingUM) {
		this.purchasingUM = purchasingUM;
	}
	public String getSellingPackaging() {
		return sellingPackaging;
	}
	public void setSellingPackaging(String sellingPackaging) {
		this.sellingPackaging = sellingPackaging;
	}	
	public String getSellingUM() {
		return sellingUM;
	}
	public void setSellingUM(String sellingUM) {
		this.sellingUM = sellingUM;
	}
	public String getStockingPackaging() {
		return stockingPackaging;
	}
	public void setStockingPackaging(String stockingPackaging) {
		this.stockingPackaging = stockingPackaging;
	}	
	public String getOtherPiece() {
		return otherPiece;
	}
	public void setOtherPiece(String otherPiece) {
		this.otherPiece = otherPiece;
	}
	public String getPurchasingPiece() {
		return purchasingPiece;
	}
	public void setPurchasingPiece(String purchasingPiece) {
		this.purchasingPiece = purchasingPiece;
	}
	public String getSellingPiece() {
		return sellingPiece;
	}
	public void setSellingPiece(String sellingPiece) {
		this.sellingPiece = sellingPiece;
	}
	public String getStockingPiece() {
		return stockingPiece;
	}
	public void setStockingPiece(String stockingPiece) {
		this.stockingPiece = stockingPiece;
	}
	public String getStockingUM() {
		return stockingUM;
	}
	public void setStockingUM(String stockingUM) {
		this.stockingUM = stockingUM;
	}
	public String getReplacementItem() {
		return replacementItem;
	}
	public void setReplacementItem(String replacementItem) {
		this.replacementItem = replacementItem;
	}
	public String getPreferredVendor() {
		return preferredVendor;
	}
	public void setPreferredVendor(String preferredVendor) {
		this.preferredVendor = preferredVendor;
	}
	public String getVendorName() {
		return vendorName;
	}
	public void setVendorName(String vendorName) {
		this.vendorName = vendorName;
	}	
	public String getPriceLevel01() {
		return priceLevel01;
	}
	public void setPriceLevel01(String priceLevel01) {
		this.priceLevel01 = priceLevel01;
	}
	public String getPriceLevel02() {
		return priceLevel02;
	}
	public void setPriceLevel02(String priceLevel02) {
		this.priceLevel02 = priceLevel02;
	}
	public String getPriceLevel03() {
		return priceLevel03;
	}
	public void setPriceLevel03(String priceLevel03) {
		this.priceLevel03 = priceLevel03;
	}
	public String getPriceLevel04() {
		return priceLevel04;
	}
	public void setPriceLevel04(String priceLevel04) {
		this.priceLevel04 = priceLevel04;
	}
	public String getPriceLevel05() {
		return priceLevel05;
	}
	public void setPriceLevel05(String priceLevel05) {
		this.priceLevel05 = priceLevel05;
	}
	public String getPriceLevel06() {
		return priceLevel06;
	}
	public void setPriceLevel06(String priceLevel06) {
		this.priceLevel06 = priceLevel06;
	}
	public String getPriceLevel07() {
		return priceLevel07;
	}
	public void setPriceLevel07(String priceLevel07) {
		this.priceLevel07 = priceLevel07;
	}
	public String getPriceLevel08() {
		return priceLevel08;
	}
	public void setPriceLevel08(String priceLevel08) {
		this.priceLevel08 = priceLevel08;
	}
	public String getPriceLevel09() {
		return priceLevel09;
	}
	public void setPriceLevel09(String priceLevel09) {
		this.priceLevel09 = priceLevel09;
	}
	public String getPriceLevel10() {
		return priceLevel10;
	}
	public void setPriceLevel10(String priceLevel10) {
		this.priceLevel10 = priceLevel10;
	}
	public String getPriceMatrix() {
		return priceMatrix;
	}
	public void setPriceMatrix(String priceMatrix) {
		this.priceMatrix = priceMatrix;
	}
	public String getSuggestedPrice() {
		return suggestedPrice;
	}
	public void setSuggestedPrice(String suggestedPrice) {
		this.suggestedPrice = suggestedPrice;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public String getDivision() {
		return division;
	}
	public void setDivision(String division) {
		this.division = division;
	}
	public String getInventoryClass() {
		return inventoryClass;
	}
	public void setInventoryClass(String inventoryClass) {
		this.inventoryClass = inventoryClass;
	}
	public String getDescription3() {
		return description3;
	}
	public void setDescription3(String description3) {
		this.description3 = description3;
	}
	public String getIsActive() {
		return isActive;
	}
	public void setIsActive(String isActive) {
		this.isActive = isActive;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getMfgCode() {
		return mfgCode;
	}
	public void setMfgCode(String mfgCode) {
		this.mfgCode = mfgCode;
	}	
	public String getPreferredVendorName() {
		return preferredVendorName;
	}
	public void setPreferredVendorName(String preferredVendorName) {
		this.preferredVendorName = preferredVendorName;
	}
	public String getQCommittedSum() {
		return qCommittedSum;
	}
	public void setQCommittedSum(String committedSum) {
		qCommittedSum = committedSum;
	}
	public String getQOnHandSum() {
		return qOnHandSum;
	}
	public void setQOnHandSum(String onHandSum) {
		qOnHandSum = onHandSum;
	}
	public String getQAvailableSum() {
		return qAvailableSum;
	}
	public void setQAvailableSum(String availableSum) {
		qAvailableSum = availableSum;
	}
	public String getMfgName() {
		return mfgName;
	}
	public void setMfgName(String mfgName) {
		this.mfgName = mfgName;
	}
	public String getDivName() {
		return divName;
	}
	public void setDivName(String divName) {
		this.divName = divName;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public RelatedCategory getRelatedCategory1()
	{
		return relatedCategory1;
	}
	public void setRelatedCategory1(RelatedCategory relatedCategory1)
	{
		this.relatedCategory1 = relatedCategory1;
	}
	public RelatedCategory getRelatedCategory2()
	{
		return relatedCategory2;
	}
	public void setRelatedCategory2(RelatedCategory relatedCategory2)
	{
		this.relatedCategory2 = relatedCategory2;
	}
	public RelatedCategory getRelatedCategory3()
	{
		return relatedCategory3;
	}
	public void setRelatedCategory3(RelatedCategory relatedCategory3)
	{
		this.relatedCategory3 = relatedCategory3;
	}
	public String getItemCategory() {
		return itemCategory;
	}
	public void setItemCategory(String itemCategory) {
		this.itemCategory = itemCategory;
	}
	public String getManufacturerIcon() {
		return manufacturerIcon;
	}
	public void setManufacturerIcon(String manufacturerIcon) {
		this.manufacturerIcon = manufacturerIcon;
	}
	public int getItemQuantity() {
		return itemQuantity;
	}
	public void setItemQuantity(int itemQuantity) {
		this.itemQuantity = itemQuantity;
	}
	public String getProductComments()
	{
		return productComments;
	}
	public void setProductComments(String productComments)
	{
		this.productComments = productComments;
	}
	public String getDeaFlag()
	{
		return deaFlag;
	}
	public void setDeaFlag(String deaFlag)
	{
		this.deaFlag = deaFlag;
	}
	public String getHazardFlag()
	{
		return hazardFlag;
	}
	public void setHazardFlag(String hazardFlag)
	{
		this.hazardFlag = hazardFlag;
	}
	public String getHeavyFlag()
	{
		return heavyFlag;
	}
	public void setHeavyFlag(String heavyFlag)
	{
		this.heavyFlag = heavyFlag;
	}
	public String getHazardSurchargeFlag()
	{
		return hazardSurchargeFlag;
	}
	public void setHazardSurchargeFlag(String hazardSurchargeFlag)
	{
		this.hazardSurchargeFlag = hazardSurchargeFlag;
	}
	public String getOversizedFlag()
	{
		return oversizedFlag;
	}
	public void setOversizedFlag(String oversizedFlag)
	{
		this.oversizedFlag = oversizedFlag;
	}
	public String getRefrigeratedFlag()
	{
		return refrigeratedFlag;
	}
	public void setRefrigeratedFlag(String refrigeratedFlag)
	{
		this.refrigeratedFlag = refrigeratedFlag;
	}
	public String getAnonPriceFlag()
	{
		return anonPriceFlag;
	}
	public void setAnonPriceFlag(String anonPriceFlag)
	{
		this.anonPriceFlag = anonPriceFlag;
	}
	public String getUnavailableFlag()
	{
		return unavailableFlag;
	}
	public void setUnavailableFlag(String unavailableFlag)
	{
		this.unavailableFlag = unavailableFlag;
	}
	public double getPointValue()
	{
		return pointValue;
	}
	public void setPointValue(double pointValue)
	{
		this.pointValue = pointValue;
	}
	public double getGetQty()
	{
		return getQty;
	}
	public void setGetQty(double getQty)
	{
		this.getQty = getQty;
	}
	public int getCartPromoNumber()
	{
		return cartPromoNumber;
	}
	public void setCartPromoNumber(int cartPromoNumber)
	{
		this.cartPromoNumber = cartPromoNumber;
	}
	public String getCartPromoItemType()
	{
		return cartPromoItemType;
	}
	public void setCartPromoItemType(String cartPromoItemType)
	{
		this.cartPromoItemType = cartPromoItemType;
	}
	public String getNewItemFlag()
	{
		return newItemFlag;
	}
	public void setNewItemFlag(String newItemFlag)
	{
		this.newItemFlag = newItemFlag;
	}
	public String getFeaturedItemFlag()
	{
		return featuredItemFlag;
	}
	public void setFeaturedItemFlag(String featuredItemFlag)
	{
		this.featuredItemFlag = featuredItemFlag;
	}
	public String getHistoryItem()
	{
		return historyItem;
	}
	public void setHistoryItem(String historyItem)
	{
		this.historyItem = historyItem;
	}
	public String getAltLocation()
	{
		return altLocation;
	}
	public void setAltLocation(String altLocation)
	{
		this.altLocation = altLocation;
	}
	public String getqOnHandSum()
	{
		return qOnHandSum;
	}
	public void setqOnHandSum(String qOnHandSum)
	{
		this.qOnHandSum = qOnHandSum;
	}
	public String getqCommittedSum()
	{
		return qCommittedSum;
	}
	public void setqCommittedSum(String qCommittedSum)
	{
		this.qCommittedSum = qCommittedSum;
	}
	public String getqAvailableSum()
	{
		return qAvailableSum;
	}
	public void setqAvailableSum(String qAvailableSum)
	{
		this.qAvailableSum = qAvailableSum;
	}
	public String getQoQtyMessage()
	{
		return qoQtyMessage;
	}
	public void setQoQtyMessage(String qoQtyMessage)
	{
		this.qoQtyMessage = qoQtyMessage;
	}
	public String getContentKey()
	{
		return contentKey;
	}
	public void setContentKey(String contentKey)
	{
		this.contentKey = contentKey;
	}
	public String getPageTitle()
	{
		return pageTitle;
	}
	public void setPageTitle(String pageTitle)
	{
		this.pageTitle = pageTitle;
	}
	public String getMetaKeywords()
	{
		return metaKeywords;
	}
	public void setMetaKeywords(String metaKeywords)
	{
		this.metaKeywords = metaKeywords;
	}
	public String getMetaDescription()
	{
		return metaDescription;
	}
	public void setMetaDescription(String metaDescription)
	{
		this.metaDescription = metaDescription;
	}
	public Vector<Object> getLocation()
	{
		return location;
	}
	public void setLocation(Vector<Object> location)
	{
		this.location = location;
	}
	public Vector<Object> getQuantityLocations()
	{
		return quantityLocations;
	}
	public void setQuantityLocations(Vector<Object> quantityLocations)
	{
		this.quantityLocations = quantityLocations;
	}
	public Vector<Object> getQtyBreaksList()
	{
		return qtyBreaksList;
	}
	public void setQtyBreaksList(Vector<Object> qtyBreaksList)
	{
		this.qtyBreaksList = qtyBreaksList;
	}
	public ArrayList<Object> getCategoryIDs()
	{
		return categoryIDs;
	}
	public void setCategoryIDs(ArrayList<Object> categoryIDs)
	{
		this.categoryIDs = categoryIDs;
	}
	public ArrayList<Object> getExternalFiles()
	{
		return externalFiles;
	}
	public void setExternalFiles(ArrayList<Object> externalFiles)
	{
		this.externalFiles = externalFiles;
	}
	public ArrayList<ItemLinks> getLinks()
	{
		return links;
	}
	public void setLinks(ArrayList<ItemLinks> links)
	{
		this.links = links;
	}
	public Vector<Object> getPromoNumbers()
	{
		return promoNumbers;
	}
	public void setPromoNumbers(Vector<Object> promoNumbers)
	{
		this.promoNumbers = promoNumbers;
	}
	public Vector<OrderGuide> getOrderGuides()
	{
		return orderGuides;
	}
	public void setOrderGuides(Vector<OrderGuide> orderGuides)
	{
		this.orderGuides = orderGuides;
	}
	public HashMap<Double, String> getPriceBreaks()
	{
		return priceBreaks;
	}
	public void setPriceBreaks(HashMap<Double, String> priceBreaks)
	{
		this.priceBreaks = priceBreaks;
	}
	public HashMap<String, String> getMinimumMultiple()
	{
		return minimumMultiple;
	}
	public void setMinimumMultiple(HashMap<String, String> minimumMultiple)
	{
		this.minimumMultiple = minimumMultiple;
	}
	public HashMap<String, Object> getItemAttributes()
	{
		return itemAttributes;
	}
	public void setItemAttributes(HashMap<String, Object> itemAttributes)
	{
		this.itemAttributes = itemAttributes;
	}
	public String getLineItemComments() {
		return lineItemComments;
	}
	public void setLineItemComments(String lineItemComments) {
		this.lineItemComments = lineItemComments;
	}
	public Integer getMatchedSearchCount()
	{
		return matchedSearchCount;
	}
	public void setMatchedSearchCount(Integer matchedSearchCount)
	{
		this.matchedSearchCount = matchedSearchCount;
	}	
	public String getItemType() {
		return itemType;
	}
	public void setItemType(String itemType) {
		this.itemType = itemType;
	}	
	public String getStyleItemNumber() {
		return styleItemNumber;
	}
	public void setStyleItemNumber(String styleItemNumber) {
		this.styleItemNumber = styleItemNumber;
	}	
	public ArrayList<SelectedVariant> getSelectedStyleVariants() {
		return selectedStyleVariants;
	}
	public void setSelectedStyleVariants(
			ArrayList<SelectedVariant> selectedStyleVariants) {
		this.selectedStyleVariants = selectedStyleVariants;
	}
	public String getStyleDspFormat() {
		return styleDspFormat;
	}
	public void setStyleDspFormat(String styleDspFormat) {
		this.styleDspFormat = styleDspFormat;
	}
	public boolean isQtyOneItem() {
		return qtyOneItem;
	}
	public void setQtyOneItem(boolean qtyOneItem) {
		this.qtyOneItem = qtyOneItem;
	}
	public boolean isCustomizable() {
		return customizable;
	}
	public void setCustomizable(boolean customizable) {
		this.customizable = customizable;
	}	
	public ArrayList<ItemPersonalization> getItemPersonalization() {
		return itemPersonalization;
	}
	public void setItemPersonalization(
			ArrayList<ItemPersonalization> itemPersonalization) {
		this.itemPersonalization = itemPersonalization;
	}
	public ArrayList<ItemSizing> getItemSizing() {
		return itemSizing;
	}
	public void setItemSizing(ArrayList<ItemSizing> itemSizing) {
		this.itemSizing = itemSizing;
	}	
	public ItemCustomizationSetting getItemCustomizationSetting() {
		return itemCustomizationSetting;
	}
	public void setItemCustomizationSetting(ItemCustomizationSetting itemCustomizationSetting) {
		this.itemCustomizationSetting = itemCustomizationSetting;
	}
	public String getMfgURL() {
		return mfgURL;
	}
	public void setMfgURL(String mfgURL) {
		this.mfgURL = mfgURL;
	}
	public String getNofollow() {
		return nofollow;
	}
	public void setNofollow(String nofollow) {
		this.nofollow = nofollow;
	}

	public String getCxmlUom()
	{
		return cxmlUom;
	}

	public void setCxmlUom(String cxmlUom)
	{
		this.cxmlUom = cxmlUom;
	}
}