# Bytecode Optimizer
A Java program to optimize a Java jar file. The optimizer was designed as a part of the Ballerina language platform. However, it can be used for any language that compiles to Java bytecode. Under the current implementation, the optimizer is capable of removing unused methods and classes in a jar file. 

# Usage
Clone the Bytecode Optimizer repository and build the fat jar using gradle.
```
gradle jar 
```
When executing the fat jar, pass the name of the input and output jar files and the name of the entry class that contains the main method.
```
java -jar BytecodeOptimizer.jar inputjar.jar name/of/entry/class outputjar.jar
```

# Implementation
To identify unused methods in the jar file, and by extension unused classes, Bytecode Optimizer constructs the callgraph of the given program, using Class Hierarchy Analysis (CHA) algorithm, starting from the main method defined in the entry class. To conduct the analysis, Bytecode Optimizer utilizes ASM's tree-based visitors including ClassNode and MethodNode. On top of the callgraph construction, ASM ClassNode is used to identified field types used in the program that are not captured through the callgraph contstruction. 

The current high-level design of the optimizer is as follows. 
![alt text](https://github.com/anjaleeps/BytecodeOptimizer/blob/main/Bytecode%20Optimizer%20design.png)

## Building Class Hierarchy

As the first step, to help the CHA analysis, the program maps the class heirarchy among the class nodes in the graph. 
```Java
public void buildClassHierarchy() {

    for (String name : nodes.keySet()) {
        ClassGraphNode current = nodes.get(name);
        setSuperNode(current);
        setInterfaces(current);
    }
    for (String name : javaNodes.keySet()) {
        ClassGraphNode current = javaNodes.get(name);
        setJavaSuperNode(current);
        setJavaInterfaces(current);
    }
}
```

## Visiting method calls

Then, the optimizer starts visiting used methods in the program starting from the main method. 

```java
findLinkedMethods(rootNode);
```
`findLinkedMethods` method contains the main logic for building the call graph. It checks used method nodes in a class node and visit its field dependencies and called methods. 

```java
public void findLinkedMethods(ClassGraphNode node) {

    //visit the unvisited but used methods in the class node
    visitNodeForMethods(node);

    for (MethodNode methodNode : node.methods) {
        MethodGraphNode method = (MethodGraphNode) methodNode;

        //check the instructions used in a used method to find out called methods
        //pass if the called methods have already been visited
        if (method.isUsed() && !method.isCalledVisited()) {

            method.markAsCalledVisited();
            visitDependencies(method);

            InsnList instructions = method.instructions;

            for (int i = 0; i < instructions.size(); i++) {
                AbstractInsnNode insnNode = instructions.get(i);

                //check if the instruction type is of INVOKE_STATIC, INVOKE_VIRTUAL,
                // INVOKE_SPECIAL, and INVOKE_INTERFACE types
                if (insnNode.getType() == AbstractInsnNode.METHOD_INSN) {
                    visitMethodInsn((MethodInsnNode) insnNode, method);
                }
                //check if instruction type id of INVOKE_DYNAMIC type
                else if (insnNode.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN) {
                    visitInvokeDynamicInsn((InvokeDynamicInsnNode) insnNode, method);
                }
            }
        }
    }
}
```
# License

Distributed under the Apache 2.0 license. See [License](https://github.com/anjaleeps/BytecodeOptimizer/blob/main/LICENSE) for more information.
