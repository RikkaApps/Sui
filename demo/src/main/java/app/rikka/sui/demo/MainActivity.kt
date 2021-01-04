package app.rikka.sui.demo

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import app.rikka.sui.demo.databinding.ActivityMainBinding
import moe.shizuku.server.IShizukuClient

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val binder = Sui.getBinder()
        val service = Sui.getService()

        service.requestPermission(0)

        Build.MODEL
        val text = try {
            """
                    binder: ${Sui.getBinder()}
                    getSystemProperty ro.build.id: ${Sui.getService().getSystemProperty("ro.build.id", "(null)")}
                    """
        } catch (e: Throwable) {
            Log.getStackTraceString(e)
        }

        binding.text1.text = text.trimIndent()
    }
}