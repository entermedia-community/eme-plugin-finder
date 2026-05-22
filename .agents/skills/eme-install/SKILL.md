---
name: eme-install
description: >
  Install and set up the EME (EMEdia) Java framework from scratch on any Unix/Linux/macOS machine.
  Use this skill whenever the user mentions "EME", "eme-lib", "EMEdia framework", wants to install
  or set up EME, run `eme init`, scaffold an EME project, or get EME working with VS Code.
  Also trigger when the user hits errors during EME installation, cloning the eme-lib repo,
  making eme.sh executable, symlinking EME, or running `eme start`. Covers macOS-specific
  prerequisites: Homebrew, latest Bash, SDKMAN, Java 26-tem, ffmpeg, imagemagick, ghostscript.
  Always use this skill for any EME setup, project creation, or first-run debugging task.
---

# EME Framework Installation Guide

EME (EMEdia) is a Java-based framework. This skill walks through the full installation:
check prerequisites → install Java → clone repo → configure binaries → init/start a project.

On **macOS**, run Steps 1a–1e before proceeding to Step 2.
On **Linux**, skip to Step 1f (Java check).

---

## Step 1a (macOS only) — Check & Install Homebrew

```bash
which brew
```

- Found → ✅ proceed to Step 1b
- Not found → install Homebrew:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

After installing, follow any printed instructions to add Homebrew to your PATH (the installer will show the exact commands for your machine — typically adding to `~/.zprofile`).

---

## Step 1b (macOS only) — Install & Configure Latest Bash

macOS ships with an ancient Bash (3.x). Install the latest via Homebrew:

```bash
brew install bash
```

Then add the new Bash to the list of allowed shells and set it as default:

```bash
# Add Homebrew bash to allowed shells
echo "$(brew --prefix)/bin/bash" | sudo tee -a /etc/shells

# Set as default shell
chsh -s "$(brew --prefix)/bin/bash"
```

### Add Homebrew bash to PATH in shell config files

Ensure both `~/.zshrc` and `~/.bash_profile` include the Homebrew prefix on `$PATH`:

```bash
# Run this once — writes to both files
for f in ~/.zshrc ~/.bash_profile; do
  grep -qxF 'export PATH="$(brew --prefix)/bin:$PATH"' "$f" 2>/dev/null || \
    echo 'export PATH="$(brew --prefix)/bin:$PATH"' >> "$f"
done
```

Reload your shell config:

```bash
source ~/.zshrc   # if using zsh (default on modern macOS)
# or
source ~/.bash_profile   # if using bash
```

Verify:

```bash
bash --version   # should show 5.x or higher
```

---

## Step 1c (macOS only) — Install SDKMAN

SDKMAN manages Java versions and is the recommended way to install Java on macOS for EME.

```bash
curl -s "https://get.sdkman.io" | bash
```

Then load SDKMAN into your current session:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

Verify:

```bash
sdk version
```

> **Note:** SDKMAN adds itself to `~/.zshrc` and `~/.bash_profile` automatically. If `sdk` is not found after a new terminal session, check that those files contain the SDKMAN init line.

---

## Step 1d (macOS only) — Install Java 26-tem via SDKMAN

```bash
sdk install java 26-tem
```

SDKMAN will set this as the default Java. Verify:

```bash
java -version   # should show 26-tem (Temurin)
```

---

## Step 1e (macOS only) — Install Media Dependencies

EME requires **ffmpeg**, **imagemagick**, and **ghostscript** for media processing:

```bash
brew install ffmpeg imagemagick ghostscript
```

Verify each:

```bash
ffmpeg -version | head -1
convert --version | head -1    # imagemagick
gs --version
```

All three should print version info without errors before continuing.

---

## Step 1f — Check Java Version (Linux)

_macOS users: Java is already installed via SDKMAN in Step 1d — skip this step._

EME requires **Java 20 or higher**. Check what's installed:

```bash
java -version
```

- `openjdk 20` or higher → ✅ proceed
- `openjdk 17`, `11`, `8`, or missing → ❌ must upgrade

```bash
# Ubuntu/Debian
sudo apt update && sudo apt install -y openjdk-21-jdk

# Fedora/RHEL
sudo dnf install java-21-openjdk

# Manual: https://adoptium.net/
```

After installing, verify again with `java -version` before continuing.

---

## Step 1g — Install Media Dependencies (Linux)

_macOS users: already done in Step 1e — skip this step._

```bash
# Ubuntu/Debian
sudo apt update && sudo apt install -y ffmpeg imagemagick ghostscript

# Fedora/RHEL
sudo dnf install ffmpeg imagemagick ghostscript
```

Verify each:

```bash
ffmpeg -version | head -1
convert --version | head -1    # imagemagick
gs --version
```

Re recommend downloading jdk-22.0.1/ or newer. Java can be configured using SDKMAN curl -s "https://get.sdkman.io" | bash to install Java and manage versions. For Linux, you can use your package manager or SDKMAN as well. After installing Java, ensure that the `java` command points to the correct version (22 or higher) and that the JAVA_HOME environment variable is set correctly.


---

## Step 2 — Clone the Repository

Clone `eme-lib` to `/usr/local/lib/eme-lib` (canonical location), or to `~/eme-lib` if the user lacks sudo:

```bash
# Preferred (system-wide, requires sudo)
sudo git clone https://github.com/entermedia-community/eme-lib /usr/local/lib/eme-lib

# Alternative (user-local, no sudo needed)
git clone https://github.com/entermedia-community/eme-lib ~/eme-lib
```

If cloning to `~/eme-lib`, adjust symlink paths in Step 3 accordingly.

