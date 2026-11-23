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

package com.mgmtp.a12.kernel.core.service.internal.extservices;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mgmtp.a12.kernel.core.parser.internal.ParserService;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.ExpandMode;
import com.mgmtp.a12.kernel.core.parser.internal.util.AnalyseService;
import com.mgmtp.a12.kernel.core.parser.internal.util.ReferencedIdsUtil;
import com.mgmtp.a12.kernel.core.parser.internal.util.ReferencedIdsUtil.ReferencedIdsMode;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IParseTreeEntity;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.common.VordruckIntern;
import com.mgmtp.a12.kernel.core.service.internal.mandatoryinformation.CollectorFuerPflichtfelderInfo;
import com.mgmtp.a12.kernel.core.service.internal.mandatoryinformation.IterationsIntervall;
import com.mgmtp.a12.kernel.core.service.internal.mandatoryinformation.LokalePflichtInfoVerdichter;
import com.mgmtp.a12.kernel.core.service.internal.mandatoryinformation.PflichtAngaben;
import com.mgmtp.a12.kernel.core.service.internal.mandatoryinformation.PflichtIdentifier;
import com.mgmtp.a12.kernel.core.service.internal.mandatoryinformation.PflichtInfoCollector;
import com.mgmtp.a12.kernel.core.service.internal.mandatoryinformation.PflichtInformation;
import com.mgmtp.a12.kernel.core.service.internal.mandatoryinformation.PflichtInformation.InfoTyp;
import com.mgmtp.a12.kernel.core.service.internal.mandatoryinformation.RegelInfo;
import com.mgmtp.a12.kernel.core.service.internal.serviceparams.MandatoryInformation;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IGroup;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IRule;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IRule.RuleSeverityType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IMandatoryInformation;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.services.IMVK_Service;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.services.IServiceConfiguration;
import com.mgmtp.a12.kernel.core.utils.internal.Pair;
import com.mgmtp.a12.kernel.core.utils.internal.ProblemReporter;

/**
 * Determines the information of local and global mandatory fields and mandatory sets.
 */
public class MandatoryInformationService {

	private static final Logger LOG = LoggerFactory.getLogger(MandatoryInformationService.class);
	private SortedSet<RegelInfo> ruleInfo;

	// Alle referenzierten Vordrucke haben als lfd Nr == 0 und USB == 0
	private Set<VordruckIntern> referenzierteVordrucke;

	private final IEC ec;
	private final IServiceConfiguration serviceConfiguration;

	/**
	 * Create an instance to be used from a vk service interface {@link IMVK_Service}.
	 * <p>
	 * Additional rules will be created if the property {@link IField#isMandatory()} is set for any
	 * field in the {@link IEC} returned by {@link IMVK_Service#getIEC()}.
	 *
	 * @param rules
	 * 		the rules specified in the editor.
	 * @param serviceConfiguration
	 * 		the service configuration, the mandatory information is to be collected for.
	 * @return a new instance of the {@link MandatoryInformationService}.
	 */
	public static MandatoryInformationService createInstance(Map<IRule, ICondition> rules,
			IServiceConfiguration serviceConfiguration) {
		return new MandatoryInformationService(rules, serviceConfiguration.getIEC(), serviceConfiguration);
	}

	/**
	 * Create an instance to be used, when the additional rules already have been added to the rule
	 * set.
	 *
	 * @param rules
	 * 		the rules containing all rules including auto-generated rules.
	 * @param ec
	 * 		the {@link IEC} containing information for repeatabilities of referenced fields.
	 * @return a new instance of the {@link MandatoryInformationService}.
	 */
	public static MandatoryInformationService createInstance(Map<IRule, ICondition> rules, IEC ec) {
		return new MandatoryInformationService(rules, ec, null);
	}

