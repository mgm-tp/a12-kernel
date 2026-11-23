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

import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.error.MVKParserError;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.SemanticIndexContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.languages.ConditionMultiLanguageHandler;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.util.EntityIdFormatterUtil;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IParseTreeEntity;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.PathNotationConfig;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEnumerationType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IGroup;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IRule;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IStringType;

/**
 * Stellt Hilfsmethoden bereit für Checks auf einer Entity.
 */
public class CheckEntityUtils extends CheckImplAbstractClass {
	public CheckEntityUtils(FullChecks_ConditionParser parser) {
		super(parser);
	}

	/**
	 * Checks if the given entity is a field.
	 *
	 * @param ref
	 * 		entity to check
	 * @param opName
	 * 		used for error handling only
	 * @param ctxForErrorMessages
	 * 		used for error handling only
	 * @return the field
	 */
	public IField checkIsField(EntityRef ref, String opName, ParserRuleContext ctxForErrorMessages) {
		if (!ref.getEntity().isField()) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_NO_GROUPS_ALLOWED, opName);
			throw createMVKParserError(ctxForErrorMessages, errMessage);
		}
		return (IField) ref.getEntity();
	}

	/**
	 * Checks if the given entity is a group.
	 *
	 * @param ref
	 * 		entity to check
	 * @param opName
	 * 		used for error handling only
	 * @param ctxForErrorMessages
	 * 		used for error handling only
	 * @return the group
	 */
	public IGroup checkIsGroup(EntityRef ref, String opName, ParserRuleContext ctxForErrorMessages) {
		if (!ref.getEntity().isGroup()) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_NO_FIELDS_ALLOWED, opName,
					ref.getNameBuiltFromConditionText());
			throw createMVKParserError(ctxForErrorMessages, errMessage);
		}
		return (IGroup) ref.getEntity();
	}

	/**
	 * checks that the entity is a number field or (if it is a group) contains only number fields.
	 *
	 * @param ref
	 * 		entity to check
	 * @param opName
	 * 		used for errorhandling only
	 * @param ctx
	 * 		used for errorhandling only
	 * @param errType
	 * 		e.g. ParserErrorTypesEnum.MVK_NO_NUMBER
	 */
	public void checkIsNumber(EntityRef ref, String opName, ParserRuleContext ctx, ParserErrorTypesEnum errType) {
		List<IField> fields = EntityUtils.expandToFieldList(ref, fullParser, ctx);
		for (IField f : fields) {
			if (!FieldUtils.isNumber(f)) {
				String errMessage = getErrorMessage(errType, opName, ref.getNameBuiltFromConditionText());
				throw createMVKParserError(ctx, errMessage);
			}
		}
	}

	public void checkIsLikeStringOrEnum(EntityRef ref, String opName, ParserRuleContext ctx, ParserErrorTypesEnum errType) {
		List<IField> fields = EntityUtils.expandToFieldList(ref, fullParser, ctx);
		for (IField f : fields) {
			if (!FieldUtils.isLikeString(f) && !FieldUtils.isEnum(f)) {
				String errMessage = getErrorMessage(errType, opName, ref.getNameBuiltFromConditionText());
				throw createMVKParserError(ctx, errMessage);
			}
		}
	}

	/**
	 * checks if the given entity can be converted to a number.
	 *
	 * @param e
	 * 		entity to check
	 * @param opName
	 * 		used for error handling only
	 * @param ctx
	 * 		used for error handling only
	 */
	public void checkIsConvertableToNumber(EntityRef e, String opName, ParserRuleContext ctx) {
		IField f = checkIsField(e, opName, ctx);
		if (f.getType() instanceof IStringType t) {
			checkStringIsConvertableToNumber(e, opName, ctx, t);
			return;
		}
		if (f.getType() instanceof IEnumerationType t) {
			checkEnumerationIsParsableToNumber(e, opName, (EntitySpecContext) ctx, t);
			return;
		}
		throwErrorNoConvertInNumber(opName, e, ctx);

	}

	private void checkEnumerationIsParsableToNumber(EntityRef ref, String opName, EntitySpecContext ctx,
			IEnumerationType enumType) {
		if (ctx.categoryName != null && !StringUtils.isEmpty(ctx.categoryName.getText())) {
			checkParsableToNumber(FieldUtils.getCategoryValues(enumType, ctx.categoryName.getText()), opName, ref, ctx);
		} else {
			checkParsableToNumber(FieldUtils.getDistinctEnumValues(enumType), opName, ref, ctx);
		}
	}

	private void checkParsableToNumber(Set<String> values, String opName, EntityRef ref, EntitySpecContext ctx) {
		for (String v : values) {
			if (v.replace(".", "").replace("-", "").length() > MAX_ALLOWED_DIGITS
					|| !numberUtilsParsableAs320(v)) {
				throwErrorNoConvertInNumber(opName, ref, ctx);
			}
		}
	}

	/**
	 *
	 * the a behavioural change from org.apache.commons:commons-lang 3.20.0 -> 3.21.0 caused out of bounds exceptions
	 * so this method retains 3.20 behaviour (as minimal fix)
	 *
	 */
	private boolean numberUtilsParsableAs320(String str){
		if (StringUtils.isEmpty(str)) {
			return false;
		}
		if (str.charAt(str.length() - 1) == '.') {
			return false;
		}
		return NumberUtils.isParsable(str);
	}

	private void checkStringIsConvertableToNumber(EntityRef ref, String opName, ParserRuleContext ctx, IStringType t) {
		if (t.getMaxLength() > MAX_ALLOWED_DIGITS || t.getMaxLength() == -1 || !"[0-9]+".equals(t.getPattern())) {
			throwErrorNoConvertInNumber(opName, ref, ctx);
		}
	}

	/**
	 * checks if the field is a date and that a date fragment of the field may be unknown
	 *
	 * @param ref
	 * 		used for error handling only
	 * @param opName
	 * 		used for error handling only
	 * @param ctx
	 * 		used for error handling only
	 * @param f
	 * 		field to check
	 */
	public void checkIsPartialKnown(EntityRef ref, String opName, ParserRuleContext ctx, IField f) {
		if (!FieldUtils.isDatePartialKnown(f)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_DATE_TYPE_FOR_OP, opName,
					ref.getNameBuiltFromConditionText());
			throw createMVKParserError(ctx, errMessage);
		}
	}

	private void throwErrorNoConvertInNumber(String opName, EntityRef ref, ParserRuleContext ctx) {
		String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_NO_CONVERT_IN_NUMBER, opName,
				ref.getNameBuiltFromConditionText());
		throw createMVKParserError(ctx, errMessage);
	}

	public void checkIsString(EntityRef ref, String opName, ParserRuleContext ctx) {
		IField f = checkIsField(ref, opName, ctx);

		if (!FieldUtils.isLikeString(f)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_NO_STRING, opName);
			throw createMVKParserError(ctx, errMessage);
		}
	}

	public void checkNoWildCards(EntityRef ref, String opName, ParserRuleContext ctx) {
		if (ref.hasWildCards()) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_NO_WILDCARDS_ALLOWED, opName);
			throw createMVKParserError(ctx, errMessage);
		}
	}

	public void checkNoSemanticIndex(EntityRef ref, String opName, ParserRuleContext ctx) {
		if (ref.getSemanticIndex() != null) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_SEMANTIC_INDEX_NOT_ALLOWED, opName);
			throw createMVKParserError(ctx, errMessage);
		}
	}

	public void checkRepeatableOnLowestLevel(EntityRef groupRef, ParserRuleContext ctx) {
		if (!EntityUtils.isRepeatableOnLowestLevel(groupRef.getEntity())) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_GROUP_NOT_REPEATABLE,
					groupRef.getNameBuiltFromConditionText());
			throw createMVKParserError(ctx, errMessage);
		}
	}

	public void checkHasWildCards(EntityRef ref, String opName, ParserRuleContext ctx) {
		if (!ref.hasWildCards()) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_WILDCARDS_REQUIRED, opName);
			throw createMVKParserError(ctx, errMessage);
		}
	}

	/**
	 * Checks if a string, enumeration or number field is referenced. Predefined types or extensible
	 * enumerations are treated like strings.
	 *
	 * @param ref
	 * 		entity to check
	 * @param opName
	 * 		used for error handling only
	 * @param ctx
	 * 		used for error handling only
	 */
	public void checkStringEnumOrNumber(EntityRef ref, String opName, ParserRuleContext ctx) {
		IField f = (IField) ref.getEntity();
		if (!FieldUtils.isStringEnumOrNumber(f)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_ONLY_STRING_ENUM_NUMBER_ALLOWED, opName,
					ref.getNameBuiltFromConditionText());
			throw createMVKParserError(ctx, errMessage);
		}
	}

	/**
	 * checks that the given field is an enumeration and specifies the given category.
	 * Throws an exception in case of error. Caller has to make sure that ref references a field.
	 *
	 * @param ref
	 * 		reference to a field
	 * @param categoryName
	 * 		name of the category
	 * @param ctx
	 * 		used for error handling only
	 */
	public void checkIsCorrectCategory(EntityRef ref, String categoryName, ParserRuleContext ctx) {
		if (fullParser.isLenient()) {
			assert false : "not expected to be called from lenient parser";
			return;
		}
		// check that the field is an enumeration
		IField f = (IField) ref.getEntity();
		if (!FieldUtils.isEnum(f)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_NO_ENUMERATION, ref.getNameBuiltFromConditionText());
			throw createMVKParserError(ctx, errMessage);
		}

		// check that the category is a known category
		IEnumerationType t = (IEnumerationType) f.getType();
		if (!t.getCategoryValues().containsKey(categoryName)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_CATEGORY_UNKOWN, categoryName);
			throw createMVKParserError(ctx, errMessage);
		}
	}

	/**
	 * for any of the given fields that is a string, this method checks that the option
	 * 'noValueValidation' is set to <code>false</code>.
	 *
	 * @param ref
	 * 		reference to a field, or list of fields
	 * @param opName
	 * 		name of the predicate
	 * @param ctx
	 * 		used for error handling only
	 */
	public void checkIsValueValidation(EntityRef ref, String opName, ParserRuleContext ctx) {
		if (fullParser.isLenient()) {
			return;
		}
		List<IField> fields = EntityUtils.expandToFieldList(ref, fullParser, ctx);
		for (IField f : fields) {
			if (FieldUtils.isNoValueValidation(f)) {
				String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_RAW_TYPE, opName);
				throw createMVKParserError(ctx, errMessage);
			}
		}
	}

	/**
	 * Checks that the given field is NOT an enumeration with table reference.
	 *
	 * @param ref
	 * 		the field reference.
	 * @param opName
	 * 		the name of the operation or predicate. Used in the error text.
	 * @param ctx
	 * 		the ANTLR-context used for error handling.
	 */
	public void checkNoTableEnum(EntityRef ref, String opName, ParserRuleContext ctx) {
		IField f = (IField) ref.getEntity();
		if (FieldUtils.isTableEnum(f.getType())) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_TABLE_ENUM, opName);
			throw createMVKParserError(ctx, errMessage);
		}
	}

	/**
	 * Checks that the reference is an enumeration with table reference.
	 *
	 * @param field
	 * 		the field to be checked
	 * @param opName
	 * 		the name of the operation or predicate. Used in the error text.
	 * @param ctx
	 * 		the ANTLR-context used for error handling.
	 */
	public void checkEnumWithTableReference(IField field, String opName, ParserRuleContext ctx) {
		if (!FieldUtils.isTableEnum(field.getType())) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_TABLE_ENUM_EXPECTED, opName);
			throw createMVKParserError(ctx, errMessage);
		}
	}

	/**
	 * checks if the root group is repeatable
	 *
	 * @param e
	 * 		entity of the root group
	 */
	public void checkRootGroupIsRepeatable(IEntity e, String opName, ParserRuleContext ctx) {
		if (EntityUtils.getMaxLfdNr(e) == 1 && EntityUtils.getMaxUsb(e) == IParseTreeEntity.UNSPECIFIED) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_CONDITION_WITHOUT_REPEATABLE_ROOTGROUP, opName);
			throw createMVKParserError(ctx, errMessage);
		}
	}

	/**
	 * checks that the entity spec does not define a category
	 *
	 * @param ctx
	 * 		context to be analysed
	 * @param opName
	 * 		name of the operation - used for the error message
	 */
	public void checkNoCategories(EntitySpecContext ctx, String opName) {
		if (ctx.categoryName != null && !StringUtils.isEmpty(ctx.categoryName.getText())) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_CATEGORY_NOT_ALLOWED, opName);
			throw createMVKParserError(ctx.getParent(), errMessage);
		}
	}

	/**
	 * Checks that the indices of the entity are correct.<br>
	 * Checks that
	 * <ul>
	 * <li>if there is iteration, check if the error field is in the rule group. If
	 * not, report {@link ParserErrorTypesEnum#MVK_ERROR_FIELD_NOT_IN_RULEGROUP}</li>
	 * <li>no iteration is specified below a wildcard</li>
	 * <li>no iteration is specified below the error field path</li>
	 * <li>no iteration is specified outside the error field path, unless parallel iteration is
	 * allowed</li>
	 * <li>no semantic index is specified below a wildcard</li>
	 * </ul>
	 */
	public void checkIndicesForA12Model(EntityRef ref, ParserRuleContext errorCtx) {
		if (fullParser.isInFilterCondition() && !ref.isOuter()) {
			//the indices in the filter condition are checked directly in CheckEntitySpecImpl
			return;
		}
		List<Long> indices = ref.getIndices();
		boolean isWildCardSet = false;
		for (int i = 0; i < indices.size(); i++) {
			long index = indices.get(i);
			checkIterationAndErrorField(ref, errorCtx, isWildCardSet, i, index);
			isWildCardSet = checkWildcardAndSemanticIndex(ref, errorCtx, isWildCardSet, index);
		}
	}

	private boolean checkWildcardAndSemanticIndex(EntityRef ref, ParserRuleContext errorCtx, boolean isWildCardSet, long index) {
		if (index == IParseTreeEntity.WILDCARD) {
			return true;
		}
		if (index == IParseTreeEntity.SEMANTIC_INDEX && isWildCardSet) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_SEMANTIC_INDEX_BELOW_WILDCARD,
					ref.getNameBuiltFromConditionText());
			throw createMVKParserError(errorCtx, errMessage);
		}
		return isWildCardSet;
	}

	private void checkIterationAndErrorField(EntityRef ref, ParserRuleContext errorCtx, boolean isWildCardSet,
			int level, long index) {
		if (index != IParseTreeEntity.ITERATION) {
			return;
		}
		// We have iteration, so check the error field in the rule context first
		checkErrorFieldInRuleContext(errorCtx);

		IEntity errorEntity = getErrorEntity();
		String fullGroupName = EntityUtils.getPathUpToLevel(level, ref.getEntity().getFullName());
		IEntity group = getEntity(fullGroupName);

		if (isWildCardSet ||
				EntityUtils.isGroupBelowErrorEntityPath(group, errorEntity) ||
				(!EntityUtils.isGroupAboveOrEqualErrorEntityPath(group, errorEntity)
						&& !checkParallelIteration(group, errorCtx, ref.getPathNotationConfig()))) {
			if (isSemanticIndex(errorCtx)) {
				String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_SEMANTIC_INDEX_INVALID_ITER,
						ref.getNameBuiltFromConditionText());
				throw createMVKParserError(errorCtx, errMessage);
			} else {
				ConditionMultiLanguageHandler mlh = new ConditionMultiLanguageHandler(fullParser.getLanguageUsedInConditionText(),
						fullParser.getPss());
				String errParam = EntityIdFormatterUtil.formatEntityForCondationParserMessageA12DV(getRule().getFullName(),
						fullGroupName, ref.getIndices(),
						ref.getPathNotationConfig(), mlh);
				String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_NO_WILDCARD, errParam,
						ref.getNameBuiltFromConditionText());
				throw createMVKParserError(errorCtx, errMessage);
			}
		}
	}

	/**
	 * Throws a {@link MVKParserError} if the error field is not in the rule group
	 * ({@link ParserErrorTypesEnum#MVK_ERROR_FIELD_NOT_IN_RULEGROUP}). The check is called if there
	 * is iteration in a rule in an A12 model (via an entity or 'RuleGroup' reference) and if the
	 * entity reference is not in a filter condition.
	 */
	private void checkErrorFieldInRuleContext(ParserRuleContext errorCtx) {
		IRule rule = fullParser.getRule();
		//is the errorField in the RuleGroup?
		String rulePathWithSlash = EntityUtils.getParentPathWithSlash(rule.getFullName());
		String errorFieldFullName = rule.getErrorEntity().getFullName();
		if (!errorFieldFullName.startsWith(rulePathWithSlash)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_ERROR_FIELD_NOT_IN_RULEGROUP, errorFieldFullName);
			throw createMVKParserError(errorCtx, errMessage);
		}
	}

	/**
	 * Checks if parallel iteration is allowed for an A12 model:
	 *
	 * <ol>
	 * <li>Spezifiziert die Wiederholungsebene ein Indexfeld?<br>
	 * Falls nein -> Fehler {@link ParserErrorTypesEnum#MVK_NO_WILDCARD} (Fehler wird in aufrufender
	 * Methode gemeldet)</li>
	 * <li>Falls ja: Spezifiziert der Pfad des Fehlerfeldes ein Indexfeld?<br>
	 * Falls nein -> Fehler {@link ParserErrorTypesEnum#MVK_ERROR_FIELD_NO_INDEX}<br>
	 * Falls ja, aber Indexfeld hat einen anderen Namen -> Fehler
	 * {@link ParserErrorTypesEnum#MVK_ERROR_FIELD_INVALID_INDEX}<br>
	 * Falls ja, aber Indexfeld hat einen anderen Typ -> Fehler
	 * {@link ParserErrorTypesEnum#MVK_ERROR_FIELD_INVALID_INDEX_TYPE}</li>
	 * <li>Spezifiziert einer der beiden Pfade mehr als ein Indexfeld?<br>
	 * Falls ja -> Fehler {@link ParserErrorTypesEnum#MVK_MULTIPLE_INDEX_FIELDS2}</li>
	 * </ol>
	 *
	 * @param referencedEntity
	 * 		entity that is not on the error field path and is referenced with iteration
	 * @return true if parallel iteration is allowed for <code>referencedEntity</code>, false
	 * 		otherwise
	 */
	private boolean checkParallelIteration(IEntity referencedEntity, ParserRuleContext errorCtx,
			PathNotationConfig pathNotationConfig) {
		if (!(referencedEntity instanceof IGroup group)) {
			return false;
		}
		if (group.getIndexFieldNames().isEmpty()) {
			return false; // error handling in calling method
		}
		IGroup errorFieldParentGroup = EntityUtils.getParentGroup(getErrorEntity(), getParser().getEc());
		IField indexField1 = getUniqueIndexField(group, group, errorCtx);
		IField indexField2 = getUniqueIndexField(errorFieldParentGroup, getErrorEntity(), errorCtx);

		ConditionMultiLanguageHandler mlh = new ConditionMultiLanguageHandler(fullParser.getLanguageUsedInConditionText(),
				fullParser.getPss());
		if (indexField2 == null) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_ERROR_FIELD_NO_INDEX,
					EntityIdFormatterUtil.formatEntityForCondationParserMessageA12DV(getRule().getFullName(), group.getFullName(),
							pathNotationConfig, mlh));
			throw createMVKParserError(errorCtx, errMessage);
		}
		String indexFieldName1 = (indexField1 != null) ? EntityUtils.getShortName(indexField1.getFullName()) : null;
		String indexFieldName2 = EntityUtils.getShortName(indexField2.getFullName());
		if (!indexFieldName2.equals(indexFieldName1)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_ERROR_FIELD_INVALID_INDEX,
					EntityIdFormatterUtil.formatEntityForCondationParserMessageA12DV(getRule().getFullName(), group.getFullName(),
							pathNotationConfig, mlh));
			throw createMVKParserError(errorCtx, errMessage);
		}

		if (!indexField1.getType().matchBaseType(indexField2.getType())) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_ERROR_FIELD_INVALID_INDEX_TYPE,
					EntityIdFormatterUtil.formatEntityForCondationParserMessageA12DV(getRule().getFullName(), group.getFullName(),
							pathNotationConfig, mlh));
			throw createMVKParserError(errorCtx, errMessage);
		}

		return true;
	}

	/**
	 * @param group
	 * @param referencedEntity
	 * 		referenced entity that lies in the group (used for error message)
	 * @return null if there is no index field, the index field if there is exactly one
	 * 		index field on the path of <code>group</code>, otherwise a parser error is thrown
	 */
	private IField getUniqueIndexField(IGroup group, IEntity referencedEntity, ParserRuleContext errorCtx) {
		IGroup indexGroup = getIndexGroup(getParser().getEc(), errorCtx, group, referencedEntity.getFullName());
		return indexGroup != null ?
				getIndexField(getParser().getEc(), indexGroup.getIndexFieldNames().iterator().next(), indexGroup,
						referencedEntity.getFullName(), errorCtx) :
				null;
	}

	/**
	 * @param ec
	 * 		IEC containing the entity
	 * @param errorCtx
	 * 		used for error message
	 * @param entity
	 * 		index field is searched in the groups on the path of the specified entity
	 * @param referencedEntity
	 * 		name of the referenced entity that lies in the group of the index field
	 * 		(used for error message)
	 * @return the group of the index field if there is exactly one group with index fields (if
	 * 		<code>allowMultipleIndexFieldsForOneGroup</code> is set to false there has to be
	 * 		exactly one index field), otherwise null
	 *
	 * @throws MVKParserError
	 * 		if there are multiple index fields in several groups (or in one group if
	 * 		<code>allowMultipleIndexFieldsForOneGroup</code> is set to false)
	 */
	public IGroup getIndexGroup(IEC ec, ParserRuleContext errorCtx, IEntity entity, String referencedEntity) {
		IGroup indexGroup = null;
		while (entity != null) {
			if (entity.isGroup() && !((IGroup) entity).getIndexFieldNames().isEmpty()) {
				if (indexGroup != null) {
					String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_MULTIPLE_INDEX_FIELDS, referencedEntity);
					throw createMVKParserError(errorCtx, errMessage);
				}
				if (((IGroup) entity).getIndexFieldNames().size() > 1) {
					String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_MULTIPLE_INDEX_FIELDS2, referencedEntity);
					throw createMVKParserError(errorCtx, errMessage);
				}
				indexGroup = (IGroup) entity;
			}
			entity = EntityUtils.getParentGroup(entity, ec);
		}
		return indexGroup;
	}

	/**
	 * @param ec
	 * 		IEC containing the index field
	 * @param indexFieldName
	 * 		name of the index field
	 * @param indexGroup
	 * 		group of the index field
	 * @param referencedEntity
	 * 		name of the referenced entity that lies in the group of the index field
	 * 		(used for error message)
	 * @param errorCtx
	 * 		used for error message
	 * @return index field if existing, otherwise an error is thrown
	 *
	 * @throws MVKParserError
	 * 		if the index field could not be found
	 */
	public IField getIndexField(IEC ec, String indexFieldName, IGroup indexGroup, String referencedEntity,
			ParserRuleContext errorCtx) {
		IEntity indexEntity = EntityUtils.getEntityInGroup(ec, indexFieldName, indexGroup.getFullName());
		if (indexEntity == null || !indexEntity.isField()) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INDEX_FIELD_INVALID, referencedEntity, indexFieldName);
			throw createMVKParserError(errorCtx, errMessage);
		}
		return (IField) indexEntity;
	}

	/**
	 * Returns, whether an enclosing context is a SemanticIndexContext.
	 *
	 * @param context
	 * @return
	 */
	public boolean isSemanticIndex(ParserRuleContext context) {
		// Any SemanticIndex on the path to the root of the parsetree => its part of a semantic index
		ParserRuleContext semIdxSearchContext = context;
		while (semIdxSearchContext != null) {
			if (semIdxSearchContext instanceof SemanticIndexContext) {
				return true;
			}
			semIdxSearchContext = semIdxSearchContext.getParent();
		}
		return false;
	}
}
