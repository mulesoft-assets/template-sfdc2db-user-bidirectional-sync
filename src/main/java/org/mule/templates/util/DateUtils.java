/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.util;

import java.util.Date;

import org.joda.time.DateTime;

/**
 * The function of this class is provide date comparation an transformation
 * functionality.
 * 
 * @author damiansima
 */
public class DateUtils {
	
	public static boolean compareDates(String sfdc, Date dbTimeStamp){
		DateTime dbDateTime = org.mule.templates.date.DateUtils.dateToDateTimeUsingProvidedOffset(dbTimeStamp, "UTC");
		DateTime sfdcDateTime = org.mule.templates.date.DateUtils.ISOStringDateToDateTime(sfdc);
		return dbDateTime.isAfter(sfdcDateTime);
	}
	
}
