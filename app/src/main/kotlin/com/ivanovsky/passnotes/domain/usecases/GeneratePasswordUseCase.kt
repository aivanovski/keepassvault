package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.domain.PasswordBuilder
import com.ivanovsky.passnotes.domain.entity.PasswordResource
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class GeneratePasswordUseCase {

    fun generate(length: Int, resources: List<PasswordResource>): String {
        if (resources.isEmpty() ||
            (resources.size == 1 && resources.contains(PasswordResource.SPACE))) {
            return EMPTY
        }

        return PasswordBuilder(length, resources)
            .restrictSpacesAtStartAndEnd()
            .generateOneCharFromEachResource()
            .generateOtherChars()
            .build()
    }
}