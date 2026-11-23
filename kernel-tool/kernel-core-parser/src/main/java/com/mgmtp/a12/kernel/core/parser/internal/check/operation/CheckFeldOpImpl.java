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

import java.util.Set;

import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.StringUtils;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckEntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.FieldUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEnumerationType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;

import jakarta.validation.constraints.NotNull;

/**
 * Stellt die Prüfungen für <code>FeldOp</code> bereit.
 * Es wird geprüft:
 * <ul>
 * <li>für alle Operationen, dass nur ein Feld als Parameter angegeben wurde. Das Feld darf keine
 * Wildcards enthalten.{@link ParserErrorTypesEnum#MVK_NO_GROUPS_ALLOWED} und
 * {@link ParserErrorTypesEnum#MVK_NO_WILDCARDS_ALLOWED}
 * <li><b>Abswert</b>: Es wird geprüft, dass das Feld eine Zahl oder ein Geldbetrag ist.
 * {@link ParserErrorTypesEnum#MVK_NO_NUMBER}
 * <li><b>Länge</b>: Es wird geprüft, dass das Feld vom Typ String ist.
 * {@link ParserErrorTypesEnum#MVK_NO_STRING}
 * <li><b>FeldWertAlsZahl</b>: Es wird geprüft, dass das Feld vom Typ String oder Enumeration ist
 * und eine maximale Länge der im Projekt vereinbarten max. Anzahl von Stellen bei Zahlen. Bei
 * ExtMP ist dies {@value MAX_ALLOWED_DIGITS}. Dies wird geprüft, da es sonst zu undefiniertem Verhalten kommen kann. Bei
 * Strings wird geprüft, ob eine Ziffernfolge definiert wurde, d.h. das Muster [0-9]+. Dies ist zwar
 * sehr restriktiv, andere Überprüfungen lohnen aber nicht den Aufwand. Bei Enumerationen wird
 * geprüft, dass alle Werte, ggf. Kategorienwerte, eine Länge {@literal <=} max Länge haben und in eine Zahl umwandelbar
 * sind (CheckEntityUtils.NumberUtilsParsableAs320) {@link ParserErrorTypesEnum#MVK_NO_CONVERT_IN_NUMBER}
 * </ul>
 */
public class CheckFeldOpImpl extends CheckImplAbstractClass {
	public CheckFeldOpImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(@NotNull FeldOpContext ctx) {

		EntitySpecContext ectx = ctx.entitySpec();
		EntityRef ref = getEntityRef(ectx);
		CheckEntityUtils checker = new CheckEntityUtils(fullParser);
		String opName = ctx.feldOpName().getText();

		//referenced entity must be a field, without a wildcard.
		checker.checkIsField(ref, opName, ectx);
		checker.checkNoWildCards(ref, opName, ectx);

		Token t = ctx.feldOpName().start;
		// While adding a new switch-case, please consider if the access to a category value (for an enumeration field) should be allowed or not.
		switch (t.getType()) {
			case GrammarOnly_ConditionParser.ABS_WERT:
				checker.checkNoCategories(ectx, opName);
				checker.checkIsNumber(ref, opName, ectx, ParserErrorTypesEnum.MVK_NO_NUMBER);
				Integer maxFractionalDigits = super.getMaxFractionalDigits((IField) ref.getEntity());
				//the result of the operation is a number.
				addNumberOpType(ctx, maxFractionalDigits, false);
				break;
			case GrammarOnly_ConditionParser.LAENGE:
				checker.checkNoCategories(ectx, opName);
				checker.checkIsString(ref, opName, ectx);
				addNumberOpType(ctx, 0, false);
				break;
			case GrammarOnly_ConditionParser.FELD_WERT_ALS_ZAHL:
				checker.checkNoTableEnum(ref, opName, ectx);
				checker.checkIsConvertableToNumber(ref, opName, ectx);
				checker.checkIsValueValidation(ref, opName, ectx);
				int maxFractionalDigitsForEnum = getMaxFractionalDigitsForEnum(ref, ectx);
				addNumberOpType(ctx, maxFractionalDigitsForEnum, false);
				break;
			case GrammarOnly_ConditionParser.FELD_WERT_ALS_STRING:
				checker.checkNoCategories(ectx, opName);
				checker.checkIsNumber(ref, opName, ectx, ParserErrorTypesEnum.MVK_NO_NUMBER);
				addStringOpType(ctx);
				break;
			default:
				throwUnknownTokenError(t);
				// never reached. Only for compiler
				throw new IllegalArgumentException();
		}
	}

	private int getMaxFractionalDigitsForEnum(EntityRef ref, EntitySpecContext ctx) {
		IField f = (IField) ref.getEntity();
		if (f.getType() instanceof IEnumerationType enumType) {
			if (ctx.categoryName != null && !StringUtils.isEmpty(ctx.categoryName.getText())) {
				return extractMaxFractionalDigits(FieldUtils.getCategoryValues(enumType, ctx.categoryName.getText()));
			} else {
				return extractMaxFractionalDigits(FieldUtils.getDistinctEnumValues(enumType));
			}
		}
		return 0;
	}

	/**
	 * Assumes '.' as the decimal separator.
	 */
	private int extractMaxFractionalDigits(Set<String> values) {
		return values.stream()
				.filter(value -> value.contains("."))
				.map(value -> value.split("\\."))
				// we know that the array length is always 2, since CheckEntityUtils.NumberUtilsParsableAs320 does not allow inputs which lead to bigger or smaller arrays
				.mapToInt(array -> array[1].length())
				.max()
				.orElse(0);
	}
}
