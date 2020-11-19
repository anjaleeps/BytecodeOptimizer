package builder;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM6;

public class ClassGraphNode extends ClassNode {

    Set<ClassGraphNode> methodDependencies = new HashSet<>();
    Set<MethodGraphNode> usedMethods = new HashSet<>();
    private List<ClassGraphNode> childNodes = new ArrayList<>();
    private Set<ClassGraphNode> dependencies = new HashSet<>();
    private ClassReader reader;
    private ClassGraphNode superNode;
    private List<ClassGraphNode> interfaceNodes;
    private boolean visited;
    private boolean used;
    private boolean isServiceProvider;

    public ClassGraphNode(String name, byte[] bytes) {

        super(ASM6);
        this.name = name;
        this.reader = new ClassReader(bytes);
        visited = false;
        used = false;
        isServiceProvider = false;
    }

    public boolean isVisited() {

        return visited;
    }

    public boolean isServiceProvider() {

        return isServiceProvider;
    }

    public void markAsServiceProvider() {

        isServiceProvider = true;
    }

    public void addChildNode(ClassGraphNode childNode) {

        childNodes.add(childNode);
    }

    public List<ClassGraphNode> getChildNodes() {

        return childNodes;
    }

    public Set<ClassGraphNode> getDependencies() {

        return dependencies;
    }

    public void setDependencies(Set<ClassGraphNode> dependencies) {

        this.dependencies = dependencies;
    }

    public Set<ClassGraphNode> getMethodDependencies() {

        return methodDependencies;
    }

    public void setMethodDependencies(Set<ClassGraphNode> methodDependencies) {

        this.methodDependencies = methodDependencies;
    }

    public String getSuperName() {

        return reader.getSuperName();
    }

    public void setSuperNode(ClassGraphNode superNode) {

        this.superNode = superNode;
    }

    public String[] getInterfaceNames() {

        return reader.getInterfaces();
    }

    public void setInterfaceNodes(List<ClassGraphNode> interfaceNodes) {

        this.interfaceNodes = interfaceNodes;
    }

    @Override
    public void accept(ClassVisitor cv) {

        ClassNode cn = (ClassNode) cv;
        cn.name = name;
        cn.methods = methods;

        if (cn instanceof ClassGraphVisitor) {
            ((ClassGraphVisitor) cn).usedMethods = usedMethods;
        }
        reader.accept(cn, 0);
    }

    public void markAsVisited() {

        visited = true;
    }

    public void markAsUsed() {

        used = true;
    }
}
