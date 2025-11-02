package io.papermc.paper.command.subcommands;

import io.papermc.paper.command.PaperSubcommand;
import net.minecraft.server.MinecraftServer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

@DefaultQualifier(NonNull.class)
public final class ReloadCommand implements PaperSubcommand {
    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        this.doReload(sender);
        return true;
    }

    private void doReload(final CommandSender sender) {
        Command.broadcastCommandMessage(sender, text("Por favor, note que este comando não é suportado e pode causar problemas.", RED));
        Command.broadcastCommandMessage(sender, text("Se você encontrar qualquer problema, use o comando /stop para reiniciar seu servidor.", RED));

        MinecraftServer server = ((CraftServer) sender.getServer()).getServer();
        server.paperConfigurations.reloadConfigs(server);
        server.server.reloadCount++;

        Command.broadcastCommandMessage(sender, text("Recarregamento da configuração do Paper completo.", GREEN));
    }
}
