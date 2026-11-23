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

package com.mgmtp.a12.kernel.core.codegen.internal.generator.vba.utils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.mgmtp.a12.kernel.core.extmpspecifics.internal.vba.VBAFieldGroupData;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;

/**
 * Computes the relevant groups for a VBA module.
 */
public class VBAGroupFilter {

	private IEC ec;

	public VBAGroupFilter(IEC ec) {
		this.ec = ec;
	}

	/**
	 * Determines the relevant group for each field group of the given module. This is defined as
	 * the lowest group containing all fields in the field group.
	 * The repeatability of the group is not considered exactly since it can be changed by rules
	 * (EXT-11184) and would hence not correspond to the saved repeatability. Since the
	 * repeatability
	 * can only shrink, we are looking for a group with repeatability less than or equal to the
	 * repeatability of the field group.
	 *
	 * @param fieldGroupData
	 * 		the VBA module for which the relevant groups is determined.
	 * @return The set of relevant groups for the field groups of the given module.
	 */
	public Set<String> determineRelevantGroups(List<VBAFieldGroupData> fieldGroupData) {
		return fieldGroupData.stream()
				.map(f -> determineRelevantGroup(f.getFields(), f.getRepeatability()))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toSet());
	}

	/**
	 * Determines the relevant group of a field group.
	 *
	 * @param fields
	 * 		the set of contained fields.
	 * @param repeatability
	 * 		the repeatability of the field group.
	 * @return the relevant group.
	 */
	public Optional<String> determineRelevantGroup(List<String> fields, long repeatability) {
		return fields.stream()
				.reduce(this::determineCommonGroup)
				.map(k -> k.substring(0, k.lastIndexOf('/'))) // entfernt '/' am Ende oder den Feldnamen bei nur einem Feld
				.map(k -> findeAhnMitWiederholbarkeitKleinerGleich(k, repeatability));
	}

	private String determineCommonGroup(String entityPfad1, String entityPfad2) {
		int index = entityPfad1.lastIndexOf('/');
		while (index >= 0) {
			if (entityPfad1.regionMatches(0, entityPfad2, 0, index + 1)) {
				return entityPfad1.substring(0, index + 1);
			}
			index = entityPfad1.lastIndexOf('/', index - 1); // add trailing '/' for reduce
		}
		return "";
	}

	private String findeAhnMitWiederholbarkeitKleinerGleich(String kontextName, long repeatability) {
		IEntity entity = ec.get(kontextName);
		if (entity == null) {
			if (kontextName.lastIndexOf('/') >= 0) {
				String newKontextName = kontextName.substring(0, kontextName.lastIndexOf('/'));
				return findeAhnMitWiederholbarkeitKleinerGleich(newKontextName, repeatability);
			} else {
				return "";
			}
		}
		long[] max = entity.getMax();
		for (int e = max.length - 1; e >= 1; e--) {
			if (max[e] <= repeatability && max[e] > 1) {
				return EntityUtils.getPathUpToLevel(e, kontextName);
			}
		}
		// Der Vordruck kann auch zusätzlich eingeschränkt werden.
		// Dann kann über die Wiederholbarkeit nicht mehr plausibilisiert werden.
		// Wenn vorher kein zulässiger wiederholbarer Kontext gefunden wurde, liefern wir daher immer den Vordruck.
		return EntityUtils.getPathUpToLevel(0, kontextName);
	}

}
