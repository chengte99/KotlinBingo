package com.chengte99.kotlinbingo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_bingo.*
import kotlinx.android.synthetic.main.single_button.view.*

class BingoActivity : AppCompatActivity() {

    private lateinit var adapter: FirebaseRecyclerAdapter<Boolean, BingoViewHolder>
    private var is_creator: Boolean = false
    lateinit private var roomID: String
    companion object {
        val TAG = BingoActivity::class.java.simpleName
        val STATUS_INIT: Int = 0
        val STATUS_CREATED: Int = 1
        val STATUS_JOINED: Int = 2
        val STATUS_CREATOR_TURN: Int = 3
        val STATUS_JOINER_TURN: Int = 4
        val STATUS_CREATOR_BINGO: Int = 5
        val STATUS_JOINER_BINGO: Int = 6
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bingo)

        roomID = intent.getStringExtra("ROOMID")
        is_creator = intent.getBooleanExtra("IS_CREATOR", false)
        Log.d(TAG, "onCreate: $roomID / $is_creator")

        if (is_creator) {
            for (i in 1..25) {
                FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomID)
                    .child("numbers")
                    .child(i.toString())
                    .setValue(false)
            }

            FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomID)
                .child("status")
                .setValue(STATUS_CREATED)
        } else {
            FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomID)
                .child("status")
                .setValue(STATUS_JOINED)
        }

        val numberMap = mutableMapOf<Int, Int>()
        val buttons = mutableListOf<NumberButton>()
        for (i in 0..24) {
            val button = NumberButton(this)
            button.number = i + 1
            buttons.add(button)
        }
        buttons.shuffle()
        for (i in 0..24) {
            numberMap.put(buttons.get(i).number, i)
        }

        recycler.setHasFixedSize(true)
        recycler.layoutManager = GridLayoutManager(this, 5)

        val query = FirebaseDatabase.getInstance().getReference("rooms")
            .child(roomID)
            .child("numbers")
            .orderByKey()
        val options = FirebaseRecyclerOptions.Builder<Boolean>()
            .setQuery(query, Boolean::class.java)
            .build()
        adapter = object : FirebaseRecyclerAdapter<Boolean, BingoViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BingoViewHolder {
                val view = layoutInflater.inflate(R.layout.single_button, parent, false)
                return BingoViewHolder(view)
            }

            override fun onBindViewHolder(holder: BingoViewHolder, position: Int, model: Boolean) {
                holder.numberButton.setText(buttons.get(position).number.toString())
//                holder.numberButton.isEnabled = !model
            }

            override fun onChildChanged(
                type: ChangeEventType,
                snapshot: DataSnapshot,
                newIndex: Int,
                oldIndex: Int
            ) {
                super.onChildChanged(type, snapshot, newIndex, oldIndex)
                Log.d(TAG, "onChildChanged: $type / ${snapshot.key} / ${snapshot.value}")
                if (type == ChangeEventType.CHANGED) {
                    val number: Int = snapshot.key!!.toInt()
                    val is_picked: Boolean = snapshot.value as Boolean
                    val pos: Int = numberMap.getValue(number)
                    val holder: BingoViewHolder =
                        recycler.findViewHolderForAdapterPosition(pos) as BingoViewHolder
                    holder.numberButton.isEnabled = !is_picked
                }
            }
        }
        recycler.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    class BingoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numberButton = itemView.number_button
    }
}
