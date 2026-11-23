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

package com.mgmtp.a12.kernel.core.rt.a12internal.utils;

import java.util.List;
import java.util.Locale;

import com.mgmtp.a12.kernel.core.rt.a12internal.Pair;
import com.mgmtp.a12.kernel.core.rt.a12internal.PresentationInformation;
import com.mgmtp.a12.kernel.core.rt.a12internal.ValidationMode;
import com.mgmtp.a12.kernel.core.rt.a12internal.meta.IMetaField;
import com.mgmtp.a12.kernel.core.rt.a12internal.meta.IMetaGroup;
import com.mgmtp.a12.kernel.core.rt.a12internal.meta.IMetaModel;
import com.mgmtp.a12.kernel.core.rt.a12internal.meta.IMetaRule;
import com.mgmtp.a12.kernel.core.rt.a12internal.validation.IData;
import com.mgmtp.a12.kernel.core.rt.a12internal.validation.IIdentifier;
import com.mgmtp.a12.kernel.core.rt.a12internal.validation.IResult;
import com.mgmtp.a12.model.utils.OnlyForUsage;

/**
 * Interface specifying constants for the string values of publicly available keys for the Meta API.
 */
@OnlyForUsage
public interface IMetaKeys {

	/**
	 * The field name must be passed as an extra parameter.
	 * <p>
	 * Returns a {@link IMetaField}, if a field with the given name exists, otherwise <code>null</code> is returned. If the short
	 * name of the field is unique within the model, this short name or the full field name can be used to identify the field,
	 * otherwise just the full field name may be used.
	 */
	String MODEL_META_FIELD = "MetaFeld";
	/**
	 * A <code>Set&lt;{@link IIdentifier}&gt;</code> has to be passed as an additional parameter.
	 * <p>
	 * Returns a <code>Set&lt;{@link IIdentifier}&gt;</code> which are mandatory relative to the fields passed as parameter. If
	 * faulty parameters are passed, <code>null</code> is returned. If there are no mandatory fields found, then an empty set is
	 * returned. For performance reasons the returned set is not copied and so may not be changed by the caller.
	 */
	String MODEL_MANDATORY_FIELDS = "PflichtFelder";
	/**
	 * As an additional parameter the full name of a rule is expected.
	 * <p>
	 * Returns an {@link IMetaRule} object or <code>null</code>, if no rule with the given name exists.
	 * <p>
	 * Note: If the generate was generated for an application without UI support, it always returns <code>null</code>.
	 */
	String MODEL_META_RULE = "MetaRegel";
	/**
	 * As an additional parameter the full name of a group is expected.
	 * <p>
	 * Returns {@link IMetaGroup} object or <code>null</code>, if no group with the given name exists.
	 */
	String MODEL_META_GROUP = "MetaContainer";
	/**
	 * Returns the set of groups directly contained in the root group as a <code>List&lt;{@link IMetaGroup}&gt;</code>. The ordering
	 * in the List corresponds to the ordering passed when generating code.
	 */
	String MODEL_GROUP_LIST = "ContainerListe";
	/**
	 * Returns all names of the application conditions used in the model type as a <code>Set&lt;Strings&gt;</code> which are used
	 * in the model type. If no application conditions are used in the model type, an empty set is returned. The order in which the
	 * conditions are returned has no meaning.
	 */
	String MODEL_APPLICATION_CONDITION = "ApplikationsBedingungen";
	/**
	 * Returns the Set&lt;String&gt; of predefined types that must be supplied by the application.
	 */
	String MODEL_CUSTOM_FIELD_TYPES_TO_BE_PROVIDED = "CustomFieldtypesToBeProvided";
	/**
	 * Returns a <code>List&lt;{@link Locale}&gt;</code>, containing all the languages supported by this generate.
	 */
	String MODEL_LANGUAGES = "Sprachen";
	/**
	 * Returns the set of fields as a Set of {@link IMetaField}, which are the calculated fields in a calculation. If there are no
	 * calculated fields, the Set is empty.
	 */
	String MODEL_CALC_FIELDS = "BerechneteFelder";
	/**
	 * Expects the following additional parameters:
	 * <ol>
	 * <li>The first parameter must be an {@link IData}-object containing the data set to be sorted.</li>
	 * <li>The second parameter must be an {@link IResult} taking possible errors. The given fields will be checked syntactically
	 * and potentially occurring errors will be placed here. In this case, the original data set will be returned.</li>
	 * <li>The third parameter is optional. An {@link com.mgmtp.a12.kernel.core.customfieldtype.ICustomFieldTypeFactory}-object must be supplied if the model contains custom field types.</li>
	 * <li>As an additional optional parameter a {@link Locale}-object can be supplied to be used as the language for the error messages put in the second parameter.</li>
	 * </ol>
	 * <p>
	 * Returns a copy of the given data set in sorted order. The sorting order is specified in the model by special sorting
	 * criteria. If no sorting criteria are given or formal errors are found in the given data, the original data set is returned.
	 * <p>
	 * The returned value will be an {@link IData}-object independent of the original, specially the method
	 * {@link IData#identifierToString(IIdentifier)}, which returns a default string representation of the given
	 * {@link IIdentifier}-object. The order of the {@link IIdentifier}-objects, which is given through {@link IData#iterator()}, is
	 * independent of the sorted data because this sort affects only the indices. The returned values are only xml-values.
	 * <p>
	 * <i>Sort algorithm:</i>
	 * <ol>
	 * <li>Check if there is any sort criteria in the model.</li>
	 * <li>Check given data formally.</li>
	 * <li>Analyze given data for each group:
	 * <ol type="a">
	 * <li>If no sort criteria was defined for the group, the data is not sorted and keeps the original
	 * group index.</li>
	 * <li>Otherwise the data is sorted by the values for the first sort field (if given). Sort by field
	 * type:
	 * <ul>
	 * <li>Number and amount: numeric.</li>
	 * <li>String, extensible enumeration and custom: alphabetic ignoring case.</li>
	 * <li>Enumeration: order of the xml values.</li>
	 * <li>Date: date sort. Dates with unknown parts ar sorted at the end of the list.</li>
	 * <li>Yes no: if ascending first yes- and then no-values.</li>
	 * </ul>
	 * </li>
	 * <li>Then by the values for the second sort field, if given.</li>
	 * <li>And so on for all defined sort fields.</li>
	 * <li>If for several repetitions the values for all sort fields are equal, these repetitions are
	 * sorted by their original group indices.</li>
	 * <li>Groups, for which the sort field is not filled, are positioned at the end of the list.</li>
	 * <li>After the data of the group is sorted, the group indices are updated according to the new
	 * order.</li>
	 * </ol>
	 * </li>
	 * </ol>
	 * </p>
	 */
	String MODEL_SORTED_DATA = "SortedData";
	/**
	 * Returns <code>true</code> if there are index fields defined in the model, otherwise <code>false</code>.
	 */
	String MODEL_INDEX_FIELDS = "IndexFields";
	/**
	 * Returns a list with the full names of all index fields in the model. If there are no index fields, an empty list is returned.
	 */
	String MODEL_INDEX_FIELD_FULL_NAMES = "IndexFieldFullNames";
	/**
	 * Returns the set of fields as a Set of {@link IMetaField}, which are defined as dynamic enumeration. If there are no such
	 * fields, the Set is empty.
	 * <p>
	 * HINT: For document models, dynamic enumerations are not generated, yet. Therefore for code generated from document models,
	 * this key always returns an empty <code>Set</code>.
	 */
	String MODEL_DYNAMIC_ENUM_FIELDS = "DynamicEnumFields";
	/**
	 * As an additional parameter the "key of annotation" of type {@link String} is expected.
	 * <p>
	 * Returns:
	 * <ul>
	 *     <li><code>null</code>, if there is no annotation with the given key.</li>
	 *     <li>Empty String, if the annotation key is present but no value is associated with it.</li>
	 *     <li>Otherwise, the value associated with the given key.</li>
	 * </ul>
	 */
	String MODEL_ANNOTATION = "Annotation";
	/**
	 * As an additional parameter the "key of annotation" of type {@link String} is expected.
	 * <p>
	 * Returns
	 * <ul>
	 *     <li><code>null</code>, in the absence of any annotation with the given key.</li>
	 *     <li>Otherwise, a map of entries of the form full entity name as the key, and empty String, or the annotation value as the
	 *     value. If the model itself has the annotation, the key in the resulting map will be an empty String.</li>
	 * </ul>
	 */
	String MODEL_ALL_ANNOTATED_ENTITIES = "ALL_AnnotatedEntities";

