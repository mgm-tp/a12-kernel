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

package com.mgmtp.a12.kernel.core.service.internal.errortext;

import org.apache.commons.lang3.StringUtils;

import com.mgmtp.a12.kernel.core.extmpspecifics.internal.utils.ExtmpUtils;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IFieldType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IModelSpecificSettings;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblemReporter;
import com.mgmtp.a12.kernel.core.utils.internal.CommonUtils;
import com.mgmtp.a12.kernel.core.utils.internal.MVKCoreError;

/**
 * Checks the error text of a {@link IFieldType}.
 */
public class FieldErrortextChecker {

	private final FehlertextErrorLookup errorHandler;
	private final IModelSpecificSettings modelSpecificSettings;
	private final IProblemReporter problemManager;

	public FieldErrortextChecker(IModelSpecificSettings modelSpecificSettings, IProblemReporter problemManager) {
		this.errorHandler = new FehlertextErrorLookup(modelSpecificSettings.getLanguageUsedInConditionText());
		this.modelSpecificSettings = modelSpecificSettings;
		this.problemManager = problemManager;
	}

	/**
	 * Checks the error text of a field (type).
	 *
	 * @param errorText
	 * 		Text to be checked
	 * @param field
	 * 		the error text's {@link IField}
	 * @return <code>true</code>, if the text is OK. Otherwise, <code>false</code> is returned and
	 * 		the error message reported in the problem manager.
	 */
	public boolean checkErrorText(String errorText, IField field) {
		return checkErrorText(errorText, field, false);
	}

	/**
	 * Checks the error text of a field (type).
	 *
	 * @param errorText
	 * 		Text to be checked
	 * @param field
	 * 		the error text's {@link IField}
	 * @param extendErrorMessage
	 * 		specifies, whether a message regarding the field should be prepended.
	 * @return <code>true</code>, if the text is OK. Otherwise, <code>false</code> is returned and
	 * 		the error message is reported in the problem manager.
	 */
	public boolean checkErrorText(String errorText, IField field, boolean extendErrorMessage) {
		String result = checkErrorTextInternal(errorText);
		if (result != null) {
			if (extendErrorMessage) {
				result = errorHandler.getBareErrorMessage(FehlertextErrorType.FIELD_INVALID_PREFIX, field.getFullName(), result);
			}
			problemManager.reportProblem(new MVKCoreError(result, field));
			return false;
		}

		return true;
	}

	private String checkErrorTextInternal(String errorText) {

		// (1) The check, regarding if the definition of an error message is allowed, is done through the meta model
		if (StringUtils.isEmpty(errorText)) {
			return null;
		}

		// (2) Right encoding? ISO-8859-15 for ExtMP, Unicode for A12
		if (ExtmpUtils.isExtmp(modelSpecificSettings)) {
			if (!FehlertextUtil.isFehlertextCharsetOK(errorText)) {
				return errorHandler.getErrorMessage(FehlertextErrorType.WRONG_ENCODING, "ISO-8859-15");
			}
		} else {
			if (!FehlertextUtil.isFehlertextCharsetUnicodeOK(errorText)) {
				return errorHandler.getErrorMessage(FehlertextErrorType.WRONG_ENCODING, "Unicode");
			}
		}

		// (3) line breaks present?
		if (!FehlertextUtil.checkLineSeparators(errorText)) {
			return errorHandler.getErrorMessage(FehlertextErrorType.INVALID_LINEBREAK);
		}

		if (!FehlertextUtil.checkControlCharacters(errorText)) {
			return errorHandler.getErrorMessage(FehlertextErrorType.INVALID_CHARACTERS);
		}

		// (4) correct number of $-chars?
		if (!FehlertextUtil.checkWrongNumberOfDollars(errorText)) {
			return errorHandler.getErrorMessage(FehlertextErrorType.WRONG_NUMBER_OF_DOLLARS);
		}
		String invalidValue = check_INVALID_PARAMETER(errorText);
		if (invalidValue != null) {
			return errorHandler.getErrorMessage(FehlertextErrorType.INVALID_PARAMETER, invalidValue);
		}

		return null;
	}

	/**
	 * Checks every second token to be <i>Feld</i>/<i>field</i> or <i>Feld.Wert</i>/
	 * <i>field.value</i>, depending on <code>languageOfConditionText</code>.
	 *
	 * @return <code>null</code>, if everything is OK. Otherwise, the invalid parameter value is
	 * 		returned.
	 */
	private String check_INVALID_PARAMETER(String errorText) {
		String[] parts = errorText.split("\\$");

		// Check only parts within the $-signs
		for (int i = 1; i < parts.length; i = i + 2) {
			String value = parts[i];
			if (!CommonUtils.CONST_FIELD.get(modelSpecificSettings.getLanguageUsedInConditionText()).contains(value)) {
				return value;
			}
		}

		return null;
	}

}
