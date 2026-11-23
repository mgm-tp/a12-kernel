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
import java.util.Collections;
import java.util.List;

import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.Lenient_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.ExpandMode;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.EntityIdImpl;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.parser.internal.util.SemanticIndexRef;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IParseTreeEntity;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.PathNotationConfig;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IGroup;

/**
 * Klasse dient dem erzeugen einer IFeldId Implementierung.
 * Die create-Methoden werden beim Traversieren aus den verschiedenen Grammatik-Regeln
 * aus aufgerufen, daher werden verschiedenen create-Methoden angeboten.
 * <p>
 * Es wird auch eine expand-Methode angeboten, die die Kontexte in ihre Feldlisten
 * expandiert.
 */
public class FeldIdCreator {

	private FeldIdCreator() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Create an entity id from an {@link EntitySpecContext}.
	 *
	 * @param ctx
	 * 		the context to extract the information for the IEntityId from.
	 * @param tc
	 * 		the {@link ParseTreeCreator} containing the parsing and expand information.
	 * @return the generated {@link IEntityId}.
	 */
	public static IEntityId create(EntitySpecContext ctx, ParseTreeCreator tc) {
		ExpandMode expandMode = ExpandMode.limitToExpand(tc.getExpandMode());
		List<IEntityId> fieldList = FeldIdCreator.createFeldIdListe(ctx, tc.getParser(), expandMode);
		assert fieldList.size() == 1 : "The expanded entity contains more than one element!";
		return fieldList.get(0);
	}

	/**
	 * Expand an entity specified by an {@link EntitySpecContext} to a list of {@link IEntityId}s.
	 *
	 * @param ctx
	 * 		the context containing the entity.
	 * @param parser
	 * 		the parser containing necessary information.
	 * @param expandMode
	 * 		specifies, how the expansion should be node.
	 * @return the list of expanded {@link IEntityId}s.
	 */
	public static List<IEntityId> createFeldIdListe(EntitySpecContext ctx, Lenient_ConditionParser parser, ExpandMode expandMode) {
		EntityRef ref = parser.getEntityRef(ctx);

		if (expandMode == ExpandMode.NO_EXPAND || ref.getEntity().isField()) {//no need to expand
			return Collections.singletonList(createEntityId(ref, parser));
		} else {//expand the group
			return expandEntityRef(ref, parser, ctx, expandMode);
		}
	}

	/**
	 * Expand an {@link EntityRef} to a list of {@link IEntityId}s using the given
	 * {@link ExpandMode}.
	 *
	 * @param ref
	 * 		the entity reference to be expanded.
	 * @param parser
	 * 		the parser containing necessary information.
	 * @param ctx
	 * 		the context containing additional information for the entities.
	 * @param expandMode
	 * 		specifies, how the expansion should be node.
	 * @return a list of expanded {@link IEntityId}s.
	 */
	public static List<IEntityId> expandEntityRef(EntityRef ref, Lenient_ConditionParser parser, EntitySpecContext ctx,
			ExpandMode expandMode) {
		List<Object> semIdx = getSemanticIndex(ref, parser.isHModel(), parser.getEc());
		return EntityUtils.expandToEntityList(ref, parser, ctx, expandMode).stream()
				.map(f -> createEntityId(f, ref, semIdx, parser))
				.toList();
	}

	public static IEntityId createEntityId(EntityRef ref, Lenient_ConditionParser parser) {
		return new EntityIdImpl(ref, parser.isHModel(), parser.getEc());
	}

	/**
	 * Alle Felder, die indirekt durch einen Kontext referenziert wurden, werden erzeugt.
	 * Als Indices werden dazu die Indices des Kontextes verwendet und die zusätzlichen Indices für
	 * das Feld angehängt. Als zusätzliche Indices wird Wildcard (falls wiederholbar) oder 1 (falls
	 * nicht wiederholbar) gesetzt.
	 * In H-Modellen sind für den Kontext bereits beide Indices (LfdNr, Zeile) vorhanden, hier
	 * wird bei einem Zeilenstern noch zusätzlich geprüft, ob das Feld überhaupt mehrzeilig ist -
	 * falls nicht, wird für die Zeile 1 gesetzt.
	 */
	public static IEntityId createFeldId(IField f, List<Long> groupIndices, long groupUsb, List<Object> semIdx, boolean isHModel,
			boolean isOuter, PathNotationConfig pathNotationConfig) {
		long[] maxFeld = EntityUtils.getMax(f, isHModel);

		// Bis zum Kontext:  nimm diese Indices
		List<Long> indices = new ArrayList<>(groupIndices);

		if (isHModel) {
			long zeile = (indices.get(1) == IParseTreeEntity.WILDCARD && maxFeld[1] == 1L) ? 1L : indices.get(1);
			indices.set(1, zeile);
		} else {
			//hänge die zusätzlichen Indices für die Felder an.
			addIndicesBelowGroup(indices, maxFeld);
		}
		return new EntityIdImpl(f, indices, groupUsb, semIdx, isHModel, isOuter, pathNotationConfig);
	}

	/**
	 * Alle Kontexte, die indirekt durch einen Kontext referenziert wurden, werden erzeugt.
	 * Als Indices werden dazu die Indices des Kontextes verwendet und die zusätzlichen Indices für
	 * das Feld angehängt. Als zusätzliche Indices wird Wildcard (falls wiederholbar) oder 1 (falls
	 * nicht wiederholbar) gesetzt.
	 */
	public static IEntityId createGroupId(IGroup group, List<Long> groupIndices, long groupUsb, List<Object> semIdx,
			boolean isHModel, IEC ec, boolean isOuter, PathNotationConfig pathNotationConfig) {
		long[] max = group.getMax();
		List<Long> indices = new ArrayList<>(groupIndices);
		addIndicesBelowGroup(indices, max);
		return new EntityIdImpl(group, indices, groupUsb, semIdx, isHModel, ec, isOuter, pathNotationConfig);
	}

	private static IEntityId createEntityId(IEntity f, EntityRef ref, List<Object> semIdx, Lenient_ConditionParser parser) {
		if (f.isField()) {
			return createFeldId((IField) f, ref.getIndices(), ref.getUsb(), semIdx, parser.isHModel(), ref.isOuter(),
					ref.getPathNotationConfig());
		} else {
			return createGroupId((IGroup) f, ref.getIndices(), ref.getUsb(), semIdx, parser.isHModel(), parser.getEc(),
					ref.isOuter(), ref.getPathNotationConfig());
		}
	}

	private static List<Object> getSemanticIndex(EntityRef ref, boolean isHModel, IEC ec) {
		if (ref.getSemanticIndex() == null) {
			return Collections.emptyList();
		}
		SemanticIndexRef semIdx = ref.getSemanticIndex();
		if (semIdx.isConstant()) {
			return Collections.singletonList(semIdx.getAsConstant());
		} else {
			return Collections.singletonList(new EntityIdImpl(semIdx.getAsEntity(), isHModel, ec));
		}
	}

	/**
	 * Add all indices for the entity below the group.
	 * As the group is expanded, we take either 1 or wildcard, depending on whether the repetition
	 * layer is repeatable.
	 * <p>
	 * ATTENTION: The given list of indices is manipulated and should be copied by the caller.
	 */
	private static void addIndicesBelowGroup(List<Long> indices, long[] maxFeld) {
		for (int i = indices.size(); i < maxFeld.length; i++) {
			Long max = (maxFeld[i] > 1) ? IParseTreeEntity.WILDCARD : 1L;
			indices.add(max);
		}
	}
}
