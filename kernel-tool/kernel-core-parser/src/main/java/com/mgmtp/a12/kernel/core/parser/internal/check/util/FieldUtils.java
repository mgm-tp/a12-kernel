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

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IDateRangeType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IDateType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IDateType.PartlyKnownDateType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEnumerationType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IFieldType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.INumberType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IPredefinedType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IStringType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IUnspecifiedType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IYesNoType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IYesType;

/**
 * Many helper methods to check the type of a field.
 */
public class FieldUtils {

	private FieldUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * returns true, if field has a specified type and the type is a number.
	 */
	public static boolean isNumber(IField f) {
		return f.getType() instanceof INumberType;
	}

	public static boolean isYes(IField f) {
		return f.getType() instanceof IYesType;
	}

	public static boolean isYesNo(IField f) {
		return f.getType() instanceof IYesNoType;
	}

	public static boolean isEnum(IField f) {
		return f.getType() instanceof IEnumerationType;
	}

	public static boolean isEnumValueValid(IEnumerationType enumType, String value) {
		return getDistinctEnumValues(enumType).contains(value);
	}

	/**
	 * @param type
	 * 		the field type to check.
	 * @return <code>true</code> if the normal enumeration has display values or if an enumeration
	 * 		with table reference has table information associated and a corresponding display
	 * 		column, and <code>false</code> in all other cases.
	 */
	public static boolean isEnumWithDisplayValues(IFieldType type) {
		if (type instanceof IEnumerationType) {
			IEnumerationType enumType = (IEnumerationType) type;
			if (!enumType.getTableName().isPresent()) {
				return enumType.hasDisplayValues();
			}
			return enumType.getColumnName().flatMap(
							n -> enumType.getTable().map(t -> t.hasDisplayColumn(n)))
					.orElse(false);
		}
		return false;
	}

	/**
	 * @param fieldType
	 * 		the type of the field to check.
	 * @param category
	 * 		the category for which values should be returned.
	 * @return the set of all category values if the field type is an enumeration with or without
	 * 		table reference. An empty set otherwise.
	 */
	public static Set<String> getCategoryValues(IFieldType fieldType, String category) {
		if (!(fieldType instanceof IEnumerationType)) {
			return Collections.emptySet();
		}
		IEnumerationType enumType = (IEnumerationType) fieldType;
		if (!enumType.getTableName().isPresent()) {
			return enumType.getCategoryValues().get(category).stream()
					.collect(Collectors.toSet());
		}
		return enumType.getTable().flatMap(t -> enumType.getColumnName()
						.map(n -> t.getDistinctCategoryValues(n, category)))
				.orElse(Collections.emptySet());
	}

	public static boolean isValidCategoryValue(IFieldType type, String categoryName, String value) {
		Set<String> categoryValues = FieldUtils.getCategoryValues(type, categoryName);
		return categoryValues.contains(value);
	}

	/**
	 * @param fieldType
	 * 		the field type to be inspected.
	 * @return the set of valid XML-values of the enumeration, if the field type is an enumeration.
	 */
	public static Set<String> getDistinctEnumValues(IFieldType fieldType) {
		if (!(fieldType instanceof IEnumerationType)) {
			return Collections.emptySet();
		}
		IEnumerationType enumType = (IEnumerationType) fieldType;
		if (!enumType.getTableName().isPresent()) {
			return enumType.getXMLValues().stream()
					.collect(Collectors.toSet());
		}
		return enumType.getTable().flatMap(t -> enumType.getColumnName()
						.map(t::getDistinctValues))
				.orElse(Collections.emptySet());
	}

	/**
	 * @param type
	 * 		the field type to be inspected.
	 * @return <code>true</code> if the field type specifies an enumeration with table reference
	 * 		without the table specification, <code>false</code> otherwise.
	 */
	public static boolean isTableEnumWithoutTable(IFieldType type) {
		return isTableEnum(type) &&
				!((IEnumerationType) type).getTable().isPresent();
	}

	/**
	 * @param type
	 * 		the field type to be inspected.
	 * @return <code>true</code> if the field type specifies an enumeration with table reference
	 * 		with the table specification, <code>false</code> otherwise.
	 */
	public static boolean isTableEnumWithTable(IFieldType type) {
		return isTableEnum(type) &&
				((IEnumerationType) type).getTable().isPresent();
	}

	/**
	 * @param type
	 * 		the field type to be inspected.
	 * @return <code>true</code> if the field type specifies an enumeration with table reference,
	 * 		<code>false</code> otherwise.
	 */
	public static boolean isTableEnum(IFieldType type) {
		return type instanceof IEnumerationType &&
				((IEnumerationType) type).getTableName().isPresent();
	}

	/**
	 * Beim Parsen sind nicht abgeschlossene Enumerationen und Vordefinierte Typen wie Strings.
	 */
	public static boolean isLikeString(IField f) {
		IFieldType t = f.getType();
		return t instanceof IStringType || t instanceof IPredefinedType;
	}

	public static boolean isString(IField f) {
		IFieldType t = f.getType();
		return t instanceof IStringType;
	}

