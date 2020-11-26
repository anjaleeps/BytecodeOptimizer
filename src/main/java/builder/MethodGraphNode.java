package builder;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM6;

/**
 * A class representing a node for each method inside a ClassGraphNode.
 * Can act as a method node and a method node visitor
 * */
public class MethodGraphNode extends MethodNode {

    String owner;
    Set<MethodGraphNode> calledMethods = new HashSet<>();
    private DependencyCollector collector;
    private boolean used;
    private boolean visited;
    private boolean calledVisited;

    public MethodGraphNode(int access, String owner, String name, String desc, String signature, String[] exceptions) {

        super(ASM6, access, name, desc, signature, exceptions);
        this.owner = owner;
        used = false;
        visited = false;
        calledVisited = false;
    }

    public void setCollector(DependencyCollector collector) {

        this.collector = collector;
    }

    public void markAsUsed() {

        used = true;
    }

    public void markAsVisited() {

        visited = true;
    }

    /**
     * Mark the method when every method call made inside the current method node is visited
     * */
    public void markAsCalledVisited(){

        calledVisited = true;
    }

    public boolean isUsed() {

        return used;
    }

    public boolean isVisited() {

        return visited;
    }

    public boolean isCalledVisited(){

        return calledVisited;
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {

        super.visitAnnotationDefault();
        return new AnnotationNodeVisitor(collector);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

//        collector.addDesc(desc);
        super.visitAnnotation(desc, visible);
        return new AnnotationNodeVisitor(collector);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {

//        collector.addDesc(desc);
        super.visitParameterAnnotation(parameter, desc, visible);
        return new AnnotationNodeVisitor(collector);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {

//        collector.addDesc(desc);
        super.visitTypeAnnotation(typeRef, typePath, desc, visible);
        return new AnnotationNodeVisitor(collector);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {

//        collector.addType(Type.getObjectType(type));
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {

//        collector.addInternalName(owner);
        collector.addDesc(desc);
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

//        collector.addInternalName(owner);
//        collector.addMethodDesc(desc);

        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {

//        collector.addMethodDesc(desc);
//        collector.addConstant(bsm);
//        for (int i = 0; i < bsmArgs.length; i++) {
//            collector.addConstant(bsmArgs[i]);
//        }
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    @Override
    public void visitLdcInsn(Object constant) {

        collector.addConstant(constant);
        super.visitLdcInsn(constant);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {

//        collector.addDesc(desc);
        super.visitMultiANewArrayInsn(desc, dims);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {

//        collector.addTypeSignature(signature);
        super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {

//        if (type != null) {
//            collector.addInternalName(type);
//        }
        super.visitTryCatchBlock(start, end, handler, type);
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof MethodGraphNode) {
            MethodGraphNode mn = (MethodGraphNode) obj;

            return owner.equals(mn.owner) && name.equals(mn.name) && desc.equals(mn.desc);
        }
        return false;
    }

}
