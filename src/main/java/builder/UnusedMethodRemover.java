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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.ClassNode;

import static org.objectweb.asm.Opcodes.ASM9;

public class UnusedMethodRemover extends ClassNode {

    private ClassWriter writer;
    private ClassGraphNode node;

    public UnusedMethodRemover(ClassWriter writer) {

        super(ASM9);
        this.writer = writer;
        this.node = node;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
                      String[] interfaces) {

        writer.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String file, String debug) {

        writer.visitSource(file, debug);
    }

    @Override
    public ModuleVisitor visitModule(String name, int access, String version) {

        return writer.visitModule(name, access, version);
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {

        writer.visitOuterClass(owner, name, desc);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {

        writer.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public void visitAttribute(Attribute attr) {

        writer.visitAttribute(attr);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

        return writer.visitAnnotation(desc, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {

        return writer.visitTypeAnnotation(typeRef, typePath, desc, visible);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {

        return writer.visitField(access, name, desc, signature, value);
    }

    /**
     * Call the method writer only for methods marked as used
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

        GraphBuilder.totalMethodCount++;
        MethodGraphNode mn = new MethodGraphNode(access, this.name, name, desc, signature, exceptions);

        int i = methods.indexOf(mn);

        if (((MethodGraphNode) methods.get(i)).isUsed()) {
            GraphBuilder.visitedMethodCount += 1;
            return writer.visitMethod(access, name, desc, signature, exceptions);
        }
        return null;
    }
}
