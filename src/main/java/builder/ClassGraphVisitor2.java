package builder;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM6;

public class ClassGraphVisitor2 extends ClassNode {

    private DependencyCollector collector;
    private Set<String> names = new HashSet<>();

    public ClassGraphVisitor2(DependencyCollector collector) {

        super(ASM6);
        this.collector = collector;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

        MethodGraphNode mn = new MethodGraphNode(access, this.name, name, desc, signature, exceptions);
        int i = methods.indexOf(mn);
        mn = (MethodGraphNode) methods.get(i);

        if (mn.isUsed() && !mn.isVisited()) {

            mn.markAsVisited();

            return mn;
        }

        return null;
    }

    @Override
    public void visitEnd() {

    }

}
