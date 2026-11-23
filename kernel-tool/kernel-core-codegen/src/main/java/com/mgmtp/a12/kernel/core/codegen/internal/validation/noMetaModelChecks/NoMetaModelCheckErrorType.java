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

package com.mgmtp.a12.kernel.core.codegen.internal.validation.noMetaModelChecks;

public enum NoMetaModelCheckErrorType {
	MVK_INVALID_NAME_4_GENERATED_CODE, // Der eingestellte Name '%s' für den generierten Code ist ungültig.

	MVK_INVALID_PACKAGE_NAME_4_GENERATED_CODE, // Der eingestellte Packagename '%s' für den generierten Code ist ungültig.

	MVK_FIELD_NO_PARENT, // Das Feld '%s' hat keine Eltern-Entität.

	MVK_PATH_INVALID, // Der Pfad '%s' ist ungültig.

	MVK_PATHS_INCONSISTENT, // Der Pfad '%s' passt nicht mit dem Pfad der Eltern-Entität '%s' zusammen.

	MVK_FIELD_WITH_CONTAINED_ENTITIES, // '%s' ist ein Feld und kann daher keine Entitäten enthalten.

	MVK_PARENT_ENTITY_MISSING, // Für '%s' ist die Eltern-Entität '%s' nicht im Entity-Container enthalten.

	MVK_MAX_VALUES_INVALID, // Die maximalen Indices für '%s' sind ungültig.

	MVK_MAX_VALUES_INCONSISTENT, // Die maximalen Indices für '%s' und '%s' passen nicht zusammen.

	MVK_NO_FORMAL_ERROR_RESOURCE_BUNDLE, // Es existiert kein ResourceBundle, das die formalen Fehlermeldungen für die Sprache '%s' spezifiziert.

	MVK_NO_PROJECT_LANGUAGE_SETTINGS, // Die projektspezifischen Darstellungsinformationen für die Sprache '%s' wurden nicht gesetzt.

	MVK_INVALID_CHAR_IN_RESTRICTED_CHARSET, // Das Zeichen '%s' des definierten Zeichensatzes darf für die Zielsprache '%s' nicht verwendet werden.

	MVK_INVALID_INDEX_FIELD, // Das als Indexfeld definierte Feld '%s' ist im Vordruck bzw. Kontext '%s' nicht vorhanden.

	MVK_INDEX_FIELD_NO_VALUE_VALIDATION, // Die Eigenschaft 'Keine Werteprüfung' für das Indexfeld '%s' darf nicht gesetzt werden.

	MVK_INVALID_SORT_FIELD, // Das als Sortierfeld definierte Feld '%s' ist im Vordruck bzw. Kontext '%s' nicht vorhanden.

	MVK_SORT_FIELD_NO_VALUE_VALIDATION, // Die Eigenschaft 'Keine Werteprüfung' für das Sortierfeld '%s' darf nicht gesetzt werden.

	MVK_ENUM_VALUE_WITH_INVALID_CHARACTER, // Für das Feld '%s' wurde der Wert '%s' definiert. Dieser enthält das Zeichen '%s', das für das Modell nicht erlaubt ist.

	MVK_DUPLICATE_DYNAMIC_ENUM, // Es gibt mehr als eine Berechnung für Dynamische Enumerationen für das Feld '%s' (z.B. '%s', '%s'). Dies ist nicht erlaubt.

	MVK_ENUM_TABLE_MISSING, // Tabelle '%s' existiert nicht. Sie wird referenziert von den Feldern %s.

	MVK_TABLE_ENUM_DEP_COLUMN_MISSING, // Die Spalte '%s', von der die Enumeration abhängt existiert in der Tabelle '%s' nicht.

	MVK_TABLE_ENUM_COLUMN_MISSING, // Die Enumerationsspalte '%s' existiert in der Tabelle '%s' nicht.

	MVK_TABLE_ENUM_DIFF_CATEGORIES, // Die in der Enumeration definierten Kategorien (%s) unterscheiden sich von den in der Tabelle '%s' definierten (%s).

	MVK_TABLE_ENUM_WITH_MULTILINGUAL_DISPLAY_VALUE, // Für die Enumerationsspalte '%s' wurden in der Tabelle '%s' Oberflächenwerte definiert. Dies ist für mehrsprachige Generate nicht erlaubt.

	MVK_INDEX_FIELD_NO_TABLE_ENUM, // Das Indexfeld '%s' darf keine Enumeration mit Tabellenbezug sein.

	MVK_TABLE_ENUMS_NOT_ALLOWED_FOR_PROGRAMMING_LANGUAGE, // Die Verwendung von Enumerationen mit Tabellenbezug ist für die Programmiersprache '%s' nicht erlaubt.

	MVK_INVALID_TIME_ZONE, // Die angegebene Zeitzone '%s' nicht valide.
}
