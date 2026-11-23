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

package com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition;

import java.util.List;

import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.LogOp;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.ast.ASTVisitor;
import com.mgmtp.a12.model.utils.OnlyForUsage;

/**
 * Eine Bedingung kann aus mehreren Teilbedingungen bestehen, die durch eine
 * logische Operation (<code>Und</code> bzw. <code>Oder</code>) verknüpft sind. In den
 * Namenskonventionen
 * eines Composite Pattern ist diese Klasse das <code>Composite</code>.
 */
@OnlyForUsage
public interface ICompositeCondition extends ICondition {

	/**
	 * @return gibt die logische Operation zurück
	 */
	public LogOp getLogOp();

	/**
	 * Da hier nur ein boolean Wert zurückgegeben wird, kann der Parsebaum
	 * doppelte Klammerungen, die textuell erlaubt sind aber semantisch keine
	 * Bedeutung haben, nicht darstellen.
	 * Dies bedeutet: doppelte Klammerungen in der textuellen Darstellung haben
	 * keinerlei Auswirkungen, da sie beim Parsen automatisch eliminiert werden.
	 *
	 * @return gibt zurück, ob die Bedingung geklammert ist.
	 */
	public boolean hasBrackets();

	/**
	 * die Liste ist nie leer oder ein-elementig. Es wird keine unäre logische
	 * Operation unterstützt.
	 *
	 * @return Liste der Bedingungen. Liste hat immer mindestens 2 Elemente.
	 */
	public List<ICondition> getConditionList();

	/**
	 * default implementation of the traversal method.
	 * Do not provide a different implementation (except e.g. for logging)
	 */
	@Override
	public default void traverse(ASTVisitor visitor) {
		if (visitor.visit(this)) {
			for (int i = 0; i < getConditionList().size(); i++) {
				getConditionList().get(i).traverse(visitor);
				if (!visitor.visitNext(i, this)) {
					break;
				}
			}
			visitor.endVisit(this);
		}
	}
}
