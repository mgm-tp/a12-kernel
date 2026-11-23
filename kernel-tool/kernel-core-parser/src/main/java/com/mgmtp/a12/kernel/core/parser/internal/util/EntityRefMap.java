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

import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.v4.runtime.ParserRuleContext;

import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;

/**
 * Provides a map to save all references to fields or groups of the rule condition text.
 * <p>
 * <b>key of the map:</b> is the parser context in the rule condition. This enables a direct access
 * to the
 * entity object and the entity ref object in a later traversal.
 * <p>
 * <b>value of the map:</b> is a {@link EntityRef}
 * <p>
 * During a parse-run the parts of the path are incrementally read.
 * If a part of the path is read, the EntityRef Object is changed accordingly.
 */
public class EntityRefMap {

	private final Map<ParserRuleContext, EntityRef> entityMap = new LinkedHashMap<>();

	public Map<ParserRuleContext, EntityRef> getEntityMap() {
		return entityMap;
	}

	public EntityRef getEntityRef(ParserRuleContext ctx) {
		return entityMap.get(ctx);
	}

	/**
	 * Speichert die Entity zu dem angegebenen Kontext in der Map.
	 * Falls zu dem Kontext bereits eine Referenz gespeichert wird, werden die
	 * Indices in der neuen Referenz übernommen.
	 * <p>
	 * die Methode ist zugeschnitten für eine Schrittweise Überprüfung einer Pfadangabe.
	 * Wird beispielsweise in der Regel <code>kx/kx*</code> referenziert, so wird zunächst geprüft
	 * ob kx vorhanden ist, dann wird
	 * geprüft ob kx/kx vorhanden ist etc.
	 *
	 * @param ctx
	 * 		used as the key in the map
	 * @param e
	 * 		entity for the reference
	 * @param nameUsedInConditionText
	 * 		used later on for nice error messages
	 * @param isHModel
	 * 		specifies if the entity is an H or A12 model entity
	 */
	public EntityRef putEntity(ParserRuleContext ctx, IEntity e, String nameUsedInConditionText, boolean isHModel) {
		EntityRef ref = getEntityRef(ctx);
		if (ref != null) {
			//there is already an entry for the given context. Use the already specified indices.
			ref.setNameUsedForErrorOutput(nameUsedInConditionText);
			ref.setEntity(e);
		} else {
			ref = isHModel ? new EntityRefForHModel(e, nameUsedInConditionText) : new EntityRef(e, nameUsedInConditionText);
			entityMap.put(ctx, ref);
		}
		return ref;
	}

}
