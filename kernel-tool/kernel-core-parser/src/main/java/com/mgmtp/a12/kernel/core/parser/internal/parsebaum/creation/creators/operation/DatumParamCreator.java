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
package com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.creators.operation;

import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.DatumParamContext;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.ConstantOperationImpl;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.OperationType;
import com.mgmtp.a12.kernel.core.utils.internal.CommonUtils;

/**
 * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>datumParam: entitySpec  | s=StringLiteral;
 * </code>
 * <p>
 * das richtige Parsebaum-Objekt.
 */
public class DatumParamCreator {

	private DatumParamCreator() {
		throw new IllegalStateException("Utility class");
	}

	public static void create(DatumParamContext ctx, ParseTreeCreator tc) {

		if (ctx.s != null) { //StringLiteral
			String s = CommonUtils.removeAndUnescapeQuotesAndRemoveCrFromCrLf(ctx.s.getText());
			tc.push(new ConstantOperationImpl(s, OperationType.STRING));
		} else if (ctx.datumExtractOp() != null) {
			// nothing to do: DateExtractionOperationImpl already on tc.opStack
		} else {
			// <code> entitySpec</code> muss in eine FeldWert-Operation umgewandelt werden.
			// wird in der Umwandlungsmethode auf den Stack gespeichert.
			FeldWertOpCreator.createFeldWertOpFromEntityCtx(ctx.entitySpec(), tc);
		}
	}
}
