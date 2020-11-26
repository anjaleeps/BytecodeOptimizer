package builder;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashSet;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM6;

/**
 * A class node visitor that returns a method node visitor for every used and not visited method when visiting methods
 * in the class so that the each used method will be visited by the method visitor
 * */
public class ClassVisitorForMethods extends ClassNode {

    private DependencyCollector collector;
    private Set<String> names = new HashSet<>();

    public ClassVisitorForMethods(DependencyCollector collector) {

        super(ASM6);
        this.collector = collector;
    }

    /**
     * Visits every method in the class and returns a method visitor object only for used and unvisited methods
     * */
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

        MethodGraphNode mn = new MethodGraphNode(access, this.name, name, desc, signature, exceptions);
        int i = methods.indexOf(mn);
        mn = (MethodGraphNode) methods.get(i);

        if (mn.isUsed() && !mn.isVisited()) {

            //add method parameter and return types to class dependencies
            if (signature == null) {
                collector.addMethodDesc(desc);
            } else {
                collector.addSignature(signature);
            }
            collector.addInternalNames(exceptions);

            mn.markAsVisited();
            GraphBuilder.visitedMethodCount += 1;
            mn.setCollector(collector);
            return mn;
        }

        return null;
    }

    @Override
    public void visitEnd() {

    }

}
