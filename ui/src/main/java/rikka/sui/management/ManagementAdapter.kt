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

import rikka.recyclerview.BaseRecyclerViewAdapter
import rikka.recyclerview.ClassCreatorPool
import rikka.sui.model.AppInfo

class ManagementAdapter : BaseRecyclerViewAdapter<ClassCreatorPool>() {

    init {
        creatorPool.putRule(AppInfo::class.java, ManagementAppItemViewHolder.CREATOR)
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItemAt<Any>(position).hashCode().toLong()
    }

    override fun onCreateCreatorPool(): ClassCreatorPool {
        return ClassCreatorPool()
    }

    fun updateData(data: List<AppInfo>) {
        getItems<Any>().clear()
        getItems<Any>().addAll(data)
        notifyDataSetChanged()
    }
}
