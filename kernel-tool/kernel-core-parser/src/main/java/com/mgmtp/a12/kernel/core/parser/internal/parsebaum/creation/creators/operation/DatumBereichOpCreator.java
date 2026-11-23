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
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.DatumBereichOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.CreatorUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.datum.DateRangeExtractionOperationImpl;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateRangeExtractionOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.DateRangeExtractionOperationName;

/**
 * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>
 * datumBereichOp: datumBereichOpName '(' datumBereichAngabe')'<p>
 * datumBereichOpName: 'StartAusDatumBereich' | 'EndeAusDatumBereich'; <p>
 * datumBereichAngabe:  entitySpec | konstanteReferenzjahr ;
 * </code>
 * <p>
 * das richtige Parsebaum-Objekt.
 */
public class DatumBereichOpCreator {

	private DatumBereichOpCreator() {
		throw new IllegalStateException("Utility class");
	}

	public static void create(DatumBereichOpContext ctx, ParseTreeCreator tc) {
		DateRangeExtractionOperationName name = extractOperationNameFromContext(ctx);

		IOperation datumBereichAngabe = getDatumBereichAngabe(ctx, tc);

		IDateRangeExtractionOperation result = new DateRangeExtractionOperationImpl(name, datumBereichAngabe);

		tc.push(result);
	}

	/**
	 * Gibt den Datumsbereich als IFeldoperation zurück.
	 * <p>
	 * Falls der Datumsbereich durch ein Feld spezifiziert wird, wird die entsprechende
	 * FeldWert-Operation erzeugt und zurückgegeben.
	 * <p>
	 * Falls der Datumsbereich über ein Referenzjahr spezifiziert wird, ist die Operation bereits gelesen
	 * worden und wird nun vom Stack genommen.
	 *
	 * @param ctx
	 * 		Kontext, der die Gesamt-Operation beschreibt.
	 * @param tc
	 * 		wird für den Operation-Stack benötigt.
	 * @return Operation, die den DatumsBereich definiert.
	 */
	private static IOperation getDatumBereichAngabe(DatumBereichOpContext ctx, ParseTreeCreator tc) {
		EntitySpecContext eCtx = ctx.datumBereichAngabe().entitySpec();
		if (eCtx != null) {
			//die Erzeugungs-Methoden speichern das Ergebnis immer auf den Stack,
			//daher wird keine Rückgabe-Variable benötigt.
			FeldWertOpCreator.createFeldWertOpFromEntityCtx(eCtx, tc);
		}
		return tc.popFop();
	}

	/**
	 * Konvertiert den Operationsnamen aus dem Kontext in die internen Namen.
	 */
	public static DateRangeExtractionOperationName extractOperationNameFromContext(DatumBereichOpContext ctx) {
		Token t = ctx.datumBereichOpName().start;
		int type = t.getType();
		switch (type) {
			case GrammarOnly_ConditionParser.START_AUS_DATUM_BEREICH:
				return DateRangeExtractionOperationName.StartOfDateRange;
			case GrammarOnly_ConditionParser.ENDE_AUS_DATUM_BEREICH:
				return DateRangeExtractionOperationName.EndOfDateRange;
			default:
				throw new IllegalArgumentException(CreatorUtils.getUnknownTokenErrorMessage(t));
		}
	}

}
