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

package com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.mgmtp.a12.kernel.core.base.utils.internal.CharsetUtil;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.ICodeGeneratorParameters.ProgrammingLanguage;

/**
 * Helper class for outputting the array of allowed characters to the Meta_-class.
 */
public class EncodingUtil {

	// carriage return and line feed
	public static final List<int[]> FORBIDDEN_CODEPOINTS_VBA = List.of(new int[] { 10, 10 }, new int[] { 13, 13 });

	private EncodingUtil() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Generate the code for LEGAL_CHARACTERS in a <code>Meta_&lt;name&gt;</code> class.
	 *
	 * @param allowedChars
	 * 		the allowed characters as an array. Must not be <code>null</code>.
	 * @param progLang
	 * 		the programming language. For C++, the encoding of the output is always
	 * 		ISO-8859-15. Therefore, some conversions need to be done.
	 * @return the code to output for the LEGAL_CHARACTERS.
	 */
	public static String getAllowedCharsCode(int[] allowedChars, ProgrammingLanguage progLang) {
		if (progLang.equals(ProgrammingLanguage.VBA)) {
			return getAllowedCharsCodeVBA(allowedChars);
		} else {
			StringBuilder result = new StringBuilder();
			result.append("   ");
			for (int ii = 0; ii < allowedChars.length; ii++) {

				if (ii > 0) {
					result.append(", ");
				}

				if (progLang.equals(ProgrammingLanguage.CPLUS)) {
					result.append(convertForCpp(allowedChars[ii]));
				} else {
					int i = allowedChars[ii];
					if (i < 100) {
						// Stelle sicher dass alle Zeichen gleich viel Platz einnehmen
						// damit das Zeichen im Kommentar richtig positioniert wird.
						result.append(" ");
					}
					result.append(i);
				}
			}
			return result.toString();
		}
	}

	public static Optional<int[][]> allowedRanges(final List<int[]> parsedRanges) {
		if (parsedRanges.isEmpty()) {
			return Optional.empty();
		}
		final int[] starts = parsedRanges.stream().mapToInt(r -> r[0]).toArray();
		final int[] ends = parsedRanges.stream().mapToInt(r -> r[1]).toArray();
		return Optional.of(new int[][] { starts, ends });
	}

	public static Optional<List<int[]>> allowedCombinedGraphemes(final Set<String> restrictedGraphemeSet) {
		if (restrictedGraphemeSet.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(restrictedGraphemeSet.stream().map(String::codePoints)
				.map(IntStream::toArray)
				.toList());
	}

	/**
	 * Generates the code fragments to initialize the Trie that gets passed to the LegalCharTester.
	 * If the programming language does not yet support combined graphemes, this function will return the empty string.
	 *
	 * @param legalGraphemes
	 * 		A list of legal combined graphemes
	 * @param progLang
	 * 		The target programming language
	 * @return String with code lines or the empty string if the programming language is not expected/implemented
	 */
	public static String graphemeTrieInitialization(final List<int[]> legalGraphemes, final ProgrammingLanguage progLang) {
		final StringBuilder code = new StringBuilder();
		String prefix;
		String suffix;
		final String hexPrefix = progLang == ProgrammingLanguage.VBA ? "&H" : "0x";
		final String hexSuffix = progLang == ProgrammingLanguage.VBA ? "&" : "";
		String newLine = "\r\n";
		String lineIndent = "    ";
		switch (progLang) {
			case JAVA:
				prefix = "LEGAL_GRAPHEME_TRIE.insert(new int[]{";
				suffix = "});";
				break;
			case GROOVY:
				prefix = "LEGAL_GRAPHEME_TRIE.insert([";
				suffix = "] as int[]);";
				break;
			case CPLUS:
				prefix = "{ ";
				suffix = " },";
				lineIndent = "\t\t";
				break;
			case JAVA_SCRIPT_VK:
				prefix = "legalGraphemeTrie.insert([";
				suffix = "]);";
				break;
			case VBA:
				prefix = "Call LEGAL_GRAPHEME_TRIE.insert(Array(";
				suffix = "))";
				break;
			default:
				return "";
		}
		for (final int[] grapheme : legalGraphemes) {
			code.append(lineIndent);
			code.append(prefix);
			code.append(hexPrefix);
			code.append(String.format("%04X", grapheme[0]));
			code.append(hexSuffix);
			for (int i = 1; i < grapheme.length; i++) {
				code.append(", ");
				code.append(hexPrefix);
				code.append(String.format("%04X", grapheme[i]));
				code.append(hexSuffix);
			}
			code.append(suffix);
			code.append(newLine);
		}

		return code.toString();
	}

	private static String getAllowedCharsCodeVBA(final int[] allowedChars) {
		final StringBuilder result = new StringBuilder();
		for (int i = 0; i < allowedChars.length; i++) {
			result.append(allowedChars[i]);
			if (i != allowedChars.length - 1) {
				// Zeilenumbruch nach 18 Symbolen, um ewig lange Zeile zu vermeiden
				final String seperator = (i + 1) % 18 == 0 ? ", _\r\n" : ", ";
				result.append(seperator);
			}
		}
		return result.toString();
	}

	/**
	 * Remove forbidden codepoints for VBA code from ranges of a charset.
	 *
	 * @param ranges
	 * 		the ranges to be filtered
	 * @return a new list of ranges where forbidden chars are not included.
	 */
	public static List<int[]> removeCrLfFromRangesForVba(final List<int[]> ranges) {
		final List<int[]> filteredRanges = new ArrayList<>();
		for (final int[] range : ranges) {
			if (range[0] > 13) { // no need to even try to find invalid chars - ranges are sorted
				filteredRanges.add(range);
			} else {
				final List<int[]> splitRanges = CharsetUtil.removeForbiddenRanges(range, FORBIDDEN_CODEPOINTS_VBA);
				if (!splitRanges.isEmpty()) {
					filteredRanges.addAll(splitRanges);
				}
			}
		}
		return filteredRanges;
	}

	private static String convertForCpp(int character) {
		StringBuilder result = new StringBuilder();
		result.append(character);

		// append the explicit sign in a comment
		result.append(" /*");
		if (character == '\n') {
			result.append("\\n");
		} else if (character == '\r') {
			result.append("\\r");
		} else {
			result.append(character);
		}
		result.append("*/");
		return result.toString();
	}

}
