package com.amee.calculation.service;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * A service providing access to the Train Route-finder API.
 */
class TrainRouteFinderService implements Service {

    private final Log log = LogFactory.getLog(getClass());

    // Retry and timeout parameters. Calls to the train service API exceeding these tolerances will
    // result in exceptions being thrown out to the calling code.
    private static final int API_TIMEOUT = 5000;
    private static final int API_RETRIES = 5;
    private static final HttpParams httpParams = new BasicHttpParams();

    static {
        httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, API_TIMEOUT);
    }

    private String serviceEndPoint;
    private ProfileFinder profileFinder;
    private Map<String, Object> values;

    // Custom retry handler configured with the defined tolerances.
    private static HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler() {
        private final Log log = LogFactory.getLog(getClass());

        public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
            log.warn("retryRequest - " + exception.getMessage() + ", executionCount: " + executionCount);
            if (executionCount >= API_RETRIES) {
                // Do not retry if over max retry count
                return false;
            } else {
                return true;
            }
        }
    };

    public TrainRouteFinderService(String serviceEndPoint) {
        this.serviceEndPoint = serviceEndPoint;
    }

    /**
     * Invoke the Train Route-finder service API.
     * <p/>
     * Note: if station1 or station2 are null or empty, a value (distance) of 0.0 is returned.
     * Ideally, i'd like to throw an IllegalArgumentException on empty strings, however i can't enforce these
     * validations as PI creation without required values is the normal use-case via the web UI and is
     * certainly not mandated in the API docs.
     * Example:
     * {@code throw new IllegalArgumentException("Invalid station: station1=" + station1 + ", station2=" + station2);}
     *
     * @return the total distance in metres returned for a given route. The start and end points of the route
     *         are defined by the station1 and station2 parameters respectively.
     * @throws IllegalArgumentException - thrown if the values of station1 or station2 do not produce a valid route.
     * @throws CalculationException     - thrown if the call to the train route-finder API failed or returned an un-parsable
     *                                  response
     */
    public String invoke() throws IllegalArgumentException, CalculationException {

        String station1 = (String) values.get("station1");
        String station2 = (String) values.get("station2");

        if (StringUtils.isBlank(station1) || StringUtils.isBlank(station2)) {
            // Removed reset of setLegDetail - as a performance tuning we are
            // not returning leg details from train route api for the moment - SM (10/2009)
            // setLegDetail(null);
            return "0.0";
        }

        try {
            // TODO: Move the hard-coded parameters below to wrapper.conf.
            // Generate the query string
            StringBuilder url = new StringBuilder(serviceEndPoint);
            url.append("?q=");
            StringBuilder q = new StringBuilder();
            q.append(station1);
            q.append(" to ");
            q.append(station2);
            url.append(URLEncoder.encode(q.toString(), "UTF-8"));
            url.append("&s=JSON&gp=0&gs=0&gd=1");

            // Make the request
            DefaultHttpClient httpclient = new DefaultHttpClient();
            httpclient.setHttpRequestRetryHandler(retryHandler);
            httpclient.setParams(httpParams);
            HttpGet httpget = new HttpGet(url.toString());
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            // Parse the response
            String jsonString = EntityUtils.toString(entity);
            int start = jsonString.indexOf("{");
            int end = jsonString.lastIndexOf("}");
            jsonString = jsonString.substring(start, end + 1);
            String totalDistance = parseResponse(jsonString);
            log.debug("invoke() - calculated distance(m) " + totalDistance + " from " + station1 + " to " + station2);
            return totalDistance;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unable to generate a valid route for station1=" + station1 + " and station2=" + station2);
        } catch (IOException e) {
            handleInvokeException(e);
            throw new CalculationException(e.getMessage());
        } catch (ParseException e) {
            handleInvokeException(e);
            throw new CalculationException(e.getMessage());
        } catch (JSONException e) {
            handleInvokeException(e);
            throw new CalculationException(e.getMessage());
        } catch (CalculationException e) {
            handleInvokeException(e);
            throw new CalculationException(e.getMessage());
        }
    }

    /**
     * Handling for Exceptions caught by invoke.
     *
     * @param e Exception
     */
    private void handleInvokeException(Exception e) {
        log.warn("invoke()", e);
        // Removed reset of setLegDetail - as a performance tuning we are
        // not returning leg details from train route api for the moment - SM (10/2009)
        // setLegDetail(null);
    }

    // Parse the JSON string response returned from the API call
    private String parseResponse(String jsonString) throws JSONException,
            IllegalArgumentException, CalculationException {

        JSONObject json = new JSONObject(jsonString);
        int errorCode = json.getInt("error");
        String errorString = json.getString("error_str");
        if (errorCode != 200) {
            // These are Google Maps API error codes. Assuming that root cause is invalid parameters.
            if (errorCode > 600) {
                log.warn("parseResponse() - Error status returned by Train Route API: " + errorString);
                throw new IllegalArgumentException();
            } else {
                throw new CalculationException(errorString, errorCode);
            }
        }

        JSONObject route = json.getJSONArray("Routes").getJSONObject(0);
        // Removed setting of setLegDetail - as a performance tuning we are.
        // not returning leg details from train route api for the moment - SM (10/2009)
        // setLegDetail(route.getJSONArray("Steps").toString());
        return route.getJSONObject("Distance").getString("meters");
    }

    private void setLegDetail(String legDetail) {
        profileFinder.setProfileItemValue("legDetail", legDetail);
    }

    /**
     * Set the Map of values containing valid values for the parameters "station1" and "station2".
     *
     * @param values - the Map of values, normally those passed to the algorithm calling this service.
     */
    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    /**
     * Set the {@link com.amee.calculation.service.ProfileFinder} instance for the calling
     * {@link com.amee.domain.profile.Profile}.
     * This will be used to set into the Profile values returned from the Train Service API.
     *
     * @param profileFinder - the {@link com.amee.calculation.service.ProfileFinder}
     *                      instance for the calling {@link com.amee.domain.profile.Profile}.
     */
    public void setProfileFinder(ProfileFinder profileFinder) {
        this.profileFinder = profileFinder;
    }
}
