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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RTA {

    List<String> instantiatedClassNames;
    MethodGraphNode mainMethod;
    GraphBuilder builder;

    public RTA(List<String> instantiatedClassNames, MethodGraphNode mainMethod, GraphBuilder builder) {

        this.instantiatedClassNames = instantiatedClassNames;
        this.mainMethod = mainMethod;
        this.builder = builder;
    }

    public void start() {

        visitMethodCalls(mainMethod);
    }

    public void visitMethodCalls(MethodGraphNode method) {

        method.markAsCheckedByRTA();
        Set<MethodGraphNode> calledMethods = new HashSet<>(method.getResolvedAtRuntimeMethodCalls());
        for (MethodGraphNode calledMethod : calledMethods) {
            if (!instantiatedClassNames.contains(calledMethod.owner)) {
                removeCallingMethod(calledMethod, method);
            } else if (!calledMethod.isCheckedByRTA()) {
                visitMethodCalls(calledMethod);
            }
        }
        for (MethodGraphNode calledMethod : method.getOtherMethodCalls()) {
            if (!calledMethod.isCheckedByRTA()) {
                visitMethodCalls(calledMethod);
            }
        }
    }

    public void checkSubGraph(MethodGraphNode unusedMethod) {

        for (MethodGraphNode calledMethod : unusedMethod.getResolvedAtRuntimeMethodCalls()) {
            removeCallingMethod(calledMethod, unusedMethod);
        }
        for (MethodGraphNode calledMethod : unusedMethod.getOtherMethodCalls()) {
            removeCallingMethod(calledMethod, unusedMethod);
        }
    }

    public void removeCallingMethod(MethodGraphNode calledMethod, MethodGraphNode callingMethod) {

        calledMethod.removeResolvedAtRuntimeMethodCall(calledMethod);
        boolean isCalledByAny = calledMethod.removeCallingMethod(callingMethod);
        if (!isCalledByAny) {
            checkSubGraph(calledMethod);
        }
    }
}
