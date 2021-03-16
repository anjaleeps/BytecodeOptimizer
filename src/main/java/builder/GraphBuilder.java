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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class to handle the graph creation and visiting
 */
public class GraphBuilder {

    static int visitedMethodCount;
    static int totalMethodCount;
    private Map<String, ClassGraphNode> nodes;
    private Map<String, ClassGraphNode> javaNodes;
    private int visitedCount;
    private int usedCount;
    private ClassGraphNode rootNode;
    private MethodGraphNode mainMethod;
    private final ConfigReader configReader;

    public GraphBuilder(ConfigReader configReader) {

        this.configReader = configReader;
        visitedCount = 0;
        usedCount = 0;
        nodes = new HashMap<>();
        javaNodes = new HashMap<>();
    }

    public void build() {

        setRootNode(configReader.rootName);
        buildClassHierarchy();
        visitNode(rootNode);
        if (!configReader.optimizeClassesOnly) {
            markMainMethod();
            findLinkedMethods(rootNode);
        }
        visitKeepClasses();
    }

    public void visitKeepClasses() {

        for (String keepClassName : configReader.getKeepClasses()) {
            if (nodes.get(keepClassName) != null) {
                ClassGraphNode keepNode = nodes.get(keepClassName);
                keepNode.markAsKeep();
                visitNode(keepNode);
                if (!configReader.optimizeClassesOnly) {
                    for (MethodNode method : keepNode.methods) {
                        ((MethodGraphNode) method).markAsUsed();
                    }
                    findLinkedMethods(keepNode);
                }
            }
        }
    }

    public void visitNode(ClassGraphNode node) {
        node.markAsVisited();
        countVisited();
        node.accept(new ClassNodeVisitor());
        visitDependentNodes(node);
        if (node.isServiceProvider()) {
            visitChildNodes(node);
        }
    }

    public void visitDependentNodes(ClassGraphNode node) {
        for (String className: node.getDependencies()) {
            if (nodes.get(className) != null && !nodes.get(className).isVisited()) {
                visitNode(nodes.get(className));
            }
        }
    }

    public void visitChildNodes(ClassGraphNode node) {
        for (ClassGraphNode childNode : node.getChildNodes()) {
            if (!childNode.isVisited()){
                visitNode(childNode);
            }
        }
    }

    /**
     * Mark the main method of the root class as used
     */
    public void markMainMethod() {

        for (MethodNode method : rootNode.methods) {

            if (method.name.equals("main")) {
                MethodGraphNode methodNode = (MethodGraphNode) method;
                methodNode.markAsUsed();
                mainMethod = methodNode;
            }
        }
        if (mainMethod == null){
            throw new IllegalArgumentException("The root node does not have a main method");
        }
    }

    /**
     * Visit methods that are marked as used in a class node and check their instructions to see
     * if the method is calling another method inside it
     */
    public void findLinkedMethods(ClassGraphNode node) {

        //visit the unvisited but used methods in the class node
        visitNodeForMethods(node);

        for (MethodNode methodNode : node.methods) {
            MethodGraphNode method = (MethodGraphNode) methodNode;

            //check the instructions used in a used method to find out called methods
            //pass if the called methods have already been visited
            if (method.isUsed() && !method.isCalledVisited()) {

                method.markAsCalledVisited();
                visitDependencies(method);

                InsnList instructions = method.instructions;

                for (int i = 0; i < instructions.size(); i++) {
                    AbstractInsnNode insnNode = instructions.get(i);

                    //check if the instruction type is of INVOKE_STATIC, INVOKE_VIRTUAL,
                    // INVOKE_SPECIAL, and INVOKE_INTERFACE types
                    if (insnNode.getType() == AbstractInsnNode.METHOD_INSN) {
                        visitMethodInsn((MethodInsnNode) insnNode, method);
                    }
                    //check if instruction type id of INVOKE_DYNAMIC type
                    else if (insnNode.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN) {
                        visitInvokeDynamicInsn((InvokeDynamicInsnNode) insnNode, method);
                    }
                }
            }
        }
    }

