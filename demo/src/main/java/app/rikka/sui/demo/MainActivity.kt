package app.rikka.sui.demo

import android.app.Activity
import android.os.Bundle
import android.util.Log
import app.rikka.sui.demo.databinding.ActivityMainBinding

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val binder = Sui.getBinder()
        val service = Sui.getService()
        val text = try {
            """
                    binder: ${Sui.getBinder()}
                    checkSelfPermission: ${service.checkSelfPermission()}
                    getUid: ${service.uid}
                    """
        } catch (e: Throwable) {
            Log.getStackTraceString(e)
        }

        binding.text1.text = text.trimIndent()
    }
}