	/**
	 * @return <code>true</code> if the field is a string and its property 'noValueValidation' is
	 * 		<code>true</code>, otherwise <code>false</code>.
	 */
	public static boolean isNoValueValidation(IField f) {
		if (isString(f)) {
			return ((IStringType) f.getType()).isNoValueValidation();
		} else {
			return false;
		}
	}

	public static boolean isExtensibleEnum(IField f) {
		IFieldType t = f.getType();
		return t instanceof IStringType && ((IStringType) t).isExtensibleEnum();
	}

	public static boolean isDate(IField f) {
		return f.getType() instanceof IDateType;
	}

	public static boolean isDateRange(IField f) {
		return f.getType() instanceof IDateRangeType;
	}

	public static String getDateFormat(IField f) {
		if (!isDate(f) && !isDateRange(f)) {
			return null;
		}
		if (isDate(f)) {
			return ((IDateType) f.getType()).getFormat();
		} else {
			return ((IDateRangeType) f.getType()).getFormatOfDate();
		}

	}

	public static String getDateRangeFormat(IField f) {
		if (!isDateRange(f)) {
			return null;
		}
		IDateRangeType dateRange = (IDateRangeType) f.getType();
		return dateRange.getFormatOfDateRange();
	}

	public static boolean isDateRangeWithSpecificInterpretationOfYear(IField f) {
		if (!isDateRange(f)) {
			return false;
		}
		return ((IDateRangeType) f.getType()).getInterpretationOfYear() == IDateRangeType.InterpretationOfYear.PREVIOUS
				|| ((IDateRangeType) f.getType()).getInterpretationOfYear() == IDateRangeType.InterpretationOfYear.SUBSEQUENT;
	}

	/**
	 * @param f
	 * 		field
	 * @return <code>true</code> if the field is a date and at least a date fragment may be unknown,
	 * 		<code>false</code>
	 */
	public static boolean isDatePartialKnown(IField f) {
		if (!isDate(f)) {
			return false;
		}
		return ((IDateType) f.getType()).partlyKnownDate() != PartlyKnownDateType.BEKANNT;
	}

	public static String getPartialKnownFragment(IField f) {
		if (isDate(f)) {
			IDateType dt = (IDateType) f.getType();
			return dt.partlyKnownDate().getDatePartSpecifier();
		}
		return "";
	}

	public static boolean isPredefined(IField f) {
		return f.getType() instanceof IPredefinedType;
	}

	public static boolean isUnspecified(IField f) {
		return f.getType() instanceof IUnspecifiedType;
	}

	/**
	 * Checks if <code>f</code> is of type string (also predefined or extensible enumeration),
	 * enumeration or number.
	 */
	public static boolean isStringEnumOrNumber(IField f) {
		return isEnum(f) || isLikeString(f) || isNumber(f);
	}

	/**
	 * Checks if <code>f</code> is of type string (NOT predefined), enumeration or extensible
	 * enumeration.
	 */
	public static boolean isStringEnumOrExtensibleEnum(IField f) {
		return isString(f) || isEnum(f) || isExtensibleEnum(f);
	}

	/**
	 * For enumerations and strings with patterns, special error messages are stored in the field
	 * type. These are extracted with this function.
	 *
	 * @param fieldType
	 * 		the type of the field probably containing an error message.
	 * @return the error message, if the fields does contain one, else <code>null</code>.
	 */
	public static String getErrorText(IFieldType fieldType, Locale language) {
		if (fieldType instanceof IStringType) {
			IStringType iet = (IStringType) fieldType;
			return iet.getErrorMessage(language);
		} else if (fieldType instanceof IEnumerationType) {
			IEnumerationType iet = (IEnumerationType) fieldType;
			return iet.getErrorMessage(language);
		}
		return null;
	}

	/**
	 * @return <code>true</code> if a special value for the field is defined (this is only possible
	 * 		for enumerations), otherwise <code>false</code>.
	 */
	public static boolean isSValueDefined(IField f) {
		return (f.getType() instanceof IEnumerationType) && !StringUtils.isEmpty(((IEnumerationType) f.getType()).getsValue());
	}

	public static String getFieldTypeName(IFieldType type) {
		if (type instanceof IDateRangeType) {
			return "DatumBereich";
		} else if (type instanceof IEnumerationType) {
			return "Enumeration";
		} else if (type instanceof INumberType) {
			return "Zahl";
		} else if (type instanceof IStringType) {
			if (((IStringType) type).isExtensibleEnum()) {
				return "NichtAbgeschlosseneEnumeration";
			}
			return "String";
		} else if (type instanceof IYesType) {
			return "Ja";
		} else if (type instanceof IPredefinedType) {
			return String.format("Vordefiniert (%s)", ((IPredefinedType) type).getName());
		} else if (type instanceof IDateType) {
			return "Datum";
		} else if (type instanceof IYesNoType) {
			return "JaNein";
		} else if (type instanceof IUnspecifiedType) {
			return "NichtSpezifiziert";
		} else {
			throw new IllegalArgumentException("Invalid field type " + type.getClass().getCanonicalName());
		}
	}
}
