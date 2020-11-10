package builder;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarHandler {

    private GraphBuilder builder;
    private String jarName;

    public JarHandler(GraphBuilder builder) {

        this.builder = builder;
    }

    public void readJar(String jarName) {

        this.jarName = jarName;
        File file = new File(jarName);

        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                try (InputStream stream = jar.getInputStream(entry)) {
                    byte[] bytes = IOUtils.toByteArray(stream);

                    if (entry.getName().endsWith(".class")) {
                        String className = getEntryClassName(entry.getName());
                        createNodeForClass(className, bytes);
                    }

                } catch (IOException e) {
                    System.out.println("Cannot read entry");
                }
            }
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

            Manifest jarManifest = jar.getManifest();
            try (JarOutputStream newJar = new JarOutputStream(new FileOutputStream("modified.jar"))){
                byte[] buffer = new byte[1024];
                int bytesRead;


                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    if (entry.getName().endsWith(".class")) {
                        String className = getEntryClassName(entry.getName());
                        ClassGraphNode classGraphNode = GraphBuilder.getNodeByName(className);

                        if (!classGraphNode.isVisited()) {
                            continue;
                        }
                    }

                    try(InputStream stream = jar.getInputStream(entry)){
                        newJar.putNextEntry(entry);

                        while ((bytesRead = stream.read(buffer)) != -1) {
                            newJar.write(buffer, 0, bytesRead);
                        }
                    }
                    catch(IOException e){
                        e.printStackTrace();
                    }
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
