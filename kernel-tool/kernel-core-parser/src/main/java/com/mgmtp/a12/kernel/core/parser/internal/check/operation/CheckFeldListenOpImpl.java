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

import org.antlr.v4.runtime.Token;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckEntityListenUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckEntityListenUtilsHModel;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntityListeContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldListenOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType.OperationResultBaseType;

/**
 * Stellt die Prüfungen für <code>FeldListenOp</code> bereit.
 * Es wird geprüft:
 * <ul>
 * <li>Alle Operanden sind Zahlen oder Geldbeträge. Wird ein Kontext referenziert, so werden alle
 * Felder des {@link ParserErrorTypesEnum#MVK_NO_NUMBER} Kontextes geprüft.
 * <li>Felder der Parameterliste dürfen nicht mehrfach (direkt oder indirekt) eingegeben werden.
 * {@link ParserErrorTypesEnum#MVK_DUPLICATE_PARAM1} und
 * {@link ParserErrorTypesEnum#MVK_DUPLICATE_PARAM2}
 * </ul>
 */
public class CheckFeldListenOpImpl extends CheckImplAbstractClass {
	public CheckFeldListenOpImpl(FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(FeldListenOpContext ctx) {
		CheckEntityListenUtils checker = new CheckEntityListenUtils(fullParser);
		CheckEntityListenUtilsHModel oChecker = new CheckEntityListenUtilsHModel(this);

		EntityListeContext elCtx = ctx.entityListe();
		String opName = ctx.feldListenOpName().getText();
		List<EntityRef> paramListe = checker.getRefList(elCtx, opName);

		checker.checkNotRootGroupReferenced(paramListe, elCtx, opName, fullParser.isHModel());

		Token t = ctx.feldListenOpName().start;
		int type = t.getType();
		if (type == GrammarOnly_ConditionParser.SUMME_VON_PRODUKTEN) {
			checker.checkOnlyFields(paramListe, elCtx, opName);
			checker.checkParamSizeIs2(paramListe, elCtx, opName);
			oChecker.checkAllFieldsSameMZI(paramListe, elCtx, opName);
			checker.checkAllEntitiesInSameGroup(paramListe, elCtx, opName);
			checker.checkAllEntitiesHaveWildcardOnLowestLevel(paramListe, elCtx, opName);
			checker.checkAllFieldsSameL(paramListe, elCtx, opName);
		} else {
			checker.checkIsMoreThanOneFieldOrIsAGroup(paramListe, elCtx, opName);
		}

		if (type == GrammarOnly_ConditionParser.ERSTER_ANGEGEBENER_WERT) {
			checker.checkNoBoolY(paramListe, elCtx, opName);
			OperationResultType opType = checker.checkAllFieldsMatchingOperationType(paramListe, elCtx, opName);
			if (opType.getBaseType() == OperationResultBaseType.NUMBER) {
				Integer maxFractionalDigits = calculateMaxFractionalDigits(ctx, paramListe);
				addNumberOpType(ctx, maxFractionalDigits, false);
			} else {
				addOpType(ctx, opType);
			}
		} else if (type == GrammarOnly_ConditionParser.MAX_WERT || type == GrammarOnly_ConditionParser.MIN_WERT) {
			OperationResultType firstType = checker.firstType(paramListe, elCtx);
			if (firstType.getBaseType() == OperationResultBaseType.DATETIME) {
				OperationResultType opType = checker.checkAllFieldsAreComparableDatetimeType(paramListe, fullParser.hasBaseYear(),
						elCtx, opName);
				addOpType(ctx, opType);
			} else {
				checker.checkAllFieldsAreNumbers(paramListe, elCtx, opName, ParserErrorTypesEnum.MVK_NOT_SORTABLE);
				Integer maxFractionalDigits = calculateMaxFractionalDigits(ctx, paramListe);
				addNumberOpType(ctx, maxFractionalDigits, false);
			}
		} else {
			checker.checkAllFieldsAreNumbers(paramListe, elCtx, opName, ParserErrorTypesEnum.MVK_NO_NUMBER);
			Integer maxFractionalDigits = calculateMaxFractionalDigits(ctx, paramListe);
			addNumberOpType(ctx, maxFractionalDigits, false);
		}
	}

	private Integer calculateMaxFractionalDigits(FeldListenOpContext ctx, List<EntityRef> paramListe) {
		int type = ctx.feldListenOpName().start.getType();
		List<Integer> fractionals = paramListe.stream()
				.flatMap(ref -> EntityUtils.expandToFieldList(ref, fullParser, ctx).stream())
				.map(this::getMaxFractionalDigits)
				.toList();
		return switch (type) {
			case GrammarOnly_ConditionParser.MIN_WERT, GrammarOnly_ConditionParser.MAX_WERT, GrammarOnly_ConditionParser.SUMME,
				 GrammarOnly_ConditionParser.ERSTER_ANGEGEBENER_WERT -> super.computeUnionOfFractionalDigits(fractionals);
			case GrammarOnly_ConditionParser.SUMME_VON_PRODUKTEN -> super.computeAdditionOfFractionalDigits(fractionals);
			default -> throw new IllegalArgumentException("Unknown CheckFeldListenOpImpl-type.");
		};
	}

}
