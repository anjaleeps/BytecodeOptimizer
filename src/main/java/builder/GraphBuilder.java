package builder;

import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class to handle the graph creation and visiting
 */
public class GraphBuilder {

    static int visitedMethodCount;
    private Map<String, ClassGraphNode> nodes;
    private List<String> instantiatedClasses;
    private int visitedCount;
    private int usedCount;
    private ClassGraphNode rootNode;

    public GraphBuilder() {

        visitedCount = 0;
        usedCount = 0;
        nodes = new HashMap<>();
        instantiatedClasses = new ArrayList<>();
    }

    public void build() {

        buildClassHierarchy();
        visitNode(rootNode);
        completeNodeVisit(rootNode);
        markMainMethod();
        findLinkedMethods(rootNode);
    }

    /**
     * Mark the main method of the root class as used
     */
    public void markMainMethod() {

        for (MethodNode method : rootNode.methods) {

            if (method.name.equals("main")) {
                MethodGraphNode methodNode = (MethodGraphNode) method;
                methodNode.markAsUsed();
            }
        }
    }

    /**
     * Visit the class for used methods and link methods called inside a used method to the used method
     */
    public void findLinkedMethods(ClassGraphNode node) {

        visitNodeForMethods(node);

        for (MethodNode methodNode : node.methods) {
            MethodGraphNode method = (MethodGraphNode) methodNode;

            if (method.isVisited() && !method.isCalledVisited()) {

                method.markAsCalledVisited();
                InsnList instructions = method.instructions;

                for (int i = 0; i < instructions.size(); i++) {
                    AbstractInsnNode insnNode = instructions.get(i);

                    if (insnNode.getType() == AbstractInsnNode.METHOD_INSN) {
                        visitMethodInsn((MethodInsnNode) insnNode, method);
                    } else if (insnNode.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN) {
                        visitInvokeDynamicInsn((InvokeDynamicInsnNode) insnNode, method);
                    }
                }
            }
        }
    }

    public void visitMethodInsn(MethodInsnNode methodInsnNode, MethodGraphNode method) {

        ClassGraphNode owner = getNodeByName(methodInsnNode.owner);

        //check if called method belongs to a Java library class
        if (owner == null) {
            return;
        }

        //if the called method instantiate an object, add it's class name to the list of initialized classes
        if (methodInsnNode.name.equals("<init>")) {
            instantiatedClasses.add(methodInsnNode.owner);
        }

        //Create MethodGraphNode for the method called inside the current method
        MethodGraphNode mn = new MethodGraphNode(-1, methodInsnNode.owner,
                methodInsnNode.name, methodInsnNode.desc, null, null);

        checkUsedMethod(owner, mn, method);
        checkChildrenForUsedMethod(owner, mn, method);
        checkInterfacesForUsedMethods(owner, mn, method);
    }

    public void visitInvokeDynamicInsn(InvokeDynamicInsnNode invokeDynamicInsnNode, MethodGraphNode method) {

        Object[] bsmArgs = invokeDynamicInsnNode.bsmArgs;

        for (int j = 0; j < bsmArgs.length; j++) {

            if (bsmArgs[j] instanceof Handle) {
                Handle handle = (Handle) bsmArgs[j];
                ClassGraphNode owner = getNodeByName(handle.getOwner());

                if (owner == null) {
                    continue;
                }

                MethodGraphNode mn = new MethodGraphNode(-1, handle.getOwner(), handle.getName(),
                        handle.getDesc(), null, null);

                checkUsedMethod(owner, mn, method);

                if (!mn.name.contains("lambda")) {
                    checkChildrenForUsedMethod(owner, mn, method);
                    checkInterfacesForUsedMethods(owner, mn, method);
                }
            }
        }
    }

