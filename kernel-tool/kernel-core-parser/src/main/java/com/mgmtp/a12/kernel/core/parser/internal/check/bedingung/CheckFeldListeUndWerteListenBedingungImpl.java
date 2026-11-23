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
package com.mgmtp.a12.kernel.core.parser.internal.check.bedingung;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckEntityListenUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckWerteListenUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.FieldUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntityListeContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldListeUndWerteListenBedingungContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.WertListenSpezifierContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IFieldType;

import jakarta.validation.constraints.NotNull;

/**
 * Stellt die Prüfungen für <code>FeldListeUndWerteListenBedingung</code> bereit.
 * Dies entspricht <code>NichtAlleFeldWerteInWerteListe</code> und
 * <code>KeinFeldWertInWerteList</code>
 * Falls die angegebenen Parameterlisten Kontexte enthalten, so werden jeweils alle Felder des
 * Kontextes betrachtet. Dies gilt sowohl für die Werte- als auch für die Feld-Liste.
 * <p>
 * Die Werteliste kann durch eine Liste von String-Konstanten oder durch eine Entity-Liste
 * spezifiziert werden.
 *
 * <ul>
 * <li>falls Werte-Liste eine <b>Liste von Entities</b> ist, wird geprüft:
 * <p>
 * dass in der Vereinigungsmenge von Werte-Liste und Feldliste keine Felder doppelt angegeben wurden
 * {@link ParserErrorTypesEnum#MVK_DUPLICATE_PARAM1},
 * {@link ParserErrorTypesEnum#MVK_DUPLICATE_PARAM2} und
 * alle Felder vom gleichen Basis-Typ sind.
 * {@link ParserErrorTypesEnum#MVK_VARYING_TYPES_NOT_ALLOWED}
 * Als Basistypen sind nur Strings, Enums oder Zahlen /Geldbeträge erlaubt.
 * {@link ParserErrorTypesEnum#MVK_ONLY_STRING_ENUM_NUMBER_ALLOWED}
 * <p>
 * <b>Anmerkung</b>: für den Fall Felder und Werte-Listen sind Enumerationen: es wird nicht mehr
 * geprüft, dass alle Werte der Enumeration der Werte-Liste in den Enumerationen der Felder-Listen
 * enthalten sind. Diese Überprüfung könnte ja zu streng sein!</li>
 * <li>falls Werte-Liste eine <b>Liste von String-Konstanten</b> ist, wird geprüft:
 * <p>
 * Alle Felder der Feldliste müssen vom Typ String oder Enumerationen sein.
 * {@link ParserErrorTypesEnum#MVK_INVALID_TYPES_FOR_COMPARISON}
 * <br>Für Typ Enumeration wird zusätzlich geprüft, dass jede String-Konstanten einem gültigen Wert mindestens einer
 * Enumeration bzw. bei Kategorienzugriff der jeweils verwendeten Kategorie entspricht.
 * {@link ParserErrorTypesEnum#MVK_INVALID_STRING_CONSTANT_FOR_ENUMERATION_OR_CATEGORY}
 * <p>
 * Es wird geprüft, dass alle Felder der Feldliste den gleichen Typ haben. Dabei wird auch hier nur
 * der Basis-Typ geprüft. {@link ParserErrorTypesEnum#MVK_VARYING_TYPES_NOT_ALLOWED}Es wird geprüft,
 * dass in der Feldliste keine Felder doppelt angegeben wurden.
 * {@link ParserErrorTypesEnum#MVK_DUPLICATE_PARAM1},
 * {@link ParserErrorTypesEnum#MVK_DUPLICATE_PARAM2}</li>
 * </ul>
 */
public class CheckFeldListeUndWerteListenBedingungImpl extends CheckImplAbstractClass {
	public CheckFeldListeUndWerteListenBedingungImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(@NotNull FeldListeUndWerteListenBedingungContext ctx) {
		CheckEntityListenUtils checker = new CheckEntityListenUtils(fullParser);
		String opName = ctx.feldListeUndWerteListenBedingungName().getText();

		EntityListeContext elCtx = ctx.entityListe();
		List<EntityRef> fields = checker.getRefList(elCtx, opName);

		checker.checkNotRootGroupReferenced(fields, elCtx, opName, fullParser.isHModel());

		//check all fields are of the same base types, and only strings, enums or numbers.
		//predefined types and extensible enumerations are regarded as strings.
		checker.checkSameBaseType(fields, elCtx, opName);

		Token bedingungToken = ctx.feldListeUndWerteListenBedingungName().start;
		if (bedingungToken.getType() == GrammarOnly_ConditionParser.MINDESTENS_EIN_DATUMSBEREICH_UEBERLAPPEND) {
			if (fields.size() != 1 || fields.get(0).getEntity().isGroup() || fields.get(0).hasWildCards()) {
				String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_PARAMETER_FOR_DATE_RANGE_COMPARISON, opName);
				throw createMVKParserError(ctx, errMessage);
			}
		} else {
			checker.checkOnlyStringEnumNumber(fields, elCtx, opName);
		}

		checker.checkAllAreValueValidation(fields, elCtx, opName);

		WertListenSpezifierContext wCtx = ctx.wertListenSpezifier();

		//the values are either specified as a string list, or as an entity list.
		if (wCtx.stringListe() != null) {// values are specified by a String List

			if (bedingungToken.getType() == GrammarOnly_ConditionParser.MINDESTENS_EIN_DATUMSBEREICH_UEBERLAPPEND) {
				String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_NO_CONST, ctx.getStart().getText());
				throw createMVKParserError(ctx, errMessage);
			}

