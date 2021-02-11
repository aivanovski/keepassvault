package com.ivanovsky.passnotes.domain.entity

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.entity.filter.*

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
            filters.add(VisiblePropertiesStrategy())
            return this
        }

        fun hidden(): Builder {
            filters.add(HiddenPropertiesStrategy())
            return this
        }

        fun notEmpty(): Builder {
            filters.add(NotEmptyPropertiesStrategy())
            return this
        }

        fun excludeTitle(): Builder {
            filters.add(ExcludeTitleStrategy())
            return this
        }

        fun sortedByType(): Builder {
            filters.add(SortedByTypeStrategy())
            return this
        }

        fun build(): PropertyFilter {
            return PropertyFilter(filters)
        }
    }
}