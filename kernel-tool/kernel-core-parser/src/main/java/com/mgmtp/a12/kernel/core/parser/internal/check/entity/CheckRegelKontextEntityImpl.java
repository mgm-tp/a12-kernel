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
package com.mgmtp.a12.kernel.core.parser.internal.check.entity;

import com.mgmtp.a12.kernel.core.parser.internal.check.entity.hdv.CheckSpezifierHModelImpl;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnum;
import com.mgmtp.a12.kernel.core.parser.internal.error.ParserErrorTypesEnumForHModel;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.FullChecks_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.RegelKontextEntityContext;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRefForHModel;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRefUtils;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IParseTreeEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;

import jakarta.validation.constraints.NotNull;

/**
 * Stellt die Prüfungen für <code>RegelKontextEntity</code> bereit.
 * Wird für H- und A12-Modelle verwendet.
 * Folgendes wird geprüft:
 * <ul>
 * <li>Für den Regelkontext darf kein Stern referenziert werden.
 * {@link ParserErrorTypesEnum#MVK_INVALID_WILDCARD_FOR_RULEGROUP}
 * <p>
 * (Es wird bereits über die Grammatik sichergestellt, dass #Zeile nur für H-Modelle
 * referenziert werden kann.)
 * <li>Für H-Modelle: Falls der Regelkontext der Vordruck ist, muss ein Vordruck-Sprachkonstrukt
 * verwendet werden. {@link ParserErrorTypesEnumForHModel#MVK_ROOT_GROUP_REFERENCED_AS_GROUP}
 * <li>Für A12-Modelle: wenn iteriert wird, muss das Fehlerfeld im Regelkontext sein.
 * {@link ParserErrorTypesEnum#MVK_ERROR_FIELD_NOT_IN_RULEGROUP}</li>
 * <li>Der Regelkontext darf nicht leer sein (kann nur in H-Modellen vorkommen)
 * {@link ParserErrorTypesEnum#MVK_GROUP_IS_EMPTY}
 */
public class CheckRegelKontextEntityImpl extends CheckRegelKontextLenientEntityImpl {
	FullChecks_ConditionParser fullParser;

	public CheckRegelKontextEntityImpl(@NotNull FullChecks_ConditionParser parser) {
		super(parser);
		this.fullParser = parser;
	}

	@Override
	protected void checksBeforeEntityCreation(RegelKontextEntityContext rctx, String nameUsedInConditionText,
			EntitySpecContext ectx, String ruleGroup, IEntity e) {
		super.checksBeforeEntityCreation(rctx, nameUsedInConditionText, ectx, ruleGroup, e);
		//Wildcard is not allowed for RegelKontext,
		//it is allowed in the grammar, so we can generate nicer error messages.
		if (rctx.stern != null) {
			String errMessage = getErrorMessage(ParserErrorTypesEnum.MVK_INVALID_WILDCARD_FOR_RULEGROUP, nameUsedInConditionText);
			throw createMVKParserError(ectx, errMessage);
		}

		// H model; root group has to be specified using the Vordruck-sprachkonstrukte
		if (fullParser.isHModel() && EntityUtils.isRootGroup(ruleGroup)) {
			String errMessage = getErrorMessage(ParserErrorTypesEnumForHModel.MVK_ROOT_GROUP_REFERENCED_AS_GROUP);
			throw createMVKParserError(ectx, errMessage);
		}

		if (e == null) {
			// some IEC implementations does not contain empty groups
			String errMessage = fullParser.getMVKErrorHandler()
					.getParserErrorMessage(ParserErrorTypesEnum.MVK_RULE_GROUP_IS_EMPTY_OR_INVALID, ruleGroup);
			throw createMVKParserError(ectx, errMessage);
		}
	}

	@Override
	protected void postProcessEntityRef(RegelKontextEntityContext rctx, EntityRef ref, IEntity e, String nameUsedInConditionText) {
		super.postProcessEntityRef(rctx, ref, e, nameUsedInConditionText);
		// The setting of refIdx in the a12-model case is handled by the super method.
		if (fullParser.isHModel()) {
			// H-Modelle: Zeile-Iteration über den Kontext ist möglich - Überprüfe die Angaben.
			if (rctx.hash_zeile != null) {
				EntityRefForHModel refOld = (EntityRefForHModel) ref;
				EntityRefUtils.checkKontextZIteration(rctx, refOld, this);
				refOld.setZeile(IParseTreeEntity.ITERATION);
			}
			if (rctx.zeile_konkret != null) {
				// H-Modelle:  Konkrete Zeilenreferenzierung - Überprüfe die Angaben.
				String ls = rctx.ZiffernFolge().getText();
				if (!(ref instanceof EntityRefForHModel refOld)) {
					throw new IllegalArgumentException("Concrete reference to a row,  but not H model");
				}
				ParserErrorTypesEnumForHModel err = ParserErrorTypesEnumForHModel.MVK_INVALID_Z_FOR_GROUP;
				long max = EntityRefUtils.getMaxZeileApplicableForGroupsAlso(rctx, ref, err, this);

				long z = new CheckSpezifierHModelImpl(getParser())
						.checkIndex(ls, rctx, max, CheckSpezifierHModelImpl.ZEILE, ref.getNameBuiltFromConditionText());

				refOld.setZeile(z);
			}
		}

		//empty groups are not allowed
		EntityRefUtils.checkNoEmptyGroup(e, nameUsedInConditionText, fullParser, rctx);
	}

	@Override public FullChecks_ConditionParser getParser() {
		return fullParser;
	}
}
