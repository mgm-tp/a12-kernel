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

import java.util.ArrayList;
import java.util.List;

import com.mgmtp.a12.model.header.Annotation;

/**
 * This interface is intended to be implemented by clients.
 * <p>
 * Common interface for {@link IField} or {@link IGroup}
 */
public interface IEntity {

	/**
	 * The full name uses the path notation.
	 * The full name starts with a single slash and the root group is the first element of the path.
	 *
	 * @return full name of the entity
	 */
	public String getFullName();

	/**
	 * Each field has minimum two repetition layers, as a field has to be included in at least one
	 * group.
	 * The number of repetition layers equals the number of slashes in the full name.
	 * In A12 the max amount of the last repetition layer is always 1, if it is a field.
	 * The last repetition layer is included in the array to handle the ExtMP old world without
	 * special treatments.
	 *
	 * @return an array specifying the max amount of repetitions for each repetition layer
	 */
	public long[] getMax();

	/**
	 * for convenience only
	 *
	 * @return true if the entity is a field, false otherwise
	 */
	public default boolean isField() {
		return this instanceof IField;
	}

	/**
	 * for convenience only
	 *
	 * @return true if the entity is a group, false otherwise
	 */
	public default boolean isGroup() {
		return this instanceof IGroup;
	}

	/**
	 * @return map from the name of an annotation to its value. The value of the annotation is an
	 * 		empty string, if no value was specified.
	 * 		May not be {@code null}.
	 */
	public default List<Annotation> getAnnotations() {
		return new ArrayList<>();
	}
}
