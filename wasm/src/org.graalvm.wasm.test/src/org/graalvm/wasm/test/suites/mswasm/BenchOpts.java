package org.graalvm.wasm.test.suites.mswasm;
import org.graalvm.wasm.utils.SystemProperties;
import java.util.Properties;

public class BenchOpts {
    /*
     * MS-Wasm, right now, will fail at the compilation stage and fallback to the interpreter.
     * 
     * Wasm succeeds and gets compiled.
     * To our understanding:    
     *   - sync will do compilation on one thread
     *     - inline will, well, inline all function calls, noline won't
     *   - async will use multiple threads, does not inline because async
     * 
     * We'll only use one option to minimize comilation time (only gets compiled once)
     * 
     * We think sync-inline will produce the fastest executable, at the expense
     * of compile time.
     * 
     * Pick a better iteration number since mswasm uses interpreter
     * 
     */
    public static Properties opts = SystemProperties.createFromOptions(
        "zero-memory = false\n" + "interpreter-iterations = 0\n" + "sync-noinline-iterations = 0\n"
                        + "sync-inline-iterations = 50000\n" + "async-iterations = 0\n");
}
