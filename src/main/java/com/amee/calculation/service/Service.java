/*
 * This file is part of AMEE.
 *
 * Copyright (c) 2007, 2008, 2009 AMEE UK LIMITED (help@amee.com).
 *
 * AMEE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * AMEE is free software and is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Created by http://www.dgen.net.
 * Website http://www.amee.cc
 */
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
