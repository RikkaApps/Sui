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

import android.content.res.Configuration
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.TextView
import kotlinx.coroutines.Job
import rikka.core.res.resolveColor
import rikka.core.res.resolveColorStateList
import rikka.html.text.toHtml
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator
import rikka.sui.R
import rikka.sui.databinding.ManagementAppItemBinding
import rikka.sui.model.AppInfo
import rikka.sui.server.SuiConfig
import rikka.sui.util.AppIconCache
import rikka.sui.util.BridgeServiceClient
import rikka.sui.util.UserHandleCompat
import java.util.*

class ManagementAppItemViewHolder(private val binding: ManagementAppItemBinding) : BaseViewHolder<AppInfo>(binding.root),
    View.OnClickListener {

    companion object {

        val CREATOR = Creator<AppInfo> { inflater: LayoutInflater, parent: ViewGroup? ->
            ManagementAppItemViewHolder(
                ManagementAppItemBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )
        }

        private val SANS_SERIF = Typeface.create("sans-serif", Typeface.NORMAL)
        private val SANS_SERIF_MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private inline val packageName get() = data.packageInfo.packageName
    private inline val ai get() = data.packageInfo.applicationInfo
    private inline val uid get() = ai.uid

    private var loadIconJob: Job? = null

    private val icon get() = binding.icon
    private val name get() = binding.title
    private val pkg get() = binding.summary
    private val spinner get() = binding.button1

    private val textColorSecondary = context.theme.resolveColorStateList(android.R.attr.textColorSecondary)
    private val textColorPrimary = context.theme.resolveColorStateList(android.R.attr.textColorPrimary)

    private val optionsAdapter: ArrayAdapter<CharSequence>

    init {
        itemView.setOnClickListener(this)

        val context = binding.root.context
        val theme = context.theme
        val isNight = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_YES != 0
        val colorAccent = theme.resolveColor(androidx.appcompat.R.attr.colorAccent)
        val colorForeground = theme.resolveColor(android.R.attr.colorForeground)
        val textColorTertiary = theme.resolveColorStateList(android.R.attr.textColorTertiary)
        val colorError = if (isNight) 0xFF8A80 else 0xFF5252

        this.optionsAdapter = object : ArrayAdapter<CharSequence>(
            binding.button1.context,
            android.R.layout.simple_spinner_item,
            arrayOf(
                String.format(
                    "<font face=\"sans-serif-medium\" color=\"#%2\$s\">%1\$s</font>",
                    context.getString(R.string.permission_allowed),
                    String.format(Locale.ENGLISH, "%06x", colorAccent and 0xffffff)
                ).toHtml(),
                String.format(
                    "<font face=\"sans-serif-medium\" color=\"#%2\$s\">%1\$s</font>",
                    context.getString(R.string.permission_denied),
                    String.format(Locale.ENGLISH, "%06x", colorError and 0xffffff)
                ).toHtml(),
                String.format(
                    "<font face=\"sans-serif-medium\" color=\"#%2\$s\">%1\$s</font>",
                    context.getString(R.string.permission_hidden),
                    String.format(Locale.ENGLISH, "%06x", colorForeground and 0xffffff)
                ).toHtml(),
                context.getString(R.string.permission_ask)
            )
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return if (convertView == null) {
                    val view = super.getView(position, convertView, parent)
                    val textView = view.findViewById<TextView>(android.R.id.text1)
                    textView.setTextColor(textColorTertiary)
                    textView.gravity = Gravity.CENTER_VERTICAL or Gravity.END
                    view
                } else {
                    super.getView(position, convertView, parent)
                }
            }
        }
        this.optionsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        this.itemView.setOnClickListener { spinner.performClick() }
    }

    private val onItemSelectedListener: OnItemSelectedListener = object : OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            val newValue = when (position) {
                0 -> SuiConfig.FLAG_ALLOWED
                1 -> SuiConfig.FLAG_DENIED
                2 -> SuiConfig.FLAG_HIDDEN
                else -> 0
            }
            try {
                BridgeServiceClient.getService()
                    .updateFlagsForUid(data.packageInfo.applicationInfo.uid, SuiConfig.MASK_PERMISSION, newValue)
            } catch (e: Throwable) {
                Log.e("SuiSettings", "updateFlagsForUid", e)
                return
            }
            data.flags = data.flags and SuiConfig.MASK_PERMISSION.inv() or newValue
            parent.setSelection(position)
            syncViewStateForFlags()
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    override fun onClick(v: View) {
        adapter.notifyItemChanged(bindingAdapterPosition, Any())
    }

    override fun onBind() {
        val pm = itemView.context.packageManager
        val userId = UserHandleCompat.getUserId(uid)

        icon.setImageDrawable(ai.loadIcon(pm))

        loadIconJob = AppIconCache.loadIconBitmapAsync(context, ai, ai.uid / 100000, icon)

        name.text = if (userId != UserHandleCompat.myUserId()) {
            "${ai.loadLabel(pm)} - ($userId)"
        } else {
            ai.loadLabel(pm)
        }
        pkg.text = ai.packageName

        spinner.adapter = optionsAdapter
        spinner.onItemSelectedListener = onItemSelectedListener

        syncViewStateForFlags()
    }

    override fun onBind(payloads: List<Any>) {
    }

    override fun onRecycle() {
        if (loadIconJob?.isActive == true) {
            loadIconJob?.cancel()
        }
    }

    private fun syncViewStateForFlags() {
        val allowed = data.flags and SuiConfig.FLAG_ALLOWED != 0
        val denied = data.flags and SuiConfig.FLAG_DENIED != 0
        val hidden = data.flags and SuiConfig.FLAG_HIDDEN != 0
        if (allowed) {
            binding.title.setTextColor(textColorPrimary)
            binding.title.typeface = SANS_SERIF_MEDIUM
            binding.button1.setSelection(0)
        } else if (denied) {
            binding.title.setTextColor(textColorSecondary)
            binding.title.typeface = SANS_SERIF
            binding.button1.setSelection(1)
        } else if (hidden) {
            binding.title.setTextColor(textColorSecondary)
            binding.title.typeface = SANS_SERIF
            binding.button1.setSelection(2)
        } else {
            binding.title.setTextColor(textColorSecondary)
            binding.title.typeface = SANS_SERIF
            binding.button1.setSelection(3)
        }
    }
}
