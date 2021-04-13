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

public class MSWasmSuite extends WasmSuiteBase {
        private String folderPath = "./src/org.graalvm.wasm.test/src/org/graalvm/wasm/test/suites/mswasm/mswasmTests/";

        private static Properties opts = SystemProperties.createFromOptions(
                        "zero-memory = false\n" + "interpreter-iterations = 0\n" + "sync-noinline-iterations = 10\n"
                                        + "sync-inline-iterations = 10\n" + "async-iterations = 10\n");

        private WasmBinaryCase[] testCases = {
                        WasmCase.create("STORE_AND_LOAD_1", WasmCase.expected(10),
                                        parseWasmFile(folderPath + "mswasm_store-load_1.wasm"), null, opts),
                        WasmCase.create("STORE_AND_LOAD_2", WasmCase.expected(0xfedc6543),
                                        parseWasmFile(folderPath + "mswasm_store-load_2.wasm"), null, opts),
                        WasmCase.create("ADD_1", WasmCase.expected(11), parseWasmFile(folderPath + "mswasm_add_1.wasm"),
                                        null, opts),
                        WasmCase.create("ADD_2", WasmCase.expected(0), parseWasmFile(folderPath + "mswasm_add_2.wasm"),
                                        null, opts),
                        WasmCase.create("ADD_3", WasmCase.expected(0x12345678),
                                        parseWasmFile(folderPath + "mswasm_add_3.wasm"), null, opts),
                        WasmCase.create("ADD64_1", WasmCase.expected(0L),
                                        parseWasmFile(folderPath + "mswasm_add64_1.wasm"), null, opts),
                        WasmCase.create("ADD64_2", WasmCase.expected(1234567895L),
                                        parseWasmFile(folderPath + "mswasm_add64_2.wasm"), null, opts),
                        WasmCase.create("HANDLEADDSUB_1", WasmCase.expected(9),
                                        parseWasmFile(folderPath + "mswasm_handleaddsub_1.wasm"), null, opts),
                        WasmCase.create("HANDLEADDSUB_2", WasmCase.expected(-19),
                                        parseWasmFile(folderPath + "mswasm_handleaddsub_2.wasm"), null, opts),
                        WasmCase.create("SEGMENTSLICE_0", WasmCase.expected(64),
                                        parseWasmFile(folderPath + "mswasm_segmentslice_0.wasm"), null, opts),
                        WasmCase.create("SEGMENTSLICE_1", WasmCase.expected(96),
                                        parseWasmFile(folderPath + "mswasm_segmentslice_1.wasm"), null, opts),
                        WasmCase.create("SEGMENTSLICE_2",
                                        WasmCase.expectedThrows(
                                                        "Slices of handles can't be freed",
                                                        WasmCaseData.ErrorType.Validation),
                                        parseWasmFile(folderPath + "mswasm_segmentslice_2.wasm"), null, opts),
                        WasmCase.create("HANDLELOADSTORE", WasmCase.expected(17),
                                        parseWasmFile(folderPath + "mswasm_handleloadstore.wasm"), null, opts),
                        WasmCase.create("HANDLELOADSTORE_TRAP",
                                        WasmCase.expectedThrows("Corrupted key does not reference a valid handle",
                                                        WasmCaseData.ErrorType.Validation),
                                        parseWasmFile(folderPath + "mswasm_handleloadstore_trap.wasm"), null, opts),
                        WasmCase.create("FREESEGMENT_ADD", WasmCase.expected(24),
                                        parseWasmFile(folderPath + "mswasm_freesegment_add.wasm"), null, opts),
                        WasmCase.create("FREESEGMENT_TRAP",
                                        WasmCase.expectedThrows(
                                                        "Segment memory is not allocated",
                                                        WasmCaseData.ErrorType.Validation),
                                        parseWasmFile(folderPath + "mswasm_freesegment_trap.wasm"), null, opts),
                        WasmCase.create("HANDLELOADSTOREADD_1", WasmCase.expected(-1),
                                        parseWasmFile(folderPath + "mswasm_handleloadstore_add_1.wasm"), null, opts),
                        WasmCase.create("HANDLELOADSTOREADD_2", WasmCase.expected(1040),
                                        parseWasmFile(folderPath + "mswasm_handleloadstore_add_2.wasm"), null, opts),
                        WasmCase.create("HANDLE_GET_OFFSET", WasmCase.expected(4),
                                        parseWasmFile(folderPath + "mswasm_handle_get_offset.wasm"), null, opts),
                        WasmCase.create("HANDLE_SET_OFFSET", WasmCase.expected(4),
                                        parseWasmFile(folderPath + "mswasm_handle_set_offset.wasm"), null, opts),
                        WasmCase.create("HANDLE_SET_OFFSET_TRAP",
                                        WasmCase.expectedThrows(
                                                        "Offset 16 is out of bounds",
                                                        WasmCaseData.ErrorType.Validation),
                                        parseWasmFile(folderPath + "mswasm_handle_set_offset_trap.wasm"), null, opts),
                        WasmCase.create("HANDLE_REFERENCE", WasmCase.expected(0),
                                        parseWasmFile(folderPath + "mswasm_handle_reference.wasm"), null, opts),
                        WasmCase.create("HANDLE_DUP",
                                        WasmCase.expectedThrows(
                                                "Segment memory is not allocated",
                                                WasmCaseData.ErrorType.Validation),
                                        parseWasmFile(folderPath + "mswasm_handle_dup.wasm"), null, opts),


                        // new year, new tests
                        WasmCase.create("MSWASM_LOOP", WasmCase.expected(12000),
                                        parseWasmFile(folderPath + "mswasm_loop.wasm"), null, opts),

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