    /**
     * Visit method instructions to find methods called inside the currently traversing method
     * instruction types: INVOKE_STATIC, INVOKE_VIRTUAL, INVOKE_SPECIAL, and INVOKE_INTERFACE
     */
    public void visitMethodInsn(MethodInsnNode methodInsnNode, MethodGraphNode method) {

        ClassGraphNode owner = getNodeByName(methodInsnNode.owner);

        //Create MethodGraphNode for the method called inside the currently traversing method
        MethodGraphNode mn = new MethodGraphNode(0, methodInsnNode.owner,
                methodInsnNode.name, methodInsnNode.desc, null, null);

        boolean resolvedAtRuntime = methodInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL ||
                methodInsnNode.getOpcode() == Opcodes.INVOKEINTERFACE;

        //check if called method belongs to a Java library class
        if (owner == null) {
            owner = getJavaNodeByName(methodInsnNode.owner);
            if (resolvedAtRuntime && owner != null && !owner.methods.contains(mn)) {
                owner.methods.add(mn.getCopy());
                checkChildrenForUsedMethod(owner, mn, method);
            }
            return;
        }

        MethodGraphNode foundMethod = findMethodInClass(owner, mn);

        if (foundMethod != null) {

            boolean calledVisitedOld = foundMethod.isCalledVisited();

            checkUsedMethod(owner, foundMethod, method, false);

            if (resolvedAtRuntime && !calledVisitedOld) {
                checkChildrenForUsedMethod(owner, mn, method);
            }
        } else {
            boolean found = (owner.access & Opcodes.ACC_INTERFACE) != 0 ? checkInterfacesForUsedMethod(owner, mn,
                    method) : checkParentForUsedMethod(owner, mn, method, resolvedAtRuntime);

            if (!found) {
                checkChildrenForUsedMethod(owner, mn, method);
            }
        }
    }

    /**
     * Visit INVOKE_DYNAMIC instructions to link their method calls
     */
    public void visitInvokeDynamicInsn(InvokeDynamicInsnNode invokeDynamicInsnNode, MethodGraphNode method) {

        //get bootstrap method's arguments
        Object[] bsmArgs = invokeDynamicInsnNode.bsmArgs;

        for (int j = 0; j < bsmArgs.length; j++) {

            //if the bsmArg is a method handle, get the method details
            if (bsmArgs[j] instanceof Handle) {
                Handle handle = (Handle) bsmArgs[j];
                ClassGraphNode owner = getNodeByName(handle.getOwner());

                //Create MethodGraphNode for the method called inside the currently traversing method
                MethodGraphNode mn = new MethodGraphNode(0, handle.getOwner(), handle.getName(),
                        handle.getDesc(), null, null);

                //check if the method owner is a java library class
                if (owner == null) {
                    return;
                }

                MethodGraphNode usedMethod = findMethodInClass(owner, mn);
                if (usedMethod != null) {
                    //Check if the used method is defined inside the owner class
                    checkUsedMethod(owner, usedMethod, method, false);
                }
            }
        }
    }

    public MethodGraphNode findMethodInClass(ClassGraphNode classNode, MethodGraphNode methodNode) {

        if (!classNode.isVisited()) {
            return null;
        }

        int index = classNode.methods.indexOf(methodNode);

        if (index < 0) {
            return null;
        }

        return (MethodGraphNode) classNode.methods.get(index);
    }

    /**
     * Check if the called method is defined inside its owner class
     */
    public void checkUsedMethod(ClassGraphNode owner, MethodGraphNode usedMethod, MethodGraphNode current,
                                boolean resolvedAtRuntime) {

        if (!owner.isUsed()) {
            owner.markAsUsed();
        }

        //add the used method to the list of methods called inside the currently traversing method
        current.addMethodCall(usedMethod);
        usedMethod.addCallingMethod(current);

        usedMethod.markAsUsed();
        if (!usedMethod.isCalledVisited()) {
            findLinkedMethods(owner);
        }
    }

