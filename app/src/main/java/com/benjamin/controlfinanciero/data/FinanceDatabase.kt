package com.benjamin.controlfinanciero.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor

@Entity(tableName="profile")
data class Profile(
    @PrimaryKey val id:Int=1,
    val incomeMinor:Long=0,
    val currency:String="MXN",
    val payFrequency:String="QUINCENAL",
    val anchorEpochDay:Long=0
)

@Entity(tableName="income_periods",indices=[Index(value=["startEpochDay","endEpochDay"])])
data class IncomePeriod(
    @PrimaryKey(autoGenerate=true) val id:Long=0,
    val startEpochDay:Long,
    val endEpochDay:Long,
    val incomeMinor:Long,
    val currency:String,
    val frequency:String
)

@Entity(tableName="fixed_expenses",indices=[Index("enabled")])
data class FixedExpense(@PrimaryKey(autoGenerate=true) val id:Long=0,val title:String,val amountMinor:Long,val currency:String,val exchangeRate:Double,val frequency:String,val dueLabel:String,val enabled:Boolean=true,val cardId:Long?=null)
@Entity(tableName="cards",indices=[Index("name",unique=true)]) data class CreditCard(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String,val limitMinor:Long,val usedMinor:Long=0,val cutDay:Int,val dueDay:Int,val salaryFunded:Boolean,val expectedPaymentMinor:Long=0,val currency:String="MXN",val exchangeRate:Double=1.0)
@Entity(tableName="expenses",indices=[Index("dateEpochMillis"),Index("cardId")],foreignKeys=[ForeignKey(entity=CreditCard::class,parentColumns=["id"],childColumns=["cardId"],onDelete=ForeignKey.SET_NULL)]) data class Expense(@PrimaryKey(autoGenerate=true) val id:Long=0,val concept:String,val category:String,val amountMinor:Long,val currency:String,val exchangeRate:Double,val dateEpochMillis:Long=System.currentTimeMillis(),val cardId:Long?=null,val note:String="")
@Entity(tableName="card_payments",indices=[Index("cardId"),Index("dateEpochMillis")],foreignKeys=[ForeignKey(entity=CreditCard::class,parentColumns=["id"],childColumns=["cardId"],onDelete=ForeignKey.CASCADE)]) data class CardPayment(@PrimaryKey(autoGenerate=true) val id:Long=0,val cardId:Long,val amountMinor:Long,val dateEpochMillis:Long=System.currentTimeMillis())
@Entity(tableName="goals",indices=[Index("deadlineEpochDay")]) data class SavingsGoal(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String,val targetMinor:Long,val savedMinor:Long=0,val currency:String,val exchangeRate:Double=1.0,val deadlineEpochDay:Long,val status:String="ACTIVE")

@Dao interface FinanceDao {
 @Query("SELECT * FROM profile WHERE id=1") fun profile():Flow<Profile?>
 @Query("SELECT * FROM profile WHERE id=1") suspend fun profileOnce():Profile?
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun saveProfile(p:Profile)
 @Query("SELECT * FROM income_periods ORDER BY startEpochDay DESC,id DESC") fun incomePeriods():Flow<List<IncomePeriod>>
 @Query("SELECT * FROM income_periods ORDER BY startEpochDay DESC,id DESC") suspend fun incomePeriodsOnce():List<IncomePeriod>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun saveIncomePeriod(period:IncomePeriod):Long
 @Query("DELETE FROM income_periods WHERE id=:id") suspend fun deleteIncomePeriod(id:Long):Int
 @Query("SELECT * FROM fixed_expenses ORDER BY enabled DESC,title") fun fixed():Flow<List<FixedExpense>>
 @Insert suspend fun addFixed(x:FixedExpense):Long
 @Query("UPDATE fixed_expenses SET enabled=:enabled WHERE id=:id") suspend fun setFixedEnabled(id:Long,enabled:Boolean)
 @Query("DELETE FROM fixed_expenses WHERE id=:id") suspend fun deleteFixed(id:Long)
 @Query("SELECT * FROM expenses WHERE dateEpochMillis>=:from AND dateEpochMillis<:to ORDER BY dateEpochMillis DESC") fun expenses(from:Long,to:Long):Flow<List<Expense>>
 @Query("SELECT COALESCE(SUM(amountMinor),0) FROM expenses WHERE dateEpochMillis>=:from AND dateEpochMillis<:to") fun spent(from:Long,to:Long):Flow<Long>
 @Insert suspend fun addExpense(e:Expense):Long
 @Query("SELECT * FROM cards ORDER BY name") fun cards():Flow<List<CreditCard>>
 @Query("SELECT * FROM cards WHERE id=:id") suspend fun card(id:Long):CreditCard?
 @Insert suspend fun addCard(c:CreditCard):Long
 @Insert suspend fun addPayment(p:CardPayment):Long
 @Query("UPDATE cards SET usedMinor=usedMinor+:delta WHERE id=:id AND usedMinor+:delta>=0 AND usedMinor+:delta<=limitMinor") suspend fun changeCardUsed(id:Long,delta:Long):Int
 @Query("DELETE FROM cards WHERE id=:id") suspend fun deleteCard(id:Long)
 @Query("SELECT * FROM goals ORDER BY deadlineEpochDay") fun goals():Flow<List<SavingsGoal>>
 @Insert suspend fun addGoal(g:SavingsGoal):Long
 @Query("UPDATE goals SET name=:name,targetMinor=:target,savedMinor=:saved,currency=:currency,exchangeRate=:rate,deadlineEpochDay=:deadline WHERE id=:id AND status='ACTIVE'") suspend fun updateGoal(id:Long,name:String,target:Long,saved:Long,currency:String,rate:Double,deadline:Long):Int
 @Query("UPDATE goals SET savedMinor=savedMinor+:amount,status=CASE WHEN savedMinor+:amount>=targetMinor THEN 'COMPLETED' ELSE status END WHERE id=:id AND status='ACTIVE' AND savedMinor+:amount<=targetMinor") suspend fun contributeGoal(id:Long,amount:Long):Int
 @Query("UPDATE goals SET status=:status WHERE id=:id AND status='ACTIVE'") suspend fun setGoalStatus(id:Long,status:String):Int
 @Query("DELETE FROM goals WHERE id=:id") suspend fun deleteGoal(id:Long)
}

