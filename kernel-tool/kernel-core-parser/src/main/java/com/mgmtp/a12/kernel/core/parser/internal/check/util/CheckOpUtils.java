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
package com.mgmtp.a12.kernel.core.parser.internal.check.util;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.StringUtils;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.BRACKET_OPContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.HeuteContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.JetztContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.OperationContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.SINGLE_OPContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType.OperationResultBaseType;
import com.mgmtp.a12.kernel.core.utils.internal.CommonUtils;

/**
 * Stellt Hilfsmethoden zum Überprüfen von Operationen bereit.
 */
public class CheckOpUtils {

	private CheckOpUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Gibt true zurück, wenn der ctx eine Konstante (oder eine geklammerte Konstante) beschreibt.
	 *
	 * @param ctx
	 * 		Kontext der überprüft werden soll
	 * @return true, wenn Kontext eine Konstante beschreibt, false sonst.
	 */
	public static boolean isConstant(OperationContext ctx) {
		if (ctx instanceof SINGLE_OPContext opc) {
			return (opc.einfacheOperation().konstante() != null);
		}
		if (ctx instanceof BRACKET_OPContext opc) {
			return isConstant(opc.operation());
		}
		return false;
	}

	/**
	 * Returns the value of the constant, if the given operation context is a constant.
	 * Quotes and surplus backslashes are removed.
	 *
	 * @param ctx
	 * 		operation context
	 * @return the value of the constants. In case of a String the quotes are removed.
	 */
	public static String getConstantValue(OperationContext ctx) {
		if (ctx instanceof SINGLE_OPContext opc) {
			return CommonUtils.removeAndUnescapeQuotesAndRemoveCrFromCrLf(opc.einfacheOperation().konstante().getText());
		}
		if (ctx instanceof BRACKET_OPContext opc) {
			return getConstantValue(opc.operation());
		}

		assert false : "method getConstant may only be called for a constant context";
		//should not be reached.
		return null;
	}

	public static boolean isHeuteOrJetzt(OperationContext ctx) {
		return isHeute(ctx) || isJetzt(ctx);
	}

	private static boolean isHeute(OperationContext ctx) {
		if (ctx instanceof SINGLE_OPContext opc) {
			return opc.einfacheOperation().konstante().commonKonstante() instanceof HeuteContext;
		}
		return false;
	}

	private static boolean isJetzt(OperationContext ctx) {
		if (ctx instanceof SINGLE_OPContext opc) {
			return opc.einfacheOperation().konstante().commonKonstante() instanceof JetztContext;
		}
		return false;
	}

	/**
	 * checks if t is a valid specification for rounding
	 *
	 * @param t
	 * 		Token to be specified
	 * @param ctx
	 * 		required for error handling
	 * @param obj
	 * 		required for error handling
	 */
	public static void isSpecificationForRounding(Token t, ParserRuleContext ctx, CheckImplAbstractClass obj) {
		//keine Angabe zum Runden -> wird akzeptiert.
		if (t == null) {
			return;
		}

		//prüfe ob token eine Zahl beschreibt
		String nk = t.getText();
		if (!StringUtils.isNumeric(nk)) {
			throwRoundingError(ctx, obj, nk);
		}

		// prüfe ob die Anzahl der Nachkommastellen korrekt ist.
		long dec = Long.parseLong(nk);
		// Allow rounding to 14 digits see ticket A12K-3691
		long max = 14;
		if (dec > max || dec < 0) {
			throwRoundingError(ctx, obj, nk);
		}

	}

