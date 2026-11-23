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

import java.util.Objects;
import java.util.Set;

import com.mgmtp.a12.kernel.core.parser.internal.check.util.FieldUtils;
import com.mgmtp.a12.kernel.core.parser.internal.util.DateFormat.DatetimeComponent;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.OperationType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEnumerationType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IFieldType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IYesNoType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IYesType;

/**
 *
 */
public class OperationResultType {

	public enum OperationResultBaseType {
		NUMBER,
		DATETIME,
		DATE_RANGE,
		STRING,
		ENUM,
		BOOLY,
		BOOLYN,
		CONSTY,
		CONSTN,
		BASE_YEAR,
		UNSPECIFIED
	}

	private final OperationResultBaseType baseType;

	/**
	 * format is required for dates, BaseYear, and date range and cannot be extracted for format
	 */
	private final DateFormat format;

	/**
	 * nameForErrorMessages is required to e.g. directly name "Today" or "BaseYear" or the date constant (cannot be extracted).
	 */
	private final String nameForErrorMessages;
	/**
	 * the category will be set if the field value operation contains a category access.
	 */
	private final String category;
	/**
	 * The {@link IFieldType} of the field in the corresponding field value operation.
	 */
	private final IFieldType fieldType;

	private final Integer maxFractionalDigits;

	private final boolean withConstant;

	private OperationResultType(OperationResultBaseType baseType, IFieldType fieldType, DateFormat format,
			String nameForErrorMessages, String category, Integer maxFractionalDigits, boolean containsConstant) {
		this.baseType = baseType;
		this.fieldType = fieldType;
		this.format = format;
		this.nameForErrorMessages = nameForErrorMessages;
		this.category = category;
		this.maxFractionalDigits = maxFractionalDigits;
		this.withConstant = containsConstant;
	}

	public static OperationResultType createBooleanType(OperationResultBaseType baseType, IFieldType fieldType) {
		return new OperationResultType(baseType, fieldType, null, null, null, null, false);
	}

	public static OperationResultType createTypeWithFormat(OperationResultBaseType baseType, IFieldType fieldType, String format,
			String nameForErrorMessages) {
		return createTypeWithFormat(baseType, fieldType, DateFormat.of(format), nameForErrorMessages);
	}

	public static OperationResultType createTypeWithFormat(OperationResultBaseType baseType, IFieldType fieldType,
			DateFormat format,
			String nameForErrorMessages) {
		//for base year, date and date ranges.
		return new OperationResultType(baseType, fieldType, format, nameForErrorMessages, null, null, false);
	}

	public OperationResultType withUnorderedDateFormat() {
		return createTypeWithFormat(baseType, fieldType, format != null ? format.asPureOrderIndependent() : null,
				nameForErrorMessages);
	}

	/**
	 * Adds a number operation type with the specified max fractional digits and the information, if
	 * a multiplicative constant is involved.
	 *
	 * @param fieldType
	 * 		the field type
	 * @param maxFractionalDigits
	 * 		the max fractional digits for the current operation
	 * @param multiplicativeConstant
	 * 		should be <code>true</code>, if a multiplicative constant is
	 * 		involved in the operation, else <code>false</code>.
	 * @see OperationResultType#hasMultiplicativeConstant() for information on the constainsConstant
	 * 		parameter.
	 */
	public static OperationResultType createNumberType(IFieldType fieldType, Integer maxFractionalDigits,
			boolean multiplicativeConstant) {
		return new OperationResultType(OperationResultBaseType.NUMBER, fieldType, null, null, null, maxFractionalDigits,
				multiplicativeConstant);
	}

	public OperationResultType(OperationResultBaseType baseType, IFieldType fieldType) {
		this(baseType, fieldType, null, null, null, null, false);
	}

	public static OperationResultType createEnumerationType(IEnumerationType enumType, String category) {
		return new OperationResultType(OperationResultBaseType.ENUM, enumType, null, null, category, null, false);
	}

	public static OperationResultType createUnspecifiedType(IFieldType fieldType) {
		//for unspecified type.
		return new OperationResultType(OperationResultBaseType.UNSPECIFIED, fieldType, null, null, null, null, false);
	}

	public OperationResultBaseType getBaseType() {
		return baseType;
	}

	public Integer getMaxFractionalDigits() {
		return this.maxFractionalDigits;
	}

	public Set<String> getEnumValues() {
		return FieldUtils.getDistinctEnumValues(fieldType);
	}

	public Set<String> getEnumCategoryValues(String category) {
		return FieldUtils.getCategoryValues(fieldType, category);
	}

	public String getCategory() {
		return category;
	}

	public DateFormat getDateFormat() {
		return format;
	}

	/**
	 * For multiplicative operations like <code>5 * [x] = [z]</code>, where x has no fractional
	 * digits and z has two fractional digits, we do not want to force the user to specify
	 * <code>5.00 * [x] = [z]</code>.
	 * <p>
	 * To account for this, we return here, whether there is a multiplicative constant in the
	 * operation.
	 *
	 * @return <code>
	 */
	public boolean hasMultiplicativeConstant() {
		return withConstant;
	}

	public String getYesValue() {
		if (fieldType instanceof IYesType) {
			return ((IYesType) fieldType).getTrueValue();
		}
		if (fieldType instanceof IYesNoType) {
			return ((IYesNoType) fieldType).getTrueValue();
		}
		return null;
	}

	public String getNoValue() {
		if (fieldType instanceof IYesNoType) {
			return ((IYesNoType) fieldType).getFalseValue();
		}
		return null;
	}

	public IFieldType getFieldType() {
		return fieldType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((baseType == null) ? 0 : baseType.hashCode());
		result = prime * result + ((format != null && format.hasComponent(DatetimeComponent.YEAR)) ? 0 : "y".hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		OperationResultType other = (OperationResultType) obj;
		if (baseType != other.baseType) {
			return false;
		}

		//DateFormat:  we do no check for equality but only if year is included or not.
		//check is only necessary, if a format is specified.
		if (format != null && other.format != null) {
			return format.hasComponent(DatetimeComponent.YEAR) == other.format.hasComponent(DatetimeComponent.YEAR);
		}
		return true;
	}

	public boolean matches(OperationResultType obj) {
		if (!this.equals(obj)) {
			return false;
		}
		OperationResultType other = obj;
		if ((format == null) != (other.format == null)
				|| (format != null && other.format != null && !format.hasSameKnownFormatStringIfExists(other.format))) {
			return false;
		}
		if (!(fieldType instanceof IYesNoType) && !(fieldType instanceof IYesType)) {
			return true;
		}
		return Objects.equals(getYesValue(), other.getYesValue()) && Objects.equals(getNoValue(), other.getNoValue());
	}

	public String getDateNameForErrorMessaging() {
		return nameForErrorMessages;
	}

	public String toString() {
		String sFormat = format != null ? format.description() : "";
		String e = getEnumValues() == null ? "" : getEnumValues().toString();
		return String.format("%s%s%s", this.baseType, sFormat, e);
	}

	public static OperationType getType(OperationResultType opType) {
		if (opType == null) { // This can happen in case of lenient parsing
			return OperationType.STRING;
		}
		switch (opType.getBaseType()) {
			case NUMBER:
			case BASE_YEAR:
				return OperationType.NUMBER;
			case DATETIME:
				return OperationType.DATE;
			case DATE_RANGE:
				return OperationType.DATE_RANGE;
			default:
				return OperationType.STRING;
		}
	}
}
