/*
 * Copyright (c)  2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package builder;

public class Main {

    public static void main(String[] args) {

        long startTime = System.nanoTime();

        GraphBuilder builder = new GraphBuilder();
        JarHandler jarHandler = new JarHandler(builder);

        String jarName = args[0];
        jarHandler.readJar(jarName);

        String rootName = args[1];
        builder.setRootNode(rootName);

        builder.build();
        jarHandler.writeJar(args[2]);

        long executionTime = System.nanoTime() - startTime;

        System.out.println("Total Nodes: " + builder.getGraphSize());
        System.out.println("Visited Nodes: " + builder.getVisitedCount());
        System.out.println("Used Nodes: " + builder.getUsedCount());
        System.out.println("Total methods: " + GraphBuilder.totalMethodCount);
        System.out.println("visited methods: " + GraphBuilder.visitedMethodCount);
        System.out.println("Execution Time in Millis: " + executionTime / 1000000);
    }
}
