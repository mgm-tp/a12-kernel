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

import java.util.ArrayList;
import java.util.List;

import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.BedingungContext;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.CompositeConditionImpl;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.LogOp;

import jakarta.validation.constraints.NotNull;

/**
 *
 */
public class ZusGesetzteBedingungCreator {

	private ZusGesetzteBedingungCreator() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Erzeugt aus der Grammatik-Regel
	 * <p>
	 * <code> bedingung:
	 * teilBedingung ( l1=logOp teilBedingung)?  (l2=logOp teilBedingung)* ;
	 * </code>
	 * <p>
	 * eine zusammengesetzte Bedingung, falls mehr als eine Teilbedingung vorhanden ist. Die
	 * einzelnen Teilbedingungen werden dazu vom Bedingungs-Stack genommen und die neu erzeugte
	 * Bedingung wird zurückgegeben. Ansonsten wird die letzte Bedingung zurückgegeben.
	 *
	 * @param ctx
	 * @param tc
	 */
	public static void create(GrammarOnly_ConditionParser.BedingungContext ctx, ParseTreeCreator tc) {
		if (ctx.getChildCount() > 1) {
			//nur die Teilbedingungen sollen verwendet werden,  überspringe die logOps.
			int numberOfConditions = (ctx.getChildCount() + 1) / 2;

			List<ICondition> liste = new ArrayList<>();
			for (int i = 0; i < numberOfConditions; i++) {
				liste.add(0, tc.popBedingung()); // take care for the correct ordering
			}
			LogOp logOp = getLogOpFromContext(ctx);
			ICondition zb = new CompositeConditionImpl(logOp, liste);
			tc.push(zb);
		}
	}

	/**
	 * Ermittle aus einem BedingungsContext die zugehörige logische Operation.
	 * Durch die Grammatik ist sichergestellt, dass in einer bedingung nur eine Art von
	 * logischen Operationen verwendet wird. Ansonsten muss geklammert werden.
	 *
	 * @param ctx
	 * @return logische Operation
	 */
	private static LogOp getLogOpFromContext(@NotNull BedingungContext ctx) {
		int type = ctx.l1.start.getType();
		if (type == GrammarOnly_ConditionParser.UND1 || type == GrammarOnly_ConditionParser.UND2 || type == GrammarOnly_ConditionParser.UND3) {
			return LogOp.And;
		} else {
			return LogOp.Or;
		}
	}

}
