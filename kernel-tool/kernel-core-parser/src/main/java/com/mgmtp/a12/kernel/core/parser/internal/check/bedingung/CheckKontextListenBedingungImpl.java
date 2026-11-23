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

import java.util.List;

import org.antlr.v4.runtime.Token;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckEntityListenUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckEntityListenUtilsHModel;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntityListeContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.KontextListenBedingungContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;

import jakarta.validation.constraints.NotNull;

/**
 * provides the additional checks for the grammar rule <code>KontextListenBedingung</code>
 */
public class CheckKontextListenBedingungImpl extends CheckImplAbstractClass {

	public CheckKontextListenBedingungImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(@NotNull KontextListenBedingungContext ctx) {
		CheckEntityListenUtils checker = new CheckEntityListenUtils(fullParser);
		CheckEntityListenUtilsHModel oChecker = new CheckEntityListenUtilsHModel(this);
		String opName = ctx.kontextListenBedingungName().getText();

		EntityListeContext elCtx = ctx.entityListe();
		List<EntityRef> paramListe = checker.getRefList(elCtx, opName);

		checker.checkNoCategories(elCtx, opName);

		if (fullParser.isHModel()) {
			checker.checkNotRootGroupReferenced(paramListe, elCtx, opName, true);
		}

		Token bedingungToken = ctx.kontextListenBedingungName().start;
		switch (bedingungToken.getType()) {
			case GrammarOnly_ConditionParser.MINDESTENS_EIN_KONTEXT_ANGEGEBEN:
			case GrammarOnly_ConditionParser.KEIN_KONTEXT_ANGEGEBEN:
				if (fullParser.isA12Model()) {
					checker.checkNoRootGroupWhenMoreThanOneParameter(paramListe, elCtx, opName);
				}
				return;
			case GrammarOnly_ConditionParser.KONTEXT_ANGEGEBEN:
			case GrammarOnly_ConditionParser.KONTEXT_NICHT_ANGEGEBEN:
				checker.checkParamSizeIs1(paramListe, elCtx, opName);
				checker.checkNoWildCards(paramListe, elCtx, opName);
				checker.checkOnlyGroups(paramListe, elCtx, opName);
				return;

			case GrammarOnly_ConditionParser.ALLE_KONTEXTE_ANGEGEBEN:
			case GrammarOnly_ConditionParser.KONTEXTE_NICHT_GEMEINSAM_ANGEGEBEN:
			case GrammarOnly_ConditionParser.NICHT_ALLE_ODER_KEIN_KONTEXT_ANGEGEBEN:
				if (fullParser.isA12Model()) { // for H model already checked (with other error message)
					checker.checkNotRootGroupReferenced(paramListe, elCtx, opName, false);
				}
				checker.checkIsMoreThanOneGroupOrField(paramListe, elCtx, opName);
				checker.checkNoWildCardsInGroup(paramListe, elCtx, opName);
				oChecker.checkNoUSBWildCard(paramListe, elCtx, opName);
				return;

			default:
				throwUnknownTokenError(bedingungToken);
		}
	}
}
