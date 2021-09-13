package com.ivanovsky.passnotes.presentation

import androidx.fragment.app.FragmentFactory
import com.github.terrakok.cicerone.androidx.FragmentScreen
import com.ivanovsky.passnotes.presentation.about.AboutFragment
import com.ivanovsky.passnotes.presentation.debugmenu.DebugMenuFragment
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerFragment
import com.ivanovsky.passnotes.presentation.filepicker.model.FilePickerArgs
import com.ivanovsky.passnotes.presentation.group_editor.GroupEditorArgs
import com.ivanovsky.passnotes.presentation.group_editor.GroupEditorFragment
import com.ivanovsky.passnotes.presentation.groups.GroupsArgs
import com.ivanovsky.passnotes.presentation.groups.GroupsFragment
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseFragment
import com.ivanovsky.passnotes.presentation.note.NoteFragment
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorArgs
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorFragment
import com.ivanovsky.passnotes.presentation.search.SearchFragment
import com.ivanovsky.passnotes.presentation.selectdb.SelectDatabaseArgs
import com.ivanovsky.passnotes.presentation.selectdb.SelectDatabaseFragment
import com.ivanovsky.passnotes.presentation.server_login.ServerLoginArgs
import com.ivanovsky.passnotes.presentation.server_login.ServerLoginFragment
import com.ivanovsky.passnotes.presentation.settings.app.AppSettingsFragment
import com.ivanovsky.passnotes.presentation.settings.database.DatabaseSettingsFragment
import com.ivanovsky.passnotes.presentation.settings.main.MainSettingsFragment
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.presentation.storagelist.StorageListFragment
import com.ivanovsky.passnotes.presentation.unlock.UnlockFragment
import java.util.UUID

object Screens {
    class UnlockScreen : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            UnlockFragment.newInstance()
    }

    class SelectDatabaseScreen(private val args: SelectDatabaseArgs) : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            SelectDatabaseFragment.newInstance(args)

        companion object {
            val RESULT_KEY = SelectDatabaseScreen::class.simpleName + "_result"
        }
    }

    // File and Storage
    class StorageListScreen(private val action: Action) : FragmentScreen {

        override fun createFragment(factory: FragmentFactory) =
            StorageListFragment.newInstance(action)

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
    class GroupsScreen(private val args: GroupsArgs) : FragmentScreen {
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

    class NoteScreen(private val noteUid: UUID) : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            NoteFragment.newInstance(noteUid)
    }

    class SearchScreen : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            SearchFragment.newInstance()
    }

    class AboutScreen : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            AboutFragment.newInstance()
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

    // Debug
    class DebugMenuScreen : FragmentScreen {
        override fun createFragment(factory: FragmentFactory) =
            DebugMenuFragment.newInstance()
    }
}