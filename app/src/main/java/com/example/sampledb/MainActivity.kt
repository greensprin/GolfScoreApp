package com.example.sampledb

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.room.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    companion object {
        lateinit var db: AppDatabase // database格納用
        lateinit var msgelems: List<User> // databaseから取得したデータ格納用（別スレッドで取得したものをUI threadでも使えるようにするため)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // View取得
        val edit_text1 = findViewById<EditText>(R.id.edit_text1)
        val add_btn    = findViewById<Button>(R.id.button1)
        val show_btn   = findViewById<Button>(R.id.button2)
        val del_btn    = findViewById<Button>(R.id.button3)
        val tbl_layout = findViewById<TableLayout>(R.id.table_layout1)

        // DataBase起動
        runBlocking {
            GlobalScope.launch {
                db = Room.databaseBuilder (
                    applicationContext,
                    AppDatabase::class.java,
                    "message-db"
                ).build()

                val dao = db.UserDao()
            }.join()
        }

        // ボタン押下
        add_btn.setOnClickListener  { addBtnClick(edit_text1) }
        show_btn.setOnClickListener { showBtnClick(this, tbl_layout) }
        del_btn.setOnClickListener  { delBtnClick(edit_text1) }
    }
}

// ===== Button Function =====
fun addBtnClick(edit_text1: EditText) {
    GlobalScope.launch {
        val text = edit_text1.text.toString()

        if (text != "") {
            val dao = MainActivity.db.UserDao()
            val num = dao.getAll().size
            var cnt = num // １つもデータがない場合は、uid = 0となるようにnumをそのまま使う (error回避用)
            if (num != 0) cnt = dao.getLastInsertRowid() + 1 // １つでもデータが追加されていれば最後尾のuidの次のuidとして登録する
            val newUser = User(cnt, text)
            dao.insertIgnore(newUser)
        }
    }
}

fun showBtnClick(context: Context, tbl_layout: TableLayout) {
    // DataBaseからデータ取得を別スレッドで行う
    // その際、取得したデータをcompanion objectで宣言したmsgelemsに格納する
    runBlocking {
        GlobalScope.launch {
            val dao = MainActivity.db.UserDao()
            MainActivity.msgelems = dao.getAll()
        }.join()
    }

    // Viewの更新はMainThread側で行う
    // databaseはMainThreadで触ることができないが、Viewの更新はMainThreadでしかできないため
    // うまくやる方法はほかにもあるかもしれないが、companion objectであれば、別threadの結果をMainThreadでも使えたので、現状はこの形にしている
    tbl_layout.removeAllViews()
    for (elem in MainActivity.msgelems) {
        val uid = elem.uid
        val msg = elem.message

        val tr = TableRow(context)

        val text_view = TextView(context)
        text_view.text = "${uid} ${msg}"
        text_view.textSize = 24f

        tr.addView(text_view)
        tbl_layout.addView(tr)
    }
}

fun delBtnClick(edit_text1: EditText) {
    runBlocking {
        GlobalScope.launch {
            val text = edit_text1.text.toString().toIntOrNull()
            val dao = MainActivity.db.UserDao()
            val last_uid = dao.getLastInsertRowid()
            if (text != null && text <= last_uid) {
                val user = dao.findByUid(text)
                if (user != null) {
                    dao.delete(user)
                } else {
                    Log.i("DELETE DATABASE", "This uid User is not found.")
                }
            } else {
                Log.i("DELETE DATABASE", "This String is not integer.")
            }
        }
    }
}