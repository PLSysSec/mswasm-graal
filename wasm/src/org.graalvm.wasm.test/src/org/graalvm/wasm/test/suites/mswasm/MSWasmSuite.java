/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.graalvm.wasm.test.suites.mswasm;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.graalvm.wasm.utils.cases.WasmCase;
import org.graalvm.wasm.utils.cases.WasmBinaryCase;
import org.graalvm.wasm.utils.cases.WasmCaseData;
import org.junit.Test;

import org.graalvm.wasm.test.WasmSuiteBase;
import java.io.InputStream;
import java.io.FileInputStream;

import java.util.Properties;

public class MSWasmSuite extends WasmSuiteBase {
    private String folderPath = "./src/org.graalvm.wasm.test/src/org/graalvm/wasm/test/suites/mswasm/tests/";

    private WasmBinaryCase[] testCases = {
                    WasmCase.create("STORE_AND_LOAD_1", WasmCase.expected(10),
                                    parseWasmFile(folderPath + "mswasm_store-load_1.wasm"),
                                    null, new Properties()), 
                    WasmCase.create("STORE_AND_LOAD_2", WasmCase.expected(0xfedc6543),
                                    parseWasmFile(folderPath + "mswasm_store-load_2.wasm"),
                                    null, new Properties()), 
                     WasmCase.create("ADD_1", WasmCase.expected(11),
                                    parseWasmFile(folderPath + "mswasm_add_1.wasm"),
                                    null, new Properties()),
                     WasmCase.create("ADD_2", WasmCase.expected(0),
                                    parseWasmFile(folderPath + "mswasm_add_2.wasm"),
                                    null, new Properties()),
                     WasmCase.create("ADD_3", WasmCase.expected(0x12345678),
                                    parseWasmFile(folderPath + "mswasm_add_3.wasm"),
                                    null, new Properties()),
                     WasmCase.create("ADD_3", WasmCase.expected(0x12345678),
                                    parseWasmFile(folderPath + "mswasm_add_3.wasm"),
                                    null, new Properties()),
                    WasmCase.create("ADD64_1", WasmCase.expected(0L),
                                    parseWasmFile(folderPath + "mswasm_add64_1.wasm"),
                                    null, new Properties()),
                    WasmCase.create("ADD64_2", WasmCase.expected(1234567895L),
                                    parseWasmFile(folderPath + "mswasm_add64_2.wasm"),
                                    null, new Properties()),
                    WasmCase.create("HANDLEADDSUB_1", WasmCase.expected(9),
                                    parseWasmFile(folderPath + "mswasm_handleaddsub_1.wasm"),
                                    null, new Properties()),
                    WasmCase.create("HANDLEADDSUB_2", WasmCase.expected(-19),
                                    parseWasmFile(folderPath + "mswasm_handleaddsub_2.wasm"),
                                    null, new Properties()),
                    WasmCase.create("SEGMENTSLICE_1", WasmCase.expected(10),
                                    parseWasmFile(folderPath + "mswasm_segmentslice_1.wasm"),
                                    null, new Properties()),
                    WasmCase.create("SEGMENTSLICE_2", WasmCase.expectedThrows("out-of-bounds handle", WasmCaseData.ErrorType.Runtime),
                                    parseWasmFile(folderPath + "mswasm_segmentslice_2.wasm"),
                                    null, new Properties()),
    };

    private byte[] parseWasmFile(String fileName) {
        try {
            File file = new File(fileName);
            InputStream input = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            input.read(bytes);
            input.close();
            return bytes;
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            return new byte[0];  
        }
    }
                    
    @Override
    protected Collection<? extends WasmCase> collectStringTestCases() {
        return Arrays.asList(testCases);
    }

    @Override
    @Test
    public void test() throws IOException {
        // This is here just to make mx aware of the test suite class.
        super.test();
    }
}
