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
package com.mgmtp.a12.kernel.core.tool.a12internal.api.ado;

import org.apache.commons.lang3.StringUtils;

/**
 * This interface is intended to be implemented by clients.
 * Models a date range. It is an ExtMP only feature!
 * <p>
 * The overall format is specified by two dates (using the same format -
 * see {@link #getFormatOfDate()} and the separator between the two dates.
 * see {@link #getSeparator()}. The separator may only be empty, if the
 * date format specifies a fixed length.
 * <p>
 * E.g. a date range like yyyy.MM.dd may use an empty separator.
 */
public interface IDateRangeType extends IFieldType {

	/**
	 * ExtMP-specific only!
	 */
	enum InterpretationOfYear {
		STANDARD,
		PREVIOUS,
		SUBSEQUENT
	}

	/**
	 * Never returns null.
	 *
	 * @return the format of the dates; for limitations see {@link IDateType#getFormat()}
	 */
	String getFormatOfDate();

	/**
	 * Returns the full format of the date range, including "(von/bis ist Referenzjahr)" for special
	 * date formats. Note that this means that the returned String might not be compatible with the
	 * Java SimpleDate format.
	 *
	 * @return the full format of the date range, including "(von/bis ist Referenzjahr)" for special
	 * 		date formats.
	 */
	default String getFormatOfDateRange() {
		InterpretationOfYear year = getInterpretationOfYear();
		if (InterpretationOfYear.PREVIOUS.equals(year)) {
			return "dd.MM-dd.MM (bis ist Referenzjahr)";
		}

		if (InterpretationOfYear.SUBSEQUENT.equals(year)) {
			return "dd.MM-dd.MM (von ist Referenzjahr)";
		}
		return getFormatOfDate() + getSeparator() + getFormatOfDate();
	}

	/**
	 * Returns the separator for the date range.
	 * May be empty, if the the format specifies a fixed length.
	 * Never returns null.
	 *
	 * @return the format string, never returns null.
	 */
	String getSeparator();

	/**
	 * ExtMP specific only!
	 */
	default InterpretationOfYear getInterpretationOfYear() {
		return InterpretationOfYear.STANDARD;
	}

	/**
	 * @return {@code true} if a date before {@code 1900-01-01} should be considered as a wrong
	 * 		input, otherwise {@code false}. The default value is {@code false}.
	 */
	boolean isAdditionalCheck();

	@Override
	default boolean matchBaseType(IFieldType expectedBaseType) {
		return expectedBaseType == null || expectedBaseType instanceof IDateRangeType;
	}

	@Override
	default boolean isValueApplicable(String value) {
		if (StringUtils.isEmpty(value)) {
			return true;
		}
		String datePattern = getFormatOfDate().replaceAll("d|M|y|H|m|s", "[0-9]").replaceAll("\\.", "\\\\.");
		String fullPattern = datePattern + getSeparator() + datePattern;
		return value.matches(fullPattern);
	}
}
