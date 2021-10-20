#!/usr/bin/env bash
# Description: build and push charts package

# Safe mode
set -euo pipefail 

PROGRAM=$(basename "$0")
EXITCODE=0

VERSION=1.0.0
APP_VERSION=latest
PUSH=0
REGISTRY=http://localhost/helm
USERNAME=
PASSWORD=

cd $(dirname $0)
WORKING_DIR=$(pwd)

usage () {
    cat <<EOF
Usage:
    $PROGRAM [OPTIONS]... 

            [ -v, --version         [Optional] charts version, default: 1.0.0 ]
            [ -a, --app-version     [Optional] app version, default: latest ]
            [ -p, --push            [Optional] Push chart package to helm remote repository. Default: Not push ]
            [ -r, --registry        [Optional] helm chars repository, default: http://localhost/helm ]
            [ --username            [Optional] helm chars repository username ]
            [ --password            [Optional] helm chars repository password ]
            [ -h, --help            [Optional] Show help ]
EOF
}

usage_and_exit () {
    usage
    exit "$1"
}

log () {
    echo "$@"
}

error () {
    echo "$@" 1>&2
    usage_and_exit 1
}

warning () {
    echo "$@" 1>&2
    EXITCODE=$((EXITCODE + 1))
}

# Parse command line parameters
(( $# == 0 )) && usage_and_exit 1
while (( $# > 0 )); do 
    case "$1" in
        -v | --version )
            shift
            VERSION=$1
            ;;
        -a | --app-version )
            shift
            APP_VERSION=$1
            ;;
        -p | --push )
            PUSH=1
            ;;
        -r | --registry )
            shift
            REGISTRY=$1
            ;;
        --username )
            shift
            USERNAME=$1
            ;;
        --password )
            shift
            PASSWORD=$1
            ;;
        --help | -h | '-?' )
            usage_and_exit 0
            ;;
        -*)
            error "Invalid param: $1"
            ;;
        *) 
            break
            ;;
    esac
    shift
done

helm package bkjob --version $VERSION --app-version $APP_VERSION
if [[ $PUSH -eq 1 ]] ; then
    helm push bkjob-$VERSION.tgz $REGISTRY -f --username $USERNAME --password $PASSWORD
fi
log "BUILD SUCCESSFUL!"
