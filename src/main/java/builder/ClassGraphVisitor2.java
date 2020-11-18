package builder;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM6;

public class ClassGraphVisitor2 extends ClassNode {

    private DependencyCollector collector;
    private Set<MethodGraphNode> usedMethods = new HashSet<>();
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

            mn.access = access;
            mn.signature = signature;
            if (exceptions != null){
                mn.exceptions.addAll(Arrays.asList(exceptions));
            }

//            if (signature == null) {
//                collector.addMethodDesc(desc);
//            } else {
//                collector.addSignature(signature);
//            }
//            collector.addInternalNames(exceptions);

            mn.markAsVisited();
            collector.visitingMethod = mn;
            return new MethodNodeVisitor(collector);
        }

        return null;
    }

    @Override
    public void visitEnd() {

    }

}
