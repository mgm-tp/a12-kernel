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

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.ArrayUtils;

import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.util.FeldIdUtil;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IParseTreeEntity;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IComparisonCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICompositeCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IDateValidityCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListAndValueListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IEntityListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IRootGroupCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.IValueListCondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IEntityListOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IFieldIdOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IGroupOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.INumberOfRootGroupsOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOpListOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateDiffOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateExtractionOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.ComparisonOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.FieldIdOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.GroupOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.LogOp;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.ValidityConditionName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.ast.ASTVisitor;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.services.FeldOperationUtil;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;

/**
 * Visitor, der eine Bedingung daraufhin untersucht, ob sie eine nicht erlaubte Iteration
 * verwendet (für den Typ der geprüften Iteration siehe {@link #iterationType}). Nicht erlaubt
 * ist eine solche Iteration, wenn die Regel auch für Wiederholungen feuern kann, für die
 * keine Felder angegeben worden sind. Der Visitor wird nur verwendet, wenn in der gesamten
 * Bedingung eine entsprechende Iteration referenziert wird.
 * <p>
 * <b>Precondition:</b> In der Bedingung wird mindestens eine Iteration referenziert.
 * <p>
 * Zur Beschreibung des Algorithmus brauchen wir folgende Begriffe:
 * <p>
 * <b>PositivBedingung</b>: ist eine Bedingung, die nur wahr werden kann, wenn ein Wert für die
 * Wiederholung angegeben ist.
 * <p>
 * <b>NegativBedingung</b>: ist eine Bedingung, die wahr wird, wenn Werte für die Wiederholung
 * nicht angegeben sind. Negativbedingungen dürfen daher für Iteration nicht verwendet werden,
 * außer sie werden in einer Kombination verwendet, dass ihre Verwendung nicht dazu führt, dass
 * für jede Wiederholung ein Wert angegeben sein muss.
 * <p>
 * Diese Klasse bietet den Analyse Algorithmus dazu. Bei der Bewertung einer Positivbedingung
 * ist wichtig, ob sie eine entsprechende Iteration referenziert. So ist beispielsweise:
 * <code>FeldAngegeben(A) Und FeldNichtAngegeben(B@#Usb)</code> nicht erlaubt.
 * <code>FeldAngegeben(A)</code> ist zwar eine Positivbedingung aber sie referenziert keine USB
 * Iteration. Das bedeutet: Ist für A ein Wert angegeben, dann muss für jeden USB für B ein Wert
 * angegeben sein. D.h. eine solche Regelbedingung ist nicht erlaubt. Daher muss neben dem Test
 * auf Positiv- bzw. Negativbedingung jeweils geprüft werden, wie die Bedingung die Iteration
 * referenziert.
 *
 * <p>
 * <br>
 * Die betroffenen Bedingungen sind:
 *
 * <ul>
 * <li>Einfache Bedingungen:</li>
 * <ul>
 * <li><b>VordruckNichtVorhanden</b></li>
 * <li><b>FeldNichtAngegeben</b> bzw. <b>KontextNichtAngegeben</b></li>
 * <li><b>NichtAlleFelderAngegeben</b> bzw. <b>NichtAlleKontexteAngegeben</b>, wenn mindestens
 * ein Feld in der Parameterliste Iteration spezifiziert</li>
 * <li><b>KeinFeldAngegeben</b> bzw. <b>KeinKontextAngegeben</b>, wenn mindestens ein Feld in
 * der Parameterliste Iteration spezifiziert</li>
 * <li><b>NichtGenauEinFeldAngegeben</b>, wenn mindestens ein Feld in der Parameterliste
 * Iteration spezifiziert</li>
 * <li><b>FeldAngegeben, KontextAngegeben</b>, wenn es keine Iteration spezifiziert
 * <li><b>MindestensEinFeldAngegeben, MindestensEinKontextAngegeben</b>, wenn es Felder mit und
 * ohne Iteration referenziert
 * <li><b>MehrAlsEinFeldAngegeben</b>, wenn es Felder mit und ohne Iteration referenziert
 * <li><b>FelderNichtGemeinsamAngegeben, KontexteNichtGemeinsamAngegeben</b>, wenn es Felder mit
 * und ohne Iteration referenziert
 * <li><b>KeinFeldWertInWerteListe</b>, wenn mindestens ein Feld in der Parameterliste eine
 * Iteration spezifiziert
 * <li><b>MindestensEinFeldWertInWerteListe</b>, wenn in der Feldliste und in der Werteliste
 * Felder ohne Iteration und in mindestens einer der Listen Felder mit Iteration vorkommen.
 * <li><b>FeldWerteNichtEindeutig</b>, <b>FeldWert(Nicht)InWerteListe</b>,
 * <b>NichtAlleFeldWerteInWerteListe</b> wenn Parameter mit und ohne Iteration verwendet
 * werden.</li>
 * <li><i>AnzahlAngegebenerFelder</i> bzw. <i>AnzahlAngegebenerKontexte</i>,
 * <i>AnzahlVerschiedenerWerte</i> und <i>MaximalAngegebenerZeilenindex</i> mit Vergleich auf
 * kleiner, auf kleiner gleich oder auf ungleich (auch UngleichMitToleranz) mit einer Zahlkonstante,
 * wenn alle Felder in der Feldliste eine Iteration spezifizieren.</li>
 * <li><i>AnzahlAngegebenerVordrucke</i> mit Vergleich auf kleiner, auf kleiner gleich oder auf
 * ungleich mit einer Zahlkonstante, wenn der Vordruck eine Iteration spezifiziert.</li>
 * <li><i>Summme</i>, <i>SummeVonProdukten</i>, <i>MaxWert</i>, <i>MinWert</i>,
 * Datumsdifferenzen/-extraktionen, <i>AnzahlWertInFeldliste</i>, <i>Auf/Ab/Runden(Wert)</i>,
 * <i>FeldWertAlsZahl</i>, <i>Laenge</i>, <i>Min/Max</i> wenn Vergleich mit 0 (mit ==, >= oder
 * <=), da dies zu einem Fehler führt, wenn nichts angegeben wurde.</li>
 * <li><i>AnzahlAngegebenerFelder</i> bzw. <i>AnzahlAngegebenerKontexte</i>,
 * <i>AnzahlVerschiedenerWerte</i>, <i>Summme</i>, <i>MaxWert</i>, <i>MinWert</i>,
 * <i>AnzahlWertInFeldliste</i>, <i>Auf/Ab/Runden</i>, <i>Min/Max</i> bei Vergleich mit einer
 * Zahlkonstante, wenn Parameter mit und ohne Iteration verwendet werden.</li>
 * </ul>
 * </ul>
 * <ul>
 * <li>Zusammengesetzte Bedingungen:</li>
 * <ul>
 * Bei den zusammengesetzten Bedingungen werden die Teilbedingungen geprüft.
 * <li>Und-Bedingung: Wenn eine Teilbedingung mit Iteration erlaubt ist, dann ist die Bedingung
 * erlaubt. Falls keine solche Teilbedingung gefunden wird, ist die Iteration für die gesamte
 * Bedingung verboten.</li>
 * <li>Oder-Bedingung: Wenn die Iteration für eine Teilbedingung verboten ist, dann ist die
 * gesamte Bedingung verboten.</li>
 * </ul>
 * </ul>
 */
