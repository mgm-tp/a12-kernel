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
import java.util.Set;

/**
 * Klasse modelliert die verschiedenen gesammelten Pflichtfeld- und
 * Pflichtmengen-Eigenschaften
 */
public class PflichtInformation {

	public enum InfoTyp {
		LokalePflichtfeldInfo,
		LokalePflichtmengenInfo,
		VordruckPflichtmengenInfo,
		VordruckPflichtfeldInfo,
		GlobalePflichtmengenInfo,
		PflichtVordruckInfo,
		GlobalePflichfeldInfo
	}

	private Set<PflichtIdentifier> additivMenge = new HashSet<>();
	private Set<PflichtIdentifier> pMenge = new HashSet<>();

	private InfoTyp infoTyp;

	private String regelName = "";

	private IterationsIntervall iterationsIntervall = new IterationsIntervall();
	private String vordruckName = ""; // nur für Vordruckpflichtfelder / mengen

	static PflichtInformation createLokalePflichtFeldInformation(
			Set<PflichtIdentifier> add, Set<PflichtIdentifier> pfl, IterationsIntervall itB) {
		return new PflichtInformation(InfoTyp.LokalePflichtfeldInfo, pfl, itB, add, null);
	}

	static PflichtInformation createLokalePflichtMengenInformation(
			Set<PflichtIdentifier> add, Set<PflichtIdentifier> pfl, IterationsIntervall itB) {
		return new PflichtInformation(InfoTyp.LokalePflichtmengenInfo, pfl, itB, add, null);
	}

	static PflichtInformation createGlobalePflichtMengenInformation(Set<PflichtIdentifier> pfl, IterationsIntervall itB) {
		return new PflichtInformation(InfoTyp.GlobalePflichtmengenInfo, pfl, itB, null, null);
	}

	static PflichtInformation createVordruckPflichtFeldInformation(String vordruck, Set<PflichtIdentifier> pFelder,
			IterationsIntervall itIntervall) {
		return new PflichtInformation(InfoTyp.VordruckPflichtfeldInfo, pFelder, itIntervall, null, vordruck);
	}

	static PflichtInformation createVordruckPflichtMengenInformation(String vordruck, Set<PflichtIdentifier> pfl,
			IterationsIntervall itB) {
		return new PflichtInformation(InfoTyp.VordruckPflichtmengenInfo, pfl, itB, null, vordruck);
	}

	static PflichtInformation createLokalePflichtInformation(InfoTyp typ, Set<PflichtIdentifier> pfl,
			IterationsIntervall itB, Set<PflichtIdentifier> add, String vordruck) {
		return new PflichtInformation(typ, pfl, itB, add, vordruck);
	}

	static PflichtInformation createPflichtVordruckInformation(String vordruck, long lStart, long lEnde) {
		return new PflichtInformation(InfoTyp.PflichtVordruckInfo, null, new IterationsIntervall(lStart, lEnde), null, vordruck);
	}

	public static PflichtInformation createPflichtfeldInformation(Set<PflichtIdentifier> pFelder, IterationsIntervall itB) {
		return new PflichtInformation(InfoTyp.GlobalePflichfeldInfo, pFelder, itB, null, null);
	}

	private PflichtInformation(
			InfoTyp infoTyp,
			Set<PflichtIdentifier> pMenge,
			IterationsIntervall iterationsIntervall,
			Set<PflichtIdentifier> additivMenge, String vordruckName) {
		this.infoTyp = infoTyp;
		this.iterationsIntervall = iterationsIntervall;
		if (pMenge != null) {
			this.pMenge = pMenge;
		}
		if (additivMenge != null) {
			this.additivMenge = additivMenge;
		}
		if (vordruckName != null) {
			this.vordruckName = vordruckName;
		}

	}

