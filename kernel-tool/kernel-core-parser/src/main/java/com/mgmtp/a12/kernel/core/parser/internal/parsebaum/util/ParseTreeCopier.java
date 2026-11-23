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

package com.mgmtp.a12.kernel.core.parser.internal.parsebaum.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.EntityIdImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.EntityIdListImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.ApplicationConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.ComparisonConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.CompositeConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.ConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.DateValidityConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.EntityListAndValueListConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.EntityListConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.PredefinedTypeValidityConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.RootGroupConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.ValueListConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.BoolConstantOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.CompositeOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.ConstantOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.EntityListOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.EntityListOperationWithValueImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.FieldIdOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.FieldIdOperationWithParametersImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.GroupOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.NumberOfRootGroupsOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.NumberOfUsbOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.OpListOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.OpListOperationWithParameterImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.PointInTimeOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.datum.DateAddOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.datum.DateConstructionOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.datum.DateDiffOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.datum.DateExtractionOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.datum.DateRangeConstructionOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.datum.DateRangeExtractionOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.datum.DateTimeConstructionOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.datum.TimeConstructionOperationImpl;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityIdList;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IApplicationCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IComparisonCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICompositeCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IDateValidityCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListAndValueListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IPredefinedTypeValidityCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IRootGroupCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IValueListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IBoolConstantOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.ICompositeOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IConstantOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IEntityListOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IEntityListOperationWithValue;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IFieldIdOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IFieldIdOperationWithParameters;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IGroupOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.INumberOfRootGroupsOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.INumberOfUsbOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOpListOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOpListOperationWithParameter;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IPointInTimeOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateAddOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateConstructionOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateDiffOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateExtractionOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateRangeConstructionOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateRangeExtractionOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateTimeConstructionOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.ITimeConstructionOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.ast.ASTVisitor;

/**
 * Visitor for creating a deep copy of a parse tree.
 */
public class ParseTreeCopier extends ASTVisitor {

	Deque<ICondition> bedingungsStack = new ArrayDeque<>();
	Deque<IOperation> opStack = new ArrayDeque<>();
	Deque<IEntityIdList> entityListStack = new ArrayDeque<>();
	Deque<IEntityId> entityStack = new ArrayDeque<>();

	/**
	 * Creates a deep copy of <code>condition</code>.
	 */
	public static ICondition copyBedingung(ICondition condition) {
		ParseTreeCopier copier = new ParseTreeCopier();
		condition.traverse(copier);
		ICondition result = copier.getBedingungResult();
		if (result instanceof ConditionImpl resultCondition && condition instanceof ConditionImpl condImpl) {
			resultCondition.setSuppressedWarnings(condImpl.getSuppressedWarnings());
		}
		return result;
	}

	/**
	 * Creates a deep copy of <code>operation</code>.
	 */
	public static IOperation copyOperation(IOperation operation) {
		ParseTreeCopier copier = new ParseTreeCopier();
		operation.traverse(copier);
		return copier.getOperationResult();
	}

	/**
	 * Creates a deep copy of <code>entityIdList</code>.
	 */
	public static IEntityIdList copyEntityIdList(IEntityIdList entityIdList) {
		ParseTreeCopier copier = new ParseTreeCopier();
		entityIdList.traverse(copier);
		return copier.getEntityIdListResult();
	}

	protected Deque<ICondition> getBedingungsStack() {
		return bedingungsStack;
	}

	/**
	 * Nach der Traversierung ist das Resultat als letzte Bedingung vorhanden.
	 * Wenn alles ohne Fehler bearbeitet wurde, sind opStack, entityListStack, und entityStack leer
	 * und der Bedingung-Stack hat genau das Resultat Objekt.
	 *
	 * @return gib das Resultat der Traversierung zurück.
	 */
	public ICondition getBedingungResult() {
		assert bedingungsStack.size() == 1;
		assert opStack.isEmpty();
		assert entityListStack.isEmpty();
		assert entityStack.isEmpty();

		return bedingungsStack.pop();
	}

	IOperation getOperationResult() {
		assert bedingungsStack.isEmpty();
		assert opStack.size() == 1;
		assert entityListStack.isEmpty();
		assert entityStack.isEmpty();

		return opStack.pop();
	}

	private IEntityIdList getEntityIdListResult() {
		assert bedingungsStack.isEmpty();
		assert opStack.isEmpty();
		assert entityListStack.size() == 1;
		assert entityStack.isEmpty();

		return entityListStack.pop();
	}

	@Override
	public void endVisit(IApplicationCondition b) {
		bedingungsStack.push(new ApplicationConditionImpl(b));
	}

	@Override
	public void endVisit(IEntityListCondition b) {
		IEntityIdList entityList = entityListStack.pop();

		Optional<IEntityId> additionalGroupReference = b.getAdditionalGroupReference();
		assert additionalGroupReference.isEmpty() ||
				(additionalGroupReference.get().getFilterCondition() == null &&
						additionalGroupReference.get().getSemanticIndex().isEmpty());
		IEntityId copiedAdditionalGroup = additionalGroupReference
				.map(e -> new EntityIdImpl(e, List.of(), null))
				.orElse(null);
		bedingungsStack.push(new EntityListConditionImpl(b, entityList, copiedAdditionalGroup));
	}

