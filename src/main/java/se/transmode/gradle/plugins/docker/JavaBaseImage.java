/**
 * Copyright 2014 Transmode AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.transmode.gradle.plugins.docker;

import org.gradle.api.JavaVersion;

/**
 * @author Matthias Grüter, matthias.gruter@transmode.com
 */
public enum JavaBaseImage {
    JAVA6("openjdk:6-jre", JavaVersion.VERSION_1_6),
    JAVA7("openjdk:7-jre", JavaVersion.VERSION_1_7),
    JAVA8("openjdk:8-jre", JavaVersion.VERSION_1_8),
    JAVA9("openjdk:9-jre", JavaVersion.VERSION_1_9);

    final String imageName;
    final JavaVersion target;

    JavaBaseImage(String imageName, JavaVersion target) {
        this.imageName = imageName;
        this.target = target;
    }

    public static JavaBaseImage imageFor(JavaVersion target) {
        for(JavaBaseImage image: JavaBaseImage.values()) {
            if(image.target == target) {
                return image;
            }
        }
        throw new IllegalArgumentException("No Java base image for target " + target + " found.");
    }
}