	/**
	 * Gibt <b>true</b> zurück, wenn info nicht mehr Information beinhaltet als diese
	 * PflichtInformation.
	 */
	public boolean umfasstInfo(PflichtInformation info) {
		//es werden nur Pflichtinfos vom gleichen Typ verglichen
		if (info.getInfoTyp() != infoTyp) {
			return false;
		}
		if (!umfasstAdditivInformation(info)) {
			return false;
		}
		switch (info.getInfoTyp()) {
			case LokalePflichtfeldInfo:
			case VordruckPflichtfeldInfo:
			case GlobalePflichfeldInfo:
				// es gibt kein Pflichtfeld aus info,  dass nicht vom this beschrieben wird.
				return this.pMenge.containsAll(info.getpMenge());

			case LokalePflichtmengenInfo:
			case VordruckPflichtmengenInfo:
			case GlobalePflichtmengenInfo:
				//eine kleinere P Menge bedeutet mehr Information. Das bedeutet,
				//hier muss die containment-Beziehung in der anderen Richtung geprüft werden.
				return info.getpMenge().containsAll(this.pMenge);
			default:
				return false;
		}
	}

	private boolean umfasstAdditivInformation(PflichtInformation info) {
		return this.iterationsIntervall.umfasst(info.iterationsIntervall)
				&& info.getVordruckName().equals(this.vordruckName)
				&& this.additivMenge.containsAll(info.getAdditivMenge());

	}

	public String toString() {
		String result = "\n    " + infoTyp +
				"\n        ";
		if (InfoTyp.VordruckPflichtmengenInfo.equals(infoTyp) ||
				InfoTyp.VordruckPflichtfeldInfo.equals(infoTyp)) {
			result = result + "Vordruck =     " + vordruckName;
		} else {
			result = result + "Additivmenge =        [";
		}
		StringBuilder buf = new StringBuilder(result);

		for (PflichtIdentifier fs : additivMenge) {
			buf.append(fs.toString());
			buf.append(",");
		}
		buf.append("]\n        Lokale Pflichtmenge = [");
		for (PflichtIdentifier fs : pMenge) {
			buf.append(fs.toString());
			buf.append(",");
		}
		buf.append("]\n        Iterationsintervall = " + iterationsIntervall.toString());
		buf.append("\n        regelName = " + regelName);
		return buf.toString();
	}

	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null) {
			return false;
		}
		if (other.getClass() != getClass()) {
			return false;
		}

		PflichtInformation o = (PflichtInformation) other;
		return o.additivMenge.equals(this.additivMenge)
				&& o.pMenge.equals(this.pMenge)
				&& o.infoTyp == this.infoTyp
				&& o.vordruckName.equals(this.vordruckName)
				&& o.iterationsIntervall.equals(this.iterationsIntervall);
	}

	public int hashCode() {
		int code = 17;
		code = 37 * code + additivMenge.hashCode();
		code = 37 * code + pMenge.hashCode();
		if (infoTyp != null) {
			code = 37 * code + infoTyp.hashCode();
		}
		code = 37 * code + vordruckName.hashCode();
		code = 37 * code + iterationsIntervall.hashCode();

		return code;
	}

	public String getRegelName() {
		return regelName;
	}

	public void setRegelName(String name) {
		regelName = name;
	}

	public Set<PflichtIdentifier> getAdditivMenge() {
		return additivMenge;
	}

	public Set<PflichtIdentifier> getpMenge() {
		return pMenge;
	}

	public void setpMenge(Set<PflichtIdentifier> pMenge) {
		this.pMenge = pMenge;
	}

	public InfoTyp getInfoTyp() {
		return infoTyp;
	}

	public String getVordruckName() {
		return vordruckName;
	}

	public void setVordruckName(String vordruckName) {
		this.vordruckName = vordruckName;
	}

	public long getLfdNrStart() {
		return iterationsIntervall.getLfdNrStart();
	}

	public long getLfdNrEnde() {
		return iterationsIntervall.getLfdNrEnde();
	}

	public IterationsIntervall getIterationsIntervall() {
		return iterationsIntervall;
	}
}
