/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.android.resources.controls;

import java.util.HashMap;
import java.util.Map;

import pxb.android.axml.AxmlVisitor;
import soot.SootClass;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.resources.controls.LayoutControl;

/**
 * Data class representing a layout control on the android screen
 * 
 * @author Steven Arzt
 *
 */
public abstract class AndroidLayoutControl extends LayoutControl {

	protected int id;
	protected SootClass viewClass;
	protected String clickListener = null;

	private Map<String, Object> additionalAttributes = null;

	AndroidLayoutControl(SootClass viewClass) {
		this(-1, viewClass);
	}

	public AndroidLayoutControl(int id, SootClass viewClass) {
		this.id = id;
		this.viewClass = viewClass;
	}

	public AndroidLayoutControl(int id, SootClass viewClass, Map<String, Object> additionalAttributes) {
		this(id, viewClass);
		this.additionalAttributes = additionalAttributes;
	}

	public int getID() {
		return this.id;
	}

	public SootClass getViewClass() {
		return this.viewClass;
	}

	/**
	 * Adds an additional attribute to this layout control
	 * 
	 * @param key   The key of the attribute
	 * @param value The value of the attribute
	 */
	public void addAdditionalAttribute(String key, String value) {
		if (additionalAttributes != null)
			additionalAttributes = new HashMap<>();
		additionalAttributes.put(key, value);
	}

	/**
	 * Gets the handler for user clicks
	 * 
	 * @return The name of the click handler if one exists, otherwise null
	 */
	public String getClickListener() {
		return clickListener;
	}

	/**
	 * Gets the additional attributes associated with this layout control
	 * 
	 * @return The additional attributes associated with this layout control
	 */
	public Map<String, Object> getAdditionalAttributes() {
		return additionalAttributes;
	}

	/**
	 * Sets the unique identifier of the layout control
	 * 
	 * @param id The unique identifier of the layout control
	 */
	void setId(int id) {
		this.id = id;
	}

	/**
	 * Handles the given attribute and copies the data from the attribute into this
	 * data object
	 * 
	 * @param attribute        The attribute to handle
	 * @param loadOptionalData True if optional data, that is not directly necessary
	 *                         for understanding the semantics of the control, shall
	 *                         be loaded as well
	 */
	void handleAttribute(AXmlAttribute<?> attribute, boolean loadOptionalData) {
		final String attrName = attribute.getName().trim();
		final int type = attribute.getType();

		if (attrName.equals("id")) {
			id = attribute.asInteger();
		} else if (isActionListener(attrName) && type == AxmlVisitor.TYPE_STRING
				&& attribute.getValue() instanceof String) {
			clickListener = attribute.asString().trim();
		} else if (loadOptionalData) {
			if (additionalAttributes == null)
				additionalAttributes = new HashMap<>();
			additionalAttributes.put(attrName, attribute.getValue());
		}
	}

	/**
	 * Checks whether this name is the name of a well-known Android listener
	 * attribute. This is a function to allow for future extension.
	 * 
	 * @param name The attribute name to check. This name is guaranteed to be in the
	 *             android namespace.
	 * @return True if the given attribute name corresponds to a listener, otherwise
	 *         false.
	 */
	protected boolean isActionListener(String name) {
		return name.equals("onClick");
	}

	@Override
	public String toString() {
		return id + " - " + viewClass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((additionalAttributes == null) ? 0 : additionalAttributes.hashCode());
		result = prime * result + ((clickListener == null) ? 0 : clickListener.hashCode());
		result = prime * result + id;
		result = prime * result + ((viewClass == null) ? 0 : viewClass.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AndroidLayoutControl other = (AndroidLayoutControl) obj;
		if (additionalAttributes == null) {
			if (other.additionalAttributes != null)
				return false;
		} else if (!additionalAttributes.equals(other.additionalAttributes))
			return false;
		if (clickListener == null) {
			if (other.clickListener != null)
				return false;
		} else if (!clickListener.equals(other.clickListener))
			return false;
		if (id != other.id)
			return false;
		if (viewClass == null) {
			if (other.viewClass != null)
				return false;
		} else if (!viewClass.equals(other.viewClass))
			return false;
		return true;
	}

}
