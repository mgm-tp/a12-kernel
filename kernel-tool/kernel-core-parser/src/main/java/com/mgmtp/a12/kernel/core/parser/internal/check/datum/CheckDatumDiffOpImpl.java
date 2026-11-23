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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.DateUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.DatumDiffOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.DateFormat;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultTypeMap;

import jakarta.validation.constraints.NotNull;

/**
 * Stellt die Prüfungen für <code>checkDatumDiffOp</code> bereit.
 * Insgesamt gilt: Wenn das Modell ein Referenzjahr spezifiziert, wird bei einer fehlenden Jahresangabe das Jahr immer hinzugefügt.
 * <ul>
 * <li>Falls ein Feld für die Spezifikation verwendet wird, wird geprüft:
 * <p>
 * Falls die Differenz in Tagen geprüft wird, muss in dem Datumsformat die Tage spezifiziert sein,
 * falls die Differenz in Monaten geprüft wird, muss in dem Datumsformat die Monate spezifiziert
 * sein, falls die Differenz in Jahren spezifiziert ist, muss eine Jahresangabe spezifiziert sei.
 * {@link ParserErrorTypesEnum#MVK_WRONG_DATE_FORMAT_FOR_OP}
 * <li>Es müssen entweder alle Datumsangaben eine Jahresangabe haben oder keine der Datumsangaben.
 * {@link ParserErrorTypesEnum#MVK_DATE_WITH_AND_WITHOUT_YEAR}
 * <li>Das Datum darf nicht teilbekannt sein. {@link ParserErrorTypesEnum#MVK_INVALID_DATE_TYPE}
 * </ul>
 * Für die Operation wird das Format NUMBER in der {@link OperationResultTypeMap} hinterlegt.
 */
public class CheckDatumDiffOpImpl extends CheckImplAbstractClass {
	public CheckDatumDiffOpImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(@NotNull DatumDiffOpContext ctx) {
		addNumberOpType(ctx, 0, false);

		Token t = ctx.datumDiffOpName().getStart();

		ParserRuleContext da1 = ctx.da1.datumAngabe() != null ? ctx.da1.datumAngabe() : ctx.da1.feldOpMitDatumSpecial();
		ParserRuleContext da2 = ctx.da2.datumAngabe() != null ? ctx.da2.datumAngabe() : ctx.da2.feldOpMitDatumSpecial();

		DateUtils.checkDateCompletelySpecified(ctx.da1, this);
		DateUtils.checkDateCompletelySpecified(ctx.da2, this);

		OperationResultType opType1 = getOpType(da1);
		OperationResultType opType2 = getOpType(da2);
		DateFormat format1 = opType1.getDateFormat();
		DateFormat format2 = opType2.getDateFormat();

		DateUtils.checkOperationConsistentToDateFormat(t, format1, da1, opType1.getDateNameForErrorMessaging(), this);
		DateUtils.checkOperationConsistentToDateFormat(t, format2, da2, opType2.getDateNameForErrorMessaging(), this);

		if (!DateFormat.isFormatWithDateSpecification(format1)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_TIME, ctx.datumDiffOpName().getText(), "1.");
			throw createMVKParserError(da1, errMessage);
		}
		if (!DateFormat.isFormatWithDateSpecification(format2)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_TIME, ctx.datumDiffOpName().getText(), "2.");
			throw createMVKParserError(da2, errMessage);
		}

		// A12K-620: Due to the unclear desired behaviour of certain operators we currently do not allow datetime operands for those
		if (t.getType() == GrammarOnly_ConditionParser.DIFFERENZ_IN_JAHREN || t.getType() == GrammarOnly_ConditionParser.DIFFERENZ_IN_MONATEN) {
			if (DateFormat.isFormatWithTimeSpecification(format1)) {
				String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_WRONG_DATE_FORMAT_FOR_OP, t.getText(),
						opType1.getDateNameForErrorMessaging());
				throw createMVKParserError(da1, errMessage);
			} else if (DateFormat.isFormatWithTimeSpecification(format2)) {
				String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_WRONG_DATE_FORMAT_FOR_OP, t.getText(),
						opType2.getDateNameForErrorMessaging());
				throw createMVKParserError(da2, errMessage);
			}
		}

		if (!DateFormat.formatsConsistentRegardingFormat(format1, format2, getParser().hasBaseYear())) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_DATE_WITH_AND_WITHOUT_YEAR,
					ctx.datumDiffOpName().getText());
			throw createMVKParserError(ctx.getParent(), errMessage);
		}
	}
}
