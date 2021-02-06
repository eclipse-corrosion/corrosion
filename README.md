<img alt="logo" src="./images/corrosionLogo.svg" width="200px"/>

# Eclipse Corrosion
### Rust edition and debug in Eclipse IDE

Corrosion is a Rust development plugin for the Eclipse IDE, providing a rich edition experience through integration with the Rust Language Server and Cargo. Available for download as a complete Eclipse package (See above), full Rust development support is just one click away.


## Download/Install

### Corrosion

Download a fully-fledged Eclipse IDE for Rust development from [Corrosion download area](https://download.eclipse.org/corrosion/releases/latest/products).

*OR*

From a working Eclipse IDE, find [Eclipse Corrosion](https://marketplace.eclipse.org/content/eclipse-corrosion) on the Eclipse Marketplace ([how to install from marketplace](https://marketplace.eclipse.org/marketplace-client-intro?mpc_install=3835145))

*OR*

From a working Eclipse IDE, install from p2 repository [http://download.eclipse.org/releases/latest/](http://download.eclipse.org/releases/latest/) (or [http://download.eclipse.org/corrosion/snapshots/](http://download.eclipse.org/corrosion/snapshots/) for development builds).

### Rust Toolchain

To install the Rust toolchain, consider visiting <https://rustup.rs/> and install Rustup.  
On Linux and MacOS, the toolchain can also be installed via Corrosion in the `Rust` preference page (`Window > Preferences`).

When installing Rustup yourself, call the following command before using Corrosion:

```bash
rustup component add rls
```

## Features

Corrosion brings together multiple different sources of features to make an enjoyable developing environment. Here are just a few of the most common features. Download today to discover all Corrosion has to offer.

### Rust Language Server
The RLS supplies Corrosion with the majority of the edition abilities. More information can be found at the [RLS GitHub Repository](https://github.com/rust-lang-nursery/rls).
 - Completion Assist
 - Documentation Hover
 - Diagnostics
 - Formatting

![RLS features](images/rls-features.gif)

### Debug
Using the rust-gdb, Corrosion enables users to debug their Rust programs with intelligent supports.
 - Variables View
 - GDB Console
 - Breakpoints
 - Expression Execution

![Debug features](images/debug-features.gif)

### Cargo Test monitoring

<img alt="logo" src="./images/testReports.png" width="600px"/>

See also [▶️video demo](https://www.screencast.com/t/1sgBo0ENGc).

### Cargo and Rustup Integration
Corrosion is built for all Rust developers, newcomers to experts, bringing a GUI to common Cargo features.
 - New Project Wizard
 - Toolchain Management
 - TOML file editing support
 - Export Crate Wizard
 - Rust Language Server Preferences

![Cargo and Rustup features](images/cargo-features.gif)


### in the powerful in versatile Eclipse IDE
Corrosion fits right into the standard Eclipse IDE workflows that boost productivity:
 - Project Outline
 - Dark Theme
 - Problems View
 - Git integration (Only available in Corrosion Package)
 - Embedded Terminal (Only available in Corrosion Package)
 - ...all Eclipse Marketplace extensions you like, including support for any other language!

![Eclipse features](images/eclipse-features.gif)

## Contributing
Corrosion is always open to new features and pull requests. If you have a feature or bug you wish to work on, [file an issue](https://github.com/eclipse/corrosion/issues) and other Corrosion developers will be able to help you get started.

Refer to our [Contributing Guide](CONTRIBUTING.md) for more instructions.

### Project Overview
For the **edition**, Corrosion uses the [lsp4e](https://projects.eclipse.org/projects/technology.lsp4e) project to integrate with the [Rust Language Server](https://github.com/rust-lang-nursery/rls) and [TM4E](https://projects.eclipse.org/projects/technology.tm4e) project to provide syntax highlighting in order to provide a rich Rust editor in the Eclipse IDE.

`rustup` is used to easily provisin the various necessary Rust tools.

**Initialization, import and export** of projects and execution are provided by integration with `cargo` command.

**Debugging** is provided by integration with `rust-gdb` and Eclipse CDT GDB support.

 > Corrosion was formerly called RedOx, but required a name change due to naming overlap with another project ([See issue #24](https://github.com/eclipse/corrosion/issues/24))

### License

Corrosion is an open-source project licensed under [The Eclipse Public License - v 2.0](https://www.eclipse.org/legal/epl-2.0/)

The Rust and Cargo logos are owned by Mozilla and distributed under the terms of the [Creative Commons Attribution license (CC-BY)](https://creativecommons.org/licenses/by/4.0/) ([More Info](https://www.rust-lang.org/en-US/legal.html)).

[More Licensing Information](NOTICE.md)
