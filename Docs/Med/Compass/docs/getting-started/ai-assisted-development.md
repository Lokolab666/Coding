---
label: AI-Assisted Development
icon: copilot
order: 20
---

# AI-Assisted Development on Compass CI

Compass CI is designed to support AI-assisted delivery workflows while preserving security, governance, and repeatable deployment patterns.

Teams can use [approved AI tools](https://app.powerbi.com/groups/me/reports/7b2fd8a4-85da-4299-9ec4-c4e2ac20eacf/a12fcb48a9bcd5eec615?ctid=d73a39db-6eda-495d-8000-7579f56d68b7&experience=power-bi) (including **GitHub Copilot**, **Cursor**, and others) to accelerate common engineering tasks across application code, pipeline configuration, and Kubernetes manifests.

## Why Compass CI Works Well with AI

- **Standardized patterns** reduce ambiguity for AI-generated changes (Dockerfiles, CI templates, Kubernetes resources).
- **Policy and security checks in pipeline** help validate AI-assisted output before deployment.
- **GitOps + code review workflow** keeps all AI-generated changes auditable in GitLab.
- **Shared templates and playbooks** improve prompt quality and output consistency.

## Common AI Use Cases

Use AI assistants to draft and accelerate work, then validate using your normal PR, test, and security review process.

### Bug Fixes
- Explain an error stack trace and propose likely root causes.
- Suggest targeted code changes with minimal blast radius.
- Generate regression test ideas based on the issue pattern.

### Vulnerability Patches and Security Remediation
- Interpret SAST/SCA/container scan findings.
- Propose package upgrade paths and safer code alternatives.
- Draft remediation pull requests and changelog notes.

### Test Case Creation
- Generate unit/integration test scaffolding for new or changed code.
- Propose edge-case and negative test scenarios.
- Expand test coverage around incident-prone areas.

### GitLab Pipeline Customization
- Draft or refactor `.gitlab-ci.yml` jobs and reusable templates.
- Add branch/environment-specific behavior.
- Troubleshoot failed jobs and suggest fixes for common pipeline errors.

### Troubleshooting and Incident Support
- Summarize logs and error signatures from Grafana/GitLab output.
- Produce hypothesis-driven debugging plans.
- Suggest rollback/forward-fix options with risk tradeoffs.

### Kubernetes Configuration
- Generate or refine manifests for `Deployment`, `Service`, `Ingress`, `CronJob`, etc.
- Suggest probe/resource/security-context improvements.
- Help align manifests with platform conventions and policy controls.

### Design Review and Implementation Options
- Use AI as a peer reviewer to evaluate implementation approaches before coding.
- Compare tradeoffs across different patterns, libraries, or deployment options.
- Brainstorm design ideas, identify risks, and pressure-test assumptions early.

### Documentation and Onboarding
- Draft runbooks, operational notes, and architecture summaries.
- Convert tribal knowledge into repeatable documentation.
- Create concise PR descriptions and release notes.

## Getting Better AI Results in VS Code

AI tools work better when VS Code has access to the repositories that define your application and its delivery model.

For Compass CI teams, that often means creating a VS Code workspace that includes:
- Your application repository
- [`semantic-release`](https://medtronic.gitlab-dedicated.com/bcp_web/devops/semantic-release)
- [`compass-ci-playbook`](https://medtronic.gitlab-dedicated.com/bcp_web/common/compass-ci-playbook)
- Any other directly relevant platform/configuration repository used by your team

This gives the AI assistant more complete context when answering questions about pipeline behavior, release automation, deployment patterns, documentation, and platform conventions.

### Recommended Setup
- Create a multi-root VS Code workspace instead of opening only your application folder.
- Add the repos your team actively depends on for day-to-day work.
- Save the workspace file so the same context can be reused by the team.

### Best Practices for Context
- Include related repos when asking cross-cutting questions about CI/CD, GitOps, or Kubernetes behavior.
- Keep the workspace focused; too many unrelated folders can reduce signal and make answers less relevant.
- Point the AI to specific files, errors, manifests, or pipeline jobs when possible.
- Ask for comparisons and tradeoffs when exploring implementation options, not just code generation.

## Choosing the Right Mode

Different AI modes are useful for different kinds of work.

### Ask / Chat Mode
- Best for explanations, troubleshooting help, code review feedback, brainstorming, and comparing implementation options.
- Use it when you want quick answers, summaries, or guidance before making changes.

### Plan Mode
- Best for step-by-step approaches before changing code or configuration.
- Use it when the task spans multiple systems, teams, or repositories and you want a clear execution path first.

### Agent Mode
- Best for repo-aware work such as searching across files, making coordinated edits, generating tests, or updating configuration in multiple places.
- Use it when you want the tool to inspect the workspace and take action, not just answer a question.

## Model Guidance

- In most cases, keep model selection set to **Auto**.
- Use faster/default models for quick questions, drafting, and lightweight code suggestions.
- Use stronger reasoning models for architecture decisions, root-cause analysis, complex refactors, and cross-repository questions.

## Approved Tool Access and Governance

For approved AI tools, access instructions, and current review/approval status, visit the [Hyperautomation Hub](https://medtronic.sharepoint.com/sites/Hyperautomation/SitePages/AI%20Tool%20Council.aspx).

## Recommended Guardrails
- Keep sensitive data out of prompts unless explicitly allowed by policy.
- Treat AI output as draft content; always review and test before merge.
- Use small, focused pull requests for easier validation.
- Rely on platform security/policy checks as required release gates.

## Related Compass CI Guides
[!ref icon="tools" text="Tooling & Access"](./tooling-and-access.md)
[!ref icon="shield" text="Security & GitLab Work items"](../cicd-pipeline/security-gitlab-issues.md)
[!ref icon="question" text="Troubleshooting Guide"](../troubleshooting-and-support/troubleshooting-guide.md)
[!ref icon="book" text="How-To Guides"](../how-to/)
