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

import java.util.List;

import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IParseTreeEntity;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.utils.FeldIdUtilities;

/**
 * Hilfsklasse, die überprüft, ob die Ebenenspezifikation der Felder der Feldliste zusammenpassen.
 */
public class EbenenChecker {

	private EbenenChecker() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * gibt false zurück, wenn Felder unterschiedlicher Wiederholungsebenen referenziert werden.
	 *
	 * @param b
	 * @return
	 */
	public static boolean checkEbenen(IEntityListCondition b) {
		List<IEntityId> felder = b.getEntityList().getEntityIdList();

		for (IEntityId feld : felder) {
			if (!checkUsbFuerFeld(feld)) {
				return false;
			}
			if (!checkZeileFuerFeld(feld, felder)) {
				return false;
			}

			if (!checkLfdNrFuerFeld(feld, felder)) {
				return false;
			}
		}
		return true;
	}

	private static boolean checkUsbFuerFeld(IEntityId id) {
		return (id.getUsb() != IParseTreeEntity.WILDCARD);
	}

	private static boolean checkZeileFuerFeld(IEntityId id, List<IEntityId> felder) {
		if (FeldIdUtilities.isEinzeilig(id)) {
			return true;
		}
		if (FeldIdUtilities.isZIteration(id)) {
			return true;
		}

		if (FeldIdUtilities.isZStern(id)) {
			return alleFelderZSternOhneEinschraenkungOderEinzeilig(felder);
		}

		return alleFelderZGleichOderEinzeilig(FeldIdUtilities.getZeile(id), felder);
	}

	private static boolean checkLfdNrFuerFeld(IEntityId id, List<IEntityId> felder) {
		if (FeldIdUtilities.isLIteration(id)) {
			return true;
		}

		if (FeldIdUtilities.isLStern(id)) {
			return alleFelderLSternOhneEinschraenkung(felder);
		}
		//es ist weder ein Muster noch ein Stern-Operator, also muss es eine konkrete Zahl sein
		return alleFelderLGleich(FeldIdUtilities.getLfdNr(id), felder);
	}

	private static boolean alleFelderZSternOhneEinschraenkungOderEinzeilig(List<IEntityId> felder) {
		for (IEntityId f : felder) {
			if (FeldIdUtilities.isMehrzeilig(f) && !FeldIdUtilities.isZStern(f)) {
				return false;
			}
			if (FeldIdUtilities.isZSternMitEinschraenkung(f)) {
				return false;
			}
		}
		return true;

	}

	private static boolean alleFelderLSternOhneEinschraenkung(List<IEntityId> felder) {
		for (IEntityId f : felder) {
			if (!FeldIdUtilities.isLStern(f)) {
				return false;
			}
			if (FeldIdUtilities.isLSternMitEinschraenkung(f)) {
				return false;
			}
		}
		return true;
	}

	private static boolean alleFelderZGleichOderEinzeilig(long zeile, List<IEntityId> felder) {

		for (IEntityId f : felder) {
			if (FeldIdUtilities.isEinzeilig(f)) {
				continue;
			}
			if (FeldIdUtilities.getZeile(f) != zeile) {
				return false;
			}
		}

		// alle Felder sind einzeilig oder spezifizieren die gleiche Zeile
		return true;
	}

	private static boolean alleFelderLGleich(long ldfNr, List<IEntityId> felder) {

		for (IEntityId f : felder) {
			if (FeldIdUtilities.getLfdNr(f) != ldfNr) {
				return false;
			}
		}

		return true;
	}

}
