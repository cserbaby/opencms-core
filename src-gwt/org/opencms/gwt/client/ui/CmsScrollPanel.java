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

package org.opencms.gwt.client.ui;

import org.opencms.gwt.client.ui.css.I_CmsLayoutBundle;
import org.opencms.gwt.client.util.CmsDebugLog;
import org.opencms.gwt.client.util.CmsFocusedScrollingHandler;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.ScrollPanel;

/**
 * Scroll panel implementation allowing focused scrolling.<p>
 */
public class CmsScrollPanel extends ScrollPanel {

    /***/
    protected class ResizeButton extends CmsPushButton {

        /**
         * Default constructor.<p>
         */
        public ResizeButton() {

            super();
            //setStyleName(I_CmsLayoutBundle.INSTANCE.buttonCss().cmsTransparentButton());
            setStyleName(I_CmsLayoutBundle.INSTANCE.buttonCss().resizeButton());

        }

        /**
         * @see com.google.gwt.user.client.ui.CustomButton#onAttach()
         */
        @Override
        protected void onAttach() {

            super.onAttach();
        }
    }

    /** The prevent outer scrolling handler. */
    private CmsFocusedScrollingHandler m_focusedScrollingHandler;

    /** The scroll handler registration. */
    private HandlerRegistration m_handlerRegistration;

    /** The preview handler registration. */
    HandlerRegistration m_previewHandlerRegistration;

    /** The button to resize the scrolling panel. */
    ResizeButton m_resize;

    /** The start X coordination. */
    int m_clientX;
    /** The start Y coordination. */
    int m_clientY;
    /** The start height. */
    double m_oldheight;

    /** The default height. */
    double m_defaultHeight = -1;

    /**
     * Constructor.<p>
     * 
     * @see com.google.gwt.user.client.ui.ScrollPanel#ScrollPanel()
     */
    public CmsScrollPanel() {

    }

    /**
     * Constructor to be used by {@link org.opencms.gwt.client.ui.CmsScrollPanelImpl}.<p>
     * 
     * @param root the root element of the widget
     * @param scrollabel the scrollable element of the widget
     * @param container the container element of the widget
     */
    protected CmsScrollPanel(Element root, Element scrollabel, Element container) {

        super(root, scrollabel, container);
        m_resize = new ResizeButton();

    }

    /**
     * @see com.google.gwt.user.client.ui.ScrollPanel#onAttach()
     */
    @Override
    protected void onAttach() {

        super.onAttach();
        m_resize.onAttach();

    }

    /**
     * Enables or disables the focused scrolling feature.<p>
     * Focused scrolling is enabled by default.<p>
     * 
     * @param enable <code>true</code> to enable the focused scrolling feature
     */
    public void enableFocusedScrolling(boolean enable) {

        if (enable) {
            if (m_handlerRegistration == null) {
                m_handlerRegistration = addScrollHandler(new ScrollHandler() {

                    public void onScroll(ScrollEvent event) {

                        ensureFocusedScrolling();
                    }
                });
            }
        } else if (m_handlerRegistration != null) {
            m_handlerRegistration.removeHandler();
            m_handlerRegistration = null;
        }
    }

    /**
     * Ensures the focused scrolling event preview handler is registered.<p>
     */
    protected void ensureFocusedScrolling() {

        if (m_focusedScrollingHandler == null) {
            m_focusedScrollingHandler = CmsFocusedScrollingHandler.installFocusedScrollingHandler(this);
        } else if (!m_focusedScrollingHandler.isRegistered()) {
            m_focusedScrollingHandler.register();
        }
    }

    /**
     * @param resize
     */
    public void setResizable(boolean resize) {

        if (m_resize != null) {
            if (resize) {
                getElement().appendChild(m_resize.getElement());
                adopt(m_resize);
                m_resize.addMouseDownHandler(new MouseDownHandler() {

                    public void onMouseDown(MouseDownEvent event) {

                        m_oldheight = Double.parseDouble(getElement().getStyle().getHeight().replace("px", ""));
                        m_clientX = event.getClientX();
                        m_clientY = event.getClientY();
                        CmsDebugLog.getInstance().printLine("Registering preview handler");
                        m_previewHandlerRegistration = Event.addNativePreviewHandler(new ResizeEventPreviewHandler());

                    }
                });
            } else {
                m_resize.removeFromParent();
            }
        }
    }

    /**
     * Drag and drop event preview handler.<p>
     * 
     * To be used while dragging.<p>
     */
    protected class ResizeEventPreviewHandler implements NativePreviewHandler {

        /**
         * @see com.google.gwt.user.client.Event.NativePreviewHandler#onPreviewNativeEvent(com.google.gwt.user.client.Event.NativePreviewEvent)
         */
        public void onPreviewNativeEvent(NativePreviewEvent event) {

            Event nativeEvent = Event.as(event.getNativeEvent());
            switch (DOM.eventGetType(nativeEvent)) {
                case Event.ONMOUSEMOVE:
                    // dragging
                    setNewHeight(nativeEvent);
                    break;
                case Event.ONMOUSEUP:
                    m_previewHandlerRegistration.removeHandler();
                    event.cancel();
                    nativeEvent.preventDefault();
                    nativeEvent.stopPropagation();
                    break;
                case Event.ONKEYDOWN:
                    break;
                case Event.ONMOUSEWHEEL:
                    //onMouseWheelScroll(nativeEvent);
                    break;
                default:
                    // do nothing
            }
            event.cancel();
            nativeEvent.preventDefault();
            nativeEvent.stopPropagation();
            onResize();
        }

    }

    /**
     * Sets the default height of the scrolling panel.
     * 
     * @param height 
     */
    public void setDefaultHeight(double height) {

        m_defaultHeight = height;
    }

    /**
     * Executed on mouse move while dragging.<p>
     * 
     * @param event the event
     */
    protected void setNewHeight(Event event) {

        double newheight = m_oldheight + (event.getClientY() - m_clientY);
        if (m_defaultHeight != -1) {
            if (newheight < m_defaultHeight) {
                newheight = m_defaultHeight;
            }
        }
        getElement().getStyle().setHeight(newheight, Unit.PX);
    }

}