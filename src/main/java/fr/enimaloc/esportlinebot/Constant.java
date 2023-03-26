/*
 * Constant
 *
 * 0.0.1
 *
 * 28/06/2022
 */
package fr.enimaloc.esportlinebot;

/**
 *
 */
public class Constant {

    public static final boolean DEBUG = System.getenv("DEBUG") != null
            || System.getProperty("DEBUG") != null
            || System.getenv("DEV") != null
            || System.getProperty("DEV") != null;

    Constant() throws IllegalAccessException {
        throw new IllegalAccessException("This class is not meant to be instantiated");
    }

    public static class Discord {

        public static final long DELETED_USER_ID = 456226577798135808L;
        public static final String DELETED_USER_MENTION = "<@" + DELETED_USER_ID + ">";
        public static final String DELETED_USER_NAME = "Deleted User";
        public static final String DELETED_USER_DISCRIMINATOR = "0000";
        public static final String DELETED_USER_TAG = DELETED_USER_NAME + "#" + DELETED_USER_DISCRIMINATOR;
        Discord() throws IllegalAccessException {
            throw new IllegalAccessException("This class is not meant to be instantiated");
        }
    }
}
