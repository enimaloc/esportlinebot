package fr.enimaloc.esportline.api.github;

import com.fasterxml.jackson.annotation.JsonAlias;

public record GithubReactions(
        String url,
        @JsonAlias("total_count") int totalCount,
        @JsonAlias("+1") int plusOne,
        @JsonAlias("-1") int minusOne,
        int laugh,
        int confused,
        int heart,
        int hooray,
        int eyes,
        int rocket
) {
}
