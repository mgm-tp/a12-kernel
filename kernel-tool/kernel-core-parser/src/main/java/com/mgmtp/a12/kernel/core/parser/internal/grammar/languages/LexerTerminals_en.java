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
package com.mgmtp.a12.kernel.core.parser.internal.grammar.languages;

import java.util.ListResourceBundle;

import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.Base_ConditionLexer;

public class LexerTerminals_en extends ListResourceBundle {

	private static final Object[][] CONTENTS = new Object[][] {
			{ "AddDays", Base_ConditionLexer.ADDIERE_TAGE },
			{ "AddHours", Base_ConditionLexer.ADDIERE_STUNDEN },
			{ "AddMinutes", Base_ConditionLexer.ADDIERE_MINUTEN },
			{ "AddMonths", Base_ConditionLexer.ADDIERE_MONATE },
			{ "AddSeconds", Base_ConditionLexer.ADDIERE_SEKUNDEN },
			{ "AddYears", Base_ConditionLexer.ADDIERE_JAHRE },
			{ "And", Base_ConditionLexer.UND1 },
			{ "and", Base_ConditionLexer.UND2 },
			{ "AND", Base_ConditionLexer.UND3 },
			{ "Abs", Base_ConditionLexer.ABS },
			{ "AbsValue", Base_ConditionLexer.ABS_WERT },
			{ "AllFieldsFilled", Base_ConditionLexer.ALLE_FELDER_ANGEGEBEN },
			{ "AllGroupsFilled", Base_ConditionLexer.ALLE_KONTEXTE_ANGEGEBEN },
			{ "AtLeastOneDateRangeOverlaps", Base_ConditionLexer.MINDESTENS_EIN_DATUMSBEREICH_UEBERLAPPEND },
			{ "AtLeastOneFieldFilled", Base_ConditionLexer.MINDESTENS_EIN_FELD_ANGEGEBEN },
			{ "AtLeastOneFieldValueIncludedInValueList", Base_ConditionLexer.MINDESTENS_EIN_FELD_WERT_IN_WERTE_LISTE },
			{ "AtLeastOneGroupFilled", Base_ConditionLexer.MINDESTENS_EIN_KONTEXT_ANGEGEBEN },
			{ "BaseYear", Base_ConditionLexer.REFERENZJAHR },
			{ "CurrentRepetition", Base_ConditionLexer.AKTUELLE_WIEDERHOLUNG },
			{ "CustomCondition", Base_ConditionLexer.APPLIKATIONS_BEDINGUNG },
			{ "Date", Base_ConditionLexer.DATUM },
			{ "DateFromDateTime", Base_ConditionLexer.DATUM_AUS_DATETIME },
			{ "DateRangesOverlap", Base_ConditionLexer.DATUMS_BEREICHE_UEBERLAPPEND },
			{ "DateRange", Base_ConditionLexer.DATUM_BEREICH },
			{ "DateTime", Base_ConditionLexer.DATETIME },
			{ "DayFromDate", Base_ConditionLexer.TAG_AUS_DATUM },
			{ "DifferenceInDays", Base_ConditionLexer.DIFFERENZ_IN_TAGEN },
			{ "DifferenceInHours", Base_ConditionLexer.DIFFERENZ_IN_STUNDEN },
			{ "DifferenceInMinutes", Base_ConditionLexer.DIFFERENZ_IN_MINUTEN },
			{ "DifferenceInMonths", Base_ConditionLexer.DIFFERENZ_IN_MONATEN },
			{ "DifferenceInSeconds", Base_ConditionLexer.DIFFERENZ_IN_SEKUNDEN },
			{ "DifferenceInYears", Base_ConditionLexer.DIFFERENZ_IN_JAHREN },
			{ "DiffersWithToleranceRange1", Base_ConditionLexer.UNGLEICH_MIT_TOLERANZ1 },
			{ "DiffersWithToleranceRange2", Base_ConditionLexer.UNGLEICH_MIT_TOLERANZ2 },
			{ "DiffersWithToleranceRange5", Base_ConditionLexer.UNGLEICH_MIT_TOLERANZ5 },
			{ "DiffersWithToleranceRange10", Base_ConditionLexer.UNGLEICH_MIT_TOLERANZ10 },
			{ "EndOfDateRange", Base_ConditionLexer.ENDE_AUS_DATUM_BEREICH },
			{ "False", Base_ConditionLexer.FALSCH },
			{ "FieldFilled", Base_ConditionLexer.FELD_ANGEGEBEN },
			{ "FieldNotFilled", Base_ConditionLexer.FELD_NICHT_ANGEGEBEN },
			{ "FieldValueAsNumber", Base_ConditionLexer.FELD_WERT_ALS_ZAHL },
			{ "FieldValueAsString", Base_ConditionLexer.FELD_WERT_ALS_STRING },
			{ "FieldValueIncludedInValueList", Base_ConditionLexer.FELD_WERT_IN_WERTE_LISTE },
			{ "FieldValueNotIncludedInValueList", Base_ConditionLexer.FELD_WERT_NICHT_IN_WERTE_LISTE },
			{ "FieldValuesNotUnique", Base_ConditionLexer.FELD_WERTE_NICHT_EINDEUTIG },
			{ "FieldsNotCollectivelyFilled", Base_ConditionLexer.FELDER_NICHT_GEMEINSAM_ANGEGEBEN },
			{ "FirstDay", Base_ConditionLexer.ERSTER_TAG },
			{ "FirstFilledValue", Base_ConditionLexer.ERSTER_ANGEGEBENER_WERT },
			{ "For", Base_ConditionLexer.FOR },
			{ "@From", Base_ConditionLexer.VON },
			{ "GroupFilled", Base_ConditionLexer.KONTEXT_ANGEGEBEN },
			{ "GroupNotFilled", Base_ConditionLexer.KONTEXT_NICHT_ANGEGEBEN },
			{ "GroupsNotCollectivelyFilled", Base_ConditionLexer.KONTEXTE_NICHT_GEMEINSAM_ANGEGEBEN },
			{ "HoursFromTime", Base_ConditionLexer.STUNDEN_AUS_ZEIT },
			{ "In", Base_ConditionLexer.IN1 },
			{ "in", Base_ConditionLexer.IN2 },
			{ "IN", Base_ConditionLexer.IN3 },
			{ "Invalid", Base_ConditionLexer.UNGUELTIG },
			{ "LastDay", Base_ConditionLexer.LETZTER_TAG },
			{ "Length", Base_ConditionLexer.LAENGE },
			{ "Max", Base_ConditionLexer.MAX },
			{ "MaxValue", Base_ConditionLexer.MAX_WERT },
			{ "Min", Base_ConditionLexer.MIN },
			{ "MinutesFromTime", Base_ConditionLexer.MINUTEN_AUS_ZEIT },
			{ "MinValue", Base_ConditionLexer.MIN_WERT },
			{ "Having", Base_ConditionLexer.MIT },
			{ "MonthFromDate", Base_ConditionLexer.MONAT_AUS_DATUM },
			{ "MoreThanOneFieldFilled", Base_ConditionLexer.MEHR_ALS_EIN_FELD_ANGEGEBEN },
			{ "NoFieldFilled", Base_ConditionLexer.KEIN_FELD_ANGEGEBEN },
			{ "NoFieldValueIncludedInValueList", Base_ConditionLexer.KEIN_FELD_WERT_IN_WERTE_LISTE },
			{ "NoGroupFilled", Base_ConditionLexer.KEIN_KONTEXT_ANGEGEBEN },
			{ "NotAllFieldValuesIncludedInValueList", Base_ConditionLexer.NICHT_ALLE_FELD_WERTE_IN_WERTE_LISTE },
			{ "NotAllFieldsFilled", Base_ConditionLexer.NICHT_ALLE_FELDER_ODER_KEIN_FELD_ANGEGEBEN },
			{ "NotAllGroupsFilled", Base_ConditionLexer.NICHT_ALLE_ODER_KEIN_KONTEXT_ANGEGEBEN },
			{ "NotExactlyOneFieldFilled", Base_ConditionLexer.NICHT_GENAU_EIN_FELD_ANGEGEBEN },
			{ "Now", Base_ConditionLexer.JETZT },
			{ "NumberOfDifferentValues", Base_ConditionLexer.ANZAHL_VERSCHIEDENER_WERTE },
			{ "NumberOfFilledFields", Base_ConditionLexer.ANZAHL_ANGEGEBENER_FELDER },
			{ "NumberOfFilledGroups", Base_ConditionLexer.ANZAHL_ANGEGEBENER_KONTEXTE },
			{ "NumberOfValueInFields", Base_ConditionLexer.ANZAHL_WERT_IN_FELDLISTE },
			{ "Or", Base_ConditionLexer.ODER1 },
			{ "or", Base_ConditionLexer.ODER2 },
			{ "OR", Base_ConditionLexer.ODER3 },
			{ "PatternMatched", Base_ConditionLexer.WIE_MUSTER },
			{ "PatternViolated", Base_ConditionLexer.NICHT_WIE_MUSTER },
			{ "QuarterFromDate", Base_ConditionLexer.QUARTAL_AUS_DATUM },
			{ "RangeAsNumber", Base_ConditionLexer.BEREICH_ALS_ZAHL },
			{ "RangeAsString", Base_ConditionLexer.BEREICH_ALS_STRING },
			{ "RepetitionNotUnique", Base_ConditionLexer.WIEDERHOLUNG_NICHT_EINDEUTIG },
			{ "RoundAccounting", Base_ConditionLexer.RUNDEN },
			{ "RoundAccountingValue", Base_ConditionLexer.RUNDEN_WERT },
			{ "RoundDown", Base_ConditionLexer.ABRUNDEN },
			{ "RoundDownValue", Base_ConditionLexer.ABRUNDEN_WERT },
			{ "RoundUp", Base_ConditionLexer.AUFRUNDEN },
			{ "RoundUpValue", Base_ConditionLexer.AUFRUNDEN_WERT },
			{ "RuleGroup", Base_ConditionLexer.REGEL_KONTEXT },
			{ "SecondsFromTime", Base_ConditionLexer.SEKUNDEN_AUS_ZEIT },
			{ "StartOfDateRange", Base_ConditionLexer.START_AUS_DATUM_BEREICH },
			{ "Sum", Base_ConditionLexer.SUMME },
			{ "SumOfProducts", Base_ConditionLexer.SUMME_VON_PRODUKTEN },
			{ "SuppressWarning", Base_ConditionLexer.UNTERDRUECKE_WARNUNG },
			{ "Time", Base_ConditionLexer.TIME },
			{ "TimeFromDateTime", Base_ConditionLexer.ZEIT_AUS_DATETIME },
			{ "to", Base_ConditionLexer.ZU1 },
			{ "To", Base_ConditionLexer.ZU2 },
			{ "TO", Base_ConditionLexer.ZU3 },
			{ "Today", Base_ConditionLexer.HEUTE },
			{ "True", Base_ConditionLexer.WAHR },
			{ "Valid", Base_ConditionLexer.GUELTIG },
			{ "ValueAsDate", Base_ConditionLexer.WERT_ALS_DATUM },
			{ "ValueNotConsistent", Base_ConditionLexer.WERT_NICHT_KONSISTENT },
			{ "YearFromDate", Base_ConditionLexer.JAHR_AUS_DATUM },
	};

	@Override
	protected Object[][] getContents() {
		return CONTENTS;
	}

}