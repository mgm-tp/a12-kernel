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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckEntityListenUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckEntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckWerteListenUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.FieldUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldUndWerteListenBedingungContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldWertOpListeContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.WertListenSpezifier2Context;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEnumerationType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;

import jakarta.validation.constraints.NotNull;

/**
 * Stellt die Prüfungen für <code>FeldUndWerteListenBedingung</code> bereit.
 * Dies entspricht <code>FeldWertInWerteListe</code> und <code>
 * FeldWertNichtInWerteListe</code>
 * <p>
 * es wird folgendes überprüft:
 * <ul>
 * <li>Der erste Parameter muss ein einzelnes Feld spezifizieren.
 * {@link ParserErrorTypesEnum#MVK_NO_GROUPS_ALLOWED},
 * {@link ParserErrorTypesEnum#MVK_NO_WILDCARDS_ALLOWED}
 * <li>Es dürfen nur Strings, Enumerationen oder Zahlen bzw. Geldbeträge als Typ verwendet werden.
 * {@link ParserErrorTypesEnum#MVK_ONLY_STRING_ENUM_NUMBER_ALLOWED}
 * <li>Falls die Werte über Konstanten-liste definiert werden wird geprüft: Ist das Feld eine Enumeration, darf nur mit
 * zulässigen Werten aus der Enumeration bzw. der jeweils spezifizierten Kategorie verglichen werden und es darf zur
 * Spezifikation der Werte nur eine Liste von Strings verwendet werden. Ist das Feld eine Zahl / ein Geldbetrag darf nur eine
 * Liste von Zahlen verwendet werden.
 * {@link ParserErrorTypesEnum#MVK_INVALID_STRING_CONSTANT_FOR_ENUMERATION},
 * {@link ParserErrorTypesEnum#MVK_INVALID_STRING_CONSTANT_FOR_ENUMERATION_CATEGORY},
 * {@link ParserErrorTypesEnum#MVK_INVALID_TYPES_FOR_COMPARISON}
 * <li>Falls die Werte über eine Liste von FeldWert-Operationen geprüft werden, wird geprüft, dass
 * alle Felder der Feldliste den gleichen Typ haben. Dabei wird auch hier nur der Basis-Typ geprüft.
 * {@link ParserErrorTypesEnum#MVK_VARYING_TYPES_NOT_ALLOWED}Es wird geprüft, dass in der Feldliste
 * keine Felder doppelt angegeben wurden. {@link ParserErrorTypesEnum#MVK_DUPLICATE_PARAM1},
 * {@link ParserErrorTypesEnum#MVK_DUPLICATE_PARAM2}</li>
 * </ul>
 * <p>
 * die Überprüfung für eine Werteliste ist analog zu
 * {@link CheckFeldListeUndWerteListenBedingungImpl}
 */
