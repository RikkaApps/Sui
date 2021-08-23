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

package rikka.sui.server;

public class SuiConfig {

    public static final int FLAG_ALLOWED = 1 << 1;
    public static final int FLAG_DENIED = 1 << 2;
    public static final int FLAG_HIDDEN = 1 << 3;
    public static final int MASK_PERMISSION = FLAG_ALLOWED | FLAG_DENIED | FLAG_HIDDEN;
}
