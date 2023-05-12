/*
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
package org.weakref.eval.core;

public class Native
{
    public static String resourceName(String library)
    {
        return library + "-" + osName() + "-" + osArch() + "." + extension();
    }

    private static String osName()
    {
        String os = System.getProperty("os.name").toLowerCase().replace(' ', '_');
        if (os.startsWith("mac")) {
            return "darwin";
        }
        else {
            return os;
        }
    }

    private static String osArch()
    {
        return System.getProperty("os.arch");
    }

    private static String extension()
    {
        if (osName().contains("os_x") || osName().contains("darwin")) {
            return "dylib";
        }
        else {
            return "so";
        }
    }
}
