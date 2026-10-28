package it.local.bollatrice

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Store {
    private const val PREF = "local_clock_data"
    private const val KEY = "days"
    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY).format(Date())
    fun now(): String = SimpleDateFormat("HH:mm", Locale.ITALY).format(Date())
    fun all(ctx: Context): JSONObject = try { JSONObject(ctx.getSharedPreferences(PREF,0).getString(KEY,"{}") ?: "{}") } catch (_:Exception) { JSONObject() }
    fun day(ctx: Context, date: String = today()): JSONObject = all(ctx).optJSONObject(date) ?: JSONObject()
    fun saveDay(ctx: Context, date: String, d: JSONObject) {
        val all = all(ctx); all.put(date,d)
        ctx.getSharedPreferences(PREF,0).edit().putString(KEY,all.toString()).apply()
    }
    fun action(ctx: Context, action: String): String {
        val date = today(); val d = day(ctx,date); val t = now()
        when(action) {
            "start" -> { if (d.optString("start").isNotEmpty()) return "Entrata già registrata"; d.put("start",t); d.put("workplace",d.optString("workplace","")) }
            "pause" -> { if (d.optString("start").isEmpty() || d.optString("end").isNotEmpty()) return "Prima registra l'entrata"; if (d.optString("pauseStart").isEmpty()) { d.put("pauseStart",t); return saveAnd(ctx,date,d,"Pausa iniziata alle $t") }; if (d.optString("pauseEnd").isEmpty()) { d.put("pauseEnd",t); return saveAnd(ctx,date,d,"Pausa terminata alle $t") }; return "Pausa giornaliera già completata" }
            "trip" -> { if (d.optString("tripStart").isEmpty()) { d.put("tripStart",t); return saveAnd(ctx,date,d,"Viaggio iniziato alle $t") }; if (d.optString("tripEnd").isEmpty()) { d.put("tripEnd",t); return saveAnd(ctx,date,d,"Viaggio terminato alle $t") }; return "Viaggio già registrato" }
            "end" -> { if (d.optString("start").isEmpty()) return "Registra prima l'entrata"; if (d.optString("end").isNotEmpty()) return "Uscita già registrata"; if (d.optString("pauseStart").isNotEmpty() && d.optString("pauseEnd").isEmpty()) d.put("pauseEnd",t); d.put("end",t) }
        }
        return saveAnd(ctx,date,d,if(action=="end") "Uscita registrata alle $t" else "Entrata registrata alle $t")
    }
    private fun saveAnd(ctx:Context,date:String,d:JSONObject,msg:String):String { saveDay(ctx,date,d); return msg }
    fun minutes(a:String,b:String):Int = try { val f=SimpleDateFormat("HH:mm",Locale.ITALY); ((f.parse(b)!!.time-f.parse(a)!!.time)/60000).toInt().coerceAtLeast(0) } catch (_:Exception) { 0 }
    fun duration(a:String,b:String):Int = if(a.isNotEmpty()&&b.isNotEmpty()) minutes(a,b) else 0
    fun csv(ctx:Context, month:String):String {
        val all=all(ctx); val out=StringBuilder("Data;Luogo di lavoro;Ora inizio giornata;Ora inizio pausa;Ora fine pausa;Ora fine giornata;Ore tot.;Ore viaggio\r\n")
        val keys=all.keys().asSequence().filter{it.startsWith(month)}.sorted().toList()
        for(k in keys){ val d=all.optJSONObject(k)?:continue
            val gross=duration(d.optString("start"),d.optString("end")); val pause=duration(d.optString("pauseStart"),d.optString("pauseEnd")); val work=(gross-pause).coerceAtLeast(0); val trip=duration(d.optString("tripStart"),d.optString("tripEnd"))
            fun q(s:String)="\""+s.replace("\"","\"\"")+"\""
            out.append(listOf(k,d.optString("workplace"),d.optString("start"),d.optString("pauseStart"),d.optString("pauseEnd"),d.optString("end"),"%d:%02d".format(work/60,work%60),"%d:%02d".format(trip/60,trip%60)).joinToString(";"){q(it)}).append("\r\n")
        }; return out.toString()
    }
}
