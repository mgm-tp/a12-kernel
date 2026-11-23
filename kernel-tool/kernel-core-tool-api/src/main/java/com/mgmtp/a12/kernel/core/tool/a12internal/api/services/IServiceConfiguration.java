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

import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IModelSpecificSettings;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.config.IProjectSpecificSettings;
import com.mgmtp.a12.model.utils.OnlyForUsage;

@OnlyForUsage
public interface IServiceConfiguration {

	/**
	 * See {@link IServiceConfiguration#strictness()}.
	 */
	enum ParserStrictness {STRICT, LENIENT}

	/**
	 * Returns the IEC object, that was used to create the service.
	 *
	 * @return IEC connected to the service.
	 */
	IEC getIEC();

	/**
	 * Returns the {@link IModelSpecificSettings}, that was used to create the service.
	 *
	 * @return the {@link IModelSpecificSettings} connected to the service.
	 */
	IModelSpecificSettings getModelSpecificSettings();

	/**
	 * Returns the {@link IProjectSpecificSettings}, the service uses.
	 *
	 * @return the project-specific settings.
	 */
	IProjectSpecificSettings getProjectSpecificSettings();

	/**
	 * Specifies, whether lenient services should be used, that can handle incomplete models, where groups or fields are missing.
	 * Beware, that no code generation will be possible in this case and not all services will be able to handle all queries in
	 * this case.
	 * <p>
	 * The created {@link com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition} may contain non-existent
	 * IVirtual fields or groups that might be handled as fields AND groups
	 * (they might return true for both IEntity#isField() and IEntity#isGroup()).
	 * </p>
	 * ATTENTION: If you use this flag, make sure to test the places where the resulting
	 * {@link com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition} is used thoroughly. The resulting ParseTree
	 * is at the creation time primarily tested by executing the formatter tests for it and check against the expectations for the
	 * normal parser.
	 *
	 * @return the leniency of the services.
	 */
	default ParserStrictness strictness() {
		return ParserStrictness.STRICT;
	}
}
