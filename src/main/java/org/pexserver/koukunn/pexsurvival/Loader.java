package org.pexserver.koukunn.pexsurvival;

import org.bukkit.plugin.java.JavaPlugin;
import org.pexserver.koukunn.pexsurvival.Core.Command.CommandManager;
import org.pexserver.koukunn.pexsurvival.Core.Feature.FeatureManager;
import org.pexserver.koukunn.pexsurvival.Commands.Pex.PexCommand;
import org.pexserver.koukunn.pexsurvival.Commands.help.HelpCommand;
import org.pexserver.koukunn.pexsurvival.Module.NoJump.NoJumpFeature;

public final class Loader extends JavaPlugin {

    private CommandManager commandManager;
    private FeatureManager featureManager;
    private org.pexserver.koukunn.pexsurvival.Core.Config.ConfigManager configManager;

    @Override
    public void onEnable() {
        // マネージャーを初期化
        commandManager = new CommandManager(this);
        featureManager = new FeatureManager(this);
        // ConfigManager を初期化（PEXConfig フォルダを作成）
        configManager = new org.pexserver.koukunn.pexsurvival.Core.Config.ConfigManager(this);
        // ConfigManager を初期化（PEXConfig フォルダを作成）

        // 機能を登録
        registerFeatures();
        // 機能登録後に設定のクリーンアップ（不要なエントリを削除）を行う
        if (featureManager != null) {
            featureManager.cleanupConfig();
        }

        // コマンドを登録
        registerCommands();


        getLogger().info("PEX Survival Plugin が有効になりました");
    }

    /**
     * すべての機能を登録
     */
    private void registerFeatures() {
        featureManager.registerFeature(new NoJumpFeature());
        // 他の機能はここに追加できます
    }

    /**
     * すべてのコマンドを登録
     */
    private void registerCommands() {
        // ヘルプコマンド
        commandManager.register(new HelpCommand(commandManager));
        
        // /pex コマンド（機能管理）
        commandManager.register(new PexCommand(featureManager));

        // 他のコマンドはここに追加できます
    }

    /**
     * CommandManager を取得
     * @return CommandManager インスタンス
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * FeatureManager を取得
     * @return FeatureManager インスタンス
     */
    public FeatureManager getFeatureManager() {
        return featureManager;
    }

    public org.pexserver.koukunn.pexsurvival.Core.Config.ConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public void onDisable() {
        // 全機能を無効化
        if (featureManager != null) {
            featureManager.disableAll();
        }
        
        getLogger().info("PEX Survival Plugin が無効になりました");
    }
}
