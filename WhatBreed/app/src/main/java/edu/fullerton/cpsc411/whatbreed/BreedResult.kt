package edu.fullerton.cpsc411.whatbreed

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

//This is the entity table containing single instances of past breed search results.


@Entity(tableName = "breedResultTable")
data class BreedResult(
        @PrimaryKey var timeFound: String="",
        @ColumnInfo(name="BreedResult")var bresult:String="",
        @ColumnInfo(name="imagePath")var picPath:String=""

){
    constructor():this("","","")
}
