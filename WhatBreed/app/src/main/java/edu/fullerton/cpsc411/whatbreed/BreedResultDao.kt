package edu.fullerton.cpsc411.whatbreed

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BreedResultDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(BreedResult: BreedResult);

    @Query("SELECT * FROM breedResultTable")
    fun getAllBreedResults(): LiveData<List<BreedResult>>


}