	/**
	 * short name of the group as a string
	 */
	String GROUP_SHORT_NAME = "Name";
	/**
	 * full name of the group as a string
	 */
	String GROUP_FULLNAME = "PfadName";
	/**
	 * returns a <code>List&lt;{@link IMetaGroup}&gt;</code>, which are <i>directly</i> included in this group.
	 * If there are no subgroups an empty list is returned. The ordering of the result lists corresponds to the ordering of the
	 * groups passed to the code generator.
	 */
	String GROUP_SUBGROUP_LIST = "ContainerListe";
	/**
	 * returns the <code>List&lt;{@link IMetaRule}&gt;</code>, which are <i>directly</i> included in this group.
	 * If there are no such rules, an empty list is returned. The ordering of the result lists corresponds to the ordering of the
	 * rules passed to the code generator
	 */
	String GROUP_RULE_LIST = "RegelListe";
	/**
	 * Returns a <code>List&lt;{@link IMetaField}&gt;</code>, which are <i>directly</i> included within this group. If there are no
	 * such fields an empty list is returned.
	 */
	String GROUP_FIELD_LIST = "FelderListe";
	/**
	 * Returns the maximum repetition of this group as a Long
	 */
	String GROUP_MAX_INDEX = "MaxIndex";
	/**
	 * Returns the list of index fields for the group. These fields are used for the semantic index and parallel iteration. If the
	 * group doesn't contain index fields, an empty list will be returned.
	 */
	String GROUP_INDEX_FIELDS = "IndexFields";
	/**
	 * with the parameter "key of annotation" of type {@link java.lang.String}, it returns:
	 * <ul>
	 *     <li><code>null</code>, if there is no annotation with the given key.</li>
	 *     <li>Empty String, if the annotation key is present but no value is associated with it.</li>
	 *     <li>Otherwise, the value associated with the given key.</li>
	 * </ul>
	 */
	String GROUP_ANNOTATION = "Annotation";

