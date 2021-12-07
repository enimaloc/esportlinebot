package fr.enimaloc.esportlinebot.exception;

public class MissingEnvironmentVariableError extends Error {
    public MissingEnvironmentVariableError() {
    }

    public MissingEnvironmentVariableError(String message) {
        super(message);
    }

    public MissingEnvironmentVariableError(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingEnvironmentVariableError(Throwable cause) {
        super(cause);
    }
}
