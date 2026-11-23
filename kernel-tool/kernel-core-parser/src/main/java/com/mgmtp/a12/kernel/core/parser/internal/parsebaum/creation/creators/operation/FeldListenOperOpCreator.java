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

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldListenOperOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.CreatorUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.OpListOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.OpListOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.OperationType;

/**
 * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>feldListenOperOp:   feldListenOperOpName '('operationListe ')'<p>
 * operationListe: operation (',' operation)*;<p>
 * feldListenOperOpName: 'Max' | 'Min' ;
 * </code>
 * <p>
 * das richtige Parsebaum-Objekt.
 */
public class FeldListenOperOpCreator {

	private FeldListenOperOpCreator() {
		throw new IllegalStateException("Utility class");
	}

	public static void create(FeldListenOperOpContext ctx, ParseTreeCreator tc) {
		OpListOperationName name = extractOperationNameFromContext(ctx);
		List<IOperation> feldOperationen = extractOperationsFromContext(ctx, tc);
		OperationResultType opType = tc.getParser().getOperationResultTypeMap().getOpType(ctx);
		OperationType type = OperationResultType.getType(opType);
		OpListOperationImpl result = new OpListOperationImpl(name, feldOperationen, type);
		tc.push(result);
	}

	/**
	 * Extrahiert die Anzahl der Operationen aus dem Kontext und holt die Operationen vom Stack.
	 */
	private static List<IOperation> extractOperationsFromContext(FeldListenOperOpContext ctx, ParseTreeCreator tc) {
		int countParams = ctx.operationListe().operation().size();
		List<IOperation> result = new ArrayList<>();
		for (int i = 0; i < countParams; i++) {
			//take care of the ordering!
			result.add(0, tc.popFop());
		}
		return result;

	}

	/**
	 * Konvertiert den Operationsnamen aus dem Kontext in die internen Namen.
	 */
	private static OpListOperationName extractOperationNameFromContext(FeldListenOperOpContext ctx) {
		Token t = ctx.feldListenOperOpName().start;
		int type = t.getType();
		switch (type) {
			case GrammarOnly_ConditionParser.MAX:
				return OpListOperationName.Max;
			case GrammarOnly_ConditionParser.MIN:
				return OpListOperationName.Min;
			default:
				throw new IllegalArgumentException(CreatorUtils.getUnknownTokenErrorMessage(t));
		}
	}

}
