package builder;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM6;

public class ClassGraphVisitor extends ClassNode {

    private DependencyCollector collector;
    private String name;
    Set<MethodGraphNode> usedMethods = new HashSet<>();

    public ClassGraphVisitor(DependencyCollector collector) {

        super(ASM6);
        this.collector = collector;
    }

    public Set<ClassGraphNode> getDependencies() {

        return collector.getDependencies();
    }

    public List<MethodNode> getMethods() {

        return methods;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
                      String[] interfaces) {

        this.name = name;
        if (signature == null) {
            if (superName != null) {
                collector.addName(superName);
            }
            collector.addInternalNames(interfaces);
        } else {
            collector.addSignature(signature);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

        collector.addDesc(desc);
        return new AnnotationNodeVisitor(collector);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {

        collector.addDesc(desc);
        return new AnnotationNodeVisitor(collector);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {

        if (signature == null) {
            collector.addDesc(desc);
        } else {
            collector.addSignature(signature);
        }
        if (value instanceof Type) {
            collector.addType((Type) value);
        }
        FieldNode fn = new FieldNode(access, name, desc, signature, value);
        return new FieldNodeVisitor(collector);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

        MethodGraphNode mn = new MethodGraphNode(access, this.name, name, desc, signature, exceptions);
        mn.setCollector(collector);

        methods.add(mn);
        return null;
    }

}