	/**
	 * short name of the field as a string.
	 */
	String FIELD_SHORT_NAME = "Name";
	/**
	 * full name of a field
	 */
	String FIELD_FULLNAME = "PfadName";
	/**
	 * returns whether the field is a mandatory field as a Boolean. The information is extracted from rules including rules
	 * generated for fields marked as mandatory.
	 */
	String FIELD_MANDATORY_FIELD = "Pflichtfeld";
	/**
	 * returns whether the field is globally mandatory or mandatory regarding a surrounding repeatable group as a Boolean. The
	 * information is extracted from rules including rules generated for fields marked as mandatory.
	 */
	String FIELD_MANDATORY_GLOBAL_OR_IN_SURROUNDING_REPEATABLE_GROUP = "MandatoryGloballyOrForRepeatableGroup";
	/**
	 * minimum length as an Integer, it always
	 * returns some value. If no value is specified, <code>1</code> is returned as this is the default.
	 * For enumerations, the length of the shortest value is returned. For Yes and Yes-No fields, the
	 * length of the shortest value is returned. For usage of {@linkplain PresentationInformation}
	 * as optional additional parameter see {@link IMetaKeys#FIELD_MAX_LENGTH}.
	 */
	String FIELD_MIN_LENGTH = "MinLaenge";
	/**
	 * maximum length as an Integer; it always returns some value. If no value is specified, <code>99999</code> is returned as this
	 * is the default.
	 * For enumerations the length of the longest value is returned. For Yes and Yes-No fields the
	 * length of the longest value is returned. Optionally, a {@linkplain PresentationInformation}
	 * object may be passed as additional parameter: in that case the length is considered as in the
	 * presentation.
	 * If the constant <code>LANGUAGE_DEFINED</code> is used as a key within the presentation
	 * information, a {@link Locale} must be passed to clarify the language.
	 * Otherwise an {@link IllegalArgumentException} is thrown.<br>
	 * If the field is a string and the property 'noValueValidation' is set to <code>true</code>, the
	 * max. length is given in MB.
	 */
	String FIELD_MAX_LENGTH = "MaxLaenge";
	/**
	 * field type as a string, one of:
	 * <ul>
	 * <li><code>{@link IMetaKeys#FIELD_TYPE_STRING}</code></li>
	 * <li><code>{@link IMetaKeys#FIELD_TYPE_NUMBER}</code></li>
	 * <li><code>{@link IMetaKeys#FIELD_TYPE_DATE}</code></li>
	 * <li><code>{@link IMetaKeys#FIELD_TYPE_DATE_RANGE}</code></li>
	 * <li><code>{@link IMetaKeys#FIELD_TYPE_MONEY_2}</code></li>
	 * <li><code>{@link IMetaKeys#FIELD_TYPE_MONEY_0}</code></li>
	 * <li><code>{@link IMetaKeys#FIELD_TYPE_YES}</code></li>
	 * <li><code>{@link IMetaKeys#FIELD_TYPE_YESNO}</code></li>
	 * <li><code>{@link IMetaKeys#FIELD_TYPE_ENUMERATION}</code></li>
	 * <li><code>{@link IMetaKeys#FIELD_TYPE_EXT_ENUMERATION}</code></li>
	 * </ul>
	 * or a project specific custom field type.
	 * Please note that fields of type IMetaKeys#FIELD_TYPE_EXT_ENUMERATION are string types with hint lists.
	 */
	String FIELD_FIELD_TYPE = "Feldtyp";
	/**
	 * pattern as a string, for fields with fieldtype {@link IMetaKeys#FIELD_TYPE_STRING} and a predefined a pattern for all other
	 * cases <code>null</code>.
	 */
	String FIELD_PATTERN = "Muster";
	/**
	 * error message as a string for fields with fieldtype {@link IMetaKeys#FIELD_TYPE_STRING} and a predefined pattern or
	 * {@link IMetaKeys#FIELD_TYPE_ENUMERATION}; for all other cases <code>null</code>.
	 * The error message is returned as it was defined in the model, i.e. error text parameters are not
	 * replaced. A {@link Locale} passed as a second parameter is interpreted as a language.
	 * If a {@link Locale} is passed that is not supported (see {@link IMetaModel} and key
	 * {@link IMetaKeys#MODEL_LANGUAGES}), an {@link IllegalArgumentException} is thrown. If no
	 * additional parameter is passed, {@link Locale#GERMANY} is chosen as a default. If for
	 * {@link IMetaKeys#FIELD_TYPE_ENUMERATION} no error message has been defined in the model,
	 * depending on the {@link Locale} passed, a default error message is returned.
	 */
	String FIELD_ERROR_MESSAGE = "Fehlertext";
	/**
	 * word wrap as a Boolean, only for fields of field type {@link IMetaKeys#FIELD_TYPE_STRING}; all other cases <code>null</code>.
	 */
	String FIELD_WORD_WRAP = "Zeilenumbruch";
	/**
	 * sign as a Boolean only for number, otherwise it returns <code>null</code>.
	 */
	String FIELD_SIGN = "Vorzeichen";
	/**
	 * Returns the minimum number of decimal places for numbers as an Integer, otherwise it returns <code>null</code>.
	 */
	String FIELD_DECIMAL_PLACES = "Nachkommastellen";
	/**
	 * Returns the maximum number of decimal places for numbers as an Integer, otherwise it returns <code>null</code>.
	 */
	String FIELD_MAX_DECIMAL_PLACES = "MaxNachkommastellen";
	/**
	 * number of places before decimal separator for numbers as an Integer, if specified in the model, otherwise it returns
	 * <code>null</code>. If the number of places before the decimal separator is not defined, <code>null</code> is returned even
	 * for numbers.
	 */
	String FIELD_INTEGER_DIGIT = "Vorkommastellen";
	/**
	 * zero allowed as a Boolean, only applicable for numbers, otherwise it returns <code>null</code>.
	 */
	String FIELD_ZERO_ALLOWED = "NullErlaubt";
	/**
	 * Indication if leading zeros are allowed as <code>Boolean</code>, only for numbers / monetary amounts, otherwise always
	 * <code>null</code>.
	 */
	String FIELD_LEADING_ZEROS = "FuehrendeNullen";
	/**
	 * returns the date format as a String, only applicable to dates; all other cases <code>null</code>.
	 */
	String FIELD_FORMAT = "Format";
	/**
	 * returns the value that expresses <i>true</i> in the internal representation, only for fields with field type YES or YES/NO;
	 * all other cases <code>null</code>.
	 */
	String FIELD_VALUE_TRUE = "WertWahr";
	/**
	 * returns the value that expresses <i>false</i> in the internal representation, only for fields with field type YES/NO;
	 * all other cases <code>null</code>.
	 */
	String FIELD_VALUE_FALSE = "WertFalsch";
	/**
	 * The symbol used as decimal separator in this model type as a String. Is returned, even if the field is not a number.
	 */
	String FIELD_DECIMAL_SEPARATOR = "DezimalTrenner";
	/**
	 * If the field is an Enumeration, a String with hint list or an extensible enumeration the following will be returned:
	 * <ul>
	 * <li>A {@link Locale} can be passed as an additional parameter. The returned value is a {@link List} of {@link String}s
	 * containing the enumeration values. If a {@link Locale} is passed that is not part of the List of supported languages (i.e.
	 * the List returned when calling the keyword {@link IMetaKeys#MODEL_LANGUAGES} in {@link IMetaModel}), an
	 * {@link IllegalArgumentException} is thrown. The order in which the values in the list are returned corresponds to the order
	 * which was specified in the model for the specific language.</li>
	 * <li>ExtMP fallback: If no {@link Locale} is passed, a {@link List} of {@link String}s is returned containing the enumeration
	 * values defined for {@link Locale#GERMANY}. The list is empty if this locale is not supported in the model.</li>
	 * </ul>
	 * In all other cases an empty list is returned.
	 */
	String FIELD_ENUMERATION_VALUES = "EnumerationWerte";
	/**
	 * returns whether the field is a root group mandatory field as a Boolean. The information is extracted from rules including
	 * rules generated for fields marked as mandatory.
	 */
	String FIELD_ROOT_GROUP_MANDATORY_FIELD = "VordruckPflichtfeld";
	/**
	 * If the field is of type enumeration and UI values have been defined, the following will be returned:
	 * <ul>
	 * <li>A {@link Locale} can be passed as an additional parameter. The returned value is a {@link List} of {@link Pair}s of
	 * {@link String}s containing the UI value as the first String and the internal value as the second. If a {@link Locale} is
	 * passed that is not part of the List of supported languages (i.e. the List returned when calling the keyword
	 * {@link IMetaKeys#MODEL_LANGUAGES} in {@link IMetaModel}), an {@link IllegalArgumentException} is thrown. The order in which
	 * the values in the list are returned corresponds to the order which was specified in the model for the specific language.</li>
	 * <li>ExtMP fallback: If no {@link Locale} is passed, a {@link List} of {@link String}s is returned containing the UI values
	 * defined for {@link Locale#GERMANY}. The list is empty if this locale is not supported in the model.</li>
	 * </ul>
	 * In all other cases an empty List is returned.
	 */
	String FIELD_ENUMERATION_UI_VALUES = "EnumerationOberflaechenwerte";
	/**
	 * returns the operands of the calculation
	 * function as a Set of <code>{@link IIdentifier}</code>. If the field is not a calculated field,
	 * the Set is empty. The set of operands is used to determine the required operands for the
	 * calculation function. The fields referenced in the prerequisites are also
	 * returned in addition to the fields referenced in the respective calculation functions.
	 * The rootgroups that are possibly referenced in rootgroup language constructions are ignored.
	 * In this case, the set of operands corresponds to the fields referenced in all
	 * calculation rules belonging to this field without the calculated field itself.
	 * For the respective repetition level, the following is specified in the <code>
	 * {@link IIdentifier}</code>
	 * <ul>
	 * <li>the constant {@link IIdentifier#ALL_INDICES}: if a set building operation was specified for
	 * this repetition level of this identifier in the underlying rule condition.</li>
	 * <li>the constant {@link IIdentifier#ITERATION}: if an iteration was specified for this
	 * repetition level of this identifier in the underlying rule condition. Before the calculation
	 * function can be called, this repetition level must always be replaced by a specific value. If,
	 * for example, a calculation for a sum is offered for each child and each participant, the
	 * calculation function must specify for which child and which participant the value is to be
	 * calculated.</li>
	 * <li>the specific repetition, if neither iteration nor a star operation was specified for this
	 * repetition level of the identifier.</li>
	 * </ul>
	 */
	String FIELD_OPERANDS_SET = "OperandenMenge";
	/**
	 * if the field is the operand of one
	 * (or more) calculation(s), all fields calculated by these calculations are returned as a Set of
	 * <code>{@link IIdentifier}</code>. If the field is not an operand of a calculation, the
	 * returned Set is empty.
	 * For the specification of the particular repetition level and the operand relation, see the
	 * description of the key <code>{@link IMetaKeys#FIELD_OPERANDS_SET}</code>.
	 */
	String FIELD_TARGET_SET_FOR_CALC = "ZielMengeFuerBerechnung";
	/**
	 * if the field is an
	 * enumeration and categories are defined with their respective values, this key returns a
	 * Map&lt;String, Map&lt;String, String&gt;&gt;. Each entry of the outer map has a xml value
	 * (key of the map entry) and another map of its respective category values per category name (value
	 * of the map entry). If the field is not an enumeration or it does not define any categories, an
	 * empty map will be returned.
	 */
	String FIELD_ENUMERATION_CATEGORY_VALUES = "EnumerationCategoryValues";
	/**
	 * if the field is a string and the property 'noValueValidation' is set to <code>true</code>, the key returns <code>true</code>,
	 * otherwise <code>false</code>.
	 */
	String FIELD_NO_VALUE_VALIDATION = "NoValueValidation";
	/**
	 * <code>true</code> if the field is a global field. Otherwise <code>false</code>.
	 * <p>
	 * Fields whose values determine the entire characteristic of the form can be marked as global fields. If a field has been
	 * marked as a global field, the caller has the possibility to add the global fields to the check-relevant fields specified by
	 * a subpage. Whether, or in which cases, the caller uses this option is the responsibility of the caller.
	 */
	String FIELD_GLOBAL = "GlobalesFeld";
	/**
	 * if the property 'transient' of the field is set to <code>true</code> this key returns <code>true</code>, else this key
	 * returns <code>false</code>.
	 */
	String FIELD_TRANSIENT = "TransientField";
	/**
	 * returns the s value of an enumeration field (if defined), otherwise the empty string. For string fields and extensible
	 * enumerations always the empty string is returned. For all other field types <code>null</code> is returned.
	 */
	String FIELD_S_VALUE = "SValue";
	/**
	 * returns <code>true</code> if the field was explicitly set as mandatory (not through rules, but through the mandatory check
	 * box), otherwise <code>false</code>.
	 */
	String FIELD_EXPLICITLY_MANDATORY = "ExplicitlyMandatory";
	/**
	 * If the field is a dynamic enumeration, the {@link Boolean} <code>true</code> will be returned, otherwise <code>false</code>.
	 */
	String FIELD_DYNAMIC_ENUMERATION = "DynamicEnumeration";
	/**
	 * if this field is a dynamic enumeration field, the key returns the operand fields necessary to calculate the dynamic
	 * enumeration values as a Set&lt;IIdentifier&gt;. As a parameter, an {@link IIdentifier} with concrete indices is expected.
	 * This parameter specifies the repetition of the dynamic enumeration field to be calculated. The name of the
	 * {@link IIdentifier} should coincide with the name of the current IMetaField.
	 */
	String FIELD_SOURCE_FIELDS_FOR_DYNAMIC_ENUM = "SourceFieldsForDynamicEnum";
	/**
	 * With this key, the values of dynamic enumerations are calculated. Three parameters have to be supplied in the given order:
	 * <ol>
	 * <li>An {@link IIdentifier} specifies the repetition of the dynamic enumeration field to be
	 * calculated. The name of the {@link IIdentifier} should coincide with the name of the
	 * current IMetaField.</li>
	 * <li>An {@link IData} object, which contains the data for the operands (see key
	 * <code>{@link IMetaKeys#FIELD_SOURCE_FIELDS_FOR_DYNAMIC_ENUM}</code>).</li>
	 * <li>A {@link ValidationMode} object, which specifies the language of the UI values and the
	 * interpretation of the data contained in parameter 2. The interpretation is analogous to the
	 * validation or calculation.</li>
	 * </ol>
	 * As result a list of Pair&lt;String, String&gt; will be returned. The first value of the pair will
	 * be the UI value, whereas the second value will be the XML value.
	 * The sort order is specified by following criteria:
	 * <ol>
	 * <li>If the base field is of type enumeration and contains UI values, the order is specified by
	 * the order of the values of the enumeration for the language given in the {@link ValidationMode}.
	 * The UI value will be taken from the UI values of the base enumeration.</li>
	 * <li>If the base field is an enumeration without UI values, the order is specified by the order of
	 * the XML values of the base enumeration. The UI values will be the same as the XML values in this
	 * case.</li>
	 * <li>If the base field is not an enumeration, the order is specified by the order given in the
	 * rule condition. Here, also the UI values will be the same as the XML values.</li>
	 * </ol>
	 */
	String FIELD_DYNAMIC_ENUM_VALUES = "DynamicEnumValues";
	/**
	 * returns {@code true} if the field is an index field, otherwise {@code false}.
	 */
	String FIELD_INDEX_FIELD = "IndexField";
	/**
	 * for fields of type date range it returns an {@link IMetaField} which is capable of handling a single date, otherwise it
	 * returns <code>null</code>.
	 */
	String FIELD_META_FIELD_DATE_OF_DATE_RANGE = "MetaFieldDateOfDateRange";
	/**
	 * with the parameter "key of annotation" of type {@link java.lang.String}, it returns:
	 * <ul>
	 *     <li><code>null</code>, if there is no annotation with the given key.</li>
	 *     <li>Empty String, if the annotation key is present but no value is associated with it.</li>
	 *     <li>Otherwise, the value associated with the given key.</li>
	 * </ul>
	 */
	String FIELD_ANNOTATION = "Annotation";
	/**
	 * error message as a string for fields marked explicitly as required, otherwise <code>null</code>. The error message is
	 * returned as it was defined in the model, i.e. error text parameters are not replaced.
	 * <p>
	 * A {@link Locale} passed as a second parameter is interpreted as a language.
	 * If a {@link Locale} is passed that is not supported (see {@link IMetaModel} and key {@link IMetaKeys#MODEL_LANGUAGES}),
	 * an {@link IllegalArgumentException} is thrown.
	 * If no additional parameter is passed, {@link Locale#GERMANY} is chosen as a default.
	 * If no error message has been defined in the model, depending on the {@link Locale} passed, a default error message is
	 * returned.
	 */
	String FIELD_CUSTOM_ERROR_MESSAGE_FOR_REQUIRED = "CustomErrorMessageForRequired";
	/**
	 * Specification of the print text as a string. If no print text is specified, an empty string is returned.
	 * <p>
	 * If a locale is passed as the second parameter, the parameter is interpreted as language.
	 * If the locale is not included in the list of supported languages (returned for {@link IMetaKeys#MODEL_LANGUAGES}), an
	 * <code>IllegalArgumentException</code> is thrown.
	 * If no second parameter is passed, the german print text is returned.
	 */
	String FIELD_LABEL = "DruckText";

