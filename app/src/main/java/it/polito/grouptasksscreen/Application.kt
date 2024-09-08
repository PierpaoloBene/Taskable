package it.polito.grouptasksscreen

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import it.polito.grouptasksscreen.model.Model

@RequiresApi(Build.VERSION_CODES.O)
class Application : Application() {
    lateinit var model : Model

    override fun onCreate() {
        super.onCreate()
        model = Model(this)
    }
}