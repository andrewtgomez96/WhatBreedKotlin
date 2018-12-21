package edu.fullerton.cpsc411.whatbreed

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_results_history.*

class ResultsHistoryActivity : AppCompatActivity() {

    private var  histdb: BreedResultDatabase? = null
    private var resultDao: BreedResultDao? = null
    var newBreedResult=BreedResult()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results_history)

        Thread {

            histdb = BreedResultDatabase.getInstance(context = this)
            resultDao = histdb?.BreedResultDao()


            val breedList: List<BreedResult>? = resultDao?.getAllBreedResults()
            var oneIndex: String = ""
            val arrayConversion = arrayOfNulls<String>(breedList!!.size)
            for(i in breedList!!.indices){
                oneIndex += breedList.get(i).bresult
                Log.d("results", breedList.get(i).bresult)
                oneIndex += breedList.get(i).picPath
                oneIndex += breedList.get(i).timeFound
                Log.d("results", breedList.get(i).timeFound)
                arrayConversion[i] = oneIndex
            }


            val adapter = ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1,
                    arrayConversion)

            hist_listview.adapter = adapter

        }.start()
    }
}
