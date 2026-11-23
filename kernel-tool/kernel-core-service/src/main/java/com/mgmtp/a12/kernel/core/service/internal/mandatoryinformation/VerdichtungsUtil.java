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
import java.util.Set;

import com.mgmtp.a12.kernel.core.service.internal.mandatoryinformation.PflichtInformation.InfoTyp;

/**
 * Diese Hilfsklasse wird beim Verdichten der ermittelten lokalen Pflichtfeld- und
 * Pflichtmengeninfos verwendet.
 */
public class VerdichtungsUtil {

	private VerdichtungsUtil() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Erzeugt eine neue lokale Pflichtinformation. Die Musterinfos (d.h. für welche Bereiche die
	 * LfdNr- bzw. Zeilenmuster gelten) sind diejenigen von <code>infoFuerAdditiv</code>. Alle
	 * informationen werden kopiert, daher keine Seiteneffekte
	 *
	 * @param infoFuerAdditiv
	 * 		additivMenge für die neue lokale PflichtInformation
	 * @param infoFuerPMenge
	 * 		PMenge für die neue lokale PflichtInformation
	 * @param typ
	 * 		notwendiger Typ
	 * @return gibt die neu erzeugte PflichtInformation zurück
	 */
	static PflichtInformation createNeueLokalePflichtInfo(
			PflichtInformation infoFuerAdditiv,
			PflichtInformation infoFuerPMenge, InfoTyp typ) {

		Set<PflichtIdentifier> pfl = new HashSet<>(infoFuerPMenge.getpMenge());
		Set<PflichtIdentifier> add = new HashSet<>(infoFuerAdditiv.getAdditivMenge());
		String vordruck = infoFuerAdditiv.getVordruckName();
		return PflichtInformation.createLokalePflichtInformation(typ, pfl, infoFuerAdditiv.getIterationsIntervall(), add, vordruck);
	}

	/**
	 * Gibt true zurück, wenn die Pflichtinformation neueInfo nichts zu den in pflichtInfos bereits
	 * vorhandenen Informationen beitragen kann, d.h. wenn die Information in neueInfo aus
	 * pflichtInfos ableitbar ist.
	 *
	 * @param neueInfo
	 * 		zu prüfende Pflichtinformation
	 * @param infos
	 * 		Menge bereits vorhandener PflichtInformationen
	 * @return true, wenn neueInfo keine zusätzliche Information beschreibt
	 */
	static boolean infoIstRedundantInInfos(PflichtInformation neueInfo, Collection<PflichtInformation> infos) {
		if (infos.contains(neueInfo)) {
			return true; // Trivialfall
		}

		for (PflichtInformation info : infos) {
			if (info.umfasstInfo(neueInfo)) {
				return true;
			}
		}
		return false;
	}

	static Set<PflichtIdentifier> berechneSchnittMenge(Collection<PflichtIdentifier> menge1, Collection<PflichtIdentifier> menge2) {
		Set<PflichtIdentifier> result = new HashSet<>();
		for (PflichtIdentifier f : menge1) {
			result.add(new PflichtIdentifier(f));
		}
		result.retainAll(menge2);
		return result;
	}

	static boolean schnittMengeIstLeer(Collection<PflichtIdentifier> menge1, Collection<PflichtIdentifier> menge2) {
		for (PflichtIdentifier f : menge1) {
			if (menge2.contains(f)) {
				return false;
			}
		}
		return true;
	}

}
