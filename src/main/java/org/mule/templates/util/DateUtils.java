/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.util;

import java.sql.Timestamp;

import org.joda.time.DateTime;

/**
 * The function of this class is provide date comparation an transformation
 * functionality.
 * 
 * @author damiansima
 */
public class DateUtils {
	
	public static boolean compareDates(String sfdc, Timestamp dbTimeStamp, String offset){
		DateTime dbDateTime = org.mule.templates.date.DateUtils.dateToDateTime(dbTimeStamp).plusHours(Integer.parseInt(offset.split(":")[0]));
		DateTime sfdcDateTime = org.mule.templates.date.DateUtils.ISOStringDateToDateTime(sfdc);		
		return dbDateTime.isAfter(sfdcDateTime);
	}
	
}
