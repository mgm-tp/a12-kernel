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

package com.mgmtp.a12.kernel.core.codegen.internal.validation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.mgmtp.a12.kernel.core.rt.a12internal.ValidationMode;
import com.mgmtp.a12.kernel.core.rt.a12internal.validation.ICustomCondition;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IModelSpecificSettings;
import com.mgmtp.a12.kernel.core.utils.a12internal.metaModel.customConditions.CheckDescription;
import com.mgmtp.a12.kernel.core.utils.a12internal.metaModel.customConditions.CheckEnumOrCategoryValue;
import com.mgmtp.a12.kernel.core.utils.a12internal.metaModel.customConditions.CheckErrorCode;
import com.mgmtp.a12.kernel.core.utils.a12internal.metaModel.customConditions.CheckFieldName;
import com.mgmtp.a12.kernel.core.utils.a12internal.metaModel.customConditions.CheckHintList;
import com.mgmtp.a12.kernel.core.utils.a12internal.metaModel.customConditions.CheckRuleName;
import com.mgmtp.a12.kernel.core.utils.internal.metaModel.customConditions.CheckStringPattern;
import com.mgmtp.a12.kernel.core.utils.internal.services.NameValidationService;
import com.mgmtp.a12.kernel.core.utils.internal.services.serviceparams.FormalErrorCodesProvider;

public class CheckConfiguration {

	public Map<String, ICustomCondition> getCustomConditions(IModelSpecificSettings modelSpecificSettings) {
		NameValidationService nameValidator = getNameValidationService(modelSpecificSettings.getLanguageUsedInConditionText());

		Map<String, ICustomCondition> appConditions = new HashMap<>();
		appConditions.put("ungueltigerFehlerCode", new CheckErrorCode(nameValidator));
		appConditions.put("ungueltigerRegelName", new CheckRuleName(nameValidator));
		// "ungueltigerFeldName" (used for A12 models, name without suffix in order to keep it similar to MM_Field where changing
		// the name of the custom condition would be bad for A12)
		appConditions.put("ungueltigerFeldName", new CheckFieldName(nameValidator, false));
		// "ungueltigerFeldNameHDV" (used for H models)
		appConditions.put("ungueltigerFeldNameHDV", new CheckFieldName(nameValidator, true));
		appConditions.put("ungueltigerNAEOberflaechenWert", new CheckEnumOrCategoryValue());
		appConditions.put("ungueltigerOberflaechenWert", new CheckEnumOrCategoryValue());
		appConditions.put("ungueltigerXMLWert", new CheckEnumOrCategoryValue());
		appConditions.put("ungueltigerKategorieWert", new CheckEnumOrCategoryValue());
		appConditions.put("ungueltigesMusterA12", new CheckStringPattern());
		appConditions.put("ungueltigeBeschreibung", new CheckDescription());
		appConditions.put("invalidValueInHintList", new CheckHintList());
		return appConditions;

	}

	public ValidationMode getValidationMode(IModelSpecificSettings modelSpecificSettings) {
		ValidationMode result = new ValidationMode();
		result.setLanguage(modelSpecificSettings.getLanguageUsedInConditionText());
		return result;
	}

	public static NameValidationService getNameValidationService(Locale languageUsedInConditionText) {
		return new NameValidationService(languageUsedInConditionText, FormalErrorCodesProvider.getFormalErrorCodes());
	}
}
