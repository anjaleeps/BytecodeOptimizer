package builder;

import java.util.Iterator;

public class GraphVisitor {

    public void visitNode(ClassGraphNode node) {

        node.visitClass();
        visitDependencies(node);
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
}
