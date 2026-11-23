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
package com.mgmtp.a12.kernel.core.parser.internal.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplLenientBaseClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.MVKParserError;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnumForHModel;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.Lenient_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.ExpandMode;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IParseTreeEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;

/**
 * Helper methods for {@link EntityRef} objects.
 */
public class EntityRefUtils {

	private EntityRefUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Filter all group references (i.e. groups in which the entity is contained) from the given
	 * entities. The ParserRuleContext is used to mark the resp. part of the condition in case of an
	 * error.
	 * returns an empty set, if no such groups are found.
	 */
	public static Set<EntityRefInRule> getGRefs(
			Set<EntityRefInRule> entities, IEntity errorField) {

		Predicate<EntityRefInRule> g = e -> e.getRef().getEntity().isGroup() &&
				!EntityUtils.isRootGroup(e.getRef().getEntity().getFullName()) &&
				EntityUtils.isAncestorOrEqual(e.getRef().getEntity(), errorField.getFullName());

		return entities.stream().filter(g).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Filter all V-references (i.e. root-groups in which the entity is contained) from the given
	 * entities. The ParserRuleContext is used to mark the resp. part of the condition in case of an
	 * error.
	 * returns an empty set, if no such root groups are found.
	 */
	public static Set<EntityRefInRule> getVRefs(
			Set<EntityRefInRule> entities, IEntity errorField) {

		Predicate<EntityRefInRule> v = e -> e.getRef().getEntity().isGroup() &&
				EntityUtils.isRootGroup(e.getRef().getEntity().getFullName()) &&
				EntityUtils.isAncestorOrEqual(e.getRef().getEntity(), errorField.getFullName());

		return entities.stream().filter(v).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Return all direct references of the given entity.
	 *
	 * @param entities
	 * 		entity references in rules
	 * @param entity
	 * 		entity
	 * @return an empty set, if no such entities are found.
	 */
	public static Set<EntityRefInRule> getDRefs(
			Set<EntityRefInRule> entities, IEntity entity) {
		return entities.stream().filter(e -> e.getRef().getEntity().getFullName().equals(entity.getFullName()))
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Returns true, if one of the entities specifies U-Iteration
	 */
	public static boolean isIterationU(Set<EntityRefInRule> entries) {
		return entries.stream().anyMatch(e -> e.getRef().getUsb() == IParseTreeEntity.ITERATION);
	}

	/**
	 * Returns true, if one of the entities specifies iteration for the given level
	 */
	public static boolean isIteration(Set<EntityRefInRule> entries, int level) {
		return entries.stream()
				.anyMatch(e -> level < e.getRef().getIndices().size() //make sure, the entity has the given level
						&& e.getRef().getIndices().get(level) == IParseTreeEntity.ITERATION);
	}

	/**
	 * Returns true, if one of the entities specifies parallel iteration for any level
	 */
	public static boolean isParallelIteration(Set<EntityRefInRule> entries) {
		return entries.stream()
				.anyMatch(e -> e.getRef().getIndices().contains(IParseTreeEntity.PARALLEL_ITERATION));
	}

	// returns true in case of a Z-Iteration
	public static boolean isZIteration(Set<EntityRefInRule> entries) {
		//precondition: is called only in H models! is responsibility of the caller.
		return isIteration(entries, 1);
	}

	public static boolean isLIteration(Set<EntityRefInRule> entries) {
		return isIteration(entries, 0);
	}

	/**
	 * returns the first entity specifying an iteration for U.
	 *
	 * @param entities
	 * 		entities to be analysed
	 * @return the first entity specifying iteration for U, if any exist
	 */
	public static Optional<EntityRefInRule> getEntityWithUIter(
			Set<EntityRefInRule> entities) {
		return entities.stream()
				.filter(e -> e.getRef().getUsb() == IParseTreeEntity.ITERATION)
				.findFirst();
	}

	/**
	 * returns the first entity not specifying an iteration for U.
	 *
	 * @param entities
	 * 		entities to be analysed
	 * @return the first entity not specifying iteration for U, if any exist
	 */
	public static Optional<EntityRefInRule> getEntityWithoutUIter(
			Set<EntityRefInRule> entities) {

		return entities.stream()
				.filter(e -> e.getRef().getUsb() != IParseTreeEntity.ITERATION)
				.findFirst();
	}

	/**
	 * returns the first entity specifying an iteration at repetition layer with the
	 * specified level.
	 *
	 * @param entities
	 * 		entities to be analysed
	 * @return the first entity specifying iteration at the specified repetition layer, if any exist
	 */
	public static Optional<EntityRefInRule> getEntityWithIter(
			Set<EntityRefInRule> entities, int level) {
		//check if the specified repetition layer exists for the entity
		return entities.stream()
				.filter(e -> level < e.getRef().getIndices().size()
						&& e.getRef().getIndices().get(level) == IParseTreeEntity.ITERATION)
				.findFirst();
	}

	/**
	 * returns the first entity specifying a parallel iteration.
	 *
	 * @param entities
	 * 		entities to be analysed
	 * @return the first entity specifying parallel iteration, if any exist
	 */
	public static Optional<EntityRefInRule> getEntityWithParallelIter(Set<EntityRefInRule> entities) {
		return entities.stream()
				.filter(e -> e.getRef().getIndices().contains(IParseTreeEntity.PARALLEL_ITERATION))
				.findFirst();
	}

	public static Set<EntityRefInRule> getAllEntityWithIter(
			Set<EntityRefInRule> entities, int level) {
		//check if the specified repetition layer exists for the entity
		return entities.stream()
				.filter(e -> level < e.getRef().getIndices().size()
						&& e.getRef().getIndices().get(level) == IParseTreeEntity.ITERATION)
				.collect(Collectors.toSet());
	}

	/**
	 * Return an entity which does not specify iteration on the given level.
	 * if the entity does not specify the given repetition layer, a wildcard
	 * is specified implictly.
	 *
	 * @param entities
	 * 		entities to be analyzed
	 * @param level
	 * 		number of the repetition layer
	 * @return entity which does not specify an iteration, if any exists.
	 */
	public static Optional<EntityRefInRule> getRefWithoutIter(
			Set<EntityRefInRule> entities, int level) {

		return entities.stream()
				.filter(e -> level >= e.getRef().getIndices().size() //specified repetition layer does not exist
						|| e.getRef().getIndices().get(level) != IParseTreeEntity.ITERATION)
				.findFirst();
	}

	/**
	 * Return an entity which does not specify parallel iteration on any level.
	 *
	 * @param entities
	 * 		entities to be analyzed
	 * @return entity which does not specify parallel iteration, if any exists.
	 */
	public static Optional<EntityRefInRule> getRefWithoutParallelIter(Set<EntityRefInRule> entities) {

		return entities.stream()
				.filter(e -> !e.getRef().getIndices().contains(IParseTreeEntity.PARALLEL_ITERATION))
				.findFirst();
	}

	/**
	 * überprüft die Iteration eines Kontext über eine Zeile
	 * Im Fehlerfall wird die entsprechend exception geworfen.
	 * <p>
	 * Precondition: H-Modell
	 *
	 * @param ctx
	 * 		nur für Fehlermeldung benötigt
	 * @param ref
	 * 		Referenz, die überprüft werden soll
	 * @param obj
	 * 		für Fehlermeldungen benötigt.
	 */
	public static void checkKontextZIteration(ParserRuleContext ctx, EntityRef ref, CheckImplLenientBaseClass obj) {
		assert obj.getParser().isHModel();
		//Fehlerfeld muss mehrzeilig sein
		if (obj.getRule().getErrorEntity().isGroup() || EntityUtils.getMaxZeile(
				(IField) obj.getRule().getErrorEntity()) == 1) {//  Fehlerfeld muss mehrzeilig sein.
			String errMessage = obj.getErrorMessage(ParserErrorTypesEnumForHModel.MVK_INVALID_Z_ITER,
					ref.getNameBuiltFromConditionText());
			throw obj.createMVKParserError(ctx, errMessage);
		}

		//alle Felder des Kontextes müssen gleichen MZI haben.
		//der MZI eines Kontextes kann nur bestimmt werden,  wenn alle Felder den gleichen MZI haben.
		//daher kann die Methode auch zum überprüfen verwendet werden.
		getMaxZeileApplicableForGroupsAlso(ctx, ref, ParserErrorTypesEnumForHModel.MVK_DIFFERENT_Z, obj);
	}

	/**
	 * Ermittelt die maximale Zeilenanzahl für Felder und Kontexte.
	 * Für Felder entspricht dies der maximalen Angabe der zweiten Wiederholungsebene.
	 * <p>
	 * Für Kontext wird zunächst geprüft, ob alle im Kontext enthaltenen Felder die gleichen
	 * max. Zeile spezifizieren. Falls ja, wird dies als Ergebnis zurückgegeben.
	 *
	 * @param ctx
	 * 		- nur für Fehlermeldungen
	 * @param ref
	 * 		- entity
	 * @param obj
	 * 		- benötigt, um Fehlermeldungen zu erzeugen
	 * @return maximale Zeile des Feldes bzw. der Felder des Kontextes.
	 */
	public static long getMaxZeileApplicableForGroupsAlso(ParserRuleContext ctx, EntityRef ref, ParserErrorTypesEnumForHModel err,
			CheckImplLenientBaseClass obj) {
		assert obj.getParser().isHModel();
		IEntity e = ref.getEntity();
		if (e.isField()) {
			return EntityUtils.getMaxZeile((IField) e);
		}

		// e is a group.  Check that all MZI's are equal and return the MZI.
		String nameUsedForErrorOutput = ref.getNameBuiltFromConditionText();

		List<IField> fields = EntityRefUtils.getAllContainedFieldsAndCheckNotEmpty(ref, obj.getParser(), ctx);
		List<Long> mzi = fields.stream().map(EntityUtils::getMaxZeile).distinct().toList();
		if (mzi.size() > 1) {
			String errMessage = obj.getErrorMessage(err, nameUsedForErrorOutput);
			throw obj.createMVKParserError(ctx, errMessage);
		}
		return mzi.isEmpty() ? 0 : mzi.get(0);
	}

	/**
	 * Extract all contained fields contained in the referenced entity.
	 * Performs the expansion for {@link ExpandMode#EXPAND}.
	 * Should only be called for group references.
	 *
	 * @param ref
	 * 		the reference to the entity.
	 * @param parser
	 * 		the parser containing additional parsing information.
	 * @param ctx
	 * 		the context containing additional information for the entities.
	 * @return the list of all fields contained in the group.
	 */
	public static List<IField> getAllContainedFieldsAndCheckNotEmpty(EntityRef ref, Lenient_ConditionParser parser,
			ParserRuleContext ctx) {
		return getAllContainedFieldsAndCheckNotEmpty(ref.getEntity().getFullName(), ref.getNameBuiltFromConditionText(), parser,
				ctx);
	}

	public static List<IField> getAllContainedFieldsAndCheckNotEmpty(String fullName, String nameUsedForErrorOutput,
			Lenient_ConditionParser parser, ParserRuleContext ctx) {
		List<IField> fields = parser.getEc().getAllContainedFields(fullName);
		if (fields.isEmpty() && !parser.isLenient()) {
			String errMessage = EntityUtils.isRootGroup(fullName) ?
					parser.getMVKErrorHandler()
							.getParserErrorMessage(ParserErrorTypesEnum.MVK_ROOT_GROUP_IS_EMPTY, nameUsedForErrorOutput)
					:
					parser.getMVKErrorHandler()
							.getParserErrorMessage(ParserErrorTypesEnum.MVK_GROUP_IS_EMPTY, nameUsedForErrorOutput);
			throw new MVKParserError(ctx.start, ctx.start, parser.getRule(), errMessage);
		}
		return fields;
	}

	/**
	 * Get a list of all entities contained (regarding the expandMode) in the group referenced by
	 * the EntityRef.
	 * ATTENTION: This method must not be called for field entities. It would result in wrong
	 * expansion for {@link ExpandMode#EXPAND_AND_ADD_GROUPS}.
	 *
	 * @param ref
	 * 		the reference to the group.
	 * @param parser
	 * 		the parser containing additional parsing information.
	 * @param ctx
	 * 		the context specifying additional parameters for the resulting entities.
	 * @param expandMode
	 * 		specifies, how the expansion should be performed.
	 * @return the list of entities in the group.
	 */
	public static List<IEntity> getAllContainedEntitiesAndCheckNotEmpty(EntityRef ref, Lenient_ConditionParser parser,
			EntitySpecContext ctx, ExpandMode expandMode) {
		switch (expandMode) {
			case NO_EXPAND:
				return Collections.singletonList(ref.getEntity());
			case EXPAND_AND_ADD_GROUPS:
				List<IEntity> result = new ArrayList<>();
				result.add(ref.getEntity());
				result.addAll(
						getAllContainedEntitiesAndCheckNotEmpty(ref.getEntity().getFullName(), ref.getNameBuiltFromConditionText(),
								parser, ctx));
				return result;
			case EXPAND_PARTIALLY, EXPAND:
				return new ArrayList<>(getAllContainedFieldsAndCheckNotEmpty(ref, parser, ctx));
			default:
				throw new IllegalArgumentException("Unknown ExpandMode " + expandMode);
		}
	}

	private static List<IEntity> getAllContainedEntitiesAndCheckNotEmpty(String fullName, String nameUsedForErrorOutput,
			Lenient_ConditionParser parser, EntitySpecContext ctx) {
		List<IEntity> entities = EntityUtils.getAllContainedEntities(fullName, parser.getEc());
		if (entities.stream().noneMatch(IEntity::isField)) {
			String errMessage = EntityUtils.isRootGroup(fullName) ?
					parser.getMVKErrorHandler()
							.getParserErrorMessage(ParserErrorTypesEnum.MVK_ROOT_GROUP_IS_EMPTY, nameUsedForErrorOutput)
					:
					parser.getMVKErrorHandler()
							.getParserErrorMessage(ParserErrorTypesEnum.MVK_GROUP_IS_EMPTY, nameUsedForErrorOutput);
			throw new MVKParserError(ctx.start, ctx.start, parser.getRule(), errMessage);
		}
		return entities;
	}

	/**
	 * Checks that, if the entity is a group, it is not empty (i.e. it has to contain fields).
	 */
	public static void checkNoEmptyGroup(IEntity entity, String nameUsedForErrorOutput, Lenient_ConditionParser parser,
			ParserRuleContext ctx) {
		if (entity.isGroup()) {
			getAllContainedFieldsAndCheckNotEmpty(entity.getFullName(), nameUsedForErrorOutput, parser, ctx);
		}
	}

}
