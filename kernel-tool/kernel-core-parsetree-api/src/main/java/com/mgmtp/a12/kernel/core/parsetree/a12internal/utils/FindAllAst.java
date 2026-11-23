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
package com.mgmtp.a12.kernel.core.parsetree.a12internal.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IParseTreeEntity;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.ast.ASTVisitor;
import com.mgmtp.a12.model.utils.OnlyForUsage;

/**
 * Returns all IParseBaumEntities satisfying the given condition
 */
@OnlyForUsage
public class FindAllAst extends ASTVisitor {

	private Predicate<IParseTreeEntity> tester;
	private List<IParseTreeEntity> result = new ArrayList<>();
	private boolean useFilterCondition = false;

	private FindAllAst(Predicate<IParseTreeEntity> tester, boolean useFilterCondition) {
		this.tester = tester;
		this.useFilterCondition = useFilterCondition;
	}

	@Override
	public void endVisitGeneral(IParseTreeEntity op) {
		if (tester.test(op)) {
			result.add(op);
		}
	}

	@Override
	public void endVisit(IEntityId p) {
		super.endVisit(p);
		if (!useFilterCondition) {
			return;
		}
		if (p.getFilterCondition() != null) {
			p.getFilterCondition().traverse(this);
		}
	}

	public static List<IParseTreeEntity> findAll(IParseTreeEntity parseTreeEntity, Predicate<IParseTreeEntity> tester,
			boolean useFilterCondition) {
		FindAllAst v = new FindAllAst(tester, useFilterCondition);
		parseTreeEntity.traverse(v);
		return v.result;
	}
}
