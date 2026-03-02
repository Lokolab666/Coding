ERROR: Error cleaning up pod: resource name may not be empty
ERROR: Job failed (system failure): prepare environment: setting up build pod: admission webhook "validate.kyverno.svc-fail" denied the request: 
resource Pod/gitlab-runner/runner-8xppzdx5t-project-23032-concurrent-0-by2obf6z was blocked due to the following policies 
restrict-image-registries:
  validate-registries: '[KP-POD-001] Unmanaged image registry is not allowed. Allowed
    registries: case.artifacts.medtronic.com/, registry.medtronic.gitlab-dedicated.com/,
    mdtuseast1ro.jfrog.io/, mdtuseast1rw.jfrog.io/, mdteucentral1rw.jfrog.io/, en4mdtprod.jfrog.io/,
    en1mdtprod.jfrog.io/, en3mdtprod.jfrog.io/, mdtprod.jfrog.io/, mdtuseast1ro.pe.jfrog.io/,
    mdtuseast1rw.pe.jfrog.io/, mdteucentral1rw.pe.jfrog.io/, en4mdtprod.pe.jfrog.io/,
    en1mdtprod.pe.jfrog.io/, en3mdtprod.pe.jfrog.io/, mdtprod.pe.jfrog.io/'
. Check https://docs.gitlab.com/runner/shells/#shell-profile-loading for more information