	@Override
	public void endVisit(IEntityListAndValueListCondition b) {
		List<String> wertListeAlsStringListe = new ArrayList<>(b.getValueListAsStringList());
		IEntityIdList wertListeAlsFeldListe = new EntityIdListImpl(Collections.emptyList());
		List<Object> mixedList = Collections.emptyList();
		if (b.getValueListAsStringList().isEmpty() && b.getValueListAsMixedList().isEmpty()) {
			wertListeAlsFeldListe = entityListStack.pop();
		} else if (!b.getValueListAsMixedList().isEmpty()) {
			// Currently expanding this would not be necessary, because all entities are fields.
			mixedList = new ArrayList<>(b.getValueListAsMixedList());
			Collections.reverse(mixedList);
			mixedList = mixedList.stream()
					.map(this::popMixedValue)
					.collect(Collectors.toList());
			Collections.reverse(mixedList);
		}

		IEntityIdList feldListe = entityListStack.pop();

		bedingungsStack.push(new EntityListAndValueListConditionImpl(b,
				feldListe, wertListeAlsFeldListe, wertListeAlsStringListe, mixedList));
	}

	private Object popMixedValue(Object value) {
		if (value instanceof String) {
			return value;
		} else if (value instanceof IEntityId) {
			return entityStack.pop();
		} else {
			throw new IllegalArgumentException(
					"Mixed value list with a value " + value + " of class " + (value == null ? "null" : value.getClass()));
		}
	}

	/**
	 * Hole die rechte und die linke Seite der Vergleichsbedingung vom
	 * Stack und setze die Objekte im Vergleichs-Objekt.
	 */
	@Override
	public void endVisit(IComparisonCondition b) {
		IOperation op2 = opStack.pop();
		IOperation op1 = opStack.pop();

		bedingungsStack.push(new ComparisonConditionImpl(b, op1, op2));
	}

	@Override
	public void endVisit(IRootGroupCondition b) {
		IEntityId entityId = null;
		if (b.getEntityId() != null) {
			entityId = entityStack.pop();
		}
		bedingungsStack.push(new RootGroupConditionImpl(b, entityId));
	}

	@Override
	public void endVisit(IDateValidityCondition b) {
		IOperation date = opStack.pop();
		bedingungsStack.push(new DateValidityConditionImpl(b, date));
	}

	@Override
	public void endVisit(IPredefinedTypeValidityCondition b) {
		IEntityId field = entityStack.pop();
		String pdConst = b.getPredefinedTypeConstant();
		bedingungsStack.push(new PredefinedTypeValidityConditionImpl(field, b, pdConst));
	}

	@Override
	public void endVisit(INumberOfUsbOperation op) {
		opStack.push(new NumberOfUsbOperationImpl(op));
	}

	@Override
	public void endVisit(IDateRangeExtractionOperation b) {
		IOperation da = opStack.pop();

		opStack.push(new DateRangeExtractionOperationImpl(b, da));
	}

	@Override
	public void endVisit(IEntityListOperation op) {
		IEntityIdList entityList = entityListStack.pop();

		opStack.push(new EntityListOperationImpl(op, entityList));
	}

	@Override
	public void endVisit(IFieldIdOperation op) {
		IEntityId id = entityStack.pop();

		opStack.push(new FieldIdOperationImpl(op, id));
	}

	@Override
	public void endVisit(IGroupOperation p) {
		IEntityId id = entityStack.pop();

		opStack.push(new GroupOperationImpl(p, id));
	}

	@Override
	public void endVisit(IEntityListOperationWithValue op) {
		IEntityIdList entityList = entityListStack.pop();
		IOperation konstante = opStack.pop();

		opStack.push(new EntityListOperationWithValueImpl(op, entityList, konstante));
	}

	@Override
	public void endVisit(IFieldIdOperationWithParameters op) {
		IEntityId id = entityStack.pop();

		opStack.push(new FieldIdOperationWithParametersImpl(op, id));
	}

	@Override
	public void endVisit(IOpListOperation op) {
		List<IOperation> ops = new ArrayList<>();
		for (int i = 0; i < op.getOperations().size(); i++) {
			ops.add(0, opStack.pop()); // erhalte die Reihenfolge
		}
		opStack.push(new OpListOperationImpl(op, ops));
	}

	@Override
	public void endVisit(IOpListOperationWithParameter op) {
		List<IOperation> ops = new ArrayList<>();
		for (int i = 0; i < op.getOperations().size(); i++) {
			ops.add(0, opStack.pop()); // erhalte die Reihenfolge
		}

		opStack.push(new OpListOperationWithParameterImpl(op, ops));
	}

	@Override
	public void endVisit(IPointInTimeOperation op) {
		opStack.push(new PointInTimeOperationImpl(op));
	}

