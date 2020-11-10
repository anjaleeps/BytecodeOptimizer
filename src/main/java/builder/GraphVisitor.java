package builder;

import org.objectweb.asm.ClassReader;

import java.util.Iterator;

public class GraphVisitor {

    private GraphBuilder builder;

    public GraphVisitor(GraphBuilder builder) {

        this.builder = builder;
    }

    public void start() {

        visitNode(builder.getRootNode());
    }

    public void visitNode(ClassGraphNode node) {

        ClassReader cr = new ClassReader(node.getBytes());
        cr.accept(node, 0);
        builder.countVisited();
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
