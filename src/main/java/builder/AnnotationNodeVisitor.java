package builder;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.ASM6;

public class AnnotationNodeVisitor extends AnnotationVisitor {

    private DependencyCollector collector;
    public AnnotationNodeVisitor(DependencyCollector collector) {

        super(ASM6);
        this.collector = collector;
    }

    @Override
    public void visit(String name, Object value) {

        if (value instanceof Type) {
            collector.addType((Type) value);
        }
    }

    @Override
    public void visitEnum(String name, String desc, String value) {

        collector.addDesc(desc);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc) {

        collector.addDesc(desc);
        return this;
    }

    @Override
    public AnnotationVisitor visitArray(String name) {

        return this;
    }
}
