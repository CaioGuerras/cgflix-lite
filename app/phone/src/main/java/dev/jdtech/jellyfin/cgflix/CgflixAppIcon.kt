package dev.jdtech.jellyfin.cgflix

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import dev.jdtech.jellyfin.core.presentation.theme.CgflixThemeChoice
import timber.log.Timber

/**
 * CGFLIX: o ícone do app acompanha o tema (pedido do Caio, 10/10), como no Telegram: Heitor = ícone
 * verde; Isis e automático = ícone roxo; no Modo Maryanne, o morango. São três `activity-alias` no
 * manifesto (`IconeIsis`, ligado por padrão, `IconeHeitor` e `IconeMaryanne`); fica ligado só um.
 *
 * A troca só acontece quando o app sai da tela (nenhuma tela visível): em alguns aparelhos o
 * sistema fecha o app ao trocar o ícone, e o launcher leva alguns segundos para mostrar o novo.
 */
object CgflixAppIcon {
    private const val ISIS = "dev.jdtech.jellyfin.cgflix.IconeIsis"
    private const val HEITOR = "dev.jdtech.jellyfin.cgflix.IconeHeitor"

    private const val MARYANNE = "dev.jdtech.jellyfin.cgflix.IconeMaryanne"

    /**
     * Liga o ícone do tema [choice] (ou o morango, no Modo Maryanne) e desliga os outros (nada a
     * fazer se já estiver certo).
     */
    fun sync(context: Context, choice: CgflixThemeChoice, maryanne: Boolean = false) {
        val pm = context.packageManager
        val target =
            when {
                maryanne -> MARYANNE
                choice == CgflixThemeChoice.HEITOR -> HEITOR
                else -> ISIS
            }
        val pkg = context.packageName
        val on = ComponentName(pkg, target)
        val offs = (listOf(ISIS, HEITOR, MARYANNE) - target).map { ComponentName(pkg, it) }
        if (
            isEnabled(pm, on, default = target == ISIS) &&
                offs.none { isEnabled(pm, it, default = it.className == ISIS) }
        ) {
            return
        }
        try {
            // Primeiro liga o novo: nunca fica sem ícone no launcher
            pm.setComponentEnabledSetting(
                on,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            offs.forEach {
                pm.setComponentEnabledSetting(
                    it,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "CGFLIX: não deu para trocar o ícone do app")
        }
    }

    private fun isEnabled(pm: PackageManager, component: ComponentName, default: Boolean) =
        when (pm.getComponentEnabledSetting(component)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> default
            else -> false
        }

    /** Chama [sync] com o tema atual toda vez que a última tela visível do app sai da tela. */
    fun syncWhenInBackground(
        application: Application,
        currentMaryanne: () -> Boolean = { false },
        currentChoice: () -> CgflixThemeChoice,
    ) {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                private var started = 0

                override fun onActivityStarted(activity: Activity) {
                    started++
                }

                override fun onActivityStopped(activity: Activity) {
                    started--
                    // Girar a tela recria a atividade: não é sair do app
                    if (started <= 0 && !activity.isChangingConfigurations) {
                        started = 0
                        sync(application, currentChoice(), currentMaryanne())
                    }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

                override fun onActivityResumed(activity: Activity) {}

                override fun onActivityPaused(activity: Activity) {}

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

                override fun onActivityDestroyed(activity: Activity) {}
            }
        )
    }
}
