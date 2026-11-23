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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.ExpandMode;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.creators.bedingung.KontextListenBedingungCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.FeldIdCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.EntityIdImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.EntityIdListImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.ConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.EntityListAndValueListConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.EntityListConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.EntityListOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.EntityListOperationWithValueImpl;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityIdList;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IParseTreeEntity;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListAndValueListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IEntityListOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IEntityListOperationWithValue;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.EntityListConditionName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.EntityListOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.LogOp;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.utils.FindAnyAst;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IGroup;

/**
 * Visitor for expanding {@link IEntityId}s of groups in a {@link ICondition} to {@link IEntityId}s
 * of the individual fields and replacing group conditions with field conditions.
 * <p>
 * Depending on the {@link #expandMode} the groups are also added to the field lists.
 */
public class ExpandService extends ParseTreeCopier {

	// only for better readability
	private static final EntityListConditionName MIND_EIN_FELD = EntityListConditionName.AtLeastOneFieldFilled;
	private static final EntityListConditionName KEIN_FELD = EntityListConditionName.NoFieldFilled;

	private final IEC ec;
	private final ExpandMode expandMode;

	/**
	 * Creates a deep copy of <code>condition</code>, where group references and group language
	 * constructs are expanded according to the given expandMode.
	 *
	 * @param condition
	 * 		the condition, which should have been parsed with {@link ExpandMode#NO_EXPAND}.
	 * @param ec
	 * 		the IEC
	 */
	public static ICondition expandBedingung(ICondition condition, IEC ec, ExpandMode expandMode) {
		ExpandService expandService = new ExpandService(ec, expandMode);
		condition.traverse(expandService);
		ICondition result = expandService.getBedingungResult();
		if (result instanceof ConditionImpl && condition instanceof ConditionImpl) {
			((ConditionImpl) result).setSuppressedWarnings(((ConditionImpl) condition).getSuppressedWarnings());
		}
		return result;
	}

	/**
	 * Creates a deep copy of <code>operation</code>, where group references (excluding root groups)
	 * are replaced by all contained fields and language elements for groups are replaced by
	 * language elements for fields. The result is the same as if the condition was parsed with
	 * {@link ExpandMode#EXPAND}.
	 * <p>
	 * The condition should have been parsed with {@link ExpandMode#NO_EXPAND}.
	 */
	public static IOperation expandOperation(IOperation operation, IEC ec, ExpandMode expandMode) {
		ExpandService expandService = new ExpandService(ec, expandMode);
		operation.traverse(expandService);
		return expandService.getOperationResult();
	}

	private ExpandService(IEC ec, ExpandMode expandMode) {
		this.ec = ec;
		this.expandMode = expandMode;
	}

	@Override
	public void endVisit(IEntityListCondition b) {
		IEntityIdList entityList = entityListStack.pop();
		ICondition expandedCondition = expandAndCreate(b, entityList);

		bedingungsStack.push(expandedCondition);
	}

	@Override
	public void endVisit(IEntityListAndValueListCondition b) {
		if (!b.getValueListAsMixedList().isEmpty()) {
			// Mixed lists can only contain fields -> no expansion necessary
			super.endVisit(b);
			return;
		}
		List<String> wertListeAlsStringListe = new ArrayList<>(b.getValueListAsStringList());
		IEntityIdList wertListeAlsFeldListe = null;
		if (b.getValueListAsStringList().isEmpty()) {
			wertListeAlsFeldListe = expandEntityIds(entityListStack.pop(), false, false);
		}
		IEntityIdList feldListe = expandEntityIds(entityListStack.pop(), false, false);

		bedingungsStack.push(new EntityListAndValueListConditionImpl(b, feldListe, wertListeAlsFeldListe,
				wertListeAlsStringListe, null));
	}

	@Override
	public void endVisit(IEntityListOperation op) {
		// for 'AnzahlAngegebenerKontexte' a special list is created where each group is followed by its contained fields
		boolean createSpecialList = (op.getOperatorName() == EntityListOperationName.NumberOfFilledGroups);
		// We also add groups in the case of NumberOfFilledGroups
		IEntityIdList entityList = expandEntityIds(entityListStack.pop(), createSpecialList, createSpecialList);

		opStack.push(new EntityListOperationImpl(op, entityList));
	}

	@Override
	public void endVisit(IEntityListOperationWithValue op) {
		IEntityIdList entityList = expandEntityIds(entityListStack.pop(), false, false);
		IOperation konstante = opStack.pop();

		opStack.push(new EntityListOperationWithValueImpl(op, entityList, konstante));
	}

	/**
	 * Should be functionally identical to {@link KontextListenBedingungCreator#expandAndCreate} for
	 * the used expansion mode.
	 */
	private ICondition expandAndCreate(IEntityListCondition b, IEntityIdList entityList) {
		EntityListConditionName bedingungName = b.getConditionName();
		switch (bedingungName) {
			case AtLeastOneGroupFilled: {
				return createFeldListenBedingung(entityList, MIND_EIN_FELD, true, null);
			}
			case AllGroupsFilled: {
				List<ICondition> bedingungen = createBedingungen(entityList, MIND_EIN_FELD, true);
				return KontextListenBedingungCreator.createBedingung(bedingungen, LogOp.And);
			}
			case NoGroupFilled: {
				return createFeldListenBedingung(entityList, KEIN_FELD, true, null);
			}
			case NotAllGroupsFilled: {
				List<ICondition> bedingungen = createBedingungen(entityList, KEIN_FELD, true);
				return KontextListenBedingungCreator.createBedingung(bedingungen, LogOp.Or);
			}
			case GroupsNotCollectivelyFilled: {
				ICondition bed1 = createFeldListenBedingung(entityList, MIND_EIN_FELD, true, null);
				IEntityIdList entityListCopy = ParseTreeCopier.copyEntityIdList(entityList);
				List<ICondition> bedingungen = createBedingungen(entityListCopy, KEIN_FELD, true);
				ICondition bed2 = KontextListenBedingungCreator.createBedingung(bedingungen, LogOp.Or);
				return KontextListenBedingungCreator.createBedingung(Arrays.asList(bed1, bed2), LogOp.And);
			}
			default: {
				return createFeldListenBedingung(entityList, bedingungName, false, b.getAdditionalGroupReference().orElse(null));
			}
		}
	}

