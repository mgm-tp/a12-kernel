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
package com.mgmtp.a12.kernel.core.parser.internal.check.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.StringUtils;

import com.mgmtp.a12.kernel.core.base.utils.internal.Java8Utils;
import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.constants.EntityIdConstants;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorHandler;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnumForHModel;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntityListeContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.DateFormat;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRefForHModel;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultType.OperationResultBaseType;
import com.mgmtp.a12.kernel.core.parser.internal.util.OperationResultTypeMap;
import com.mgmtp.a12.kernel.core.parser.internal.util.SemanticIndexRef;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IParseTreeEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IDateRangeType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IDateType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IDateType.PartlyKnownDateType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEnumerationType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEnumerationType.EnumerationValue;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IFieldType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IStringType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IYesNoType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IYesType;

/**
 * Provides Utility methods for all entity lists.
 */
public class CheckEntityListenUtils extends CheckImplAbstractClass {

	public CheckEntityListenUtils(FullChecks_ConditionParser parser) {
		super(parser);
	}

	/**
	 * returns the list of EntityRefs specified for the given context
	 * checks if the parameters are referenced only once in the index list
	 *
	 * @param ctx
	 * 		context specifying the entity list
	 * @param opName
	 * 		name of operation
	 * @return list of entities
	 */
	public List<EntityRef> getRefList(EntityListeContext ctx, String opName) {
		List<EntityRef> result = ctx.entitySpec().stream()
				.map(this::getEntityRef).toList();
		checkParamsReferencedOnlyOnce(result, ctx, opName);
		return result;
	}

