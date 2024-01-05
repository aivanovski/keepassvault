package com.ivanovsky.passnotes.injection.modules

import com.ivanovsky.passnotes.domain.usecases.AddTemplatesUseCase
import com.ivanovsky.passnotes.domain.usecases.CheckNoteAutofillDataUseCase
import com.ivanovsky.passnotes.domain.usecases.DecodePasswordWithBiometricUseCase
import com.ivanovsky.passnotes.domain.usecases.EncodePasswordWithBiometricUseCase
import com.ivanovsky.passnotes.domain.usecases.FindNotesForAutofillUseCase
import com.ivanovsky.passnotes.domain.usecases.GeneratePasswordUseCase
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.GetGroupUseCase
import com.ivanovsky.passnotes.domain.usecases.GetNoteUseCase
import com.ivanovsky.passnotes.domain.usecases.GetRecentlyOpenedFilesUseCase
import com.ivanovsky.passnotes.domain.usecases.GetUsedFileUseCase
import com.ivanovsky.passnotes.domain.usecases.IsDatabaseOpenedUseCase
import com.ivanovsky.passnotes.domain.usecases.LockDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.MoveGroupUseCase
import com.ivanovsky.passnotes.domain.usecases.MoveNoteUseCase
import com.ivanovsky.passnotes.domain.usecases.RemoveBiometricDataUseCase
import com.ivanovsky.passnotes.domain.usecases.RemoveUsedFileUseCase
import com.ivanovsky.passnotes.domain.usecases.SortGroupsAndNotesUseCase
import com.ivanovsky.passnotes.domain.usecases.SyncUseCases
import com.ivanovsky.passnotes.domain.usecases.UpdateNoteUseCase
import com.ivanovsky.passnotes.domain.usecases.UpdateNoteWithAutofillDataUseCase
import com.ivanovsky.passnotes.domain.usecases.UpdateUsedFileUseCase
import com.ivanovsky.passnotes.domain.usecases.test.GetTestCredentialsUseCase
import com.ivanovsky.passnotes.domain.usecases.test.GetTestPasswordUseCase
import org.koin.dsl.module

object UseCaseModule {

    fun build() =
        module {
            single { GetTestCredentialsUseCase(get()) }
            single { GetTestPasswordUseCase(get(), get()) }
            single { LockDatabaseUseCase() }
            single { GetRecentlyOpenedFilesUseCase(get(), get()) }
            single { SyncUseCases(get(), get(), get()) }
            single { AddTemplatesUseCase(get(), get(), get()) }
            single { GetDatabaseUseCase(get(), get()) }
            single { MoveNoteUseCase(get(), get(), get()) }
            single { MoveGroupUseCase(get(), get(), get()) }
            single { GetGroupUseCase(get(), get()) }
            single { IsDatabaseOpenedUseCase(get()) }
            single { GetNoteUseCase(get(), get()) }
            single { FindNotesForAutofillUseCase(get(), get()) }
            single { UpdateNoteWithAutofillDataUseCase(get(), get(), get()) }
            single { CheckNoteAutofillDataUseCase(get()) }
            single { UpdateNoteUseCase(get(), get(), get()) }
            single { RemoveUsedFileUseCase(get(), get()) }
            single { GetUsedFileUseCase(get(), get()) }
            single { UpdateUsedFileUseCase(get(), get()) }
            single { SortGroupsAndNotesUseCase(get(), get()) }
            single { GeneratePasswordUseCase() }
            single { RemoveBiometricDataUseCase(get(), get(), get()) }
            single { DecodePasswordWithBiometricUseCase(get()) }
            single { EncodePasswordWithBiometricUseCase(get()) }
        }
}