public class IterationNotValidVisitor extends ASTVisitor {

	private static final int NO_SIMPLE_ITERATION = -1;
	public static final int NO_SIMPLE_ITERATION_ERROR = -1;

	/**
	 * Specifies for which iteration type the checks are done.
	 */
	private final IterationType iterationType;

	private final int iterationLevel;

	/**
	 * Iteration type for that iteration is not allowed, if the condition can be true for a
	 * repetition without filled fields.
	 */
	private enum IterationType {
		USB,
		PARALLEL,
		SIMPLE
	}

	public static boolean testUSBIteration(ICondition b) {
		//* <b>Precondition:</b> In der Bedingung wird mindestens ein USB Muster referenziert.
		if (!AnalyseService.isUIteration(b)) {
			return true;
		}
		IterationNotValidVisitor visitor = new IterationNotValidVisitor(IterationType.USB, NO_SIMPLE_ITERATION);
		b.traverse(visitor);
		return visitor.erlaubt;
	}

	public static boolean testParallelIteration(ICondition b) {
		//* <b>Precondition:</b> In der Bedingung wird parallele Iteration referenziert.
		if (!AnalyseService.isParallelIteration(b, true)) {
			return true;
		}
		IterationNotValidVisitor visitor = new IterationNotValidVisitor(IterationType.PARALLEL, NO_SIMPLE_ITERATION);
		b.traverse(visitor);
		return visitor.erlaubt;
	}

