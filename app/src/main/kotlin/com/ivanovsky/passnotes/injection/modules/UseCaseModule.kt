package com.ivanovsky.passnotes.injection.modules

import com.ivanovsky.passnotes.domain.usecases.AddTemplatesUseCase
import com.ivanovsky.passnotes.domain.usecases.CheckNoteAutofillDataUseCase
import com.ivanovsky.passnotes.domain.usecases.CleanUpFilesUseCase
import com.ivanovsky.passnotes.domain.usecases.DecodePasswordWithBiometricUseCase
import com.ivanovsky.passnotes.domain.usecases.EncodePasswordWithBiometricUseCase
import com.ivanovsky.passnotes.domain.usecases.ExportDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.FindNoteForAutofillUseCase
import com.ivanovsky.passnotes.domain.usecases.FindParentGroupsUseCase
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
import com.ivanovsky.passnotes.domain.usecases.SearchUseCases
import com.ivanovsky.passnotes.domain.usecases.SetupRecycleBinUseCase
import com.ivanovsky.passnotes.domain.usecases.SortGroupsAndNotesUseCase
import com.ivanovsky.passnotes.domain.usecases.SyncUseCases
import com.ivanovsky.passnotes.domain.usecases.UpdateNoteUseCase
import com.ivanovsky.passnotes.domain.usecases.UpdateNoteWithAutofillDataUseCase
import com.ivanovsky.passnotes.domain.usecases.UpdateUsedFileUseCase
import com.ivanovsky.passnotes.domain.usecases.diff.ApplyDiffUseCase
import com.ivanovsky.passnotes.domain.usecases.diff.GetDiffUseCase
import com.ivanovsky.passnotes.domain.usecases.history.GetHistoryUseCase
import com.ivanovsky.passnotes.domain.usecases.test.GetTestCredentialsUseCase
import com.ivanovsky.passnotes.domain.usecases.test.GetTestPasswordUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

object UseCaseModule {

    fun build() =
        module {
            singleOf(::GetTestCredentialsUseCase)
            singleOf(::GetTestPasswordUseCase)
            singleOf(::LockDatabaseUseCase)
            singleOf(::GetRecentlyOpenedFilesUseCase)
            singleOf(::SyncUseCases)
            singleOf(::AddTemplatesUseCase)
            singleOf(::GetDatabaseUseCase)
            singleOf(::MoveNoteUseCase)
            singleOf(::MoveGroupUseCase)
            singleOf(::GetGroupUseCase)
            singleOf(::IsDatabaseOpenedUseCase)
            singleOf(::GetNoteUseCase)
            singleOf(::FindNoteForAutofillUseCase)
            singleOf(::UpdateNoteWithAutofillDataUseCase)
            singleOf(::CheckNoteAutofillDataUseCase)
            singleOf(::UpdateNoteUseCase)
            singleOf(::RemoveUsedFileUseCase)
            singleOf(::GetUsedFileUseCase)
            singleOf(::UpdateUsedFileUseCase)
            singleOf(::SortGroupsAndNotesUseCase)
            singleOf(::GeneratePasswordUseCase)
            singleOf(::RemoveBiometricDataUseCase)
            singleOf(::DecodePasswordWithBiometricUseCase)
            singleOf(::EncodePasswordWithBiometricUseCase)
            singleOf(::SearchUseCases)
            singleOf(::FindParentGroupsUseCase)
            singleOf(::GetDiffUseCase)
            singleOf(::GetHistoryUseCase)
            singleOf(::ExportDatabaseUseCase)
            singleOf(::SetupRecycleBinUseCase)
            singleOf(::ApplyDiffUseCase)
            singleOf(::CleanUpFilesUseCase)
        }
}