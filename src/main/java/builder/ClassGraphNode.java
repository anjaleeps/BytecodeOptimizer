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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM9;

/**
 * A class representing a node created for each class file in the jar file.
 */
public class ClassGraphNode extends ClassNode {

    private List<ClassGraphNode> childNodes = new ArrayList<>();
    private ClassReader reader;
    private ClassGraphNode superNode;
    private List<ClassGraphNode> interfaceNodes;
    private boolean visited;
    private boolean used;
    private boolean isServiceProvider;
    private DependencyCollector collector;

    public ClassGraphNode(String name) {
        super(ASM9);
        this.name = name;
        visited = false;
        used = false;
        isServiceProvider = false;
        collector = new DependencyCollector();
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

    public Set<String> getDependencies() {
        return collector.getDependencies();
    }

    public void addChildNode(ClassGraphNode childNode) {
        childNodes.add(childNode);
    }

    public void setReader(byte[] bytes) {
        reader = new ClassReader(bytes);
    }

    public void setReader() {
        try {
            this.reader = new ClassReader(this.name);
        } catch (IOException ignored) {
        }
    }

    public List<ClassGraphNode> getChildNodes() {
        return childNodes;
    }

    public String getSuperName() {
        if (reader == null) {
            return null;
        }
        this.superName = reader.getSuperName();
        return superName;
    }

    public ClassGraphNode getSuperNode() {
        return superNode;
    }

    public void setSuperNode(ClassGraphNode superNode) {
        this.superNode = superNode;
    }

    public String[] getInterfaceNames() {
        if (reader == null) {
            return null;
        }
        this.interfaces = Arrays.asList(reader.getInterfaces());
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
     **/
    @Override
    public void accept(ClassVisitor cv) {
        ClassNode cn = (ClassNode) cv;
        cn.name = name;
        cn.methods = methods;
        cn.access = access;
        cn.superName = superName;

        if (cn instanceof ClassNodeVisitor) {
            ClassNodeVisitor cnv = (ClassNodeVisitor) cn;
            cnv.setCollector(collector);
        }
        reader.accept(cn, 0);
        this.methods = cn.methods;
        this.access = cn.access;
    }
}
