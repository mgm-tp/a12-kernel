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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.common.VordruckIntern;
import com.mgmtp.a12.kernel.core.rt.a12internal.validation.IIdentifier;
import com.mgmtp.a12.kernel.core.service.internal.mandatoryinformation.PflichtInformation.InfoTyp;

/**
 * Verdichtet die verschiedenen lokalen Pflichtfeld- bzw. Pflichtmengeninformationen. Der
 * Algorithmus besteht aus folgenden Schritten:
 *
 * <ul>
 * <li>Je zwei lokale Pflichtfeldinformationen (A1, P1), (A2, P2) werden miteinander verdichtet,
 * wenn einer der Fälle zutrifft:
 * <ul>
 * <li>(a1) A1 = A2: Dann wird P1 = P1 vereinigt mit P2 gesetzt und die 2. Information gelöscht
 * <li>(a2) A1 Teilmenge von A2: Dann wird P1 = P1 vereinigt mit P2 gesetzt
 * <li>(a3) P1 = P2 : Dann wird A1 = A1 vereinigt mit A2 gesetzt und die 2. Information gelöscht
 * <li>(a4) P1 Teilmenge von P2: Dann wird A1 = A1 vereinigt mit A2 gesetzt
 * <li>(a5) P1 geschnitten mit A2 ist nicht leer: Dann wird P1 = P1 vereinigt mit P2 gesetzt
 * <li>(a6) Pi geschnitten Pj nicht leer: neue Pflichtfeldinfo (Ai vereinigt Aj, Pi geschnitten Pj)
 * </ul>
 * <li>Für eine lokale Pflichtfeldinformation (A1, P1) und eine lokale Pflichtmengeninformation (A2,
 * M2) sind folgende Fälle von Interesse:
 * <ul>
 * <li>(b1) M2 Teilmenge von A1: Dann wird A1 = A1 vereinigt mit A2 gesetzt
 * <li>(b2) A2 geschnitten mit P1 ist nicht leer: Dann wird A2 = A2 vereinigt mit A1 gesetzt
 * <li>(b3) A2 Teilmenge von A1 und P1 geschnitten mit M2 ist nicht leer: (A2, M2) wird gelöscht
 * </ul>
 * <li>Je zwei Pflichtmengeninformationen (A1, M1), (A2, M2) werden miteinander in diesen Fällen
 * verdichtet:
 * <ul>
 * <li>(c1) M1 = M2: Dann wird A1 = A1 vereinigt mit A2 gesetzt und die 2. Information gelöscht
 * <li>(c2) M1 Teilmenge von M2: Dann wird A2 = A1 vereinigt mit A2 gesetzt
 * <li>(c3) M2 Teilmenge von A1: Dann wird A1 = A1 vereinigt mit A2 gesetzt
 * </ul>
 * <li>Für Vordruckpflichtangaben werden folgende Situationen behandelt:
 * <ul>
 * <li>(d1) (V, P1) Vordruckpflichtfeldinfo und (A2, P2) lokale Pflichtfeldinfo mit P1 geschnitten
 * mit A2 nicht leer: Dann wird P1 = P1 vereinigt mit P2 gesetzt.
 * <li>(d2) (V, M1) Vordruckpflichtmengeninfo und (A2, P2) lokale Pflichtfeldinfo mit M1 Teilmenge
 * von A2. Dann wird P2 den Pflichtfeldern von V hinzugefügt, falls eine solche
 * Vordruckpflichtfeldinfo existiert. Sonst wird eine solche Info erzeugt.
 * <li>(d3) (V,P1) Vordruckpflichtfeldinfo und (A2, M2) lokale Pflichtmengeninfo mit P1 geschnitten
 * A2 nicht leer. Dann wird eine neue Vordruckpflichtmengeninfo (V, M2) erzeugt.
 * <li>(d4) (V, M1) Vordruckpflichtmengeninfo und (A2, M2) lokale Pflichtmengeninfo mit M1 Teilmenge
 * von A2. Dann wird eine neue Vordruckpflichtmengeninfo (V, M2) erzeugt.
 * </ul>
 * </ul>
 */
public class LokalePflichtInfoVerdichter {

	private static final Logger logger = LoggerFactory.getLogger(LokalePflichtInfoVerdichter.class);

	boolean aenderungenVorgenommen = false;
	LokalePflichtInfoComparator comp = new LokalePflichtInfoComparator();

	private LinkedHashSet<PflichtInformation> sort(Set<PflichtInformation> infos) {
		macheRegelnamenEindeutig(infos);
		SortedSet<PflichtInformation> sortiert = new TreeSet<>(comp);
		sortiert.addAll(infos);
		assert sortiert.size() == infos.size() : "number of sorted infos != number of unsorted infos.";
		logger.debug(String.format("sortiert = %d ursprünglich %d", sortiert.size(), infos.size()));
		return new LinkedHashSet<>(sortiert);
	}

	/**
	 * Mache die Regelnamen für jede Pflichtinfo eindeutig und verdichte auf jeweils
	 * nur einen Regelnamen. Dies ist ein Vorbereitungsschritt für die spätere sortierung.
	 * Die Sortierung erfolgt nach dem Regelnamen pro PflichtInformation. Daher muss die
	 * Menge der Regelnamen auf ein element reduziert werden und die Regelnamen eindeutig über die
	 * Menge der PflichtInformation sein.
	 *
	 * @param infos
	 * 		Menge der PflichtInformation, die sortiert werden soll.
	 */
	private void macheRegelnamenEindeutig(Set<PflichtInformation> infos) {
		Set<String> regelNamen = new HashSet<>();
		for (PflichtInformation info : infos) {
			StringBuilder regelName = new StringBuilder(info.getRegelName());
			if (!regelNamen.add(regelName.toString())) {
				logger.debug("Regel erzeugt mehrere Pflichtinfos: " + regelName.toString());
				regelName.append("_1");
				while (!regelNamen.add(regelName.toString())) {//die Regel erzeugt sogar mehr als zwei Infos
					regelName.append("_1");
				}
				info.setRegelName(regelName.toString());
			}
		}
	}

