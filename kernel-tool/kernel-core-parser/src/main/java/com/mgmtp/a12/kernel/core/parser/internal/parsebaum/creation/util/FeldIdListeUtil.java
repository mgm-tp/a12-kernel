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
package com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntityListeContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.Lenient_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.ExpandMode;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.EntityIdImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.EntityIdListImpl;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;

/**
 * The class <code>FeldIdListeUtil</code> provides helper methods to create {@link EntityIdListImpl}
 * -objects from grammar-context-objects.
 */
public class FeldIdListeUtil {

	private FeldIdListeUtil() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Create a list of entities from an {@link EntityListeContext}. All the individual entities in
	 * the {@link EntityListeContext} are expanded according to the given {@link ExpandMode}.
	 *
	 * @param elCtx
	 * 		the context containing the base entity list.
	 * @param parser
	 * 		the parser containing the
	 * @param expandMode
	 * 		specifies, how the expansion should be done.
	 * @return an {@link EntityIdListImpl}, usable inside of a condition element.
	 */
	public static EntityIdListImpl createEntityIdListeFromContext(EntityListeContext elCtx, Lenient_ConditionParser parser,
			ExpandMode expandMode) {
		List<IEntityId> entityIds = elCtx.entitySpec().stream()
				.flatMap(ectx -> FeldIdCreator.createFeldIdListe(ectx, parser, expandMode).stream())
				.collect(Collectors.toList());
		return new EntityIdListImpl(entityIds);
	}

	/**
	 * Create a list of entities from an {@link EntitySpecContext}. All the entity in
	 * the {@link EntitySpecContext} are expanded according to the given {@link ExpandMode}.
	 *
	 * @param eCtx
	 * 		the context containing the specified entity.
	 * @param parser
	 * 		the parser containing necessary parameters.
	 * @param expandMode
	 * 		specifies the expansion that should be applied.
	 * @return a list of the eventually expanded field-/group-entities
	 */
	public static EntityIdListImpl createFeldIdListeFromContext(EntitySpecContext eCtx, Lenient_ConditionParser parser,
			ExpandMode expandMode) {
		List<IEntityId> entityIds = FeldIdCreator.createFeldIdListe(eCtx, parser, expandMode);
		return new EntityIdListImpl(entityIds);
	}

	/**
	 * expand with special semantic for <code>AnzahlAngegebeneKontexte</code>
	 * expand to FeldId for the group, followed by feldIds for all contained fields.
	 * <p>
	 * The method is only called,
	 *
	 * @param entityListe
	 * @param tc
	 * @return list of FeldIds
	 */
	public static EntityIdListImpl createExpFeldIdListeForAnzAngKontexte(EntityListeContext entityListe, ParseTreeCreator tc) {
		List<IEntityId> felder = new ArrayList<>();
		for (EntitySpecContext eCtx : entityListe.entitySpec()) {
			EntityRef ref = tc.getParser().getEntityRef(eCtx);
			//we take always the directly referenced id
			IEntityId id = new EntityIdImpl(ref, tc.getParser().isHModel(), tc.getParser().getEc());
			felder.add(id);

			//if it is a group and expand mode is set.
			if ((!id.isField()) && tc.getExpandMode() != ExpandMode.NO_EXPAND &&
					tc.getExpandMode() != ExpandMode.EXPAND_PARTIALLY) {
				felder.addAll(FeldIdCreator.expandEntityRef(ref, tc.getParser(), eCtx, tc.getExpandMode()));
			}
		}
		return new EntityIdListImpl(felder);
	}
}
