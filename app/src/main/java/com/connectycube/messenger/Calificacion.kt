package com.connectycube.messenger

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.observe
import com.connectycube.auth.session.ConnectycubeSessionManager
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.core.EntityCallback
import com.connectycube.core.exception.ResponseException
import com.connectycube.messenger.adapters.ChatDialogAdapter
import com.connectycube.messenger.data.User
import com.connectycube.messenger.utilities.InjectorUtils
import com.connectycube.messenger.utilities.SharedPreferencesManager
import com.connectycube.messenger.viewmodels.ChatDialogListViewModel
import com.connectycube.messenger.viewmodels.UserListViewModel
import com.connectycube.messenger.vo.Status
import com.connectycube.users.ConnectycubeUsers
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.android.synthetic.main.activity_calificacion.*
import kotlinx.android.synthetic.main.activity_chatdialogs.*
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login.progressbar
//import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber




class Calificacion : BaseChatActivity() {
    private lateinit var webView: WebView
    private val PREFS_NAME = "preferences"
    private val PREF_ID = "id"
    private var UnameValue: String? = null
    private val DefaultIDValue = ""
    private var currentDialogId: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentDialogId = intent.getStringExtra("dialogid")


        setContentView(R.layout.activity_calificacion)

        webView = findViewById(R.id.web3view)

        webView.settings.javaScriptEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.addJavascriptInterface(WebAppInterface(this), "Android");
        webView.settings.domStorageEnabled = true

        if(isConnected(this)) {
            webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK;
            webView.setWebViewClient(object : WebViewClient() {
            })

            val settings = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            )

            // Get value
            UnameValue = settings.getString(PREF_ID, DefaultIDValue)
            val url2 = "https://app3.q-far.cl/qfs/movil/usuario/calificar.php?idd=$currentDialogId" + "&idc=" + UnameValue;
            Log.d("link", url2)
            webView.loadUrl(url2)








            webView.webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: JsResult?
                ): Boolean {
                    // Mostrar el mensaje de alerta en la aplicación
                    AlertDialog.Builder(this@Calificacion)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            result?.confirm()
                        }
                        .show()
                    return true
                }
            }





        }else{
            webView.settings.cacheMode = WebSettings.LOAD_CACHE_ONLY
            val builder1 = AlertDialog.Builder(this@Calificacion)
            builder1.setMessage("Se encuentra sin conexión para revisar la información ")
            builder1.setCancelable(true)

            builder1.setPositiveButton(
                "Aceptar"
            ) { dialog, id ->
                dialog.cancel()
                finish()
            }


            val alert11 = builder1.create()
            alert11.show()
        }

    }

    override fun onBackPressed() {
        // Bloquear la acción de retroceso (no hacer nada)
        // Elimina el siguiente comentario si deseas mostrar un mensaje al usuario antes de bloquear el botón:
        // Toast.makeText(this, "Presiona dos veces para salir", Toast.LENGTH_SHORT).show()
    }

}
/*class WebAppInterface2 internal constructor(var mContext: Context) {
    var data: String? = null
    @JavascriptInterface
    fun sendData(data: String?) {
        //Get the string value to process
        if (data != null) {
            Log.d("Data", data)
            val parts: List<String> = data.split(",");
            var dialog2 = parts[0];
            var userid = parts[1];
            var ctoken = parts[2];
            var username2 = parts[3];


            //Log.d("Data", data)
            (mContext as Calificacion).web3view?.post {

                (mContext as MainActivity).makeLogin(dialog2, username2, dialog2)
            }

        }
    }
}*/

