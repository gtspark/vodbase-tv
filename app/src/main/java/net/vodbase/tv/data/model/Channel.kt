package net.vodbase.tv.data.model

enum class Channel(
    val id: String,
    val displayName: String,
    val theaterName: String,
    val tagline: String,
    val approxVods: String,
    val years: String,
    val enterText: String,
    val avatarUrl: String
) {
    JERMA("jerma", "JERMA985", "Jerma985 Theater",
        "The psycho streamer's complete archive",
        "1000+", "15", "ENTER THE CHAOS",
        "https://vodbase.net/images/jerma.png"),
    SIPS("sips", "SIPS_", "Sips Theater",
        "The real guy, the best guy",
        "4800+", "7", "PLAY TAPE",
        "https://vodbase.net/images/sips.png"),
    NL("nl", "NORTHERNLION", "Northernlion Theater",
        "The bald genius's complete collection",
        "800+", "9", "EXECUTE",
        "https://vodbase.net/images/northernlion.png"),
    MOONMOON("moonmoon", "MOONMOON", "MOONMOON Theater",
        "The variety king's complete collection",
        "3900+", "6", "ENTER DOMAIN",
        "https://vodbase.net/images/moonmoon.png");

    companion object {
        fun fromId(id: String): Channel = entries.first { it.id == id }
    }
}
