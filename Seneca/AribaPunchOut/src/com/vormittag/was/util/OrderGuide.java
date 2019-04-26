package com.vormittag.was.util;

public class OrderGuide implements Comparable
{
	String orderGuideCompany;
	String orderGuideName;
	String orderGuideUser;
	String orderGuideCustomer;
	String orderGuideCatalogID;
	String orderGuideDescription;
	String orderGuidePrivate;
	String orderGuideAddedBy;
	String orderGuideAddedDate;
	String orderGuideAddedTime;
	String orderGuideChangedBy;
	String orderGuideChangedDate;
	String orderGuideChangedTime;
	int orderGuideNumber;
	String orderGuideCustomerName = "";
	String orderGuideCustomerAddress1 = "";
	String orderGuideCustomerAddress2 = "";
	String orderGuideCustomerCity = "";
	String orderGuideCustomerState = "";
	String orderGuideCustomerZip = "";
	String orderGuideCustomerShipToNumber = "";
	String selected;
	String orderGuideAllShipTos = "N";
	
	public OrderGuide()
	{
		orderGuideCompany = null;
		orderGuideName = null;
		orderGuideUser = null;
		orderGuideCustomer = null;
		orderGuideCatalogID = null;
		orderGuideDescription = null;
		orderGuidePrivate = null;
		orderGuideAddedBy = null;
		orderGuideAddedDate = null;
		orderGuideAddedTime = null;
		orderGuideChangedBy = null;
		orderGuideChangedDate = null;
		orderGuideChangedTime = null;
		orderGuideNumber = 0;
		orderGuideAllShipTos = "N";
	}

	public OrderGuide(String company, String name, String user, String customer, String catalogID, String description, String isPrivate, String allShipTos, String addedBy, String dateAdded, String timeAdded, String changedBy, String dateChanged, String timeChanged)
	{
		orderGuideCompany = company;
		orderGuideName = name;
		orderGuideUser = user;
		orderGuideCustomer = customer;
		orderGuideCatalogID = catalogID;
		orderGuideDescription = description;
		orderGuidePrivate = isPrivate;
		orderGuideAllShipTos = allShipTos;
		orderGuideAddedBy = addedBy;
		orderGuideAddedDate = dateAdded;
		orderGuideAddedTime = timeAdded;
		orderGuideChangedBy = changedBy;
		orderGuideChangedDate = dateChanged;
		orderGuideChangedTime = timeChanged;
	}

	public int compareTo(Object arg0)
	{
		if (arg0 instanceof OrderGuide)
		{
			// Order guides are sorted according to their customer number
			// The ship to number is used for order guides with the same customer number
			OrderGuide arg = (OrderGuide)arg0;
			if (arg.getOrderGuideCustomer().equals(this.orderGuideCustomer))
			{
				if (arg.getOrderGuideAllShipTos().equals(this.orderGuideAllShipTos))
					return this.orderGuideCustomerShipToNumber.compareTo(arg.orderGuideCustomerShipToNumber);
				else
					return this.orderGuideAllShipTos.compareTo(arg.orderGuideAllShipTos);
			}
			else
				return this.orderGuideCustomer.compareTo(arg.orderGuideCustomer);
		}
		else
			return 0;
	}

