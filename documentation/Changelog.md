# Corrosion Changelog

This log contains the most visible changes to the Corrosion project over its version history.

## [Unreleased]

### Added

- Added support for the `Run test` CodeLens, allowing execution of single test cases directly from within the code editor.
- Added support for "surround with" style snippets for wrapping a block of code.
- Added an option to the `Rust > Debug` preference page to define a default GDB for Rust Debug launch configurations. The default value of this option is `rust-gdb`, which is the GDB wrapper script shipped with the Rust toolchain for Linux and MacOS.

### Changed

- The Rust Debug launch configurations now shows an error if GDB cannot be launched properly.
- Installing Rustup via the Rust preference page on Linux and MacOS will now install the RLS by default.
- Updated grammar definition for syntax highlighting to reflect changes in the Rust language.

## [0.4.2] - 2019-09-06

### Added

- Added an editor icon
- Added hyperlinks in terminal for errors to navigate to source files

### Changed

- Run configurations Rust applications now take the environment variable definitions into account

## [0.4.1] - 2019-04-09

### Changed

- Updated dependencies

## [0.4.0] - 2019-03-13

### Changed

- Added Run toolbar to Resource perspective
- Removed dedicated Rust perspective, since it had no additional value
- Associating `Cargo.toml` files with the RLS
- Fix the Rust launch configuration to take all attributes into account
- Fixed handling of Rust toolchain location if path contains spaces

## [0.3.0] - 2018-12-04

Note: this entry includes changes that likely shipped with version 0.2.0.

### Changed

- Syntax highlighting now works for all `*.toml` files, instead just `Cargo.toml`.
- Auto-importing the user's `~\.cargo` directory as a project into the workspace, fixing debugging issues.
- Better resolving for program file location in Cargo Run and Cargo Test launch configurations.
- Updated grammar definition for syntax highlighting
- Fixed default lookup locations for Rust toolchain
- Using `rust-gdb` as the default debugger for Rust debug launch config


## 0.2.0 - ?

Unfortunately, there are no known entries for this release.

## [0.1.0] - 2018-06-04

### Added

- Cargo Test launch configuration
- Rust preference page for text editor options
- Import wizard for existing Cargo projects
- Grammars for Rust and TOML syntax highlighting


[Unreleased]: https://github.com/eclipse/corrosion/compare/0.4.2...HEAD
[0.4.2]: https://github.com/eclipse/corrosion/compare/0.4.1...0.4.2
[0.4.1]: https://github.com/eclipse/corrosion/compare/0.4.0...0.4.1
[0.4.0]: https://github.com/eclipse/corrosion/compare/0.3.0...0.4.0
[0.3.0]: https://github.com/eclipse/corrosion/compare/0.1.0...0.3.0
[0.1.0]: https://github.com/eclipse/corrosion/compare/09f4fa5d...0.1.0