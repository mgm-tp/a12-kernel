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

package com.mgmtp.a12.kernel.core.parser.internal.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.MVKParserError;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorHandler;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IParseTreeEntity;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IComparisonCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICompositeCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IConstantOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IEntityListOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.ComparisonOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.EntityListOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.ast.ASTVisitor;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.services.FeldOperationUtil;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IRule;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblemReporter;

/**
 * Überprüft für eine Regelbedingung, ob diese eine Einschränkung für einen Kontext modelliert. Die
 * Methode
 * {@link #checkAndSaveAnwendungsSpezifischeKontextwiederholbarkeit(ModelChanges, Map, IEC, IProblemReporter, Locale)}
 * bietet den Extraktionsservice für den Aufrufer an.
 */
public class CheckerFuerKontextEinschraenkung extends ASTVisitor {

	private IProblemReporter problemManager;
	private Locale locale;

	private IRule regel;

	private List<IRule> rulesModellingRestriction = new ArrayList<>();
	private Map<String, Long> kontextToMax = new HashMap<>();

	private CheckerFuerKontextEinschraenkung(IProblemReporter problemManager, Locale locale) {
		this.problemManager = problemManager;
		this.locale = locale;
	}

	/**
	 * Methode überprüft, ob Regeln eine anwendungsspezifische Kontextwiederholbarkeit modellieren.
	 * Falls ja, werden die entsprechenden Regeln und die zu ändernde Information für alle
	 * betroffenen Felder/Kontexte in modelChanges gespeichert.
	 */
	public static void checkAndSaveAnwendungsSpezifischeKontextwiederholbarkeit(ModelChanges modelChanges,
			Map<IRule, ICondition> regeln, IEC ec,
			IProblemReporter problemManager, Locale locale) {
		CheckerFuerKontextEinschraenkung tester = new CheckerFuerKontextEinschraenkung(problemManager, locale);

		// Teste alle Regeln
		for (Entry<IRule, ICondition> entry : regeln.entrySet()) {
			IRule r = entry.getKey();
			ICondition condition = entry.getValue();
			tester.setRegel(r);
			condition.traverse(tester);
		}

		// markiere zu löschende Regeln
		for (IRule rule : tester.rulesModellingRestriction) {
			modelChanges.addRuleModellingMaxValueRestrictions(rule);
		}

		// Setze die Information um.
		for (String kontextPfad : tester.kontextToMax.keySet()) {
			IEntity kontext = ec.get(kontextPfad);
			long newMaxValue = tester.kontextToMax.get(kontextPfad);
			int kontextLevel = EntityUtils.getLevel(kontextPfad);
			if (newMaxValue < kontext.getMax()[kontextLevel]) {
				List<IEntity> toChange = new ArrayList<>();
				toChange.add(kontext);
				toChange.addAll(EntityUtils.getAllContainedEntities(kontextPfad, ec));
				for (IEntity entity : toChange) {
					modelChanges.setNewMaxValue(entity, kontextLevel, newMaxValue);
				}
			}
		}
	}

	private void setRegel(IRule r) {
		this.regel = r;
	}

	/**
	 * eine Zusammengesetzte Bedingung kann nie eine Einschränkung für einen Kontext
	 * spezifizieren.
	 */
	@Override
	public boolean visit(ICompositeCondition b) {
		return false;
	}

