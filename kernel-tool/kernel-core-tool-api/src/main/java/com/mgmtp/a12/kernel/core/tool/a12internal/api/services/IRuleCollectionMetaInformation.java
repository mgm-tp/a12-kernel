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

package com.mgmtp.a12.kernel.core.tool.a12internal.api.services;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IRule;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IMandatoryInformation;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IModelChanges;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblemReporter;
import com.mgmtp.a12.model.utils.OnlyForUsage;

@OnlyForUsage
public interface IRuleCollectionMetaInformation {

	/**
	 * <b>Precondition</b>
	 * <br>
	 * Rule condition text of all rules must be valid. Problems are reported to the problem
	 * reporter. The method returns all names of the application conditions in form of a list.
	 *
	 * @param rules
	 * 		specifies a collection of rules: at minimum condition text, fullname and error
	 * 		field must be provided; the collection and its content may not be null
	 * @param pr
	 * 		problem reporter, may not be null
	 * @return names of all application conditions used in the rules. The list is empty, if no
	 * 		application conditions are used.
	 */
	public List<String> getApplicationConditionNames(Collection<? extends IRule> rules, IProblemReporter pr);

	/**
	 * Can be called no matter which expand mode was used for parsing the conditions.
	 *
	 * @param conditions
	 * 		Collection of conditions to be analyzed, may not be null.
	 * @return Names of all application conditions used in the <code>conditions</code>. If one of the conditions is null it is
	 * 		ignored.
	 */
	public List<String> getApplicationConditionNames(Collection<ICondition> conditions);

	/**
	 * Analyzes all rules to get the information, if a field is mandatory or mandatory regarding its
	 * root group or if a root group is mandatory. <br>
	 * Some simple examples:
	 * <ul>
	 * <li>FieldNotFilled(/A/a) specifies that /A/a is mandatory.</li>
	 * <li>GroupFilled(/A) and FieldNotFilled(/A/a) specifies that /A/a is mandatory regarding its
	 * root group.</li>
	 * <li>GroupNotFilled(/A) specifies that /A is mandatory.</li>
	 * </ul>
	 * Notes:
	 * <ul>
	 * <li>A field or root group is only regarded as mandatory if it is mandatory for all
	 * repetitions, i.e. 'NoFieldFilled(g&#42;/f)' or 'NoGroupFilled(/A*)' do not specify a
	 * mandatory field or root group, respectively.</li>
	 * <li>A mandatory field implies that it is also mandatory regarding its root group.</li>
	 * </ul>
	 * <br>
	 * <b>Precondition</b>
	 * <br>
	 * Rule condition text of all rules must be valid. Problems are reported to the problem
	 * reporter.
	 *
	 * @param rules
	 * 		specifies the collection of rules: at minimum condition text, fullname and error
	 * 		field must be provided; the collection and its content may not be null
	 * @param pr
	 * 		problem reporter; may not be null
	 * @return information about the mandatory fields. Never returns null. If no mandatory
	 * 		information is found, the mandatory information provides this information.
	 * 		<br>
	 * 		In case of an error, the return value is empty.
	 */
	public IMandatoryInformation getMandatoryInformationFromRules(Collection<? extends IRule> rules, IProblemReporter pr);

	/**
	 * Precondition: The conditions have to be expanded, i.e. the condition text has to be parsed
	 * with ExpandMode "EXPAND".
	 *
	 * @param rules
	 * 		Contains for each rule the parsed condition. The map and the rules may not be
	 * 		null. If a condition is null, it is ignored.
	 * @return information about the mandatory fields and root groups
	 */
	IMandatoryInformation getMandatoryInformationFromRules(Map<IRule, ICondition> rules);

	/**
	 * Checks all rules, if the condition specifies a change in a max value of an entity and the
	 * rule is an error rule.
	 * <br>
	 * A rule may not reduce the max value of a repeatable layer to 1. If a rule specifies such a
	 * change or if the condition text of a rule is invalid, empty model changes are returned and
	 * the problem is reported to the problem reporter.
	 *
	 * @param rules
	 * 		list of rules to analyze. The collection and its content may not be null.
	 * @param pr
	 * 		problem reporter, may not be null
	 * @return the changes to the model as specified by the rules or an empty set, if no changes are
	 * 		specified.
	 * 		<p>
	 * 		In case of an error, the return value is empty.
	 */
	IModelChanges getChangesAsSpecifiedByRules(Collection<? extends IRule> rules, IProblemReporter pr);

	/**
	 * Checks all rules, if the condition specifies a change in a max value of an entity and the
	 * rule is an error rule.
	 * <br>
	 * A rule may not reduce the max value of a repeatable layer to 1. If a rule specifies such a
	 * change or if the condition text of a rule is invalid, empty model changes are returned and
	 * the problem is reported to the problem reporter.
	 *
	 * @param ruleToCondition
	 * 		map of rules to analyze with their corresponding parsed condition. The map and its content may not be null.
	 * @param pr
	 * 		problem reporter, may not be null
	 * @return the changes to the model as specified by the rules or an empty set, if no changes are
	 * 		specified.
	 * 		<p>
	 * 		In case of an error, the return value is empty.
	 */
	IModelChanges getChangesAsSpecifiedByRules(Map<IRule, ICondition> ruleToCondition, IProblemReporter pr);

	/**
	 * Search the given rules for conditions of the form
	 * <code>Length([field]) &gt; [constant]</code> where the field type of [field] is a string
	 * without value validation.
	 * <p>
	 * Return a map from field name to the constant value given in the condition.
	 *
	 * @param rules
	 * 		the rules to search.
	 * @param pr
	 * 		the problem reporter, problems will be reported to. If problems are found for a
	 * 		rule, the rule is ignored during search.
	 * @return a {@link Map} containing the full names of the fields as keys an the corresponding
	 * 		constant values of the constant from the condition.
	 */
	Map<String, Integer> getNoValidationLength(List<IRule> rules, IProblemReporter pr);

	/**
	 * Extract the values for "constant dynamic enumerations".
	 * <p>
	 * Constant dynamic enumeration rules have the form
	 *
	 * <pre>
	 * 	FieldFilled(F)
	 * 		And [Field] != "const1"
	 * 		And [Field] != "const2"
	 * 		...
	 * </pre>
	 * <p>
	 * For corresponding rules the full name of the field (e.g. <code>/RG/G/F</code>) with the
	 * allowed enumeration values (e.g. <code>["const1", "const2", ...]</code>) will be added to the
	 * result.
	 *
	 * @param rules
	 * 		the rules to study.
	 * @param pr
	 * 		contains errors for rules whose condition text is not parseable or which are no
	 * 		general dynamic enumerations in form.
	 * @return a map, whose keys are the dynamic enumeration fields and the values are the lists of
	 * 		corresponding constant values for the dynamic enumeration.
	 */
	Map<String, List<String>> getDynamicEnumerationStringValues(List<IRule> rules,
			IProblemReporter pr); // (Feld-Vollname, Erlaubte Werte)

}
