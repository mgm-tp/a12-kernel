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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.mgmtp.a12.kernel.core.extmpspecifics.a12internal.api.ado.IExtmpEntity;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parser.internal.constants.EntityIdConstants;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.IParseTreeEntity;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.PathNotationConfig;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.api.condition.ICondition;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;

/**
 * Specifies the reference of a field or a group in the rule condition text.
 * <p>
 * As these objects are also used to get nice error messages, the name as used in the
 * condition text is saved as well.
 * <p>
 * EntityRef-Objects are saved in the {@link EntityRefMap}. The map provides an
 * easy access to the entity objects.
 * <p>
 * For H models, subclass {@link EntityRefForHModel} is used.
 */
public class EntityRef {

	private IEntity entity;

	/**
	 * list of indices for the entity.
	 * The indices specify the way, the entity is referenced in the condition text.
	 * <p>
	 * In A12 models: an index is either 1 (in case of non repeatable groups)
	 * or {@link IParseTreeEntity#ITERATION} if the group is repeatable and no wildcard is set
	 * or {@link IParseTreeEntity#WILDCARD} if the group is repeatable and a wildcard is set
	 */
	protected List<Long> indices = new ArrayList<>();

	/**
	 * used for error message and for formatting of the rule condition text
	 */
	private String nameUsedForErrorOutput = "";

	/**
	 * in case the entity was defined with a category
	 */
	private String categoryName;

	protected long usb = IParseTreeEntity.UNSPECIFIED;

	private SemanticIndexRef semanticIndex;

	// The filter condition is set during parse tree creation
	private ICondition filterCondition = null;
	// This flag is set during parsing
	private boolean hasFilterCondition = false;
	private boolean inFilterCondition = false;

	private int startIdxInCondition = -1;
	private int stopIdxInCondition = -1;

	private PathNotationConfig pathNotationConfig = PathNotationConfig.DEFAULT_MAYBELOSSY;

	private boolean isOuter = false;

	/**
	 * create and entity ref for the given entity and the name used in the condition text.
	 * The name used in the condition text is required for nice error messaging - in the error
	 * messages the full name of the entity should not be used.
	 * <p>
	 * Initialises the usb. If an usb is specified for the entity, the init value is
	 * {@link IParseTreeEntity#ITERATION}. If no usb is specified for the given entity,
	 * the init value is {@link IParseTreeEntity#UNSPECIFIED}
	 * <p>
	 * The list of indices is filled after the construction of the ref object.
	 */
	public EntityRef(IEntity entity, String nameUsedInConditionText) {
		this.entity = entity;
		this.nameUsedForErrorOutput = nameUsedInConditionText;

		// in case of an usb:  default is iteration.
		if (EntityUtils.hasMaxUsb(entity)) {
			usb = IParseTreeEntity.ITERATION;
		}
	}

	public String toString() {
		String o = isOuter ? "$" : "";
		String u = usb == IParseTreeEntity.UNSPECIFIED ? "" : String.valueOf(usb);
		String catName = categoryName == null ? "" : EntityIdConstants.CATEGORY_VALUE_ACCESS + categoryName;
		return String.format("%s%s(%s)%s-%s%s", o, entity.getFullName(), nameUsedForErrorOutput, indices.toString(), u, catName);
	}

	public List<Long> getIndices() {
		return indices;
	}

