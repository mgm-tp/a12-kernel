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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICompositeCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.LogOp;

/**
 * Klasse dient dem Sammeln der lokalen Pflichtfeldinformationen zu einer Regel. Da eine Regel durch
 * eine bzw. mehrere Oder Verknüpfungen auch mehrere Pflichtfeldinfos modellieren kann, wird dazu
 * eine Visitorklasse implementiert. <br>
 * Aus einer Bedingung wird eine (oder mehrere) Pflichtfeldinformation(en) ermittelt, wenn sie der
 * Form <br>
 * MindestensEinFeldAngegegebn(A1,...,An) Und NichtAlleFelderOderKeinFeldAngegeben(P1,..,Pm) <br>
 * ist oder wenn es eine zusammengesetzte Oder-Bedingung ist, bei der mindestens ein Zweig die obige
 * Form hat.
 */
public class CollectorFuerLokalePflichtFeldInfo extends PflichtInfoCollector {
	private static final Logger logger = LoggerFactory.getLogger(CollectorFuerLokalePflichtFeldInfo.class);

	public CollectorFuerLokalePflichtFeldInfo(RegelInfo regelInfo) {
		this.iterationsIntervall = regelInfo.getIterationsIntervall();
		this.regelname = regelInfo.getRegelName();
	}

	void createAndAddPflichtInfo(ICompositeCondition b) {
		PflichtInformation info = createInfoFromZusammengesetzteBedingung(b);
		if (info != null) {
			info.setRegelName(regelname);
			logger.debug("Fuege lokale Pflichtfeldinformation hinzu: " + info);
			infos.add(info);
		}

	}

	/**
	 * Für eine zusammengesetzte Bedingung der Form
	 * <ul>
	 * <li>MindestensEinFeldAngegeben(feldListe1) Und
	 * NichtAlleFelderOderKeinFeldAngegeben(feldListe2), oder
	 * <li>NichtAlleFelderOderKeinFeldAngegeben(feldListe2) Und
	 * MindestensEinFeldAngegeben(feldListe1)
	 * </ul>
	 * werden die Felder von feldListe1 zu den additivFeldern, die von feldListe2 zu den
	 * pflichtFeldern.<br>
	 * Ist die Bedingung von einer dieser Formen, so wird die entsprechende
	 * {@link PflichtInformation} zurückgegeben, sonst {@code null}.
	 *
	 * @param b
	 * 		- die zu untersuchende Bedingung
	 * @return die erzeugte {@link PflichtInformation} oder {@code null}, wenn aus der
	 * 		Bedingung keine {@link PflichtInformation} erzeugt werden kann
	 */
	private PflichtInformation createInfoFromZusammengesetzteBedingung(ICompositeCondition bedingung) {
		List<IEntityListCondition> bedingungen = getAdditivUndPflichtfeldBedingung(bedingung);

		//falsche Form, kann kein lokales Pflichtfeld erzeugen.
		if (bedingungen == null) {
			return null;
		}

		//sortiere additivBedingung
		IEntityListCondition additivBed = bedingungen.get(0);
		IEntityListCondition pflichtBedingung = bedingungen.get(1);

		if (!EbenenChecker.checkEbenen(additivBed)) {
			return null;
		}

		return createInfoZuAdditivUndPflichtBedingung(additivBed, pflichtBedingung);
	}

	private PflichtInformation createInfoZuAdditivUndPflichtBedingung(IEntityListCondition additivBedingung,
			IEntityListCondition pflichtBedingung) {
		Set<PflichtIdentifier> add = createPflichtIdentifierMenge(additivBedingung);
		Set<PflichtIdentifier> pfl = createPflichtIdentifierMenge(pflichtBedingung);
		if (pfl.size() == 1) {
			// behandelt den Fall MindestensEinFeldAngegeben(a,b,..) Und FeldNichtAngegeben(c)
			// in diesem Fall wird c der Additivmenge hinzugefügt, weil die Bedingung äquivalent ist zu
			// MindestensEinFeldAngegeben(a,b,c,..) Und FeldNichtAngegeben(c)
			// damit muss die Additivmenge vergrößert werden
			add.addAll(pfl);
		}
		return PflichtInformation.createLokalePflichtFeldInformation(add, pfl, iterationsIntervall);
	}

