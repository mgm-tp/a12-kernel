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

import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldWertOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.FeldIdCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.FieldIdOperationImpl;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.FieldIdOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.OperationType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IDateRangeType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IDateType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IFieldType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.INumberType;

/**
 * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>
 * feldWertOp: '[' entitySpec ']'
 * </code>
 * das richtige Parsebaum-Objekt.
 * <p>
 * Wird auch in Datums-Operation verwendet. Daher werden zwei verschiedenen
 * Create-Methoden angeboten.
 */
public class FeldWertOpCreator {

	private FeldWertOpCreator() {
		throw new IllegalStateException("Utility class");
	}

	public static void create(FeldWertOpContext ctx, ParseTreeCreator tc) {
		createFeldWertOpFromEntityCtx(ctx.entitySpec(), tc);
	}

	public static void createFeldWertOpFromEntityCtx(EntitySpecContext ctx, ParseTreeCreator tc) {
		IEntityId feldId = FeldIdCreator.create(ctx, tc);
		IFieldType fieldType = ((IField) tc.getParser().getEntityRef(ctx).getEntity()).getType();

		FieldIdOperationImpl result = new FieldIdOperationImpl(FieldIdOperationName.FieldValue, feldId,
				getFeldOperationArt(fieldType));

		//in case of a date or date range: set the format
		if (fieldType instanceof IDateType dt) {
			result.setFormat(dt.getFormat());
		}
		if (fieldType instanceof IDateRangeType dt) {
			result.setFormat(dt.getFormatOfDate());
		}

		tc.push(result);
	}

	static OperationType getFeldOperationArt(IFieldType fieldType) {
		if (fieldType instanceof INumberType) {
			return OperationType.NUMBER;
		}
		if (fieldType instanceof IDateType) {
			return OperationType.DATE;
		}
		if (fieldType instanceof IDateRangeType) {
			return OperationType.DATE_RANGE;
		}
		/*
		 * All other types (predefined, extensible enumeration, enumeration as well as yes and yes/no) are treated like strings if
		 * they occur in a field value operation.
		 */
		return OperationType.STRING;
	}
}