	/**
	 * @return level with error for simple iteration (non guarded iteration), or
	 *        {@link #NO_SIMPLE_ITERATION_ERROR} if there is no error
	 */
	public static int testSimpleIteration(ICondition c, IEC ec, boolean isHModel) {
		SortedSet<Integer> itLevels = getIterationLevels(c, ec, isHModel);
		for (int i : itLevels) {
			IterationNotValidVisitor visitor = new IterationNotValidVisitor(IterationType.SIMPLE, i);
			c.traverse(visitor);
			if (!visitor.erlaubt) {
				return i;
			}
		}
		return NO_SIMPLE_ITERATION_ERROR;
	}

	private IterationNotValidVisitor(IterationType iterationType, int itLevel) {
		this.iterationType = iterationType;
		this.iterationLevel = itLevel;
	}

	private static SortedSet<Integer> getIterationLevels(IParseTreeEntity c, IEC ec, boolean isHModel) {
		final SortedSet<Integer> iterationsEbenen = new TreeSet<>();

		class Visitor extends ASTVisitor {
			boolean inFilterCondition;

			Visitor(boolean inFilterCondition) {
				this.inFilterCondition = inFilterCondition;
			}

			@Override
			public boolean visit(IEntityId id) {
				if (inFilterCondition && !id.isOuter()) {
					return false;
				}
				for (int i = 0; i < id.getIndices().length; i++) {
					if (id.getIndices()[i] == IParseTreeEntity.ITERATION &&
							isIterationRelevant(id, i, ec, isHModel)) {
						iterationsEbenen.add(i);
					}
				}
				if (id.getFilterCondition() != null) {
					id.getFilterCondition().traverse(new Visitor(true));
				}
				return true;
			}

			@Override
			public boolean visit(IRootGroupCondition ifb) {
				if (ifb.getEntityId() != null) {
					// If we have an entityId, check that
					return true;
				}
				if (ifb.getRootGroupIndex() == IParseTreeEntity.ITERATION) {
					IEntity rootGroup = ec.get(EntityUtils.createRootGroupPath(ifb.getRootGroupName()));
					if (rootGroup.getMax()[0] != 2) {
						iterationsEbenen.add(0);
					}
				}
				return false;
			}

		}

		Visitor v = new Visitor(false);
		c.traverse(v);
		return iterationsEbenen;
	}

	/**
	 * Non guarded iteration is forbidden with the following exceptions:
	 * <ul>
	 * <li>H models: For a root group with repeatability 2 iteration over this level is always
	 * allowed.</li>
	 * <li>A12 models: For a group or root group with index field and repeatability 2 iteration over
	 * this level is always allowed.</li>
	 * </ul>
	 *
	 * @param id
	 * 		entity reference from the condition
	 * @param i
	 * 		level for that the entity reference specifies iteration
	 * @param ec
	 * @param isHModel
	 * @return true if a non guarded iteration for the level is forbidden, false otherwise
	 */
	private static boolean isIterationRelevant(IEntityId id, int i, IEC ec, boolean isHModel) {
		long[] maxIndexes = id.getMaxValues();
		if (maxIndexes[i] != 2) {
			return true;
		}
		if (id.isField() && i == maxIndexes.length - 1) {
			return true;
		}
		// iteration on group (or root group) level with repeatability 2
		if (isHModel) {
			return false;
		}
		return !EntityUtils.getIndexFieldLevels(id.getFullName(), ec).contains(i);
	}

	private boolean erlaubt = true;

