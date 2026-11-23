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

package com.mgmtp.a12.kernel.core.tool.a12internal.api.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblemReporter;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.ProblemException;
import com.mgmtp.a12.model.utils.OnlyForUsage;

/**
 * The interface <code>ICalculationService</code> defines methods to
 * <ul>
 * <li>check the consistency of a calculation rule,</li>
 * <li>create the calculation text for a rule, and</li>
 * <li>extract the calculation information to be passed to the code generator.</li>
 * </ul>
 * <p>
 * To prepare the calculation info for code generation from a set of rules,
 * <ol>
 * <li>ICalculationRule-Objects need to be created for all relevant rules.</li>
 * <li>These should be grouped by the calculated field.</li>
 * <li>Then all the rules should be checked by
 * {@link #checkCalculationRule(ICalculationRule, List, IProblemReporter)}, using the grouped rules
 * in the second parameter.</li>
 * <li>Then the calculationInfo can be extracted via
 * {@link #extractCalculationInfo(ICalculationRule, IProblemReporter)}</li>
 * </ol>
 */
@OnlyForUsage
public interface ICalculationService extends IBaseCalculationService {

	public interface IRefType {
	}

	public interface IDependentFieldRef extends IRefType {
		String getReferencedFieldName();
	}

	public interface IRuleRef extends IRefType {
		String getFullName();
	}

	/**
	 * Precondition:
	 * The <code>condition</code> must not be expanded, i.e. the condition text has to be parsed
	 * with {@link ExpandMode#NO_EXPAND}.
	 */
	public boolean checkCalculationRule(ICalculationRule calculationRule, ICondition condition, IProblemReporter pm);

	/**
	 * This method should only be called on a ICalculationService initialized with the calculation rules. For A12,
	 * the initialization of the calculation rules is done in the DocumentModelService method.
	 *
	 * @param extraDependencies
	 * 		the dependencies of dependent fields as a Map from dependent fields to all its operands.
	 * @return {@link Optional#empty()} if no cycle was detected. Otherwise a list of pairs that describe the detected cycle: the
	 * 		cycle is spanned by the field names given in the first entry of the Pair. The second entry of the Pair is either a RuleRef
	 * 		containing the full name of the rule that links the current field with the next one or a DependentFieldRef if the fields
	 * 		are
	 * 		linked by a dependent field reference (which was given via extraDependencies).
	 *
	 * @throws ProblemException
	 * 		if there are problems parsing the calculations.
	 */
	Optional<List<Pair<String, IRefType>>> checkForCycle(Map<String, Set<String>> extraDependencies) throws
			ProblemException;
}
