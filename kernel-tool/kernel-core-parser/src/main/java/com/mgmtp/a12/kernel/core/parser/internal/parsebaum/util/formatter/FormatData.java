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
package com.mgmtp.a12.kernel.core.parser.internal.parsebaum.util.formatter;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * UtilClass for formatting.
 */
public class FormatData {

	private static final int INDENT = 4;

	public enum FormatModus {
		NORMAL,
		ARG_LIST,
		BEDINGUNG,
		KLAMMER,
		BLOCK
	}

	static class FormatInfo {
		FormatModus modus;
		int localIndent;

		FormatInfo(FormatModus modus, int localIndent) {
			this.modus = modus;
			this.localIndent = localIndent;
		}
	}

	private StringBuilder line = new StringBuilder();
	private int indentLevel;

	private final int width;
	private String indentStr = "                                                                              "; // JA, viele Blanks!
	private List<String> allLines = new ArrayList<>();
	private Deque<FormatInfo> modi = new LinkedList<>();

	public FormatData(int width, int indentLevel) {
		this.width = width;
		this.indentLevel = indentLevel;
	}

	public FormatData append(String arg) {
		if ((line.length() + arg.length()) > width) {
			if (!modi.isEmpty()) {
				FormatInfo fi = modi.peek();
				switch (fi.modus) {
					case ARG_LIST:
						newline();
						String x = indentStr.substring(0, (fi.localIndent * INDENT));
						line.append(x);
						break;

					case BLOCK:
						break;

					default:
						newline();
						break;
				}
			} else {
				newline();
			}
		}
		line.append(arg);
		return this;
	}

	public FormatData appendWithBreak(String arg) {
		newline();
		line.append(arg);
		return this;
	}

	public FormatData appendNoBreak(String arg) {
		line.append(arg);
		return this;
	}

	public FormatData startModus(FormatModus modus) {
		switch (modus) {
			case ARG_LIST:
				modi.push(new FormatInfo(modus, 2));
				break;
			case BEDINGUNG:
			case BLOCK:
				modi.push(new FormatInfo(modus, indentLevel));
				break;
			case KLAMMER:
				indentLevel++;
				modi.push(new FormatInfo(modus, indentLevel));
				break;
			default:
		}
		return this;
	}

	public FormatData endModus() {
		FormatInfo fi = modi.pop();
		if (fi.modus == FormatModus.KLAMMER) {
			indentLevel--;
		}
		return this;
	}

	public FormatData newline() {
		return newline(indentLevel);
	}

	public FormatData newline(int level) {
		// We do not create empty lines
		if (isCurLineBlank()) {
			return this;
		}
		allLines.add(line.toString().stripTrailing());
		line = new StringBuilder(indentStr.substring(0, (level * INDENT)));
		return this;
	}

	boolean isCurLineBlank() {
		return StringUtils.isBlank(line.toString());
	}

	public String getOutput() {
		if (StringUtils.isNotBlank(line.toString())) {
			allLines.add(line.toString().stripTrailing());
		}
		StringBuilder ret = new StringBuilder();

		boolean first = true;
		for (String s : allLines) {
			if (first) {
				first = false;
			} else {
				ret.append("\n");
			}
			ret.append(s);
		}
		return ret.toString();
	}

}
