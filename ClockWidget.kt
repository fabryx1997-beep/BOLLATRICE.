package it.local.bollatrice

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ClockWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context,intent)
        if(intent.action=="it.local.bollatrice.ACTION") {
            val action=intent.getStringExtra("action") ?: return
            Store.action(context,action)
            updateAll(context)
        }
    }
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { ids.forEach { update(context,manager,it) } }
    private fun updateAll(ctx:Context) { val m=AppWidgetManager.getInstance(ctx); val ids=m.getAppWidgetIds(ComponentName(ctx,ClockWidget::class.java)); onUpdate(ctx,m,ids) }
    private fun update(ctx:Context,m:AppWidgetManager,id:Int) {
        val d=Store.day(ctx); val status=when { d.optString("end").isNotEmpty()->"Giornata terminata"; d.optString("pauseStart").isNotEmpty()&&d.optString("pauseEnd").isEmpty()->"In pausa"; d.optString("start").isNotEmpty()->"Al lavoro"; else->"Pronto per timbrare" }
        val v=RemoteViews(ctx.packageName,R.layout.widget); v.setTextViewText(R.id.widget_status,status)
        bind(ctx,v,R.id.w_start,"start"); bind(ctx,v,R.id.w_pause,"pause"); bind(ctx,v,R.id.w_trip,"trip"); bind(ctx,v,R.id.w_end,"end")
        m.updateAppWidget(id,v)
    }
    private fun bind(ctx:Context,v:RemoteViews,viewId:Int,action:String) { val i=Intent(ctx,ClockWidget::class.java).setAction("it.local.bollatrice.ACTION").putExtra("action",action); val p=PendingIntent.getBroadcast(ctx,action.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); v.setOnClickPendingIntent(viewId,p) }
}