	/**
	 * Hier erfolgt die eigentliche Analyse der Bedingung. Ist bed eine zusammengesetzte Bedingung
	 * der Form MindestensEinFeldAngegeben(A,B) Und P mit
	 * <ul>
	 * <li>P = NichtAlleFelderOderKeinFeldAngegeben(C,D) bzw.</li>
	 * <li>P = NichtAlleFelderOderKeinFeldAngegeben(C,D) Oder ...</li>
	 * </ul>
	 * so wird eine zweielementige Liste zurückgegeben, deren erstes Element
	 * MindestensEinFeldAngegeben ist und das zweite Element NichtAlleFelderOderKeinFeldAngegeben.
	 * Sonst wird {@code null} zurückgegeben.
	 *
	 * @param bed
	 * 		- die zu untersuchende Bedingung
	 * @return {@code null} oder Ergebnisliste.
	 */
	private List<IEntityListCondition> getAdditivUndPflichtfeldBedingung(ICompositeCondition bed) {

		if (!isKanonischeForm(bed)) {
			return null;
		}
		List<ICondition> teilBedingungen = bed.getConditionList();
		IEntityListCondition additivBed = null;
		IEntityListCondition pflichtBed = null;

		for (int i = 0; i < 2; i++) {
			ICondition teilBed = teilBedingungen.get(i);
			if (teilBed instanceof IEntityListCondition) {
				IEntityListCondition feldListBed = (IEntityListCondition) teilBed;
				if (MIND_EIN_FELD.equals(feldListBed.getConditionName())) {
					additivBed = feldListBed;
					continue;
				}
				if (NICHT_ALLE_FELDER.equals(feldListBed.getConditionName())) {
					pflichtBed = feldListBed;
					continue;
				}
				return null;
			}
			if (teilBed instanceof ICompositeCondition) {
				ICompositeCondition zusGesBed = (ICompositeCondition) teilBed;
				if (LogOp.And.equals(zusGesBed.getLogOp())) {
					return null;
				}
				pflichtBed = getPflichtFeldBed(zusGesBed);

				if (pflichtBed == null) {
					return null;
				}
			}
		}
		/**
		 * Es wird überprüft, dass eine Bedingung die Additiv-Bedingung ist und die andere die
		 * Zusatzbedingung.
		 */
		if (additivBed == null || pflichtBed == null) {
			return null;
		}
		return Arrays.asList(additivBed, pflichtBed);

	}

	/**
	 * Gibt {@code true} zurück, wenn die zusammengesetzte Bedingung eine Und-Bedingung ist
	 * bestehend aus genau zwei Teilbedingungen, von denen mindestens eine eine
	 * {@link IEntityListCondition} ist.
	 *
	 * @param b
	 * 		- die zu untersuchende Bedingung
	 * @return {@code true}, wenn die zusammengesetzte Bedingung kanonische Form hat
	 */
	private boolean isKanonischeForm(ICompositeCondition b) {
		if (b.getLogOp().equals(LogOp.Or)) {
			return false;
		}

		List<ICondition> bedingungen = b.getConditionList();
		return (bedingungen.size() == 2 && ((bedingungen.get(0) instanceof IEntityListCondition) || (bedingungen.get(
				1) instanceof IEntityListCondition)));
	}

	/**
	 * Gibt die erste Teilbedingung der zusammengesetzten Bedingung bed zurück, die eine
	 * NichtAlleFelderOderKeinFeldAngegeben-Bedingung ist, falls eine solche Teilbedingung existiert
	 * und null sonst.
	 *
	 * @param bed
	 * 		- die zu untersuchende Bedingung
	 * @return die erste NichtAlleFelderOderKeinFeldAngegeben-Teilbedingung bzw. {@code null},
	 * 		wenn es keine solche Teilbedingung gibt.
	 */
	private IEntityListCondition getPflichtFeldBed(ICompositeCondition bed) {
		List<ICondition> teilBedingungen = bed.getConditionList();
		for (ICondition b : teilBedingungen) {
			if (b instanceof IEntityListCondition) {
				IEntityListCondition fb = (IEntityListCondition) b;
				if (NICHT_ALLE_FELDER.equals(fb.getConditionName())) {
					return fb;
				}
			}
		}
		return null;
	}

}
