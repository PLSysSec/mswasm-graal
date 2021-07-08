
x=`date +%m_%d_%Y_%H_%M_%S`
folder="/home/eric/benchmark_logs/mswasm_$x"

mkdir "$folder"

mx --dy /truffle,/compiler benchmark wasm:WASM_BENCHMARKCASES -- --jvm=server -Dwasmbench.benchmarkName=mswasm_loop -- MSWasmBenchmarkSuite > "$folder/mswasm_loop_benchmark.txt"
mx --dy /truffle,/compiler benchmark wasm:WASM_BENCHMARKCASES -- --jvm=server -Dwasmbench.benchmarkName=mswasm_loop_1M -- MSWasmBenchmarkSuite > "$folder/mswasm_loop_1M_benchmark.txt"
mx --dy /truffle,/compiler benchmark wasm:WASM_BENCHMARKCASES -- --jvm=server -Dwasmbench.benchmarkName=mswasm_salsa20 -- MSWasmBenchmarkSuite > "$folder/mswasm_salsa20_benchmark.txt"
mx --dy /truffle,/compiler benchmark wasm:WASM_BENCHMARKCASES -- --jvm=server -Dwasmbench.benchmarkName=mswasm_store-load_1 -- MSWasmBenchmarkSuite > "$folder/mswasm_store_load_1_benchmark.txt"