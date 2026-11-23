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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IComparisonCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICompositeCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IEntityListOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.ComparisonOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.services.FeldOperationUtil;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.utils.FeldIdUtilities;

/**
 * Sammelt die globalen Pflichtmengeninformationen aus einzelnen Regeln. Eine globale
 * Pflichtmengeninformation besagt, daß immer mindestens ein Feld aus der Pflichtmenge angegeben
 * sein muß. Eine solche Information kann aus Regeln der Form KeinFeldAngegeben(a1,..,an) Oder ...
 * abgeleitet werden.
 * <p>
 * Auch aus einer Regel der Form {@literal "AnzahlAngegebenerFelder(feld.*) < Konstante Oder ..."}
 * kann eine Pflichtmenge abgeleitet werden.
 */
public class CollectorFuerGlobalePflichtmengenInfo extends PflichtInfoCollector {

	private static final Logger logger = LoggerFactory.getLogger(CollectorFuerGlobalePflichtmengenInfo.class);

	public CollectorFuerGlobalePflichtmengenInfo(RegelInfo regelInfo) {
		this.iterationsIntervall = regelInfo.getIterationsIntervall();
		this.regelname = regelInfo.getRegelName();
	}

	void createAndAddPflichtInfo(ICompositeCondition bed) {
		// Not needed for this type of required fields
	}

	@Override
	public boolean visit(IEntityListCondition b) {
		List<IEntityId> felder = b.getEntityList().getEntityIdList();
		if (KEIN_FELD.equals(b.getConditionName())) {
			createAndAddGolbalePflichtInfo(felder);
		}
		if (NICHT_ALLE_FELDER.equals(b.getConditionName())) {
			behandleEingeschraenkteZeilen(felder);
		}
		return false; // es muss nicht weiter traversiert werden
	}

	private void createAndAddGolbalePflichtInfo(List<IEntityId> felder) {
		Set<PflichtIdentifier> pfl = createPflichtIdentifierMenge(felder);
		PflichtInformation info = PflichtInformation.createGlobalePflichtMengenInformation(pfl, iterationsIntervall);
		info.setRegelName(regelname);
		logger.debug("Fuege globale Pflichtmengeninformation hinzu: " + info);
		infos.add(info);
	}

	@Override
	public boolean visit(IComparisonCondition vBed) {

		IOperation op1 = vBed.getOperation1();
		IOperation op2 = vBed.getOperation2();

		IEntityListOperation fop = FeldOperationUtil.getAnzahlAngegebnerFelderOrKontexteOrVerschiedenerWerteOp(op1);
		int z = FeldOperationUtil.getZahlKonstanteWert(op2);
		if (fop != null) {
			if (z > 0 && (vBed.getCmpOp().equals(ComparisonOperation.LE) || vBed.getCmpOp().equals(ComparisonOperation.LT))) {
				List<IEntityId> felder = fop.getEntityList().getEntityIdList();
				addPflichtinfoWennFeldlisteEinelementig(felder);
			}
		} else {
			fop = FeldOperationUtil.getAnzahlAngegebnerFelderOrKontexteOrVerschiedenerWerteOp(op2);
			z = FeldOperationUtil.getZahlKonstanteWert(op1);
			if (fop != null && z > 0 &&
					(vBed.getCmpOp().equals(ComparisonOperation.GE) || vBed.getCmpOp().equals(ComparisonOperation.GT))) {
				List<IEntityId> felder = fop.getEntityList().getEntityIdList();
				addPflichtinfoWennFeldlisteEinelementig(felder);
			}
		}
		return false;
	}

	private void addPflichtinfoWennFeldlisteEinelementig(List<IEntityId> felder) {
		if (felder.size() == 1) {
			IEntityId id = felder.get(0);
			if (!FeldIdUtilities.isLStern(id) && !FeldIdUtilities.isUStern(id) && !FeldIdUtilities.hasKontexteMitStern(id)) {
				createAndAddGolbalePflichtInfo(felder);
			}
		}
	}

	/**
	 * Felder, die nur für eine oder mehrere bestimmte Zeilen (aber nicht für alle Zeilen) angegeben
	 * werden müssen, sind keine Pflichtfelder. Da sie bei der Pflichtmengenprüfung jedoch
	 * berücksichtigt werden müssen, werden hier für solche Felder einelementige Pflichtmengen
	 * erzeugt. (EXT-9806)
	 * <p>
	 * Beispiele:
	 * FeldNichtAngegeben(feld.1)
	 * NichtAlleFelderOderKeinFeldAngegeben(feld1.1, feld2.1)
	 * FeldNichtAngegeben(feld) mit eingeschränkter Iteration
	 */
	private void behandleEingeschraenkteZeilen(List<IEntityId> felder) {
		felder.stream().filter(id -> !CollectorFuerPflichtfelderInfo.giltFuerAlleZeilen(id, iterationsIntervall))
				.forEach(id -> createAndAddGolbalePflichtInfo(Arrays.asList(id)));
	}
}
