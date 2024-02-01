package fr.enimaloc.esportline.api.github;

import com.fasterxml.jackson.annotation.JsonAlias;

public record GithubUser(
        String name,
        String email,
        String login,
        int id,
        @JsonAlias("node_id") String nodeId,
        @JsonAlias("avatar_url") String avatarUrl,
        @JsonAlias("gravatar_id") String gravatarId,
        String url,
        @JsonAlias("html_url") String htmlUrl,
        @JsonAlias("followers_url") String followersUrl,
        @JsonAlias("following_url") String followingUrl,
        @JsonAlias("gists_url") String gistsUrl,
        @JsonAlias("starred_url") String starredUrl,
        @JsonAlias("subscriptions_url") String subscriptionsUrl,
        @JsonAlias("organizations_url") String organizationsUrl,
        @JsonAlias("repos_url") String reposUrl,
        @JsonAlias("events_url") String eventsUrl,
        @JsonAlias("received_events_url") String receivedEventsUrl,
        String type,
        @JsonAlias("site_admin") boolean siteAdmin,
        @JsonAlias("starred_at") String starredAt
) {
}
