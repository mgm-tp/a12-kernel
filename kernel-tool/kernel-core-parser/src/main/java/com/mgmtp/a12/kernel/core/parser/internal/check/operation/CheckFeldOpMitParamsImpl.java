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

import org.antlr.v4.runtime.Token;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckEntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.FieldUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldOpMitParamsContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;

import jakarta.validation.constraints.NotNull;

/**
 * Stellt die Prüfungen für <code>FeldOpMitParams</code> bereit.
 * Dies sind <code>BereichAlsString</code><code>BereichAlsZahl</code>
 * <p>
 * Es wird geprüft:
 * <ul>
 * <li>dass nur ein Feld als Parameter angegeben wurde. Das Feld darf keine Wildcards enthalten.
 * {@link ParserErrorTypesEnum#MVK_NO_GROUPS_ALLOWED} und
 * {@link ParserErrorTypesEnum#MVK_NO_WILDCARDS_ALLOWED}
 * <li>Nur Strings, Vordefinierte Typen oder Nicht-abgeschlossene-Enumerationen sind erlaubt. {@link ParserErrorTypesEnum#MVK_INVALID_TYPE}</li>
 * {@link ParserErrorTypesEnum#MVK_INVALID_TYPE}</li>
 * <li>Das angegebene Interval muss gültig sein, d.h. {@literal 0 < start < ende}.
 * {@link ParserErrorTypesEnum#MVK_INVALID_INTERVAL}</li>
 * </ul>
 */
public class CheckFeldOpMitParamsImpl extends CheckImplAbstractClass {
	public CheckFeldOpMitParamsImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(@NotNull FeldOpMitParamsContext ctx) {
		EntitySpecContext ectx = ctx.entitySpec();
		EntityRef ref = fullParser.getEntityRef(ectx);
		CheckEntityUtils checker = new CheckEntityUtils(fullParser);
		String opName = ctx.feldOpMitParamsName().getText();

		//reference must be a field, without a wildcard.
		checker.checkIsField(ref, opName, ectx);
		checker.checkNoWildCards(ref, opName, ectx);
		checker.checkNoCategories(ectx, opName);

		//check the specification of the interval
		int begin = Integer.parseInt(ctx.begin.getText());
		int ende = Integer.parseInt(ctx.ende.getText());
		if (begin < 1 || begin > ende) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_INTERVAL, ctx.feldOpMitParamsName().getText());
			throw createMVKParserError(ctx, errMessage);
		}

		//check the restrictions regarding the specific operations.
		Token t = ctx.feldOpMitParamsName().start;
		checkType(ctx, ref);
		checker.checkIsValueValidation(ref, opName, ectx);

		switch (t.getType()) {
			case GrammarOnly_ConditionParser.BEREICH_ALS_ZAHL:
				addNumberOpType(ctx, 0, false);
				return;
			case GrammarOnly_ConditionParser.BEREICH_ALS_STRING:
				addStringOpType(ctx);
				return;
			default:
				throwUnknownTokenError(t);
		}
	}

	private void checkType(FeldOpMitParamsContext ctx, EntityRef ref) {
		IField f = (IField) ref.getEntity();

		if (!FieldUtils.isLikeString(f)) {
			throwInvalidTypeException(ctx, ref);
		}
	}

	private void throwInvalidTypeException(FeldOpMitParamsContext ctx, EntityRef ref) {
		String errMessage = getErrorMessage(
				ParserErrorTypesEnum.MVK_INVALID_TYPE,
				ctx.feldOpMitParamsName().getText(),
				ref.getNameBuiltFromConditionText());
		throw createMVKParserError(ctx, errMessage);
	}

}
