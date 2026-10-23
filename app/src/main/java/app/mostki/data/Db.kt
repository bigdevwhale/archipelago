package app.mostki.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update

@Entity(tableName = "island")
data class IslandRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val detail: String,
    val traits: String,
    val harbor: Long?,
    val angle: Double,
    val made: Long,
    val anchoredAt: Long?,
)

@Entity(tableName = "contact", indices = [Index("island")])
data class ContactRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val island: Long,
    val at: Long,
    val minutes: Int,
    val what: String,
    val kind: String,
    val gathering: Long?,
)

@Entity(tableName = "ferry")
data class FerryRow(@PrimaryKey(autoGenerate = true) val id: Long = 0, val a: Long, val b: Long, val at: Long)

@Entity(tableName = "harbor")
data class HarborRow(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val made: Long)

@Entity(tableName = "visit", indices = [Index("harbor")])
data class VisitRow(@PrimaryKey(autoGenerate = true) val id: Long = 0, val harbor: Long, val at: Long)

@Entity(tableName = "sortie")
data class SortieRow(@PrimaryKey(autoGenerate = true) val id: Long = 0, val at: Long, val rung: Int, val task: Int, val ok: Boolean)

@Entity(tableName = "memo", indices = [Index("island")])
data class MemoRow(@PrimaryKey(autoGenerate = true) val id: Long = 0, val island: Long, val text: String, val at: Long, val done: Boolean)

@Dao
interface ChartDao {
    @Query("SELECT * FROM island ORDER BY made, id") suspend fun islands(): List<IslandRow>
    @Query("SELECT * FROM contact ORDER BY at, id") suspend fun contacts(): List<ContactRow>
    @Query("SELECT * FROM ferry ORDER BY at, id") suspend fun ferries(): List<FerryRow>
    @Query("SELECT * FROM harbor ORDER BY made, id") suspend fun harbors(): List<HarborRow>
    @Query("SELECT * FROM visit ORDER BY at, id") suspend fun visits(): List<VisitRow>
    @Query("SELECT * FROM sortie ORDER BY at, id") suspend fun sorties(): List<SortieRow>
    @Query("SELECT * FROM memo ORDER BY at, id") suspend fun memos(): List<MemoRow>

    @Insert suspend fun insert(r: IslandRow): Long
    @Insert suspend fun insert(r: ContactRow): Long
    @Insert suspend fun insert(r: FerryRow): Long
    @Insert suspend fun insert(r: HarborRow): Long
    @Insert suspend fun insert(r: VisitRow): Long
    @Insert suspend fun insert(r: SortieRow): Long
    @Insert suspend fun insert(r: MemoRow): Long
    @Update suspend fun update(r: IslandRow)
    @Update suspend fun update(r: MemoRow)

    @Query("UPDATE island SET anchoredAt = :at WHERE id = :id AND anchoredAt IS NULL") suspend fun anchor(id: Long, at: Long)
    @Query("UPDATE island SET harbor = NULL WHERE harbor = :harbor") suspend fun unharbor(harbor: Long)
    @Query("DELETE FROM island WHERE id = :id") suspend fun deleteIsland(id: Long)
    @Query("DELETE FROM contact WHERE island = :id") suspend fun deleteContacts(id: Long)
    @Query("DELETE FROM contact WHERE id = :id") suspend fun deleteContact(id: Long)
    @Query("DELETE FROM memo WHERE island = :id") suspend fun deleteMemos(id: Long)
    @Query("DELETE FROM memo WHERE id = :id") suspend fun deleteMemo(id: Long)
    @Query("DELETE FROM ferry WHERE a = :id OR b = :id") suspend fun deleteFerries(id: Long)
    @Query("DELETE FROM harbor WHERE id = :id") suspend fun deleteHarbor(id: Long)
    @Query("DELETE FROM visit WHERE harbor = :id") suspend fun deleteVisits(id: Long)

    @Query("DELETE FROM island") suspend fun clearIslands()
    @Query("DELETE FROM contact") suspend fun clearContacts()
    @Query("DELETE FROM ferry") suspend fun clearFerries()
    @Query("DELETE FROM harbor") suspend fun clearHarbors()
    @Query("DELETE FROM visit") suspend fun clearVisits()
    @Query("DELETE FROM sortie") suspend fun clearSorties()
    @Query("DELETE FROM memo") suspend fun clearMemos()

    @Transaction
    suspend fun clear() {
        clearIslands(); clearContacts(); clearFerries(); clearHarbors(); clearVisits(); clearSorties(); clearMemos()
    }

    @Transaction
    suspend fun forget(id: Long) {
        deleteContacts(id); deleteMemos(id); deleteFerries(id); deleteIsland(id)
    }

    @Transaction
    suspend fun closeHarbor(id: Long) {
        unharbor(id); deleteVisits(id); deleteHarbor(id)
    }
}

@Database(
    entities = [IslandRow::class, ContactRow::class, FerryRow::class, HarborRow::class, VisitRow::class, SortieRow::class, MemoRow::class],
    version = 1,
    exportSchema = true,
)
abstract class ChartDb : RoomDatabase() {
    abstract fun dao(): ChartDao

    companion object {
        fun create(c: Context): ChartDb = Room.databaseBuilder(c, ChartDb::class.java, "chart.db").build()
        fun memory(c: Context): ChartDb = Room.inMemoryDatabaseBuilder(c, ChartDb::class.java).build()
    }
}
