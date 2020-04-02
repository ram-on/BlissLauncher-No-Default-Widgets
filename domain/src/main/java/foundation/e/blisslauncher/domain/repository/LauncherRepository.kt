package foundation.e.blisslauncher.domain.repository

import android.os.UserHandle
import foundation.e.blisslauncher.domain.entity.ApplicationItem
import foundation.e.blisslauncher.domain.entity.LauncherItem

/**
 * Repository to manage [LauncherItem]
 */
interface LauncherRepository: Repository<LauncherItem, Long>

/*fun getAllActivities(user: UserHandle, quietMode: Boolean): List<ApplicationItem>

    *//**
 * Functions to fetch/add/update/remove AppsRepository
 *//*
    fun add(packageName: String, user: UserHandle, quietMode: Boolean): List<ApplicationItem>

    fun remove(packageName: String, user: UserHandle)

    fun updatedPackages(
        packages: Array<out String>,
        user: UserHandle,
        quietMode: Boolean
    ): List<LauncherItem>

    fun suspendPackages(packages: Array<out String>, user: UserHandle): List<LauncherItem>

    fun unsuspendPackages(packages: Array<out String>, user: UserHandle): List<LauncherItem>

    fun updateUserAvailability(user: UserHandle, quietMode: Boolean): List<LauncherItem>

    fun makePackagesUnavailable(packages: Array<out String>, user: UserHandle): List<LauncherItem>

    fun removePackages(packages: Array<out String>, user: UserHandle): List<LauncherItem>*/