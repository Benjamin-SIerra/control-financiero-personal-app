package com.benjamin.controlfinanciero.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="profile") data class Profile(@PrimaryKey val id:Int=1,val incomeMinor:Long=0,val currency:String="MXN",val payFrequency:String="QUINCENAL",val anchorEpochDay:Long=0)
@Entity(tableName="fixed_expenses",indices=[Index("enabled")]) data class FixedExpense(@PrimaryKey(autoGenerate=true) val id:Long=0,val title:String,val amountMinor:Long,val currency:String,val exchangeRate:Double,val frequency:String,val dueLabel:String,val enabled:Boolean=true,val cardId:Long?=null)
@Entity(tableName="cards",indices=[Index("name",unique=true)]) data class CreditCard(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String,val limitMinor:Long,val usedMinor:Long=0,val cutDay:Int,val dueDay:Int,val salaryFunded:Boolean,val expectedPaymentMinor:Long=0,val currency:String="MXN",val exchangeRate:Double=1.0)
@Entity(tableName="expenses",indices=[Index("dateEpochMillis"),Index("cardId")],foreignKeys=[ForeignKey(entity=CreditCard::class,parentColumns=["id"],childColumns=["cardId"],onDelete=ForeignKey.SET_NULL)]) data class Expense(@PrimaryKey(autoGenerate=true) val id:Long=0,val concept:String,val category:String,val amountMinor:Long,val currency:String,val exchangeRate:Double,val dateEpochMillis:Long=System.currentTimeMillis(),val cardId:Long?=null,val note:String="")
@Entity(tableName="card_payments",indices=[Index("cardId"),Index("dateEpochMillis")],foreignKeys=[ForeignKey(entity=CreditCard::class,parentColumns=["id"],childColumns=["cardId"],onDelete=ForeignKey.CASCADE)]) data class CardPayment(@PrimaryKey(autoGenerate=true) val id:Long=0,val cardId:Long,val amountMinor:Long,val dateEpochMillis:Long=System.currentTimeMillis())
@Entity(tableName="goals",indices=[Index("deadlineEpochDay")]) data class SavingsGoal(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String,val targetMinor:Long,val savedMinor:Long=0,val currency:String,val exchangeRate:Double=1.0,val deadlineEpochDay:Long)

@Dao interface FinanceDao {
 @Query("SELECT * FROM profile WHERE id=1") fun profile():Flow<Profile?>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun saveProfile(p:Profile)
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
 @Query("UPDATE goals SET savedMinor=savedMinor+:amount WHERE id=:id AND savedMinor+:amount<=targetMinor") suspend fun contributeGoal(id:Long,amount:Long):Int
 @Query("DELETE FROM goals WHERE id=:id") suspend fun deleteGoal(id:Long)
}

@Database(entities=[Profile::class,FixedExpense::class,CreditCard::class,Expense::class,CardPayment::class,SavingsGoal::class],version=1,exportSchema=true)
abstract class FinanceDatabase:RoomDatabase(){ abstract fun dao():FinanceDao; companion object { fun create(context:android.content.Context)=Room.databaseBuilder(context,FinanceDatabase::class.java,"control-financiero-personal.db").build() } }