	@Override
	public boolean visit(ICompositeCondition zBed) {
		if (zBed.getLogOp() == LogOp.Or) {
			for (ICondition teilBed : zBed.getConditionList()) {
				IterationNotValidVisitor visitor = new IterationNotValidVisitor(iterationType, iterationLevel);
				teilBed.traverse(visitor);
				//wenn die ODER-Bedingung eine nicht erlaubte Teilbedingung hat, ist sie nicht erlaubt
				boolean isErlaubt = visitor.isErlaubt();
				if (!isErlaubt || !isIteration(teilBed)) {
					erlaubt = false;
					return false;
				}
			}
		} else { // Und
			for (ICondition teilBed : zBed.getConditionList()) {
				IterationNotValidVisitor visitor = new IterationNotValidVisitor(iterationType, iterationLevel);
				teilBed.traverse(visitor);
				//wenn die UND-Bedingung eine erlaubte Teilbedingung mit Iteration hat, ist sie auch erlaubt
				boolean isErlaubt = visitor.isErlaubt();
				if (isErlaubt && isIteration(teilBed)) {
					return false;
				}
			}
			erlaubt = false;
		}
		return false;//in der Schleife bereits traversiert -> höre auf
	}

	@Override
	public boolean visit(IRootGroupCondition ib) {
		switch (ib.getConditionName()) {
			case KeinVordruckAngegeben:
				if (isIteration(ib)) {
					erlaubt = false;
				}
				break;
			case MindestensEinVordruckAngegeben:
				if (!isIteration(ib)) {
					erlaubt = false;
				}
				break;
			default:
				return false;
		}
		return false;
	}

	@Override
	public boolean visit(IEntityListCondition b) {
		switch (b.getConditionName()) {

			case NotAllFieldsFilled:
			case NotAllGroupsFilled:
			case NoFieldFilled:
			case NotExactlyOneFieldFilled:
			case NoGroupFilled:
				//wenn diese Bedingungen mindestens ein Feld mit Iteration referenzieren, können sie wahr werden
				//auch für eine nicht angegebene Wiederholung
				if (isIteration(b)) {
					erlaubt = false;
				}
				break;
			case AtLeastOneFieldFilled:
			case FieldsNotCollectivelyFilled:
			case AtLeastOneGroupFilled:
			case GroupsNotCollectivelyFilled:
				//wenn diese Bedingungen mindestens ein Feld ohne Iteration referenzieren, können sie wahr werden
				//auch für einen nicht angegebene Wiederholung
				if (referenziertOhneIteration(b, 1)) {
					erlaubt = false;
				}
				break;
			case FieldValuesNotUnique:
			case MoreThanOneFieldFilled:
				if (referenziertOhneIteration(b, 2)) {
					erlaubt = false;
				}
				break;
			/*
			 * wir behandeln den Fall von AlleFelderAngegeben mit nur einem Feld analog zu
			 * FeldAngegeben
			 */
			case AllFieldsFilled:
				if (FeldIdUtil.referenceOneField(b) && referenziertOhneIteration(b, 1)) {
					erlaubt = false;
				}
				break;
			/*
			 * wir behandeln den Fall von AlleKontexteAngegeben mit nur einem Kontext resp. Feld
			 * analog zu
			 * KontextAngegeben
			 */
			case AllGroupsFilled:
				if (FeldIdUtil.referenceOneFieldorOneGroup(b) && referenziertOhneIteration(b, 1)) {
					erlaubt = false;
				}
				break;
			default:
				return false;
		}
		return false;
	}

	@Override
	public boolean visit(IComparisonCondition ib) {

		IOperation op1 = ib.getOperation1();
		IOperation op2 = ib.getOperation2();

		if (fordertExistenzWennVergleichMitKleiner(op1) ||
				fordertExistenzWennVergleichMitKleiner(op2)) {
			checkIfVergleichMitKleiner(op1, op2, ib.getCmpOp());
		}

		if ((fordertExistenzWennVergleichMitNull(op1) ||
				fordertExistenzWennVergleichMitNull(op2))
				&& (ib.getCmpOp().equals(ComparisonOperation.EQ) ||
				ib.getCmpOp().equals(ComparisonOperation.GE) ||
				ib.getCmpOp().equals(ComparisonOperation.LE))) {
			checkIfVergleichMitNull(op1, op2);
		}

		checkIfComparisonWithNumberConstant(op1, op2);
		checkIfComparisonWithNumberConstant(op2, op1);

		return true;
	}

