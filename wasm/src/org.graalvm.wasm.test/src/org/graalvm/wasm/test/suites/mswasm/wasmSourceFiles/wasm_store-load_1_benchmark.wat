(module (memory 1)
  (func $store_and_load (param $j i32) (param $i i32) (result i32) 
    (i32.store8 (get_local $j) (get_local $i)) 
    (i32.load8_s (get_local $j)) 
  ) (export "ms_store_and_load" (func $store_and_load))

  (func (export "_benchmarkRun") (result i32)
    (call $store_and_load (i32.const 8) (i32.const 10))
  )

  ;; empty methods to run benchmarks
  (func (export "_benchmarkTeardownEach") )
  (func (export "_benchmarkSetupEach") )
  (func (export "_benchmarkSetupOnce") )
)