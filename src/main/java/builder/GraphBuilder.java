package builder;

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

    private Map<String, ClassGraphNode> nodes;
    private int visitedCount;
    private ClassGraphNode rootNode;

    public GraphBuilder() {

        visitedCount = 0;
        nodes = new HashMap<>();
    }

    public void build() {

        buildClassHierarchy();
        visitNode(rootNode);
        markMainMethod();
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

                    if (!owner.isVisited()) {
                        visitNode(owner);
                    }

                    MethodGraphNode mn = new MethodGraphNode(-1, methodInsnNode.owner,
                            methodInsnNode.name,
                            methodInsnNode.desc, null, null);
                    int index = owner.methods.indexOf(mn);

                    if (index < 0) {
                        continue;
                    }

                    MethodGraphNode usedMethod = (MethodGraphNode) owner.methods.get(index);
                    if (!usedMethod.isUsed()) {
                        System.out.println(methodInsnNode.owner + " " +methodInsnNode.name);
                        usedMethod.markAsUsed();
                        visitForMethods(owner);
                    }
                }
                method.markAsCalledVisited();
                break;
            }
        }
    }

    public void visitNode(ClassGraphNode node) {

        DependencyCollector collector = new DependencyCollector(this);
        GraphVisitor cv = new GraphVisitor(collector);
        node.accept(cv);
        countVisited();
        updateNode(node, cv, collector);
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

    public int getVisitedCount() {

        return visitedCount;
    }

    public void addNewNode(String name, byte[] bytes) {

        ClassGraphNode newNode = new ClassGraphNode(name, bytes);
        nodes.put(name, newNode);
    }

    public void setRootNode(String rootName) {

        rootNode = getNodeByName(rootName);
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

            if (superNode.isServiceProvider()) {
                superNode.addChildNode(current);
            }
        }
    }

    public void setInterfaces(ClassGraphNode current) {

        List<ClassGraphNode> interfaceNodes = new ArrayList<>();

        for (int i = 0; i < current.getInterfaceNames().length; i++) {
            ClassGraphNode itf = getNodeByName(current.getInterfaceNames()[i]);

            if (itf != null) {
                interfaceNodes.add(itf);
                if (itf.isServiceProvider()) {
                    itf.addChildNode(current);
                }
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
