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
package com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.creators.operation;

import org.antlr.v4.runtime.Token;

import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldListenAnzahlOpContext;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.ExpandMode;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.CreatorUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.EntityListUtil;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.FeldIdListeUtil;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.EntityIdListImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.EntityListOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.operation.NumberOfRootGroupsOperationImpl;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.operation.IOperation;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.EntityListOperationName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.OperationType;

/**
 * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>feldListenAnzahlOpName: 'AnzahlAngegebenerKontexte' | 'AnzahlAngegebenerFelder' | 'AnzahlVerschiedenerWerte';
 * </code>
 * <p>
 * das richtige Parsebaum-Objekt.
 * <p>
 * Für AnzahlAngegebenerKontext wird der Kontext auf gesonderte Art expandiert:
 * Dort wird der expandierten Feld-Liste eine FeldId mit dem Kontext vorangestellt.
 * <p>
 * Falls der Vordruck referenziert wird, wird nicht expandiert und das entsprechende
 * Vordruck-Parsebaum-Objekt erzeugt.
 */
public class FeldListenAnzahlOpCreator {

	private FeldListenAnzahlOpCreator() {
		throw new IllegalStateException("Utility class");
	}

	public static void create(FeldListenAnzahlOpContext ctx, ParseTreeCreator tc) {
		EntityListOperationName name = extractOperationNameFromContext(ctx);

		IOperation result;
		if (EntityListUtil.referencesRootGroup(ctx.entityListe(), tc.getParser())) {
			assert tc.getParser().isA12Model() && (ctx.entityListe().entitySpec()
					.size() == 1) && (name == EntityListOperationName.NumberOfFilledGroups);

			EntityRef ref = tc.getParser().getEntityRef(ctx.entityListe().entitySpec().get(0));
			String rootGroupName = EntityUtils.getRootGroupName(ref.getEntity().getFullName());
			result = new NumberOfRootGroupsOperationImpl(rootGroupName, ref.getUsb());
		} else {
			EntityIdListImpl fListe;
			if (name == EntityListOperationName.NumberOfFilledGroups) {
				fListe = FeldIdListeUtil.createExpFeldIdListeForAnzAngKontexte(ctx.entityListe(), tc);
			} else {
				ExpandMode expandMode = ExpandMode.limitToExpand(tc.getExpandMode());
				fListe = FeldIdListeUtil.createEntityIdListeFromContext(ctx.entityListe(), tc.getParser(), expandMode);
			}
			result = new EntityListOperationImpl(name, fListe, OperationType.NUMBER);
		}
		tc.push(result);
	}

	/**
	 * Konvertiert den Operationsnamen aus dem Kontext in die internen Namen.
	 */
	private static EntityListOperationName extractOperationNameFromContext(FeldListenAnzahlOpContext ctx) {
		Token t = ctx.feldListenAnzahlOpName().start;
		int type = t.getType();
		switch (type) {
			case GrammarOnly_ConditionParser.ANZAHL_ANGEGEBENER_FELDER:
				return EntityListOperationName.NumberOfFilledFields;
			case GrammarOnly_ConditionParser.ANZAHL_ANGEGEBENER_KONTEXTE:
				return EntityListOperationName.NumberOfFilledGroups;
			case GrammarOnly_ConditionParser.ANZAHL_VERSCHIEDENER_WERTE:
				return EntityListOperationName.NumberOfDifferentValues;
			default:
				throw new IllegalArgumentException(CreatorUtils.getUnknownTokenErrorMessage(t));
		}
	}

}
