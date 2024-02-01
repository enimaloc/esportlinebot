package fr.enimaloc.esportline.api.github;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.Arrays;

public record LatestGithubRelease(
        String url,
        @JsonAlias("html_url") String htmlUrl,
        @JsonAlias("assets_url") String assetsUrl,
        @JsonAlias("upload_url") String uploadUrl,
        @JsonAlias("tarball_url") String tarballUrl,
        @JsonAlias("zipball_url") String zipballUrl,
        int id,
        @JsonAlias("node_id") String nodeId,
        @JsonAlias("tag_name") String tagName,
        @JsonAlias("target_commitish") String targetCommitish,
        String name,
        String body,
        boolean draft,
        boolean prerelease,
        @JsonAlias("created_at") String createdAt,
        @JsonAlias("published_at") String publishedAt,
        GithubUser author,
        GithubAsset[] assets,
        @JsonAlias("body_html") String bodyHtml,
        @JsonAlias("body_text") String bodyText,
        @JsonAlias("mentions_count") int mentionsCount,
        @JsonAlias("discussion_url") String discussionUrl,
        GithubReactions reactions
) {
    @Override
    public String toString() {
        return "LatestGithubRelease{" +
                "url='" + url + '\'' +
                ", htmlUrl='" + htmlUrl + '\'' +
                ", assetsUrl='" + assetsUrl + '\'' +
                ", uploadUrl='" + uploadUrl + '\'' +
                ", tarballUrl='" + tarballUrl + '\'' +
                ", zipballUrl='" + zipballUrl + '\'' +
                ", id=" + id +
                ", nodeId='" + nodeId + '\'' +
                ", tagName='" + tagName + '\'' +
                ", targetCommitish='" + targetCommitish + '\'' +
                ", name='" + name + '\'' +
                ", body='" + body + '\'' +
                ", draft=" + draft +
                ", prerelease=" + prerelease +
                ", createdAt='" + createdAt + '\'' +
                ", publishedAt='" + publishedAt + '\'' +
                ", author=" + author +
                ", assets=" + Arrays.toString(assets) +
                ", bodyHtml='" + bodyHtml + '\'' +
                ", bodyText='" + bodyText + '\'' +
                ", mentionsCount=" + mentionsCount +
                ", discussionUrl='" + discussionUrl + '\'' +
                ", reactions=" + reactions +
                '}';
    }
}
