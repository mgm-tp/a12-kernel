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

import java.util.Set;

import com.mgmtp.a12.kernel.core.rt.a12internal.meta.IMetaField;
import com.mgmtp.a12.kernel.core.rt.a12internal.meta.IMetaGroup;
import com.mgmtp.a12.kernel.core.rt.a12internal.meta.IMetaModel;
import com.mgmtp.a12.kernel.core.rt.a12internal.utils.IMetaKeys;
import com.mgmtp.a12.kernel.core.rt.a12internal.validation.IData;
import com.mgmtp.a12.kernel.core.rt.a12internal.validation.IIdentifier;

/**
 * This class treats the default values supported by the on-the-fly test.
 */
class DefaultValuesHandler {

	private final IMetaModel metaModel;
	private final IData data;
	private final Set<IIdentifier> defaultValues;

	DefaultValuesHandler(IMetaModel metaModel, IData data, Set<IIdentifier> defaultValues) {
		this.metaModel = metaModel;
		this.data = data;
		this.defaultValues = defaultValues;
	}

	void addDefaultValuesToData() {
		if (defaultValues == null) {
			return;
		}
		for (IIdentifier field : defaultValues) {
			data.addValue(field, deriveDefaultValue(field));
		}
	}

	private String deriveDefaultValue(IIdentifier field) {
		IMetaField metaField = (IMetaField) metaModel.getValue(IMetaKeys.MODEL_META_FIELD, field.getName());
		if (metaField == null) {
			IMetaGroup metaGroup = (IMetaGroup) metaModel.getValue(IMetaKeys.MODEL_META_GROUP, field.getName());
			if (metaGroup == null) {
				throw new RuntimeException(String.format("Field '%s' is unknown in the current model.", field.getName()));
			}
		}
		// For default values formal validation is not executed => the value does not matter, just existence of a value is important.
		return "x";
	}
}
