package builder;

import org.objectweb.asm.signature.SignatureVisitor;

import static org.objectweb.asm.Opcodes.ASM6;

public class SignatureNodeVisitor extends SignatureVisitor {

    String signatureClassName;
    private DependencyCollector collector;

    public SignatureNodeVisitor(DependencyCollector collector) {

        super(ASM6);
        this.collector = collector;
    }

    @Override
    public void visitClassType(String name) {

        signatureClassName = name;
        collector.addInternalName(name);
    }

    @Override
    public void visitInnerClassType(String name) {

        signatureClassName = signatureClassName + "$" + name;
        collector.addInternalName(signatureClassName);
    }
}


