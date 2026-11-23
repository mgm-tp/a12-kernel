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

import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplAbstractClass;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.DatumAngabe2Context;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.DatumAngabeContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.DateFormat;
import com.mgmtp.a12.kernel.core.parser.internal.util.DateFormat.DatetimeComponent;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;

/**
 * Hilfsmethoden für Datumsoperationen und das Überprüfen von Datumsangaben.
 */
public class DateUtils {

	public static final Pattern DATUM_PATTERN_WITHOUT_YEAR = Pattern.compile("[0-9][0-9]\\.[0-9][0-9]\\.");
	public static final Pattern DATUM_PATTERN_WITH_YEAR = Pattern.compile("[0-9][0-9]\\.[0-9][0-9]\\.[1-2][0-9][0-9][0-9]");
	public static final Pattern DATETIME_PATTERN = Pattern.compile(
			"[0-9][0-9]\\.[0-9][0-9]\\.[1-2][0-9][0-9][0-9]T[0-2][0-9]:[0-5][0-9]:[0-6][0-9]");
	// We use 0-6 for the first digit of seconds because of potential leap seconds
	public static final Pattern TIME_PATTERN = Pattern.compile("[0-2][0-9]:[0-5][0-9]:[0-6][0-9]");

	public static final String DATUM_FORMAT_WITH_YEAR = "dd.MM.yyyy";
	public static final String DATUM_FORMAT_WITHOUT_YEAR = "dd.MM.";
	public static final String YEAR_FORMAT = "yyyy";

	public static final String TIME_FORMAT = "HH:mm:ss";
	public static final String DATETIME_FORMAT = "dd.MM.yyyy'T'HH:mm:ss";

	/**
	 * SimpleDate Format ist nicht thread-safe
	 */
	private static final ThreadLocal<SimpleDateFormat> formatterWITHOUT_YEAR = ThreadLocal.withInitial(
			() -> initializeValue(DATUM_FORMAT_WITHOUT_YEAR));

	private static final ThreadLocal<SimpleDateFormat> formatterWITH_YEAR = ThreadLocal.withInitial(
			() -> initializeValue(DATUM_FORMAT_WITH_YEAR));

	private static final ThreadLocal<SimpleDateFormat> formatterDateTime = ThreadLocal.withInitial(
			() -> initializeValue(DATETIME_FORMAT));

	private static final ThreadLocal<SimpleDateFormat> formatterTime = ThreadLocal.withInitial(() -> initializeValue(TIME_FORMAT));

	private static SimpleDateFormat initializeValue(String pattern) {
		SimpleDateFormat df = new SimpleDateFormat(pattern);
		df.setLenient(false);
		return df;
	}

