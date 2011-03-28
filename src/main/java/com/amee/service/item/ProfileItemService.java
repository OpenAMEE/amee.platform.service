package com.amee.service.item;

import com.amee.base.transaction.TransactionController;
import com.amee.domain.*;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.profile.BaseProfileItemValue;
import com.amee.domain.item.profile.ProfileItem;
import com.amee.domain.item.profile.ProfileItemNumberValue;
import com.amee.domain.item.profile.ProfileItemTextValue;
import com.amee.domain.profile.Profile;
import com.amee.platform.science.StartEndDate;
import com.amee.service.profile.OnlyActiveProfileService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProfileItemService extends ItemService implements IProfileItemService {

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

    @Override
    public boolean equivalentProfileItemExists(ProfileItem profileItem) {
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
}