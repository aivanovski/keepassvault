package com.ivanovsky.passnotes.presentation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.github.terrakok.cicerone.androidx.FragmentScreen
import com.ivanovsky.passnotes.presentation.about.AboutFragment
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

object Screens {
    class UnlockScreen(private val args: UnlockScreenArgs) : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            UnlockFragment.newInstance(args)
    }

    // File and Storage
    class StorageListScreen(private val args: StorageListArgs) : FragmentScreen {

        override fun createFragment(factory: FragmentFactory) =
            StorageListFragment.newInstance(args)

        companion object {
            val RESULT_KEY = StorageListScreen::class.simpleName + "_result"
        }
    }

    class FilePickerScreen(private val args: FilePickerArgs) : FragmentScreen {

        override fun createFragment(factory: FragmentFactory) =
            FilePickerFragment.newInstance(args)

        companion object {
            val RESULT_KEY = FilePickerScreen::class.simpleName + "_result"
        }
    }

    // Network
    class ServerLoginScreen(private val args: ServerLoginArgs) : FragmentScreen {

        override fun createFragment(factory: FragmentFactory) =
            ServerLoginFragment.newInstance(args)

        companion object {
            val RESULT_KEY = ServerLoginScreen::class.simpleName + "_result"
        }
    }

    // Database
    class NewDatabaseScreen : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            NewDatabaseFragment.newInstance()
    }

    // View Notes and Groups
    class GroupsScreen(private val args: GroupsScreenArgs) : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            GroupsFragment.newInstance(args)
    }

    class GroupEditorScreen(private val args: GroupEditorArgs) : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            GroupEditorFragment.newInstance(args)
    }

    class NoteEditorScreen(private val args: NoteEditorArgs) : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            NoteEditorFragment.newInstance(args)
    }

    class NoteScreen(private val args: NoteScreenArgs) : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            NoteFragment.newInstance(args)
    }

    class AboutScreen : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            AboutFragment.newInstance()
    }

    class PasswordGeneratorScreen : FragmentScreen {

        override fun createFragment(factory: FragmentFactory): Fragment =
            PasswordGeneratorFragment.newInstance()

        companion object {
            val RESULT_KEY = PasswordGeneratorScreen::class.simpleName + "_result"
        }
    }

    class SetupOneTimePasswordScreen(
        private val args: SetupOneTimePasswordArgs
    ) : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            SetupOneTimePasswordFragment.newInstance(args)

        companion object {
            val RESULT_KEY = SetupOneTimePasswordScreen::class.simpleName + "_result"
        }
    }

    class EnterDbCredentialsScreen(
        private val args: EnterDbCredentialsScreenArgs
    ) : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            EnterDbCredentialsFragment.newInstance(args)

        companion object {
            val RESULT_KEY = EnterDbCredentialsScreen::class.simpleName + "_result"
        }
    }

    // Settings
    class MainSettingsScreen : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            MainSettingsFragment.newInstance()
    }

    class AppSettingsScreen : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            AppSettingsFragment.newInstance()
    }

    class DatabaseSettingsScreen : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            DatabaseSettingsFragment.newInstance()
    }

    // Diff
    class DiffViewerScreen(private val args: DiffViewerScreenArgs) : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            DiffViewerFragment.newInstance(args)
    }

    // Debug
    class DebugMenuScreen : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            DebugMenuFragment.newInstance()
    }
}