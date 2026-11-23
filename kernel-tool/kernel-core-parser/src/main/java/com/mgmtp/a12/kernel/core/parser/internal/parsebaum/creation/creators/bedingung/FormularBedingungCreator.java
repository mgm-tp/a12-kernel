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
package com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.creators.bedingung;

import org.antlr.v4.runtime.Token;

import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FormularBedingungContext;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.CreatorUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.FormularUtil;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.RootGroupConditionImpl;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.RootGroupConditionName;

/**
 * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>formularBedingung: formularBedingungName '(' formularSpec ')' ;<p>
 * formularSpec : nameOhnePunkt formularEbenenSpecHModel;   <p>
 * formularBedingungName: <p>
 * 'MindestensEinVordruckAngegeben' |'VordruckNichtAngegeben' | 'KeinVordruckAngegeben' | 'VordruckAngegeben' |
 * 'VordruckNichtFortlaufend' | 'UsbNichtFortlaufend';<p>
 * </code>
 * <p>
 * das richtige Parsebaum-Objekt.
 */
public class FormularBedingungCreator {

	private FormularBedingungCreator() {
		throw new IllegalStateException("Utility class");
	}

	public static void create(FormularBedingungContext ctx, ParseTreeCreator tc) {

		RootGroupConditionName bName = createBedingungNameFromContext(ctx);
		String formularName = getFormularName(ctx);

		/**
		 * ermittle die maximale lfd. Nr. und den max USB für das Formular
		 */
		long maxLfdNr = FormularUtil.getMaxLfdNr(formularName, tc);
		long maxUsb = FormularUtil.getMaxUsb(formularName, tc);

		FormularUtil v = FormularUtil.create(maxLfdNr, maxUsb, ctx.formularSpec());

		//Sonderbehandlung für VordruckNichtFortlaufend und UsbNichtFortlaufend
		long lfdNr = (bName == RootGroupConditionName.VordruckNichtFortlaufend || bName == RootGroupConditionName.UsbNichtFortlaufend) ?
				1 :
				v.getLfdNr();
		long usb = (bName == RootGroupConditionName.UsbNichtFortlaufend) ? 1 : v.getUsb();

		RootGroupConditionImpl b = new RootGroupConditionImpl(bName, formularName, lfdNr, usb, null);
		tc.push(b);
	}

	/**
	 * Extrahiere aus dem Kontext den Formular-Namen.
	 *
	 * @param ctx
	 * @return
	 */
	private static String getFormularName(FormularBedingungContext ctx) {
		return ctx.formularSpec().nameOhnePunkt().getText();
	}

	/**
	 * Konvertiert den Bedingungsnamen aus dem Kontext in die internen Namen.
	 */
	private static RootGroupConditionName createBedingungNameFromContext(FormularBedingungContext ctx) {
		Token t = ctx.formularBedingungName().start;
		int type = t.getType();
		switch (type) {
			case GrammarOnly_ConditionParser.MINDESTENS_EIN_VORDRUCK_ANGEGEBEN:
			case GrammarOnly_ConditionParser.VORDRUCK_ANGEGEBEN:
				return RootGroupConditionName.MindestensEinVordruckAngegeben;
			case GrammarOnly_ConditionParser.VORDRUCK_NICHT_ANGEGEBEN:
			case GrammarOnly_ConditionParser.KEIN_VORDRUCK_ANGEGEBEN:
				return RootGroupConditionName.KeinVordruckAngegeben;
			case GrammarOnly_ConditionParser.VORDRUCK_NICHT_FORTLAUFEND:
				return RootGroupConditionName.VordruckNichtFortlaufend;
			case GrammarOnly_ConditionParser.USB_NICHT_FORTLAUFEND:
				return RootGroupConditionName.UsbNichtFortlaufend;

			default:
				throw new IllegalArgumentException(CreatorUtils.getUnknownTokenErrorMessage(t));
		}
	}

}
