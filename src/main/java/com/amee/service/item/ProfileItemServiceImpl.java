package com.amee.service.item;

import com.amee.base.domain.ResultsWrapper;
import com.amee.base.transaction.TransactionController;
import com.amee.calculation.service.CalculationService;
import com.amee.domain.*;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.data.ItemValueMap;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.NumberValue;
import com.amee.domain.item.profile.BaseProfileItemValue;
import com.amee.domain.item.profile.ProfileItem;
import com.amee.domain.item.profile.ProfileItemNumberValue;
import com.amee.domain.item.profile.ProfileItemTextValue;
import com.amee.domain.profile.Profile;
import com.amee.platform.science.ExternalNumberValue;
import com.amee.platform.science.ReturnValue;
import com.amee.platform.science.StartEndDate;
import com.amee.service.profile.OnlyActiveProfileService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@Service
public class ProfileItemServiceImpl extends AbstractItemService implements ProfileItemService {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private TransactionController transactionController;

    @Autowired
    private DataItemService dataItemService;

    @Autowired
    private ProfileItemServiceDAO dao;

    @Autowired
    private OnlyActiveProfileService onlyActiveProfileService;

    @Autowired
    private AMEEStatistics ameeStatistics;

    @Autowired
    private CalculationService calculationService;

    @Override
    public ProfileItem getItemByUid(String uid) {
        ProfileItem profileItem = dao.getItemByUid(uid);
        // If this ProfileItem is trashed then return null. A ProfileItem may be trash if it itself has been
        // trashed or an owning entity has been trashed.
        if ((profileItem != null) && (!profileItem.isTrash())) {
            checkProfileItem(profileItem);
            return profileItem;
        } else {
            return null;
        }
    }

