package com.darkglass.launcher

import com.darkglass.launcher.model.Favorites
import com.darkglass.launcher.model.Greeting
import com.darkglass.launcher.model.InstalledApp
import com.darkglass.launcher.model.Mappings
import com.darkglass.launcher.model.SlotResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavoritesTest {

    private val defaultOrder = listOf("globoplay", "spotify", "netflix", "youtube", "prime", "disney", "hdplayer")

    @Test fun `ordem padrão segue a especificação`() {
        assertEquals(defaultOrder, Favorites.defaultOrder())
        assertEquals(7, Favorites.DEFAULT.size)
    }

    @Test fun `ordem salva inválida é corrigida e completada`() {
        val parsed = Favorites.parseOrder("netflix,xyz,netflix,globoplay")
        assertEquals(listOf("netflix", "globoplay", "spotify", "youtube", "prime", "disney", "hdplayer"), parsed)
        assertEquals(defaultOrder, Favorites.parseOrder(null))
        assertEquals(defaultOrder, Favorites.parseOrder(""))
    }

    @Test fun `mover respeita as bordas`() {
        assertEquals(defaultOrder, Favorites.move(defaultOrder, "globoplay", -1))
        assertEquals("spotify", Favorites.move(defaultOrder, "globoplay", +1)[0])
        assertEquals("hdplayer", Favorites.move(defaultOrder, "hdplayer", +1).last())
        assertEquals(defaultOrder, Favorites.move(defaultOrder, "inexistente", 1))
    }

    @Test fun `mapeamentos ida e volta`() {
        val map = mapOf("hdplayer" to "com.exemplo.player", "netflix" to "com.netflix.ninja")
        assertEquals(map, Mappings.parse(Mappings.serialize(map)))
        assertEquals(emptyMap<String, String>(), Mappings.parse("lixo;=x;y="))
    }

    @Test fun `resolve por pacote conhecido, manual e nome`() {
        val installed = listOf(
            InstalledApp("com.netflix.ninja", "Netflix"),
            InstalledApp("com.google.android.youtube.tv", "YouTube"),
            InstalledApp("com.qualquer.player", "HD Player Pro"),
            InstalledApp("com.outro.app", "Outro"),
        )
        val netflix = Favorites.byId("netflix")!!
        val hd = Favorites.byId("hdplayer")!!
        val disney = Favorites.byId("disney")!!
        assertEquals("com.netflix.ninja", SlotResolver.resolve(netflix, installed, null))
        assertEquals("com.qualquer.player", SlotResolver.resolve(hd, installed, null))
        assertEquals("com.outro.app", SlotResolver.resolve(hd, installed, "com.outro.app"))
        // Associação manual a app desinstalado cai para a descoberta automática.
        assertEquals("com.qualquer.player", SlotResolver.resolve(hd, installed, "com.removido"))
        assertNull(SlotResolver.resolve(disney, installed, null))
    }

    @Test fun `saudação por horário`() {
        assertEquals("Bom dia", Greeting.forHour(5))
        assertEquals("Bom dia", Greeting.forHour(11))
        assertEquals("Boa tarde", Greeting.forHour(12))
        assertEquals("Boa tarde", Greeting.forHour(17))
        assertEquals("Boa noite", Greeting.forHour(18))
        assertEquals("Boa noite", Greeting.forHour(2))
    }
}
