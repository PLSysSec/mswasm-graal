(module
  (type (;0;) (func (param i32)))
  (type (;1;) (func))
  (type (;2;) (func (param handle) (result handle)))
  (type (;3;) (func (result i32)))
  (import "wasi_snapshot_preview1" "proc_exit" (func $__wasi_proc_exit (type 0)))
  (func $__wasm_call_ctors (type 1))
  (func $_start (type 1)
    (local i32)
    call $__wasm_call_ctors
    call $__original_main
    local.set 0
    call $__prepare_for_exit
    block  ;; label = @1
      local.get 0
      i32.eqz
      br_if 0 (;@1;)
      local.get 0
      call $__wasi_proc_exit
      unreachable
    end)
  (func $aux (type 2) (param handle) (result handle)
    local.get 0
    i32.const 28
    i32.store
    local.get 0)
  (func $main (type 3) (result i32)
    (local handle i32)
    i32.const 2097152
    new_segment
    i32.const 2097152
    handle.add
    global.set 0
    global.get 0
    i32.const -32
    handle.add
    local.tee 0
    global.set 0
    local.get 0
    i32.const 8
    handle.add
    i32.const 7
    i32.store
    local.get 0
    i32.const 20
    handle.add
    call $aux
    drop
    local.get 0
    i32.const 24
    handle.add
    i32.load
    local.set 1
    local.get 0
    i32.const 32
    handle.add
    global.set 0
    local.get 1)
  (func $__original_main (type 3) (result i32)
    call $main)
  (func $_Exit (type 0) (param i32)
    local.get 0
    call $__wasi_proc_exit
    unreachable)
  (func $dummy (type 1))
  (func $__prepare_for_exit (type 1)
    call $dummy
    call $dummy)
  (func $exit (type 0) (param i32)
    call $dummy
    call $dummy
    local.get 0
    call $_Exit
    unreachable)
  (table (;0;) 1 1 funcref)
  (memory (;0;) 2)
  (global (;0;) (mut handle) (handle.null))
  (global (;1;) i32 (i32.const 1024))
  (global (;2;) i32 (i32.const 1024))
  (global (;3;) i32 (i32.const 1024))
  (global (;4;) i32 (i32.const 66560))
  (global (;5;) i32 (i32.const 0))
  (global (;6;) i32 (i32.const 1))
  (export "memory" (memory 0))
  (export "__wasm_call_ctors" (func $__wasm_call_ctors))
  (export "_start" (func $_start))
  (export "__original_main" (func $__original_main))
  (export "__prepare_for_exit" (func $__prepare_for_exit))
  (export "aux" (func $aux))
  (export "main" (func $main))
  (export "__main_void" (func $main))
  (export "_Exit" (func $_Exit))
  (export "_exit" (func $_Exit))
  (export "__funcs_on_exit" (func $dummy))
  (export "__stdio_exit" (func $dummy))
  (export "exit" (func $exit))
  (export "_fini" (func $dummy))
  (export "__dso_handle" (global 1))
  (export "__data_end" (global 2))
  (export "__global_base" (global 3))
  (export "__heap_base" (global 4))
  (export "__memory_base" (global 5))
  (export "__table_base" (global 6)))
