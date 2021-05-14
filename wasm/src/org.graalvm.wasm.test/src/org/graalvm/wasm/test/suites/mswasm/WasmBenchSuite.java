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
import org.graalvm.wasm.utils.SystemProperties;

public class WasmBenchSuite extends WasmSuiteBase {
        private String folderPath = "./src/org.graalvm.wasm.test/src/org/graalvm/wasm/test/suites/mswasm/bench/runnable/";

        private static Properties opts = BenchOpts.opts;

        private WasmBinaryCase[] testCases = {
                        WasmCase.create("WASM_LOOP", WasmCase.expected(120000),
                                        parseWasmFile(folderPath + "wasm_loop.wasm"), null, opts),
                        WasmCase.create("WASM_SALSA20", WasmCase.expected(2050581199),
                                        parseWasmFile(folderPath + "salsa20.wasm"), null, opts),

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
