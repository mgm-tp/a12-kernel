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

import org.antlr.v4.runtime.Token;

import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.FeldListenBedingungContext;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.ExpandMode;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.CreatorUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.FeldIdListeUtil;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.EntityIdListImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.EntityListConditionImpl;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.EntityListConditionName;

/**
 * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>feldListenBedingung: feldListenBedingungName '(' entityListe ')' ;
 * <p>
 * feldListenBedingungName: <p>
 * 'MindestensEinFeldAngegeben' | 'MehrAlsEinFeldAngegeben' | 'KeinFeldAngegeben' | <p>
 * 'FeldAngegeben' | 'FeldNichtAngegeben' |<p>
 * 'AlleFelderAngegeben' | 'FelderNichtGemeinsamAngegeben' | <p>
 * 'NichtAlleFelderOderKeinFeldAngegeben' | <p>
 * 'DatumsBereicheUeberlappend' | 'NichtGenauEinFeldAngegeben' | <p>
 * 'FeldWerteNichtEindeutig';
 * </code>
 * <p>
 * das richtige Parsebaum-Objekt.
 */
public class FeldListenBedingungCreator {

	private FeldListenBedingungCreator() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Erzeugt aus dem Parser-Objekt ein Parsebaum Objekt.
	 */
	public static void create(
			FeldListenBedingungContext ctx, ParseTreeCreator tc) {
		EntityListConditionName name = createBedingungNameFromContext(ctx);
		ExpandMode expandMode = ExpandMode.limitToExpand(tc.getExpandMode());
		EntityIdListImpl fListe = FeldIdListeUtil.createEntityIdListeFromContext(ctx.entityListe(), tc.getParser(), expandMode);

		tc.push(new EntityListConditionImpl(name, fListe, null));
	}

	/**
	 * Konvertiert den Bedingungsnamen aus dem Context in die internen Namen.
	 * <p>
	 * Falls die Bedingung normaliziert werden soll, werden die Sprachkonstrukte
	 * <ul>
	 * <li>
	 * <code>FeldAngegeben</code> in <code>AlleFelderAngegeben</code>
	 * <li><code>FeldNichtAngegeben</code> in <code>KeinFeldAngegeben</code>
	 * </ul>
	 * konvertiert.
	 */
	public static EntityListConditionName createBedingungNameFromContext(FeldListenBedingungContext ctx) {
		Token t = ctx.feldListenBedingungName().start;
		int type = t.getType();
		switch (type) {
			case GrammarOnly_ConditionParser.MINDESTENS_EIN_FELD_ANGEGEBEN:
				return EntityListConditionName.AtLeastOneFieldFilled;
			case GrammarOnly_ConditionParser.MEHR_ALS_EIN_FELD_ANGEGEBEN:
				return EntityListConditionName.MoreThanOneFieldFilled;
			case GrammarOnly_ConditionParser.FELD_NICHT_ANGEGEBEN:
			case GrammarOnly_ConditionParser.KEIN_FELD_ANGEGEBEN:
				return EntityListConditionName.NoFieldFilled;
			case GrammarOnly_ConditionParser.FELD_ANGEGEBEN:
			case GrammarOnly_ConditionParser.ALLE_FELDER_ANGEGEBEN:
				return EntityListConditionName.AllFieldsFilled;
			case GrammarOnly_ConditionParser.FELDER_NICHT_GEMEINSAM_ANGEGEBEN:
				return EntityListConditionName.FieldsNotCollectivelyFilled;
			case GrammarOnly_ConditionParser.NICHT_ALLE_FELDER_ODER_KEIN_FELD_ANGEGEBEN:
				return EntityListConditionName.NotAllFieldsFilled;
			case GrammarOnly_ConditionParser.DATUMS_BEREICHE_UEBERLAPPEND:
				return EntityListConditionName.DateRangesOverlap;
			case GrammarOnly_ConditionParser.NICHT_GENAU_EIN_FELD_ANGEGEBEN:
				return EntityListConditionName.NotExactlyOneFieldFilled;
			case GrammarOnly_ConditionParser.FELD_WERTE_NICHT_EINDEUTIG:
				return EntityListConditionName.FieldValuesNotUnique;

			default:
				throw new IllegalArgumentException(CreatorUtils.getUnknownTokenErrorMessage(t));
		}

	}

}
