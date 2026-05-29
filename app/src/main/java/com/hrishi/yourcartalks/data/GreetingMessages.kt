package com.hrishi.yourcartalks.data

object GreetingMessages {

    private val messages = listOf(
        // Performance / Hype Vibes
        "Strap in. %1\$s is ready to run.",
        "%1\$s is live. Let's go.",
        "Engine warm. %1\$s awaits.",
        "%1\$s \u2014 full throttle mode activated.",
        "Systems go. %1\$s is yours.",

        // Racing / Track Inspired
        "Driver, %1\$s is ready for launch.",
        "Lights out. %1\$s is go.",
        "%1\$s on the grid. Drive well.",
        "Pit lane's clear. %1\$s is ready.",
        "Green flag. %1\$s is live.",

        // Sci-Fi / KITT Vibes
        "Good to see you. %1\$s online.",
        "%1\$s systems initialized. Drive safe.",
        "AI core loaded. %1\$s at your command.",
        "%1\$s is online. Where are we headed?",

        // Attitude / Personality
        "%1\$s never keeps you waiting.",
        "You and %1\$s. Let's make it count.",
        "%1\$s is fueled and ready. Are you?",
        "%1\$s \u2014 because ordinary is boring.",

        // Time-Based (Morning / Night)
        "Good morning. %1\$s is ready for the day.",
        "Late night drive? %1\$s has got you."
    )

    fun all(): List<String> = messages

    fun format(message: String, carName: String): String {
        return String.format(message, carName)
    }

    fun random(): String = messages.random()

    fun preview(message: String, carName: String): String {
        return format(message, carName)
    }
}
