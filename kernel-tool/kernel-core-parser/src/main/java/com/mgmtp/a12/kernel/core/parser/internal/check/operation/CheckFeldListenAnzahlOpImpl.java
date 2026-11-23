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
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnumForHModel;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntityListeContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldListenAnzahlOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType.OperationResultBaseType;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultTypeMap;

import jakarta.validation.constraints.NotNull;

/**
 * Stellt die Prüfungen für <code>checkFeldListenAnzahlOpImpl</code> bereit.
 * Es wird geprüft:
 * <ul>
 * <li>Für <b>alle Operationen</b> wird geprüft:
 * <p>
 * In der Parameterliste sollen keine Parameter doppelt enthalten sein. Dies ist möglich entweder
 * durch eine Doppelnennung {@link ParserErrorTypesEnum#MVK_DUPLICATE_PARAM1}, oder durch indirekte
 * Doppelnennung {@link ParserErrorTypesEnum#MVK_DUPLICATE_PARAM2}, da ein Feld / ein Kontext auch
 * indirekt durch einen umfassenden Kontext spezifiziert werden kann. Die Überprüfung wurde
 * eingeführt, um Fehleingaben z.B. durch Copy/Paste-Fehler zu vermeiden.
 * <p>
 * Durch die Grammatik wird bereits geprüft, dass die Parameterliste nicht leer ist.</li>
 *
 * <li>für H-Modelle wird überprüft: Falls der Kontext der Vordruck ist, muss ein
 * Vordruck-Sprachkonstrukt verwendet werden (
 * {@link ParserErrorTypesEnumForHModel#MVK_ROOT_GROUP_REFERENCED_AS_GROUP}).
 * </li>
 * <li>für <b>AnzahlAngegebenerKontexte</b> wird geprüft:
 * <p>
 * <ul>
 * <li>dass alle Parameter Kontexte sind.{@link ParserErrorTypesEnum#MVK_NO_FIELDS_ALLOWED}
 *
 * <li>H-Modelle: Es wird geprüft, dass nicht über Zeilen iteriert wird. (nur H-Modelle)
 * {@link ParserErrorTypesEnumForHModel#MVK_NO_Z_ITERATION}
 * <li>A12-Modelle: ein Vordruck darf nur als einziger Parameter angegeben werden
 * {@link ParserErrorTypesEnum#MVK_ROOT_GROUP_WITH_OTHER_PARAMETERS}
 *
 * </ul>
 *
 * <li>für <b>AnzahlAngegebenerFelder</b> wird geprüft:
 * <p>
 * <ul>
 * <li>Es wird geprüft, dass in der Parameterliste mehr als ein Feld oder mindestens ein Kontext
 * oder
 * ein Feld mit mindestens einem Stern angegeben wurde.
 * {@link ParserErrorTypesEnum#MVK_PARAMSIZE_INVALIDN}
 * <li>Es darf kein Vordruck referenziert werden (nur relevant für A12-Modelle, da für H-Modelle
 * allgemein verboten) {@link ParserErrorTypesEnum#MVK_ROOT_GROUP_REFERENCED}
 * </ul>
 *
 * <li>für <b>AnzahlVerschiedenerWerte</b> wird geprüft, dass es sich nicht um ein H-Modell
 * handelt und dass die Parametertypen erlaubt und kompatibel sind (siehe Code für Details).
 * </ul>
 * <p>
 * Für die Operation wird das Format NUMBER in der {@link OperationResultTypeMap} hinterlegt.
 */
public class CheckFeldListenAnzahlOpImpl extends CheckImplAbstractClass {
	public CheckFeldListenAnzahlOpImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(@NotNull FeldListenAnzahlOpContext ctx) {
		addNumberOpType(ctx, 0, false);

		CheckEntityListenUtils checker = new CheckEntityListenUtils(fullParser);
		CheckEntityListenUtilsHModel oChecker = new CheckEntityListenUtilsHModel(this);
		String opName = ctx.feldListenAnzahlOpName().getText();

		EntityListeContext elCtx = ctx.entityListe();
		checker.checkNoCategories(elCtx, opName);

		List<EntityRef> paramListe = checker.getRefList(elCtx, opName);

		if (fullParser.isHModel()) {
			checker.checkNotRootGroupReferenced(paramListe, elCtx, opName, true);
		}

		Token t = ctx.feldListenAnzahlOpName().start;
		switch (t.getType()) {
			case GrammarOnly_ConditionParser.ANZAHL_ANGEGEBENER_KONTEXTE:
				if (fullParser.isA12Model()) {
					checker.checkNoRootGroupWhenMoreThanOneParameter(paramListe, elCtx, opName);
				}
				checker.checkNoSemanticIndex(paramListe, elCtx, opName);
				checker.checkOnlyGroups(paramListe, elCtx, opName);
				checker.checkIsMoreThanOneGroup(paramListe, elCtx, opName);
				oChecker.checkNoZIteration(paramListe, elCtx, opName);
				return;
			case GrammarOnly_ConditionParser.ANZAHL_ANGEGEBENER_FELDER:
				if (fullParser.isA12Model()) { // for H models already checked (with other error message)
					checker.checkNotRootGroupReferenced(paramListe, elCtx, opName, false);
				}
				checker.checkIsMoreThanOneFieldOrIsAGroup(paramListe, elCtx, opName);
				return;
			case GrammarOnly_ConditionParser.ANZAHL_VERSCHIEDENER_WERTE:
				if (fullParser.isHModel()) {
					String errMessage = getErrorMessage(ParserErrorTypesEnumForHModel.MVK_INVALID_OPERATION_IN_H_MODEL, opName);
					throw createMVKParserError(ctx, errMessage);
				}

				checker.checkNotRootGroupReferenced(paramListe, elCtx, opName, false);
				checker.checkIsMoreThanOneFieldOrIsAGroup(paramListe, elCtx, opName);

				OperationResultBaseType firstType = checker.firstType(paramListe, elCtx).getBaseType();
				if (firstType == OperationResultBaseType.STRING
						|| firstType == OperationResultBaseType.ENUM) {
					checker.checkAllFieldsLikeStringOrEnum(paramListe, elCtx, opName,
							ParserErrorTypesEnum.MVK_STRING_ENUM_AND_NON_STRING_ENUM);
					checker.checkAllAreValueValidation(paramListe, elCtx, opName);
					return;
				} else if (firstType == OperationResultBaseType.NUMBER) {
					checker.checkAllFieldsAreNumbers(paramListe, elCtx, opName, ParserErrorTypesEnum.MVK_NUMBER_AND_NON_NUMBER);
					return;
				} else if (firstType == OperationResultBaseType.DATETIME) {
					checker.checkAllFieldsAreComparableDatetimeType(paramListe, fullParser.hasBaseYear(), elCtx, opName);
					return;
				} else {
					checker.throwExceptionWithOpNameAndEntityRef(ParserErrorTypesEnum.MVK_ONLY_STRING_ENUM_NUMBER_CMP_DATE_ALLOWED,
							ctx, paramListe.get(0), opName);
				}
			default:
				throwUnknownTokenError(t);
		}
	}

}
