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
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.DatumExtractOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.CreatorUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.datum.DateExtractionOperationImpl;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateExtractionOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.DateExtractionOperationName;

/**
 * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>
 * datumExtractOp: datumExtractOpName '(' datumAngabe ')';
 * <p>
 * datumExtractOpName: 'JahrAusDatum' | 'QuartalAusDatum' | 'MonatAusDatum' | 'TagAusDatum' | 'StundenAusZeit' | 'MinutenAusZeit' |
 * 'SekundenAusZeit' | 'DatumAusDateTime' | 'ZeitAusDateTime';
 * </code>
 * <p>
 * das richtige Parsebaum-Objekt.
 */
public class DatumExtractOpCreator {

	private DatumExtractOpCreator() {
		throw new IllegalStateException("Utility class");
	}

	public static void create(DatumExtractOpContext ctx, ParseTreeCreator tc) {

		DateExtractionOperationName name = extractOperationNameFromContext(ctx);
		// in case of BaseYear transformation to date type is necessary as number is default
		IOperation datumAngabe = tc.popFop();

		IDateExtractionOperation result = new DateExtractionOperationImpl(name, datumAngabe);

		tc.push(result);
	}

	/**
	 * Konvertiert den Operationsnamen aus dem Kontext in die internen Namen.
	 */
	public static DateExtractionOperationName extractOperationNameFromContext(DatumExtractOpContext ctx) {
		Token t = ctx.datumExtractOpName().start;
		int type = t.getType();
		switch (type) {
			case GrammarOnly_ConditionParser.TAG_AUS_DATUM:
				return DateExtractionOperationName.DayFromDate;
			case GrammarOnly_ConditionParser.MONAT_AUS_DATUM:
				return DateExtractionOperationName.MonthFromDate;
			case GrammarOnly_ConditionParser.JAHR_AUS_DATUM:
				return DateExtractionOperationName.YearFromDate;
			case GrammarOnly_ConditionParser.QUARTAL_AUS_DATUM:
				return DateExtractionOperationName.QuarterFromDate;
			case GrammarOnly_ConditionParser.STUNDEN_AUS_ZEIT:
				return DateExtractionOperationName.HoursFromTime;
			case GrammarOnly_ConditionParser.MINUTEN_AUS_ZEIT:
				return DateExtractionOperationName.MinutesFromTime;
			case GrammarOnly_ConditionParser.SEKUNDEN_AUS_ZEIT:
				return DateExtractionOperationName.SecondsFromTime;
			case GrammarOnly_ConditionParser.DATUM_AUS_DATETIME:
				return DateExtractionOperationName.DateFromDateTime;
			case GrammarOnly_ConditionParser.ZEIT_AUS_DATETIME:
				return DateExtractionOperationName.TimeFromDateTime;
			default:
				throw new IllegalArgumentException(CreatorUtils.getUnknownTokenErrorMessage(t));
		}
	}

}
