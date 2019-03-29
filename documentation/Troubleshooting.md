## Troubleshooting guide

This is a collection of troubleshooting tips and workarounds which have often popped up in discussion on the https://github.com/eclipse/corrosion/issues GitHub tracker.

Before posting a bug report or asking for help it is recommended to collect as much information as you can about the problem you're having. This is so there are higher chances that 1) you ask the right people/project and 2) the people above will know where to look for the bug/issue without digging too much. This will generally result in much better solutions and shorter turnarounds.

Reading through this document should provide you with a very high-altitude view on where the problem may be rooted and how to best look for, and feed back, debugging information.

However, pinpointing a malfunctioning component is often not trivial, in which case searching the main https://github.com/eclipse/corrosion/issues, or posting to it, is usually a good first step, but will likely take longer to be addressed.

----

### Components and where to find them

The Eclipse IDE for Rust Developer functionality is provided by cooperation of a few main components. In the general case, the installation process will spare you the pain of finding and putting these together, but it's often necessary at least to know about them when tracking the root cause of an issue.

#### The **Eclipse IDE** platform

Latest releases available at https://www.eclipse.org/downloads/packages/. Multiple flavours available for all your programming needs. In general, Rust plugin development targets recent builds, so if you're having problem of sorts, make sure you have a decently up-to-date platform. Eclipse can be configured to update itself via repositories. In general, the automatic update works as expected. If the IDE fails after an update, for any reason, there are two workarounds to try:

- start Eclipse from a new workspace
- start Eclipse from a new install.

Multiple workspaces and versions of Eclipse can be easily installed side by side so you don't have to destroy your carefully crafted setup just for the sake of testing a new version.

#### The **Eclipse Corrosion** plugin

Support for Rust is provided by the Corrosion plugin. There are two ways to get this quickly up-and-running:

1. You can get the all-in-one Eclipse IDE for Rust developer packages from here. This is a self-contained solution which quickly enables Rust development out-of-the box at https://www.eclipse.org/downloads/packages/release/2019-03/r/eclipse-ide-rust-developers-includes-incubating-components
2. Install any flavour of Eclipse from the link above and then add the Corrosion plugin with **Help** -> **Install new software** -> **Add**, then choose one of the repositories:
    - Corrosion Releases - http://download.eclipse.org/corrosion/releases/
    - Corrosion Snapshots - http://download.eclipse.org/corrosion/snapshots/
 
The Corrosion Releases channel contains older, more stable code while the Snapshots one is more up-to-date with bug fixes but also more likely to contain unfinished features and regression bugs.

For support, contributions and troubleshooting, here's a set of reference URLs

- GitHub project: https://github.com/eclipse/corrosion
- Issue tracker: https://github.com/eclipse/corrosion/issues
- Source code Git repository for cloning: https://github.com/eclipse/corrosion.git

#### The **TextMate plugin** for Eclipse

The TextMate project offers wide support for general language-aware text editing. Rust support is provided as one of the options via a pre-built configuration.

- Original project at Eclipse: https://projects.eclipse.org/projects/technology.tm4e
- GitHub project: https://github.com/eclipse/tm4e
- Issue tracker: https://github.com/eclipse/tm4e/issues
- Source code Git repository for cloning: https://github.com/eclipse/tm4e.git

It is sometimes necessary, albeit very rarely, to get the latest version of TextMate from the snapshot project repository:

- TM4E Snapshots: http://download.eclipse.org/tm4e/snapshots/

#### The **Rust Language Server** (RLS)

RLS runs in the background as a separate process and provides most of the IDE functionality, such as autocompletion, symbol search and refactoring. It is distributed as a Rust toolchain component rather than a part of Corrosion and it's a pre-requisite. For the impatient:

```
rustup update
rustup component add rls-preview rust-analysis rust-src
```
For those who don't mind to skim throught detail: GitHub project: https://github.com/rust-lang/rls

As the RLS is started as a child process of Eclipse, all the OS shenanigans of environment variable and working directories inheritance do apply.

##### Note on `rustup` vs Distro packages

