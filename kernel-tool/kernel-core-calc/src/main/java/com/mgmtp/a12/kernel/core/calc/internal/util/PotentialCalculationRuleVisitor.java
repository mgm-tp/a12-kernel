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

package com.mgmtp.a12.kernel.core.calc.internal.util;

import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IComparisonCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IFieldIdOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.ComparisonOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.FieldIdOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.OperationType;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.ast.ASTVisitor;

/**
 * This visitor analyzes if a rule is a calculation rule or may be transformed to a calculation
 * rule. It uses a simple heuristic: a rule may be transformed to a calculation rule if it contains
 * a comparison of a number field value with an unequal or tolerance operation.
 */
public class PotentialCalculationRuleVisitor extends ASTVisitor {

	private boolean result = false;

	/**
	 * Checks, if the condition contains a subcondition of the following structures:
	 * <ul>
	 * <li><code>[field] &lt;cmpOp&gt;</code> ...</li>
	 * <li>... <code>&lt;cmpOp&gt; [field]</code></li>
	 * </ul>
	 * Where <code>field</code> is a number field and <code>&lt;cmpOp&gt;</code> is one of !=,
	 * UngleichMitToleranz1, UngleichMitToleranz2, UngleichMitToleranz5, UngleichMitToleranz10
	 * (see {@link ComparisonOperation#NE}, {@link ComparisonOperation#NE1},
	 * {@link ComparisonOperation#NE2}, {@link ComparisonOperation#NE5} and
	 * {@link ComparisonOperation#NE10}).
	 *
	 * @param condition
	 * 		parsed condition (expanded or non-expanded)
	 * @return true, if the condition is a potential calculation rule, false otherwise
	 */
	public static boolean isPotentialCalculationRule(ICondition condition) {
		if (condition == null) {
			return false;
		}
		PotentialCalculationRuleVisitor visitor = new PotentialCalculationRuleVisitor();
		condition.traverse(visitor);
		return visitor.result;
	}

	private PotentialCalculationRuleVisitor() {
	}

	@Override
	public boolean visit(IComparisonCondition condition) {
		if (isCalculationComparison(condition)) {
			result = true;
		}
		return false; // stop traversal
	}

	private boolean isCalculationComparison(IComparisonCondition condition) {
		return isUnequalComparison(condition.getCmpOp()) && (isNumberFieldValue(condition.getOperation1()) || isNumberFieldValue(
				condition.getOperation2()));
	}

	private boolean isUnequalComparison(ComparisonOperation cmpOp) {
		return (cmpOp == ComparisonOperation.NE) || (cmpOp == ComparisonOperation.NE1) || (cmpOp == ComparisonOperation.NE2)
				|| (cmpOp == ComparisonOperation.NE5) || (cmpOp == ComparisonOperation.NE10);
	}

	private boolean isNumberFieldValue(IOperation op) {
		if (op instanceof IFieldIdOperation) {
			IFieldIdOperation fop = (IFieldIdOperation) op;
			return (fop.getOperatorName() == FieldIdOperationName.FieldValue) && (fop.getType() == OperationType.NUMBER);
		}
		return false;
	}

}
