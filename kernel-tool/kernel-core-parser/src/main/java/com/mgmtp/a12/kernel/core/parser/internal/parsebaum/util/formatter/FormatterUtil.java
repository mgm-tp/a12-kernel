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

package com.mgmtp.a12.kernel.core.parser.internal.parsebaum.util.formatter;

import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.Base_ConditionLexer;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.languages.ConditionMultiLanguageHandler;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.CalcOp;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.ComparisonOperation;

/**
 * Helper methods for formatting conditions.
 */
public class FormatterUtil {

	private FormatterUtil() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Returns the arithmetic operator for a calculation operation, i.e. "+" for add, "*" for
	 * multiply and so on.
	 */
	public static String getCalcOperator(CalcOp calcOp) {
		switch (calcOp) {
			case ADD:
				return "+";
			case SUBTRACT:
				return "-";
			case MULTIPLY:
				return "*";
			case DIVIDE:
				return "/";
			case POWER:
				return "^";
			default:
				throw new IllegalArgumentException("Calculation operation " + calcOp + " not supported.");
		}
	}

	/**
	 * Returns the language element that is used as comparison sign for a comparison operation, e.g.
	 * {@literal ">="} for
	 * {@link ComparisonOperation#GE}} or "UngleichMitToleranz1" for
	 * {@link ComparisonOperation#NE1}.
	 */
	public static String getComparisonOperator(ConditionMultiLanguageHandler multiLangHandler, ComparisonOperation vglOp) {
		switch (vglOp) {
			case GT:
				return ">";
			case NE:
				return "!=";
			case LT:
				return "<";
			case EQ:
				return "==";
			case GE:
				return ">=";
			case LE:
				return "<=";
			case NE10:
				return multiLangHandler.getTerminalName(Base_ConditionLexer.UNGLEICH_MIT_TOLERANZ10);
			case NE5:
				return multiLangHandler.getTerminalName(Base_ConditionLexer.UNGLEICH_MIT_TOLERANZ5);
			case NE2:
				return multiLangHandler.getTerminalName(Base_ConditionLexer.UNGLEICH_MIT_TOLERANZ2);
			case NE1:
				return multiLangHandler.getTerminalName(Base_ConditionLexer.UNGLEICH_MIT_TOLERANZ1);
			case NM:
				return multiLangHandler.getTerminalName(Base_ConditionLexer.NICHT_WIE_MUSTER);
			case WM:
				return multiLangHandler.getTerminalName(Base_ConditionLexer.WIE_MUSTER);
			default:
				throw new IllegalArgumentException("Comparison operation " + vglOp + " not supported.");
		}
	}
}
