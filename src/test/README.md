# Note about the tests

The functionality of the library contains APIs that read and write files.
Testing these can be done reading and writing test files.
The tests are referencing these, assuming that the current working directory (CWD) is the project root.
This is true when running `mvn verify` from the command line as well as when executing the tests from the IDE.

This is only true because this is a simple, and not a multimodule project.
In multimodule projects the CWD is the module root in the IDE and the project root when starting `mvn verify`.
In multimodule projects, the test code has to find the project root directory to have path reference to the test files.
You can see examples for that in the project Jamal at https://github.com/verhas/jamal