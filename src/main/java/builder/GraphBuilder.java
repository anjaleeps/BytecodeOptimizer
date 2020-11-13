package builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GraphBuilder {

    private final GraphVisitor visitor;
    private Map<String, ClassGraphNode> nodes;
    private int visitedCount;
    private ClassGraphNode rootNode;

    public GraphBuilder() {

        visitedCount = 0;
        nodes = new HashMap<>();
        visitor = new GraphVisitor();
    }

    public void build() {

        buildClassHeirarchy();
        visitor.visitNode(rootNode);
    }

    public void updateNode(String name, ClassGraphNode node) {

        nodes.put(name, node);
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

        ClassGraphNode newNode = new ClassGraphNode(name, bytes, this);
        nodes.put(name, newNode);
    }

    public ClassGraphNode getRootNode() {

        return rootNode;
    }

    public void setRootNode(String rootName) {

        rootNode = getNodeByName(rootName);
    }

    public void buildClassHeirarchy() {

        for (String name : nodes.keySet()) {
            ClassGraphNode current = nodes.get(name);
            current.setSuperNode();
            current.setInterfaces();

//            if (current.superNode != null){
//                current.superNode.addChildNode(current);
//            }
//
//            List<ClassGraphNode> intfs = current.interfaceNodes;
//            for (int i = 0; i < intfs.size(); i++){
//                intfs.get(i).addChildNode(current);
//            }
        }
    }

//    public List<ClassGraphNode> getVisitedNodes() {
//
//        List<ClassGraphNode> visitedNodes = new ArrayList<>();
//
//        for (String name : nodes.keySet()) {
//            ClassGraphNode current = nodes.get(name);
//            if (current.isVisited()) {
//                visitedNodes.add(current);
//            }
//        }
//        return visitedNodes;
//    }

}