public class CheckFeldUndWerteListenBedingungImpl extends CheckImplAbstractClass {
	public CheckFeldUndWerteListenBedingungImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
	}

	public void check(@NotNull FeldUndWerteListenBedingungContext ctx) {
		String opName = ctx.feldUndWerteListenBedingungName().getText();
		WertListenSpezifier2Context wCtx = ctx.wertListenSpezifier2();

		EntitySpecContext ectx = ctx.entitySpec();
		EntityRef ref = getEntityRef(ectx);

		IField f = checkField(ectx, opName);

		//Check the value list
		if (wCtx.feldWertOpListe() != null) {//value list defined by fields
			checkFelderWerteListe(wCtx.feldWertOpListe(), ref, opName);
		} else {//value list defined by constants
			checkConstantValueList(ctx, ref, f, opName);
		}
	}

	/**
	 * Checks that the given entity specified is one single field of type enum, string or number.
	 * 'noValueValidation' must be set to <code>false</code>.
	 *
	 * @param ectx
	 * 		context of the entity - used for errorMessaging only
	 * @param opName
	 * 		name of the operation, used for error messages
	 * @return field referenced in the condition
	 */
	private IField checkField(EntitySpecContext ectx, String opName) {
		EntityRef ref = getEntityRef(ectx);
		CheckEntityUtils checker = new CheckEntityUtils(fullParser);

		IField f = checker.checkIsField(ref, opName, ectx);
		checker.checkNoWildCards(ref, opName, ectx);
		checker.checkStringEnumOrNumber(ref, opName, ectx);
		checker.checkIsValueValidation(ref, opName, ectx);
		return f;
	}

	/**
	 * Checks the value list defined by constants.
	 * If and only if the field is a number, the list has to be defined by a number list.
	 * If the field is an enum, all constants in the list have to be possible xml values of the
	 * enum.
	 */
	private void checkConstantValueList(FeldUndWerteListenBedingungContext ctx, EntityRef ref, IField f, String opName) {
		if (FieldUtils.isTableEnumWithoutTable(f.getType())) {
			return;
		}
		//If field is a number you must define a number list
		if (FieldUtils.isNumber(f) && ctx.wertListenSpezifier2().numberListe() == null ||
				ctx.wertListenSpezifier2().numberListe() != null && !FieldUtils.isNumber(f)) {
			throwInvalidTypesForComp(ctx, opName);
		}

		//If field is an enum,  check the constants
		if (FieldUtils.isEnum(f)) {
			List<String> stringList = CheckWerteListenUtils.readListe(ctx.wertListenSpezifier2().stringListe());
			IEnumerationType type = (IEnumerationType) f.getType();
			if (StringUtils.isNotEmpty(ref.getCategoryName())) {
				Optional<String> invalidValueOpt = stringList.stream()
						.filter(stringConstant -> !FieldUtils.isValidCategoryValue(type, ref.getCategoryName(), stringConstant))
						.findAny();
				if (invalidValueOpt.isPresent()) {
					String errMessage = getErrorMessage(
							ParserErrorTypesEnum.MVK_INVALID_STRING_CONSTANT_FOR_ENUMERATION_CATEGORY,
							opName, invalidValueOpt.get(), ref.getCategoryName());
					throw createMVKParserError(ctx, errMessage);
				}
			} else {
				Optional<String> invalidValueOpt = stringList.stream()
						.filter(stringConstant -> !FieldUtils.isEnumValueValid(type, stringConstant))
						.findAny();
				if (invalidValueOpt.isPresent()) {
					String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_STRING_CONSTANT_FOR_ENUMERATION, opName,
							invalidValueOpt.get(), ref.getNameBuiltFromConditionText());
					throw createMVKParserError(ctx, errMessage);
				}
			}
		}
	}

	/**
	 * checks the list of the []-operations. Each field may be referenced only once.
	 * All fields referenced by the []-operation must match the same base type.
	 *
	 * @param ctx
	 * 		context specifying the operation
	 * @param eRef
	 * 		field, specifying the base type
	 * @param opName
	 * 		used only for error messages
	 */
	private void checkFelderWerteListe(FeldWertOpListeContext ctx, EntityRef eRef, String opName) {
		CheckEntityListenUtils checker = new CheckEntityListenUtils(fullParser);

		IField f = (IField) eRef.getEntity();

		List<EntityRef> valueListRefs = ctx.feldWertOp().stream()
				.map(c -> getEntityRef(c.entitySpec()))
				.collect(Collectors.toList());

		List<EntityRef> allRefs = new ArrayList<>(valueListRefs);
		allRefs.add(eRef);

		checker.checkParamsReferencedOnlyOnce(allRefs, ctx, opName);
		checker.matchBaseType(valueListRefs, ctx, f.getType(), opName);
		checker.checkAllAreValueValidation(valueListRefs, ctx, opName);
	}

	private void throwInvalidTypesForComp(FeldUndWerteListenBedingungContext ctx, String opName) {
		String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_TYPES_FOR_COMPARISON, opName);
		throw createMVKParserError(ctx, errMessage);
	}
}
