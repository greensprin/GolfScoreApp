package com.example.sampledb

import androidx.room.*

// ===== Data Base =====
// DataBaseで管理する内容（列）を記載する
@Entity
data class User (
    @PrimaryKey val uid : Int,
    @ColumnInfo(name = "message") val message : String?
)

// Sqliteの操作を行う
@Dao
interface UserDao {
    // すべてのユーザーを取得する
    @Query("SELECT * FROM user")
    fun getAll() : List<User>

    // uidで検索をかけ、１つ取得する
    @Query("SELECT * FROM user WHERE uid LIKE :id LIMIT 1")
    fun findByUid(id: Int) : User

    // messageから検索をかけて、１つ取得する（未使用）
    @Query("SELECT * FROM user WHERE message LIKE :msg LIMIT 1")
    fun findByName(msg: String) : User

    // 最後尾のuidを取得する（追加の際に、最後尾の次のuidを設定して追加したいため）
    @Query("SELECT max(uid) FROM user")
    fun getLastInsertRowid() : Int

    // 新規ユーザー追加（uid被ってたら無視する）
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(user: User)

    // データの更新をかける
    @Update
    fun update(user: User)

    // データ削除
    @Delete
    fun delete(user: User)
}

// 不明・・・
@Database(entities = arrayOf(User::class), version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun UserDao(): UserDao
}
