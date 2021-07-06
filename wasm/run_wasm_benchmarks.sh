x=`date +%m_%d_%Y_%H_%M_%S`
folder="/home/eric/benchmark_logs/wasm_$x"

mkdir "$folder"

mx --dy /truffle,/compiler benchmark wasm:WASM_BENCHMARKCASES -- --jvm=server -Dwasmbench.benchmarkName=wasm_loop -- WasmBenchmarkSuite > "$folder/wasm_loop_benchmark.txt"
mx --dy /truffle,/compiler benchmark wasm:WASM_BENCHMARKCASES -- --jvm=server -Dwasmbench.benchmarkName=wasm_loop_1M -- WasmBenchmarkSuite > "$folder/wasm_loop_1M_benchmark.txt"
mx --dy /truffle,/compiler benchmark wasm:WASM_BENCHMARKCASES -- --jvm=server -Dwasmbench.benchmarkName=wasm_salsa20 -- WasmBenchmarkSuite > "$folder/wasm_salsa20_benchmark.txt"
mx --dy /truffle,/compiler benchmark wasm:WASM_BENCHMARKCASES -- --jvm=server -Dwasmbench.benchmarkName=wasm_store-load_1 -- WasmBenchmarkSuite > "$folder/wasm_store_load_1_benchmark.txt"
mx --dy /truffle,/compiler benchmark wasm:WASM_BENCHMARKCASES -- --jvm=server -Dwasmbench.benchmarkName=wasm_store_only -- WasmBenchmarkSuite > "$folder/wasm_load_only_benchmark.txt"
mx --dy /truffle,/compiler benchmark wasm:WASM_BENCHMARKCASES -- --jvm=server -Dwasmbench.benchmarkName=wasm_load_only -- WasmBenchmarkSuite > "$folder/wasm_store_only_benchmark.txt"