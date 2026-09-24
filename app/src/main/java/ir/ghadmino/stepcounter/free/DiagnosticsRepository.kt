package ir.ghadmino.stepcounter.free

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import ir.ghadmino.stepcounter.health.HealthConnectRepository
import ir.ghadmino.stepcounter.step.StepCounterService

data class Diagnostics(val sensor:Boolean,val activityPermission:Boolean,val notificationPermission:Boolean,val locationPermission:Boolean,val healthConnect:String,val serviceState:String)

suspend fun diagnostics(c:Context):Diagnostics{
 val sm=c.getSystemService(Context.SENSOR_SERVICE) as SensorManager
 val sensor=sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!=null
 val activity=Build.VERSION.SDK_INT<Build.VERSION_CODES.Q||ContextCompat.checkSelfPermission(c,Manifest.permission.ACTIVITY_RECOGNITION)==PackageManager.PERMISSION_GRANTED
 val notification=Build.VERSION.SDK_INT<Build.VERSION_CODES.TIRAMISU||ContextCompat.checkSelfPermission(c,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
 val location=ContextCompat.checkSelfPermission(c,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED||ContextCompat.checkSelfPermission(c,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
 val hc=try{HealthConnectRepository.availability(c).toString()}catch(_:Exception){"خطا"}
 return Diagnostics(sensor,activity,notification,location,hc,if(StepCounterService.sensorAvailable)"فعال" else "غیرفعال")
}
