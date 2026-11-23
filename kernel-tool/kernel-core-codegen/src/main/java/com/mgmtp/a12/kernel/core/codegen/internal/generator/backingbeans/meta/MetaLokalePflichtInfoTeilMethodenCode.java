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

import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.STParameterClassBase;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.StringTemplateGroup;
import com.mgmtp.a12.kernel.core.service.internal.mandatoryinformation.PflichtInformation;

/**
 * Hilfsklasse für die Codegenerierung, Methoden werden von den Templates aus aufgerufen. Stellt die
 * Metadaten über lokale Pflichtfelder bereit.
 */
@StringTemplateGroup("meta")
public class MetaLokalePflichtInfoTeilMethodenCode extends STParameterClassBase {

	int methodenIndex;
	int anzahlInfosProMethode;
	List<PflichtInformation> infos = new ArrayList<>();
	private final Map<String, String> uniqueNames;
	private final String name;

	public MetaLokalePflichtInfoTeilMethodenCode(List<PflichtInformation> infos, Map<String, String> uniqueNames, int methodenIndex,
			int anzahlInfosProMethode, String name) {
		this.methodenIndex = methodenIndex;
		this.anzahlInfosProMethode = anzahlInfosProMethode;
		this.infos = infos;
		this.uniqueNames = uniqueNames;
		this.name = name;
	}

	public String getInfosCode() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < Math.min(anzahlInfosProMethode, infos.size()); i++) {
			int infoNr = methodenIndex * anzahlInfosProMethode + i;
			MetaLokalePflichtInfoCode info = new MetaLokalePflichtInfoCode(infos.get(i), infoNr, uniqueNames);
			sb.append(info.toCode());
		}
		return sb.toString();
	}

	public String getMethodenNameCode() {
		return "addLokalePflichtFeldInfos";
	}

	public String getMethodenIndexCode() {
		return String.valueOf(methodenIndex);
	}

	public String getClassName() {
		return "Meta_PfFeld_" + name;
	}
}
