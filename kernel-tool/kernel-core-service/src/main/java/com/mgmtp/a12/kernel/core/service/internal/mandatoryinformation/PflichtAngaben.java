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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Modelliert die Gesamtheit der lokalen Pflichtfeld- und Pflichtmengen-Informationen
 */
public class PflichtAngaben {

	private Set<PflichtInformation> lokalePflichtfelder = new HashSet<>();
	private Set<PflichtInformation> lokalePflichtmengen = new HashSet<>();

	private Set<PflichtInformation> vordruckPflichtfelder = new HashSet<>();
	private Set<PflichtInformation> vordruckPflichtmengen = new HashSet<>();

	private Set<PflichtInformation> globalePflichtfelder = new HashSet<>();
	private Set<PflichtInformation> globalePflichtmengenInfo = new HashSet<>();
	private Set<String> mandatoryFieldsForRepeatableGroup = new HashSet<>();

	public Set<PflichtInformation> getLokalePflichtFelder() {
		return lokalePflichtfelder;
	}

	public void setLokalePflichtFelder(Set<PflichtInformation> set) {
		lokalePflichtfelder = set;
	}

	public void setLokalePflichtMengen(Set<PflichtInformation> set) {
		lokalePflichtmengen = set;
	}

	public void setLocalMandatoryFieldsForRepeatableGroup(Set<String> mandatoryFieldsForRepeatableGroup) {
		this.mandatoryFieldsForRepeatableGroup = mandatoryFieldsForRepeatableGroup;
	}

	public Set<PflichtInformation> getLokalePflichtMengen() {
		return lokalePflichtmengen;
	}

	public Set<PflichtInformation> getVordruckPflichtFelder() {
		return vordruckPflichtfelder;
	}

	public void setVordruckPflichtFelder(Set<PflichtInformation> set) {
		vordruckPflichtfelder = set;
	}

	public Set<PflichtInformation> getVordruckPflichtMengen() {
		return vordruckPflichtmengen;
	}

	public void setVordruckPflichtMengen(Set<PflichtInformation> set) {
		vordruckPflichtmengen = set;
	}

	public Set<PflichtInformation> getGlobalePflichtfelder() {
		return globalePflichtfelder;
	}

	public void setGlobalePflichtfelder(Set<PflichtInformation> set) {
		this.globalePflichtfelder = set;
	}

	public Set<PflichtInformation> getGlobalePflichtMengenInfos() {
		return globalePflichtmengenInfo;
	}

	public void setGlobalePflichtMengenInfos(Set<PflichtInformation> set) {
		globalePflichtmengenInfo = set;
	}

	public List<PflichtInformation> getLokalePflichtFeldInfosAsSortedList() {
		return getSortierteInfos(lokalePflichtfelder);
	}

	public Set<String> getLocalMandatoryFieldsForRepeatableGroup() {
		return mandatoryFieldsForRepeatableGroup;
	}

	private static List<PflichtInformation> getSortierteInfos(Set<PflichtInformation> infos) {
		if (infos == null) {
			return new ArrayList<>();
		}
		List<PflichtInformation> infosAsList = new ArrayList<>(infos);
		Collections.sort(infosAsList, (o1, o2) -> {
			if (o1.getAdditivMenge().size() != o2.getAdditivMenge().size()) {
				return o1.getAdditivMenge().size() - o2.getAdditivMenge().size();
			}
			return o1.getRegelName().compareToIgnoreCase(o2.getRegelName());
		});
		return infosAsList;

	}

}
