package com.amee.calculation.service;

import java.util.Map;

/**
 * A service interface for Algorithm access to external APIs or services.
 */
public interface Service {

    /**
     * Invoke the service.
     * <p/>
     * The service will return some string representation of the result and may also set results into the
     * passed-in Map of values and ProfileFinder.
     *
     * @return the string representation of the service result
     */
    String invoke() throws CalculationException;

    /**
     * Set a Map of values which contains parameters required for calling the external API or service.
     *
     * @param values - the Map of values
     */
    void setValues(Map<String, Object> values);

    /**
     * Set the {@link com.amee.calculation.service.ProfileFinder} instance for the calling
     * {@link com.amee.domain.profile.Profile}.
     * This will be used to set into the Profile values returned from the remote service.
     *
     * @param profileFinder - the {@link com.amee.calculation.service.ProfileFinder}
     *                      instance for the calling {@link com.amee.domain.profile.Profile}.
     */
    void setProfileFinder(ProfileFinder profileFinder);
}