	/**
	 * short name of the rule as a string
	 */
	String RULE_NAME = "Name";
	/**
	 * full name of the rule as a string
	 */
	String RULE_FULL_NAME = "PfadName";
	/**
	 * error code of the rule as a string
	 */
	String RULE_ERROR_CODE = "Fehlercode";
	/**
	 * error type as a string, i.e.
	 * <ul>
	 * <li><code>{@link IMetaKeys#RULE_TYPE_ERROR}</code>,</li>
	 * <li><code>{@link IMetaKeys#RULE_TYPE_WARNING}</code>, or</li>
	 * <li><code>{@link IMetaKeys#RULE_TYPE_INFO}</code></li>
	 * </ul>
	 */
	String RULE_TYPE = "Regelart";
	/**
	 * Returns a set of root group names referenced by the rule that can cause omission errors.
	 * The set is empty if there are no such root groups.
	 */
	String RULE_REF_ROOTGROUP_OMISSION_ERROR = "RefVordruckeAuslassungsFehler";
	/**
	 * the error field of the rule as an {@link IMetaField}
	 */
	String RULE_ERROR_FIELD = "Fehlerfeld";
	/**
	 * with the parameter "key of annotation" of type {@link java.lang.String}, it returns:
	 * <ul>
	 *     <li><code>null</code>, if there is no annotation with the given key.</li>
	 *     <li>Empty String, if the annotation key is present but no value is associated with it.</li>
	 *     <li>Otherwise, the value associated with the given key.</li>
	 * </ul>
	 */
	String RULE_ANNOTATION = "Annotation";

