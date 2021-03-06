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
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;

import static org.objectweb.asm.Opcodes.ASM9;

/**
 * A class node visitor to visit the class attributes and annotations of a graph node.
 * Also adds the names of all the methods in the class to the method list.
 */
public class ClassNodeVisitor extends ClassNode {

    private String name;
    private boolean isAnonymousClass;
    private DependencyCollector collector;

    public ClassNodeVisitor() {
        super(ASM9);
        isAnonymousClass = false;

    }

    public void setCollector(DependencyCollector collector) {
        this.collector = collector;
    }

    /**
     * Visit class's super class and implemented interfaces to the list of used dependency classes
     */
    @Override
    public void visit(int version, int access, String name, String signature, String superName,
                      String[] interfaces) {
        this.name = name;
        this.access = access;
        String[] parts = name.split("[$]");
        if (parts.length > 1 && parts[parts.length - 1].matches("\\d+")) {
            isAnonymousClass = true;
        }
        if (signature == null) {
            if (superName != null) {
                collector.addName(superName);
            }
            collector.addInternalNames(interfaces);
        } else {
            collector.addSignature(signature);
        }
    }

    @Override
    public void visitSource(String file, String debug) {
    }

    @Override
    public ModuleVisitor visitModule(String name, int access, String version) {
        return null;
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        if (desc != null) {
            collector.addDesc(desc);
        }
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
    }

    @Override
    public void visitAttribute(Attribute attr) {
        collector.addInternalName(attr.type);
        if (attrs == null) {
            attrs = new ArrayList<>(1);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        collector.addDesc(desc);
        return new AnnotationNodeVisitor(collector);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        collector.addDesc(desc);
        return new AnnotationNodeVisitor(collector);
    }

    /**
     * Visit class level fields and add field types to class-level dependencies
     */
    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (signature == null) {
            collector.addDesc(desc);
        } else {
            collector.addSignature(signature);
        }
        if (value instanceof Type) {
            collector.addType((Type) value);
        }
        return new FieldNodeVisitor(collector);
    }

    /**
     * Create a MethodGraphNode for each method in a class and adds it to the method list
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodGraphNode mn = new MethodGraphNode(access, this.name, name, desc, signature, exceptions);
        if (name.equals("<init>") || name.equals("<clinit>") || isAnonymousClass) {
            mn.markAsUsed();
        }
        methods.add(mn);
        if (signature == null) {
            collector.addMethodDesc(desc);
        } else {
            collector.addSignature(signature);
        }
        collector.addInternalNames(exceptions);
        return new MethodNodeVisitor(collector);
    }
}
