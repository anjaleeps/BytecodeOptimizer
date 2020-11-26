package builder;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.tree.MethodNode;

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

/**
 * A class for reading and writing the jar file
 * */
public class JarHandler {

    private final GraphBuilder builder;
    private String jarName;

    public JarHandler(GraphBuilder builder) {

        this.builder = builder;
    }

    public void readJar(String jarName) {

        this.jarName = jarName;
        File file = new File(jarName);

        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> entries = jar.entries();

            List<String> serviceProviders = new ArrayList<>();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                try (InputStream stream = jar.getInputStream(entry)) {
                    byte[] bytes = IOUtils.toByteArray(stream);

                    //if the current file is listed as a Service provider for the program add it to the service
                    // provider list
                    if (!entry.isDirectory() && entry.getName().contains("META-INF/services/")) {

                        String providerName = entry.getName();
                        int i = providerName.lastIndexOf('/');
                        providerName = providerName.substring(i + 1);
                        serviceProviders.add(providerName.replace(".", "/"));
                    }

                    //if file name ends with .class create a ClassGraphNode for it
                    if (entry.getName().endsWith(".class")) {
                        String className = getEntryClassName(entry.getName());
                        createNodeForClass(className, bytes);
                    }

                } catch (IOException e) {
                    System.out.println("Cannot read entry");
                }
            }

            builder.setServiceProviders(serviceProviders);

        } catch (IOException e) {
            System.out.println("Cannot read Jar file");
        }
    }

    public String getEntryClassName(String entryName) {

        int n = entryName.lastIndexOf('.');
        return entryName.substring(0, n);
    }

    public void createNodeForClass(String className, byte[] bytes) {

        builder.addNewNode(className, bytes);
    }

    public void writeJar() {

        File file = new File(jarName);

        try (JarFile jar = new JarFile(file)) {

            try (JarOutputStream newJar = new JarOutputStream(new FileOutputStream("modified.jar"))) {
                byte[] buffer = new byte[1024];
                int bytesRead;

                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    if (entry.getName().endsWith(".class")) {
                        String className = getEntryClassName(entry.getName());
                        ClassGraphNode classGraphNode = builder.getNodeByName(className);

                        //If the ClassGraphNode is marked as used add its class to the final jar file.
                        if (!classGraphNode.isUsed()) {
                            continue;
                        }

                        builder.countUsed();
                    }

                    try (InputStream stream = jar.getInputStream(entry)) {
                        newJar.putNextEntry(entry);

                        while ((bytesRead = stream.read(buffer)) != -1) {
                            newJar.write(buffer, 0, bytesRead);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
