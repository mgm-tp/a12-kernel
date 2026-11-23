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

import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * This interface is intended to be implemented by clients.
 * Models a number. To specify, if the sign is allowed use
 * the min value.
 */
public interface INumberType extends IFieldType {

	/**
	 * @return true if zero (0) is a valid value
	 */
	public boolean isZeroAllowed();

	/**
	 * If the number of fractional digits is fixed, {@link #getMaxFractionalDigits()} and
	 * {@link #getMinFractionalDigits()} must return the same value.
	 * If no fractional digits are allowed, 0 is returned.
	 *
	 * @return the minimum of fractional digits.
	 */

	public int getMinFractionalDigits();

	/**
	 * If the number of fractional digits is fixed, {@link #getMaxFractionalDigits()} and
	 * {@link #getMinFractionalDigits()} must return the same value.
	 * If no fractional digits are allowed, 0 is returned.
	 *
	 * @return the maximum of fractional digits.
	 */
	public int getMaxFractionalDigits();

	/**
	 * The maximal number of digits before the decimal place.
	 *
	 * @return An {@link Optional} containing the maximal number of digits before the decimal place
	 * 		if defined.
	 */
	public Optional<Integer> getMaxNumberOfDigitsBeforeDecimalPlace();

	/**
	 * If the min value and the max value are specified, max value must be greater than or equal to
	 * the
	 * min value.
	 *
	 * @return the minimal value or null, if the value is undefined.
	 */
	public Optional<Double> getMinValue();

	/**
	 * @return the maximal value or null, if the value is undefined.
	 */
	public Optional<Double> getMaxValue();

	/**
	 * @return true, if the number represents an amount (of money), false otherwise.
	 */
	public boolean isNumberTraitAmount();

	/**
	 * Returns true, if a number may start with leading zeros. Leading zeros are removed,
	 * if the number is compared. E.g. 01 is equal to 001 and to 1.
	 *
	 * @return true, if leading zeros are allowed, false otherwise.
	 */
	public boolean leadingZerosAllowed();

	/**
	 * Returns the max length (counting the decimal sign and the sign, if needed)
	 * E.g. -3.00 has the length 5; -3 has the length 2.
	 *
	 * @return An {@link Optional}, containing the max length of the field, if it is defined.
	 */
	public Optional<Integer> getMaxLength();

	/**
	 * Returns the min length (counting the decimal sign and the sign, if needed)
	 * E.g. -3.00 has the length 5; -3 has the length 2.
	 *
	 * @return An {@link Optional} containing the min length of the field if it is defined.
	 */
	public Optional<Integer> getMinLength();

	/**
	 * Returns true, if the field may have a sign, otherwise false. Only the negative sign is
	 * supported.
	 */
	public boolean hasSign();

	@Override
	public default boolean matchBaseType(IFieldType expectedBaseType) {
		return expectedBaseType == null || expectedBaseType instanceof INumberType;
	}

	@Override
	default boolean isValueApplicable(String value) {
		if (StringUtils.isEmpty(value)) {
			return true;
		}
		int minFractionalDigits = Math.max(getMinFractionalDigits(), 0);

		String numberPatternString = getNumberPattern(minFractionalDigits, getMaxFractionalDigits());

		Pattern numberPattern = Pattern.compile(numberPatternString);
		return numberPattern.matcher(value).matches();
	}

	static String getNumberPattern(int minFractionalDigits, int maxFractionalDigits) {
		String numberPatternString = "[-]?[0-9]+";

		if (maxFractionalDigits <= 0) {
			return numberPatternString;
		}

		StringBuilder suffix = new StringBuilder();
		if (minFractionalDigits == 0) {
			suffix.append("([.,]");// both decimal separators allowed
			suffix.append("[0-9]{1,");
			suffix.append(maxFractionalDigits);
			suffix.append("})?");
		}
		if (minFractionalDigits > 0) {
			suffix.append("[.,]");// both decimal separators allowed
			suffix.append("[0-9]{");
			suffix.append(minFractionalDigits);
			suffix.append(",");
			suffix.append(maxFractionalDigits);
			suffix.append("}?");
		}

		numberPatternString += suffix;
		return numberPatternString;
	}
}
