package org.mule.templates.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.Schedulers;
import org.mule.processor.chain.InterceptingChainLifecycleWrapper;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.templates.AbstractTemplatesTestCase;
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

	private static final String ANYPOINT_TEMPLATE_NAME = "usr-bidi-sync";
	private static final String A_INBOUND_FLOW_NAME = "triggerSyncFromAFlow";
	private static final String B_INBOUND_FLOW_NAME = "triggerSyncFromBFlow";
	private static final int TIMEOUT_MILLIS = 60;

	private SubflowInterceptingChainLifecycleWrapper upsertUserInAFlow;
	private SubflowInterceptingChainLifecycleWrapper insertUserInBFlow;
	private InterceptingChainLifecycleWrapper queryUserFromAFlow;
	private InterceptingChainLifecycleWrapper queryUserFromBFlow;
	private BatchTestHelper batchTestHelper;

	private List<Map<String, Object>> createdUsersInA = new ArrayList<Map<String, Object>>();
	private List<Map<String, Object>> createdUsersInB = new ArrayList<Map<String, Object>>();

	@BeforeClass
	public static void beforeTestClass() {
		System.setProperty("page.size", "1000");

		// Set polling frequency to 10 seconds
		System.setProperty("polling.frequency", "10000");

		// Set default water-mark expression to current time
		System.clearProperty("watermark.default.expression");
		DateTime now = new DateTime(DateTimeZone.UTC);
		DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		System.setProperty("watermark.default.expression", now.toString(dateFormat));
	}

	@Before
	public void setUp() throws MuleException {
		stopAutomaticPollTriggering();
		getAndInitializeFlows();

		batchTestHelper = new BatchTestHelper(muleContext);
	}

	@After
	public void tearDown() throws Exception {
	}

	private void stopAutomaticPollTriggering() throws MuleException {
		stopFlowSchedulers(A_INBOUND_FLOW_NAME);
		stopFlowSchedulers(B_INBOUND_FLOW_NAME);
	}

	private void getAndInitializeFlows() throws InitialisationException {
		// Flow for updating a user in A instance
		upsertUserInAFlow = getSubFlow("upsertUserInAFlow");
		upsertUserInAFlow.initialise();

		// Flow for updating a user in B instance
		insertUserInBFlow = getSubFlow("insertUserInBFlow");
		insertUserInBFlow.initialise();

		// Flow for querying the user in A instance
		queryUserFromAFlow = getSubFlow("queryUserFromAFlow");
		queryUserFromAFlow.initialise();

		// Flow for querying the user in B instance
		queryUserFromBFlow = getSubFlow("queryUserFromBFlow");
		queryUserFromBFlow.initialise();
	}

	@Test
	public void whenUpdatingAnUserInInstanceBTheBelongingUserGetsUpdatedInInstanceA() throws MuleException, Exception {
		// test db -> sfdc

		Map<String, Object> user_0_B = new HashMap<String, Object>();
		String infixB = "_0_B_" + ANYPOINT_TEMPLATE_NAME + "_" + System.currentTimeMillis();
		user_0_B.put("Id", UUID.getUUID());
		user_0_B.put("Username", "Name" + infixB + "@example.com");
		user_0_B.put("FirstName", "fm" + infixB);
		user_0_B.put("LastName", "ln" + infixB);
		user_0_B.put("Email", "email" + infixB + "@example.com");
		user_0_B.put("ProfileId", "00e80000001C34eAAC");
		user_0_B.put("Alias", "alias0B");
		user_0_B.put("TimeZoneSidKey", "GMT");
		user_0_B.put("LocaleSidKey", "en_US");
		user_0_B.put("EmailEncodingKey", "ISO-8859-1");
		user_0_B.put("LanguageLocaleKey", "en_US");
		user_0_B.put("CommunityNickname", "cn" + infixB);
		createdUsersInB.add(user_0_B);
		
		insertUserInBFlow.process(getTestEvent(user_0_B, MessageExchangePattern.REQUEST_RESPONSE));
	
		Thread.sleep(1001);
		
		// Execution
		executeWaitAndAssertBatchJob(B_INBOUND_FLOW_NAME);

		// Assertions
		Map<String, Object> payload = (Map<String, Object>) queryUser(user_0_B, queryUserFromBFlow); // TODO test NullPayload
		Assert.assertNotNull("Synchronized user should not be null", payload);
		Assert.assertEquals("The user should have been sync and new name must match", user_0_B.get("FirstName"), payload.get("FirstName"));
		Assert.assertEquals("The user should have been sync and new title must match", user_0_B.get("LastName"), payload.get("LastName"));

		// cleanup
		deleteTestUsersFromSandBoxA(createdUsersInB); // will fail because user can't be deleted from SFDC
		deleteTestUsersFromSandBoxB(createdUsersInB);

		// test sfdc -> db
		
		Map<String, Object> user_0_A = new HashMap<String, Object>();
		String infixA = "_0_A_" + ANYPOINT_TEMPLATE_NAME + "_" + System.currentTimeMillis();
		user_0_A.put("Username", "Name" + infixA + "@example.com");
		user_0_A.put("FirstName", "fn" + infixA);
		user_0_A.put("LastName", "ln" + infixA);
		user_0_A.put("Email", "email" + infixA + "@example.com");
		user_0_A.put("ProfileId", "00e80000001C34eAAC");
		user_0_A.put("Alias", "alias0A");
		user_0_A.put("TimeZoneSidKey", "GMT");
		user_0_A.put("LocaleSidKey", "en_US");
		user_0_A.put("EmailEncodingKey", "ISO-8859-1");
		user_0_A.put("LanguageLocaleKey", "en_US");
		user_0_A.put("CommunityNickname", "cn" + infixA);
		createdUsersInA.add(user_0_A);

		MuleEvent event = upsertUserInAFlow.process(getTestEvent(Collections.singletonList(user_0_A), MessageExchangePattern.REQUEST_RESPONSE));
		user_0_A.put("Id", (((UpsertResult) ((List<?>) event.getMessage().getPayload()).get(0))).getId());

		Thread.sleep(1001);

		// Execution
		executeWaitAndAssertBatchJob(A_INBOUND_FLOW_NAME);

		Map<String, Object> payload1 = (Map<String, Object>) queryUser(user_0_A, queryUserFromBFlow); // TODO test NullPayload
		Assert.assertNotNull("Synchronized user should not be null", payload1);
		Assert.assertEquals("The user should have been sync and new name must match", user_0_A.get("FirstName"), payload1.get("FirstName"));
		Assert.assertEquals("The user should have been sync and new title must match", user_0_A.get("LastName"), payload1.get("LastName"));
		
		// cleanup
		SubflowInterceptingChainLifecycleWrapper deleteUsersAfterSfdc2Db = getSubFlow("deleteUsersAfterSfdc2Db");
		deleteUsersAfterSfdc2Db.initialise();
		deleteUsersAfterSfdc2Db.process(getTestEvent(createdUsersInA, MessageExchangePattern.REQUEST_RESPONSE));
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
	
	private void deleteTestUsersFromSandBoxB(List<Map<String, Object>> createdUsersInA) throws InitialisationException, MuleException, Exception {
		SubflowInterceptingChainLifecycleWrapper deleteUserFromBFlow = getSubFlow("deleteUserFromBFlow");
		deleteUserFromBFlow.initialise();
		deleteTestEntityFromSandBox(deleteUserFromBFlow, createdUsersInA);
	}

	private void deleteTestUsersFromSandBoxA(List<Map<String, Object>> createdUsersInB) throws InitialisationException, MuleException, Exception {
		List<Map<String, Object>> createdUsersInA = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> c : createdUsersInB) {
			Map<String, Object> user = invokeRetrieveFlow(queryUserFromAFlow, c);
			if (user != null) {
				createdUsersInA.add(user);
			}
		}
		SubflowInterceptingChainLifecycleWrapper deleteUserFromAFlow = getSubFlow("deleteUserFromAFlow");
		deleteUserFromAFlow.initialise();
		deleteTestEntityFromSandBox(deleteUserFromAFlow, createdUsersInA);
	}
	
	private void deleteTestEntityFromSandBox(SubflowInterceptingChainLifecycleWrapper deleteFlow, List<Map<String, Object>> entitities) throws MuleException, Exception {
		List<String> idList = new ArrayList<String>();
		for (Map<String, Object> c : entitities) {
			idList.add(c.get("Id").toString());
		}
		
		deleteFlow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	protected Map<String, Object> invokeRetrieveFlow(InterceptingChainLifecycleWrapper flow, Map<String, Object> payload) throws Exception {
		MuleEvent event = flow.process(getTestEvent(payload, MessageExchangePattern.REQUEST_RESPONSE));
		Object resultPayload = event.getMessage().getPayload();
		return resultPayload instanceof NullPayload ? null : (Map<String, Object>) resultPayload;
	}

}
