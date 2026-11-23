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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICompositeCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.EntityListConditionName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.LogOp;

/**
 * Abstrakte Oberklasse für die Klassen, die für das Sammeln der lokalen Pflichtfeld- bzw.
 * Pflichtmengen-Informationen zuständig sind.
 */
public abstract class PflichtInfoCollector extends com.mgmtp.a12.kernel.core.parsetree.a12internal.ast.ASTVisitor {

	protected List<PflichtInformation> infos = new ArrayList<>();
	protected String regelname;
	protected IterationsIntervall iterationsIntervall;

	protected static final EntityListConditionName MIND_EIN_FELD = EntityListConditionName.AtLeastOneFieldFilled;
	protected static final EntityListConditionName NICHT_ALLE_FELDER = EntityListConditionName.NotAllFieldsFilled;
	protected static final EntityListConditionName KEIN_FELD = EntityListConditionName.NoFieldFilled;
	protected static final EntityListConditionName NICHT_GENAU_EIN_FELD = EntityListConditionName.NotExactlyOneFieldFilled;

	@Override
	public boolean visit(ICompositeCondition b) {
		if (LogOp.And.equals(b.getLogOp())) {
			createAndAddPflichtInfo(b);
			return false;
		}
		//Traversiere nur bei einer ODER Bedingung weiter.
		return true;
	}

	@Override
	public boolean visit(IEntityListCondition b) {
		return false; // traversiere nie weiter
	}

	protected Set<PflichtIdentifier> createPflichtIdentifierMenge(IEntityListCondition b) {
		return createPflichtIdentifierMenge(b.getEntityList().getEntityIdList());
	}

	/**
	 * Jedes Feld der Feldliste wird zu einem PflichtIdentifier transformiert.
	 *
	 * @param feldIds
	 * 		- die zu transformierenden Felder
	 * @return Menge der transformierten EntityIds
	 */
	protected Set<PflichtIdentifier> createPflichtIdentifierMenge(List<IEntityId> feldIds) {
		Set<PflichtIdentifier> result = new HashSet<>();
		for (IEntityId feld : feldIds) {
			if (feld.getFilterCondition() != null) {
				continue;
			}
			PflichtIdentifier fs = new PflichtIdentifier(feld);
			result.add(fs);
		}
		return result;
	}

	abstract void createAndAddPflichtInfo(ICompositeCondition b);

	public List<PflichtInformation> getInfos() {
		return infos;
	}

	public static PflichtInfoCollector create(PflichtInformation.InfoTyp typ, RegelInfo regelInfo) {
		switch (typ) {

			case LokalePflichtfeldInfo:
				return new CollectorFuerLokalePflichtFeldInfo(regelInfo);
			case LokalePflichtmengenInfo:
				return new CollectorFuerLokalePflichtmengenInfo(regelInfo);
			case VordruckPflichtmengenInfo:
				return new CollectorFuerVordruckPflichtmengenInfo(regelInfo);
			case GlobalePflichtmengenInfo:
				return new CollectorFuerGlobalePflichtmengenInfo(regelInfo);
			case PflichtVordruckInfo:
				return new CollectorFuerPflichtVordruckInfo(regelInfo);
			default:
				throw new IllegalArgumentException(String.format("Method not supported for info type %s.", typ));
		}
	}
}