> If `git` is not installed: `sudo apt install git` / `brew install git`

---

## Step 3 — Make `eme.sh` Executable and Symlink

### Make executable

```bash
sudo chmod +x /usr/local/lib/eme-lib/resources/bin/eme.sh
```

(If using `~/eme-lib`: `chmod +x ~/eme-lib/resources/bin/eme.sh`)

### Symlink eme-lib (if cloned elsewhere)

If the user cloned to a path other than `/usr/local/lib/eme-lib`, create the canonical symlink:

```bash
sudo ln -s ~/eme-lib /usr/local/lib/eme-lib
```

### Symlink the `eme` command

```bash
sudo ln -sf /usr/local/lib/eme-lib/resources/bin/eme.sh /usr/local/bin/eme
```

### Verify

```bash
which eme       # should print /usr/local/bin/eme
eme --version   # or eme --help — confirms it runs
```

If `which eme` fails, `/usr/local/bin` may not be on `$PATH`. Fix:

```bash
echo 'export PATH="/usr/local/bin:$PATH"' >> ~/.zshrc   # or ~/.bashrc
source ~/.zshrc
```

---

## Step 4 — Initialize a Project

```bash
eme init path/to/project
```

Replace `path/to/project` with the desired project directory (e.g., `~/projects/my-eme-app`).

This scaffolds the project structure inside that path.

---

## Step 5 — Run the Project

### Option A — VS Code (if installed)

The `eme init` command will open the project as a VS Code workspace automatically if VS Code is detected.

- Press **F5** inside VS Code to start debugging.
  **Check if VS Code is installed:**

```bash
which code    # prints path if installed
```

If `code` is not on PATH but VS Code is installed, the user may need to run
**"Shell Command: Install 'code' command in PATH"** from the VS Code Command Palette (`Cmd+Shift+P` / `Ctrl+Shift+P`).

### Option B — Terminal (no VS Code)

```bash
eme start path/to/project
```

---

## Troubleshooting

| Symptom                                  | Fix                                                                                          |
| ---------------------------------------- | -------------------------------------------------------------------------------------------- |
| `eme: command not found`                 | Verify `/usr/local/bin` is in `$PATH`; re-source shell config                                |
| `Permission denied` on eme.sh            | Run `sudo chmod +x /usr/local/lib/eme-lib/resources/bin/eme.sh`                              |
| Symlink already exists error             | Use `ln -sf` (force) instead of `ln -s`                                                      |
| Git clone fails (SSL/auth)               | Try `git clone http://` variant, or check network/proxy                                      |
| Java version wrong after install         | Run `sudo update-alternatives --config java` (Linux) or `sdk use java 26-tem` (macOS/SDKMAN) |
| `eme init` runs but VS Code doesn't open | Ensure `code` CLI is on PATH; see Step 5 Option A                                            |
| `sdk: command not found` after install   | Run `source "$HOME/.sdkman/bin/sdkman-init.sh"` or open a new terminal                       |
| `brew: command not found` after install  | Follow the PATH instructions the Homebrew installer printed; re-source shell config          |
| `bash --version` still shows 3.x         | Ensure `$(brew --prefix)/bin` is first in `$PATH` in both `~/.zshrc` and `~/.bash_profile`   |
| `ffmpeg`/`convert`/`gs` not found        | Run `brew install ffmpeg imagemagick ghostscript` and re-source shell config                 |

---

## Quick Reference — Full Install (macOS, copy-paste)

```bash
# 1. Homebrew
which brew || /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 2. Latest Bash
brew install bash
echo "$(brew --prefix)/bin/bash" | sudo tee -a /etc/shells
chsh -s "$(brew --prefix)/bin/bash"
for f in ~/.zshrc ~/.bash_profile; do
  grep -qxF 'export PATH="$(brew --prefix)/bin:$PATH"' "$f" 2>/dev/null || \
    echo 'export PATH="$(brew --prefix)/bin:$PATH"' >> "$f"
done
source ~/.zshrc

# 3. SDKMAN + Java
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 26-tem

# 4. Media dependencies
brew install ffmpeg imagemagick ghostscript

# 5. Clone EME
sudo git clone https://github.com/entermedia-community/eme-lib /usr/local/lib/eme-lib

# 6. Make executable + symlink
sudo chmod +x /usr/local/lib/eme-lib/resources/bin/eme.sh
sudo ln -sf /usr/local/lib/eme-lib/resources/bin/eme.sh /usr/local/bin/eme

# 7. Verify
which eme

# 8. Create a project
eme init ~/projects/my-eme-app

# 9a. Open in VS Code and press F5
# 9b. Or start via terminal:
eme start ~/projects/my-eme-app
```

## Quick Reference — Full Install (Linux, copy-paste, system-wide)

```bash
# 1. Verify/install Java 20+
java -version
# If missing: sudo apt update && sudo apt install -y openjdk-21-jdk

# 2. Media dependencies
sudo apt update && sudo apt install -y ffmpeg imagemagick ghostscript
# Fedora/RHEL: sudo dnf install ffmpeg imagemagick ghostscript

# 3. Clone
sudo git clone https://github.com/entermedia-community/eme-lib /usr/local/lib/eme-lib

# 4. Make executable + symlink
sudo chmod +x /usr/local/lib/eme-lib/resources/bin/eme.sh
sudo ln -sf /usr/local/lib/eme-lib/resources/bin/eme.sh /usr/local/bin/eme

# 5. Verify
which eme

# 6. Create a project
eme init ~/projects/my-eme-app

# 7a. Open in VS Code and press F5
# 7b. Or start via terminal:
eme start ~/projects/my-eme-app
```