	private static void throwRoundingError(ParserRuleContext ctx, CheckImplAbstractClass obj, String nk) {
		String errMessage = obj.getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_DIGIT_FOR_ROUNDING, nk);
		throw obj.createMVKParserError(ctx, errMessage);
	}

	/**
	 * checks that the operation is a number or BaseYear. If not, an error is thrown.
	 *
	 * @param ctx
	 * 		context to be checked.
	 * @param opName
	 * 		used for nice error messages only
	 * @param obj
	 * 		used for error messages
	 * @param errType
	 * 		e.g. {@link ParserErrorTypesEnum#MVK_NO_NUMBER} or
	 *        {@link ParserErrorTypesEnum#MVK_NOT_SORTABLE}
	 */
	public static void checkOpIsLikeANumber(OperationContext ctx, String opName, CheckImplAbstractClass obj,
			ParserErrorTypesEnum errType) {
		if (!isOpLikeNumber(obj.getOpType(ctx))) {
			String errMessage = obj.getErrorMessage(errType, opName);
			throw obj.createMVKParserError(ctx, errMessage);
		}
	}

	/**
	 * checks if the given context specifies an operation that matches a number.
	 * This is either a number or in ExtMP World a base year.
	 * This should be called in the context of subtraction, multiplication or division.
	 * For other operations use
	 * {@link #checkOpIsLikeANumber(OperationContext, String, CheckImplAbstractClass, ParserErrorTypesEnum)}.
	 *
	 * @param ctx
	 * 		context to be analyzed.
	 * @param obj
	 * 		used for error messages and type inference.
	 */
	public static void checkOpIsANumberInCalc(ParserRuleContext ctx, CheckImplAbstractClass obj) {
		if (!CheckOpUtils.isOpLikeNumber(obj.getOpType(ctx))) {
			String errMessage = obj.getErrorMessage(ParserErrorTypesEnum.MVK_NO_NUMBER_FOR_CALC);
			throw obj.createMVKParserError(ctx, errMessage);
		}
	}

	/**
	 * returns true, if the operation result is like a number (i.e. a number or a BaseYear)
	 *
	 * @param rt
	 * 		type of the operation result
	 * @return <code>true</code> if operation result is like a number, <code>false</code> otherwise.
	 */
	public static boolean isOpLikeNumber(OperationResultType rt) {
		OperationResultBaseType t = rt.getBaseType();
		return (t == OperationResultBaseType.NUMBER || t == OperationResultBaseType.BASE_YEAR);
	}

	/**
	 * returns true, if the operation result is like a string (i.e. a string or an enum)
	 *
	 * @param rt
	 * 		type of the operation result
	 * @return <code>true</code> if operation result is like a string, <code>false</code> otherwise.
	 */
	public static boolean isOpLikeString(OperationResultType rt) {
		OperationResultBaseType t = rt.getBaseType();
		return (t == OperationResultBaseType.STRING || t == OperationResultBaseType.ENUM);
	}

	/**
	 * Checks that the given Operation context is no constant. If it is a constant, an error is
	 * thrown.
	 *
	 * @param ctx
	 * 		context to be checked
	 * @param opName
	 * 		used for nice error messages only
	 * @param obj
	 * 		used for error messages
	 */
	public static void checkOpNoConst(OperationContext ctx, String opName, CheckImplAbstractClass obj) {
		if (CheckOpUtils.isConstant(ctx)) {
			String errMessage = obj.getErrorMessage(ParserErrorTypesEnum.MVK_NO_CONST, opName);
			throw obj.createMVKParserError(ctx, errMessage);
		}
	}

	/**
	 * Checks that the given constant is a String constant not a date constant.
	 *
	 * @param konstante
	 * 		the context of the constant.
	 * @param obj
	 * 		the checker object used for parser access and error creation.
	 */
	public static void checkNoDateConstant(GrammarOnly_ConditionParser.KonstanteContext konstante, String opName,
			CheckImplAbstractClass obj) {
		if (obj.getParser().getOperationResultTypeMap().getOpType(konstante).getBaseType() != OperationResultBaseType.STRING) {
			String errMessage = obj.getErrorMessage(ParserErrorTypesEnum.MVK_NO_DATE_CONSTANT, opName);
			throw obj.createMVKParserError(konstante, errMessage);
		}
	}
}
