I’m currently facing two issues:The Docker base images are available to build the Docker image, but the Docker daemon is not accessible when using Docker‑in‑Docker. I also tried using Kaniko, but that approach failed as well due to connec
The Docker base images are available to build the Docker image, but the Docker daemon is not accessible when using Docker‑in‑Docker. I also tried using Kaniko, but that approach failed as well due to connectivity issues.
In the CI/CD pipeline, for running unit tests I’m using the Medtronic‑approved image case.artifacts.medtronic.com/ext-docker-hub-remote/python:3.12-slim, but this image does not have pytest installed, which is required for the tests.

