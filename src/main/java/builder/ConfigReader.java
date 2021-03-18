/*
 * Copyright (c)  2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ConfigReader {

    final String inputJarName;
    final String outputJarName;
    final String rootName;
    final boolean optimizeClassesOnly;
    private List<String> keepClasses = new ArrayList<>();

    public ConfigReader(String configFilePath) {
        Properties properties = new Properties();

        try (FileReader reader = new FileReader(configFilePath)) {
            properties.load(reader);
            this.inputJarName = properties.getProperty("inputJar");
            this.rootName = properties.getProperty("mainMethodClass");
            this.optimizeClassesOnly = Boolean.parseBoolean(properties.getProperty("noUnusedMethodRemoval"));
            this.outputJarName = properties.getProperty("outputJar");
            if (!outputJarName.endsWith(".jar")){
                throw new IllegalArgumentException("Output file name should be of jar type");
            }
            addKeepClasses(properties.getProperty("keepClasses"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Config file does not exist", e);
        } catch (IOException e) {
            throw new RuntimeException("Error reading the config file", e);
        }
    }

    private void addKeepClasses(String classNames) {
        if (classNames != null) {
            keepClasses.addAll(Arrays.stream(classNames.split(",")).map(String::trim).collect(Collectors.toList()));
        }
    }

    public List<String> getKeepClasses() {
        return keepClasses;
    }
}
