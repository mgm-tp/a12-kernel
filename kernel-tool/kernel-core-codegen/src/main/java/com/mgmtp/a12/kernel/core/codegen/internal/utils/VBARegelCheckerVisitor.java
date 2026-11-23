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
package com.mgmtp.a12.kernel.core.codegen.internal.utils;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.mgmtp.a12.kernel.core.parser.internal.util.AnalyseService;
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
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IBoolConstantOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.ICompositeOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IConstantOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IEntityListOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IEntityListOperationWithValue;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IFieldIdOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IFieldIdOperationWithParameters;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IGroupOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.INumberOfRootGroupsOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.INumberOfUsbOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOpListOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOpListOperationWithParameter;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IPointInTimeOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateAddOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateConstructionOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateDiffOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateExtractionOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateRangeConstructionOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateRangeExtractionOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.IDateTimeConstructionOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.date.ITimeConstructionOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.BoolConstantOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.CalcOp;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.ComparisonOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.DateAddOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.DateDiffOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.DateExtractionOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.DateRangeExtractionOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.EntityListAndValueListConditionName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.EntityListConditionName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.EntityListOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.FieldIdOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.GroupOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.OpListOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.OperationType;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.PointInTimeOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.RootGroupConditionName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.ValueListConditionName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.ast.ASTVisitor;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;

/**
 * Visitor zur Ermittlung der relevanten Regeln für den VBA-Code. Über getFehlerMeldung kann ein
 * Fehlertext abgefragt werden.
 */
public class VBARegelCheckerVisitor extends ASTVisitor {
	/**
	 * Idea: See CommonUtils - pNegativeListEscapeSequence: Find unevenly escaped linefeed
	 */
	private static final Pattern UNEVENLY_ESCAPED_LINEFEED = Pattern.compile("(?<!\\\\)(\\\\\\\\)*\\\\n");

	private String fehlermeldung = null;
	private final String regelName;
	private final IEC ec;

	public VBARegelCheckerVisitor(String regelName, IEC ec) {
		this.regelName = regelName;
		this.ec = ec;
	}

	@Override
	public boolean visit(IEntityId id) {
		if (!StringUtils.isEmpty(id.getCategoryName())) {
			setFehlerSpracheelementNichtUnterstuetzt("Zugriff über Kategorien");
			return false;
		}
		if (!id.getSemanticIndex().isEmpty()) {
			setFehlerSpracheelementNichtUnterstuetzt("Semantischer Index");
			return false;
		}
		if (id.getFilterCondition() != null) {
			setFehlerSpracheelementNichtUnterstuetzt("Filterbedingung");
			return false;
		}
		if (AnalyseService.isParallelIteration(id, true)) {
			fehlermeldung = "Regel '" + regelName + "' enthält parallele Iteration. Dies wird für die Zielsprache VBA nicht unterstützt.";
			return false;
		}
		return false;
	}

	@Override
	public boolean visit(IEntityListOperation fop) {
		EntityListOperationName operatorName = fop.getOperatorName();
		switch (operatorName) {
			case Sum:
			case NumberOfFilledFields:
				return true;
			case MaximalAngegebenerZeilenindex:
			case MaxValue:
			case NumberOfFilledGroups:
			case FirstFilledValue:
			case NumberOfDifferentValues:
			default:
				setFehlerSpracheelementNichtUnterstuetzt(operatorName.getRuntimeName());
				return false;
		}
	}

	@Override
	public boolean visit(IEntityListOperationWithValue fop) {
		EntityListOperationName operatorName = fop.getOperatorName();
		switch (operatorName) {
			// Sollte AnzahlWertInFeldliste
			// für VBA unterstützt werden, soll noch geprüft werden ob ein Zugriff auf einen Kategorienwert vorhanden ist
			// (dies wird noch nicht erlaubt).
			case NumberOfValueInFields:
			default:
				setFehlerSpracheelementNichtUnterstuetzt(operatorName.getRuntimeName());
				return false;
		}
	}

