/*
 * This file is part of Sui.
 *
 * Sui is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sui is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sui.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 Sui Contributors
 */
package rikka.sui.management

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.lifecycle.Resource
import rikka.lifecycle.Status
import rikka.sui.model.AppInfo
import rikka.sui.util.AppInfoComparator
import rikka.sui.util.BridgeServiceClient

class ManagementViewModel : ViewModel() {

    private val fullList = ArrayList<AppInfo>()

    val appList = MutableLiveData<Resource<List<AppInfo>>>(null)

    private fun handleList() {
        val list = fullList.sortedWith(AppInfoComparator()).toList()

        appList.postValue(Resource.success(list))
    }

    fun invalidateList() {
        if (appList.value?.status != Status.SUCCESS) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            handleList()
        }
    }

    fun reload(context: Context) {
        appList.postValue(Resource.loading(null))

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val result = BridgeServiceClient.getApplications(-1 /* ALL */).apply {
                    forEach { it.label = it.packageInfo.applicationInfo.loadLabel(pm) }
                }

                fullList.clear()
                fullList.addAll(result)

                handleList()
            } catch (e: CancellationException) {

            } catch (e: Throwable) {
                appList.postValue(Resource.error(e, null))
            }
        }
    }
}