    /**
     * Checks if the given {@link ProfileItem} has any non-zero values with a time-based perUnit (eg year).
     *
     * @param profileItem the ProfileItem to test.
     * @return true if any ProfileItem value is non-zero and has a time-based perUnit.
     */
    @Override
    public boolean hasNonZeroPerTimeValues(ProfileItem profileItem) {
        for (BaseItemValue value : getItemValues(profileItem)) {

            // Only ProfileItemNumberValues have units and perUnits.
            if (ProfileItemNumberValue.class.isAssignableFrom(value.getClass()) &&
                    isNonZeroPerTimeValue((ProfileItemNumberValue) value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given value is non-zero and has a time-based perUnit (eg year).
     *
     * @param value the BaseItemValue to check.
     * @return true if the given value is non-zero and has a time-based perUnit.
     */
    @Override
    public boolean isNonZeroPerTimeValue(ProfileItemNumberValue value) {
        return value.hasPerTimeUnit() && value.isNonZero();
    }

    //TODO - TEMP HACK - will remove as soon we decide how to handle return units in V1 correctly.

    @Override
    public boolean isSingleFlight(ProfileItem profileItem) {
        for (BaseItemValue iv : getItemValues(profileItem)) {
            if ((iv.getName().startsWith("IATA") && iv.getValueAsString().length() > 0) ||
                    (iv.getName().startsWith("Lat") && !iv.getValueAsString().equals("-999")) ||
                    (iv.getName().startsWith("Lon") && !iv.getValueAsString().equals("-999"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get an {@link BaseItemValue} belonging to this Item using some identifier and prevailing datetime context.
     *
     * @param identifier - a value to be compared to the path and then the uid of the {@link BaseItemValue}s belonging
     *                   to this Item.
     * @return the matched {@link BaseItemValue} or NULL if no match is found.
     */
    @Override
    public BaseItemValue getItemValue(BaseItem item, String identifier) {
        if (!ProfileItem.class.isAssignableFrom(item.getClass()))
            throw new IllegalStateException("A ProfileItem instance was expected.");
        return getItemValue(item, identifier, item.getEffectiveStartDate());
    }

    @Override
    public int getProfileItemCount(Profile profile, DataCategory dataCategory) {
        return dao.getProfileItemCount(profile, dataCategory);
    }

    @Override
    public ResultsWrapper<ProfileItem> getProfileItems(Profile profile, ProfileItemsFilter filter) {
        ResultsWrapper<ProfileItem> resultsWrapper = dao.getProfileItems(profile, filter);
        List<ProfileItem> profileItems = new ArrayList<ProfileItem>(resultsWrapper.getResults());
        
        if ("prorata".equals(filter.getMode())) {
            List<ProfileItem> prorata =
                prorataProfileItems(profileItems, filter.getStartDate(), filter.getEndDate());
            resultsWrapper.setResults(prorata);
        }
        
        return resultsWrapper;
    }

    /**
     * Retrieve a list of {@link com.amee.domain.item.profile.ProfileItem}s belonging to a {@link Profile} and {@link DataCategory}
     * occuring on or immediately proceeding the given date context.
     *
     * @param profile      - the {@link Profile} to which the {@link com.amee.domain.item.profile.ProfileItem}s belong
     * @param dataCategory - the DataCategory containing the ProfileItems
     * @param profileDate  - the date context
     * @return the active {@link com.amee.domain.item.profile.ProfileItem} collection
     */
    public List<ProfileItem> getProfileItems(
            Profile profile,
            IDataCategoryReference dataCategory,
            Date profileDate) {
        List<ProfileItem> profileItems = dao.getProfileItems(profile, dataCategory, profileDate);
        loadItemValuesForItems((List) profileItems);
        // Order the returned collection by pi.name, di.name and pi.startDate DESC
        Collections.sort(profileItems, new Comparator<ProfileItem>() {
            public int compare(ProfileItem p1, ProfileItem p2) {
                int nd = p1.getName().compareTo(p2.getName());
                int dnd = p1.getDataItem().getName().compareTo(p2.getDataItem().getName());
                int sdd = p2.getStartDate().compareTo(p1.getStartDate());
                if (nd != 0) return nd;
                if (dnd != 0) return dnd;
                if (sdd != 0) return sdd;
                return 0;
            }
        });
        return checkProfileItems(onlyActiveProfileService.getProfileItems(profileItems));
    }

    /**
     * Retrieve a list of {@link com.amee.domain.item.profile.ProfileItem}s belonging to a {@link Profile} and {@link DataCategory}
     * occurring between a given date context.
     *
     * @param profile      - the {@link Profile} to which the {@link com.amee.domain.item.profile.ProfileItem}s belong
     * @param dataCategory - the DataCategory containing the ProfileItems
     * @param startDate    - the start of the date context
     * @param endDate      - the end of the date context
     * @return the active {@link com.amee.domain.item.profile.ProfileItem} collection
     */
    public List<ProfileItem> getProfileItems(
            Profile profile,
            IDataCategoryReference dataCategory,
            StartEndDate startDate,
            StartEndDate endDate) {
        List<ProfileItem> profileItems = dao.getProfileItems(profile, dataCategory, startDate, endDate);
        loadItemValuesForItems((List) profileItems);
        // Order the returned collection by pi.startDate DESC
        Collections.sort(profileItems, new Comparator<ProfileItem>() {
            public int compare(ProfileItem p1, ProfileItem p2) {
                return p2.getStartDate().compareTo(p1.getStartDate());
            }
        });
        return checkProfileItems(profileItems);
    }

    private List<ProfileItem> checkProfileItems(List<ProfileItem> profileItems) {
        if (log.isDebugEnabled()) {
            log.debug("checkProfileItems() start");
        }
        if (profileItems == null) {
            return null;
        }
        List<ProfileItem> activeProfileItems = new ArrayList<ProfileItem>();

        // Remove any trashed ProfileItems
        for (ProfileItem profileItem : profileItems) {
            if (!profileItem.isTrash()) {
                checkProfileItem(profileItem);
                activeProfileItems.add(profileItem);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("checkProfileItems() done (" + activeProfileItems.size() + ")");
        }
        return activeProfileItems;
    }


    /**
     * Add to the {@link com.amee.domain.item.profile.ProfileItem} any {@link com.amee.domain.item.profile.BaseProfileItemValue}s it is missing.
     * This will be the case on first persist (this method acting as a reification function), and between GETs if any
     * new {@link com.amee.domain.data.ItemValueDefinition}s have been added to the underlying
     * {@link com.amee.domain.data.ItemDefinition}.
     * <p/>
     * Any updates to the {@link com.amee.domain.item.profile.ProfileItem} will be persisted to the database.
     *
     * @param profileItem to check
     * @return the supplied ProfileItem or null
     */
    private ProfileItem checkProfileItem(ProfileItem profileItem) {

        if (profileItem == null) {
            return null;
        }

        // Get APIVersion via Profile & User.
        APIVersion apiVersion = profileItem.getProfile().getUser().getAPIVersion();

        // APIVersion apiVersion = profileItem.getProfile().getAPIVersion();
        Set<ItemValueDefinition> existingItemValueDefinitions = this.getItemValueDefinitionsInUse(profileItem);
        Set<ItemValueDefinition> missingItemValueDefinitions = new HashSet<ItemValueDefinition>();

        // find ItemValueDefinitions not currently implemented in this Item
        for (ItemValueDefinition ivd : profileItem.getItemDefinition().getItemValueDefinitions()) {
            if (ivd.isFromProfile() && ivd.getApiVersions().contains(apiVersion)) {
                if (!existingItemValueDefinitions.contains(ivd)) {
                    missingItemValueDefinitions.add(ivd);
                }
            }
        }

        // Do we need to add any ItemValueDefinitions?
        if (missingItemValueDefinitions.size() > 0) {

            // Ensure a transaction has been opened. The implementation of open-session-in-view we are using
            // does not open transactions for GETs. This method is called for certain GETs.
            transactionController.begin(true);

            // create missing ItemValues
            for (ItemValueDefinition ivd : missingItemValueDefinitions) {
                // start default value with value from ItemValueDefinition
                String defaultValue = ivd.getValue();
                // next give DataItem a chance to set the default value, if appropriate
                if (ivd.isFromData()) {
                    BaseItemValue dataItemValue =
                            dataItemService.getItemValue(profileItem.getDataItem(), ivd.getPath(), profileItem.getStartDate());
                    if ((dataItemValue != null) && (dataItemValue.getValueAsString().length() > 0)) {
                        defaultValue = dataItemValue.getValueAsString();
                    }
                }
                // create missing ItemValue
                BaseProfileItemValue profileItemValue;
                if (ivd.getValueDefinition().getValueType().equals(ValueType.INTEGER) ||
                        ivd.getValueDefinition().getValueType().equals(ValueType.DOUBLE)) {
                    // Item is a number.
                    profileItemValue = new ProfileItemNumberValue(ivd, profileItem, defaultValue);
                } else {
                    // Item is text.
                    profileItemValue = new ProfileItemTextValue(ivd, profileItem, defaultValue);
                }
                persist(profileItemValue);
                ameeStatistics.createProfileItemValue();
            }

            // Clear cache.
            clearItemValues();
        }

        return profileItem;
    }

    @Override
    public boolean isUnique(ProfileItem pi) {
        return !equivalentProfileItemExists(pi);
    }

    private boolean equivalentProfileItemExists(ProfileItem profileItem) {
        return dao.equivalentProfileItemExists(profileItem);
    }

    @Override
    public Collection<Long> getProfileDataCategoryIds(Profile profile) {
        return dao.getProfileDataCategoryIds(profile);
    }

    @Override
    public void persist(ProfileItem profileItem) {
        dao.persist(profileItem);
        checkProfileItem(profileItem);
    }

    @Override
    public void remove(ProfileItem profileItem) {
        profileItem.setStatus(AMEEStatus.TRASH);
    }

    // ItemValues.

    @Override
    public void persist(BaseItemValue itemValue) {
        dao.persist(itemValue);
    }

    @Override
    public void remove(BaseItemValue itemValue) {
        itemValue.setStatus(AMEEStatus.TRASH);
    }

    @Override
    protected ProfileItemServiceDAO getDao() {
        return dao;
    }

    /**
     * Updates the Profile Item Values for the supplied ProfileItem based on the properties of the values
     * bean within the ProfileItem. Internally uses the Spring and Java beans API to access values in the
     * CGLIB created ProfileItem.values JavaBean.
     * <p/>
     * If a Profile Item Value is modified then the Profile Item is also marked as modified.
     *
     * @param profileItem the ProfileItem to update.
     */
    @Override
    public void updateProfileItemValues(ProfileItem profileItem) {
        boolean modified = false;
        Object values = profileItem.getValues();
        Object units = profileItem.getUnits();
        Object perUnits = profileItem.getPerUnits();
        ItemValueMap itemValues = getItemValuesMap(profileItem);
        for (String key : itemValues.keySet()) {
            BaseItemValue value = itemValues.get(key);

            // Values
            PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(values.getClass(), key);
            if (pd != null) {
                Method readMethod = pd.getReadMethod();
                if (readMethod != null) {
                    try {
                        Object v = readMethod.invoke(values);
                        if (v != null) {
                            value.setValue(v.toString());
                            modified = true;
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Caught IllegalAccessException: " + e.getMessage(), e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException("Caught InvocationTargetException: " + e.getMessage(), e);
                    }
                } else {
                    log.warn("updateProfileItemValues() Read Method was null: " + key);
                }
            } else {
                log.warn("updateProfileItemValues() PropertyDescriptor was null: " + key);
            }

            // Units (only number values have units and perUnits)
            if (ExternalNumberValue.class.isAssignableFrom(value.getClass())) {

                // Unit
                pd = BeanUtils.getPropertyDescriptor(units.getClass(), key);
                if (pd != null) {
                    Method readMethod = pd.getReadMethod();
                    if (readMethod != null) {
                        try {
                            Object v = readMethod.invoke(units);
                            if (v != null) {
                                ((ProfileItemNumberValue) value).setUnit(v.toString());
                                modified = true;
                            }
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Caught IllegalAccessException: " + e.getMessage(), e);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException("Caught InvocationTargetException: " + e.getMessage(), e);
                        }
                    } else {
                        log.warn("updateProfileItemValues() Read Method was null: " + key);
                    }
                } else {
                    log.warn("updateProfileItemValues() PropertyDescriptor was null: " + key);
                }

                // Per Unit
                pd = BeanUtils.getPropertyDescriptor(perUnits.getClass(), key);
                if (pd != null) {
                    Method readMethod = pd.getReadMethod();
                    if (readMethod != null) {
                        try {
                            Object v = readMethod.invoke(perUnits);
                            if (v != null) {
                                ((ProfileItemNumberValue) value).setPerUnit(v.toString());
                                modified = true;
                            }
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Caught IllegalAccessException: " + e.getMessage(), e);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException("Caught InvocationTargetException: " + e.getMessage(), e);
                        }
                    } else {
                        log.warn("updateProfileItemValues() Read Method was null: " + key);
                    }
                } else {
                    log.warn("updateProfileItemValues() PropertyDescriptor was null: " + key);
                }
            }
        }

        // Mark the profile item as modified if values were modified.
        if (modified) {
            profileItem.onModify();
        }
    }
    
    private List<ProfileItem> prorataProfileItems(List<ProfileItem> profileItems, Date startDate, Date endDate) {
        log.debug("prorataProfileItems() start");

        // The profile items with values adjusted (if required) for prorata.
        List<ProfileItem> requestedItems = new ArrayList<ProfileItem>();

        // The requested interval to prorata the values over.
        Interval requestInterval = getInterval(startDate, endDate);

        for (ProfileItem pi : profileItems) {

            // Update ProfileItem with start and end dates.
            pi.setEffectiveStartDate(startDate);
            pi.setEffectiveEndDate(endDate);

            if (log.isDebugEnabled()) {
                log.debug("prorataProfileItems() - ProfileItem: " + pi.getName() + " has un-prorated Amounts: " + pi.getAmounts());
            }

            // Find the intersection of the profile item with the requested window.
            Interval intersect = requestInterval;
            if (intersect.getStart().toDate().before(pi.getStartDate())) {
                intersect = intersect.withStartMillis(pi.getStartDate().getTime());
            }
            if ((pi.getEndDate() != null) && pi.getEndDate().before(intersect.getEnd().toDate())) {
                intersect = intersect.withEndMillis(pi.getEndDate().getTime());
            }

            if (log.isDebugEnabled()) {
                log.debug("prorataProfileItems() - request interval: " + requestInterval + ", intersect:" + intersect);
            }

            if (hasNonZeroPerTimeValues(pi)) {

                // The ProfileItem has perTime ItemValues. In this case, the ItemValues are multiplied by
                // the (intersect/PerTime) ratio and the CO2 value recalculated.

                if (log.isDebugEnabled()) {
                    log.debug("prorataProfileItems() - ProfileItem: " + pi.getName() + " has PerTime ItemValues.");
                }

                for (BaseItemValue iv : getItemValues(pi)) {

                    // TODO: Extract method?
                    if (ProfileItemNumberValue.class.isAssignableFrom(iv.getClass()) &&
                        isNonZeroPerTimeValue((ProfileItemNumberValue) iv) &&
                        iv.getItemValueDefinition().isFromProfile()) {
                        double proratedItemValue = getProRatedItemValue(intersect, (ProfileItemNumberValue)iv);
                        if (log.isDebugEnabled()) {
                            log.debug("prorataProfileItems() - ProfileItem: " + pi.getName() +
                                ". ItemValue: " + iv.getName() + " = " + iv.getValueAsString() +
                                " has PerUnit: " + ((ProfileItemNumberValue)iv).getPerUnit() +
                                ". Pro-rated ItemValue = " + proratedItemValue);
                        }

                        // Set the override value (which will not be persisted)
                        ((ProfileItemNumberValue)iv).setValueOverride(proratedItemValue);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("prorataProfileItems() - ProfileItem: " + pi.getName() +
                                ". Unchanged ItemValue: " + iv.getName());
                        }
                    }
                }

                // Perform the calculation using the prorated values.
                calculationService.calculate(pi);

                if (log.isDebugEnabled()) {
                    log.debug("prorataProfileItems() - ProfileItem: " + pi.getName() + ". Adding prorated Amounts: " + pi.getAmounts());
                }

                requestedItems.add(pi);
            } else if (pi.getEndDate() != null) {

                // The ProfileItem has no perTime ItemValues and is bounded.
                // In this case, the CO2 value is multiplied by the (intersection/item duration) ratio.

                // TODO - make Item a deep copy (and so inc. ItemValues). Will need to implement equals() in ItemValue
                // TODO - such that overwriting in the ItemValue collection is handled correctly.

                long itemDurationInMillis = getInterval(pi.getStartDate(), pi.getEndDate()).toDurationMillis();
                double eventIntersectRatio = intersect.toDurationMillis() / (double) itemDurationInMillis;

                // Iterate over the return values and for each amount, store the prorated value
                for (Map.Entry<String, ReturnValue> entry : pi.getAmounts().getReturnValues().entrySet()) {
                    String type = entry.getKey();
                    ReturnValue value = entry.getValue();
                    double proRatedValue = value.getValue() * eventIntersectRatio;
                    pi.getAmounts().putValue(type, value.getUnit(), value.getPerUnit(), proRatedValue);
                }

                if (log.isDebugEnabled()) {
                    log.debug("prorataProfileItems() - ProfileItem: " + pi.getName() +
                        " is bounded (" + getInterval(pi.getStartDate(), pi.getEndDate()) +
                        ") and has no PerTime ItemValues.");
                    log.debug("prorataProfileItems() - Adding pro-rated Amounts: " + pi.getAmounts());
                }
                requestedItems.add(pi);
            } else {

                // The ProfileItem has no perTime ItemValues and is unbounded.
                // In this case, the ReturnValues are not prorated.

                if (log.isDebugEnabled()) {
                    log.debug("prorataProfileItems() - ProfileItem: " + pi.getName() +
                        " is unbounded and has no PerTime ItemValues. Adding un-prorated Amounts: " + pi.getAmounts());
                }
                requestedItems.add(pi);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("prorataProfileItems() done (" + requestedItems.size() + ")");
        }

        return requestedItems;
    }

    // endDate may be null. See: com.amee.service.BaseBrowser.getQueryEndDate().
    private Interval getInterval(Date startDate, Date endDate) {
        DateTime start = new DateTime(startDate.getTime());
        DateTime end;
        if (endDate != null) {
            end = new DateTime(endDate.getTime());
        } else {
            end = new DateTime();
        }
        return new Interval(start, end);
    }

    private double getProRatedItemValue(Interval interval, NumberValue itemValue) {

        // The ProfileItemNumberValue will always have a time based per unit.
        @SuppressWarnings(value = "unchecked")
        Measure<Integer, Duration> measure = Measure.valueOf(1, ((Unit<Duration>)itemValue.getPerUnit().toUnit()));

        double perTime = measure.doubleValue(SI.MILLI(SI.SECOND));
        double intersectPerTimeRatio = (interval.toDurationMillis()) / perTime;
        return itemValue.getValueAsDouble() * intersectPerTimeRatio;
    }
}