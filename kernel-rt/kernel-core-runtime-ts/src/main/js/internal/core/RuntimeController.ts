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

import { HashSet as HashTable } from "@com.mgmtp.a12.utils/utils-collections";

import {
	DateAndFormat,
	DateInterval,
	DateUtil,
	PartiallyKnownDatesUtil,
	TimeZonedDate,
	ValidationDateParser
} from "@com.mgmtp.a12.utils/utils-localization/lib/main/conversion/a12internal/index.js";

import type { IIterator, IResult, ValidationMode } from "@com.mgmtp.a12.kernel/kernel-core-runtime-api-ts/a12internal";
import {
	ArrayMap,
	IIdentifier,
	numbersconv
} from "@com.mgmtp.a12.kernel/kernel-core-runtime-api-ts/a12internal";

import { CustomFieldTypeValidationParamImpl } from "../custom/CustomFieldTypeValidationParamImpl.js";
import type { FormatDefinitionDatum } from "../formatdef/FormatDefinitionDatum.js";
import type { FormatDefinitionDatumBereich } from "../formatdef/FormatDefinitionDatumBereich.js";
import { FormatDefinitionVordefiniert } from "../formatdef/FormatDefinitionVordefiniert.js";
import type { IFormatDefinition } from "../formatdef/IFormatDefinition.js";
import type { ValidationLogger } from "../logging/ValidationLogger.js";
import { DarstellungsInformationIntern } from "../util/DarstellungsInformationIntern.js";
import { DatumBereich } from "../util/DatumBereich.js";
import { DreiWertBool } from "../util/DreiWertBool.js";
import { EbenenIterator } from "../util/EbenenIterator.js";
import { EntityIterator } from "../util/EntityIterator.js";
import { EntityUtil } from "../util/EntityUtil.js";
import { IterationState } from "../util/IterationState.js";
import { KontextIterator } from "../util/KontextIterator.js";
import { MetaDataUtilValidierung } from "../util/MetaDataUtilValidierung.js";
import { RtIdentifierTemplate } from "../util/RtIdentifierTemplate.js";
import { RtInternalIdentifier } from "../util/RtInternalIdentifier.js";
import { RuntimeUtils } from "../util/RuntimeUtils.js";
import { ValidierungArt } from "../util/ValidierungArt.js";
import { ValidType } from "../util/ValidType.js";
import { VkBigDecimal } from "../util/VkBigDecimal.js";
import { VkDate } from "../util/VkDate.js";
import { VkDateRange } from "../util/VkDateRange.js";
import { VkString } from "../util/VkString.js";

import { IteratorWrapper } from "../util/IteratorWrapper.js";
import { ParallelIteratorWrapper } from "../util/ParallelIteratorWrapper.js";

import { BedingungsOperatorHelper, VglOp } from "./BedingungsOperatorHelper.js";
import { BigDecimalUtils } from "./BigDecimalUtils.js";
import type { Combiner } from "./Combiner.js";
import {
	MaxValueCombiner as DateMaxValueCombiner,
	MinValueCombiner as DateMinValueCombiner
} from "./DateCombiner.js";
import { FirstValueCombinerDate, FirstValueCombinerDateRange, FirstValueCombinerNumber, FirstValueCombinerString } from "./FirstValueCombiner.js";
import type { IMetaDataValidierung } from "./IMetaDataValidierung.js";
import {
	MaxWertCombiner,
	MinWertCombiner,
	SummeCombiner
} from "./NumberCombiner.js";
import { NumberOfDifferentValuesCombiner } from "./NumberOfDifferentValuesCombiner.js";
import type { RepetitionNotUniqueErrorCache } from "./RepetitionNotUniqueErrorCache.js";
import type { ValidationCache } from "./ValidationCache.js";
import { ValidierungsErgebnis } from "./ValidierungsErgebnis.js";

/**
 * Die Klasse {@link RuntimeController} ist die zentrale Klasse zum
 * Laufzeit-Zugriff auf die Nutzdaten. Sie stellt die Basisoperationen der
 * Regelsprache zur Verfügung.
 */
export abstract class RuntimeController {
	protected readonly metaData: IMetaDataValidierung;
	/**
	 * von der Applikation übergebene Schnittstelle zum Einfügen
	 * eventuell gefundener Fehler oder Hinweise.
	 */
	protected readonly pruefErgebnis: IResult;
	/**
	 * von der Applikation übergebener Logger
	 */
	protected readonly logger: ValidationLogger;
	/**
	 * von der Applikation übergebener Validierungsmodus.
	 * Hier werden Validierungsparameter übergeben.
	 */
	protected readonly modus: ValidationMode;
	/**
	 * Zur Laufzeitoptimierung werden die übergebenen Werte und
	 * verdichtete Werte zur Zugriffsoptimierung gecached.
	 */
	protected readonly validationCache: ValidationCache;

	/**
	 * Falls Teilvalidierung nach 3-Wert-Logik, wird zusätzlich gespeichert, ob immer alle
	 * Kontexte eines Feldes prüfungsrelevant, wenn das Feld prüfungsrelevant.
	 * Wird zur Laufzeitoptimierung bei Teilvalidierungen verwendet.
	 */
	protected validierungArt: ValidierungArt = new ValidierungArt();

	/**
	 * Definiert den aktuellen Validierungszeitpunkt.
	 */
	protected readonly validationTime: Date;

	protected readonly timeZone: string;
	/**
	 * Caches the parsed date per (format, isTeilbekannt, field value) for the lifetime of this
	 * controller, i.e. for a single validation or computation run. Parsing is deterministic given
	 * {@link timeZone} and the base year, and both are fixed for a controller instance.
	 *
	 * Without this cache the same field value is parsed again for every rule evaluation that reads
	 * it. When rules iterate over large repeatabilities that adds up to millions of parses of a few
	 * thousand distinct values, and date parsing then dominates the whole run.
     * e.g. for A12-19111, caching here achieves a 12x-16x speedup for the overall validateAll call.
	 *
	 * The epoch milliseconds are stored rather than the {@link Date}, so that every caller still
	 * receives its own {@link Date} instance (because Date is not immutable).
     * A {@code null} entry means the value has an unknown year and yields {@link VkDate.NON_RELEVANT_DATE}.
	 */
	private readonly _dateValueCache: Map<string, number | null> = new Map<string, number | null>();

	private static _bereichAlsZahlRegExp = /^(?:\d+)$/;

	/**
	 * Erzeugt ein Validator-Objekt mit den übergebenen Nutzdaten.
	 *
	 * @param ergebnisse Ergebnis-Objekt zu dem die gefundenen Fehler und Hinweise hinzugefügt werden.
	 * @param valCache Cache für die Nutzdaten und die Optimierungsstrukturen.
	 * @param logger Logger Objekt
	 * @param modus Parameter für die Validierung
	 * @param metaData bietet Zugriff auf die Metadaten
	 */
	public constructor(
		pruefErgebnis: IResult,
		validationCache: ValidationCache,
		logger: ValidationLogger,
		modus: ValidationMode,
		metaData: IMetaDataValidierung
	) {
		this.pruefErgebnis = pruefErgebnis;
		this.validationCache = validationCache;
		this.logger = logger;
		this.modus = modus;
		this.metaData = metaData;
		const heuteFuerTest: Date | undefined = this.modus.getCurrentDateForTest();
		this.validationTime = heuteFuerTest == undefined ? new Date() : heuteFuerTest;
		this.timeZone = metaData.getTimeZone();
	}

	/**
	 * Prüft, ob das Feld prüfungsrelevant ist.
	 *
	 * Ein Feld ist <b>nicht</b> prüfungsrelevant, wenn
	 * - Fehler bei der formalen Prüfung
	 * - Teilvalidierung: Feld ist nicht in der Menge der relevanten Felder
	 *
	 * TestRoutine in MainValidatorTest.testIsPruefungsRelevant
	 *
	 * @param f 	Identifier, der geprüft wird
	 * @return true falls prüfungsrelevant, false sonst.
	 */
	private _isPruefungsRelevant(f: RtInternalIdentifier, partialIsUnknown: boolean): boolean {
		if (!f) {
			return false;
		}
		return EntityUtil.isPruefungsRelevant(
			f,
			this.validationCache,
			partialIsUnknown
		);
	}

	/**
	 * Following values will be returned:
	 * <ul>
	 * <li>identifier not relevant -> UNKNOWN</li>
	 * <li>identifier relevant, but not contained in the data -> FALSE</li>
	 * <li>identifier relevant and contained in the data -> TRUE</li>
	 * </ul>
	 * For group identifier, the relevance depends on the parameter partialIsUnknown.
	 * If the parameter is set, the group is only determined as relevant, if the group is fully
	 * relevant, i.e. all fields of the given repetition are relevant.
	 * If the parameter is not set, the group is already determined as relevant, if the group is
	 * only partially relevant.
	 *
	 * @param id 	Identifier, to be checked for given data.
	 * @param partialIsUnknown for a group identifier, which is only partially relevant/unknown for the given
	 *            repetition, this parameter specifies, whether this fact leads to the value
	 *            {@link DreiWertBool.UNKNOWN}.
	 *
	 * @return TRUE/FALSE/UNKNOWN je nachdem ob Wert angegeben/prüfungsrelevant
	 */
	public isWertAngegeben(id: RtInternalIdentifier, partialIsUnknown: boolean): DreiWertBool {
		if (!this._isPruefungsRelevant(id, partialIsUnknown)) {
			// Wenn das Feld nicht prüfungsrelevant ist,  wird UNKNOWN zurückgegeben.
			return DreiWertBool.UNKNOWN;
		}

		// Feldwert ist nun entweder angegeben oder nicht angegeben.
		return this.validationCache.isEntityFilled(id) ? DreiWertBool.TRUE : DreiWertBool.FALSE;
	}

	public mindestensEinVordruckAngegeben(
		vordruckName: string,
		l: number,
	): ValidierungsErgebnis; // method 1
	public mindestensEinVordruckAngegeben(
		vordruckName: string,
		l: number,
		indices: IterationState | undefined,
		semIdxEntity: RtIdentifierTemplate
	): ValidierungsErgebnis; // method 2
	/**
	 * Überprüft ob ein Formular mit Nummer l vorhanden ist.
	 * Es wird TRUE_WF oder FALSE zurückgegeben
	 *
	 * @param vordruckName 	Name des Vordrucks
	 * @param l 	Nummer des Vordrucks oder 0 für irgendeine Vordrucknummer
	 */
	public mindestensEinVordruckAngegeben(
		vordruckName: string,
		l: number,
		indices?: IterationState,
		semIdxEntity?: RtIdentifierTemplate
	): ValidierungsErgebnis {
		const groupPath = EntityUtil.getRootGroupPath(vordruckName);
		//kein (prüfungsrelevanter) Eintrag vorhanden -> kein Fehler
		if (this.validationCache.numberOfFilledGroupInstances(groupPath) === 0) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}

		if (l === IIdentifier.ALL_INDICES) {
			// Es muss nur irgendein Eintrag vorhanden sein -> Fehler
			return ValidierungsErgebnis.TRUE_WF;
		}

		const specificLfdNr = this._deriveSpecificLfdNr(vordruckName, l, indices, semIdxEntity);

