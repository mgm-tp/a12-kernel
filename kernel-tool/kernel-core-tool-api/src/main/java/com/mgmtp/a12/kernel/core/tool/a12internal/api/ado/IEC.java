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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IEntityChangeSet;

/**
 * The Interface Entity Container (IEC) is a container for all fields and groups for a given model.
 * <p>
 * It is the base for certain operations performed by the parser (e.g. validation,
 * calculation and code generation).
 * </p>
 * Consistency Constraints:
 * <ul>
 * <li>Empty root groups and empty groups are included in this container. The user is responsible
 * for filtering out empty root groups and empty groups if necessary.
 * <br>
 * <b>Note:</b> The only exception for this is in the ExtMP world where (root) groups that are
 * empty due to application restrictions are <b>not</b> included in this container.
 * <li>If a field is an element of this container, all parent groups have to be included in this
 * container as well.<br>
 * E.g.: If the field <code>/a/b/f</code> is an element of this container, then root group
 * <code>/a</code> and group <code>/a/b</code> have to be elements of this container as well.
 * Otherwise this container is in an inconsistent and invalid state.
 * </ul>
 * <p>
 * This interface is intended to be implemented by clients.
 * </p>
 */
public interface IEC {

	/**
	 * Gets a group or field by their fullName from this container, if present.
	 *
	 * @param fullName
	 * 		specifies the entity that should be returned. <br>
	 * 		It must follow the path notation as described in {@link IEntity#getFullName()}
	 * 		<br>
	 * 		(e.g. <code>"/a/b/f"</code> if the field <code>f</code> shall be returned).
	 * @return the {@link IEntity} with the given fullName or <code>null</code>, if no such
	 * 		entity exists. <br>
	 * 		In order to identify the sub-type of the returned entity use
	 *        {@link IEntity#isField()} and {@link IEntity#isGroup()}.
	 */
	public IEntity get(String fullName);

	/**
	 * Returns all fields within a group (including the fields of the sub-groups) specified by the
	 * given pathName. <br>
	 * The returned list is empty if
	 * <ul>
	 * <li>pathName is <code>null</code>
	 * <li>there is no (root) group with the given pathName
	 * <li>there is no field within the group and its sub-groups
	 * <li>pathName is the fullName of a field
	 * </ul>
	 * <p>
	 * The ordering of the list reflects the ordering of the fields in the underlying model. <br>
	 * The ordering of the fields may not change between two calls.
	 * </p>
	 * <p>
	 * Only fields are returned, no groups.
	 * </p>
	 * Examples:
	 *
	 * <pre>
	 * Model A (group order follows the depth-first principle):
	 * /o/a/f1
	 * /o/a/b/f2
	 * /o/a/b/f3
	 * /o/z/f5
	 * getAllContainedFields("/o/a") returns [/o/a/f1, /o/a/b/f2, /o/a/b/f3].
	 * getAllContainedFields("/o/a/b") returns [/o/a/b/f2, /o/a/b/f3].
	 * getAllContainedFields("/") returns [] (as '/' does not refer to a specific (root) group).
	 * getAllContainedFields("/o/z/f5") returns [].
	 * getAllContainedFields("/c") returns [].
	 * getAllContainedFields(null) returns [].
	 *
	 * Model B (group order follows the breadth-first principle):
	 * /o/a/f1
	 * /o/z/f5
	 * /o/a/b/f2
	 * /o/a/b/f3
	 * getAllContainedFields("/o") returns [/o/a/f1, /o/z/f5, /o/a/b/f2, /o/a/b/f3].
	 * </pre>
	 *
	 * @param pathName
	 * 		specifies the group (including sub-groups), whose fields should be returned.
	 * 		It must follow the path notation as described in {@link IEntity#getFullName()}.
	 * @return a list of all fields having a name starting with pathName. The list might be empty.
	 * 		Never returns <code>null</code>.
	 */
	public List<IField> getAllContainedFields(String pathName);