	private void checkIfComparisonWithNumberConstant(IOperation op1, IOperation op2) {
		if (requiresExistenceIfComparisonWithNumberConstant(op1) && FeldOperationUtil.isZahlKonstante(
				op2) && referenziertMitUndOhneIteration(op1)) {
			erlaubt = false;
		}
	}

	private boolean requiresExistenceIfComparisonWithNumberConstant(IOperation op) {

		if (op instanceof IEntityListOperation) {
			IEntityListOperation fop = (IEntityListOperation) op;
			switch (fop.getOperatorName()) {
				case NumberOfFilledGroups:
				case NumberOfFilledFields:
				case NumberOfDifferentValues:
				case NumberOfValueInFields:
				case Sum:
				case MinValue:
				case MaxValue:
				case FirstFilledValue:
					return true;
				case SumOfProducts:
				case MaximalAngegebenerZeilenindex:
					return false;
				default:
					throw new IllegalArgumentException("Unknown IEntityListOperation " + fop.getOperatorName());
			}
		}
		return op instanceof IOpListOperation;
	}

	@Override
	public boolean visit(IEntityListAndValueListCondition b) {

		if (isIteration(b)) {

			switch (b.getConditionName()) {
				case NoFieldValueIncludedInValueList:
					//ein Feld mit Iteration reicht, damit es nicht erlaubt ist
					erlaubt = false;
					break;
				case AtLeastOneFieldValueIncludedInValueList:
					if (referencesWithoutIteration(b.getEntityList())
							&& (referencesWithoutIteration(b.getValueListAsEntityList()) || !b.getValueListAsStringList()
							.isEmpty())) {
						erlaubt = false;
					}
					break;
				case NotAllFieldValuesIncludedInValueList:
					if (referencesWithoutIteration(b.getEntityList())) {
						erlaubt = false;
					}
					break;
				case AtLeastOneDateRangeOverlaps:
				case ValueNotConsistent:
				default:
					break;
			}
		}
		return false;
	}

	@Override
	public boolean visit(IValueListCondition cond) {

		if (isIteration(cond) && !isIteration(cond.getField())) {
			switch (cond.getConditionName()) {
				case FieldValueIncludedInValueList:
					// is not allowed if the field does not iterate and in the value list are fields with and without iteration
					if (cond.getValueList().stream()
							.anyMatch(this::referencesWithoutIteration)) {
						erlaubt = false;
					}
					break;
				case FieldValueNotIncludedInValueList:
					// is not allowed if the field does not iterate and the value list does
					erlaubt = false;
					break;
			}
		}

		return false;
	}

	@Override
	public boolean visit(IDateValidityCondition b) {
		if (b.getConditionName() == ValidityConditionName.Invalid && isIteration(b)) {
			erlaubt = false;
		}
		return false;
	}

	@Override
	public boolean visit(IGroupOperation op) {
		if (op.getOperatorName() == GroupOperationName.CurrentRepetition && isIteration(op)) {
			erlaubt = false;
		}
		return false;
	}

	/**
	 * Gibt true zurück, genau dann wenn der Parsebaum eine Mischung aus Feldern mit und ohne
	 * Iteration enthaelt
	 *
	 * @param p
	 * 		zu prüfender Parsebaum
	 */
	private boolean referenziertMitUndOhneIteration(IParseTreeEntity p) {

		return referencesWithoutIteration(p) && isIteration(p);
	}

	/**
	 * Return true, if the parse tree entity contains at least one entity id without iteration
	 *
	 * @param p
	 * 		parse tree to check
	 */
	private boolean referencesWithoutIteration(IParseTreeEntity p) {
		boolean[] result = { false }; // enthaeltFeldOhneIteration
		class Visitor extends ASTVisitor {
			@Override
			public boolean visit(IEntityId id) {
				if (!isIteration(id)) {
					result[0] = true;
				}
				return false;
			}
		}
		p.traverse(new Visitor());
		return result[0];
	}

