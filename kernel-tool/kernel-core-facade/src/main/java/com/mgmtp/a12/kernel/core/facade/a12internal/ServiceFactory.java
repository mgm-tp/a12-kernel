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
package com.mgmtp.a12.kernel.core.facade.a12internal;

import java.util.List;

import com.mgmtp.a12.kernel.core.calc.internal.CalculationService;
import com.mgmtp.a12.kernel.core.calc.internal.dynEnum.DynamicEnumerationService;
import com.mgmtp.a12.kernel.core.codegen.internal.CodeGeneratorService;
import com.mgmtp.a12.kernel.core.facade.internal.RuleCollectionMetaInformation;
import com.mgmtp.a12.kernel.core.parser.internal.ConditionFormatter;
import com.mgmtp.a12.kernel.core.parser.internal.RuleMetaInformation;
import com.mgmtp.a12.kernel.core.service.internal.ChangeRuleSupport;
import com.mgmtp.a12.kernel.core.service.internal.FieldErrorTextParser;
import com.mgmtp.a12.kernel.core.service.internal.RuleParser;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.services.IBaseCalculationService;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.services.ICalculationService;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.services.IChangeRuleSupport;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.services.ICodeGeneratorService;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.services.IConditionFormatter;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.services.IDynamicEnumerationService;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.services.IFieldErrorTextParser;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.services.IRuleCollectionMetaInformation;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.services.IRuleMetaInformation;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.services.IRuleParser;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.services.IServiceConfiguration;
import com.mgmtp.a12.model.utils.OnlyForUsage;

/**
 * Factory class to create an MVK Service. The project specific settings are specified by the type parameter.
 */
@OnlyForUsage
public class ServiceFactory {

	/**
	 * Get a calculation service.
	 *
	 * @param serviceConfiguration
	 * 		an Object holding the parameters to configure the service, which are:
	 * 		<ul>
	 * 			<li>entityContainer: representing all fields and groups of model</li>
	 * 		  	<li>modelSpecificSetiings: additional model specific properties for the entity container</li>
	 * 		  	<li>projectSpecificSettings: specifies the project specifics</li>
	 * 		</ul>
	 * @return a {@link ICalculationService} for handling field calculations.
	 */
	public ICalculationService getCalculationService(IServiceConfiguration serviceConfiguration) {
		return new CalculationService(serviceConfiguration);
	}

	/**
	 * Get a calculation service.
	 *
	 * @param serviceConfiguration
	 * 		an Object holding the parameters to configure the service, which are:
	 * 		<ul>
	 * 			<li>entityContainer: representing all fields and groups of model</li>
	 * 		  	<li>modelSpecificSetiings: additional model specific properties for the entity container</li>
	 * 		  	<li>projectSpecificSettings: specifies the project specifics</li>
	 * 		</ul>
	 * @param allCalculations
	 * 		the calculation rules, the service should consider.
	 * @return a {@link ICalculationService} for handling field calculations.
	 */
	public ICalculationService getCalculationService(IServiceConfiguration serviceConfiguration,
			List<IBaseCalculationService.ICalculationRule> allCalculations) {
		return new CalculationService(serviceConfiguration, allCalculations);
	}

	/**
	 * Get a service extracting information for dynamic enumerations.
	 *
	 * @param serviceConfiguration
	 * 		the service configuration.
	 * @return an {@link IDynamicEnumerationService} for handling dynamic enumerations.
	 */
	public IDynamicEnumerationService getDynamicEnumerationService(IServiceConfiguration serviceConfiguration) {
		return new DynamicEnumerationService(serviceConfiguration);
	}

	/**
	 * Get a service for code generation.
	 *
	 * @param serviceConfiguration
	 * 		the service configuration
	 * @return an {@link ICodeGeneratorService} for generating code.
	 *
	 * @deprecated Use com.mgmtp.a12.kernel.md.model.api.services.IDocumentModelService#generateValidationCode instead
	 */
	@Deprecated(since = "30.5.0")
	public ICodeGeneratorService getCodeGeneratorService(IServiceConfiguration serviceConfiguration) {
		return new CodeGeneratorService(serviceConfiguration);
	}

	/**
	 * Get a service for parsing rules.
	 *
	 * @param serviceConfiguration
	 * 		the service configuration
	 * @return an {@link IRuleParser} for parsing rules.
	 */
	public IRuleParser getRuleParser(IServiceConfiguration serviceConfiguration) {
		return new RuleParser(serviceConfiguration);
	}

	/**
	 * Get a service providing methods to extract meta information of rule collections.
	 *
	 * @param serviceConfiguration
	 * 		the service configuration
	 * @return an {@link IRuleCollectionMetaInformation} instance.
	 */
	public IRuleCollectionMetaInformation getRuleCollectionMetaInformation(IServiceConfiguration serviceConfiguration) {
		return new RuleCollectionMetaInformation(serviceConfiguration);
	}

	/**
	 * Get a service providing methods to extract meta information of a rule.
	 *
	 * @param serviceConfiguration
	 * 		the service configuration
	 * @return an {@link IRuleMetaInformation} instance.
	 */
	public IRuleMetaInformation getRuleMetaInformation(IServiceConfiguration serviceConfiguration) {
		return new RuleMetaInformation(serviceConfiguration);
	}

	/**
	 * Get a service providing
	 *
	 * @param serviceConfiguration
	 * 		the service configuration
	 * @return an {@link IChangeRuleSupport} instance.
	 */
	public IChangeRuleSupport getChangeRuleSupport(IServiceConfiguration serviceConfiguration) {
		return new ChangeRuleSupport(serviceConfiguration, getRuleParser(serviceConfiguration));
	}

	/**
	 * Get a service for formatting conditions.
	 *
	 * @param serviceConfiguration
	 * 		the service configuration
	 * @return an {@link IConditionFormatter} instance.
	 */
	public IConditionFormatter getConditionFormatter(IServiceConfiguration serviceConfiguration) {
		return new ConditionFormatter(serviceConfiguration, getRuleParser(serviceConfiguration));
	}

	/**
	 * Get a service for parsing field error texts.
	 *
	 * @param serviceConfiguration
	 * 		the service configuration
	 * @return an {@link IFieldErrorTextParser} instance.
	 */
	public IFieldErrorTextParser getFieldErrorTextParser(IServiceConfiguration serviceConfiguration) {
		return new FieldErrorTextParser(serviceConfiguration);
	}
}