	/**
	 * Returns a list of all {@link IEntity}s contained in the group
	 * (excluding the content of the sub-groups).
	 * <p>
	 * The returned list may contain fields and groups.
	 * To get all root groups a single slash "/" can be used as pathName.
	 * </p>
	 * <p>
	 * The entity with the fullName that equals the given pathName is not included in the list.
	 * </p>
	 * The returned list is empty if
	 * <ul>
	 * <li>pathName is <code>null</code>
	 * <li>there is no (root) group with the given pathName
	 * (the only exception is the single slash "/")
	 * <li>there is no field and sub-group within the specified group
	 * <li>pathName is the fullName of a field
	 * </ul>
	 * <p>
	 * The ordering of the list reflects the ordering of the fields of the underlying model. <br>
	 * The ordering of the fields does not change between two calls.
	 * </p>
	 * <p>
	 * Examples:
	 *
	 * <pre>
	 * Model A (group order follows the depth-first principle):
	 * /o/f6
	 * /o/a/f1
	 * /o/a/b/f2
	 * /o/a/b/f3
	 * /o/z/f5
	 * getDirectlyContainedEntities("/o/a") returns [/o/a/f1, /o/a/b].
	 * getDirectlyContainedEntities("/o/a/b") returns [/o/a/b/f2, /o/a/b/f3].
	 * getDirectlyContainedEntities("/") returns [/o].
	 * getDirectlyContainedEntities("/o/z/f5") returns [].
	 * getDirectlyContainedEntities("/o") returns [/o/f6, /o/a, /o/z].
	 * getDirectlyContainedEntities("/c") returns [].
	 * getDirectlyContainedEntities(null) returns [].
	 *
	 * Model B (group order follows the breadth-first principle):
	 * /o/f6
	 * /o/a/f1
	 * /o/z/f5
	 * /o/a/b/f2
	 * /o/a/b/f3
	 * getDirectlyContainedEntities("/o") returns [/o/f6, /o/a, /o/z].
	 * </pre>
	 *
	 * @param pathName
	 * 		specifies the (root) group whose direct entities should be
	 * 		returned. It must follow the path notation
	 * 		as described in {@link IEntity#getFullName()}.
	 * @return a list of all entities contained in the specified group by the given pathName.
	 * 		The list may be empty. Never returns <code>null</code>.
	 */
	public List<IEntity> getDirectlyContainedEntities(String pathName);

	/**
	 * Applies all changes to the entities of this container, specified by the given parameter.
	 * <p>
	 * By default, this method returns an UnsupportedOperationException.
	 * </p>
	 * <p>
	 * In concurrent environments, the access to this method must be synchronized
	 * (including upcoming operations depending on the specified changes on the entities).
	 * </p>
	 * <p>
	 * <u>For ExtMP only</u>: maxUsb must not be set here,
	 * because there is a discrepancy between the maximum value of an USB and the maximum number of
	 * different USBs. The rules restrict the maximum number of different USBs,
	 * not the maximum value. But the latter is represented in IExtmpEntity.getMaxUsb(). <br>
	 * </p>
	 * <p>
	 * Has to be implemented if used for code generation.
	 * </p>
	 *
	 * @param changes
	 * 		is a list of changes to entities of this container. Must not be <code>null</code>.
	 */
	public default void applyChanges(Set<IEntityChangeSet> changes) {
		throw new UnsupportedOperationException("applyChanges is not supported.");
	}

	/**
	 * Retrieve all the full names of fields having the same given short name.
	 * <p>
	 * By default, this method returns an UnsupportedOperationException.
	 * </p>
	 * <p>
	 * Has to be implemented, if used for parsing rules. So it is also needed for code generation.
	 * </p>
	 *
	 * @return the set of full names corresponding to fields having the shortName as name.
	 */
	public default Set<String> shortName2FullName(String shortName) {
		return Collections.emptySet();
	}

	/**
	 * Get the tables defining the values for enumerations with table reference.
	 *
	 * @return a map from table name to table data.
	 * 		The return value should be:
	 * 		<ul>
	 * 		<li><code>null</code> if neither the name nor the content of the tables are
	 * 		known.</li>
	 * 		<li>A map from table name to <code>null</code>- values if only the names of the
	 * 		tables are known.</li>
	 * 		<li>A map from table name to the full table data represented as
	 *                                                {@link IEnumerationDataTable}.</li>
	 * 		</ul>
	 * 		The table data should use the implementation EnumerationDataTable from package
	 * 		kernel-core-utils.
	 */
	default Map<String, IEnumerationDataTable> getEnumerationTables() {
		return null;
	}
}
