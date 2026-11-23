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

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckEntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.FieldUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldWertOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultTypeMap;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;

import jakarta.validation.constraints.NotNull;

/**
 * Stellt die Prüfungen für <code>FeldWertOp</code> bereit.
 * * Es wird geprüft:
 * <ul>
 * <li>dass nur ein Feld als Parameter angegeben wurde. Das Feld darf keine Wildcards enthalten.
 * {@link ParserErrorTypesEnum#MVK_NO_GROUPS_ALLOWED} und
 * {@link ParserErrorTypesEnum#MVK_NO_WILDCARDS_ALLOWED}
 * <li>Feld-WertOperation ist auf teilbekanntem Datum nicht erlaubt
 * {@link ParserErrorTypesEnum#MVK_INVALID_DATE_TYPE}
 * </ul>
 * <p>
 * Ergebnis der operation wird in der {@link OperationResultTypeMap} gespeichert.
 */

public class CheckFeldWertOpImpl extends CheckImplAbstractClass {

	public CheckFeldWertOpImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(@NotNull FeldWertOpContext ctx) {
		CheckEntityUtils checker = new CheckEntityUtils(fullParser);
		EntitySpecContext ectx = ctx.entitySpec();
		EntityRef ref = getEntityRef(ectx);

		//get the name of the operation,  because the notation is only '[', ']' and we
		//want to have nice error messages.
		String feldWertString = getBareErrorMessage(ParserErrorTypesEnum.MVK_FIELD_REF_OP);
		IField f = checker.checkIsField(ref, feldWertString, ctx);
		checker.checkNoWildCards(ref, feldWertString, ctx);
		checker.checkIsValueValidation(ref, feldWertString, ectx);

		if (FieldUtils.isDatePartialKnown(f)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_DATE_TYPE, feldWertString,
					ref.getNameBuiltFromConditionText());
			throw createMVKParserError(ctx, errMessage);
		}

		addOpTypeForFields(ctx, f);
	}

}
