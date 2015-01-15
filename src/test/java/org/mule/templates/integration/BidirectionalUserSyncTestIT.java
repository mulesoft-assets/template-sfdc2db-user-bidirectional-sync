/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.processor.chain.InterceptingChainLifecycleWrapper;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.templates.AbstractTemplatesTestCase;
import org.mule.templates.db.MySQLDbCreator;
import org.mule.transport.NullPayload;
import org.mule.util.UUID;

import com.mulesoft.module.batch.BatchTestHelper;
import com.sforce.soap.partner.UpsertResult;

/**
 * The objective of this class is validating the correct behavior of the flows
 * for this Mule Anypoint Template
 * 
 */
@SuppressWarnings("unchecked")
public class BidirectionalUserSyncTestIT extends AbstractTemplatesTestCase {

	private static final String VAR_ID = "Id";
	private static final String VAR_USERNAME = "Username";
	private static final String VAR_LAST_NAME = "LastName";
	private static final String VAR_FIRST_NAME = "FirstName";
	private static final String VAR_EMAIL = "Email";
	private static String SFDC_PROFILE_ID;
	
	private static final String ANYPOINT_TEMPLATE_NAME = "userBiSync";
	private static final String SALESFORCE_INBOUND_FLOW_NAME = "triggerSyncFromSalesforceFlow";
	private static final String DATABASE_INBOUND_FLOW_NAME = "triggerSyncFromDatabaseFlow";
	private static final int TIMEOUT_MILLIS = 60;

	private SubflowInterceptingChainLifecycleWrapper upsertUserInSalesforceFlow;
	private SubflowInterceptingChainLifecycleWrapper insertUserInDatabaseFlow;
	private InterceptingChainLifecycleWrapper queryUserFromSalesforceFlow;
	private InterceptingChainLifecycleWrapper queryUserFromDatabaseFlow;
	private BatchTestHelper batchTestHelper;
	
	private static final String PATH_TO_TEST_PROPERTIES = "./src/test/resources/mule.test.properties";
	private static final String PATH_TO_SQL_SCRIPT = "src/main/resources/sfdc2jdbc.sql";
	private static final String DATABASE_NAME = "SFDC2DBAccountBroadcast" + new Long(new Date().getTime()).toString();
	private static final MySQLDbCreator DBCREATOR = new MySQLDbCreator(DATABASE_NAME, PATH_TO_SQL_SCRIPT, PATH_TO_TEST_PROPERTIES);
	private static final Object EMAIL = "noreply@chatter.salesforce.com";
	private static final Object EMAIL1 = "bwillisss@gmailtest.com";
	private static String SFDC_ID = null;

	private List<Map<String, Object>> createdUsersInSalesforce = new ArrayList<Map<String, Object>>();
	private List<Map<String, Object>> createdUsersInDatabase = new ArrayList<Map<String, Object>>();
	private static String PROFILE_ID;

