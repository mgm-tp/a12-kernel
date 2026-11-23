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

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckOpUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldOperOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.OperationContext;

/**
 * Stellt die Prüfungen für <code>FeldOperOp</code> bereit.
 * Es wird geprüft:
 * <ul>
 * <li>Alle Operanden sind Zahlen.{@link ParserErrorTypesEnum#MVK_NO_NUMBER}
 *
 * </ul>
 * <p>
 * Stellt die Prüfungen für <code>FeldOperOp</code> bereit.
 * Es wird geprüft:
 * <ul>
 * <li>Es wird geprüft, dass die Operation auf einer Zahl spezifiziert wird und keine Konstante
 * verwendet wird. {@link ParserErrorTypesEnum#MVK_NO_CONST} und
 * {@link ParserErrorTypesEnum#MVK_NO_NUMBER}
 * </ul>
 */
public class CheckFeldOperOpImpl extends CheckImplAbstractClass {
	public CheckFeldOperOpImpl(FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(FeldOperOpContext ctx) {
		OperationContext opCtx = ctx.operation();
		String opName = ctx.feldOperOpName().getText();

		CheckOpUtils.checkOpNoConst(opCtx, opName, this);
		CheckOpUtils.checkOpIsLikeANumber(opCtx, opName, this, ParserErrorTypesEnum.MVK_NO_NUMBER);

		addOpType(ctx, getOpType(ctx.operation()));
	}

}