	/**
	 * Ermittelt aus der Menge der übergebenen Regeln alle referenzierten Vordrucke und
	 * die Menge der Regelinfos.
	 * <p>
	 * Precondition:
	 * The conditions have to be expanded, i.e. the condition text has to be parsed with
	 * {@link ExpandMode#EXPAND}.
	 *
	 * @param regeln
	 * 		übergebene Regelmenge (mit bereits geparsten Bedingungen), es werden nur die
	 * 		Fehler-Regeln berücksichtigt
	 * @param serviceConfiguration
	 * 		the service configuration used to determine repeatabilities, parse additionally created rules,
	 * 		and determine H model property.
	 */
	private MandatoryInformationService(Map<IRule, ICondition> regeln, IEC ec, IServiceConfiguration serviceConfiguration) {
		this.ec = ec;
		this.serviceConfiguration = serviceConfiguration;

		setzeRegelinfosUndVordrucke(regeln);
	}

	public IMandatoryInformation getMandatoryInformation() {

		PflichtAngaben pflichtAngaben = ermittlePflichtAngaben(false);

		Set<String> mandatory = getPflichtFuerAlleLfdNrn(pflichtAngaben.getGlobalePflichtfelder());
		Set<String> mandatoryForRootGroup = getPflichtFuerAlleLfdNrn(pflichtAngaben.getVordruckPflichtFelder());
		Set<String> mandatoryRootGroups = getPflichtVordruckeIntern().stream()
				.filter(vordruck -> vordruck.getLfdNummer() == IParseTreeEntity.WILDCARD).map(VordruckIntern::getName)
				.collect(Collectors.toSet());

		return new MandatoryInformation(mandatory, mandatoryForRootGroup, mandatoryRootGroups);
	}

	/**
	 * Determines all repeatable fields that are mandatory with respect to the nearest containing
	 * repeatable group.
	 * The result is needed for the meta key
	 * IMetaKeys#FIELD_MANDATORY_GLOBAL_OR_IN_SURROUNDING_REPEATABLE_GROUP.
	 */
	private Set<String> calculateMandatoryFieldsForRepeatableGroup(Set<PflichtInformation> localMandatoryFields,
			Set<PflichtInformation> mandatoryFieldsForRootGroup) {
		Set<String> result = new HashSet<>();
		Set<String> repeatableGroupNames = EntityUtils.getAllGroups(ec).stream()
				.filter(EntityUtils::isRepeatableOnLowestLevel)
				.map(IGroup::getFullName)
				.collect(Collectors.toSet());
		for (String repeatableGroupName : repeatableGroupNames) {
			if (EntityUtils.isRootGroup(repeatableGroupName)) {
				String rootGroupName = EntityUtils.getRootGroupName(repeatableGroupName);
				// For root groups, check the mandatory fields for root groups
				mandatoryFieldsForRootGroup.stream()
						.filter(pi -> rootGroupName.equals(pi.getVordruckName()))
						.flatMap(pi -> pi.getpMenge().stream())
						.map(PflichtIdentifier::getFullName)
						.forEach(result::add);
			}
			// Check the local mandatory fields for the contained fields of the group.
			Set<String> fieldNamesForGroup = ec.getAllContainedFields(repeatableGroupName).stream()
					.map(IField::getFullName)
					.collect(Collectors.toSet());
			localMandatoryFields.stream()
					.filter(lmf -> containsAllFields(lmf.getAdditivMenge(), fieldNamesForGroup))
					.flatMap(pi -> pi.getpMenge().stream())
					.map(PflichtIdentifier::getFullName)
					.filter(f -> EntityUtils.isDescendantOrEqual(f, repeatableGroupName))
					.forEach(result::add);
		}
		return result;
	}

	private boolean containsAllFields(Set<PflichtIdentifier> additivMenge, Set<String> fieldNames) {
		Set<String> additivFieldNames = additivMenge.stream()
				.map(PflichtIdentifier::getFullName)
				.collect(Collectors.toSet());
		return additivFieldNames.containsAll(fieldNames);
	}

