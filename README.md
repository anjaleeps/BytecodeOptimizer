# Bytecode Optimizer
A Java program to optimize jar files. The optimizer was designed as a part of the Ballerina language platform. However, it can be used for any language that compiles into Java bytecode. The optimizer is capable of removing unused methods and classes in a jar file. It is capable of reducing the size of Ballerina-generated jar files by 40%-65%.

# Usage
Extract the optimizer fat jar and run it using the `java -jar` command. Pass the path to the config file containing config options as an argument. 
```
java -jar optimizer.jar ./optimizer.config
```
The config file accepts following configuaration options. 

>
>`inputJar`: Path to the jar file that needs to be optimized (mandatory)   
>`outputJar`: Path to the jar file the optimized program should be written to (mandatory)   
>`mainMethodClass`: Name of the class that contains the main method (mandatory)     
>`noUnusedMethodRemoval`: Set to `true` if the optimizer should remove only unused classes without removing unused methods (optional)   
>`keepClasses`: A comma separated list of class names that needs to be preserved by default during the optimization. All these classes and their methods will be >preserved in the output jar as they are. (optional)    
>

An example configuration file is shown below. 

```
//optimizer.config

inputJar:input.jar
outputJar:output.jar
mainMethodClass:user/demo/Main
noUnusedMethodRemoval:false
keepClasses:user/demo/KeepClass1,user/demo/KeepClass2,user/demo/KeepClass3

```

# Implementation
To identify unused methods in the jar file, and by extension unused classes, Bytecode Optimizer constructs the callgraph of the given program, using Class Hierarchy Analysis (CHA) algorithm, starting from the main method defined in the entry class. To conduct the analysis, Bytecode Optimizer utilizes ASM's tree-based visitors including ClassNode and MethodNode. On top of the callgraph construction, ASM ClassNode is used to identified field types used in the program that are not captured through the callgraph contstruction. 

The current high-level design of the optimizer is as follows. 
![design](/design.png)

The optimization process is carried out in three main steps. 
1. Build class hierarchy
2. Identify used classes
3. Identify used methods 


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

## Identifying Used Classes

To identify dependent classes of a used class, `ClassNodeVisitor` object is passed to each used class starting from the root node, the node which contains the main method. Dependent classes are then recognized as used classes and the process continues until all the used classes in the jar are identified by statically analyzing bytecode. 

```java
public void visitNode(ClassGraphNode node) {
    node.markAsVisited();
    node.accept(new ClassNodeVisitor());
    visitDependentNodes(node);
    if (node.isServiceProvider()) {
        visitChildNodes(node);
    }
}

private void visitDependentNodes(ClassGraphNode node) {
    for (String className: node.getDependencies()) {
        if (nodes.get(className) != null && !nodes.get(className).isVisited()) {
            visitNode(nodes.get(className));
        }
    }
}

private void visitChildNodes(ClassGraphNode node) {
    for (ClassGraphNode childNode : node.getChildNodes()) {
        if (!childNode.isVisited()){
            visitNode(childNode);
        }
    }
}
```

## Identifying Used Methods
To identify the used methods, the optimizer builds the callgraph of the program following an approach similar to Class Hierarchy Analysis. The main method in the root node is considered as the entry point to the callgraph construction. 

```java
private void findLinkedMethods(ClassGraphNode node) {

    visitNodeForMethods(node);

    for (MethodNode methodNode : node.methods) {
        MethodGraphNode method = (MethodGraphNode) methodNode;

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
`FindLinkedMethod` is then recursively called to identify methods called inside a used method until all the used methods are identified. 

# License

Distributed under the Apache 2.0 license. See [License](https://github.com/anjaleeps/BytecodeOptimizer/blob/main/LICENSE) for more information.