	/**
	 * verdichte die Information zu dne Pflichtangaben.
	 *
	 * @param pflichtAngaben
	 * 		Angaben die sortiert werden sollen.
	 * @param pflichtVordrucke
	 */
	public void verdichteInfos(PflichtAngaben pflichtAngaben, Set<VordruckIntern> pflichtVordrucke) {

		//sortiere die Angaben vor dem Verdichten,  damit die analyse bei jedem durchgang in gleicher Reihenfolge
		//erfolgt.  Dies ist notwendig,  um eine eventuelle Fehleranalyse zu ermöglichen.
		sortInfos(pflichtAngaben);

		Set<PflichtInformation> pflichtFeldInfos = pflichtAngaben.getLokalePflichtFelder();
		Set<PflichtInformation> pflichtMengenInfos = pflichtAngaben.getLokalePflichtMengen();
		Set<PflichtInformation> vordruckPflichtMengenInfos = pflichtAngaben.getVordruckPflichtMengen();
		Set<PflichtInformation> vordruckPflichtFeldInfos = pflichtAngaben.getVordruckPflichtFelder();

		Set<PflichtInformation> globalePflichtMengenInfos = pflichtAngaben.getGlobalePflichtMengenInfos();
		Set<PflichtInformation> globalePflichtfelder = pflichtAngaben.getGlobalePflichtfelder();

		logger.debug(pflichtFeldInfos.size() + " pfi, " + pflichtMengenInfos.size() + " pmi, "
				+ vordruckPflichtMengenInfos.size() + " vpm, " + vordruckPflichtFeldInfos.size() + " vpf");

		Set<String> mandatoryRootGroups = pflichtVordrucke.stream()
				// Take only root groups that are mandatory for all repetitions (and don't use usb)
				.filter(v -> v.getLfdNummer() == IIdentifier.ALL_INDICES && v.getUsb() == IIdentifier.USB_NOT_SPECIFIED)
				.map(VordruckIntern::getName)
				.collect(Collectors.toSet());

		copyMandatoryRootGroupInformationToGlobal(mandatoryRootGroups, vordruckPflichtMengenInfos, globalePflichtMengenInfos,
				mi -> PflichtInformation.createGlobalePflichtMengenInformation(mi.getpMenge(), mi.getIterationsIntervall()));
		copyMandatoryRootGroupInformationToGlobal(mandatoryRootGroups, vordruckPflichtFeldInfos, globalePflichtfelder,
				mi -> PflichtInformation.createPflichtfeldInformation(mi.getpMenge(), mi.getIterationsIntervall()));

		//Verdichte Pflichtangaben solange noch etwas geändert wird
		boolean geaendert = true;
		while (geaendert) {
			geaendert = false;
			if (verdichtePflichtFeldInfo(pflichtFeldInfos)) {
				geaendert = true;
			}
			if (verdichtePflichtMengenInfo(pflichtMengenInfos)) {
				geaendert = true;
			}
			if (verdichtePflichtFeldMitPflichtMengenInfo(pflichtFeldInfos, pflichtMengenInfos)) {
				geaendert = true;
			}
		}
		// verdichte VordruckPflichtangaben solange noch etwas geändert wird
		geaendert = true;
		while (geaendert) {
			geaendert = false;
			if (verdichteVordruckPflichtFeldInfoMitLokalerPflichtFeldInfo(vordruckPflichtFeldInfos, pflichtFeldInfos)) {
				geaendert = true;
			}

			if (verdichteVordruckPflichtFeldInfoMitLokalerPflichtMengenInfo(vordruckPflichtFeldInfos,
					pflichtMengenInfos, vordruckPflichtMengenInfos)) {
				geaendert = true;
			}
			if (verdichteVordruckPflichtMengenInfoMitLokalerPflichtFeldInfo(vordruckPflichtMengenInfos,
					pflichtFeldInfos, vordruckPflichtFeldInfos)) {
				geaendert = true;
			}
			if (verdichteVordruckPflichtMengenInfoMitLokalerPflichtMengenInfo(vordruckPflichtMengenInfos, pflichtMengenInfos)) {
				geaendert = true;
			}
		}
		// verdichte globale Pflichtangaben
		geaendert = true;
		while (geaendert) {
			geaendert = false;
			if (verdichteGlobalePflichtMengenInfoMitLokalerPflichtMengenInfo(globalePflichtMengenInfos, pflichtMengenInfos)) {
				geaendert = true;
			}
			if (verdichteGlobalePflichtMengenInfoMitLokalerPflichtFeldInfo(globalePflichtMengenInfos, pflichtFeldInfos,
					globalePflichtfelder)) {
				geaendert = true;
			}
			if (verdichteGlobalePflichtFeldInfoMitLokalerPflichtMengenInfo(globalePflichtfelder, pflichtMengenInfos,
					globalePflichtMengenInfos)) {
				geaendert = true;
			}
			if (verdichteGlobalePflichtFeldInfoMitLokalerPflichtFeldInfo(globalePflichtfelder, pflichtFeldInfos)) {
				geaendert = true;
			}

		}

		addPflichtfelderZuVordruckpflichtfelder(vordruckPflichtFeldInfos, globalePflichtfelder);

		//nach der Verdichtung sortieren wir alles nochmal, da evtl. neue Infos dazugekommen sind
		sortInfos(pflichtAngaben);
	}

	private void copyMandatoryRootGroupInformationToGlobal(Set<String> mandatoryRootGroups,
			Set<PflichtInformation> rootGroupMandatoryInfos,
			Set<PflichtInformation> globalMandatoryInfos,
			UnaryOperator<PflichtInformation> convert) {
		for (PflichtInformation rootGroupMandatoryInfo : rootGroupMandatoryInfos) {
			if (mandatoryRootGroups.contains(rootGroupMandatoryInfo.getVordruckName())) {
				PflichtInformation globalMandatoryInfo = convert.apply(rootGroupMandatoryInfo);
				if (!VerdichtungsUtil.infoIstRedundantInInfos(globalMandatoryInfo, globalMandatoryInfos)) {
					globalMandatoryInfos.add(globalMandatoryInfo);
				}
			}
		}
	}

	private void sortInfos(PflichtAngaben pflichtAngaben) {
		LinkedHashSet<PflichtInformation> pflichtFeldInfos = sort(pflichtAngaben.getLokalePflichtFelder());
		pflichtAngaben.setLokalePflichtFelder(pflichtFeldInfos);

		LinkedHashSet<PflichtInformation> pflichtMengenInfos = sort(pflichtAngaben.getLokalePflichtMengen());
		pflichtAngaben.setLokalePflichtMengen(pflichtMengenInfos);

		LinkedHashSet<PflichtInformation> vordruckPflichtMengenInfos = sort(pflichtAngaben.getVordruckPflichtMengen());
		pflichtAngaben.setVordruckPflichtMengen(vordruckPflichtMengenInfos);

		LinkedHashSet<PflichtInformation> vordruckPflichtFeldInfos = sort(pflichtAngaben.getVordruckPflichtFelder());
		pflichtAngaben.setVordruckPflichtFelder(vordruckPflichtFeldInfos);

		LinkedHashSet<PflichtInformation> globalePflichtMengenInfos = sort(pflichtAngaben.getGlobalePflichtMengenInfos());
		pflichtAngaben.setGlobalePflichtMengenInfos(globalePflichtMengenInfos);

		LinkedHashSet<PflichtInformation> globalePflichtfelder = sort(pflichtAngaben.getGlobalePflichtfelder());
		pflichtAngaben.setGlobalePflichtfelder(globalePflichtfelder);

	}

	/**
	 * Verdichte die PflichtfeldInfo
	 *
	 * @param infos
	 * 		Menge an lokalen PflichtfeldInformationen
	 * @return gibt true zurück, wenn sich etwas geändert hat.
	 */
	private boolean verdichtePflichtFeldInfo(Set<PflichtInformation> infos) {
		boolean result = false; //speichert das Rückgabeergebnis
		loescheRedundanteInfos(infos); // lösche redundante Informationen
		boolean aenderungen = true;
		while (aenderungen) { // solange sich bei einem Schleifendruchgang etwas geändert hat, prüfe erneut
			aenderungen = false;
			if (verdichteWegenAdditivMengenEigenschaften(infos)) {
				aenderungen = true;
			}
			if (verdichteWegenPflichtfeldMengenEigenschaften(infos)) {
				aenderungen = true;
			}
			if (verdichteWegenAdditivPflichtfelderMengenEigenschaft(infos)) {
				aenderungen = true;
			}
			if (aenderungen) {
				result = true; // es hat sich mal was geändert,  gebe true zurück
			}
		}
		return result;
	}

