---
label: Tooling & Access
icon: tools
order: 150
---

# Tooling & Access

This page outlines the required tools for local development and the access requests needed to successfully onboard to the Compass CI platform.

## Local Development Tools

### Required Tooling
| Tool | Download | Purpose |
|------|----------|---------|
| **Visual Studio Code** | [Download](https://code.visualstudio.com/download) | Code editor with recommended extensions<br/><br/>**Recommended Extensions:**<br/>- **SQL Developer** - For database interactions<br/>- **Dev Containers** - For consistent dev environments between developers<br/>- **GitLab Workflow** - For GitLab integration<br/>- **Draw.io Integration** - For creating and editing diagrams<br/>- **vscode-sops** - Decrypt/edit/re-encrypt SOPS files directly in VS Code<br/>- **vscode-base64** - Quickly base64 encode/decode your text selections in VS Code<br/>- **GitHub Copilot** - AI-powered code completion<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1. Submit a [request here for access](https://medtronicprod.service-now.com/it?id=sc_cat_item&table=sc_cat_item&sys_id=a9bb3531971a86d0f95e3ffce053afb2)<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2. Select the `GitHub Copilot for Business - Non-GitHub User` Role<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;3. Once you are given access, log in to GitHub from VS Code using your `userid_mdtcop` account |
| **Docker Desktop** | [Download](https://www.docker.com/products/docker-desktop/) and [Order a License](https://mspm1bapps0129.ent.core.medtronic.com/esd/Items/Details?PackageId=344) | Container runtime for local development |
| **Draw.io** | [Download](https://www.drawio.com/) | Diagram editor for infrastructure & application diagrams |
| **Git** | [Download](https://git-scm.com/install/windows) | Version control for repository operations |
| **Postman** | [Download](https://www.postman.com/downloads/) | API testing and development |
| **Node.js** | [Download](https://nodejs.org/en/download) | Runtime for JavaScript/TypeScript apps and build tools |
| **pre-commit** | [Install](https://pre-commit.com/) | Local git hooks for linting, formatting, and commit message checks |

For practical guidance on using GitHub Copilot and other approved AI tools for bug fixes, security remediation, test generation, and pipeline/Kubernetes support, see [AI-Assisted Development](./ai-assisted-development.md).

---

### VS Code SOPS Setup (Recommended)

Use the `vscode-sops` extension with a shared key file so encrypted files can be opened, edited, and re-encrypted in place across multiple projects.

#### 1) Create a shared SOPS key directory

Create this folder on your local machine:

```powershell
New-Item -ItemType Directory -Force -Path "$HOME\.sops"
```

#### 2) Keep single-project key files in `.sops`

Keep your per-project key files in:

```text
C:\Users\<your-user>\.sops\
```

Examples:
- `C:\Users\<your-user>\.sops\key-einstein.txt`
- `C:\Users\<your-user>\.sops\key-contract-gpt.txt`

#### 3) Create the shared `keys.txt` in the Windows default SOPS location

Create the file here:

```text
C:\Users\<your-user>\AppData\Roaming\sops\age\keys.txt
```

This file should contain **all AGE private keys** you use across projects.

Create the folder first if needed:

```powershell
New-Item -ItemType Directory -Force -Path "$env:APPDATA\sops\age"
```

#### 4) Add private keys for all projects

For each project key file (for example `key-einstein.txt`, `key-contract-gpt.txt`), copy the `AGE-SECRET-KEY-...` line into `keys.txt` (one key per line).

Example `keys.txt`:

```text
# Einstein
AGE-SECRET-KEY-1EXAMPLEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA

# Contract GPT
AGE-SECRET-KEY-1EXAMPLEBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB
```

You can also append an entire key file into `keys.txt`:

```powershell
Get-Content "$HOME\.sops\key-einstein.txt" | Add-Content "$env:APPDATA\sops\age\keys.txt"
Get-Content "$HOME\.sops\key-contract-gpt.txt" | Add-Content "$env:APPDATA\sops\age\keys.txt"
```

#### 5) Point VS Code and terminal to `keys.txt`

In your workspace or user VS Code settings:

```json
{
   "sops.defaults.ageKeyFile": "C:/Users/<your-user>/AppData/Roaming/sops/age/keys.txt",
   "terminal.integrated.env.windows": {
      "SOPS_AGE_KEY_FILE": "C:/Users/<your-user>/AppData/Roaming/sops/age/keys.txt"
   }
}
```

With this setup, opening an encrypted SOPS file in VS Code uses any matching key in `keys.txt`, allowing decrypt/edit/re-encrypt in place.

!!!warning
Never commit `keys.txt` or any private key file to Git. Keep them only on your local machine and share privately only when required by your team process.
!!!

---

### Pre-Commit (Recommended)

`pre-commit` runs checks automatically before each commit so issues are caught locally instead of in CI.

**Why use it:**
- Enforce Conventional/semantic commit messages
- Run linters/formatters consistently
- Catch common issues (trailing whitespace, invalid YAML)

**Suggested hooks for typical repos:**
- YAML checks: `check-yaml`, `yamllint`
- Dockerfile linting: `hadolint`
- Shell script linting: `shellcheck`
- JSON checks: `check-json`
- Safety checks: `detect-private-key`, `check-merge-conflict`, `check-added-large-files`

#### Install

**Windows:**
1. Install Python from [python.org](https://www.python.org/downloads/windows/) (this includes `pip`).
2. Run:
```bash
py -m pip install --user pre-commit
```

**macOS:**
```bash
brew install pre-commit
```

For per-repository pre-commit hook setup (`.pre-commit-config.yaml`, `pre-commit install`, and `commit-msg` hook), see [GitLab Repository Configuration](./gitlab-repository-configuration.md).

##### Troubleshooting: `pre-commit` command not found

If your shell shows `pre-commit` as not recognized, you can also run the same commands through Python:

```bash
python -m pre_commit install
python -m pre_commit install --hook-type commit-msg
```

On Windows, this usually means your Python user scripts directory is not on `PATH`.
Typical path:

```text
C:\Users\<your-user>\AppData\Roaming\Python\Python3xx\Scripts
```

After updating `PATH`, open a new terminal and verify:

```bash
pre-commit --version
```

**What the commands do:**
- `pre-commit install` installs the default pre-commit hook (runs checks on staged files).
- `pre-commit install --hook-type commit-msg` installs the commit message hook. Git treats commit messages as a separate hook, so without this, `commitlint` will not run (Commitizen is optional and only helps authors).

**Note:** The commit message hook uses `npx`, so Node.js must be installed.

---

### VS Code Development Containers (Optional)

Development Containers standardize your development environment across all developers, regardless of machine (Mac, Windows, or Linux). Everyone gets the same versions of Node.js, Python, pre-commit, and other tools, which reduces "works on my machine" issues. This is especially useful for larger development teams and also helps streamline onboarding by getting new developers up and running the application faster.

**Why use Dev Containers:**
- ✅ **Consistency** - Same environment for all developers (Windows/Mac/Linux)
- ✅ **Isolation** - Development tools don't clutter your host machine
- ✅ **One-click setup** - VS Code opens your repository inside the container
- ✅ **Pre-configured** - Pre-commit, Node.js, and helpful extensions already installed
- ✅ **No extra cost** - Uses Docker Desktop (already required)

#### Setup for Your Project

**Add to your repository:**

1. Create a `.devcontainer/` folder at your repository root:
```
your-repo/
├── .devcontainer/
│   ├── devcontainer.json
│   ├── Dockerfile (optional)
│   └── post-create.sh
├── .git/
├── src/
├── package.json
└── ...
```

2. Download these template files and place them in `.devcontainer/`:
   - [devcontainer.json](./../static/templates/.devcontainer/devcontainer.json) - VS Code dev environment config
   - [post-create.sh](./../static/templates/.devcontainer/post-create.sh) - Runs on first container creation (installs pre-commit, npm deps)
   - `Dockerfile` (optional) - Use when you need to customize the base image before VS Code features are installed

**When is a `Dockerfile` needed?**

Most teams can start with just `devcontainer.json` + `post-create.sh`.

Add a `Dockerfile` when you need image-level customization, for example:
- Installing Linux packages or OS-level dependencies with `apt` / `apk`
- Adding internal CA certificates or trusted package sources
- Removing/fixing problematic repos in the base image
- Pinning a custom base image with pre-installed tools

In `devcontainer.json`, use `"image"` for simple setups and switch to `"build": { "dockerfile": "Dockerfile" }` when you need these customizations.

3. Commit `.devcontainer/` to your repository:
   ```bash
   git add .devcontainer/
   git commit -m "chore: add VS Code Dev Container setup"
   git push
   ```

**Configure your local VS Code:**

1. Install the **Dev Containers** extension:
   - Open Extensions (`Ctrl+Shift+X` / `Cmd+Shift+X`)
   - Search for `Dev Containers` by Microsoft
   - Click **Install**

2. Restart VS Code

#### Using Dev Containers

**Open your repository in a container (first time):**

1. Ensure Docker Desktop daemon is running
1. Open a new VS Code window (`File → New Window`)
2. Press `Ctrl+Shift+P` (Windows/Linux) or `Cmd+Shift+P` (Mac)
3. Type **"Dev Containers: Open Folder in Container"** and press Enter
4. Select your repository folder
5. VS Code builds the container (2–3 minutes first time, faster on subsequent runs)
6. Once complete, you'll see a blue `><` indicator in the bottom-left corner—you're now inside the container!

**What happens automatically:**
- ✅ All dependencies installed (pre-commit, npm packages from package.json)
- ✅ Pre-commit hooks configured and ready
- ✅ VS Code extensions installed (GitLab Workflow, Copilot, linting tools, etc.)
- ✅ Your repository is mounted—all changes sync automatically

**Test that everything works:**

In the VS Code terminal (now running inside the container), verify:
```bash
node --version       # Should show v20.x
npm --version        # Should show 10.x or higher
pre-commit --version # Should show installed version
git --version        # Should work
```

Your team members can now:
- Commit code using pre-commit hooks
- Run `npm install` / `npm run build`
- Push to GitLab without PATH or version mismatches

**Switching between container and host:**

- **Exit container:** Click the green `><` button in bottom-left → **"Reopen Folder Locally"**
- **Re-enter container:** Click `><` → **"Open Folder in Container"**

See the [VS Code Dev Containers documentation](https://code.visualstudio.com/docs/devcontainers/containers) for advanced configuration options.

---

## What Teams Should Include in Their Project

To ensure every developer on your team has the same tools, add these files to your repository:

| File/Folder | Purpose | Location |
|-------------|---------|----------|
| `.devcontainer/devcontainer.json` | Defines base image, extensions, and setup | `.devcontainer/` |
| `.devcontainer/post-create.sh` | Auto-runs on container creation (installs pre-commit, npm deps) | `.devcontainer/` |
| `.pre-commit-config.yaml` | Pre-commit hook configuration | Repository root |
| `commitlint.config.js` | Conventional commit validation | Repository root |
| `package.json` | Node.js dependencies (including commitlint, semantic-release) | Repository root |
| `.markdownlint.json` | Markdown linting rules (optional) | Repository root |
| `.cz.toml` | Commitizen config for guided commits (optional) | Repository root |

**This gives every developer:**
- ✅ Node.js v20 (or your specified version)
- ✅ Pre-commit with all hooks ready
- ✅ Commitlint for enforcing commit format
- ✅ VS Code with recommended extensions (GitLab Workflow, Copilot, linting tools)
- ✅ Proper markdown and code linting
- ✅ Optional guided commit workflow with Commitizen

**Result:** No more "works on my machine"—every developer in your team operates in the same environment.

## Platform Access

### GitLab & JFrog Artifactory Access
See the [Shared Services documentation](https://it-sharedservices.medtronic.gitlab-dedicated.site/documentation/resources/self-service/#request-a-new-team) for detailed information.

For now, submit a generic ServiceNow ticket to **Infra-SourceCode-Global** for GitLab Dedicated access updates.
- Use the ticket to add or remove users as needed.
- Use the same assignment group to request new Teams (top-level groups) in GitLab Dedicated.

!!!warning
After approvals complete, log into [medtronic.gitlab-dedicated.com](https://medtronic.gitlab-dedicated.com/) to activate your account before accessing additional projects.
!!!

### Grafana Dashboards (Logs and Metrics)

==- Access to Existing Team Dashboard
1. Submit an [ETS Central Grafana Access Request Form](https://medtronicprod.service-now.com/it?id=mdtit_sc_cat_item&sys_id=8825570f47618a9441bba09d416d43d0):
   - **Requested for**: `<your userid>`
   - **What do you want to do?**: Add access
   - **Select Role**: ENT-centgrafana-user-SECURE

   This allows generic access to log in to Grafana.

2. Submit another [ETS Central Grafana Access Request Form](https://medtronicprod.service-now.com/it?id=mdtit_sc_cat_item&sys_id=8825570f47618a9441bba09d416d43d0):
   - **Requested for**: `<your userid>`
   - **What do you want to do?**: Add access
   - **Select Role**: ENT-centgrafana-*teamname*-SECURE

   This gives specific permissions to view the given team dashboards.
===

==- New Dashboard Configuration
If your application needs a new dashboard:

1. Send an email to [dl.itargocoreteam@medtronic.com](mailto:dl.itargocoreteam@medtronic.com?subject=Central%20Grafana%20Dashboard%20Configuration%20Request&body=Application%20Name:%0DApplication%20Environment(s):%0DTeam%20Name:%0DOther%20Team%20Contacts:) with:
   - Application Name(s) that should be included in your dashboard
   - Your team name
   - List of other users/team members who should have access

2. Submit the access request form for ENT-centgrafana-user-SECURE (see steps in "Access to Existing Team Dashboard" above)

3. Once the team-specific AD group and Grafana Team are created, submit another request for access to that specific team dashboard
===

Once access is granted, visit [Central Grafana](https://g-05c60f9e9b.grafana-workspace.us-east-1.amazonaws.com/).

### Contrast Security Access

For access to the Contrast dashboard for detailed vulnerability information:

Submit a ServiceNow request with:
- **Configuration Item**: Contrast Security - PROD
- **Assignment Group**: Platform Connectivity and Application Security
- **Description**: Edit level access to WebDevApps application
- **Detailed description**: Please add `<userid>` (`<email>`) with edit level access to the WebDevApps application in Contrast

For additional information, see the [Contrast page on Security Central](https://medtronic.sharepoint.com/sites/GSO/SitePages/Contrast-Security-Tool.aspx).

### D2 MRCS Access

Documents related to application design, requirements, test plans, test scripts, validation scope decisions, test execution results, etc. should all be stored in D2.

1. See this [Knowledge Article](https://medtronicprod.service-now.com/it/?id=mdtit_kb_article&sys_id=7cf629244f7cae00061056701310c7c2) for instructions on required training to obtain access to MRCS in [D2](https://mrcsd2.medtronic.com/D2/)
2. Request access to [IT Systems folder](https://medtronicprod.service-now.com/it/?id=mdtit_sc_cat_item&sys_id=4488882f4f136600bc427bcd0210c782). This folder contains documentation for existing web applications and can be a good resource for finding previous design information or test scripts

### CyberArk Access

Submit a ServiceNow request at [it.medtronic.com](https://it.medtronic.com/) - Search for `PRIVILEGED GLOBAL USER REQUEST`:
- **Requested for**: Enter user's name
- **Request Type**: Select appropriate type
- **Enterprise IT Role**: Platform Administrator
- **User Role**: CyberArk - Vault Administrator
- Select the appropriate Permission, Scope, Residency and Urgency
- **Intended Use**: Enter the specific vault names

**Example:**
> Need read-only access to: WebSolutions-Database and SSIT-WebSolutions

---

### Compass CI Platform Community

Join the [Global IT Argo Platform Community](https://engage.cloud.microsoft/main/org/medtronic.com/groups/eyJfdHlwZSI6Ikdyb3VwIiwiaWQiOiIyMzg5MjY4ODA3NjgifQ) on Viva Engage to receive important Argo platform updates and collaborate with other users and platform experts.

---

## Optional Tools & Access

### Medtronic Internal SSL Certificates

To create internally signed SSL certificates for sites, you may need access to [certrequest.medtronic.com](https://certrequest.medtronic.com/CMSAdminEnroll).

Submit [this form](https://medtronicprod.service-now.com/it/?id=mdtit_sc_cat_item&sys_id=0ee1285d1b414d506ed99603b24bcb3c):
- **Requested for**: Enter user's name
- Select Request Type
- **Please select a template**: Web Server Template
- Click Order Now

See the [Custom Hostnames training video](../education-and-training/index.md#custom-hostnames) for a walkthrough.


### Kubernetes Upgrade Notifications

Subscribe to the [Global IT Kubernetes Platform Community](https://engage.cloud.microsoft/main/org/medtronic.com/groups/eyJfdHlwZSI6Ikdyb3VwIiwiaWQiOiIxNzAzMTgyMjU0MDgifQ) on Viva Engage to receive updates on Kubernetes environment upgrades.

!!!
Cluster upgrades will happen in dev prior to the same upgrade being applied to production. Use this as an opportunity to regression test your applications.
!!!

### CTS Support for Your Application

If your application is not yet supported by the CTS Web App support team, submit the [Service Introduction for Managed Services](https://medtronicprod.service-now.com/bsp?id=sc_cat_item_guide&sys_id=13885e6b1b605dd06ed99603b24bcb8d&sysparm_category=7d805eccdbbc19d43f9366d405961988) request.

If you don't know whether your application is already supported, contact [RS.managedservices@medtronic.com](mailto:RS.managedservices@medtronic.com).

---

## Quick Start Checklist

Use this checklist to track your onboarding progress:

### Local Development Tools
- [ ] Visual Studio Code installed
  - [ ] SQL Developer extension
  - [ ] GitLab Workflow extension
   - [ ] vscode-sops extension
   - [ ] `C:\Users\<your-user>\AppData\Roaming\sops\age\keys.txt` created and configured in VS Code settings
   - [ ] Per-project key files stored in `C:\Users\<your-user>\.sops\key-<project>.txt`
  - [ ] GitHub Copilot access requested and configured
- [ ] Docker Desktop installed and licensed
- [ ] Draw.io installed
- [ ] Git installed
- [ ] Postman installed
- [ ] Node.js installed
- [ ] pre-commit installed and hooks enabled (recommended)

### Platform Access
- [ ] GitLab & Artifactory access requested (CI/CD Team Membership)
- [ ] First login to medtronic.gitlab-dedicated.com completed
- [ ] Grafana dashboard access requested
- [ ] Contrast Security access requested (if vulnerability scanning needed)
- [ ] D2 MRCS training completed and access requested
- [ ] CyberArk access requested (if secrets management needed)
- [ ] Subscribed to Compass CI Platform Community on Viva Engage
- [ ] **Platform team action** — Add application group to `trivy-policies` CI_JOB_TOKEN allowlist configured for onboarding group. See [Token Architecture](./gitlab-repository-configuration.md#platform-team-steps) for exact steps.

### Optional Access
- [ ] Subscribed to Kubernetes Platform Community on Viva Engage
- [ ] SSL certificate creation access requested (if custom hostnames needed)
- [ ] CTS support requested for application (if production application)

---

## Next Steps

Once you have completed the tooling and access setup:

[!ref icon="repo" text="Configure Your Repository"](./gitlab-repository-configuration.md)
[!ref icon="package" text="Review Kubernetes Manifests"](./kubernetes-manifests.md)
[!ref icon="workflow" text="Set Up Pipeline Configuration"](../cicd-pipeline/index.md)

For questions or assistance, contact Infra-Argo-Global through ServiceNow.
