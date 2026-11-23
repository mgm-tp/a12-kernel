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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Intended to be implemented by the client.
 * Specifies the parameters required in both code generation and on-the-fly testing.
 */
public interface IModelCodeGeneratorParameters {

	/**
	 * The method specifies, if the generated code restricts the allowed character set. If the
	 * returned {@link Optional} is not present, no additional checks are included in the generated
	 * code. If the method returns a non-empty {@link Optional}, the returned char array specifies
	 * the allowed character set.
	 *
	 * @return an {@link Optional} containing the valid character set, if restrictions of the
	 * 		character set are specified. Otherwise, an empty {@link Optional}.
	 *
	 * @throws IllegalStateException
	 * 		<ul>
	 * 					<li>If invalid range definitions are encountered (lower bound > upper bound)</li>
	 * 					<li>if this method was called on an extended charset which contains combined characters.</li>
	 * 				</ul>
	 * @deprecated since version 29.2.0.
	 * 		<p>
	 * 		We deprecate this function because of the need to support DIN 91379 charsets. (A12-15974)
	 * 		Please implement {@link #restrictedValidGraphemeSet()} instead.
	 * 		The new default implementation for this method will handle single characters and expand ranges. Combined characters,
	 * 		which are made up of multiple Unicode code points, will throw an Exception.
	 * 		</p>
	 * 		<p>
	 * 		If the given charset (DataOrderResponse or A12-Document) was created or exported after release 29,
	 * 		one can simply return its List representation as the source for {@link #restrictedValidGraphemeSet()}.
	 * 		</p>
	 * 		How to map this method to the new implementation for old charset definitions, which only contain single
	 * 		characters:
	 * 		<ul>
	 * 		    <li>If Optional.empty() was returned: return an empty list.</li>
	 * 		    <li>Otherwise: Return a list containing String.valueOf(char) foreach char in the char array</li>
	 * 		</ul>
	 */
	@Deprecated(since = "29.2.0")
	default Optional<char[]> restrictedValidCharacterSet() {
		final List<String> graphemeSet = restrictedValidGraphemeSet();
		if (graphemeSet.isEmpty()) {
			return Optional.empty();
		}
		final List<String> singleCharsExpanded = new ArrayList<>();
		for (final String graphemeDefinition : graphemeSet) {
			if (1 == graphemeDefinition.length()) {
				// single character
				singleCharsExpanded.add(graphemeDefinition);
			} else if (3 == graphemeDefinition.length() && '-' == graphemeDefinition.charAt(1)) {
				// expand ranges, in case minified charsets are encountered
				final int rangeStart = graphemeDefinition.codePointAt(0);
				final int rangeEnd = graphemeDefinition.codePointAt(2);
				if (rangeStart <= rangeEnd) {
					for (int codePoint = rangeStart; codePoint <= rangeEnd; codePoint++) {
						singleCharsExpanded.add(String.valueOf((char) codePoint));
					}
				} else {
					throw new IllegalStateException("Incorrect range encountered: " + graphemeDefinition);
				}
			} else {
				// should only throw if combined characters are included or the charset was broken
				throw new IllegalStateException(
						"Called deprecated #restrictedValidCharacterSet on an extended character set. Please implement #restrictedValidGraphemeSet.");
			}
		}
		final char[] chars = new char[singleCharsExpanded.size()];
		for (int i = 0; i < singleCharsExpanded.size(); i++) {
			chars[i] = singleCharsExpanded.get(i).charAt(0);
		}
		return Optional.of(chars);
	}

	/**
	 * The method specifies, if the generated code restricts the allowed grapheme set. If the
	 * returned list is empty, no additional checks are included in the generated
	 * code. If the method returns a non-empty list, the strings in the list can contain
	 *
	 * <ul>
	 *     <li>Graphemes of length 1 (single characters defined by one Unicode code point)</li>
	 *     <li>Ranges of Graphemes of length 1 (Unicode code point ranges)</li>
	 *     <li>Graphemes of length > 1 (Graphemes made up of code point sequences)</li>
	 * </ul>
	 *
	 * @return a list of Strings containing the valid grapheme set, if restrictions of the
	 * 		character set are specified. Otherwise, an empty list.
	 */
	default List<String> restrictedValidGraphemeSet() {
		final List<String> restrictedDefaultGraphemeSet = new ArrayList<>();
		final Optional<char[]> oldRestrictedValidCharacterSet = restrictedValidCharacterSet();
		if (oldRestrictedValidCharacterSet.isPresent()) {
			final char[] chars = oldRestrictedValidCharacterSet.get();
			for (final char c : chars) {
				restrictedDefaultGraphemeSet.add(String.valueOf(c));
			}
		}
		return restrictedDefaultGraphemeSet;
	}

	/**
	 * Specifies the decimal separator the generated code uses to check the input data.
	 * <p>
	 * Please note: this specification is independent from the {@link Locale}s specified by
	 * {@link ICodeGeneratorParameters#getLanguages()}. At the moment only point (.) or comma (,)
	 * may be used as decimal separators.
	 *
	 * @return the character of the decimal separator.
	 */
	char getDecimalSeparator();

	/**
	 * Returns the meta information, which should be generated. The ordering of the list specifies
	 * the ordering in the generated code.
	 *
	 * @return the list of meta information objects.
	 */
	List<IMetaInfoCodeGenParams> getMetaInfoCodeGenParams();
}
