(module
  (type (;0;) (func (param i32) (result handle)))
  (type (;1;) (func (param handle)))
  (type (;2;) (func (result i32))
  (import "wasi-libc" "malloc" (func $__malloc (type 0)))
  (import "wasi-libc" "free" (func $__free (type 1)))
  (func (export "_start") (type 0) (local (;0;) handle)
    call $__malloc
    local.set 0
    i32.segment_store (local.get 0) (i32.const 5)
    local.get 0
    i32.segment_load
    call $__free
  )
)
