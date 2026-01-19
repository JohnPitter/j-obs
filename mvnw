#!/bin/sh
# Maven Wrapper script
# https://github.com/apache/maven-wrapper

set -euf

MAVEN_WRAPPER_VERSION="3.2.0"
MAVEN_VERSION="3.9.5"
MAVEN_HOME="${HOME}/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}"
MAVEN_CMD="${MAVEN_HOME}/bin/mvn"

# Download Maven if not present
if [ ! -f "${MAVEN_CMD}" ]; then
    echo "Downloading Maven ${MAVEN_VERSION}..."
    mkdir -p "${MAVEN_HOME}"
    MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"

    if command -v curl > /dev/null; then
        curl -fsSL "${MAVEN_URL}" | tar -xzf - -C "${MAVEN_HOME}" --strip-components=1
    elif command -v wget > /dev/null; then
        wget -qO- "${MAVEN_URL}" | tar -xzf - -C "${MAVEN_HOME}" --strip-components=1
    else
        echo "Error: curl or wget required to download Maven"
        exit 1
    fi
fi

exec "${MAVEN_CMD}" "$@"
