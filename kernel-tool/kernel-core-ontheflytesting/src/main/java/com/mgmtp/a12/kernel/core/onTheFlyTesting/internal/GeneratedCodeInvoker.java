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

package com.mgmtp.a12.kernel.core.onTheFlyTesting.internal;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;

import com.mgmtp.a12.kernel.core.onTheFlyTesting.a12internal.api.IOnTheFlyTestingValidationParameter;
import com.mgmtp.a12.kernel.core.onTheFlyTesting.a12internal.api.IValidationInvoker;
import com.mgmtp.a12.kernel.core.rt.a12internal.ValidationMode;
import com.mgmtp.a12.kernel.core.rt.a12internal.meta.IMetaModel;
import com.mgmtp.a12.kernel.core.rt.a12internal.validation.ICustomCondition;
import com.mgmtp.a12.kernel.core.rt.a12internal.validation.IValidator;

import groovy.lang.Script;

/**
 * Used to invoke validation on the generated code.
 */
@SuppressWarnings("removal")
class GeneratedCodeInvoker implements IValidationInvoker {

	Script modelGroovyCode;
	IMetaModel metaModel;

	GeneratedCodeInvoker(Script modelGroovyCode) {
		this.modelGroovyCode = modelGroovyCode;
		this.metaModel = (IMetaModel) modelGroovyCode.invokeMethod("getMetaModel", null);
	}

	public IMetaModel getMetaModel() {
		return metaModel;
	}

	@Override
	public void checkAll(IOnTheFlyTestingValidationParameter params) {
		try {
			ValidationMode mode = createValidationMode(params);

			addDefaultValuesToData(metaModel, params);
			Map<String, ICustomCondition> customConditions = params.getCustomConditionsByName();

			IValidator validator = (IValidator) modelGroovyCode.invokeMethod("getValidator", null);
			Method mValidateAllWithDefaults = null;
			for (Method m : validator.getClass().getMethods()) {
				if ("validateAllWithDefaults".equals(m.getName())) {
					mValidateAllWithDefaults = m;
					break;
				}
			}
			if (mValidateAllWithDefaults == null) {
				throw new IllegalArgumentException(
						String.format("Method 'validateAllWithDefaults' not found in '%s'", validator.getClass().getName()));
			}
			mValidateAllWithDefaults.invoke(validator, params.getData(), customConditions, params.getResult(), mode,
					params.getDefaultValues());
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to execute validation.", e);
		}
	}

	private static ValidationMode createValidationMode(IOnTheFlyTestingValidationParameter params) {
		ValidationMode mode = new ValidationMode();
		mode.setLanguage(params.getLanguage());
		mode.setCurrentDateForTest(params.currentDateForTest());
		mode.setPresentationInformation(null);// Notwendig, damit DateTime-Formate in XML-Darstellung validiert werden.
		if(params.getAdditionalInformation4PredefinedFieldTypes()!=null) {
			params.getAdditionalInformation4PredefinedFieldTypes().forEach((k, v) -> mode.setAdditionalInformation(k, v));
		}
		mode.setCustomConditionFactory(params.customConditionFactory());
		mode.setCustomFieldTypeFactory(params.customFieldTypeFactory());
		return mode;
	}

	private void addDefaultValuesToData(IMetaModel metaModel, IOnTheFlyTestingValidationParameter params) {
		DefaultValuesHandler defaultsHandler = new DefaultValuesHandler(metaModel, params.getData(), params.getDefaultValues());
		defaultsHandler.addDefaultValuesToData();
	}
}
