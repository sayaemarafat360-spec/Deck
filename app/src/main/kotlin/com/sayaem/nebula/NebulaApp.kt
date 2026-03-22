package com.sayaem.nebula

import android.app.Application
import com.google.android.gms.ads.MobileAds

class NebulaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }
}
