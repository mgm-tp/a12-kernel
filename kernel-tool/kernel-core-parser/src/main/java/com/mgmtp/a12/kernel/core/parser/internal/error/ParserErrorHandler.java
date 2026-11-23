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
package com.mgmtp.a12.kernel.core.parser.internal.error;

import java.util.Locale;

import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.Lenient_ConditionParser;
import com.mgmtp.a12.kernel.core.utils.internal.EnumBasedErrorLookup;

/**
 * Special case of the error lookup using different bundles for different error enum types.
 * Implements additional methods for the enum types that are not returned from
 * {@link #getErrorBundle()}
 */
public class ParserErrorHandler extends EnumBasedErrorLookup<ParserErrorTypesEnum> {

	static final String PARSER_ERROR_BUNDLE = "internal/vk_parser_error_messages/error_messages";
	static final String PARSER_ERROR_OLD_WORLD_BUNDLE = "internal/vk_parser_error_messages_old_world/error_messages";

	public ParserErrorHandler(Locale locale) {
		super(locale);
	}

	@Override
	protected String getErrorBundle() {
		return PARSER_ERROR_BUNDLE;
	}

	public String getParserErrorMessage(ParserErrorTypesEnum errorTypes, Object... args) {
		return getErrorMessage(errorTypes, args);
	}

	// H models:  we have only error messages in German.
	public String getParserErrorMessage(ParserErrorTypesEnumForHModel errorTypes, Object... args) {
		return getErrorMessage(errorTypes.name(), PARSER_ERROR_OLD_WORLD_BUNDLE, args);
	}

	public static void reportError(Lenient_ConditionParser parser, ParserErrorTypesEnum errKey, Object... args) {
		String errMsg = parser.getMVKErrorHandler().getParserErrorMessage(errKey, args);
		MVKParserError err = new MVKParserError(null, null, parser.getRule(), errMsg);
		parser.getPm().reportProblem(err);
	}

	public static void reportError(Lenient_ConditionParser parser, ParserErrorTypesEnumForHModel errKey, Object... args) {
		String errMsg = parser.getMVKErrorHandler().getParserErrorMessage(errKey, args);
		MVKParserError err = new MVKParserError(null, null, parser.getRule(), errMsg);
		parser.getPm().reportProblem(err);
	}
}
