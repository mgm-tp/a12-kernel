/*
 * SPDX-License-Identifier: EUPL-1.2 OR LicenseRef-commercial
 *
 * Copyright (c) 2012-2026 mgm technology partners GmbH
 *
 * Dual License
 * ------------
 * This source file is part of the mgm A12 Platform and available under
 * a choice of two different licenses:
 *
 * 1. Open-Source License – EUPL v1.2
 *    You may redistribute and/or modify this file under the terms of the
 *    European Union Public License, version 1.2 - see https://eupl.eu/.
 *
 * 2. Commercial License
 *    Alternatively, you may obtain a commercial license from
 *    mgm technology partners GmbH, that permits use of this software
 *    under different terms (including support and maintenance services).
 *
 *    Please contact a12-license@mgm-tp.com for more information.
 *
 * You must select and comply with exactly one of the above license options.
 *
 * Warranty Disclaimer (applies to either option)
 * ----------------------------------------------
 * THIS SOFTWARE IS PROVIDED “AS IS” AND WITHOUT WARRANTY OF ANY KIND,
 * WHETHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT, EXCEPT WHERE SUCH DISCLAIMERS ARE HELD TO BE
 * LEGALLY INVALID. SEE THE RESPECTIVE LICENSE TEXT FOR DETAILS.
 */

package com.mgmtp.a12.kernel.core.onTheFlyTesting.internal;

import com.mgmtp.a12.kernel.core.rt.a12internal.validation.ResultBaseImpl;
import com.mgmtp.a12.kernel.core.rt.a12internal.validation.ResultMessageV1;
import com.mgmtp.a12.kernel.core.rt.a12internal.validation.IIdentifier;
import com.mgmtp.a12.kernel.core.rt.a12internal.validation.IResult;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblemReporter;
import com.mgmtp.a12.kernel.core.utils.internal.MVKCoreError;
import com.mgmtp.a12.model.notification.Severity;

/**
 * Reports all infos, hints, and errors to the {@link IProblemReporter}.
 */
public class ResultsCollector extends ResultBaseImpl implements IResult {

	private final IProblemReporter pr;

	public ResultsCollector(IProblemReporter pr) {
		this.pr = pr;
	}

	@Override protected void messageV1Hook(ResultMessageV1 messageV1) {
		Severity mdSeverity = switch (messageV1.severity()) { // this enum mapping could be extracted to a util place and deduplicated
			case INFO -> Severity.INFO;
			case HINT -> Severity.WARNING;
			case ERROR -> Severity.ERROR;
		};
		pr.reportProblem(
				new MVKCoreError(messageV1.messageStr(), mdSeverity, deriveOrigin(messageV1.ruleName(), messageV1.field())));
	}

	private String deriveOrigin(String ruleName, IIdentifier field) {
		if (IResult.FORMAL_VALIDATION_RULE_NAME.equals(ruleName)) {
			return String.format("%s in %s", ruleName, field.getName());
		} else {
			return ruleName;
		}
	}
}