	/**
	 * Informationen können redundant sein, wenn eine Information bereits eine andere umfasst. Diese
	 * sollen gelöscht werden, um die Mengen, auf denen operiert wird möglichst klein zu halten.
	 *
	 * @param infos
	 * 		Menge der Pflichtinformationen, in dieser werden die redundaten Angaben gelöscht.
	 */
	private void loescheRedundanteInfos(Set<PflichtInformation> infos) {

		PflichtInformation[] copyZumIterieren = infos.toArray(new PflichtInformation[infos.size()]);

		// Teste jede Information mit jeder,  daher geschachtelte Schleife.
		for (int i = 0; i < copyZumIterieren.length; i++) {
			PflichtInformation iInfo = copyZumIterieren[i];

			for (int j = i + 1; j < copyZumIterieren.length; j++) {
				PflichtInformation jInfo = copyZumIterieren[j];
				if (!infos.contains(iInfo)) {
					break;
				}
				if (!infos.contains(jInfo)) {
					continue;
				}

				if (jInfo.umfasstInfo(iInfo)) {// jInfo beschreibt bereits alle Information von iInfo,
					boolean res = infos.remove(iInfo);
					assert res;
					logger.debug("lösche redundante Info 1 ");
				}
				if (iInfo.umfasstInfo(jInfo)) {
					boolean res = infos.remove(jInfo);
					assert res;
					logger.debug("lösche redundante Info 2 ");
				}
			}
		}
	}

	/**
	 * Methode realisiert den Schritt 1 und 2 des Verdichtungsalgorithmus (siehe Konzept) Jede
	 * Information wird mit jeder Information verglichen. Durch das Vereinigen der PMengen ist es
	 * möglich, dass eine LokalePflichtInformation dann doppelt enthalten ist, und somit nicht mehr
	 * in der Menge vorkommt. Da dies bei jedem Iterationsschritt möglich ist, muss immer geprüft
	 * werden, ob iInfo bzw. jInfo noch in der zu analyiserenden Menge enthalten ist.
	 *
	 * @param infos
	 * 		Menge der zu analysierenden Pflichtinformation
	 * @return true, falls sich etwas geändert hat, false sonst.
	 */
	private boolean verdichteWegenAdditivMengenEigenschaften(Set<PflichtInformation> infos) {

		aenderungenVorgenommen = false; // globale Variable wird in vereinigePMengen gesetzt, falls nötig
		PflichtInformation[] copyZumIterieren = infos.toArray(new PflichtInformation[infos.size()]);

		for (int i = 0; i < copyZumIterieren.length; i++) {
			PflichtInformation iInfo = copyZumIterieren[i];

			for (int j = i + 1; j < copyZumIterieren.length; j++) {
				PflichtInformation jInfo = copyZumIterieren[j];
				//Da als Seiteneffekt iInfo oder jInfo in den unteren Bearbeitungsschritten entfernt
				//worden sein können,  muss hier zunächst getestet werden,  ob diese noch in der
				//Ausgangsmenge enthalten sind. Da immer nur ein Bearbeitungsschritt in einem
				//Schleifendurchgang erfolgt,  muss zwischendrin nicht mehr auf contains getestet werden.
				if (!infos.contains(iInfo)) {
					break; // wurde bereits gelöscht,
				}
				if (!infos.contains(jInfo)) {
					continue; // wurde bereits gelöscht
				}

				if (testeAdditivMengenGleich(iInfo, jInfo)) {//Konzept Schritt 1
					int size = infos.size();
					// es wurde geprüft ob jInfo enthalten ist,  daher muss remove immer einen Effekt haben.
					infos.remove(jInfo); //jInfo wird gelöscht, weil verdichtet.
					assert infos.size() == size - 1; //hier war ein hot-spot der Fehler EXT-4881,
					// daher werden die remove-Operationen per assertion abgesichert
					vereinigePMengen(infos, iInfo, jInfo);
					continue;
				}

				//Konzept Schritt 2:  beide Richtungen werden geprüft
				if (testeAdditivMengenSindTeilmengen(iInfo, jInfo)) {//Konzept Schritt 2
					vereinigePMengen(infos, iInfo, jInfo);
					continue;
				}

				if (testeAdditivMengenSindTeilmengen(jInfo, iInfo)) {//Konzept Schritt 2
					vereinigePMengen(infos, jInfo, iInfo);
				}
			}
		}

		return aenderungenVorgenommen;
	}

	/**
	 * Hilfsmethode zum Vereinigen der PMengen: Der HashCode von iInfo verändert, wenn sich die
	 * PMenge durch die Vereinigung wirklich verändert. Um den HashSet trotzdem weiter verwenden zu
	 * können, wird zunächst iInfo entfernt und dann die geänderte iInfo wieder in die Menge
	 * eingefügt.
	 *
	 * @param infos
	 * 		zu analysierende Menge
	 * @param iInfo
	 * 		information, zu der die zusätzliche PMenge hinzugefügt wird
	 * @param jInfo
	 * 		andere Pflichtinformation
	 */
	private void vereinigePMengen(Set<PflichtInformation> infos,
			PflichtInformation iInfo, PflichtInformation jInfo) {
		if (!iInfo.getpMenge().containsAll(jInfo.getpMenge())) { //es muss wirklich was geändert werden
			int size = infos.size();
			// es ist sichergestellt, dass iInfo in der Menge enthalten ist.
			// in EXT-4881 war das remove ein hot-spot der Fehler,  daher wird dies mit assertions
			// abgesichert.
			boolean res = infos.remove(iInfo);
			assert res;
			assert infos.size() == size - 1;
			iInfo.getpMenge().addAll(jInfo.getpMenge()); //Pi = Pi vereinigt mit Pj
			iInfo.setRegelName(iInfo.getRegelName() + "&" + jInfo.getRegelName());

			//Nur wenn die geänderte Information nicht redundant ist füge sie hinzu
			if (!VerdichtungsUtil.infoIstRedundantInInfos(iInfo, infos)) {
				// Durch die Änderung von iInfo können andere Informationen redundant geworden sein.
				// diese sollen gelöscht werden.
				loescheRedundanteInfos(iInfo, infos);
				aenderungenVorgenommen = true;
				res = infos.add(iInfo);
				assert res;
				logger.debug(infos.size() + " = add info in vereinigeP");
			}
		}
	}

	/**
	 * Löscht aus der Menge infos alle LokalePflichtInformationen, die redundant sind in Bezug auf
	 * neueInfo, d. h. die von neueInfo "umfasst" sind.
	 */
	private void loescheRedundanteInfos(PflichtInformation neueInfo,
			Collection<PflichtInformation> infos) {
		PflichtInformation[] copyZumIterieren = infos.toArray(new PflichtInformation[infos.size()]);
		for (int i = 0; i < copyZumIterieren.length; i++) {
			PflichtInformation iInfo = copyZumIterieren[i];
			if (neueInfo.umfasstInfo(iInfo)) {
				boolean res = infos.remove(iInfo);
				assert res;
				logger.debug("Redundant gewordene Info gelöscht.");
			}
		}
	}

