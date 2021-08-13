(module
  (type (;0;) (func (param i32) (result handle)))
  (type (;1;) (func (param handle)))
  (func (export "malloc") (type 0)
    local.get 0
    new_segment
  )
  (func (export "free") (type 1)
    local.get 0
    free_segment
  )
)