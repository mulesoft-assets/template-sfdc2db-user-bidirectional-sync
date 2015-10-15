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

import org.junit.After;
import org.junit.AfterClass;
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
	private SubflowInterceptingChainLifecycleWrapper deleteUserFromSalesforceFlow;
	private SubflowInterceptingChainLifecycleWrapper deleteUserFromDatabaseFlow;
	private BatchTestHelper batchTestHelper;
	
	private static final String PATH_TO_TEST_PROPERTIES = "./src/test/resources/mule.test.properties";
	private static final String PATH_TO_SQL_SCRIPT = "src/main/resources/sfdc2jdbc.sql";
	private static final String DATABASE_NAME = "SFDC2DBUserBiDir" + new Long(new Date().getTime()).toString();
	private static final MySQLDbCreator DBCREATOR = new MySQLDbCreator(DATABASE_NAME, PATH_TO_SQL_SCRIPT, PATH_TO_TEST_PROPERTIES);
	private static final String EMAIL = "some.email.1@fakemail.com";
	private static final String EMAIL1 = "eeee@gmail.com";
	private static String SFDC_ID = null;

	private List<Map<String, Object>> createdUsersInSalesforce = new ArrayList<Map<String, Object>>();
	private List<Map<String, Object>> createdUsersInDatabase = new ArrayList<Map<String, Object>>();

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
		System.setProperty("poll.frequencyMillis", "10000");
		
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
		deleteTestUsersFromSalesforce();
		deleteTestUsersFromDatabase();
	}
	
	@AfterClass
	public static void tearDownDB() throws Exception {
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
		insertUserInDatabaseFlow.setMuleContext(muleContext);

		// Flow for querying the user in Salesforce
		queryUserFromSalesforceFlow = getSubFlow("queryUserFromSalesforceFlow");
		queryUserFromSalesforceFlow.initialise();

		// Flow for querying the user in Database
		queryUserFromDatabaseFlow = getSubFlow("queryUserFromDatabaseFlow");
		queryUserFromDatabaseFlow.initialise();
		
		// Flow for cleaning up users in Salesforce
		deleteUserFromSalesforceFlow = getSubFlow("deleteUserFromSalesforceFlow");
		deleteUserFromSalesforceFlow.initialise();
		
		// Flow for cleaning up users in Database
		deleteUserFromDatabaseFlow = getSubFlow("deleteUserFromDatabaseFlow");
		deleteUserFromDatabaseFlow.initialise();
	}
	
	@Test
	public void testSalesforceDirection() throws MuleException, Exception {	
		final Map<String, Object> salesforceUser0 = new HashMap<String, Object>();
		final String infixSalesforce = "_0_SFDC_" + ANYPOINT_TEMPLATE_NAME + "_" + System.currentTimeMillis();
		salesforceUser0.put(VAR_ID, SFDC_ID);
		salesforceUser0.put(VAR_USERNAME, "Name" + infixSalesforce + "@example.com");
		salesforceUser0.put(VAR_FIRST_NAME, "fn" + infixSalesforce);
		salesforceUser0.put(VAR_LAST_NAME, "ln" + infixSalesforce);
		salesforceUser0.put(VAR_EMAIL, EMAIL);
		salesforceUser0.put("ProfileId", SFDC_PROFILE_ID);
		salesforceUser0.put("IsActive", true);
		salesforceUser0.put("Alias", "al0Sfdc");
		salesforceUser0.put("TimeZoneSidKey", "GMT");
		salesforceUser0.put("LocaleSidKey", "en_US");
		salesforceUser0.put("EmailEncodingKey", "ISO-8859-1");
		salesforceUser0.put("LanguageLocaleKey", "en_US");
		salesforceUser0.put("CommunityNickname", "cn" + infixSalesforce);
		createdUsersInSalesforce.clear();
		createdUsersInSalesforce.add(salesforceUser0);

		final MuleEvent event = upsertUserInSalesforceFlow.process(getTestEvent(Collections.singletonList(salesforceUser0), MessageExchangePattern.REQUEST_RESPONSE));
		salesforceUser0.put(VAR_ID, (((UpsertResult) ((List<?>) event.getMessage().getPayload()).get(0))).getId());

		Thread.sleep(1001);

		// Execution
		executeWaitAndAssertBatchJob(SALESFORCE_INBOUND_FLOW_NAME);

		// FIXME above call does not wait for batch to complete
		Thread.sleep(10000);
		
		// Assertions
		final Object object = queryUser(salesforceUser0, queryUserFromDatabaseFlow);
		Assert.assertFalse("Synchronized user should not be null payload", object instanceof NullPayload);
		final Map<String, Object> payload = (Map<String, Object>) object;
		Assert.assertEquals("The user should have been sync and new name must match", salesforceUser0.get(VAR_FIRST_NAME), payload.get(VAR_FIRST_NAME));
		Assert.assertEquals("The user should have been sync and new title must match", salesforceUser0.get(VAR_LAST_NAME), payload.get(VAR_LAST_NAME));
	}
	
	@Test
	public void testDatabaseDirection() throws MuleException, Exception {	
		final Map<String, Object> databaseUser0 = new HashMap<String, Object>();
		final String infixDatabase = "_0_DB_" + ANYPOINT_TEMPLATE_NAME + "_" + System.currentTimeMillis();
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
		createdUsersInDatabase.clear();
		createdUsersInDatabase.add(databaseUser0);
		
		final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		list.add(databaseUser0);
		insertUserInDatabaseFlow.process(getTestEvent(list, muleContext));
		
		Thread.sleep(1001);
		
		// Execution
		executeWaitAndAssertBatchJob(DATABASE_INBOUND_FLOW_NAME);
		
		Thread.sleep(10000);

		// Assertions
		final Object object =  queryUser(databaseUser0, queryUserFromSalesforceFlow);
		Assert.assertFalse("Synchronized user should not be null payload", object == null);
		final Map<String, Object> retrievedSalesforceUser = (Map<String, Object>) object;
		createdUsersInSalesforce.add(retrievedSalesforceUser);
		Assert.assertEquals("The user should have been sync and new name must match", databaseUser0.get(VAR_FIRST_NAME), retrievedSalesforceUser.get(VAR_FIRST_NAME));
		Assert.assertEquals("The user should have been sync and new title must match", databaseUser0.get(VAR_LAST_NAME), retrievedSalesforceUser.get(VAR_LAST_NAME));
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

	protected Map<String, Object> invokeRetrieveFlow(InterceptingChainLifecycleWrapper flow, Map<String, Object> payload) throws Exception {
		final MuleEvent event = flow.process(getTestEvent(payload, MessageExchangePattern.REQUEST_RESPONSE));
		final Object resultPayload = event.getMessage().getPayload();
		return resultPayload instanceof NullPayload ? null : (Map<String, Object>) resultPayload;
	}
	
	private void deleteTestUsersFromDatabase() throws InitialisationException, MuleException, Exception {		
		final List<String> idList = new ArrayList<String>();
		for (Map<String, Object> c : createdUsersInDatabase) {
			idList.add(c.get(VAR_ID).toString());
		}
		
		deleteUserFromDatabaseFlow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	private void deleteTestUsersFromSalesforce() throws InitialisationException, MuleException, Exception {
		List<Map<String, Object>> idList = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> c : createdUsersInSalesforce) {
			logger.info("deleting SFDC user: " + c.get(VAR_ID));			
			Map<String, Object> entry = new HashMap<String, Object>();
			entry.put("Id", c.get(VAR_ID));
			entry.put("isActive", false);
			idList.add(entry);
		}
		
		deleteUserFromSalesforceFlow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));		
	}
}
