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

import java.util.Map;

/**
 * Intended to be implemented by the client.
 * <p>
 * Models all information which is simply passed to the code generator to generate meta information
 * for the meta entites <code> model, group, field, rule</code>.
 * <p>
 * <b>Example 1:</b>
 * <p>
 * Generate a project specific attribute for the model model which is provided by the key
 * <code>"VersionBased"</code>. This is achieved by an object implementing the
 * <code>IMetaInfoCodeGenParams</code> interface with
 * <ul>
 * <li>
 * {@link #getMetaElementType()} returns <code>{@link #META_ELEMENT_TYPE_MODEL} </code>
 * <li>{@link #getKey()} returns <code>"VersionBased"</code>
 * <li>{@link #getDefaultCode()} is not required and thus can return any value
 * <li>{@link #getNameValueMap()} returns a map with one entry. The value is the String "true", if
 * we
 * want to model the value true for the key. The string has to be wrapped in an Object, which
 * returns the string as an implementation for the toCode method.
 * </ul>
 * <p>
 * <b>Example 2:</b>
 * <p>
 * Generate a project specific attribute for fields, which is provided by the key
 * <code>"formatFlag"</code> and provides some additional formatting information. This is achieved
 * by an object, implementing the <code>IMetaInfoCodeGenParams</code> interface with
 * <ul>
 * <li>
 * {@link #getMetaElementType()} returns <code>{@link #META_ELEMENT_TYPE_FIELD} </code>
 * <li>{@link #getKey()} returns <code>"formatFlag"</code>
 * <li>{@link #getDefaultCode()} returns an empty string (if wanted as default) or e.g. "standard"
 * (if this format is wanted as the default value)
 * <li>{@link #getNameValueMap()} returns a map with an entry for each field. As the key of the
 * entry, use the full name of the field. As the value of the entry use the String with the
 * formatFlag, e.g. "formatX", "formatY". The String has to be wrapped in an Object, which returns
 * the string as an implementation for the toCode method.
 * </ul>
 *
 * <b>Note</b>
 * <p>
 * maybe we need more flexibility in future: something like prefix code fragments, import statements
 * and so on. This will be added, if needed.
 */
public interface IMetaInfoCodeGenParams {

	//Project specific types may be added in future, so we use strings and not an enumeration to
	//define the entity of the meta information
	public static final String META_ELEMENT_TYPE_MODEL = "MODEL";
	public static final String META_ELEMENT_TYPE_FIELD = "FIELD";
	public static final String META_ELEMENT_TYPE_GROUP = "GROUP";
	public static final String META_ELEMENT_TYPE_RULE = "RULE";

	/**
	 * specifies a specific value for a meta information.
	 * The toCode method is used for code generation.
	 * We do not want to use the standard toString method, as
	 * the toCode method may implement some special things.
	 */
	@FunctionalInterface
	public interface MetaInfoValue {
		public String toCode();
	}

	/**
	 * Returns the name of the meta-element. It is one of the constant strings specified in the
	 * interface or a project specific extension.
	 * For the near future, the interface provides no general approach for project specific
	 * extensions.
	 * However, this point of extension is used in ExtMP.
	 *
	 * @return name of the meta element
	 */
	public String getMetaElementType();

	/**
	 * Key which is to be used at the meta-interface.
	 * It is in the responsibility of the client to make sure that keys are unique
	 *
	 * @return name of the key, may not be null
	 */
	public String getKey();

	/**
	 * @return the code fragment used for all objects of the specified entity type which are not
	 * 		specified in the map returned by {@link #getNameValueMap()}
	 */
	public String getDefaultCode();

	/**
	 * returns a map which defines a value for all entities of the given element type
	 * for the given key. For all entities of the given element type which are not included in the
	 * map the default value will be generated see {@link #getDefaultCode()} The key values in the
	 * map identify the meta entities.
	 * For fields, groups and rules the full path name is used.
	 * For the model the key is not checked. It is in the responsibility of the client to make
	 * sure that the map includes only one entry if getMetaElementType() returns "MODEL"
	 *
	 * @return map specifying the values for the given key specified by getKey.
	 */
	public Map<String, MetaInfoValue> getNameValueMap();

}
