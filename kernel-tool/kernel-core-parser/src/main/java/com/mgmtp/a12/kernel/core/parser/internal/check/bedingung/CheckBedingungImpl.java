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
package com.mgmtp.a12.kernel.core.parser.internal.check.bedingung;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.BedingungContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.LogOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.UndContext;

import jakarta.validation.constraints.NotNull;

/**
 * Stellt die Prüfungen für <code>checkBedingung</code> bereit.
 * <p>
 * Wenn eine Bedingung sowohl <code>Und</code> als auch <code>Oder</code> enthält,
 * müssen die Teilbedingungen geklammert werden.
 * <p>
 * Diese Überprüfung wird gemacht, um Fehleingaben oder Fehlinterpretationen zu vermeiden.
 * Für eine reine Code-Erzeugung wäre diese Überprüfung nicht notwendig.
 * <p>
 * gemeldete Fehler:
 * <ul>
 * <li>{@link ParserErrorTypesEnum#MVK_BRACKET_MISSING}</li>
 * </ul>
 */
public class CheckBedingungImpl extends CheckImplAbstractClass {

	public CheckBedingungImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	/**
	 * Die Überprüfung kann so einfach gehalten werden, da sie beim Parsen jeweils beim
	 * Hinzufügen eine logische Operation aufgerufen wird. Die check-Methode wird innerhalb der
	 * zweiten Teil-Bedingung, die beliebig oft auftreten kann, aufgerufen.
	 * siehe Grammatikauszug <code>
	 * teilBedingung ( l1=logOp teilBedingung)?
	 * (l2=logOp teilBedingung {checkBedingung(_localctx);})* ;
	 * </code>
	 *
	 * @param ctx
	 * 		context of the condition
	 */
	public void check(@NotNull BedingungContext ctx) {
		boolean b1 = isUnd(ctx.l1);
		boolean b2 = isUnd(ctx.l2);
		if (b1 != b2) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_BRACKET_MISSING);
			throw createMVKParserError(ctx.l1.getStart(), ctx.l2.getStop(), errMessage);
		}
	}

	// zur besseren Lesbarkeit -> gibt true zurück,  wenn eine Und verwendet wurde.
	private boolean isUnd(@NotNull LogOpContext op) {
		return op instanceof UndContext;
	}
}
