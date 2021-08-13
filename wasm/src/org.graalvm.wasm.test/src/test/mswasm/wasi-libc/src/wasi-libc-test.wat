(module
  (type (;0;) (func (param i32) (result handle)))
  (type (;1;) (func (param handle)))
  (type (;2;) (func (result i32)))
  (import "wasi-libc" "malloc" (func $__malloc (type 0)))
  (import "wasi-libc" "free" (func $__free (type 1)))
  (func (export "_start") (type 2) (local (;0;) handle)
    i32.const 4
    call $__malloc
    local.set 0
    (i32.store (local.get 0) (i32.const 5))
    local.get 0
    i32.load
    local.get 0
    call $__free
  )
  (memory 1)
)
