package at.asitplus.signum.supreme.os

import at.asitplus.shouldSucceed
import at.asitplus.signum.supreme.azString
import at.asitplus.testballoon.matrix.matrixSuite
import io.kotest.matchers.shouldBe
import kotlin.random.Random


import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

val IosKeychainProviderTest by matrixSuite {
    "Creating a key with an alias that already exists" - {

        "fails but leaves the existing key untouched when the new config is broken" {
            val alias = Random.azString(32)
            try {
                // an existing key lives under this alias
                val original = IosKeychainProvider.createSigningKey(alias).getOrThrow()

                // attempt to create another key with the same alias, using a config that throws
                val secondAttempt = IosKeychainProvider.createSigningKey(alias) {
                    throw IllegalStateException("intentionally broken config")
                }
                secondAttempt.isFailure shouldBe true

                // the original key must still be present and unchanged
                val recovered = IosKeychainProvider.getSignerForKey(alias)
                recovered.shouldSucceed()
                recovered.getOrThrow().publicKey shouldBe original.publicKey
            } finally {
                IosKeychainProvider.deleteSigningKey(alias)
            }
        }

        "fails but leaves the existing key untouched when the new config is valid" {
            val alias = Random.azString(32)
            try {
                val original = IosKeychainProvider.createSigningKey(alias).getOrThrow()

                val secondAttempt = IosKeychainProvider.createSigningKey(alias)
                secondAttempt.isFailure shouldBe true

                val recovered = IosKeychainProvider.getSignerForKey(alias)
                recovered.shouldSucceed()
                recovered.getOrThrow().publicKey shouldBe original.publicKey
            } finally {
                IosKeychainProvider.deleteSigningKey(alias)
            }
        }
    }

    "Signing operation" - {
        "cancels cleanly when the enclosing coroutine is cancelled" {
            val alias = Random.azString(32)
            try {
                val signer = IosKeychainProvider.createSigningKey(alias).getOrThrow()
                val job = GlobalScope.launch {
                    signer.sign(byteArrayOf(1, 2, 3, 4))
                }
                yield()
                job.cancel()
                job.join()
                job.isCancelled shouldBe true
            } finally {
                IosKeychainProvider.deleteSigningKey(alias)
            }
        }
    }
}
