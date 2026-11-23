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

package com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams;

/**
 * Models the different profiles for code generation. Each profile bundles different options,
 * e.g which meta information is generated.
 * <p>
 * The profiles in detail:
 *
 * <ul>
 * <li><code>CP_STANDARD</code> - Standard code generation for all languages except C++:
 * <ul>
 * <li>WITH omission errors.</li>
 * <li>WITH the entire meta-code, in particular with meta-code for mandatory fields and mandatory
 * field sets.</li>
 * <li>WITH partial validation.</li>
 * </ul>
 * </li>
 * <li><code>CP_STANDARD</code> - Standard code generation for C++:
 * <ul>
 * <li>WITHOUT omission errors.</li>
 * <li>WITHOUT the entire meta-code in C++, WITH the entire meta-code in JAVA.</li>
 * <li>WITH partial validation.</li>
 * </ul>
 * </li>
 * <li><code>CP_MAPPED</code> - Generation for assigned models for the mapping from eXML to
 * uXML. The code is reduced as in <code>CP_MAPPER_ONLY</code>.
 * </li>
 * <li><code>CP_MAPPER_ONLY</code> - Generation of reduced code for the mapping from eXML to
 * uXML (in this case the reduction is also applied for the eXML model):
 * <ul>
 * <li>WITHOUT rules.</li>
 * <li>WITH just field meta information which is relevant for the mapping from eXML to uXML.</li>
 * <li>WITH information about the mapping from eXML to uXML.</li>
 * </ul>
 * </li>
 * </ul>
 */
public enum CodeProfileType {

	/**
	 * Full code generation for all languages except C++, where code generation is without full meta data support and no omission
	 * error support.
	 */
	CP_STANDARD,

	/**
	 * Code generation for the mapped uXML model.
	 */
	CP_MAPPED,
	/**
	 * Generation of reduced code, mainly containing code necessary for the mapping from eXML to
	 * uXML. For details see {@link CodeProfileType}
	 */
	CP_MAPPER_ONLY;

}
