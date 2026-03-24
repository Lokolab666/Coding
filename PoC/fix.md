Ese error no viene del Python. Viene de esta línea del job:

nl -ba "$POLICY_FULL_PATH" | sed -n '1,60p'

Ahora cambiaste FILE_NAME a un archivo nuevo, por ejemplo:

antrea_clusternetworkpolicy_egress-amiv2.yaml

Como ese archivo todavía no existe en el repo, nl falla con No such file or directory y el job se cae antes de ejecutar python3 scripts/parse_input.py.

Tu script Python ya estaba preparado para esto:

si el archivo existe, lo carga

si no existe, load_existing_yaml() devuelve None

luego ensure_base_document(...) crea el YAML base nuevo


Así que el problema no es la lógica de generación. El problema es que tu paso de “debug” asume que el archivo ya existe.

Debes cambiar esa parte del generate_job para soportar ambos casos: archivo existente o archivo nuevo.

Usa esto:

generate_job:
  stage: generate
  script:
    - git config --global user.name "Cristian Fandino"
    - git config --global user.email "cristian.a.fandinomesa@medtronic.com"
    - git config --global --add safe.directory "$CI_PROJECT_DIR"

    - TARGET_REPO_URL="https://${GIT_USERNAME}:${GIT_WRITE_TOKEN}@code.medtronic.com/io-shared-services/atlas/kubernetes-management/abrc1l-atlas-dev.git"
    - rm -rf "$CI_PROJECT_DIR/target-repo"
    - git clone "$TARGET_REPO_URL" "$CI_PROJECT_DIR/target-repo"

    - cd "$CI_PROJECT_DIR/target-repo"
    - git fetch origin "$TARGET_BASE_BRANCH"
    - git checkout -B "$TARGET_BASE_BRANCH" "origin/$TARGET_BASE_BRANCH"

    - export POLICY_FULL_PATH="$CI_PROJECT_DIR/target-repo/$OUTPUT_PATH$FILE_NAME"
    - mkdir -p "$(dirname "$POLICY_FULL_PATH")"

    - echo "==== Existing file before parsing ===="
    - |
      if [ -f "$POLICY_FULL_PATH" ]; then
        nl -ba "$POLICY_FULL_PATH" | sed -n '1,80p'
      else
        echo "File does not exist yet. A new policy file will be created:"
        echo "$POLICY_FULL_PATH"
      fi

    - cd "$CI_PROJECT_DIR"
    - python3 scripts/parse_input.py

    - echo "==== File after parsing ===="
    - nl -ba "$POLICY_FULL_PATH" | sed -n '1,120p'

    - cp "$POLICY_FULL_PATH" "$CI_PROJECT_DIR/generated_policy.yaml"
    - ls -l "$CI_PROJECT_DIR/generated_policy.yaml"
  artifacts:
    paths:
      - generated_policy.yaml

El cambio clave es este bloque:

if [ -f "$POLICY_FULL_PATH" ]; then
  nl -ba "$POLICY_FULL_PATH" | sed -n '1,80p'
else
  echo "File does not exist yet. A new policy file will be created:"
  echo "$POLICY_FULL_PATH"
fi

Y también este:

mkdir -p "$(dirname "$POLICY_FULL_PATH")"

Eso evita otro fallo si por alguna razón la carpeta destino no existe todavía.

Hay otro detalle: en tu log escribiste nl-ba sin espacio:

$ nl-ba "$POLICY_FULL_PATH" | sed -n '1,60p'

Eso también está mal. El comando correcto es:

nl -ba "$POLICY_FULL_PATH" | sed -n '1,60p'

Si en el .gitlab-ci.yml quedó nl-ba, corrígelo. Si solo fue un copy/paste del log, ignóralo.

En resumen: para crear un archivo nuevo no debes cambiar el Python. Debes dejar de asumir en el pipeline que el archivo ya existe. El Python ya sabe crear uno nuevo; tu generate_job era el que lo estaba bloqueando antes de tiempo.
