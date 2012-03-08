package com.amee.service.data;

import com.amee.domain.IDataCategoryReference;
import com.amee.domain.sheet.Choice;
import com.amee.domain.sheet.Choices;

import java.util.List;

public interface DrillDownService {

    /**
     * Gets the next available drill down choices for the given data category and drill down selections.
     *
     * @param dc the Datacategory to get the drill downs for.
     * @param selections the selected drill down choices.
     * @return a Choices object containing the next available drill down choices.
     */
    Choices getChoices(IDataCategoryReference dc, List<Choice> selections);

    void clearDrillDownCache();
}
