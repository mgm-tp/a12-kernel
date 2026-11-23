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

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckEntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.DateUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.FieldUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.DatumAngabeContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.DatumExtractOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType.OperationResultBaseType;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultTypeMap;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;
import com.mgmtp.a12.kernel.core.utils.internal.CommonUtils;

import jakarta.validation.constraints.NotNull;

/**
 * Stellt die Prüfungen für <code>checkDatumAngabe</code> bereit.
 * Es wird geprüft, ob die Datumangabe korrekt ist. Je nach Alternative wird unterschiedlich
 * geprüft:
 * <ul>
 * <li><b>DatumsAngabe durch Spezifikation eines Feldes</b>
 * <p>
 * Es wird geprüft, dass nur ein Feld als Parameter angegeben wurde.
 * {@link ParserErrorTypesEnum#MVK_NO_GROUPS_ALLOWED} Das Feld darf keine Wildcards enthalten.
 * {@link ParserErrorTypesEnum#MVK_NO_WILDCARDS_ALLOWED} Feld muss vom Typ Datum sein.
 * {@link ParserErrorTypesEnum#MVK_NO_DATE}
 * <li><b>DatumsAngabe durch einen String </b>
 * <p>
 * Der String muss in der Form dd.MM.yyyy oder dd.MM sein.
 * {@link ParserErrorTypesEnum#MVK_INVALID_DATE_STRING}
 * </ul>
 * <p>
 * Für die Datumsangabe wird das Format Datum in der {@link OperationResultTypeMap} hinterlegt.
 */
public class CheckDatumAngabeImpl extends CheckImplAbstractClass {
	public CheckDatumAngabeImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	/**
	 * checks the Datum Angabe depending on the specified alternative.
	 * Save the Op, the format and the name of the op in the result map.
	 */
	public void check(@NotNull DatumAngabeContext ctx) {
		if (ctx.datumFeld != null) {
			checkDatumFeld(ctx);
			return;
		}
		if (ctx.heute != null) {
			addDateOpType(ctx, DateUtils.DATUM_FORMAT_WITH_YEAR, ctx.heute.getText());
		}
		if (ctx.jetzt != null) {
			addDateOpType(ctx, DateUtils.DATETIME_FORMAT, ctx.jetzt.getText());
		}
		if (ctx.konstanteReferenzjahr() != null) {
			addDateOpType(ctx, DateUtils.DATUM_FORMAT_WITH_YEAR, ctx.konstanteReferenzjahr().getText());
		}
		if (ctx.datumBereichOp() != null) {
			addOpType(ctx, getOpType(ctx.datumBereichOp()));
		}
		if (ctx.datumAddOp() != null) {
			addOpType(ctx, getOpType(ctx.datumAddOp()));
		}
		if (ctx.dateTimeKonstrukt() != null) {
			addOpType(ctx, getOpType(ctx.dateTimeKonstrukt()));
		}
		if (ctx.timeKonstrukt() != null) {
			addOpType(ctx, getOpType(ctx.timeKonstrukt()));
		}
		if (ctx.datumKonstrukt() != null) {
			addOpType(ctx, getOpType(ctx.datumKonstrukt()));
		}
		if (ctx.datumString != null) {
			checkDatumString(ctx);
		}
		if (ctx.datumExtractOp() != null) {
			checkDatumExtractOp(ctx);
		}
	}

	private void checkDatumExtractOp(DatumAngabeContext ctx) {
		OperationResultType opType = getOpType(ctx.datumExtractOp());
		if (opType.getBaseType() != OperationResultBaseType.DATETIME) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_DATE_FOR_OP, getParentOpName(ctx),
					ctx.getStart().getText());
			throw createMVKParserError(ctx, errMessage);
		}
		addOpType(ctx, opType);
	}

	/**
	 * checks if the string provided by the given context is a valid date and adds the
	 * date operation to the operation result map.
	 *
	 * @param ctx
	 */
	private void checkDatumString(DatumAngabeContext ctx) {
		String dateString = CommonUtils.removeAndUnescapeQuotesAndRemoveCrFromCrLf(ctx.datumString.getText());
		if (!DateUtils.isStringAValidDate(dateString)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_DATE_STRING, getParentOpName(ctx), dateString);
			throw createMVKParserError(ctx, errMessage);
		}
		String format = DateUtils.getDateFormatFromDate(dateString);
		addDateOpType(ctx, format, dateString);
	}

	/**
	 * checks if the field provided by the given context is a valid date and adds the
	 * date operation to the operation result map.
	 *
	 * @param ctx
	 */
	private void checkDatumFeld(DatumAngabeContext ctx) {
		EntitySpecContext ectx = ctx.datumFeld;
		CheckEntityUtils checker = new CheckEntityUtils(fullParser);

		EntityRef ref = getEntityRef(ectx);
		String opName = getParentOpName(ctx);
		String fieldNameForErrorOutput = ref.getNameBuiltFromConditionText();
		checker.checkNoCategories(ectx, opName);

		//references must be a field without a wildcard.
		IField f = checker.checkIsField(ref, opName, ectx);
		checker.checkNoWildCards(ref, opName, ectx);

		if (!FieldUtils.isDate(f)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_NO_DATE, opName, fieldNameForErrorOutput);
			throw createMVKParserError(ectx, errMessage);
		}

		addDateOpType(ctx, FieldUtils.getDateFormat(f), fieldNameForErrorOutput);
	}

	private String getParentOpName(DatumAngabeContext ctx) {
		// for DatumDiffOps there is a DatumAngabe2Context in between
		return ctx.getParent() instanceof DatumExtractOpContext ?
				ctx.getParent().getStart().getText() :
				ctx.getParent().getParent().getStart().getText();
	}
}
