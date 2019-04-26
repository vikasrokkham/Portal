package com.vormittag.was.util;

public class RelatedCategory
{
	private int categoryID = 0;
	private String categoryName = "";
	private String image = "";

	public boolean equals(Object o)
	{
		if (o instanceof RelatedCategory)
		{
			RelatedCategory c = (RelatedCategory)o;
			return (categoryID == c.getCategoryID() && categoryName.equals(c.getCategoryName()) &&
				image.equals(c.getImage()));
		}
		else
			return false;
	}
	
	public int getCategoryID()
	{
		return categoryID;
	}
	public void setCategoryID(int categoryID)
	{
		this.categoryID = categoryID;
	}
	public String getCategoryName()
	{
		return categoryName;
	}
	public void setCategoryName(String categoryName)
	{
		this.categoryName = categoryName;
	}
	public String getImage()
	{
		return image;
	}
	public void setImage(String image)
	{
		this.image = image;
	}
}