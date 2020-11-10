package builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphBuilder {

    private static Map<String, ClassGraphNode> nodes;
    private int visitedCount;
    private ClassGraphNode rootNode;

    public GraphBuilder() {

        visitedCount = 0;
        nodes = new HashMap<>();
    }

    public static void updateNode(String name, ClassGraphNode node) {

        nodes.put(name, node);
    }

    public static ClassGraphNode getNodeByName(String name) {

        return nodes.get(name);
    }

    public static int getGraphSize() {

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

    public List<ClassGraphNode> getVisitedNodes() {

        List<ClassGraphNode> visitedNodes = new ArrayList<>();

        for (String name : nodes.keySet()) {
            ClassGraphNode current = nodes.get(name);
            if (current.isVisited()) {
                visitedNodes.add(current);
            }
        }
        System.out.println(visitedNodes.size());
        return visitedNodes;
    }

}
