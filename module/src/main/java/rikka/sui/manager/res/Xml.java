package rikka.sui.manager.res;

import android.annotation.SuppressLint;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import static rikka.sui.manager.ManagerConstants.LOGGER;

public class Xml {

    private static ByteBuffer[] buffers;
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

    public static void setBuffers(ByteBuffer[] buffers) {
        Xml.buffers = buffers;
    }

    public static XmlPullParser get(int res) {
        ByteBuffer bb = buffers[res];
        bb.rewind();
        byte[] bytes = new byte[bb.remaining()];
        bb.get(bytes);

        try {
            return (XmlPullParser) newParser.invoke(xmlBlockConstructor.newInstance((Object) bytes));
        } catch (Throwable e) {
            LOGGER.e(e, "getXml %d", res);
            return null;
        }
    }
}
