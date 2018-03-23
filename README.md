# Corrosion

> Corrosion was formerly called RedOx, but required a name change due to naming overlap with another project ([See issue #24](https://github.com/eclipse/corrosion/issues/24))

Support for Rust editing in Eclipse IDE.

Corrosion is a Rust development plugin for the Eclipse IDE. Both [issue reports](https://github.com/eclipse/corrosion/issues) and [pull requests](https://github.com/eclipse/corrosion/pulls) are greatly appreciated.

[![Build Status](https://travis-ci.org/eclipse/corrosion.svg?branch=master)](https://travis-ci.org/eclipse/corrosion)

![Screenshot](images/editorOverview.png "Screenshot of Corrosion editor")

## Installation
Refer to our [Installation Guide](documentation/Installation.md)

## Prerequisites

The Rustup and Cargo commands are required for accessing the language server and performing most tasks. Go into the Rust preferences and either install the commands or input their paths if not automatically found.

## Contributing
Refer to our [Contributing Guide](CONTRIBUTING.md)

## Concept

Corrosion uses the [lsp4e](https://projects.eclipse.org/projects/technology.lsp4e) project to integrate with the [Rust Language Server](https://github.com/rust-lang-nursery/rls) and [TM4E](https://projects.eclipse.org/projects/technology.tm4e) project to provide syntax highlighting in order to provide a rich Rust editor in the Eclipse IDE.

The Rust and Cargo logos are owned by Mozilla and distributed under the terms of the [Creative Commons Attribution license (CC-BY)](https://creativecommons.org/licenses/by/4.0/). [More Info](https://www.rust-lang.org/en-US/legal.html)