	/**
	 * wird nur aufgerufen, wenn Vergleich auf Gleichheit und eine fop, die Existenz fordert wenn
	 * vergleich mit 0.
	 */
	private void checkIfVergleichMitNull(IOperation op1, IOperation op2) {
		IOperation op = null;
		int z = -1;
		if (fordertExistenzWennVergleichMitNull(op1)) {
			op = op1;
			z = FeldOperationUtil.getZahlKonstanteWert(op2);
		} else {
			op = op2;
			z = FeldOperationUtil.getZahlKonstanteWert(op1);

		}
		if (op != null && z == 0 && alleFelderMitIteration(op)) {
			erlaubt = false;
		}
	}

	private void checkIfVergleichMitKleiner(
			IOperation op1,
			IOperation op2,
			ComparisonOperation vgl) {

		if (fordertExistenzWennVergleichMitKleiner(op1)) {
			int z = FeldOperationUtil.getZahlKonstanteWert(op2);
			if (z > 0 && (vgl.equals(ComparisonOperation.LE)
					|| vgl.equals(ComparisonOperation.LT)
					|| vgl.equals(ComparisonOperation.NE)
					|| vgl.equals(ComparisonOperation.NE1)
					|| vgl.equals(ComparisonOperation.NE2)
					|| vgl.equals(ComparisonOperation.NE5)
					|| vgl.equals(ComparisonOperation.NE10))) {
				checkIteration(op1);
			}
		}

		if (fordertExistenzWennVergleichMitKleiner(op2)) {
			int z = FeldOperationUtil.getZahlKonstanteWert(op1);
			if (z > 0 && (vgl.equals(ComparisonOperation.GE)
					|| vgl.equals(ComparisonOperation.GT)
					|| vgl.equals(ComparisonOperation.NE)
					|| vgl.equals(ComparisonOperation.NE1)
					|| vgl.equals(ComparisonOperation.NE2)
					|| vgl.equals(ComparisonOperation.NE5)
					|| vgl.equals(ComparisonOperation.NE10))) {
				checkIteration(op2);
			}
		}

	}

	private void checkIteration(IOperation op1) {
		if (op1 instanceof IEntityListOperation) {
			IEntityListOperation fop = (IEntityListOperation) op1;
			if (alleFelderMitIteration(fop.getEntityList())) {
				erlaubt = false;
			}
		}
		if (op1 instanceof INumberOfRootGroupsOperation &&
				isIteration((INumberOfRootGroupsOperation) op1)) {
			erlaubt = false;
		}
	}

	private boolean fordertExistenzWennVergleichMitKleiner(IOperation op) {
		if (op instanceof IEntityListOperation) {
			IEntityListOperation fop = (IEntityListOperation) op;
			switch (fop.getOperatorName()) {
				case NumberOfFilledGroups:
				case NumberOfFilledFields:
				case NumberOfDifferentValues:
				case MaximalAngegebenerZeilenindex:
				case NumberOfValueInFields:
					return true;
				case SumOfProducts:
				case Sum:
				case MinValue:
				case MaxValue:
				case FirstFilledValue:
					return false;
				default:
					throw new IllegalArgumentException("Unknown Operation name " + fop.getOperatorName());
			}
		}
		return op instanceof INumberOfRootGroupsOperation;
	}

	private boolean fordertExistenzWennVergleichMitNull(IOperation op) {
		if (op instanceof IEntityListOperation) {
			IEntityListOperation fop = (IEntityListOperation) op;
			switch (fop.getOperatorName()) {
				case Sum:
				case MinValue:
				case MaxValue:
				case FirstFilledValue:
				case SumOfProducts:
				case NumberOfValueInFields:
					return true;
				case MaximalAngegebenerZeilenindex:
				case NumberOfFilledGroups:
				case NumberOfFilledFields:
				case NumberOfDifferentValues:
					return false;
				default:
					throw new IllegalArgumentException("Unknown Operation name " + fop.getOperatorName());
			}
		}
		if ((op instanceof IFieldIdOperation) && ((IFieldIdOperation) op).getOperatorName() != FieldIdOperationName.AktuelleVordruckNummer) {
			return true;
		}
		if (op instanceof IOpListOperation) {
			return true;
		}
		if (op instanceof IDateDiffOperation) {
			return true;
		}
		return op instanceof IDateExtractionOperation;
	}

