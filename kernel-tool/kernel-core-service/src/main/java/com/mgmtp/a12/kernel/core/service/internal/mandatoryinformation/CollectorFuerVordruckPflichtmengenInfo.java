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

package com.mgmtp.a12.kernel.core.service.internal.mandatoryinformation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICompositeCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IRootGroupCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.EntityListConditionName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.LogOp;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.RootGroupConditionName;
import com.mgmtp.a12.kernel.core.utils.internal.Pair;

/**
 * Sammelt die Vordruck-Pflichtmengeninformationen aus einzelnen Regeln. Die einfachsten
 * Bedingungen, die betrachtet werden müssen sind der Form
 * <ul>
 * <li>VordruckVorhanden(V) Und KeinFeldAngegeben(d, e) <br>
 * (führt zu lokaler Pflichtmenge {d, e} für den Vordruck V)
 * </ul>
 * <p>
 * Komplexeres Beispiel:
 * <ul>
 * <li>VordruckVorhanden(V) Und KeinFeldAngegeben(d, e) Und NichtAlleFelderOderKeinFeldAngegeben(f,
 * g) <br>
 * (führt zu lokaler Pflichtmenge {d, e, f, g} für den Vordruck V)
 * </ul>
 * Allgemeine Form: B1 Und ... Und Bn, mit
 * <ul>
 * <li>genau ein Bi == VordruckVorhanden(V)
 * <li>und alle anderen der Form
 * <ul>
 * <li>(KeinFeldAngegeben(d,e) Oder ...) bzw.
 * <li>(NichtAlleFelderOderKeinFeldAngegeben(f,g) Oder ...)
 * </ul>
 * Dies führt zu einer lokalen Pflichtmenge {d, e, f, g} für den Vordruck V.
 */
public class CollectorFuerVordruckPflichtmengenInfo extends PflichtInfoCollector {

	private static final Logger logger = LoggerFactory.getLogger(CollectorFuerVordruckPflichtmengenInfo.class);

	public CollectorFuerVordruckPflichtmengenInfo(RegelInfo rInfo) {
		this.iterationsIntervall = new IterationsIntervall(rInfo.getLfdNrStart(), rInfo.getLfdNrEnde());
		this.regelname = rInfo.getRegelName();
	}

	void createAndAddPflichtInfo(ICompositeCondition bed) {

		PflichtInformation info = createInfoFromBedingung(bed);
		if (info != null) {
			info.setRegelName(regelname);
			logger.debug("Fuege Vordruckpflichtmengeninformation hinzu: " + info);
			infos.add(info);
		}
	}

	private PflichtInformation createInfoFromBedingung(ICompositeCondition bed) {
		Pair<IRootGroupCondition, List<IEntityListCondition>> bedingungen = getRelevanteBedingungen(bed);
		if (bedingungen == null) {
			return null;
		}
		return createInfoFromBedingungen(bedingungen);
	}

	private PflichtInformation createInfoFromBedingungen(Pair<IRootGroupCondition, List<IEntityListCondition>> bedingungen) {
		IRootGroupCondition additivBed = bedingungen.getEntry1();
		List<IEntityListCondition> pflichtBeds = bedingungen.getEntry2();
		return createInfo(additivBed, pflichtBeds);
	}

	private PflichtInformation createInfo(IRootGroupCondition additivBed, List<IEntityListCondition> pflichtBeds) {
		String vordruck = additivBed.getRootGroupName();
		Set<PflichtIdentifier> pfl = new HashSet<>();
		for (IEntityListCondition bed : pflichtBeds) {
			pfl.addAll(createPflichtIdentifierMenge(bed));
		}
		return PflichtInformation.createVordruckPflichtMengenInformation(vordruck, pfl, iterationsIntervall);
	}

