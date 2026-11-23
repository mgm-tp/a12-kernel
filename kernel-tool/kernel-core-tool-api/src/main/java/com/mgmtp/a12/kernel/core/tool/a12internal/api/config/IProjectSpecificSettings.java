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
import java.util.Map;

import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.ICodeGeneratorParameters;

/**
 * Common interface for settings which are specific for a context where Kernel is used. This interface does <em>not</em> refer to an
 * A12 project.
 * Kernel provides <b>a12internal</b> implementations for ExtMP (see ExtmpSettings) and A12 (see A12SpecificSettings).
 * <p>
 * <em>NOTE: All implementations are provided by Kernel. Neither other A12 components nor A12 projects
 * must provide custom implementations.<br/>
 * Kernel will assume that no external implementations exist when planning
 * breaking/non-breaking changes.</em> See also @OnlyForUsage.
 * <p>
 */
public interface IProjectSpecificSettings {
	int MAXDIGITS_IN_NUMBERS = 15;

	/**
	 * Get a mapping from each supported language, the code should be generated for (compare with
	 * {@link ICodeGeneratorParameters#getLanguages()}), to the {@link IProjectLanguageSettings}
	 * that should be used for this specific language.
	 *
	 * @return a map, specifying project specific language settings for all supported
	 *        {@link Locale}s
	 */
	Map<Locale, IProjectLanguageSettings> getProjectLanguageSettings();

}
