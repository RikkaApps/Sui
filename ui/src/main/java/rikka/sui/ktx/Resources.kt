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

@file:Suppress("unused")

package rikka.sui.ktx

import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi

fun Resources.Theme.resolveResourceId(@AttrRes attrId: Int): Int {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getResourceId(0, 0)
    a.recycle()
    return res
}

@ColorInt
fun Resources.Theme.resolveColor(@AttrRes attrId: Int):  Int {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getColor(0, 0)
    a.recycle()
    return res
}

fun Resources.Theme.resolveColorStateList(@AttrRes attrId: Int): ColorStateList? {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getColorStateList(0)
    a.recycle()
    return res
}

fun Resources.Theme.resolveBoolean(@AttrRes attrId: Int, defaultResult: Boolean): Boolean {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getBoolean(0, defaultResult)
    a.recycle()
    return res
}

fun Resources.Theme.resolveDrawable(@AttrRes attrId: Int): Drawable? {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getDrawable(0)
    a.recycle()
    return res
}

@RequiresApi(api = Build.VERSION_CODES.O)
fun Resources.Theme.resolveFont(@AttrRes attrId: Int): Typeface? {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getFont(0)
    a.recycle()
    return res
}

fun Resources.Theme.resolveFloat(@AttrRes attrId: Int, defaultResult: Float): Float {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getFloat(0, defaultResult)
    a.recycle()
    return res
}

fun Resources.Theme.resolveDimension(@AttrRes attrId: Int, defaultResult: Float): Float {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getDimension(0, defaultResult)
    a.recycle()
    return res
}

fun Resources.Theme.resolveDimensionPixelOffset(@AttrRes attrId: Int, defaultResult: Int): Int {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getDimensionPixelOffset(0, defaultResult)
    a.recycle()
    return res
}

fun Resources.Theme.resolveDimensionPixelSize(@AttrRes attrId: Int, defaultResult: Int): Int {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getDimensionPixelSize(0, defaultResult)
    a.recycle()
    return res
}

fun Resources.Theme.resolveInt(@AttrRes attrId: Int, defaultResult: Int): Int {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getInt(0, defaultResult)
    a.recycle()
    return res
}

fun Resources.Theme.resolveInteger(@AttrRes attrId: Int, defaultResult: Int): Int {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getInteger(0, defaultResult)
    a.recycle()
    return res
}

fun Resources.Theme.resolveText(@AttrRes attrId: Int): CharSequence {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getText(0)
    a.recycle()
    return res
}

fun Resources.Theme.resolveTextArray(@AttrRes attrId: Int): Array<CharSequence> {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getTextArray(0)
    a.recycle()
    return res
}