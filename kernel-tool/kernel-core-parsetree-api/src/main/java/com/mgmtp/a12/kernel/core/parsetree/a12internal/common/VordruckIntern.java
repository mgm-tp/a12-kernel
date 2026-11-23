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

package com.mgmtp.a12.kernel.core.parsetree.a12internal.common;

import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IParseTreeEntity;
import com.mgmtp.a12.model.utils.OnlyForUsage;

/**
 * Die Klasse VordruckIntern beschreibt einen Vordruck / ein Formular.
 */
@OnlyForUsage
public class VordruckIntern {
	private String name;
	private long lfdNummer;
	private long usb;

	public VordruckIntern(String name, long lfdNummer) {
		this(name, lfdNummer, IParseTreeEntity.UNSPECIFIED);
	}

	/**
	 * Konstruktor, der den Namen und die laufende Nummer und den Usb setzt. Ist die laufende Nummer
	 * nicht relevant, so wird 0 angegeben. Ist der Usb nicht relevant, so wird 0 angegeben.
	 */
	public VordruckIntern(String name, long lfdNummer, long usb) {
		this.name = name;
		this.lfdNummer = lfdNummer;
		this.usb = usb;
	}

	public long getLfdNummer() {
		return lfdNummer;
	}

	public String getName() {
		return name;
	}

	public void setLfdNummer(long l) {
		lfdNummer = l;
	}

	public long getUsb() {
		return usb;
	}

	public void setUsb(long u) {
		usb = u;
	}

	public String toString() {
		String u = (usb == IParseTreeEntity.WILDCARD) ? "@*" : "";
		String l = (lfdNummer > 0) ? String.format("/%s", lfdNummer) : "";
		return String.format("%s%s%s", name, l, u);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (lfdNummer ^ (lfdNummer >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (int) (usb ^ (usb >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		VordruckIntern other = (VordruckIntern) obj;
		if (lfdNummer != other.lfdNummer) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (usb != other.usb) {
			return false;
		}
		return true;
	}

}
