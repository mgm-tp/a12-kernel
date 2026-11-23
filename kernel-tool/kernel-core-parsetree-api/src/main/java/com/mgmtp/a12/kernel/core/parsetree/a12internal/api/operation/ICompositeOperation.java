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

package com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation;

import java.util.List;

import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.CalcOp;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.ast.ASTVisitor;
import com.mgmtp.a12.model.utils.OnlyForUsage;

/**
 * Represents an operation that is a composite of multiple operations that are combined with a
 * calculation operation, i.e. +, -, * or /.
 */
@OnlyForUsage
public interface ICompositeOperation extends IOperation {

	/**
	 * The list is never empty or a singleton list. Unary calculation operations are not supported.
	 *
	 * @return list of the operations, always has more than one element
	 */
	public List<IOperation> getOperations();

	/**
	 * Specifies how the operations are combined.
	 *
	 * @return calculation operation
	 */
	public CalcOp getCalcOp();

	/**
	 * Since a boolean value returned, the parse tree cannot model multiple bracketing, which is
	 * allowed in the condition text but has no semantic effect.
	 * This means multiple bracketing in the condition text is automatically removed while parsing.
	 *
	 * @return true if the operation has brackets, false otherwise
	 */
	public boolean hasBrackets();

	/**
	 * default implementation of the traversal method.
	 * Do not provide a different implementation (except e.g. for logging)
	 */
	@Override
	public default void traverse(ASTVisitor visitor) {
		if (visitor.visit(this)) {
			for (int i = 0; i < getOperations().size(); i++) {
				IOperation fop = getOperations().get(i);
				fop.traverse(visitor);
				if (!visitor.visitNext(i, this)) {
					break;
				}
			}
			visitor.endVisit(this);

		}
	}

	/**
	 * Sets the operations to <code>fops</code>. Is used to transform divisions for code generation.
	 *
	 * @param fops
	 */
	public void setOperations(List<IOperation> fops);

	/**
	 * Sets the calculation operation to <code>op</code>. Is used to transform divisions for code
	 * generation.
	 *
	 * @param op
	 */
	public void setCalcOp(CalcOp op);
}
