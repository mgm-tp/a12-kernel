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

import org.antlr.v4.runtime.Token;

import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntityListeContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.KontextListenBedingungContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.Lenient_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.ExpandMode;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.ParseTreeCreator;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.CreatorUtils;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.EntityListUtil;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.creation.util.FeldIdListeUtil;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.EntityIdImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.EntityIdListImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.CompositeConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.EntityListConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.parsebaum.impl.bedingung.RootGroupConditionImpl;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IEntityId;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.EntityListConditionName;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.LogOp;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.types.RootGroupConditionName;

/**
 * * Erzeugt für die Grammatik-Regel
 * <p>
 * <code>kontextListenBedingung: kontextListenBedingungName '(' entityListe ')';<p>
 * kontextListenBedingungName:<p>
 * 'MindestensEinKontextAngegeben'| 'KeinKontextAngegeben' | 'KontextAngegeben' | <p>
 * 'KontextNichtAngegeben'        |'AlleKontexteAngegeben' | <p>
 * 'KontexteNichtGemeinsamAngegeben' | 'NichtAlleOderKeinKontextAngegeben';
 * </code>
 * <p>
 * das richtige Parsebaum-Objekt.
 * <p>
 * Falls expandiert werden soll, werden die Kontext-Sprachkonstrukte in die entsprechenden
 * Feld-Sprachkonstrukte expandiert.
 * <p>
 * Da in A12-Modellen die Vordrucks-Sprachkonstrukte verboten sind, werden hier für die Wurzel-Kontexte die entsprechenden
 * Vordruck-Parsebaum-Objekte erzeugt.
 */
public class KontextListenBedingungCreator {

	// nur für bessere Lesbarkeit.
	private static final EntityListConditionName MIND_EIN_FELD = EntityListConditionName.AtLeastOneFieldFilled;
	private static final EntityListConditionName KEIN_FELD = EntityListConditionName.NoFieldFilled;

