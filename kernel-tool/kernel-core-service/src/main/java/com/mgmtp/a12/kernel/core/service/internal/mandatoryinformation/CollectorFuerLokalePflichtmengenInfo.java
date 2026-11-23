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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICompositeCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.EntityListConditionName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.LogOp;

/**
 * Sammelt die lokalen Pflichtmengeninformationen aus einzelnen Regeln. Eine lokale
 * Pflichtmengeninformation besagt im Wesentlichen folgendes: wenn mindestens eines der Felder aus
 * der "Additivmenge" angegeben ist, dann muß auch mindestens ein Feld aus der "Pflichtmenge"
 * angegeben sein, wenn keine Regel feuern soll. <br>
 * Die einfachsten Bedingungen, die betrachtet werden müssen, sind der Form
 * <ul>
 * <li>MindestensEinFeldAngegeben(a, b, c) Und KeinFeldAngegeben(d, e) <br>
 * (führt zu lokaler Pflichtmenge {d, e} zur Additivmenge {a, b, c, d, e})
 * </ul>
 * <p>
 * Komplexeres Beispiel:
 * <ul>
 * <li>MindestensEinFeldAngegeben(a, b, c) Und KeinFeldAngegeben(d, e) Und
 * NichtAlleFelderOderKeinFeldAngegeben(f, g) <br>
 * (führt zu lokaler Pflichtmenge {d, e, f, g} zur Additivmenge {a, b, c, d, e, f, g})
 * </ul>
 * Allgemeine Form: B1 Und ... Und Bn, mit
 * <ul>
 * <li>genau ein Bi == MindestensEinFeldAngegeben(a,b,c)
 * <li>und alle anderen der Form
 * <ul>
 * <li>(KeinFeldAngegeben(d,e) Oder ...) bzw.
 * <li>(NichtAlleFelderOderKeinFeldAngegeben(f,g) Oder ...)
 * </ul>
 * </ul>
 * Dies führt zu einer lokalen Pflichtmenge {d, e, f, g} zur Additivmenge {a, b, c, d, e, f, g}.
 * <br>
 * Außerdem müssen alle zusammengesetzten Oder-Bedingungen berücksichtigt werden, bei denen eine
 * Teilbedingung die obige Form hat.
 */
public class CollectorFuerLokalePflichtmengenInfo extends PflichtInfoCollector {

	private static final Logger logger = LoggerFactory.getLogger(CollectorFuerLokalePflichtmengenInfo.class);

	public CollectorFuerLokalePflichtmengenInfo(RegelInfo regelInfo) {
		this.iterationsIntervall = regelInfo.getIterationsIntervall();
		this.regelname = regelInfo.getRegelName();
	}

	void createAndAddPflichtInfo(ICompositeCondition bed) {
		PflichtInformation info = createInfoFromBedingung(bed);
		if (info != null) {
			info.setRegelName(regelname);
			logger.debug("Fuege lokale Pflichtmengeninformation hinzu: " + info);
			infos.add(info);
		}
	}

	private PflichtInformation createInfoFromBedingung(ICompositeCondition bed) {
		List<IEntityListCondition> bedingungen = getRelevanteBedingungen(bed);
		if (bedingungen == null) {
			return null;
		}
		IEntityListCondition additivBed = bedingungen.remove(0);
		if (!EbenenChecker.checkEbenen(additivBed)) {
			return null;
		}

		return createInfo(additivBed, bedingungen);
	}

	private PflichtInformation createInfo(IEntityListCondition additivBed,
			List<IEntityListCondition> pflichtBeds) {
		Set<PflichtIdentifier> pfl = new HashSet<>();
		Set<PflichtIdentifier> add = createPflichtIdentifierMenge(additivBed);
		for (IEntityListCondition bed : pflichtBeds) {
			pfl.addAll(createPflichtIdentifierMenge(bed));
		}
		// für lokale Pflichtmengeninfos wird die Pflichtmenge der Additivmenge hinzugefügt
		add.addAll(pfl);
		PflichtInformation info = PflichtInformation.createLokalePflichtMengenInformation(add, pfl,
				iterationsIntervall);
		info.getAdditivMenge().addAll(info.getpMenge());
		return info;
	}