			List<String> werteListe = CheckWerteListenUtils.readListe(wCtx.stringListe());
			checkValuesSpecifiedByStringList(ctx, fields, werteListe);
		} else {// values are specified by an entity list.
			checkValuesSpecifiedByEntityList(bedingungToken, fields, wCtx, elCtx, opName);
		}
	}

	/**
	 * check the value list which is specified by the given felder-List
	 * check that
	 * <ul>
	 * <li>all entities match the same base type
	 * <li>the field list and the value list have no common members and that there are no
	 * duplicates.
	 * </ul>
	 * In case of an error, an exception is thrown.
	 *
	 * @param bedingungToken
	 * 		the token specifying the condition type
	 * @param refs
	 * 		list of references specifying the fields
	 * @param wCtx
	 * 		context specifying the values; grammar assures that the list is not empty
	 * @param ctxForErrorMessages
	 * 		context needed for nice error messaging
	 * @param opName
	 * 		name of operation - needed for nice error messaging.
	 */
	private void checkValuesSpecifiedByEntityList(Token bedingungToken, List<EntityRef> refs, WertListenSpezifierContext wCtx,
			EntityListeContext ctxForErrorMessages, String opName) {
		CheckEntityListenUtils checker = new CheckEntityListenUtils(fullParser);
		EntityListeContext elCtx = wCtx.entityListe();
		List<EntityRef> values = checker.getRefList(elCtx, opName);
		IFieldType expectedBaseType = getFirstField(refs, wCtx).getType();

		checker.checkNotRootGroupReferenced(values, elCtx, opName, fullParser.isHModel());

		//check the base types
		checker.matchBaseType(values, ctxForErrorMessages, expectedBaseType, opName);

		//check that fields and values have no duplicates
		List<EntityRef> temp = new ArrayList<>(refs);
		temp.addAll(values);

		checker.checkParamsReferencedOnlyOnce(temp, wCtx, opName);
		checker.checkAllAreValueValidation(values, elCtx, opName);

		if (bedingungToken.getType() == GrammarOnly_ConditionParser.MINDESTENS_EIN_DATUMSBEREICH_UEBERLAPPEND) {
			//check all fields are date range plus
			checker.checkDateRangeWithoutSpecificInterpretationOfYear(temp, ctxForErrorMessages, opName);

			checker.checkDateRangeConsistentRegardingYear(temp, ctxForErrorMessages, opName);
		}
	}

	private void checkValuesSpecifiedByStringList(FeldListeUndWerteListenBedingungContext ctx, List<EntityRef> refs,
			List<String> stringListe) {
		// we have to check only the first field,  as all fields are of the same type.
		IField f = getFirstField(refs, ctx);
		if (FieldUtils.isNumber(f)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_TYPES_FOR_COMPARISON, ctx.getStart().getText());
			throw createMVKParserError(ctx, errMessage);
		}
		if (FieldUtils.isEnum(f)) {
			checkEnumValuesForAllFields(refs, stringListe, ctx);
		}
	}

	/**
	 * checks if the list of values are valid enum xml or category values for the given entities
	 *
	 * @param refs
	 * 		enum fields
	 * @param valueList
	 * 		values to be checked
	 * @param ctxForErrorMessaging
	 * 		context needed for nice error messages
	 */
	private void checkEnumValuesForAllFields(List<EntityRef> refs, Collection<String> valueList,
			ParserRuleContext ctxForErrorMessaging) {

		//get all xml values from all fields
		List<IField> fields = EntityUtils.expandToFieldList(refs, getParser(), ctxForErrorMessaging);
		if (fields.stream().anyMatch(f -> FieldUtils.isTableEnumWithoutTable(f.getType()))) {
			// No checks if any of the enumerations is an enumeration with table reference without table spec.
			return;
		}
		List<EntityRef> noCategoryAccess = refs.stream()
				.filter(entityRef -> entityRef.getCategoryName() == null)
				.toList();
		List<IField> fieldsWithoutCategoryAccess = EntityUtils.expandToFieldList(noCategoryAccess, getParser(),
				ctxForErrorMessaging);
		// get all xml values from all fields without category access
		Set<String> xmlValues = fieldsWithoutCategoryAccess.stream()
				.flatMap(f -> FieldUtils.getDistinctEnumValues(f.getType()).stream())
				.collect(Collectors.toSet());
		Map<String, List<EntityRef>> refsWithCategoryAccess = refs.stream()
				.filter(entityRef -> entityRef.getCategoryName() != null)
				.collect(Collectors.groupingBy(EntityRef::getCategoryName));

		Set<String> categoryValues = new HashSet<>();
		refsWithCategoryAccess.keySet()
				.forEach(categoryName -> addCategoryValues(refsWithCategoryAccess.get(categoryName), categoryName,
						categoryValues, ctxForErrorMessaging));

		CheckWerteListenUtils.checkEnumValues(xmlValues, categoryValues, valueList, ctxForErrorMessaging, this);
	}

	private void addCategoryValues(List<EntityRef> entityRefs, String categoryName, Set<String> categoryValues,
			ParserRuleContext ctxForErrorMessaging) {
		List<IField> fieldsWithCategoryAccess = EntityUtils.expandToFieldList(entityRefs, getParser(), ctxForErrorMessaging);
		categoryValues.addAll(fieldsWithCategoryAccess.stream()
				.flatMap(f -> FieldUtils.getCategoryValues(f.getType(), categoryName).stream())
				.collect(Collectors.toSet()));
	}

	/**
	 * get the first field from the list of references.
	 * As the first entity may be a group, the first entry of the list must be extended.
	 * The grammar assures, that the list is not empty.
	 *
	 * @param refs
	 * 		list of references
	 * @return first field specified via the list
	 */
	private IField getFirstField(List<EntityRef> refs, ParserRuleContext ctx) {
		return EntityUtils.expandToFieldList(refs.get(0), getParser(), ctx).get(0);
	}
}
