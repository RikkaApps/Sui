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

package rikka.sui.server.api

import android.os.*
import rikka.sui.server.ServerConstants.LOGGER
import java.io.FileDescriptor

open class SystemServiceBinder<T : IInterface>(val name: String, private val converter: (binder: IBinder) -> T) : IBinder {

    private var binderCache: IBinder? = null
    private var serviceCache: T? = null

    private val binder: IBinder?
        get() = binderCache ?: ServiceManager.getService(name)?.let {
            LOGGER.v("get service $name")

            try {
                it.linkToDeath({
                    binderCache = null
                    serviceCache = null
                }, 0)

                // save binder only if linkToDeath succeed
                binderCache = it
            } catch (e: Throwable) {
                LOGGER.w(e, "linkToDeath $name failed")
            }
            it
        }

    val service: T?
        get() = serviceCache ?: binder?.let {
            serviceCache = converter(this)
            serviceCache
        }

    @Throws(RemoteException::class)
    override fun transact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        return binder?.let {
            try {
                it.transact(code, data, reply, flags)
            } catch (e: DeadObjectException) {
                LOGGER.w(e, "transact $name $code failed")

                // try again when DeadObjectException
                binderCache = null

                binder?.transact(code, data, reply, flags)
            }
        } ?: false
    }

    @Throws(RemoteException::class)
    override fun getInterfaceDescriptor(): String? {
        return binder?.interfaceDescriptor
    }

    override fun pingBinder(): Boolean {
        return binder?.pingBinder() ?: false
    }

    override fun isBinderAlive(): Boolean {
        return binder?.isBinderAlive ?: false
    }

    override fun queryLocalInterface(descriptor: String): IInterface? {
        return binder?.queryLocalInterface(descriptor)
    }

    @Throws(RemoteException::class)
    override fun dump(fd: FileDescriptor, args: Array<String>?) {
        binder?.dump(fd, args)
    }

    @Throws(RemoteException::class)
    override fun dumpAsync(fd: FileDescriptor, args: Array<String>?) {
        binder?.dumpAsync(fd, args)
    }

    @Throws(RemoteException::class)
    override fun linkToDeath(recipient: IBinder.DeathRecipient, flags: Int) {
        binder?.linkToDeath(recipient, flags)
    }

    override fun unlinkToDeath(recipient: IBinder.DeathRecipient, flags: Int): Boolean {
        return binder?.unlinkToDeath(recipient, flags) ?: false
    }
}