		const id = new RtInternalIdentifier(groupPath, [specificLfdNr]);
		return this.validationCache.isGroupFilled(id)
			? ValidierungsErgebnis.TRUE_WF
			: ValidierungsErgebnis.FALSE_OR_UNKNOWN;
	}

	private _deriveSpecificLfdNr(
		vordruckName: string,
		lfdNr: number,
		indices: IterationState | undefined,
		semIdxEntity: RtIdentifierTemplate | undefined
	): number {
		if (lfdNr !== IIdentifier.PARALLEL_ITERATION && lfdNr !== IIdentifier.SEMANTIC_INDEX) {
			// If we don't have parallel iteration or a semantic index for the root group repetition, we return the lfdNr
			// Iteration (-1) is already handled in the given parameter lfdNr.
			return lfdNr;
		}
		let indexValue: string;
		if (lfdNr === IIdentifier.PARALLEL_ITERATION) {
			if (!indices) {
				return lfdNr;
			}
			// cannot be undefined, because of the check above
			// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
			indexValue = indices.getValue()!;
		} else {
			if (!semIdxEntity) {
				return lfdNr;
			}
			const semIdxSpec = semIdxEntity.getSemanticIndex().createSemanticIndex(indices);
			if (!semIdxSpec) {
				return lfdNr;
			}
			indexValue = this.validationCache.getSemanticIndexValue(semIdxSpec);
		}
		return this.validationCache.getSpecificIndexForSemanticIndex(vordruckName, indexValue);
	}

	public keinVordruckAngegeben(
		formularName: string,
		l: number,
		entity: RtIdentifierTemplate
	): ValidierungsErgebnis; // method 1
	public keinVordruckAngegeben(
		formularName: string,
		l: number,
		indices: IterationState,
		entity: RtIdentifierTemplate
	): ValidierungsErgebnis; // method 2
	/**
	 * Überprüft, dass kein Formular mit laufender Nummer l vorhanden ist. <br>
	 * Es wird TRUE_AF oder FALSE zurückgegeben. <br>
	 * Soll geprüft werden ob überhaupt kein Formular mit dem angegebenen Namen vorhanden ist,
	 * so wird für l eine 0 eingegeben.
	 * Wird die Methode im Rahmen einer Teilvalidierung aufgerufen, so wird immer FALSE zurückgegeben.
	 * Daher kann die Methode auch nicht als Negation der Methode mindestensEinVordruckAngegeben
	 * implementiert werden.
	 */
	public keinVordruckAngegeben(
		formularName: string,
		l: number,
		indicesOrEntity: IterationState | RtIdentifierTemplate,
		entity?: RtIdentifierTemplate
	): ValidierungsErgebnis {
		return this._keinVordruckAngegeben(
			formularName,
			l,
			indicesOrEntity instanceof RtIdentifierTemplate ? undefined : indicesOrEntity,
			// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
			indicesOrEntity instanceof RtIdentifierTemplate ? indicesOrEntity : entity!
		);
	}

	private _keinVordruckAngegeben(
		formularName: string,
		l: number,
		iterationState: IterationState | undefined,
		entity: RtIdentifierTemplate
	): ValidierungsErgebnis {
		if (this.validierungArt.isValidationPartWith3ValueLogic()) {
			// Bei einer Teilvalidierung nach 3-Wert-Logik wird immer false zurückgegeben
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		if (
			this.mindestensEinVordruckAngegeben(
				formularName,
				l,
				iterationState,
				entity
			).isKnownAndTrue()
			|| !this._isPruefungsRelevant(
				new RtInternalIdentifier(
					"/" + formularName,
					[this._deriveSpecificLfdNr(formularName, l, iterationState, entity)]),
				true
			)
		) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}

		return ValidierungsErgebnis.TRUE_AF;
	}

	/**
	 * Überprüft, ob alle Felder der FeldListe angegeben sind.
	 * Es wird nur TRUE_WF oder FALSE zurückgegeben.
	 * Es wird über die feldListe iteriert: Sobald ein Feld unknown oder
	 * nicht angegeben ist, wird FALSE zurückgegeben.
	 * Es wird nicht optimiert, da
	 * davon ausgegangen wird, dass diese Methode nicht für Sternoperationen
	 * mit großen Wiederholbarkeiten verwendet wird.
	 */
	public alleFelderAngegeben(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		return this.alleEntitiesAngegeben(index, entityList);
	}

	private alleEntitiesAngegeben(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		let containsFilter = false;
		for (const rtIdContainer of entityList) {
			if (rtIdContainer.isAggregation()) {
				if (rtIdContainer.getFilter()) {
					containsFilter = true;
				}
				for (const id of new EntityIterator(rtIdContainer, index)) {
					if (this.isWertAngegeben(id, false) !== DreiWertBool.TRUE) {
						return ValidierungsErgebnis.FALSE_OR_UNKNOWN; // einen nicht angegebenen Wert gefunden -> gib false zurück
					}
				}
			} else {
				const rtId = RtInternalIdentifier.createFromTemplate(rtIdContainer, index);
				if (this.isWertAngegeben(rtId, false) !== DreiWertBool.TRUE) {
					return ValidierungsErgebnis.FALSE_OR_UNKNOWN; // einen nicht angegebenen Wert gefunden -> gib false zurück
				}
			}
		}
		if (containsFilter) {
			return ValidierungsErgebnis.TRUE_AF;
		} else {
			return ValidierungsErgebnis.TRUE_WF;
		}
	}

	/**
	 * Überprüft, ob kein Feld aus der FeldListe angegeben ist.
	 * Es wird nur TRUE_AF oder FALSE zurückgegeben.
	 * Es wird über die ganze feldListe iteriert: Sobald ein Feld unknown oder
	 * angegeben ist, wird FALSE zurückgegeben.
	 */
	public keinFeldAngegeben(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		return this.keineEntityAngegeben(index, entityList);
	}

	private keineEntityAngegeben(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		for (const rtIdContainer of entityList) {
			if (rtIdContainer.isAggregation()) {
				const fIt: EntityIterator = new EntityIterator(rtIdContainer, index);
				if (this._internMindestensEinFeldAngegebenOderNichtPruefungsrelevant(fIt)) {
					return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
				}
			} else {
				const rtId = RtInternalIdentifier.createFromTemplate(rtIdContainer, index);
				if (this.isWertAngegeben(rtId, true) !== DreiWertBool.FALSE) {
					return ValidierungsErgebnis.FALSE_OR_UNKNOWN; // field filled or not relevant
				}
			}
		}
		return ValidierungsErgebnis.TRUE_AF;
	}

	private _internMindestensEinFeldAngegebenOderNichtPruefungsrelevant(
		fIt: EntityIterator
	): boolean {
		this._setzeEndIndicesAufMaxGesetzt(fIt, true);
		for (const id of fIt) {
			// If one field is not relevant, we want to return true
			if (this.isWertAngegeben(id, true) !== DreiWertBool.FALSE) {
				return true; // Feld angegeben oder nicht prüfungsrelevant
			}
		}
		return false;
	}

	/**
	 * Überprüft, ob mindestens ein Feld angegeben ist.
	 * Es wird über die ganze feldListe iteriert: Sobald ein prüfungsrelevantes Feld angegeben
	 * ist, wird TRUE_WF zurückgegeben, wenn das Feld keine Filterbedingung hat, TRUE_AF sonst.
	 */
	public mindestensEinFeldAngegeben(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		return this.mindestensEineEntityAngegeben(index, entityList);
	}

	private mindestensEineEntityAngegeben(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		// sobald ein Feld angegeben ist, gebe true zurück
		for (const rtIdContainer of entityList) {
			if (rtIdContainer.isAggregation()) {
				const fIt: EntityIterator = new EntityIterator(rtIdContainer, index);
				if (this._internMindestensEinFeldAngegeben(fIt)) {
					return rtIdContainer.getFilter()
						? ValidierungsErgebnis.TRUE_AF
						: ValidierungsErgebnis.TRUE_WF;
				}
			} else {
				const rtId = RuntimeController.makeIdentifier(index, rtIdContainer);
				if (this.isWertAngegeben(rtId, false) === DreiWertBool.TRUE) {
					return ValidierungsErgebnis.TRUE_WF;
				}
			}
		}
		return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
	}

	private _internMindestensEinFeldAngegeben(fIt: EntityIterator): boolean {
		// Optimierung auch wenn Teilvalidierung für konkrete lfdNummer möglich
		// da nicht prüfungsrelevante Felder genauso behandelt werden wie nicht angegebene
		// Felder
		this._setzeEndIndicesAufMaxGesetzt(fIt, false);
		fIt.optimiereMitGesetztenWiederholungenWennSinnvoll(this.validationCache);

		for (const id of fIt) {
			// one field relevant suffices for TRUE
			if (this.isWertAngegeben(id, false) === DreiWertBool.TRUE) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Sobald ein prüfungsrelevantes Feld nicht angegeben ist, wird TRUE_AF zurückgegeben.
	 * Es wird nicht optimiert, da
	 * davon ausgegangen wird, dass diese Methode nicht für Sternoperationen
	 * mit großen Wiederholbarkeiten verwendet wird.
	 */
	public nichtAlleFelderOderKeinFeldAngegeben(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		return this.nichtAlleOderKeineEntityAngegeben(index, entityList);
	}

	private nichtAlleOderKeineEntityAngegeben(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		for (const rtIdContainer of entityList) {
			if (rtIdContainer.isAggregation()) {
				for (const id of new EntityIterator(rtIdContainer, index)) {
					// If only one field is not relevant, FALSE should never be returned
					if (this.isWertAngegeben(id, true) === DreiWertBool.FALSE) {
						return ValidierungsErgebnis.TRUE_AF;
					}
				}
			} else {
				const rtId = RtInternalIdentifier.createFromTemplate(rtIdContainer, index);
				if (this.isWertAngegeben(rtId, true) === DreiWertBool.FALSE) {
					return ValidierungsErgebnis.TRUE_AF;
				}
			}
		}
		return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
	}

	/**
	 * Checks whether all groups in the list are filled.
	 * If all groups are definitely filled {@link ValidierungsErgebnis.TRUE_WF} is returned.
	 * If one group is not filled, or one group is irrelevant, {@link ValidierungsErgebnis.FALSE_OR_UNKNOWN} is
	 * returned.
	 */
	public alleKontexteAngegeben(
		index: IterationState,
		groupList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		return this.alleEntitiesAngegeben(index, groupList);
	}

	/**
	 * Checks whether no group in the list is filled.
	 * If all groups are definitely not filled {@link ValidierungsErgebnis.TRUE_AF} is returned.
	 * If one group is not filled or not fully relevant, {@link ValidierungsErgebnis.FALSE_OR_UNKNOWN} is
	 * returned.
	 */
	public keinKontextAngegeben(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		return this.keineEntityAngegeben(index, entityList);
	}

	/**
	 * Checks if at least one group in the list is filled.
	 * If at least one group is definitely filled, {@link ValidierungsErgebnis.TRUE_WF} is returned.
	 * If no group is relevant or no relevant group is filled, {@link ValidierungsErgebnis.FALSE_OR_UNKNOWN} is
	 * returned.
	 */
	public mindestensEinKontextAngegeben(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		return this.mindestensEineEntityAngegeben(index, entityList);
	}

	/**
	 * If there is one group that is fully relevant and is not filled
	 * {@link ValidierungsErgebnis.TRUE_AF} is returned.
	 * If all groups are either not fully relevant or filled, {@link ValidierungsErgebnis.FALSE_OR_UNKNOWN} is
	 * returned.
	 * Groups, which are not fully relevant, are ignored.
	 */
	public nichtAlleOderKeinKontextAngegeben(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		return this.nichtAlleOderKeineEntityAngegeben(index, entityList);
	}

	/**
	 * Sobald zwei prüfungsrelevante Felder angegeben sind, wird TRUE_AF zurückgegeben, wenn ein
	 * Feld mit Filterbedingung gefunden wurde, TRUE_WF sonst.
	 */
	public mehrAlsEinFeldAngegeben(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		// sobald mehr als ein Feld gefunden wurde, gebe TRUE_WF zurück
		let anzGefFelder = 0;
		let containsFilter = false;

		for (const rtIdContainer of entityList) {
			if (rtIdContainer.isAggregation()) {
				const fIt: EntityIterator = new EntityIterator(rtIdContainer, index);
				// es reicht, über die tatsächlich vorhandenen Felder zu iterieren, da
				// prüfungsrelevante UND angegebene Werte gezählt werden müssen
				this._setzeEndIndicesAufMaxGesetzt(fIt, false);

				for (const id of fIt) {
					if (this.isWertAngegeben(id, false) === DreiWertBool.TRUE) {
						if (rtIdContainer.getFilter()) {
							containsFilter = true;
						}
						anzGefFelder++;
						if (anzGefFelder > 1) {
							return containsFilter ? ValidierungsErgebnis.TRUE_AF : ValidierungsErgebnis.TRUE_WF;
						}
					}
				}
			} else {
				const rtId = RtInternalIdentifier.createFromTemplate(rtIdContainer, index);
				if (this.isWertAngegeben(rtId, false) === DreiWertBool.TRUE) {
					anzGefFelder++;
					if (anzGefFelder > 1) {
						return containsFilter ? ValidierungsErgebnis.TRUE_AF : ValidierungsErgebnis.TRUE_WF;
					}
				}
			}
		}
		return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
	}

	/**
	 * Implementiert das Sprachkonstrukt WiederholungNichtEindeutig.
	 * <p>
	 * Prüft ob die Wertkombination dieser Wiederholung gleich ist der Wertkombination einer anderen
	 * Wiederholung.<br>
	 * Im Fehlerfall wird TRUE_AF zurückgegeben, wenn für ein Feld in der Feldliste kein Wert
	 * angegeben wurde, sonst TRUE_WF.
	 * </p>
	 * Durch den Parser wird zugesichert:
	 * <ul>
	 * <li>keines der Felder in der Feldliste spezifiziert einen Stern-Operator.</li>
	 * <li>alle Felder liegen auf einem Pfad</li>
	 * <li>Felder dürfen entweder Zahlen (oder Geldbeträge) sein oder Strings. Andere Feldtypen sind
	 * nicht in der Feldliste erlaubt.</li>
	 * </ul>
	 * <p>
	 * ACHTUNG: diese Implementierung verläßt sich darauf, daß die next()-Methode des
	 * EbenenIterators von "rechts nach links" weiterschaltet.
	 * </p>
	 *
	 * @param index
	 *            die aktuelle Wiederholung, für die die Regel
	 *            ausgewertet wird
	 * @param entityList
	 *            Feldliste
	 * @param searchedDuplicateLayers
	 *            die Ebenen, auf denen Duplikate gesucht werden. Entspricht
	 *            der Menge der
	 *            wiederholbaren Ebenen unterhalb der Ebene des
	 *            Regelkontextes+1 bzw. der Ebene des
	 *            Regelkontextes, falls Duplikate für Vordrucke gesucht
	 *            werden.
	 * @param repNotUnqCache
	 *            cache to collect the erroneous repetitions
	 */
	public wiederholungNichtEindeutig(
		index: IterationState,
		entityList: RtIdentifierTemplate[],
		searchedDuplicateLayers: number[],
		repNotUnqCache: RepetitionNotUniqueErrorCache,
		semanticIndexLevel: number
	): ValidierungsErgebnis {
		const indexes: number[] = index.getIndexes();
		/*
		 * Es kann sein, daß das Sprachkonstrukt für die erste Wiederholung nicht aufgerufen wurde,
		 * z.B. weil eine andere Teilbedingung für diese falsch war. Daher wird die Variable
		 * 'initialisiert' verwendet.
		 */
		if (!repNotUnqCache.isInitialised()) {
			const iter: EbenenIterator = this._createEbenenIteratorAbPosition(
				indexes,
				searchedDuplicateLayers,
				semanticIndexLevel
			);
			this._ermittleWiederholungenMitDuplikaten(entityList, iter, repNotUnqCache);
			repNotUnqCache.setInitialised();
		}

		/*
		 * die Wiederholungen mit nicht eindeutigen Wertekonstellationen sind aufgesammelt; nun
		 * können wir das Ergebnis für die aktuelle Wiederholung zurückgeben
		 */
		if (repNotUnqCache.hasValueError(indexes)) {
			return ValidierungsErgebnis.TRUE_WF;
		}
		if (repNotUnqCache.hasOmissionError(indexes)) {
			return ValidierungsErgebnis.TRUE_AF;
		}
		return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
	}

	/**
	 * Erzeugt einen EbenenIterator, der eine Iteration über alle Wiederholungen ermöglicht, die für
	 * die Ebenen bis zur übergebenen Position der Wiederholung aus dem übergebenen Index-Array
	 * entsprechen, ab da für alle Wiederholungen, für die Werte angegeben sind.<br>
	 * ACHTUNG: Die Methode kann aktuell nicht in einer Filterbedingung mit Regeliterationsspezifikation
	 * verwendet werden, da der erzeugte Ebeneniterator dafür nicht geeignet ist.<br>
	 * <b>Beispiel:</b><br>
	 * index = [3,5,1,6]<br>
	 * searchedDuplicateLayers = [1,3]<br>
	 * Dann hat der erzeugte EbenenIterator als startIndices=[3,1,1,1], endeIndices=[3,7,1,8],
	 * wobei 7 und 8 die max. gesetzten Werte für die jeweiligen Kontextebenen sind.
	 */
	private _createEbenenIteratorAbPosition(
		index: number[],
		searchedDuplicateLayers: number[],
		semanticIndexLevel: number
	): EbenenIterator {
		const startIndices: number[] = index.slice(0);
		const endeIndices: number[] = index.slice(0);
		for (const layer of searchedDuplicateLayers) {
			startIndices[layer] = 1;
			endeIndices[layer] = this.validationCache.getMaxGesetzterKontext(layer);
		}
		const zeile = startIndices.length - 1;
		startIndices[zeile] = 1;
		endeIndices[zeile] = 1;

		return new EbenenIterator(
			startIndices,
			endeIndices,
			this.validationCache.getGesetzteWiederholungen(),
			semanticIndexLevel
		);
	}

	/**
	 * Sammelt für alle Wiederholungen, die durch den übergebenen Iterator gegeben sind, die
	 * Wertekonstellation der Felder. Kommt die gleiche Wertekonstellation bereits für eine andere
	 * Wiederholung vor, werden beide Wiederholungen (d.h. die entsprechenden long-Arrays) in die
	 * passende
	 * Menge eingefügt: <br>
	 * In die Menge mit den Wertefehlern, wenn für die Wiederholung alle Felder angegeben sind und
	 * in die Menge mit den Auslassungsfehlern, andernfalls.
	 */
	private _ermittleWiederholungenMitDuplikaten(
		entityList: RtIdentifierTemplate[],
		iter: EbenenIterator,
		repNotUnqCache: RepetitionNotUniqueErrorCache
	): void {
		const werteProWiederholung: ArrayMap<number[], (string | VkBigDecimal | undefined)[]> = new ArrayMap();
		for (const aktW of new IteratorWrapper(iter.iterator())) {
			const aktuelleWerte = this._getWerteOrNullIfEinFeldNichtPruefungsRelevant(aktW, entityList);
			if (!aktuelleWerte) {
				continue;
			}
			const aktuelleWiederholung: number[] = aktW.getIndexes();
			// suche aktuelle Wertekonstellation in den bereits gesammelten
			for (const [w, value] of werteProWiederholung) {
				const result: ValidierungsErgebnis = this._werteKonstellationenNichtEindeutig(
					aktuelleWerte,
					value
				);
				if (result === ValidierungsErgebnis.TRUE_AF) {
					repNotUnqCache.addErroneousRepetitionWithOmissionsError(w, aktuelleWiederholung);
				}
				if (result === ValidierungsErgebnis.TRUE_WF) {
					repNotUnqCache.addErroneousRepetitionWithValueError(w, aktuelleWiederholung);
				}
			}
			werteProWiederholung.set(aktuelleWiederholung, aktuelleWerte);
		}
	}

	/**
	 * Hilfsmethode für wiederholungNichtEindeutig. <br>
	 * Für beide Wertekonstellation wird überprüft, ob sie in jedem Wert gleich sind.<br>
	 * Es wird über die Wertelisten iteriert. Sobald an einer Stelle der Liste für beide
	 * Wertekonstellationen ein Wert angegeben ist, der nicht gleich ist, kann FALSE zurückgegeben
	 * werden.<br>
	 * Ist für einen Vergleichswert ein Wert <code>undefined</code> und der andere angegeben, wird
	 * ebenfalls FALSE zurückgegeben.<br>
	 * Sind die Wertekosntallationen gleich, wird TRUE_AF zurückgegeben, wenn ein <code>undefined</code>
	 * Wert gefunden, sonst TRUE_WF.
	 */
	private _werteKonstellationenNichtEindeutig(
		werte1: (string | VkBigDecimal | undefined)[],
		werte2: (string | VkBigDecimal | undefined)[],
	): ValidierungsErgebnis {
		if (werte1 == undefined || werte2 == undefined) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		let anzNullGefunden = 0;
		for (let i = 0; i < werte1.length; i++) {
			const wert1 = werte1[i];
			const wert2 = werte2[i];
			if (wert1 == undefined && wert2 == undefined) {
				anzNullGefunden++;
				continue;
			}
			if (wert1 == undefined || wert2 == undefined) {
				// einer der Werte nicht undefined wg. vorheriger Prüfung -> Werte ungleich
				return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
			}

			if (!this._isEqual(wert1, wert2)) {
				return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
			}
		}
		// Falls kein Feld für beide Wiederholungen angegeben wurde, gelten die Werte nicht als gleich.
		if (anzNullGefunden === werte1.length) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		if (anzNullGefunden > 0) {
			return ValidierungsErgebnis.TRUE_AF;
		}
		return ValidierungsErgebnis.TRUE_WF;
	}

	/**
	 * Hilfsmethode für wiederholungNichtEindeutig. <br>
	 * <b>Vorbedingung:</b> nur Zahlen (oder Geldbeträge) oder Strings als Feldtypen erlaubt.
	 * <p>
	 * Schreibt die Werte der Wiederholung, die durch das Index-Array spezifiziert wurde, in die
	 * Rückgabeliste. <br>
	 * Wenn ein Wert nicht prüfungsrelevant ist oder alle Felder nicht angegeben, wird
	 * <code>undefined</code> zurückgegeben.<br>
	 * Wenn ein Wert nicht angegeben ist, andere Werte aber schon, wird <code>undefined</code> in die
	 * Rückgabeliste geschrieben.<br>
	 * Jeder angegebene Wert wird als VkBigDecimal oder als String in die Rückgabeliste
	 * geschrieben, so dass dann die richtige equals-Methode aufgerufen wird.
	 * </p>
	 */
	private _getWerteOrNullIfEinFeldNichtPruefungsRelevant(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): (string | VkBigDecimal | undefined)[] | undefined {
		const werteFuerWiederholung: (string | VkBigDecimal | undefined)[] = [];
		let angegebenesFeldGefunden = false;
		for (const rtIdContainer of entityList) {
			const id: RtInternalIdentifier = RtInternalIdentifier.createFromTemplate(
				rtIdContainer,
				index
			);

			const b: DreiWertBool = this.isWertAngegeben(id, false);
			switch (b) {
				case DreiWertBool.UNKNOWN:
					return undefined;
				case DreiWertBool.FALSE:
					werteFuerWiederholung.push(undefined);
					break;
				case DreiWertBool.TRUE:
					angegebenesFeldGefunden = true;
					if (MetaDataUtilValidierung.isNumberFeld(id.getName(), this.metaData)) {
						const v: VkBigDecimal = this.feldWertAlsZahl(id);
						werteFuerWiederholung.push(v);
					} else {
						werteFuerWiederholung.push(this._feldWertIntern(id));
					}
					break;
				default:
					throw new Error(
						"The type 'DreiWertBool' can only hold the values true, false or unknown."
					);
			}
		}

		// ein Wert ist angegeben => gib das Ergebnis zurück
		if (angegebenesFeldGefunden) {
			return werteFuerWiederholung;
		}
		// kein Feld angegeben
		return undefined;
	}

	/**
	 * nichtGenauEinFeldAngegeben gibt
	 * <ul>
	 * <li>TRUE_AF zurück, falls alle Felder aus entityList prüfungsrelevant sind, aber keins
	 * angegeben wurde oder wenn mehr als ein Feld angegeben wurde und eines von diesen eine
	 * Filterbedingung hat</li>
	 * <li>TRUE_WF zurück, falls mehr als ein prüfungsrelevantes Feld ohne Filterbedingung angegeben
	 * wurde</li>
	 * <li>FALSE sonst</li>
	 * </ul>
	 */
	public nichtGenauEinFeldAngegeben(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		if (this.keinFeldAngegeben(index, entityList).isKnownAndTrue()) {
			return ValidierungsErgebnis.TRUE_AF;
		}
		return this.mehrAlsEinFeldAngegeben(index, entityList);
	}

	/**
	 * Implements the operator <code>Invalid</code> for date constructions. Returns
	 * <ul>
	 * <li>{@link ValidierungsErgebnis.TRUE_WF}, if all fields used for the creation of date are
	 * relevant and filled and date is a not valid date</li>
	 * <li>{@link ValidierungsErgebnis.TRUE_AF}, if all fields used for the creation of date are
	 * relevant, but at least one them is not filled</li>
	 * <li>{@link ValidierungsErgebnis.FALSE_OR_UNKNOWN} otherwise</li>
	 * </ul>
	 *
	 * @param date 	the constructed date to be checked
	 * @return s. above
	 */
	public dateUngueltig(date: VkDate): ValidierungsErgebnis {
		if (date.hasValue || date.isNichtPruefungsrelevant) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		return date.isNichtAngegeben ? ValidierungsErgebnis.TRUE_AF : ValidierungsErgebnis.TRUE_WF;
	}

	/**
	 * Implements the operator <code>Valid</code> for date constructions. Returns
	 * <ul>
	 * <li>{@link ValidierungsErgebnis.TRUE_WF}, if all fields used for the creation of date are
	 * relevant and filled and date is a valid date</li>
	 * <li>{@link ValidierungsErgebnis.FALSE_OR_UNKNOWN} otherwise</li>
	 * </ul>
	 *
	 * @param date 	the constructed date to be checked
	 * @return s. above
	 */
	public dateGueltig(date: VkDate): ValidierungsErgebnis {
		if (!date.hasValue || date.isNichtPruefungsrelevant) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		return date.isNichtAngegeben ? ValidierungsErgebnis.TRUE_AF : ValidierungsErgebnis.TRUE_WF;
	}

	/**
	 * Implements the operator <code>Gueltig</code> for predefined types. Returns
	 * <ul>
	 * <li>{@link ValidierungsErgebnis#TRUE_WF}, if the field is relevant, filled and valid relating
	 * to the predefined type. <br>
	 * <b>Note:</b> For predefined types that define a display value different to the XML value, the
	 * check is carried out without any conversion i.e. it is checked if the value is a valid XML
	 * value for the predefined type.</li>
	 * <li>{@link ValidierungsErgebnis#FALSE} otherwise</li>
	 * </ul>
	 */
	public predefinedTypeGueltig(
		idx: IterationState,
		rtIdCon1: RtIdentifierTemplate,
		customFieldType: string
	): ValidierungsErgebnis {
		const _id = RuntimeController.makeIdentifier(idx, rtIdCon1);
		if (this.isWertAngegeben(_id, false) != DreiWertBool.TRUE) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		const _formatDef = new FormatDefinitionVordefiniert(
			CustomFieldTypeValidationParamImpl.UNSPECIFIED_LENGTH,
			CustomFieldTypeValidationParamImpl.UNSPECIFIED_LENGTH,
			customFieldType
		);
		const _value = this.validationCache.getXMLValue(_id);

		// value is always defined, because we check if the value is set
		// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
		if (ValidType.Valid === _formatDef.validateWithoutErrorReporting(_value!, this.modus, false)) {
			return ValidierungsErgebnis.TRUE_WF;
		}
		return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
	}

	/**
	 * Implements the operator <code>Ungueltig</code> for predefined types. Returns
	 * <ul>
	 * <li>{@link ValidierungsErgebnis#TRUE_WF}, if the field is relevant, filled and invalid
	 * relating to the predefined type.
	 * <b>Note:</b> For predefined types that define a display value different to the XML value, the
	 * check is carried out without any conversion i.e. it is checked if the value is a valid XML
	 * value for the predefined type.</li>
	 * <li>{@link ValidierungsErgebnis#FALSE} otherwise</li>
	 * </ul>
	 */
	public predefinedTypeUngueltig(
		idx: IterationState,
		rtIdCon1: RtIdentifierTemplate,
		customFieldType: string
	): ValidierungsErgebnis {
		const _id = RuntimeController.makeIdentifier(idx, rtIdCon1);
		if (this.isWertAngegeben(_id, false) != DreiWertBool.TRUE) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		const _formatDef = new FormatDefinitionVordefiniert(
			CustomFieldTypeValidationParamImpl.UNSPECIFIED_LENGTH,
			CustomFieldTypeValidationParamImpl.UNSPECIFIED_LENGTH,
			customFieldType
		);
		const _value = this.validationCache.getXMLValue(_id);

		// value is always defined, because we check if the value is set
		// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
		if (ValidType.Valid !== _formatDef.validateWithoutErrorReporting(_value!, this.modus, false)) {
			return ValidierungsErgebnis.TRUE_WF;
		}
		return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
	}

	/**
	 * felderNichtGemeinsamAngegeben gibt TRUE_AF zurück, falls mindestens eins,
	 * aber nicht alle prüfungsrelevanten Felder angegeben wurden.
	 * <p>
	 * Es wird nicht optimiert, da für diese Methode keine Stern-Operation für hohe
	 * Wiederholbarkeiten erwartet wird.
	 * </p>
	 */
	public felderNichtGemeinsamAngegeben(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		return this.entitiesNichtGemeinsamAngegeben(index, entityList);
	}

	public kontexteNichtGemeinsamAngegeben(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		return this.entitiesNichtGemeinsamAngegeben(index, entityList);
	}

	private entitiesNichtGemeinsamAngegeben(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		if (this.mindestensEineEntityAngegeben(index, entityList) === ValidierungsErgebnis.FALSE_OR_UNKNOWN) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		if (this.nichtAlleOderKeineEntityAngegeben(index, entityList) === ValidierungsErgebnis.FALSE_OR_UNKNOWN) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		return ValidierungsErgebnis.TRUE_AF;
	}

	/**
	 * Liefert
	 * <ul>
	 * <li>TRUE_WF, wenn zwei prüfungsrelevante Felder ohne Filterbedingung in der entityList den
	 * gleichen String- bzw. Zahl-Wert haben.</li>
	 * <li>TRUE_AF, wenn zwei prüfungsrelevante Felder in der entityList den gleichen String- bzw.
	 * Zahl-Wert haben und eines der Felder in entityList eine Filterbedingung hat.</li>
	 * <li>FALSE sonst</li>
	 * </ul>
	 */
	public feldWerteNichtEindeutig(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		// all fields must have the same type
		const isZahl: boolean = this.isZahlFeld(entityList[0].getUniqueName());
		// speichert die gefundenen Werte
		const werte: HashTable<VkBigDecimal | string> = new HashTable<VkBigDecimal | string>(
			(n: VkBigDecimal | string) => numbersconv(n),
			(a: VkBigDecimal | string, b: VkBigDecimal | string) => this._feldWerteEquals(a, b)
		);
		let containsFilter = false;
		for (const rtIdContainer of entityList) {
			if (rtIdContainer.isAggregation()) {
				const fIt: EntityIterator = new EntityIterator(rtIdContainer, index);
				this._setzeEndIndicesAufMaxGesetzt(fIt, false);
				for (const id of fIt) {
					if (this.isWertAngegeben(id, false) === DreiWertBool.TRUE) {
						if (rtIdContainer.getFilter()) {
							containsFilter = true;
						}
						// nur prüfungsrelavante, angegebene Werte werden gesammelt
						const v: VkBigDecimal | string = isZahl
							? this.feldWertAlsZahl(id)
							: this._feldWertIntern(id);
						if (!v || werte.has(v)) {
							return containsFilter ? ValidierungsErgebnis.TRUE_AF : ValidierungsErgebnis.TRUE_WF;
						} else {
							werte.add(v);
						}
					}
				}
			} else {
				const rtId = RtInternalIdentifier.createFromTemplate(rtIdContainer, index);
				if (this.isWertAngegeben(rtId, false) === DreiWertBool.TRUE) {
					// nur prüfungsrelavante, angegebene Werte werden gesammelt
					const v: VkBigDecimal | string = isZahl
						? this.feldWertAlsZahl(rtId)
						: this._feldWertIntern(rtId);
					if (!v || werte.has(v)) {
						return containsFilter ? ValidierungsErgebnis.TRUE_AF : ValidierungsErgebnis.TRUE_WF;
					} else {
						werte.add(v);
					}
				}
			}
		}
		return ValidierungsErgebnis.FALSE_OR_UNKNOWN; // alle prüfungsrelevanten Felder der Feldliste haben verschiedene Werte
	}

	private _feldWerteEquals(a: VkBigDecimal | string, b: VkBigDecimal | string): boolean {
		if (a instanceof VkBigDecimal && b instanceof VkBigDecimal) {
			return a.compareTo(b) === 0;
		} else {
			return a === b;
		}
	}

	/**
	 * Konvertiert ein any-Array in ein Array von VkStrings. Die Werte sind entweder Strings
	 * (bei Referenzierung von Stringkonstanten) oder VkStrings (bei Verwendung von Feldwert oder
	 * BereichAlsString).
	 *
	 * Vorbedingung: das any-Array enthält nur Objekte vom Typ String und/oder VkString.
	 */
	private _convertToVkStrings(values: any[]): VkString[] {
		const vkStrings: VkString[] = new Array<VkString>(values.length);
		for (let i = 0; i < values.length; i++) {
			if (typeof values[i] === "string") {
				vkStrings[i] = new VkString(<string>values[i]);
			} else if (values[i] instanceof VkString) {
				vkStrings[i] = values[i];
			}
		}
		return vkStrings;
	}

	/**
	 * gibt TRUE_WF zurück, falls der Wert des Feldes in der Werteliste ist
	 * (mittels Stringvergleich ermittelt).
	 * Ist der Wert nicht angegeben, wird FALSE zurückgegeben.
	 */
	public feldWertInWerteListeSTRING(
		idOrIdx: RtInternalIdentifier | IterationState,
		entityOrValues: RtIdentifierTemplate | any[] | VkString[],
		werte?: any[] | VkString[]
	): ValidierungsErgebnis {
		let values: VkString[];
		let id: RtInternalIdentifier;

		if (idOrIdx instanceof RtInternalIdentifier) {
			id = idOrIdx;
			values = RuntimeUtils.isVkStringArray(werte)
				? <VkString[]>werte
				: this._convertToVkStrings(<any[]>werte);
		} else {
			id = RuntimeController.makeIdentifier(idOrIdx, <RtIdentifierTemplate>entityOrValues);
			values = RuntimeUtils.isVkStringArray(werte)
				? <VkString[]>werte
				: this._convertToVkStrings(<any[]>werte);
		}

		const value: VkString = this.feldWert(id);
		// nicht prüfungsrelevante oder nicht angegebene Felder in der Werteliste können nicht zu einem Fehler führen
		if (value.length() !== 0 && values.some(v => v.equals(value))) {
			// wir haben einen Wert und dieser ist in der Werteliste enthalten
			return ValidierungsErgebnis.TRUE_WF;
		}
		return ValidierungsErgebnis.FALSE_OR_UNKNOWN; // kein Wert in der Werteliste gefunden
	}

	/**
	 * liefert TRUE_WF, falls der Wert des Feldes in der Werteliste ist
	 * (mittels Zahlvergleich ermittelt).
	 * Ist der Wert nicht angegeben, wird FALSE zurückgegeben.
	 */

	public feldWertInWerteListe(
		idx: IterationState,
		entity: RtIdentifierTemplate,
		werte: VkBigDecimal[]
	): ValidierungsErgebnis {
		const id: RtInternalIdentifier = RuntimeController.makeIdentifier(idx, entity);
		return this._feldWertInWerteListe(id, werte);
	}

	private _feldWertInWerteListe(
		id: RtInternalIdentifier,
		werte: VkBigDecimal[]
	): ValidierungsErgebnis {
		const wert: string = this._feldWertIntern(id);
		if (wert.length !== 0) {
			// wir haben einen Wert
			const d: VkBigDecimal = BigDecimalUtils.makeDecimal(wert);
			for (const w of werte) {
				/**
				 * Da in der Werteliste neben Konstanten nur Feldwerte (keine Rechenoperationen)
				 * stehen können, bedeutet kann größer / kleiner werden hier, dass das Feld
				 * nicht angegeben war.
				 */
				if (
					d.equals(w) &&
					!w.isNichtPruefungsrelevant &&
					!w.kannGroesserWerden &&
					!w.kannKleinerWerden
				) {
					return ValidierungsErgebnis.TRUE_WF;
				}
			}
		}
		return ValidierungsErgebnis.FALSE_OR_UNKNOWN; // kein Wert in der Werteliste gefunden
	}

	public feldWertNichtInWerteListeSTRING(
		idx: IterationState,
		entity: RtIdentifierTemplate,
		werte: any[]
	): ValidierungsErgebnis {
		const id: RtInternalIdentifier = RuntimeController.makeIdentifier(idx, entity);
		const values: VkString[] = RuntimeUtils.isVkStringArray(werte)
			? werte
			: this._convertToVkStrings(werte);
		return this._feldWertNichtInWerteListeSTRING(id, values);
	}

	/**
	 * Liefert TRUE_WF bzw. TRUE_AF, wenn der Wert des Feldes nicht in der Werteliste vorkommt: <br>
	 * TRUE_WF, wenn alle betroffenen Felder angegeben sind (Werte in der
	 * Werteliste können auch Ergebnisse von Feldoperationen sein). <br>
	 * TRUE_AF, wenn eines der beteiligten Felder nicht angegeben ist. <br>
	 * FALSE sonst.
	 */
	private _feldWertNichtInWerteListeSTRING(
		id: RtInternalIdentifier,
		werte: VkString[]
	): ValidierungsErgebnis {
		const value: VkString = this.feldWert(id);
		if (value.length() === 0 || werte.some(wert => wert.equals(value))) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		for (const s of werte) {
			if (s.isNichtPruefungsrelevant) {
				return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
			}
			if (s.isNichtAngegeben) {
				return ValidierungsErgebnis.TRUE_AF;
			}
		}
		return ValidierungsErgebnis.TRUE_WF;
	}

	private _isEqual(
		value1: string | VkBigDecimal,
		value2: string | VkBigDecimal,
	): boolean {
		if (value1 instanceof VkBigDecimal) {
			return value1.equals(value2);
		} else {
			return value1 === value2;
		}
	}

	/**
	 * Liefert TRUE_WF bzw. TRUE_AF (bei nicht angegebenen Feldern), wenn der Wert des Feldes nicht
	 * in der Werteliste vorkommt, FALSE sonst. <br>
	 */
	public feldWertNichtInWerteListe(
		idx: IterationState,
		entity: RtIdentifierTemplate,
		werte: VkBigDecimal[]
	): ValidierungsErgebnis {
		const id: RtInternalIdentifier = RuntimeController.makeIdentifier(idx, entity);
		return this._feldWertNichtInWerteListe(id, werte);
	}
	private _feldWertNichtInWerteListe(
		id: RtInternalIdentifier,
		werte: VkBigDecimal[]
	): ValidierungsErgebnis {
		const wert: string = this._feldWertIntern(id);
		const d: VkBigDecimal = BigDecimalUtils.makeDecimal(wert);
		if (wert.length === 0) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		let nichtAngegebenesFeldGefunden = false;
		for (const w of werte) {
			if (w.isNichtPruefungsrelevant) {
				return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
			}
			/**
			 * Da in der Werteliste neben Konstanten nur Feldwerte (keine Rechenoperationen) stehen
			 * können, bedeutet kann größer / kleiner werden hier, dass das Feld nicht angegeben
			 * war.
			 */
			if (w.kannGroesserWerden || w.kannKleinerWerden) {
				nichtAngegebenesFeldGefunden = true;
			} else if (d.equals(w)) {
				return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
			}
		}
		return nichtAngegebenesFeldGefunden
			? ValidierungsErgebnis.TRUE_AF
			: ValidierungsErgebnis.TRUE_WF;
	}

	/**
	 * Leerstring wird zu der Zahl 0, dies entspricht dem definierten Defaultverhalten.
	 */
	public feldWertAlsZahl(id: RtInternalIdentifier): VkBigDecimal;
	/**
	 * This is the method which is used in the generated code
	 */
	public feldWertAlsZahl(index: IterationState, entity: RtIdentifierTemplate): VkBigDecimal;
	public feldWertAlsZahl(
		idOrIndex: RtInternalIdentifier | IterationState,
		entity?: RtIdentifierTemplate
	): VkBigDecimal {
		let _id: RtInternalIdentifier;
		if (idOrIndex instanceof RtInternalIdentifier) {
			_id = idOrIndex;
		} else {
			// cannot be undefined because the method with both parameter set is called
			// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
			_id = RuntimeController.makeIdentifier(idOrIndex, entity!);
		}
		const fd: IFormatDefinition = this.metaData.getIFormatDefinition(_id.getName());
		return BigDecimalUtils.makeDecimal(this.feldWert(_id), fd);
	}

	public feldWertAlsString(index: IterationState, entity: RtIdentifierTemplate): VkString {
		return this.feldWert(index, entity);
	}

	/**
	 * Zugriff auf die Feldwerte.
	 * Falls das Feld nicht prüfungsrelevant oder nicht angegeben ist, merkt man sich diese
	 * Information im Rückgabeobjekt.
	 * Es wird immer der Wert des Feldes oder Leerstring falls Wert nicht angegeben ist,
	 * zurückgegeben.
	 */
	public feldWert(id: RtInternalIdentifier): VkString;
	/**
	 * This is the method which is used in the generated code.
	 */
	public feldWert(index: IterationState, entity: RtIdentifierTemplate): VkString;
	public feldWert(
		idOrIndex: RtInternalIdentifier | IterationState,
		entity?: RtIdentifierTemplate
	): VkString {
		let _id: RtInternalIdentifier;
		if (idOrIndex instanceof RtInternalIdentifier) {
			_id = idOrIndex;
		} else {
			// cannot be undefined because the method with both parameter set is called
			// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
			_id = RuntimeController.makeIdentifier(idOrIndex, entity!);
		}
		if (!this._isPruefungsRelevant(_id, false)) {
			return VkString.NICHT_PRUEF_REL_STRING;
		}
		const wertXML: string | undefined = this.validationCache.getXMLValue(_id);
		// Falls nicht im Cache: nicht vorhanden d.h. gebe Leerstring zurück
		if (!wertXML) {
			return VkString.NICHT_ANGEGEBENER_STRING;
		} else {
			return new VkString(wertXML);
		}
	}

	/**
	 * Wird an allen Stellen verwendet, an denen die zusätzlichen Informationen von VkString nicht
	 * benötigt werden.
	 */
	private _feldWertIntern(id: RtInternalIdentifier): string {
		if (!this._isPruefungsRelevant(id, false)) {
			return "";
		}
		// Falls nicht im Cache: nicht vorhanden d.h. gebe Leerstring zurück
		return this.validationCache.getXMLValue(id) ?? "";
	}

	/**
	 * Gibt den Wert des Feldes als Datum zurück.
	 *
	 * Anmerkung:
	 * Direkt von der Sprache darf bei teilbekanntem Datum nicht auf den FeldWert zugegriffen werden.
	 * Die Methode wird aber intern verwendet für MonatAusDatum, JahrAusDatum.
	 * Es ist sichergestellt, dass dann das abgefragte Datumsfragment nicht teilbekannt ist. d.h.
	 * eine Ersetzung durch jeweils "01" bei Teilbekanntheit ist o.k., da diese Teile nicht
	 * abgefragt werden.
	 */
	public feldWertAlsDatum(idx: IterationState, entity: RtIdentifierTemplate): VkDate;
	public feldWertAlsDatum(id: RtInternalIdentifier): VkDate;
	public feldWertAlsDatum(
		idxOrId: IterationState | RtInternalIdentifier,
		entity?: RtIdentifierTemplate
	): VkDate {
		let _id: RtInternalIdentifier;
		if (idxOrId instanceof RtInternalIdentifier) {
			_id = idxOrId;
		} else {
			// cannot be undefined because the method with both parameter set is called
			// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
			_id = RuntimeController.makeIdentifier(idxOrId, entity!);
		}
		const feldWert: VkString = this.feldWert(_id);

		const formatDef: FormatDefinitionDatum = <FormatDefinitionDatum>(
			this.metaData.getIFormatDefinition(_id.getName())
		);
		return this.feldWertAlsDatumIntern(feldWert, formatDef);
	}

	public feldWertAlsDatumIntern(feldWert: VkString, formatDef: FormatDefinitionDatum) {
		if (formatDef == undefined) {
			return new VkDate(undefined);
		}
		if (!feldWert.hasValue || feldWert.isNichtPruefungsrelevant) {
			return new VkDate(undefined, feldWert.isNichtAngegeben, feldWert.isNichtPruefungsrelevant);
		}
        const millis: number | null = this._millisForDatumFeldwert(feldWert.wert, formatDef.format, formatDef.isTeilbekannt);
        return millis === null
            ? VkDate.NON_RELEVANT_DATE
            : new VkDate(new Date(millis), feldWert.isNichtAngegeben, false);
	}

    private _millisForDatumFeldwert(wert: string, format: string, isTeilbekannt: boolean): number | null {
        const cacheKeySeparator = "\u0000";
        const cacheKey: string =
            wert + cacheKeySeparator +
            format + cacheKeySeparator +
            (isTeilbekannt ? "T" : "F");
        const cachedMillis: number | null | undefined = this._dateValueCache.get(cacheKey);
        if (cachedMillis !== undefined) {
            return cachedMillis;
        }
        const millis = this._millisForDatumFeldWertUncached(wert, format, isTeilbekannt);
        this._dateValueCache.set(cacheKey, millis);
        return millis;
    }

	private _millisForDatumFeldWertUncached(wert: string, format: string, isTeilbekannt: boolean): number | null {
        const dateWithFormat: DateAndFormat = new DateAndFormat(wert, format, this.timeZone);
        if (PartiallyKnownDatesUtil.isYearUnknown(dateWithFormat)) {
            /* This should not happen as
             * 1. the parser is supposed to prevent the situation that this method is called for date fields with YEAR_OPTIONAL
             * 2. the method is only called with formally correct values
             * to prevent a ParseException in case that a parser check is missing, return a non relevant value
             */
            return null;
        }
        if (isTeilbekannt) {
            PartiallyKnownDatesUtil.replaceMonthAndYearIfUnknown(
                dateWithFormat,
                DateInterval.INTERVAL_START
            );
        }
        const datum: Date = DateUtil.getValueAsDate(dateWithFormat, this.metaData.getBaseYearOrDefault());
        return datum.getTime();
    }

    public feldWertAlsDateRange(idx: IterationState, entity: RtIdentifierTemplate): VkDateRange;
	public feldWertAlsDateRange(id: RtInternalIdentifier): VkDateRange;
	public feldWertAlsDateRange(
		idxOrId: IterationState | RtInternalIdentifier,
		entity?: RtIdentifierTemplate
	): VkDateRange {
		let _id: RtInternalIdentifier;
		if (idxOrId instanceof RtInternalIdentifier) {
			_id = idxOrId;
		} else {
			// cannot be undefined because the method with both parameter set is called
			// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
			_id = RuntimeController.makeIdentifier(idxOrId, entity!);
		}
		const feldWert: VkString = this.feldWert(_id);

		const formatDef: FormatDefinitionDatumBereich = <FormatDefinitionDatumBereich>(
			this.metaData.getIFormatDefinition(_id.getName())
		);
		return this.feldWertAlsDateRangeIntern(feldWert, formatDef);
	}

	public feldWertAlsDateRangeIntern(feldWert: VkString, formatDef: FormatDefinitionDatumBereich): VkDateRange {
		if (formatDef == undefined) {
			return VkDateRange.NULL_DATE_RANGE;
		}
		if (!feldWert.hasValue || feldWert.isNichtPruefungsrelevant) {
			return new VkDateRange(undefined, undefined, feldWert.isNichtAngegeben, feldWert.isNichtPruefungsrelevant);
		}
		const wert: string = feldWert.wert;
		const di: DarstellungsInformationIntern = new DarstellungsInformationIntern(
			this.metaData.getDezimalTrenner(),
			this.modus.getDataFormats(),
			this.metaData.getBaseYearOrDefault(),
			this.timeZone
		);

		const datumBereichTrenner: string = formatDef.trenner;
		const datumFormat: string = formatDef.format;
		const datumTrennerHintenOpt: boolean = di.datumTrennerHintenOptional(datumFormat);
		const jahr: number = this.metaData.getBaseYearOrDefault();

		const dates: DateAndFormat[] = DateUtil.getDateRange(
			wert,
			datumBereichTrenner,
			datumFormat,
			datumTrennerHintenOpt,
			jahr,
			formatDef.interpretationOfYear,
			this.timeZone
		);

		return RuntimeController.datesAndFormatsToDateRange(dates, jahr, feldWert.isNichtAngegeben);
	}

	private static datesAndFormatsToDateRange(dates: DateAndFormat[], jahr: number, canChangeIfEmtpyFieldGiven: boolean): VkDateRange {
		const start = DateUtil.createDateFromDateFragment(dates[0], DateInterval.INTERVAL_START, jahr)
		const end = DateUtil.createDateFromDateFragment(dates[1], DateInterval.INTERVAL_END, jahr)

		return new VkDateRange(start, end, canChangeIfEmtpyFieldGiven, false);
	}

	/**
	 * Gibt die Anzahl der angegebenen Kontexte zurück.
	 * In jedem Kontext wird nur solange iteriert, bis ein angegebenes Feld gefunden wurde oder
	 * bis es keine neuen Felder und keine neuen Wiederholungen gibt.
	 *
	 * @param kIters 	Liste der Kontexte als KontextIteratoren, von denen die Anzahl der angegebenen
	 *            Kontexte ermittelt wird
	 *
	 * @return Anzahl der angegebenen Kontexte <p>
	 *         <b>Anmerkung:</b> aus Performancegründen wird bei der Teilvalidierung nicht weiter
	 *         ausgewertet, sobald ein nicht-prüfungsrelevantes Feld gefunden wird. Für den
	 *         Sonderfall, dass in einem untersuchten Kontext prüfungsrelevante und
	 *         nicht-prüfungsrelevante Felder vorhanden sind, führt dies dazu, dass die Reihenfolge
	 *         der Felder relevant wird. <br>
	 *         Beispiel: im Kontext K sind die Felder F1, F2, und F3. F2 ist nicht prüfungsrelevant,
	 *         F1 und F3 sind prüfungsrelevant. <br>
	 *         Dann gibt es folgende Szenarien:
	 *         <ul>
	 *         <li>F1 ist angegeben--> dann gilt der Kontext als angegeben und wird gezählt</li>
	 *         <li>F1 ist nicht angegeben, aber F3 ist angegeben. Da aber F2 vor F3 geprüft wird,
	 *         gilt die Bedingung als nicht auswertbar in der Teilbedingung.</li>
	 *         </ul>
	 *         Das Beispiel zeigt, dass die Reihenfolge der Felder bei der Auswertung relevant ist.
	 *         Die Behandlung führt jedoch nie dazu, dass fälschlicherweise eine Regelbedingung zu
	 *         true evaluiert (und dann einen Fehler auslösen würde). Es kann jedoch sein, dass bei
	 *         einer Teilvalidierung eine Regel nicht zu true evaluiert, obwohl man sie eigentlich
	 *         auswerten könnte. Dieses Vorgehen wurde aber aus Optimierungsgründen auch in anderen
	 *         Fällen gewählt. Ausserdem ist davon auszugehen, dass im Normalfall entweder alle
	 *         Felder eines Kontextes prüfungsrelevant sind oder keine. Daher tritt diese
	 *         Unsymmetrie bei der Auswertung nur in Sonderfällen auf.</p>
	 *
	 */
	public anzahlAngegebenerKontexte(
		index: IterationState,
		entityList: RtIdentifierTemplate[] | Map<RtIdentifierTemplate, RtIdentifierTemplate[]>
	): VkBigDecimal {
		if (entityList instanceof Map) {
			const kIters: KontextIterator[] = this._createKontextIteratorListe(index, entityList);
			return this._anzahlAngegebenerKontexteInternal(kIters);
		} else {
			return this._anzahlAngegebenerEntities(index, entityList);
		}
	}

	private _anzahlAngegebenerKontexteInternal(kIters: KontextIterator[]): VkBigDecimal {
		let anzAngKontexte = 0;
		let anzAllerKontexte = 0;
		for (const kIter of kIters) {
			if (
				this.validierungArt.isValidationPartWith3ValueLogic() &&
				kIter.istEineWiederholungNichtPruefungsRelevant(this.validationCache, false)
			) {
				return VkBigDecimal.NICHT_PRUEF_REL_ZAHL;
			}
			anzAllerKontexte += kIter.getAnzahlKontexte();
			kIter.optimiereIteratorWennMoeglich(this.validationCache, this.validierungArt);
			let id: RtInternalIdentifier | undefined = kIter.getNextEntity();
			while (id) {
				const b: DreiWertBool = this.isWertAngegeben(id, false);
				if (b === DreiWertBool.UNKNOWN) {
					return VkBigDecimal.NICHT_PRUEF_REL_ZAHL; // ein Feld nicht prüf. rel. -> korrekter Wert nicht relevant
				}
				if (b === DreiWertBool.TRUE) {
					anzAngKontexte++;
					kIter.setToNextWiederholung();
				}
				id = kIter.getNextEntity();
			}
		}
		const kannGroesserWerden: boolean = anzAllerKontexte > anzAngKontexte;
		return new VkBigDecimal(anzAngKontexte, false, kannGroesserWerden, false);
	}

	/**
	 * Erzeugt aus der Stringdarstellung einer Kontextliste inkl. Feldern eine Liste von
	 * Kontextiteratoren. Iterationsvariablen in den Kontext- und Feldlisten-Strings werden durch
	 * die Wiederholungen ersetzt, die durch das übergebene Index-Array gegeben sind.
	 */
	private _createKontextIteratorListe(
		index: IterationState,
		groupedEntityLists: Map<RtIdentifierTemplate, RtIdentifierTemplate[]>
	): KontextIterator[] {
		const kontextListe: KontextIterator[] = [];
		for (const [key, value] of groupedEntityLists) {
			kontextListe.push(new KontextIterator(key, value, index, this.metaData));
		}
		return kontextListe;
	}

	/**
	 * Gibt die Anzahl der angegebenen Felder in der Feldliste zurück.
	 * Ist ein Feld nicht prüfungsrelevant, wird 0 zurückgegeben.
	 * Die Information über nicht angegebene oder nicht prüfungsrelevante Felder
	 * wird gespeichert.
	 */
	public anzahlAngegebenerFelder(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): VkBigDecimal {
		return this._anzahlAngegebenerEntities(index, entityList);
	}

	private _anzahlAngegebenerEntities(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): VkBigDecimal {
		let anzAngFelder = 0;
		let anzAllerFelder = 0;
		let containsFilteredValue = false;
		for (const rtIdContainer of entityList) {
			const fIt: EntityIterator = new EntityIterator(rtIdContainer, index);
			if (
				this.validierungArt.isValidationPartWith3ValueLogic() &&
				fIt.istEineWiederholungNichtPruefungsRelevant(
					this.validationCache,
					true // always A12 model
				)
			) {
				return VkBigDecimal.NICHT_PRUEF_REL_ZAHL; // ein Feld nicht prüf. rel. -> korrekter Wert nicht relevant
			}
			anzAllerFelder += fIt.anzahlFelder();
			this._setzeEndIndicesAufMaxGesetzt(fIt, true);
			fIt.optimiereMitGesetztenWiederholungenWennSinnvoll(this.validationCache);
			for (const id of fIt) {
				const isGiven: DreiWertBool = this.isWertAngegeben(id, false);
				const b: DreiWertBool =
					isGiven !== DreiWertBool.FALSE ? isGiven : this.isWertAngegeben(id, true);
				if (b === DreiWertBool.UNKNOWN) {
					return VkBigDecimal.NICHT_PRUEF_REL_ZAHL; // ein Feld nicht prüf. rel. -> korrekter Wert nicht relevant
				}
				if (b === DreiWertBool.TRUE) {
					if (rtIdContainer.getFilter()) {
						containsFilteredValue = true;
					}
					anzAngFelder++;
				}
			}
		}
		if (containsFilteredValue) {
			return new VkBigDecimal(anzAngFelder, false, true, true);
		}
		const kannGroesserWerden: boolean = anzAllerFelder > anzAngFelder;
		return new VkBigDecimal(anzAngFelder, false, kannGroesserWerden, false);
	}

	public anzahlVerschiedenerWerteZahl(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): VkBigDecimal {
		return this._combineFeldListe(
			index,
			entityList,
			new NumberOfDifferentValuesCombiner<VkBigDecimal>(),
			x => this.feldWertAlsZahl(x),
			true
		);
	}

	public anzahlVerschiedenerWerteString(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): VkBigDecimal {
		return this._combineFeldListe(
			index,
			entityList,
			new NumberOfDifferentValuesCombiner<VkString>(),
			x => this.feldWert(x),
			true
		);
	}

	public anzahlVerschiedenerWerteDatum(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): VkBigDecimal {
		return this._combineFeldListe(
			index,
			entityList,
			new NumberOfDifferentValuesCombiner<VkDate>(),
			x => this.feldWertAlsDatum(x),
			true
		);
	}

	/**
	 * Implementiert das Sprachkonstrukt AnzahlAngegebenerVordrucke.
	 * Vergleiche, die dieses Sprachkonstrukt verwenden, führen bei einer Teilvalidierung nach 3-Wert-Logik nie zum
	 * Fehler.
	 */
	public anzVordrucke(vordruckName: string): VkBigDecimal {
		if (this.validierungArt.isValidationPartWith3ValueLogic()) {
			// Wert irrelevant, umfassende Vergleichsbedingung wird nicht ausgewertet
			return VkBigDecimal.NICHT_PRUEF_REL_ZAHL;
		}

		const groupPath = EntityUtil.getRootGroupPath(vordruckName);
		const anzVordrucke: number = this.validationCache.numberOfFilledGroupInstances(groupPath);
		const kannGroesserWerden = true; // für Auslassungsfehler
		return new VkBigDecimal(anzVordrucke, false, kannGroesserWerden, false);
	}

	public aktuelleVordruckNummer(id: RtInternalIdentifier): VkBigDecimal;
	/**
	 * This is the method which is used in the generated code
	 */
	public aktuelleVordruckNummer(index: IterationState, entity: RtIdentifierTemplate): VkBigDecimal;
	/**
	 * Gibt die laufende Nummer Vordruck des aktuellen Feldes zurück.
	 * Falls das Feld nicht prüfungsrelevant oder nicht angegeben ist,
	 * wird 0 zurückgegeben. 0 führt dazu, dass ein späterer
	 * Vergleich immer zu false führt.
	 */
	public aktuelleVordruckNummer(
		idOrIndex: RtInternalIdentifier | IterationState,
		entity?: RtIdentifierTemplate
	): VkBigDecimal {
		let _id: RtInternalIdentifier;
		if (idOrIndex instanceof RtInternalIdentifier) {
			_id = idOrIndex;
		} else {
			// no categories allowed
			// cannot be undefined because the method with both parameter set is called
			// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
			_id = RuntimeController.makeIdentifier(idOrIndex, entity!);
		}
		if (this.isWertAngegeben(_id, false) === DreiWertBool.TRUE) {
			// Feld angegeben und relevant
			return new VkBigDecimal(_id.getIndices()[0]);
		}
		return VkBigDecimal.NICHT_PRUEF_REL_ZAHL;
	}

	public aktuelleWiederholung(id: RtInternalIdentifier): VkBigDecimal;
	/**
	 * This is the method which is used in the generated code
	 */
	public aktuelleWiederholung(index: IterationState, entity: RtIdentifierTemplate): VkBigDecimal;
	/**
	 * Implements the language construct <code>CurrentRepetition</code>.
	 * If the current repetition is unknown (this can happen for semantic index or parallel
	 * iteration if the group is not relevant or not filled),
	 * {@link VkBigDecimal#NICHT_PRUEF_REL_ZAHL} is
	 * returned, which leads to the surrounding comparison evaluating to false.
	 */
	public aktuelleWiederholung(
		idOrIndex: RtInternalIdentifier | IterationState,
		entity?: RtIdentifierTemplate
	): VkBigDecimal {
		let id: RtInternalIdentifier;
		if (idOrIndex instanceof IterationState) {
			// if entity is undefined, we have an implementation error
			// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
			id = RuntimeController.makeIdentifier(idOrIndex, entity!);
		} else {
			id = idOrIndex;
		}
		const resolvedId = this.validationCache.resolveSemanticIndex(id);
		const indices = resolvedId.getIndices();
		const currentRepetition = indices[indices.length - 1];
		// If the current repetition is known, the number can be useful
		return currentRepetition !== IIdentifier.UNKNOWN
			? new VkBigDecimal(currentRepetition)
			: VkBigDecimal.NICHT_PRUEF_REL_ZAHL;
	}

	/**
	 * Implementiert das Sprachkonstrukt 'MinWert'.
	 */
	public minWert(index: IterationState, entityList: RtIdentifierTemplate[]): VkBigDecimal {
		return this._combineFeldListe(
			index,
			entityList,
			new MinWertCombiner(),
			x => this.feldWertAlsZahl(x),
			true
		);
	}

	/**
	 * Hilfsmethode für Feldlistenoperationen wie z.B. Summe, MaxWert, MinWert etc.
	 * @param sparsenessOptimization
	 *            allow optimization to skip (even non-trailing) empty fields. Empty fields will be
	 *            reported in summary to {@link Combiner#setOmittedTail(IFormatDefinition)} but the
	 *            order of empty-vs-filled fields may be lost (relevant with e.g.
	 *            {@link FirstValueCombinerDate} etc). Also, during partial validation, this
	 *            optimization will in some cases report falsely-non-relevant values.<br>
	 *
	 *            Note that empty fields beyond the highest non-empty repetition may be skipped
	 *            (and reported in summary to {@link Combiner#setOmittedTail(IFormatDefinition)})
	 *            regardless of the sparsenessOptimization flag.
	 */
	private _combineFeldListe<I, O>(
		index: IterationState,
		entityList: RtIdentifierTemplate[],
		c: Combiner<I, O>,
		valueProvider: (id: RtInternalIdentifier) => I,
		sparsenessOptimization: boolean
	): O {
		let containsFilter = false;
		for (const rtIdContainer of entityList) {
			const fIt: EntityIterator = new EntityIterator(rtIdContainer, index);
			const numFields: number = fIt.anzahlFelder();
			let visitedFields = 0;
			const fd = this.metaData.getIFormatDefinition(fIt.getEntityName());

			if (this._setzeEndIndicesAufMaxGesetzt(fIt, true)) {
				c.setOmittedTail(fd);
			}

			if (rtIdContainer.getFilter()) {
				containsFilter = true;
			}

			if (sparsenessOptimization) {
				fIt.optimiereMitGesetztenWiederholungenWennSinnvoll(this.validationCache);
				// note that istEineWiederholungNichtPruefungsRelevant returns false-positive in some cases (see docstring), which seems acceptable here
				if (
					this.validierungArt.isValidationPartWith3ValueLogic() &&
					fIt.istEineWiederholungNichtPruefungsRelevant(
						this.validationCache,
						true // always A12 model
					)
				) {
					return c.getNonRelevantValue(); //ein Feld nicht prüf. rel. -> korrekter Wert nicht relevant
				}
			}

			for (const id of fIt) {
				if (!this._isPruefungsRelevant(id, false)) {
					return c.getNonRelevantValue();
				}
				const value: I = valueProvider(id);
				c.addValue(value, fd, true);
				if (!c.canChange()) {
					return c.getValue(containsFilter);
				}
				visitedFields += 1;
			}
			if (sparsenessOptimization && visitedFields < numFields) {
				c.setOmittedTail(fd);
			}
		}
		return c.getValue(containsFilter);
	}

	public min(...operatorListe: VkBigDecimal[]): VkBigDecimal {
		return this._combineOperatorListe(operatorListe, new MinWertCombiner());
	}

	public minDatum(...operatorListe: VkDate[]): VkDate {
		return this._combineOperatorListe(operatorListe, new DateMinValueCombiner());
	}

	private _combineOperatorListe<I, O>(operatorListe: I[], c: Combiner<I, O>): O {
		for (const operator of operatorListe) {
			c.addValue(operator, undefined, false);
		}
		return c.getValue(false);
	}

	public vergleicheDATUM(op: VglOp, d1: VkDate, d2: VkDate): ValidierungsErgebnis {
		const nichtPruefungsrelevant: boolean =
			d1.isNichtPruefungsrelevant || d2.isNichtPruefungsrelevant;
		const nichtAngegeben: boolean = d1.isNichtAngegeben || d2.isNichtAngegeben;
		const vergleich: boolean = BedingungsOperatorHelper.vergleicheDATUM(op, d1.date, d2.date);

		if (!vergleich || nichtPruefungsrelevant) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		} else if (nichtAngegeben) {
			return ValidierungsErgebnis.TRUE_AF;
		} else {
			return ValidierungsErgebnis.TRUE_WF;
		}
	}

	public vergleicheDATERANGE(op: VglOp, d1: VkDateRange, d2: VkDateRange): ValidierungsErgebnis {
		const nichtPruefungsrelevant: boolean =
			d1.isNichtPruefungsrelevant || d2.isNichtPruefungsrelevant;
		const nichtAngegeben: boolean = d1.isNichtAngegeben || d2.isNichtAngegeben;
		const vergleich: boolean = BedingungsOperatorHelper.vergleicheDATERANGE(op, d1, d2);

		if (!vergleich || nichtPruefungsrelevant) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		} else if (nichtAngegeben) {
			return ValidierungsErgebnis.TRUE_AF;
		} else {
			return ValidierungsErgebnis.TRUE_WF;
		}
	}

	public vergleicheSTRING(
		op: VglOp,
		v1: VkString | string,
		v2: VkString | string
	): ValidierungsErgebnis {
		if (v1 instanceof VkString && this.isStringNotRelevant(v1)) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		if (v2 instanceof VkString && this.isStringNotRelevant(v2)) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}

		const canChange: boolean = (v1 instanceof VkString ? v1.isNichtAngegeben : false) || (v2 instanceof VkString ? v2.isNichtAngegeben : false);
		return BedingungsOperatorHelper.vergleicheSTRING(
			op,
			v1 instanceof VkString ? v1.wert : v1,
			v2 instanceof VkString ? v2.wert : v2
		)
			? (canChange ? ValidierungsErgebnis.TRUE_AF : ValidierungsErgebnis.TRUE_WF)
			: ValidierungsErgebnis.FALSE_OR_UNKNOWN;
	}

	private isStringNotRelevant(str: VkString) {
		return !str.hasValue || str.isNichtPruefungsrelevant;
	}

	public vergleiche(op: VglOp, v1: VkBigDecimal, v2: VkBigDecimal): ValidierungsErgebnis {
		return BedingungsOperatorHelper.vergleiche(op, v1, v2);
	}

	private _getSubstring(id: RtInternalIdentifier, start: number, ende: number): VkString {
		const feldWert: VkString = this.feldWert(id);
		const wert: string = feldWert.wert;
		// Es ist sichergestellt, dass start eine positive Zahl ist und <= ende
		const substring: string =
			!wert || 0 === wert.length || ende > wert.length ? "" : wert.substring(start, ende);
		return new VkString(substring, feldWert.isNichtAngegeben, feldWert.isNichtPruefungsrelevant);
	}

	/**
	 * bereichAlsString gibt den Substring des Feldwertes zurück. Es wird ein
	 * Leerstring zurückgegeben, wenn (1) der Feldwert nicht angegeben ist (2)
	 * die Indices nicht zur Länge des Feldwertes passen
	 *
	 * @param id 	Identifier der betrachtet wird
	 * @param start	Index Beginn (startend bei 0, wird bei der Codegenerierung
	 *            umgesetzt)
	 * @param ende 	Index Ende
	 * @return gibt den Teilstring zurück oder Leerstring, wenn das Interval außerhalb des Strings liegt
	 */
	public bereichAlsString(id: RtInternalIdentifier, start: number, ende: number): VkString {
		return this._getSubstring(id, start, ende);
	}

	/**
	 * bereichAlsZahl gibt den Substring des Feldwertes als Zahl zurück. Es
	 * wird 0 zurückgegeben, wenn <br>
	 * (1) der Feldwert nicht angegeben <br>
	 * (2) Indices nicht zur Länge des Feldwertes passen <br>
	 * (3) der Teilstring nicht in eine Zahl umgewandelt werden kann
	 *
	 * @param start	Index Beginn (startend bei 0, wird bei der Codegenerierung umgesetzt)
	 * @param ende 	Index Ende
	 * @return Teilstring als Zahl
	 */
	public bereichAlsZahl(id: RtInternalIdentifier, start: number, ende: number): VkBigDecimal {
		let d: VkBigDecimal = VkBigDecimal.ZAHL_0;
		const s: VkString = this._getSubstring(id, start, ende);
		const substring: string = s.wert;
		// EXT-7360: nur Ganzzahlen erlaubt
		if (RuntimeController._bereichAlsZahlRegExp.test(substring)) {
			d = BigDecimalUtils.makeDecimal(substring);
		}
		// kleiner werden kann es nicht (BereichAlsZahl gibt nur Zahlen >= 0 zurück)
		return new VkBigDecimal(d.toString(), s.isNichtPruefungsrelevant, s.isNichtAngegeben, false);
	}

	/**
	 * Vorbedingung:
	 * Feld ist Datumsfeld mit Tagesgenauigkeit Optional, Monatsgenauigkeit Optional oder
	 * Jahresgenauigkeit Optional. <br>
	 * Falls Feld nicht angegeben ist, wird undefined zurückgegeben.
	 * Wenn der Tag mit 00 spezifiziert ist, wird je nach Wert des
	 * Parameters isErsterTag der erste Tag oder der letzte Tag des Monats zurückgegeben.
	 * Wenn der Monat mit 00 spezifiziert ist, wird 01 oder 12 zurückgegeben.
	 * Wenn das Jahr mit 0000 spezifiziert ist, wird das Feld als nicht prüfungsrelevant behandelt.
	 *
	 * @return Datum oder undefined, falls kein valider Datumswert angegeben wurde
	 */
	public wertAlsDatum(id: RtInternalIdentifier, isAnfang: boolean, format: string): VkDate {
		const feldWert: VkString = this.feldWert(id);
		return this.wertAlsDatumIntern(feldWert, isAnfang, format);
	}

	public wertAlsDatumIntern(feldWert: VkString, isAnfang: boolean, format: string): VkDate {
		const wert: string = feldWert.wert;
		if (!feldWert.hasValue || feldWert.isNichtPruefungsrelevant) {
			return new VkDate(undefined, feldWert.isNichtAngegeben, feldWert.isNichtPruefungsrelevant);
		}

		const dateWithFormat: DateAndFormat = new DateAndFormat(wert, format, this.timeZone);
		if (PartiallyKnownDatesUtil.isYearUnknown(dateWithFormat)) {
			return VkDate.NON_RELEVANT_DATE; // Angabe 00.00.0000 wird behandelt wie nicht prüfungsrelevant
		}
		const intervalSpec: DateInterval = DateInterval.create(isAnfang);
		PartiallyKnownDatesUtil.replaceMonthAndYearIfUnknown(dateWithFormat, intervalSpec);
		return new VkDate(ValidationDateParser.parseDateWithoutSyntaxCheck(dateWithFormat), feldWert.isNichtAngegeben, false);
	}

	/**
	 * Gibt ein Datum zurück, konstruiert aus den Angaben Tag, Monat und Jahr.
	 * Ergeben die Angaben kein gültiges Datum, so wird undefined zurückgegeben.
	 *
	 * @return erzeugtes Datum oder undefined falls kein Datum erzeugt werden kann.
	 */
	constructDatum(
		tag: VkBigDecimal,
		monat: VkBigDecimal,
		jahrhundert: VkBigDecimal,
		jahrImJahrhundert?: VkBigDecimal
	): VkDate; // method 1 + 2
	constructDatum(wert: string, format: string): VkDate; // method 3
	constructDatum(
		tagODERwert: VkBigDecimal | string,
		monatODERformat: VkBigDecimal | string,
		jahrhundertODERjahr?: VkBigDecimal,
		jahrImJahrhundert?: VkBigDecimal
	): VkDate {
		if (typeof tagODERwert === "string" && typeof monatODERformat === "string") {
			// method 3
			if (!tagODERwert) {
				return new VkDate(undefined);
			}
			return new VkDate(
				DateUtil.getValueAsDate(
					new DateAndFormat(tagODERwert, monatODERformat, this.timeZone),
					this.metaData.getBaseYearOrDefault()
				)
			);
		} else if (!jahrImJahrhundert) {
			// method 1
			let date: Date | undefined;
			const tag: VkBigDecimal = <VkBigDecimal>tagODERwert;
			const monat: VkBigDecimal = <VkBigDecimal>monatODERformat;
			const jahr: VkBigDecimal = <VkBigDecimal>jahrhundertODERjahr;
			if (tag.intValue() !== 0 && monat.intValue() !== 0 && jahr.intValue() !== 0) {
				date = DateUtil.createDate(
					tag.intValue(),
					monat.intValue(),
					jahr.intValue(),
					this.timeZone
				);
			}
			const dateParts: VkBigDecimal[] = [tag, monat, jahr];
			return this._constructDatumHelper(date, dateParts);
		} else {
			// method 2
			let date: Date | undefined;
			const tag: VkBigDecimal = <VkBigDecimal>tagODERwert;
			const monat: VkBigDecimal = <VkBigDecimal>monatODERformat;
			const jahrhundert: VkBigDecimal = <VkBigDecimal>jahrhundertODERjahr;
			if (
				tag.intValue() !== 0 &&
				monat.intValue() !== 0 &&
				jahrhundert.intValue() !== 0 &&
				!jahrImJahrhundert.kannGroesserWerden &&
				!jahrImJahrhundert.kannKleinerWerden
			) {
				date = DateUtil.createDate(
					tag.intValue(),
					monat.intValue(),
					jahrhundert.intValue() * 100 + jahrImJahrhundert.intValue(),
					this.timeZone
				);
			}
			const dateParts: VkBigDecimal[] = [tag, monat, jahrhundert, jahrImJahrhundert];
			return this._constructDatumHelper(date, dateParts);
		}
	}

	constructTime(hour: VkBigDecimal, minute: VkBigDecimal, second: VkBigDecimal): VkDate {
		let date: Date | undefined;
		if (this._isRegular(hour) && this._isRegular(minute) && this._isRegular(second)) {
			date = DateUtil.createTime(
				hour.intValue(),
				minute.intValue(),
				second.intValue(),
				this.timeZone
			);
		}
		const dateParts: VkBigDecimal[] = [hour, minute, second];
		return this._constructDatumHelper(date, dateParts);
	}

	constructDateTime(date: VkDate, time: VkDate): VkDate {
		const result: Date | undefined = DateUtil.createDateTime(
			date.date,
			time.date,
			this.timeZone
		);
		const nichtAngegeben: boolean = date.isNichtAngegeben || time.isNichtAngegeben;
		const nichtPruefungsrelevant: boolean =
			date.isNichtPruefungsrelevant || time.isNichtPruefungsrelevant;
		if (nichtPruefungsrelevant) {
			return VkDate.NON_RELEVANT_DATE;
		}
		return new VkDate(result, nichtAngegeben, nichtPruefungsrelevant);
	}

	constructDateRange(index: IterationState, idStart: RtIdentifierTemplate, idEnd: RtIdentifierTemplate): VkDateRange {
		const startString: VkString = this.feldWert(index, idStart);
		const endString: VkString = this.feldWert(index, idEnd);
		const nichtAngegeben: boolean = startString.isNichtAngegeben || endString.isNichtAngegeben;
		const nichtPruefungsrelevant: boolean =
			startString.isNichtPruefungsrelevant || endString.isNichtPruefungsrelevant;
		const isEmpty: boolean = !startString.hasValue || !endString.hasValue;
		if (nichtPruefungsrelevant) {
			return VkDateRange.NON_RELEVANT_DATE_RANGE;
		}
		if (isEmpty) {
			return VkDateRange.NOT_GIVEN_DATE_RANGE;
		}
		const formatStart = (<FormatDefinitionDatum>this.metaData.getIFormatDefinition(idStart.getUniqueName())).format;
		const formatEnd = (<FormatDefinitionDatum>this.metaData.getIFormatDefinition(idEnd.getUniqueName())).format;

		const datesAndFormats: DateAndFormat[] = [
			new DateAndFormat(startString.wert, formatStart, this.timeZone),
			new DateAndFormat(endString.wert, formatEnd, this.timeZone)
		];

		const baseYear = this.metaData.getBaseYearOrDefault();
		return RuntimeController.datesAndFormatsToDateRange(datesAndFormats, baseYear, nichtAngegeben);
	}

	/**
	 * @return <code>true</code> if the given number was actually filled and is relevant for the
	 *         validation, otherwise <code>false</false>
	 */
	private _isRegular(num: VkBigDecimal): boolean {
		return !num.kannGroesserWerden && !num.kannKleinerWerden && !num.isNichtPruefungsrelevant;
	}

	private _constructDatumHelper(date: Date | undefined, dateParts: VkBigDecimal[]): VkDate {
		let nichtAngegeben = false;
		let nichtPruefungsrelevant = false;
		for (const d of dateParts) {
			/**
			 * Da ein Datum nur aus Feldern (nicht aus Rechenoperationen) konstruiert werden kann,
			 * bedeutet kann größer / kleiner werden hier, dass das Feld nicht angegeben war.
			 */
			if (d.kannGroesserWerden || d.kannKleinerWerden) {
				nichtAngegeben = true;
			}
			if (d.isNichtPruefungsrelevant) {
				nichtPruefungsrelevant = true;
			}
		}
		if (nichtPruefungsrelevant) {
			return VkDate.NON_RELEVANT_DATE;
		}
		return new VkDate(date, nichtAngegeben, nichtPruefungsrelevant);
	}

	/**
	 * Gibt das Startdatum aus einem Feld vom Typ Datumbereich als Date zurück.
	 * Falls das Feld nicht angegeben ist, wird undefined zurückgegeben.
	 */
	public getStartAusDatumBereich(id: RtInternalIdentifier): VkDate {
		return this._getDateRangeTeil(id, DateInterval.INTERVAL_START);
	}

	/**
	 * Gibt das Endedatum aus einem Feld vom Typ Datumbereich als Date zurück
	 * Falls das Feld nicht angegeben ist, wird undefined zurückgegeben.
	 */
	public getEndeAusDatumBereich(id: RtInternalIdentifier): VkDate {
		return this._getDateRangeTeil(id, DateInterval.INTERVAL_END);
	}

	private _getDateRangeTeil(id: RtInternalIdentifier, intervalSpec: DateInterval): VkDate {
		const dateRange: VkDateRange = this.feldWertAlsDateRange(id);
		if (dateRange.isNichtPruefungsrelevant || !dateRange.hasValue) {
			return new VkDate(undefined, dateRange.isNichtAngegeben, dateRange.isNichtPruefungsrelevant);
		}
		let date: Date | undefined;
		if (intervalSpec === DateInterval.INTERVAL_START) {
			date = dateRange.start;
		} else if (intervalSpec == DateInterval.INTERVAL_END) {
			date = dateRange.end
		} else {
			throw new Error(
				"unknown date interval spec" + intervalSpec
			);
		}
		return new VkDate(date, dateRange.isNichtAngegeben, false);
	}

	/**
	 * Gibt den Tag aus dem Datum als Zahl zurück.
	 * Ist das Datum undefined, so wird 0 zurückgegeben.
	 *
	 * Die Information über nicht angegebene / nicht prüfungsrelevante Felder wird im Rückgabewert
	 * gespeichert.
	 */
	public getTagAusDatum(datum: VkDate): VkBigDecimal {
		return BedingungsOperatorHelper.getDatumsTeil(datum, "day", this.timeZone);
	}

	/**
	 * Gibt den Monat aus dem Datum als Zahl zurück.
	 * Ist das Datum undefined, so wird 0 zurückgegeben.
	 *
	 * Die Information über nicht angegebene / nicht prüfungsrelevante Felder wird im Rückgabewert
	 * gespeichert.
	 */
	public getMonatAusDatum(datum: VkDate): VkBigDecimal {
		return BedingungsOperatorHelper.getDatumsTeil(datum, "month", this.timeZone);
	}

	/**
	 * Gibt den Jahr aus dem Datum als Zahl zurück.
	 * Ist das Datum undefined, so wird 0 zurückgegeben.
	 *
	 * Die Information über nicht angegebene / nicht prüfungsrelevante Felder wird im Rückgabewert
	 * gespeichert.
	 */
	public getJahrAusDatum(datum: VkDate): VkBigDecimal {
		return BedingungsOperatorHelper.getDatumsTeil(datum, "year", this.timeZone);
	}

	/**
	 * Gibt das Quartal aus dem Datum als Zahl zurück.
	 * Ist das Datum undefined, so wird 0 zurückgegeben.
	 *
	 * Die Information über nicht angegebene / nicht prüfungsrelevante Felder wird im Rückgabewert
	 * gespeichert.
	 */
	public getQuartalAusDatum(datum: VkDate): VkBigDecimal {
		const monatWert: VkBigDecimal = this.getMonatAusDatum(datum);
		const monat: number = monatWert.intValue();
		const quartal: number = monat === 0 ? 0 : Math.floor((monat - 1) / 3) + 1;

		return new VkBigDecimal(
			quartal,
			monatWert.isNichtPruefungsrelevant,
			monatWert.kannGroesserWerden,
			monatWert.kannKleinerWerden
		);
	}

	/**
	 * Gibt die Stunden aus dem Datum als Zahl zurück.
	 * Ist das Datum undefined, so wird 0 zurückgegeben.
	 *
	 * Die Information über nicht angegebene / nicht prüfungsrelevante Felder wird im Rückgabewert
	 * gespeichert.
	 */
	public getStundenAusZeit(datum: VkDate): VkBigDecimal {
		return BedingungsOperatorHelper.getDatumsTeil(datum, "hour", this.timeZone);
	}

	/**
	 * Gibt die Minuten aus dem Datum als Zahl zurück.
	 * Ist das Datum undefined, so wird 0 zurückgegeben.
	 *
	 * Die Information über nicht angegebene / nicht prüfungsrelevante Felder wird im Rückgabewert
	 * gespeichert.
	 */
	public getMinutenAusZeit(datum: VkDate): VkBigDecimal {
		return BedingungsOperatorHelper.getDatumsTeil(datum, "minute", this.timeZone);
	}

	/**
	 * Gibt die Sekunden aus dem Datum als Zahl zurück.
	 * Ist das Datum undefined, so wird 0 zurückgegeben.
	 *
	 * Die Information über nicht angegebene / nicht prüfungsrelevante Felder wird im Rückgabewert
	 * gespeichert.
	 */
	public getSekundenAusZeit(datum: VkDate): VkBigDecimal {
		return BedingungsOperatorHelper.getDatumsTeil(datum, "second", this.timeZone);
	}

	/**
	 * Gibt den Datumsanteil der DateTime-Angabe zurück => d.h. die Zeit wird auf 00:00:00 Uhr
	 * gesetzt.
	 *
	 * Die Information über nicht angegebene / nicht prüfungsrelevante Felder wird im Rückgabewert
	 * gespeichert.
	 */
	public getDatumAusDateTime(datum: VkDate): VkDate {
		if (datum.date == undefined) {
			return datum;
		}
		let date: TimeZonedDate = TimeZonedDate.createFromDate(datum.date, this.timeZone);
		date = date.clearTime();
		return new VkDate(date.toDate(), datum.isNichtAngegeben, datum.isNichtPruefungsrelevant);
	}

	/**
	 * Gibt den Zeitanteil der DateTime-Angabe zurück => d.h. das Datum wird auf den 01.01.1970
	 * gesetzt.
	 *
	 * Die Information über nicht angegebene / nicht prüfungsrelevante Felder wird im Rückgabewert
	 * gespeichert.
	 */
	public getZeitAusDateTime(datum: VkDate): VkDate {
		if (datum.date == undefined) {
			return datum;
		}
		const date: TimeZonedDate = TimeZonedDate.createFromDate(datum.date, this.timeZone);
		const time: Date | undefined = DateUtil.createTime(
			date.hour(),
			date.minute(),
			date.second(),
			this.timeZone
		);
		return new VkDate(time, datum.isNichtAngegeben, datum.isNichtPruefungsrelevant);
	}

	/**
	 * Gibt die Differenz zwischen den beiden Datumsangaben in Jahren zurück
	 * Ist eines der Datumsangaben undefined, so wird 0 zurückgegeben.
	 *
	 * Die Information über nicht angegebene / nicht prüfungsrelevante Felder wird im Rückgabewert
	 * gespeichert.
	 */
	public getDifferenzInJahren(datum1: VkDate, datum2: VkDate): VkBigDecimal {
		return BedingungsOperatorHelper.getDatumsDiff(datum1, datum2, "year", this.timeZone);
	}

	public getDifferenzInMonaten(datum1: VkDate, datum2: VkDate): VkBigDecimal {
		return BedingungsOperatorHelper.getDatumsDiff(datum1, datum2, "month", this.timeZone);
	}

	public getDifferenzInTagen(datum1: VkDate, datum2: VkDate): VkBigDecimal {
		return BedingungsOperatorHelper.getDatumsDiff(datum1, datum2, "day", this.timeZone);
	}

	public getDifferenzInStunden(datum1: VkDate, datum2: VkDate): VkBigDecimal {
		return BedingungsOperatorHelper.getDatumsDiff(datum1, datum2, "hour", this.timeZone);
	}

	public getDifferenzInMinuten(datum1: VkDate, datum2: VkDate): VkBigDecimal {
		return BedingungsOperatorHelper.getDatumsDiff(datum1, datum2, "minute", this.timeZone);
	}

	public getDifferenzInSekunden(datum1: VkDate, datum2: VkDate): VkBigDecimal {
		return BedingungsOperatorHelper.getDatumsDiff(datum1, datum2, "second", this.timeZone);
	}

	public addiereJahre(datum: VkDate, amount: VkBigDecimal): VkDate {
		return BedingungsOperatorHelper.addTime(datum, amount, "year", this.timeZone);
	}

	public addiereMonate(datum: VkDate, amount: VkBigDecimal): VkDate {
		return BedingungsOperatorHelper.addTime(datum, amount, "month", this.timeZone);
	}

	public addiereTage(datum: VkDate, amount: VkBigDecimal): VkDate {
		return BedingungsOperatorHelper.addTime(datum, amount, "day", this.timeZone);
	}

	public addiereStunden(datum: VkDate, amount: VkBigDecimal): VkDate {
		return BedingungsOperatorHelper.addTime(datum, amount, "hour", this.timeZone);
	}

	public addiereMinuten(datum: VkDate, amount: VkBigDecimal): VkDate {
		return BedingungsOperatorHelper.addTime(datum, amount, "minute", this.timeZone);
	}

	public addiereSekunden(datum: VkDate, amount: VkBigDecimal): VkDate {
		return BedingungsOperatorHelper.addTime(datum, amount, "second", this.timeZone);
	}

	/**
	 * Vorbedingung:
	 * Alle Felder in entityList sind vom Typ DatumBereich mit Zusatzprüfung. <br>
	 * Liefert TRUE_WF oder TRUE_AF, wenn zwei prüfungsrelevante Felder der FeldListe sich als
	 * Datumsbereiche überlappen, FALSE sonst.<br>
	 * TRUE_WF, wenn keiner der Datumsbereiche eine Filterbedingung hat, TRUE_AF sonst.<br>
	 * Start- und Enddatum der Datumsbereiche werden dabei mitberücksichtigt.
	 */
	public datumsBereicheUeberlappend(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		const datumBereiche: DatumBereich[] = [];
		let containsFilter = false;
		for (const rtIdContainer of entityList) {
			const fIt: EntityIterator = new EntityIterator(rtIdContainer, index);
			this._setzeEndIndicesAufMaxGesetzt(fIt, false);
			for (const id of fIt) {
				if (this.isWertAngegeben(id, false) !== DreiWertBool.TRUE) {
					continue;
				}
				const datBerAkt: DatumBereich = new DatumBereich(
					// following two non-null assertions are ok since we throw an error if they are null or undefined
					// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
					this.getStartAusDatumBereich(id).date!,
					// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
					this.getEndeAusDatumBereich(id).date!
				);
				if (rtIdContainer.getFilter()) {
					containsFilter = true;
				}
				if (datBerAkt.ueberlapptMitEinemAusMenge(datumBereiche)) {
					return containsFilter ? ValidierungsErgebnis.TRUE_AF : ValidierungsErgebnis.TRUE_WF;
				}
				datumBereiche.push(datBerAkt);
			}
		}
		return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
	}

	/**
	 * Setzt im EntityIterator die Endzeile auf 1 und die LfdNr auf den maximalen Wert, für den ein
	 * Feld angegeben wurde, wenn dieser Wert zw. Start- und Endindex des EntityIterators liegt. Ist
	 * der Wert kleiner als der Startindex, wird Endindex gleich Startindex gesetzt. Sonst bleibt
	 * der Iterator unverändert.<br>
	 * Hat der boolesche Parameter den Wert <code>true</code>, wird die Änderung jeweils nur dann
	 * durchgeführt, wenn die Teilvalidierungsart für den betreffenden Index "gleich" ist, d.h.
	 * wenn alle prüfungsrelevanten Felder für alle Werte für diesen Index prüfungsrelevant sind.
	 *
	 * @return true, falls mindestens ein Endeindex geändert wurde, false sonst
	 */
	private _setzeEndIndicesAufMaxGesetzt(
		fIt: EntityIterator,
		setzeNurWennTVArtGleich: boolean
	): boolean {
		let indexGesetzt = false;
		if (setzeNurWennTVArtGleich) {
			if (
				fIt.setzeAlleEbenenMitZeileAufMaxGesetztIfPossible(
					this.validationCache,
					this.validierungArt,
					0
				)
			) {
				indexGesetzt = true;
			}
		} else {
			const anzWiederholungsEbenen: number = fIt.anzahlWiederholungsEbenen;
			const maxEbene: number = fIt.isField() ? anzWiederholungsEbenen - 1 : anzWiederholungsEbenen;
			for (let i = 1; i < maxEbene; i++) {
				if (
					fIt.setEndeIndexFallsKleinerAktuellesEnde(
						i,
						this.validationCache.getMaxGesetzteWiederholungOhneZeile(i)
					)
				) {
					indexGesetzt = true;
				}
			}
			if (fIt.isField() && fIt.setEndeZeileAufEins()) {
				indexGesetzt = true;
			}
		}
		return indexGesetzt;
	}

	/**
	 * Gibt TRUE_WF zurück,
	 * wenn wert nicht dem Muster regexp entspricht und op==NM ('NichtWieMuster', englisch 'PatternViolated') oder
	 * wenn wert dem Muster regexp entspricht und op==WM ('WieMuster', englisch 'PatternMatched')
	 * sonst FALSE
	 */
	public vergleichePatternSTRING(op: VglOp, str: VkString, regexp: string): ValidierungsErgebnis {
		const wert: string = str.wert;
		if (!str.hasValue) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}

		// op kann nur NM oder WM sein, die Bedingung entspricht einem XNOR
		return BedingungsOperatorHelper.getPattern(regexp).test(wert) !== (op === VglOp.NM)
			? ValidierungsErgebnis.TRUE_WF
			: ValidierungsErgebnis.FALSE_OR_UNKNOWN;
	}

	public getLogger(): ValidationLogger {
		return this.logger;
	}

	/**
	 * Hilfsmethode, erzeugt einen boolean-Array für alle Wiederholungsebenen und initialisiert
	 * diesen mit true.
	 */
	public createArrayFuerEbenenGleich(): boolean[] {
		let anzEbenen = 2;
		for (const id of this.validationCache.getAllRelevantEntitiesIntern()) {
			const isField: boolean = this.metaData.isUniqueFieldName(id.getName());
			// The rows are represented separately on each layer, so for groups we need to add an additional row
			const numIndicesNecessary: number = isField
				? id.getIndices().length
				: id.getIndices().length + 1;
			anzEbenen = Math.max(anzEbenen, numIndicesNecessary);
		}

		return Array.from({ length: anzEbenen }, () => true);
	}

	/**
	 * Erzeuge einen Identifier aus der Stringrepräsentation eines Feldes.
	 * Variablen in der Stringrepräsentation werden durch den konkreten Index ersetzt.
	 * Vorbedingung: Keine Stern-Operation
	 * Es wird durch die Grammatik und durch die Codegenerierung garantiert, dass
	 * makeIdentifier nie für eine string-Repräsentation aufgerufen wird, die
	 * eine Stern-Spezifikation enthält.
	 */
	public static makeIdentifier(
		indices: IterationState,
		entity: RtIdentifierTemplate
	): RtInternalIdentifier {
		return RtInternalIdentifier.createFromTemplate(entity, indices);
	}

	public getHeute(): VkDate {
		let today: TimeZonedDate = TimeZonedDate.createFromDate(this.validationTime, this.timeZone);
		today = today.clearTime();
		return new VkDate(today.toDate());
	}

	public getJetzt(): VkDate {
		return new VkDate(this.validationTime);
	}

	public getReferenzjahr(): VkDate {
		return new VkDate(this.metaData.getBaseYearAsDate());
	}

	/**
	 * Die ersten zwei Parameter beschreiben die Feldliste. Deren Werte werden überprüft, ob diese
	 * in der WerteListe (repräsentiert durch eine FeldListe) vorhanden sind.
	 * Es wird zunächst die WerteListe berechnet und dann die Methode verwendet, die
	 * die FeldListe zu einer WerteListe überprüft.
	 */
	public nichtAlleFeldWerteInWerteListeFL(
		index: IterationState,
		entityList: RtIdentifierTemplate[],
		valueList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		// if no entity is filled, NotAllFieldsInValueList can not evaluate to true
		if (this._noEntityFilled(index, entityList)) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		const values: Set<string> = new Set<string>();
		const einWertDerWerteListeNichtAngegeben: boolean[] = [false];
		if (
			!this.fuegeFeldWerteZuWerteListe(index, valueList, values, einWertDerWerteListeNichtAngegeben)
		) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		const foundNonFilledField: boolean = einWertDerWerteListeNichtAngegeben[0];
		const isNumber: boolean = this.isZahlFeld(entityList[0].getUniqueName());
		const werteMenge: any[] = this.convertStringArrayToMenge(Array.from(values), isNumber);
		return this._nichtAlleFeldWerteInWerteListeOLIntern(
			index,
			entityList,
			werteMenge,
			foundNonFilledField,
			isNumber
		);
	}

	/**
	 * Die ersten zwei Parameter beschreiben die Feldliste. Deren Werte werden überprüft, ob diese
	 * in der Werteliste vorhanden sind.
	 */
	public nichtAlleFeldWerteInWerteListeOL(
		index: IterationState,
		entityList: RtIdentifierTemplate[],
		values: any[],
		foundNonFilledField: boolean,
		foundNonRelevantFieldInValueList: boolean,
		isNumber: boolean
	): ValidierungsErgebnis {
		if (foundNonRelevantFieldInValueList) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		return this._nichtAlleFeldWerteInWerteListeOLIntern(
			index,
			entityList,
			values,
			foundNonFilledField,
			isNumber
		);
	}

	private _nichtAlleFeldWerteInWerteListeOLIntern(
		index: IterationState,
		entityList: RtIdentifierTemplate[],
		values: any[],
		foundNonFilledField: boolean,
		isNumber: boolean
	): ValidierungsErgebnis {
		const nichtAlleFeldWerteInWerteListe: ValidierungsErgebnis =
			this._nichtAlleFeldWerteInWerteListeIntern(index, entityList, values, isNumber);
		if (foundNonFilledField && nichtAlleFeldWerteInWerteListe.isKnownAndTrue()) {
			return ValidierungsErgebnis.TRUE_AF;
		}
		return nichtAlleFeldWerteInWerteListe;
	}

	/**
	 * Der erste Parameter ist immer eine FeldListe.
	 * Dessen Werte werden überprüft, ob diese in der übergebenen Werteliste vorhanden
	 * sind. Wird ein Feld gefunden, dessen wert nicht in der Werteliste vorhanden ist,
	 * wird TRUE_WF zurückgegeben, FALSE sonst.
	 * Je nach Feldtyp wird auf Strings oder auf Zahlen verglichen.
	 */
	public nichtAlleFeldWerteInWerteListeSL(
		index: IterationState,
		entityList: RtIdentifierTemplate[],
		...werteListeAlsStringListe: string[]
	): ValidierungsErgebnis {
		const isNumber: boolean = this.isZahlFeld(entityList[0].getUniqueName());
		const values: any[] = this.convertStringArrayToMenge(werteListeAlsStringListe, isNumber);

		return this._nichtAlleFeldWerteInWerteListeIntern(index, entityList, values, isNumber);
	}

	private _nichtAlleFeldWerteInWerteListeIntern(
		index: IterationState,
		entityList: RtIdentifierTemplate[],
		values: any[],
		isNumber: boolean
	): ValidierungsErgebnis {
		for (const rtIdContainer of entityList) {
			if (rtIdContainer.isAggregation()) {
				const fIt: EntityIterator = new EntityIterator(rtIdContainer, index);
				// es werden nur gesetzte Felder betrachet,  daher muss die
				// Optimierung keine Besonderheiten berücksichtigen.
				this._setzeEndIndicesAufMaxGesetzt(fIt, false);
				for (const id of fIt) {
					if (this._isValueNotContainedInArray(id, values, isNumber)) {
						if (!rtIdContainer.getFilter()) {
							return ValidierungsErgebnis.TRUE_WF;
						} else {
							return ValidierungsErgebnis.TRUE_AF;
						}
					}
				}
			} else {
				const rtId = RtInternalIdentifier.createFromTemplate(rtIdContainer, index);
				if (this._isValueNotContainedInArray(rtId, values, isNumber)) {
					return ValidierungsErgebnis.TRUE_WF;
				}
			}
		}
		return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
	}

	private _noEntityFilled(index: IterationState, entityList: RtIdentifierTemplate[]): boolean {
		for (const rtIdContainer of entityList) {
			if (rtIdContainer.isAggregation()) {
				const fIt: EntityIterator = new EntityIterator(rtIdContainer, index);
				// es werden nur gesetzte Felder betrachet,  daher muss die
				// Optimierung keine Besonderheiten berücksichtigen.
				this._setzeEndIndicesAufMaxGesetzt(fIt, false);
				for (const id of fIt) {
					if (this.isWertAngegeben(id, false) === DreiWertBool.TRUE) {
						return false;
					}
				}
			} else {
				const rtId: RtInternalIdentifier = RtInternalIdentifier.createFromTemplate(
					rtIdContainer,
					index
				);
				if (this.isWertAngegeben(rtId, false) === DreiWertBool.TRUE) {
					return false;
				}
			}
		}
		return true;
	}

	private _isValueNotContainedInArray(
		id: RtInternalIdentifier,
		valueArray: any[],
		isNumber: boolean
	): boolean {
		if (this.isWertAngegeben(id, false) === DreiWertBool.TRUE) {
			const value = this._getValue(isNumber, id);
			if (!valueArray.some(v => this._isEqual(value, v))) {
				return true;
			}
		}
		return false;
	}

	public convertStringArrayToMenge(werteListeAlsStringListe: string[], isZahl: boolean): any[] {
		const result: any[] = [];
		if (isZahl) {
			for (const wert of werteListeAlsStringListe) {
				result.push(BigDecimalUtils.makeDecimal(wert));
			}
		} else {
			for (const wert of werteListeAlsStringListe) {
				result.push(wert);
			}
		}
		return result;
	}

	/**
	 * Füge die Werte der feldListe in die werte ein.
	 * Sind nicht alle Feldwerte angegeben oder hat eines der Felder in valueList eine
	 * Filterbedingung, setze einWertDerWerteListeNichtAngegeben auf true.
	 * Wird ein nicht prüfungsrelevantes Feld gefunden, gebe false zurück, true sonst.
	 */
	public fuegeFeldWerteZuWerteListe(
		index: IterationState,
		valueList: RtIdentifierTemplate[],
		werte: Set<string>,
		einWertDerWerteListeNichtAngegeben: boolean[]
	): boolean {
		einWertDerWerteListeNichtAngegeben[0] = false;
		for (const rtIdContainer of valueList) {
			if (rtIdContainer.getFilter()) {
				einWertDerWerteListeNichtAngegeben[0] = true;
			}
			const fIt: EntityIterator = new EntityIterator(rtIdContainer, index);

			let atLeastOneWasSet = false;
			const anzWiederholungsEbenen: number = fIt.isField()
				? fIt.anzahlWiederholungsEbenen - 1
				: fIt.anzahlWiederholungsEbenen;
			for (let i = 0; i < anzWiederholungsEbenen; ++i) {
				if (this.validierungArt.istEbeneGleich(i)) {
					atLeastOneWasSet =
						atLeastOneWasSet ||
						fIt.setEndeIndexFallsKleinerAktuellesEnde(
							i,
							this.validationCache.getMaxGesetzteWiederholungOhneZeile(i)
						);
				}
			}

			if (atLeastOneWasSet) {
				// nicht für jede Wiederholung ist was angegeben -> merken wir es uns für Auslassungsfehler
				einWertDerWerteListeNichtAngegeben[0] = true;
			}
			for (const id of fIt) {
				const wertAngegeben: DreiWertBool = this.isWertAngegeben(id, false);
				switch (wertAngegeben) {
					case DreiWertBool.TRUE: {
						const value: string = this._feldWertIntern(id);
						werte.add(value);
						break;
					}
					case DreiWertBool.FALSE:
						einWertDerWerteListeNichtAngegeben[0] = true;
						break;
					case DreiWertBool.UNKNOWN:
						return false;
					default:
						break;
				}
			}
		}
		return true;
	}

	/**
	 * Gibt alle Werte prüfungsrelevanter Felder der Feldliste zurück.
	 *
	 * @return <code>true</code>, wenn die Werteliste ein Feld mit Filterbedingung enthält,
	 *         <code>false</code> sonst.
	 */
	public fuegePruefungsrelevanteFeldWerteZuWerteListe(
		index: IterationState,
		valueList: RtIdentifierTemplate[],
		werte: Set<string>
	): boolean {
		let containsFilter = false;
		for (const rtIdContainer of valueList) {
			const fIt: EntityIterator = new EntityIterator(rtIdContainer, index);
			// Optimierung auch wenn Teilvalidierung für konkrete lfdNummer möglich
			// da nicht prüfungsrelevante Felder genauso behandelt werden wie nicht angegebene
			// Felder
			this._setzeEndIndicesAufMaxGesetzt(fIt, false);
			fIt.optimiereMitGesetztenWiederholungenWennSinnvoll(this.validationCache);
			for (const id of fIt) {
				if (this.isWertAngegeben(id, false) === DreiWertBool.TRUE) {
					const value: string = this._feldWertIntern(id);
					werte.add(value);
					if (rtIdContainer.getFilter()) {
						containsFilter = true;
					}
				}
			}
		}
		return containsFilter;
	}

	/**
	 * Implementiert das Sprachkonstrukt KeinFeldWertInWerteListe, wenn die Werteliste auch durch
	 * eine Feldliste gegeben ist.
	 * Gibt TRUE_WF/ TRUE_AF zurück, wenn alle Felder in beiden Feldlisten prüfungsrelevant sind
	 * und für kein Feld aus valueList
	 * der Wert in den Werten der Felder aus entityList vorkommt. Das ist insbesondere
	 * der Fall, wenn
	 * gar kein Feld in valueList oder in entityList angegeben wurde.
	 */
	public keinFeldWertInWerteListeFL(
		index: IterationState,
		entityList: RtIdentifierTemplate[],
		valueList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		const werte: Set<string> = new Set<string>();
		const einWertDerWerteListeNichtAngegeben: boolean[] = [true];
		if (
			!this.fuegeFeldWerteZuWerteListe(index, valueList, werte, einWertDerWerteListeNichtAngegeben)
		) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		const isNumber: boolean = this.isZahlFeld(entityList[0].getUniqueName());
		const values: any[] = this.convertStringArrayToMenge(Array.from(werte), isNumber);
		const foundNonFilledFieldInValueList: boolean = einWertDerWerteListeNichtAngegeben[0];
		return this._keinFeldWertInWerteListeOLIntern(
			index,
			entityList,
			values,
			foundNonFilledFieldInValueList,
			isNumber
		);
	}

	/**
	 * Implementiert das Sprachkonstrukt KeinFeldWertInWerteListe, wenn die Werteliste auch durch
	 * eine Feldliste gegeben ist und die dadurch gegebenen Werte bereits ermittelt wurden.
	 * Gibt TRUE_WF/ TRUE_AF zurück, wenn alle Felder in beiden Feldlisten prüfungsrelevant sind
	 * und für kein Feld aus feldListe der Wert in den Werten in <code>values</code> vorkommt. Das
	 * ist insbesondere der Fall, wenn gar kein Feld in feldListe angegeben wurde.
	 */
	public keinFeldWertInWerteListeOL(
		index: IterationState,
		entityList: RtIdentifierTemplate[],
		values: any[],
		foundNonFilledFieldInValueList: boolean,
		foundNonRelevantFieldInValueList: boolean,
		isNumber: boolean
	): ValidierungsErgebnis {
		if (foundNonRelevantFieldInValueList) {
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		return this._keinFeldWertInWerteListeOLIntern(
			index,
			entityList,
			values,
			foundNonFilledFieldInValueList,
			isNumber
		);
	}

	private _keinFeldWertInWerteListeOLIntern(
		index: IterationState,
		entityList: RtIdentifierTemplate[],
		values: any[],
		foundNonFilledFieldInValueList: boolean,
		isNumber: boolean
	): ValidierungsErgebnis {
		const keinFeldWerteInWerteListe: ValidierungsErgebnis = this._keinFeldWertInWerteListeIntern(
			index,
			entityList,
			values,
			isNumber
		);
		if (foundNonFilledFieldInValueList && keinFeldWerteInWerteListe.isKnownAndTrue()) {
			return ValidierungsErgebnis.TRUE_AF;
		}
		return keinFeldWerteInWerteListe;
	}

	/**
	 * Implementiert das Sprachkonstrukt KeinFeldWertInWerteListe, wenn die Werteliste durch eine
	 * Liste von Stringkonstanten gegeben ist.
	 * Gibt TRUE_WF / TRUE_AF zurück, wenn alle Felder in feldListe prüfungsrelevant sind und für
	 * kein Feld aus feldListe der Wert in der Liste der Stringkonstanten vorkommt. Das ist
	 * insbesondere der Fall, wenn gar kein Feld in feldListe angegeben wurde.
	 */
	public keinFeldWertInWerteListeSL(
		index: IterationState,
		entityList: RtIdentifierTemplate[],
		...werteListeAlsStringListe: string[]
	): ValidierungsErgebnis {
		const isNumber: boolean = this.isZahlFeld(entityList[0].getUniqueName());
		const values: any[] = this.convertStringArrayToMenge(werteListeAlsStringListe, isNumber);

		return this._keinFeldWertInWerteListeIntern(index, entityList, values, isNumber);
	}

	private _keinFeldWertInWerteListeIntern(
		index: IterationState,
		entityList: RtIdentifierTemplate[],
		values: any[],
		isNumber: boolean
	): ValidierungsErgebnis {
		let isAuslassungsFehler = false;

		for (const rtIdContainer of entityList) {
			const fIt: EntityIterator = new EntityIterator(rtIdContainer, index);

			let atLeastOneWasSet = false;
			const anzWiederholungsEbenen: number = fIt.isField()
				? fIt.anzahlWiederholungsEbenen - 1
				: fIt.anzahlWiederholungsEbenen;
			for (let i = 0; i < anzWiederholungsEbenen; ++i) {
				if (this.validierungArt.istEbeneGleich(i)) {
					// Schleife nur über die Wiederholungen, für die Werte vorhanden sind
					atLeastOneWasSet =
						atLeastOneWasSet ||
						fIt.setEndeIndexFallsKleinerAktuellesEnde(
							i,
							this.validationCache.getMaxGesetzteWiederholungOhneZeile(i)
						);
				}
			}

			if (atLeastOneWasSet || rtIdContainer.getFilter()) {
				// nicht für jede Wiederholung ist was angegeben -> merken wir es uns für Auslassungsfehler
				isAuslassungsFehler = true;
			}
			for (const id of fIt) {
				const wertAngegeben: DreiWertBool = this.isWertAngegeben(id, false);
				switch (wertAngegeben) {
					case DreiWertBool.TRUE: {
						// Feld ist angegeben und prüfungsrelevant => falls in der Werteliste, ist Prüfung zuende
						const value = this._getValue(isNumber, id);
						if (values.some(v => this._isEqual(value, v))) {
							return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
						}
						break;
					}
					case DreiWertBool.FALSE:
						isAuslassungsFehler = true;
						break;
					case DreiWertBool.UNKNOWN:
						// nicht prüfungsrelevantes Feld in der Feldliste -> Prüfung ist zuende
						return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
					default:
						break;
				}
			}
		}
		return isAuslassungsFehler ? ValidierungsErgebnis.TRUE_AF : ValidierungsErgebnis.TRUE_WF;
	}

	/**
	 * Implements the predicate AtLeastOneDateRangeOverlaps
	 * (MindestensEinDatumsBereichUeberlappend).
	 * Returns
	 * <ul>
	 * <li>TRUE_WF, if the date range of the field contained in <code>singleEntityList</code> overlaps
	 * with at least one date range from the fields contained in <code>entityList</code> and this field
	 * does <b>not</b> define a filter condition.</li>
	 * <li>TRUE_AF, if the date range of the field contained in <code>singleEntityList</code> overlaps
	 * with at least one date range from the fields contained in <code>entityList</code> and this field
	 * defines a filter condition.</li>
	 * <li>Otherwise FALSE.</li>
	 * </ul>
	 * The parser ensures that the first field-parameter is a single field without '*'-operator.
	 */
	public mindestensEinDatumsBereichUeberlappendFL(
		// keep arguments to have the same signature as in java

		_index: IterationState,

		_singleEntityList: RtIdentifierTemplate[],

		_entityList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		if (_singleEntityList.length !== 1) {
			throw new Error(
				"error during code generation: singleEntityInList should contains exactly one element"
			);
		}
		const feldIt = new EntityIterator(_singleEntityList[0], _index);
		if (feldIt.anzahlFelder() !== 1) {
			throw new Error("error during code generation: Number of fields in first param != 1");
		}
		const feldIdentifier = feldIt.get();
		if (this.isWertAngegeben(feldIdentifier, false) !== DreiWertBool.TRUE) {
			// id is never a group, parameter irrelevant
			// trivialer Fall
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		const datBerFeld = new DatumBereich(
			// following two non-null assertions are ok since we throw an error if they are null or undefined
			// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
			this.getStartAusDatumBereich(feldIdentifier).date!,
			// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
			this.getEndeAusDatumBereich(feldIdentifier).date!
		);

		for (const rtIdContainer of _entityList) {
			const fIt = new EntityIterator(rtIdContainer, _index);
			//Optimierung auch wenn Teilvalidierung für konkrete lfdNummer möglich
			//da nicht prüfungsrelevante Felder genauso behandelt werden wie nicht angegebene
			//Felder
			this._setzeEndIndicesAufMaxGesetzt(fIt, false);
			fIt.optimiereMitGesetztenWiederholungenWennSinnvoll(this.validationCache);

			for (const id of fIt) {
				if (this.isWertAngegeben(id, false) !== DreiWertBool.TRUE) {
					// id is never a group, parameter irrelevant
					continue;
				}
				const datBerAkt = new DatumBereich(
					// following two non-null assertions are ok since we throw an error if they are null or undefined
					// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
					this.getStartAusDatumBereich(id).date!,
					// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
					this.getEndeAusDatumBereich(id).date!
				);
				if (datBerFeld.ueberlappt(datBerAkt)) {
					return rtIdContainer.getFilter() != null
						? ValidierungsErgebnis.TRUE_AF
						: ValidierungsErgebnis.TRUE_WF;
				}
			}
		}
		return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
	}

	/**
	 * Implementiert das Sprachkonstrukt MindestensEinFeldWertInWerteListe, wenn die Werteliste auch
	 * durch eine Feldliste gegeben ist.<br>
	 * Wenn für mindestens ein prüfungsrelevantes Feld der ersten Feldliste der Wert in den Werten
	 * der prüfungsrelevanten Felder aus werteListeAlsFeldIdListe vorkommt, wird TRUE_WF oder
	 * TRUE_AF zurückgegeben: TRUE_WF, wenn keines der betroffenen Felder eine Filterbedingung hat,
	 * TRUE_AF sonst.
	 */
	public mindestensEinFeldWertInWerteListeFL(
		index: IterationState,
		entityList: RtIdentifierTemplate[],
		valueList: RtIdentifierTemplate[]
	): ValidierungsErgebnis {
		const werte = new Set<string>();
		const containsValueListFilter: boolean = this.fuegePruefungsrelevanteFeldWerteZuWerteListe(
			index,
			valueList,
			werte
		);
		const isNumber: boolean = this.isZahlFeld(entityList[0].getUniqueName());
		const valuesAsObjectList: any[] = this.convertStringArrayToMenge(Array.from(werte), isNumber);
		const resultIntern: ValidierungsErgebnis = this._mindestensEinFeldWertInWerteListeIntern(
			index,
			entityList,
			valuesAsObjectList,
			isNumber
		);
		if (containsValueListFilter && resultIntern.isKnownAndTrue()) {
			return ValidierungsErgebnis.TRUE_AF;
		}
		return resultIntern;
	}

	/**
	 * Implementiert das Sprachkonstrukt MindestensEinFeldWertInWerteListe, wenn die Werteliste auch
	 * durch eine Feldliste gegeben ist und wenn die Werteliste bereits berechnet wurde.<br>
	 * Wenn für mindestens ein prüfungsrelevantes Feld in entityList der Wert in der Werteliste
	 * vorkommt, wird TRUE_WF oder TRUE_AF zurückgegeben: TRUE_WF, wenn keines der betroffenen
	 * Felder eine Filterbedingung hat, TRUE_AF sonst.
	 *
	 */
	public mindestensEinFeldWertInWerteListeOL(
		idx: IterationState,
		entityList: RtIdentifierTemplate[],
		values: any[],
		containsValueListFilter: boolean,
		isNumber: boolean
	): ValidierungsErgebnis {
		const resultIntern: ValidierungsErgebnis = this._mindestensEinFeldWertInWerteListeIntern(
			idx,
			entityList,
			values,
			isNumber
		);
		if (containsValueListFilter && resultIntern.isKnownAndTrue()) {
			return ValidierungsErgebnis.TRUE_AF;
		}
		return resultIntern;
	}

	/**
	 * Implementiert das Sprachkonstrukt MindestensEinFeldWertInWerteListe, wenn die Werteliste
	 * durch eine Liste von Stringkonstanten gegeben ist.
	 * Gibt TRUE_WF zurück, wenn für mindestens ein prüfungsrelevantes Feld in entityList der Wert in
	 * der Liste der Stringkonstanten vorkommt.
	 */
	public mindestensEinFeldWertInWerteListeSL(
		index: IterationState,
		entityList: RtIdentifierTemplate[],
		...werteListeAlsStringListe: string[]
	): ValidierungsErgebnis {
		const isNumber = this.isZahlFeld(entityList[0].getUniqueName());
		const values = this.convertStringArrayToMenge(werteListeAlsStringListe, isNumber);

		return this._mindestensEinFeldWertInWerteListeIntern(index, entityList, values, isNumber);
	}

	private _mindestensEinFeldWertInWerteListeIntern(
		index: IterationState,
		entityList: RtIdentifierTemplate[],
		values: any[],
		isNumber: boolean
	): ValidierungsErgebnis {
		if (values.length <= 0) {
			// trivialer Fall
			return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
		}
		for (const rtIdContainer of entityList) {
			if (rtIdContainer.isAggregation()) {
				const fIt: EntityIterator = new EntityIterator(rtIdContainer, index);
				// Optimierung auch wenn Teilvalidierung für konkrete lfdNummer möglich
				// da nicht prüfungsrelevante Felder genauso behandelt werden wie nicht angegebene
				// Felder
				this._setzeEndIndicesAufMaxGesetzt(fIt, false);
				fIt.optimiereMitGesetztenWiederholungenWennSinnvoll(this.validationCache);
				for (const id of fIt) {
					if (this._isValueContainedInArray(id, values, isNumber)) {
						return rtIdContainer.getFilter()
							? ValidierungsErgebnis.TRUE_AF
							: ValidierungsErgebnis.TRUE_WF;
					}
				}
			} else {
				const rtId = RtInternalIdentifier.createFromTemplate(rtIdContainer, index);
				if (this._isValueContainedInArray(rtId, values, isNumber)) {
					return ValidierungsErgebnis.TRUE_WF;
				}
			}
		}
		return ValidierungsErgebnis.FALSE_OR_UNKNOWN;
	}

	private _isValueContainedInArray(
		id: RtInternalIdentifier,
		valueArray: any[],
		isNumber: boolean
	): boolean {
		if (this.isWertAngegeben(id, false) === DreiWertBool.TRUE) {
			// Feld ist angegeben und prüfungsrelevant => falls in der Werteliste, ist Prüfung zuende
			const value = this._getValue(isNumber, id);
			if (valueArray.some(v => this._isEqual(value, v))) {
				return true;
			}
		}
		return false;
	}

	public isZahlFeld(entityName: string): boolean {
		return MetaDataUtilValidierung.isNumberFeld(entityName, this.metaData);
	}

	/**
	 * Gibt die Anzahl der Felder in der Feldliste zurück, deren Wert der 'Zahl' entsprechen.
	 * Ist ein Feld nicht prüfungsrelevant, wird 0 zurückgegeben.
	 * Die Information über nicht prüfungsrelevante Felder wird gespeichert.
	 */
	public anzahlWertInFeldlisteZahl(
		zahl: VkBigDecimal,
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): VkBigDecimal {
		return this.anzahlWertInFeldlisteZahlIntern(zahl, index, entityList);
	}

	/**
	 * Gibt die Anzahl der Felder in der Feldliste zurück, die dem 'stringWert' entsprechen.
	 * Ist ein Feld nicht prüfungsrelevant, wird 0 zurückgegeben.
	 * Die Information über nicht prüfungsrelevante Felder wird gespeichert.
	 */
	public anzahlWertInFeldlisteString(
		stringWert: string,
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): VkBigDecimal {
		return this.anzahlWertInFeldlisteStringIntern(stringWert, index, entityList);
	}

	public anzahlWertInFeldlisteZahlIntern(
		zahl: VkBigDecimal,
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): VkBigDecimal {
		return this._anzahlWertInFeldlisteIntern(zahl, index, entityList, true);
	}

	public anzahlWertInFeldlisteStringIntern(
		stringWert: string,
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): VkBigDecimal {
		return this._anzahlWertInFeldlisteIntern(stringWert, index, entityList, false);
	}

	private _anzahlWertInFeldlisteIntern(
		wert: any,
		index: IterationState,
		entityList: RtIdentifierTemplate[],
		isNumber: boolean
	): VkBigDecimal {
		let d = 0;
		let kannGroesserWerden = false;
		let containsFilteredValue = false;
		for (const rtIdContainer of entityList) {
			const fIt: EntityIterator = new EntityIterator(rtIdContainer, index);
			if (this._setzeEndIndicesAufMaxGesetzt(fIt, true)) {
				kannGroesserWerden = true;
			}
			if (rtIdContainer.getFilter()) {
				kannGroesserWerden = true;
			}
			for (const id of fIt) {
				const b: DreiWertBool = this.isWertAngegeben(id, false);
				if (b === DreiWertBool.UNKNOWN) {
					return VkBigDecimal.NICHT_PRUEF_REL_ZAHL;
				}
				if (b === DreiWertBool.FALSE) {
					kannGroesserWerden = true;
				}
				if (b === DreiWertBool.TRUE) {
					const value = this._getValue(isNumber, id);
					if (this._isEqual(value, wert)) {
						if (rtIdContainer.getFilter()) {
							containsFilteredValue = true;
						}
						d++;
					}
				}
			}
		}
		if (containsFilteredValue) {
			return new VkBigDecimal(d, false, true, true);
		}
		return new VkBigDecimal(d, false, kannGroesserWerden, false);
	}

	private _getValue(isNumber: boolean, id: RtInternalIdentifier): string | VkBigDecimal {
		if (isNumber) {
			return this.feldWertAlsZahl(id);
		} else {
			return this._feldWertIntern(id);
		}
	}

	public getMaxGesetzterKontext(kontextIndex: number): number {
		return this.validationCache.getMaxGesetzterKontext(kontextIndex);
	}

	public startMesspunkt(): number {
		return this.logger.startMesspunkt();
	}

	public logMessung(
		berechnungsAufrufNameOrRegelName: string,
		startMesspunktOrL: number,
		startMesspunkt?: number
	): void {
		if (startMesspunkt == undefined) {
			this.logger.logMessung(berechnungsAufrufNameOrRegelName, startMesspunktOrL);
		} else {
			this.logger.logMessung(berechnungsAufrufNameOrRegelName, startMesspunktOrL, startMesspunkt);
		}
	}

	/*
	 * ACHTUNG: Die folgenden Methoden müssen in dieser Klasse bleiben, obwohl
	 * sie meist den Aufruf nur an BigDecimalUtils delegieren, denn diese Methoden
	 * werden aus den generierten Validator-Klassen gerufen.
	 * Falls man diese Methoden hier entfernen wollte würden die Templates, aus denen
	 * die Validator-Klassen generiert werden, komplexer.
	 *
	 * Ist ein Feld nicht angegeben oder nicht prüfungrelevant, so gibt feldWert(id)
	 * immer einen leeren String zurück.
	 */
	public abrundenWert(id: RtInternalIdentifier, anzStellen: number): VkBigDecimal {
		return BigDecimalUtils.abrunden(this.feldWertAlsZahl(id), anzStellen);
	}

	public aufrundenWert(id: RtInternalIdentifier, anzStellen: number): VkBigDecimal {
		return BigDecimalUtils.aufrunden(this.feldWertAlsZahl(id), anzStellen);
	}

	public rundenWert(id: RtInternalIdentifier, anzStellen: number): VkBigDecimal {
		return BigDecimalUtils.runden(this.feldWertAlsZahl(id), anzStellen);
	}

	/**
	 * This method is also used in the interpreter
	 */
	public laenge(id: RtInternalIdentifier): VkBigDecimal;
	/**
	 * This is the method which is used in the generated code
	 */
	public laenge(index: IterationState, entity: RtIdentifierTemplate): VkBigDecimal;
	public laenge(
		idOrIndex: RtInternalIdentifier | IterationState,
		entity?: RtIdentifierTemplate
	): VkBigDecimal {
		let _id: RtInternalIdentifier;
		if (idOrIndex instanceof RtInternalIdentifier) {
			_id = idOrIndex;
		} else {
			// no categories allowed
			// cannot be undefined because the method with both parameter set is called
			// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
			_id = RuntimeController.makeIdentifier(idOrIndex, entity!);
		}
		return BigDecimalUtils.laenge(this.feldWert(_id));
	}

	public runden(operator: VkBigDecimal, anzStellen: number): VkBigDecimal {
		return BigDecimalUtils.runden(operator, anzStellen);
	}

	public aufrunden(operator: VkBigDecimal, anzStellen: number): VkBigDecimal {
		return BigDecimalUtils.aufrunden(operator, anzStellen);
	}

	public abrunden(operator: VkBigDecimal, anzStellen: number): VkBigDecimal {
		return BigDecimalUtils.abrunden(operator, anzStellen);
	}

	public makeDecimal(wertOrD: string | number): VkBigDecimal {
		return BigDecimalUtils.makeDecimal(wertOrD);
	}

	public abs(operator: VkBigDecimal): VkBigDecimal {
		return BigDecimalUtils.absOp(operator);
	}

	/**
	 * This is the method which is used in the generated code
	 */
	public absWert(index: IterationState, entity: RtIdentifierTemplate): VkBigDecimal {
		const d: VkBigDecimal = this.feldWertAlsZahl(index, entity);
		return d.abs();
	}

	/**
	 * Implementiert das Sprachkonstrukt 'Summe'. Berechnet die Summe der Feldwerte in feldliste.
	 */
	public summe(index: IterationState, entityList: RtIdentifierTemplate[]): VkBigDecimal {
		return this._combineFeldListe(
			index,
			entityList,
			new SummeCombiner(),
			x => this.feldWertAlsZahl(x),
			true
		);
	}

	/**
	 * Implementiert das Sprachkonstrukt 'SummeVonProdukten'. Berechnet die Summe der
	 * Produkte-Pro-Wiederholung der Feldwerte in feldliste.
	 */
	public summeVonProdukten(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): VkBigDecimal {
		let result: VkBigDecimal = VkBigDecimal.ZAHL_0;
		let nichtAngFeldGefunden = false;

		// Iteratoren optimieren
		const innerFeldIteratoren: IIterator<RtInternalIdentifier>[] = [];
		for (let i = 0; i < 2; ++i) {
			const fIt: EntityIterator = new EntityIterator(entityList[i], index);

			if (this._setzeEndIndicesAufMaxGesetzt(fIt, true)) {
				nichtAngFeldGefunden = true;
			}
			if (entityList[i].getFilter()) {
				nichtAngFeldGefunden = true;
			}
			innerFeldIteratoren.push(fIt.iterator());
		}

		// Wert berechnen. Wenn hier ein Feld nicht angegeben ist, wird dies automatisch im Ergebnis beachtet
		for (const [id1, id2] of new ParallelIteratorWrapper(innerFeldIteratoren)) {
			if (!this._isPruefungsRelevant(id1, false) || !this._isPruefungsRelevant(id2, false)) {
				return VkBigDecimal.NICHT_PRUEF_REL_ZAHL;
			}
			result = result.add(this.feldWertAlsZahl(id1).multiply(this.feldWertAlsZahl(id2)));
		}

		if (nichtAngFeldGefunden) {
			return new VkBigDecimal(result.toString(), false, true, true);
		}
		return result;
	}

	public max(...operatorListe: VkBigDecimal[]): VkBigDecimal {
		return this._combineOperatorListe(operatorListe, new MaxWertCombiner());
	}

	public maxDatum(...operatorListe: VkDate[]): VkDate {
		return this._combineOperatorListe(operatorListe, new DateMaxValueCombiner());
	}

	/**
	 * Implementiert das Sprachkonstrukt 'MaxWert'.
	 */
	public maxWert(index: IterationState, entityList: RtIdentifierTemplate[]): VkBigDecimal {
		return this._combineFeldListe(
			index,
			entityList,
			new MaxWertCombiner(),
			x => this.feldWertAlsZahl(x),
			true
		);
	}

	public maxWertDatum(index: IterationState, entityList: RtIdentifierTemplate[]): VkDate {
		return this._combineFeldListe(
			index,
			entityList,
			new DateMaxValueCombiner(),
			x => this.feldWertAlsDatum(x),
			true
		);
	}

	public minWertDatum(index: IterationState, entityList: RtIdentifierTemplate[]): VkDate {
		return this._combineFeldListe(
			index,
			entityList,
			new DateMinValueCombiner(),
			x => this.feldWertAlsDatum(x),
			true
		);
	}

	public ersterAngegebenerWertZahl(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): VkBigDecimal {
		return this._combineFeldListe(
			index,
			entityList,
			new FirstValueCombinerNumber(),
			x => this.feldWertAlsZahl(x),
			false
		);
	}

	public ersterAngegebenerWert(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): VkString {
		return this._combineFeldListe(
			index,
			entityList,
			new FirstValueCombinerString(),
			x => this.feldWert(x),
			false
		);
	}

	public ersterAngegebenerWertDatum(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): VkDate {
		return this._combineFeldListe(
			index,
			entityList,
			new FirstValueCombinerDate(),
			x => this.feldWertAlsDatum(x),
			false
		);
	}

	public ersterAngegebenerWertDateRange(
		index: IterationState,
		entityList: RtIdentifierTemplate[]
	): VkDateRange {
		return this._combineFeldListe(
			index,
			entityList,
			new FirstValueCombinerDateRange(),
			x => this.feldWertAlsDateRange(x),
			false
		);
	}

	public getValidationCache(): ValidationCache {
		return this.validationCache;
	}
}