    /**
     * Check if the called method is defined inside its owner class
     */
    public void checkUsedMethod(ClassGraphNode owner, MethodGraphNode mn, MethodGraphNode current) {

        if (!owner.isVisited()) {
            visitNode(owner);
        }

        int index = owner.methods.indexOf(mn);

        //filters methods that are not defined inside the class the called method was called with
        if (index < 0) {
            //check if the parent node defines the called method
            checkParentForUsedMethod(owner, mn, current);
            return;
        }

        if (!owner.isUsed()) {

            owner.markAsUsed();
            completeNodeVisit(owner);
        }

        visitMethodOwnerNode(owner, index, current);
    }

    /**
     * When the class the called method was called with does not define the called method,
     * check if its parent node defines the class
     * <p>
     * Ex:
     * class Foo {
     * public void read(){}
     * }
     * class Bar extends Foo{}
     * <p>
     * Bar bar = new Bar()
     * bar.read()
     */
    public void checkParentForUsedMethod(ClassGraphNode owner, MethodGraphNode mn, MethodGraphNode current) {

        ClassGraphNode superNode = owner.getSuperNode();
        if (superNode != null) {
            mn.owner = superNode.name;
            checkUsedMethod(superNode, mn, current);
        }
    }

    /**
     * If an interface implemented by the owner of the called class defines the called method, mark it as used inside
     * the interface to preserve it from being removed
     */
    public void checkInterfacesForUsedMethods(ClassGraphNode owner, MethodGraphNode mn, MethodGraphNode current) {

        for (ClassGraphNode interfaceNode : owner.getInterfaceNodes()) {

            mn.owner = interfaceNode.name;
            checkUsedMethod(interfaceNode, mn, current);
        }
    }

    /**
     * Check if the called method is defined inside a child method of the owner class, since which method is called
     * is resolved at the runtime
     * <p>
     * Ex:
     * <p>
     * class Foo {
     * public void read()
     * }
     * <p>
     * class Bar extends Foo{
     * public void read()
     * }
     * <p>
     * Scenario 1:
     * <p>
     * Class Main {
     * public void main(){
     * Foo foo = new Bar()
     * foo.read()
     * }
     * }
     * <p>
     * Scenario 2:
     * <p>
     * Class Baz {
     * public void do(Foo foo){
     * foo.read()
     * }
     * }
     * <p>
     * Class Main {
     * public void main(){
     * Baz baz = new Baz()
     * baz.do(new Bar())
     * }
     * }
     */
    public void checkChildrenForUsedMethod(ClassGraphNode owner, MethodGraphNode mn, MethodGraphNode current) {

        for (ClassGraphNode childNode : owner.getChildNodes()) {

            //if the child class was instantiated up till this point, check if the child class has also implemented
            // the called method
            if (instantiatedClasses.contains(childNode.name)) {
                mn.owner = childNode.name;
                checkUsedMethod(childNode, mn, current);
            } else if (owner.isServiceProvider()) {
                mn.owner = childNode.name;
                checkUsedMethod(childNode, mn, current);
            }
        }
    }

    /**
     * Get the MethodGraphNe object of the called method from the method list of its owner.
     * Link the called method to the current calle method as a called method.
     * Visit the owner class of the used method if it is newly marked as used.
     */
    public void visitMethodOwnerNode(ClassGraphNode owner, int methodIndex, MethodGraphNode current) {

        MethodGraphNode usedMethod = (MethodGraphNode) owner.methods.get(methodIndex);
        current.calledMethods.add(usedMethod);

        if (!usedMethod.isUsed()) {

            usedMethod.markAsUsed();
            findLinkedMethods(owner);
        }
    }

    /**
     * Mark the static blocks used inside a class
     **/
    public void markStaticBlocks(ClassGraphNode node) {

        //Create a MethodGraphNode for a potential static block
        MethodGraphNode mn = new MethodGraphNode(-1, node.name, "<clinit>", "()V", null, null);
        int i = node.methods.indexOf(mn);

        //Filters classes that have static blocks
        if (i >= 0) {
            mn = (MethodGraphNode) node.methods.get(i);
            mn.markAsUsed();
        }
    }

