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

import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.CommonKonstanteContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.DatumAngabeContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FalschContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.HeuteContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.JetztContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.KonstanteContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.KonstanteReferenzjahrContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.StringLitContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.WahrContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.ZahlContext;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.CreatorUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.BoolConstantOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.ConstantOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.PointInTimeOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.BoolConstantOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.OperationType;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.PointInTimeOperationName;
import com.mgmtp.a12.kernel.core.utils.internal.CommonUtils;

/**
 * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>
 * konstante: commonKonstante ;<p>
 * commonKonstante: <p>
 * 'Falsch' #falsch <p>
 * |'Wahr' #wahr<p>
 * |'Heute' #heute<p>
 * |'Jetzt' #jetzt<p>
 * | StringLiteral #stringLit<p>
 * | minus='-'? integerDigits=ZiffernFolge (decimalSeparator='.' fractionalDigits=ZiffernFolge)? #zahl;<p>
 * </code>
 *
 * <p>
 * und für die Grammatik-Regel
 * <br>
 * <code><p>
 * konstanteReferenzjahr:  'Referenzjahr'<p>
 * </code>
 * <br><p>
 * die richtigen Parse-Baum Objekte.
 */
public class KonstanteCreator {

	private KonstanteCreator() {
		throw new IllegalStateException("Utility class");
	}

	public static void create(KonstanteContext ctx, ParseTreeCreator tc) {
		CommonKonstanteContext cctx = ctx.commonKonstante();
		String text = cctx.getText();
		IOperation result = null;

		// Durch die Grammatik ist zugesichert, dass genau eine Alternative wahr ist.
		// Siehe auch CheckKonstanteImpl.check (aehnlicher Code)
		if (cctx instanceof WahrContext) {
			result = new BoolConstantOperationImpl(BoolConstantOperationName.True);
		}
		if (cctx instanceof FalschContext) {
			result = new BoolConstantOperationImpl(BoolConstantOperationName.False);
		}
		if (cctx instanceof HeuteContext) {
			result = new PointInTimeOperationImpl(PointInTimeOperationName.Today);
		}
		if (cctx instanceof JetztContext) {
			result = new PointInTimeOperationImpl(PointInTimeOperationName.Now);
		}
		if (cctx instanceof StringLitContext) {
			String s = CommonUtils.removeAndUnescapeQuotesAndRemoveCrFromCrLf(text);
			OperationType typeAccordingToParser = OperationResultType.getType(
					tc.getParser().getOperationResultTypeMap().getOpType(ctx));
			if (typeAccordingToParser != OperationType.DATE && typeAccordingToParser != OperationType.STRING) {
				throw new IllegalArgumentException("Unexpected type for string literal constant: " + typeAccordingToParser);
			}
			result = new ConstantOperationImpl(s, typeAccordingToParser);
		}
		if (cctx instanceof ZahlContext) {
			String s = removeLeadingZeros((ZahlContext) cctx);
			result = new ConstantOperationImpl(s, OperationType.NUMBER);
		}
		if (result == null) {
			//Durch die Grammatik ist zugesichert, dass genau eine Alternative wahr ist.
			// dieser Fehler kann somit nur auftreten,  wenn die Grammatik geändert wurde,
			// ohne dass dieser Code angepasst wurde.
			throw new IllegalArgumentException("Unknown terminal '" + text + "'");
		}
		tc.push(result);
	}

	public static void createBaseYearConstant(KonstanteReferenzjahrContext ctx, ParseTreeCreator tc) {
		if (ctx.parent instanceof DatumAngabeContext) {
			tc.push(new PointInTimeOperationImpl(PointInTimeOperationName.BaseYear, OperationType.DATE));
		} else {
			tc.push(new PointInTimeOperationImpl(PointInTimeOperationName.BaseYear));
		}
	}

	public static String removeLeadingZeros(ZahlContext ctx) {
		String sign = CreatorUtils.tokenTextOrEmptyString(ctx.minus);
		String integerDigits = ctx.integerDigits.getText();
		String decimalSeparator = CreatorUtils.tokenTextOrEmptyString(ctx.decimalSeparator);
		String fractionalDigits = CreatorUtils.tokenTextOrEmptyString(ctx.fractionalDigits);

		//remove leading zeros if necessary
		integerDigits = integerDigits.replaceFirst("^0+(?!$)", "");
		return String.format("%s%s%s%s", sign, integerDigits, decimalSeparator, fractionalDigits);
	}

}
