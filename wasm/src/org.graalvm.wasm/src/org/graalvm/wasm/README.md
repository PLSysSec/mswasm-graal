# Status

## Currently Working On
- Handling use-after-frees/duplicate handles/slices (how do we know whether the memory is allocated?)
- FREESEGMENT_TRAP causes a double free Java error

## TODO
- Remove generic error message for double-free

## Finished
- Handle.java reimplemented on a basic level to hold addresses to segments
- Loading and storing handles (might still need some debugging)
- Removing Handle->Segment map and instead accessing segment from handle itself





# An Overview Of What We've Done

## Their linear memory implementation:
[*memory/UnsafeWasmMemory.java*](https://github.com/aemichael/mswasm-graal/blob/mswasm/dev/wasm/src/org.graalvm.wasm/src/org/graalvm/wasm/memory/UnsafeWasmMemory.java)

Using sun.misc.Unsafe to directly allocate memory: this is a system
library that Java libraries use to directly manage memory a la malloc,
using a hack to get access to it

-   See
    [*http://mishadoff.com/blog/java-magic-part-4-sun-dot-misc-dot-unsafe/*](http://mishadoff.com/blog/java-magic-part-4-sun-dot-misc-dot-unsafe/)
-   [*https://dzone.com/articles/understanding-sunmiscunsafe*](https://dzone.com/articles/understanding-sunmiscunsafe)
-   Mild documentation:
    [*http://www.docjar.com/html/api/sun/misc/Unsafe.java.html*](http://www.docjar.com/html/api/sun/misc/Unsafe.java.html)

They use unsafe methods to grab data: getInt to get Int, etc

They only check addresses by seeing if the memory is within the page
size: validateAddress to see if it's negative or if address+offset is
greater than the pageSize.

Wasm load methods only take in a certain datatype: load_i64 can only
load i64s, etc, so this works

## Our segment implementation:

Updated 5/4/2021

New segments are allocated from the underlying Java memory (Java.Sun.Unsafe)
with each call to `new_segment`. This is the same memory used to allocate
Wasm linear memory; the Java memory manager ensures that neither linear memory
nor any existing segments are doubly allocated into a new segment.

`free_segment` calls the Java memory manager to free that segment's allocated
memory, and marks the internal represenatation of the segment as freed. The
second step is necessary so that duplicate handles and slices into that segment
are also treated as freed.

Handles access memory at (`base` + `offset`), where `base` is the absolute
address of the segment, and `offset` is a relative address within the segment.
Handles can be moved around freely within a segment with `handle.add` (increase
the relative memory offset), `handle.sub` (decrease the offset), and
`handle.set_offset` (set the offset to a specified value).

Slices are special handles produced by `slice_segment` that point into an
existing segment, with narrower bounds. A slice produced from a given segment
can have a higher base address, lower bound, or both. Slices can access only the
memory within their narrowed bounds. Memory that falls within the original
segment, but not within the sliced bounds, is inaccessible.

[*mswasm/Segment.java*](https://github.com/aemichael/mswasm-graal/blob/mswasm/dev/wasm/src/org.graalvm.wasm/src/org/graalvm/wasm/mswasm/Segment.java)
represents a segment. Used to track whether a shared segment of memory has been freed.

[*mswasm/Handle.java*](https://github.com/aemichael/mswasm-graal/blob/mswasm/dev/wasm/src/org.graalvm.wasm/src/org/graalvm/wasm/mswasm/Handle.java)
represents handles and also manages segment memory allocation, reads, and writes.

[*constants/Instructions.java*](https://github.com/aemichael/mswasm-graal/blob/mswasm/dev/wasm/src/org.graalvm.wasm/src/org/graalvm/wasm/constants/Instructions.java)
contains opcodes. All Wasm load and store opcodes have been converted into MSWasm
segment loads and stores. New MSWasm opcodes have been added.

### stores & loads

Update 2/22/2021
- static mapping table for Handle keys
- set isCorrupted to be true when we free something
- Load: loads a key corresponding to a handle, uses internal mapping to check whether handle is valid or not
    - load key at address
    - validate key: if invalid, throw a trap (TODO: do we want to return a corrupted handle instead?)
    - return valid handle
- Store:
    -  add handle to key table before storing

- We store and load by quite literally just getting it from the map, or 
  putting it in the map
- We are running a lot of checks:
- See if handle is valid by seeing if it isn't corrupted, and less than 
  or equal to the bound
- DOES NOT check allocation

### `containsKey` ([*SegmentMemory.java*](https://github.com/aemichael/mswasm-graal/blob/mswasm/dev/wasm/src/org.graalvm.wasm/src/org/graalvm/wasm/mswasm/SegmentMemory.java))

- We check allocation in segments.containsKey
- Just check if it exists there, that's it
- If it doesn't, we just increase the size (but we don't use size, oopsie)
- Use `put()` function

### `loadFromSegment` ([*SegmentMemory.java*](https://github.com/aemichael/mswasm-graal/blob/mswasm/dev/wasm/src/org.graalvm.wasm/src/org/graalvm/wasm/mswasm/SegmentMemory.java))

- Checks if handle is valid, exists in map
- Then gets the value, figures out the type, sets handle as corrupted

## Ideas for improvements:

Use the same unsafe memory to speed up store & load operations, rather
than using a HashMap to store everything.

### memory management:

-   Use unsafe to allocate a block of memory per segment, with slices &
    handle arithmetic giving alternate views into that segment.
-   Store set of allocated memory blocks
-   On free, deallocate the block of memory through the unsafecheck if
    handle is valid & check if the memory (that the handle references)
    is accessible

### stores/loads:

-   Split up store & load methods by data type so we don't have the
    overhead of checking the data type on every store/load
-   Checks: make sure handle is not corrupted & is valid (base + offset
    \<= bound), and check that the referenced block of bytes is
    currently allocated (how?)
-   Handles: convert each into a pair of longs: one containing base +
    offset, other contains the bound + isCorrupted

## [*The Haskell interpreter*](https://github.com/aemichael/mswasm-graal/blob/mswasm/dev/wasm/src/org.graalvm.wasm/src/org/graalvm/wasm/mswasm/SegmentMemory.java)

Hijacks an existing tool to prototype, mainly used now to convert from
.wat (WebAssembly Text) to .wasm (WebAssembly but actual) using our
defined opcodes. It's much easier to feed bytecode to GraalVM, we
weren't able to find an easy way to go from WasmText to bytecode.

## The opcodes we have

[*constants/Instructions.java*](https://github.com/aemichael/mswasm-graal/blob/mswasm/dev/wasm/src/org.graalvm.wasm/src/org/graalvm/wasm/constants/Instructions.java)


Instruction           |Opcode      |Type                             |Explanation
----------------------|------------|---------------------------------|-------------------------------------------------------------------------------------------------------------------------------
I32_SEGMENT_LOAD      |0x28        |\[handle\] → \[i32\]             |Pushes the i32 at the referenced segment onto the stack. Traps if the handle is invalid or the segment isn't allocated.
I64_SEGMENT_LOAD      |0x29        |\[handle\] → \[i64\]             |Pushes the i64 at the referenced segment onto the stack. Traps if the handle is invalid or the segment isn't allocated.
I32_SEGMENT_LOAD_8S   |0x2C        |\[handle\] → \[i32\]             |Pushes the signed 8-byte i32 at the referenced segment onto the stack. Traps if the handle is invalid or the segment isn't allocated.
I32_SEGMENT_LOAD_8U   |0x2D        |\[handle\] → \[i32\]             |Pushes the unsigned 8-byte i32 at the referenced segment onto the stack. Traps if the handle is invalid or the segment isn't allocated.
I32_SEGMENT_LOAD_16S  |0x2E        |\[handle\] → \[i32\]             |Pushes the signed 16-byte i32 at the referenced segment onto the stack. Traps if the handle is invalid or the segment isn't allocated.
I32_SEGMENT_LOAD_16U  |0x2F        |\[handle\] → \[i32\]             |Pushes the unsigned 16-byte i32 at the referenced segment onto the stack. Traps if the handle is invalid or the segment isn't allocated.
I64_SEGMENT_LOAD_8S   |0x30        |\[handle\] → \[i32\]             |Pushes the signed 8-byte i64 at the referenced segment onto the stack. Traps if the handle is invalid or the segment isn't allocated.
I64_SEGMENT_LOAD_8U   |0x31        |\[handle\] → \[i32\]             |Pushes the unsigned 8-byte i64 at the referenced segment onto the stack. Traps if the handle is invalid or the segment isn't allocated.
I64_SEGMENT_LOAD_16S  |0x32        |\[handle\] → \[i32\]             |Pushes the signed 16-byte i64 at the referenced segment onto the stack. Traps if the handle is invalid or the segment isn't allocated.
I64_SEGMENT_LOAD_16U  |0x33        |\[handle\] → \[i32\]             |Pushes the unsigned 16-byte i64 at the referenced segment onto the stack. Traps if the handle is invalid or the segment isn't allocated.
I64_SEGMENT_LOAD_32S  |0x34        |\[handle\] → \[i32\]             |Pushes the signed 32-byte i64 at the referenced segment onto the stack. Traps if the handle is invalid or the segment isn't allocated.
I64_SEGMENT_LOAD_32U  |0x35        |\[handle\] → \[i32\]             |Pushes the unsigned 32-byte i64 at the referenced segment onto the stack. Traps if the handle is invalid or the segment isn't allocated.
I32_SEGMENT_STORE     |0x36        |\[handle i32\] → \[\]            |Stores the provided int in the segment referenced by the handle. Traps if the handle is invalid or the segment isn't allocated.
I64_SEGMENT_STORE     |0x37        |\[handle i64\] → \[\]            |Stores the provided long in the segment referenced by the handle. Traps if the handle is invalid or the segment isn't allocated.
I32_SEGMENT_STORE_8   |0x3A        |\[handle\] → \[i32\]             |Stores the provided 8-byte int in the segment referenced by the handle. Traps if the handle is invalid or the segment isn't allocated.
I32_SEGMENT_STORE_16  |0x3B        |\[handle\] → \[i32\]             |Stores the provided 16-byte int in the segment referenced by the handle. Traps if the handle is invalid or the segment isn't allocated.
I64_SEGMENT_STORE_8   |0x3C        |\[handle\] → \[i32\]             |Stores the provided 8-byte long in the segment referenced by the handle. Traps if the handle is invalid or the segment isn't allocated.
I64_SEGMENT_STORE_16  |0x3D        |\[handle\] → \[i32\]             |Stores the provided 16-byte long in the segment referenced by the handle. Traps if the handle is invalid or the segment isn't allocated.
I64_SEGMENT_STORE_32  |0x3E        |\[handle\] → \[i32\]             |Stores the provided 32-byte long in the segment referenced by the handle. Traps if the handle is invalid or the segment isn't allocated.
NEW_SEGMENT           |0xF4        |\[i32\] → \[handle\]             |Allocates segment of provided byte size, returns handle that points to it.
FREE_SEGMENT          |0xF5        |\[handle\] → \[\]                |Deallocates segment pointed to by handle.
SEGMENT_SLICE         |0xF6        |\[handle i32 i32\] → \[handle\]  |Takes handle to be sliced, offset of new base from old base, offset of new bound from old base.
HANDLE_SEGMENT_LOAD   |0xF7        |\[handle\] → \[handle\]          |Pushes the handle at the referenced segment onto the stack. Traps if the handle is invalid or the segment isn't allocated.
HANDLE_SEGMENT_STORE  |0xF8        |\[handle handle\] → \[\]         |Stores the provided handle in the segment referenced by the handle. Traps if the handle is invalid or the segment isn't allocated.
HANDLE_ADD            |0xF9        |\[i32 handle\] → \[handle\]      |Adds the provided i32 to the handle's offset and returns the result.
HANDLE_SUB            |0xFA        |\[i32 handle\] → \[handle\]      |Subtracts the provided i32 from the handle's offset and returns the result.
HANDLE_GET_OFFSET     |0xFB        |\[handle\] → \[i32\]             |Returns the provided handle's offset.
HANDLE_SET_OFFSET     |0xFC        |\[handle i32\] → \[handle\]      |Sets the handle's offset to the provided i32 and returns the resulting handle.
---------------------- ------------ --------------------------------- ------------------------------------------------------------------------------------------------------------------------------------
