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

import rikka.sui.util.Logger;

public class ServerConstants {

    public static final Logger LOGGER = new Logger("SuiServer");

    public static final int BINDER_TRANSACTION_getApplications = 10001;
    public static final int BINDER_TRANSACTION_showManagement = 10002;
    public static final int BINDER_TRANSACTION_openApk = 10003;
    public static final int BINDER_TRANSACTION_setIntent = 10004;
    public static final int BINDER_TRANSACTION_getIntent = 10005;

}
