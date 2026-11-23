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

package com.mgmtp.a12.kernel.core.service.internal.errortext;

import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.util.AnalyseService;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICompositeCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IRootGroupCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.LogOp;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.RootGroupConditionName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.ast.ASTVisitor;
import com.mgmtp.a12.kernel.core.service.internal.errortext.grammar.gen.ParserUtils;

/**
 * The visitor performs additional checks for the parameters
 * <ul>
 * <li><b>$#grp Fuer semIdx$</b></li>
 * <li><b>$#grp$</b> in case of a parallel iteration for the group specified by the parameter</li>
 * </ul>
 * in a rule error text.
 * <br>
 * We are rather strict here. The above parameters are only allowed if in every subformula of a
 * composite formula with OR there is a subformula which ensures the existence of the repetition of
 * <b>grp</b> using one of the following subformulas:
 * <ul>
 * <li><code>FeldAngegeben(f Fuer semIdx)</code> / <code>FeldAngegeben(f)</code> and f is element of
 * (a subgroup of) <b>grp</b></li>
 * <li><code>MindestensEinFeldAngegeben(e1 Fuer semIdx, ... , en Fuer semIdx)</code> /
 * <code>MindestensEinFeldAngegeben(eList)</code> and all fields / groups in the list are elements /
 * subgroups of (a descendant group of) <b>grp</b></li>
 * <li><code>MehrAlsEinFeldAngegeben(e1 Fuer semIdx, ... , en Fuer semIdx)</code> /
 * <code>MehrAlsEinFeldAngegeben(eList)</code> and all fields / groups in the list are elements /
 * subgroups of (a subgroup of) <b>grp</b></li>
 * <li><code>AlleFelderAngegeben(e1 Fuer semIdx, ... , en Fuer semIdx)</code>
 * / <code>AlleFelderAngegeben(fList)</code> and at least one field / group in the list is element /
 * subgroup of (a subgroup of) <b>grp</b></li>
 * <li><code>KontextAngegeben(g Fuer semIdx)</code> / <code>KontextAngegeben(g)</code> and g equals
 * to or is a subgroup of <b>grp</b></li>
 * <li><code>MindestensEinKontextAngegeben(g1 Fuer semIdx, ..., gn Fuer semIdx)</code> /
 * <code>MindestensEinKontextAngegeben(gList)</code> and all groups in the list are equal to or
 * subgroups of <b>grp</b></li>
 * <li><code>AlleKontexteAngegeben(g1 Fuer semIdx, ..., gn Fuer semIdx)</code> /
 * <code>AlleKontexteAngegeben(gList)</code> and at least one group in the list equals to or is a
 * subgroup of <b>grp</b></li>
 * <li><code>VordruckAngegeben(<b>grp</b>)</code></li>
 * </ul>
 */
public class RuleErrorTextIterationParamVisitor extends ASTVisitor {
	private boolean paramAllowed = false;

	/**
	 * Stores the information if all referenced entities of the traversed subformula are in the
	 * group specified by the error text parameter.<br>
	 * If the parameter specifies a semantic index, additionally all entities must have the same
	 * semantic index as the parameter, otherwise the variable is set to <code>false</code>. <br>
	 * If the parameter has no semantic index (i.e. if the condition specifies parallel iteration
	 * for the group of the error text parameter) all entities must specify parallel iteration too.
	 */
	private boolean allEntitiesInGroup = true;
	/**
	 * Stores the information if there is one referenced entity of the traversed subformula that is
	 * in the group specified by the error text parameter.<br>
	 * If the parameter specifies a semantic index, additionally the entity must have the same
	 * semantic index as the parameter, otherwise the variable is not set to <code>true</code>. <br>
	 * If the parameter has no semantic index (i.e. if the condition specifies parallel iteration
	 * for the group of the error text parameter) the entity must specify parallel iteration too.
	 * If the entity specifies a filter condition, the field is not set, because
	 * AlleFelderAngegeben(...) can fire without a repetition given.
	 */
	private boolean oneEntityInGroupWithoutFilterCondition = false;

	private final String grpParamFullname;
	private final String semIdxParam;

	private final String rulePath;

	/**
	 * @param grpParamFullname
	 * 		full name of the group referenced by <code>$#group$</code> or
	 * 		<code>$#group Fuer semIdxParam$</code> in the rule error text
	 * @param semIdxParam
	 * 		the semantic index parameter as represented in the rule error text;
	 * 		<code>null</code>, if it is a parallel iteration
	 * @param rulePath
	 * 		path of the rule the error text of which is checked
	 */
	public RuleErrorTextIterationParamVisitor(String grpParamFullname, String semIdxParam, String rulePath) {
		this.grpParamFullname = grpParamFullname;
		this.semIdxParam = semIdxParam;
		this.rulePath = rulePath;
	}

