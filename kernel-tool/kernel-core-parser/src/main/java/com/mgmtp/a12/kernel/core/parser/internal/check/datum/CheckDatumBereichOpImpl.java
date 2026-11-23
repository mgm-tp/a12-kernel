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
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.DatumBereichOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultTypeMap;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;

import jakarta.validation.constraints.NotNull;

/**
 * Stellt die Prüfungen für <code>checkDatumAngabe</code> bereit.
 * <p>
 * Falls ein Feld für die Spezifikation verwendet wird, wird geprüft,
 * <ul>
 * <li>ob es sich um ein echtes Feld handelt.{@link ParserErrorTypesEnum#MVK_NO_GROUPS_ALLOWED}
 * <li>Das Feld darf keinen Stern spezifizieren.
 * {@link ParserErrorTypesEnum#MVK_NO_WILDCARDS_ALLOWED}
 * <li>Das Feld muss ein Datumsbereich sein. {@link ParserErrorTypesEnum#MVK_NO_DATE_RANGE}
 * </ul>
 * Für die Operation wird das Format Datum in der {@link OperationResultTypeMap} hinterlegt.
 * <p>
 */
public class CheckDatumBereichOpImpl extends CheckImplAbstractClass {
	public CheckDatumBereichOpImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(@NotNull DatumBereichOpContext ctx) {
		EntitySpecContext ectx = ctx.datumBereichAngabe().entitySpec();
		String format = null;
		if (ectx != null) {
			EntityRef ei = getEntityRef(ectx);
			CheckEntityUtils checker = new CheckEntityUtils(fullParser);
			String opName = ctx.datumBereichOpName().getText();
			IField f = checker.checkIsField(ei, opName, ctx);
			checker.checkNoWildCards(ei, opName, ctx);
			checker.checkNoCategories(ectx, opName);

			format = FieldUtils.getDateRangeFormat(f);
			if (format == null) {
				String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_NO_DATE_RANGE, opName,
						ei.getNameBuiltFromConditionText());
				throw createMVKParserError(ectx, errMessage);
			}

			format = FieldUtils.getDateFormat(f);

			if (FieldUtils.isDateRangeWithSpecificInterpretationOfYear(f)) {
				format = DateUtils.DATUM_FORMAT_WITH_YEAR;
			}
		} else {
			format = DateUtils.DATUM_FORMAT_WITH_YEAR;
		}

		addDateOpType(ctx, format, ctx.getStart().getText());
	}
}
