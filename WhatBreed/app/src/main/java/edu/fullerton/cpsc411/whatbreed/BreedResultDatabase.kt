package edu.fullerton.cpsc411.whatbreed

import android.content.Context
import androidx.room.*


@Database(entities=arrayOf(BreedResult::class),version=1)
@TypeConverters(Converter::class)
abstract class BreedResultDatabase: RoomDatabase(){

    abstract fun BreedResultDao(): BreedResultDao


    companion object{
         var instance: BreedResultDatabase?=null


         fun getInstance(context:Context): BreedResultDatabase?{
             if(instance==null) {
                 synchronized(BreedResultDatabase::class) {
                     if(instance==null){
                        instance=Room.databaseBuilder(context.applicationContext, BreedResultDatabase::class.java, "breedResults.db").build()
                     }
                 }
             }
             return instance
         }
    }
}