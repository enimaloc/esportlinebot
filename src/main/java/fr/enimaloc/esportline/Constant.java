package fr.enimaloc.esportline;

public class Constant {
    Constant() {
        throw new IllegalStateException("Constant class");
    }

    public static class Github {
        Github() {
            throw new IllegalStateException("Constant.Github class");
        }

        public static final String BASE_URL = "https://api.github.com/";
        public static final String BASE_REPO_URL = BASE_URL + "repos/%s/%s/";
        public static final String LATEST_RELEASE = BASE_REPO_URL + "releases/latest";
    }
}
