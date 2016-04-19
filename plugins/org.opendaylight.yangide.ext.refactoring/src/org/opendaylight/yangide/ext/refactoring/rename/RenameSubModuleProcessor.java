/*******************************************************************************
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.opendaylight.yangide.ext.refactoring.rename;

import org.opendaylight.yangide.core.dom.QName;
import org.opendaylight.yangide.core.dom.SubModule;
import org.opendaylight.yangide.core.indexing.ElementIndexReferenceInfo;
import org.opendaylight.yangide.core.indexing.ElementIndexReferenceType;
import org.opendaylight.yangide.core.model.YangModelManager;

/**
 * @author Konstantin Zaitsev
 * date: Aug 6, 2014
 */
public class RenameSubModuleProcessor extends YangRenameProcessor<SubModule> {

    public RenameSubModuleProcessor(SubModule subModule) {
        super(subModule);
    }

    @Override
    public String getIdentifier() {
        return "org.opendaylight.yangide.ext.refactoring.rename.RenameSubModuleProcessor";
    }

    @Override
    protected ElementIndexReferenceType getReferenceType() {
        return ElementIndexReferenceType.INCLUDE;
    }

    @Override
    protected ElementIndexReferenceInfo[] getReferences() {
        SubModule subModule = getNode();
        QName qname = new QName(subModule.getName(), null, subModule.getName(), subModule.getRevision());
        return YangModelManager.getIndexManager().searchReference(qname, getReferenceType(), getFile().getProject());
    }
}