    /**
     * When the class the called method was called with does not define the called method,
     * check if its parent node defines the class
     * <p>
     * Ex:
     * class Foo {
     * public void read(){}
     * }
     * class Bar extends Foo{}
     * <p>
     * Bar bar = new Bar()
     * bar.read()
     */
    public boolean checkParentForUsedMethod(ClassGraphNode owner, MethodGraphNode mn, MethodGraphNode current,
                                            boolean resolvedAtRuntime) {

        ClassGraphNode superNode = owner.getSuperNode();

        if (superNode != null) {

            mn.owner = superNode.name;
            MethodGraphNode foundMethod = findMethodInClass(superNode, mn);

            if (foundMethod != null) {
                boolean calledVisitedOld = foundMethod.isCalledVisited();
                checkUsedMethod(superNode, foundMethod, current, false);

                if (resolvedAtRuntime && !calledVisitedOld) {
                    checkChildrenForUsedMethod(superNode, mn, current);
                }
                return true;
            } else {
                return checkParentForUsedMethod(superNode, mn, current, resolvedAtRuntime);
            }
        }
        return false;
    }

    /**
     * If an interface implemented by the owner of the called class defines the called method, mark it as used inside
     * the interface to preserve it from being removed
     */
    public boolean checkInterfacesForUsedMethod(ClassGraphNode owner, MethodGraphNode mn, MethodGraphNode current) {

        boolean found = false;
        for (ClassGraphNode interfaceNode : owner.getInterfaceNodes()) {

            mn.owner = interfaceNode.name;
            MethodGraphNode foundMethod = findMethodInClass(interfaceNode, mn);

            //check if the method is defined inside an extended interface of the current interface
            if (foundMethod != null) {
                boolean calledVisitedOld = foundMethod.isCalledVisited();
                checkUsedMethod(interfaceNode, foundMethod, current, false);
                if (!calledVisitedOld) {
                    checkChildrenForUsedMethod(interfaceNode, mn, current);
                }
                return true;
            } else {
                found = checkInterfacesForUsedMethod(interfaceNode, mn, current);
            }
        }

        return found;
    }

    /**
     * Check if the called method is defined inside a child method of the owner class, since which method is called
     * is resolved at the runtime
     * <p>
     * Ex:
     * <p>
     * class Foo {
     * public void read()
     * }
     * <p>
     * class Bar extends Foo{
     * public void read()
     * }
     * <p>
     * Scenario 1:
     * <p>
     * Class Main {
     * public void main(){
     * Foo foo = new Bar()
     * foo.read()
     * }
     * }
     * <p>
     * Scenario 2:
     * <p>
     * Class Baz {
     * public void do(Foo foo){
     * foo.read()
     * }
     * }
     * <p>
     * Class Main {
     * public void main(){
     * Baz baz = new Baz()
     * baz.do(new Bar())
     * }
     * }
     */
    public void checkChildrenForUsedMethod(ClassGraphNode owner, MethodGraphNode mn, MethodGraphNode current) {

        for (ClassGraphNode childNode : owner.getChildNodes()) {

            if (getJavaNodeByName(childNode.name) != null) {
                checkChildrenForUsedMethod(childNode, mn, current);
                continue;
            }

            mn.owner = childNode.name;
            MethodGraphNode foundMethod = findMethodInClass(childNode, mn);

            //check if the children of the current node defines the same method
            if (foundMethod != null) {
                boolean calledVisitedOld = foundMethod.isCalledVisited();
                if (owner.isServiceProvider()){
                    checkUsedMethod(childNode, foundMethod, current, false);
                }
                else {
                    checkUsedMethod(childNode, foundMethod, current, true);
                }
                if (!calledVisitedOld) {
                    checkChildrenForUsedMethod(childNode, mn, current);
                }
                continue;
            }

            checkChildrenForUsedMethod(childNode, mn, current);

        }
    }

    /**
     * Visit the unvisited methods marked as used inside a class
     */
    public void visitNodeForMethods(ClassGraphNode node) {

        ClassVisitorForMethods cv = new ClassVisitorForMethods();

        //Visit the ClassGraphNode for the second time using the ClassVisitorForMethods
        node.accept(cv);
    }

    /**
     * Visit the class dependencies of the current class
     **/
    public void visitDependencies(MethodGraphNode method) {

        for (String dependentClassName : method.getDependentClassNames()) {

            ClassGraphNode dependentClassNode = getNodeByName(dependentClassName);
            if (dependentClassNode == null || !dependentClassNode.isVisited()){
                continue;
            }

            if (!dependentClassNode.isUsed()) {
                dependentClassNode.markAsUsed();
                findLinkedMethods(dependentClassNode);
            }
        }
    }

