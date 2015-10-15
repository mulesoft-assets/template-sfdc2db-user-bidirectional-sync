/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.util;

import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.Test;
import org.mule.api.transformer.TransformerException;

public class DateUtilsTest {
	
	@Test
	public void dateAIsBeforeThedateBInDifferentTimezoneZulu() throws TransformerException {
		String dateA = "2013-12-09T05:00:33.001Z";
		Timestamp dateB = new Timestamp(new Date().getTime());
		assertTrue("The date B should be after the date A", DateUtils.compareDates(dateA, dateB));
	}

}
