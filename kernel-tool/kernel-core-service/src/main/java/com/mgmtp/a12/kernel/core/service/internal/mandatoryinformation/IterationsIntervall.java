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
import com.mgmtp.a12.kernel.core.parser.internal.util.AnalyseService;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.common.CommonConstants;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.utils.IndexArt;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IRule;

/**
 * Modelliert das Intervall für die Iteration für laufende Nummer und Zeile, für den eine
 * Pflichtfeld- bzw. Pflichtmengen-Information gültig ist.
 * Anforderung aus EXT-2800.
 */
public class IterationsIntervall {

	private long lStart = CommonConstants.NOT_INITIALISED;
	private long lEnde = CommonConstants.NOT_INITIALISED;
	private long zStart = CommonConstants.NOT_INITIALISED;
	private long zEnde = CommonConstants.NOT_INITIALISED;

	/**
	 * Ein Musterbereich besteht aus einem optionalen Index für laufende Nummer und optional für
	 * Zeile
	 */

	public IterationsIntervall(long lStart, long lEnde) {
		this.lStart = lStart;
		this.lEnde = lEnde;
	}

	private IterationsIntervall(long lStart, long lEnde, long zStart, long zEnde) {
		this(lStart, lEnde);
		this.zStart = zStart;
		this.zEnde = zEnde;
	}

	public IterationsIntervall() {
	}

	public static IterationsIntervall create(IRule rule, ICondition b) {
		long lStart = ermittleStartIndex(IndexArt.LFDNUMMER, rule, b);
		long lEnde = ermittleEndIndex(IndexArt.LFDNUMMER, rule, b);

		long zStart = ermittleStartIndex(IndexArt.ZEILE, rule, b);
		long zEnde = ermittleEndIndex(IndexArt.ZEILE, rule, b);

		return new IterationsIntervall(lStart, lEnde, zStart, zEnde);
	}

	private static long ermittleStartIndex(IndexArt art, IRule rule, ICondition b) {
		assert art == IndexArt.LFDNUMMER || art == IndexArt.ZEILE;
		if (AnalyseService.referenziertIteration(b, art)) {
			Optional<IterationInterval> interval = getIterationRestriction(art, rule);
			return interval.isPresent() ? interval.get().getStart() : 1;
		}
		return CommonConstants.NOT_INITIALISED;
	}

	private static long ermittleEndIndex(IndexArt art, IRule rule, ICondition b) {
		assert art == IndexArt.LFDNUMMER || art == IndexArt.ZEILE;
		if (AnalyseService.referenziertIteration(b, art)) {
			Optional<IterationInterval> interval = getIterationRestriction(art, rule);
			return interval.isPresent() ? interval.get().getEnd() : getMaxIndex(art, rule.getErrorEntity());
		}
		return CommonConstants.NOT_INITIALISED;
	}

	private static Optional<IterationInterval> getIterationRestriction(IndexArt art, IRule rule) {
		if (rule instanceof IExtmpRule extmpRule) {
			return art == IndexArt.LFDNUMMER
					? extmpRule.getRootgroupIterationRestriction()
					: extmpRule.getRowIterationRestriction();
		}
		return Optional.empty();
	}

	private static long getMaxIndex(IndexArt art, IEntity entity) {
		long[] maxValues = entity.getMax();
		if (art == IndexArt.LFDNUMMER) {
			return maxValues[0];
		} else if (entity.isGroup()) {
			return 1;
		} else {
			return maxValues[maxValues.length - 1];
		}
	}

	public boolean lfdEquals(IterationsIntervall intervall) {
		return this.lStart == intervall.getLfdNrStart() && this.lEnde == intervall.getLfdNrEnde();
	}

	/**
	 * Gibt true zurück wenn für die Zeile und für die laufende Nummer:
	 * <ol>
	 * <li>this und der übergebene Parameter kein Muster spezifizieren oder</li>
	 * <li>this und der übergebene Parameter beide ein Muster spezifizieren und this größer oder
	 * gleich dem vom übergebenen Parameter spezifizierten Bereich ist.</li>
	 * </ol>
	 */
	public boolean umfasst(IterationsIntervall intervall) {
		return umfasstLfdNrIntervall(intervall) && umfasstZeilenIntervall(intervall);
	}

	private boolean umfasstZeilenIntervall(IterationsIntervall other) {
		if (zStart == CommonConstants.NOT_INITIALISED && other.zStart == CommonConstants.NOT_INITIALISED) {
			return true;
		}

		return zStart <= other.zStart && other.zEnde <= zEnde;
	}

	public boolean umfasstLfdNrIntervall(IterationsIntervall other) {
		if (lStart == CommonConstants.NOT_INITIALISED && other.lStart == CommonConstants.NOT_INITIALISED) {
			return true;
		}
		return lStart <= other.lStart && other.lEnde <= lEnde;
	}

	IterationsIntervall getIntersection(IterationsIntervall other) {
		long resultLStart = Math.max(lStart, other.lStart);
		long resultLEnde = Math.min(lEnde, other.lEnde);
		long resultZStart = Math.max(zStart, other.zStart);
		long resultZEnde = Math.min(zEnde, other.zEnde);
		if (resultLStart > resultLEnde || resultZStart > resultZEnde) {
			return null;
		}
		return new IterationsIntervall(resultLStart, resultLEnde, resultZStart, resultZEnde);
	}

	public long getZeileStart() {
		return zStart;
	}

	public long getZeileEnde() {
		return zEnde;
	}

	public long getLfdNrStart() {
		return lStart;
	}

	public long getLfdNrEnde() {
		return lEnde;
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

		IterationsIntervall o = (IterationsIntervall) other;
		return lStart == o.lStart && lEnde == o.lEnde && zStart == o.zStart && zEnde == o.zEnde;
	}

	public int hashCode() {
		int code = 17;
		code = 37 * code + (int) (lStart ^ (lStart >>> 32));
		code = 37 * code + (int) (lEnde ^ (lEnde >>> 32));
		code = 37 * code + (int) (zStart ^ (zStart >>> 32));
		code = 37 * code + (int) (zEnde ^ (zEnde >>> 32));

		return code;
	}

	public String toString() {
		return "LfdNr: [" + lStart + "-" + lEnde + "], Zeile: [" + zStart + "-" + zEnde + "]";
	}
}
