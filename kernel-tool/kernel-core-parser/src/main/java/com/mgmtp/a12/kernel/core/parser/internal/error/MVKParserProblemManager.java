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
package com.mgmtp.a12.kernel.core.parser.internal.error;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblem;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblemReporter;
import com.mgmtp.a12.model.notification.Severity;

/**
 * Problem manager for all problems in parsing and lexing.
 * The problem manager is used during parsing, lexing and code generation.
 * After the parsing, it is checked if errors have occured. In case of errors, the
 * problems are reported to the problem manager provided by the caller.
 */
public class MVKParserProblemManager implements IProblemReporter {
	private List<IProblem> problemList = new ArrayList<>();
	private Map<Severity, Integer> problemCountMap = new EnumMap<>(Severity.class);

	@Override
	public void reportProblem(IProblem problem) {
		Severity sev = problem.getSeverity();
		int noOfProblems = getNumberOfProblems(sev);
		problemCountMap.put(sev, noOfProblems + 1);
		problemList.add(problem);
	}

	public boolean hasProblemOccured(Severity sevType) {
		Integer i = problemCountMap.get(sevType);
		if (i == null) {
			return false;
		}
		return problemCountMap.get(sevType) > 0;
	}

	public List<IProblem> getProblems() {
		return problemList;
	}

	public int getNumberOfProblems(Severity sevType) {
		return hasProblemOccured(sevType) ? problemCountMap.get(sevType) : 0;
	}

	/**
	 * Copies all the problems contained in this problem manager to another {@link IProblemReporter}
	 * .
	 *
	 * @param destPm
	 * 		the problem reporter, the problem should be transferred to.
	 */
	public void transferProblems(IProblemReporter destPm) {
		for (IProblem p : problemList) {
			destPm.reportProblem(p);
		}
	}
}
