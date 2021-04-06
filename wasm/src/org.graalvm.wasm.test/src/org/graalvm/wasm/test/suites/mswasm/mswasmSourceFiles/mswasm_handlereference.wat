(module (memory 1)
 (func $ms_handlereferencetest (param $i1 i32) (result i32)
    (local $h1 handle) (local $h2 handle)
    
    (set_local $h1 (new_segment (i32.const 32)))
    (set_local $h2 (new_segment (i32.const 32)))
    (i32.segment_store (get_local $h1) (get_local $i1))
    
    (set_local $i1 (i32.add (i32.segment_load (get_local $h1)) (i32.const 1)))
    
    (get_offset (get_local $h1))
    (get_offset (get_local $h2))
    (get_local $i1)
  )

  (func (export "_main") (result i32)
    (call $ms_handlereferencetest (i32.const 1) )
  ) 
)
