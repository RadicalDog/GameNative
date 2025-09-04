package app.gamenative.enums

enum class Source(val value: Int) {
    /*
    Values are 0-31, and are inserted as first 5 bits of each appId
    This lets the appId lead directly to the Source, as well as the LibraryItem, as well as the container
     */

    STEAM(0), // Steam gets the luxury of being 0 and having its appIds unconverted, since SteamService is so big and uncompromising
    MANUAL(1),
    // EPIC,
    // GOG,
    // etc
}