	/**
	 * Zu einer zusammengesetzten Bedingung der Form
	 * <p>
	 * B1 Und ... Und Bn,
	 * <p>
	 * wobei genau ein Bi == MindestenEinFeldAngegeben(a1,...,am) ist und die anderen alle entweder
	 * der Form
	 * <ul>
	 * <li>(KeinFeldAngegeben(b1,..,bl) Oder ...) oder
	 * <li>(NichtAlleFelderOderKeinFeldAngegeben(b1,..,bl) Oder ...)
	 * </ul>
	 * sind, wird eine Liste von Bedingungen zurükgegeben, in der die erste Bedingung die
	 * "Additivbedingung" (MindestenEinFeldAngegeben(a1,...,am)) ist und die anderen
	 * (KeinFeldAngegeben bzw. NichtAlleFelderOderKeinFeldAngegeben) die "Pflichtbedingungen". Ist
	 * die Bedingung anders aufgebaut, so wird null zurückgegeben.
	 */
	private List<IEntityListCondition> getRelevanteBedingungen(ICompositeCondition bed) {
		//interessant sind nur UND-Bedingungen
		if (!LogOp.And.equals(bed.getLogOp())) {
			return null;
		}

		IEntityListCondition additivBed = null;
		List<IEntityListCondition> pflichtBeds = new ArrayList<>();
		List<ICondition> teilBedingungen = bed.getConditionList();
		for (ICondition teilBed : teilBedingungen) {
			// ist eine Teilbedingung der Und-Bedingung weder eine Feldlisten- noch eine
			// zusammengesetzte Bedingung, so wird keine lokale Pflichtmengeninfo ermittelt.
			if (!(teilBed instanceof IEntityListCondition) && !(teilBed instanceof ICompositeCondition)) {
				return null;
			}
			if (teilBed instanceof ICompositeCondition) {
				if (!LogOp.Or.equals(((ICompositeCondition) teilBed).getLogOp())) {
					return null;
				}
				IEntityListCondition pflichtBed = getPflichtMengenBed((ICompositeCondition) teilBed);
				if (pflichtBed == null || !isRelevantePflichtBedingung(pflichtBed.getConditionName(), teilBedingungen.size())) {
					return null;
				}
				pflichtBeds.add(pflichtBed);
				continue;

			}

			IEntityListCondition fb = (IEntityListCondition) teilBed;
			EntityListConditionName bedName = fb.getConditionName();

			if (MIND_EIN_FELD.equals(bedName) && additivBed == null) {
				//Prüfung additivBed == null: es darf nur eine "MindestensEinFeldAngegeben"-Bedingung in der
				//zusammengesetzten Und-Bedingung vorkommen, sonst nicht interessant für die Ermittlung
				additivBed = fb;
				continue;
			}
			if (!isRelevantePflichtBedingung(bedName, teilBedingungen.size())) {
				return null;
			}
			pflichtBeds.add(fb);
		}
		if (additivBed == null || pflichtBeds.isEmpty()) {
			return null;
		}
		List<IEntityListCondition> result = new ArrayList<>(Arrays.asList(additivBed));
		result.addAll(pflichtBeds);
		return result;
	}

	private boolean isRelevantePflichtBedingung(EntityListConditionName bedName, int anzahlTeilbedingungen) {
		return KEIN_FELD.equals(bedName) || (NICHT_ALLE_FELDER.equals(bedName) && anzahlTeilbedingungen > 2);
	}

	/**
	 * Liefert die erste NichtAlleFelderOderKeinFeldAngegeben- oder KeinFeldAngegeben-Teilbedingung
	 * von bed, oder null, falls keine solche Teilbedingung existiert.
	 */
	private IEntityListCondition getPflichtMengenBed(ICompositeCondition bed) {
		for (ICondition b : bed.getConditionList()) {
			if (b instanceof IEntityListCondition) {
				IEntityListCondition fb = (IEntityListCondition) b;
				if (NICHT_ALLE_FELDER.equals(fb.getConditionName()) || KEIN_FELD.equals(fb.getConditionName())) {
					return fb;
				}
			}
		}
		return null;
	}

}
