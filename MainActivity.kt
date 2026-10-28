package it.local.bollatrice

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private var pendingCsv: String = ""
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }
    private fun render() {
        root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,24,24,24); setBackgroundColor(0xFFF3F6FB.toInt()) }
        val sc=ScrollView(this); sc.addView(root); setContentView(sc)
        root.addView(TextView(this).apply { text="La mia bollatrice"; textSize=25f; setTextColor(0xFF17345C.toInt()); setPadding(0,0,0,8) })
        root.addView(TextView(this).apply { text="Timbrature locali · nessuna sincronizzazione online"; textSize=14f; setTextColor(0xFF52647A.toInt()) })
        status=TextView(this).apply { textSize=17f; setPadding(0,20,0,16) }; root.addView(status)
        val actions=listOf("▶ Inizio lavoro" to "start","☕ Inizio/fine pausa" to "pause","🚗 Inizio/fine viaggio" to "trip","■ Fine lavoro" to "end")
        actions.forEach { (label,action)-> root.addView(Button(this).apply { text=label; textSize=16f; setOnClickListener { status.text=Store.action(this@MainActivity,action); refreshStatus() } },LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin=6 }) }
        root.addView(Button(this).apply { text="Correggi / inserisci una giornata passata"; setOnClickListener { manualEntry() } },full())
        root.addView(Button(this).apply { text="Luogo di lavoro di oggi"; setOnClickListener { workplace() } },full())
        root.addView(Button(this).apply { text="Esporta mese in CSV per Excel"; setOnClickListener { exportMonth() } },full())
        root.addView(Button(this).apply { text="Storico recente"; setOnClickListener { history() } },full())
        root.addView(TextView(this).apply { text="Il file CSV usa le colonne del modello: luogo, entrata, pausa, uscita, ore totali e ore viaggio. Il luogo di lavoro puoi inserirlo manualmente."; textSize=13f; setPadding(0,18,0,0); setTextColor(0xFF52647A.toInt()) })
        refreshStatus()
    }
    private fun full()=LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin=5; bottomMargin=5 }
    private fun refreshStatus() { val d=Store.day(this); val state=when { d.optString("end").isNotEmpty()->"Giornata terminata"; d.optString("pauseStart").isNotEmpty()&&d.optString("pauseEnd").isEmpty()->"In pausa"; d.optString("start").isNotEmpty()->"Al lavoro"; else->"Non hai ancora timbrato" }; status.text="Oggi: $state\nEntrata: ${d.optString("start","—")}   Uscita: ${d.optString("end","—")}\nPausa: ${d.optString("pauseStart","—")} – ${d.optString("pauseEnd","—")}\nViaggio: ${d.optString("tripStart","—")} – ${d.optString("tripEnd","—")}" }
    private fun workplace() { val d=Store.day(this); val input=EditText(this).apply { hint="Luogo di lavoro"; setText(d.optString("workplace")) }; AlertDialog.Builder(this).setTitle("Luogo di lavoro di oggi").setView(input).setPositiveButton("Salva"){_,_->d.put("workplace",input.text.toString());Store.saveDay(this,Store.today(),d);refreshStatus()}.setNegativeButton("Annulla",null).show() }
    private fun manualEntry() {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(20,0,20,0) }
        val fields=linkedMapOf("Data (AAAA-MM-GG)" to Store.today(),"Luogo di lavoro" to "","Entrata (HH:MM)" to "","Inizio pausa (HH:MM)" to "","Fine pausa (HH:MM)" to "","Uscita (HH:MM)" to "","Inizio viaggio (HH:MM)" to "","Fine viaggio (HH:MM)" to "")
        val inputs=linkedMapOf<String,EditText>()
        fields.forEach { (label,default)-> val e=EditText(this).apply { hint=label; setSingleLine(true); setText(default) }; box.addView(e); inputs[label]=e }
        AlertDialog.Builder(this).setTitle("Inserimento / correzione manuale").setView(ScrollView(this).apply{addView(box)}).setPositiveButton("Salva") { _,_->
            val date=inputs["Data (AAAA-MM-GG)"]!!.text.toString().trim()
            if(!date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { Toast.makeText(this,"Formato data non valido",Toast.LENGTH_LONG).show();return@setPositiveButton }
            val d=JSONObject(); d.put("workplace",inputs["Luogo di lavoro"]!!.text.toString().trim()); d.put("start",inputs["Entrata (HH:MM)"]!!.text.toString().trim()); d.put("pauseStart",inputs["Inizio pausa (HH:MM)"]!!.text.toString().trim()); d.put("pauseEnd",inputs["Fine pausa (HH:MM)"]!!.text.toString().trim()); d.put("end",inputs["Uscita (HH:MM)"]!!.text.toString().trim()); d.put("tripStart",inputs["Inizio viaggio (HH:MM)"]!!.text.toString().trim()); d.put("tripEnd",inputs["Fine viaggio (HH:MM)"]!!.text.toString().trim()); Store.saveDay(this,date,d); Toast.makeText(this,"Giornata salvata",Toast.LENGTH_SHORT).show();refreshStatus()
        }.setNegativeButton("Annulla",null).show()
    }
    private fun exportMonth() {
        val month=SimpleDateFormat("yyyy-MM",Locale.ITALY).format(Date()); val input=EditText(this).apply { setText(month); hint="AAAA-MM" }
        AlertDialog.Builder(this).setTitle("Esporta mese").setMessage("Indica il mese da esportare (AAAA-MM)").setView(input).setPositiveButton("Salva file CSV") { _,_->
            val m=input.text.toString().trim(); if(!m.matches(Regex("\\d{4}-\\d{2}"))) { Toast.makeText(this,"Mese non valido",Toast.LENGTH_LONG).show();return@setPositiveButton }
            pendingCsv=Store.csv(this,m)
            val create=Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="text/csv"; putExtra(Intent.EXTRA_TITLE,"presenze_$m.csv") }
            startActivityForResult(create,501)
        }.setNegativeButton("Annulla",null).show()
    }
    @Deprecated("Deprecated in Android API; kept for broad device compatibility")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==501 && resultCode==RESULT_OK && data?.data!=null) {
            try { contentResolver.openOutputStream(data.data!!)?.bufferedWriter(Charsets.UTF_8)?.use { it.write("\uFEFF"); it.write(pendingCsv) }; Toast.makeText(this,"CSV salvato",Toast.LENGTH_LONG).show() }
            catch(e:Exception) { Toast.makeText(this,"Errore nel salvataggio: ${e.message}",Toast.LENGTH_LONG).show() }
        }
    }
    private fun history() { val all=Store.all(this); val keys=all.keys().asSequence().toList().sortedDescending().take(20); val text=keys.joinToString("\n\n"){ k->val d=all.optJSONObject(k)!!; "$k  ${d.optString("workplace")}\nEntrata ${d.optString("start","—")} · Uscita ${d.optString("end","—")}\nPausa ${d.optString("pauseStart","—")}–${d.optString("pauseEnd","—")} · Viaggio ${d.optString("tripStart","—")}–${d.optString("tripEnd","—")}" }.ifEmpty { "Nessuna timbratura." }; AlertDialog.Builder(this).setTitle("Storico recente").setMessage(text).setPositiveButton("Chiudi",null).show() }
}