	@Override
	public boolean visit(IComparisonCondition b) {
		if (isAnzAngKontexteMitEinemKontextMitStern(b.getOperation1()) && FeldOperationUtil.isZahlKonstante(b.getOperation2())) {
			// AnzahlAngegebenerKontexte > Konstante oder AnzahlAngegebenerKontexte >= Konstante
			if (b.getCmpOp() != ComparisonOperation.GT && b.getCmpOp() != ComparisonOperation.GE) {
				return false;
			}
			checkFeldOp((IEntityListOperation) b.getOperation1(), (IConstantOperation) b.getOperation2(),
					b.getCmpOp() == ComparisonOperation.GT);
			return false;
		}
		if (FeldOperationUtil.isZahlKonstante(b.getOperation1()) && isAnzAngKontexteMitEinemKontextMitStern(b.getOperation2())) {
			if (b.getCmpOp() != ComparisonOperation.LT && b.getCmpOp() != ComparisonOperation.LE) {
				return false;
			}
			checkFeldOp((IEntityListOperation) b.getOperation2(), (IConstantOperation) b.getOperation1(),
					b.getCmpOp() == ComparisonOperation.LT);
			return false;
		}

		return false;
	}

	/**
	 * Damit durch AnzahlAngegebenerKontexte die Kontextwiederholbarkeit eingeschränkt wird,
	 * darf nur ein Kontext mit Stern (ohne Stern auf höheren Ebenen) referenziert werden.
	 * Außerdem darf kein semantischer Index verwendet werden.
	 */
	private boolean isAnzAngKontexteMitEinemKontextMitStern(IOperation feldOperation) {
		if (!(feldOperation instanceof IEntityListOperation)) {
			return false;
		}
		IEntityListOperation feldListenFeldOperation = (IEntityListOperation) feldOperation;
		if (feldListenFeldOperation.getOperatorName() != EntityListOperationName.NumberOfFilledGroups) {
			return false;
		}
		List<IEntityId> entityIdList = feldListenFeldOperation.getEntityList().getEntityIdList();
		if (entityIdList.size() != 1) {
			return false;
		}
		if (!entityIdList.get(0).getSemanticIndex().isEmpty()) {
			return false;
		}
		if (entityIdList.get(0).getFilterCondition() != null) {
			return false;
		}
		long[] indices = entityIdList.get(0).getIndices();
		if (indices[indices.length - 1] != IParseTreeEntity.WILDCARD) {
			return false;
		}
		for (int i = 0; i < indices.length - 1; i++) {
			if (indices[i] == IParseTreeEntity.WILDCARD) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Prüft ob die Regel eine Einschränkung der Kontextwiederholbarkeit modelliert. Je nach Art des
	 * Vergleichsoperators wird die Konstante oder eins kleiner der Konstante modelliert.
	 * <p>
	 * Wenn relevante Regel gefunden wurde, wird überprüft, ob für den Kontext bereits eine andere
	 * Regel relevant war. Falls ja, wird das Minimum gespeichert.
	 */
	private void checkFeldOp(IEntityListOperation feldListenFeldOperation, IConstantOperation konstante,
			boolean unEqualComparison) {
		long maxWdhNeu = Long.parseLong(konstante.getValue());
		//Falls AnzahlAngegebenerKontexte >= Konstante
		if (!unEqualComparison) {
			maxWdhNeu--;
		}

		String kontextPfad = feldListenFeldOperation.getEntityList().getEntityIdList().get(0).getFullName();

		if (maxWdhNeu == 1) {
			// Die Einschränkung auf eine Kontextwiederholbarkeit von 1 ist nicht zulässig.
			problemManager.reportProblem(
					new MVKParserError(regel, new ParserErrorHandler(locale).getParserErrorMessage(
							ParserErrorTypesEnum.MVK_MAX_GROUP_RESTRICTION_TO_ONE, kontextPfad)));
			return;
		} else if (maxWdhNeu < 1) {
			return;
		}
		rulesModellingRestriction.add(regel);

		Long bereitsGesetzteMaxWdh = kontextToMax.get(kontextPfad);
		// für den Kontext wurde bereits eine Wiederholbarkeit gesetzt, und diese ist kleiner,  -> nix ändern
		if (bereitsGesetzteMaxWdh != null && bereitsGesetzteMaxWdh < maxWdhNeu) {
			return;
		}
		kontextToMax.put(kontextPfad, maxWdhNeu);
	}
}
