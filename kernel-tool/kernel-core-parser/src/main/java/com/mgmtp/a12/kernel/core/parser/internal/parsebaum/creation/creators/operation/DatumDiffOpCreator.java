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
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.DatumDiffOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.CreatorUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.datum.DateDiffOperationImpl;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateDiffOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.DateDiffOperationName;

/**
 * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>
 * datumDiffOp: datumDiffOpName '(' da1=datumAngabe ',' da2=datumAngabe ')' ;
 * <p>
 * datumDiffOpName: 'DifferenzInTagen' | 'DifferenzInMonaten' | 'DifferenzInJahren';
 * </code>
 * <p>
 * das richtige Parsebaum-Objekt.
 */
public class DatumDiffOpCreator {

	private DatumDiffOpCreator() {
		throw new IllegalStateException("Utility class");
	}

	public static void create(DatumDiffOpContext ctx, ParseTreeCreator tc) {

		DateDiffOperationName name = extractOperationNameFromContext(ctx);

		//reverse ordering, because it is a stack
		IOperation datumAngabe2 = tc.popFop();
		IOperation datumAngabe1 = tc.popFop();

		IDateDiffOperation result = new DateDiffOperationImpl(name, datumAngabe1, datumAngabe2);

		tc.push(result);
	}

	/**
	 * Konvertiert den Operationsnamen aus dem Kontext in die internen Namen.
	 */
	public static DateDiffOperationName extractOperationNameFromContext(DatumDiffOpContext ctx) {
		Token t = ctx.datumDiffOpName().start;
		int type = t.getType();
		switch (type) {
			case GrammarOnly_ConditionParser.DIFFERENZ_IN_SEKUNDEN:
				return DateDiffOperationName.DifferenceInSeconds;
			case GrammarOnly_ConditionParser.DIFFERENZ_IN_MINUTEN:
				return DateDiffOperationName.DifferenceInMinutes;
			case GrammarOnly_ConditionParser.DIFFERENZ_IN_STUNDEN:
				return DateDiffOperationName.DifferenceInHours;
			case GrammarOnly_ConditionParser.DIFFERENZ_IN_TAGEN:
				return DateDiffOperationName.DifferenceInDays;
			case GrammarOnly_ConditionParser.DIFFERENZ_IN_MONATEN:
				return DateDiffOperationName.DifferenceInMonths;
			case GrammarOnly_ConditionParser.DIFFERENZ_IN_JAHREN:
				return DateDiffOperationName.DifferenceInYears;
			default:
				throw new IllegalArgumentException(CreatorUtils.getUnknownTokenErrorMessage(t));
		}
	}
}