	@Override
	public boolean visit(IEntityListCondition fop) {
		EntityListConditionName bedingungName = fop.getConditionName();
		switch (bedingungName) {
			case AllFieldsFilled:
			case NotAllFieldsFilled:
			case NoFieldFilled:
			case AtLeastOneFieldFilled:
			case FieldsNotCollectivelyFilled:
			case NotAllGroupsFilled:
			case AtLeastOneGroupFilled:
			case NoGroupFilled:
			case GroupsNotCollectivelyFilled:
			case AllGroupsFilled:
			case RepetitionNotUnique:
				return true;
			case MoreThanOneFieldFilled:
			case ZeilenNichtAufsteigend:
			case ZeilenLueckenlos:
			case NotExactlyOneFieldFilled:
			case ZeileNichtEindeutig:
			case FieldValuesNotUnique:
			case DateRangesOverlap:
			default:
				setFehlerSpracheelementNichtUnterstuetzt(bedingungName.getRuntimeName());
				return false;
		}
	}

	@Override
	public boolean visit(IOpListOperation fop) {
		OpListOperationName operatorName = fop.getOperatorName();
		switch (operatorName) {
			case Max:
			case Min:
			case Abs:
			default:
				setFehlerSpracheelementNichtUnterstuetzt(operatorName.getRuntimeName());
				return false;
		}
	}

	@Override
	public boolean visit(IOpListOperationWithParameter fop) {
		OpListOperationName operatorName = fop.getOperatorName();
		switch (operatorName) {
			case RoundAccounting:
			case RoundUp:
			case RoundDown:
			default:
				setFehlerSpracheelementNichtUnterstuetzt(operatorName.getRuntimeName());
				return false;
		}
	}

	@Override
	public boolean visit(IValueListCondition fop) {
		ValueListConditionName bedingungName = fop.getConditionName();
		switch (bedingungName) {
			// Sollten FeldWertInWerteListe und FeldWertNichtInWerteListe
			// für VBA unterstützt werden, soll noch geprüft werden ob ein Zugriff auf einen Kategorienwert vorhanden ist
			// (dies wird noch nicht erlaubt).
			case FieldValueIncludedInValueList:
			case FieldValueNotIncludedInValueList:
			default:
				setFehlerSpracheelementNichtUnterstuetzt(bedingungName.getRuntimeName());
				return false;
		}
	}

	@Override
	public void endVisit(INumberOfRootGroupsOperation fop) {
		setFehlerSpracheelementNichtUnterstuetzt("AnzahlAngegebeneVordrucke");
	}

	@Override
	public void endVisit(INumberOfUsbOperation fop) {
		setFehlerSpracheelementNichtUnterstuetzt("AnzahlAngegebenerUsb");
	}

	@Override
	public void endVisit(IBoolConstantOperation fop) {
		BoolConstantOperationName value = fop.getValue();
		switch (value) {
			case True:
			case False:
				return;
			default:
				setFehlerSpracheelementNichtUnterstuetzt(value.getRuntimeName());
				return;
		}
	}

	@Override
	public void endVisit(IPointInTimeOperation fop) {
		PointInTimeOperationName operatorName = fop.getOperatorName();
		switch (operatorName) {
			case Today:
				return;
			case BaseYear:
			case Now:
			default:
				setFehlerSpracheelementNichtUnterstuetzt(operatorName.getRuntimeName());
		}
	}

	@Override
	public boolean visit(IComparisonCondition fop) {
		ComparisonOperation vglOp = fop.getCmpOp();
		switch (vglOp) {
			case EQ:
			case GT:
			case NE:
			case LT:
			case GE:
			case LE:
			case NE10:
			case NE5:
			case NE2:
			case NE1:
				return true;
			case NM:// Nicht wie Muster
			case WM:// WieMuster
				return noLinefeedInRegex(fop);
			default:
				setFehlerSpracheelementNichtUnterstuetzt(vglOp.toString());
				return false;
		}
	}

	/**
	 * Filter rules with regular expressions that match linefeed after pattern compilation
	 */
	private boolean noLinefeedInRegex(final IComparisonCondition fop) {
		final IOperation rightOperand = fop.getOperation2();
		if (rightOperand instanceof final IConstantOperation constantOperation && (constantOperation.getValue().contains("\n") ||
				UNEVENLY_ESCAPED_LINEFEED.matcher(constantOperation.getValue()).find())) {
			fehlermeldung = String.format(
					"Die Regel '%s' enthält ein Muster mit Zeilenumbrüchen. Das wird für die Zielsprache VBA nicht unterstützt.",
					regelName);
			return false;
		}
		return true;
	}

