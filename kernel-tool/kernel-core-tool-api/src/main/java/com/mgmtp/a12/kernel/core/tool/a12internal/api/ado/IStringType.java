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

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IModelSpecificSettings;

/**
 * This interface is intended to be implemented by clients.
 * Models the field type 'String'.
 */
public interface IStringType extends IFieldType {

	/**
	 * Returns the suggested values.
	 *
	 * @return all suggested values for the locale or an empty list if no suggested values are specified for the locale.
	 */
	public default List<String> getHintList(Locale locale) {
		return Collections.emptyList();
	}

	/**
	 * @return true, if the string represents an extensible enumeration, false otherwise.
	 */
	public default boolean isExtensibleEnum() {
		return false;
	}

	/**
	 * Returns a String, specifying the required pattern, or null, if no pattern is specified.
	 * The pattern must always match the entire string. Thus no starting ^ or trailing $ should be
	 * included in the pattern.
	 * The allowed patterns are limited - see documentation of language constructs {@literal PatternMatched / PatternViolated}.
	 * These limitations are required to support the different target languages.
	 *
	 * @return the used pattern or null, if no pattern is used.
	 */
	public String getPattern();

	/**
	 * Returns a String, specifying the error message, or null, if no error message is specified.
	 * The method has to return a string not equal to null and not empty, if a pattern is specified.
	 * <p>
	 * The parameter specifies the {@link Locale} of the error message. Please do not mix up with
	 * the
	 * {@link Locale} of the project specific settings.
	 * An error message must contain a reference to the field. This reference to the field is
	 * specified by $&lt;fieldref&gt;$. The fieldref string depends on the language setting of the
	 * project.
	 * see {@link IModelSpecificSettings#getLanguageUsedInConditionText()}
	 *
	 * @param loc
	 * 		specifies the language of the error message
	 * @return the error message of the given {@link Locale} or null, if no error message is used.
	 *
	 * @throws throws
	 *        {@link IllegalArgumentException} if called with an unsupported {@link Locale}.
	 */
	public String getErrorMessage(Locale loc);

	/**
	 * @return true, if the value of the field may contain line breaks, false otherwise.
	 */
	public boolean hasLineBreaksPermitted();

	/**
	 * Returns the minimum length of the string. The minimum is always greater or equal 1.
	 * A minimum length of 0 is not allowed, as empty strings are not regarded as a valid
	 * value for a string. Empty strings are handled as values, which are not specified.
	 *
	 * @return the minimum length of the string
	 */
	public int getMinLength();

	/**
	 * Returns the maximal length of the string. If the maximal length is undefined, returns a
	 * {@link IFieldType#UNDEFINED}. If the maximal length is set, the max is always greater or
	 * equal 1 and greater or equal to the minimum length.
	 *
	 * @return the maximal length of the string
	 */
	public int getMaxLength();

	@Override
	public default boolean matchBaseType(IFieldType expectedBaseType) {
		return expectedBaseType == null || expectedBaseType instanceof IStringType
				|| expectedBaseType instanceof IPredefinedType;
	}

	/**
	 * @return true, if the value of the field must <b>not</b> be validated, false otherwise.
	 */
	public boolean isNoValueValidation();

	@Override
	default boolean isValueApplicable(String value) {
		if (StringUtils.isEmpty(value)) {
			return true;
		}
		if (getMaxLength() > 0 && getMaxLength() < value.length()) {
			return false;
		}
		if (getMinLength() > 0 && getMinLength() > value.length()) {
			return false;
		}
		String pattern = getPattern();
		if (!StringUtils.isEmpty(pattern)) {
			return value.matches(pattern);
		}
		return true;
	}
}