The recommended way to install `rls` is to use `rustup`. Some users find the `| sh` method of installing `rustup` questionable, which is sometimes understandable. As an alternative, some Linux distribution provide a `rust stable` package which is usually not terribly up to date. While this is a good option in many cases, it does not play well with Corrosion, which, being still in active development, often requires very up-to-date versions of `rust` and `rls` as they may well contain critical bug fixes.

#### The **Language Server Protocol for Eclipse** (LSP4E)

This component allows Eclipse to communicate to all the Language servers, including the RLS, via Language Server protocol.

- LSP4E Snapshots - http://download.eclipse.org/lsp4e/snapshots/

----

### Support communication channels

Discussions and support are mainly provided via the GitHub issue tracker at https://github.com/eclipse/corrosion/issues. 

There is also a relatively quiet Gitter channel for real-time discussions https://gitter.im/eclipse_rust_development/Corrosion

----
### Configuring Rust and RLS

At the time of writing, RLS relies mostly on the host application (ie Eclipse) for configuration. Configuration options are kind of shuffled around in multiple **Preference tabs**. Before calling a feature "missing" you may want to look at the following tab settings

- **Rust**
- **Language Servers**
- **General** -> **Editors**
- **TextMate** -> **Grammar**
- **TextMate** -> **Language Configuration**
- **TextMate** -> **Theme**

As Corrosion uses TextMate, the **Editors** section for **Colors and Fonts** has no effect. Customization is limited to choosing one of the **TextMate** -> **Theme** themes (which fortunately provide a decent default for both a light and a dark setup).

#### rls.conf

Functionality to read `.rls.conf` from the RLS directly has been long removed. Eclipse provides an alternative for this configuration file. The `$HOME/.cargo/rls.conf` file contains the startup settings for the `rls` and it is optional. In this file you should be able to specify any of the valid RLS settings, as listed in https://github.com/rust-lang/rls#configuration.

If this file exists, the plugin attempts to load and parse it as as a Json object, then forwards its whole content to the initialize RLS method as the payload of the `initializationOptions` field. If you don't have one, Corrosion kindly let you know that *RLS settings for Corrosion path not found at $HOME/.cargo/rls.conf* and make up for it by guessing a sensible default, literally: 

```
{
	"settings": {
		"rust": {
			"clippy_preference": "on"
		}
	}
}
```

The location of this file on the filesystem can be configured in **Preferences** -> **Rust** -> **Rls config**.

More details at https://github.com/eclipse/corrosion/pull/183

#### rustfmt caveat 

Source code formatting is provided by `rustfmt` via RLS. This means that the vast majority of formatting rules applied are the ones that would be applied by `cargo fmt` from the root of the project, including user tweaks via `.rustfmt.toml`.

There is one **exception** of how **tab indents** are handled. The following settings will **override* the `rustfmt`**defaults**:

**General** -> **Text editors** -> **Insert spaces for tabs** 

By default, the setting above will be **unticked** so `Ctrl+Shift+F` will use **tabs** for indent, while `cargo fmt` will use **4 spaces**.

In order to restore consistency between the two, there are two options:

##### indent with tabs

To make sure both Corrosion and `cargo fmt` use tabs, **untick** the setting above and add the following settings to `.rustfmt.toml` in the root of your Rust project:

```
hard_tabs=true
```

##### indent with spaces

**tick** the setting above and make sure the **Displayed tab width** in the same form is set to **4**

----

## Configuring logging

Logging is a powerful diagnostic tools... If you know where the logs are, that is. The various Corrosion components and dependencies may scatter logging information across multiple destinations, so, depending on where your error originates, the related messages may appear in different log files, or in no logs at all if not configured appropriately.

#### Enable RLS logging

Language Server log to file can be enabled for the the Rust Language server here:

![image](https://user-images.githubusercontent.com/889291/49970871-9b0c9a00-ff24-11e8-92c8-16886f4b7b08.png)

Once the logging is active, and you have restarted your IDE, Eclipse will output some RLS logging here:

`path/to/your/eclipse_rust_workspace/languageServers-log/org.eclipse.corrosion.rls.log`

It is possible to redirect logs to the Eclipse **console** with the settings above, but it's been reported that the process may cause Eclipse to hang randomly, so logging to files is recommended.

#### Even more logging

Error messages that are enabled via the environment variable `RUST_LOG` are written to std-err.

In LSP4E std-err outputs are written to the language server log. This mixes both regular LSP JSON messages with std-err outputs. For me personally, this is not a big issue, if you care for such separation, please open an issue on the LSP4E project.

As the `RUST_LOG` can't be (yet) set from inside the Rust configuration dialog in the IDE, it has to be set externally, before starting Eclipse IDE.

##### Env var examples

Linux:
- open a terminal window
- type:
```
RUST_LOG=rls=debug /path/to/my/eclipse&
```

Windows:
- add `RUST_LOG` environment variable from the Environment Variables system dialog, with value `rls=debug`
- restart Eclipse

### Other source of logs

Some useful diagnostic messages are sent to the Eclipse `Error log` view:

![image](https://user-images.githubusercontent.com/889291/49970820-6c8ebf00-ff24-11e8-8bc2-72ab4c92ece4.png)

### JVM stack trace

If Eclipse gets stuck and become unresponsive, whoever is investigating your report will need to know what the Java VM machine running Eclipse was doing at the time of the blockage.

If this happens, it is very handy if a `jstack` dump is attached to the bug/problem report. On Linux you can attach a stack dump

1. finding out the PID of the JVM running Eclipse, typically using `ps -ef | grep java` or `pgrep java`
2. generating a stack dump of the PID listed by the command above by running `jstack <PID> > java_stack_trace.txt`
3. uploading the `java_stack_trace.txt` to the bug report

----

## Common issues

#### Multiple duplicate highlight icons in perspective toolbar

Occasionally, the toolbar spawns an extra highlight icon. After some time you can end up with a crowded toolbar, like this:  

![crowded_toolbar](https://user-images.githubusercontent.com/889291/48922009-88f68900-ee9b-11e8-9b10-93764e254ab1.png)

This is a long known issue with the eclipse platform, reported a https://bugs.eclipse.org/bugs/show_bug.cgi?id=534325

GitHub Corrosion Reference: https://github.com/eclipse/corrosion/issues/120

##### Workaround

1. Close the Eclipse IDE
2. open `path/to/workspace_rust/.metadata/.plugins/org.eclipse.e4.workbench/workbench.xmi` in a text editor
3. Locate the section below and locate the duplicate `menu:HandledToolItem`
```xml
<children xsi:type="menu:ToolBar" xmi:id="_CsPCkrN_EeicRcvSpkqvKw" elementId="org.eclipse.ui.edit.text.actionSet.presentation">
        <tags>Draggable</tags>
        <children xsi:type="menu:HandledToolItem" xmi:id="_CsPCk7N_EeicRcvSpkqvKw" elementId="org.eclipse.ui.genericeditor.togglehighlight" visible="false" iconURI="platform:/plugin/org.eclipse.ui.genericeditor/icons/full/etool16/mark_occurrences.png" type="Check" command="_CsXlFrN_EeicRcvSpkqvKw">
          <persistedState key="IIdentifier" value="org.eclipse.ui.genericeditor/org.eclipse.ui.genericeditor.togglehighlight"/>
          <visibleWhen xsi:type="ui:CoreExpression" xmi:id="_CsPClLN_EeicRcvSpkqvKw" coreExpressionId="programmatic.value"/>
        </children>
        <children xsi:type="menu:HandledToolItem" xmi:id="_CsPClbN_EeicRcvSpkqvKw" elementId="org.eclipse.ui.genericeditor.togglehighlight" iconURI="platform:/plugin/org.eclipse.ui.genericeditor/icons/full/etool16/mark_occurrences.png" type="Check" command="_CsXlFrN_EeicRcvSpkqvKw">
          <persistedState key="IIdentifier" value="org.eclipse.ui.genericeditor/org.eclipse.ui.genericeditor.togglehighlight"/>
          <visibleWhen xsi:type="ui:CoreExpression" xmi:id="_CsPClrN_EeicRcvSpkqvKw" coreExpressionId="programmatic.value"/>
        </children>
[...snip...]
```
4. Manually delete `<children xsi:type="menu:HandledToolItem" .../?` elements in excess, leaving only one copy
5. Save and exit
6. Start Eclipse Ide

#### Cannot place breakpoint in library code/Cannot browse/Ctrl+click/go-to-definition in library code 

In order for a Rust breakpoint to be placed in a source file, the source file must be part of the workspace, and that includes all the files in the `.cargo` cache.

Corrosion should automatically import the `.cargo` cache in the workspace as a project if it doesn't find it.

Should this process fail, some useful functionality will not work.

##### Workaround

Manually import the `.cargo` folder as a project in your Rust IDE workspace by:

1. Select **File** -> **Open projects from File System...** from the main menu
2. Click on **Directory...*** and select the `.cargo` folder from your home directory (or wherever you have placed your `.cargo`)
3. **Import source** `/your/home/path/.cargo` should be selected
4. Click on **Finish**

#### Error compiling dependent crate (or RLS is being nasty)

Sometimes RLS just stops working with not much of a diagnosis:

 https://github.com/eclipse/corrosion/issues/162

This may be caused by a number of reasons, possibly a misconfiguration, or a missing path, or a regression in either RLS or the IDE.

https://github.com/rust-lang-nursery/rls/issues/1078
https://github.com/eclipse/corrosion/issues/141

You can try to run 

`rls --cli`

from the root of your crate and at least see if there is a workaround for that specific crate. 

##### Workarounds

- It has been reported that, in some cases, closing the IDE and doing a full `cargo clean` of the affected project can fix the problem.

----

## Old common issues

#### Missing field 'codeActionKind'.

> org.eclipse.lsp4j.jsonrpc.ResponseErrorException: missing field 'codeActionKind'.

This was caused by a bug in the Language Server Protocol handler in Eclipse, fixed in:

https://bugs.eclipse.org/bugs/show_bug.cgi?id=541851
https://git.eclipse.org/c/lsp4e/lsp4e.git/commit/?id=6cc786f0d9bc1c12b818f7677796e2b4efdefbef
https://github.com/rust-lang/rls/issues/1161

##### Fix

Installing/upgrading to 2018-09 or newer version of Eclipse IDE for Rust Developers, or manually updating from the Snapshot update sites, will resolve it.

#### Annoying autoclosing

The implementation of closing bracket or quote sometimes inserts more than desired when typing quickly. This was addressed in https://github.com/eclipse/tm4e/issues/192.

##### Workaround

In the meantime, if you find autoclosing too annoying, you can disable it altogether:

1. **Preferences** -> **TextMate** -> **Language Configurations** -> **org.eclipse.corrosion.rust** -> **Auto Closing Pairs**
2. (disable) Enable auto closing brackets` as a workaround.

----

## Living on the bleeding edge

When everything else fails, there is still the last resort of starting over and upgrading to the latest version of *everything*

1. make sure you have a recent/working toolchain with RLS (`nightly-2018-12-13` or newer)
2. download and install Eclipse IDE For Rust Developer from https://www.eclipse.org/downloads/packages/release/2019-03/r/eclipse-ide-rust-developers-includes-incubating-components
3. start the installed Eclipse.

1. **Help** -> **Install new software** .Do not press `Next >`. Instead, add the following three sites via `Add...` or `Manage...`
  * Corrosion Snapshots - http://download.eclipse.org/corrosion/snapshots/
  * TM4E Snapshots - http://download.eclipse.org/tm4e/snapshots/
  * LSP4E Snapshots - http://download.eclipse.org/lsp4e/snapshots/
1. Close the "Install" window
2. **Help** -> **Check for updates**
3. accept the proposed selection (in my case Corrosion wanted to upgrade)
4. press **Next**/**Finish** to the end
5. restart Eclipse when prompted