    /**
     * Visit the ClassGraphNode for the first time using ClassNodeVisitor
     **/
    public void visitNode(ClassGraphNode node) {

        DependencyCollector collector = new DependencyCollector(this);
        ClassNodeVisitor cv = new ClassNodeVisitor(collector);
        node.accept(cv);
        markStaticBlocks(node);
        updateNode(node, cv, collector);
        countVisited();
    }

    public void visitNodeForMethods(ClassGraphNode node) {

        DependencyCollector collector = new DependencyCollector(this);
        ClassVisitorForMethods cv = new ClassVisitorForMethods(collector);

        //Visit the ClassGraphNode for the second time using the ClassVisitorForMethods
        node.accept(cv);
        updateNode(node, cv, collector);
        visitDependencies(node);
    }

    public void completeNodeVisit(ClassGraphNode node) {

        visitDependencies(node);

        //Visit the class's child nodes only if the class is a service provider
        if (node.isServiceProvider()) {
            visitChildNodes(node);
        }
    }

    /**
     * Visit the class dependencies of the current class
     **/
    public void visitDependencies(ClassGraphNode node) {

        for (ClassGraphNode current : node.getDependencies()) {
            current.markAsUsed();

            if (!current.isVisited()) {
                visitNode(current);
                completeNodeVisit(current);
            }

            findLinkedMethods(current);
        }
    }

    /**
     * Visit the child nodes of the current class
     **/
    public void visitChildNodes(ClassGraphNode node) {

        for (ClassGraphNode current : node.getChildNodes()) {
            current.markAsUsed();

            if (!current.isVisited()) {
                visitNode(current);
                findLinkedMethods(current);
            }

            completeNodeVisit(current);
        }
    }

    /**
     * Pass the information gathered by the ClassVisitor to the ClassGraphNode object
     **/
    public void updateNode(ClassGraphNode node, ClassNode cv, DependencyCollector collector) {

        node.setDependencies(collector.getDependencies());
        node.methods = cv.methods;
        node.markAsVisited();
        nodes.put(node.name, node);
    }

    public ClassGraphNode getNodeByName(String name) {

        return nodes.get(name);
    }

    public int getGraphSize() {

        return nodes.size();
    }

    public void countVisited() {

        visitedCount++;
    }

    public void countUsed() {

        usedCount++;
    }

    public int getVisitedCount() {

        return visitedCount;
    }

    public int getUsedCount() {

        return usedCount;
    }

    public void addNewNode(String name, byte[] bytes) {

        ClassGraphNode newNode = new ClassGraphNode(name, bytes);
        nodes.put(name, newNode);
    }

    public void setRootNode(String rootName) {

        rootNode = getNodeByName(rootName);
        rootNode.markAsUsed();
    }

    /**
     * Visit every ClassGraphNode created and build a class hierarchy by assigning their child and super nodes
     */
    public void buildClassHierarchy() {

        for (String name : nodes.keySet()) {
            ClassGraphNode current = nodes.get(name);
            setSuperNode(current);
            setInterfaces(current);
        }
    }

    public void setSuperNode(ClassGraphNode current) {

        ClassGraphNode superNode = getNodeByName(current.getSuperName());
        if (superNode != null) {
            current.setSuperNode(superNode);
            superNode.addChildNode(current);
        }
    }

    public void setInterfaces(ClassGraphNode current) {

        List<ClassGraphNode> interfaceNodes = new ArrayList<>();

        for (int i = 0; i < current.getInterfaceNames().length; i++) {
            ClassGraphNode itf = getNodeByName(current.getInterfaceNames()[i]);

            if (itf != null) {
                interfaceNodes.add(itf);
                itf.addChildNode(current);
            }
        }

        current.setInterfaceNodes(interfaceNodes);
    }

    /**
     * Mark the class as a service provider
     */
    public void setServiceProviders(List<String> serviceProviders) {

        for (String name : serviceProviders) {
            ClassGraphNode providerNode = getNodeByName(name);
            if (providerNode != null) {
                providerNode.markAsServiceProvider();
            }
        }
    }
}
