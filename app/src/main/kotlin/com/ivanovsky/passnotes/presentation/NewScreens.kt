package com.ivanovsky.passnotes.presentation

import androidx.fragment.app.Fragment
import com.ivanovsky.passnotes.presentation.about.AboutFragment
import com.ivanovsky.passnotes.presentation.core.navigation.Screen
import com.ivanovsky.passnotes.presentation.debugmenu.DebugMenuFragment
import com.ivanovsky.passnotes.presentation.diffViewer.DiffViewerFragment
import com.ivanovsky.passnotes.presentation.diffViewer.DiffViewerScreenArgs
import com.ivanovsky.passnotes.presentation.enterDbCredentials.EnterDbCredentialsFragment
import com.ivanovsky.passnotes.presentation.enterDbCredentials.EnterDbCredentialsScreenArgs
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerArgs
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerFragment
import com.ivanovsky.passnotes.presentation.groupEditor.GroupEditorArgs
import com.ivanovsky.passnotes.presentation.groupEditor.GroupEditorFragment
import com.ivanovsky.passnotes.presentation.groups.GroupsFragment
import com.ivanovsky.passnotes.presentation.groups.GroupsScreenArgs
import com.ivanovsky.passnotes.presentation.history.HistoryFragment
import com.ivanovsky.passnotes.presentation.history.HistoryScreenArgs
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseFragment
import com.ivanovsky.passnotes.presentation.note.NoteFragment
import com.ivanovsky.passnotes.presentation.note.NoteScreenArgs
import com.ivanovsky.passnotes.presentation.noteEditor.NoteEditorArgs
import com.ivanovsky.passnotes.presentation.noteEditor.NoteEditorFragment
import com.ivanovsky.passnotes.presentation.passwordGenerator.PasswordGeneratorFragment
import com.ivanovsky.passnotes.presentation.serverLogin.ServerLoginArgs
import com.ivanovsky.passnotes.presentation.serverLogin.ServerLoginFragment
import com.ivanovsky.passnotes.presentation.settings.app.AppSettingsFragment
import com.ivanovsky.passnotes.presentation.settings.database.DatabaseSettingsFragment
import com.ivanovsky.passnotes.presentation.settings.main.MainSettingsFragment
import com.ivanovsky.passnotes.presentation.setupOneTimePassword.SetupOneTimePasswordArgs
import com.ivanovsky.passnotes.presentation.setupOneTimePassword.SetupOneTimePasswordFragment
import com.ivanovsky.passnotes.presentation.storagelist.StorageListArgs
import com.ivanovsky.passnotes.presentation.storagelist.StorageListFragment
import com.ivanovsky.passnotes.presentation.unlock.UnlockFragment
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs

object NewScreens {

    class UnlockScreen(private val args: UnlockScreenArgs) : Screen {
        override fun tag(): String = UnlockFragment::class.java.simpleName as String
        override fun create() = UnlockFragment.newInstance(args)
    }

    // File and Storage
    class StorageListScreen(private val args: StorageListArgs) : Screen {
        override fun tag(): String = StorageListFragment::class.java.simpleName
        override fun create() = StorageListFragment.newInstance(args)
    }

    class FilePickerScreen(private val args: FilePickerArgs) : Screen {
        override fun tag(): String = FilePickerFragment::class.java.simpleName
        override fun create() = FilePickerFragment.newInstance(args)
    }

    // Network
    class ServerLoginScreen(private val args: ServerLoginArgs) : Screen {
        override fun tag(): String = ServerLoginFragment::class.java.simpleName
        override fun create(): Fragment = ServerLoginFragment.newInstance(args)
    }

    // Database
    class NewDatabaseScreen : Screen {
        override fun tag(): String = NewDatabaseFragment::class.java.simpleName
        override fun create(): Fragment = NewDatabaseFragment.newInstance()
    }

    // View Notes and Groups
    class GroupsScreen(private val args: GroupsScreenArgs) : Screen {
        override fun tag(): String = GroupsFragment::class.java.simpleName
        override fun create(): Fragment = GroupsFragment.newInstance(args)
    }

    class GroupEditorScreen(private val args: GroupEditorArgs) : Screen {
        override fun tag(): String = GroupEditorFragment::class.java.simpleName
        override fun create(): Fragment = GroupEditorFragment.newInstance(args)
    }

    class NoteEditorScreen(private val args: NoteEditorArgs) : Screen {
        override fun tag(): String = NoteEditorFragment::class.java.simpleName
        override fun create(): Fragment = NoteEditorFragment.newInstance(args)
    }

    class NoteScreen(private val args: NoteScreenArgs) : Screen {
        override fun tag(): String = NoteFragment::class.java.simpleName
        override fun create(): Fragment = NoteFragment.newInstance(args)
    }

    class AboutScreen : Screen {
        override fun tag(): String = AboutFragment::class.java.simpleName
        override fun create(): Fragment = AboutFragment.newInstance()
    }

    class PasswordGeneratorScreen : Screen {
        override fun tag(): String = PasswordGeneratorFragment::class.java.simpleName
        override fun create(): Fragment = PasswordGeneratorFragment.newInstance()
    }

    class SetupOneTimePasswordScreen(private val args: SetupOneTimePasswordArgs) : Screen {
        override fun tag(): String = SetupOneTimePasswordFragment::class.java.simpleName
        override fun create(): Fragment = SetupOneTimePasswordFragment.newInstance(args)
    }

    class EnterDbCredentialsScreen(private val args: EnterDbCredentialsScreenArgs) : Screen {
        override fun tag(): String = EnterDbCredentialsFragment::class.java.simpleName
        override fun create(): Fragment = EnterDbCredentialsFragment.newInstance(args)
    }

    class HistoryScreen(private val args: HistoryScreenArgs) : Screen {
        override fun tag(): String = HistoryFragment::class.java.simpleName
        override fun create(): Fragment = HistoryFragment.newInstance(args)
    }

    // Settings
    class MainSettingsScreen : Screen {
        override fun tag(): String = MainSettingsFragment::class.java.simpleName
        override fun create(): Fragment = MainSettingsFragment.newInstance()
    }

    class AppSettingsScreen : Screen {
        override fun tag(): String = AppSettingsFragment::class.java.simpleName
        override fun create(): Fragment = AppSettingsFragment.newInstance()
    }

    class DatabaseSettingsScreen : Screen {
        override fun tag(): String = DatabaseSettingsFragment::class.java.simpleName
        override fun create(): Fragment = DatabaseSettingsFragment.newInstance()
    }

    // Diff
    class DiffViewerScreen(private val args: DiffViewerScreenArgs) : Screen {
        override fun tag(): String = DiffViewerFragment::class.java.simpleName
        override fun create(): Fragment = DiffViewerFragment.newInstance(args)
    }

    // Debug
    class DebugMenuScreen : Screen {
        override fun tag(): String = DebugMenuFragment::class.java.simpleName
        override fun create(): Fragment = DebugMenuFragment.newInstance()
    }
}