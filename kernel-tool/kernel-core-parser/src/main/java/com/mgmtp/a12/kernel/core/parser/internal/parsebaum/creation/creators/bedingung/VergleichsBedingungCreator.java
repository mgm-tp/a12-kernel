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
package com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.creators.bedingung;

import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.OrderingVglContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.PatternVglContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.ToleranceVglContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.VergleichsBedingungContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.VglOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.creators.operation.DatumOpCreatorUtil;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.CreatorUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.ComparisonConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.FieldIdOperationImpl;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IBoolConstantOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IEntityListOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IFieldIdOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.ComparisonOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.OperationType;

/**
 * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>vergleichsBedingung: operation vglOp operation; <p>
 * vglOp: <p>
 * eVgl= equalityVgl | oVgl=orderingVgl | pVgl=patternVgl | tVgl=toleranceVgl;<p>
 * </code>
 * <p>
 * das richtige Parsebaum-Objekt.
 */
public class VergleichsBedingungCreator {

	private VergleichsBedingungCreator() {
		throw new IllegalStateException("Utility class");
	}

	public static void create(VergleichsBedingungContext vergleichsBedingung, ParseTreeCreator tc) {

		ComparisonOperation op = extractVglOpFromContext(vergleichsBedingung.vglOp());
		IOperation op2 = tc.popFop();
		IOperation op1 = tc.popFop();

		//Bool-Konstanten müssen speziell behandelt werden,  weil bei der Boolkonstanten
		//der jeweilige Ja bzw. Nein-Wert des zu vergleichenden Feldes hinterlegt wird.
		setBoolConstantValueIfNecessary(op1, op2, tc);
		setBoolConstantValueIfNecessary(op2, op1, tc);

		//setDatumsKonstruktion, falls notwendig
		op1 = transformIfDateCompare(op1, op2);
		op2 = transformIfDateCompare(op2, op1);

		ComparisonConditionImpl b = new ComparisonConditionImpl(op, op1, op2);
		tc.push(b);
	}

	/**
	 * If we compare dates, we have to handle the following special situation:
	 * <ul>
	 * <li>ExtMP only: <code>BaseYear</code> specifies a date and not a number
	 * </ul>
	 * this method provides the transformation to the correct operation.
	 *
	 * @param op1
	 * 		operation to be transformed if necessary
	 * @param op2
	 * 		operation to be check if it is a date
	 * @return correct operation for op1
	 */
	private static IOperation transformIfDateCompare(IOperation op1, IOperation op2) {
		if (op2.getType() == OperationType.DATE) {
			return DatumOpCreatorUtil.transformToDateFeldOperationIfNecessary(op1);
		}

		return op1;
	}

	/**
	 * Falls in der Vergleichsbedingung eine boolsche Konstante verwendet wird,
	 * wird in der Implementierung der entsprechende Ja, bzw. NeinWert gesetzt.
	 * Precondition: Da der Parse-Baum korrekt sein muss, kann es nicht vorkommen, dass zwei
	 * Konstanten mit einander verglichen werden. Wenn eine Boolsche Konstante verglichen wird,
	 * muss der andere Parameter eine Feldwert-Operation sein.
	 *
	 * @param op1
	 * 		1. zu prüfende Feldoperation
	 * @param op2
	 * 		2. zu prüfende Feldoperation
	 * @param tc
	 */
	private static void setBoolConstantValueIfNecessary(IOperation op1, IOperation op2, ParseTreeCreator tc) {
		if (op1 instanceof IBoolConstantOperation && op2 instanceof FieldIdOperationImpl) {
			IFieldIdOperation id = (IFieldIdOperation) op2;
			CreatorUtils.setActualValueInBoolKonstanteIfNecessary(op1, id.getField(), tc);
		}

		if (op1 instanceof IBoolConstantOperation && op2 instanceof IEntityListOperation) {
			IEntityListOperation id = (IEntityListOperation) op2;
			CreatorUtils.setActualValueInBoolKonstanteIfNecessary(op1, id.getEntityList().getEntityIdList().get(0), tc);
		}

	}

	/**
	 * Konvertiert die Vergleichsoperation aus dem Grammatik-ContextObjekt in die
	 * interne vom ParseBaum verwendete Vergleichsoperation.
	 */
	private static ComparisonOperation extractVglOpFromContext(VglOpContext vglOp) {
		if (vglOp.eVgl != null) {
			return vglOp.eVgl.eq != null ? ComparisonOperation.EQ : ComparisonOperation.NE;
		}
		if (vglOp.orderingVgl() != null) {
			OrderingVglContext o = vglOp.oVgl;
			if (o.gt != null) {
				return ComparisonOperation.GT;
			}
			if (o.lt != null) {
				return ComparisonOperation.LT;
			}
			if (o.ge != null) {
				return ComparisonOperation.GE;
			}
			return ComparisonOperation.LE;
		}
		if (vglOp.pVgl != null) {
			PatternVglContext p = vglOp.pVgl;
			if (p.nm != null) {
				return ComparisonOperation.NM;
			}
			return ComparisonOperation.WM;
		}
		if (vglOp.tVgl != null) {
			ToleranceVglContext t = vglOp.tVgl;
			if (t.tol1 != null) {
				return ComparisonOperation.NE1;
			}
			if (t.tol2 != null) {
				return ComparisonOperation.NE2;
			}
			if (t.tol5 != null) {
				return ComparisonOperation.NE5;
			}
			return ComparisonOperation.NE10;
		}

		throw new IllegalArgumentException("Unknown comparison " + vglOp.getText());
	}

}
