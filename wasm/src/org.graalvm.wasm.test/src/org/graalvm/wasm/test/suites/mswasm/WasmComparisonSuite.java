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

public class WasmComparisonSuite extends WasmSuiteBase {
    private final String FOLDER_PATH = "./src/org.graalvm.wasm.test/src/org/graalvm/wasm/test/suites/mswasm/wasmTests/";

    private static Properties opts = SystemProperties
            .createFromOptions("zero-memory = false\n" + "interpreter-iterations = 0\n"
                    + "sync-noinline-iterations = 10\n" + "sync-inline-iterations = 10\n" + "async-iterations = 10\n");

    private WasmBinaryCase[] testCases = {
            WasmCase.create("STORE_AND_LOAD_1", WasmCase.expected(10),
                    parseWasmFile(FOLDER_PATH + "wasm_store-load_1.wasm"), null, opts),
            WasmCase.create("ADD_1", WasmCase.expected(11), parseWasmFile(FOLDER_PATH + "wasm_add_1.wasm"), null, opts),
            WasmCase.create("ADD64_1", WasmCase.expected(0L), parseWasmFile(FOLDER_PATH + "wasm_add64_1.wasm"), null,
                    opts),
            WasmCase.create("FREESEGMENT_ADD", WasmCase.expected(24),
                    parseWasmFile(FOLDER_PATH + "wasm_freesegment_add.wasm"), null, opts),

            // new year, new tests
            WasmCase.create("WASM_LOOP", WasmCase.expected(12000),
                    parseWasmFile(FOLDER_PATH + "wasm_loop.wasm"), null, opts),
            WasmCase.create("WASM_SALSA", WasmCase.expected(2050581199),
                    parseWasmFile(FOLDER_PATH + "salsa20.wasm"), null, opts),
            WasmCase.create("WASM_VARIOUS_MATH", WasmCase.expected(-138411552),
                    parseWasmFile(FOLDER_PATH + "wasm_various_math.wasm"), null, opts), };

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
