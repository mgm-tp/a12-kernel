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

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckEntityListenUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckOpUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.AnzahlWertOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.CommonKonstanteContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntityListeContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FalschContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.StringLitContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.WahrContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.ZahlContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultTypeMap;
import com.mgmtp.a12.kernel.core.utils.internal.CommonUtils;

import jakarta.validation.constraints.NotNull;

/**
 * Stellt die Prüfungen für <code>checkAnzahlAngegebenOp</code> bereit.
 * dies entspricht <code>AnzahlWertInFeldliste</code>
 * <p>
 * Es wird geprüft:
 * <ul>
 * <li>
 * In der Parameterliste sollen keine Parameter doppelt enthalten sein. Dies ist möglich entweder
 * durch eine Doppelnennung {@link ParserErrorTypesEnum#MVK_DUPLICATE_PARAM1}, oder durch indirekte
 * Doppelnennung {@link ParserErrorTypesEnum#MVK_DUPLICATE_PARAM2}, da ein Feld / ein Kontext auch
 * indirekt durch einen umfassenden Kontext spezifiziert werden kann. Die Überprüfung wurde
 * eingeführt, um Fehleingaben z.B. durch Copy/Paste-Fehler zu vermeiden.
 * <p>
 * Durch die Grammatik wird bereits geprüft, dass die Parameterliste nicht leer ist.</li>
 * <li>Es wird geprüft, dass mehr als ein Feld oder mindestens ein Kontext oder ein Feld mit
 * mindestens einem Stern angegeben wurde.{@link ParserErrorTypesEnum#MVK_PARAMSIZE_INVALIDN}
 * <li>Der Wert ist vom Typ Zahl, String oder Wahr/Falsch (
 * {@link ParserErrorTypesEnum#MVK_INVALID_CONST}). Die Feldliste darf nur Felder mit dazu
 * kompatiblen Typen enthalten. Je nach Typ der Konstante wird geprüft. Im Fehlerfall wird eine der
 * vier folgenden Fehlermeldungen ausgegeben:
 * <ul>
 * <li>{@link ParserErrorTypesEnum#MVK_NO_NUMBER}</li>
 * <li>{@link ParserErrorTypesEnum#MVK_NO_TYPE_YES_OR_YESNO}</li>
 * <li>{@link ParserErrorTypesEnum#MVK_NO_STRING_OR_ENUM}</li>
 * <li>{@link ParserErrorTypesEnum#MVK_NO_TYPEYESNO}</li>
 * </ul>
 * <li>Falls die Felder Enumerationen sind, wird geprüft, ob die Konstante ein gültiger Wert der verwendeten Enumerationen bzw.
 * bei Kategorienzugriff der verwendeten Kategorien ist.
 * {@link ParserErrorTypesEnum#MVK_INVALID_STRING_CONSTANT_FOR_ENUMERATION} bzw.
 * {@link ParserErrorTypesEnum#MVK_INVALID_STRING_CONSTANT_FOR_ENUMERATION_CATEGORY}</li>
 * <li>Falls die Konstante 'Wahr' ist, wird geprüft, ob alle Felder (vom Typ Ja oder JaNein)
 * den gleichen Ja-Wert spezifizieren. {@link ParserErrorTypesEnum#MVK_DIFFERENT_YES_VALUE}</li>
 * <li>Falls die Konstante 'Falsch' ist, wird geprüft, ob alle Felder den gleichen Nein-Wert
 * spezifizieren. {@link ParserErrorTypesEnum#MVK_DIFFERENT_NO_VALUE}</li>
 * </ul>
 * <p>
 * Für die Operation wird das Format NUMBER in der {@link OperationResultTypeMap} hinterlegt.
 */
public class CheckAnzahlWertOpImpl extends CheckImplAbstractClass {
	public CheckAnzahlWertOpImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(@NotNull AnzahlWertOpContext ctx) {
		addNumberOpType(ctx, 0, false);

		CheckEntityListenUtils checker = new CheckEntityListenUtils(fullParser);

		EntityListeContext elCtx = ctx.entityListe();
		String opName = ctx.anzahlWertOpName().getText();
		List<EntityRef> paramListe = checker.getRefList(elCtx, opName);

		checker.checkIsMoreThanOneFieldOrIsAGroup(paramListe, elCtx, opName);
		checker.checkNotRootGroupReferenced(paramListe, elCtx, opName, fullParser.isHModel());
		CommonKonstanteContext cctx = ctx.konstante().commonKonstante();

		if (cctx instanceof FalschContext) {
			checker.checkFieldsAreYesNoWithSameNoValue(paramListe, elCtx, opName);
			return;
		}

		if (cctx instanceof WahrContext) {
			checker.checkFieldsAreYesOrYesNoWithSameYesValue(paramListe, elCtx, opName);
			return;
		}

		if (cctx instanceof StringLitContext sctx) {
			String value = CommonUtils.removeAndUnescapeQuotesAndRemoveCrFromCrLf(sctx.getText());
			checker.checkAllFieldsAreValidStringOrEnums(paramListe, elCtx, value, opName);
			checker.checkAllAreValueValidation(paramListe, elCtx, opName);
			CheckOpUtils.checkNoDateConstant(ctx.konstante(), opName, this);
			return;
		}
		if (cctx instanceof ZahlContext) {
			checker.checkAllFieldsAreNumbers(paramListe, elCtx, opName, ParserErrorTypesEnum.MVK_NO_NUMBER);
			return;
		}

		String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_CONST, ctx.anzahlWertOpName().getText());
		throw createMVKParserError(ctx, errMessage);

	}

}
