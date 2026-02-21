package net.vodbase.tv.data.model

enum class Channel(
    val id: String,
    val displayName: String,
    val theaterName: String
) {
    JERMA("jerma", "Jerma985", "Jerma985 Theater"),
    SIPS("sips", "Sips", "Sips Theater"),
    NL("nl", "Northernlion", "Northernlion Theater"),
    MOONMOON("moonmoon", "MOONMOON", "MOONMOON Theater");

    companion object {
        fun fromId(id: String): Channel = entries.first { it.id == id }
    }
}
