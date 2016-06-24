package model.builder.web.api

/**
 * Defines SDP REST service interface to un-authenticated endpoints.
 */
interface OpenAPI {

    /**
     * Retrieve the api keys associated with a user's email address.
     * <p>
     * The email input should be lower-cased in implementations of this method
     * to match SDP expectations.
     *
     * @param email {@link String} email address
     * @return {@link WebResponse} from the SDP
     */
    WebResponse apiKeys(String email)
}