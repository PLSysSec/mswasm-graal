(module (memory 1)
  ;; generate keys and return a pointer to the public key
  (func $trusted (result handle)
    (local $pub_addr handle) (local $secret_addr handle)
    (local $public i64) (local $secret i64)
    
    (set_local $public (i64.const 5555555555))
    (set_local $secret (i64.const 1234567890))

    (set_local $secret_addr (new_segment (i32.const 8)))
    (i64.segment_store (get_local $secret_addr) (get_local $secret))

    ;; public address = entirely new handle
    (set_local $pub_addr (new_segment (i32.const 8)))
    (i64.segment_store (get_local $pub_addr) (get_local $public))

    (get_local $pub_addr) ;; return the public key's address
  )

  ;; successfully retrieves the PUBLIC key
  (func $untrusted (param $addr handle) (result i64)
    (i64.segment_load (get_local $addr))
  )

  ;; correctly returns the PUBLIC key
  (func (export "_main") (result i64)
    (call $trusted)
    (call $untrusted)
  )
)