	@Override
	public boolean visit(IFieldIdOperation fb) {
		FieldIdOperationName operatorName = fb.getOperatorName();
		switch (operatorName) {
			case RangeAsString:
			case FieldValue:
			case ValueAsDate:
				return true;
			case Length:
			case RoundAccountingValue:
			case RoundUpValue:
			case RoundDownValue:
			case AbsValue:
			case RangeAsNumber:
			case FieldValueAsNumber:
			case FieldValueAsString:
			case AktuelleVordruckNummer:
			default:
				setFehlerSpracheelementNichtUnterstuetzt(operatorName.getRuntimeName());
				return false;
		}
	}

	@Override
	public boolean visit(IGroupOperation gop) {
		GroupOperationName operatorName = gop.getOperatorName(); // at the moment only AktuelleWiederholung
		setFehlerSpracheelementNichtUnterstuetzt(operatorName.getRuntimeName());
		return false;
	}

	@Override
	public boolean visit(IFieldIdOperationWithParameters fop) {
		FieldIdOperationName operatorName = fop.getOperatorName();
		switch (operatorName) {
			case RangeAsString:
			case FieldValue:
			case ValueAsDate:
				return true;
			case Length:
			case RoundAccountingValue:
			case RoundUpValue:
			case RoundDownValue:
			case AbsValue:
			case RangeAsNumber:
			case FieldValueAsNumber:
			case FieldValueAsString:
			case AktuelleVordruckNummer:
			default:
				setFehlerSpracheelementNichtUnterstuetzt(operatorName.getRuntimeName());
				return false;
		}
	}

	@Override
	public boolean visit(IRootGroupCondition fop) {
		RootGroupConditionName bedingungName = fop.getConditionName();
		String grammarName = bedingungName.toString();
		switch (bedingungName) {
			case MindestensEinVordruckAngegeben:
				if (fop.getRootGroupIndex() != IParseTreeEntity.WILDCARD && fop.getUsb() != IParseTreeEntity.WILDCARD) {
					grammarName = "VordruckAngegeben";
				}
				break;
			case KeinVordruckAngegeben:
				if (fop.getRootGroupIndex() != IParseTreeEntity.WILDCARD && fop.getUsb() != IParseTreeEntity.WILDCARD) {
					grammarName = "VordruckNichtAngegeben";
				}
				break;
			case VordruckNichtFortlaufend:
			case UsbNichtFortlaufend:
			default:
				grammarName = bedingungName.toString();
		}
		setFehlerSpracheelementNichtUnterstuetzt(grammarName);
		return false;
	}

	@Override
	public boolean visit(IEntityListAndValueListCondition fop) {
		EntityListAndValueListConditionName bedingungName = fop.getConditionName();
		switch (bedingungName) {
			// Sollten NichtAlleFeldWerteInWerteListe, KeinFeldWertInWerteListe und MindestensEinFeldWertInWerteListe
			// für VBA unterstützt werden, soll noch geprüft werden ob ein Zugriff auf einen Kategorienwert vorhanden ist
			// (dies wird noch nicht erlaubt).
			case NotAllFieldValuesIncludedInValueList:
			case NoFieldValueIncludedInValueList:
			case AtLeastOneFieldValueIncludedInValueList:
			case AtLeastOneDateRangeOverlaps:
			case ValueNotConsistent:
			default:
				setFehlerSpracheelementNichtUnterstuetzt(bedingungName.getRuntimeName());
				return false;
		}
	}

	@Override
	public boolean visit(IDateConstructionOperation op) {
		setFehlerSpracheelementNichtUnterstuetzt("Datum");
		return false;
	}

	@Override
	public boolean visit(IDateTimeConstructionOperation op) {
		setFehlerSpracheelementNichtUnterstuetzt("DateTime");
		return false;
	}

	@Override
	public boolean visit(ITimeConstructionOperation op) {
		setFehlerSpracheelementNichtUnterstuetzt("Zeit");
		return false;
	}

