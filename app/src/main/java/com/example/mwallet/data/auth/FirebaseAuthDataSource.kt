package com.example.mwallet.data.auth

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseAuthDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private var pendingPhoneCredential: PhoneAuthCredential? = null

    suspend fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun registerWithEmailAndLinkPhone(
        email: String,
        password: String,
        phoneCredential: PhoneAuthCredential
    ) {
        auth.createUserWithEmailAndPassword(email, password).await()
        val user = auth.currentUser ?: error("Не удалось создать пользователя")
        user.linkWithCredential(phoneCredential).await()
    }

    suspend fun getIdToken(): String {
        val user = auth.currentUser ?: error("Пользователь Firebase не авторизован")
        return user.getIdToken(true).await().token ?: error("Не удалось получить token")
    }

    suspend fun sendPhoneCode(
        activity: Activity,
        phoneNumber: String,
        autoSignIn: Boolean = true
    ): String = suspendCoroutine { continuation ->
        pendingPhoneCredential = null
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                if (autoSignIn) {
                    auth.signInWithCredential(credential)
                        .addOnSuccessListener { continuation.resume("") }
                        .addOnFailureListener { continuation.resumeWithException(it) }
                } else {
                    pendingPhoneCredential = credential
                    continuation.resume("")
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                continuation.resumeWithException(e)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                continuation.resume(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun buildPhoneCredential(verificationId: String, code: String): PhoneAuthCredential {
        pendingPhoneCredential?.let { credential ->
            pendingPhoneCredential = null
            return credential
        }
        return PhoneAuthProvider.getCredential(verificationId, code)
    }

    suspend fun signInWithPhoneCredential(verificationId: String, code: String) {
        val credential = buildPhoneCredential(verificationId, code)
        auth.signInWithCredential(credential).await()
    }

    suspend fun updatePassword(newPassword: String) {
        val user = auth.currentUser ?: error("Пользователь не авторизован")
        user.updatePassword(newPassword).await()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://mwallet-b9f2b.firebaseapp.com/reset")
            .setHandleCodeInApp(true)
            .setAndroidPackageName("com.example.mwallet", true, null)
            .build()
        auth.sendPasswordResetEmail(email, actionCodeSettings).await()
    }

    suspend fun verifyPasswordResetCode(oobCode: String): String {
        return auth.verifyPasswordResetCode(oobCode.trim()).await()
    }

    suspend fun confirmPasswordReset(oobCode: String, newPassword: String) {
        auth.confirmPasswordReset(oobCode.trim(), newPassword).await()
    }

    fun signOut() {
        auth.signOut()
        pendingPhoneCredential = null
    }

    fun currentPhone(): String? = auth.currentUser?.phoneNumber
}
