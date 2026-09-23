CI/CD and Release notes
=======================

Releases
--------

Pushing a version tag (`v*`, or `wear-*`, `sun-*`, `widget-*` for a single app)
builds signed APKs and Play bundles and puts them on a draft GitHub release:

    git tag wear-1.3.0 && git push github wear-1.3.0

Signing uses these repository secrets, and the build falls back to unsigned
artifacts if they're missing. This is the current release key (a PKCS12
keystore, where the key password is the keystore password):

| Secret              | Value                                                    |
| ------------------- | -------------------------------------------------------- |
| `KEYSTORE_BASE64`   | keystore, base64 encoded (`base64 -w0 24h-analog.p12`)   |
| `KEYSTORE_PASSWORD` | keystore password                                        |
| `KEY_ALIAS`         | key alias within the keystore                            |
| `KEY_PASSWORD`      | password for that key                                    |

### Key rotation

The apps were first released with a 1024-bit RSA key from 2009. Releases now use
a stronger key, and `signing/lineage` is the proof of rotation that lets Android
accept an update signed by the new key over an install signed by the old one.
It contains only public certificates, so it's safe to keep in the repository.

- The **Play bundles and the Sun app** (a new app) use the current key.
- The **watch face and widget APKs** are signed with the current key and the
  lineage. Both need Android 13 (see their `minSdkVersion`), which is also the
  version from which Android recognizes a rotated key, so the original key from
  2009 is never needed to produce a release: every device that can install
  these apps recognizes the new one directly.

`signing/sign-apk.sh` does the signing after the Gradle build, and
`./signing/sign-apk.sh check-lineage` (also run in CI) checks the lineage file.
To sign a release build locally, set `KEYSTORE_FILE` (a path), plus the same
passwords and alias, and run `./gradlew assembleRelease bundleRelease`. That
signs with the current key only; use the script for the lineage.
