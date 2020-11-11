package builder;

public class Main {

    public static void main(String[] args) {

        GraphBuilder builder = new GraphBuilder();
        JarHandler jarHandler = new JarHandler(builder);

        String jarName = "helloWorld.jar";
        jarHandler.readJar(jarName);

        String rootName = "user/helloWorld/___init";
        builder.setRootNode(rootName);

        GraphVisitor visitor = new GraphVisitor(builder);
        visitor.start();

        System.out.println("Total Nodes: " + builder.getGraphSize());
        System.out.println("Visited Nodes: " + builder.getVisitedCount());

        jarHandler.writeJar();
    }
}
