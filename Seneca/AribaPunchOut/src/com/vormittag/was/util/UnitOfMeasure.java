package com.vormittag.was.util;

public class UnitOfMeasure {
	private String unitOfMeasure = "";
	private String unitOfMeasureDescription = "";
	private String unitMearurePrice = "";
	private String availableQty = "";
	private String discount = "";
	private String priceCode = "";
	private String errorCode = "";
	private String contractSource = "";
	private String contractNumber = "";
	private String contractTier = "";
	private String contractExpiration = "";
	private String contractEffDate = "";
	private double multiplier = 1.00;
	private String suggestedPrice = "";
	public boolean isRestricted = false;
	private String cost = "";
	
	public boolean isRestricted() {
		return isRestricted;
	}
	public void setRestricted(boolean isRestricted) {
		this.isRestricted = isRestricted;
	}
	public String getContractExpiration() {
		return contractExpiration;
	}
	public void setContractExpiration(String contractExpiration) {
		this.contractExpiration = contractExpiration;
	}
	public String getContractEffDate() {
		return contractEffDate;
	}
	public void setContractEffDate(String contractEffDate) {
		this.contractEffDate = contractEffDate;
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
	public String getAvailableQty() {
		return availableQty;
	}
	public void setAvailableQty(String availableQty) {
		this.availableQty = availableQty;
	}
	public String getDiscount() {
		if (this.discount == null || this.discount.length() == 0)
		{
			this.discount = "0.00";
		}
		return discount;
	}
	public void setDiscount(String discount) {
		this.discount = discount;
	}
	public String getPriceCode() {
		return priceCode;
	}
	public void setPriceCode(String priceCode) {
		this.priceCode = priceCode;
	}
	public String getUnitOfMeasure() {
		return unitOfMeasure;
	}
	public void setUnitOfMeasure(String unitOfMeasure) {
		this.unitOfMeasure = unitOfMeasure;
	}
	public String getUnitOfMeasureDescription() {
		return unitOfMeasureDescription;
	}
	public void setUnitOfMeasureDescription(String unitOfMeasureDescription) {
		this.unitOfMeasureDescription = unitOfMeasureDescription;
	}
	public String getUnitMearurePrice() {
		return unitMearurePrice;
	}
	public void setUnitMearurePrice(String unitMearurePrice) {
		this.unitMearurePrice = unitMearurePrice;
	}
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public double getMultiplier()
	{
		return multiplier;
	}
	public void setMultiplier(double multiplier)
	{
		this.multiplier = multiplier;
	}
	public String getSuggestedPrice() {
		return suggestedPrice;
	}
	public void setSuggestedPrice(String suggestedPrice) {
		this.suggestedPrice = suggestedPrice;
	}
	public String getCost() {
		return cost;
	}
	public void setCost(String cost) {
		this.cost = cost;
	}
}