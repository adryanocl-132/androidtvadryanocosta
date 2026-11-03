package com.darkglass.launcher.model

/**
 * Lógica pura (sem Android) dos sete atalhos favoritos — testável em JVM.
 */

data class AppSlot(
    val id: String,
    val title: String,
    /** Pacotes conhecidos, em ordem de preferência (versão TV primeiro). */
    val knownPackages: List<String>,
    /** Trechos do nome visível usados como último recurso na descoberta. */
    val labelHints: List<String>,
)

data class InstalledApp(val packageName: String, val label: String)

data class ResolvedSlot(val slot: AppSlot, val packageName: String?, val appLabel: String?) {
    val isInstalled: Boolean get() = packageName != null
}

object Favorites {
    val DEFAULT: List<AppSlot> = listOf(
        AppSlot("globoplay", "Globoplay", listOf("com.globo.globotv"), listOf("globoplay")),
        AppSlot("spotify", "Spotify", listOf("com.spotify.tv.android", "com.spotify.music"), listOf("spotify")),
        AppSlot("netflix", "Netflix", listOf("com.netflix.ninja", "com.netflix.mediaclient"), listOf("netflix")),
        AppSlot(
            "youtube", "YouTube",
            listOf("com.google.android.youtube.tv", "com.google.android.youtube.googletv", "com.google.android.youtube"),
            listOf("youtube"),
        ),
        AppSlot(
            "prime", "Prime Video",
            listOf("com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"),
            listOf("prime video"),
        ),
        AppSlot("disney", "Disney+", listOf("com.disney.disneyplus"), listOf("disney+", "disney")),
        // Pacote do HD Player Pro NÃO é presumido: descoberta pelo nome ou associação manual.
        AppSlot("hdplayer", "HD Player Pro", emptyList(), listOf("hd player pro", "hdplayer pro", "hd player")),
    )

    private val ids = DEFAULT.map { it.id }

    fun byId(id: String): AppSlot? = DEFAULT.firstOrNull { it.id == id }

    fun defaultOrder(): List<String> = ids

    /** Ordem salva → lista válida e completa (ids desconhecidos descartados, faltantes no fim). */
    fun parseOrder(raw: String?): List<String> {
        val parsed = raw.orEmpty().split(',').map { it.trim() }.filter { it in ids }.distinct()
        return parsed + ids.filterNot { it in parsed }
    }

    fun serializeOrder(order: List<String>): String = order.joinToString(",")

    /** Move o atalho [id] em [delta] posições, limitado às bordas. */
    fun move(order: List<String>, id: String, delta: Int): List<String> {
        val from = order.indexOf(id)
        if (from < 0) return order
        val to = (from + delta).coerceIn(0, order.lastIndex)
        if (to == from) return order
        val list = order.toMutableList()
        list.removeAt(from)
        list.add(to, id)
        return list
    }
}

object Mappings {
    /** "slot=pacote;slot=pacote" → mapa. */
    fun parse(raw: String?): Map<String, String> =
        raw.orEmpty().split(';').mapNotNull { entry ->
            val i = entry.indexOf('=')
            if (i <= 0 || i == entry.lastIndex) null
            else entry.substring(0, i).trim() to entry.substring(i + 1).trim()
        }.filter { it.first.isNotEmpty() && it.second.isNotEmpty() }.toMap()

    fun serialize(map: Map<String, String>): String =
        map.entries.joinToString(";") { "${it.key}=${it.value}" }
}

object SlotResolver {
    /**
     * Prioridade: associação manual (se ainda instalada) → pacote conhecido → nome visível.
     */
    fun resolve(slot: AppSlot, installed: Collection<InstalledApp>, manual: String?): String? {
        val packages = installed.mapTo(HashSet()) { it.packageName }
        if (manual != null && manual in packages) return manual
        slot.knownPackages.firstOrNull { it in packages }?.let { return it }
        for (hint in slot.labelHints) {
            installed.firstOrNull { it.label.lowercase().contains(hint) }?.let { return it.packageName }
        }
        return null
    }

    fun resolveAll(
        order: List<String>,
        installed: List<InstalledApp>,
        manual: Map<String, String>,
    ): List<ResolvedSlot> = order.mapNotNull { id ->
        val slot = Favorites.byId(id) ?: return@mapNotNull null
        val pkg = resolve(slot, installed, manual[id])
        ResolvedSlot(slot, pkg, installed.firstOrNull { it.packageName == pkg }?.label)
    }
}

object Greeting {
    fun forHour(hour: Int): String = when (hour) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }
}
