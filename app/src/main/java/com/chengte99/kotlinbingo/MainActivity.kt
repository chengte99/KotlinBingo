package com.chengte99.kotlinbingo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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
            Log.d(TAG, "onAuthStateChanged: ${it.uid}")
            it.displayName?.run {
                FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(it.uid)
                    .child("displayName")
                    .setValue(this)
                    .addOnCompleteListener { Log.d(TAG, "onAuthStateChanged: done"); }
            }
        } ?: signUp()

        /*if (auth.currentUser != null) {
            Log.d(TAG, "onAuthStateChanged: ${auth.currentUser?.email} / ${auth.currentUser?.uid}")
        }else {
            signUp()
        }*/
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