	/**
	 * Should be functionally identical to
	 * {@link KontextListenBedingungCreator#createFeldListenBedingung}.
	 */
	private ICondition createFeldListenBedingung(IEntityIdList entityList, EntityListConditionName bedingungName,
			boolean addGroups, IEntityId additionalGroupId) {
		IEntityIdList fieldList = expandEntityIds(entityList, false, addGroups);
		return new EntityListConditionImpl(bedingungName, fieldList, additionalGroupId);
	}

	/**
	 * Should be functionally identical to {@link KontextListenBedingungCreator#createBedingungen}.
	 */
	private List<ICondition> createBedingungen(IEntityIdList entityIdList, EntityListConditionName bedingungName,
			boolean addGroup) {
		return entityIdList.getEntityIdList().stream()
				.map(id -> new EntityListConditionImpl(bedingungName, new EntityIdListImpl(expandEntityId(id, addGroup)), null))
				.collect(Collectors.toList());
	}

	/**
	 * @param entityList
	 * @param createSpecialList
	 * 		create a special list where each group is followed by its contained fields
	 * @return
	 */
	private IEntityIdList expandEntityIds(IEntityIdList entityList, boolean createSpecialList, boolean addGroups) {
		List<IEntityId> result = new ArrayList<>();
		for (IEntityId id : entityList.getEntityIdList()) {
			if (createSpecialList && ExpandMode.EXPAND_PARTIALLY != expandMode) {
				result.add(id);
			}
			result.addAll(expandEntityId(id, addGroups));
		}
		return new EntityIdListImpl(result);
	}

	private List<IEntityId> expandEntityId(IEntityId id, boolean addGroup) {
		ICondition expandedFilterCondition = null;
		if (id.getFilterCondition() != null) {
			expandedFilterCondition = ExpandService.expandBedingung(id.getFilterCondition(), ec, expandMode);
		}
		if (id.isField()) {
			if (id.getFilterCondition() == null) {
				return Collections.singletonList(id);
			} else {
				return Collections.singletonList(new EntityIdImpl(id, id.getSemanticIndex(), expandedFilterCondition));
			}
		}
		List<? extends IEntity> entities;
		switch (expandMode) {
			case EXPAND_AND_ADD_GROUPS:
				if (addGroup) {
					// currently, groups are not allowed to specify a filter condition, so we ignore it.
					List<IEntity> tmpEntities = EntityUtils.getAllContainedEntities(id.getFullName(), ec);
					tmpEntities.add(0, ec.get(id.getFullName()));
					entities = tmpEntities;
					break;
				}
				// fall through if groups are not to be added.
			case EXPAND:
				// currently, groups are not allowed to specify a filter condition, so we ignore it
				entities = ec.getAllContainedFields(id.getFullName());
				break;
			case EXPAND_PARTIALLY:
				if (addGroup) {
					return Collections.singletonList(new EntityIdImpl(id, id.getSemanticIndex(), expandedFilterCondition));
				} else {
					entities = ec.getAllContainedFields(id.getFullName());
				}
				break;
			default:
				return Collections.singletonList(new EntityIdImpl(id, id.getSemanticIndex(), expandedFilterCondition));

		}
		return entities.stream()
				.map(e -> createEntityId(id, e))
				.collect(Collectors.toList());
	}

	private IEntityId createEntityId(IEntityId containingGroup, IEntity entity) {
		List<Long> indices = Arrays.stream(containingGroup.getIndices())
				.boxed()
				.collect(Collectors.toList());
		if (entity.isField()) {
			return FeldIdCreator.createFeldId((IField) entity, indices, containingGroup.getUsb(),
					containingGroup.getSemanticIndex(), containingGroup.isHmodel(), containingGroup.isOuter(),
					containingGroup.getPathNotationConfig());
		} else {
			return FeldIdCreator.createGroupId((IGroup) entity, indices, containingGroup.getUsb(),
					containingGroup.getSemanticIndex(), containingGroup.isHmodel(), ec, containingGroup.isOuter(),
					containingGroup.getPathNotationConfig());
		}
	}

	public static boolean isExpandableEntity(IParseTreeEntity pte) {
		if (pte instanceof IEntityId && !((IEntityId) pte).isField()) {
			return true;
		}
		if (pte instanceof IEntityListCondition) {
			switch (((IEntityListCondition) pte).getConditionName()) {
				case AllGroupsFilled:
				case NoGroupFilled:
				case GroupsNotCollectivelyFilled:
				case AtLeastOneGroupFilled:
				case NotAllGroupsFilled:
					return true;
				default:
					return false;
			}
		}
		return false;
	}

	public static boolean containsExpandableEntity(IParseTreeEntity tree) {
		return FindAnyAst.findeInParseBaumEntity(tree, ExpandService::isExpandableEntity, true).isPresent();
	}

}
