Worked for 2m 16s

The ticket is not an uv package/version problem. pip cannot validate the TLS certificate chain for pypi.org, so it never retrieves the package index. The final “No matching distribution” message is only a consequence of that earlier SSL failure.

Documentation finding

The Compass documentation says Python packages should use the Medtronic Artifactory-backed PyPI registry:

https://case.artifacts.medtronic.com/artifactory/api/pypi/ext-pypi-python-remote/simple

See:

Containerizing Your Application — “Package Access via Artifactory → Pip”

Troubleshooting Guide — missing root/issuing CA guidance

Contacts and Assignment Groups — Artifactory support is Infra-CICD IT Support-Global


Recommended resolution

Run these commands in PowerShell with the virtual environment active:

.\.venv\Scripts\Activate.ps1

python -m pip config --site set global.index-url `
  "https://case.artifacts.medtronic.com/artifactory/api/pypi/ext-pypi-python-remote/simple"

python -m pip install --upgrade pip
python -m pip install uv

uv --version

Using --site stores the configuration in this virtual environment rather than changing every Python environment on the laptop.

For a one-time test before saving the configuration:

python -m pip install uv `
  --index-url "https://case.artifacts.medtronic.com/artifactory/api/pypi/ext-pypi-python-remote/simple"

If Artifactory also returns an SSL error

Then Python does not trust the Medtronic root or issuing CA. Collect:

python --version
python -m pip --version
python -m pip config debug

curl.exe -Iv `
  "https://case.artifacts.medtronic.com/artifactory/api/pypi/ext-pypi-python-remote/simple/uv/"

The documentation’s certificate principle applies here: trust the approved root/issuing CA, not an individual server certificate. Once an approved PEM CA bundle is supplied by support, configure it for this environment:

python -m pip config --site set global.cert "C:\path\to\approved-medtronic-ca-bundle.pem"
python -m pip install uv

Do not use --trusted-host, disable SSL verification, or download certificates from an unverified source.

Ticket classification

This is currently a local Python/package-source configuration issue, not a Compass cluster or deployment incident.

If the Artifactory endpoint returns 401/403, or uv is unavailable through the remote repository: assign to Infra-CICD IT Support-Global, CI Jfrog Artifactory - PROD.

If Artifactory works in the browser/curl but fails only in Python: route to workstation/Python certificate-trust support.

Route to Infra-Argo-Global only if the same problem happens during a Compass CI pipeline or inside a deployed Compass workload.


Suggested ticket response

> The reported “No matching distribution found for uv” is secondary. The actual failure is TLS certificate validation while pip connects directly to pypi.org.

Compass documentation requires Python packages to be obtained through the Medtronic Artifactory-backed PyPI registry. Please activate the virtual environment and run:

python -m pip config --site set global.index-url "https://case.artifacts.medtronic.com/artifactory/api/pypi/ext-pypi-python-remote/simple"
python -m pip install --upgrade pip
python -m pip install uv
uv --version

If this produces an SSL, 401, or 403 error against case.artifacts.medtronic.com, please provide python --version, python -m pip --version, python -m pip config debug, and the new error. Artifactory access or CA-chain issues should be routed to Infra-CICD IT Support-Global.



The documentation has a small gap: it gives the Linux /etc/pip.conf example, but does not currently show the equivalent Windows/PowerShell configuration above.
