/*
 * Copyright (c)  2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package builder;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM6;

/**
 * A class representing a node created for each class file in the jar file
 */
public class ClassGraphNode extends ClassNode {

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

    public boolean isUsed() {

        return used;
    }

    public void markAsVisited() {

        visited = true;
    }

    public void markAsUsed() {

        used = true;
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

    public String getSuperName() {

        return reader.getSuperName();
    }

    public ClassGraphNode getSuperNode() {

        return superNode;
    }

    public void setSuperNode(ClassGraphNode superNode) {

        this.superNode = superNode;
    }

    public String[] getInterfaceNames() {

        return reader.getInterfaces();
    }

    public List<ClassGraphNode> getInterfaceNodes() {

        return interfaceNodes;
    }

    public void setInterfaceNodes(List<ClassGraphNode> interfaceNodes) {

        this.interfaceNodes = interfaceNodes;
    }

    /**
     * accepts a ClassVisitor object and pass it to the accept method of ClassReader
     *
     **/
    @Override
    public void accept(ClassVisitor cv) {

        if (cv instanceof ClassNode){
            ClassNode cn = (ClassNode) cv;
            cn.name = name;
            cn.methods = methods;
            cn.access = access;
        }

        reader.accept(cv, 0);
    }
}
