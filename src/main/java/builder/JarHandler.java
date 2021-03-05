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
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package builder;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * A class for reading and writing jar files
 */
public class JarHandler {

    private final GraphBuilder builder;
    private String jarName;

    public JarHandler(GraphBuilder builder) {

        this.builder = builder;
    }

    /**
     * Read the jar file and create graph nodes for .class filed
     */
    public void readJar(String jarName) {

        this.jarName = jarName;
        File file = new File(jarName);

        if (!file.exists()) {
            throw new IllegalArgumentException("Jar file doesn't exist");
        }

        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> entries = jar.entries();

            List<String> serviceProviders = new ArrayList<>();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                try (InputStream stream = jar.getInputStream(entry)) {
                    byte[] bytes = IOUtils.toByteArray(stream);

                    //if the current file is listed as a Service provider add it to the service
                    // providers list
                    if (!entry.isDirectory() && entry.getName().contains("META-INF/services/")) {

                        String providerName = getServiceProviderClassName(entry.getName());
                        serviceProviders.add(providerName);
                    }

                    //if file name ends with .class create a ClassGraphNode for it
                    if (entry.getName().endsWith(".class")) {
                        String className = getEntryClassName(entry.getName());
                        createNodeForClassFile(className, bytes);
                    }

                } catch (IOException e) {
                    System.out.println("Cannot read entry");
                }
            }

            //mark service provider class nodes for class names in the service provider list
            builder.setServiceProviders(serviceProviders);

        } catch (IOException e) {
            System.out.println("Cannot read Jar file");
        }
    }

    public void writeJar(String newJarName) {

        File file = new File(jarName);

        try (JarFile jar = new JarFile(file)) {

            //create a new jar file to add the optimized program files
            try (JarOutputStream newJar = new JarOutputStream(new FileOutputStream(newJarName))) {
                byte[] buffer = new byte[1024];
                int bytesRead;

                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    InputStream stream;

                    if (entry.getName().endsWith(".class")) {
                        String className = getEntryClassName(entry.getName());
                        ClassGraphNode classGraphNode = builder.getNodeByName(className);

                        //If the ClassGraphNode is not marked as used don't add it to the final jar file.
                        if (!classGraphNode.isUsed() && ((classGraphNode.access & Opcodes.ACC_INTERFACE) == 0)) {
                            continue;
                        }

                        builder.countUsed();

                        //remove unused methods and get the byte array of the modified class
                        byte[] modifiedClassBytes = builder.removeUnusedMethods(classGraphNode);

                        stream = new ByteArrayInputStream(modifiedClassBytes);

                        //create a new JarEntry for the modified class
                        entry = new JarEntry(new ZipEntry(entry.getName()));
                        entry.setSize(modifiedClassBytes.length);
                    } else {
                        stream = jar.getInputStream(entry);
                    }

                    newJar.putNextEntry(entry);

                    while ((bytesRead = stream.read(buffer)) != -1) {
                        newJar.write(buffer, 0, bytesRead);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the class name from the name of the .class file
     */
    public String getEntryClassName(String entryName) {

        int n = entryName.lastIndexOf('.');
        return entryName.substring(0, n);
    }

    /**
     * Get the class name of the service provider from the file in "META-INF/services/"
     */
    public String getServiceProviderClassName(String providerFileName) {

        int i = providerFileName.lastIndexOf('/');
        return providerFileName.substring(i + 1).replace(".", "/");
    }

    public void createNodeForClassFile(String className, byte[] bytes) {

        builder.addNewNode(className, bytes);
    }
}
