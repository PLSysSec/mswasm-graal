(module (memory 1)
  (func $load_only (param $j i32) (param $i i32) (result i32) 
    (i32.load8_s (get_local $j)) 
  ) (export "wasm_load_only" (func $load_only))

  (func (export "_main") (result i32)
    (call $load_only (i32.const 8) (i32.const 10))
  )
)