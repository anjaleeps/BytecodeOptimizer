package builder;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM6;

/**
 * A class node visitor to visit the class attributes and annotations of a graph node.
 * Also adds the names of all the methods in the class to the method list.
 */
public class ClassNodeVisitor extends ClassNode {

    private DependencyCollector collector;
    private String name;

    public ClassNodeVisitor(DependencyCollector collector) {

        super(ASM6);
        this.collector = collector;
    }

    public Set<ClassGraphNode> getDependencies() {

        return collector.getDependencies();
    }

    public List<MethodNode> getMethods() {

        return methods;
    }

    /**
     * Visit class's super class and implemented interfaces to the list of used dependency classes
     * */
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

    /**
     * Visit class level fields and add field types to class-level dependencies
     * */
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
//        FieldNode fn = new FieldNode(access, name, desc, signature, value);
        return new FieldNodeVisitor(collector);
    }

    /**
     * Create a MethodGraphNode for each method in a class and adds it to the method list
     * */
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

        MethodGraphNode mn = new MethodGraphNode(access, this.name, name, desc, signature, exceptions);
        methods.add(mn);
        return null;
    }

    @Override
    public void visitEnd() {

    }

}
