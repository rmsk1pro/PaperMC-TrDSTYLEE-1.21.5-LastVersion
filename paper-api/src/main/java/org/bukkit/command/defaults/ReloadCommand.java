package org.bukkit.command.defaults;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand extends BukkitCommand {
    public ReloadCommand(@NotNull String name) {
        super(name);
        this.description = "Recarrega a configuração do servidor e plugins";
        this.usageMessage = "/reload";
        this.setPermission("bukkit.command.reload");
        this.setAliases(Arrays.asList("rl"));
    }

    @org.jetbrains.annotations.ApiStatus.Internal // Paper
    public static final String RELOADING_DISABLED_MESSAGE = "Não é possível recarregar os plugins pois um manipulador de eventos do ciclo de vida está registrado"; // Paper

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String currentAlias, @NotNull String[] args) { // Paper
        if (!testPermission(sender)) return true;

        // Paper start - Reload permissions.yml & require confirm
        boolean confirmed = System.getProperty("LetMeReload") != null;
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("permissions")) {
                Bukkit.getServer().reloadPermissions();
                Command.broadcastCommandMessage(sender, net.kyori.adventure.text.Component.text("Permissões recarregadas com sucesso.", net.kyori.adventure.text.format.NamedTextColor.GREEN));
                return true;
            } else if ("commands".equalsIgnoreCase(args[0])) {
                if (Bukkit.getServer().reloadCommandAliases()) {
                    Command.broadcastCommandMessage(sender, net.kyori.adventure.text.Component.text("Aliases de comandos recarregados com sucesso.", net.kyori.adventure.text.format.NamedTextColor.GREEN));
                } else {
                    Command.broadcastCommandMessage(sender, net.kyori.adventure.text.Component.text("Ocorreu um erro ao tentar recarregar os aliases de comandos.", net.kyori.adventure.text.format.NamedTextColor.RED));
                }
                return true;
            } else if ("confirm".equalsIgnoreCase(args[0])) {
                confirmed = true;
            } else {
                Command.broadcastCommandMessage(sender, net.kyori.adventure.text.Component.text("Use: " + usageMessage, net.kyori.adventure.text.format.NamedTextColor.RED));
                return true;
            }
        }
        if (!confirmed) {
            Command.broadcastCommandMessage(sender, net.kyori.adventure.text.Component.text("Tem certeza de que deseja recarregar o servidor? Isso pode causar bugs e vazamentos de memória. Recomenda-se reiniciar o servidor em vez de usar /reload. Para confirmar, por favor digite", net.kyori.adventure.text.format.NamedTextColor.RED).append(net.kyori.adventure.text.Component.text("/reload confirm", net.kyori.adventure.text.format.NamedTextColor.YELLOW)));
            return true;
        }
        // Paper end

        Command.broadcastCommandMessage(sender, ChatColor.RED + "✔ Esse comando pode ocorrer bugs nos seus plugins.");
        Command.broadcastCommandMessage(sender, ChatColor.RED + "✔ Qualquer erro use /stop...");
        // Paper start - lifecycle events
        try {
            Bukkit.reload();
        } catch (final IllegalStateException ex) {
            if (ex.getMessage().equals(RELOADING_DISABLED_MESSAGE)) {
                Command.broadcastCommandMessage(sender, ChatColor.RED + RELOADING_DISABLED_MESSAGE);
                return true;
            }
        }
        // Paper end - lifecycle events
        Command.broadcastCommandMessage(sender, ChatColor.GREEN + " ✔ Reload completado.");

        return true;
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        return com.google.common.collect.Lists.newArrayList("permissions", "commands"); // Paper
    }
}
