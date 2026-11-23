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
package com.mgmtp.a12.kernel.core.parser.internal.util;

import java.util.IdentityHashMap;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;

import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldWertOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.NameOhnePunktContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType.OperationResultBaseType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IDateRangeType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IDateType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEnumerationType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IFieldType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.INumberType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IPredefinedType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IStringType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IUnspecifiedType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IYesNoType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IYesType;

/**
 * This class saves the type of an operation.
 * This is used to check, whether an operation is consistent with the types of its operands.
 */
public class OperationResultTypeMap {

	private final IdentityHashMap<RuleContext, OperationResultType> map = new IdentityHashMap<>();

	public void addOpType(RuleContext ctx, OperationResultType type) {
		map.put(ctx, type);
	}

	public OperationResultType getOpType(RuleContext ctx) {
		return map.get(ctx);
	}

	public void moveToThisContext(ParserRuleContext ctx) {
		OperationResultType value = map.get(ctx.getChild(ParserRuleContext.class, 0));
		if (value == null) { // to handle brackets.
			value = map.get(ctx.getChild(ParserRuleContext.class, 1));
		}
		map.put(ctx, value);
	}

	private static boolean isLikeStringType(IFieldType t) {
		return t == null || t instanceof IStringType || t instanceof IPredefinedType;
	}

	/**
	 * Add the operation result type for a given field type.
	 * <ul>
	 * <li>strings, predefined types, and extensible enumerations are treated are STRINGS
	 * <li>for the date and the date range, the format is added.
	 * <li>for numbers, the max fractional digits are transferred.
	 *
	 * @param ctx
	 * 		context to which the operation result type should be added
	 * @param t
	 * 		type of the field.
	 */
	public void addOpTypeForField(FeldWertOpContext ctx, IFieldType t) {
		addOpType(ctx, getOpTypeForField(ctx, t));
	}

	public static OperationResultType getOpTypeForField(RuleContext ctx, IFieldType t) {
		if (isLikeStringType(t)) {
			return new OperationResultType(OperationResultBaseType.STRING, t);
		}

		if (t instanceof INumberType) {
			Integer maxFractionalDigits = ((INumberType) t).getMaxFractionalDigits();
			// Normalize -1 to null
			maxFractionalDigits = maxFractionalDigits == null || maxFractionalDigits == -1 ? null : maxFractionalDigits;
			return OperationResultType.createNumberType(t, maxFractionalDigits, false);
		}
		if (t instanceof IEnumerationType) {
			IEnumerationType et = (IEnumerationType) t;
			String category = extractCategory(ctx);
			return OperationResultType.createEnumerationType(et, category);
		}

		if (t instanceof IDateType) {
			return OperationResultType.createTypeWithFormat(OperationResultBaseType.DATETIME, t, ((IDateType) t).getFormat(), "");
		}
		if (t instanceof IDateRangeType) {
			return OperationResultType.createTypeWithFormat(OperationResultBaseType.DATE_RANGE, t,
					((IDateRangeType) t).getFormatOfDate(),
					"");
		}
		if (t instanceof IYesNoType) {
			IYesNoType yesNoType = (IYesNoType) t;
			return OperationResultType.createBooleanType(OperationResultBaseType.BOOLYN, yesNoType);
		}

		if (t instanceof IYesType) {
			IYesType yesType = (IYesType) t;
			return OperationResultType.createBooleanType(OperationResultBaseType.BOOLY, yesType);
		}

		if (t instanceof IUnspecifiedType) {
			IUnspecifiedType unspecifiedType = (IUnspecifiedType) t;
			return OperationResultType.createUnspecifiedType(unspecifiedType);
		}

		throw new RuntimeException("Im nothing!" + t.getClass().getName());
	}

	private static String extractCategory(RuleContext ctx) {
		String category = null;
		if (ctx instanceof FeldWertOpContext) {
			NameOhnePunktContext categoryCtx = ((FeldWertOpContext) ctx).entitySpec().categoryName;
			category = categoryCtx == null ? null : categoryCtx.getText();
		}
		return category;
	}
}
