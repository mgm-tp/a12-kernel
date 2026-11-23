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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mgmtp.a12.kernel.core.customfieldtype.CustomFieldTypeService;
import com.mgmtp.a12.kernel.core.onTheFlyTesting.a12internal.api.IOnTheFlyTestingParameter;
import com.mgmtp.a12.kernel.core.onTheFlyTesting.a12internal.api.IOnTheFlyTestingService;
import com.mgmtp.a12.kernel.core.onTheFlyTesting.a12internal.api.IValidationInvoker;
import com.mgmtp.a12.kernel.core.onTheFlyTesting.a12internal.api.IValidationInvokerCache;
import com.mgmtp.a12.kernel.core.onTheFlyTesting.a12internal.error.TestRuleErrorLookup;
import com.mgmtp.a12.kernel.core.onTheFlyTesting.a12internal.error.TestRuleErrorType;
import com.mgmtp.a12.kernel.core.parser.internal.ParserService;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.ExpandMode;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.rt.a12internal.meta.IMetaModel;
import com.mgmtp.a12.kernel.core.rt.a12internal.utils.IMetaKeys;
import com.mgmtp.a12.kernel.core.rt.a12internal.validation.IIdentifier;
import com.mgmtp.a12.kernel.core.service.internal.extservices.ReferencedFieldsWithValueAccessVisitor;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IRule;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.services.IMVK_Service;
import com.mgmtp.a12.kernel.core.utils.internal.HashingHelper;
import com.mgmtp.a12.kernel.core.utils.internal.MVKCoreError;
import com.mgmtp.a12.model.notification.Severity;

public class OnTheFlyTestingService implements IOnTheFlyTestingService {

	static {
		// For groovy version 3 there is a compilation issue for each new generated code, caused by the newly introduced Parrot parser, which is disabled with setting groovy.antlr4 to false.
		// In future groovy versions we need to check, if we still need to disable this parser, so that the groovy compilation does not slow down.
		System.setProperty("groovy.antlr4", "false");
	}

	private static final Logger LOG = LoggerFactory.getLogger(OnTheFlyTestingService.class);

	private final IMVK_Service mvkService;
	private final IValidationInvokerCache scriptCache;
	private final Map<Locale, TestRuleErrorLookup> errorLookupCache = new HashMap<>();

	public OnTheFlyTestingService(IMVK_Service mvkService, IValidationInvokerCache scriptCache) {
		this.mvkService = mvkService;
		this.scriptCache = scriptCache;
	}

