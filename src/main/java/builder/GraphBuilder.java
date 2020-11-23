package builder;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphBuilder {

    static int visitedMethodCount;
    private Map<String, ClassGraphNode> nodes;
    private int visitedCount;
    private int usedCount;
    private ClassGraphNode rootNode;

    public GraphBuilder() {

        visitedCount = 0;
        usedCount = 0;
        nodes = new HashMap<>();
    }

    public void build() {

        buildClassHierarchy();
        visitNode(rootNode);
        markMainMethod();
        System.out.println("visited methods: " + GraphBuilder.visitedMethodCount);
    }

    public void markMainMethod() {

        for (MethodNode method : rootNode.methods) {

            if (method.name.equals("main")) {
                MethodGraphNode methodNode = (MethodGraphNode) method;
                methodNode.markAsUsed();
            }
        }
        visitForMethods(rootNode);
    }

    public void visitForMethods(ClassGraphNode node) {

        DependencyCollector collector = new DependencyCollector(this);
        ClassGraphVisitor2 cv = new ClassGraphVisitor2(collector);

        node.accept(cv);
        node.methods = cv.methods;
        nodes.put(node.name, node);

        for (MethodNode methodNode : node.methods) {
            MethodGraphNode method = (MethodGraphNode) methodNode;

            if (method.isVisited()) {

                InsnList instructions = method.instructions;

                for (int i = 0; i < instructions.size(); i++) {
                    AbstractInsnNode insnNode = instructions.get(i);

                    if (insnNode.getType() != AbstractInsnNode.METHOD_INSN) {
                        continue;
                    }

                    MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                    ClassGraphNode owner = getNodeByName(methodInsnNode.owner);

                    if (owner == null) {
                        continue;
                    }

                    MethodGraphNode mn = new MethodGraphNode(-1, methodInsnNode.owner,
                            methodInsnNode.name,
                            methodInsnNode.desc, null, null);

                    checkUsedMethod(owner, mn);
                    if (methodInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL || methodInsnNode.getOpcode() == Opcodes.INVOKEINTERFACE){
                        checkChildsForUsedMethod(owner, mn);
                    }
                    checkInterfacesForUsedMethods(owner, mn);
                }
                method.markUsedAsVisited();
            }
        }
    }

    public void checkUsedMethod(ClassGraphNode owner, MethodGraphNode mn) {

        if (!owner.isVisited()) {
            visitNode(owner);
        }

        int index = owner.methods.indexOf(mn);

        if (index < 0) {
            checkParentForUsedMethod(owner, mn);
            return;
        }

        owner.markAsUsed();
        visitMethodOwnerNode(owner, index);
    }

    public void checkParentForUsedMethod(ClassGraphNode owner, MethodGraphNode mn){
        ClassGraphNode superNode = owner.getSuperNode();
        if (superNode != null) {
            mn.owner = superNode.name;
            checkUsedMethod(superNode, mn);
        }
    }

    public void checkInterfacesForUsedMethods(ClassGraphNode owner, MethodGraphNode mn){

        for (ClassGraphNode interfaceNode : owner.getInterfaceNodes()){

            mn.owner = interfaceNode.name;
           checkUsedMethod(interfaceNode, mn);
        }
    }

    public void checkChildsForUsedMethod(ClassGraphNode owner, MethodGraphNode mn) {

        for (ClassGraphNode childNode : owner.getChildNodes()) {

            mn.owner = childNode.name;
            checkUsedMethod(childNode, mn);
        }
    }

    public void visitMethodOwnerNode(ClassGraphNode owner, int methodIndex) {

        MethodGraphNode usedMethod = (MethodGraphNode) owner.methods.get(methodIndex);
        if (!usedMethod.isUsed()) {

            usedMethod.markAsUsed();
            visitForMethods(owner);
        }
    }

    public void visitNode(ClassGraphNode node) {

        DependencyCollector collector = new DependencyCollector(this);
        GraphVisitor cv = new GraphVisitor(collector);
        node.accept(cv);
        updateNode(node, cv, collector);
        countVisited();
//        visitDependencies(node);
//
//        if (node.isServiceProvider()) {
//            visitChildNodes(node);
//        }
    }

    public void updateNode(ClassGraphNode node, ClassNode cv, DependencyCollector collector) {

        node.setDependencies(collector.getDependencies());
        node.methods = cv.methods;
        node.markAsVisited();
        nodes.put(node.name, node);
    }

    public void visitDependencies(ClassGraphNode node) {

        for (ClassGraphNode current : node.getDependencies()) {

            if (!current.isVisited()) {
                visitNode(current);
            }
        }
    }

    public void visitChildNodes(ClassGraphNode node) {

        for (ClassGraphNode current : node.getChildNodes()) {

            if (!current.isVisited()) {

                visitNode(current);
            }
        }
    }

    public ClassGraphNode getNodeByName(String name) {

        return nodes.get(name);
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

        ClassGraphNode newNode = new ClassGraphNode(name, bytes);
        nodes.put(name, newNode);
    }

    public void setRootNode(String rootName) {

        rootNode = getNodeByName(rootName);
        rootNode.markAsUsed();
    }

    public void buildClassHierarchy() {

        for (String name : nodes.keySet()) {
            ClassGraphNode current = nodes.get(name);
            setSuperNode(current);
            setInterfaces(current);
        }
    }

    public void setSuperNode(ClassGraphNode current) {

        ClassGraphNode superNode = getNodeByName(current.getSuperName());
        if (superNode != null) {
            current.setSuperNode(superNode);
            superNode.addChildNode(current);

        }
    }

    public void setInterfaces(ClassGraphNode current) {

        List<ClassGraphNode> interfaceNodes = new ArrayList<>();

        for (int i = 0; i < current.getInterfaceNames().length; i++) {
            ClassGraphNode itf = getNodeByName(current.getInterfaceNames()[i]);

            if (itf != null) {
                interfaceNodes.add(itf);
                itf.addChildNode(current);
            }
        }

        current.setInterfaceNodes(interfaceNodes);
    }

    public void setServiceProviders(List<String> serviceProviders) {

        for (String name : serviceProviders) {
            ClassGraphNode providerNode = getNodeByName(name);
            if (providerNode != null) {
                providerNode.markAsServiceProvider();
            }
        }
    }

}
