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

import org.graalvm.wasm.test.CompWasmSuiteBase;
import java.io.InputStream;
import java.io.FileInputStream;

import java.util.Properties;
import org.graalvm.wasm.utils.SystemProperties;

public class CompMSWasmSuite extends CompWasmSuiteBase {
        private String folderPath = "./src/org.graalvm.wasm.test/src/org/graalvm/wasm/test/suites/mswasm/craig/";

        private static Properties opts = SystemProperties.createFromOptions(
                        "zero-memory = false\n" + "interpreter-iterations = 0\n" + "sync-noinline-iterations = 10\n"
                                        + "sync-inline-iterations = 10\n" + "async-iterations = 10\n");

        private WasmBinaryCase[] testCases = {
                        WasmCase.create("HACL_CHACHA20_CORE", WasmCase.expected(0),
                                        parseWasmFile(folderPath + "Hacl_Chacha20_core.wasm"), null, opts),
                        WasmCase.create("CRAIG_1", WasmCase.expected(0),
                                         parseWasmFile(folderPath + "test1.wasm"), null, opts),
                        WasmCase.create("CRAIG_2", WasmCase.expected(0),
                                         parseWasmFile(folderPath + "test2.wasm"), null, opts),
                        WasmCase.create("CRAIG_4", WasmCase.expected(0),
                                         parseWasmFile(folderPath + "test4.wasm"), null, opts)
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
        protected String includedExternalModules() {
            return super.includedExternalModules() + ",memory,wasi,wasi_snapshot_preview1";
        }

        @Override
        @Test
        public void test() throws IOException {
                // This is here just to make mx aware of the test suite class.
                super.test();
        }
}
