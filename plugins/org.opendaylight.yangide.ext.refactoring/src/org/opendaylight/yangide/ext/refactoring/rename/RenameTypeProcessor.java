/*******************************************************************************
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.opendaylight.yangide.ext.refactoring.rename;

import org.opendaylight.yangide.core.dom.TypeDefinition;
import org.opendaylight.yangide.core.indexing.ElementIndexReferenceType;

/**
 * @author Konstantin Zaitsev
 * date: Aug 2, 2014
 */
public class RenameTypeProcessor extends YangRenameProcessor<TypeDefinition> {

    public RenameTypeProcessor(TypeDefinition type) {
        super(type);
    }

    @Override
    public String getIdentifier() {
        return "org.opendaylight.yangide.ext.refactoring.rename.RenameTypeProcessor";
    }

    @Override
    protected ElementIndexReferenceType getReferenceType() {
        return ElementIndexReferenceType.TYPE_REF;
    }
}
