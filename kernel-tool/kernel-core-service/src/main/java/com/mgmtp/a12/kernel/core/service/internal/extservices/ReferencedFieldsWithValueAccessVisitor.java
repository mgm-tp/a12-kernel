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

package com.mgmtp.a12.kernel.core.service.internal.extservices;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.ExpandMode;
import com.mgmtp.a12.kernel.core.parser.internal.util.AnalyseService;
import com.mgmtp.a12.kernel.core.parser.internal.util.ReferencedIdsUtil;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListAndValueListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IValueListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IEntityListOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IEntityListOperationWithValue;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IFieldIdOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IFieldIdOperationWithParameters;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateRangeConstructionOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.EntityListConditionName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.EntityListOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.FieldIdOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.ast.ASTVisitor;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;

/**
 * Visitor for extracting those fields from a condition, for which their value is checked. Can be
 * used to determine the fields that cannot be set to a default value for on the fly testing.
 */
public class ReferencedFieldsWithValueAccessVisitor extends ASTVisitor {

	private List<IEntityId> fields = new ArrayList<>();
	private final IEC ec;

	private ReferencedFieldsWithValueAccessVisitor(IEC ec) {
		this.ec = ec;
	}

	/**
	 * Determine all fields, whose values are accessed in the condition. E.g. in a condition '[f1]
	 * == "test"' the value of the field f1 is accessed, whereas in "FieldNotFilled(f1)" only the
	 * presence of the field f1 is checked and not its value.
	 *
	 * @param condition
	 * 		the parsed condition to be checked, the condition has to be expanded, i.e. it has
	 * 		to be parsed with {@link ExpandMode#EXPAND}
	 * @return all fields whose values are accessed in the condition, or an empty list if there is
	 * 		no such field
	 */
	public static List<IEntityId> getReferencedFieldsWithValueAccess(ICondition condition, IEC ec) {
		ReferencedFieldsWithValueAccessVisitor visitor = new ReferencedFieldsWithValueAccessVisitor(ec);
		condition.traverse(visitor);
		return new ArrayList<>(visitor.fields);
	}

	@Override
	public void endVisit(IValueListCondition cond) {
		fields.add(cond.getField());
	}

	@Override
	public void endVisit(IFieldIdOperationWithParameters op) {
		fields.add(op.getField());
	}

	@Override
	public void endVisit(IFieldIdOperation op) {
		if (checksFieldValue(op.getOperatorName())) {
			fields.add(op.getField());
		}
	}

	private boolean checksFieldValue(FieldIdOperationName name) {
		switch (name) {
			case FieldValue:
			case Length:
			case AbsValue:
			case FieldValueAsNumber:
			case FieldValueAsString:
				// the following are already treated as IFieldIdOperationWithParameters, but to be safe, list them anyway:
			case RoundAccountingValue:
			case RoundUpValue:
			case RoundDownValue:
			case RangeAsNumber:
			case RangeAsString:
			case ValueAsDate:
				return true;
			default:
				return false;
		}
	}

	@Override
	public void endVisit(IEntityListAndValueListCondition cond) {
		fields.addAll(cond.getEntityList().getEntityIdList());
		fields.addAll(cond.getValueListAsEntityList().getEntityIdList());
		fields.addAll(cond.getValueListAsMixedList().stream()
				.filter(o -> o instanceof IEntityId)
				.map(IEntityId.class::cast)
				.collect(Collectors.toList()));
	}

	@Override
	public void endVisit(IEntityListCondition cond) {
		if (checksFieldValues(cond.getConditionName())) {
			fields.addAll(cond.getEntityList().getEntityIdList());
		}
	}

	private boolean checksFieldValues(EntityListConditionName name) {
		switch (name) {
			case FieldValuesNotUnique:
			case DateRangesOverlap:
			case ZeileNichtEindeutig:
			case RepetitionNotUnique:
				return true;
			default:
				return false;
		}
	}

	@Override
	public void endVisit(IEntityListOperation op) {
		if (checksFieldValues(op.getOperatorName())) {
			fields.addAll(op.getEntityList().getEntityIdList());
		}
	}

	private boolean checksFieldValues(EntityListOperationName name) {
		switch (name) {
			case Sum:
			case SumOfProducts:
			case MaxValue:
			case MinValue:
			case FirstFilledValue:
			case NumberOfDifferentValues:
				// the following is already treated as IEntityListOperationWithValue, but to be safe, list it anyway:
			case NumberOfValueInFields:
				return true;
			default:
				return false;
		}
	}

	@Override
	public void endVisit(IEntityListOperationWithValue op) {
		fields.addAll(op.getEntityList().getEntityIdList());
	}

	@Override public void endVisit(IDateRangeConstructionOperation p) {
		fields.add(p.getDate1Id());
		fields.add(p.getDate2Id());
	}

	@Override
	public boolean visit(IEntityId id) {
		if (AnalyseService.isParallelIteration(id, false) || !id.getSemanticIndex().isEmpty()) {
			fields.add(ReferencedIdsUtil.createUniqueIndexFieldEntityId(id, ec));
		}
		if (!id.getSemanticIndex().isEmpty() && id.getSemanticIndex().get(0) instanceof IEntityId) {
			IEntityId semanticIndex = (IEntityId) id.getSemanticIndex().get(0);
			fields.add(semanticIndex);
		}
		if (id.getFilterCondition() != null) {
			id.getFilterCondition().traverse(this);
		}
		return true;
	}
}
