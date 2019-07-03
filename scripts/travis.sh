#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(dirname "${BASH_SOURCE[0]}")
PROJECT_DIR=$(dirname "${SCRIPT_DIR}")
cd "${PROJECT_DIR}" || exit 1

# lint all scripts, abort if there's any warning.
function shellcheck_must_pass()
{
    if [[ $(shellcheck "$1") ]]; then
        echo "shellcheck $1 failed"
        shellcheck "$1"
        exit 1
    fi
}
shellcheck_must_pass ./scripts/travis.sh


# start pegasus onebox environment
wget https://github.com/XiaoMi/pegasus/releases/download/v1.11.5/pegasus-server-1.11.5-ba0661d--release.zip
unzip pegasus-server-1.11.5-ba0661d--release.zip
cd pegasus-server-1.11.5-ba0661d--release

./run.sh start_onebox -w
cd ../

if ! sbt test
then
    cd pegasus-server-1.11.5-ba0661d--release
    ./run.sh list_onebox
    exit 1
fi