	//Generated hashCode, but we use only the fullName of the entity to get the hash.
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entity == null) ? 0 : entity.getFullName().hashCode());
		result = prime * result + ((indices == null) ? 0 : indices.hashCode());
		result = prime * result + (int) (usb ^ (usb >>> 32));
		result = prime * result + ((categoryName == null) ? 0 : categoryName.hashCode());
		result = prime * result + ((semanticIndex == null) ? 0 : semanticIndex.hashCode());
		result = prime * result + ((filterCondition == null) ? 0 : filterCondition.hashCode());
		result = prime * result + (isOuter ? 1 : 0);
		return result;
	}

	//Generated equals, but we use only the fullName of the entity to compare
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
		EntityRef other = (EntityRef) obj;
		if (entity == null) {
			if (other.entity != null) {
				return false;
			}
		} else if (other.entity == null) {
			return false;
		} else if (!entity.getFullName().equals(other.entity.getFullName())) {
			return false;
		}
		if (categoryName == null) {
			if (other.categoryName != null) {
				return false;
			}
		} else if (!categoryName.equals(other.categoryName)) {
			return false;
		}
		if (indices == null) {
			if (other.indices != null) {
				return false;
			}
		} else if (!indices.equals(other.indices)) {
			return false;
		}
		if (semanticIndex == null) {
			if (other.semanticIndex != null) {
				return false;
			}
		} else if (!semanticIndex.equals(other.semanticIndex)) {
			return false;
		}
		if (usb != other.usb) {
			return false;
		}
		if (!Objects.equals(filterCondition, other.filterCondition)) {
			return false;
		}
		return isOuter == other.isOuter;
	}

	public void setIndices(List<Long> indices) {
		this.indices = indices;
	}

	public void setIndex(int position, long index) {
		indices.set(position, index);
	}

	public void addIndex(Long l) {
		indices.add(l);
	}

	/**
	 * @return true if a wildcard is specified for any repetition layer.
	 */
	public boolean hasWildCards() {
		return indices.contains(IParseTreeEntity.WILDCARD) || getUsb() == IParseTreeEntity.WILDCARD;
	}

	public IEntity getEntity() {
		return entity;
	}

	public long getUsb() {
		return usb;
	}

	/**
	 * For ACEntities, this must be of the form <code>//modelName/...</code> The modelName is
	 * extracted from this representation. It must not start with <code>//</code> for other
	 * entities.
	 *
	 * @return a string representation, primarily used for nice error messages.
	 */
	public String getNameBuiltFromConditionText() {
		return nameUsedForErrorOutput;
	}

	public void setNameUsedForErrorOutput(String nameUsedForErrorOutput) {
		this.nameUsedForErrorOutput = nameUsedForErrorOutput;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public void setUsb(long usb) {
		throw new IllegalArgumentException("set USB not supported in A12 model");
	}

	public long getMaxLfdNr() {
		return EntityUtils.getMaxLfdNr(getEntity());
	}

	//used only in H models and Vordruck-Sprachkonstrukte.
	public void setLfdNr(long lfdNr) {
		if (indices.isEmpty()) {
			indices.add(lfdNr);
		} else {
			indices.set(0, lfdNr);
		}
	}

	public void setEntity(IEntity entity) {
		this.entity = entity;
	}

	public long[] getMax() {
		return entity.getMax();
	}

	public long getMaxUsb() {
		return entity instanceof IExtmpEntity ? ((IExtmpEntity) entity).getMaxUsb() : IExtmpEntity.UNDEFINED;
	}

	public long getLfdNr() {
		return indices.get(0);
	}

	public boolean hasWildCardsInGroup() {
		if (entity.isGroup()) {
			return hasWildCards();
		}
		// For fields, we ignore the line number
		return indices.subList(0, indices.size() - 1).contains(IParseTreeEntity.WILDCARD);
	}

	public void setSemanticIndex(SemanticIndexRef semanticIndexRef) {
		this.semanticIndex = semanticIndexRef;
	}

	public SemanticIndexRef getSemanticIndex() {
		return semanticIndex;
	}

	public void setStartAndStopIdxInCondition(int startIdxInCondition, int stopIdxInCondition) {
		this.startIdxInCondition = startIdxInCondition;
		this.stopIdxInCondition = stopIdxInCondition;
	}

	public int getStartIdxInCondition() {
		return startIdxInCondition;
	}

	public int getStopIdxInCondition() {
		return stopIdxInCondition;
	}

	public ICondition getFilterCondition() {
		return filterCondition;
	}

	public void setFilterCondition(ICondition filterCondition) {
		this.filterCondition = filterCondition;
	}

	public boolean isInFilterCondition() {
		return inFilterCondition;
	}

	public void setInFilterCondition(boolean inFilterCondition) {
		this.inFilterCondition = inFilterCondition;
	}

	public boolean hasFilterCondition() {
		return hasFilterCondition;
	}

	public void setHasFilterCondition(boolean hasFilterCondition) {
		this.hasFilterCondition = hasFilterCondition;
	}

	public PathNotationConfig getPathNotationConfig() {
		return pathNotationConfig;
	}

	public void setPathNotationConfig(PathNotationConfig pathNotationConfig) {
		this.pathNotationConfig = pathNotationConfig;
	}

	public boolean isOuter() {
		return isOuter;
	}

	public void setOuter(boolean outer) {
		isOuter = outer;
	}
}
