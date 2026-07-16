package com.shahriarhasan.usedphoneinspector.core.permissions

import android.Manifest
import android.os.Build

enum class PermissionFeature { CAMERA, MICROPHONE, BLUETOOTH, WIFI_SCAN, TELEPHONY }

object PermissionPolicy {
    fun permissionsFor(feature: PermissionFeature): Array<String> = when (feature) {
        PermissionFeature.CAMERA -> arrayOf(Manifest.permission.CAMERA)
        PermissionFeature.MICROPHONE -> arrayOf(Manifest.permission.RECORD_AUDIO)
        PermissionFeature.BLUETOOTH -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        PermissionFeature.WIFI_SCAN -> if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            emptyArray()
        }
        PermissionFeature.TELEPHONY -> arrayOf(Manifest.permission.READ_PHONE_STATE)
    }
}

