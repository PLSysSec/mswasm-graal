---
layout: docs
toc_group: native-image
link_title: Native Image Options
permalink: /reference-manual/native-image/Options/
---
# Native Image Options

The `native-image` builder needs to provide the classpath for all classes using the familiar option from the `java` launcher: `-cp` is followed by a list of directories or JAR files, separated by `:`.
The name of the class containing the `main` method is the last argument, or you can use `-jar` and provide a JAR file that specifies the `main` method in its manifest.

The syntax of the `native-image` command is:

- `native-image [options] class [imagename] [options]` to build an executable file for a class in the current working directory. Invoking it executes the native-compiled code of that class.

- `native-image [options] -jar jarfile [imagename] [options]` to build an image for a JAR file.

The options passed to `native-image` are evaluated left-to-right.
For more information, see [Native Image Build Configuration](BuildConfiguration.md#order-of-arguments-evaluation).

The options fall into four categories: image generation options, macro options, non-standard options, and server options.
Non-standard and server options are subject to change through a deprecation cycle.

Command-line help is available.
Run `native-image --help` to get the commands overview, and `native-image --help-extra` to print help on non-standard, macro, and server options.

### Options to Native Image Builder

Depending on the GraalVM edition, the options to the `native-image` builder may differ.
The following options are equally supported with both GraalVM Communty and Enterprise editions:

* `-cp, -classpath, --class-path <class search path of directories and zip/jar files>`: a `:` separated list of directories, JAR archives, and ZIP archives to search for class files
* `-p <module path>, --module-path <module path>`: a `:` separated list of directories; each directory is a directory of modules
* `--add-modules <module name>[,<module name>...]`: add root modules to resolve in addition to the initial module. `<module name>` can also be `ALL-DEFAULT`, `ALL-SYSTEM`, `ALL-MODULE-PATH`
* `-D<name>=<value>`: set a system property
* `-J<flag>`: pass `<flag>` directly to the JVM running the `native-image` builder
* `-O<level>`: 0 for no optimizations, or 1 for basic optimizations (default)
* `--verbose`: enable verbose output
* `--version`: print the product version and exit
* `--help`: print this help message
* `--help-extra`: print help on non-standard options
* `--allow-incomplete-classpath`: allow image building with an incomplete class path; report type resolution errors at run time when they are accessed the first time, instead of during image building
* `--auto-fallback`: build a standalone image if possible
* `--configure-reflection-metadata`: enable runtime instantiation of reflection objects for non-invoked methods
* `--enable-all-security-services`: add all security service classes to the generated image
* `--enable-http`: enable http support in the generated image
* `--enable-https`: enable https support in the generated image
* `--enable-url-protocols`: list comma-separated URL protocols to enable
* `--features`: a comma-separated list of fully qualified Feature implementation classes
* `--force-fallback`: force building of a fallback image
* `--gc=<value>`: select Native Image garbage collector implementation. Allowed options for `<value>` are: `G1` for G1 garbage collector; `epsilon` for Epsilon garbage collector; `serial` for Serial garbage collector (default). GraalVM Enterprise only
* `--initialize-at-build-time`: a comma-separated list of packages and classes (and implicitly all of their superclasses) that are initialized during image generation. An empty string designates all packages.
* `--initialize-at-run-time`: a comma-separated list of packages and classes (and implicitly all of their subclasses) that must be initialized at run time and not during image building. An empty string is currently not supported.
* `--install-exit-handlers`: provide `java.lang.Terminator` exit handlers
* `--libc`: select the `libc` native library implementation to use. Available implementations are `glibc` and `musl`.
* `--native-compiler-options`: provide a custom C compiler option used to query code compilation
* `--native-compiler-path`: provide a custom path to the C compiler used to query code compilation
and linking
* `--native-image-info`: show native toolchain information and image's build settings
* `--no-fallback`: build a standalone image or report a failure
* `--pgo`: a comma-separated list of files from which to read the data  collected for profile-guided optimization of AOT compiled code (reads from  _default.iprof_ if nothing is specified). GraalVM Enterprise only
* `--pgo-instrument`: instrument AOT compiled code to collect data for profile-guided optimization into the _default.iprof_ file. GraalVM Enterprise only
* `--report-unsupported-elements-at-runtime`: report the usage of unsupported methods and fields at runtime when they are accessed the first time, instead of an error during image building
* `--shared`: build a shared library
* `--static`: build a statically-linked executable (requires `libc` and `zlib` static libraries)
* `--target`: select the native image compilation target (in <OS>-<architecture> format). It defaults to the host's OS-architecture pair
* `--trace-class-initialization`: provide a comma-separated list of fully-qualified class names that a class initialization is traced for
* `--trace-object-instantiation`: provide a comma-separated list of fully-qualified class names that an object instantiation is traced for
* `-da`: disable assertions with specified granularity in the generated image. The  `-da[:[packagename]|:[classname]` or -`disableassertions[:[packagename]|:[classname]` variants are also supported.
* `-dsa`: disable assertions in all system classes.
* `-ea`: enable assertions with specified granularity in a generated image. The  `-ea[:[packagename]|:[classname]` or -`enableassertions[:[packagename]|:[classname]` variants are also supported.
* `-esa`: enable assertions in all system classes.
* `-g`: generate debugging information. Please be informed that debug information produced on GraalVM Community will differ from that generated on GraalVM Enterprise.

### Macro Options
* `--language:nfi`: make the Truffle Native Function Interface language available
* `--language:python`: make Python available as a language for the image
* `--language:R`: make R available as a language for the image
* `--language:regex`: make the Truffle Regular Expression engine available
* `--language:wasm`: make WebAssembly available as a language for the image
* `--language:java`: make Java available as a language for the image
* `--language:llvm`: make LLVM bitcode available as a language for the image
* `--language:js`: make JavaScript available as a language for the image
* `--language:nodejs`: make Node.js available for the image
* `--language:ruby`: make Ruby available as a language for the image
* `--tool:coverage`: add source code coverage support to a GraalVM supported language
* `--tool:insight`: add support for detailed access to a program's runtime behavior, allowing users to inspect values and types at invocation or allocation sites
* `--tool:dap`: add support to allow image to open a debugger port serving the Debug Adapter Protocol in IDEs like VS Code
* `--tool:chromeinspector`: add debugging support to a GraalVM supported language
* `--tool:insightheap`: snapshot a region of image heap during the execution
* `--tool:lsp`: add the Language Server Protocol support to later attach compatible debuggers to GraalVM in IDEs like VS Code
* `--tool:sandbox`: enables the Truffle sandbox resource limits. For more information, check the [dedicated documentation](..reference-manual/embedding/embed-languages.md/#enterprise-sandbox-resource-limits)
* `--tool:profiler`: add profiling support to a GraalVM supported language

The `--language:nodejs`, `--language:python`, `--language:ruby`, `--language:R`, `--language:wasm`, and `--language:llvm` polyglot macro options become available once the corresponding languages are added to the base GraalVM installation (see the [GraalVM Updater](../graalvm-updater.md) guide).

### Non-standard Options
* `--expert-options`: list image build options for experts
* `--expert-options-all `: list all image build options for experts (use at your own risk). Options marked with _[Extra help available]_ contain help that can be shown with `--expert-options-detail`
* `--expert-options-detail`: display all available help for a comma-separated list of option names. Pass `*` to show extra help for all options that contain it
* `--configurations-path <search path of option-configuration directories>`: a separated list of directories to be treated as option-configuration directories
* `--debug-attach[=< port >]`: attach to debugger during image building (default port is 8000)
* `--diagnostics-mode`: enable logging of image-build information to a diagnostics folder
* `--dry-run`: output the command line that would be used for building
* `--list-cpu-features`: list all possible `-H:CPUFeatures`
* `-V<key>=<value>`:  provide values for placeholders in _native-image.properties_ files
* `--add-exports`: value `<module>/<package>=<target-module>(,<target-module>)*` updates `<module>` to export `<package>` to `<target-module>`, regardless of module declaration. `<target-module>` can be `ALL-UNNAMED` to export to all unnamed modules
* `--add-opens`: value `<module>/<package>=<target-module>(,<target-module>)*` updates `<module>` to open `<package>` to `<target-module>`, regardless of module declaration
* `--add-reads`: value `<module>=<target-module>(,<target-module>)*` updates `<module>` to read `<target-module>`, regardless of module declaration. `<target-module>` can be `ALL-UNNAMED` to read all unnamed modules

Native Image options are also distinguished as hosted and runtime options. Continue reading to the [Native Image Hosted and Runtime Options](HostedvsRuntimeOptions.md) guide.
