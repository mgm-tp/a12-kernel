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

package com.mgmtp.a12.kernel.core.tool.a12internal.api.config;

import java.util.Locale;

import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.ICodeGeneratorParameters;

/**
 * Interface specifies all settings for a specific language, used for code generation.
 * Intended to be implemented by the client.
 */
public interface IProjectLanguageSettings {

	/**
	 * @return name of the language used at the user interface and for internal error messages.
	 */
	String getName();

	/**
	 * @return returns the local, specifying the language
	 */
	Locale getLocale();

	/**
	 * Get the decimal sign used in the user interface representation.
	 * Please note: There is a difference between the decimal sign used for the model and
	 * the decimal sign used for the specified language.
	 * E.g. for {@link Locale#GERMANY} we use ',' as the decimal sign. However, the model may
	 * use a different decimal sign. See also {@link ICodeGeneratorParameters#getDecimalSeparator()}
	 *
	 * @return the decimal separator, used for the language.
	 */
	char getDecimalSeparator();

	/**
	 * Get the character used to optionally separate the blocks of three digits in numbers.
	 * The default implementation returns the empty string.
	 *
	 * @return the thousands separator used for the language.
	 */
	default String getThousandsSeparator() {
		return "";
	}

	/**
	 * Returns the date format, used for the language in the user interface representation.
	 * E.g. dd.MM.yyyy for {@link Locale#GERMANY} or MM/dd/yyyy for {@link Locale#US}
	 * Please note: There is a difference between date format for a field in the model and
	 * the date format of the language.
	 *
	 * @return the date format, used for the language
	 */
	String getDateFormat();

	/**
	 * Returns the information, if the padding zeros are optional - as used for the language in the
	 * user interface representation.
	 * Specifies e.g. if 7.7.2014 is an allowed date. (or if it has to be 07.07.2014)
	 *
	 * @return true, if leading zeros may be omitted in the date string, false if not.
	 */
	boolean isDateNullOptional();

	/**
	 * Specifies the sign in the date range between the two dates, used for the language.
	 * It has to be assured, that e.g. for an english format like MM/dd/yyyy we do not take
	 * '/' as a sign. However, for a german date like dd.MM.yyyy, the slash may be used.
	 *
	 * @return the sign used between the two dates of a date range.
	 */
	char getDateRangeSeparator();

	/**
	 * Returns the date format, used for the language in the user interface representation,
	 * containing also time information.
	 * E.g. dd.MM.yyyy HH:mm:ss for {@link Locale#GERMANY} or MM/dd/yyyy HH:mm:ss for
	 * {@link Locale#US}
	 * Please note: There is a difference between date format for a field in the model and
	 * the date format of the language.
	 *
	 * @return the date time format for the language.
	 */
	String getDateTimeFormat();

	/**
	 * Returns the time format, used for the language in the user interface representation,
	 * containing also time information.
	 * E.g. HH:mm:ss for {@link Locale#GERMANY} or HHmmss for
	 * {@link Locale#US}
	 * Please note: There is a difference between date format for a field in the model and
	 * the date format of the language.
	 *
	 * @return the date time format for the language.
	 */
	String getTimeFormat();

}