	/**
	 * Hole die Parameter für die DatumAngabe-Konstrukt in der richtigen Reihenfolge vom
	 * Stack und setze die Objekte in das neue DatumAngabeKonstrukt.
	 */
	@Override
	public void endVisit(IDateConstructionOperation op) {
		IOperation jahr2 = (op.getYear2() != null) ? opStack.pop() : null;
		IOperation jahr1 = (op.getYear1() != null) ? opStack.pop() : null;
		IOperation monat = opStack.pop();
		IOperation tag = opStack.pop();

		opStack.push(new DateConstructionOperationImpl(op, tag, monat, jahr1, jahr2));
	}

	@Override
	public void endVisit(IDateTimeConstructionOperation op) {
		IOperation time = opStack.pop();
		IOperation date = opStack.pop();

		opStack.push(new DateTimeConstructionOperationImpl(op, date, time));
	}

	@Override
	public void endVisit(ITimeConstructionOperation op) {
		IOperation second = (op.getSecond() != null) ? opStack.pop() : null;
		IOperation minute = (op.getMinute() != null) ? opStack.pop() : null;
		IOperation hour = (op.getHour() != null) ? opStack.pop() : null;

		opStack.push(new TimeConstructionOperationImpl(op, hour, minute, second));
	}

	@Override
	public void endVisit(IDateRangeConstructionOperation op) {
		IEntityId date2Id = entityStack.pop();
		IEntityId date1Id = entityStack.pop();

		opStack.push(new DateRangeConstructionOperationImpl(date1Id, date2Id, op.getFormat()));
	}

	@Override
	public void endVisit(IConstantOperation op) {
		opStack.push(new ConstantOperationImpl(op));
	}

	@Override
	public void endVisit(IDateExtractionOperation op) {
		IOperation da = opStack.pop();

		opStack.push(new DateExtractionOperationImpl(op, da));
	}

	@Override
	public void endVisit(IDateAddOperation op) {
		IOperation amountSpec = opStack.pop();
		IOperation dateSpec = opStack.pop();

		opStack.push(new DateAddOperationImpl(op, dateSpec, amountSpec));
	}

	@Override
	public void endVisit(IDateDiffOperation op) {
		IOperation datumAngabe2 = opStack.pop();
		IOperation datumAngabe1 = opStack.pop();

		opStack.push(new DateDiffOperationImpl(op, datumAngabe1, datumAngabe2));
	}

	@Override
	public void endVisit(ICompositeOperation op) {
		List<IOperation> ops = new ArrayList<>();
		for (int i = 0; i < op.getOperations().size(); i++) {
			ops.add(0, opStack.pop()); // gewährleiste die Ursprungsreihenfolge
		}

		opStack.push(new CompositeOperationImpl(ops, op));
	}

	@Override
	public void endVisit(IValueListCondition b) {
		IEntityId entityId = entityStack.pop();
		List<IOperation> ops = new ArrayList<>();
		for (int i = 0; i < b.getValueList().size(); i++) {
			ops.add(0, opStack.pop()); //erhalte Reihenfolge
		}

		bedingungsStack.push(new ValueListConditionImpl(b, entityId, ops));
	}

	@Override
	public void endVisit(INumberOfRootGroupsOperation b) {
		opStack.push(new NumberOfRootGroupsOperationImpl(b));
	}

	@Override
	public void endVisit(IBoolConstantOperation b) {
		opStack.push(new BoolConstantOperationImpl(b));
	}

	@Override
	public void endVisit(ICompositeCondition b) {
		List<ICondition> bedingungen = new ArrayList<>();
		for (int i = 0; i < b.getConditionList().size(); i++) {
			bedingungen.add(0, bedingungsStack.pop()); // erhalte die Reihenfolge
		}

		bedingungsStack.push(new CompositeConditionImpl(b, bedingungen));
	}

	@Override
	public void endVisit(IEntityId id) {
		ICondition filterCondition = null;
		if (id.getFilterCondition() != null) {
			filterCondition = ParseTreeCopier.copyBedingung(id.getFilterCondition());
		}
		entityStack.push(cloneEntityIdWithFilterCondition(id, filterCondition));
	}

	protected EntityIdImpl cloneEntityIdWithFilterCondition(IEntityId id, ICondition filterCondition) {
		if (id.getSemanticIndex().isEmpty()) {
			return new EntityIdImpl(id, Collections.emptyList(), filterCondition);
		}
		List<Object> semIdxs = id.getSemanticIndex();
		List<Object> copiedSemIdxs = new ArrayList<>(semIdxs);
		// We iterate in reverse order because the semantic indexes are popped in reverse order from entityStack.
		for (int i = semIdxs.size() - 1; i >= 0; i--) {
			Object semIdx = semIdxs.get(i);
			if (semIdx instanceof IEntityId) {
				copiedSemIdxs.set(i, entityStack.pop());
			}
		}
		return new EntityIdImpl(id, copiedSemIdxs, filterCondition);
	}

	@Override
	public void endVisit(IEntityIdList l) {
		List<IEntityId> ids = new ArrayList<>();
		for (int i = 0; i < l.getEntityIdList().size(); i++) {
			ids.add(0, entityStack.pop()); // erhalte die Reihenfolge
		}

		entityListStack.push(new EntityIdListImpl(ids));
	}

}
