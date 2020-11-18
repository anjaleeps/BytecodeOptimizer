package builder;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;

import static org.objectweb.asm.Opcodes.ASM6;

public class FieldNodeVisitor extends FieldVisitor {

    private DependencyCollector collector;

    public FieldNodeVisitor(DependencyCollector collector) {

        super(ASM6);
        this.collector = collector;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

        collector.addDesc(desc);
        return new AnnotationNodeVisitor(collector);
    }
}
