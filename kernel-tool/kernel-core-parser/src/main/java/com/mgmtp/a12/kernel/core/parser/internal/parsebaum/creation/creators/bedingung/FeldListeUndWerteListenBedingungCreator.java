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

import java.util.List;

import org.antlr.v4.runtime.Token;

import com.mgmtp.a12.kernel.core.parser.internal.check.util.CheckWerteListenUtils;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldListeUndWerteListenBedingungContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.WertListenSpezifierContext;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.ExpandMode;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.CreatorUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.FeldIdListeUtil;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.EntityListAndValueListConditionImpl;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityIdList;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.EntityListAndValueListConditionName;

/**
 * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>feldListeUndWerteListenBedingung: <p>
 * feldListeUndWerteListenBedingungName '(' entityListe inOp wertListenSpezifier  ')';<p>
 * <p>
 * feldListeUndWerteListenBedingungName: <p>
 * 'NichtAlleFeldWerteInWerteListe' | 'KeinFeldWertInWerteListe' | 'MindestensEinFeldWertInWerteListe' |
 * 'MindestensEinDatumsBereichUeberlappend';<p>
 * wertListenSpezifier: entityListe | stringListe;
 * </code>
 * <p>
 * das richtige Parsebaum-Objekt.
 */
public class FeldListeUndWerteListenBedingungCreator {

	private FeldListeUndWerteListenBedingungCreator() {
		throw new IllegalStateException("Utility class");
	}

	public static void create(FeldListeUndWerteListenBedingungContext ctx, ParseTreeCreator tc) {
		EntityListAndValueListConditionName name = createBedingungNameFromContext(ctx);

		ExpandMode expandMode = ExpandMode.limitToExpand(tc.getExpandMode());
		IEntityIdList ids = FeldIdListeUtil.createEntityIdListeFromContext(ctx.entityListe(), tc.getParser(), expandMode);

		WertListenSpezifierContext wctx = ctx.wertListenSpezifier();

		List<String> werte = null;
		IEntityIdList wertIds = null;
		if (wctx.stringListe() != null) {//Werte wurden über eine String-Liste spezifiziert.
			werte = CheckWerteListenUtils.readListe(wctx.stringListe());
		} else {//Werte wurde über eine Entity-Liste spezifiziert.
			wertIds = FeldIdListeUtil.createEntityIdListeFromContext(wctx.entityListe(), tc.getParser(), expandMode);
		}
		EntityListAndValueListConditionImpl result = new EntityListAndValueListConditionImpl(name, ids, wertIds, werte, null);

		tc.push(result);
	}

	/**
	 * Konvertiert den Bedingungsnamen aus dem Kontext in die internen Namen.
	 */
	private static EntityListAndValueListConditionName createBedingungNameFromContext(FeldListeUndWerteListenBedingungContext ctx) {
		Token t = ctx.feldListeUndWerteListenBedingungName().start;
		int type = t.getType();
		switch (type) {
			case GrammarOnly_ConditionParser.NICHT_ALLE_FELD_WERTE_IN_WERTE_LISTE:
				return EntityListAndValueListConditionName.NotAllFieldValuesIncludedInValueList;
			case GrammarOnly_ConditionParser.KEIN_FELD_WERT_IN_WERTE_LISTE:
				return EntityListAndValueListConditionName.NoFieldValueIncludedInValueList;
			case GrammarOnly_ConditionParser.MINDESTENS_EIN_FELD_WERT_IN_WERTE_LISTE:
				return EntityListAndValueListConditionName.AtLeastOneFieldValueIncludedInValueList;
			case GrammarOnly_ConditionParser.MINDESTENS_EIN_DATUMSBEREICH_UEBERLAPPEND:
				return EntityListAndValueListConditionName.AtLeastOneDateRangeOverlaps;
			default:
				throw new IllegalArgumentException(CreatorUtils.getUnknownTokenErrorMessage(t));
		}
	}

}