	private KontextListenBedingungCreator() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Depending on the expansion mode set for the given {@link ParseTreeCreator} the
	 * <ul>
	 * <li>{@link KontextListenBedingungContext} is transformed to the corresponding
	 * {@link EntityListConditionImpl}.
	 * <li>groups are expanded into the contained fields (transitive hull), and, for
	 * {@link ExpandMode#EXPAND_AND_ADD_GROUPS}, the contained groups including the group itself.
	 * </li>
	 * </ul>
	 * <p>
	 * If the case of {@link ExpandMode#NO_EXPAND}, a {@link EntityListConditionImpl} for the
	 * groups or a {@link RootGroupConditionImpl} is returned depending on the occurrence of root
	 * groups.
	 *
	 * @param ctx
	 * @param tc
	 */
	public static void create(KontextListenBedingungContext ctx, ParseTreeCreator tc) {
		if (EntityListUtil.referencesRootGroup(ctx.entityListe(), tc.getParser())) {
			createOnly(ctx, tc);
			return;
		}
		switch (tc.getExpandMode()) {
			case NO_EXPAND:
				createOnly(ctx, tc);
				break;
			case EXPAND, EXPAND_AND_ADD_GROUPS:
				expandAndCreate(ctx, tc);
				break;
			case EXPAND_PARTIALLY:
				createOnly(ctx, tc);
		}
	}

	/**
	 * Erzeugt aus den Kontext-Sprachkonstrukten die entsprechenden Feld-Sprachkonstrukte.
	 * <ul>
	 * <li><code>MindestensEinKontextAngegeben</code> bzw. <code>KontextAngegeben</code> wird zu
	 * <code>MindestensEinFeldAngegeben</code>
	 * und alle Kontext-Parameter sind zu Feldlisten expandiert.</li>
	 * <li><code>AlleKontexteAngegeben(a,b,c)</code>wird zu <code>MindestensEinFeldAngegeben(ax)
	 * und MindestensEinFeldAngegeben(bx) und MindestensEinFeldAngegeben(bx)</code> wobei ax, bx,
	 * cx jeweils die expandierten Feldlisten sind.</li>
	 * <li><code>KeinKontextAngegeben</code> bzw. <code>KontextNichtAngegeben</code> wird zu
	 * <code>KeinFeldAngegeben</code> und alle Kontext-Parameter sind zu Feldlisten expandiert.</li>
	 * <li><code>NichtAlleAngegebenOderKeinKontextAngegeben(a,b,c)</code> wird zu
	 * <code>KeinFeldAngegeben(ax) Oder KeinFeldAngegeben(bx) Oder KeinFeldAngegeben(cx)</code>
	 * wobei ax, bx, cx jeweils die expandierten Feldlisten sind.</li>
	 * <li><code>KontexteNichtGemeinsamAngegeben(a,b,c)</code> wird zu
	 * <code>MindestensEinFeldAngegeben(ax,bx,cx) Und (KeinFeldAngegeben(ax) Oder KeinFeldAngegeben(bx) Oder KeinFeldAngegeben(cx))</code>
	 * wobei ax, bx, cx jeweils die expandierten Feldlisten sind.</li>
	 * </ul>
	 *
	 * @param kctx
	 * @param tc
	 */
	private static void expandAndCreate(KontextListenBedingungContext kctx, ParseTreeCreator tc) {
		Token t = kctx.kontextListenBedingungName().start;
		int type = t.getType();
		ExpandMode expandMode = tc.getExpandMode();
		switch (type) {
			//MindestensEinKontextAngegeben(a,b,c)  wird zu MindestensEinFeldAngegeben mit alle Kontexte zu Feldern expandiert
			case GrammarOnly_ConditionParser.MINDESTENS_EIN_KONTEXT_ANGEGEBEN, GrammarOnly_ConditionParser.KONTEXT_ANGEGEBEN: {
				ICondition bed = createFeldListenBedingung(kctx.entityListe(), MIND_EIN_FELD, tc.getParser(), expandMode);
				tc.push(bed);
				break;
			}
			//AlleKontexteAngegeben(a,b,c)  wird zu MindestensEinFeldAngegeben(ax) und MindestensEinFeldAngegeben(bx)
			// und MindestensEinFeldAngegeben(cx)
			// mit ax als expandierte Feldliste für Kontext a,  bx analog etc.
			case GrammarOnly_ConditionParser.ALLE_KONTEXTE_ANGEGEBEN: {
				List<ICondition> bedingungen = createBedingungen(kctx.entityListe(), MIND_EIN_FELD, tc.getParser(), expandMode);
				ICondition bed = createBedingung(bedingungen, LogOp.And);
				tc.push(bed);
				break;
			}
			//KeinKontextAngegeben(a,b,c)  wird zu KeinFeldAngegeben mit alle Kontexte zu Feldern expandiert
			case GrammarOnly_ConditionParser.KEIN_KONTEXT_ANGEGEBEN, GrammarOnly_ConditionParser.KONTEXT_NICHT_ANGEGEBEN: {
				ICondition bed = createFeldListenBedingung(kctx.entityListe(), KEIN_FELD, tc.getParser(), expandMode);
				tc.push(bed);
				break;
			}

			// NichtAlleAngegeben wird zu KeinFeldAngegeben(a) Oder KeinFeldAngegeben(b) Oder
			case GrammarOnly_ConditionParser.NICHT_ALLE_ODER_KEIN_KONTEXT_ANGEGEBEN: {
				List<ICondition> bedingungen = createBedingungen(kctx.entityListe(), KEIN_FELD, tc.getParser(), expandMode);
				ICondition bed = createBedingung(bedingungen, LogOp.Or);
				tc.push(bed);
				break;
			}
			// NichtGemeinsamAngegeben wird zu MindestensEinFeldAngegeben(a,b,c..) Und (KeinFeldAngegeben(a) Oder KeinFeldAngegeben(b)...)
			case GrammarOnly_ConditionParser.KONTEXTE_NICHT_GEMEINSAM_ANGEGEBEN: {
				ICondition bed1 = createFeldListenBedingung(kctx.entityListe(), MIND_EIN_FELD, tc.getParser(), expandMode);

				List<ICondition> bedingungen = createBedingungen(kctx.entityListe(), KEIN_FELD, tc.getParser(), expandMode);
				ICondition bed2 = createBedingung(bedingungen, LogOp.Or);
				ICondition bed = createBedingung(bed1, bed2, LogOp.And);
				tc.push(bed);
				break;
			}
			default:
				throw new IllegalArgumentException(CreatorUtils.getUnknownTokenErrorMessage(t));
		}
	}

	/**
	 * Erzeuge eine Kontext-Listenbedingung ohne Expandierung bzw. eine Vordruckbedingung, falls ein
	 * Vordruck referenziert wird.
	 *
	 * @param ctx
	 * @param tc
	 */
	private static void createOnly(KontextListenBedingungContext ctx, ParseTreeCreator tc) {
		if (EntityListUtil.referencesRootGroup(ctx.entityListe(), tc.getParser())) {
			// group conditions that reference a root group (only allowed in A12 models) are internally treated as root group conditions
			assert tc.getParser().isA12Model() && (ctx.entityListe().entitySpec().size() == 1);

			RootGroupConditionName conditionName = createRootGroupConditionNameFromContext(ctx);
			EntityRef ref = tc.getParser().getEntityRef(ctx.entityListe().entitySpec().get(0));
			IEntityId entityId = new EntityIdImpl(ref, !tc.getParser().isA12Model(), tc.getParser().getEc());
			String rootGroupName = EntityUtils.getRootGroupName(ref.getEntity().getFullName());
			tc.push(new RootGroupConditionImpl(conditionName, rootGroupName, ref.getLfdNr(), ref.getUsb(), entityId));
		} else {
			EntityListConditionName name = createBedingungNameFromContext(ctx);
			EntityIdListImpl fListe = FeldIdListeUtil.createEntityIdListeFromContext(ctx.entityListe(), tc.getParser(),
					ExpandMode.NO_EXPAND);

			tc.push(new EntityListConditionImpl(name, fListe, null));
		}
	}

	/**
	 * Konvertiert den Bedingungsnamen aus dem Grammatik-ContextObjekt in die internen Namen.
	 */
	private static EntityListConditionName createBedingungNameFromContext(KontextListenBedingungContext ctx) {
		Token t = ctx.kontextListenBedingungName().start;
		int type = t.getType();
		/*
		 * 'MindestensEinKontextAngegeben'| 'KeinKontextAngegeben' | 'KontextAngegeben' |
		 * 'KontextNichtAngegeben' |'AlleKontexteAngegeben' |'KontexteNichtGemeinsamAngegeben' |
		 * 'NichtAlleOderKeinKontextAngegeben'
		 */
		return switch (type) {
			case GrammarOnly_ConditionParser.MINDESTENS_EIN_KONTEXT_ANGEGEBEN -> EntityListConditionName.AtLeastOneGroupFilled;
			case GrammarOnly_ConditionParser.KONTEXT_NICHT_ANGEGEBEN, GrammarOnly_ConditionParser.KEIN_KONTEXT_ANGEGEBEN ->
					EntityListConditionName.NoGroupFilled;
			case GrammarOnly_ConditionParser.KONTEXT_ANGEGEBEN, GrammarOnly_ConditionParser.ALLE_KONTEXTE_ANGEGEBEN ->
					EntityListConditionName.AllGroupsFilled;
			case GrammarOnly_ConditionParser.KONTEXTE_NICHT_GEMEINSAM_ANGEGEBEN ->
					EntityListConditionName.GroupsNotCollectivelyFilled;
			case GrammarOnly_ConditionParser.NICHT_ALLE_ODER_KEIN_KONTEXT_ANGEGEBEN -> EntityListConditionName.NotAllGroupsFilled;
			default -> throw new IllegalArgumentException(CreatorUtils.getUnknownTokenErrorMessage(t));
		};
	}

	/**
	 * Konvertiert den Bedingungsnamen aus dem Grammatik-ContextObjekt in die internen Namen.
	 */
	private static RootGroupConditionName createRootGroupConditionNameFromContext(KontextListenBedingungContext ctx) {
		Token t = ctx.kontextListenBedingungName().start;
		int type = t.getType();
		return switch (type) {
			case GrammarOnly_ConditionParser.MINDESTENS_EIN_KONTEXT_ANGEGEBEN, GrammarOnly_ConditionParser.KONTEXT_ANGEGEBEN ->
					RootGroupConditionName.MindestensEinVordruckAngegeben;
			case GrammarOnly_ConditionParser.KONTEXT_NICHT_ANGEGEBEN, GrammarOnly_ConditionParser.KEIN_KONTEXT_ANGEGEBEN ->
					RootGroupConditionName.KeinVordruckAngegeben;
			case GrammarOnly_ConditionParser.ALLE_KONTEXTE_ANGEGEBEN,
				 GrammarOnly_ConditionParser.KONTEXTE_NICHT_GEMEINSAM_ANGEGEBEN,
				 GrammarOnly_ConditionParser.NICHT_ALLE_ODER_KEIN_KONTEXT_ANGEGEBEN ->
					throw new IllegalArgumentException("internal error: '" + t.getText() + "' not allowed with root group");
			default -> throw new IllegalArgumentException(CreatorUtils.getUnknownTokenErrorMessage(t));
		};
	}

	private static ICondition createBedingung(ICondition b1, ICondition b2, LogOp op) {
		List<ICondition> bedingungen = new ArrayList<>();
		bedingungen.add(b1);
		bedingungen.add(b2);
		return createBedingung(bedingungen, op);
	}

	public static ICondition createBedingung(List<ICondition> bedingungen, LogOp op) {
		if (bedingungen.size() == 1) {
			//wenn die Liste nur ein Element enthält, muss keine Zusammengesetzte
			//Bedingung erzeugt werden.
			return bedingungen.get(0);
		}

		//bei einer Expansion setzen wir Klammern.
		CompositeConditionImpl zb = new CompositeConditionImpl(op, bedingungen);
		zb.setGeklammert();
		return zb;

	}

	/**
	 * Create a entity list condition with the given name for each entity object contained in the
	 * given list.
	 *
	 * @param elCtx
	 * 		context describing the entity list.
	 * @param bName
	 * 		name of the entity list condition.
	 * @param parser
	 * 		the parser used to get parameters from.
	 * @param expandMode
	 * 		specifies, how group identifiers should be expanded in language constructs.
	 * @return alle erzeugten Bedingungen als Liste
	 */
	private static List<ICondition> createBedingungen(EntityListeContext elCtx, EntityListConditionName bName,
			Lenient_ConditionParser parser, ExpandMode expandMode) {
		List<ICondition> liste = new ArrayList<>();

		for (EntitySpecContext eCtx : elCtx.entitySpec()) {
			ICondition b = createFeldListenBedingungFromCtx(eCtx, bName, parser, expandMode);
			liste.add(b);
		}
		return liste;
	}

	/**
	 * Erzeuge eine FeldListenBedingung mit dem übergebenen Namen
	 *
	 * @param ctxs
	 * 		Kontext, der die Entitäten spezifiziert
	 * @param bName
	 * 		Name der FeldListenBedingung
	 * @param parser
	 * 		the parser used to get parameters from.
	 * @param expandMode
	 * 		specifies, how group identifiers should be expanded in language constructs.
	 * @return neu erzeugte FeldListenBedingung
	 */
	private static ICondition createFeldListenBedingung(EntityListeContext ctxs, EntityListConditionName bName,
			Lenient_ConditionParser parser, ExpandMode expandMode) {
		EntityIdListImpl fListe;
		fListe = FeldIdListeUtil.createEntityIdListeFromContext(ctxs, parser, expandMode);
		return new EntityListConditionImpl(bName, fListe, null);
	}

	/**
	 * Erzeugt eine FeldListenBedingung mit dem übergebenen Namen
	 *
	 * @param eCtx
	 * 		Kontext, der eine Entity spezifiziert
	 * @param bName
	 * 		Name der FeldListenBedingung
	 * @param parser
	 * 		the parser used to get parameters from.
	 * @param expandMode
	 * 		specifies, whether and how groups should be expanded.
	 * @return neu erzeugte FeldListenBedingung
	 */
	private static ICondition createFeldListenBedingungFromCtx(EntitySpecContext eCtx, EntityListConditionName bName,
			Lenient_ConditionParser parser, ExpandMode expandMode) {
		EntityIdListImpl fListe = FeldIdListeUtil.createFeldIdListeFromContext(eCtx, parser, expandMode);
		return new EntityListConditionImpl(bName, fListe, null);
	}
}
