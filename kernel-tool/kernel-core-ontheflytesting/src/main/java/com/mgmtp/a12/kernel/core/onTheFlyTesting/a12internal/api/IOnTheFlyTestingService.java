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

package com.mgmtp.a12.kernel.core.onTheFlyTesting.a12internal.api;

import java.util.Collection;

import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IRule;
import com.mgmtp.a12.model.utils.OnlyForUsage;

@OnlyForUsage
public interface IOnTheFlyTestingService {

	/**
	 * Performs formal validation for the given data (returned by
	 * {@link IOnTheFlyTestingParameter#getData()}) and tests the rules. Returns
	 * the error messages in the language specified by
	 * {@link IOnTheFlyTestingParameter#getLanguage()}.
	 * <br>
	 * Error text parameters are replaced by the values. E.g. if the error text uses an error text
	 * parameter to specify the value of a field, the error text parameter is replaced by the actual
	 * value of the field - or the default value, if the value of the field is not specified. Throws
	 * an {@link IllegalArgumentException}, if the rule does not provide an error message for the
	 * given languages.
	 * <br>
	 * If the rule fires, the error message is reported to the problem reporter
	 * {@link IOnTheFlyTestingParameter#getProblemReporter()}.
	 *
	 * @param rules
	 * 		specifies the rules of the model, may not be null
	 * @param params
	 * 		contains all the information that is additionally required here, for example
	 * 		<i>data</i> or <i>problem reporter</i>
	 * @deprecated will be removed with A12K-3593
	 */
	@Deprecated(since = "30.2.0")
	void testRule(Collection<? extends IRule> rules, IOnTheFlyTestingParameter params);

	/**
	 * Performs formal validation for the given data (returned by
	 * {@link IOnTheFlyTestingParameter#getData()}. Returns
	 * the error message in the language specified by
	 * {@link IOnTheFlyTestingParameter#getLanguage()}.
	 *
	 * @param params
	 * 		contains all the information that is additionally required here, for example
	 * 		<i>data</i> or <i>problem reporter</i>
	 */
	void testField(IOnTheFlyTestingParameter params);
}
