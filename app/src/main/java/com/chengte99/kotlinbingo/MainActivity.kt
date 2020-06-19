package com.chengte99.kotlinbingo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener {
    companion object {
        val TAG = MainActivity::class.java.simpleName
        val RC_LOGIN = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: ")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        Log.d(TAG, "onStart: ")
        super.onStart()

        FirebaseAuth.getInstance().addAuthStateListener(this)
    }

    override fun onStop() {
        Log.d(TAG, "onStop: ")
        super.onStop()

        FirebaseAuth.getInstance().removeAuthStateListener(this)
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        Log.d(TAG, "onAuthStateChanged: ")
        auth.currentUser?.also {
            Log.d(TAG, "onAuthStateChanged: ${it.email} ${it.uid}")
            FirebaseDatabase.getInstance().getReference("users")
                .child(it.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {

                    }

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val member = dataSnapshot.getValue(Member::class.java)
                        member?.nickname?.also { nick ->
                            Log.d(TAG, "onDataChange: ${nick}")
                            nickname.setText(nick)
                        } ?: showNicknameDialog(it)
                    }
                })

            it.displayName?.run {
                FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(it.uid)
                    .child("displayName")
                    .setValue(this)
                    .addOnCompleteListener { Log.d(TAG, "onAuthStateChanged: done"); }
            }

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

    private fun showNicknameDialog(user: FirebaseUser) {
        val edNickname = EditText(this)
        edNickname.setText(user.displayName)
        AlertDialog.Builder(this)
            .setTitle("Your nickname")
            .setMessage("Please input your nickname")
            .setView(edNickname)
            .setPositiveButton("OK") { dialog, which ->
                FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.uid)
                    .child("nickname")
                    .setValue(edNickname.text.toString())
            }
            .show()
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
}
