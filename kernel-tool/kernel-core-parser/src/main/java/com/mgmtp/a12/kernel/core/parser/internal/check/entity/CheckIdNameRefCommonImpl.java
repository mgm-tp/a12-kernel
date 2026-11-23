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

import org.antlr.v4.runtime.ParserRuleContext;

import com.mgmtp.a12.kernel.core.parser.internal.check.CheckImplLenientBaseClass;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.error.MVKParserError;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.GrammarOnly_ConditionParser.EntitySpecContext;
import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.Lenient_ConditionParser;
import com.mgmtp.a12.kernel.core.parser.internal.util.EntityRef;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.PathNotationConfig;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;

import jakarta.validation.constraints.NotNull;

/**
 * Klasse bietet gemeinsame Hilfsmethoden für H- und A12-Modelle an, die
 * für die Verarbeitung der Referenzierungen von Felder und Kontexten in den Regelbedingungen
 * verwendet werden.
 */
public abstract class CheckIdNameRefCommonImpl extends CheckImplLenientBaseClass {

	protected EntitySpecContext eSpecCtx;
	protected ParserRuleContext idNameCtx;

	protected CheckIdNameRefCommonImpl(@NotNull Lenient_ConditionParser parser) {
		super(parser);
	}

	protected String getRulePrefix() {
		return EntityUtils.getRuleGroupName(getRule());
	}

	//has to be abstract, as it is used from H models and A12 models.
	//both H and A12 models use different context-names.
	protected abstract boolean isAbsolutPath();

	protected void setCtxs(ParserRuleContext ctx) {
		if (ctx == null) {
			return;
		}
		eSpecCtx = getEnclosingEntitySpecContext(ctx.getParent());
		idNameCtx = ctx;
	}

	/**
	 * returns the name that is used in the condition text.
	 * If a reference is already saved for the actual entity context,
	 * the parameter <code>namePart</code> is appended to the already saved name.
	 * If no reference has been saved for the actual entity context, take the
	 * parameter <code>namePart</code> as the new name.
	 * <p>
	 * This method is required, as we read the parts of the name incrementally.
	 * e.g. /a/b is read in two steps: first a, then b. Thus the first call
	 * would be <code>getNameUsedInConditionText("a")</code> returning "/a" the
	 * second call would be <code>getNameUsedInConditionText("b")</code> returning "/a/b".
	 * <p>
	 * Note: method can be used in H and A12 models.
	 *
	 * @param namePart
	 * @return complete name used in the condition text.
	 */
	protected String getNameBuiltFromConditionText(String namePart) {
		EntityRef ref = getEntityRef(eSpecCtx);
		if (ref == null) {
			return isAbsolutPath() ? "/" + namePart : namePart;
		}

		return EntityUtils.createPath(ref.getNameBuiltFromConditionText(), namePart);
	}

	protected MVKParserError createMVKParserError(String msg) {
		if (idNameCtx == null) {
			return new MVKParserError(getRule(), msg);
		}
		return new MVKParserError(idNameCtx.getParent().start, idNameCtx.start, getRule(), msg);
	}

	/**
	 * adds the entity to the entity ref map
	 *
	 * @param e
	 * 		entity to be added
	 * @param nameUsedInConditionText
	 * 		name of the entity as used in the condition text.
	 * @param pathNotationConfig
	 * 		specifies if the reference of the entity in the condition text uses relative
	 * 		upward references {@link EntityUtils#PATH_UP}
	 * @return the created entity
	 */
	protected EntityRef putEntityToEntityMap(IEntity e, String nameUsedInConditionText, PathNotationConfig pathNotationConfig) {

		// add the entity to the map
		if (getEntityRef(eSpecCtx) == null) { // first time a namePart is read
			return parser.createEntryForEntity(eSpecCtx, e, nameUsedInConditionText, pathNotationConfig);
		} else {
			// Pass relative reference to the child
			return parser.putEntityToMap(eSpecCtx, e, nameUsedInConditionText, pathNotationConfig);
		}
	}

}