	private DateUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Checks if the given String is a valid date String. The date string may use the formats
	 * {@link #DATUM_FORMAT_WITHOUT_YEAR} or {@link #DATUM_FORMAT_WITH_YEAR}
	 *
	 * @param datumString
	 * 		string specifying a date
	 * @return true if it is a valid date string.
	 */
	public static boolean isStringAValidDate(String datumString) {
		if (datumString == null) {
			return false;
		}
		if (datumString.length() == 6) {
			return checkDate(datumString, DATUM_PATTERN_WITHOUT_YEAR, formatterWITHOUT_YEAR.get());
		}
		if (datumString.length() == 8) {
			return checkDate(datumString, TIME_PATTERN, formatterTime.get());
		}
		if (datumString.length() == 10) {
			return checkDate(datumString, DATUM_PATTERN_WITH_YEAR, formatterWITH_YEAR.get());
		}
		if (datumString.length() == 19) {
			return checkDate(datumString, DATETIME_PATTERN, formatterDateTime.get());
		}

		//if not even the length is correct, it cannot be a valid date String.
		return false;
	}

	public static String getDateFormatFromDate(String datumString) {
		return switch (datumString.length()) {
			case 6 -> DATUM_FORMAT_WITHOUT_YEAR;
			case 8 -> TIME_FORMAT;
			case 10 -> DATUM_FORMAT_WITH_YEAR;
			case 19 -> DATETIME_FORMAT;
			default -> throw new IllegalArgumentException(
					"The format for the date string \"" + datumString + "\" cannot be resolved!");
		};
	}

	private static boolean checkDate(String datumString, Pattern p, SimpleDateFormat f) {
		if (!p.matcher(datumString).matches()) {
			return false;
		}
		try {
			f.parse(datumString);
			return true;
		} catch (Exception e) {
			return false; // Es gab eine Parse-Exception, gebe false zurück
		}
	}

	public static DateFormat.DatetimeComponent typeToComponent(Token t, CheckImplAbstractClass obj) {
		switch (t.getType()) {
			case GrammarOnly_ConditionParser.ADDIERE_SEKUNDEN,
				 GrammarOnly_ConditionParser.SEKUNDEN_AUS_ZEIT,
				 GrammarOnly_ConditionParser.DIFFERENZ_IN_SEKUNDEN:
				return DatetimeComponent.SECOND;
			case GrammarOnly_ConditionParser.ADDIERE_MINUTEN,
				 GrammarOnly_ConditionParser.MINUTEN_AUS_ZEIT,
				 GrammarOnly_ConditionParser.DIFFERENZ_IN_MINUTEN:
				return DatetimeComponent.MINUTE;
			case GrammarOnly_ConditionParser.ADDIERE_STUNDEN,
				 GrammarOnly_ConditionParser.STUNDEN_AUS_ZEIT,
				 GrammarOnly_ConditionParser.DIFFERENZ_IN_STUNDEN:
				return DatetimeComponent.HOUR;
			case GrammarOnly_ConditionParser.DIFFERENZ_IN_TAGEN,
				 GrammarOnly_ConditionParser.TAG_AUS_DATUM,
				 GrammarOnly_ConditionParser.ADDIERE_TAGE:
				return DatetimeComponent.DAY;
			case GrammarOnly_ConditionParser.DIFFERENZ_IN_MONATEN,
				 GrammarOnly_ConditionParser.MONAT_AUS_DATUM,
				 GrammarOnly_ConditionParser.QUARTAL_AUS_DATUM,
				 GrammarOnly_ConditionParser.ADDIERE_MONATE:
				return DatetimeComponent.MONTH; // a capital M to make checks against the date format easier.
			case GrammarOnly_ConditionParser.DIFFERENZ_IN_JAHREN,
				 GrammarOnly_ConditionParser.JAHR_AUS_DATUM,
				 GrammarOnly_ConditionParser.ADDIERE_JAHRE:
				return DatetimeComponent.YEAR;
			default:
				obj.throwUnknownTokenError(t);
				throw new IllegalArgumentException("unknown token " + t); // never reached.
		}
	}

	/**
	 * @param fragment
	 * 		a date fragment code which represents optional parts of a date, as returned by
	 * 		IDateType.PartlyKnownDateType.getDatePartSpecifier()
	 * @param component
	 * @return
	 */
	public static boolean doesPartKnownFragmentCorrespondToDatetimeComponent(String fragment,
			DateFormat.DatetimeComponent component) {
		// means "no optional parts"
		return switch (fragment) {
			case "d" -> component == DatetimeComponent.DAY;
			case "m" -> component == DatetimeComponent.MONTH;
			case "y" -> component == DatetimeComponent.YEAR;
			case "" -> throw new IllegalArgumentException("This check is only meaningful for dates that have optional parts");
			default -> throw new IllegalArgumentException("unrecognized partially-optional date fragment type: " + fragment);
		};
	}

	public static void checkDateCompletelySpecified(DatumAngabe2Context datum2Ctx, CheckImplAbstractClass obj) {
		if (datum2Ctx.datumAngabe() != null) {
			checkDateCompletelySpecified(datum2Ctx.datumAngabe(), obj);
		}
	}

	public static void checkDateCompletelySpecified(DatumAngabeContext datumCtx, CheckImplAbstractClass obj) {
		if (datumCtx.datumFeld != null) {
			checkDateCompletelySpecified(datumCtx.datumFeld, obj);
		}
	}

	public static void checkDateCompletelySpecified(EntitySpecContext ectx, CheckImplAbstractClass obj) {
		EntityRef ref = obj.getParser().getEntityRef(ectx);
		IField f = (IField) ref.getEntity();
		String opName = ectx.getParent().getParent().getParent().getStart().getText();
		if (FieldUtils.isDatePartialKnown(f)) {
			String errMessage = obj.getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_DATE_TYPE, opName,
					ref.getNameBuiltFromConditionText());
			throw obj.createMVKParserError(ectx.getParent(), errMessage);
		}

	}

	public static void checkOperationConsistentToPartialKnown(Token t, EntitySpecContext ectx, CheckImplAbstractClass obj) {
		EntityRef ref = obj.getParser().getEntityRef(ectx);
		IField f = (IField) ref.getEntity();
		if (FieldUtils.isDatePartialKnown(f)) {
			DateFormat.DatetimeComponent opFragment = DateUtils.typeToComponent(t, obj);
			String partKnownFragment = FieldUtils.getPartialKnownFragment(f);
			String opName = t.getText();
			String refNameForErrorOutput = ref.getNameBuiltFromConditionText();
			String errMessage = obj.getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_DATE_TYPE, opName, refNameForErrorOutput);

			if (DateFormat.isTimeFragment(opFragment)) {
				throw obj.createMVKParserError(ectx.getParent(), errMessage);
			}

			if (doesPartKnownFragmentCorrespondToDatetimeComponent(partKnownFragment, opFragment)) {
				throw obj.createMVKParserError(ectx.getParent(), errMessage);
			}
			if ("m".equals(partKnownFragment) && opFragment == DatetimeComponent.DAY) {
				throw obj.createMVKParserError(ectx.getParent(), errMessage);
			}
			if ("y".equals(partKnownFragment)) {
				throw obj.createMVKParserError(ectx.getParent(), errMessage);
			}
		}
	}

	public static void checkOperationConsistentToDateFormat(Token t, DateFormat format, ParserRuleContext ctx, String name,
			CheckImplAbstractClass obj) {
		DateFormat.DatetimeComponent dateFragment = DateUtils.typeToComponent(t, obj);

		// model has base year; special handling of years
		if (obj.getParser().hasBaseYear() && dateFragment == DatetimeComponent.YEAR) {
			return;
		}
		String opName = t.getText();
		if (!format.hasComponent(dateFragment)) {
			String errMessage = obj.getErrorMessage(ParserErrorTypesEnum.MVK_WRONG_DATE_FORMAT_FOR_OP, opName, name);
			throw obj.createMVKParserError(ctx, errMessage);
		}

	}

	public static void unload() {
		formatterWITHOUT_YEAR.remove();
		formatterWITH_YEAR.remove();
		formatterDateTime.remove();
		formatterTime.remove();
	}
}
