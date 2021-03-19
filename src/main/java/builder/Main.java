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

/**
 * The main class of the optimizer.
 */
public class Main {

    public static void main(String[] args) {
        if (args[0].isEmpty()) {
            throw new IllegalArgumentException("No config file provided");
        }
        String configFilePath = args[0].trim();
        ConfigReader configReader = new ConfigReader(configFilePath);
        GraphBuilder builder = new GraphBuilder(configReader);
        JarHandler jarHandler = new JarHandler(builder, configReader);

        jarHandler.readJar();
        builder.build();
        jarHandler.writeJar();

        System.out.println("Total Nodes: " + builder.getGraphSize());
        System.out.println("Visited Nodes: " + builder.getVisitedCount());
        System.out.println("Used Nodes: " + builder.getUsedCount());
    }
}
