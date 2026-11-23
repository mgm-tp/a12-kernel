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
package com.mgmtp.a12.kernel.core.parser.internal.check;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.RuleContext;
import org.apache.commons.lang3.StringUtils;

import com.mgmtp.a12.kernel.core.parser.internal.check.entity.CheckIdNameRefImpl;
import com.mgmtp.a12.kernel.core.parser.internal.check.entity.CheckRegelKontextEntityImpl;
import com.mgmtp.a12.kernel.core.parser.internal.check.entity.hdv.CheckIdNameRefHModelImpl;
import com.mgmtp.a12.kernel.core.parser.internal.error.MVKParserError;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldWertOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.DateFormat;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRefMap;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType.OperationResultBaseType;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultTypeMap;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.INumberType;

/**
 * Abstract super class für alle Checker-Klassen
 * <p>
 * Die einzelnen Checkerklassen implementieren jeweils die check-Methoden, die in der Grammatik
 * beim Parsen aufgerufen werden.
 * <p>
 * Als Namenskonvention wird jeweils der Name der check-Methode mit dem Präfix <code>Check</code>
 * und dem Suffix <code>Imp</code> für
 * den Klassennamen verwendet. Der Name der Klasse ohne Suffix und Präfix entspricht dann immer der
 * Produktionsregel in der Grammatik.
 * <p>
 * Im Falle eines Fehlers wird ein {@link MVKParserError} geworfen und das Parsen unterbrochen.
 * Somit wird jeweils höchstens ein Fehler gemeldet. Mehrere Fehler zu melden ist auch nicht
 * sinnvoll,
 * da in der Regel ein Fehler bereits so gravierend ist, dass ein sinnvolles Parsen nicht mehr
 * möglich ist.
 * Somit wird das Melden von Folgefehlern vermieden und die Komplexität der Fehlerbehandlung
 * wesentlich reduziert.
 * <p>
 * Die abstrakte Superklasse stellt folgendes bereit:
 * <ul>
 * <li>Hilfsmethoden für das Error Handling</li>
 * <li>Zugriff auf eine Map {@link EntityRefMap} , in der alle in der Regelbedingung geparsten
 * Entities gespeichert werden. Als Key in der Map wird der jeweilige EntityContext verwendet. Diese
 * Map wird während des Parsens gefüllt {@link CheckIdNameRefImpl}, {@link CheckIdNameRefHModelImpl}
 * und {link {@link CheckRegelKontextEntityImpl}</li>
 * <li>Zugriff auf eine Map {@link OperationResultTypeMap}, in der der Ergebnistyp
 * {@link OperationResultType} der Operationen gespeichert wird. Diese Map wird während des Parsens
 * gefüllt und für spätere Überprüfungen verwendet.</li>
 */
public abstract class CheckImplAbstractClass extends CheckImplLenientBaseClass {

	protected FullChecks_ConditionParser fullParser;

	protected CheckImplAbstractClass(FullChecks_ConditionParser parser) {
		super(parser);
		this.fullParser = parser;
	}

	@Override public FullChecks_ConditionParser getParser() {
		return fullParser;
	}

	protected static Predicate<EntitySpecContext> isCategoryDefined = x -> (x.categoryName != null && !StringUtils.isEmpty(
			x.categoryName.getText()));

	/**
	 * Returns the type of the operation specified in the given RuleContext.
	 * If no operation type is saved for the rule context, null is returned.
	 *
	 * @param ctx
	 * 		context to get the operation type for
	 * @return type of the operation
	 */
	public OperationResultType getOpType(RuleContext ctx) {
		OperationResultTypeMap typeMap = fullParser.getOperationResultTypeMap();
		return typeMap.getOpType(ctx);
	}

	public void addOpType(RuleContext ctx, OperationResultType type) {
		fullParser.getOperationResultTypeMap().addOpType(ctx, type);
	}

	public void addDateOpType(RuleContext ctx, String format, String textForErrorMessaging) {
		OperationResultType aDate = OperationResultType.createTypeWithFormat(OperationResultBaseType.DATETIME, null,
				DateFormat.of(format), textForErrorMessaging);
		addOpType(ctx, aDate);
	}

	public void addDateRangeOpType(RuleContext ctx, DateFormat format, String textForErrorMessaging) {
		OperationResultType resultType = OperationResultType.createTypeWithFormat(OperationResultBaseType.DATE_RANGE, null, format,
				textForErrorMessaging);
		addOpType(ctx, resultType);
	}

	/**
	 * Adds a number operation type with the specified max fractional digits and the information, if
	 * a multiplicative constant is involved.
	 *
	 * @param ctx
	 * 		the context of the operation
	 * @param maxFractionalDigits
	 * 		the max fractional digits for the current operation
	 * @param multiplicativeConstant
	 * 		should be <code>true</code>, if a multiplicative constant is involved in the
	 * 		operation, else <code>false</code>.
	 * @see OperationResultType#hasMultiplicativeConstant() for information on the constainsConstant
	 * 		parameter.
	 */
	public void addNumberOpType(RuleContext ctx, Integer maxFractionalDigits, boolean multiplicativeConstant) {
		// The specific number type cannot always be extracted
		addOpType(ctx, OperationResultType.createNumberType(null, maxFractionalDigits, multiplicativeConstant));
	}

	public void addStringOpType(RuleContext ctx) {
		// The specific string type cannot always be extracted
		addOpType(ctx, new OperationResultType(OperationResultBaseType.STRING, null));
	}

	public void addOpTypeForFields(FeldWertOpContext ctx, IField f) {
		fullParser.getOperationResultTypeMap().addOpTypeForField(ctx, f.getType());
	}

	protected Integer computeUnionOfFractionalDigits(List<Integer> fractionals) {
		return fractionals.stream()
				// We cannot transform this to the reduce method, because there null is not allowed as result
				.collect(Collectors.reducing((a, b) -> a == null || b == null ? null : Math.max(a, b)))
				.orElse(null);
	}

	protected Integer getMaxFractionalDigits(IField field) {
		INumberType type = (INumberType) field.getType();
		int maxFractionalDigits = type.getMaxFractionalDigits();
		if (maxFractionalDigits == -1) {
			return null;
		}
		return maxFractionalDigits;
	}

	protected Integer getMaxFractionalDigits(OperationResultType t) {
		return switch (t.getBaseType()) {
			case NUMBER -> t.getMaxFractionalDigits();
			case BASE_YEAR -> 0;
			default -> throw new IllegalArgumentException("Wrong operation type " + t.getBaseType() + " to treat as number!");
		};
	}

	protected boolean hasMultiplicativeConstant(OperationResultType t) {
		return switch (t.getBaseType()) {
			case NUMBER -> t.hasMultiplicativeConstant();
			case BASE_YEAR -> false;
			default -> throw new IllegalArgumentException("Wrong operation type " + t.getBaseType() + " to treat as number!");
		};
	}

	protected Integer computeAdditionOfFractionalDigits(List<Integer> fractionals) {
		return fractionals.stream()
				// We cannot transform this to the reduce method, because there null is not allowed as result
				.collect(Collectors.reducing((a, b) -> a == null || b == null ? null : a + b))
				.orElse(null);
	}

}
