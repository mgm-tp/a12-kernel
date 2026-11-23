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
package com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.creators.bedingung;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.Token;

import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckWerteListenUtils;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldUndWerteListenBedingungContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.WertListenSpezifier2Context;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.CreatorUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.FeldIdCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.ValueListConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.ConstantOperationImpl;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IFieldIdOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.FieldIdOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.OperationType;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.ValueListConditionName;

/**
 * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>feldUndWerteListenBedingung: <p>
 * feldUndWerteListenBedingungName '(' entitySpec ',' wertListenSpezifier2 ')';<p>
 * feldUndWerteListenBedingungName: 'FeldWertInWerteListe' | 'FeldWertNichtInWerteListe';
 * <p>
 * wertListenSpezifier2: stringListe | numberListe | feldWertOpListe;
 * </code>
 * <p>
 * das richtige Parsebaum-Objekt.
 */
public class FeldUndWerteListenBedingungCreator {

	private FeldUndWerteListenBedingungCreator() {
		throw new IllegalStateException("Utility class");
	}

	public static void create(FeldUndWerteListenBedingungContext ctx, ParseTreeCreator tc) {
		ValueListConditionName name = createBedingungNameFromContext(ctx);

		IEntityId feldId = FeldIdCreator.create(ctx.entitySpec(), tc);

		List<IOperation> fops = createFopsFromContext(ctx.wertListenSpezifier2(), tc);

		tc.push(new ValueListConditionImpl(name, feldId, fops));
	}

	/**
	 * Erzeugt aus dem übergebenen Kontext eine Liste von IFeldOperationen
	 * <p>
	 * Der Kontext modelliert die drei Alternativen:
	 * <p>
	 * <code>
	 * wertListenSpezifier2: stringListe | numberListe | feldWertOpListe;
	 * </code>
	 * <p>
	 *
	 * @param wlCtx
	 * @param tc
	 * @return Liste von IFeldOperationen, d.h. die benötigten IParsebaum-Objekte
	 */
	private static List<IOperation> createFopsFromContext(WertListenSpezifier2Context wlCtx, ParseTreeCreator tc) {
		List<IOperation> result = new ArrayList<>();
		// Alternative 1: string-Liste
		if (wlCtx.stringListe() != null) {
			//fops have to be created
			result = CheckWerteListenUtils.readListe(wlCtx.stringListe())
					.stream()
					.map(s -> new ConstantOperationImpl(s, OperationType.STRING))
					.collect(Collectors.toList());
		}

		// Alternative 2: number-Liste
		if (wlCtx.numberListe() != null) {
			//fops have to be created
			result = CheckWerteListenUtils.readListe(wlCtx.numberListe())
					.stream()
					.map(FeldUndWerteListenBedingungCreator::removeLeadingZeros)
					.map(s -> new ConstantOperationImpl(s, OperationType.NUMBER))
					.collect(Collectors.toList());
		}

		// Alternative 3: Werte-Liste
		if (wlCtx.feldWertOpListe() != null) {
			for (int i = 0; i < wlCtx.feldWertOpListe().feldWertOp().size(); i++) {
				IOperation fop = tc.popFop();
				//take the operation from the stack, have to be FeldWert-Operations.
				assert fop instanceof IFieldIdOperation : "unexpected class name " + fop.getClass().getName();
				assert ((IFieldIdOperation) fop).getOperatorName() == FieldIdOperationName.FieldValue : "unexpected fop found " + fop;
				result.add(0, fop); // make sure the ordering ist correct. (on a stack the ordering is reverse)
			}
		}

		assert !result.isEmpty();//bereits durch die Grammatik wird verhindert, dass dieser Fall auftritt.
		return result;
	}

	public static String removeLeadingZeros(String s) {
		assert s.length() > 0; //bereits durch die Grammatik zugesichert.
		int i = (s.charAt(0) == '-') ? 1 : 0;
		String sign = (s.charAt(0) == '-') ? "-" : "";
		String nu = s.substring(i)
				.replaceFirst("^0+(?!$)", ""); // Zahlkonstante enthält keine Nachkommastellen (durch Grammatik zugesichert)

		if (nu.equals("0")) { // handle the special case of -0
			sign = "";
		}
		return String.format("%s%s", sign, nu);
	}

	/**
	 * Konvertiert den Bedingungsnamen aus dem Kontext in die internen Namen.
	 */
	private static ValueListConditionName createBedingungNameFromContext(FeldUndWerteListenBedingungContext ctx) {
		Token t = ctx.feldUndWerteListenBedingungName().start;
		int type = t.getType();
		switch (type) {
			case GrammarOnly_ConditionParser.FELD_WERT_IN_WERTE_LISTE:
				return ValueListConditionName.FieldValueIncludedInValueList;
			case GrammarOnly_ConditionParser.FELD_WERT_NICHT_IN_WERTE_LISTE:
				return ValueListConditionName.FieldValueNotIncludedInValueList;
			default:
				throw new IllegalArgumentException(CreatorUtils.getUnknownTokenErrorMessage(t));
		}
	}

}
