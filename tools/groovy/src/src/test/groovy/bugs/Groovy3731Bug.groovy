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

import gls.CompilableTestSupport

class Groovy3731Bug extends CompilableTestSupport {
    
    void testWrongGenericsUseInFieldsAndMethods() {
        shouldNotCompile """
            public class G3731A {
                Map<Object> m = [:]
            }
        """

        shouldNotCompile """
            public class G3731B {
                void m1(x, Map<Object> y) {}
            }
        """

        shouldNotCompile """
            public class G3731C {
                Map<Object> m1(x, y) {null}
            }
        """
    }
}