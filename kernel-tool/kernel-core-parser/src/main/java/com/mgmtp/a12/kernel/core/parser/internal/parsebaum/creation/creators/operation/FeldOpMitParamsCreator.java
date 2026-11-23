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

import org.antlr.v4.runtime.Token;

import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldOpMitParamsContext;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.CreatorUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.FeldIdCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.FieldIdOperationWithParametersImpl;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.FieldIdOperationName;

/**
 * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>
 * feldOpMitParams: <p>
 * feldOpMitParamsName '(' entitySpec ',' start = ZiffernFolge ','ende = ZiffernFolge ')' ; <p>
 * feldOpMitParamsName: z= 'BereichAlsZahl' | s='BereichAlsString';
 * </code>
 * <p>
 * das richtige Parsebaum-Objekt.
 */
public class FeldOpMitParamsCreator {

	private FeldOpMitParamsCreator() {
		throw new IllegalStateException("Utility class");
	}

	public static void create(FeldOpMitParamsContext ctx, ParseTreeCreator tc) {
		FieldIdOperationName name = extractOperationNameFromContext(ctx);
		IEntityId feldId = FeldIdCreator.create(ctx.entitySpec(), tc);

		//In der Sprache wird 1 verwendet,  wenn intern 0 benutzt wird.
		// Daher muss der Wert -1 genommen werden.
		int begin = Integer.parseInt(ctx.begin.getText()) - 1;
		int end = Integer.parseInt(ctx.ende.getText());

		tc.push(new FieldIdOperationWithParametersImpl(name, feldId, begin, end));
	}

	/**
	 * Konvertiert den Operationsnamen aus dem Kontext in die internen Namen.
	 */
	private static FieldIdOperationName extractOperationNameFromContext(FeldOpMitParamsContext ctx) {
		Token t = ctx.feldOpMitParamsName().start;
		int type = t.getType();
		switch (type) {
			case GrammarOnly_ConditionParser.BEREICH_ALS_STRING:
				return FieldIdOperationName.RangeAsString;
			case GrammarOnly_ConditionParser.BEREICH_ALS_ZAHL:
				return FieldIdOperationName.RangeAsNumber;
			default:
				throw new IllegalArgumentException(CreatorUtils.getUnknownTokenErrorMessage(t));
		}
	}

}
