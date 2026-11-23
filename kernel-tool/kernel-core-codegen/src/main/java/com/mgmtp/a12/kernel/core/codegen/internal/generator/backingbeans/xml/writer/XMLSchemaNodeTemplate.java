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

package com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.xml.writer;

import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.CharParameter;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.StringParameter;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.NullTerminal;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.STParameterClassBase;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.StringTemplateGroup;
import com.mgmtp.a12.kernel.core.extmpspecifics.a12internal.api.xml.XMLSchemaNode;

/**
 * Generiert die Informationen für einen XML-Schema-Knoten.
 */
@StringTemplateGroup("base")
public class XMLSchemaNodeTemplate extends STParameterClassBase {

	private static final String NULL_STRING = "nullString";
	private final XMLSchemaNode xmlSchemaNode;
	private final int position;

	public XMLSchemaNodeTemplate(XMLSchemaNode xmlSchemaKnoten, int position) {
		this.xmlSchemaNode = xmlSchemaKnoten;
		this.position = position;
	}

	public int getPosition() {
		return position;
	}

	public String getPseudoXPath() {
		return new StringParameter(xmlSchemaNode.getPseudoXpath()).toCode();
	}

	public String getZuordnungsTyp() {
		char ret;
		switch (xmlSchemaNode.getAssignmentType()) {
			case NONE:
				ret = 'X';
				break;
			case V:
				ret = 'V';
				break;
			case K:
				ret = 'K';
				break;
			case F:
				ret = 'F';
				break;
			case S:
				ret = 'S';
				break;
			case X:
				ret = 'X';
				break;
			default:
				ret = ' ';
		}
		return new CharParameter(ret).toCode();
	}

	public String getVordruck() {
		if (xmlSchemaNode.getRootGroup() == null) {
			return new NullTerminal().toCode(NULL_STRING);
		}

		return new StringParameter(xmlSchemaNode.getRootGroup()).toCode();
	}

	public String getKontext() {
		if (xmlSchemaNode.getGroup() == null) {
			return new NullTerminal().toCode(NULL_STRING);
		}
		return new StringParameter(xmlSchemaNode.getGroup()).toCode();
	}

	public String getFeld() {

		if (xmlSchemaNode.getField() == null) {
			return new NullTerminal().toCode(NULL_STRING);
		}
		return new StringParameter(xmlSchemaNode.getField()).toCode();
	}

	public boolean isMziWiederholung() {
		return xmlSchemaNode.isMziRepeat();
	}

	public boolean isVirtual() {
		return xmlSchemaNode.isVirtual();
	}
}
