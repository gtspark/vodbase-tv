package net.vodbase.tv.data.model

enum class Channel(
    val id: String,
    val displayName: String,
    val tagline: String,
    val approxVods: String,
    val years: String,
    val enterText: String,
    val avatarUrl: String
) {
    JERMA("jerma", "JERMA985",
        "The psycho streamer's complete archive",
        "1000+", "15", "ENTER THE CHAOS",
        "https://vodbase.net/images/jerma.png"),
    SIPS("sips", "SIPS_",
        "The real guy, the best guy",
        "4800+", "7", "PLAY TAPE",
        "https://vodbase.net/images/sips.png"),
    NL("nl", "NORTHERNLION",
        "The bald genius's complete collection",
        "800+", "9", "EXECUTE",
        "https://vodbase.net/images/northernlion.png"),
    MOONMOON("moonmoon", "MOONMOON",
        "The variety king's complete collection",
        "3900+", "6", "ENTER DOMAIN",
        "https://vodbase.net/images/moonmoon.png");

    companion object {
        fun fromId(id: String): Channel = entries.firstOrNull { it.id == id } ?: JERMA
    }
}