	/**
	 * Schritt 3, 4 und 6 des Verdichtungsalgorithmuss
	 *
	 * @param infos
	 * 		zu analysierende Informationsmenge
	 * @return true, falls Änderungen vorgenommen wurden.
	 */
	private boolean verdichteWegenPflichtfeldMengenEigenschaften(
			Set<PflichtInformation> infos) {
		aenderungenVorgenommen = false;

		PflichtInformation[] copyZumIterieren = infos.toArray(new PflichtInformation[infos.size()]);

		for (int i = 0; i < copyZumIterieren.length; i++) {
			PflichtInformation iInfo = copyZumIterieren[i];

			for (int j = i + 1; j < copyZumIterieren.length; j++) {
				PflichtInformation jInfo = copyZumIterieren[j];
				//Da als Seiteneffekt iInfo oder jInfo in den unteren Bearbeitungsschritten entfernt
				//worden sein können,  muss hier zunächst getestet werden,  ob diese noch in der
				//Ausgangsmenge enthalten sind. Da immer nur ein Bearbeitungsschritt in einem
				//Schleifendurchgang erfolgt,  muss zwischendrin nicht mehr auf contains getestet werden.
				if (!infos.contains(iInfo)) {
					break;
				}
				if (!infos.contains(jInfo)) {
					continue;
				}

				//Konzept Schritt 3
				if (testePflichtfeldMengenGleich(iInfo, jInfo)) {//Konzept Schritt 3
					assert !iInfo.equals(jInfo) : iInfo + " " + jInfo;
					int size = infos.size();
					boolean res = infos.remove(jInfo);
					assert res;
					assert infos.size() == size - 1;
					vereinigeAMengen(infos, iInfo, jInfo);
					continue;
				}

				//Konzept Schritt 4
				if (testePflichtfeldMengenSindTeilmengen(iInfo, jInfo)) {//Konzept Schritt 4
					vereinigeAMengen(infos, iInfo, jInfo);
					continue;
				}
				if (testePflichtfeldMengenSindTeilmengen(jInfo, iInfo)) {//Konzept Schritt 4
					vereinigeAMengen(infos, jInfo, iInfo);
					continue;
				}

				if (testPMengenHabenNichtLeerenSchnitt(iInfo, jInfo)) {//Konzept Schritt 6
					PflichtInformation neueInfo = VerdichtungsUtil.createNeueLokalePflichtInfo(iInfo, jInfo,
							InfoTyp.LokalePflichtfeldInfo);

					neueInfo.getAdditivMenge().addAll(jInfo.getAdditivMenge());
					Set<PflichtIdentifier> pSchnittMenge = VerdichtungsUtil.berechneSchnittMenge(jInfo.getpMenge(),
							iInfo.getpMenge());
					neueInfo.setpMenge(pSchnittMenge);
					neueInfo.setRegelName(iInfo.getRegelName());

					if (!VerdichtungsUtil.infoIstRedundantInInfos(neueInfo, infos)) {
						loescheRedundanteInfos(neueInfo, infos);
						logger.debug(infos.size() + "  neue info");
						boolean res = infos.add(neueInfo);
						assert res;
						aenderungenVorgenommen = true;
					}
				}
			}
		}

		return aenderungenVorgenommen;
	}

	/**
	 * Hilfsmethode zum Vereinigen der AMengen: Der HashCode von iInfo verändert, wenn sich die
	 * AMenge durch die Vereinigung wirklich verändert. Um den HashSet trotzdem weiter verwenden zu
	 * können, wird zunächst iInfo entfernt und dann die geänderte iInfo wieder in die Menge
	 * eingefügt.
	 *
	 * @param infos
	 * 		zu analysierende Menge
	 * @param iInfo
	 * 		information, zu der die zusätzliche AMenge hinzugefügt wird
	 * @param jInfo
	 * 		andere Pflichtinformation
	 */
	private void vereinigeAMengen(Set<PflichtInformation> infos,
			PflichtInformation iInfo, PflichtInformation jInfo) {
		if (!iInfo.getAdditivMenge().containsAll(jInfo.getAdditivMenge())) {
			int size = infos.size();
			boolean res = infos.remove(iInfo);
			assert res;
			assert infos.size() == size - 1;
			iInfo.getAdditivMenge().addAll(jInfo.getAdditivMenge());// Ai = Ai vereinigt mit Aj
			iInfo.setRegelName(iInfo.getRegelName() + "&" + jInfo.getRegelName());
			if (!VerdichtungsUtil.infoIstRedundantInInfos(iInfo, infos)) {
				loescheRedundanteInfos(iInfo, infos);
				res = infos.add(iInfo);
				logger.debug(infos.size() + " add info in vereinige A");
				assert res;
				aenderungenVorgenommen = true;
			}
		}
	}

	/**
	 * Schritt 5 des Algorithmus
	 *
	 * @param infos
	 * 		zu analysierende Informationsmenge
	 * @return true, falls Änderungen vorgenommen wurden
	 */
	private boolean verdichteWegenAdditivPflichtfelderMengenEigenschaft(
			Set<PflichtInformation> infos) {
		//Konzept Schritt 5
		aenderungenVorgenommen = false;
		PflichtInformation[] copyZumIterieren = infos.toArray(new PflichtInformation[infos.size()]);

		for (int i = 0; i < copyZumIterieren.length; i++) {
			PflichtInformation iInfo = copyZumIterieren[i];

			for (int j = i + 1; j < copyZumIterieren.length; j++) {
				PflichtInformation jInfo = copyZumIterieren[j];
				behandleAdditivHatNichtLeerenSchnittMitPMenge(infos, iInfo, jInfo);
				behandleAdditivHatNichtLeerenSchnittMitPMenge(infos, jInfo, iInfo);

			}
		}
		return aenderungenVorgenommen;
	}

	/**
	 * Aj geschnitten Pi nicht leer -> Pi = Pi vereinigt Pj
	 */
	private void behandleAdditivHatNichtLeerenSchnittMitPMenge(
			Set<PflichtInformation> infos,
			PflichtInformation iInfo, PflichtInformation jInfo) {
		//falls bereits in einem vorherigen Schritt gelöscht,  tu nix.
		if (!infos.contains(iInfo) || !infos.contains(jInfo)) {
			return;
		}

		if (testAdditivHatNichtLeerenSchnittMitPflichtfeldMenge(iInfo, jInfo)
				&& !iInfo.getpMenge().containsAll(jInfo.getpMenge())) {
			vereinigePMengen(infos, iInfo, jInfo);
		}
	}

