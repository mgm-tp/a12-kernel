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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

import com.mgmtp.a12.kernel.core.rt.a12internal.validation.IData;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.ICodeGeneratorParameters.ProgrammingLanguage;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.config.IProjectSpecificSettings;
import com.mgmtp.a12.kernel.core.utils.internal.metaModel.IdentifierImpl;
import com.mgmtp.a12.model.header.Annotation;

/**
 * Common utility methods to fill fields in the meta model.
 */
public class DataCollectorUtils {

	public static final String PATH_PREFIX_MODELPROPERTIES = "/MVK[1]/ModelProperties[1]/";

	private DataCollectorUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Adds the given id-value pair to the given data. If the given value is blank, the id-value
	 * pair won't be added to the given data.
	 */
	public static void addValue(IData data, String id, String value) {
		if (StringUtils.isBlank(value)) {
			return;
		}
		data.addValue(IdentifierImpl.create(id), value.trim());
	}

	/**
	 * For each given value an id will be generated based on the given pattern and this id-value
	 * pair will be added to the given data (see also
	 * {@link DataCollectorUtils#addValue(IData, String, String)}).
	 */
	public static void addValueCollection(IData data, String idPattern, Collection<String> values) {
		AtomicInteger idx = new AtomicInteger();
		values.forEach(v -> DataCollectorUtils.addValue(data, String.format(idPattern, idx.incrementAndGet()), v));
	}

	/**
	 * For other field types than predefined types: Creates an id for the given error message and
	 * adds the id-value pair to the given data (see also
	 * {@link DataCollectorUtils#addValue(IData, String, String)}). If the given error message
	 * is blank, the id-value pair won't be added to the given data.
	 */
	public static void addErrorMessage(IData data, String parentPrefix, String language, String errorMessage) {
		addErrorMessageIntern(data, parentPrefix, language, errorMessage, "errorMessage");
	}

	/**
	 * For predefined types: creates an id for the given error message and adds the id-value pair to
	 * the given data (see also {@link DataCollectorUtils#addValue(IData, String, String)}). If the
	 * given error message is blank, the id-value pair won't be added to the given data.
	 */
	public static void addErrorMessage4PredefinedType(IData data, String parentPrefix, String language, String errorMessage) {
		addErrorMessageIntern(data, parentPrefix, language, errorMessage, "text");
	}

	private static void addErrorMessageIntern(IData data, String parentPrefix, String language, String errorMessage,
			String mmFieldName4ErrorMessage) {
		if (StringUtils.isBlank(errorMessage)) {
			return;
		}
		DataCollectorUtils.addValue(data, parentPrefix + "language[1]", language);
		DataCollectorUtils.addValue(data, parentPrefix + mmFieldName4ErrorMessage + "[1]", errorMessage);
	}

	static void addHintList(IData data, String parentPrefix, String language, List<String> values) {
		DataCollectorUtils.addValue(data, parentPrefix + "language[1]", language);
		AtomicInteger idx = new AtomicInteger();
		values.forEach(
				v -> DataCollectorUtils.addValue(data, String.format(parentPrefix + "Values[%d]/value[1]",
						idx.incrementAndGet()), v));
	}

	/**
	 * Adds all given language strings to the given data (see also
	 * {@link DataCollectorUtils#addValueCollection(IData, String, Collection)}).
	 *
	 * @param langStrs
	 * 		locale converted to language string
	 */
	public static void addSupportedLanguages(IData data, Collection<String> langStrs) {
		DataCollectorUtils.addValueCollection(data, PATH_PREFIX_MODELPROPERTIES + "SupportedLanguages[%d]/language[1]", langStrs);
	}

	/**
	 * Adds the model property oldWorld to the given data (see also
	 * {@link DataCollectorUtils#addValue(IData, String, String)}).
	 */
	public static void addIsOldWorld(IData data, boolean isHModel) {
		if (isHModel) {
			DataCollectorUtils.addValue(data, PATH_PREFIX_MODELPROPERTIES + "oldWorld[1]", Boolean.TRUE.toString());
		}
	}

	/**
	 * Adds the given programming language to the given data (see also
	 * {@link DataCollectorUtils#addValue(IData, String, String)}).
	 */
	public static void addProgrammingLanguageToModelProperties(IData data, ProgrammingLanguage programmingLanguage) {
		final String programmingLanguageStr = programmingLanguage.name();
		DataCollectorUtils.addValue(data, PATH_PREFIX_MODELPROPERTIES + "programmingLanguage[1]", programmingLanguageStr);
	}

	/**
	 * Adds the max digits from {@link IProjectSpecificSettings#MAXDIGITS_IN_NUMBERS} to the given
	 * data.
	 */
	public static void addMaxDigits(IData data) {
		final int maxDigits = IProjectSpecificSettings.MAXDIGITS_IN_NUMBERS;
		DataCollectorUtils.addValue(data, PATH_PREFIX_MODELPROPERTIES + "maxDigits[1]", String.valueOf(maxDigits));
	}

	public static void addAnnotations(IData data, String parentPrefix, List<Annotation> annotations) {
		String idPrefixPattern = parentPrefix + "Annotations[1]/Annotation_list[%d]/";
		AtomicInteger idx = new AtomicInteger();
		annotations.stream().forEach(e -> {
			String idPrefix = String.format(idPrefixPattern, idx.incrementAndGet());
			addValue(data, idPrefix + "name[1]", e.getName());
			addValue(data, idPrefix + "value[1]", e.getValue());
		});
	}
}
