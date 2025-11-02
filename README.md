
<img width="1630" height="930" alt="PaperTPS" src="https://github.com/user-attachments/assets/9aedc822-cbe8-4408-8509-8dd1cc8c07ac" />

<img width="1636" height="898" alt="PaperEnderChest" src="https://github.com/user-attachments/assets/42c6eb1e-0a5c-43f9-8d44-4865e21ab184" />

<img width="1220" height="368" alt="Pl" src="https://github.com/user-attachments/assets/acbf614d-7ce9-4b1d-b5c1-2d1680940edf" />




# 🧩 PaperSpigot 1.21.5 – Edição Modificada por TrDSTYLEE

🚀 Uma versão especial do servidor Paper, otimizada e adaptada para servidores brasileiros!

---

## ✨ Principais Atualizações

### ✅ Algumas Traduções para PT-BR  
🔤 Diversos comandos e mensagens do console/jogador traduzidos para o português brasileiro, melhorando a experiência nativa.

### ✅ Comandos Modificados e Otimizados  
🛠️ Alterações e melhorias em comandos como:  
- `/pl`, `/ver`, `/version`, `/tps`, `/reload`  
- Bloqueios, aliases personalizados e ajustes de segurança.

### ✅ Comando TPS com Estilo  
📊 O comando `/tps` foi reformulado para exibir os ticks com precisão e visual limpo e legível.

### ✅ Melhorias de Desempenho  
⚙️ Ajustes internos focados em estabilidade, consumo de memória e desempenho geral do servidor.

### ✅ Verificação de Versão Desativada  
🔒 Sem chamadas externas ao GitHub. Console limpo e livre de erros 403.

### ✅ Console com Suporte ANSI e UTF-8  
🌈 Exibição com cores reais, ícones visuais e codificação internacional ativada para total compatibilidade.

---

## 🔧 Informações Técnicas

- **Base:** Paper 1.21.5 (última versão estável)
- **Modificado por:** `TrDSTYLEE`
- **Atualizado em:** Julho de 2025

---

## 💡 Ideal para

✔️ Servidores PvP  
✔️ Survival  
✔️ Criativo  
✔️ Arenas  
✔️ Redes com sistema próprio e alta personalização

---

## 💬 Comunidade

Foco em acessibilidade, desempenho e suporte para servidores brasileiros!  
Sinta-se à vontade para usar, modificar e compartilhar.

---


### ✅ EnderChest Plus (54 slots)  
📦 Clique com o botão direito em um **Ender Chest físico** e uma versão expandida será aberta:  
- **Slots expandidos (54)** se a versão/modificação permitir  
- Som de abertura e fechamento customizados  
- Comando adicional: `/echestplus`

```java
@EventHandler
public void onEnderChestOpen(PlayerInteractEvent event) {
    if (event.getAction() == Action.RIGHT_CLICK_BLOCK &&
        event.getClickedBlock() != null &&
        event.getClickedBlock().getType() == Material.ENDER_CHEST) {

        event.setCancelled(true); // Impede a abertura padrão

        Player player = event.getPlayer();
        Inventory realEnderChest = player.getEnderChest(); // Pode estar modificado para 54 slots

        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
        player.openInventory(realEnderChest);
    }
}

@EventHandler
public void onEnderChestClose(InventoryCloseEvent event) {
    if (event.getInventory().equals(event.getPlayer().getEnderChest())) {
        Player player = (Player) event.getPlayer();
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 1.0f, 1.0f);
    }
}