	/**
	 * checks that no item in the list defines a category
	 *
	 * @param ctx
	 * 		context specifying the entity list
	 * @param opName
	 * 		name of operation
	 */
	public void checkNoCategories(EntityListeContext ctx, String opName) {
		if (ctx.entitySpec().stream().anyMatch(isCategoryDefined)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_CATEGORY_NOT_ALLOWED, opName);
			throw createMVKParserError(ctx.getParent(), errMessage);
		}
	}

	/**
	 * checks that no item in the list defines 'noValueValidation'
	 *
	 * @param refs
	 * 		list of referenced fields
	 * @param ctx
	 * 		context specifying the entity list
	 * @param opName
	 * 		name of operation
	 */
	public void checkAllAreValueValidation(List<EntityRef> refs, ParserRuleContext ctx, String opName) {
		List<IField> fields = EntityUtils.expandToFieldList(refs, getParser(), ctx);
		boolean noValueValidationPresent = fields.stream()
				.filter(FieldUtils::isString)
				.map(f -> (IStringType) f.getType())
				.anyMatch(IStringType::isNoValueValidation);
		if (noValueValidationPresent) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_RAW_TYPE, opName);
			throw createMVKParserError(ctx, errMessage);
		}
	}

	/**
	 * checks, if an entity is referenced directly or indirectly by another entity of the list
	 *
	 * @param paramList
	 * 		list of entities to be checked
	 * @param ctx
	 * 		- required only for the error message
	 * @param opName
	 * 		name of operation
	 */
	public void checkParamsReferencedOnlyOnce(List<EntityRef> paramList, ParserRuleContext ctx, String opName) {

		//(1) check for direct duplicates
		checkDuplicatesInDirectRefs(paramList, ctx, opName);

		//(2) check for indirect duplicates
		checkDuplicatesInIndirectRefs(paramList, ctx, opName);
	}

	private void checkDuplicatesInIndirectRefs(List<EntityRef> paramList, ParserRuleContext ctx, String opName) {
		Set<EntityRef> entities = new HashSet<>(paramList);
		//check if param is referenced indirectly by a group
		for (EntityRef ref : paramList) {
			if (isEntityIndirectlyReferenced(ref, entities)) {
				throwExceptionWithOpNameAndEntityRef(ParserErrorTypesEnum.MVK_DUPLICATE_PARAM2, ctx, ref, opName);
			}
		}
	}

	private void checkDuplicatesInDirectRefs(List<EntityRef> paramList, ParserRuleContext ctx, String opName) {
		Set<EntityRef> temp = new HashSet<>();
		for (EntityRef ref : paramList) {
			if (ref.hasWildCards() && !parser.isHModel()) {
				continue;
			}
			if (!temp.add(ref)) {
				throwExceptionWithOpNameAndEntityRef(ParserErrorTypesEnum.MVK_DUPLICATE_PARAM1, ctx, ref, opName);
			}
		}
	}

	/**
	 * returns true, if entity is referenced indirectly by a group
	 * The semantic index and categories used in the entity and group are compared, too.
	 */
	private boolean isEntityIndirectlyReferenced(EntityRef e, Set<EntityRef> refs) {
		return refs.stream().anyMatch(s -> e.getEntity().getFullName().startsWith(s.getEntity().getFullName() + "/") &&
				Objects.equals(e.getSemanticIndex(), s.getSemanticIndex()) &&
				Objects.equals(e.getCategoryName(), s.getCategoryName()));
	}

	/**
	 * Checks that the paramList contains only one element
	 *
	 * @param paramList
	 * 		list to be checked
	 * @param ctx
	 * 		only required for error handling
	 * @param opName
	 * 		name of the operation: used in the error message
	 */
	public void checkParamSizeIs1(List<EntityRef> paramList, EntityListeContext ctx, String opName) {
		if (paramList.size() != 1) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_PARAMSIZE_INVALID1, opName);
			throw createMVKParserError(ctx.getParent(), errMessage);
		}
	}

	/**
	 * Checks that the paramList contains exactly two elements
	 *
	 * @param paramList
	 * 		list to be checked
	 * @param ctx
	 * 		only required for error handling
	 * @param opName
	 * 		name of the operation: used in the error message
	 */
	public void checkParamSizeIs2(List<EntityRef> paramList, EntityListeContext ctx, String opName) {
		if (paramList.size() != 2) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_PARAMSIZE_INVALID_2, opName);
			throw createMVKParserError(ctx.getParent(), errMessage);
		}
	}

	/**
	 * Checks that the paramList contains only one groups
	 *
	 * @param paramList
	 * 		list to be checked
	 * @param ctx
	 * 		only required for error handling
	 * @param opName
	 * 		name of operation
	 */
	public void checkOnlyGroups(List<EntityRef> paramList, EntityListeContext ctx, String opName) {
		paramList.forEach(ref -> {
			if (!ref.getEntity().isGroup()) {
				throwException(ParserErrorTypesEnum.MVK_NO_FIELDS_ALLOWED, ctx, opName, ref.getNameBuiltFromConditionText());
			}
		});
	}

	/**
	 * Checks that the paramList contains only fields
	 *
	 * @param ctx
	 * 		only required for error handling
	 * @param opName
	 * 		name of operation
	 * @param paramListe
	 * 		list to be checked
	 */

	public void checkOnlyFields(List<EntityRef> paramListe, EntityListeContext ctx, String opName) {
		CheckEntityUtils checker = new CheckEntityUtils(fullParser);
		paramListe.forEach(ei -> checker.checkIsField(ei, opName, ctx.getParent()));
	}

	/**
	 * Checks that the paramList contains only entities in the same root group
	 *
	 * @param refs
	 * 		list to be checked
	 * @param ctx
	 * 		only required for error handling
	 * @param opName
	 * 		name of operation
	 */

	public void checkAllEntitiesInSameRootGroup(List<EntityRef> refs, EntityListeContext ctx, String opName) {
		String rootGroup = null;
		for (EntityRef ref : refs) {
			String v = EntityUtils.getRootGroupName(ref.getEntity().getFullName());
			if (rootGroup != null && !rootGroup.equals(v)) {
				String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_DIFFERENT_ROOT_GROUPS, opName,
						ref.getNameBuiltFromConditionText());
				throw createMVKParserError(ctx.getParent(), errMessage);
			}
			rootGroup = v;
		}
	}

	/**
	 * Checks that the paramList contains only entities in the same group
	 *
	 * @param refs
	 * 		list to be checked
	 * @param ctx
	 * 		only required for error handling
	 * @param opName
	 * 		name of operation
	 */
	public void checkAllEntitiesInSameGroup(List<EntityRef> refs, EntityListeContext ctx, String opName) {
		String groupPath = null;
		for (EntityRef ref : refs) {
			String g = EntityUtils.getParentPath(ref.getEntity().getFullName());
			if (groupPath != null && !groupPath.equals(g)) {
				String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_DIFFERENT_GROUPS, opName,
						ref.getNameBuiltFromConditionText());
				throw createMVKParserError(ctx.getParent(), errMessage);
			}
			groupPath = g;
		}
	}

	/**
	 * Checks that repeatable groups referenced in the paramList (also indirectly, e.g. via
	 * referencing a field, that lies in the group) lie on the same path
	 *
	 * @param paramList
	 * 		list to be checked
	 * @param ctx
	 * 		only required for error handling
	 * @param opName
	 * 		name of operation
	 */
	public void checkAllRepeatableGroupsOnSamePath(List<EntityRef> paramList, EntityListeContext ctx, String opName) {
		String path = null;
		for (EntityRef ref : paramList) {
			IEntity entity = ref.getEntity();
			if (EntityUtils.isRepeatableOnOneLevel(entity, false)) {
				String p = EntityUtils.getLowestRepeatableGroupPath(entity) + "/";
				String actPath = getLongestSharedPath(path, p);
				if (actPath == null) {
					throwExceptionWithOpNameAndEntityRef(ParserErrorTypesEnum.MVK_NOT_IN_SAME_PATH, ctx, ref, opName);
				}
				path = actPath;
			}
		}
	}

	/**
	 * <b>Precondition</b>: the paths end with slash
	 *
	 * @param path1
	 * 		first path
	 * @param path2
	 * 		second path, may not be null
	 * @return the longest of the two paths if one is a sub path of the other, otherwise null
	 */
	private String getLongestSharedPath(String path1, String path2) {
		if (path1 == null) {
			return path2;
		}
		String pMin = path1;
		String pMax = path2;
		if (path1.length() > path2.length()) {
			pMin = path2;
			pMax = path1;
		}

		//the paths always end with slash,  so the use of startsWith is safe.
		return (pMax.startsWith(pMin)) ? pMax : null;
	}

	/**
	 * Checks that the paramList contains only entities in the rule group
	 *
	 * @param refs
	 * 		list to be checked
	 * @param ctx
	 * 		only required for error handling
	 * @param opName
	 * 		name of operation
	 */

	public void checkAllEntitiesInRuleGroup(List<EntityRef> refs, EntityListeContext ctx, String opName) {
		String ruleGroupWithSlash = EntityUtils.getParentPathWithSlash(fullParser.getRule().getFullName());
		for (EntityRef ref : refs) {
			String p = EntityUtils.getParentPathWithSlash(ref.getEntity().getFullName());
			if (!p.startsWith(ruleGroupWithSlash)) {
				throwExceptionWithOpNameAndEntityRef(ParserErrorTypesEnum.MVK_NOT_IN_RULEGROUP, ctx, ref, opName);
			}
		}
	}

	public void checkAllEntitiesInGroup(List<EntityRef> refs, EntityRef groupRef, EntityListeContext ctx, String opName) {
		String groupNameWithSlash = groupRef.getEntity().getFullName() + "/";
		for (EntityRef ref : refs) {
			String p = EntityUtils.getParentPathWithSlash(ref.getEntity().getFullName());
			if (!p.startsWith(groupNameWithSlash)) {
				String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_NOT_IN_GROUP, opName,
						getEntityRefStringRepresentation(groupRef), getEntityRefStringRepresentation(ref));
				throw createMVKParserError(ctx, errMessage);
			}
		}
	}

	/**
	 * checks that all entities of the paramList have a repeatable group below the rule group
	 * <p>
	 * E.g. /a/b/c/rulegroup/c/d/e
	 * level is 4, thus skip a,b,c,rulegroup and check index for level c, d, e.
	 *
	 * @param refs
	 * 		references to be checked
	 * @param ctx
	 * 		for nice errorMessages only
	 * @param opName
	 * 		name of operation - for nice error Messages only
	 */

	public void checkAllEntitiesHaveRepeatableGroupBelowRuleGroup(List<EntityRef> refs, EntityListeContext ctx, String opName) {
		assert fullParser.isA12Model();
		String ruleGroup = EntityUtils.getParentPath(fullParser.getRule().getFullName());
		int level = StringUtils.countMatches(ruleGroup, "/");

		//check for all references, if there is a repeatable group below the given level.
		//below given level is implemented by skipping the levels which are not relevant for the check.
		refs.forEach(ref -> {
			long[] max = EntityUtils.getMaxInA12Model(ref.getEntity());
			if (Arrays.stream(max).skip(level).noneMatch(i -> i > 1)) {
				String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_REPEATABLE_GROUP_MISSING, opName,
						ref.getNameBuiltFromConditionText());
				throw createMVKParserError(ctx.getParent(), errMessage);
			}
		});

	}

	/**
	 * one of the following cases have to hold:
	 * <ol>
	 * <li>the rule group is the root group and all entities in the parameter list are fields
	 * of the root group, and the root group is repeatable</li>
	 * <li>all entites have a repeatable group below the rule group
	 * </ol>
	 * <p>
	 * As a precondition: it has already been checked, that all parameters are on the
	 * path of the rule group and specify no wildcards.
	 * Returns <code>true</code> if all of the following holds:
	 * <ul>
	 * <li>the rule group is the root group</li>
	 * <li>the root group is repeatable</li>
	 * <li>all parameters are directly contained in the root group</li>
	 * </ul>
	 *
	 * @param refs
	 * 		parameters to be checked.
	 * @return <code>true</code> if the above conditions all hold, false otherwise.
	 */
	public boolean areAllEntitiesDirectlyInRepeatableRootGroup(List<EntityRef> refs) {
		assert fullParser.isA12Model();
		String ruleGroup = EntityUtils.getParentPath(fullParser.getRule().getFullName());

		//rule group has to be the root group
		if (!EntityUtils.isRootGroup(ruleGroup)) {
			return false;
		}
		//rule group has to be repeatable
		if (fullParser.getEc().get(ruleGroup).getMax()[0] == 1) {
			return false;
		}
		// all parameters have to be directly contained in a root group
		return refs.stream()
				.map(ref -> StringUtils.countMatches(ref.getEntity().getFullName(), "/"))
				.allMatch(s -> s <= 2);
	}

	/**
	 * checks that no wildcards are specified for any entity of the paramList
	 *
	 * @param refs
	 * 		list to be checked
	 * @param ctx
	 * 		for nice error messages only
	 * @param opName
	 * 		name of operation
	 */
	public void checkNoWildCards(List<EntityRef> refs, EntityListeContext ctx, String opName) {
		CheckEntityUtils checker = new CheckEntityUtils(fullParser);
		refs.forEach(ref -> checker.checkNoWildCards(ref, opName, ctx.getParent()));
	}

	public void checkAllFieldsAreNumbers(List<EntityRef> refs, EntityListeContext ctx, String opName,
			ParserErrorTypesEnum errType) {
		CheckEntityUtils checker = new CheckEntityUtils(fullParser);
		refs.forEach(ref -> checker.checkIsNumber(ref, opName, ctx.getParent(), errType));
	}

	public void checkAllFieldsLikeStringOrEnum(List<EntityRef> refs, EntityListeContext ctx, String opName,
			ParserErrorTypesEnum errType) {
		CheckEntityUtils checker = new CheckEntityUtils(fullParser);
		refs.forEach(ref -> checker.checkIsLikeStringOrEnum(ref, opName, ctx.getParent(), errType));
	}

	public void checkAllAreValueValidation(List<EntityRef> refs, EntityListeContext ctx, String opName) {
		CheckEntityUtils checker = new CheckEntityUtils(fullParser);
		refs.forEach(ref -> checker.checkIsValueValidation(ref, opName, ctx.getParent()));
	}

	public void checkIsMoreThanOneFieldOrIsAGroup(List<EntityRef> refs, EntityListeContext ctx, String opName) {
		checkIsMoreThanOneEntity(refs, ctx, ParserErrorTypesEnum.MVK_PARAMSIZE_INVALIDN, opName);
	}

	public void checkIsMoreThanOneField(List<EntityRef> refs, EntityListeContext ctx, String opName) {
		checkIsMoreThanOneEntity(refs, ctx, ParserErrorTypesEnum.MVK_PARAMSIZE_INVALIDFN, opName);
	}

	private void checkIsMoreThanOneEntity(List<EntityRef> refs, EntityListeContext ctx, ParserErrorTypesEnum err, String opName) {
		if (refs.size() == 1 && refs.get(0).getEntity().isField() && !refs.get(0).hasWildCards()) {
			throwException(err, ctx, opName);
		}
	}

	/**
	 * precondition: all refs in the list are date range types
	 * checks that all entity refs in the list either contain all a year fragment, or none contain a
	 * year fragment.
	 * In ExtMP: special case of base year: no checks are necessary.
	 *
	 * @param refs
	 * 		list to be checked
	 * @param ctxForErrorMessaging
	 * 		for nice error messages
	 * @param opName
	 * 		name of operation - for nice error messages
	 */
	public void checkDateRangeConsistentRegardingYear(List<EntityRef> refs, EntityListeContext ctxForErrorMessaging,
			String opName) {
		if (getParser().hasBaseYear()) { // if specification of the year is missing,  base year is used as year.
			return;
		}

		Boolean flag = null;
		for (EntityRef ref : refs) {
			List<Boolean> res = EntityUtils.expandToFieldList(ref, fullParser, ctxForErrorMessaging)
					.stream().map(f -> ((IDateRangeType) f.getType()).getFormatOfDate()).distinct()
					.map(s -> s.contains("y")).toList();
			if (res.size() > 1 || (flag != null && !flag.equals(res.get(0)))) {
				throwExceptionWithOpNameAndEntityRef(ParserErrorTypesEnum.MVK_DATE_WITH_AND_WITHOUT_YEAR, ctxForErrorMessaging, ref,
						opName);
			}
			flag = res.get(0);
		}

	}

	public void throwExceptionWithOpNameAndEntityRef(ParserErrorTypesEnum err, ParserRuleContext ctx, EntityRef ei,
			String opName) {
		String errMessage = getErrorMessage(err, opName, getEntityRefStringRepresentation(ei));
		throw createMVKParserError(ctx.getParent(), errMessage);
	}

	/**
	 * Create a string representation of an {@link EntityRef} to be output in an error message.
	 *
	 * @param ei
	 * 		the entity reference to output.
	 * @return a string representation containing the name and the category and semantic index if
	 * 		present.
	 */
	public static String getEntityRefStringRepresentation(EntityRef ei) {
		StringBuilder result = new StringBuilder(ei.getNameBuiltFromConditionText());
		if (ei.getCategoryName() != null) {
			result.append(EntityIdConstants.CATEGORY_VALUE_ACCESS).append(ei.getCategoryName());
		}
		if (ei.getSemanticIndex() != null) {
			result.append(" ");
			result.append(EntityIdConstants.SEMANTIC_INDEX_TOKEN);
			result.append(" ");
			result.append(getSemanticIndexStringRepresentation(ei.getSemanticIndex()));
		}
		return result.toString();
	}

	private static String getSemanticIndexStringRepresentation(SemanticIndexRef semanticIndex) {
		if (semanticIndex.isConstant()) {
			return "\"" + semanticIndex.getAsConstant() + "\"";
		} else {
			return getEntityRefStringRepresentation(semanticIndex.getAsEntity());
		}
	}

	private void throwException(ParserErrorTypesEnum err, ParserRuleContext ctx, Object... args) {
		String errMessage = getErrorMessage(err, args);
		throw createMVKParserError(ctx.getParent(), errMessage);
	}

	public void checkIsMoreThanOneGroupOrField(List<EntityRef> paramListe, EntityListeContext ctxForErrorMessaging, String opName) {
		if (!isMoreThanOneParameter(paramListe)) {
			throwException(ParserErrorTypesEnum.MVK_PARAMSIZE_INVALID2, ctxForErrorMessaging, opName);
		}
	}

	/**
	 * Only checks the number of entities in the parameter list, NOT if there are (only) groups in
	 * the list.
	 */
	public void checkIsMoreThanOneGroup(List<EntityRef> paramListe, EntityListeContext ctxForErrorMessaging, String opName) {
		if (!isMoreThanOneParameter(paramListe)) {
			throwException(ParserErrorTypesEnum.MVK_PARAMSIZE_INVALIDGN, ctxForErrorMessaging, opName);
		}
	}

	private boolean isMoreThanOneParameter(List<EntityRef> paramListe) {
		if (paramListe.size() > 1) {
			return true;
		}
		return paramListe.get(0).hasWildCards();
	}

	public void checkNoWildCardsInGroup(List<EntityRef> refs, EntityListeContext ctxForErrorMessaging, String opName) {
		refs.forEach(ref -> {
			if (ref.hasWildCardsInGroup()) {
				throwException(ParserErrorTypesEnum.MVK_NO_WILDCARDS_G_ALLOWED, ctxForErrorMessaging, opName);
			}
		});

	}

	public void checkOnlyStringEnumNumber(List<EntityRef> refs, EntityListeContext ctxForErrorMessaging, String opName) {
		for (EntityRef ref : refs) {
			List<IField> entities = EntityUtils.expandToFieldList(ref, fullParser, ctxForErrorMessaging);
			for (IField f : entities) {
				if (!FieldUtils.isStringEnumOrNumber(f)) {
					throwExceptionWithOpNameAndEntityRef(ParserErrorTypesEnum.MVK_ONLY_STRING_ENUM_NUMBER_ALLOWED,
							ctxForErrorMessaging, ref, opName);
				}
			}
		}
	}

	/**
	 * Checks that all referenced fields are string-like (including predefined types and extensible
	 * enumerations), enumeration, number or date. In case of type date it additionally checks that
	 * all fields specify the same date-format.
	 */
	public void checkOnlyStringEnumNumberOrDateWithSameFormat(List<EntityRef> refs, EntityListeContext ctxForErrorMessaging,
			String opName) {
		String dateFormat = null;
		for (EntityRef ref : refs) {
			List<IField> entities = EntityUtils.expandToFieldList(ref, fullParser, ctxForErrorMessaging);
			for (IField f : entities) {
				if (FieldUtils.isStringEnumOrNumber(f)) {
					continue;
				}
				if (!FieldUtils.isDate(f)) {
					throwExceptionWithOpNameAndEntityRef(ParserErrorTypesEnum.MVK_ONLY_STRING_ENUM_NUMBER_DATE_ALLOWED,
							ctxForErrorMessaging, ref, opName);
				}
				//it's a date type
				String format = ((IDateType) f.getType()).getFormat();

				if (dateFormat != null && !dateFormat.equals(format)) {
					throwExceptionWithOpNameAndEntityRef(ParserErrorTypesEnum.MVK_ONLY_STRING_ENUM_NUMBER_DATE_ALLOWED,
							ctxForErrorMessaging, ref, opName);
				}
				dateFormat = format;

			}
		}
	}

	public void checkNoBoolY(List<EntityRef> refs, EntityListeContext ctxForErrorMessaging, String opName) {
		for (EntityRef ref : refs) {
			List<IField> entities = EntityUtils.expandToFieldList(ref, fullParser, ctxForErrorMessaging);
			for (IField f : entities) {
				if (FieldUtils.isYes(f)) {
					throwExceptionWithOpNameAndEntityRef(ParserErrorTypesEnum.MVK_NO_BOOLY_ALLOWED, ctxForErrorMessaging, ref,
							opName);
				}
			}
		}
	}

	public void checkSameBaseType(List<EntityRef> refs, ParserRuleContext ctxForErrorMessages, String opName) {
		matchBaseType(refs, ctxForErrorMessages, null, opName);
	}

	/**
	 * Checks if all fields referenced by the paramList are of the base type specified in expected
	 * type.
	 * If the expected type is null, the method checks only, that all fields are of the same base
	 * type.
	 * If one of the fields in the paramList has no type (this is possible in case of incomplete
	 * specification of a field) an error is thrown, because it is not possible to check if all
	 * fields are correct.
	 *
	 * @param refs
	 * 		list of fields or groups to be checked.
	 * 		In case of the groups, all fields in the groups are checked.
	 * @param ctxForErrorMessages
	 * 		for nice error messages
	 * @param expectedType
	 * 		type to be checked against
	 * @param opName
	 * 		name of operation, for error messages
	 */
	public void matchBaseType(List<EntityRef> refs, ParserRuleContext ctxForErrorMessages, IFieldType expectedType, String opName) {
		IFieldType expected = expectedType;
		for (IField f : EntityUtils.expandToFieldList(refs, fullParser, ctxForErrorMessages)) {
			if (f.getType() == null || !f.getType().matchBaseType(expected)) {
				throwException(ParserErrorTypesEnum.MVK_VARYING_TYPES_NOT_ALLOWED, ctxForErrorMessages, opName);
			}
			expected = f.getType();
		}
	}

	public OperationResultType checkAllFieldsMatchingOperationType(List<EntityRef> paramListe, ParserRuleContext elCtx,
			String opName) {
		List<IField> fields = EntityUtils.expandToFieldList(paramListe, fullParser, elCtx);
		OperationResultType result = null;
		for (IField field : fields) {
			OperationResultType curType = OperationResultTypeMap.getOpTypeForField(elCtx, field.getType());
			curType = treatEnumAsString(curType);
			if (result == null) {
				result = curType;
			} else if (!result.matches(curType)) {
				throwException(ParserErrorTypesEnum.MVK_VARYING_TYPES_NOT_ALLOWED, elCtx, opName);
			}
		}
		return result;
	}

	public OperationResultType firstType(List<EntityRef> paramListe, ParserRuleContext elCtx) {
		List<IField> fields = EntityUtils.expandToFieldList(paramListe, fullParser, elCtx);
		if (!fields.isEmpty()) {
			return OperationResultTypeMap.getOpTypeForField(elCtx, fields.get(0).getType());
		}
		throw new IllegalArgumentException("firstType should not be called on a list that may be empty");
	}

	public OperationResultType checkAllFieldsAreComparableDatetimeType(List<EntityRef> paramListe, boolean hasBaseYear,
			ParserRuleContext elCtx, String opName) {
		List<OperationResultType> otypes = opTypesFromEntityList(paramListe, elCtx);
		return checkOpTypesAreComparableDatetimeType(otypes, hasBaseYear, elCtx, opName);
	}

	private List<OperationResultType> opTypesFromEntityList(List<EntityRef> paramListe, ParserRuleContext elCtx) {
		return EntityUtils.expandToFieldList(paramListe, fullParser, elCtx).stream().map(
				(IField field) -> OperationResultTypeMap.getOpTypeForField(elCtx, field.getType())).toList();
	}

	public OperationResultType checkOpTypesAreComparableDatetimeType(List<OperationResultType> otypes, boolean hasBaseYear,
			ParserRuleContext elCtx, String opName) {
		OperationResultType first = null;
		for (OperationResultType curType : otypes) {
			if (curType.getBaseType() != OperationResultBaseType.DATETIME) {
				throw createMVKParserError(elCtx,
						getErrorMessage(ParserErrorTypesEnum.MVK_DATE_AND_NONDATE, opName, curType.getBaseType()));
			}
			PartlyKnownDateType optParts = partlyKnownDateTypeIfPresent(curType);
			if (optParts != null && optParts != PartlyKnownDateType.BEKANNT) {
				throw createMVKParserError(elCtx,
						getErrorMessage(ParserErrorTypesEnum.MVK_PARTIAL_DATE_NOT_ALLOWED, opName, optParts));
			}
			if (first == null) {
				first = curType;
			} else {
				// formatsConsistentRegardingFormat seems to be weaker than hasSameComponents and is probably redundant here
				if (!DateFormat.formatsConsistentRegardingFormat(first.getDateFormat(), curType.getDateFormat(), hasBaseYear)
						|| !first.getDateFormat().hasSameComponents(curType.getDateFormat(), hasBaseYear)) {
					throw createMVKParserError(elCtx,
							getErrorMessage(ParserErrorTypesEnum.MVK_DATEFORMATS_NOT_COMPATIBLE,
									opName,
									first.getDateFormat().description(),
									curType.getDateFormat().description()));
				}
			}
		}
		if (first == null) {
			throw new IllegalArgumentException("Cannot infer common datetime type from empty list of types");
		}
		return first.withUnorderedDateFormat();
	}

	private PartlyKnownDateType partlyKnownDateTypeIfPresent(OperationResultType t) {
		IFieldType ft = t.getFieldType();
		return (!(ft instanceof IDateType dateType)) ? null : dateType.partlyKnownDate();
	}

	private OperationResultType treatEnumAsString(OperationResultType curType) {
		if (curType.getBaseType() == OperationResultBaseType.ENUM) {
			return new OperationResultType(OperationResultBaseType.STRING, curType.getFieldType());
		} else {
			return curType;
		}
	}

	public void checkFieldsAreYesOrYesNoWithSameYesValue(List<EntityRef> refs, ParserRuleContext ctxForErrorMessages,
			String opName) {
		String value = null;

		for (EntityRef ref : refs) {
			for (IField f : EntityUtils.expandToFieldList(ref, fullParser, ctxForErrorMessages)) {
				String trueValue = getYesValue(f, ctxForErrorMessages, opName);

				if (value != null && !value.equals(trueValue)) {
					throwException(ParserErrorTypesEnum.MVK_DIFFERENT_YES_VALUE, ctxForErrorMessages, opName);
				}
				value = trueValue;
			}
		}
	}

	private String getYesValue(IField f, ParserRuleContext ctxForErrorMessages, String opName) {
		if (FieldUtils.isYes(f)) {
			return ((IYesType) f.getType()).getTrueValue();
		} else if (FieldUtils.isYesNo(f)) {
			return ((IYesNoType) f.getType()).getTrueValue();
		}
		throwException(ParserErrorTypesEnum.MVK_NO_TYPE_YES_OR_YESNO, ctxForErrorMessages, opName);
		return null; //never reached
	}

	public void checkAllFieldsAreValidStringOrEnums(List<EntityRef> paramList, EntityListeContext ctx, String value,
			String opName) {
		paramList.forEach(ei -> checkIsValidStringOrEnums(ei, ctx, value, opName));
	}

	private void checkIsValidStringOrEnums(EntityRef e, EntityListeContext ctx, String value, String opName) {
		List<IField> fields = EntityUtils.expandToFieldList(e, fullParser, ctx);
		for (IField f : fields) {
			if (FieldUtils.isLikeString(f) || FieldUtils.isTableEnumWithoutTable(f.getType())) {
				continue;
			}
			if (FieldUtils.isEnum(f)) {
				checkEnumType(e, ctx, value, opName, f);
				continue;
			}
			throwException(ParserErrorTypesEnum.MVK_NO_STRING_OR_ENUM, ctx, opName);
		}
	}

	private void checkEnumType(EntityRef e, EntityListeContext ctx, String value, String opName, IField f) {
		IEnumerationType enumType = (IEnumerationType) f.getType();
		if (StringUtils.isNotEmpty(e.getCategoryName())) {
			if (!FieldUtils.isValidCategoryValue(enumType, e.getCategoryName(), value)) {
				String errMessage = getErrorMessage(
						ParserErrorTypesEnum.MVK_INVALID_STRING_CONSTANT_FOR_ENUMERATION_CATEGORY,
						opName, value, e.getCategoryName());
				throw createMVKParserError(ctx.getParent(), errMessage);
			}
		} else if (!FieldUtils.isEnumValueValid(enumType, value)) {
			String enumerationFieldName = e.getEntity().isField() ? e.getNameBuiltFromConditionText() : f.getFullName();
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_STRING_CONSTANT_FOR_ENUMERATION, opName,
					value, enumerationFieldName);
			throw createMVKParserError(ctx.getParent(), errMessage);
		}
	}

	/**
	 * Checks if all entities are repeatable and specify a wildcard at the lowest repetition level.
	 * Wildcards at higher levels are not allowed. Note that for the ExtMP world the USB is
	 * regarded here as a higher repetition level than the root group level (only for simplicity,
	 * generally there is no hierarchy between USB and root group level).
	 *
	 * @param refs
	 * 		references to be checked
	 * @param ctx
	 * 		for nice errorMessages only
	 * @param opName
	 * 		name of operation - for nice error Messages only
	 */
	public void checkAllEntitiesHaveWildcardOnLowestLevel(List<EntityRef> refs, EntityListeContext ctx, String opName) {
		for (EntityRef ref : refs) {
			boolean wildcardFound = false;
			for (int i = ref.getMax().length - 1; i >= 0; i--) {
				if (checkWildcard(wildcardFound, ref.getMax()[i], ref.getIndices().get(i), isWildcardRestricted(ref, i), ctx,
						opName)) {
					wildcardFound = true;
				}
			}
			if (checkWildcard(wildcardFound, ref.getMaxUsb(), ref.getUsb(), false, ctx, opName)) {
				wildcardFound = true;
			}
			if (!wildcardFound) {
				String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_REPEATABLE_LEVEL_REQUIRED, opName);
				throw createMVKParserError(ctx.getParent(), errMessage);
			}
		}
	}

	private boolean isWildcardRestricted(EntityRef ref, int index) {
		if (!(ref instanceof EntityRefForHModel refHModel)) {
			return ref.hasFilterCondition();
		}
		if (index == 0) {
			return refHModel.isLSternEingeschraenkt();
		} else if (index == (ref.getMax().length - 1)) {
			return refHModel.isZSternEingeschraenkt();
		}
		return false;
	}

	/**
	 * Check that there is no entity specifying a semantic index.
	 *
	 * @param refs
	 * 		the entities to test.
	 * @param ctx
	 * 		the context for error position specification.
	 * @param opName
	 * 		the operation name for error reporting.
	 */
	public void checkNoSemanticIndex(List<EntityRef> refs, ParserRuleContext ctx, String opName) {
		CheckEntityUtils checker = new CheckEntityUtils(fullParser);
		refs.forEach(ref -> checker.checkNoSemanticIndex(ref, opName, ctx.getParent()));
	}

	/**
	 * Helper method for
	 * {@link #checkAllEntitiesHaveWildcardOnLowestLevel(List, EntityListeContext, String)}. Checks
	 * if a wildcard is specified (only) for the lowest repeatable layer.
	 *
	 * @param wildcardFound
	 * 		specifies, if a wildcard is set for a lower repetition layer
	 * @param maxIndex
	 * 		max. value for the index
	 * @param indexSpec
	 * 		index specification
	 * @param wildcardRestricted
	 * 		specifies a restricted wildcard (e.g. *(2-3))
	 * @param ctx
	 * 		for nice errorMessages only
	 * @param opName
	 * 		name of operation - for nice error Messages only
	 * @return true if indexSpec is {@link IParseTreeEntity#WILDCARD}, false otherwise
	 */
	private boolean checkWildcard(boolean wildcardFound, long maxIndex, long indexSpec, boolean wildcardRestricted,
			EntityListeContext ctx, String opName) {
		if (!wildcardFound && maxIndex > 1 && (indexSpec != IParseTreeEntity.WILDCARD || wildcardRestricted)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_WILDCARD_AT_LOWEST_LEVEL_REQUIRED, opName);
			throw createMVKParserError(ctx.getParent(), errMessage);
		} else if (wildcardFound && (indexSpec == IParseTreeEntity.WILDCARD)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_WILDCARD_ONLY_AT_LOWEST_LEVEL_ALLOWED, opName);
			throw createMVKParserError(ctx.getParent(), errMessage);
		}
		return (indexSpec == IParseTreeEntity.WILDCARD);
	}

	/**
	 * checks that all entities in the list have the same lfdNR.
	 *
	 * @param refs
	 * 		list for entities to be checked.
	 * @param ctx
	 * 		for nice error messages only
	 * @param opName
	 * 		name of the operation, for nice error messages only
	 */
	public void checkAllFieldsSameL(List<EntityRef> refs, EntityListeContext ctx, String opName) {

		//boxing required because used in the expression below.
		Long[] l = new Long[1];

		refs.forEach(r -> {
			if (l[0] != null && r.getLfdNr() != l[0]) {
				String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_PARAMS_FOR_OP, opName,
						r.getNameBuiltFromConditionText());
				throw createMVKParserError(ctx, errMessage);
			}
			l[0] = r.getLfdNr();
		});
	}

	public void checkFieldsAreYesNoWithSameNoValue(List<EntityRef> refs, ParserRuleContext ctxForErrorMessages, String opName) {
		String value = null;

		for (EntityRef ref : refs) {
			for (IField f : EntityUtils.expandToFieldList(ref, fullParser, ctxForErrorMessages)) {
				String noValue = getNoValue(f, ctxForErrorMessages, opName);

				if (value != null && !value.equals(noValue)) {
					throwException(ParserErrorTypesEnum.MVK_DIFFERENT_NO_VALUE, ctxForErrorMessages, opName);
				}
				value = noValue;
			}
		}
	}

	private String getNoValue(IField f, ParserRuleContext ctxForErrorMessages, String opName) {
		if (FieldUtils.isYesNo(f)) {
			return ((IYesNoType) f.getType()).getFalseValue();
		}
		throwException(ParserErrorTypesEnum.MVK_NO_TYPEYESNO, ctxForErrorMessages, opName);
		return null; //never reached
	}

	/**
	 * checks that, if there is more than one parameter, no root group is referenced
	 *
	 * @param refs
	 * 		list to be checked
	 * @param ctx
	 * 		for nice error messages only
	 * @param opName
	 * 		name of operation
	 */
	public void checkNoRootGroupWhenMoreThanOneParameter(List<EntityRef> refs, EntityListeContext ctx, String opName) {
		if ((refs.size() > 1) && refs.stream().anyMatch(r -> EntityUtils.isRootGroup(r.getEntity().getFullName()))) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_ROOT_GROUP_WITH_OTHER_PARAMETERS, opName);
			throw createMVKParserError(ctx, errMessage);
		}
	}

	/**
	 * checks if one of the references in the list is a root group.
	 *
	 * @param refs
	 * 		list of references to be checked.
	 * @param ctx
	 * 		for nice error messages only.
	 * @param opName
	 * 		name of operation
	 * @param isHModel
	 * 		the error message for H and A12 models is different
	 */
	public void checkNotRootGroupReferenced(List<EntityRef> refs, EntityListeContext ctx, String opName, boolean isHModel) {
		if (refs.stream().anyMatch(r -> EntityUtils.isRootGroup(r.getEntity().getFullName()))) {
			String errMessage = isHModel ? getErrorMessage(ParserErrorTypesEnumForHModel.MVK_ROOT_GROUP_REFERENCED_AS_GROUP)
					: getErrorMessage(ParserErrorTypesEnum.MVK_ROOT_GROUP_REFERENCED, opName);
			throw createMVKParserError(ctx, errMessage);
		}
	}

	public void checkDateRangeWithoutSpecificInterpretationOfYear(List<EntityRef> paramListe,
			EntityListeContext ctxForErrorMessaging, String opName) {
		paramListe.forEach(ref -> EntityUtils.expandToFieldList(ref, getParser(), ctxForErrorMessaging).forEach(f -> {

			if (!FieldUtils.isDateRange(f)) {
				String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_NO_DATE_RANGE,
						opName, ref.getNameBuiltFromConditionText());
				throw createMVKParserError(ctxForErrorMessaging.getParent(), errMessage);
			}

			if (FieldUtils.isDateRangeWithSpecificInterpretationOfYear(f)) {
				//ExtMP-specific format:  thus converting in german format is o.k.
				String formatForErrorMsg = FieldUtils.getDateRangeFormat(f).replace("d", "T");
				String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_DATE_RANGE_FORMAT,
						opName, formatForErrorMsg);
				throw createMVKParserError(ctxForErrorMessaging.getParent(), errMessage);
			}
		}));
	}

	/**
	 * Check for all enumeration values of <code>enumType2</code> that are contained in <code>enumType1</code> (i. e. xml and/or
	 * display value matches) that the display and xml values, respectively, are the same for <code>enumType1</code> and
	 * <code>enumType2</code>. If the enumeration types are not compatible, an {@link Optional} containing an error message is
	 * returned. Examples (notation: "(a=A, b=B)" means an enumeration with xml values a and b and corresponding display values A
	 * and B):
	 * <ul>
	 *     <li>enumerations (a=A, b=B, c=C) and (a=A, b=B, c=C) are compatible (all values match)</li>
	 *     <li>enumerations (a=A, b=B, c=C) and (c=C, d=D, e=E) are compatible (for the common xml value c the display value C
	 *     matches and vice versa)
	 *     </li>
	 *     <li>enumerations (a=A, b=B, c=C) and (d=D, e=E, f=F) are compatible (no common xml or display values - nothing has to
	 *     match) - for calculations this case is forbidden by NormalisierteBerechnungsFormHelper#isEnumValueCompatible</li>
	 *     <li>enumerations (a=A, b=B, c=C1) and (c=C2, d=D, e=E) are incompatible (for the common xml value c the display values
	 *     C1 and C2 do not match)
	 *     </li>
	 *     <li>enumerations (a=A, b=B, c1=C) and (c1=C, d=D, e=E) are incompatible (for the common display value C the xml values
	 *     c1 and c2 do not match)
	 *     </li>
	 * </ul>
	 * <br>
	 * <b>Note:</b> For enumerations with table reference, no check is done.
	 *
	 * @param enumType1
	 * @param enumType2
	 * @return {@link Optional} containing an error message, if the enum types are incompatible, otherwise {@link Optional#empty()}.
	 */
	public static Optional<String> checkDisplayValues(IEnumerationType enumType1, IEnumerationType enumType2,
			ParserErrorHandler errHandler) {
		if (FieldUtils.isTableEnumWithTable(enumType1) || FieldUtils.isTableEnumWithTable(enumType2)) {
			// No detail check for enums with table reference
			return Optional.empty();
		}

		// These checks should not be necessary
		checkXmlValuesUnique(enumType1);
		checkXmlValuesUnique(enumType2);

		// both have display values
		Set<Locale> locales = new HashSet<>(enumType1.getSupportedDisplayLocales());
		locales.retainAll(enumType2.getSupportedDisplayLocales());
		return locales.stream()
				.map(l -> checkEnumValuesForLocale(enumType1, enumType2, l, errHandler))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst();
	}

	private static void checkXmlValuesUnique(IEnumerationType enumType) {
		// This check should not be necessary, because it is checked in the MetaModel, and such values should not be saveable in the editor
		Map<String, Set<String>> duplicates = Java8Utils.findDuplicates(enumType.getXMLValues().stream(), Function.identity());
		if (!duplicates.isEmpty()) {
			throw new IllegalArgumentException(
					String.format("In enumerations, the values need to be distinct. Duplicate Values: %s.",
							duplicates.keySet()));
		}
	}

	private static Optional<String> checkEnumValuesForLocale(IEnumerationType enumType1, IEnumerationType enumType2, Locale locale,
			ParserErrorHandler errHandler) {
		Map<String, String> xmlToUi1 = enumType1.getDisplayValues(locale) == null ? null
				: enumType1.getDisplayValues(locale).stream()
				.collect(Collectors.toMap(EnumerationValue::getXmlValue, EnumerationValue::getDisplayValue));
		Map<String, String> xmlToUi2 = enumType2.getDisplayValues(locale) == null ? null
				: enumType2.getDisplayValues(locale).stream()
				.collect(Collectors.toMap(EnumerationValue::getXmlValue, EnumerationValue::getDisplayValue));
		Optional<String> errMsgOpt = checkSameValues(xmlToUi1, xmlToUi2, errHandler);
		if (errMsgOpt.isPresent()) {
			return errMsgOpt;
		}

		Map<String, String> uiToXml1 = enumType1.getDisplayValues(locale) == null ? null
				: enumType1.getDisplayValues(locale).stream()
				.collect(Collectors.toMap(EnumerationValue::getDisplayValue, EnumerationValue::getXmlValue));
		Map<String, String> uiToXml2 = enumType2.getDisplayValues(locale) == null ? null
				: enumType2.getDisplayValues(locale).stream()
				.collect(Collectors.toMap(EnumerationValue::getDisplayValue, EnumerationValue::getXmlValue));
		return checkSameValues(uiToXml1, uiToXml2, errHandler);
	}

	private static Optional<String> checkSameValues(Map<String, String> map1, Map<String, String> map2,
			ParserErrorHandler errHandler) {
		if (map1 == null || map2 == null) {
			// Can happen only for inconsistent enumerations, not specifying display values for all languages.
			return Optional.empty();
		}
		for (Entry<String, String> entry : map1.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (!map2.containsKey(key)) {
				continue;
			}
			if (!value.equals(map2.get(key))) {
				String errMessage = errHandler.getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_COMPARE_ENUM_TO_ENUM, key, value,
						map2.get(key));
				return Optional.of(errMessage);
			}
		}
		return Optional.empty();
	}
}