    /**
     * Visit every ClassGraphNode created and build a class hierarchy by assigning their child and super nodes
     */
    public void buildClassHierarchy() {

        for (String name : nodes.keySet()) {
            ClassGraphNode current = nodes.get(name);
            setSuperNode(current);
            setInterfaces(current);
        }
        for (String name : javaNodes.keySet()) {
            ClassGraphNode current = javaNodes.get(name);
            setJavaSuperNode(current);
            setJavaInterfaces(current);
        }
    }

    public void setJavaSuperNode(ClassGraphNode current) {

        if (current.getSuperName() != null) {
            ClassGraphNode superNode = getJavaNodeByName(current.getSuperName());
            if (superNode != null) {
                superNode.addChildNode(current);
            }
        }
    }

    public void setJavaInterfaces(ClassGraphNode current) {

        if (current.getInterfaceNames() != null) {
            for (int i = 0; i < current.getInterfaceNames().length; i++) {
                ClassGraphNode itf = getJavaNodeByName(current.getInterfaceNames()[i]);

                if (itf != null) {
                    itf.addChildNode(current);
                }
            }
        }
    }

    public void setSuperNode(ClassGraphNode current) {

        String superName = current.getSuperName();

        ClassGraphNode superNode = getNodeByName(superName);
        if (superNode != null) {
            current.setSuperNode(superNode);
            superNode.addChildNode(current);
        } else if (getJavaNodeByName(superName) != null) {
            superNode = getJavaNodeByName(superName);
            superNode.addChildNode(current);
        } else if (superName != null && !current.getSuperName().equals("java/lang/Object")) {
            superNode = new ClassGraphNode(superName);
            superNode.setReader();
            javaNodes.put(superName, superNode);
            superNode.addChildNode(current);
        }
    }

    public void setInterfaces(ClassGraphNode current) {

        List<ClassGraphNode> interfaceNodes = new ArrayList<>();

        for (int i = 0; i < current.getInterfaceNames().length; i++) {
            String interfaceName = current.getInterfaceNames()[i];
            ClassGraphNode itf = getNodeByName(interfaceName);

            if (itf != null) {
                interfaceNodes.add(itf);
            } else if (getJavaNodeByName(interfaceName) != null) {
                itf = getJavaNodeByName(interfaceName);
            } else {
                itf = new ClassGraphNode(interfaceName);
                itf.setReader();
                javaNodes.put(interfaceName, itf);
            }
            itf.addChildNode(current);
        }

        current.setInterfaceNodes(interfaceNodes);
    }

    /**
     * Mark the class as a service provider
     */
    public void setServiceProviders(List<String> serviceProviders) {

        for (String name : serviceProviders) {
            ClassGraphNode providerNode = getNodeByName(name);
            if (providerNode != null) {
                providerNode.markAsServiceProvider();
            }
        }
    }

    /**
     * Remove methods marked as unused using the UnusedMethodRemover
     * Returns the byte array generated by the class writer during the visit
     */
    public byte[] removeUnusedMethods(ClassGraphNode node) {

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new UnusedMethodRemover(writer);
        node.accept(visitor);
        return writer.toByteArray();
    }

    public ClassGraphNode getNodeByName(String name) {

        return nodes.get(name);
    }

    public ClassGraphNode getJavaNodeByName(String name) {

        return javaNodes.get(name);
    }

    public int getGraphSize() {

        return nodes.size();
    }

    public void countVisited() {

        visitedCount++;
    }

    public void countUsed() {

        usedCount++;
    }

    public int getVisitedCount() {

        return visitedCount;
    }

    public int getUsedCount() {

        return usedCount;
    }

    public void addNewNode(String name, byte[] bytes) {

        ClassGraphNode newNode = new ClassGraphNode(name);
        newNode.setReader(bytes);
        nodes.put(name, newNode);
    }

    public void setRootNode(String rootName) {

        rootNode = getNodeByName(rootName);
        if (rootNode == null) {
            throw new IllegalArgumentException("root file doesn't exist");
        }
        rootNode.markAsUsed();
    }

}