@Database(entities=[Profile::class,IncomePeriod::class,FixedExpense::class,CreditCard::class,Expense::class,CardPayment::class,SavingsGoal::class],version=3,exportSchema=true)
abstract class FinanceDatabase:RoomDatabase(){
 abstract fun dao():FinanceDao
 companion object {
  val MIGRATION_1_2:Migration=object:Migration(1,2){
   override fun migrate(database:SupportSQLiteDatabase){
    database.execSQL("CREATE TABLE IF NOT EXISTS `income_periods` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startEpochDay` INTEGER NOT NULL, `endEpochDay` INTEGER NOT NULL, `incomeMinor` INTEGER NOT NULL, `currency` TEXT NOT NULL, `frequency` TEXT NOT NULL)")
    database.execSQL("CREATE INDEX IF NOT EXISTS `index_income_periods_startEpochDay_endEpochDay` ON `income_periods` (`startEpochDay`, `endEpochDay`)")
    database.query("SELECT `incomeMinor`,`currency`,`payFrequency`,`anchorEpochDay` FROM `profile` WHERE `id`=1").use { cursor ->
     if(cursor.moveToFirst()){
      val amount=cursor.getLong(0)
      val currency=cursor.getString(1)
      val frequency=cursor.getString(2)
      val anchorEpoch=cursor.getLong(3)
      val perYear=when(frequency){"SEMANAL"->52.0;"CATORCENAL"->26.0;"QUINCENAL"->24.0;"MENSUAL"->12.0;else->24.0}
      val durationMillis=(365.2425/perYear*86_400_000.0).toLong().coerceAtLeast(1)
      val zone=ZoneId.systemDefault()
      val anchorMillis=LocalDate.ofEpochDay(anchorEpoch).atStartOfDay(zone).toInstant().toEpochMilli()
      val nowMillis=LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
      val index=floor((nowMillis-anchorMillis).toDouble()/durationMillis).toLong()
      val startMillis=anchorMillis+index*durationMillis
      val start=Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
      val end=Instant.ofEpochMilli(startMillis+durationMillis-1).atZone(zone).toLocalDate()
      database.execSQL("INSERT INTO `income_periods` (`startEpochDay`,`endEpochDay`,`incomeMinor`,`currency`,`frequency`) VALUES (?,?,?,?,?)",arrayOf(start.toEpochDay(),end.toEpochDay(),amount,currency,frequency))
     }
    }
   }
  }
  val MIGRATION_2_3:Migration=object:Migration(2,3){override fun migrate(database:SupportSQLiteDatabase){database.execSQL("ALTER TABLE `goals` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'ACTIVE'");database.execSQL("UPDATE goals SET status='COMPLETED' WHERE savedMinor >= targetMinor")}}
  fun create(context:Context)=Room.databaseBuilder(context,FinanceDatabase::class.java,"control-financiero-personal.db").addMigrations(MIGRATION_1_2,MIGRATION_2_3).build()
 }
}