	public boolean visit(IDateRangeConstructionOperation op) {
		setFehlerSpracheelementNichtUnterstuetzt("DatumBereich");
		return false;
	}

	@Override
	public boolean visit(IDateExtractionOperation fop) {
		DateExtractionOperationName operatorName = fop.getOperatorName();
		switch (operatorName) {
			case DayFromDate:
			case MonthFromDate:
			case YearFromDate:
				return true;
			case QuarterFromDate:
			default:
				setFehlerSpracheelementNichtUnterstuetzt(operatorName.getRuntimeName());
				return false;
		}
	}

	@Override
	public boolean visit(IDateDiffOperation fop) {
		DateDiffOperationName operatorName = fop.getOperatorName();
		switch (operatorName) {
			case DifferenceInSeconds:
			case DifferenceInMinutes:
			case DifferenceInHours:
			case DifferenceInDays:
			case DifferenceInMonths:
			case DifferenceInYears:
			default:
				setFehlerSpracheelementNichtUnterstuetzt(operatorName.getRuntimeName());
				return false;
		}
	}

	@Override
	public boolean visit(IDateAddOperation fop) {
		DateAddOperationName operatorName = fop.getOperatorName();
		switch (operatorName) {
			case AddYears:
			case AddMonths:
			case AddDays:
			case AddHours:
			case AddMinutes:
			case AddSeconds:
			default:
				setFehlerSpracheelementNichtUnterstuetzt(operatorName.getRuntimeName());
				return false;
		}
	}

	@Override
	public boolean visit(IDateRangeExtractionOperation fop) {
		DateRangeExtractionOperationName operatorName = fop.getOperatorName();
		switch (operatorName) {
			case StartOfDateRange:
			case EndOfDateRange:
			default:
				setFehlerSpracheelementNichtUnterstuetzt(operatorName.getRuntimeName());
				return false;
		}
	}

	@Override
	public boolean visit(ICompositeOperation fop) {
		CalcOp calcOp = fop.getCalcOp();
		switch (calcOp) {
			case ADD:
				if (fop.getOperations().get(0).getType() == OperationType.STRING) {
					setFehlerSpracheelementNichtUnterstuetzt("Stringkonkatenation");
					return false;
				}
				return true;
			case MULTIPLY:
			case SUBTRACT:
			case DIVIDE:
				return true;
			default:
				setFehlerSpracheelementNichtUnterstuetzt(calcOp.name());
				return false;
		}
	}

	@Override
	public boolean visit(ICompositeCondition fop) {
		List<ICondition> bedingungsListe = fop.getConditionList();
		Iterator<ICondition> iterator = bedingungsListe.iterator();
		while (iterator.hasNext()) {
			VBARegelCheckerVisitor vbaVisitor = new VBARegelCheckerVisitor(regelName, ec);
			ICondition nextBedingung = iterator.next();
			nextBedingung.traverse(vbaVisitor);
			if (!vbaVisitor.isWirdUnterstuetzt()) {
				// Punkt 5 im Algortithmus erstmal nicht berücksichtigen
				fehlermeldung = vbaVisitor.getFehlerMeldung();
				return false;
			}
		}
		// falls alle Elemente entfernt
		if (fop.getConditionList().isEmpty()) {
			setBedingungEnthaeltKeinUnterstuetzesSprachelement();
			return false;
		}
		return true;
	}

	@Override
	public boolean visit(IDateValidityCondition c) {
		setFehlerSpracheelementNichtUnterstuetzt(c.getConditionName().getRuntimeName() + " für Datumsangaben");
		return false;
	}

	private boolean isWirdUnterstuetzt() {
		return fehlermeldung == null;
	}

	private void setFehlerSpracheelementNichtUnterstuetzt(String sprachelement) {
		fehlermeldung = String.format("Das Sprachelement '%s' in der Regel '%s' wird für die Zielsprache VBA nicht unterstützt.",
				sprachelement, regelName);
	}

	private void setBedingungEnthaeltKeinUnterstuetzesSprachelement() {
		fehlermeldung = String.format("Die Regel '%s' enthält kein für die Zielsprache VBA unterstütztes Sprachelement.",
				regelName);
	}

	public String getFehlerMeldung() {
		return fehlermeldung;
	}
}