	/**
	 * @param iInfo
	 * 		- lokale Pflichmengeninfo (Ai, Pi)
	 * @param jInfo
	 * 		- lokale Pflichmengeninfo (Aj, Pj)
	 * @return true, falls Aj geschnitten Pi nicht leer und der Musterbereich von jInfo den von
	 * 		iInfo umfasst
	 */
	private boolean testAdditivHatNichtLeerenSchnittMitPflichtfeldMenge(
			PflichtInformation iInfo, //(Ai, Pi)
			PflichtInformation jInfo) { //(Aj, Pj)

		if (!jInfo.getIterationsIntervall().umfasst(iInfo.getIterationsIntervall())) {
			return false;
		}
		return !VerdichtungsUtil.schnittMengeIstLeer(jInfo.getAdditivMenge(), iInfo.getpMenge());
	}

	/**
	 * @param iInfo
	 * 		- lokale Pflichmengeninfo (Ai, Pi)
	 * @param jInfo
	 * 		- lokale Pflichmengeninfo (Aj, Pj)
	 * @return true, falls Pi geschnitten Pj nicht leer ist und iInfo und jInfo die gleichen
	 * 		Musterbereiche haben
	 */
	private boolean testPMengenHabenNichtLeerenSchnitt(
			PflichtInformation iInfo,
			PflichtInformation jInfo) {
		if (!iInfo.getIterationsIntervall().equals(jInfo.getIterationsIntervall())) {
			return false;
		}
		return !VerdichtungsUtil.schnittMengeIstLeer(jInfo.getpMenge(), iInfo.getpMenge());
	}

	/**
	 * @param iInfo
	 * 		- lokale Pflichmengeninfo (Ai, Pi)
	 * @param jInfo
	 * 		- lokale Pflichmengeninfo (Aj, Pj)
	 * @return true, falls Pi = Pj ist und iInfo und jInfo die gleichen Musterbereiche haben
	 */
	private boolean testePflichtfeldMengenGleich(
			PflichtInformation iInfo,
			PflichtInformation jInfo) {

		if (!iInfo.getIterationsIntervall().equals(jInfo.getIterationsIntervall())) {
			return false;
		}
		return iInfo.getpMenge().equals(jInfo.getpMenge());
	}

	/**
	 * @param iInfo
	 * 		- lokale Pflichmengeninfo (Ai, Pi)
	 * @param jInfo
	 * 		- lokale Pflichmengeninfo (Aj, Pj)
	 * @return true, falls Ai = Aj ist und iInfo und jInfo die gleichen Musterbereiche haben
	 */
	private boolean testeAdditivMengenGleich(PflichtInformation iInfo,
			PflichtInformation jInfo) {
		if (!iInfo.getIterationsIntervall().equals(jInfo.getIterationsIntervall())) {
			return false;
		}
		return jInfo.getAdditivMenge().equals(iInfo.getAdditivMenge());//Ai = Aj
	}

	/**
	 * @param iInfo
	 * 		- lokale Pflichmengeninfo (Ai, Pi)
	 * @param jInfo
	 * 		- lokale Pflichmengeninfo (Aj, Pj)
	 * @return true, falls Ai Teilmenge von Aj und der Musterbereich von jInfo den von iInfo umfasst
	 */
	private boolean testeAdditivMengenSindTeilmengen(
			PflichtInformation iInfo,
			PflichtInformation jInfo) {

		if (!jInfo.getIterationsIntervall().umfasst(iInfo.getIterationsIntervall())) {
			return false;
		}
		return jInfo.getAdditivMenge().containsAll(iInfo.getAdditivMenge());
	}

	/**
	 * @param iInfo
	 * 		- lokale Pflichmengeninfo (Ai, Pi)
	 * @param jInfo
	 * 		- lokale Pflichmengeninfo (Aj, Pj)
	 * @return true, falls Pi Teilmenge von Pj ist und der Musterbereich von jInfo den von iInfo
	 * 		umfasst
	 */
	private boolean testePflichtfeldMengenSindTeilmengen(
			PflichtInformation iInfo,
			PflichtInformation jInfo) {

		if (!jInfo.getIterationsIntervall().umfasst(iInfo.getIterationsIntervall())) {
			return false;
		}
		return jInfo.getpMenge().containsAll(iInfo.getpMenge());
	}

	/**
	 * Konzept Schritte 10-12
	 */
	private boolean verdichtePflichtMengenInfo(Set<PflichtInformation> pflichtMengenInfos) {
		boolean aenderungen = true;
		boolean result = false; // gibt true zurück,  wenn irgendeine Änderung erfolgt.
		while (aenderungen) {
			aenderungen = false;

			int count = pflichtMengenInfos.size();
			loescheRedundanteInfos(pflichtMengenInfos);
			if (pflichtMengenInfos.size() < count) {
				logger.debug("in verdichtePflichtMengenInfo vorher " + count + " jetzt " + pflichtMengenInfos.size());
			}

			if (verdichteWegenPflichtmengenEigenschaften(pflichtMengenInfos)) {
				aenderungen = true;
			}
			if (verdichteWegenAdditivUndPflichtmengenEigenschaften(pflichtMengenInfos)) {
				aenderungen = true;
			}

			if (aenderungen) {
				result = true; // es gab Änderungen,  melde nach oben.
			}
		}
		return result;
	}

	private boolean verdichteWegenPflichtmengenEigenschaften(Set<PflichtInformation> infos) {
		aenderungenVorgenommen = false;
		PflichtInformation[] copyZumIterieren = infos.toArray(new PflichtInformation[infos.size()]);

		for (int i = 0; i < copyZumIterieren.length; i++) {
			PflichtInformation iInfo = copyZumIterieren[i];

			for (int j = i + 1; j < copyZumIterieren.length; j++) {
				if (!infos.contains(iInfo)) {
					break; // wurde bereits gelöscht
				}
				PflichtInformation jInfo = copyZumIterieren[j];
				if (!infos.contains(jInfo)) {
					continue; // wurde bereits gelöscht
				}

				//Konzept Schritt 10
				if (testePflichtfeldMengenGleich(iInfo, jInfo)) {
					int size = infos.size();
					infos.remove(jInfo);
					assert infos.size() == size - 1;
					vereinigeAMengen(infos, iInfo, jInfo);
					continue;
				}

				if (testePflichtMengenSindTeilMengen(iInfo, jInfo)) {//Konzept Schritt 11
					vereinigeAMengen(infos, jInfo, iInfo); //Aj = Ai vereinigt Aj
					continue;
				}
				if (testePflichtMengenSindTeilMengen(jInfo, iInfo)) {//Konzept Schritt 11
					vereinigeAMengen(infos, iInfo, jInfo); //Aj = Ai vereinigt Aj
				}
			}
		}
		return aenderungenVorgenommen;

	}

	/**
	 * @param iInfo
	 * 		- lokale Pflichmengeninfo (Ai, Mi)
	 * @param jInfo
	 * 		- lokale Pflichmengeninfo (Aj, Mj)
	 * @return true, falls Mi echte Teilmenge von Mj ist und der Musterbereich von Mi den von Mj
	 * 		umfasst
	 */
	private boolean testePflichtMengenSindTeilMengen(
			PflichtInformation iInfo,
			PflichtInformation jInfo) {

		if (!iInfo.getIterationsIntervall().umfasst(jInfo.getIterationsIntervall())) {
			return false;
		}
		return jInfo.getpMenge().containsAll(iInfo.getpMenge());
	}

