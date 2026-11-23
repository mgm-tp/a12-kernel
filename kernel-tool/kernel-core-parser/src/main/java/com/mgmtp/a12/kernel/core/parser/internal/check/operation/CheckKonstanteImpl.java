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

import java.math.BigDecimal;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.DateUtils;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.CommonKonstanteContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FalschContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.HeuteContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.JetztContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.KonstanteContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.StringLitContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.WahrContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.ZahlContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType.OperationResultBaseType;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultTypeMap;
import com.mgmtp.a12.kernel.core.utils.internal.CommonUtils;

import jakarta.validation.constraints.NotNull;

/**
 * Stellt die Prüfungen für <code>checkKonstante</code> bereit.
 * Je nach Konstanten-Typ wird ein unterschiedlicher Wert in der {@link OperationResultTypeMap}
 * gespeichert. Bei String-Konstanten wird untersucht, ob der String eine Datumskonstante ist.
 * Falls ja, wird der Wert als Datum in der {@link OperationResultTypeMap} gespeichert.
 */
public class CheckKonstanteImpl extends CheckImplAbstractClass {

	public CheckKonstanteImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(@NotNull KonstanteContext ctx) {
		if (ctx.commonKonstante() != null) {
			// Siehe auch KonstanteCreator.create (aehnlicher code)
			CommonKonstanteContext cctx = ctx.commonKonstante();
			if (cctx instanceof WahrContext) {
				addOpType(ctx, new OperationResultType(OperationResultBaseType.CONSTY, null));
			}
			if (cctx instanceof FalschContext) {
				addOpType(ctx, new OperationResultType(OperationResultBaseType.CONSTN, null));
			}
			if (cctx instanceof HeuteContext) {
				addDateOpType(ctx, DateUtils.DATUM_FORMAT_WITH_YEAR, cctx.getText());
			}
			if (cctx instanceof JetztContext) {
				addDateOpType(ctx, DateUtils.DATETIME_FORMAT, cctx.getText());
			}
			if (cctx instanceof StringLitContext) {
				String s = CommonUtils.removeAndUnescapeQuotesAndRemoveCrFromCrLf(cctx.getText());
				if (DateUtils.isStringAValidDate(s)) {
					addDateOpType(ctx, DateUtils.getDateFormatFromDate(s), s);
					return;
				}
				addStringOpType(ctx);
			}
			if (cctx instanceof ZahlContext) {
				int fractional = calcFractionalDigits((ZahlContext) cctx);
				addNumberOpType(ctx, fractional, true);
			}
		}
	}

	private int calcFractionalDigits(ZahlContext cctx) {
		if (cctx.fractionalDigits == null) {
			BigDecimal value = new BigDecimal(cctx.integerDigits.getText());
			value = value.stripTrailingZeros();
			return value.scale();
		}
		return cctx.fractionalDigits.getText().trim().length();
	}
}
