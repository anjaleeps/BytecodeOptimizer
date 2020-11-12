package builder;

import java.util.Iterator;
import java.util.List;

public class GraphVisitor {

    public void visitNode(ClassGraphNode node) {

        node.visitNode();
        visitDependencies(node);
        visitChildNodes(node);
    }

    public void visitDependencies(ClassGraphNode node) {

        Iterator<ClassGraphNode> dependencyIterator = node.getDependencies().iterator();

        while (dependencyIterator.hasNext()) {

            ClassGraphNode current = dependencyIterator.next();

            if (!current.isVisited()) {
                visitNode(current);
            }
        }
    }

    public void visitChildNodes(ClassGraphNode node) {

        List<ClassGraphNode> childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.size(); i++) {
            if (!childNodes.get(i).isVisited()){
                visitNode(childNodes.get(i));
            }
        }
    }
}
