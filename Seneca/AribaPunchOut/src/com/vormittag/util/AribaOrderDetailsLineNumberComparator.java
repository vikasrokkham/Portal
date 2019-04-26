package com.vormittag.util;

import java.util.Comparator;

public class AribaOrderDetailsLineNumberComparator implements Comparator<AribaOrderDetails>
{	
	@Override
	public int compare(AribaOrderDetails o1, AribaOrderDetails o2)
	{
		return o1.getLineNumber()-o2.getLineNumber();
	}
}
