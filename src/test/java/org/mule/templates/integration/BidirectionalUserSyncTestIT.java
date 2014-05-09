package org.mule.templates.integration;

import java.util.ArrayList;
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
import org.mule.processor.chain.InterceptingChainLifecycleWrapper;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.templates.AbstractTemplatesTestCase;
import org.mule.transport.NullPayload;
import org.mule.util.UUID;

import com.mulesoft.module.batch.BatchTestHelper;

/**
 * The objective of this class is validating the correct behavior of the flows
 * for this Mule Anypoint Template
 * 
 */
@SuppressWarnings("unchecked")
public class BidirectionalUserSyncTestIT extends AbstractTemplatesTestCase {

	private static final String ANYPOINT_TEMPLATE_NAME = "user-bidirectional-sync";
	private static final String A_INBOUND_FLOW_NAME = "triggerSyncFromAFlow";
	private static final String B_INBOUND_FLOW_NAME = "triggerSyncFromBFlow";
	private static final int TIMEOUT_MILLIS = 60;

	private SubflowInterceptingChainLifecycleWrapper updateUserInAFlow;
	private SubflowInterceptingChainLifecycleWrapper updateUserInBFlow;
	private InterceptingChainLifecycleWrapper queryUserFromAFlow;
	private InterceptingChainLifecycleWrapper queryUserFromBFlow;
	private BatchTestHelper batchTestHelper;

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
		deleteTestAccountsFromSandBoxA(createdUsersInB);
		deleteTestAccountsFromSandBoxB(createdUsersInB);
	}

	private void stopAutomaticPollTriggering() throws MuleException {
		stopFlowSchedulers(A_INBOUND_FLOW_NAME);
		stopFlowSchedulers(B_INBOUND_FLOW_NAME);
	}

	private void getAndInitializeFlows() throws InitialisationException {
		// Flow for updating a user in A instance
		updateUserInAFlow = getSubFlow("updateUserInAFlow");
		updateUserInAFlow.initialise();

		// Flow for updating a user in B instance
		updateUserInBFlow = getSubFlow("updateUserInBFlow");
		updateUserInBFlow.initialise();

		// Flow for querying the user in A instance
		queryUserFromAFlow = getSubFlow("queryUserFromAFlow");
		queryUserFromAFlow.initialise();

		// Flow for querying the user in B instance
		queryUserFromBFlow = getSubFlow("queryUserFromBFlow");
		queryUserFromBFlow.initialise();
	}

	@Test
	public void whenUpdatingAnUserInInstanceBTheBelongingUserGetsUpdatedInInstanceA() throws MuleException, Exception {
		Map<String, Object> user_0_B = new HashMap<String, Object>();
		user_0_B.put("Id", UUID.getUUID());
		user_0_B.put("Username", "Username1@bar.com");
		user_0_B.put("FirstName", "FirstName1");
		user_0_B.put("LastName", "LastName1");
		user_0_B.put("Email", "foo121@bar.com");
		user_0_B.put("ProfileId", "00e80000001C34eAAC");
		user_0_B.put("Alias", "alias1");
		user_0_B.put("TimeZoneSidKey", "GMT");
		user_0_B.put("LocaleSidKey", "en_US");
		user_0_B.put("EmailEncodingKey", "ISO-8859-1");
		user_0_B.put("LanguageLocaleKey", "en_US");
		createdUsersInB.add(user_0_B);
		
		SubflowInterceptingChainLifecycleWrapper createAccountInAFlow = getSubFlow("insertUserInBFlow");
		createAccountInAFlow.initialise();
	
		createAccountInAFlow.process(getTestEvent(user_0_B, MessageExchangePattern.REQUEST_RESPONSE));
	
		// Execution
		executeWaitAndAssertBatchJob(B_INBOUND_FLOW_NAME);

		// Assertions
		Map<String, Object> payload = (Map<String, Object>) queryUser(user_0_B, queryUserFromBFlow);
		Assert.assertNotNull("Synchronized user should not be null", payload);
		Assert.assertEquals("The user should have been sync and new name must match", user_0_B.get("FirstName"), payload.get("FirstName"));
		Assert.assertEquals("The user should have been sync and new title must match", user_0_B.get("LastName"), payload.get("LastName"));
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
	
	private void deleteTestAccountsFromSandBoxB(List<Map<String, Object>> createdAccountsInA) throws InitialisationException, MuleException, Exception {
		SubflowInterceptingChainLifecycleWrapper deleteAccountFromBFlow = getSubFlow("deleteUserFromBFlow");
		deleteAccountFromBFlow.initialise();
		deleteTestEntityFromSandBox(deleteAccountFromBFlow, createdAccountsInA);
	}

	private void deleteTestAccountsFromSandBoxA(List<Map<String, Object>> createdUsersInB) throws InitialisationException, MuleException, Exception {
		List<Map<String, Object>> createdUsersInA = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> c : createdUsersInB) {
			Map<String, Object> account = invokeRetrieveFlow(queryUserFromAFlow, c);
			if (account != null) {
				createdUsersInA.add(account);
			}
		}
		SubflowInterceptingChainLifecycleWrapper deleteAccountFromAFlow = getSubFlow("deleteUserFromAFlow");
		deleteAccountFromAFlow.initialise();
		deleteTestEntityFromSandBox(deleteAccountFromAFlow, createdUsersInA);
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
