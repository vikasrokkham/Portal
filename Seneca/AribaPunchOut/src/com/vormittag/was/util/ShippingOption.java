package com.vormittag.was.util;

public class ShippingOption
{
	private String shippingOption = "";
	private String shipViaCode = "";
	private String description = "";
	private double amount = 0.00;
	private boolean selected = false;
	private String shipDate = "";
	
	public boolean equals(Object o)
	{
		if (o instanceof ShippingOption)
		{
			ShippingOption obj = (ShippingOption)o;
			return this.shipViaCode.equals(obj.getShipViaCode());
		}
		else
			return false;
	}
	
	public String getShippingOption()
	{
		return shippingOption;
	}
	public void setShippingOption(String shippingOption)
	{
		this.shippingOption = shippingOption;
	}
	public String getShipViaCode()
	{
		return shipViaCode;
	}
	public void setShipViaCode(String shipViaCode)
	{
		this.shipViaCode = shipViaCode;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	public double getAmount()
	{
		return amount;
	}
	public void setAmount(double amount)
	{
		this.amount = amount;
	}
	public boolean isSelected() {
		return selected;
	}
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	public String getShipDate() {
		return shipDate;
	}
	public void setShipDate(String shipDate) {
		this.shipDate = shipDate;
	}
	
}