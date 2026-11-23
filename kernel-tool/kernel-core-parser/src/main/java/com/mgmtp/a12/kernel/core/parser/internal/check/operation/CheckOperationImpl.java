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
package com.mgmtp.a12.kernel.core.parser.internal.check.operation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckOpUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.FieldUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.ADDSUB_OPContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.BRACKET_OPContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EinfacheOperationContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.MULTDIV_OPContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.OperationContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.POWER_OPContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.SINGLE_OPContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParserBaseListener;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;

import jakarta.validation.constraints.NotNull;

/**
 * Stellt die Prüfungen für <code>checkOperation</code> bereit.
 * Es wird geprüft:
 * <ul>
 * <li>Es wird geprüft, dass Berechnungen nur auf Zahlen durchgeführt werden.
 * {@link ParserErrorTypesEnum#MVK_NO_NUMBER_FOR_CALC}
 * <p>
 * *
 * <li>Es darf nur eine Division in einer Teil-Operation verwendet werden. Dies bedeutet, dass
 * Kettenbrüche nicht erlaubt sind. Natürlich kann innerhalb einer Bedingung mehrfach eine Division
 * verwendet werden – also z.B 1/5 * [Feld1] + 1/3 *[Feld2]. Diese Einschränkung dient dazu, dass
 * weitere Umformungen vereinfacht werden können. Außerdem erscheint die Einschränkung aus Gründen
 * der Lesbarkeit der Berechnungen sinnvoll. {@link ParserErrorTypesEnum#MVK_TOO_MANY_DIV_FOR_CALC}
 * </ul>
 */

public class CheckOperationImpl extends CheckImplAbstractClass {

	public CheckOperationImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(@NotNull OperationContext ctx) {
		if (isSingleOrBracketOp(ctx)) { // nothing has to be checked.
			moveToThisContext(ctx);
			return;
		}
		checkAndSetTypes(ctx);
		checkOnlyOneDiv(ctx);
		checkOnlyOnePow(ctx);
	}

	/**
	 * checks, that all operations are type consistent.
	 * Multiplication division and subtraction is only applicable to numbers.
	 * Addition is applicable to numbers and strings, but only coherently.
	 *
	 * @param ctx
	 * 		specifies the given operation context.
	 */
	private void checkAndSetTypes(OperationContext ctx) {
		if (ctx instanceof POWER_OPContext powerOpContext) {
			checkAndSetPowerType(powerOpContext);
		}
		if (ctx instanceof MULTDIV_OPContext mdCtx) {
			checkAndSetMultDivType(mdCtx);
		}
		if (ctx instanceof ADDSUB_OPContext asCtx) {
			checkAndSetAddSubType(asCtx);
		}
	}

	private void checkAndSetPowerType(POWER_OPContext powerOpContext) {
		CheckOpUtils.checkOpIsANumberInCalc(powerOpContext.op1, this);
		CheckOpUtils.checkOpIsANumberInCalc(powerOpContext.op2, this);
		checkNoFractionalDigitsInExponent(powerOpContext.op2);
		Integer maxFractionalDigits = calcFractionalDigits(powerOpContext);
		addNumberOpType(powerOpContext, maxFractionalDigits, false);
	}

	private Integer calcFractionalDigits(POWER_OPContext powerOpContext) {
		// Only check the simplest case of no fractional digit base with constant positive exponent
		Integer fractionalDigitsBase = getMaxFractionalDigits(getOpType(powerOpContext.op1));
		if (fractionalDigitsBase == null || fractionalDigitsBase != 0) {
			return null;
		}
		String constant = extractSimpleConstant(powerOpContext.op2);
		if (constant == null) {
			return null;
		}
		try {
			BigDecimal value = new BigDecimal(constant);
			if (value.signum() >= 0) {
				return 0;
			}
		} catch (NumberFormatException ex) {
			return null;
		}
		return null;
	}

	private String extractSimpleConstant(OperationContext opCtx) {
		if (opCtx instanceof SINGLE_OPContext singleOp) {
			GrammarOnly_ConditionParser.KonstanteContext constant = singleOp.einfacheOperation().konstante();
			if (constant != null) {
				return constant.getText();
			}
		}
		return null;
	}

