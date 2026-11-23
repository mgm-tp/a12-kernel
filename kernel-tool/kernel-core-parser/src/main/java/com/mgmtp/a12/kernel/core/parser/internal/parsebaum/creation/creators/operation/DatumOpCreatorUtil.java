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

import com.mgmtp.a12.kernel.core.parser.internal.check.operation.CheckKonstanteImpl;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.PointInTimeOperationImpl;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IPointInTimeOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.OperationType;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.PointInTimeOperationName;

/**
 * Utility methods for date operations.
 */
public class DatumOpCreatorUtil {

	private DatumOpCreatorUtil() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * If we have operations on or compare dates, we have to handle the following special situation:
	 * <ul>
	 * <li>ExtMP only: <code>BaseYear</code> can be used as a number or as a date.
	 * Thus, it has to be transformed, if used as a date.</li>
	 * </ul>
	 * this method provides the transformation to the correct operation.
	 * <p>
	 * If BaseYear is used in a context where only dates can be used, they should already be dates by means of {@link KonstanteCreator#create(GrammarOnly_ConditionParser.KonstanteReferenzjahrContext, ParseTreeCreator)}.
	 * <b>Note:</b> <code>BaseYear</code> is the only operation with different types
	 * depending on the context of usage. For other operations the correct type is set directly
	 * during parsing. In particular, string literals that represent a date are already labeled as
	 * dates by {@link CheckKonstanteImpl#check}, and not here.
	 *
	 * @param datumAngabe
	 * 		operation to be transformed if necessary
	 * @return the date operation for the given operation
	 */
	public static IOperation transformToDateFeldOperationIfNecessary(IOperation datumAngabe) {
		if (isBaseYearOperation(datumAngabe)) {
			return new PointInTimeOperationImpl(PointInTimeOperationName.BaseYear, OperationType.DATE);
		}
		return datumAngabe;
	}

	private static boolean isBaseYearOperation(IOperation op) {
		if (op instanceof IPointInTimeOperation pointInTimeOperation) {
			return pointInTimeOperation.getOperatorName() == PointInTimeOperationName.BaseYear;
		}
		return false;
	}

}
