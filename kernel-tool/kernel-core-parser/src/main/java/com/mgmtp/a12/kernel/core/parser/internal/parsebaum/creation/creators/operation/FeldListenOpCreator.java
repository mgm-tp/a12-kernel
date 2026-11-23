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
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldListenOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.ExpandMode;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.CreatorUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.FeldIdListeUtil;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.EntityIdListImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.EntityListOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.EntityListOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.OperationType;

/**
 * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>feldListenOp: feldListenOpName '(' entityListe ')' ;
 * <p>
 * feldListenOpName: 'MaxWert' | 'MinWert' | 'Summe' ;
 * <p>
 * das richtige Parsebaum-Objekt.
 */
public class FeldListenOpCreator {

	private FeldListenOpCreator() {
		throw new IllegalStateException("Utility class");
	}

	public static void create(FeldListenOpContext ctx, ParseTreeCreator tc) {
		EntityListOperationName name = extractOperationNameFromContext(ctx);
		ExpandMode expandMode = ExpandMode.limitToExpand(tc.getExpandMode());
		EntityIdListImpl fListe = FeldIdListeUtil.createEntityIdListeFromContext(ctx.entityListe(), tc.getParser(), expandMode);
		OperationResultType opType = tc.getParser().getOperationResultTypeMap().getOpType(ctx);

		OperationType type = OperationResultType.getType(opType);

		IOperation op = new EntityListOperationImpl(name, fListe, type);
		tc.push(op);
	}

	/**
	 * Konvertiert den Operationsnamen aus dem Kontext in die internen Namen.
	 */
	private static EntityListOperationName extractOperationNameFromContext(FeldListenOpContext ctx) {
		Token t = ctx.feldListenOpName().start;
		int type = t.getType();
		switch (type) {
			case GrammarOnly_ConditionParser.MAX_WERT:
				return EntityListOperationName.MaxValue;
			case GrammarOnly_ConditionParser.MIN_WERT:
				return EntityListOperationName.MinValue;
			case GrammarOnly_ConditionParser.SUMME:
				return EntityListOperationName.Sum;
			case GrammarOnly_ConditionParser.SUMME_VON_PRODUKTEN:
				return EntityListOperationName.SumOfProducts;
			case GrammarOnly_ConditionParser.ERSTER_ANGEGEBENER_WERT:
				return EntityListOperationName.FirstFilledValue;
			default:
				throw new IllegalArgumentException(CreatorUtils.getUnknownTokenErrorMessage(t));
		}
	}

}
