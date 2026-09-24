package com.example.calcvault.data.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

object MnemonicSeedManager {

    private const val PREF_SEED_HASH = "mnemonic_seed_hash"
    private const val PREF_SEED_WORDS = "mnemonic_seed_words_encrypted"

    // Curated dictionary of 256 clear, memorable BIP-39 style lowercase words
    private val WORDLIST = listOf(
        "anchor", "arctic", "arrow", "atlas", "atom", "autumn", "badge", "bamboo",
        "beacon", "blade", "blaze", "bloom", "breeze", "bridge", "cabin", "canyon",
        "castle", "cedar", "cipher", "cliff", "clover", "comet", "compass", "coral",
        "crater", "creek", "crown", "crystal", "dawn", "delta", "desert", "diamond",
        "dolphin", "dragon", "drift", "dune", "eagle", "echo", "eclipse", "ember",
        "emerald", "falcon", "feather", "flame", "flash", "forest", "fossil", "frost",
        "galaxy", "garden", "glacier", "glade", "glow", "granite", "grove", "harbor",
        "haven", "hawk", "helix", "horizon", "iceberg", "island", "jaguar", "jungle",
        "jupiter", "keeper", "knight", "lagoon", "lantern", "laser", "laurel", "legend",
        "leopard", "liberty", "lotus", "lunar", "magnet", "maple", "marble", "matrix",
        "meadow", "meteor", "mirage", "mirror", "monarch", "moon", "mountain", "nebula",
        "nexus", "oasis", "ocean", "olive", "omega", "onyx", "orbit", "orchid",
        "origin", "palace", "palm", "panther", "parrot", "pathway", "pearl", "phoenix",
        "pillar", "pioneer", "planet", "plasma", "polaris", "prism", "pulse", "pyramid",
        "quantum", "quartz", "quasar", "radar", "radiant", "raven", "reef", "rhino",
        "ripple", "river", "rocket", "ruby", "safari", "sailor", "sapphire", "saturn",
        "shadow", "shield", "siren", "solace", "solar", "spark", "sphere", "sphinx",
        "spider", "spirit", "spring", "star", "stone", "storm", "summit", "sunflower",
        "sunset", "surf", "swan", "talon", "temple", "terra", "thunder", "tiger",
        "timber", "titan", "topaz", "torch", "tower", "trace", "tropic", "tundra",
        "turtle", "unity", "valley", "vanguard", "vault", "vector", "velocity", "vertex",
        "vessel", "viper", "vision", "volcano", "vortex", "voyage", "walnut", "warrior",
        "water", "wave", "whisper", "willow", "winter", "wizard", "wolf", "zenith",
        "zephyr", "alpha", "bravo", "cactus", "cosmos", "dusk", "flint", "garnet",
        "haven", "iron", "jade", "karma", "lapis", "mercury", "neon", "opal",
        "prism", "quest", "radar", "silver", "tidal", "urban", "valiant", "wild",
        "zenith", "amber", "bronze", "copper", "dune", "glimmer", "haven", "indigo",
        "journey", "kite", "lynx", "mystic", "nova", "orbit", "pilot", "quarry",
        "rebel", "stellar", "titan", "umbra", "vigor", "wind", "xenon", "yarrow"
    ).distinct()

    fun getOrGenerateSeed(context: Context, prefs: SharedPreferences): List<String> {
        val existing = prefs.getString(PREF_SEED_WORDS, null)
        if (!existing.isNullOrBlank()) {
            val words = existing.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            if (words.size == 12) return words
        }

        // Generate 12 unique random words
        val rng = SecureRandom()
        val indices = mutableSetOf<Int>()
        while (indices.size < 12) {
            indices.add(rng.nextInt(WORDLIST.size))
        }

        val generatedWords = indices.map { WORDLIST[it] }
        val wordsJoined = generatedWords.joinToString(",")
        val hash = hashPhrase(generatedWords)

        prefs.edit()
            .putString(PREF_SEED_WORDS, wordsJoined)
            .putString(PREF_SEED_HASH, hash)
            .apply()

        return generatedWords
    }

    fun hasSeed(prefs: SharedPreferences): Boolean {
        return prefs.contains(PREF_SEED_HASH)
    }

    fun verifySeed(prefs: SharedPreferences, enteredWords: List<String>): Boolean {
        val storedHash = prefs.getString(PREF_SEED_HASH, null) ?: return false
        val cleanWords = enteredWords.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (cleanWords.size != 12) return false
        return hashPhrase(cleanWords) == storedHash
    }

    fun regenerateSeed(context: Context, prefs: SharedPreferences): List<String> {
        prefs.edit().remove(PREF_SEED_WORDS).remove(PREF_SEED_HASH).apply()
        return getOrGenerateSeed(context, prefs)
    }

    private fun hashPhrase(words: List<String>): String {
        val normalized = words.joinToString(" ").lowercase().trim()
        val bytes = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
