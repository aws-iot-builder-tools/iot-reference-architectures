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
    private static final String ABOUT = "about";
    private static final String RASPBERRYPI = "raspberrypi";
    private static final String BUILDS = "builds";
    private static final String SYSTEMS = "systems";
    private static final String TERMINALS = "terminals";
    private static final String START = ABOUT;

    // The static methods are necessary for auto-complete support in the ui.xml files in IntelliJ

    public static String start() {
        return START;
    }

    public static String about() {
        return ABOUT;
    }

    public static String raspberryPi() {
        return RASPBERRYPI;
    }

    public static String builds() {
        return BUILDS;
    }

    public static String systems() {
        return SYSTEMS;
    }

    public static String terminals() {
        return TERMINALS;
    }
}
