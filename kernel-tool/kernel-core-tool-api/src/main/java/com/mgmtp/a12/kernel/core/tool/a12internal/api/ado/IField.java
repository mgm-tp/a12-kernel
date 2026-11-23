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

import java.util.Locale;
import java.util.Optional;

/**
 * This interface is intended to be implemented by clients.
 * Provides the information about a field.
 */
public interface IField extends IEntity {

	/**
	 * Configuration for required fields.
	 */
	interface IRequirednessConfig {

		enum Mode {
			absoluteOrRelativeToNextRepAncestor,
			relativeToParent
		}

		/**
		 * Semantic of the required field.
		 *
		 * @return the mode. Is never {@code null}.
		 */
		Mode getMode();

		/**
		 * Custom error message.
		 *
		 * @param loc
		 * 		specifies the {@link Locale} for the error message
		 * @return the defined error message.
		 */
		Optional<String> getErrorMessage(Locale loc);
	}

	/**
	 * For future use: a type may be null, if the type is not yet specified.
	 * However, this handling is not implemented yet!
	 *
	 * @return the field type - At the moment: never returns null!
	 */
	public IFieldType getType();

	/**
	 * @return true, if the field is explicitly marked as mandatory no matter the semantic or configuration (see
	 *        {@link #getRequirednessConfig()}) and false otherwise.
	 */
	public default boolean isMandatory() {
		return getRequirednessConfig().isPresent();
	}

	/**
	 * @return the corresponding configuration if it is a required field.
	 */
	public Optional<IRequirednessConfig> getRequirednessConfig();

	/**
	 * Specifies whether the field is global or not.
	 * <p>
	 * Global fields should always be passed to a partial validation in addition to the usual
	 * relevant entities. The caller of the validation is responsible for passing the global fields.
	 * The information will be transferred to the key "GlobalesFeld" of IMetaField.
	 *
	 * @return {@code true} if the property <i>global</i> was set to {@code true}, otherwise
	 *        {@code false}.
	 */
	public default boolean isGlobal() {
		return false;
	}

	/**
	 * Specifies whether the field is transient or not.
	 * <p>
	 * Transient fields are not meant to be persisted (i.e. xml format).
	 * If transient fields are referenced in rules, they should be passed to the validation, if they
	 * are calculated, they should be passed to the calculation.
	 *
	 * @return {@code true} if the property <i>transient</i> was set to {@code true}, otherwise
	 *        {@code false}.
	 */
	public default boolean isTransient() {
		return false;
	}
}
