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

import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.mgmtp.a12.kernel.core.extmpspecifics.a12internal.api.ado.IExtmpEntity;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.MVKParserError;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorHandler;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnumForHModel;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IComparisonCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICompositeCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.INumberOfUsbOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.ComparisonOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.LogOp;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.services.FeldOperationUtil;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IRule;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblemReporter;

/**
 * Überprüft für eine Regelbedingung, ob diese eine Einschränkung für den USB
 * modelliert. Die Methode
 * {@link #checkAndSaveAnwendungsSpezifischenUsb(ModelChanges, Map, IEC, IProblemReporter, Locale)}
 * bietet den Extraktionsservice für den Aufrufer an.
 */
public class CheckerFuerUsbEinschraenkung extends com.mgmtp.a12.kernel.core.parsetree.a12internal.ast.ASTVisitor {

	private IProblemReporter problemManager;
	private Locale locale;
	private IRule rule;
	private Integer maxUSB;

	/**
	 * Methode überprüft, ob Regeln einen anwendungsspezifischen max. USB modellieren. Falls ja,
	 * wird die zu ändernde Information für alle betroffenen Vordrucke in modelChanges gespeichert.
	 */
	public static void checkAndSaveAnwendungsSpezifischenUsb(ModelChanges modelChanges, Map<IRule, ICondition> regeln, IEC ec,
			IProblemReporter problemManager, Locale locale) {
		Optional<IEntity> usbVordruck = ec.getDirectlyContainedEntities("/").stream()
				.filter(EntityUtils::hasMaxUsb)
				.findAny();
		if (!usbVordruck.isPresent()) {
			return;
		}
		long oldMaxUsb = ((IExtmpEntity) usbVordruck.get()).getMaxUsb(); // max. USB muss für alle USB Vordrucke gleich sein

		CheckerFuerUsbEinschraenkung tester = new CheckerFuerUsbEinschraenkung(problemManager, locale, oldMaxUsb);

		// Teste alle Regeln
		for (Entry<IRule, ICondition> entry : regeln.entrySet()) {
			IRule r = entry.getKey();
			ICondition condition = entry.getValue();
			// Nur falls die Bedingung 'AnzahlAngegebenerUSB' enthält, muss sie analysiert werden.
			if (!AnalyseService.beinhaltetAnzahlAngegebenerUsb(regeln.get(r))) {
				continue;
			}
			tester.setRegel(r);
			condition.traverse(tester);
		}

		// Setze die Information um.
		if (tester.maxUSB < oldMaxUsb) {
			modelChanges.setMaxUsb(tester.maxUSB);
		}
	}

	public CheckerFuerUsbEinschraenkung(IProblemReporter problemManager, Locale locale, long maxUSB) {
		this.problemManager = problemManager;
		this.locale = locale;
		this.maxUSB = (int) maxUSB;
	}

	private void setRegel(IRule r) {
		this.rule = r;
	}

	/**
	 * Prüfen ob die Bedingung aus mehreren zusammengesetzten Bedingungen besteht wobei
	 * 'AnzahlAngegebenerUSB' Teil einer UND-Verküpfung ist. Ist das der Fall so wird durch
	 * diese Bedingung MaxAnzahlUsb nicht beschränkt und es wird null zurückgegeben.
	 */
	@Override
	public boolean visit(ICompositeCondition b) {
		if (LogOp.And == b.getLogOp()) {
			return false;
		}
		//Traversiere weiter.
		return true;
	}

	/**
	 * MaxAnzahlUSB aus 'AnzahlAngegebenerUSB' bestimmen.
	 */
	@Override
	public boolean visit(IComparisonCondition vglBed) {
		Integer currentMaxAnzahlUsb = deriveMaxAnzahlUsb(vglBed.getOperation1(),
				vglBed.getOperation2(), vglBed.getCmpOp());

		if (currentMaxAnzahlUsb != null) {
			if (currentMaxAnzahlUsb == 1) {
				// Eine Einschränkung des USB auf 1 ist nicht zulässig (siehe EXT-8606).
				problemManager.reportProblem(
						new MVKParserError(rule, new ParserErrorHandler(locale).getParserErrorMessage(
								ParserErrorTypesEnumForHModel.MVK_MAX_INDEX_RESTRICTION_TO_ONE, "USB")));
				return false;
			} else if (currentMaxAnzahlUsb > 1) {
				maxUSB = deriveMinOfPositivInt(maxUSB, currentMaxAnzahlUsb);
			}
		}

		// Traversiere NICHT weiter!
		return false;
	}

	private Integer deriveMaxAnzahlUsb(IOperation feldOp1, IOperation feldOp2, ComparisonOperation vglOp) {
		if (feldOp1 instanceof INumberOfUsbOperation && FeldOperationUtil.isZahlKonstante(feldOp2)) {
			int anzahlUsb = FeldOperationUtil.getZahlKonstanteWert(feldOp2);
			if (ComparisonOperation.GT == vglOp
					|| ComparisonOperation.NE == vglOp) {
				return anzahlUsb;
			}
			if (ComparisonOperation.GE == vglOp) {
				return (--anzahlUsb);
			}
		}
		if (FeldOperationUtil.isZahlKonstante(feldOp1) && feldOp2 instanceof INumberOfUsbOperation) {
			int anzahlUsb = FeldOperationUtil.getZahlKonstanteWert(feldOp1);
			if (ComparisonOperation.LT == vglOp
					|| ComparisonOperation.NE == vglOp) {
				return anzahlUsb;
			}
			if (ComparisonOperation.LE == vglOp) {
				return (--anzahlUsb);
			}
		}

		return null;
	}

	/**
	 * Liefert die minimale positive Zahl der beiden übergebenen Parameter oder null, falls keiner
	 * der beiden Parameter positiv ist.
	 *
	 * @return Zahl >= 0 oder null
	 */
	private static Integer deriveMinOfPositivInt(Integer i1, Integer i2) {
		int x1 = ((i1 != null && i1 >= 0) ? i1 : Integer.MAX_VALUE);
		int x2 = ((i2 != null && i2 >= 0) ? i2 : Integer.MAX_VALUE);
		int min = Math.min(x1, x2);
		return (min != Integer.MAX_VALUE ? min : null);
	}
}
