#!/bin/bash

set -ex


version=$(git describe)
lein_v="lein-v-$version"

cleanup() {
    rm -f "$lein_v.tar.gz"
    rm -rf "$lein_v"
}
trap cleanup INT TERM EXIT

cleanup
git checkout-index --prefix="$lein_v/" -a
tar czf "$lein_v.tar.gz" $lein_v/
s3cmd put "$lein_v.tar.gz" "s3://rk-chef/lein-v/"
