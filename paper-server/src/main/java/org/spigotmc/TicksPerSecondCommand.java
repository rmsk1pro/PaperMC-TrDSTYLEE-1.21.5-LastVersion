package org.spigotmc;

import com.google.common.base.Joiner;
import net.minecraft.server.MinecraftServer;
import com.google.common.collect.Iterables;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class TicksPerSecondCommand extends Command
{

    public TicksPerSecondCommand(String name)
    {
        super( name );
        this.description = "Obtém os ticks atuais por segundo para o servidor";
        this.usageMessage = "/tps";
        this.setPermission( "bukkit.command.tps" );
    }

    @Override
    public boolean execute(CommandSender sender, String currentAlias, String[] args)
    {
        if ( !testPermission( sender ) )
        {
            return true;
        }

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        String progress = "§f[" + getProgressBar(heapUsage.getUsed(), heapUsage.getMax(), 20, "|", "§a", "§7") + "§f]";
        String startTime = CalendarioAPI.format2(ManagementFactory.getRuntimeMXBean().getStartTime()) + ".";

        int chunks = 0, entitys = 0;
        for (World w : Bukkit.getWorlds()) {
            chunks = w.getLoadedChunks().length;
            entitys = w.getEntities().size();
        }


        sender.sendMessage("§1");
        sender.sendMessage("§e Informações de momento sobre o servidor:");
        sender.sendMessage("§2");
        sender.sendMessage("§b■ §f Status: §aOnline");
        sender.sendMessage("§b■ §f Tempo online: §a" + startTime + "");
        sender.sendMessage("§b■ §f Chunks: §a" + chunks + "");
        sender.sendMessage("§b■ §f Entidades: §a" + entitys + "");
        sender.sendMessage("§b■ §f RAM: §a" + getRamUsage());
        sender.sendMessage("§b■ §f Jogadores: §a" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        sender.sendMessage("§b■ §f Versão: §a" + Bukkit.getBukkitVersion());
        sender.sendMessage("§b■ §f Processadores CPU: §a" + Runtime.getRuntime().availableProcessors());
        sender.sendMessage("§b■ §f Sistema operacional: §3" + System.getProperty("os.name"));
        sender.sendMessage("§b■ §f Load Average: §a" + String.valueOf(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage()));
        sender.sendMessage("");
        StringBuilder sb = new StringBuilder("§b■  §fTPS:§a " );
        for ( double tps : MinecraftServer.getServer().server.getTPS() ) {
            sb.append( format( tps ) );
            sb.append( ", " );
        }
        sender.sendMessage( sb.substring( 0, sb.length() - 2 ) );
        //sender.sendMessage("§b■ §f TPS: §a" + LagAPI.getTPSCOLOR());
        sender.sendMessage("");
        sender.sendMessage("§b■ §f Uso de RAM:");
        sender.sendMessage("§b■ §a " + progress + "");
        //sender.sendMessage("§b■ §a " + getRamUsageBar());
        //sender.sendMessage("§b■ §4");
        sender.sendMessage("");
        return true;
    }


    private static String getRamUsageBar() {
        final long maxmem = getMaxMemory();
        final long usedmem = getUsedMemory();
        final float percent = usedmem / (float)maxmem * 100.0f;
        final int steps = (int)(percent / 5.0f);
        String membar = "§r§2";
        char color = 'c';
        for (int i = 0; i <= 30; ++i) {
            if (i >= steps) {
                color = '7';
            }
            membar = String.valueOf(membar) + "§" + color + "|";
        }
        return membar;
    }

    public static long getTotalMemory() {
        return Runtime.getRuntime().totalMemory() / 1048576L;
    }

    public static long getFreeMemory() {
        return Runtime.getRuntime().freeMemory() / 1048576L;
    }

    public static long getUsedMemory() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576L;
    }

    public static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory() / 1048576L;
    }

    public static String getRamUsage() {
        final float percent = getUsedMemory() / (float)getMaxMemory() * 100.0f;
        final String ret = "§7[" + getUsedMemory() + "MB/" + getMaxMemory() + "MB] §a(" + Math.floor(percent) + "%)";
        return ret;
    }

    public static String getProgressBar(double current, double max, int totalBars, String symbol, String completedColor, String notCompletedColor) {
        float percent = (float) current / (float) max;
        int progressBars = (int) ((float) totalBars * percent);
        int leftOver = totalBars - progressBars;
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(TXT.parse(completedColor));

        int index;
        for (index = 0; index < progressBars; ++index) stringBuilder.append(symbol);

        stringBuilder.append(TXT.parse(notCompletedColor));

        for (index = 0; index < leftOver; ++index) stringBuilder.append(symbol);

        return stringBuilder.toString();
    }

    private String format(double tps)
    {
        return ( ( tps > 18.0 ) ? ChatColor.GREEN : ( tps > 16.0 ) ? ChatColor.YELLOW : ChatColor.RED ).toString()
            + ( ( tps > 20.0 ) ? "*" : "" ) + Math.min( Math.round( tps * 100.0 ) / 100.0, 20.0 );
    }
}
