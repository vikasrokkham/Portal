package com.vormittag.was.util;

public class ItemSizing implements Comparable<Object> {
	private int sizeId = 0;
	private String sizeDescription = "";
	private String sizeValue = "";
	private int sortOrder = 0;
	
	public int compareTo(Object obj)
	{
		ItemSizing i = (ItemSizing)obj;
		int sortOrd = sortOrder;
		return sortOrd - i.getSortOrder();
	}
	
	public int getSizeId() {
		return sizeId;
	}
	public void setSizeId(int sizeId) {
		this.sizeId = sizeId;
	}
	public String getSizeDescription() {
		return sizeDescription;
	}
	public void setSizeDescription(String sizeDescription) {
		this.sizeDescription = sizeDescription;
	}
	public String getSizeValue() {
		return sizeValue;
	}
	public void setSizeValue(String sizeValue) {
		this.sizeValue = sizeValue;
	}
	public int getSortOrder() {
		return sortOrder;
	}
	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}	
}
