#!/usr/bin/env bash
#
# Release signing for APKs that are sideloaded from GitHub releases, after the signing key was
# rotated from the original 2009 key to a stronger one.
#
# Play signs what it distributes itself. This script makes the sideloadable APKs carry the same
# key, plus a proof of rotation (signing/lineage), so a device that has an APK signed by the old
# key accepts an update signed by the new key.
#
# Usage:
#   sign-apk.sh sign [--modern-only] <unsigned.apk> <signed.apk>
#   sign-apk.sh check-lineage
#
# sign
#   Signs with the newest key and the lineage.
#
#   --modern-only  For apps that need Android 13 or newer (the watch face). Uses only a v3
#                  signature, which doesn't need the old key.
#
#   Without --modern-only the APK also carries the legacy signatures that Android 12 and older
#   need, and those must be made by the OLD key, so it has to be provided (OLD_KEYSTORE_FILE,
#   below). Devices on Android 13+ use the new key; older ones keep using the old key. That is
#   what Google Play does too, so Play and sideloaded installs stay compatible on every version.
#   If the old key isn't provided the APK is signed with the new key only, and existing installs
#   on Android 12 and older can't update in place.
#
# The keys come from the environment; passwords never appear on a command line:
#   KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD      the newest key
#   OLD_KEYSTORE_FILE, OLD_KEYSTORE_PASSWORD, OLD_KEY_ALIAS,
#   OLD_KEY_PASSWORD                                               the original key (optional)
#   LINEAGE_FILE     lineage to use (default: lineage next to this script)
#   APKSIGNER        path to apksigner (default: found in the Android SDK)
#
# Keep key and lineage paths free of spaces: apksigner.bat on Windows can't handle them.

set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
lineage="${LINEAGE_FILE:-$here/lineage}"

# SHA-256 of the original certificate, which is always the first signer in the lineage. The
# override is for tests that use throwaway keys.
original_digest="${ORIGINAL_DIGEST:-d42189b36569f69acf5559931d3d3fc527632f74deceb9df6142aae339b0d131}"

die() {
    echo "error: $*" >&2
    exit 1
}

# apksigner.bat needs Windows paths when this runs under Git Bash.
native() {
    if command -v cygpath >/dev/null 2>&1; then
        cygpath -w "$1"
    else
        printf '%s' "$1"
    fi
}

find_apksigner() {
    if [ -n "${APKSIGNER:-}" ]; then
        printf '%s' "$APKSIGNER"
        return
    fi

    if command -v apksigner >/dev/null 2>&1; then
        command -v apksigner
        return
    fi

    local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" newest name
    [ -n "$sdk" ] || die "apksigner not found; set APKSIGNER or ANDROID_HOME"

    newest="$(ls -d "$sdk"/build-tools/*/ 2>/dev/null | sort -V | tail -n 1)"
    for name in apksigner apksigner.bat; do
        if [ -f "${newest}${name}" ]; then
            printf '%s' "${newest}${name}"
            return
        fi
    done

    die "no apksigner in $sdk/build-tools"
}

apksigner="$(find_apksigner)"

# Scratch directory for signing, removed on exit. Global, so the exit trap can still see it.
work=""
trap '[ -z "$work" ] || rm -rf "$work"' EXIT

# The SHA-256 digest of each certificate in the lineage, oldest first.
lineage_digests() {
    "$apksigner" lineage --in "$(native "$lineage")" --print-certs |
        sed -n 's/.*lineage certificate SHA-256 digest: *//p' | tr -d '\r'
}

check_lineage() {
    [ -f "$lineage" ] || die "lineage file not found: $lineage"

    local digests
    mapfile -t digests < <(lineage_digests)

    [ "${#digests[@]}" -ge 2 ] || die "the lineage has ${#digests[@]} signer(s); it needs at least 2"
    [ "${digests[0]}" = "$original_digest" ] ||
        die "the lineage doesn't start with the original certificate (found ${digests[0]})"

    echo "lineage OK: ${#digests[@]} signers, newest certificate ${digests[${#digests[@]} - 1]}"
}

sign_apk() {
    local modern_only=false
    if [ "${1:-}" = "--modern-only" ]; then
        modern_only=true
        shift
    fi

    [ $# -eq 2 ] || die "usage: sign-apk.sh sign [--modern-only] <unsigned.apk> <signed.apk>"
    local input="$1" output="$2"
    [ -f "$input" ] || die "no such APK: $input"

    : "${KEYSTORE_FILE:?KEYSTORE_FILE is not set}" "${KEYSTORE_PASSWORD:?KEYSTORE_PASSWORD is not set}"
    : "${KEY_ALIAS:?KEY_ALIAS is not set}" "${KEY_PASSWORD:?KEY_PASSWORD is not set}"

    check_lineage >/dev/null

    local newest
    newest="$(lineage_digests | tail -n 1)"

    # Sign a copy in a scratch directory: apksigner leaves a partial file behind when it fails,
    # and its Windows launcher can't take the spaces that repository paths may contain.
    work="$(mktemp -d)"
    cp "$input" "$work/in.apk"

    local new_key=(--ks "$(native "$KEYSTORE_FILE")" --ks-pass env:KEYSTORE_PASSWORD
        --ks-key-alias "$KEY_ALIAS" --key-pass env:KEY_PASSWORD)
    local args

    if $modern_only; then
        echo "signing $(basename "$input"): newest key + lineage, v3 only (Android 13+)"
        args=("${new_key[@]}" --lineage "$(native "$lineage")"
            --v1-signing-enabled false --v2-signing-enabled false)
    elif [ -n "${OLD_KEYSTORE_FILE:-}" ]; then
        : "${OLD_KEYSTORE_PASSWORD:?OLD_KEYSTORE_PASSWORD is not set}"
        : "${OLD_KEY_ALIAS:?OLD_KEY_ALIAS is not set}" "${OLD_KEY_PASSWORD:?OLD_KEY_PASSWORD is not set}"

        echo "signing $(basename "$input"): original key + newest key + lineage"
        args=(--ks "$(native "$OLD_KEYSTORE_FILE")" --ks-pass env:OLD_KEYSTORE_PASSWORD
            --ks-key-alias "$OLD_KEY_ALIAS" --key-pass env:OLD_KEY_PASSWORD
            --next-signer "${new_key[@]}" --lineage "$(native "$lineage")")
    else
        echo "::warning::No OLD_KEYSTORE_FILE, so $(basename "$input") is signed with the new key only." \
            "Installs on Android 12 and older that have an older build can't update in place."
        args=("${new_key[@]}")
    fi

    "$apksigner" sign "${args[@]}" --v4-signing-enabled false \
        --out "$(native "$work/out.apk")" "$(native "$work/in.apk")" ||
        die "apksigner failed for $input"

    # Check the result the way an Android 13+ device would see it.
    local report
    report="$("$apksigner" verify --min-sdk-version 33 --print-certs "$(native "$work/out.apk")")" ||
        die "the signed APK doesn't verify"

    grep -qi "certificate SHA-256 digest: $newest" <<<"$report" ||
        die "the signed APK isn't signed by the newest key in the lineage ($newest)"

    mkdir -p "$(dirname "$output")"
    cp "$work/out.apk" "$output"
    echo "signed $output (signer $newest)"
}

case "${1:-}" in
    sign)
        shift
        sign_apk "$@"
        ;;
    check-lineage)
        check_lineage
        ;;
    *)
        sed -n '2,/^set -euo/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//' >&2
        exit 2
        ;;
esac