	@BeforeClass
	public static void beforeTestClass() {
		final Properties props = new Properties();
		try {
			props.load(new FileInputStream(PATH_TO_TEST_PROPERTIES));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		
		SFDC_ID = props.getProperty("sfdc.testuser.id");
		SFDC_PROFILE_ID = props.getProperty("sfdc.user.profile.id");
		System.setProperty("page.size", "1000");

		// Set polling frequency to 10 seconds
		System.setProperty("polling.frequency", "10000");

		// Set default water-mark expression to current time
		System.clearProperty("watermark.default.expression");
		DateTime now = new DateTime(DateTimeZone.UTC);
		DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		System.setProperty("watermark.default.expression", now.toString(dateFormat));
		
		System.setProperty("db.jdbcUrl", DBCREATOR.getDatabaseUrlWithName());
		DBCREATOR.setUpDatabase();
		
	}

	@Before
	public void setUp() throws MuleException {
		
		stopAutomaticPollTriggering();
		getAndInitializeFlows();

		batchTestHelper = new BatchTestHelper(muleContext);
	}

	@After
	public void tearDown() throws Exception {
		DBCREATOR.tearDownDataBase();
	}

	private void stopAutomaticPollTriggering() throws MuleException {
		stopFlowSchedulers(SALESFORCE_INBOUND_FLOW_NAME);
		stopFlowSchedulers(DATABASE_INBOUND_FLOW_NAME);
	}

	private void getAndInitializeFlows() throws InitialisationException {
		// Flow for updating a user in Salesforce
		upsertUserInSalesforceFlow = getSubFlow("upsertUserInSalesforceFlow");
		upsertUserInSalesforceFlow.initialise();

		// Flow for updating a user in Database
		insertUserInDatabaseFlow = getSubFlow("insertUserInDatabaseFlow");
		insertUserInDatabaseFlow.initialise();

		// Flow for querying the user in Salesforce
		queryUserFromSalesforceFlow = getSubFlow("queryUserFromSalesforceFlow");
		queryUserFromSalesforceFlow.initialise();

		// Flow for querying the user in Database
		queryUserFromDatabaseFlow = getSubFlow("queryUserFromDatabaseFlow");
		queryUserFromDatabaseFlow.initialise();
	}

	@Test
	public void whenUpdatingAnUserInDatabaseTheBelongingUserGetsUpdatedInSalesforce() throws MuleException, Exception {
		// test db -> sfdc

		Map<String, Object> databaseUser0 = new HashMap<String, Object>();
		String infixDatabase = "_0_DB_" + ANYPOINT_TEMPLATE_NAME + "_" + System.currentTimeMillis();
		databaseUser0.put(VAR_ID, UUID.getUUID());
		databaseUser0.put(VAR_USERNAME, "Name" + infixDatabase + "@example.com");
		databaseUser0.put(VAR_FIRST_NAME, "fm" + infixDatabase);
		databaseUser0.put(VAR_LAST_NAME, "ln" + infixDatabase);
		databaseUser0.put(VAR_EMAIL, EMAIL1);
		databaseUser0.put("ProfileId", SFDC_PROFILE_ID);
		databaseUser0.put("Alias", "al0Db");
		databaseUser0.put("TimeZoneSidKey", "GMT");
		databaseUser0.put("LocaleSidKey", "en_US");
		databaseUser0.put("EmailEncodingKey", "ISO-8859-1");
		databaseUser0.put("LanguageLocaleKey", "en_US");
		databaseUser0.put("CommunityNickname", "cn" + infixDatabase);
		createdUsersInDatabase.add(databaseUser0);
		
		insertUserInDatabaseFlow.process(getTestEvent(Collections.singletonList(databaseUser0), MessageExchangePattern.REQUEST_RESPONSE));
	
		Thread.sleep(1001);
		
		// Execution
		executeWaitAndAssertBatchJob(DATABASE_INBOUND_FLOW_NAME);

		// Assertions
		{
			Object object =  queryUser(databaseUser0, queryUserFromDatabaseFlow);
			Assert.assertFalse("Synchronized user should not be null payload", object instanceof NullPayload);
			Map<String, Object> payload = (Map<String, Object>) object;
			Assert.assertEquals("The user should have been sync and new name must match", databaseUser0.get(VAR_FIRST_NAME), payload.get(VAR_FIRST_NAME));
			Assert.assertEquals("The user should have been sync and new title must match", databaseUser0.get(VAR_LAST_NAME), payload.get(VAR_LAST_NAME));
		}

		// cleanup
		deleteTestUsersFromDatabase();

		// test sfdc -> db
		
		Map<String, Object> salesforceUser0 = new HashMap<String, Object>();
		String infixSalesforce = "_0_SFDC_" + ANYPOINT_TEMPLATE_NAME + "_" + System.currentTimeMillis();
		salesforceUser0.put(VAR_ID, SFDC_ID);
		salesforceUser0.put(VAR_USERNAME, "Name" + infixSalesforce + "@example.com");
		salesforceUser0.put(VAR_FIRST_NAME, "fn" + infixSalesforce);
		salesforceUser0.put(VAR_LAST_NAME, "ln" + infixSalesforce);
		salesforceUser0.put(VAR_EMAIL, EMAIL);
		salesforceUser0.put("ProfileId", SFDC_PROFILE_ID);
		salesforceUser0.put("Alias", "al0Sfdc");
		salesforceUser0.put("TimeZoneSidKey", "GMT");
		salesforceUser0.put("LocaleSidKey", "en_US");
		salesforceUser0.put("EmailEncodingKey", "ISO-8859-1");
		salesforceUser0.put("LanguageLocaleKey", "en_US");
		salesforceUser0.put("CommunityNickname", "cn" + infixSalesforce);
		createdUsersInSalesforce.add(salesforceUser0);

		MuleEvent event = upsertUserInSalesforceFlow.process(getTestEvent(Collections.singletonList(salesforceUser0), MessageExchangePattern.REQUEST_RESPONSE));
		salesforceUser0.put(VAR_ID, (((UpsertResult) ((List<?>) event.getMessage().getPayload()).get(0))).getId());

		Thread.sleep(1001);

		// Execution
		executeWaitAndAssertBatchJob(SALESFORCE_INBOUND_FLOW_NAME);

		// FIXME above call does not wait for batch to complete
		Thread.sleep(10000);
		
		// Assertions
		{
			Object object = queryUser(salesforceUser0, queryUserFromDatabaseFlow);
			Assert.assertFalse("Synchronized user should not be null payload", object instanceof NullPayload);
			Map<String, Object> payload = (Map<String, Object>) object;
			Assert.assertEquals("The user should have been sync and new name must match", salesforceUser0.get(VAR_FIRST_NAME), payload.get(VAR_FIRST_NAME));
			Assert.assertEquals("The user should have been sync and new title must match", salesforceUser0.get(VAR_LAST_NAME), payload.get(VAR_LAST_NAME));
		}
		
		// cleanup
		SubflowInterceptingChainLifecycleWrapper deleteUsersAfterSfdc2Database = getSubFlow("deleteUsersAfterSfdc2Database");
		deleteUsersAfterSfdc2Database.initialise();
		deleteUsersAfterSfdc2Database.process(getTestEvent(createdUsersInSalesforce, MessageExchangePattern.REQUEST_RESPONSE));
	}

	private Object queryUser(Map<String, Object> user, InterceptingChainLifecycleWrapper queryUserFlow) throws MuleException, Exception {
		return queryUserFlow.process(getTestEvent(user, MessageExchangePattern.REQUEST_RESPONSE)).getMessage().getPayload();
	}

	private void executeWaitAndAssertBatchJob(String flowConstructName) throws Exception {
		// Execute synchronization
		runSchedulersOnce(flowConstructName);

		// Wait for the batch job execution to finish
		batchTestHelper.awaitJobTermination(TIMEOUT_MILLIS * 1000, 500);
		batchTestHelper.assertJobWasSuccessful();
	}
	
	private void deleteTestUsersFromDatabase() throws InitialisationException, MuleException, Exception {
		SubflowInterceptingChainLifecycleWrapper deleteUserFromDatabaseFlow = getSubFlow("deleteUserFromDatabaseFlow");
		deleteUserFromDatabaseFlow.initialise();
		deleteTestEntityFromSandBox(deleteUserFromDatabaseFlow, createdUsersInDatabase);
	}
	
	private void deleteTestEntityFromSandBox(SubflowInterceptingChainLifecycleWrapper deleteFlow, List<Map<String, Object>> entitities) throws MuleException, Exception {
		List<String> idList = new ArrayList<String>();
		for (Map<String, Object> c : entitities) {
			idList.add(c.get(VAR_ID).toString());
		}
		
		deleteFlow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	protected Map<String, Object> invokeRetrieveFlow(InterceptingChainLifecycleWrapper flow, Map<String, Object> payload) throws Exception {
		MuleEvent event = flow.process(getTestEvent(payload, MessageExchangePattern.REQUEST_RESPONSE));
		Object resultPayload = event.getMessage().getPayload();
		return resultPayload instanceof NullPayload ? null : (Map<String, Object>) resultPayload;
	}

}
