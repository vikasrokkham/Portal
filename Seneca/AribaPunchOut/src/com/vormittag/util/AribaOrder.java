package com.vormittag.util;

import java.util.ArrayList;
import java.util.List;

public class AribaOrder implements Comparable<AribaOrder>
{
	private String poNumber = null;
	private int orderNumber = 0;
	
	private String
		customer_nbr = null,
		shipto_nbr = null,
		name = null,
		address1 = null,
		address2 = null,
		address3 = null,
		city = null,
		state = null,
		zip = null,
		country = null,
		location = null,
		email = null,
		shipVia = null,
		phoneNumber = null,
		faxNumber = null;
	
	private int
		company = 1,
		cartKey = -1;
	
	private boolean updateShipTo = false;
	
	private List<AribaOrderDetails> itemDetails = new ArrayList<AribaOrderDetails>();
	
	public AribaOrder(String poNumber, int orderNumber)
	{
		this.poNumber = poNumber;
		this.orderNumber = orderNumber;
	}
	public String getPoNumber()
	{
		return poNumber;
	}
	public void setOrderNumber(int orderNumber)
	{
		this.orderNumber = orderNumber;
	}
	public int getOrderNumber()
	{
		return orderNumber;
	}
	public String getCustomer_nbr()
	{
		return customer_nbr;
	}
	public String getShipto_nbr()
	{
		return shipto_nbr;
	}
	public List<AribaOrderDetails> getItemDetails()
	{
		return itemDetails;
	}
	public void setCustomer_nbr(String customer_nbr)
	{
		this.updateShipTo = true;
		this.customer_nbr = sanitizeInputString(customer_nbr);
	}
	public void setShipto_nbr(String shipto_nbr)
	{
		this.updateShipTo = true;
		this.shipto_nbr = sanitizeInputString(shipto_nbr);
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.updateShipTo = true;
		this.name = sanitizeInputString(name);
	}
	public String getAddress1()
	{
		return address1;
	}
	public void setAddress1(String address1)
	{
		this.updateShipTo = true;
		this.address1 = sanitizeInputString(address1);
	}
	public String getAddress2()
	{
		return address2;
	}
	public void setAddress2(String address2)
	{
		this.updateShipTo = true;
		this.address2 = sanitizeInputString(address2);
	}
	public String getAddress3()
	{
		return address3;
	}
	public void setAddress3(String address3)
	{
		this.updateShipTo = true;
		this.address3 = sanitizeInputString(address3);
	}
	public String getCity()
	{
		return city;
	}
	public void setCity(String city)
	{
		this.updateShipTo = true;
		this.city = sanitizeInputString(city);
	}
	public String getState()
	{
		return state;
	}
	public void setState(String state)
	{
		this.updateShipTo = true;
		this.state = sanitizeInputString(state);
	}
	public String getZip()
	{
		return zip;
	}
	public void setZip(String zip)
	{
		this.updateShipTo = true;
		this.zip = sanitizeInputString(zip);
	}
	public String getCountry()
	{
		return country;
	}
	public void setCountry(String country)
	{
		this.updateShipTo = true;
		this.country = sanitizeInputString(country);
	}
	public String getLocation()
	{
		return location;
	}
	public void setLocation(String location)
	{
		this.updateShipTo = true;
		this.location = sanitizeInputString(location);
	}
	public String getEmail()
	{
		return email;
	}
	public void setEmail(String email)
	{
		this.updateShipTo = true;
		this.email = sanitizeInputString(email);
	}
	public String getShipVia()
	{
		return shipVia;
	}
	public void setShipVia(String shipVia)
	{
		this.updateShipTo = true;
		this.shipVia = sanitizeInputString(shipVia);
	}
	public String getPhoneNumber()
	{
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber)
	{
		this.updateShipTo = true;
		this.phoneNumber = sanitizeInputString(phoneNumber);
	}
	public String getFaxNumber()
	{
		return faxNumber;
	}
	public void setFaxNumber(String faxNumber)
	{
		this.updateShipTo = true;
		this.faxNumber = sanitizeInputString(faxNumber);
	}
	public int getCompany()
	{
		return company;
	}
	public void setCompany(int company)
	{
		this.updateShipTo = true;
		this.company = company;
	}
	public int getCartKey()
	{
		return cartKey;
	}
	public void setCartKey(int cartKey)
	{
		this.cartKey = cartKey;
	}
	public boolean needToUpdateShipTo()
	{
		return this.updateShipTo;
	}
	
	protected static String sanitizeInputString(String input)
	{
		if (input == null)
		{
			input = "";
		}
		else
		{
			input = input.trim();
		}
		return input;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((itemDetails == null) ? 0 : itemDetails.hashCode());
		result = prime * result + orderNumber;
		result = prime * result + ((poNumber == null) ? 0 : poNumber.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (!(obj instanceof AribaOrder))
		{
			return false;
		}
		AribaOrder other = (AribaOrder) obj;
		if (itemDetails == null)
		{
			if (other.itemDetails != null)
			{
				return false;
			}
		}
		else if (!itemDetails.equals(other.itemDetails))
		{
			return false;
		}
		if (orderNumber != other.orderNumber)
		{
			return false;
		}
		if (poNumber == null)
		{
			if (other.poNumber != null)
			{
				return false;
			}
		}
		else if (!poNumber.equals(other.poNumber))
		{
			return false;
		}
		return true;
	}
	@Override
	public int compareTo(AribaOrder that)
	{
		int value = this.poNumber.compareTo(that.poNumber);
		if (value == 0)
		{
			value = Integer.compare(this.orderNumber, that.orderNumber);
		}
		return value;
	}
	
	@Override
	public AribaOrder clone()
	{
		AribaOrder newOrder = new AribaOrder(this.poNumber, this.orderNumber);
		newOrder.setAddress1(this.address1);
		newOrder.setAddress2(this.address2);
		newOrder.setAddress3(this.address3);
		newOrder.setCity(this.city);
		newOrder.setCountry(this.country);
		newOrder.setCustomer_nbr(this.customer_nbr);
		newOrder.setLocation(this.location);
		newOrder.setName(this.name);
		newOrder.setShipto_nbr(this.shipto_nbr);
		newOrder.setState(this.state);
		newOrder.setZip(this.zip);
		return newOrder;
	}
}
