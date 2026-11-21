package de.xitee.buttonmapper

import android.app.Application
import de.xitee.buttonmapper.data.ConfigRepository
import de.xitee.buttonmapper.shizuku.ShizukuMonitor

class App : Application() {
    val repository by lazy { ConfigRepository(this) }

    override fun onCreate() {
        super.onCreate()
        ShizukuMonitor.init()
    }
}
