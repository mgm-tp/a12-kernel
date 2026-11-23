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

package com.mgmtp.a12.kernel.core.codegen.internal.generator.vba.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.mgmtp.a12.kernel.core.codegen.internal.generator.CodeGenData;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.RuleInfo;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.RuleIterationLimitCreator;
import com.mgmtp.a12.kernel.core.codegen.internal.utils.VBARegelCheckerVisitor;
import com.mgmtp.a12.kernel.core.extmpspecifics.a12internal.api.ado.IExtmpRule;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.util.AnalyseService;
import com.mgmtp.a12.kernel.core.parser.internal.util.ReferencedIdsUtil;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.utils.IndexArt;
import com.mgmtp.a12.kernel.core.service.internal.errortext.api.IErrortextParameter;
import com.mgmtp.a12.kernel.core.service.internal.errortext.api.IIndexFieldParameter;
import com.mgmtp.a12.kernel.core.service.internal.errortext.api.IUSBParameter;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IRule;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblem;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblemReporter;
import com.mgmtp.a12.kernel.core.utils.internal.MVKCoreError;
import com.mgmtp.a12.model.notification.Severity;

/**
 * Computes the relevant rules for a VBA module.
 */
public class VBARuleFilter {

	private final CodeGenData data;
	private final List<RuleInfo> ruleInfos;
	private final IProblemReporter pr;

	public VBARuleFilter(CodeGenData data, List<RuleInfo> ruleInfos, IProblemReporter pr) {
		this.data = data;
		this.ruleInfos = ruleInfos;
		this.pr = pr;
	}

	/**
	 * Filters the given rules s.t. only rules
	 * * with an iteration in the relevant groups
	 * * with supported language elements
	 * * without the usage of '$#ACTION' in error texts
	 * * with severity other than '<code>INFO</code>'
	 * are returned.
	 */
	public List<RuleInfo> filterRules(Set<String> relevantGroups, Set<String> fields) {

		List<RuleInfo> filteredRules = new ArrayList<>();

		ruleLoop:
		for (RuleInfo rule : ruleInfos) {
			IEntity errorEntity = rule.getErrorEntity();

			if (errorEntity.isField() && !fields.contains(errorEntity.getFullName()) ||
					errorEntity.isGroup() && !relevantGroups.contains(errorEntity.getFullName())) {
				continue;
			}

			if (errorEntity.isField() && errorEntity.getMax()[errorEntity.getMax().length - 1] > 1) {
				// must be an iteration on all rows
				if (!isRowIterationForAllRows(rule)) {
					continue;
				}
			} else {
				// We have to determine whether the iteration level of the rule contains the repetition level of a relevant group.
				// If this is not the case we ignore the rule.
				if (relevantGroups.stream()
						.filter(k -> EntityUtils.isDescendantOrEqual(rule.getErrorEntity().getFullName(), k))
						.map(EntityUtils::getLevel)
						.noneMatch(e -> isIterationOnLevel(rule, data, e + 1))) {
					continue;
				}
			}
			// Rule condition ok?
			// This is tested before the referenced fields to show the user which language elements are supported.
			String errorMsg = isSupportedCondition(rule.getCondition(), rule.getFullName(), data.getEc());
			if (errorMsg != null) {
				// Currently, no error is generated, the rule is only ignored.
				report(errorMsg, Severity.INFO);
				continue;
			}

			Set<String> refFieldNames = ReferencedIdsUtil.getDirectlyReferencedFieldNames(rule.getCondition(), data.getEc());
			boolean enthaeltAlle = true;
			for (String fieldName : refFieldNames) {
				if (!fields.contains(fieldName)) {
					enthaeltAlle = false;
					break;
				}
			}
			if (!enthaeltAlle) {
				continue;
			}

			for (IErrortextParameter parameter : rule.getErrorTextParameters().values()) {
				if (parameter instanceof IUSBParameter) {
					report(String.format(
							"Die Regel '%s' enthält einen Fehlertext mit einer '#'-Fehlertextersetzung, die von VBA nicht unterstützt wird. [VBA_ERROR_TEXT_PARAMETER_NOT_SUPPORTED]",
							rule.getFullName()), Severity.INFO);
					continue ruleLoop;
				}
				if (parameter instanceof IIndexFieldParameter) {
					report(String.format("Die Regel '%s' enthält einen Fehlertext mit einer Indexfeldreferenz. " +
							"[VBA_ERROR_TEXT_PARAMETER_NOT_SUPPORTED]", rule.getFullName()), Severity.INFO);
					continue ruleLoop;
				}
			}

			if (rule.getSeverityType() == IRule.RuleSeverityType.INFO) {
				report(String.format("Die Regel '%s' hat den nicht unterstützten Typ 'Info'. " +
						"[VBA_INFO_RULE_NOT_SUPPORTED]", rule.getFullName()), Severity.INFO);
				continue;
			}

			filteredRules.add(rule);
		}

		// Renumber rules
		// Not renumbering the rules leads to non-compiling VBA code.
		AtomicInteger ruleNumber = new AtomicInteger();
		filteredRules.forEach(r -> r.setRuleNumber(ruleNumber.getAndIncrement()));
		return filteredRules;
	}

	private void report(String message, Severity severity) {
		IProblem problem = new MVKCoreError(message, severity, this);
		pr.reportProblem(problem);
	}

	private String isSupportedCondition(ICondition condition, String ruleName, IEC ec) {
		// check operators
		VBARegelCheckerVisitor vbaRegelCheckerVisitor = new VBARegelCheckerVisitor(ruleName, ec);
		condition.traverse(vbaRegelCheckerVisitor);
		return vbaRegelCheckerVisitor.getFehlerMeldung();
	}

	private boolean isRowIterationForAllRows(RuleInfo rule) {
		IRule r = rule.getRule();
		boolean rowIterationRestrictionPresent = false;
		if (r instanceof IExtmpRule) {
			rowIterationRestrictionPresent = ((IExtmpRule) r).getRowIterationRestriction().isPresent();
		}

		return AnalyseService.referenziertIteration(rule.getCondition(), IndexArt.ZEILE) && !rowIterationRestrictionPresent;
	}

	/**
	 * Tests whether the given rule iterates over the given label. For this the USB is not relevant.
	 * <p>
	 * An iteration is ______ if for a level the end is greater than the start. Using this method
	 * special cases such as an iteration from 8 to 8 are correctly detected as no iteration.
	 *
	 * @param repetitionLevel
	 * 		level on which the iteration is checked. 0 corresponds to the USB, 1 to the
	 * 		rootgroup, ....
	 * @return True if the rule is an iteration on the given level.
	 */
	private boolean isIterationOnLevel(RuleInfo rule, CodeGenData data, int repetitionLevel) {
		RuleIterationLimitCreator r = new RuleIterationLimitCreator(rule, data);

		long[] iterStart = r.getEbenenArrayFuerIterationsStart();
		long[] iterEnde = r.getEbenenArrayFuerIterationsEnde();
		if (repetitionLevel < 0 || repetitionLevel > iterStart.length) {
			return false;
		}
		return iterEnde[repetitionLevel] > iterStart[repetitionLevel];

	}

}
