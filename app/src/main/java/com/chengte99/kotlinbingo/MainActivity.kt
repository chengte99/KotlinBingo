package com.chengte99.kotlinbingo

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.room_row.view.*
import java.util.*

class MainActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener, View.OnClickListener {
    companion object {
        val TAG = MainActivity::class.java.simpleName
        val RC_LOGIN = 100
    }

    private lateinit var adapter: FirebaseRecyclerAdapter<GameRoom, RoomViewHolder>
    private var member: Member? = null

    var avatarIds = intArrayOf(
        R.drawable.avatar_0,
        R.drawable.avatar_1,
        R.drawable.avatar_2,
        R.drawable.avatar_3,
        R.drawable.avatar_4
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: ")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nickname.setOnClickListener(){
            FirebaseAuth.getInstance().currentUser?.let {
                showNicknameDialog(it.uid, nickname.text.toString())
            }
        }
        group_avatars.visibility = View.GONE
        avatar.setOnClickListener(){
            group_avatars.visibility = if (group_avatars.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        avatar_0.setOnClickListener(this)
        avatar_1.setOnClickListener(this)
        avatar_2.setOnClickListener(this)
        avatar_3.setOnClickListener(this)
        avatar_4.setOnClickListener(this)

        fab.setOnClickListener(){
            val roomEdit = EditText(this)
            roomEdit.setText("Welcome")
            AlertDialog.Builder(this)
                .setTitle("Game room")
                .setMessage("Please input room title")
                .setView(roomEdit)
                .setPositiveButton("OK") { dialog, which ->
                    val room = GameRoom(roomEdit.text.toString(), member)
                    FirebaseDatabase.getInstance()
                        .getReference("rooms")
                        .push()
                        .setValue(room)
                }
                .show()
        }

        recycler.setHasFixedSize(true)
        recycler.layoutManager = LinearLayoutManager(this)

        val query = FirebaseDatabase.getInstance().getReference("rooms")
            .limitToFirst(30)
        val options = FirebaseRecyclerOptions.Builder<GameRoom>()
            .setQuery(query, GameRoom::class.java)
            .build()
        adapter = object : FirebaseRecyclerAdapter<GameRoom, RoomViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
                val view = layoutInflater.inflate(R.layout.room_row, parent, false)
                return RoomViewHolder(view)
            }

            override fun onBindViewHolder(holder: RoomViewHolder, position: Int, model: GameRoom) {
                holder.roomAvatar.setImageResource(avatarIds[model.init!!.avatar])
                holder.roomTitle.setText(model.title)
            }
        }
        recycler.adapter = adapter
    }

    class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var roomAvatar = itemView.room_avatar
        var roomTitle = itemView.room_title
    }

    override fun onStart() {
        Log.d(TAG, "onStart: ")
        super.onStart()

        FirebaseAuth.getInstance().addAuthStateListener(this)
        adapter.startListening()
    }

    override fun onStop() {
        Log.d(TAG, "onStop: ")
        super.onStop()

        FirebaseAuth.getInstance().removeAuthStateListener(this)
        adapter.stopListening()
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        Log.d(TAG, "onAuthStateChanged: ")
        auth.currentUser?.also {
            Log.d(TAG, "onAuthStateChanged: ${it.email} ${it.uid}")
            it.displayName?.run {
                FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(it.uid)
                    .child("displayName")
                    .setValue(this)
                    .addOnCompleteListener { Log.d(TAG, "onAuthStateChanged: done"); }
            }

            FirebaseDatabase.getInstance().getReference("users")
                .child(it.uid)
                .child("uid")
                .setValue(it.uid)

            FirebaseDatabase.getInstance().getReference("users")
                .child(it.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {

                    }

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        member = dataSnapshot.getValue(Member::class.java)
                        member?.nickname?.also { nick ->
                            Log.d(TAG, "onDataChange: ${nick}")
                            nickname.setText(nick)
                        } ?: showNicknameDialog(it)
                        member?.also {
                            avatar.setImageResource(avatarIds[it.avatar])
                        }
                    }
                })

//            FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(it.uid)
//                .child("nickname")
//                .addListenerForSingleValueEvent(object : ValueEventListener {
//                    override fun onCancelled(error: DatabaseError) {
//
//                    }
//
//                    override fun onDataChange(dataSnapshot: DataSnapshot) {
//                        dataSnapshot.value?.also {nick ->
//                            Log.d(TAG, "onDataChange: ${nick}")
//                        } ?: showNicknameDialog(it)
//                    }
//                })
        } ?: signUp()

        /*if (auth.currentUser != null) {
            Log.d(TAG, "onAuthStateChanged: ${auth.currentUser?.email} / ${auth.currentUser?.uid}")
        }else {
            signUp()
        }*/
    }

    private fun showNicknameDialog(uid: String, nick: String?) {
        val edNickname = EditText(this)
        edNickname.setText(nick)
        AlertDialog.Builder(this)
            .setTitle("Your nickname")
            .setMessage("Please input your nickname")
            .setView(edNickname)
            .setPositiveButton("OK") { dialog, which ->
                FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .child("nickname")
                    .setValue(edNickname.text.toString())
            }
            .show()
    }

    private fun showNicknameDialog(user: FirebaseUser) {
        val uid = user.uid;
        val nick = user.displayName

        showNicknameDialog(uid, nick)
    }

    private fun signUp() {
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setAvailableProviders(Arrays.asList(
                                AuthUI.IdpConfig.EmailBuilder().build(),
                                AuthUI.IdpConfig.GoogleBuilder().build()
                        ))
                        .setIsSmartLockEnabled(false)
                        .build(),
                RC_LOGIN)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
//        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_menu_signout -> FirebaseAuth.getInstance().signOut()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(view: View?) {
        val selected = when(view!!.id) {
            R.id.avatar_0 -> 0
            R.id.avatar_1 -> 1
            R.id.avatar_2 -> 2
            R.id.avatar_3 -> 3
            R.id.avatar_4 -> 4
            else -> 0
        }
        group_avatars.visibility = View.GONE
        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child("avatar")
            .setValue(selected)
    }
}