	private boolean verdichteWegenAdditivUndPflichtmengenEigenschaften(
			Set<PflichtInformation> infos) {

		aenderungenVorgenommen = false;
		PflichtInformation[] copyZumIterieren = infos.toArray(new PflichtInformation[infos.size()]);

		for (int i = 0; i < copyZumIterieren.length; i++) {
			PflichtInformation iInfo = copyZumIterieren[i];

			for (int j = i + 1; j < copyZumIterieren.length; j++) {
				if (!infos.contains(iInfo)) {
					break; // wurde bereits gelöscht
				}
				PflichtInformation jInfo = copyZumIterieren[j];
				if (!infos.contains(jInfo)) {
					continue; // wurde bereits gelöscht
				}

				if (testePflichtMengeTeilmengeVonAdditiv(iInfo, jInfo)) {//Konzept Schritt 11
					vereinigeAMengen(infos, iInfo, jInfo);
					//hier kein continue, da der nächste Fall auch noch eintreten kann!
				}
				if (testePflichtMengeTeilmengeVonAdditiv(jInfo, iInfo)) {//Konzept Schritt 11
					vereinigeAMengen(infos, jInfo, iInfo);
				}
			}
		}
		return aenderungenVorgenommen;
	}

	/**
	 * @param iInfo
	 * 		- lokale Pflichmengeninfo (Ai, Mi)
	 * @param jInfo
	 * 		- lokale Pflichmengeninfo (Aj, Mj)
	 * @return true, falls Mj Teilmenge von Ai und der Musterbereich von Mj den von Mi umfasst
	 */
	private boolean testePflichtMengeTeilmengeVonAdditiv(
			PflichtInformation iInfo,
			PflichtInformation jInfo) {

		if (!jInfo.getIterationsIntervall().umfasst(iInfo.getIterationsIntervall())) {
			return false;
		}
		return iInfo.getAdditivMenge().containsAll(jInfo.getpMenge());
	}

	/**
	 * Konzept Schritte 7-9
	 */
	private boolean verdichtePflichtFeldMitPflichtMengenInfo(
			Set<PflichtInformation> pflichtFeldInfos,
			Set<PflichtInformation> pflichtMengenInfos) {

		aenderungenVorgenommen = false;
		PflichtInformation[] pflichtFeldInfosZumIterieren = pflichtFeldInfos.toArray(
				new PflichtInformation[pflichtFeldInfos.size()]);
		PflichtInformation[] pflichtMengenInfosZumIterieren = pflichtMengenInfos.toArray(
				new PflichtInformation[pflichtMengenInfos.size()]);

		for (PflichtInformation pfInfo : pflichtFeldInfosZumIterieren) {
			for (PflichtInformation pmInfo : pflichtMengenInfosZumIterieren) {
				if (!pflichtFeldInfos.contains(pfInfo)) {
					break;
				}

				if (!pflichtMengenInfos.contains(pmInfo)) {
					continue;
				}

				if (testeAdditivTeilmengeSchnittVonPflichtangabenNichtLeer(pfInfo, pmInfo)) {//Konzept Schritt 9
					int size = pflichtMengenInfos.size();
					pflichtMengenInfos.remove(pmInfo);
					assert pflichtMengenInfos.size() == size - 1;
					continue;
				}
				if (testePflichtMengeIstTeilmengeVonAdditiv(pfInfo, pmInfo)) {
					//Konzept Schritt 7
					vereinigeAMengen(pflichtFeldInfos, pfInfo, pmInfo);
					continue;
				}
				if (testePflichtFeldMengeHatNichtLeerenSchnittMitAdditiv(pfInfo, pmInfo)) {
					//Konzept Schritt 8
					vereinigeAMengen(pflichtMengenInfos, pmInfo, pfInfo);
				}
			}
		}
		return aenderungenVorgenommen;
	}

	/**
	 * @param pfInfo
	 * 		- (Ai, P)
	 * @param pmInfo
	 * 		- (Aj, M)
	 * @return true, falls Aj Teilmenge von Ai, Pi geschnitten Pj nicht leer ist und die
	 * 		Musterbereiche gleich sind.
	 */
	private boolean testeAdditivTeilmengeSchnittVonPflichtangabenNichtLeer(
			PflichtInformation pfInfo, //(Ai, P)
			PflichtInformation pmInfo//(Aj, M)
	) {

		if (!pfInfo.getIterationsIntervall().equals(pmInfo.getIterationsIntervall())) {
			return false;
		}
		return pfInfo.getAdditivMenge().containsAll(pmInfo.getAdditivMenge()) && //Aj Teilmenge Ai
				!VerdichtungsUtil.schnittMengeIstLeer(pfInfo.getpMenge(), pmInfo.getpMenge());//P geschnitten M nicht leer
	}

	/**
	 * @param pInfo1
	 * 		- (A1, M1)
	 * @param pInfo2
	 * 		- (A2, M2)
	 * @return true, falls M2 Teilmenge von A1 ist und der Musterbereich von pInfo2 den von pInfo1
	 * 		umfasst
	 */
	private boolean testePflichtMengeIstTeilmengeVonAdditiv(
			PflichtInformation pInfo1, //(Ai, M1)
			PflichtInformation pInfo2) { //(Aj, M2)

		if (!pInfo2.getIterationsIntervall().umfasst(pInfo1.getIterationsIntervall())) {
			return false;
		}
		return pInfo1.getAdditivMenge().containsAll(pInfo2.getpMenge());
	}

	/**
	 * @param pfInfo
	 * 		- (Ai, P)
	 * @param pmInfo
	 * 		- (Aj, M)
	 * @return true, falls P geschnitten Aj nicht leer ist und der Musterbereich von pfInfo den von
	 * 		pmInfo umfasst
	 */
	private boolean testePflichtFeldMengeHatNichtLeerenSchnittMitAdditiv(
			PflichtInformation pfInfo, //(Ai, P)
			PflichtInformation pmInfo) { //(Aj, M)

		if (!pfInfo.getIterationsIntervall().umfasst(pmInfo.getIterationsIntervall())) {
			return false;
		}
		return !VerdichtungsUtil.schnittMengeIstLeer(pfInfo.getpMenge(), pmInfo.getAdditivMenge());
	}

	/**
	 * (V,P1) und (A, P2) mit P1 geschnitten A nicht leer -> P1 = P1 vereinigt mit P2 <br>
	 * Konzept Schritt 13
	 */
	private boolean verdichteVordruckPflichtFeldInfoMitLokalerPflichtFeldInfo(
			Set<PflichtInformation> vordruckPflichtFeldInfos,
			Set<PflichtInformation> pflichtFeldInfos) {

		aenderungenVorgenommen = false;
		PflichtInformation[] vordruckPflichtFeldInfosIterieren = vordruckPflichtFeldInfos.toArray(
				new PflichtInformation[vordruckPflichtFeldInfos.size()]);
		PflichtInformation[] pflichtFeldInfosIterieren = pflichtFeldInfos.toArray(new PflichtInformation[pflichtFeldInfos.size()]);

		for (PflichtInformation vpfInfo : vordruckPflichtFeldInfosIterieren) {
			if (!vordruckPflichtFeldInfos.contains(vpfInfo)) {
				continue;
			}

			for (PflichtInformation lpfInfo : pflichtFeldInfosIterieren) {
				if (!pflichtFeldInfos.contains(lpfInfo)) {
					continue;
				}

				if (!lpfInfo.getIterationsIntervall().umfasstLfdNrIntervall(vpfInfo.getIterationsIntervall())) {
					continue;
				}

				Set<PflichtIdentifier> additivMenge = lpfInfo.getAdditivMenge();//A
				Set<PflichtIdentifier> vordruckPflichtfeldMenge = vpfInfo.getpMenge();//P1
				if (!VerdichtungsUtil.schnittMengeIstLeer(additivMenge, vordruckPflichtfeldMenge)) {//A geschnitten P1 nicht leer
					vereinigePMengen(vordruckPflichtFeldInfos, vpfInfo, lpfInfo);
				}
			}
		}
		return aenderungenVorgenommen;
	}