	protected void checkAndSetMultDivType(MULTDIV_OPContext mdCtx) {
		CheckOpUtils.checkOpIsANumberInCalc(mdCtx.op1, this);
		CheckOpUtils.checkOpIsANumberInCalc(mdCtx.op2, this);
		if (mdCtx.mult == null) {
			// For division, we cannot really make a prediction
			addNumberOpType(mdCtx, null, false);
		} else {
			List<Integer> fractionals = new ArrayList<>();
			fractionals.add(getMaxFractionalDigits(getOpType(mdCtx.op1)));
			fractionals.add(getMaxFractionalDigits(getOpType(mdCtx.op2)));
			// If either of the operands has a multiplicative operand, the multiplication also has the same.
			boolean containsConstant = hasMultiplicativeConstant(getOpType(mdCtx.op1)) || hasMultiplicativeConstant(
					getOpType(mdCtx.op2));
			Integer maxFractionalDigits = super.computeAdditionOfFractionalDigits(fractionals);
			addNumberOpType(mdCtx, maxFractionalDigits, containsConstant);
		}
	}

	protected void checkAndSetAddSubType(ADDSUB_OPContext addSubCtx) {
		if (addSubCtx.add != null) {
			checkAndSetAdditionType(addSubCtx);
		} else {
			CheckOpUtils.checkOpIsANumberInCalc(addSubCtx.op1, this);
			CheckOpUtils.checkOpIsANumberInCalc(addSubCtx.op2, this);
			addAddSubNumberOpType(addSubCtx);
		}
	}

	private void addAddSubNumberOpType(ADDSUB_OPContext addSubCtx) {
		List<Integer> fractionals = new ArrayList<>();
		OperationResultType opType1 = getOpType(addSubCtx.op1);
		OperationResultType opType2 = getOpType(addSubCtx.op2);
		fractionals.add(getMaxFractionalDigits(opType1));
		fractionals.add(getMaxFractionalDigits(opType2));
		Integer maxFractionalDigits = super.computeUnionOfFractionalDigits(fractionals);
		// For additions, both operands must have a multiplicative constant to be expandable
		boolean withConstants = opType1.hasMultiplicativeConstant() && opType2.hasMultiplicativeConstant();
		// Adding a constant should not influence the fractionals
		addNumberOpType(addSubCtx, maxFractionalDigits, withConstants);
	}

