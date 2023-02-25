package com.ivanovsky.passnotes.domain.entity

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.Property.Companion.PROPERTY_NAME_AUTOFILL_APP_ID
import com.ivanovsky.passnotes.data.entity.Property.Companion.PROPERTY_NAME_TEMPLATE_UID
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.entity.filter.*

// TODO: In some scenarios strategies with including and excluding logic can't be mixed
//  in one filter, so there should be a way to avoid it
class PropertyFilter private constructor(
    private val filters: List<PropertyFilterStrategy>
) {

    fun apply(properties: List<Property>): List<Property> {
        var sequence = properties.asSequence()

        for (filter in filters) {
            sequence = filter.apply(sequence)
        }

        return sequence.toList()
    }

    class Builder {

        private val filters = mutableListOf<PropertyFilterStrategy>()

        fun visible(): Builder {
            filters.add(FilterVisibleStrategy())
            return this
        }

        fun hidden(): Builder {
            filters.add(FilterHiddenStrategy())
            return this
        }

        fun notEmpty(): Builder {
            filters.add(FilterNotEmptyPropertiesStrategy())
            return this
        }

        fun notEmptyName(): Builder {
            filters.add(FilterNotEmptyNameStrategy())
            return this
        }

        fun excludeTitle(): Builder {
            filters.add(ExcludeTitleStrategy())
            return this
        }

        fun filterTitle(): Builder {
            filters.add(FilterTitleStrategy())
            return this
        }

        fun sortedByType(): Builder {
            filters.add(SortedByTypeStrategy())
            return this
        }

        fun filterByName(name: String): Builder {
            filters.add(FilterByNameStrategy(name))
            return this
        }

        fun filterByType(vararg types: PropertyType): Builder {
            filters.add(FilterByTypeStrategy(*types))
            return this
        }

        fun excludeByName(vararg names: String): Builder {
            filters.add(ExcludeByNameStrategy(*names))
            return this
        }

        fun excludeByType(vararg types: PropertyType): Builder {
            filters.add(ExcludeByTypeStrategy(*types))
            return this
        }

        fun filterTemplateUid(): Builder {
            return filterByName(PROPERTY_NAME_TEMPLATE_UID)
        }

        fun filterAutofillAppId(): Builder {
            return filterByName(PROPERTY_NAME_AUTOFILL_APP_ID)
        }

        fun filterUrl(): Builder {
            return filterByType(PropertyType.URL)
        }

        fun excludeDefaultTypes(): Builder {
            filters.add(FilterCustomTypesStrategy())
            return this
        }

        fun filterDefaultTypes(): Builder {
            filters.add(FilterDefaultTypesStrategy())
            return this
        }

        fun includeNullType(): Builder {
            filters.add(FilterNullTypeStrategy())
            return this
        }

        fun filterTemplateIndicator(): Builder {
            filters.add(
                FilterByNameAndValueStrategy(
                    name = Property.PROPERTY_NAME_TEMPLATE,
                    value = Property.PROPERTY_VALUE_TEMPLATE
                )
            )
            return this
        }

        fun build(): PropertyFilter {
            return PropertyFilter(filters)
        }
    }

    companion object {

        fun filterTitle(properties: List<Property>): Property? =
            INCLUDE_TITLE_FILTER
                .apply(properties)
                .firstOrNull()

        fun filterTemplateUid(properties: List<Property>): Property? =
            TEMPLATE_UID_FILTER
                .apply(properties)
                .firstOrNull()

        fun filterHidden(properties: List<Property>): List<Property> =
            HIDDEN_PROPERTIES_FILTER
                .apply(properties)

        fun filterVisible(properties: List<Property>): List<Property> =
            VISIBLE_PROPERTIES_FILTER
                .apply(properties)

        fun filterTemplateIndicator(properties: List<Property>): Property? =
            TEMPLATE_INDICATOR_FILTER
                .apply(properties)
                .firstOrNull()

        fun filterUserName(properties: List<Property>): Property? =
            USER_NAME_FILTER
                .apply(properties)
                .firstOrNull()

        fun filterPassword(properties: List<Property>): Property? =
            PASSWORD_FILTER
                .apply(properties)
                .firstOrNull()

        private val PASSWORD_FILTER = Builder()
            .filterByType(PropertyType.PASSWORD)
            .build()

        private val USER_NAME_FILTER = Builder()
            .filterByType(PropertyType.USER_NAME)
            .build()

        private val INCLUDE_TITLE_FILTER = Builder()
            .filterTitle()
            .build()

        private val TEMPLATE_UID_FILTER = Builder()
            .filterTemplateUid()
            .build()

        private val HIDDEN_PROPERTIES_FILTER = Builder()
            .hidden()
            .build()

        private val VISIBLE_PROPERTIES_FILTER = Builder()
            .visible()
            .build()

        private val TEMPLATE_INDICATOR_FILTER = Builder()
            .filterTemplateIndicator()
            .build()
    }
}