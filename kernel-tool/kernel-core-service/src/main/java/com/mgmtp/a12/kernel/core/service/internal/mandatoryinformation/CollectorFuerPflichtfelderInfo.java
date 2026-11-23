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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IParseTreeEntity;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICompositeCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.LogOp;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.common.VordruckIntern;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.utils.FeldIdUtilities;
import com.mgmtp.a12.kernel.core.service.internal.mandatoryinformation.PflichtInformation.InfoTyp;

/**
 * Sammelt die Pflichtfelder für eine Regel.
 */
public class CollectorFuerPflichtfelderInfo extends PflichtInfoCollector {

	private Set<PflichtInformation> pfInfos = new LinkedHashSet<>();

	private Set<PflichtIdentifier> neuePflFelder = new LinkedHashSet<>();

	private Set<VordruckIntern> pflichtVordrucke = new LinkedHashSet<>();

	private long aktLfdNr;

	void createAndAddPflichtInfo(ICompositeCondition bed) {
		// Not needed for this type of required fields
	}

	/**
	 * Sammelt die Pflichtfeldinformationen für eine Regel und fügt sie der übergebenen Menge von
	 * Pflichtinformationen hinzu. <br>
	 * Sollen Vordruckpflichtfelder für einen bestimmten Vordruck ermittelt werden, kann der
	 * entsprechende Vordruck den pflichtVordrucken hinzugefügt werden und beim Aufruf von
	 * {@link #addNeueInfo(String)} übergeben werden.<br>
	 * Ist die übergebene aktuelle LfdNr 0, so werden Pflichtfelder gesammelt, die für alle
	 * laufenden Nummern aus dem LfdNr-Intervall der Regel Pflichtfelder sind.<br>
	 * Ist die übergebene LfdNr ungleich 0, so werden Pflichffelder gesammelt, die für diese LfdNr
	 * Pflichtfelder sind.
	 */
	public CollectorFuerPflichtfelderInfo(RegelInfo regelInfo, Set<PflichtInformation> pfInfos,
			Set<VordruckIntern> pflichtVordrucke,
			long aktLfdNr) {
		this.regelname = regelInfo.getRegelName();
		this.pfInfos = pfInfos;
		this.iterationsIntervall = regelInfo.getIterationsIntervall();
		this.pflichtVordrucke = pflichtVordrucke;
		this.aktLfdNr = aktLfdNr;
	}

	private CollectorFuerPflichtfelderInfo(CollectorFuerPflichtfelderInfo orig) {

		this.regelname = orig.regelname;
		this.pfInfos = orig.pfInfos;
		this.iterationsIntervall = orig.iterationsIntervall;
		this.pflichtVordrucke = orig.pflichtVordrucke;
		this.aktLfdNr = orig.aktLfdNr;
	}

	@Override
	public boolean visit(ICompositeCondition zBed) {
		if (zBed.getLogOp() == LogOp.Or) {
			return true;
		}

		Set<PflichtIdentifier> gemeinsamePFelder = new HashSet<>();
		boolean first = true;
		for (ICondition tb : zBed.getConditionList()) {
			EvaluatorVisitor ev = new EvaluatorVisitor(pfInfos, pflichtVordrucke, aktLfdNr);
			tb.traverse(ev);
			if (!ev.isTrue()) {
				CollectorFuerPflichtfelderInfo visitor = new CollectorFuerPflichtfelderInfo(this);
				tb.traverse(visitor);
				if (visitor.neuePflFelder.isEmpty()) {
					return false;
				}
				Set<PflichtIdentifier> neuePflFelderTmp = new HashSet<>(visitor.neuePflFelder);
				if (first) {
					gemeinsamePFelder = neuePflFelderTmp;
					first = false;
					continue;
				}
				gemeinsamePFelder.retainAll(neuePflFelderTmp);
				if (gemeinsamePFelder.isEmpty()) {
					return false;
				}
			}
		}
		neuePflFelder.addAll(gemeinsamePFelder);
		return false;
	}

	@Override
	public boolean visit(IEntityListCondition flBed) {
		List<IEntityId> felder = flBed.getEntityList().getEntityIdList();

		switch (flBed.getConditionName()) {
			case NotAllFieldsFilled:
				neuePflFelder.addAll(createPflichtIdentifierMenge(getFuerAlleZeilenGueltigeFeldIds(felder)));
				break;
			case NotExactlyOneFieldFilled: // kann eigentlich nicht vorkommen wegen Normalisierung
			case NoFieldFilled:
				addPflichtfelderWennFeldlisteEinelementigUndGiltFuerAlleZeilen(felder);
				break;
			default:
				break;
		}
		return false;
	}

	private List<IEntityId> getFuerAlleZeilenGueltigeFeldIds(List<IEntityId> felder) {
		return felder.stream().filter(id -> giltFuerAlleZeilen(id, iterationsIntervall)).collect(Collectors.toList());
	}

	private void addPflichtfelderWennFeldlisteEinelementigUndGiltFuerAlleZeilen(List<IEntityId> felder) {
		if (felder.size() == 1) {
			IEntityId id = felder.get(0);
			if (!FeldIdUtilities.hasWildCard(id) && giltFuerAlleZeilen(id, iterationsIntervall)) {
				neuePflFelder.addAll(createPflichtIdentifierMenge(felder));
			}
		}
	}