	private void setzeRegelinfosUndVordrucke(Map<IRule, ICondition> regeln) {
		Map<IRule, ICondition> allRules;
		if (serviceConfiguration != null) {
			Map<IRule, ICondition> additionalRules = createAdditionalRules();
			// [Optimization]: do not copy rules if additional rules are empty
			allRules = new HashMap<>(regeln);
			allRules.putAll(additionalRules);
		} else {
			allRules = regeln;
		}

		ruleInfo = allRules.entrySet().stream()
				.filter(e -> RuleSeverityType.ERROR == e.getKey().getSeverityType())
				.filter(e -> e.getValue() != null)
				.filter(e -> !AnalyseService.usesSemanticIndex(e.getValue()))
				.filter(e -> !AnalyseService.isParallelIteration(e.getValue(), true))
				.filter(e -> !AnalyseService.referencesDifferentRootGroups(e.getValue()))
				.filter(e -> AnalyseService.getFilterConditions(Collections.singletonList(e.getValue())).isEmpty())
				.map(e -> RegelInfo.createRegelInfo(e.getKey(), e.getValue(), ec))
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(() -> new TreeSet<>((r1, r2) -> r1.getRegelName().compareTo(r2.getRegelName()))));

		referenzierteVordrucke = ruleInfo.stream()
				.flatMap(ri -> ReferencedIdsUtil.getDirectlyReferencedFieldNames(ri.getNormalizedBedingung(), ec).stream())
				.map(EntityUtils::getRootGroupName)
				.distinct()
				.map(v -> new VordruckIntern(v, IParseTreeEntity.WILDCARD))
				.collect(Collectors.toSet());
	}

	private Map<IRule, ICondition> createAdditionalRules() {
		AutogeneratedRulesService rulesService = new AutogeneratedRulesService(serviceConfiguration.getIEC(),
				serviceConfiguration.getModelSpecificSettings(), serviceConfiguration.getProjectSpecificSettings(), null);
		return Stream.concat(rulesService.createRulesForMandatoryFields().stream(),
						rulesService.createMandatoryRulesForIndexFields(true).stream())
				.map(r -> Pair.of(r, parseRule(r)))
				// Ignore erroneous rules (condition == null)
				.filter(p -> p.getEntry2() != null)
				.collect(Collectors.toMap(Pair::getEntry1, Pair::getEntry2));
	}

	private ICondition parseRule(IRule r) {
		ParserService parserService = new ParserService(serviceConfiguration);
		ProblemReporter pm = new ProblemReporter();
		// We ignore errors and return null. In #setzeRegelinfosUndVordrucke, such rules are filtered out and hence ignored.
		// For the documentation generation, the flag itself is also used as an indicator for mandatory fields. Therefore we are safe there.
		return parserService.parseAndCheckCondition(r, r.getErrorConditionText(), pm, ExpandMode.EXPAND);
	}

	/**
	 * Ermittle alle Pflichtangaben aus allen Regeln und gebe sie als Gesamtstruktur zurück
	 *
	 * @param allePflichtAngaben
	 * 		gibt an, ob auch Pflichtmengen und lokale Pflichtfelder ermittelt werden sollen
	 * 		(diese Infos werden nur für Anwendungen mit Oberfläche generiert) oder nur
	 * 		(Vordruck)-Pflichtfelder.
	 */
	public PflichtAngaben ermittlePflichtAngaben(boolean allePflichtAngaben) {
		PflichtAngaben pflichtAngaben = new PflichtAngaben();

		//in jedem Fall zunächst alle Informationen ermitteln, da die Verdichtung der Informationen zu neuen (Vordruck-)Pflichtfeldern führen kann
		Set<PflichtInformation> lokalePflichtfelder = analysiereRegeln(InfoTyp.LokalePflichtfeldInfo);
		pflichtAngaben.setLokalePflichtFelder(lokalePflichtfelder);

		Set<PflichtInformation> lokalePflichtmengen = analysiereRegeln(InfoTyp.LokalePflichtmengenInfo);
		pflichtAngaben.setLokalePflichtMengen(lokalePflichtmengen);

		Set<PflichtInformation> vordruckPflichtmengen = analysiereRegeln(InfoTyp.VordruckPflichtmengenInfo);
		pflichtAngaben.setVordruckPflichtMengen(vordruckPflichtmengen);

		Set<PflichtInformation> globalePflichtmengen = analysiereRegeln(InfoTyp.GlobalePflichtmengenInfo);
		pflichtAngaben.setGlobalePflichtMengenInfos(globalePflichtmengen);

		Set<PflichtInformation> vordruckPflichtfelder = getVordruckPflichtfeldInfos();
		pflichtAngaben.setVordruckPflichtFelder(vordruckPflichtfelder);

		Set<PflichtInformation> globalePflichtfelder = getPflichtfeldInfos();
		pflichtAngaben.setGlobalePflichtfelder(globalePflichtfelder);

		new LokalePflichtInfoVerdichter().verdichteInfos(pflichtAngaben, getPflichtVordruckeIntern());

		Set<String> mandatoryForRepeatableGroups = calculateMandatoryFieldsForRepeatableGroup(
				pflichtAngaben.getLokalePflichtFelder(), pflichtAngaben.getVordruckPflichtFelder());
		pflichtAngaben.setLocalMandatoryFieldsForRepeatableGroup(mandatoryForRepeatableGroups);

		logResult(pflichtAngaben);

		if (allePflichtAngaben) {
			return pflichtAngaben;
		}
		//nur Pflichtfelder und Vordruckpflichtfelder
		PflichtAngaben result = new PflichtAngaben();
		result.setVordruckPflichtFelder(pflichtAngaben.getVordruckPflichtFelder());
		result.setGlobalePflichtfelder(pflichtAngaben.getGlobalePflichtfelder());
		return result;
	}

	private void logResult(PflichtAngaben pflichtAngaben) {
		LOG.debug(pflichtAngaben.getLokalePflichtFelder().size() + " pfi, "
				+ pflichtAngaben.getLokalePflichtMengen().size() + " pmi, "
				+ pflichtAngaben.getVordruckPflichtMengen().size() + " vpm, "
				+ pflichtAngaben.getVordruckPflichtFelder().size() + " vpf, "
				+ pflichtAngaben.getGlobalePflichtMengenInfos().size() + " gpmi, "
				+ pflichtAngaben.getGlobalePflichtfelder().size() + " gpf");
	}

	/**
	 * Ermittelt zu dem übergebenen Infotyp die entsprechende Menge von PflichtInformation.
	 *
	 * @param infoTyp
	 * 		- Art der Pflichtinfo, die ermittelt werden soll
	 * @return Menge der Pflichtinformation
	 */
	private Set<PflichtInformation> analysiereRegeln(InfoTyp infoTyp) {
		Set<PflichtInformation> result = new LinkedHashSet<>();

		for (RegelInfo regelInfo : ruleInfo) {
			PflichtInfoCollector visitor = PflichtInfoCollector.create(infoTyp, regelInfo);
			ICondition condition = regelInfo.getNormalizedBedingung();
			// If the condition contains a filter condition, we do not regard this rule as relevant
			condition.traverse(visitor);
			result.addAll(visitor.getInfos());
		}
		return result;
	}

	public static Set<String> getPflichtFuerAlleLfdNrn(Set<PflichtInformation> pfInfos) {
		return pfInfos.stream().flatMap(pfInfo -> pfInfo.getpMenge().stream()
						.filter(pfIdentifier -> istPflichtfeldFuerAllLfdNr(pfInfo, pfIdentifier)))
				.map(PflichtIdentifier::getFullName)
				.collect(Collectors.toSet());
	}

	private Set<PflichtInformation> getVordruckPflichtfeldInfos() {

		Set<VordruckIntern> pflichtVordrucke = getPflichtVordruckeEinzeln();

		Set<PflichtInformation> vordruckPflichtfelder = new LinkedHashSet<>();

		// Setze jeden Vordruck als Pflichtvordruck:
		// Felder, die daraufhin Pflichtfelder werden, sind VordruckPflichtfelder
		for (VordruckIntern v : referenzierteVordrucke) {
			LOG.debug("Analysiere Vordruck " + v.getName());
			Set<VordruckIntern> vs = new HashSet<>(pflichtVordrucke);
			vs.add(v);

			Set<PflichtInformation> vpfInfos = getPflichtfeldInfosIntern(vs, v.getName());
			for (PflichtInformation pInfo : vpfInfos) {
				pInfo.setVordruckName(v.getName());
			}
			vordruckPflichtfelder.addAll(vpfInfos);
		}
		return vordruckPflichtfelder;
	}

	/**
	 * Ermittelt die LfdNummer-Menge für den Musterbereich und den PflichtIdentifier
	 *
	 * @return Menge der laufenden Nummern;
	 */
	public static Set<Integer> ermittlePflichtLfdNummern(PflichtInformation pInfo, PflichtIdentifier pf) {
		IterationsIntervall itIntervall = pInfo.getIterationsIntervall();
		Set<Integer> lfdNummern = new HashSet<>();
		if (pf.getMaxLfdNr() == 1) { //kein wiederholbarer vordruck
			lfdNummern.add(0);
			return lfdNummern;
		}

		// Behandle Iteration:
		if (pf.getLfdNummer() == IParseTreeEntity.ITERATION) {
			if (itIntervall.getLfdNrStart() == 1 && itIntervall.getLfdNrEnde() == pf.getMaxLfdNr()) {
				lfdNummern.add(0); // wir iterieren von Anfang bis Ende.
				return lfdNummern;
			}

			// Eingeschränkte Iteration: Füge die lfd-Nummern dazu.
			for (int l = (int) itIntervall.getLfdNrStart(); l <= (int) itIntervall.getLfdNrEnde(); l++) {
				lfdNummern.add(l);
			}
			return lfdNummern;
		}

		// Keine Iteration, aber wiederholbar: Füge die lfd-Nummer dazu.
		lfdNummern.add((int) pf.getLfdNummer());
		return lfdNummern;
	}

	/**
	 * Ermittelt die Pflichtvordrucke
	 *
	 * @return Menge der Pflichtvordrucke
	 */
	private Set<VordruckIntern> getPflichtVordruckeIntern() {
		Set<VordruckIntern> pflichtVordrucke = getPflichtVordruckeEinzeln();

		Set<PflichtInformation> pflichtfeldInfos = new LinkedHashSet<>();
		ermittlePflichtFeldInfosUndPflichtVordrucke(pflichtfeldInfos, pflichtVordrucke, null);
		entferneRedundanteVordruckAngaben(pflichtVordrucke);
		setzeLfdNrFuerNichtWiederholbareVordrucke(pflichtVordrucke);

		return pflichtVordrucke;
	}

	/**
	 * Redundante Vordruckangaben müssen entfernt werden: Wird für einen Vordruck bereits 0 für die
	 * laufende Nummer Vordruck spezifiziert, so müssen alle anderen Spezifikationen entfernt
	 * werden.
	 */
	private static void entferneRedundanteVordruckAngaben(Set<VordruckIntern> vordrucke) {
		Set<String> pfMitAlle = new HashSet<>();
		for (VordruckIntern v : vordrucke) {
			if (v.getLfdNummer() == 0) {
				pfMitAlle.add(v.getName());
			}
		}
		Set<Long> lfdNummern = new HashSet<>();
		for (String name : pfMitAlle) {
			lfdNummern.clear();
			for (VordruckIntern v : vordrucke) {
				if (v.getLfdNummer() > 0 && name.equals(v.getName())) {
					lfdNummern.add(v.getLfdNummer());
				}
			}
			for (Long l : lfdNummern) {
				VordruckIntern v = new VordruckIntern(name, l);
				vordrucke.remove(v);
			}
		}
	}

	/**
	 * Ermittelt die Pflichtvordrucke für den ersten Iterationsschritt
	 *
	 * @return Menge der Pflichtvordrucke (1. Stufe)
	 */
	private Set<VordruckIntern> getPflichtVordruckeEinzeln() {
		Set<VordruckIntern> result = new HashSet<>();

		for (PflichtInformation info : analysiereRegeln(InfoTyp.PflichtVordruckInfo)) {
			for (long l = info.getLfdNrStart(); l <= info.getLfdNrEnde(); l++) {
				result.add(new VordruckIntern(info.getVordruckName(), l));
			}
		}
		setzeLfdNrFuerNichtWiederholbareVordrucke(result);
		return result;
	}

	private void setzeLfdNrFuerNichtWiederholbareVordrucke(Collection<VordruckIntern> set) {
		for (VordruckIntern v : set) {
			if (ec.get("/" + v.getName()).getMax()[0] == 1) {
				v.setLfdNummer(IParseTreeEntity.WILDCARD);
			}
		}
	}

	private Set<PflichtInformation> getPflichtfeldInfos() {
		Set<VordruckIntern> pflichtVordrucke = getPflichtVordruckeEinzeln();
		return getPflichtfeldInfosIntern(pflichtVordrucke, null);
	}

	private Set<PflichtInformation> getPflichtfeldInfosIntern(Set<VordruckIntern> pflichtVordrucke, String aktVordruck) {
		Set<PflichtInformation> pflichtfeldInfos = new LinkedHashSet<>();
		ermittlePflichtFeldInfosUndPflichtVordrucke(pflichtfeldInfos, pflichtVordrucke, aktVordruck);
		return pflichtfeldInfos;
	}

	private void ermittlePflichtFeldInfosUndPflichtVordrucke(Set<PflichtInformation> pflichtfeldInfos,
			Set<VordruckIntern> pflichtVordrucke, String aktVordruck) {
		boolean aenderung = true;
		while (aenderung) {
			aenderung = false;
			for (RegelInfo rInfo : ruleInfo) {
				if (rInfo.istGleichFuerAlleLfdNr()) {
					if (ermittlePflFelderUndVordruckeAusRegel(pflichtfeldInfos, pflichtVordrucke, rInfo, 0, aktVordruck)) {
						aenderung = true;
					}
				} else {
					for (long l = rInfo.getLfdNrStart(); l <= rInfo.getLfdNrEnde(); l++) {
						if (ermittlePflFelderUndVordruckeAusRegel(pflichtfeldInfos, pflichtVordrucke, rInfo, l, aktVordruck)) {
							aenderung = true;
						}
					}
				}
			}
			aendereRegelInfosMitSpezifischemPflichtfeld(pflichtfeldInfos);
		}
	}

	/**
	 * Wenn ein Feld, das nur für spezifische lfd-Nummern ein Pflichtfeld ist, in einer Regel
	 * referenziert ist, muss dies in der zur Regel gehörenden RegelInfo gespeichert werden, damit
	 * bei der Ermittlung weiterer Pflichtfelder die Regel für jede lfd-Nr separat untersucht wird.
	 */
	private void aendereRegelInfosMitSpezifischemPflichtfeld(Set<PflichtInformation> pflichtfeldInfos) {
		for (PflichtInformation pInfo : pflichtfeldInfos) {
			for (PflichtIdentifier pf : pInfo.getpMenge()) {
				if (istPflichtfeldFuerAllLfdNr(pInfo, pf)) {
					continue;
				}

				//Pflichtfeld nicht für alle lfdNummern gleich, setze Regelinfo um.
				for (RegelInfo rInfo : ruleInfo) {
					if (rInfo.istGleichFuerAlleLfdNr()) {
						Collection<IEntityId> ids = ReferencedIdsUtil.getDirektReferenzierteIFeldIds(rInfo.getNormalizedBedingung(),
								ReferencedIdsMode.IGNORE_FILTERCONDITION, ec, true);
						Collection<String> refFeldNamen = ids.stream().map(IEntityId::getFullName).toList();
						if (refFeldNamen.contains(pf.getFullName())) {
							rInfo.setNichtGleichFuerAlleLfdNr();
						}
					}
				}
			}
		}
	}

	private static boolean istPflichtfeldFuerAllLfdNr(PflichtInformation pInfo, PflichtIdentifier pf) {
		Set<Integer> pflLfdNummern = ermittlePflichtLfdNummern(pInfo, pf);
		return pflLfdNummern.contains(0);
	}

	/**
	 * Gibt <i>true</i> zurück, wenn neue Pflichtfelder ermittelt wurden.
	 */
	private static boolean ermittlePflFelderUndVordruckeAusRegel(Set<PflichtInformation> pflichtfeldInfos,
			Set<VordruckIntern> pflichtVordrucke, RegelInfo rInfo, long l, String aktVordruck) {
		CollectorFuerPflichtfelderInfo coll = new CollectorFuerPflichtfelderInfo(rInfo, pflichtfeldInfos, pflichtVordrucke, l);
		ICondition condition = rInfo.getNormalizedBedingung();
		condition.traverse(coll);
		return coll.addNeueInfo(aktVordruck);
	}

}
