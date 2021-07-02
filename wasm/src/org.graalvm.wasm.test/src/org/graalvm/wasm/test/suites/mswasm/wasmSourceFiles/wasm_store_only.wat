(module (memory 1)
  (func $store_only (param $j i32) (param $i i32) 
    (i32.store8 (get_local $j) (get_local $i)) 
  ) (export "wasm_store_only" (func $store_only))

  (func (export "_main")
    (call $store_only (i32.const 8) (i32.const 10))
  )
)