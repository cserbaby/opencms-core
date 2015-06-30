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
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.workplace.tools.modules;

import org.opencms.file.CmsObject;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.CmsSystemInfo;
import org.opencms.main.OpenCms;
import org.opencms.module.CmsModule;
import org.opencms.module.CmsModuleImportExportHandler;
import org.opencms.report.I_CmsReportThread;
import org.opencms.util.CmsFileUtil;
import org.opencms.util.CmsStringUtil;
import org.opencms.workplace.list.A_CmsListReport;
import org.opencms.workplace.threads.CmsExportThread;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;

/**
 * Provides a report for exporting modules.<p>
 *
 * @since 6.0.0
 */
public class CmsModulesListExportReport extends A_CmsListReport {

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsModulesListExportReport.class);

    /** Modulename. */
    private String m_paramModule;

    /**
     * Public constructor with JSP action element.<p>
     *
     * @param jsp an initialized JSP action element
     */
    public CmsModulesListExportReport(CmsJspActionElement jsp) {

        super(jsp);
    }

    /**
     * Public constructor with JSP variables.<p>
     *
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsModulesListExportReport(PageContext context, HttpServletRequest req, HttpServletResponse res) {

        this(new CmsJspActionElement(context, req, res));
    }

    /**
     * Gets the module parameter.<p>
     *
     * @return the module parameter
     */
    public String getParamModule() {

        return m_paramModule;
    }

    /**
     *
     * @see org.opencms.workplace.list.A_CmsListReport#initializeThread()
     */
    @Override
    public I_CmsReportThread initializeThread() {

        I_CmsReportThread exportThread = new CmsExportThread(getCms(), getExportHandler(), false);

        return exportThread;
    }

    /**
     * Sets the module parameter.<p>
     * @param paramModule the module parameter
     */
    public void setParamModule(String paramModule) {

        m_paramModule = paramModule;
    }

    /**
     * Gets the module export handler containing all resources used in the module export.<p>
     * @return CmsModuleImportExportHandler with all module resources
     */
    private CmsModuleImportExportHandler getExportHandler() {

        String moduleName = getParamModule();

        // get all module resources
        CmsModule module = OpenCms.getModuleManager().getModule(moduleName);
        List<String> resList = module.getResources();

        // check if all resources are valid
        List<String> resListCopy = new ArrayList<String>();

        CmsObject resourceCheckCms = getCms();
        try {
            resourceCheckCms = OpenCms.initCmsObject(resourceCheckCms);
            String importSite = module.getImportSite();
            if (!CmsStringUtil.isEmptyOrWhitespaceOnly(importSite)) {
                resourceCheckCms.getRequestContext().setSiteRoot(importSite);
            }
        } catch (CmsException e) {
            // should never happen
            LOG.error(e.getLocalizedMessage(), e);
        }
        for (String res : resList) {
            try {
                if (res != null) {
                    resourceCheckCms.readResource(res);
                    resListCopy.add(res);
                }
            } catch (CmsException e) {
                // resource did not exist / could not be read
                if (LOG.isInfoEnabled()) {
                    LOG.warn(Messages.get().getBundle().key(Messages.ERR_READ_RESOURCE_1, res), e);
                }
            }
        }
        resListCopy = CmsFileUtil.removeRedundancies(resListCopy);
        String[] resources = new String[resListCopy.size()];

        for (int i = 0; i < resListCopy.size(); i++) {
            resources[i] = resListCopy.get(i);
        }

        String filename = OpenCms.getSystemInfo().getAbsoluteRfsPathRelativeToWebInf(
            OpenCms.getSystemInfo().getPackagesRfsPath()
                + CmsSystemInfo.FOLDER_MODULES
                + moduleName
                + "_"
                + module.getVersion().toString());

        CmsModuleImportExportHandler moduleExportHandler = new CmsModuleImportExportHandler();
        moduleExportHandler.setFileName(filename);
        moduleExportHandler.setModuleName(moduleName.replace('\\', '/'));
        moduleExportHandler.setAdditionalResources(resources);
        moduleExportHandler.setDescription(
            Messages.get().getBundle(getLocale()).key(
                Messages.GUI_MODULES_LIST_EXPORT_REPORT_HANDLER_NAME_1,
                moduleExportHandler.getModuleName()));

        return moduleExportHandler;
    }
}