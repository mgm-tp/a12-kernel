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
package com.mgmtp.a12.kernel.core.parser.internal.check.datum;

import org.antlr.v4.runtime.Token;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.DateUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.DatumAngabeContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.DatumExtractOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.DateFormat;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultTypeMap;

import jakarta.validation.constraints.NotNull;

/**
 * Stellt die Prüfungen für <code>checkDatumExtractOp</code> bereit.
 * Folgendes wird geprüft:
 * <ul>
 * <li>dass für die Datumsangabe keine Konstante (d.h. kein DatumString) verwendet wurde.
 * {@link ParserErrorTypesEnum#MVK_NO_CONST}
 * <li>dass die Datumsangabe die passenden Datumsfragmente für die Operation hat.
 * {@link ParserErrorTypesEnum#MVK_WRONG_DATE_FORMAT_FOR_OP}
 * <li>Wenn das Datum ein Teilbekanntes Datum ist, muss die Teilbekanntheit zur Operation passen.
 * Das heißt, das was man extrahieren möchte, muss bekannt sein.
 * {@link ParserErrorTypesEnum#MVK_INVALID_DATE_TYPE}
 * </ul>
 * Für die Operation wird das Format NUMBER in der {@link OperationResultTypeMap} hinterlegt.
 */
public class CheckDatumExtractOpImpl extends CheckImplAbstractClass {
	public CheckDatumExtractOpImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(@NotNull DatumExtractOpContext ctx) {
		Token t = ctx.datumExtractOpName().getStart();
		switch (t.getType()) {
			case GrammarOnly_ConditionParser.DATUM_AUS_DATETIME:
			case GrammarOnly_ConditionParser.ZEIT_AUS_DATETIME:
				checkAndAddDateOpType(ctx, t);
				break;
			default:
				checkAndAddNumberOpType(ctx, t);
				break;
		}
	}

	private void checkAndAddDateOpType(DatumExtractOpContext ctx, Token t) {
		OperationResultType type = getOpType(ctx.datumAngabe());
		if (!DateFormat.isValidDateTimeFormat(type.getDateFormat())) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_WRONG_DATE_FORMAT_FOR_OP, ctx.getStart().getText(),
					ctx.datumAngabe().getStart().getText());
			throw createMVKParserError(ctx, errMessage);
		}
		switch (t.getType()) {
			case GrammarOnly_ConditionParser.DATUM_AUS_DATETIME:
				addDateOpType(ctx, DateUtils.DATUM_FORMAT_WITH_YEAR, t.getText());
				break;
			case GrammarOnly_ConditionParser.ZEIT_AUS_DATETIME:
				addDateOpType(ctx, DateUtils.TIME_FORMAT, t.getText());
				break;
			default:
				throw new IllegalArgumentException("Unexpected type for date extraction operation.");
		}
	}

	private void checkAndAddNumberOpType(DatumExtractOpContext ctx, Token t) {
		DatumAngabeContext daCtx = ctx.datumAngabe();

		if (daCtx.datumFeld != null) {
			DateUtils.checkOperationConsistentToPartialKnown(t, daCtx.datumFeld, this);
		}

		if (daCtx.datumString != null) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_NO_CONST, ctx.getStart().getText());
			throw createMVKParserError(ctx, errMessage);
		}

		OperationResultType type = getOpType(daCtx);
		DateFormat format = type.getDateFormat();
		DateUtils.checkOperationConsistentToDateFormat(t, format, daCtx, type.getDateNameForErrorMessaging(), this);

		addNumberOpType(ctx, 0, false);
	}
}
