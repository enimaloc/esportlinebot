package fr.enimaloc.esportline.api.github;

import com.fasterxml.jackson.annotation.JsonAlias;

public record GithubAsset(
        String url,
        @JsonAlias("browser_download_url") String browserDownloadUrl,
        int id,
        @JsonAlias("node_id") String nodeId,
        String name,
        String label,
        State state,
        @JsonAlias("content_type") String contentType,
        int size,
        @JsonAlias("download_count") int downloadCount,
        @JsonAlias("created_at") String createdAt,
        @JsonAlias("updated_at") String updatedAt,
        GithubUser uploader
) {
    public enum State {
        UPLOADED,
        OPEN
    }
}
