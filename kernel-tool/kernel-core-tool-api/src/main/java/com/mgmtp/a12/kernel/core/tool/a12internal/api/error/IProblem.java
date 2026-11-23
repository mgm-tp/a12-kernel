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
package com.mgmtp.a12.kernel.core.tool.a12internal.api.error;

import java.util.Date;

import com.mgmtp.a12.model.notification.RankedNotification;

/**
 * Description of a problem, as detected by the validation kernel.
 * Problems are managed in the {@link IProblemReporter}.
 * <p>
 * A problem provides access to:
 * <ol>
 * <li>its location (if relevant) (source position, line number) and its origin</li>
 * <li>its message description and its severity (error, warning, info)</li>
 * <li>its creation date</li>
 * </ol>
 */
public interface IProblem extends RankedNotification {

	/**
	 * @return the creation date of the problem
	 */
	public Date getDate();

	/**
	 * @return the line number (1-based, inclusive), where the problem begins,
	 * 		or <code>-1</code> if unknown or not applicable
	 */
	public int getLine();

	/**
	 * @return the line number (1-based, inclusive), where the problem ends,
	 * 		or <code>-1</code> if unknown or not applicable.
	 * 		May default to {{@link #getLine()}}
	 */
	public default int getLineEnd() {
		return getLine();
	}

	/**
	 * @return the column (0-based, inclusive) in the line reported by {@link #getLine()}
	 * 		where the problem starts,
	 * 		or <code>-1</code> if unknown or not applicable
	 */
	public int getSourceStart();

	/**
	 * @return the column (0-based, exclusive) in the line reported by {@link #getLineEnd()}
	 * 		where the problem ends,
	 * 		or <code>-1</code>, if unknown or not applicable
	 */
	public int getSourceEnd();
}
