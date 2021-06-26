(module (memory 1)
  ;; generate keys and return a pointer to the public key
  (func $trusted (result i32)
    (local $pub_addr i32) (local $secret_addr i32)
    (local $public i32) (local $secret i32)

    (set_local $public (i64.const 555555555))
    (set_local $secret (i64.const 123456789))

    (set_local $secret_addr (i32.const 128))
    (i32.store (get_local $secret_addr) (get_local $secret))

    ;; public address = secret address - 4
    (set_local $pub_addr (i32.sub (get_local $secret_addr) (i32.const 4)))
    (i32.store (get_local $pub_addr) (get_local $public))

    (get_local $pub_addr) ;; return the public key's address
  )

  ;; supposedly retrieves the PUBLIC key
  ;; actually retrieves the PRIVATE key
  (func $untrusted (param $addr i32) (result i32)
    (i32.add (get_local $addr) (i32.const 4))
    (i32.load) ;; load private key
  )

  ;; returns the PRIVATE key
  (func (export "_main") (result i32)
    (call $trusted)
    (call $untrusted)
  )
)