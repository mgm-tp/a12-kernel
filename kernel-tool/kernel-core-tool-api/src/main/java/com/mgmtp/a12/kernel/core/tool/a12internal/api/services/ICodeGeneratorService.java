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

package com.mgmtp.a12.kernel.core.tool.a12internal.api.services;

import java.util.Collection;
import java.util.Locale;

import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IRule;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.ICodeGeneratorParameters;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.ICodeGeneratorParameters.ProgrammingLanguage;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblemReporter;
import com.mgmtp.a12.model.utils.OnlyForUsage;

@OnlyForUsage @Deprecated(since = "30.5.0")
public interface ICodeGeneratorService {

	/**
	 * Returns the source code as zip or null in case of an error. All problems are reported to the
	 * problem reporter:
	 * <ul>
	 * <li>rules are not valid</li>
	 * <li>error messages of the rules are not valid</li>
	 * <li>a rule or a field exists which does not provide an error text for a {@link Locale}
	 * specified by {@link ICodeGeneratorParameters#getLanguages()}
	 * <li>programming language specified by
	 * {@link ICodeGeneratorParameters#getProgrammingLanguage()} is not supported
	 * <li>required settings for the specified programming language are not provided or are invalid
	 * <li>decimal separator specified by {@link ICodeGeneratorParameters#getDecimalSeparator()} is
	 * not valid
	 * <li>the name {@link ICodeGeneratorParameters#getName()} is not valid
	 * </ul>
	 * <p>
	 * The resulting source code will be encoded depending on
	 * {@link ICodeGeneratorParameters#getProgrammingLanguage()}:
	 * <dl>
	 * <dt>{@link ProgrammingLanguage#JAVA},
	 * {@link ProgrammingLanguage#JAVA_SCRIPT_VK},
	 * {@link ProgrammingLanguage#JAVA_SCRIPT_REDUCED},
	 * {@link ProgrammingLanguage#GROOVY},
	 * {@link ProgrammingLanguage#CPLUS}</dt>
	 * <dd>UTF-8</dd>
	 * <dt>{@link ProgrammingLanguage#VBA}</dt>
	 * <dd>CP1252</dd>
	 * </dl>
	 *
	 * @param rules
	 * 		specifies the rules of the model, may not be null
	 * @param params
	 * 		specifies the code generation params, may not be null
	 * @param pr
	 * 		problem reporter, may not be null
	 * @return zipped source code as a byte array
	 *
	 * @deprecated Use com.mgmtp.a12.kernel.md.model.api.services.IDocumentModelService#generateValidationCode instead
	 */
	@Deprecated(since = "30.5.0")
	public byte[] getSourceCodeAsZip(Collection<? extends IRule> rules, ICodeGeneratorParameters params, IProblemReporter pr);

}
