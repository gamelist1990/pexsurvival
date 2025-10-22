package org.pexserver.koukunn.pexsurvival.Commands.Pex;

import org.pexserver.koukunn.pexsurvival.Core.Command.BaseCommand;
import org.pexserver.koukunn.pexsurvival.Core.Command.PermissionLevel;
import org.pexserver.koukunn.pexsurvival.Core.Command.CompletionUtils;
import org.pexserver.koukunn.pexsurvival.Core.Feature.FeatureManager;
import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /pex メインコマンド
 * サブコマンド: toggle, list, reload
 */
public class PexCommand extends BaseCommand {

    private final FeatureManager featureManager;

    public PexCommand(FeatureManager featureManager) {
        this.featureManager = featureManager;
    }

    @Override
    public String getName() {
        return "pex";
    }

    @Override
    public String getDescription() {
        return "PEX Survival の機能管理コマンド";
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ADMIN;  // 管理者のみ
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "toggle":
                return handleToggle(sender, args);

            case "list":
                return handleList(sender, args);

            case "reload":
                return handleReload(sender, args);

            default:
                sendError(sender, "不明なサブコマンド: " + subCommand);
                showHelp(sender);
                return false;
        }
    }

    /**
     * toggle サブコマンド処理
     */
    private boolean handleToggle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "使用法: /pex toggle <機能名>");
            return false;
        }

        String featureName = args[1].toLowerCase();

        if (!featureManager.hasFeature(featureName)) {
            sendError(sender, "機能が見つかりません: " + featureName);
            return false;
        }

        boolean enabled = featureManager.toggleFeature(featureName);
        String status = enabled ? "§a有効" : "§c無効";
        
        sender.sendMessage(Component.text("§b[PEX] " + featureName + " を " + status + "§b に切り替えました"));
        return true;
    }

    /**
     * list サブコマンド処理
     */
    private boolean handleList(CommandSender sender, String[] args) {
        sender.sendMessage(Component.text("§b========== 登録済み機能一覧 =========="));

        if (featureManager.getFeatures().isEmpty()) {
            sender.sendMessage(Component.text("§c登録済みの機能がありません"));
            sender.sendMessage(Component.text("§b========================================"));
            return true;
        }

        for (String name : featureManager.getFeatures().keySet()) {
            var feature = featureManager.getFeature(name);
            String status = feature.isEnabled() ? "§a有効" : "§c無効";
            sender.sendMessage(Component.text("§e• " + name + " " + status + 
                                             "§f - " + feature.getDescription()));
        }

        sender.sendMessage(Component.text("§b========================================"));
        return true;
    }

    /**
     * reload サブコマンド処理
     */
    private boolean handleReload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "使用法: /pex reload <機能名>");
            return false;
        }

        String featureName = args[1].toLowerCase();

        if (!featureManager.hasFeature(featureName)) {
            sendError(sender, "機能が見つかりません: " + featureName);
            return false;
        }

        if (featureManager.reloadFeature(featureName)) {
            sendSuccess(sender, "機能をリロードしました: " + featureName);
            return true;
        } else {
            sendError(sender, "機能のリロードに失敗しました: " + featureName);
            return false;
        }
    }

    /**
     * ヘルプを表示
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("§b========== /pex コマンドヘルプ =========="));
        sender.sendMessage(Component.text("§e/pex toggle <機能名>§f - 機能を切り替え（有効/無効）"));
        sender.sendMessage(Component.text("§e/pex list§f - 登録済み機能一覧を表示"));
        sender.sendMessage(Component.text("§e/pex reload <機能名>§f - 機能をリロード"));
        sender.sendMessage(Component.text("§b======================================"));
    }

    @Override
    public String getUsage() {
        return "/pex <toggle|list|reload>";
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return new ArrayList<>();
        }

        // サブコマンドの補完
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("toggle", "list", "reload");
            return CompletionUtils.filterBySimilarity(args[0], subCommands);
        }

        // 機能名の補完（toggle と reload の第2引数）
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if ("toggle".equals(subCommand) || "reload".equals(subCommand)) {
                List<String> featureNames = new ArrayList<>(featureManager.getFeatures().keySet());
                return CompletionUtils.filterBySimilarity(args[1], featureNames);
            }
        }

        return new ArrayList<>();
    }
}
