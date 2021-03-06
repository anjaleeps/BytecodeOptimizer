/*
 * Copyright (c)  2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package builder;

import org.objectweb.asm.signature.SignatureVisitor;

import static org.objectweb.asm.Opcodes.ASM9;

/**
 * A visitor class used to visit signatures and collect used class types.
 */
public class SignatureNodeVisitor extends SignatureVisitor {

    String signatureClassName;
    private DependencyCollector collector;

    public SignatureNodeVisitor(DependencyCollector collector) {
        super(ASM9);
        this.collector = collector;
    }

    @Override
    public void visitClassType(String name) {
        signatureClassName = name;
        collector.addInternalName(name);
    }

    @Override
    public void visitInnerClassType(String name) {
        signatureClassName = signatureClassName + "$" + name;
        collector.addInternalName(signatureClassName);
    }
}


