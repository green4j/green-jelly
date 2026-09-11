# GitHub Actions in green-jelly

This directory contains CI/CD workflows for `green-jelly`:

- `workflows/build.yml` - regular build and test validation.
- `workflows/release.yml` - publish artifacts to Sonatype (snapshots and releases).

## Build Workflow

`build.yml` runs on:

- `pull_request` to `master`
- `push` to `master`
- manual trigger (`workflow_dispatch`)

What it does:

1. Checks out the repository.
2. Runs a JVM matrix with Temurin JDK versions `11`, `17`, `21`, and `25`.
3. Uses Gradle cache for each matrix job.
4. Runs:

```bash
./gradlew --no-daemon --stacktrace clean build
```

`build` is the whole definition of green here - unit tests, Checkstyle and JaCoCo all hang off it.
There is no separate long-running suite to split out.

## Release and Publish Workflow

`release.yml` supports two publishing modes:

- **snapshot** - publish `*-SNAPSHOT` versions to Sonatype snapshots repository.
- **release** - publish release versions and finalize through Sonatype Central Portal.

The publish workflow runs on **JDK 11** to keep release artifacts built from the minimum supported Java baseline.

### Triggers

- manual trigger (`workflow_dispatch`) with `publish_mode` input (`snapshot` or `release`)
- git tag push matching `v*` (automatically uses `release` mode)

### Commands

- Snapshot mode:

```bash
./gradlew --no-daemon --stacktrace clean build publish
```

- Release mode:

```bash
./gradlew --no-daemon --stacktrace clean build publish uploadArtifactsToSonatypeCentralPortal
```

Both run `build` first - nothing is published which has not passed the tests, Checkstyle and JaCoCo of the
build itself, rather than trusting an earlier run on another commit.

## An example of release

1. Set release version
```
echo 0.1.7 > version.txt
git add version.txt && git commit -m "Release 0.1.7"
git push
```
2. Initiate release pushing the tag
```
git tag v0.1.7
git push origin v0.1.7
```
3. Increment version to next snapshot
```
echo 0.1.8-SNAPSHOT > version.txt
git add version.txt && git commit -m "Back to snapshot" && git push
```

## Published Artifacts

Group `io.github.green4j`:

| Artifact | What it is |
|---|---|
| `green-jelly` | the JSON parser and generator, the whole library |

It carries a sources jar and a javadoc jar.

## Required GitHub Secrets

Configure these secrets in repository or organization settings:

- `SONATYPE_USERNAME`
- `SONATYPE_PASSWORD`
- `SIGNING_GPG_SECRET_KEY` (ASCII-armored private key)
- `SIGNING_GPG_PASSWORD`

The Gradle build reads these from environment variables in `build.gradle`.

## Version Rules

Version is read from `version.txt`.

- Snapshot mode requires version ending with `-SNAPSHOT`.
- Release mode requires version **without** `-SNAPSHOT`.

The workflow validates this before running Gradle. A tag push is validated further: the tag has to be
`v<version>` of the same `version.txt`, so a tag names what was actually published.

## Recommended Release Procedure

1. Ensure `build.yml` is green on `master`.
2. Update `version.txt` to a non-snapshot version (for example `0.1.7`).
3. Push the release commit and create/push tag `v0.1.7` (or run `release.yml` manually with `publish_mode=release`).
4. Wait for `release.yml` to publish and finalize artifacts.
5. After release, bump `version.txt` to next snapshot (for example `0.1.8-SNAPSHOT`).

## Troubleshooting

- **Missing secrets**: release workflow fails early with a clear message.
- **Version/mode mismatch**: verify `version.txt` suffix and selected `publish_mode`.
- **Signing failures**: ensure the key is ASCII-armored and password matches the key.
- **Sonatype upload errors**: retry after verifying credentials and Sonatype account permissions.
