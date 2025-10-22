package org.pexserver.koukunn.pexsurvival.Core.Feature;

import org.bukkit.event.Listener;

/**
 * プラグイン機能の基底インターフェース
 * すべての機能はこのインターフェースを実装し、イベントリスナーとして機能します
 */
public interface Feature extends Listener {

    /**
     * 機能の名前を返します
     * @return 機能名（/pex toggle <name> で使用）
     */
    String getFeatureName();

    /**
     * 機能の説明を返します
     * @return 機能の説明
     */
    String getDescription();

    /**
     * 機能が有効かどうかを返します
     * @return 有効な場合 true
     */
    boolean isEnabled();

    /**
     * 機能を有効にします
     */
    void enable();

    /**
     * 機能を無効にします
     */
    void disable();

    /**
     * 機能をリロードします
     */
    void reload();

    /**
     * この機能のデフォルトの有効/無効値を返します。
     * 既存互換のためデフォルトは true（従来の振る舞い）です。
     * 実行環境では Config の "states" が存在する場合はそれを優先し、
     * "states" が存在するが当該キーが無ければ無効とみなします。
     */
    default boolean getDefaultEnabled() {
        return false;
    }
}
