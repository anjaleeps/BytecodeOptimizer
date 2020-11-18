package builder;

import org.objectweb.asm.tree.ClassNode;
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
    }

    public void start() {

    }

//    public void visitRootNode() {
//
//        DependencyCollector collector = new DependencyCollector(this);
//        ClassGraphVisitor cv = new ClassGraphVisitor(collector);
//        rootNode.accept(cv);
//        updateNode(rootNode, cv, collector);
//
//        for (MethodNode method : rootNode.methods) {
//            MethodGraphNode methodNode = (MethodGraphNode) method;
//            methodNode.markAsUsed();
//        }
//        visitClassNode(rootNode);
//    }

//    public void visitClassNode(ClassGraphNode node){
//
//        DependencyCollector collector = new DependencyCollector(this);
//        if (!node.isVisited()){
//            ClassGraphVisitor cv = new ClassGraphVisitor(collector);
//            node.accept(cv);
//            updateNode(node, cv, collector);
//        }
//        ClassGraphVisitor2 cv = new ClassGraphVisitor2(collector);
//        node.accept(cv);
//        node.methods = cv.methods;
//        nodes.put(node.name, node);
//
//        for (MethodNode method: node.methods){
//            for (MethodGraphNode calledMethod: ((MethodGraphNode) method).calledMethods){
//                ClassGraphNode next = getNodeByName(calledMethod.owner);
//                MethodGraphNode mn = (MethodGraphNode) calledMethod;
//                if (mn.isUsed() && !mn.isVisited()){
//                    visitClassNode(next);
//                }
//            }
//        }
//    }

//    public void visitMethods(ClassGraphNode node){
//
//    }

    public void visitNode(ClassGraphNode node) {

        DependencyCollector collector = new DependencyCollector(this);
        GraphVisitor cv = new GraphVisitor(collector);
        node.accept(cv);
        countVisited();
        updateNode(node, cv, collector);
        visitDependencies(node);
        visitChildNodes(node);
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

    public ClassGraphNode getRootNode() {

        return rootNode;
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

}