	public String getOrderGuideCustomerShipToNumber()
	{
		return orderGuideCustomerShipToNumber;
	}
	public void setOrderGuideCustomerShipToNumber(String orderGuideCustomerShipToNumber)
	{
		this.orderGuideCustomerShipToNumber = orderGuideCustomerShipToNumber;
	}
	public String getOrderGuideCustomerAddress1()
	{
		return orderGuideCustomerAddress1;
	}
	public void setOrderGuideCustomerAddress1(String orderGuideCustomerAddress1)
	{
		this.orderGuideCustomerAddress1 = orderGuideCustomerAddress1;
	}
	public String getOrderGuideCustomerAddress2()
	{
		return orderGuideCustomerAddress2;
	}
	public void setOrderGuideCustomerAddress2(String orderGuideCustomerAddress2)
	{
		this.orderGuideCustomerAddress2 = orderGuideCustomerAddress2;
	}
	public String getOrderGuideCustomerCity()
	{
		return orderGuideCustomerCity;
	}
	public void setOrderGuideCustomerCity(String orderGuideCustomerCity)
	{
		this.orderGuideCustomerCity = orderGuideCustomerCity;
	}
	public String getOrderGuideCustomerName()
	{
		return orderGuideCustomerName;
	}
	public void setOrderGuideCustomerName(String orderGuideCustomerName)
	{
		this.orderGuideCustomerName = orderGuideCustomerName;
	}
	public String getOrderGuideCustomerState()
	{
		return orderGuideCustomerState;
	}
	public void setOrderGuideCustomerState(String orderGuideCustomerState)
	{
		this.orderGuideCustomerState = orderGuideCustomerState;
	}
	public String getOrderGuideCustomerZip()
	{
		return orderGuideCustomerZip;
	}
	public void setOrderGuideCustomerZip(String orderGuideCustomerZip)
	{
		this.orderGuideCustomerZip = orderGuideCustomerZip;
	}
	public int getOrderGuideNumber()
	{
		return orderGuideNumber;
	}
	public void setOrderGuideNumber(int orderGuideNumber)
	{
		this.orderGuideNumber = orderGuideNumber;
	}
	public String getOrderGuideAddedBy()
	{
		return orderGuideAddedBy;
	}
	public void setOrderGuideAddedBy(String orderGuideAddedBy)
	{
		this.orderGuideAddedBy = orderGuideAddedBy;
	}
	public String getOrderGuideAddedDate()
	{
		return orderGuideAddedDate;
	}
	public void setOrderGuideAddedDate(String orderGuideAddedDate)
	{
		this.orderGuideAddedDate = orderGuideAddedDate;
	}
	public String getOrderGuideAddedTime()
	{
		return orderGuideAddedTime;
	}
	public void setOrderGuideAddedTime(String orderGuideAddedTime)
	{
		this.orderGuideAddedTime = orderGuideAddedTime;
	}
	public String getOrderGuideCatalogID()
	{
		return orderGuideCatalogID;
	}
	public void setOrderGuideCatalogID(String orderGuideCatalogID)
	{
		this.orderGuideCatalogID = orderGuideCatalogID;
	}
	public String getOrderGuideChangedBy()
	{
		return orderGuideChangedBy;
	}
	public void setOrderGuideChangedBy(String orderGuideChangedBy)
	{
		this.orderGuideChangedBy = orderGuideChangedBy;
	}
	public String getOrderGuideChangedDate()
	{
		return orderGuideChangedDate;
	}
	public void setOrderGuideChangedDate(String orderGuideChangedDate)
	{
		this.orderGuideChangedDate = orderGuideChangedDate;
	}
	public String getOrderGuideChangedTime()
	{
		return orderGuideChangedTime;
	}
	public void setOrderGuideChangedTime(String orderGuideChangedTime)
	{
		this.orderGuideChangedTime = orderGuideChangedTime;
	}
	public String getOrderGuideCompany()
	{
		return orderGuideCompany;
	}
	public void setOrderGuideCompany(String orderGuideCompany)
	{
		this.orderGuideCompany = orderGuideCompany;
	}
	public String getOrderGuideCustomer()
	{
		return orderGuideCustomer;
	}
	public void setOrderGuideCustomer(String orderGuideCustomer)
	{
		this.orderGuideCustomer = orderGuideCustomer;
	}
	public String getOrderGuideDescription()
	{
		return orderGuideDescription;
	}
	public void setOrderGuideDescription(String orderGuideDescription)
	{
		this.orderGuideDescription = orderGuideDescription;
	}
	public String getOrderGuideName()
	{
		return orderGuideName;
	}
	public void setOrderGuideName(String orderGuideName)
	{
		this.orderGuideName = orderGuideName;
	}
	public String getOrderGuidePrivate()
	{
		return orderGuidePrivate;
	}
	public void setOrderGuidePrivate(String orderGuidePrivate)
	{
		this.orderGuidePrivate = orderGuidePrivate;
	}
	public String getOrderGuideUser()
	{
		return orderGuideUser;
	}
	public void setOrderGuideUser(String orderGuideUser)
	{
		this.orderGuideUser = orderGuideUser;
	}

	public String getSelected() {
		return selected;
	}

	public void setSelected(String selected) {
		this.selected = selected;
	}
	
	public String getOrderGuideAllShipTos() {
		return orderGuideAllShipTos;
	}

	public void setOrderGuideAllShipTos(String orderGuideAllShipTos) {
		this.orderGuideAllShipTos = orderGuideAllShipTos;
	}	
}