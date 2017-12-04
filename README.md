# RedOx
Support for Rust editing in Eclipse IDE.

RedOx (short for reduction–oxidation, the reaction that causes rust), is a Rust development plugin for the Eclipse IDE. In its very alpha stages, RedOx is __NOT__ recommended for use at this time.

## Prerequisites
Install Rust Language Server(RLS) and all Rust prerequisites:
```
curl https://sh.rustup.rs -sSf | sh
```

As the RLS is in an alpha stage, the newest version of rustup intermittently does not include the RLS, so we must use the beta version:
```
rustup update beta
rustup component add rls-preview --toolchain beta
rustup component add rust-analysis --toolchain beta
rustup component add rust-src --toolchain beta
```

## Concept

RedOx uses the [lsp4e](https://projects.eclipse.org/projects/technology.lsp4e) project to integrate with the [Rust Language Server](https://github.com/rust-lang-nursery/rls) and [TM4E](https://projects.eclipse.org/projects/technology.tm4e) project to provide syntax highlighting in order to provide a rich Rust editor in the Eclipse IDE.