	/**
	 * Hier erfolgt die eigentliche Analyse der Bedingung. Interessant sind zusammengesetzte
	 * Bedingungen der Form
	 * <p>
	 * B1 Und ... Und Bn,
	 * <p>
	 * wobei genau ein Bi == VordruckVorhanden(V) ist und die anderen alle entweder der Form
	 * <ul>
	 * <li>(KeinFeldAngegeben(b1,..,bk) Oder ...) oder
	 * <li>(NichtAlleFelderOderKeinFeldAngegeben(b1,..,bk) Oder ...)
	 * </ul>
	 * sind. <br>
	 * Für eine solche Bedingung wird ein Paar von Bedingungen zurückgegeben, bei dem die erste
	 * Komponente die "Additivbedingung" (VordruckVorhanden(V)) ist und die andere eine Liste der
	 * restlichen (KeinFeldAngegeben- bzw. NichtAlleFelderOderKeinFeldAngegeben-) Bedingungen -- die
	 * "Pflichtbedingungen". Ist die Bedingung anders aufgebaut, so wird null zurückgegeben.
	 * <p>
	 * Wenn in einer Oder-Bedingung mehrere Sprachkonstrukte NichtAlleFelderOderKeinFeldAngegeben
	 * oder KeinFeldAngegeben vorkommen wird nur immer die erste derartige Bedingung herangezogen.
	 * Das kann dazu führen, dass mögliche Pflichtmengen nicht berücksichtigt werden oder im
	 * speziellen Fall, wenn die Regel in der Form
	 * <code>VordruckAngegeben(A) Und (NichtAlleFelderOderKeinFeldAngegeben(b1,...,bk) Oder ...)</code>
	 * vorliegt gar keine Bedingung zurückgeliefert wird.
	 * Allerdings werden derartige Pflichtmengen primär in projBeta verwendet und dort gibt es bisher
	 * keine wiederholbaren Kontexte, wodurch dieser Fall nie auftritt.
	 * Eine Änderung ist deshalb nicht angeraten.
	 */
	private Pair<IRootGroupCondition, List<IEntityListCondition>> getRelevanteBedingungen(ICompositeCondition bed) {

		// interessant sind nur UND-Bedingungen
		if (!LogOp.And.equals(bed.getLogOp())) {
			return null;
		}

		IRootGroupCondition additivBed = null;
		List<IEntityListCondition> pflichtBeds = new ArrayList<>();
		List<ICondition> teilBedingungen = bed.getConditionList();
		for (int i = 0; i < teilBedingungen.size(); i++) {
			ICondition teilBed = teilBedingungen.get(i);
			// ist eine Teilbedingung der Und-Bedingung weder eine Vordruck-, noch eine Feldlisten- noch eine
			// zusammengesetzte Bedingung, so wird keine Vordruckpflichtmengeninfo ermittelt.
			if (!(teilBed instanceof ICompositeCondition)
					&& !(teilBed instanceof IRootGroupCondition)
					&& !(teilBed instanceof IEntityListCondition)) {
				return null;
			}
			if (teilBed instanceof ICompositeCondition) {
				// nur Oder-Teilbedingungen sind relevant
				if (!LogOp.Or.equals(((ICompositeCondition) teilBed).getLogOp())) {
					return null;
				}
				IEntityListCondition pflichtBed = getPflichtMengenBed((ICompositeCondition) teilBed);
				if (pflichtBed == null
						|| !isRelevantePflichtBedingung(pflichtBed.getConditionName(), teilBedingungen.size())) {
					return null;
				}
				pflichtBeds.add(pflichtBed);
			} else if (teilBed instanceof IRootGroupCondition) {
				IRootGroupCondition vordruckBed = (IRootGroupCondition) teilBed;
				if (additivBed != null || vordruckBed.getConditionName() != RootGroupConditionName.MindestensEinVordruckAngegeben) {
					// Prüfung auf additivBed != null: es darf nur eine VordruckVorhanden-Bedingung in der
					// zusammengesetzten Und-Bedingung vorkommen, sonst ist sie nicht interessant für die Ermittlung
					return null;
				}
				additivBed = vordruckBed;
			} else {
				IEntityListCondition fb = (IEntityListCondition) teilBed;
				EntityListConditionName bedName = fb.getConditionName();

				if (!isRelevantePflichtBedingung(bedName, teilBedingungen.size())) {
					return null;
				}
				pflichtBeds.add(fb);
			}
		} // Ende der Schleife

		if (additivBed == null || pflichtBeds.isEmpty()) {
			return null;
		}
		return new Pair<>(additivBed, pflichtBeds);
	}

	private boolean isRelevantePflichtBedingung(
			EntityListConditionName bedName, int anzahlTeilbedingungen) {
		return KEIN_FELD.equals(bedName)
				|| (NICHT_ALLE_FELDER.equals(bedName) && anzahlTeilbedingungen > 2);
	}

	/**
	 * Gibt die erste Teilbedingung der zusammengesetzten Bedingung bed zurück, die eine
	 * NichtAlleFelderOderKeinFeldAngegeben- oder eine KeinFeldAngegeben- Bedingung ist, falls eine
	 * solche Teilbedingung existiert und null sonst.
	 */
	private IEntityListCondition getPflichtMengenBed(ICompositeCondition bed) {
		for (ICondition b : bed.getConditionList()) {
			if (b instanceof IEntityListCondition) {
				IEntityListCondition fb = (IEntityListCondition) b;
				if (NICHT_ALLE_FELDER.equals(fb.getConditionName())
						|| KEIN_FELD.equals(fb.getConditionName())) {
					return fb;
				}
			}
		}
		return null;
	}

}
