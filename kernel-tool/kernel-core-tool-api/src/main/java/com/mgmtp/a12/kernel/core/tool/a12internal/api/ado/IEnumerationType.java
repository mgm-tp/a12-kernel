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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * This interface is intended to be implemented by clients.
 */
public interface IEnumerationType extends IFieldType {

	interface EnumerationValue {

		/**
		 * @return the display value - Never returns null or an empty string.
		 */
		public String getDisplayValue();

		/**
		 * @return the xml value - Never returns null or an empty string.
		 */
		public String getXmlValue();
	}

	/**
	 * @param locale
	 * 		specifies the {@link Locale} for the display values
	 * @return <ul>
	 * 		<li>all display values for the given {@link Locale}. The ordering of the list
	 * 		corresponds to the ordering of the display values for the given {@link Locale}. The
	 * 		list is never empty. The size of the list must be equal to the size of the list
	 * 		returned by {@link #getXMLValues()}.</li>
	 * 		<li>or {@code null}, if no display values are defined for the given
	 *        {@link Locale}. For enumerations with table reference, this will also be the case
	 * 		even if display values are defined in the table.</li>
	 * 		</ul>
	 */
	public List<EnumerationValue> getDisplayValues(Locale locale);

	/**
	 * The result of {@link #getDisplayValues(Locale)} for the returned {@link Locale}s will never
	 * be {@code null}.
	 *
	 * @return the supported {@link Locale}s for display value access (see also
	 *        {@link #getDisplayValues(Locale)}). The {@link Set} is empty if no display values are
	 * 		defined at all or if the enumeration is with table reference. Is never {@code null}.
	 */
	public Set<Locale> getSupportedDisplayLocales();

	/**
	 * @return <code>true</code> if the enumeration has any display values that are not identical to
	 * 		the corresponding XML-value, <code>false</code> otherwise. For enumerations with
	 * 		table reference <code>false</code> is returned regardless of existing display values
	 * 		in the table.
	 */
	public default boolean hasDisplayValues() {
		Set<Locale> supportedLocales = getSupportedDisplayLocales();
		for (Locale loc : supportedLocales) {
			List<EnumerationValue> dispValues = getDisplayValues(loc);
			if (dispValues == null) {
				continue;
			}
			for (EnumerationValue enumValue : dispValues) {
				if (!enumValue.getDisplayValue().equals(enumValue.getXmlValue())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns all XML values - Never returns null. Only for enumerations with table reference
	 * an empty list will be returned.
	 *
	 * @return all XML values
	 */
	public List<String> getXMLValues();

	/**
	 * Returns a map containing the category information for all categories. The name of the
	 * category is the key and the value is the list of category values. The list of the category
	 * values uses the ordering of the list returned by {@link #getXMLValues()} and must provide a
	 * non-empty and non-null value for list entry.
	 * Never returns null.
	 * <p>
	 * Example: if {@link #getXMLValues()} returns ["a","b","c"] the method has to return only lists
	 * with three values: e.g. ("greek_category", ["alpha", "beta", "gamma"]), ("tree_view",
	 * ["1.1", "1.2", "1.3"])
	 * <p>
	 * For enumerations with table reference, the returned value will be empty regardless of
	 * categories existing in the table.
	 *
	 * @return a map specifying the category information or an empty map if no category information
	 * 		is specified.
	 */
	public Map<String, List<String>> getCategoryValues();

	/**
	 * Returns the error message; see {@link IStringType#getErrorMessage}
	 *
	 * @param loc
	 * 		specifies the {@link Locale} for the error message
	 * @return error message
	 *
	 * @throws throws
	 *        {@link IllegalArgumentException} if called with an unsupported {@link Locale}.
	 */
	public String getErrorMessage(Locale loc);

	/**
	 * @return the s value, if defined, otherwise the empty string.
	 */
	public default String getsValue() {
		return "";
	}

	@Override
	public default boolean matchBaseType(IFieldType expectedBaseType) {
		return expectedBaseType == null || expectedBaseType instanceof IEnumerationType;
	}

	@Override
	default boolean isValueApplicable(String value) {
		if (StringUtils.isEmpty(value)) {
			return true;
		}
		if (getTableName().isPresent()) {
			return getTable()
					.map(t -> t.getDistinctValues(getTableName().get())
							.contains(value))
					.orElse(true);
		}
		return getXMLValues().contains(value);
	}

	/**
	 * @return the name of the table for enumerations with table reference.
	 */
	public Optional<String> getTableName();

	/**
	 * @return the column name for the XML-value for enumeration with table reference.
	 */
	public Optional<String> getColumnName();

	/**
	 * @return the dependent columns for enumeration with table reference.
	 */
	public List<String> getDependentColumns();

	/**
	 * @return the enumeration table data for enumerations with table reference. Default is
	 *        {@link Optional#empty()} if the table data is not known.
	 */
	public default Optional<IEnumerationDataTable> getTable() {
		return Optional.empty();
	}
}
