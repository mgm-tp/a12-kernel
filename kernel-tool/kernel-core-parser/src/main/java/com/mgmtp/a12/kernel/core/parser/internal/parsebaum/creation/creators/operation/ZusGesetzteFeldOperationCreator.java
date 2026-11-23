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
package com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.creators.operation;

import java.util.Arrays;

import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.ADDSUB_OPContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.MULTDIV_OPContext;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.CompositeOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType.OperationResultBaseType;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.CalcOp;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.OperationType;

/**
 * Creates the correct parse tree object for the grammar rule
 * <p>
 * <code>
 * op1=operation (power='^') op2=operation {this.checkOperation(_localctx);} #POWER_OP
 * | op1=operation (mult='*' | div = '/')  op2=operation   #MULTDIV_OP<p>
 * | op1=operation (add='+' | sub = '-') op2=operation  #ADDSUB_OP
 * </code>
 * <p>
 */
public class ZusGesetzteFeldOperationCreator {

	private ZusGesetzteFeldOperationCreator() {
		throw new IllegalStateException("Utility class");
	}

	public static void createPower(ParseTreeCreator tc) {
		create(CalcOp.POWER, tc, OperationType.NUMBER);
	}

	//Für jede der Regel-Operationen wird eine create-Methode angeboten.
	public static void create(MULTDIV_OPContext ctx, ParseTreeCreator tc) {
		CalcOp op = (ctx.mult != null) ? CalcOp.MULTIPLY : CalcOp.DIVIDE;
		create(op, tc, OperationType.NUMBER);
	}

	public static void create(ADDSUB_OPContext ctx, ParseTreeCreator tc) {
		CalcOp op = (ctx.add != null) ? CalcOp.ADD : CalcOp.SUBTRACT;
		OperationResultType opResultType = tc.getParser().getOperationResultTypeMap().getOpType(ctx);
		OperationType opType = OperationType.NUMBER;
		if (opResultType == null || opResultType.getBaseType() == OperationResultBaseType.STRING) {
			opType = OperationType.STRING;
		}
		create(op, tc, opType);
	}

	/**
	 * Internal method to create binary operation. Used for add/sub as well as for mult/div and power.
	 * Precondition: Operation is binary.
	 *
	 * @param op
	 * 		the operation that should be created
	 * @param tc
	 * 		the parse tree creator containing the stack of operations.
	 * @param type
	 * 		the result type of the operations.
	 */

	private static void create(CalcOp op, ParseTreeCreator tc, OperationType type) {
		IOperation op2 = tc.popFop(); //take care of the ordering !
		IOperation op1 = tc.popFop();
		IOperation fop = new CompositeOperationImpl(Arrays.asList(op1, op2), op, type);
		tc.push(fop);
	}

}
