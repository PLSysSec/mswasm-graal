(module (memory 1)
 (func $ms_handle_offset (result i32)
    (handle.offset (handle.add (new_segment (i32.const 8)) (i32.const 4)))
  )

 (func (export "_main") (result i32)
    (call $ms_add)
 )
)