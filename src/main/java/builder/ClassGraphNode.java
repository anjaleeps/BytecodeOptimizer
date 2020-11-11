package builder;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.objectweb.asm.Opcodes.ASM6;

public class ClassGraphNode extends ClassNode {

    private final Set<ClassGraphNode> dependencies = new HashSet<>();
    private boolean visited;
    private byte[] bytes;
    Pattern pattern;

    public ClassGraphNode(String name, byte[] bytes) {

        super(ASM6);
        this.name = name;
        this.bytes = bytes;
        visited = false;
        pattern = Pattern.compile("([a-z]\\w+(/|[.]))+((\\w|[$])*)+");
    }

    public boolean isVisited() {

        return visited;
    }

    public byte[] getBytes() {

        return bytes;
    }

    public Set<ClassGraphNode> getDependencies() {

        return dependencies;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
                      String[] interfaces) {

        this.superName = superName;
        this.access = access;
        this.version = version;
        this.signature = signature;
        this.interfaces = Arrays.asList(interfaces);
        markAsVisited();

        if (signature == null) {
            if (superName != null) {
                addInternalName(superName);
            }
            addInternalNames(interfaces);
        } else {
            addSignature(signature);
        }
    }

    @Override
    public ModuleVisitor visitModule(String name, int access, String version) {

        return new ModuleNodeVisitor();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

        addDesc(desc);
        return new AnnotationNodeVisitor();
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {

        addDesc(desc);
        return new AnnotationNodeVisitor();
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {

        outerClass = name;
        if (desc != null) {
            addDesc(desc);
        }
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {

        if (outerName != null){
            addInternalName(outerName);

        }

        InnerClassNode icn = new InnerClassNode(name, outerName, innerName, access);
        innerClasses.add(icn);
    }

    @Override
    public void visitAttribute(Attribute attr) {

        addInternalName(attr.type);
        if (attrs == null) {
            attrs = new ArrayList<>(1);
        }
        attrs.add(attr);

    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {

        if (signature == null) {
            addDesc(desc);
        } else {
            addSignature(signature);
        }
        if (value instanceof Type) {
            addType((Type) value);
        }
        FieldNode fn = new FieldNode(access, name, desc, signature, value);
        fields.add(fn);
        return new FieldNodeVisitor();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

        if (signature == null) {
            addMethodDesc(desc);
        } else {
            addSignature(signature);
        }
        addInternalNames(exceptions);
        MethodNode mn = new MethodNode(access, name, desc, signature, exceptions);
        methods.add(mn);
        return new MethodNodeVisitor();
    }

    @Override
    public void visitEnd() {

        GraphBuilder.updateNode(this.name, this);

    }

    public void addType(Type type) {

        switch (type.getSort()) {
            case Type.ARRAY:
                addType(type.getElementType());
                break;
            case Type.OBJECT:
                addName(type.getInternalName());
                break;
            case Type.METHOD:
                addMethodDesc(type.getDescriptor());
                break;
        }
    }

    public void addDesc(String desc) {

        addType(Type.getType(desc));
    }

    private void addSignature(String signature) {

        if (signature != null) {

            new SignatureReader(signature).accept(new SignatureNodeVisitor());
        }
    }

    private void addTypeSignature(String signature) {

        if (signature != null) {
            new SignatureReader(signature).acceptType(new SignatureNodeVisitor());
        }
    }

    private void addConstant(Object constant) {

        if (constant instanceof Type) {
            addType((Type) constant);
        } else if (constant instanceof Handle) {
            Handle handle = (Handle) constant;
            addInternalName(handle.getOwner());
            addMethodDesc(handle.getDesc());
        } else if (constant instanceof String) {
            String s = (String) constant;
            if (checkStringConstant(s)) {
                addInternalName(s.replace('.', '/'));
            }
        }

    }

    private void addMethodDesc(String desc) {

        addType(Type.getReturnType(desc));
        Type[] types = Type.getArgumentTypes(desc);
        for (int i = 0; i < types.length; i++) {
            addType(types[i]);
        }
    }

    public void addInternalName(String name) {

        addType(Type.getObjectType(name));
    }

    public void addInternalNames(String[] names) {

        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                addInternalName(names[i]);
            }
        }
    }

    public void addName(String name) {

        ClassGraphNode node = GraphBuilder.getNodeByName(name);
        if (node != null) {
            dependencies.add(node);
        }
    }

    public boolean checkStringConstant(String s) {

        Matcher matcher = pattern.matcher(s);

        if (matcher.matches()) {
            return true;
        }

        return false;
    }

    public void markAsVisited() {

        visited = true;
    }

    public class ModuleNodeVisitor extends ModuleVisitor {

        public ModuleNodeVisitor() {

            super(ASM6);
        }

        @Override
        public void visitUse(String service) {

            System.out.println(service);
        }

        @Override
        public void visitProvide(String service, String... providers) {

            for (int i = 0; i < providers.length; i++) {
                System.out.println(providers);
            }
        }

    }

    public class AnnotationNodeVisitor extends AnnotationVisitor {

        public AnnotationNodeVisitor() {

            super(ASM6);
        }

        @Override
        public void visit(String name, Object value) {

            if (value instanceof Type) {
                addType((Type) value);
            }
        }

        @Override
        public void visitEnum(String name, String desc, String value) {

            addDesc(desc);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {

            addDesc(desc);
            return this;
        }

        @Override
        public AnnotationVisitor visitArray(String name) {

            return this;
        }
    }

    public class FieldNodeVisitor extends FieldVisitor {

        public FieldNodeVisitor() {

            super(ASM6);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

            addDesc(desc);
            return new AnnotationNodeVisitor();
        }

        @Override
        public void visitAttribute(Attribute attr) {

            System.out.println(attr.type);
        }
    }

    public class MethodNodeVisitor extends MethodVisitor {

        public MethodNodeVisitor() {

            super(ASM6);
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {

            return new AnnotationNodeVisitor();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

            addDesc(desc);
            return new AnnotationNodeVisitor();
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {

            addDesc(desc);
            return new AnnotationNodeVisitor();
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {

            addDesc(desc);
            return new AnnotationNodeVisitor();
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {

            addType(Type.getObjectType(type));
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {

            addInternalName(owner);
            addDesc(desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

            addInternalName(owner);
            addMethodDesc(desc);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {

            addMethodDesc(desc);
            addConstant(bsm);
            for (int i = 0; i < bsmArgs.length; i++) {
                addConstant(bsmArgs[i]);
            }
        }

        @Override
        public void visitLdcInsn(Object constant) {

            addConstant(constant);
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {

            addDesc(desc);
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {

            addTypeSignature(signature);
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {

            if (type != null) {
                addInternalName(type);
            }
        }
    }

    public class SignatureNodeVisitor extends SignatureVisitor {

        String signatureClassName;

        public SignatureNodeVisitor() {

            super(ASM6);
        }

        @Override
        public void visitClassType(String name) {

            signatureClassName = name;
            addInternalName(name);
        }

        @Override
        public void visitInnerClassType(String name) {

            signatureClassName = signatureClassName + "$" + name;
            addInternalName(signatureClassName);
        }
    }

}
