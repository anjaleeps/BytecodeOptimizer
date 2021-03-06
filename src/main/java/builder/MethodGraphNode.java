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
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM9;

/**
 * A class representing a node for each method inside a ClassGraphNode.
 * Can act as a method node and a method node visitor.
 */
public class MethodGraphNode extends MethodNode {

    String owner;
    private Set<MethodGraphNode> methodCalls = new HashSet<>();
    private Set<MethodGraphNode> callingMethods = new HashSet<>();
    private DependencyCollector collector;
    private boolean used;
    private boolean visited;
    private boolean calledVisited;

    public MethodGraphNode(int access, String owner, String name, String desc, String signature, String[] exceptions) {
        super(ASM9, access, name, desc, signature, exceptions);
        this.owner = owner;
        used = false;
        visited = false;
        calledVisited = false;
    }

    public void setCollector(DependencyCollector collector) {
        this.collector = collector;
    }

    public Set<String> getDependentClassNames() {
        return collector.getDependencies();
    }

    public void addMethodCall(MethodGraphNode calledMethod) {
        methodCalls.add(calledMethod);
    }

    public void addCallingMethod(MethodGraphNode callingMethod) {
        callingMethods.add(callingMethod);
    }

    public void markAsUsed() {
        used = true;
    }

    public void markAsVisited() {
        visited = true;
    }

    /**
     * Mark the method when every method call made inside the current method node is visited
     */
    public void markAsCalledVisited() {
        calledVisited = true;
    }

    public boolean isUsed() {
        return used;
    }

    public boolean isVisited() {
        return visited;
    }

    public boolean isCalledVisited() {
        return calledVisited;
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        return null;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        collector.addDesc(desc);
        return null;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        collector.addDesc(desc);
        return null;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {

        collector.addDesc(desc);
        return null;
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        collector.addInternalName(owner);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        collector.addInternalName(owner);
    }

    /**
     * Visit INVOKESTATIC, INVOKESPECIAL, INVOKEINTERFACE, INVOKEVIRTUAL method calls
     */
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        collector.addInternalName(owner);
        collector.addMethodDesc(desc);
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    /**
     * Visit INVOKEDYNAMIC method calls
     */
    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    @Override
    public void visitLdcInsn(Object constant) {
        collector.addConstant(constant);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MethodGraphNode) {
            MethodGraphNode mn = (MethodGraphNode) obj;
            return owner.equals(mn.owner) && name.equals(mn.name) && desc.equals(mn.desc);
        }
        return false;
    }

    public MethodGraphNode getCopy() {
        return new MethodGraphNode(access, owner, name, desc, null, null);
    }

}