	public boolean isParameterAllowed() {
		return paramAllowed;
	}

	@Override
	public boolean visit(ICompositeCondition cc) {
		if (cc.getLogOp() == LogOp.Or) {
			checkOrCondition(cc);
		} else {
			checkAndCondition(cc);
		}
		//the subformulas traversed with separate visitors in the called methods
		return false;
	}

	protected void checkAndCondition(ICompositeCondition cc) {
		for (ICondition c : cc.getConditionList()) {
			RuleErrorTextIterationParamVisitor v = new RuleErrorTextIterationParamVisitor(grpParamFullname, semIdxParam, rulePath);
			c.traverse(v);
			if (v.isParameterAllowed()) {
				//parameter for one subformula of an AND-formula allowed -> parameter allowed for the AND-Formula
				this.paramAllowed = true;
				return;
			}
		}
		// no 'good' subformula found
		this.paramAllowed = false;
	}

	protected void checkOrCondition(ICompositeCondition cc) {
		for (ICondition c : cc.getConditionList()) {
			RuleErrorTextIterationParamVisitor v = new RuleErrorTextIterationParamVisitor(grpParamFullname, semIdxParam, rulePath);
			c.traverse(v);
			if (!v.isParameterAllowed()) {
				//parameter for one subformula of an OR-formula not allowed -> parameter not allowed for the OR-formula
				this.paramAllowed = false;
				return;
			}
		}
		// no 'bad' subformula found
		this.paramAllowed = true;
	}

	@Override
	public boolean visit(IEntityListCondition elc) {
		//initialize for the entity list
		allEntitiesInGroup = true;
		oneEntityInGroupWithoutFilterCondition = false;
		return true;
	}

	@Override
	public void endVisit(IEntityListCondition elc) {
		switch (elc.getConditionName()) {
			case AtLeastOneFieldFilled:
			case AtLeastOneGroupFilled:
			case MoreThanOneFieldFilled:
				paramAllowed = allEntitiesInGroup;
				break;
			case AllFieldsFilled:
			case AllGroupsFilled:
				paramAllowed = oneEntityInGroupWithoutFilterCondition;
				break;
			default:
				paramAllowed = false;
		}
	}

	@Override
	public boolean visit(IRootGroupCondition rgc) {
		// initialize
		allEntitiesInGroup = true;
		oneEntityInGroupWithoutFilterCondition = false;
		return true;
	}

	@Override
	public void endVisit(IRootGroupCondition rgc) {
		if (rgc.getConditionName() == RootGroupConditionName.MindestensEinVordruckAngegeben) {
			paramAllowed = allEntitiesInGroup;
		} else {
			paramAllowed = false;
		}
	}

	@Override
	public boolean visit(IEntityId e) {
		visitEntityInternal(e);
		// semantic index entity was checked in the method
		return false;
	}

	private void visitEntityInternal(IEntityId e) {
		String fullname = e.getFullName();
		if (!EntityUtils.isDescendantOrEqual(fullname, grpParamFullname)) {
			allEntitiesInGroup = false;
			return;
		}
		//entity specifies an element or a subgroup of the group in the error text parameter
		//-> check the semantic index if present and check if it specifies parallel iteration otherwise
		if (semIdxParam == null) {
			if (AnalyseService.isParallelIterationId(e)) {//parallel iteration -> OK
				if (e.getFilterCondition() == null) {
					oneEntityInGroupWithoutFilterCondition = true;
				}
			} else {
				allEntitiesInGroup = false;
			}
			return;
		}
		//semantic index in error text
		if (e.getSemanticIndex().isEmpty()) {//no semantic index in the entity
			allEntitiesInGroup = false;
			return;
		}
		//does the entity specify the same semantic index as the error text parameter?
		String semIdxRep = ParserUtils.getSemanticIndexRepresentation(e.getSemanticIndex().get(0));
		if (!semIdxRep.equals(semIdxParam)) {
			allEntitiesInGroup = false;
			return;
		}
		// Currently, the opposite cannot happen, because semantic index and filterCondition are not combinable
		if (e.getFilterCondition() == null) {
			oneEntityInGroupWithoutFilterCondition = true;
		}
	}

}
