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

package com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.CodeGenModel;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.StringTemplateGroup;
import com.mgmtp.a12.kernel.core.service.internal.mandatoryinformation.PflichtAngaben;
import com.mgmtp.a12.kernel.core.service.internal.mandatoryinformation.PflichtInformation;

// No explicit template for this class
@StringTemplateGroup("meta")
public abstract class CodeGenMetaPflicht extends CodeGenModel {

	protected static final int ANZAHL_PFLICHTINFOS_PRO_METHODE = 100;
	protected PflichtAngaben pflichtAngaben;
	protected Map<String, String> uniqueFieldNames;

	public CodeGenMetaPflicht(CodeGenMetaPflichtBuilder builder) {
		super(builder);

		pflichtAngaben = builder.getMandatoryInformation();
		uniqueFieldNames = data.getUniqueFieldNames();
	}

	protected int berechneAnzahlMethoden(int anzahlInfos) {
		if (anzahlInfos % ANZAHL_PFLICHTINFOS_PRO_METHODE == 0) {
			return anzahlInfos / ANZAHL_PFLICHTINFOS_PRO_METHODE;
		}
		return (anzahlInfos / ANZAHL_PFLICHTINFOS_PRO_METHODE) + 1;
	}

	protected String erzeugeLokalePflichtinfoMethodenAufrufCode(int anzahlMethoden) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < anzahlMethoden; i++) {
			LokalePflichtInfoMethodenAufrufCode aufruf = new LokalePflichtInfoMethodenAufrufCode(i);
			sb.append(aufruf.toCode());
		}
		return sb.toString();
	}

	protected String erzeugeLokalePflichtInfoMethodenCode(List<PflichtInformation> lokalePflichtInfos) {
		int anzahlMethoden = berechneAnzahlMethoden(lokalePflichtInfos.size());
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < anzahlMethoden; i++) {
			List<PflichtInformation> infos = new ArrayList<>();
			int start = i * ANZAHL_PFLICHTINFOS_PRO_METHODE;
			int ende = Math.min((i + 1) * ANZAHL_PFLICHTINFOS_PRO_METHODE, lokalePflichtInfos.size());
			for (int j = start; j < ende; j++) {
				infos.add(lokalePflichtInfos.get(j));
			}
			MetaLokalePflichtInfoTeilMethodenCode info = new MetaLokalePflichtInfoTeilMethodenCode(infos, uniqueFieldNames, i,
					ANZAHL_PFLICHTINFOS_PRO_METHODE, getName());
			sb.append(info.toCode());
		}
		return sb.toString();
	}
}