	private void checkAndSetAdditionType(ADDSUB_OPContext ctx) {
		OperationResultType opType1 = getOpType(ctx.op1);
		OperationResultType opType2 = getOpType(ctx.op2);
		if (CheckOpUtils.isOpLikeNumber(opType1) && CheckOpUtils.isOpLikeNumber(opType2)) {
			addAddSubNumberOpType(ctx);
			return;
		}
		if (CheckOpUtils.isOpLikeString(opType1) && CheckOpUtils.isOpLikeString(opType2)) {
			checkWithoutDisplayValues(ctx.op1);
			checkWithoutDisplayValues(ctx.op2);
			addStringOpType(ctx);
			return;
		}
		String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_NO_STRING_OR_NUMBER_FOR_ADD);
		throw createMVKParserError(ctx, errMessage);
	}

	private void checkWithoutDisplayValues(OperationContext opCtx) {
		if (!(opCtx instanceof SINGLE_OPContext singleOpContext)) {
			return;
		}
		EinfacheOperationContext eoCtx = singleOpContext.einfacheOperation();
		if (eoCtx.feldWertOp() == null) {
			return;
		}
		EntitySpecContext entitySpec = eoCtx.feldWertOp().entitySpec();
		EntityRef entityRef = getEntityRef(entitySpec);
		if (entityRef.getCategoryName() != null) {
			// category access is allowed
			return;
		}
		IField field = (IField) entityRef.getEntity();
		if (!FieldUtils.isEnum(field)) {
			// non-enums are unproblematic -> at the moment only strings and enums are allowed
			return;
		}
		if (!FieldUtils.isEnumWithDisplayValues(field.getType())) {
			// enums without display values are ok
			return;
		}
		String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_NO_STRING_OR_NUMBER_FOR_ADD);
		throw createMVKParserError(opCtx, errMessage);
	}

	private void checkNoFractionalDigitsInExponent(OperationContext ctx) {
		Integer fractionalDigits = getMaxFractionalDigits(getOpType(ctx));
		if (fractionalDigits == null || fractionalDigits > 0) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_NUMBER_FOR_EXP);
			throw createMVKParserError(ctx, errMessage);
		}
	}

	/**
	 * checks that power operations are not nested
	 *
	 * @param ctx
	 * 		context to be specified.
	 */
	private void checkOnlyOnePow(OperationContext ctx) {

		class MyListener extends GrammarOnly_ConditionParserBaseListener {
			@Override public void enterPOWER_OP(POWER_OPContext ctx) {
				if (ctx.op1 instanceof POWER_OPContext) {
					String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_TOO_MANY_POW_FOR_CALC);
					throw createMVKParserError(ctx, errMessage);
				}
			}
		}
		ParseTreeWalker walker = new ParseTreeWalker();
		MyListener m = new MyListener();
		walker.walk(m, ctx);
	}

	/**
	 * checks that only one div is used in a mult-div operation.
	 * 1/3 + 1/4 is allowed; but 1/3 *1/4 is not allowed.
	 * <p>
	 * the generated listener is used.
	 * <p>
	 * if a division is found: if a division has already been found, throw an error, otherwise set
	 * the flag
	 * <code>divFound</code> to true.
	 * <p>
	 * if a addition or subtraction is found: set the flag <code>divFound</code> to false.
	 *
	 * @param ctx
	 * 		context to be specified.
	 */
	private void checkOnlyOneDiv(OperationContext ctx) {

		class MyListener extends GrammarOnly_ConditionParserBaseListener {
			public final Boolean[] divFound = new Boolean[1];

			public MyListener() {
				//initialise: no div found,
				divFound[0] = false;
			}

			@Override
			public void enterMULTDIV_OP(@NotNull GrammarOnly_ConditionParser.MULTDIV_OPContext ctx) {
				if (ctx.div != null) {
					if (divFound[0]) { //an second div is found -> error
						String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_TOO_MANY_DIV_FOR_CALC);
						throw createMVKParserError(ctx, errMessage);
					}
					divFound[0] = true;
				}
			}

			//Note:  eigentlich könnten auch Fälle, die geklammert sind, als mehrfache Divisionen  but the current test data do not fail in case of
			// erkannt werden.  D.h. Fälle wie: a / {b/c}
			// Da dies jedoch auch im alten Parser nicht als Fehler erkannt wurde,  wird auch hier dieser
			// Fall akzeptiert - die Testdaten sind entsprechend.
			// Falls hier genauer überprüft werden soll,  muss diese Methode entfernt werden und die Testdaten
			// entsprechend angepasst werden.
			@Override
			public void enterBRACKET_OP(@NotNull GrammarOnly_ConditionParser.BRACKET_OPContext ctx) {
				ParseTreeWalker walker = new ParseTreeWalker();
				MyListener m1 = new MyListener();
				walker.walk(m1, ctx.operation());
				//we stop the current traversal: using an exception is the only way to directly stop traversing.
				throw new ParseCancellationException();
			}

			@Override
			public void enterADDSUB_OP(@NotNull GrammarOnly_ConditionParser.ADDSUB_OPContext ctx) {
				ParseTreeWalker walker = new ParseTreeWalker();
				MyListener m1 = new MyListener();
				walker.walk(m1, ctx.op1);

				MyListener m2 = new MyListener();
				walker.walk(m2, ctx.op2);

				//we stop the current traversal: using an exception is the only way to directly stop traversing.
				throw new ParseCancellationException();
			}

			@Override public void enterPOWER_OP(POWER_OPContext ctx) {
				ParseTreeWalker walker = new ParseTreeWalker();
				MyListener m1 = new MyListener();
				walker.walk(m1, ctx.op1);

				MyListener m2 = new MyListener();
				walker.walk(m2, ctx.op2);

				//we stop the current traversal: using an exception is the only way to directly stop traversing.
				throw new ParseCancellationException();
			}
		}
		try {
			ParseTreeWalker walker = new ParseTreeWalker();
			MyListener m = new MyListener();
			walker.walk(m, ctx);
		} catch (ParseCancellationException e) { // NOPMD
			//just used to bail out.
		}
	}

	//helper method to get readable code.
	private boolean isSingleOrBracketOp(OperationContext op) {
		return (op instanceof SINGLE_OPContext || op instanceof BRACKET_OPContext);
	}

}
