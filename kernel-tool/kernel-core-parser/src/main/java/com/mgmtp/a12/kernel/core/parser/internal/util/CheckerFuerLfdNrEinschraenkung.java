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
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IComparisonCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICompositeCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IConstantOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.INumberOfRootGroupsOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.ComparisonOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.services.FeldOperationUtil;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IRule;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblemReporter;

/**
 * Überprüft für eine Regelbedingung, ob diese eine Einschränkung für die laufende Nummer
 * modelliert. Die Methode
 * {@link #checkAndSaveAnwendungsSpezifischeLfdNr(ModelChanges, Map, IEC, IProblemReporter, Locale)}
 * bietet den Extraktionsservice für den Aufrufer an.
 */
public class CheckerFuerLfdNrEinschraenkung extends com.mgmtp.a12.kernel.core.parsetree.a12internal.ast.ASTVisitor {

	/**
	 * Methode überprüft, ob Regeln eine anwendungsspezifische LfdNr modellieren. Falls ja, werden
	 * die entsprechenden Regeln und die zu ändernde Information bei den Vordrucken und für alle
	 * betroffenen Felder/Kontexte in modelChanges gespeichert.
	 */
	public static void checkAndSaveAnwendungsSpezifischeLfdNr(ModelChanges modelChanges, Map<IRule, ICondition> regeln, IEC ec,
			IProblemReporter problemManager, Locale locale) {
		CheckerFuerLfdNrEinschraenkung tester = new CheckerFuerLfdNrEinschraenkung(problemManager, locale);

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
		for (String vordruckPfad : tester.laufendeNummerXVordruck.keySet()) {
			IEntity vordruck = ec.get(vordruckPfad);
			long newMaxLfdNr = tester.laufendeNummerXVordruck.get(vordruckPfad);
			if (newMaxLfdNr < vordruck.getMax()[0]) {
				List<IEntity> toChange = new ArrayList<>();
				toChange.add(vordruck);
				toChange.addAll(EntityUtils.getAllContainedEntities(vordruckPfad, ec));
				for (IEntity entity : toChange) {
					modelChanges.setNewMaxValue(entity, 0, newMaxLfdNr);
				}
			}
		}
	}

	private Map<String, Long> laufendeNummerXVordruck = new HashMap<>();
	private IProblemReporter problemManager;
	private Locale locale;
	private IRule regel;
	private List<IRule> rulesModellingRestriction = new ArrayList<>();

	public CheckerFuerLfdNrEinschraenkung(IProblemReporter problemManager, Locale locale) {
		this.problemManager = problemManager;
		this.locale = locale;
	}

	private void setRegel(IRule r) {
		this.regel = r;
	}

	/**
	 * eine Zusammengesetzte Bedingung kann nie eine Einschränkung für eine laufende Nummer
	 * spezifizieren.
	 */
	@Override
	public boolean visit(ICompositeCondition b) {
		return false;
	}

	@Override
	public boolean visit(IComparisonCondition b) {
		if (b.getOperation1() instanceof INumberOfRootGroupsOperation && FeldOperationUtil.isZahlKonstante(b.getOperation2())) {
			// anzAngegebenerVordruck > 99 oder anzAngegebenerVordruck >= 99
			if (b.getCmpOp() != ComparisonOperation.GT && b.getCmpOp() != ComparisonOperation.GE) {
				return false;
			}
			checkFeldOp((INumberOfRootGroupsOperation) b.getOperation1(), (IConstantOperation) b.getOperation2(),
					b.getCmpOp() == ComparisonOperation.GT);
			return false;
		}
		if (b.getOperation2() instanceof INumberOfRootGroupsOperation && FeldOperationUtil.isZahlKonstante(b.getOperation1())) {
			if (b.getCmpOp() != ComparisonOperation.LT && b.getCmpOp() != ComparisonOperation.LE) {
				return false;
			}
			checkFeldOp((INumberOfRootGroupsOperation) b.getOperation2(), (IConstantOperation) b.getOperation1(),
					b.getCmpOp() == ComparisonOperation.LT);
			return false;
		}

		return false;
	}

	/**
	 * Die Regel eine Einschränkung. Je nach Art des Vergleichsoperators
	 * wird die Konstante oder eins kleiner der Konstante modelliert.
	 * <p>
	 * Wenn relevante Regel gefunden wurde, wird überprüft, ob für den Vordruck bereits eine andere
	 * Regel relevant war. Falls ja, wird das Minimum gespeichert.
	 */
	private void checkFeldOp(INumberOfRootGroupsOperation anzOp, IConstantOperation konstante, boolean unEqualComparison) {
		long maxLfdNrNeu = Long.parseLong(konstante.getValue());
		//Falls anzAngegebenerVordruck > Konstante
		if (!unEqualComparison) {
			maxLfdNrNeu--;
		}

		if (maxLfdNrNeu == 1) {
			// Die Einschränkung auf eine Vordruckwiederholung ist nicht zulässig.
			problemManager.reportProblem(
					new MVKParserError(regel, new ParserErrorHandler(locale).getParserErrorMessage(
							ParserErrorTypesEnum.MVK_MAX_LFDNR_RESTRICTION_TO_ONE)));
			return;
		} else if (maxLfdNrNeu < 1) {
			return;
		}
		rulesModellingRestriction.add(regel);

		String vordruckPfad = "/" + anzOp.getRootGroupName();

		Long bereitsGesetzteLfdNr = laufendeNummerXVordruck.get(vordruckPfad);
		// für den Vordruck wurde bereits eine laufende Nummer gesetzt, und diese ist kleiner,  -> nix ändern
		if (bereitsGesetzteLfdNr != null && bereitsGesetzteLfdNr < maxLfdNrNeu) {
			return;
		}
		laufendeNummerXVordruck.put(vordruckPfad, maxLfdNrNeu);
	}
}
