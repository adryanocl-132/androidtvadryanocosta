# Dark Glass Launcher — Android TV (TCL)

Tela inicial própria, escura e sem publicidade, com os 7 favoritos (Globoplay, Spotify, Netflix, YouTube, Prime Video, Disney+, HD Player Pro). Kotlin + Jetpack Compose, navegação 100% por controle remoto. Sem root, sem mexer no firmware, launcher original intacto.

## Estrutura

```
app/src/main/java/com/darkglass/launcher/
  MainActivity.kt          navegação entre telas, Voltar/Início
  model/Favorites.kt       lógica pura: favoritos, ordem, associação, saudação (testada)
  data/AppRepository.kt    descoberta de apps (PackageManager + <queries>), banners, abertura, alias HOME
  data/SettingsStore.kt    preferências locais (DataStore)
  ui/Glass.kt              paleta, papéis de parede, painel de vidro, botões focáveis
  ui/HomeScreen.kt         topo (nome, relógio, data, Ajustes), saudação, dock
  ui/SettingsScreen.kt     ajustes + seletor de app para associar atalhos
app/src/test/...           testes unitários JVM
.github/workflows/build.yml  compilação, testes, lint, verificação de assinatura, release
```

Decisões técnicas
- **minSdk 24 / targetSdk 35.** Cobre Android TV 7 a 15.
- **Vidro simulado por gradientes** (sem blur/RenderEffect): roda igual em qualquer chip de TV; fundo estático = zero custo por quadro.
- **Ícones reais**: usa o banner 16:9 de TV de cada app; se não houver, o ícone.
- **HD Player Pro**: pacote não presumido — descoberta pelo nome visível ou associação manual em Ajustes.
- **Tela inicial**: o manifesto traz um `activity-alias` HOME **desativado**. Só é ligado pelo botão "Oferecer como tela inicial" nos Ajustes, e desligado pelo botão oposto ou por "Sair e voltar à tela original".
- **Assinatura**: chave de teste fixa (`app/signing/darkglass-test.jks`, senha `darkglass`) para que cada build nova instale por cima da anterior. Não é chave de produção.
- Sem permissões, sem internet, sem telemetria, sem anúncios.

## 1. Compilar (GitHub Actions)

Cada push na `main` roda testes, lint, gera o APK, verifica a assinatura (`apksigner verify`) e o manifesto (`aapt2 dump badging`) e publica uma release com:

```
https://github.com/<usuario>/<repositorio>/releases/latest/download/DarkGlass.apk
```

O repositório precisa ser **público** para a TV baixar sem login. O hash SHA-256 de cada APK fica na release (`DarkGlass.apk.sha256`).

## 2. Instalar só com iPhone/iPad + TV (caminho recomendado)

Sem computador na rede, a instalação é **manual pela própria TV** (download HTTPS + instalador do Android) — não é ADB.

1. Na TV: Google Play → instale **Downloader** (AFTVnews).
2. Abra o Downloader e digite o link da release acima. Para digitar mais fácil, use o teclado do app Google TV no iPhone, se a sua TCL aceitar esse controle.
3. Ao terminar o download, toque **Instalar**. Na primeira vez a TV pede para permitir "instalar apps desconhecidos" **para o Downloader** — permita só para ele.
4. Abra **Dark Glass** pela lista de apps da TV.
5. Depois de instalar, apague o APK baixado quando o Downloader perguntar.

Atualizar: repita o passo 2 (mesma assinatura, instala por cima).

### Alternativa ADB pelo iPhone
Existe ao menos um cliente ADB para iOS na App Store (Bugjaeger – Mobile ADB), mas **não confirmei** que ele faz pareamento por código e instalação de APK na versão atual. Só use se a descrição da App Store confirmar as duas coisas. Fluxo, se confirmar:
TV → Configurações → Opções do desenvolvedor → Depuração sem fio → **Parear dispositivo com código** → digite IP:porta de pareamento e o código **no app do iPhone** (o código não deve ser enviado a ninguém) → conecte na porta de conexão (diferente da de pareamento) → instale o APK.

### Com um computador na mesma rede Wi-Fi
```
adb pair IP_DA_TV:PORTA_DE_PAREAMENTO      # código digitado localmente
adb connect IP_DA_TV:PORTA_DE_CONEXAO
adb install -r DarkGlass.apk
adb shell am start -n com.darkglass.launcher/.MainActivity
```
Antes de instalar, confira o hash: `shasum -a 256 DarkGlass.apk` deve bater com o `.sha256` da release.

## 3. Usar como tela inicial (opcional, só depois de testar)

Ajustes → Tela inicial → **Oferecer como tela inicial** → pressione **Início** no controle.
- Se a TV mostrar a escolha, selecione Dark Glass.
- Se nada mudar, a TCL não permite trocar o launcher por meios oficiais. O Dark Glass continua como app comum. Não use comandos que desativem o launcher original — este projeto não faz isso.

## 4. Recuperação e desinstalação

- **Voltar ao launcher original**: Ajustes → *Sair e voltar à tela original* (desliga a oferta como tela inicial e fecha o app). Ou Ajustes → *Não oferecer*.
- **Configurações do Android** sempre acessíveis: Ajustes → *Configurações do Android* (ou pelo botão de engrenagem/Configurações do próprio controle).
- **Desinstalar**: Configurações da TV → Apps → Dark Glass → Desinstalar. Via ADB: `adb uninstall com.darkglass.launcher`.
- Como o launcher original nunca é desativado, desinstalar o Dark Glass devolve a TV exatamente ao estado anterior.

## 5. Registro de testes

| Teste | Tipo | Status |
|---|---|---|
| Lógica de favoritos, ordem, associação, saudação | Unitário (CI) | Executado no GitHub Actions a cada build |
| Compilação, lint, assinatura, manifesto | Automático (CI) | Ver resumo da execução no Actions |
| Itens abaixo | Físico, na TCL | Pendente |

Checklist físico (marque na TV):
- [ ] Abre sem travar; relógio, data e saudação corretos
- [ ] Setas movem o foco no dock; foco sempre visível; OK abre o app
- [ ] Os 7 apps abrem; app ausente mostra "Associar"
- [ ] Voltar dentro do app de streaming retorna ao Dark Glass
- [ ] Ajustes: papel de parede, vidro, animações, ordem e associação salvam após reabrir
- [ ] HD Player Pro associado manualmente abre corretamente
- [ ] Fluidez aceitável com "Intenso"; se não, usar "Suave" e desativar animações
- [ ] Reiniciar a TV: Dark Glass continua instalado e configurado
- [ ] Configurações do Android abrem pelo Dark Glass
- [ ] Desinstalar: TV volta ao normal
