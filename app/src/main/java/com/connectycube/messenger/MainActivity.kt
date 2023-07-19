package com.connectycube.messenger

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import androidx.core.content.ContextCompat.startActivity
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
import kotlinx.android.synthetic.main.activity_chatdialogs.*
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login.progressbar
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature



class MainActivity : BaseChatActivity() {
    private lateinit var webView: WebView
    private val PREFS_NAME = "preferences"
    private val PREF_ID = "id"
    private var UnameValue: String? = null
    private val DefaultIDValue = ""
    private var chat_dialog = ""
    private lateinit var users: ArrayList<ConnectycubeUser>
    private lateinit var users2: ArrayList<ConnectycubeUser>
    private lateinit var adapter: ArrayAdapter<String>
    // Variable para manejar la solicitud de carga de archivos.
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private var fileUploadCallbackLegacy: ValueCallback<Uri>? = null
    private val FILE_CHOOSER_RESULT_CODE = 1001

    private val chatDialogListViewModel: ChatDialogListViewModel by viewModels {
        InjectorUtils.provideChatDialogListViewModelFactory(this)
    }

    private lateinit var chatDialogAdapter: ChatDialogAdapter
    private val FILE_UPLOAD_PERMISSION_REQUEST_CODE = 1

    // Método para solicitar permisos de almacenamiento en tiempo de ejecución
    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                FILE_UPLOAD_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Método para manejar el resultado de la solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            FILE_UPLOAD_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFileChooser()
                } else {
                    // Permiso de almacenamiento denegado, mostrar un mensaje o realizar otra acción
                }
                return
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // Método para abrir el selector de archivos
    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Establecer el color deseado para la barra de navegación
            window.navigationBarColor = ContextCompat.getColor(this, R.color.azul)
        }

        webView = findViewById(R.id.web2view)
        // Habilitar la selección de archivos en el WebView
        webView.settings.allowFileAccess = true
        webView.settings.allowFileAccessFromFileURLs = true
        webView.settings.allowUniversalAccessFromFileURLs = true
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.addJavascriptInterface(WebAppInterface(this), "Android");



        if(isConnected(this)) {
            webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK;
            webView.setWebViewClient(object : WebViewClient() {


                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    if (url.startsWith("https://app3.q-far.cl/qfs/movil/usuario/datos-medicos.php")) {
                        val settings = getSharedPreferences(
                            PREFS_NAME,
                            MODE_PRIVATE
                        )
                        val editor = settings.edit()
                        val resultado = url.substring(url.lastIndexOf("?idc=") + 5)
                        editor.putString(PREF_ID, resultado)
                        Log.d("codigo", resultado)
                        editor.commit()
                    }
                    Log.d("URL", url)
                    return false
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    // Modificar el HTML antes de mostrarlo en el WebView
                    webView.loadUrl("""
                    javascript:(function() {
                        var inputs = document.getElementsByTagName('input');
                        for (var i = 0; i < inputs.length; i++) {
                            inputs[i].setAttribute('autofill', 'off');
                        }
                    })();
                """.trimIndent())
                }



            })

            webView.webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: JsResult?
                ): Boolean {
                    // Mostrar el mensaje de alerta en la aplicación
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            result?.confirm()
                        }
                        .show()
                    return true
                }

                // Este método se llama cuando se selecciona un archivo en el WebView
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    fileUploadCallback = filePathCallback
                    // Solicitar permisos de almacenamiento antes de abrir el selector de archivos
                    requestStoragePermissions()
                    return true
                }

            }

            /*myWebView.setWebViewClient(new WebViewClient(){
                @Override
                public  boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if ((url.startsWith("http:")
                            || url.startsWith("https:")) && !url.contains("www.mipcitrus.cl") ) {
                        //here open external links in external browser or app
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }else{
                        return false;
                    }

                }
            });*/
            val settings = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            )

            // Get value


            // Get value
            UnameValue = settings.getString(PREF_ID, DefaultIDValue)
            Log.d("uname", UnameValue!!)

            if (!UnameValue.equals("", ignoreCase = true)) {
                val url2 =
                    "https://app3.q-far.cl/qfs/movil/usuario/index.php?idc=$UnameValue"
                Log.d("link", url2)
                webView.loadUrl(url2)
            } else {
                webView.loadUrl("https://app3.q-far.cl/qfs/movil/usuario/index2.php")
            }


        }else{
            webView.settings.cacheMode = WebSettings.LOAD_CACHE_ONLY
            val builder1 = AlertDialog.Builder(this@MainActivity)
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

    fun makeLogin(dialog: String, username: String, chat_dialog2: String) {
        if (SharedPreferencesManager.getInstance(applicationContext).currentUserExists()) {
//            showProgress(progressbar)
            val user = SharedPreferencesManager.getInstance(applicationContext).getCurrentUser()
//            text_view.text = getString(R.string.user_logged_in, user.fullName ?: user.login)
            startChatDialogsActivity(chat_dialog2)
        } else {
            //var usuario = ConnectycubeUser(id=null, createdAt=null, updatedAt=null, fullName='null', email='null', login='cquezada', phone='null', website='null', lastRequestAt='null', externalId=null, facebookId=null, twitterId=null, blobId=null, tags='null', password='12345678', oldPassword='null', customData='null', avatar='null')
            //users2= ArrayList[ConnectycubeUser{id=null, createdAt=null, updatedAt=null, fullName='null', email='null', login='cquezada', phone='null', website='null', lastRequestAt='null', externalId=null, facebookId=null, twitterId=null, blobId=null, tags='null', password='12345678', oldPassword='null', customData='null', avatar='null'}]
            users2 = ArrayList<ConnectycubeUser>()
            users2.add(ConnectycubeUser(username, "12345678"))
            users2.add(ConnectycubeUser("analogising", "12345678"))
            //chat_dialog = chat_dialog2
            //Log.d("users2", users2.toString())
            //loginTo(users2)
            initUsers()
            initUserAdapter()

        }
    }

    private fun loginTo(user: ConnectycubeUser) {
        //showProgress(progressbar)
        Log.d("called loginTo user",user.toString())
        val usersLogins = ArrayList<String>()
        users.forEach { usersLogins.add(it.login) }
        Log.d("called loginTo users", users.toString())
        val userListViewModel: UserListViewModel by viewModels {
            InjectorUtils.provideUserListViewModelFactory(this, usersLogins)

        }
        Log.d("users userListViewModel", userListViewModel.toString())
        Log.d("loginTo usersLogins = ", usersLogins.toString())
        fun errorProcessing(msg: String) {
            //hideProgress(progressbar)
            Toast.makeText(
                applicationContext,
                msg,
                Toast.LENGTH_SHORT
            ).show()
        }
        userListViewModel.getUsers().observe(this) { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    val listUser = resource.data
                    Log.d("listUser", listUser.toString());

                    Log.d("called login LoginTo", user.login.toString())

                    val userRaw: User? = listUser?.find { it.login == user.login }
                    if (userRaw != null) {
                        Timber.d("proceed loginTo user= $userRaw, conUser= ${userRaw.conUser}")
                        val userToLogin = userRaw.conUser.also { it.password = user.password }
                        Log.d("loginTo", userToLogin.toString());
                        signInRestIdNeed(userToLogin)
                    } else {
                        errorProcessing(getString(R.string.loading_users_empty))
                    }
                }
                Status.ERROR ->
                    errorProcessing(getString(R.string.loading_users_error, resource.message))
                Status.LOADING -> {
                }
            }
        }
    }

    private fun isSignedInREST(user: ConnectycubeUser) =
        ConnectycubeSessionManager.getInstance().sessionParameters?.userId == user.id ?: false

    private fun signInRestIdNeed(user: ConnectycubeUser) {
        if (!isSignedInREST(user)) {
            Log.d("signInRestIdNeed", user.toString());
            signInRest(user)

        } else {
            //startChatDialogsActivity()
        }
    }

    private fun signInRest(user: ConnectycubeUser) {
        ConnectycubeUsers.signIn(user).performAsync(object : EntityCallback<ConnectycubeUser> {
            override fun onSuccess(conUser: ConnectycubeUser, args: Bundle) {
                Log.d("signInRest", user.toString());
                SharedPreferencesManager.getInstance(applicationContext).saveCurrentUser(user)

                //startChatDialogsActivity()
            }

            override fun onError(ex: ResponseException) {
                hideProgress(progressbar)
                Toast.makeText(
                    applicationContext,
                    getString(R.string.login_chat_login_error, ex.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    /*fun startDialogsScreen() {
        //hideProgress(progressbar)

        startDialogs()
    }

    fun startDialogs() {
        Timber.d("ChatDialogActivity.start")
        startChatDialogsActivity()
    }*/

    private fun startChatDialogsActivity(chat2: String) {
        val intent = Intent(this, ChatDialogActivity::class.java).apply {
            putExtra(EXTRA_DIALOG_ID, chat2)
            putExtra("dialogid", chat2)
        }

        startActivity(intent)
        //finish()
    }

    /*private fun startChatActivity(chat: String) {
        val intent = Intent(this, ChatMessageActivity::class.java).apply {
            putExtra(EXTRA_CHAT, chat)
        }
        startActivityForResult(intent, REQUEST_CHAT_DIALOG_ID)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }*/

    private fun initUsers() {
        users = users2
        Timber.d("called initUsers users = $users")

    }

    private fun initUserAdapter() {
        val userList: ArrayList<String> = ArrayList(users.size)
        users.forEach { userList.add(it.login)
            Log.d("initUserAdapter222", it.login)}
        var position = 0
        Timber.d("called initUserAdapter users = $users")
        Log.d("users_position333", position.toString())
        Log.d("users_position222", users[position].toString())

        loginTo(users[position])

    }

    //NUEVO
    private fun startChatActivity(chat: ConnectycubeChatDialog) {
        val intent = Intent(this, ChatMessageActivity::class.java).apply {
            putExtra(EXTRA_CHAT, chat)
        }
        startActivityForResult(intent, REQUEST_CHAT_DIALOG_ID)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    //FIN NUEVO
    // Método para manejar el resultado de la selección de archivos
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
                // Obtiene la URI del archivo seleccionado
                val uri = data?.data
                if (uri != null) {
                    // Devuelve la URI al WebView
                    fileUploadCallback?.onReceiveValue(arrayOf(uri))
                    fileUploadCallback = null
                }
            } else {
                // Cancelar la selección de archivos, devuelve nulo al WebView
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = null
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }



}

fun isConnected(context: Context): Boolean {

    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

    val actNetInfo = connectivityManager!!.activeNetworkInfo

    return actNetInfo != null && actNetInfo.isConnected
}



class WebAppInterface internal constructor(var mContext: Context) {
    var data: String? = null
    var data2: String? = null
    @JavascriptInterface
    fun sendData(data: String?) {
        //Get the string value to process
        if (data != null) {
            Log.d("Data", data)
            val parts: List<String> = data.split(",");
            var dialog2 = parts[0];
            var nombre = parts[1];
            var avatar = parts[2];
            var userid = parts[3];
            var ctoken = parts[4];
            var username2 = parts[5];

            // Obtén una instancia de SharedPreferences
            val sharedPreferences = mContext.getSharedPreferences("variables_chat", Context.MODE_PRIVATE)

            // Obtiene un editor para modificar los valores en SharedPreferences
            val editor = sharedPreferences.edit()

            // Escribe el valor de la variable
            editor.putString("nombre_qf", nombre)
            editor.putString("avatar_qf", avatar)

            // Guarda los cambios
            editor.apply()


            //Log.d("Data", data)
            (mContext as MainActivity).web2view?.post {

                (mContext as MainActivity).makeLogin(dialog2, username2, dialog2)
            }

        }


    }
    @JavascriptInterface
    fun sendData2(data2: String?) {
        //Get the string value to process
        if (data2 != null) {
            Log.d("Data", data2)
            val parts2: List<String> = data2.split(",");
            var dialog2 = parts2[0];
            var userid = parts2[1];
            var ctoken = parts2[2];
            var username2 = parts2[3];


            val intent = Intent(mContext, Calificacion::class.java)
            intent.putExtra("dialogid", dialog2)
            mContext.startActivity(intent)
            //finish()

        }
    }
}