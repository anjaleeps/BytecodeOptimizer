package builder;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DependencyCollector {

    final Pattern pattern;
    private Set<ClassGraphNode> dependencies = new HashSet<>();
    private HashSet<ClassGraphNode> methodDependencies = new HashSet<>();
    private GraphBuilder builder;
    MethodGraphNode visitingMethod;

    public DependencyCollector(GraphBuilder builder){
        pattern = Pattern.compile("([a-zA-Z]\\w+(/|[.]))+(\\w|[$])+");
        this.builder = builder;
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

    public void addSignature(String signature) {

        if (signature != null) {

            new SignatureReader(signature).accept(new SignatureNodeVisitor(this));
        }
    }

    public void addTypeSignature(String signature) {

        if (signature != null) {
            new SignatureReader(signature).acceptType(new SignatureNodeVisitor(this));
        }
    }

    public void addConstant(Object constant) {

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

    public void addMethodDesc(String desc) {

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

        ClassGraphNode node = builder.getNodeByName(name);
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

    public Set<ClassGraphNode> getDependencies(){

        return dependencies;
    }



    public Set<ClassGraphNode> getMethodDependencies(){

        return methodDependencies;
    }

    public boolean markUsedMethod(String owner, MethodGraphNode mn){

        ClassGraphNode node = builder.getNodeByName(owner);
        if (node != null){
            node.markAsUsed();
            mn.markAsUsed();

            if (node.methods.indexOf(mn) < 0){
                node.methods.add(mn);
            }
            return true;
        }
        return false;
    }
}
