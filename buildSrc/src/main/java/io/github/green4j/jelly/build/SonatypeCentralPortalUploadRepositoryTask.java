/*
 * MIT License
 *
 * Copyright (c) 2018-2026 Anatoly Gudkov and others
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.green4j.jelly.build;

import io.github.green4j.jelly.simple.JsonArray;
import io.github.green4j.jelly.simple.JsonObject;
import io.github.green4j.jelly.simple.JsonValue;
import io.github.green4j.jelly.simple.JsonValueParser;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscribers;
import java.net.http.HttpResponse.ResponseInfo;
import java.time.Duration;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class SonatypeCentralPortalUploadRepositoryTask extends DefaultTask {
    private static final String CENTRAL_PORTAL_OSSRH_API_URI = "https://ossrh-staging-api.central.sonatype.com";
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(30);

    private final Property<String> portalUsername;
    private final Property<String> portalPassword;
    private final Property<String> groupId;
    private final Property<Boolean> snapshotRelease;

    /**
     * Creates new task instance.
     */
    public SonatypeCentralPortalUploadRepositoryTask() {
        portalUsername = getProject().getObjects().property(String.class);
        portalPassword = getProject().getObjects().property(String.class);
        groupId = getProject().getObjects().property(String.class);
        snapshotRelease = getProject().getObjects().property(Boolean.class);
    }

    /**
     * Returns property to set Central Portal username.
     *
     * @return Central Portal username.
     */
    @Input
    @Optional
    public Property<String> getPortalUsername() {
        return portalUsername;
    }

    /**
     * Returns property to set Central Portal password.
     *
     * @return Central Portal password.
     */
    @Input
    @Optional
    public Property<String> getPortalPassword() {
        return portalPassword;
    }

    /**
     * Returns property to set {@code groupId} of the project.
     *
     * @return {@code groupId} of the project.
     */
    @Input
    public Property<String> getGroupId() {
        return groupId;
    }

    /**
     * Returns property to set snapshot release.
     *
     * @return {@code true} if snapshot release.
     */
    @Input
    public Property<Boolean> getSnapshotRelease() {
        return snapshotRelease;
    }

    @TaskAction
    public void run() throws IOException, InterruptedException {
        if (!portalUsername.isPresent()) {
            return; // release is not configured
        }

        if (snapshotRelease.get()) {
            return; // snapshots are published directly
        }

        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT)
                .build();

        final String userNameAndPassword = portalUsername.get() + ":" + portalPassword.get();
        final String bearer = new String(
                Base64.getEncoder().encode(userNameAndPassword.getBytes(US_ASCII)), US_ASCII);

        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + bearer);

        final URI apiUri = URI.create(CENTRAL_PORTAL_OSSRH_API_URI);

        final String repositoryKey = findOpenRepository(apiUri, httpClient, requestBuilder);
        uploadRepositoryToPortal(apiUri, httpClient, requestBuilder, repositoryKey);
        dropRepository(apiUri, httpClient, requestBuilder, repositoryKey);
    }

    private String findOpenRepository(final URI apiUri,
                                      final HttpClient httpClient,
                                      final HttpRequest.Builder requestBuilder)
            throws IOException, InterruptedException {

        final HttpRequest request = requestBuilder
                .copy()
                .GET()
                .uri(apiUri.resolve("/manual/search/repositories?ip=client"))
                .build();
        final HttpResponse<String> response = httpClient.send(
                request, (ResponseInfo responseInfo) -> BodySubscribers.ofString(US_ASCII));

        final String body = response.body();

        if (200 != response.statusCode()) {
            throw new IllegalStateException("Failed to query repositories: "
                    + "status=" + response.statusCode() + ", response=" + body);
        }
        final String noOpenRepositoryFoundError = "No open repositories found!";

        final JsonValue jsonBody = new JsonValueParser().parseAndEoj(body);
        final JsonObject rootObject = jsonBody.asObject();
        if (rootObject == null) {
            throw new IllegalStateException(noOpenRepositoryFoundError);
        }
        final JsonArray repositories = rootObject.getArray("repositories");
        if (repositories == null) {
            throw new IllegalStateException(noOpenRepositoryFoundError);
        }

        String repositoryKey = null;

        final String group = groupId.get();
        for (int i = 0; i < repositories.size(); i++) {
            final JsonObject repo = repositories.getObject(i);
            if ("open".equals(repo.getString("state"))) {
                final String key = repo.getString("key");
                if (key.contains(group)) {
                    repositoryKey = key;
                    break;
                }
            }
        }

        if (null == repositoryKey) {
            throw new IllegalStateException(noOpenRepositoryFoundError);
        }
        return repositoryKey;
    }

    private static void uploadRepositoryToPortal(final URI apiUri,
                                                 final HttpClient httpClient,
                                                 final HttpRequest.Builder requestBuilder,
                                                 final String repositoryKey) throws IOException, InterruptedException {
        final HttpRequest request = requestBuilder
                .copy()
                .POST(HttpRequest.BodyPublishers.noBody())
                .uri(apiUri.resolve("/manual/upload/repository/" + repositoryKey + "?publishing_type=automatic"))
                .build();
        final HttpResponse<String> response = httpClient.send(
                request, (ResponseInfo responseInfo) -> BodySubscribers.ofString(US_ASCII));

        if (200 != response.statusCode()) {
            throw new IllegalStateException("Failed to upload repository: repository_key=" + repositoryKey +
                    ", status=" + response.statusCode() + ", response=" + response.body());
        }
    }

    private static void dropRepository(final URI apiUri,
                                       final HttpClient httpClient,
                                       final HttpRequest.Builder requestBuilder,
                                       final String repositoryKey) throws IOException, InterruptedException {
        final HttpRequest request = requestBuilder
                .copy()
                .DELETE()
                .uri(apiUri.resolve("/manual/drop/repository/" + repositoryKey))
                .build();
        final HttpResponse<String> response = httpClient.send(
                request, (ResponseInfo responseInfo) -> BodySubscribers.ofString(US_ASCII));

        if (204 != response.statusCode()) {
            throw new IllegalStateException("Failed to drop repository: repository_key=" + repositoryKey +
                    ", status=" + response.statusCode() + ", response=" + response.body());
        }
    }
}