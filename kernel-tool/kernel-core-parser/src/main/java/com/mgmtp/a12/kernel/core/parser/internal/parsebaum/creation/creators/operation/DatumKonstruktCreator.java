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

import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.datum.DateConstructionOperationImpl;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateConstructionOperation;

/**
 * Erzeugt ein Datumskonstrukt mit 2, 3 oder 4 Parametern.
 * <p>
 * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>
 * <code>
 * 'Datum' '(' dp1=datumParam ',' dp2=datumParam (',' dp3=datumParam (',' dp4 =
 * datumParam )? )?')' </code>
 * * </code>
 * <p>
 * das richtige Parsebaum-Objekt.
 * <p>
 * Parameter3 und Parameter4 sind optional, da man ein Datum
 * <ul>
 * <li>ohne Jahresangabe
 * <li>mit einem Parameter als Jahresangabe
 * <li>mit zwei Parametern als Jahresangabe (Jahrhundert, Jahrzehnt)
 * </ul>
 * modellieren kann.
 */
public class DatumKonstruktCreator {

	private DatumKonstruktCreator() {
		throw new IllegalStateException("Utility class");
	}

	public static void create(ParseTreeCreator tc) {

		// Die letzten zwei Parameter sind optional.
		// Die Parameter werden in umgekehrter Reihenfolge zurückgegeben,  weil sie auf
		// dem Stack gespichert sind. Daher ist die Semantik der Parameter erst bekann, last two parameters are optional.  But we have to take care of the fact,  that
		// wenn die Anzahl der Parameter bekannt sind.

		//Wir haben immer zwei Parameter,  d.h. hier muss nichts zusätzlich überprüft werden.
		IOperation dp1 = tc.popFop();
		IOperation dp2 = tc.popFop();

		if (noMoreParams(tc)) { // nur zwei Parameter.
			tc.popFop();
			tc.push(new DateConstructionOperationImpl(dp2, dp1, null, null));
			return;
		}

		IOperation dp3 = tc.popFop();
		if (noMoreParams(tc)) {// nur drei Parameter.
			tc.popFop();
			tc.push(new DateConstructionOperationImpl(dp3, dp2, dp1, null));
			return;
		}

		//four params
		IOperation dp4 = tc.popFop();
		tc.popFop();
		tc.push(new DateConstructionOperationImpl(dp4, dp3, dp2, dp1));
	}

	//Überprüft,  ob die Operation ein DatumAngabeKonstrukt ist.
	//Diese Objekt wurde beim Traversieren zunächst als Markierung auf den
	//Stack gelegt. siehe
	private static boolean noMoreParams(ParseTreeCreator tc) {
		return (tc.peekFop() instanceof IDateConstructionOperation);
	}

	/**
	 * an empty <code> DatumAngabeKonstrukt </code> object is pushed on the stack.
	 * This is done, to make the handling of the optional number of params easy.
	 *
	 * @param tc
	 */
	public static void addAnEmptyDatumKonstrukt(ParseTreeCreator tc) {
		tc.push(new DateConstructionOperationImpl(null, null, null, null));
	}
}