	/**
	 * Gibt zurück, ob die FeldId für alle Zeilen gültig ist. Dies ist nicht der Fall, wenn die
	 * Iteration eingeschränkt ist, oder wenn eine konkrete Zeile referenziert wird.
	 * <p>
	 * Nur wenn ein Feld für alle Zeilen angegeben sein muss, ist es ein Pflichtfeld. (EXT-9806)
	 */
	static boolean giltFuerAlleZeilen(IEntityId id, IterationsIntervall itIntervall) {
		if (FeldIdUtilities.isEinzeilig(id)) {
			return true;
		}
		if (FeldIdUtilities.isZIteration(id)) {
			return itIntervall.getZeileStart() == 1 && itIntervall.getZeileEnde() == id.getMaxValues()[id.getMaxValues().length - 1];
		}
		return FeldIdUtilities.isZStern(
				id); // eingeschränkter Zeilenstern wird hier nicht berücksichtigt (siehe interne Doku zum Codegenerator)
	}

	/**
	 * Fügt für die neu erkannten Pflichtfelder je nach übergebenem Parameter neue Pflichtvordrucke
	 * oder neue Pflichtfelder hinzu.
	 *
	 * @param aktVordruck
	 * 		der Vordruckname des Vordruck, für den die Felder gegebenenfalls
	 * 		Vordruckpflichtfelder sind, bzw. <code>null</code> für die bestimmung von
	 * 		Pflichtfeldern.
	 * @return
	 */
	public boolean addNeueInfo(String aktVordruck) {

		if (neuePflFelder.isEmpty()) {
			return false;
		}
		addNeuePflichtVordrucke();
		IterationsIntervall itIntervall = aktLfdNr == 0 ? iterationsIntervall : new IterationsIntervall(aktLfdNr, aktLfdNr);
		return (aktVordruck == null) ?
				addNeuePflichtfeldInfo(itIntervall) :
				addNeueVordruckPflichtfeldInfo(itIntervall, aktVordruck);
	}

	private void addNeuePflichtVordrucke() {
		for (PflichtIdentifier pf : neuePflFelder) {
			VordruckIntern vAlle = new VordruckIntern(pf.getVordruckName(), IParseTreeEntity.WILDCARD);
			if (pflichtVordrucke.contains(vAlle)) {
				continue;
			}
			if (pf.getLfdNummer() == IParseTreeEntity.ITERATION) {
				if (iterationsIntervall.getLfdNrStart() == 1 && iterationsIntervall.getLfdNrEnde() == pf.getMaxLfdNr()) {
					pflichtVordrucke.add(new VordruckIntern(pf.getVordruckName(), IParseTreeEntity.WILDCARD));
				} else {
					for (long l = iterationsIntervall.getLfdNrStart(); l <= iterationsIntervall.getLfdNrEnde(); l++) {
						pflichtVordrucke.add(new VordruckIntern(pf.getVordruckName(), l));
					}
				}
			} else {
				pflichtVordrucke.add(new VordruckIntern(pf.getVordruckName(), pf.getLfdNummer()));
			}

		}
	}

	private boolean addNeuePflichtfeldInfo(IterationsIntervall itIntervall) {
		PflichtInformation neuePflInfo = PflichtInformation.createPflichtfeldInformation(neuePflFelder, itIntervall);
		for (PflichtInformation pInfo : pfInfos) {
			if (pInfo.umfasstInfo(neuePflInfo)) {
				return false;
			}
		}

		Set<PflichtIdentifier> toDelete = new HashSet<>();
		for (PflichtIdentifier pf : neuePflFelder) {
			for (PflichtInformation pInfo : pfInfos) {
				if (pInfo.getIterationsIntervall().equals(itIntervall)) {
					return pInfo.getpMenge().addAll(neuePflFelder);
				}
				if (infoEnthaeltPflichtfeld(pInfo, pf, itIntervall)) {
					toDelete.add(pf);
				}
			}
		}
		neuePflFelder.removeAll(toDelete);
		if (neuePflFelder.isEmpty()) {
			return false;
		}
		neuePflInfo.setRegelName(regelname);
		pfInfos.add(neuePflInfo);
		return true;
	}

	private boolean addNeueVordruckPflichtfeldInfo(IterationsIntervall itIntervall, String aktVordruck) {
		Set<PflichtIdentifier> felderNichtAufAktVordruck = new HashSet<>();
		for (PflichtIdentifier pf : neuePflFelder) {
			if (!pf.getVordruckName().equals(aktVordruck)) {
				felderNichtAufAktVordruck.add(pf);
			}
		}
		neuePflFelder.removeAll(felderNichtAufAktVordruck);
		if (neuePflFelder.isEmpty()) {
			return false;
		}
		for (PflichtInformation pInfo : pfInfos) {
			if (pInfo.getInfoTyp() == InfoTyp.VordruckPflichtfeldInfo
					&& pInfo.getVordruckName().equals(aktVordruck)
					&& pInfo.getIterationsIntervall().lfdEquals(itIntervall)) {
				return pInfo.getpMenge().addAll(neuePflFelder);
			}
		}
		PflichtInformation vpfInfo = PflichtInformation.createVordruckPflichtFeldInformation(aktVordruck, neuePflFelder,
				itIntervall);
		vpfInfo.setRegelName(regelname);
		pfInfos.add(vpfInfo);
		return true;
	}

	private boolean infoEnthaeltPflichtfeld(PflichtInformation pInfo, PflichtIdentifier pf, IterationsIntervall itIntervall) {
		for (PflichtIdentifier f : pInfo.getpMenge()) {
			if (pf.equals(f) && pInfo.getIterationsIntervall().umfasstLfdNrIntervall(itIntervall)) {
				return true;
			}
		}
		return false;
	}
}
