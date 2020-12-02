package builder;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM6;

/**
 * A class node visitor that returns a method node visitor for every used and not visited method when visiting methods
 * in the class so that the each used method will be visited by the method visitor
 */
public class ClassVisitorForMethods extends ClassNode {

    private DependencyCollector collector;
    private Set<String> names = new HashSet<>();

    public ClassVisitorForMethods(DependencyCollector collector) {

        super(ASM6);
        this.collector = collector;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
                      String[] interfaces) {

    }

    @Override
    public void visitSource(String file, String debug) {

    }

    @Override
    public ModuleVisitor visitModule(String name, int access, String version) {

        return null;
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {

    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {

    }

    @Override
    public void visitAttribute(Attribute attr){

    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

        return null;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {

        return null;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {

        return null;
    }

    /**
     * Visits every method in the class and returns a method visitor object only for used and unvisited methods
     */
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
            mn.setCollector(collector);
            return mn;
        }

        return null;
    }

    @Override
    public void visitEnd() {

    }

}
