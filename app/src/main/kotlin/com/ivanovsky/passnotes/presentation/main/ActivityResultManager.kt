package com.ivanovsky.passnotes.presentation.main

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

class ActivityResultManager(
    private val registry: ActivityResultRegistry,
    private val lifecycleOwner: LifecycleOwner,
    private val onPermissionResult: (isGranted: Boolean) -> Unit,
    private val onAllFilePermissionResult: (result: Intent?) -> Unit
) : DefaultLifecycleObserver {

    private val launchers = mutableMapOf<LauncherType<*>, ActivityResultLauncher<*>>()

    override fun onCreate(owner: LifecycleOwner) {
        for (launcherType in ALL_LAUNCHER_TYPES) {
            launchers[launcherType] = registerLauncher(launcherType)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        for ((type, launcher) in launchers) {
            Timber.d("onDestroy: unregister launcher, type=%s", type)
            launcher.unregister()
        }
        launchers.clear()
    }

    fun <Input> getLauncherByType(
        type: LauncherType<Input>
    ): ActivityResultLauncher<Input> {
        @Suppress("UNCHECKED_CAST")
        return launchers[type] as? ActivityResultLauncher<Input>
            ?: throw IllegalArgumentException("Unable to find launcher for type: $type")
    }

    private fun registerLauncher(type: LauncherType<*>): ActivityResultLauncher<*> {
        if (type.minSdk != null && Build.VERSION.SDK_INT < type.minSdk) {
            val message = String.format(
                "Invalid launcher type: type=%s, minSdk=%s",
                type,
                Build.VERSION.SDK_INT
            )

            Timber.e(message)
            throw IllegalStateException(message)
        }

        return when (type) {
            is LauncherType.Permission -> {
                registry.register(
                    type.registryKey,
                    lifecycleOwner,
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    handleOnPermissionResult(isGranted)
                }
            }

            is LauncherType.AllFilesPermission -> {
                registry.register(
                    type.registryKey,
                    lifecycleOwner,
                    createAllFilesPermissionContract()
                ) { result ->
                    handleOnAllFilePermissionResult(type, result)
                }
            }
        }
    }

    private fun handleOnPermissionResult(isGranted: Boolean) {
        Timber.d("handleOnPermissionResult: isGranted=%s", isGranted)

        onPermissionResult.invoke(isGranted)
    }

    private fun handleOnAllFilePermissionResult(type: LauncherType<*>, result: Intent?) {
        Timber.d("handleOnActivityResult: type=%s, result=%s", type, result)

        onAllFilePermissionResult.invoke(result)
    }

    private fun createAllFilesPermissionContract(): ActivityResultContract<Unit, Intent?> {
        return object : ActivityResultContract<Unit, Intent?>() {
            @RequiresApi(30)
            override fun createIntent(context: Context, input: Unit): Intent {
                return Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
                return intent
            }
        }
    }

    sealed class LauncherType<T>(
        val registryKey: String,
        val inputType: Class<T>,
        val minSdk: Int?
    ) {

        object Permission : LauncherType<String>(
            registryKey = Permission::class.java.simpleName,
            inputType = String::class.java,
            minSdk = null
        )

        object AllFilesPermission : LauncherType<Unit>(
            registryKey = Permission::class.java.simpleName,
            inputType = Unit::class.java,
            minSdk = 30
        )
    }

    companion object {
        private val ALL_LAUNCHER_TYPES = listOf(
            LauncherType.Permission,
            LauncherType.AllFilesPermission
        )
    }
}