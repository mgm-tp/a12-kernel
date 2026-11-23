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

package com.mgmtp.a12.kernel.core.parser.internal.util;

/**
 * An instance of <code>SemanticIndexRef</code> represents a semantic index, which can be either an
 * {@link EntityRef} or a {@link String} constant.
 */
public class SemanticIndexRef {

	final Object semIdx;

	public SemanticIndexRef(EntityRef ref) {
		semIdx = ref;
	}

	public SemanticIndexRef(String constant) {
		semIdx = constant;
	}

	/**
	 * Checks whether the semantic index is a String.
	 */
	public boolean isConstant() {
		return semIdx instanceof String;
	}

	/**
	 * Checks, whether the semantic index is a entity.
	 */
	public boolean isEntity() {
		return semIdx instanceof EntityRef;
	}

	/**
	 * Cast the index to a String.
	 */
	public String getAsConstant() {
		return (String) semIdx;
	}

	/**
	 * Cast the index to {@link EntityRef}.
	 */
	public EntityRef getAsEntity() {
		return (EntityRef) semIdx;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((semIdx == null) ? 0 : semIdx.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SemanticIndexRef other = (SemanticIndexRef) obj;
		if (semIdx == null) {
			if (other.semIdx != null) {
				return false;
			}
		} else if (!semIdx.equals(other.semIdx)) {
			return false;
		}
		return true;
	}
}