	@Override
	public void testRule(Collection<? extends IRule> rules, IOnTheFlyTestingParameter params) {
		StopWatch sw = new StopWatch();
		sw.start();
		doOnTheFlyTest(rules, params);
		sw.stop();
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("%s ms in 'testRule'", sw.getDuration().toMillis()));
		}
	}

	@Override
	public void testField(IOnTheFlyTestingParameter params) {
		StopWatch sw = new StopWatch();
		sw.start();
		doOnTheFlyTest(new HashSet<>(), params);
		sw.stop();
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("%s ms in 'testField'", sw.getDuration().toMillis()));
		}
	}

	private void doOnTheFlyTest(Collection<? extends IRule> rules, IOnTheFlyTestingParameter params) {
		/*
		 * This would be better called in GeneratedCodeInvoker. But GeneratedCodeInvoker should not
		 * depend on the rules and the parser service, which are necessary for the check.
		 */
		if (!checkDefaultValues(rules, params)) {
			return;
		}
		Optional<IValidationInvoker> invokerOpt = getValidatorFromCache(rules, params);
		if (invokerOpt.isEmpty()) {
			return;
		}
		GeneratedCodeInvoker invoker = (GeneratedCodeInvoker) invokerOpt.get();
		if (checkCustomFieldTypes(invoker, params)) {
			invoker.checkAll(params);
		}
	}

	// A12K-3589 remove this SuppressWarnings
	@SuppressWarnings("removal")
	private boolean checkCustomFieldTypes(GeneratedCodeInvoker invoker, IOnTheFlyTestingParameter params) {
		IMetaModel metaModel = invoker.getMetaModel();
		@SuppressWarnings("unchecked")
		Set<String> customFieldTypesToBeProvided = (Set<String>) metaModel.getValue(
				IMetaKeys.MODEL_CUSTOM_FIELD_TYPES_TO_BE_PROVIDED);
		if (!customFieldTypesToBeProvided.isEmpty()) {
			Set<String> supportedCustomFieldtypes = new HashSet<>();
			for (String cftName : customFieldTypesToBeProvided) {
				if (params.customFieldTypeFactory() != null) {
					if (params.customFieldTypeFactory().createCustomFieldValidator(cftName).isPresent()) {
						supportedCustomFieldtypes.add(cftName);
					}
				} else {
					if (CustomFieldTypeService.getInstance().getCustomFieldType(cftName).isPresent()) {
						supportedCustomFieldtypes.add(cftName);
					}
				}
			}
			if (!supportedCustomFieldtypes.containsAll(customFieldTypesToBeProvided)) {
				String missingCustomFieldTypes = customFieldTypesToBeProvided.stream()
						.filter(cft -> !supportedCustomFieldtypes.contains(cft))
						.collect(Collectors.joining(", "));
				params.getProblemReporter().reportProblem(new MVKCoreError(
						"Implementierung für vordefinierten Feldtyp '" + missingCustomFieldTypes + "' nicht gefunden.",
						null));
				return false;
			}
		}
		return true;
	}

	private Optional<IValidationInvoker> getValidatorFromCache(Collection<? extends IRule> rules,
			IOnTheFlyTestingParameter params) {
		OnTheFlyTestingValidatorGenerator generator = new OnTheFlyTestingValidatorGenerator(mvkService, params);
		Optional<String> generatedCode = generator.generateCode(rules);
		if (generatedCode.isEmpty()) {
			return Optional.empty();
		}
		String hashCode = HashingHelper.calculateHash(generatedCode.get());
		return Optional.of(scriptCache.computeIfAbsent(hashCode, () -> generator.generateValidator(generatedCode.get())));
	}

	/**
	 * Checks if any rule references the field value of any field, for which default value is used.
	 *
	 * @param rules
	 * @param params
	 * @return false, if a default value is referenced (which is not allowed), true otherwise
	 */
	private boolean checkDefaultValues(Collection<? extends IRule> rules, IOnTheFlyTestingParameter params) {
		ParserService ps = new ParserService(mvkService);
		Map<String, List<IIdentifier>> defaultValuesByFieldName = params.getDefaultValues().stream()
				.collect(Collectors.groupingBy(IIdentifier::getName));

		for (IRule rule : rules) {
			ICondition condition = ps.parseAndCheckCondition(rule, rule.getErrorConditionText(), params.getProblemReporter(),
					ExpandMode.EXPAND);
			if (condition == null) { // invalid condition
				return false;
			}
			List<IEntityId> fields = ReferencedFieldsWithValueAccessVisitor.getReferencedFieldsWithValueAccess(condition,
					mvkService.getIEC());
			Optional<IEntityId> field = fields.stream()
					.filter(f -> defaultValuesByFieldName.containsKey(f.getFullName()))
					.findAny();

			if (field.isPresent()) {
				TestRuleErrorLookup errorLookup = errorLookupCache.computeIfAbsent(
						mvkService.getModelSpecificSettings().getLanguageUsedInConditionText(), TestRuleErrorLookup::new);
				IIdentifier defaultValue = defaultValuesByFieldName.get(field.get().getFullName()).iterator().next();
				String errMsg = errorLookup.getErrorMessage(TestRuleErrorType.MVK_RULE_TEST_NO_DEFAULT_VALUE,
						params.getData().identifierToString(defaultValue));
				params.getProblemReporter().reportProblem(new MVKCoreError(errMsg, Severity.INFO, rule));
				return false;
			}
		}
		return true;
	}

}
