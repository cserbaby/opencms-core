/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.ade.contenteditor.client.widgets;

import com.alkacon.acacia.client.widgets.I_EditWidget;

import org.opencms.ade.contenteditor.client.css.I_CmsLayoutBundle;
import org.opencms.ade.galleries.client.ui.CmsGalleryField;

import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;

/**
 *
 * */
public class CmsFileWidget extends Composite implements I_EditWidget {

    /** Value of the activation. */
    private boolean m_active = true;

    /** If files should selectable. */
    private boolean m_includeFiles;

    /** The link selector. */
    private CmsGalleryField m_linkSelect;

    /** The reference path. */
    private String m_referencePath;

    /** If the site selector should be visible. */
    private boolean m_showSiteSelector;

    /** The start site. */
    private String m_startSite;

    /** The resource types. */
    private String m_types;

    /**
     * Constructs an CmsComboWidget with the in XSD schema declared configuration.<p>
     * 
     * @param openerTitle the gallery opener title
     * @param config the widget configuration string
     */
    public CmsFileWidget(String openerTitle, String config) {

        m_linkSelect = new CmsGalleryField();
        m_linkSelect.addStyleName(I_CmsLayoutBundle.INSTANCE.widgetCss().galleryWidget());
        parseConfiguration(config);
        m_linkSelect.setVfsConfiguration(m_includeFiles, m_showSiteSelector, m_startSite, m_referencePath, m_types);
        m_linkSelect.setGalleryOpenerTitle(openerTitle);
        m_linkSelect.addStyleName(I_CmsLayoutBundle.INSTANCE.widgetCss().galleryWidget());
        m_linkSelect.addValueChangeHandler(new ValueChangeHandler<String>() {

            public void onValueChange(ValueChangeEvent<String> event) {

                fireChangeEvent();

            }
        });
        // All composites must call initWidget() in their constructors.
        initWidget(m_linkSelect);

    }

    /**
     * @see com.google.gwt.event.dom.client.HasFocusHandlers#addFocusHandler(com.google.gwt.event.dom.client.FocusHandler)
     */
    public HandlerRegistration addFocusHandler(FocusHandler handler) {

        return null;
    }

    /**
     * @see com.google.gwt.event.logical.shared.HasValueChangeHandlers#addValueChangeHandler(com.google.gwt.event.logical.shared.ValueChangeHandler)
     */
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {

        return addHandler(handler, ValueChangeEvent.getType());
    }

    /**
     * Represents a value change event.<p>
     * 
     */
    public void fireChangeEvent() {

        ValueChangeEvent.fire(this, m_linkSelect.getFormValueAsString());
    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#getValue()
     */
    public String getValue() {

        return m_linkSelect.getFormValueAsString();
    }

    /**
     * @see com.alkacon.acacia.client.widgets.I_EditWidget#isActive()
     */
    public boolean isActive() {

        return m_active;
    }

    /**
     * @see com.alkacon.acacia.client.widgets.I_EditWidget#onAttachWidget()
     */
    public void onAttachWidget() {

        super.onAttach();
    }

    /**
     * @see com.alkacon.acacia.client.widgets.I_EditWidget#setActive(boolean)
     */
    public void setActive(boolean active) {

        if (m_active == active) {
            return;
        }
        m_active = active;
        if (active) {
            fireChangeEvent();
        }

    }

    /**
     * @see com.alkacon.acacia.client.widgets.I_EditWidget#setName(java.lang.String)
     */
    public void setName(String name) {

        m_linkSelect.setName(name);

    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object)
     */
    public void setValue(String value) {

        setValue(value, false);

    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object, boolean)
     */
    public void setValue(String value, boolean fireEvents) {

        m_linkSelect.setFormValueAsString(value);
        if (fireEvents) {
            fireChangeEvent();
        }

    }

    /**
     * Parses the configuration.<p>
     * 
     * @param configuration the widget configuration
     */
    private native void parseConfiguration(String configuration)/*-{
        var config = @org.opencms.gwt.client.util.CmsDomUtil::parseJSON(Ljava/lang/String;)(configuration);
        if (config.includefiles)
            this.@org.opencms.ade.contenteditor.client.widgets.CmsFileWidget::m_includeFiles = config.includefiles;
        if (config.showsiteselector)
            this.@org.opencms.ade.contenteditor.client.widgets.CmsFileWidget::m_showSiteSelector = config.showsiteselector;
        if (config.startsite)
            this.@org.opencms.ade.contenteditor.client.widgets.CmsFileWidget::m_startSite = config.startsite;
        if (config.referencepath)
            this.@org.opencms.ade.contenteditor.client.widgets.CmsFileWidget::m_referencePath = config.referencepath;
    }-*/;
}