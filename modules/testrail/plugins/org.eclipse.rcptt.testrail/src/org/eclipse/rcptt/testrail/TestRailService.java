/*******************************************************************************
 * Copyright (c) 2009, 2016 Xored Software Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Xored Software Inc - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.rcptt.testrail;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.rcptt.internal.core.model.Q7TestCase;
import org.eclipse.rcptt.internal.launching.Executable;
import org.eclipse.rcptt.internal.launching.ExecutionSession;
import org.eclipse.rcptt.internal.launching.GroupExecutable;
import org.eclipse.rcptt.internal.launching.PrepareExecutionWrapper;
import org.eclipse.rcptt.internal.launching.ecl.EclScenarioExecutable;
import org.eclipse.rcptt.internal.testrail.ErrorMessages;
import org.eclipse.rcptt.internal.testrail.TestRailAPIClient;
import org.eclipse.rcptt.internal.testrail.TestRailPlugin;
import org.eclipse.rcptt.launching.IExecutable;
import org.eclipse.rcptt.launching.ITestEngine;
import org.eclipse.rcptt.reporting.util.ReportUtils;
import org.eclipse.rcptt.sherlock.core.model.sherlock.report.Node;
import org.eclipse.rcptt.sherlock.core.model.sherlock.report.Report;
import org.eclipse.rcptt.testrail.domain.TestRailTestResult;
import org.eclipse.rcptt.testrail.domain.TestRailTestRun;

public class TestRailService implements ITestEngine {
	private static final String TESTRAIL_ID_PARAM = "testrail-id";
	private static final String TESTRAILCONFIG_ADDRESS_PARAM = "testRailAddress";
	private static final String TESTRAILCONFIG_USERNAME_PARAM = "testRailUsername";
	private static final String TESTRAILCONFIG_PASSWORD_PARAM = "testRailPassword";
	private static final String TESTRAILCONFIG_PROJECTID_PARAM = "testRailProjectId";
	private static final String TESTRESULT_CONTEXT_PREFIX = "__Contexts:__ ";
	private static final String TESTRESULT_FAILMSG_PREFIX = "__Fail message:__\n";
	private static final String TESTRUN_DEFAULT_NAME = "Tests";

	private TestRailAPIClient testRailAPI;
	private boolean testRailEnabled;
	private String testRunId;
	private Map<String, String> config;

	public TestRailService() {
	}

	@Override
	public void configuredTestRunStarted(Map<String, String> config, List<Q7TestCase> tests) {
		applyConfig(config);

		TestRailTestRun testRunDraft = createTestRunDraft(tests);
		if (testRunDraft == null) {
			return;
		}

		TestRailTestRun testRun = testRailAPI.addRun(testRunDraft);
		if (testRun != null) {
			this.testRunId = testRun.getId();
		}
	}

	@Override
	public void configuredTestRunCompleted() {
		cleanConfig();
	}

	@Override
	public void sessionStarted(ExecutionSession session) {
		if (config == null || config.isEmpty()) {
			applyDefaultConfig();
		} else {
			return;
		}
		if (!testRailEnabled) {
			return;
		}
		if (testRailAPI == null) {
			return;
		}

		TestRailTestRun testRunDraft = createTestRunDraft(session);
		if (testRunDraft == null) {
			return;
		}

		TestRailTestRun testRun = testRailAPI.addRun(testRunDraft);
		if (testRun != null) {
			this.testRunId = testRun.getId();
		}
	}

	@Override
	public void sessionCompleted(ExecutionSession session) {
	}

	@Override
	public void executionStarted(EclScenarioExecutable scenario) {
	}

	@Override
	public void executionCompleted(EclScenarioExecutable scenario, Report report) {
		if (!testRailEnabled) {
			return;
		}
		if (testRailAPI == null) {
			return;
		}
		if (testRunId == null) {
			return;
		}

		TestRailTestResult testResultDraft = createTestResultDraft(scenario, report);
		if (testResultDraft == null) {
			return;
		}

		testRailAPI.addResultForTestCase(testResultDraft);
	}

	private void applyDefaultConfig() {
		this.testRunId = null;
		this.testRailEnabled = TestRailPlugin.getTestRailState();

		String address = TestRailPlugin.getTestRailAddress();
		String username = TestRailPlugin.getTestRailUsername();
		String password = TestRailPlugin.getTestRailPassword();
		if (password == null) {
			TestRailPlugin.log(ErrorMessages.TestRailService_FailedToSetUpConnection);
			this.testRailAPI = null;
			return;
		}
		String projectId = TestRailPlugin.getTestRailProjectId();
		this.testRailAPI = new TestRailAPIClient(address, username, password, projectId);
	}

	private void applyConfig(Map<String, String> config) {
		String address = config.get(TESTRAILCONFIG_ADDRESS_PARAM);
		String username = config.get(TESTRAILCONFIG_USERNAME_PARAM);
		String password = config.get(TESTRAILCONFIG_PASSWORD_PARAM);
		String projectId = config.get(TESTRAILCONFIG_PROJECTID_PARAM);

		this.testRunId = null;
		this.testRailEnabled = true;
		this.testRailAPI = new TestRailAPIClient(address, username, password, projectId);
		this.config = config;
	}

	private void cleanConfig() {
		this.testRunId = null;
		this.testRailEnabled = false;
		this.testRailAPI = null;
		this.config = Collections.emptyMap();
	}

	private TestRailTestRun createTestRunDraft(ExecutionSession session) {
		String name = getTestRunName(session.getName());
		List<String> caseIds = getTestRunCaseIds(session);
		if (caseIds.isEmpty()) {
			return null;
		}

		TestRailTestRun testRunDraft = new TestRailTestRun();
		testRunDraft.setName(name);
		testRunDraft.setIncludeAll(false);
		testRunDraft.setCaseIds(caseIds);
		return testRunDraft;
	}

	private TestRailTestRun createTestRunDraft(List<Q7TestCase> tests) {
		String name = getTestRunName(TESTRUN_DEFAULT_NAME);
		List<String> caseIds = tests.stream()
				.map(test -> getTestRailId(test))
				.filter(testRailId -> testRailId != null && !testRailId.equals(""))
				.collect(Collectors.toList());
		if (caseIds.isEmpty()) {
			return null;
		}

		TestRailTestRun testRunDraft = new TestRailTestRun();
		testRunDraft.setName(name);
		testRunDraft.setIncludeAll(false);
		testRunDraft.setCaseIds(caseIds);
		return testRunDraft;
	}

	private TestRailTestResult createTestResultDraft(EclScenarioExecutable scenario, Report report) {
		Q7TestCase testCase = (Q7TestCase) scenario.getActualElement();
		String testCaseId = getTestRailId(testCase);
		if (testCaseId == null) {
			return null;
		}

		String testCaseStatus = getTestRailStatus(scenario);
		if (testCaseStatus == null) {
			return null;
		}

		String testCaseDuration = getTestRailDuration(report);
		String testCaseComment = getTestRailComment(scenario, report);

		TestRailTestResult testResultDraft = new TestRailTestResult();
		testResultDraft.setRunId(testRunId);
		testResultDraft.setCaseId(testCaseId);
		testResultDraft.setStatus(testCaseStatus);
		testResultDraft.setElapsed(testCaseDuration);
		testResultDraft.setComment(testCaseComment);
		return testResultDraft;
	}

	private EclScenarioExecutable getScenario(Executable wrapper) {
		if (wrapper instanceof PrepareExecutionWrapper) {
			IExecutable executable = ((PrepareExecutionWrapper) wrapper).getExecutable();
			if (executable instanceof GroupExecutable) {
				IExecutable scenario = ((GroupExecutable) executable).getRoot();
				if (scenario instanceof EclScenarioExecutable)
					return (EclScenarioExecutable) scenario;
			}
		}
		return null;
	}

	private String getTestRailId(Q7TestCase testCase) {
		try {
			String testCaseId = testCase.getProperties().get(TESTRAIL_ID_PARAM);
			return testCaseId.substring(1); // remove "C"
		} catch (Exception e) {
			TestRailPlugin.log(
					MessageFormat.format(ErrorMessages.TestRailService_ErrorWhileGettingTestCaseProperty,
							TESTRAIL_ID_PARAM),
					e);
			return null;
		}
	}

	private String getTestRunName(String name) {
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
		LocalDateTime localDate = LocalDateTime.now();
		return MessageFormat.format("{0} {1}", name, dateFormatter.format(localDate));
	}

	private List<String> getTestRunCaseIds(ExecutionSession session) {
		final Executable[] executables = session.getExecutables();
		final List<Q7TestCase> testCases = Arrays.stream(executables)
				.map(wrapper -> (Q7TestCase) getScenario(wrapper).getActualElement())
				.filter(testCase -> testCase != null)
				.collect(Collectors.toList());

		final List<String> caseIds = testCases.stream()
				.map(scenario -> getTestRailId(scenario))
				.filter(testRailId -> testRailId != null && !testRailId.equals(""))
				.collect(Collectors.toList());

		return caseIds;
	}

	private String getTestRailDuration(Report report) {
		Node reportRoot = report.getRoot();
		long duration = (reportRoot.getEndTime() - reportRoot.getStartTime()) / 1000;

		if (duration != 0) {
			return MessageFormat.format("{0}s", String.format("%d", duration));
		}
		return null;
	}

	private String getTestRailStatus(EclScenarioExecutable scenario) {
		int severity = scenario.getResultStatus().getSeverity();

		switch (severity) {
		case IStatus.OK:
		case IStatus.INFO:
			return "1";
		case IStatus.WARNING:
		case IStatus.ERROR:
			return "5";
		case IStatus.CANCEL:
			return null;
		}
		return null;
	}

	private String getTestRailComment(EclScenarioExecutable scenario, Report report) {
		String testCaseComment = "";

		List<String> variantName = scenario.getVariantName();
		if (variantName != null && !variantName.isEmpty()) {
			String contextNames = variantName.stream()
					.collect(Collectors.joining(", "))
					.toString();
			testCaseComment = TESTRESULT_CONTEXT_PREFIX + contextNames;
		}

		int testCaseSeveriry = scenario.getResultStatus().getSeverity();
		if (testCaseSeveriry == IStatus.ERROR) {
			if (!testCaseComment.equals("")) {
				testCaseComment += "\n\n";
			}

			Node reportRoot = report.getRoot();
			testCaseComment += TESTRESULT_FAILMSG_PREFIX + ReportUtils.getFailMessage(reportRoot);
		}

		return testCaseComment;
	}
}