	private boolean alleFelderMitIteration(IParseTreeEntity p) {
		return isIteration(p) && !referencesWithoutIteration(p);
	}

	public boolean isErlaubt() {
		return erlaubt;
	}

	/**
	 * @param b
	 * 		condition to test
	 * @param minNum
	 * 		the minimum number of occurrences that lead to an error.
	 * @return <code>true</code> iff the given number of fields or root group conditions don't
	 * 		specify
	 * 		iteration.
	 */
	private boolean referenziertOhneIteration(IEntityListCondition b, int minNum) {
		if (b == null) {
			return false;
		}
		final AtomicLong num = new AtomicLong(0);
		class Visitor extends ASTVisitor {
			@Override
			public boolean visitGeneral(IParseTreeEntity e) {
				// escape, if condition met.
				return num.get() < minNum;
			}

			@Override
			public boolean visit(IEntityId id) {
				if (!isIteration(id)) {
					long repetitions = wildcardLevelRepetitions(id);
					num.addAndGet(repetitions);
				}
				return false;
			}
		}
		b.traverse(new Visitor());
		return num.get() >= minNum;
	}

	private boolean isIteration(INumberOfRootGroupsOperation op) {
		switch (iterationType) {
			case USB:
				return op.getUsb() == IParseTreeEntity.ITERATION;
			case PARALLEL:
			case SIMPLE:
			default:
				return false;
		}
	}

	private boolean isIteration(IRootGroupCondition cond) {
		if (cond.getEntityId() != null) {
			return isIteration(cond.getEntityId());
		}
		switch (iterationType) {
			case USB:
				return cond.getUsb() == IParseTreeEntity.ITERATION;
			case PARALLEL:
				return cond.getRootGroupIndex() == IParseTreeEntity.PARALLEL_ITERATION;
			case SIMPLE:
				return iterationLevel == 0 && cond.getRootGroupIndex() == IParseTreeEntity.ITERATION;
			default:
				return false;
		}
	}

	private boolean isIteration(IEntityId id) {
		if (!id.getSemanticIndex().isEmpty()) {
			Object semIdx = id.getSemanticIndex().get(0);
			if (semIdx instanceof IEntityId && isIteration((IEntityId) semIdx)) {
				return true;
			}
		}
		switch (iterationType) {
			case USB:
				return id.getUsb() == IParseTreeEntity.ITERATION;
			case PARALLEL:
				return Arrays.asList(ArrayUtils.toObject(id.getIndices())).contains(IParseTreeEntity.PARALLEL_ITERATION);
			case SIMPLE:
				if (iterationLevel >= id.getIndices().length) {
					return false;
				}
				return id.getIndices()[iterationLevel] == IParseTreeEntity.ITERATION;
			default:
				return false;
		}
	}

	private long wildcardLevelRepetitions(IEntityId id) {
		long result = 1;
		long[] indexes = id.getIndices();
		int pos = ArrayUtils.indexOf(indexes, IParseTreeEntity.WILDCARD);
		while (pos >= 0) {
			result *= id.getMaxValues()[pos];
			pos = ArrayUtils.indexOf(indexes, IParseTreeEntity.WILDCARD, pos + 1);
		}
		if (id.getUsb() == IParseTreeEntity.WILDCARD) {
			result *= id.getMaxUsb();
		}
		return result;
	}

	private boolean isIteration(IEntityListCondition cond) {
		return cond.getEntityList().getEntityIdList().stream()
				.anyMatch(this::isIteration);
	}

	private boolean isIteration(IParseTreeEntity p) {
		switch (iterationType) {
			case USB:
				return AnalyseService.isUIteration(p);
			case PARALLEL:
				return AnalyseService.isParallelIteration(p, false);
			case SIMPLE:
				return AnalyseService.isIteration(p, iterationLevel, false);
			default:
				return false;
		}
	}

}
