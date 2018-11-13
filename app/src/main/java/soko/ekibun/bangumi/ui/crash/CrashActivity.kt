package soko.ekibun.bangumi.ui.crash

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import kotlinx.android.synthetic.main.activity_crash.*
import retrofit2.Call
import soko.ekibun.bangumiplayer.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.xxxlin.Xxxlin
import soko.ekibun.bangumi.api.xxxlin.bean.BaseResult
import soko.ekibun.bangumi.util.AppUtil

class CrashActivity : AppCompatActivity() {

    var uploadCall : Call<BaseResult>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)
        setSupportActionBar(toolbar)
        val content = intent.getStringExtra(EXTRA_CRASH)
        item_content.text = content
        item_upload.setOnClickListener {
            uploadCall?.cancel()
            uploadCall = Xxxlin.createInstance().crashReport(content, AppUtil.getVersionCode(this), AppUtil.getVersionName(this))
            uploadCall?.enqueue(ApiHelper.buildCallback(this, {
                Snackbar.make(item_upload, if(it.code == 0) "日志上传成功" else "日志上传失败：${it.msg}", Snackbar.LENGTH_SHORT).show()
                item_upload.setOnClickListener{}
            }, {}))
        }
    }

    companion object {
        private const val EXTRA_CRASH = "extraCrash"
        fun startActivity(context: Context, crash: String){
            val intent = Intent(context.applicationContext, CrashActivity::class.java)
            intent.putExtra(EXTRA_CRASH, crash)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}