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
 * Models a date.
 * At the moment, the format may contain only days, months, years.
 * E.g. yyyy-MM-dd is a valid format.
 * This restriction is only necessary for ExtMP (because of the target language C++).
 */
public interface IDateType extends IFieldType {

	public enum PartlyKnownDateType {
		BEKANNT(""),
		TAG_OPTIONAL("d"),
		MONAT_OPTIONAL("m"),
		JAHR_OPTIONAL("y");

		private final String datePartSpecifier;

		private PartlyKnownDateType(String datePartSpecifier) {
			this.datePartSpecifier = datePartSpecifier;
		}

		/**
		 * Get the relevant date part specifier.
		 * This is "d" for {@link #TAG_OPTIONAL}, "m" for {@link #MONAT_OPTIONAL}, "y" for
		 * {@link #JAHR_OPTIONAL} and an empty string for {@link #BEKANNT}.
		 */
		public String getDatePartSpecifier() {
			return datePartSpecifier;
		}
	}

	/**
	 * Never returns null.
	 *
	 * @return the format of the date e.g. as used by the Java SimpleDateFormatter
	 */
	public String getFormat();

	/**
	 * @return <code>true</code>, if the syntactic checks for the field should contain a comparison
	 * 		to the year 1900.
	 */
	public boolean isAdditionalCheck();

	/**
	 * In order to model birth dates, partly known dates may be used,
	 * when the exact date is not known.
	 * Instead of a concrete date e.g. "01.01.2014", "00.01.2014" is also a valid date if the day is
	 * optional.
	 * We differ between
	 * <ul>
	 * <li>day is optional. return {@link PartlyKnownDateType#TAG_OPTIONAL}</li>
	 * <li>month is optional. return {@link PartlyKnownDateType#MONAT_OPTIONAL}</li>
	 * <li>year is optional. return {@link PartlyKnownDateType#JAHR_OPTIONAL}</li>
	 * </ul>
	 *
	 * @return {@link PartlyKnownDateType#BEKANNT}, if the date is fully known
	 * 		and otherwise the specific enum value of {@link PartlyKnownDateType}.
	 */
	public PartlyKnownDateType partlyKnownDate();

	@Override
	public default boolean matchBaseType(IFieldType expectedBaseType) {
		return expectedBaseType == null || expectedBaseType instanceof IDateType;
	}

	@Override
	default boolean isValueApplicable(String value) {
		if (StringUtils.isEmpty(value)) {
			return true;
		}
		// last replacement: 'T' -> T in case of time
		String datePattern = getFormat().replaceAll("d|M|y|H|m|s", "[0-9]").replaceAll("\\.", "\\\\.")
				.replaceAll("'([^']*)'", "$1");
		return value.matches(datePattern);
	}
}
