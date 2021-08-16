/*
 * #%L
 * GwtMaterial
 * %%
 * Copyright (C) 2015 - 2017 GwtMaterialDesign
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package awslabs.client.place;

public class NameTokens {
    public static final String CREATE_AND_VALIDATE = "createAndValidate";
    public static final String ATTRIBUTION = "attribution";
    public static final String TEST = "test";
    private static final String START = CREATE_AND_VALIDATE;

    // The static methods are necessary for auto-complete support in the ui.xml files in IntelliJ
    public static String start() {
        return START;
    }

    public static String createAndValidate() {
        return CREATE_AND_VALIDATE;
    }

    public static String attribution() {
        return ATTRIBUTION;
    }

    public static String test() {
        return TEST;
    }
}
