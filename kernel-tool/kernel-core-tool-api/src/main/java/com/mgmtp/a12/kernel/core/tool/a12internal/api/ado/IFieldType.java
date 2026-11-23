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

/**
 * Common interface for all basic field types.
 * This interface is intended to be implemented by clients.
 */
public interface IFieldType {

	/**
	 * Used for undefined values for maxLength, minLength, fractionalDigits, or others
	 */
	public int UNDEFINED = -1;

	/**
	 * Returns true, if the field type matches the expected base type.
	 * Base type is either
	 * <ul>
	 * <li>IDateRangeType</li>
	 * <li>IDateType</li>
	 * <li>IEnumerationType</li>
	 * <li>IStringType and IPredefinedType are considered the same
	 * type</li>
	 * <li>INumberType</li>
	 * <li>IYesNoType</li>
	 * <li>IYesType</li>
	 * <li>IUnspecifiedType</li>
	 * </ul>
	 *
	 * @param expectedBaseType
	 * 		the expected base type
	 * @return true, if the field type is the expected base type, false otherwise.
	 */
	public boolean matchBaseType(IFieldType expectedBaseType);

	/**
	 * Returns true, if the given value can be applied to the type.
	 * Only base checks are applied.
	 *
	 * @param value
	 * 		the value to be checked.
	 * @return <code>false</code>, if the given value cannot be applied to the type.
	 * 		<code>true</code>, if unsure or the given value can be applied to the type.
	 */
	public boolean isValueApplicable(String value);

}
