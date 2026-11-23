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

import com.mgmtp.a12.kernel.core.parser.internal.check.util.DateUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.DatumKonstruktContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.DatumParamContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultTypeMap;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IFieldType;

import jakarta.validation.constraints.NotNull;

/**
 * Stellt die Prüfungen für <code>checkDatumKonstruktOp</code> bereit.
 * Folgendes wird geprüft:
 * <ul>
 * <li>Wenn die Datumsangaben über <b>String-Konstanten</b> erfolgen:
 * <p>
 * der konkatenierte String (mit dem Datumstrenner Punkt) ergibt korrektes Datumsformat ergibt.
 * Anstelle von „01“ kann auch „1“ notiert werden (dies wird im produktiven Datenbestand auch so
 * verwendet). Eine Jahresangabe kann entweder in einer Angabe erfolgen oder in zwei Teilen zu
 * jeweils zwei Stellen. Es muss geprüft werden, ob das gesamte Format Tagesangabe, Monatsangabe,
 * und vierstellige Jahresangabe umfasst. Für Tagesangaben wird geprüft, dass Angabe zwischen 1 und
 * 31 – nicht genau für den Monat. In der ExtMP-Welt ist erlaubt, dass in Modellen mit Referenzjahr
 * keine Jahresangabe erfolgt. {@link ParserErrorTypesEnum#MVK_MISSING_YEAR}
 * {@link ParserErrorTypesEnum#MVK_WRONG_DATE_FORMAT_FOR_OP}
 * <li>Falls die Datumsangaben über <b>Felder</b> erfolgen:
 * <p>
 * ob es sich um ein Feld handelt. {@link ParserErrorTypesEnum#MVK_NO_GROUPS_ALLOWED}
 * <p>
 * Das Feld darf keinen Stern spezifizieren. {@link ParserErrorTypesEnum#MVK_NO_WILDCARDS_ALLOWED}
 * <p>
 * Das Feld muss entweder eine Zahl ohne Vorzeichen und ohne Nachkommastellen sein und für das Feld
 * muss
 * <ul>
 * <li>entweder eine max. Länge spezifiziert sein, 2 für Tag, Monat, Jahresangabe falls bestehend
 * aus zwei Feldern, 4 für Jahresangabe bestehend aus einem Feld</li>
 * <li>oder ein max. Wert, 31 für den Tag, 12 für den Monat, 99 für die Jahrzehntangabe und zwischen
 * 10 und 99 für die Jahrhundertangabe, falls bestehend aus 2 Feldern bzw. ein Wert zwischen 1000
 * und 9999 falls bestehend aus einem Feld</li>
 * </ul>
 * Oder das Feld muss ein String mit Muster sein mit der Länge wie bei Zahlen. Als Muster sind
 * [0-9]+, [0-9]* [0-9]{maxLaenge} bzw. \d+, \d* \d{maxLaenge} erlaubt. Für eine
 * Jahresangabe bestehend aus einem Feld ist auch ein Feld vom Typ Datum mit Format YYYY erlaubt.
 * Jahresangabe kann entweder über ein Feld / ein String oder über zwei Felder / 2 Strings erfolgen.
 * </ul>
 * Für die Datumsangabe wird das Format Datum in der {@link OperationResultTypeMap} hinterlegt.
 */
public class CheckDatumKonstruktImpl extends CheckBaseDateConstructionImpl {

	private static final int MAX_VALUE_DAY = 31;
	private static final int MAX_VALUE_MONTH = 12;
	private static final int MAX_VALUE_SHORT_YEAR = 99;

	public CheckDatumKonstruktImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(@NotNull DatumKonstruktContext ctx) {

		checkDayParam(ctx.dp1);
		checkMonatParam(ctx.dp2);
		checkJahresParam(ctx.dp3, ctx.dp4, ctx);

		String format = ctx.dp3 == null ? DateUtils.DATUM_FORMAT_WITHOUT_YEAR : DateUtils.DATUM_FORMAT_WITH_YEAR;
		addDateOpType(ctx, format, ctx.getStart().getText());
	}

	private void checkJahresParam(DatumParamContext dp3, DatumParamContext dp4, DatumKonstruktContext ctx) {
		if (dp3 == null) {//without year,  taken as o.k.
			if (fullParser.hasBaseYear()) {
				return;
			}
			String opName = ctx.getStart().getText();
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_MISSING_YEAR, opName);
			throw createMVKParserError(ctx, errMessage);

		}

		if (dp4 == null) {
			checkYearFormat(dp3);
		} else {
			checkYearFormat(dp3, dp4);
		}
	}

	private void checkYearFormat(DatumParamContext century, DatumParamContext shortYear) {
		if (century.entitySpec() != null) {
			checkParamIfEntity(century, 2, MAX_VALUE_CENTURY, true);
		} else if (century.datumExtractOp() != null) {
			handleDatumExtractOpAtYearParam(century);
		} else {
			checkConstant(century, 18, 22, 2);
		}

		if (shortYear.entitySpec() != null) {
			checkParamIfEntity(shortYear, 2, MAX_VALUE_SHORT_YEAR, false);
		} else if (shortYear.datumExtractOp() != null) {
			handleDatumExtractOpAtYearParam(shortYear);
		} else {
			checkConstant(shortYear, 0, 100, 2);
		}
	}

	private void checkYearFormat(DatumParamContext dp) {
		if (dp.entitySpec() != null) {
			checkParamIfEntity(dp, 4, 9999, true);
		} else if (dp.datumExtractOp() != null) {
			checkCorrectDatumExtractOp(dp, GrammarOnly_ConditionParser.JAHR_AUS_DATUM);
		} else {
			checkConstant(dp, 1800, 2200, 4);
		}
	}

	private void checkMonatParam(DatumParamContext dp) {
		if (dp.entitySpec() != null) {
			checkParamIfEntity(dp, 2, MAX_VALUE_MONTH, false);
		} else if (dp.datumExtractOp() != null) {
			checkCorrectDatumExtractOp(dp, GrammarOnly_ConditionParser.MONAT_AUS_DATUM);
		} else {
			checkConstant(dp, 1, 13, IFieldType.UNDEFINED);
		}

	}

	private void checkDayParam(DatumParamContext dp) {
		if (dp.entitySpec() != null) {//specified by a field
			checkParamIfEntity(dp, 2, MAX_VALUE_DAY, false);
		} else if (dp.datumExtractOp() != null) {
			checkCorrectDatumExtractOp(dp, GrammarOnly_ConditionParser.TAG_AUS_DATUM);
		} else { // specified by a constant
			checkConstant(dp, 1, 32, IFieldType.UNDEFINED);
		}
	}

	/**
	 * Treats the specification of a datumExtractOp as a year parameter when specifying four
	 * parameters by creating a suitable error message.
	 *
	 * @param dp
	 * 		context of the provided datumExtractOp
	 */
	private void handleDatumExtractOpAtYearParam(DatumParamContext dp) {
		String datumConstruct = dp.getParent().getStart().getText();
		String providedDatumExtractOpName = dp.getStart().getText();

		String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_EXTRACT_OP_NOT_ALLOWED_4_DATE_CONSTRUCT_WITH_4_PARAMS,
				datumConstruct, providedDatumExtractOpName);
		throw createMVKParserError(dp, errMessage);
	}
}
