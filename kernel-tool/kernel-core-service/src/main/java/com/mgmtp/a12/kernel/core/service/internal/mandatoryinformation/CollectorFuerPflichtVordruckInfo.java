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
import java.util.List;

import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityIdList;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IParseTreeEntity;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IComparisonCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICompositeCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IRootGroupCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IEntityListOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.ComparisonOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.LogOp;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.RootGroupConditionName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.services.FeldOperationUtil;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.utils.FeldIdUtilities;

/**
 * Sammelt die Pflichtvordrucke
 */
public class CollectorFuerPflichtVordruckInfo extends PflichtInfoCollector {

	public CollectorFuerPflichtVordruckInfo(RegelInfo regelInfo) {
		this.regelname = regelInfo.getRegelName();
		long lStart = regelInfo.istGleichFuerAlleLfdNr() ? 0 : regelInfo.getLfdNrStart();
		long lEnde = regelInfo.istGleichFuerAlleLfdNr() ? 0 : regelInfo.getLfdNrEnde();
		this.iterationsIntervall = new IterationsIntervall(lStart, lEnde);
	}

	private CollectorFuerPflichtVordruckInfo(IterationsIntervall iterationsIntervall, String rName) {
		this.iterationsIntervall = iterationsIntervall;
		this.regelname = rName;
	}

	void createAndAddPflichtInfo(ICompositeCondition bed) {
		// Not needed for this type of required fields
	}

	// Falls es sich um eine Und Bedingung handelt:  bildet die Schnittmenge aus allen
	// Pflichtvordrucken in allen Teilbedingungen. Dies sind die Pflichtvordrucke aus dieser
	// Zusammengesetzten Bedingung
	@Override
	public boolean visit(ICompositeCondition zBed) {
		if (LogOp.Or == zBed.getLogOp()) {
			return true;
		}

		List<List<PflichtInformation>> pflichtVordruckeProTeilbedingung = new ArrayList<>();

		for (ICondition b : zBed.getConditionList()) {
			CollectorFuerPflichtVordruckInfo visitor = new CollectorFuerPflichtVordruckInfo(iterationsIntervall, regelname);
			b.traverse(visitor);
			if (visitor.getInfos().isEmpty()) {
				return false;
			}
			pflichtVordruckeProTeilbedingung.add(visitor.getInfos());
		}

		/*
		 * es handelt sich um eine Und-Bedingung; nur Pflichtvordruck, wenn in allen Teilbedingungen
		 * Pflichtvordruck, wir bilden also die Schnittmenge
		 */
		List<PflichtInformation> pflichtVordrucke = pflichtVordruckeProTeilbedingung.get(0);
		for (List<PflichtInformation> pflichtVordruck : pflichtVordruckeProTeilbedingung) {
			pflichtVordrucke = bildeSchnittmenge(pflichtVordrucke, pflichtVordruck);
			if (pflichtVordrucke.isEmpty()) {
				return false;
			}
		}
		infos.addAll(pflichtVordrucke);

		return false;
	}

	private List<PflichtInformation> bildeSchnittmenge(List<PflichtInformation> infos1, List<PflichtInformation> infos2) {

		List<PflichtInformation> result = new ArrayList<>();
		for (PflichtInformation info : infos1) {
			if (infos2.contains(info)) {
				result.add(info);
			}
		}
		return result;
	}

	@Override
	public boolean visit(IRootGroupCondition b) {
		if (((RootGroupConditionName.KeinVordruckAngegeben == b.getConditionName()) && isVordruckListeEinelementig(b))) {
			long lStart = b.getRootGroupIndex() == IParseTreeEntity.ITERATION ?
					iterationsIntervall.getLfdNrStart() :
					b.getRootGroupIndex();
			long lEnde = b.getRootGroupIndex() == IParseTreeEntity.ITERATION ?
					iterationsIntervall.getLfdNrEnde() :
					b.getRootGroupIndex();
			infos.add(PflichtInformation.createPflichtVordruckInformation(b.getRootGroupName(), lStart, lEnde));
		}
		return false;
	}

	private boolean isVordruckListeEinelementig(IRootGroupCondition b) {
		return (b.getRootGroupIndex() != IParseTreeEntity.WILDCARD) && (b.getUsb() != IParseTreeEntity.WILDCARD);
	}

