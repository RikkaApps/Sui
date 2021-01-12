package rikka.sui.manager.res;

import android.annotation.SuppressLint;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static rikka.sui.manager.ManagerConstants.LOGGER;

public class Xml {

    private static Constructor<?> xmlBlockConstructor;
    private static Method newParser;

    static {
        try {
            @SuppressLint("PrivateApi")
            Class<?> xmlBlock = Class.forName("android.content.res.XmlBlock");
            xmlBlockConstructor = xmlBlock.getConstructor(byte[].class);
            newParser = xmlBlock.getDeclaredMethod("newParser");
            xmlBlockConstructor.setAccessible(true);
            newParser.setAccessible(true);
        } catch (Throwable e) {
            LOGGER.w(Log.getStackTraceString(e));
        }
    }

    public static XmlPullParser get(byte[] res) {
        try {
            return (XmlPullParser) newParser.invoke(xmlBlockConstructor.newInstance((Object) res));
        } catch (Throwable e) {
            return null;
        }
    }
}
