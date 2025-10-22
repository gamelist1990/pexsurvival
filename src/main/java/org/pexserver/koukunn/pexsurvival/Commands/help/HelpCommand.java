package org.pexserver.koukunn.pexsurvival.Commands.help;

import org.bukkit.command.CommandSender;
import org.pexserver.koukunn.pexsurvival.Core.Command.BaseCommand;
import org.pexserver.koukunn.pexsurvival.Core.Command.CommandManager;
import org.pexserver.koukunn.pexsurvival.Core.Command.PermissionLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ヘルプコマンド
 * プラグインで利用可能なコマンド一覧を表示します
 */
public class HelpCommand extends BaseCommand {

    private final CommandManager commandManager;

    public HelpCommand(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public String getName() {
        return "pexhelp";
    }

    @Override
    public String getDescription() {
        return "PEX Survival のコマンド一覧を表示";
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ANY;  // 誰でも実行可能
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage("§b========== PEX Survival コマンド一覧 ==========");

        for (BaseCommand cmd : commandManager.getCommands().values()) {
            PermissionLevel permLevel = cmd.getPermissionLevel();
            String permText = "§e[" + permLevel.getDescription() + "]";
            sender.sendMessage("§f/" + cmd.getName() + " " + permText + " - " + cmd.getDescription());
        }

        sender.sendMessage("§b==========================================");
        return true;
    }

    @Override
    public String getUsage() {
        return "/pexhelp [ページ番号]";
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return new ArrayList<>();
        }

        // ページ番号の補完
        if (args.length == 1) {
            List<String> pages = new ArrayList<>();
            int totalPages = (int) Math.ceil((double) commandManager.getCommands().size() / COMMANDS_PER_PAGE);
            
            for (int i = 1; i <= totalPages; i++) {
                pages.add(String.valueOf(i));
            }
            
            return pages.stream()
                    .filter(p -> p.startsWith(args[0]))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    private static final int COMMANDS_PER_PAGE = 5;
}