	@Override
	public boolean visit(IEntityListCondition flBed) {
		if (flBed.getConditionName() == KEIN_FELD || flBed.getConditionName() == NICHT_GENAU_EIN_FELD) {
			addInfoWennFelderAufSelbemVordruck(flBed.getEntityList());
		}
		if (flBed.getConditionName() == NICHT_ALLE_FELDER) {
			flBed.getEntityList().getEntityIdList().stream()
					.forEach(feldId -> addInfo(FeldIdUtilities.getVordruckName(feldId), FeldIdUtilities.getLfdNr(feldId)));
		}
		return false;
	}

	/**
	 * Ermittelt ob alle Felder der fliste auf dem selben Vordruck (inkl. laufende Nummer) liegen.
	 * Falls ja, wird dieser Vordruck der Lösungsmenge hinzugefügt.
	 */
	private void addInfoWennFelderAufSelbemVordruck(IEntityIdList fliste) {
		String vordruck = null;
		long lfdNr = 0;
		boolean isMuster = false;
		for (IEntityId feld : fliste.getEntityIdList()) {
			if (FeldIdUtilities.isLStern(feld) || FeldIdUtilities.isUStern(feld)) {
				return;
			}
			String vName = FeldIdUtilities.getVordruckName(feld);
			//Initialisierung des Vordrucks
			if (vordruck == null) {
				vordruck = vName;
				lfdNr = FeldIdUtilities.getLfdNr(feld);
				if (lfdNr == IParseTreeEntity.ITERATION) {
					isMuster = true;
				} else if (lfdNr == IParseTreeEntity.SEMANTIC_INDEX) {
					return;
				}
			}

			//Felder liegen nicht auf dem gleichen Vordruck
			if (!vName.equals(vordruck)) {
				return;
			}
			if (isMuster != FeldIdUtilities.isLIteration(feld)) {
				//Felder referenzieren unterschiedliche Vordrucksnummern
				return;
			}
			if (!isMuster) {
				long aktLfdNr = FeldIdUtilities.getLfdNr(feld);
				//Felder referenzieren unterschiedliche Vordrucksnummern
				if (lfdNr != aktLfdNr) {
					return;
				}
			}

		}

		if (vordruck != null) {
			addInfo(vordruck, lfdNr);
		}
	}

	private void addInfo(String vordruck, long lfdNr) {
		long lStart = (lfdNr != IParseTreeEntity.ITERATION) ? lfdNr : iterationsIntervall.getLfdNrStart();
		long lEnde = (lfdNr != IParseTreeEntity.ITERATION) ? lfdNr : iterationsIntervall.getLfdNrEnde();

		infos.add(PflichtInformation.createPflichtVordruckInformation(vordruck, lStart, lEnde));
	}

	/**
	 * Bei einer Vergleichsbedingung der Form
	 * <ul>
	 * <li>{@literal AnzahlAngegebenerFelder(a,b,..) < Konstante, bzw.}</li>
	 * <li>{@literal Konstante > AnzahlAngegebenerFelder(a,b,..)}</li>
	 * </ul>
	 * mit a,b,... auf dem selben Vordruck und Konstante größer 0 wird eine
	 * PflichtVordruckInformation erzeugt. Ebenso bei Vergleich mit <= und >=.
	 */
	@Override
	public boolean visit(IComparisonCondition b) {

		IOperation op1 = b.getOperation1();
		IOperation op2 = b.getOperation2();

		IEntityListOperation fop = FeldOperationUtil.getAnzahlAngegebnerFelderOrKontexteOrVerschiedenerWerteOp(op1);
		double z = FeldOperationUtil.getZahlKonstanteWert(op2);
		if (fop != null) {
			if (z > 0 &&
					(b.getCmpOp().equals(ComparisonOperation.LE) || b.getCmpOp().equals(ComparisonOperation.LT))) {
				addInfoWennFelderAufSelbemVordruck(fop.getEntityList());
			}
		} else {
			fop = FeldOperationUtil.getAnzahlAngegebnerFelderOrKontexteOrVerschiedenerWerteOp(op2);
			z = FeldOperationUtil.getZahlKonstanteWert(op1);
			if (fop != null && z > 0 &&
					(b.getCmpOp().equals(ComparisonOperation.GE) || b.getCmpOp().equals(ComparisonOperation.GT))) {
				addInfoWennFelderAufSelbemVordruck(fop.getEntityList());
			}
		}
		return false;
	}

}
