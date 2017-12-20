# RedOx
Support for Rust editing in Eclipse IDE.

RedOx (short for reduction–oxidation, the reaction that causes rust), is a Rust development plugin for the Eclipse IDE. Both [issue reports](https://github.com/LucasBullen/redOx/issues) and [pull requests](https://github.com/LucasBullen/redOx/pulls) are greatly appreciated.

[![Build Status](https://travis-ci.org/LucasBullen/redOx.svg?branch=master)](https://travis-ci.org/LucasBullen/redOx)

![Screenshot](images/editorOverview.png "Screenshot of RedOx editor")

## Installation
Follow the [installation instructions](https://lucasbullen.github.io/redOx/site/)

## Prerequisites

The Rustup and Cargo commands are required for accessing the language server and performing most tasks. Go into the Rust preferences and either install the commands or input their paths if not automatically found.

## Concept

RedOx uses the [lsp4e](https://projects.eclipse.org/projects/technology.lsp4e) project to integrate with the [Rust Language Server](https://github.com/rust-lang-nursery/rls) and [TM4E](https://projects.eclipse.org/projects/technology.tm4e) project to provide syntax highlighting in order to provide a rich Rust editor in the Eclipse IDE.

The Rust and Cargo logos are owned by Mozilla and distributed under the terms of the [Creative Commons Attribution license (CC-BY)](https://creativecommons.org/licenses/by/4.0/). [More Info](https://www.rust-lang.org/en-US/legal.html)