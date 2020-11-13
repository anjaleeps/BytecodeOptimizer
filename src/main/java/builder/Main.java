package builder;

public class Main {

    public static void main(String[] args) {

        long startTime = System.nanoTime();

        GraphBuilder builder = new GraphBuilder();
        JarHandler jarHandler = new JarHandler(builder);

        String jarName = "helloWorldMain.jar";
        jarHandler.readJar(jarName);

        String rootName = "user/helloWorldMain/___init";
        builder.setRootNode(rootName);

        builder.build();
        jarHandler.writeJar();

        long executionTime = System.nanoTime() - startTime;

        System.out.println("Total Nodes: " + builder.getGraphSize());
        System.out.println("Visited Nodes: " + builder.getVisitedCount());
        System.out.println("Execution Time in Millis: " + executionTime / 1000000);
    }
}
