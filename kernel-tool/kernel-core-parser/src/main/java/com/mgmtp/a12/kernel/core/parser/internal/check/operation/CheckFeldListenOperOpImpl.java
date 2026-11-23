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
package com.mgmtp.a12.kernel.core.parser.internal.check.operation;

import java.util.List;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckEntityListenUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckOpUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldListenOperOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.OperationContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType.OperationResultBaseType;

/**
 * Stellt die Prüfungen für <code>FeldListenOperOp</code> bereit.
 * Es wird geprüft:
 * <ul>
 * <li>Alle Operanden sind vom Typ Zahl oder Datum. {@link ParserErrorTypesEnum#MVK_NOT_SORTABLE}
 * <li>Es wird höchstens eine Konstante verwendet.
 * {@link ParserErrorTypesEnum#MVK_TOO_MANY_CONST_FOR_CALC_OP}
 * </ul>
 */
public class CheckFeldListenOperOpImpl extends CheckImplAbstractClass {
	public CheckFeldListenOperOpImpl(FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(FeldListenOperOpContext ctx) {
		CheckEntityListenUtils checker = new CheckEntityListenUtils(fullParser);
		List<OperationContext> ops = ctx.operationListe().operation();
		String opName = ctx.feldListenOperOpName().getText();

		checkMaxOneConstant(ctx, ops, opName);
		OperationResultBaseType firstType = this.getOpType(ops.get(0)).getBaseType();
		if (firstType == OperationResultBaseType.DATETIME) {
			List<OperationResultType> ots = ops.stream().map(this::getOpType).toList();
			OperationResultType commonType = checker.checkOpTypesAreComparableDatetimeType(ots, fullParser.hasBaseYear(), ctx,
					opName);
			addOpType(ctx, commonType);
		} else {
			checkAllOpsLikeNumbers(ops, opName, ParserErrorTypesEnum.MVK_NOT_SORTABLE);
			calcAndAddNumberOpType(ctx, ops);
		}
	}

	private void checkAllOpsLikeNumbers(java.util.List<OperationContext> ops, String opName, ParserErrorTypesEnum errType) {
		ops.forEach(op -> CheckOpUtils.checkOpIsLikeANumber(op, opName, this, errType));
	}

	private void checkMaxOneConstant(FeldListenOperOpContext ctx, java.util.List<OperationContext> ops, String opName) {
		//only one constants may be found: stop counting if 2 have been found
		long numConstants = ops.stream()
				.map(CheckOpUtils::isConstant).filter(b -> b).limit(2).count();
		if (numConstants >= 2) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_TOO_MANY_CONST_FOR_CALC_OP, opName);
			throw createMVKParserError(ctx.operationListe(), errMessage);
		}
	}

	private void calcAndAddNumberOpType(FeldListenOperOpContext ctx, java.util.List<OperationContext> ops) {
		List<Integer> maxFractionals = ops.stream()
				.map(this::getOpType)
				.map(this::getMaxFractionalDigits)
				.toList();
		Integer maxDigits = super.computeUnionOfFractionalDigits(maxFractionals);
		boolean allWithConstant = ops.stream()
				.map(this::getOpType)
				.allMatch(OperationResultType::hasMultiplicativeConstant);
		// Maximizing with a constant should not influence the fractionals
		addNumberOpType(ctx, maxDigits, allWithConstant);
	}

}