	/**
	 * (V,P) und (A, M) mit P geschnitten A2 nicht leer -> M ist Vordruckpflichtmenge für Vordruck V
	 * <br>
	 * Konzept Schritt 15
	 */
	private boolean verdichteVordruckPflichtFeldInfoMitLokalerPflichtMengenInfo(
			Set<PflichtInformation> vordruckPflichtFeldInfos,
			Set<PflichtInformation> pflichtMengenInfos,
			Set<PflichtInformation> vordruckPflichtMengenInfos) {
		boolean geaendert = false;

		for (PflichtInformation vpfInfo : vordruckPflichtFeldInfos) {
			for (PflichtInformation lpmInfo : pflichtMengenInfos) {

				if (!lpmInfo.getIterationsIntervall().umfasstLfdNrIntervall(vpfInfo.getIterationsIntervall())) {
					continue;
				}

				if (!VerdichtungsUtil.schnittMengeIstLeer(vpfInfo.getpMenge(),
						lpmInfo.getAdditivMenge())) {//P geschnitten A nicht leer
					PflichtInformation neueInfo = VerdichtungsUtil.createNeueLokalePflichtInfo(vpfInfo, lpmInfo,
							InfoTyp.VordruckPflichtmengenInfo);
					if (!VerdichtungsUtil.infoIstRedundantInInfos(neueInfo, vordruckPflichtMengenInfos)) {
						neueInfo.setRegelName(vpfInfo.getRegelName());
						vordruckPflichtMengenInfos.add(neueInfo);
						geaendert = true;
					}
				}
			}
		}
		return geaendert;
	}

	/**
	 * (V, M) und (A, P) mit M Teilmenge von A -> P sind Vordruckpflichtfelder für V <br>
	 * Konzept Schritt 14
	 */
	private boolean verdichteVordruckPflichtMengenInfoMitLokalerPflichtFeldInfo(
			Set<PflichtInformation> vordruckPflichtMengenInfos,
			Set<PflichtInformation> pflichtFeldInfos,
			Set<PflichtInformation> vordruckPflichtFeldInfos) {

		boolean geaendert = false;

		for (PflichtInformation vpmInfo : vordruckPflichtMengenInfos) {
			for (PflichtInformation lpfInfo : pflichtFeldInfos) {

				if (!lpfInfo.getIterationsIntervall().umfasstLfdNrIntervall(vpmInfo.getIterationsIntervall())) {
					continue;
				}
				Set<PflichtIdentifier> additivMenge = lpfInfo.getAdditivMenge();
				Set<PflichtIdentifier> vordruckPflichtMenge = vpmInfo.getpMenge();
				if (additivMenge.containsAll((vordruckPflichtMenge))) {
					PflichtInformation neueInfo = VerdichtungsUtil.createNeueLokalePflichtInfo(vpmInfo, lpfInfo,
							InfoTyp.VordruckPflichtfeldInfo);
					if (!VerdichtungsUtil.infoIstRedundantInInfos(neueInfo, vordruckPflichtFeldInfos)) {
						neueInfo.setRegelName(vpmInfo.getRegelName());
						vordruckPflichtFeldInfos.add(neueInfo);
						geaendert = true;
					}
				}
			}
		}
		return geaendert;
	}

	/**
	 * (V, M1) und (A, M2) mit M1 Teilmenge von A -> M2 ist Vordruckpflichtmenge für Vordruck V <br>
	 * Konzept Schritt 16
	 */
	private boolean verdichteVordruckPflichtMengenInfoMitLokalerPflichtMengenInfo(
			Set<PflichtInformation> vordruckPflichtMengenInfos,
			Set<PflichtInformation> pflichtMengenInfos) {

		boolean geaendert = false;
		Set<PflichtInformation> neueInfos = new HashSet<>();

		for (PflichtInformation vpmInfo : vordruckPflichtMengenInfos) {
			for (PflichtInformation lpmInfo : pflichtMengenInfos) {

				if (!lpmInfo.getIterationsIntervall().umfasstLfdNrIntervall(vpmInfo.getIterationsIntervall())) {
					continue;
				}
				Set<PflichtIdentifier> additivMenge = lpmInfo.getAdditivMenge();
				Set<PflichtIdentifier> vordruckPflichtMenge = vpmInfo.getpMenge();
				if (additivMenge.containsAll((vordruckPflichtMenge))) {
					PflichtInformation neueInfo = VerdichtungsUtil.createNeueLokalePflichtInfo(vpmInfo, lpmInfo,
							InfoTyp.VordruckPflichtmengenInfo);
					if (VerdichtungsUtil.infoIstRedundantInInfos(neueInfo, neueInfos)) {
						continue;
					}
					if (!VerdichtungsUtil.infoIstRedundantInInfos(neueInfo, vordruckPflichtMengenInfos)) {
						neueInfo.setRegelName(vpmInfo.getRegelName());
						neueInfos.add(neueInfo);
						geaendert = true;
					}
				}
			}
		}
		vordruckPflichtMengenInfos.addAll(neueInfos);
		return geaendert;
	}

	/**
	 * ("", M1), (A, M2) mit M1 Teilmenge von A -> ("", M2), d.h. M2 ist globale Pflichtmenge
	 */
	private boolean verdichteGlobalePflichtMengenInfoMitLokalerPflichtMengenInfo(
			Set<PflichtInformation> globalePflichtMengenInfos,
			Set<PflichtInformation> pflichtMengenInfos) {

		boolean geaendert = false;
		Set<PflichtInformation> neueInfos = new HashSet<>();
		Set<PflichtInformation> infosToDelete = new HashSet<>();

		for (PflichtInformation gpmInfo : globalePflichtMengenInfos) {
			//Pflichtmengeninfos können Zeileneinschränkungen haben, globale Pflichtfeldinfos dürfen das nicht
			if (hasRowRestriction(gpmInfo)) {
				continue;
			}

			for (PflichtInformation lpmInfo : pflichtMengenInfos) {
				IterationsIntervall neuesIntervall = lpmInfo.getIterationsIntervall()
						.getIntersection(gpmInfo.getIterationsIntervall());
				if (neuesIntervall == null || !lpmInfo.getAdditivMenge().containsAll(gpmInfo.getpMenge())) {
					continue;
				}
				PflichtInformation neueInfo = PflichtInformation.createGlobalePflichtMengenInformation(lpmInfo.getpMenge(),
						neuesIntervall);
				if (VerdichtungsUtil.infoIstRedundantInInfos(neueInfo, neueInfos)) {
					continue;
				}
				if (VerdichtungsUtil.infoIstRedundantInInfos(neueInfo, globalePflichtMengenInfos)) {
					continue;
				}
				neueInfo.setRegelName(gpmInfo.getRegelName());
				neueInfos.add(neueInfo);

				infosToDelete.add(lpmInfo);

				geaendert = true;
			}
		}

		pflichtMengenInfos.removeAll(infosToDelete);
		globalePflichtMengenInfos.addAll(neueInfos);
		return geaendert;
	}

