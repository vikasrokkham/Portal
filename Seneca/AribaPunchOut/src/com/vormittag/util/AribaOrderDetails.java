package com.vormittag.util;

public class AribaOrderDetails implements Comparable<AribaOrderDetails>
{
	private String itemNumber = null;
	private String unitOfMeasure = null;
	private int quantity = 0;
	private int lineNumber = 0;
	
	public AribaOrderDetails(int lineNumber, String itemNumber, int quantity)
	{
		this.lineNumber = lineNumber;
		this.itemNumber = itemNumber;
		this.quantity = quantity;
	}
	
	public AribaOrderDetails(int lineNumber, String itemNumber, String unitOfMeasure, int quantity)
	{
		this(lineNumber, itemNumber, quantity);
		this.unitOfMeasure = unitOfMeasure;
	}
	
	public int getLineNumber()
	{
		return lineNumber;
	}
	public String getItemNumber()
	{
		return itemNumber;
	}
	public String getUnitOfMeasure()
	{
		return unitOfMeasure;
	}
	public int getQuantity()
	{
		return quantity;
	}
	@Override
	public int compareTo(AribaOrderDetails that)
	{
		int value = this.itemNumber.compareTo(that.itemNumber);
		if (value == 0 && this.unitOfMeasure != null && that.unitOfMeasure != null)
		{
			value = this.unitOfMeasure.compareTo(that.unitOfMeasure);
		}
		if (value == 0)
		{
			value = //Integer.compare(this.quantity, that.quantity);
					(this.quantity < that.quantity) ? -1 : ((this.quantity == that.quantity) ? 0 : 1);
					//taken from the Java 7 API source since we have to support java 6
		}
		return value;
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("AribaOrderDetails [lineNumber=");
		builder.append(lineNumber);
		builder.append(", itemNumber=");
		builder.append(itemNumber);
		builder.append(", unitOfMeasure=");
		builder.append(unitOfMeasure);
		builder.append(", quantity=");
		builder.append(quantity);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((itemNumber == null) ? 0 : itemNumber.hashCode());
		result = prime * result + lineNumber;
		result = prime * result + quantity;
		result = prime * result + ((unitOfMeasure == null) ? 0 : unitOfMeasure.hashCode());
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
		if (!(obj instanceof AribaOrderDetails))
		{
			return false;
		}
		AribaOrderDetails other = (AribaOrderDetails) obj;
		if (itemNumber == null)
		{
			if (other.itemNumber != null)
			{
				return false;
			}
		}
		else if (!itemNumber.equals(other.itemNumber))
		{
			return false;
		}
		if (lineNumber != other.lineNumber)
		{
			return false;
		}
		if (quantity != other.quantity)
		{
			return false;
		}
		if (unitOfMeasure == null)
		{
			if (other.unitOfMeasure != null)
			{
				return false;
			}
		}
		else if (!unitOfMeasure.equals(other.unitOfMeasure))
		{
			return false;
		}
		return true;
	}
}