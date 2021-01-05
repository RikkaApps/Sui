package rikka.sui.ktx

import android.content.IContentProvider
import android.os.Bundle
import android.os.RemoteException
import rikka.sui.util.BuildUtils

@Throws(RemoteException::class)
fun IContentProvider.callCompat(callingPkg: String?, featureId: String?, authority: String?, method: String?, arg: String?, extras: Bundle?): Bundle {
    return when {
        BuildUtils.atLeast30() -> {
            call(callingPkg, featureId, authority, method, arg, extras)
        }
        BuildUtils.atLeast29() -> {
            call(callingPkg, authority, method, arg, extras)
        }
        else -> {
            call(callingPkg, method, arg, extras)
        }
    }
}