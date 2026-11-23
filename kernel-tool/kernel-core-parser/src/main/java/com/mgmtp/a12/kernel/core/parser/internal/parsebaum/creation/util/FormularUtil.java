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
package com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util;

import org.antlr.v4.runtime.tree.ParseTreeWalker;

import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeKonkretContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeNewSternContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeOldSternContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FormularSpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.UsbSpezifierContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParserBaseListener;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IParseTreeEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;

import jakarta.validation.constraints.NotNull;

/**
 * Hilfsklasse: wird verwendet, um laufende Nummer und USB aus der FormularEbenenSpec zu erhalten.
 */
public class FormularUtil extends GrammarOnly_ConditionParserBaseListener {
	private long lfdNr;
	private long usb;

	private FormularUtil(long maxLfdNr, long maxUsb) {
		// wenn das Formular nicht wiederholbar ist,  wird immer 1 verwendet,
		this.lfdNr = maxLfdNr > 1 ? IParseTreeEntity.ITERATION : 1;
		//Falls USB spezifiziert wird,  ist der default Iteration
		this.usb = maxUsb == IParseTreeEntity.UNSPECIFIED ? IParseTreeEntity.UNSPECIFIED : IParseTreeEntity.ITERATION;
	}

	@Override
	public void exitFeKonkret(@NotNull FeKonkretContext ctx) {
		lfdNr = Long.parseLong(ctx.ZiffernFolge().getText());
	}

	@Override
	public void exitFeNewStern(@NotNull FeNewSternContext ctx) {
		lfdNr = IParseTreeEntity.WILDCARD;
	}

	@Override
	public void exitFeOldStern(@NotNull FeOldSternContext ctx) {
		lfdNr = IParseTreeEntity.WILDCARD;
	}

	@Override
	public void exitUsbSpezifier(@NotNull UsbSpezifierContext ctx) {
		usb = IParseTreeEntity.WILDCARD;
	}

	public long getUsb() {
		return usb;
	}

	public long getLfdNr() {
		return lfdNr;
	}

	/**
	 * Ermittle die für das Formular spezifizierte laufende Nummer und den USB.
	 * die Ermittlung erfolgt eine Traversierung über FormularSpecContext.
	 *
	 * @param maxLfdNr
	 * @param ctx
	 * @return
	 */
	public static FormularUtil create(long maxLfdNr, long maxUsb, FormularSpecContext ctx) {
		FormularUtil v = new FormularUtil(maxLfdNr, maxUsb);
		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk(v, ctx);
		return v;
	}

	public static long getMaxLfdNr(String formularName, ParseTreeCreator tc) {
		return EntityUtils.getMaxLfdNr(getEntity(formularName, tc));
	}

	public static long getMaxUsb(String formularName, ParseTreeCreator tc) {
		return EntityUtils.getMaxUsb(getEntity(formularName, tc));
	}

	private static IEntity getEntity(String formularName, ParseTreeCreator tc) {
		return tc.getParser().getEc().get("/" + formularName);
	}
}
