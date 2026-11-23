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

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckEntityListenUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckEntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnumForHModel;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntityListeContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.WiederholungBedingungContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.AnalyseService;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;

import jakarta.validation.constraints.NotNull;

/**
 * Provides the additional checks for grammar rule <code>wiederholungBedingung</code> (language
 * construct <code>RepetitionNotUnique</code>)
 */
public class CheckWiederholungBedingungImpl extends CheckImplAbstractClass {
	public CheckWiederholungBedingungImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(@NotNull WiederholungBedingungContext ctx) {

		CheckEntityListenUtils checker = new CheckEntityListenUtils(fullParser);
		String opName = ctx.wiederholungBedingungName().getText();

		// in HDVs this operation is not allowed
		if (fullParser.isHModel()) {
			String errMessage = getErrorMessage(ParserErrorTypesEnumForHModel.MVK_INVALID_OPERATION_IN_H_MODEL, opName);
			throw createMVKParserError(ctx, errMessage);
		}

		EntityListeContext elCtx = ctx.entityListe();
		List<EntityRef> paramListe = checker.getRefList(elCtx, opName);

		checker.checkNoCategories(elCtx, opName);

		checker.checkOnlyFields(paramListe, elCtx, opName);
		checker.checkNoWildCards(paramListe, elCtx, opName);
		checker.checkNoSemanticIndex(paramListe, elCtx, opName);
		checker.checkAllAreValueValidation(paramListe, elCtx, opName);

		EntityRef from = ctx.from != null ? fullParser.getEntityRef(ctx.from) : null;
		if (from != null) {
			String fromOpName = ctx.fromOp.getText();
			CheckEntityUtils entityChecker = new CheckEntityUtils(fullParser);
			entityChecker.checkIsGroup(from, fromOpName, ctx.from);
			entityChecker.checkNoWildCards(from, fromOpName, ctx.from);
			entityChecker.checkNoSemanticIndex(from, fromOpName, ctx.from);
			entityChecker.checkRepeatableOnLowestLevel(from, ctx.from);

			checker.checkAllEntitiesInGroup(paramListe, from, elCtx, opName);
		} else {
			checker.checkAllRepeatableGroupsOnSamePath(paramListe, elCtx,
					opName); // they must probably also all be on the path of the error field's iwk
			checker.checkAllEntitiesInRuleGroup(paramListe, elCtx, opName);

			//either the rule group is the (repeatable) root group and all parameters are directly
			// a member of the root group or check that all entities have a repeatable group below
			// the rule group.
			if (!checker.areAllEntitiesDirectlyInRepeatableRootGroup(paramListe)) {
				checker.checkAllEntitiesHaveRepeatableGroupBelowRuleGroup(paramListe, elCtx, opName);
			}
		}
	}

	/**
	 * Parallel iteration is not allowed for a rule containing <code>RepetitionNotUnique</code>.
	 * This is checked after parsing because for groups on the error field path parallel iteration
	 * can be detected only in the context of the whole condition.
	 * <p>
	 * Preconditions: Has to be called after parsing is complete. The parallel iteration indices in
	 * the entity refs have to be set.
	 *
	 * @param condition
	 * @return false if the condition contains <code>RepetitionNotUnique</code> and parallel
	 * 		iteration, true otherwise
	 */
	public static boolean checkForParallelIteration(ICondition condition) {
		boolean containsRepetitionNotUnique = AnalyseService.beinhaltetWiederholungNichtEindeutig(condition);
		boolean containsParallelIteration = AnalyseService.isParallelIteration(condition, true);

		return !(containsRepetitionNotUnique && containsParallelIteration);
	}
}