	/**
	 * ("", M), (A, P) mit M Teilmenge von A -> ("", P), d.h. Felder in P sind globale
	 * Pflichtfelder
	 */
	private boolean verdichteGlobalePflichtMengenInfoMitLokalerPflichtFeldInfo(Set<PflichtInformation> globalePflichtMengenInfos,
			Set<PflichtInformation> pflichtFeldInfos, Set<PflichtInformation> globalePflichtfelder) {
		boolean geaendert = false;
		Set<PflichtInformation> neueInfos = new HashSet<>();
		Set<PflichtInformation> infosToDelete = new HashSet<>();

		for (PflichtInformation gpmInfo : globalePflichtMengenInfos) {
			//Pflichtmengeninfos können Zeileneinschränkungen haben, globale Pflichtfeldinfos dürfen das nicht
			if (hasRowRestriction(gpmInfo)) {
				continue;
			}

			for (PflichtInformation lpfInfo : pflichtFeldInfos) {

				IterationsIntervall neuesIntervall = lpfInfo.getIterationsIntervall()
						.getIntersection(gpmInfo.getIterationsIntervall());
				if (neuesIntervall == null || !lpfInfo.getAdditivMenge().containsAll(gpmInfo.getpMenge())) {
					continue;
				}
				PflichtInformation neueInfo = PflichtInformation.createPflichtfeldInformation(lpfInfo.getpMenge(), neuesIntervall);
				if (VerdichtungsUtil.infoIstRedundantInInfos(neueInfo, neueInfos)) {
					continue;
				}
				if (VerdichtungsUtil.infoIstRedundantInInfos(neueInfo, globalePflichtMengenInfos)) {
					continue;
				}
				neueInfo.setRegelName(gpmInfo.getRegelName());
				neueInfos.add(neueInfo);

				infosToDelete.add(lpfInfo);

				geaendert = true;
			}
		}

		pflichtFeldInfos.removeAll(infosToDelete);
		globalePflichtfelder.addAll(neueInfos);
		return geaendert;
	}

	private boolean hasRowRestriction(PflichtInformation gpmInfo) {
		for (PflichtIdentifier pFeld : gpmInfo.getpMenge()) {
			long zeileStart = gpmInfo.getIterationsIntervall().getZeileStart();
			long zeileEnde = gpmInfo.getIterationsIntervall().getZeileEnde();
			if (zeileStart > 1 || (zeileStart >= 1 && zeileEnde < pFeld.getMaxZeile())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * ("", P), (A, M), A enthält Feld aus P -> ("", M) globale Pflichtmengeninfo
	 */
	private boolean verdichteGlobalePflichtFeldInfoMitLokalerPflichtMengenInfo(Set<PflichtInformation> globalePflichtfelder,
			Set<PflichtInformation> pflichtMengenInfos, Set<PflichtInformation> globalePflichtMengenInfos) {
		if (globalePflichtfelder.isEmpty() || pflichtMengenInfos.isEmpty()) {
			return false;
		}
		boolean geaendert = false;
		Set<PflichtInformation> neueInfos = new HashSet<>();

		for (PflichtInformation gpfInfo : globalePflichtfelder) {
			for (PflichtInformation lpmInfo : pflichtMengenInfos) {
				if (!testAdditivHatNichtLeerenSchnittMitPflichtfeldMenge(gpfInfo, lpmInfo)) {
					continue;
				}
				PflichtInformation neueInfo = PflichtInformation.createGlobalePflichtMengenInformation(lpmInfo.getpMenge(),
						gpfInfo.getIterationsIntervall());
				if (VerdichtungsUtil.infoIstRedundantInInfos(neueInfo, neueInfos)) {
					continue;
				}
				if (VerdichtungsUtil.infoIstRedundantInInfos(neueInfo, globalePflichtMengenInfos)) {
					continue;
				}
				neueInfo.setRegelName(gpfInfo.getRegelName());
				neueInfos.add(neueInfo);

				geaendert = true;
			}
		}

		globalePflichtMengenInfos.addAll(neueInfos);
		return geaendert;
	}

	/**
	 * ("", P1), (A, P2), A enthält Feld aus P1 -> ("", P2) d.h. Felder in P2 sind neue globale
	 * Pflichtfelder
	 */
	private boolean verdichteGlobalePflichtFeldInfoMitLokalerPflichtFeldInfo(Set<PflichtInformation> globalePflichtfelder,
			Set<PflichtInformation> pflichtFeldInfos) {
		if (globalePflichtfelder.isEmpty() || pflichtFeldInfos.isEmpty()) {
			return false;
		}
		boolean geaendert = false;
		Set<PflichtInformation> neueInfos = new HashSet<>();

		for (PflichtInformation gpfInfo : globalePflichtfelder) {
			for (PflichtInformation lpfInfo : pflichtFeldInfos) {
				if (!testAdditivHatNichtLeerenSchnittMitPflichtfeldMenge(gpfInfo, lpfInfo)) {
					continue;
				}
				PflichtInformation neueInfo = PflichtInformation.createPflichtfeldInformation(lpfInfo.getpMenge(),
						gpfInfo.getIterationsIntervall());
				if (VerdichtungsUtil.infoIstRedundantInInfos(neueInfo, neueInfos)) {
					continue;
				}
				if (VerdichtungsUtil.infoIstRedundantInInfos(neueInfo, globalePflichtfelder)) {
					continue;
				}
				neueInfo.setRegelName(gpfInfo.getRegelName());
				neueInfos.add(neueInfo);

				geaendert = true;
			}
		}

		globalePflichtfelder.addAll(neueInfos);
		return geaendert;
	}

	private void addPflichtfelderZuVordruckpflichtfelder(Set<PflichtInformation> vordruckPflichtFeldInfos,
			Set<PflichtInformation> globalePflichtfelder) {
		for (PflichtInformation gpf : globalePflichtfelder) {
			for (PflichtIdentifier pi : gpf.getpMenge()) {
				String vordruck = EntityUtils.getRootGroupName(pi.getFullName());
				Set<PflichtIdentifier> pFelder = new HashSet<>();
				pFelder.add(pi);
				PflichtInformation neueInfo = PflichtInformation.createVordruckPflichtFeldInformation(vordruck, pFelder,
						gpf.getIterationsIntervall());
				if (!VerdichtungsUtil.infoIstRedundantInInfos(neueInfo, vordruckPflichtFeldInfos)) {
					neueInfo.setRegelName(gpf.getRegelName());
					vordruckPflichtFeldInfos.add(neueInfo);
				}
			}
		}

	}

}
