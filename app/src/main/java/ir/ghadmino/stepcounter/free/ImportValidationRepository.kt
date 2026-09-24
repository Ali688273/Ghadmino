package ir.ghadmino.stepcounter.free

import org.json.JSONObject

data class ImportValidation(val valid:Boolean,val version:Int,val preferenceGroups:Int,val entries:Int,val message:String)

object ImportValidationRepository {
    fun validate(json:String):ImportValidation{
        return try{
            val root=JSONObject(json)
            val format=root.optString("format")
            val version=root.optInt("version",0)
            val all=root.optJSONObject("preferences")
            if(format!="ghadmino_backup"||all==null||version !in 1..4) ImportValidation(false,version,0,0,"فرمت یا نسخه پشتیبان معتبر نیست.")
            else{
                var entries=0
                val keys=all.keys()
                var groups=0
                while(keys.hasNext()){val key=keys.next();groups++;entries+=all.optJSONObject(key)?.length()?:0}
                ImportValidation(true,version,groups,entries,"فایل معتبر است.")
            }
        }catch(_:Exception){ImportValidation(false,0,0,0,"فایل JSON خراب یا ناقص است.")}
    }
}