	//Constants for the rule types
	String RULE_TYPE_ERROR = "Fehler";
	String RULE_TYPE_WARNING = "Hinweis";
	String RULE_TYPE_INFO = "Info";

	//Constants for the field types
	String FIELD_TYPE_STRING = "String";
	String FIELD_TYPE_NUMBER = "Zahl";
	String FIELD_TYPE_DATE = "Datum";
	String FIELD_TYPE_DATE_RANGE = "DatumBereich";
	/**
	 * This represents a monetary amount with cents, i.e. with 2 decimal places
	 */
	String FIELD_TYPE_MONEY_2 = "GeldBetragMitCent";
	/**
	 * This represents a monetary amount without cents, i.e. with 0 decimal places.
	 */
	String FIELD_TYPE_MONEY_0 = "GeldBetragOhneCent";
	String FIELD_TYPE_YES = "Ja";
	String FIELD_TYPE_YESNO = "JaNein";
	String FIELD_TYPE_ENUMERATION = "Enumeration";
	/**
	 * This represents an enumeration which can be extended by the user of the form. E.g. if the
	 * enumeration contains the values 'Audi', 'BMW' and 'Mercedes', the user has the possibility to
	 * type in the brand of his own car if it is different from the ones in the enumeration, e.g.
	 * 'Tesla'.
	 */
	String FIELD_TYPE_EXT_ENUMERATION = "NichtAbgeschlosseneEnumeration";

}
