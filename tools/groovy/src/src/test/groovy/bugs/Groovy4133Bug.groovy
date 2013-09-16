/*
 * Copyright 2003-2010 the original author or authors.
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
package groovy.bugs

class Groovy4133Bug extends GroovyTestCase {
    void testDelegateAnnotationWithNativeMethods() {
        new GroovyShell().evaluate """
            class String4133 {
                @Delegate String str
            }
            
            /* loading of class used to fail earlier with ClassFormatError */
            assert String4133 != null
        """
    }
}