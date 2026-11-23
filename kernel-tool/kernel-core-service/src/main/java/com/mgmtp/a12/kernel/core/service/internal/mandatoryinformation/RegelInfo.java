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

import java.util.Optional;

import com.mgmtp.a12.kernel.core.extmpspecifics.a12internal.api.ado.IExtmpRule;
import com.mgmtp.a12.kernel.core.extmpspecifics.a12internal.api.ado.IExtmpRule.IterationInterval;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.util.ParseTreeCopier;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IRule;

/**
 *
 */
public class RegelInfo {
	private ICondition normalizedBedingung;
	private String regelName = "";
	private IterationsIntervall iterationsIntervall;
	private boolean gleichFuerAlleLfdNr = true;

	private RegelInfo(IRule regel, ICondition b) {
		regelName = regel.getFullName();
		normalizedBedingung = b;

		iterationsIntervall = IterationsIntervall.create(regel, b);
		gleichFuerAlleLfdNr = !isLfdNrIterationEingeschraenkt(regel);
	}

	private boolean isLfdNrIterationEingeschraenkt(IRule regel) {
		Optional<IterationInterval> lfdNrInterval = regel instanceof IExtmpRule ?
				((IExtmpRule) regel).getRootgroupIterationRestriction() :
				Optional.empty();
		if (lfdNrInterval.isPresent()) {
			return (lfdNrInterval.get().getStart() > 1) || (lfdNrInterval.get().getEnd() < regel.getErrorEntity().getMax()[0]);

		}
		return false;
	}

	/**
	 * Erzeugt aus der Regel die notwendigen Informationsstrukturen für die Pflichtfelderkennung.
	 */
	public static RegelInfo createRegelInfo(IRule regel, ICondition b, IEC ec) {
		if (b == null) {
			return null;
		}

		b = ParseTreeCopier.copyBedingung(b);
		b = NormalizeService.normalize(b, ec);

		return new RegelInfo(regel, b);
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

	public String getRegelName() {
		return regelName;
	}

	/**
	 * Liefert <code>false</code>, wenn
	 * <ol>
	 * <li>Die Regel ein Regelmuster mit Einschränkung spezifiziert oder</li>
	 * <li>Die Regelbedingung ein Feld referenziert, das Pflichtfeld nur für spezifische laufende
	 * Nummern ist</li>
	 * </ol>
	 */
	public boolean istGleichFuerAlleLfdNr() {
		return gleichFuerAlleLfdNr;
	}

	public void setNichtGleichFuerAlleLfdNr() {
		this.gleichFuerAlleLfdNr = false;
	}

	public ICondition getNormalizedBedingung() {
		return normalizedBedingung;
	}
}
